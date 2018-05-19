package org.wikipathways.cytoscapeapp.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.wikipathways.cytoscapeapp.Annots;

public class GpmlReaderFactoryImpl implements GpmlReaderFactory  {

	private WPManager manager;
	public WPManager getWPManager() { return manager;	}
	private WPClient client;
	@Override public void setClient(WPClient inClient) {	client = inClient;  manager.setClient(client); }
	@Override public  WPClient getClient() {	return client;}
	
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
	private GpmlNetworkStyle networkStyle;
    String organism;
	CyServiceRegistrar registrar;
  final Map<CyNetwork,GpmlConversionMethod> conversionMethods = new HashMap<CyNetwork,GpmlConversionMethod>();
  final Map<CyNetwork,List<DelayedVizProp>> pendingVizProps = new HashMap<CyNetwork,List<DelayedVizProp>>();

  public GpmlReaderFactoryImpl(CyServiceRegistrar registrar)
  {
	  System.out.println("GpmlReaderFactoryImpl");
	  this.registrar = registrar;
	  eventHelper = registrar.getService(CyEventHelper.class);
      netMgr =  registrar.getService(CyNetworkManager.class);
      netNaming = registrar.getService(CyNetworkNaming.class);
      netFactory = registrar.getService(CyNetworkFactory.class);
      netViewMgr = registrar.getService(CyNetworkViewManager.class);
      netViewFactory = registrar.getService(CyNetworkViewFactory.class);
      layoutMgr = registrar.getService(CyLayoutAlgorithmManager.class);
//      networkTF = registrar.getService(NetworkTaskFactory.class);
      annots = new Annots(registrar);
      vizStyle = new GpmlVizStyle(registrar);
      networkStyle = new GpmlNetworkStyle(registrar);
      manager = new WPManager(registrar,annots, this );
   }
//--------------------------------------------------------------
  public TaskIterator createReader(final String id, final Reader gpmlContents, final CyNetwork network, 
		  final GpmlConversionMethod conversionMethod, final boolean setNetworkName) {
    conversionMethods.put(network, conversionMethod);
    return new TaskIterator(new ReaderTask(manager, gpmlContents, network, conversionMethod, setNetworkName));
  }

  public TaskIterator createViewBuilder(final String id, final CyNetwork gpmlNetwork, final CyNetworkView networkView) {

    final GpmlConversionMethod method = conversionMethods.get(gpmlNetwork);
    if (method == null) 
        throw new IllegalArgumentException("gpmlNetwork is invalid");
    boolean importNetwork = GpmlConversionMethod.NETWORK.equals(method);

    final TaskIterator iterator = new TaskIterator();
    iterator.append(new ApplyVizProps(gpmlNetwork, networkView));
    if (importNetwork) 
    		applyLayout(networkView, iterator);
    iterator.append(new UpdateViewTask(networkView, !importNetwork));
    return iterator;
  }

  // CyLayoutAlgorithm.createTaskIterator() must be run inside its own AbstractTask
  // because the tasks that make up the task iterator are not known at
  // the time when createTaskIterator() is invoked
  private void applyLayout(final CyNetworkView networkView, TaskIterator iterator)
  {
      final CyLayoutAlgorithm layout = layoutMgr.getLayout("force-directed");
      iterator.append(new AbstractTask() {
        public void run(TaskMonitor monitor) {
          final TaskIterator layoutIterator = layout.createTaskIterator(networkView, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
          super.insertTasksAfterCurrentTask(layoutIterator);
        }
      });
  
  }

  public TaskIterator createReaderAndViewBuilder(final String id, final Reader gpmlContents, final GpmlConversionMethod conversionMethod) {
	  System.out.println("createReaderAndViewBuilderOuter");
   final CyNetwork network = netFactory.createNetwork();
    network.getRow(network).set(CyNetwork.NAME, id);
    netMgr.addNetwork(network);
    final CyNetworkView view = netViewFactory.createNetworkView(network);
    netViewMgr.addNetworkView(view);
    return createReaderAndViewBuilder(id, gpmlContents, view, conversionMethod, true);
  }

  public TaskIterator createReaderAndViewBuilder(final String id,  final Reader gpmlContents, final CyNetworkView networkView,
	      final GpmlConversionMethod conversionMethod, final boolean setNetworkName) 
	  {
		  System.out.println("createReaderAndViewBuilder");
	    final TaskIterator iterator = new TaskIterator();
	    final CyNetwork network = networkView.getModel();
	    iterator.append(createReader(id, gpmlContents, network, conversionMethod, setNetworkName));
	    iterator.append(createViewBuilder(id, network, networkView));
	    iterator.append(new EnsemblIdColumnTask(network, registrar, organism));
	    return iterator;
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
        final boolean setNetworkName) 
    {
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
			PathwayElement info = pathway.getMappInfo();
			organism = info.getOrganism();
			if (setNetworkName) {
				final String name = info.getMapInfoName() + " - " + organism;
				final String nonConflictingName = netNaming.getSuggestedNetworkTitle(name);
				network.getRow(network).set(CyNetwork.NAME, nonConflictingName);
				if (network instanceof CySubNetwork) {
					final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
					root.getRow(root).set(CyNetwork.NAME, nonConflictingName);
				}
			}

			List<DelayedVizProp> vizProps = null;
			switch (conversionMethod) {
			case PATHWAY: 	vizProps = (new GpmlToPathway(manager, eventHelper, annots, pathway, network)).convert(); 	break;
			case NETWORK:  (new GpmlToNetwork(eventHelper, pathway, network)).convert();	 				break;
			}

			if (vizProps != null)
				pendingVizProps.put(network, vizProps);

		} catch (Exception e) {
			throw new Exception("Pathway not available -- invalid GPML", e);
		} finally {
			// manager.turnOnEvents();
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
      if (vizProps != null)
    	  	vizProps.clear(); // be nice to the GC
      pendingVizProps.remove(network);
    }
  }

//-----------------------------------------------------------------------
static String ENSEMBL_COLUMN = "Ensembl";

class EnsemblIdColumnTask extends AbstractTask {
    private static final boolean VERBOSE = false;
	final CyNetwork network;
    final CyServiceRegistrar registrar;
    final CyTable table;
    
    public EnsemblIdColumnTask(final CyNetwork network, final CyServiceRegistrar reg, String organism) {
        this.network = network;
        table = network.getDefaultNodeTable();
       this.registrar = reg;
    }

    public void run(TaskMonitor monitor) 
    {
    		System.out.println("running EnsemblIdColumnTask " + organism);
    		if (bridgeDbAvailable())			//ensemblColumn == null && 
   			buildIdMapBatch();
    }

	private boolean bridgeDbAvailable() {
		
//		registrar.getService(null, "");
		return true;
	}

	private void buildIdMapBatch() {
		HashMap<Long, String> map = new HashMap<Long, String>();
		List<String> sources = new ArrayList<String>();
		CyColumn ensemblColumn  = table.getColumn(ENSEMBL_COLUMN);
		if  (ensemblColumn == null)
			table.createColumn(ENSEMBL_COLUMN, String.class, true);
		System.out.println("\nbuildIdMapBatch\n");
		List<CyRow> rows = table.getAllRows();
		if (rows.isEmpty()) 
			{
			System.out.println("rows.isEmpty() ");
			return;
			}
		CyRow first = rows.get(0);
		String firstSource = first.get("XRefDataSource", String.class);
		System.out.println("firstXRefDataSource: " + firstSource);
		boolean monoSourced = true;
		for (CyRow row : table.getAllRows())
		{
			Long suid = row.get("SUID", Long.class);
			String id = row.get("XRefId", String.class);
			String src = row.get("XRefDataSource", String.class);
			String name = row.get("name", String.class);
			String wptype = row.get("WP.type", String.class);
			String type = row.get("Type", String.class);
			
			if (suid == null || id == null || src == null) continue;
			if (map.get(suid) != null) continue;
			
			if (VERBOSE) System.out.print(suid + ": " + id + "  \t" + src + "\t" + name + "\t" + type + "\t" + wptype);
			String[] goodTypeArray = { "Gene", "GeneProduct", "RNA", "Protein" };
			List<String> goodTypes = Arrays.asList(goodTypeArray);
			boolean good = goodTypes.contains(wptype);
			if (VERBOSE) System.out.println("\t" + (good ? "Y" : "N"));

			if (!good) continue;
			monoSourced &= src.equals(firstSource);
			String record = id + "\t" + src + "\t" + name;
			map.put(suid, record);
			if (!sources.contains(src))
				sources.add(src);
		}
		System.out.println("Mono: " + monoSourced + "\n\n");
		if (!monoSourced)
		{
			for (String s : sources)
			{
				System.out.println(s);
				for (Long suid : map.keySet())
				{
					String fields = map.get(suid);
					if (fields.contains(s))
					{
						System.out.println("\t\t" + fields);
					}
				}
				System.out.println();
			}
		}
	}
    
  }

//-----------------------------------------------------------------------
  class UpdateViewTask extends AbstractTask implements ObservableTask {
    final CyNetworkView view;
  
    boolean importAsPathway;
    public UpdateViewTask(final CyNetworkView view, boolean importAsPathway) 
    { 
    	this.view = view;
    	this.importAsPathway = importAsPathway;
    }

    public void run(TaskMonitor monitor) throws Exception {
      monitor.setTitle("Build network view");
      try {
        updateView(importAsPathway);
      } catch (Exception e) {  throw new Exception("Failed to build view", e);  }
    }

    
    private void updateView(boolean importAsPathway) throws Exception {
      if (!SwingUtilities.isEventDispatchThread()) {
        SwingUtilities.invokeAndWait(new Runnable() {
          public void run() {
            try {
              updateViewInner(importAsPathway);
            } catch (Exception e) { e.printStackTrace();  }
          }
        });
      } else  updateViewInner(importAsPathway);
    }

    private void updateViewInner(boolean importAsPathway) throws Exception {
    if (importAsPathway) 	vizStyle.apply(view);
     else    				networkStyle.apply(view);
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

