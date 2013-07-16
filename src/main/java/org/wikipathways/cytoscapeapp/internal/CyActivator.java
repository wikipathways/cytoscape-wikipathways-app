// WikiPathways App for Cytoscape
// opens pathways from WikiPathways as networks in Cytoscape
//
// Copyright 2013 WikiPathways.org
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.wikipathways.cytoscapeapp.internal;

import java.util.Properties;

import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.wikipathways.cytoscapeapp.internal.model.GPMLNetworkManager;
import org.wikipathways.cytoscapeapp.internal.model.GPMLNetworkManagerImpl;
import org.wikipathways.cytoscapeapp.internal.io.GpmlReaderTaskFactory;
import org.osgi.framework.BundleContext;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;

/**
 * 
 * @author martinakutmon
 * gets all required services provided by Cytoscape
 * initializes the GPMLNetworkManager
 *
 */
public class CyActivator extends AbstractCyActivator {

    public static CyNetworkManager netMgr = null;
    public static CyNetworkViewManager netViewMgr = null;
    public static CyNetworkViewFactory netViewFactory = null;
    public static CyNetworkFactory netFactory = null;

	@Override
	public void start(BundleContext context) throws Exception {
		
		// get Cytoscape services from OSGi context
		CySwingApplication cySwingApp = getService(context,CySwingApplication.class);

        netMgr = getService(context,CyNetworkManager.class);
        netViewMgr = getService(context,CyNetworkViewManager.class);
        netViewFactory = getService(context,CyNetworkViewFactory.class);
        netFactory = getService(context,CyNetworkFactory.class);
        FileUtil fileUtil = getService(context,FileUtil.class);
        StreamUtil streamUtil = getService(context,StreamUtil.class);
        
        // currently not used - will probably be needed in the future
//      VisualMappingManager visMappingMgr = getService(context,VisualMappingManager.class);
//      CyApplicationManager cyAppMgr = getService(context,CyApplicationManager.class);
//      StreamUtil streamUtil = getService(context,StreamUtil.class);
//      CyEventHelper cyEventHelper = getService(context,CyEventHelper.class);

        // initialize GPML network manager
        GPMLNetworkManager gpmlNetMgr = new GPMLNetworkManagerImpl(netMgr, netFactory, netViewFactory, netViewMgr);

        registerService(context, new GpmlReaderTaskFactory(streamUtil), InputStreamTaskFactory.class, new Properties());

	}

}
