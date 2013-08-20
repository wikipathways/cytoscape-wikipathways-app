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
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.WebServiceClient;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
//import org.cytoscape.view.presentation.annotations.AnnotationFactory;
//import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;

import org.wikipathways.cytoscapeapp.internal.model.GPMLNetworkManager;
import org.wikipathways.cytoscapeapp.internal.model.GPMLNetworkManagerImpl;
import org.wikipathways.cytoscapeapp.internal.io.GpmlReaderTaskFactory;
import org.wikipathways.cytoscapeapp.internal.webclient.CyWPClient;
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
    public static CyTableFactory tableFactory = null;
    public static CyTableManager tableMgr = null;
    public static CyEventHelper eventHelper = null;
    public static VisualMappingManager vizMapMgr = null;
    public static VisualStyleFactory vizStyleFactory = null;
    //public static AnnotationManager annotationMgr = null;
    //public static AnnotationFactory annotationFactory = null;
    public static CyNetworkReaderManager netReaderMgr = null;
    public static TaskManager taskMgr = null;
    public static CyLayoutAlgorithmManager layoutMgr = null;

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
        tableMgr = getService(context,CyTableManager.class);
        tableFactory = getService(context,CyTableFactory.class);
        eventHelper = getService(context,CyEventHelper.class);
        vizMapMgr = getService(context,VisualMappingManager.class);
        vizStyleFactory = getService(context,VisualStyleFactory.class);
        //annotationMgr = getService(context, AnnotationManager.class);
        //annotationFactory = getService(context, AnnotationFactory.class);
        netReaderMgr = getService(context, CyNetworkReaderManager.class);
        taskMgr = getService(context, DialogTaskManager.class);
        layoutMgr = getService(context, CyLayoutAlgorithmManager.class);
        
        // currently not used - will probably be needed in the future
//      CyApplicationManager cyAppMgr = getService(context,CyApplicationManager.class);
//      StreamUtil streamUtil = getService(context,StreamUtil.class);

        // initialize GPML network manager
        GPMLNetworkManager gpmlNetMgr = new GPMLNetworkManagerImpl(netMgr, netFactory, netViewFactory, netViewMgr);
        final GpmlReaderTaskFactory gpmlReaderTaskFactory = new GpmlReaderTaskFactory(streamUtil);
        registerService(context, gpmlReaderTaskFactory, InputStreamTaskFactory.class, new Properties());

        // initialize web service client
        final CyWPClient wpClient = new CyWPClient(gpmlReaderTaskFactory);
        registerAllServices(context, wpClient, new Properties());

	}

}
