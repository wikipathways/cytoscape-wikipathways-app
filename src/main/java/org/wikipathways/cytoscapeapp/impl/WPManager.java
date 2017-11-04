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
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.wikipathways.cytoscapeapp.Annots;

public class WPManager {

	final CyServiceRegistrar registrar;
	final Annots annots;
	final CyNetworkViewManager viewMgr;
	final GpmlReaderFactory gpmlReader;
	
	public WPManager(CyServiceRegistrar reg, Annots annotations, GpmlReaderFactory gpml)
	{
		registrar = reg;
		viewMgr = registrar.getService(CyNetworkViewManager.class);
		annots = annotations;
		gpmlReader = gpml;
	}
	public CyNetworkViewManager getNetworkViewMgr() 	{	return viewMgr;	}
	public CyServiceRegistrar getRegistrar() 		{	return registrar;	}
	public BendFactory getBendFactory() 				{	return registrar.getService(BendFactory.class);	}
	public Annots getAnnots() 						{	return annots;	}
	public HandleFactory getHandleFactory() 			{ 	return registrar.getService(HandleFactory.class);  }
	public CyEventHelper getEventHelper() 			{ 	return registrar.getService(CyEventHelper.class);  }

	

	//-----------------------------------------------------
	private Object networkView;
	Map<String,CyTable> tables;
boolean bypass = true;

	public void turnOnEvents() {
		if (bypass) return;
		System.out.println("turnOnEvents");
		CyEventHelper eventHelper = getEventHelper();
		for (CyTable table : tables.values())
			eventHelper.unsilenceEventSource(table);
		eventHelper.flushPayloadEvents();
	}
	public void turnOffEvents() {
	if (bypass) return;
	System.out.println("turnOffEvents");
		CyEventHelper eventHelper = getEventHelper();
		for (CyTable table : tables.values())
			eventHelper.silenceEventSource(table);
	}
	public void setUpTableRefs(CyNetwork cyNet) {
		if (networkView == null)
		{
			CyNetworkTableManager netTablMgr = registrar.getService(CyNetworkTableManager.class);
			networkView = getNetworkViewMgr().getNetworkViews(cyNet).iterator().next();
			tables = netTablMgr.getTables(cyNet, CyNode.class);		
		}
	}
}