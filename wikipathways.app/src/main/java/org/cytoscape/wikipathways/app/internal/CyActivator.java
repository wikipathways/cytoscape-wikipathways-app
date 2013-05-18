package org.cytoscape.wikipathways.app.internal;

import java.util.Properties;

import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.wikipathways.app.internal.io.MenuActionLoadGPML;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManager;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManagerImpl;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;

public class CyActivator<T,C> extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		
		// get Cytoscape services from OSGi context
		CySwingApplication cySwingApp = getService(context,CySwingApplication.class);

        CyNetworkManager cyNetMgr = getService(context,CyNetworkManager.class);
        CyNetworkViewManager cyNetViewMgr = getService(context,CyNetworkViewManager.class);
        CyNetworkViewFactory cyNetViewFactory = getService(context,CyNetworkViewFactory.class);
        CyNetworkFactory cyNetFactory = getService(context,CyNetworkFactory.class);
        FileUtil fileUtil = getService(context,FileUtil.class);
		TaskManager<T,C> taskMgr = getService(context,TaskManager.class);
        
        // currently not used - will probably be needed in the future
//      VisualMappingManager visMappingMgr = getService(context,VisualMappingManager.class);
//      CyApplicationManager cyAppMgr = getService(context,CyApplicationManager.class);
//      StreamUtil streamUtil = getService(context,StreamUtil.class);
//      CyEventHelper cyEventHelper = getService(context,CyEventHelper.class);

        // initialize GPML network manager
        GPMLNetworkManager gpmlNetMgr = new GPMLNetworkManagerImpl(cyNetMgr, cyNetFactory, cyNetViewFactory, cyNetViewMgr);
        MenuActionLoadGPML<T, C> loadAction = new MenuActionLoadGPML<T, C>(cySwingApp,taskMgr,gpmlNetMgr,fileUtil);
        registerService(context,loadAction,CyAction.class, new Properties());  
	}

}
