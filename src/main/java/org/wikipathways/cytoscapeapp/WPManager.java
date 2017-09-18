package org.wikipathways.cytoscapeapp;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;

public class WPManager {

	final CyServiceRegistrar registrar;
	final Annots annots;
	final CyNetworkViewManager viewMgr;
	
	public WPManager(CyServiceRegistrar reg, Annots annotations)
	{
		registrar = reg;
		viewMgr = registrar.getService(CyNetworkViewManager.class);
		annots = annotations;
	}
	public CyNetworkViewManager getNetworkViewMgr() {	return viewMgr;	}
	public CyServiceRegistrar getRegistrar() 		{	return registrar;	}
	public BendFactory getBendFactory() 			{	return registrar.getService(BendFactory.class);	}
	public Annots getAnnots() 						{	return annots;	}
	public HandleFactory getHandleFactory() 		{ 	return registrar.getService(HandleFactory.class);  }
	public CyEventHelper getEventHelper() 			{ 	return registrar.getService(CyEventHelper.class);  }

	
	//-----------------------------------------------------
	private Object networkView;
	private CyTable cyNodeTbl;
	private CyTable cyEdgeTbl;

	public void turnOnEvents() {
		System.out.println("turnOnEvents");
		CyEventHelper eventHelper = getEventHelper();
		eventHelper.unsilenceEventSource(cyNodeTbl);
		eventHelper.unsilenceEventSource(cyEdgeTbl);
	}
	public void turnOffEvents() {
		System.out.println("turnOffEvents");
		CyEventHelper eventHelper = getEventHelper();
		eventHelper.silenceEventSource(cyNodeTbl);
		eventHelper.silenceEventSource(cyEdgeTbl);		
	}
	public void setUpTableRefs(CyNetwork cyNet) {
		if (networkView == null)
		{
			networkView = getNetworkViewMgr().getNetworkViews(cyNet).iterator().next();
			cyNodeTbl = cyNet.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
			cyEdgeTbl = cyNet.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
		}
		
	}
}