package org.wikipathways.cytoscapeapp;

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
}