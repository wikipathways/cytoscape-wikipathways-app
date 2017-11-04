package org.wikipathways.cytoscapeapp.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.pathvisio.core.model.Pathway;
import org.wikipathways.cytoscapeapp.Annots;

public class GpmlReaderFactoryImpl implements GpmlReaderFactory  {

	private WPManager manager;
	private CyEventHelper eventHelper;
	private CyNetworkFactory netFactory;
	private CyNetworkManager netMgr;
	private CyNetworkNaming netNaming;
	private CyNetworkViewFactory netViewFactory;
	private CyNetworkViewManager netViewMgr;
	final CyLayoutAlgorithmManager layoutMgr;
//	private NetworkTaskFactory networkTF;
	private Annots annots;
	private GpmlVizStyle vizStyle;

  final Map<CyNetwork,GpmlConversionMethod> conversionMethods = new HashMap<CyNetwork,GpmlConversionMethod>();
  final Map<CyNetwork,List<DelayedVizProp>> pendingVizProps = new HashMap<CyNetwork,List<DelayedVizProp>>();

  public GpmlReaderFactoryImpl(CyServiceRegistrar registrar)
     {
	  System.out.println("GpmlReaderFactoryImpl");
	  eventHelper = registrar.getService(CyEventHelper.class);
      netMgr =  registrar.getService(CyNetworkManager.class);
      netNaming = registrar.getService(CyNetworkNaming.class);
      netFactory = registrar.getService(CyNetworkFactory.class);
      netViewMgr = registrar.getService(CyNetworkViewManager.class);
      netViewFactory = registrar.getService(CyNetworkViewFactory.class);
      layoutMgr = registrar.getService(CyLayoutAlgorithmManager.class);
//      networkTF = registrar.getService(NetworkTaskFactory.class);
      annots = new Annots(
    		  registrar.getService(AnnotationManager.class),
              (AnnotationFactory<ArrowAnnotation>) registrar.getService(AnnotationFactory.class,"(type=ArrowAnnotation.class)"),
              (AnnotationFactory<ShapeAnnotation>)registrar.getService( AnnotationFactory.class,"(type=ShapeAnnotation.class)"),
              (AnnotationFactory<TextAnnotation>) registrar.getService( AnnotationFactory.class,"(type=TextAnnotation.class)"));
      vizStyle = new GpmlVizStyle(
    		  registrar.getService( VisualStyleFactory.class),
    		  registrar.getService( VisualMappingManager.class),
    		  registrar.getService( VisualMappingFunctionFactory.class, "(mapping.type=continuous)"),
              registrar.getService( VisualMappingFunctionFactory.class, "(mapping.type=discrete)"),
              registrar.getService( VisualMappingFunctionFactory.class, "(mapping.type=passthrough)"));
      manager = new WPManager(registrar,annots, this );
      }

  public TaskIterator createReader(final String id, final Reader gpmlContents, final CyNetwork network, 
		  final GpmlConversionMethod conversionMethod, final boolean setNetworkName) {
    conversionMethods.put(network, conversionMethod);
    return new TaskIterator(new ReaderTask(manager, gpmlContents, network, conversionMethod, setNetworkName));
  }

  public TaskIterator createViewBuilder(final String id, final CyNetwork gpmlNetwork, final CyNetworkView networkView) {
    if (!conversionMethods.containsKey(gpmlNetwork)) 
      throw new IllegalArgumentException("gpmlNetwork is invalid");

    final GpmlConversionMethod method = conversionMethods.get(gpmlNetwork);
    final TaskIterator iterator = new TaskIterator();
    iterator.append(new ApplyVizProps(gpmlNetwork, networkView));

    if (GpmlConversionMethod.NETWORK.equals(method)) {
      final CyLayoutAlgorithm layout = layoutMgr.getLayout("force-directed");
      // CyLayoutAlgorithm.createTaskIterator() must be run inside its own AbstractTask
      // because the tasks that make up the task iterator are not known at
      // the time when createTaskIterator() is invoked
      iterator.append(new AbstractTask() {
        public void run(TaskMonitor monitor) {
          final TaskIterator layoutIterator = layout.createTaskIterator(networkView, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
          super.insertTasksAfterCurrentTask(layoutIterator);
        }
      });
    }

//    iterator.append(networkTF.createTaskIterator(gpmlNetwork));
    iterator.append(new UpdateViewTask(networkView));
    return iterator;
  }

  public TaskIterator createReaderAndViewBuilder(final String id,  final Reader gpmlContents, final CyNetworkView networkView,
      final GpmlConversionMethod conversionMethod, final boolean setNetworkName) 
  {
	  System.out.println("createReaderAndViewBuilder");
    final TaskIterator iterator = new TaskIterator();
    final CyNetwork network = networkView.getModel();
    iterator.append(createReader(id, gpmlContents, network, conversionMethod, setNetworkName));
    iterator.append(createViewBuilder(id, network, networkView));
    return iterator;
  }

  public TaskIterator createReaderAndViewBuilder(final String id, final Reader gpmlContents, final GpmlConversionMethod conversionMethod) {
	  System.out.println("createReaderAndViewBuilder2");
   final CyNetwork network = netFactory.createNetwork();
    network.getRow(network).set(CyNetwork.NAME, id);
    netMgr.addNetwork(network);
    final CyNetworkView view = netViewFactory.createNetworkView(network);
    netViewMgr.addNetworkView(view);
    return createReaderAndViewBuilder(id, gpmlContents, view, conversionMethod, true);
  }
//-----------------------------------------------------------------------
  class ReaderTask extends AbstractTask {
    volatile Reader gpmlContents = null;
    final CyNetwork network;
    final GpmlConversionMethod conversionMethod;
    final boolean setNetworkName;
    final WPManager manager;

    public ReaderTask(
        final WPManager wpMgr,
        final Reader gpmlContents,
        final CyNetwork network,
        final GpmlConversionMethod conversionMethod,
        final boolean setNetworkName
      ) {
        this.manager = wpMgr;
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
//      char buf[] = new char[1000000];
//      gpmlContents.reset();
//      int len = gpmlContents.read(buf);
//      String buffer = String.copyValueOf(buf);
//      System.out.println(buffer);
      if (setNetworkName) {
        final String name = pathway.getMappInfo().getMapInfoName();
        final String nonConflictingName = netNaming.getSuggestedNetworkTitle(name);
        network.getRow(network).set(CyNetwork.NAME, nonConflictingName);
        if (network instanceof CySubNetwork) {
          final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
          root.getRow(root).set(CyNetwork.NAME, nonConflictingName);
        }
      }

      List<DelayedVizProp> vizProps = null;
      switch(conversionMethod) {
      case PATHWAY:  	vizProps = (new GpmlToPathway(manager, eventHelper, annots, pathway, network)).convert(); 	break;
      case NETWORK: 	vizProps = (new GpmlToNetwork(eventHelper, pathway, network)).convert(); 		 break;
      }

       pendingVizProps.put(network, vizProps);
      } catch (Exception e) { throw new Exception("Pathway not available -- invalid GPML", e);  }
      finally
      {
//    	  manager.turnOnEvents();
      }
    }

    public void cancel() {
      final Reader gpmlContents2 = gpmlContents;
      if (gpmlContents2 == null) return;
      try {
          gpmlContents2.close();
        } catch (IOException e) {}
        gpmlContents = null;
   }
  }

//-----------------------------------------------------------------------
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
      try { Thread.sleep(1000); } catch (Exception e) {} // wait for flush payload events to take hold
      DelayedVizProp.applyAll(view, vizProps, manager); // apply our visual style
      vizProps.clear(); // be nice to the GC
      pendingVizProps.remove(network);
    }
  }

//-----------------------------------------------------------------------
  class UpdateViewTask extends AbstractTask implements ObservableTask {
    final CyNetworkView view;
  
    public UpdateViewTask(final CyNetworkView view) 
    { 
    	this.view = view;
    }

    public void run(TaskMonitor monitor) throws Exception {
      monitor.setTitle("Build network view");
      try {
        updateView();
      } catch (Exception e) {  throw new Exception("Failed to build view", e);  }
    }

    
    private void updateView() throws Exception {
      if (!SwingUtilities.isEventDispatchThread()) {
        SwingUtilities.invokeAndWait(new Runnable() {
          public void run() {
            try {
              updateViewInner();
            } catch (Exception e) { e.printStackTrace();  }
          }
        });
      } else  updateViewInner();
    }

    private void updateViewInner() throws Exception {
      vizStyle.apply(view);
      view.fitContent();
      view.updateView();
    }

    public <R> R getResults(Class<? extends R> type) {
      final CyNetwork network = view.getModel();
      if (String.class.equals(type))  		return (R) network.getRow(network).get(CyNetwork.NAME, String.class);
      if (Long.class.equals(type))         	return (R) network.getSUID();
      if (CyNetwork.class.equals(type))     return (R) network;
      if (CyNetworkView.class.equals(type)) return (R) view;
      return null;
    }
  }
}

