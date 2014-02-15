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
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPClientFactory;
import org.wikipathways.cytoscapeapp.impl.WPClientRESTFactoryImpl;

import org.wikipathways.cytoscapeapp.internal.io.GpmlVizStyle;
import org.wikipathways.cytoscapeapp.internal.io.GpmlReaderTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.WPCyGUIClient;
/**
 * 
 * @author martinakutmon
 * gets all required services provided by Cytoscape
 * initializes the GPMLNetworkManager
 *
 */
public class CyActivator extends AbstractCyActivator {
	@Override
	public void start(BundleContext context) throws Exception {
		final CySwingApplication cySwingApp = getService(context,CySwingApplication.class);
    final CyNetworkManager netMgr = getService(context,CyNetworkManager.class);
    final CyNetworkViewManager netViewMgr = getService(context,CyNetworkViewManager.class);
    final CyNetworkViewFactory netViewFactory = getService(context,CyNetworkViewFactory.class);
    final CyNetworkFactory netFactory = getService(context,CyNetworkFactory.class);
    final FileUtil fileUtil = getService(context,FileUtil.class);
    final StreamUtil streamUtil = getService(context,StreamUtil.class);
    final CyTableManager tableMgr = getService(context,CyTableManager.class);
    final CyTableFactory tableFactory = getService(context,CyTableFactory.class);
    final CyEventHelper eventHelper = getService(context,CyEventHelper.class);
    final VisualMappingManager vizMapMgr = getService(context,VisualMappingManager.class);
    final VisualStyleFactory vizStyleFactory = getService(context,VisualStyleFactory.class);
    final AnnotationManager annotationMgr = getService(context, AnnotationManager.class);
    final AnnotationFactory annotationFactory = getService(context, AnnotationFactory.class);
    final CyNetworkReaderManager netReaderMgr = getService(context, CyNetworkReaderManager.class);
    final TaskManager taskMgr = getService(context, DialogTaskManager.class);
    final CyLayoutAlgorithmManager layoutMgr = getService(context, CyLayoutAlgorithmManager.class);

    final WPClientFactory clientFactory = new WPClientRESTFactoryImpl();
    registerService(context, clientFactory, WPClientFactory.class, new Properties());

    final WPClient client = clientFactory.create();
    final GpmlVizStyle gpmlStyle = new GpmlVizStyle(vizStyleFactory, vizMapMgr);

    final GpmlReaderTaskFactory gpmlReaderTaskFactory = new GpmlReaderTaskFactory(eventHelper, netFactory, netMgr, netViewFactory, netViewMgr, layoutMgr, streamUtil, gpmlStyle);
    registerService(context, gpmlReaderTaskFactory, InputStreamTaskFactory.class, new Properties());

    final WPCyGUIClient guiClient = new WPCyGUIClient(eventHelper, taskMgr, netFactory, netMgr, netViewFactory, netViewMgr, layoutMgr, gpmlStyle, client);
    registerAllServices(context, guiClient, new Properties());
  }
}
