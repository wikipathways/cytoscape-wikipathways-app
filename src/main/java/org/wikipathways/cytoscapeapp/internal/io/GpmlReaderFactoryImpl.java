package org.wikipathways.cytoscapeapp.internal.io;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.Reader;
import java.io.IOException;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkView;

import org.cytoscape.event.CyEventHelper;

import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.AbstractTask;

import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.layout.CyLayoutAlgorithm;

import org.pathvisio.core.model.Pathway;

import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;

public class GpmlReaderFactoryImpl implements GpmlReaderFactory  {

  final CyEventHelper eventHelper;
  final CyNetworkFactory netFactory;
  final CyNetworkManager netMgr;
  final CyNetworkNaming netNaming;
  final CyNetworkViewFactory netViewFactory;
  final CyNetworkViewManager netViewMgr;
  final CyLayoutAlgorithmManager layoutMgr;
  final NetworkTaskFactory showLODTF;
  final Annots annots;
  final GpmlVizStyle vizStyle;

  final Map<CyNetwork,GpmlConversionMethod> conversionMethods = new HashMap<CyNetwork,GpmlConversionMethod>();
  final Map<CyNetwork,List<DelayedVizProp>> pendingVizProps = new HashMap<CyNetwork,List<DelayedVizProp>>();

  public GpmlReaderFactoryImpl(
      final CyEventHelper eventHelper,
      final CyNetworkFactory netFactory,
      final CyNetworkManager netMgr,
      final CyNetworkNaming netNaming,
      final CyNetworkViewFactory netViewFactory,
      final CyNetworkViewManager netViewMgr,
      final CyLayoutAlgorithmManager layoutMgr,
      final NetworkTaskFactory showLODTF,
      final Annots annots,
      final GpmlVizStyle vizStyle
    ) {
    this.eventHelper = eventHelper;
    this.netFactory = netFactory;
    this.netMgr = netMgr;
    this.netNaming = netNaming;
    this.netViewFactory = netViewFactory;
    this.netViewMgr = netViewMgr;
    this.layoutMgr = layoutMgr;
    this.showLODTF = showLODTF;
    this.annots = annots;
    this.vizStyle = vizStyle;
  }

  public TaskIterator createReader(
      final Reader gpmlContents,
      final CyNetwork network,
      final GpmlConversionMethod conversionMethod,
      final boolean setNetworkName) {
    conversionMethods.put(network, conversionMethod);
    return new TaskIterator(new ReaderTask(gpmlContents, network, conversionMethod, setNetworkName));
  }

  public TaskIterator createViewBuilder(final CyNetwork gpmlNetwork, final CyNetworkView networkView) {
    if (!conversionMethods.containsKey(gpmlNetwork)) {
      throw new IllegalArgumentException("gpmlNetwork is invalid");
    }
    final GpmlConversionMethod method = conversionMethods.get(gpmlNetwork);

    final TaskIterator iterator = new TaskIterator();

    iterator.append(new ApplyVizProps(gpmlNetwork, networkView));

    if (GpmlConversionMethod.NETWORK.equals(method)) {
      final CyLayoutAlgorithm layout = layoutMgr.getLayout("force-directed");
      // CyLayoutAlgorithm.createTaskIterator() must be run inside its own AbstractTask
      // because the tasks that make up the task iterator are not known at construction time
      iterator.append(new AbstractTask() {
        public void run(TaskMonitor monitor) {
          final TaskIterator layoutIterator = layout.createTaskIterator(networkView, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
          super.insertTasksAfterCurrentTask(layoutIterator);
        }
      });
    }

    iterator.append(showLODTF.createTaskIterator(gpmlNetwork));

    iterator.append(new UpdateViewTask(networkView));

    return iterator;
  }

  public TaskIterator createReaderAndViewBuilder(
      final Reader gpmlContents,
      final CyNetworkView networkView,
      final GpmlConversionMethod conversionMethod,
      final boolean setNetworkName) {
    final TaskIterator iterator = new TaskIterator();
    final CyNetwork network = networkView.getModel();
    iterator.append(createReader(gpmlContents, network, conversionMethod, setNetworkName));
    iterator.append(createViewBuilder(network, networkView));
    return iterator;
  }

  public TaskIterator createReaderAndViewBuilder(final Reader gpmlContents, final GpmlConversionMethod conversionMethod) {
    final CyNetwork network = netFactory.createNetwork();
    netMgr.addNetwork(network);
    final CyNetworkView view = netViewFactory.createNetworkView(network);
    netViewMgr.addNetworkView(view);

    return createReaderAndViewBuilder(gpmlContents, view, conversionMethod, true);
  }

  class ReaderTask extends AbstractTask {
    volatile Reader gpmlContents = null;
    final CyNetwork network;
    final GpmlConversionMethod conversionMethod;
    final boolean setNetworkName;

    public ReaderTask(
        final Reader gpmlContents,
        final CyNetwork network,
        final GpmlConversionMethod conversionMethod,
        final boolean setNetworkName
      ) {
      this.gpmlContents = gpmlContents;
      this.network = network;
      this.conversionMethod = conversionMethod;
      this.setNetworkName = setNetworkName;
    }

    public void run(TaskMonitor monitor) throws Exception {
      monitor.setTitle("Construct network");

      monitor.setStatusMessage("Parsing pathways file");
      final Pathway pathway = new Pathway();
      try {
        pathway.readFromXml(gpmlContents, true);
      } catch (Exception e) {
        throw new Exception("Pathway not available -- invalid GPML", e);
      }

      if (setNetworkName) {
        final String name = pathway.getMappInfo().getMapInfoName();
        network.getRow(network).set(CyNetwork.NAME, netNaming.getSuggestedNetworkTitle(name));
      }

      List<DelayedVizProp> vizProps = null;
      switch(conversionMethod) {
      case PATHWAY:
        vizProps = (new GpmlToPathway(eventHelper, annots, pathway, network)).convert();
        break;
      case NETWORK:
        vizProps = (new GpmlToNetwork(eventHelper, pathway, network)).convert();
        break;
      }

      pendingVizProps.put(network, vizProps);
    }

    public void cancel() {
      final Reader gpmlContents2 = gpmlContents;
      if (gpmlContents2 != null) {
        try {
          gpmlContents2.close();
        } catch (IOException e) {}
        gpmlContents = null;
      }
    }
  }

  class ApplyVizProps extends AbstractTask {
    final CyNetwork network;
    final CyNetworkView view;

    public ApplyVizProps(final CyNetwork network, final CyNetworkView view) {
      this.network = network;
      this.view = view;
    }

    public void run(TaskMonitor monitor) {
      final List<DelayedVizProp> vizProps = pendingVizProps.get(network);
      eventHelper.flushPayloadEvents(); // guarantee that all node and edge views have been created
      DelayedVizProp.applyAll(view, vizProps); // apply our visual style
      vizProps.clear(); // be nice to the GC
      pendingVizProps.remove(vizProps);
    }
  }

  class UpdateViewTask extends AbstractTask {
    final CyNetworkView view;
    public UpdateViewTask(final CyNetworkView view) {
      this.view = view;
    }

    public void run(TaskMonitor monitor) {
      monitor.setTitle("Build network view");

      vizStyle.apply(view);
      view.fitContent();
      view.updateView();
    }
  }
}

