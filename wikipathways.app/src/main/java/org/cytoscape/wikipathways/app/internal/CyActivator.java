package org.cytoscape.wikipathways.app.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		
		CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
		CyNetworkViewFactory networkViewFactory = getService(context, CyNetworkViewFactory.class);
		StreamUtil streamUtil = getService(context, StreamUtil.class);
		CyNetworkFactory networkFactory = getService(context, CyNetworkFactory.class);
		CyServiceRegistrar serviceRegistrar = getService(context, CyServiceRegistrar.class);
		
		WikiPathwaysAppHandler handler = new WikiPathwaysAppHandler();
		handler.registerImporter(streamUtil, networkViewFactory, networkFactory, serviceRegistrar);
		
		MenuAction action = new MenuAction(cyApplicationManager, "WikiPathways App");
		
		Properties properties = new Properties();
		
		registerAllServices(context, action, properties);
	}

}
