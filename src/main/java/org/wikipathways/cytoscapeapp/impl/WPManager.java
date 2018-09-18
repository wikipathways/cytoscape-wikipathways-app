package org.wikipathways.cytoscapeapp.impl;

import java.io.Reader;
import java.util.Map;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.wikipathways.cytoscapeapp.Annots;
import org.wikipathways.cytoscapeapp.WPClient;

public class WPManager {

	final CyServiceRegistrar registrar;
	final Annots annots;
	final CyNetworkViewManager viewMgr;
	final GpmlReaderFactory gpmlReader;
	private	WPClient client;			// this is the http client, to be filled in after constructor
	public void setClient(WPClient c) { client = c;	}
	
	public WPManager(CyServiceRegistrar reg, Annots annotations, GpmlReaderFactory gpml)
	{
		registrar = reg;
		viewMgr = registrar.getService(CyNetworkViewManager.class);
		annots = annotations;
		gpmlReader = gpml;
	}
	public CyNetworkViewManager getNetworkViewMgr() 	{	return viewMgr;	}
	public CyServiceRegistrar getRegistrar() 			{	return registrar;	}
	public BendFactory getBendFactory() 				{	return registrar.getService(BendFactory.class);	}
	public Annots getAnnots() 							{	return annots;	}
	public HandleFactory getHandleFactory() 			{ 	return registrar.getService(HandleFactory.class);  }
	public CyEventHelper getEventHelper() 				{ 	return registrar.getService(CyEventHelper.class);  }


	//-----------------------------------------------------
	private Object networkView;
	private Map<String,CyTable> tables;
	
	public void setUpTableRefs(CyNetwork cyNet) {
		if (networkView == null)
		{
			CyNetworkTableManager netTablMgr = registrar.getService(CyNetworkTableManager.class);
			networkView = getNetworkViewMgr().getNetworkViews(cyNet).iterator().next();
			tables = netTablMgr.getTables(cyNet, CyNode.class);		
		}
	}
	//-----------------------------------------------------
	// this code SHOULD disable event processing during a lengthy import.  Not sure it works!
	boolean bypass = false;

	public void turnOnEvents() {
		if (bypass) return;
		if (tables == null || tables.isEmpty()) return;
		System.out.println("turnOnEvents");
		CyEventHelper eventHelper = getEventHelper();
		for (CyTable table : tables.values())
			eventHelper.unsilenceEventSource(table);
		eventHelper.flushPayloadEvents();
	}
	
	public void turnOffEvents() {
	if (bypass) return;
	if (tables == null || tables.isEmpty()) return;
	System.out.println("turnOffEvents");
		CyEventHelper eventHelper = getEventHelper();
		for (CyTable table : tables.values())
			eventHelper.silenceEventSource(table);
	}
	
	//-----------------------------------------------------
	// Do the work:  fetch , read and display GPML
	public void loadPathway(GpmlConversionMethod method, WPPathway pathway, TaskManager<?, ?> taskMgr) {
		  final ResultTask<Reader> loadPathwayTask = client.gpmlContentsTask(pathway);
	      final TaskIterator taskIterator = new TaskIterator(loadPathwayTask);
	      taskIterator.append(new AbstractTask() {
	        public void run(TaskMonitor monitor) {
	          super.insertTasksAfterCurrentTask(gpmlReader.createReaderAndViewBuilder(pathway.getId(), loadPathwayTask.get(), method));
	        }
	      });
	     taskMgr.execute(taskIterator, new TaskObserver() {
	        public void taskFinished(ObservableTask t) {}
	        public void allFinished(FinishStatus status) { }
	 });

//    		}   		
	}
}