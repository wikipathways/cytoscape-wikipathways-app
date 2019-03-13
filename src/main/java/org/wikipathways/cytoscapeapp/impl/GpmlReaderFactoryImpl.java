package org.wikipathways.cytoscapeapp.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
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
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.internal.cmd.EnsemblIdTask;


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
//	private Annots annots;
	private GpmlVizStyle vizStyle;
	private GpmlNetworkStyle networkStyle;
    String organism;
	CyServiceRegistrar registrar;
  final Map<CyNetwork,GpmlConversionMethod> conversionMethods = new HashMap<CyNetwork,GpmlConversionMethod>();
  final Map<CyNetwork,List<DelayedVizProp>> pendingVizProps = new HashMap<CyNetwork,List<DelayedVizProp>>();

  private boolean semaphore = false;
  
	public void setSemaphore()
	{
		  System.out.println("setSemaphore");
		  if (semaphore)
			  System.out.println("FAIL");
		  else semaphore = true;

		
	}
	public void clearSemaphore()
	{
		  System.out.println("clearSemaphore");
		  semaphore = false;
	}


  public GpmlReaderFactoryImpl(CyServiceRegistrar registrar)
  {
//	  System.out.println("GpmlReaderFactoryImpl");
	  this.registrar = registrar;
	  eventHelper = registrar.getService(CyEventHelper.class);
      netMgr =  registrar.getService(CyNetworkManager.class);
      netNaming = registrar.getService(CyNetworkNaming.class);
      netFactory = registrar.getService(CyNetworkFactory.class);
      netViewMgr = registrar.getService(CyNetworkViewManager.class);
      netViewFactory = registrar.getService(CyNetworkViewFactory.class);
      layoutMgr = registrar.getService(CyLayoutAlgorithmManager.class);
//      networkTF = registrar.getService(NetworkTaskFactory.class);
      vizStyle = new GpmlVizStyle(registrar);
      networkStyle = new GpmlNetworkStyle(registrar);
      manager = new WPManager(registrar, this );
   }
//--------------------------------------------------------------
  public TaskIterator createReader(final String id, final Reader gpmlContents, final CyNetwork network, 
		  final GpmlConversionMethod conversionMethod) {
    conversionMethods.put(network, conversionMethod);
    ReaderTask t = new ReaderTask(manager, gpmlContents, network, conversionMethod);
    return new TaskIterator(t);
  }

  public TaskIterator createViewBuilder(final String id, final CyNetwork gpmlNetwork, final CyNetworkView networkView) {

    final GpmlConversionMethod method = conversionMethods.get(gpmlNetwork);
    if (method == null) 
        throw new IllegalArgumentException("gpmlNetwork is invalid");
    boolean importNetwork = GpmlConversionMethod.NETWORK.equals(method);

    final TaskIterator iterator = new TaskIterator();
    iterator.append(new ApplyVizPropsTask(gpmlNetwork, networkView));
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
    String a = gpmlContents.toString();
    final TaskIterator iterator = new TaskIterator();
    iterator.append(createReader(id, gpmlContents, network, conversionMethod));
    iterator.append(createViewBuilder(id, network, view));
    if (organism == null)		
    	readOrganismKeywork(gpmlContents);

    iterator.append(new EnsemblIdTask(network, registrar, organism));
    return iterator;
  }


  	//HACK -- read organsim out of the raw xml
  	private void readOrganismKeywork(final Reader gpmlContents) {
	
		try 
		{ 
//			int len = ((StringReader) gpmlContents).read();
			char[] chs = new char[2001];
			gpmlContents.read(chs, 0, 2000);
			gpmlContents.reset();
			String str = new String(chs);
			int idx = str.indexOf("Organism=");
			if (idx > 0)
			{
				int start = idx + "Organism=".length();
				if (str.charAt(start) == '"')
				{
					String tail = str.substring(start + 1);
					int end = tail.indexOf('"');
					organism = tail.substring(0,end);
					System.out.println(organism);
					
				}
			}
		}
		catch (Exception e) {}
		
	}
//-----------------------------------------------------------------------
  class ReaderTask extends AbstractTask {
    volatile Reader gpmlContents = null;
    final CyNetwork network;
    final GpmlConversionMethod conversionMethod;
    final WPManager manager;

    public ReaderTask(
        final WPManager wpMgr,
        final Reader gpmlContents,
        final CyNetwork network,
        final GpmlConversionMethod conversionMethod
        )
    {
        this.manager = wpMgr;
        this.gpmlContents = gpmlContents;
        this.network = network;
        this.conversionMethod = conversionMethod;
    }

    private void addNetworkTableColumns(String ...strings)
    {
		for (String s : strings)
		{
			CyColumn col = network.getDefaultNetworkTable().getColumn(s);
			if (col == null)
				 network.getDefaultNetworkTable().createColumn(s, String.class, true);
		
		}
    	
    }
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Construct network");

		monitor.setStatusMessage("Parsing pathways file");
		final Pathway pathway = new Pathway();
		try {
			gpmlContents.mark(10000000);
			char[] chars = new char[100];
			StringBuilder builder = new StringBuilder();
			int charsRead = -1;
			
			 do{
				 charsRead = gpmlContents.read(chars,0,chars.length);
				    //if we have valid chars, append them to end of string.
				    if(charsRead>0)
				        builder.append(chars,0,charsRead);	 
			 } while (charsRead > 0);
			String raw = builder.toString();
			String pmids = scrapePMIDs(raw);
//			System.out.println(raw);
			gpmlContents.reset();
			pathway.readFromXml(gpmlContents, true);
			PathwayElement info = pathway.getMappInfo();
			organism = info.getOrganism();
			CyRow row = network.getRow(network);
			final String name = info.getMapInfoName() + " - " + organism;
			final String description = info.findComment("WikiPathways-description");
			final String nonConflictingName = netNaming.getSuggestedNetworkTitle(name);
			row.set(CyNetwork.NAME, nonConflictingName);
			addNetworkTableColumns("organism", "description", "title", "pmids");
			CyColumn col = network.getDefaultNetworkTable().getColumn("organism");
//				if (col == null)
//				{
//					 network.getDefaultNetworkTable().createColumn("organism", String.class, true);
//					col =  network.getDefaultNetworkTable().getColumn("organism");
//				}
			row.getAllValues();
			row.set("pmids", pmids);
			row.set("organism", organism);
			row.set("description", description);
			row.set("title", info.getMapInfoName());
			if (network instanceof CySubNetwork) {
				final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
				root.getRow(root).set(CyNetwork.NAME, nonConflictingName);
			}

			List<DelayedVizProp> vizProps = null;
			switch (conversionMethod) {
			case PATHWAY: 	vizProps = (new GpmlToPathway(manager, eventHelper, pathway, network)).convert(); 	break;
			case NETWORK:  (new GpmlToNetwork(eventHelper, pathway, network)).convert();	 				break;
			}

			if (vizProps != null)
				pendingVizProps.put(network, vizProps);

		} catch (Exception e) {
			throw new Exception("Pathway not available -- invalid GPML", e);
		} finally {
 			 manager.turnOnEvents();
		}
	}


	private String scrapePMIDs(String raw) {
		String start = "<bp:ID ";
		String end = "</bp:ID>";
		StringBuilder pmids = new StringBuilder();
		int idx = 0;
		while (true) {
			idx = raw.indexOf(start, idx);
			if (idx < 0) break;
			int idx2 = raw.indexOf(end,  idx + 1);
			String subString = raw.substring(idx, idx2);
			int idx3 = subString.lastIndexOf(">") + 1;
			String id = subString.substring(idx3);
			if (!id.startsWith("PW"))
				pmids.append(id).append(", ");
			idx++; 
		}
		
		String output = pmids.toString();
		output = output.substring(0, output.length()-2);
		return output;
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
  class ApplyVizPropsTask extends AbstractTask {
    final CyNetwork network;
    final CyNetworkView view;

    public ApplyVizPropsTask(final CyNetwork network, final CyNetworkView view) {
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

