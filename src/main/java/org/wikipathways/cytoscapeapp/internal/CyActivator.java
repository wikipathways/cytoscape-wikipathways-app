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

import javax.swing.ImageIcon;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPClientFactory;
import org.wikipathways.cytoscapeapp.impl.WPClientRESTFactoryImpl;
import org.wikipathways.cytoscapeapp.impl.search.WPNetworkSearchTaskFactory;
import org.wikipathways.cytoscapeapp.internal.cmd.GpmlImportCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.cmd.WPImportCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSpeciesCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.WPCyGUIClient;
import org.wikipathways.cytoscapeapp.internal.io.GpmlCyReaderTaskFactory;
import org.wikipathways.cytoscapeapp.internal.io.GpmlReaderFactoryImpl;
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
    final StreamUtil streamUtil = getService(context,StreamUtil.class);
    final TaskManager taskMgr = getService(context, DialogTaskManager.class);
    final OpenBrowser openBrowser = getService(context, OpenBrowser.class);
    final CyApplicationConfiguration appConf = getService(context, CyApplicationConfiguration.class);
    final CyServiceRegistrar registrar = getService(context, CyServiceRegistrar.class);
//    

    final WPClientFactory clientFactory = new WPClientRESTFactoryImpl(appConf);
    registerService(context, clientFactory, WPClientFactory.class);

    final WPClient client = clientFactory.create();

    final GpmlReaderFactory gpmlReaderFactory = new GpmlReaderFactoryImpl(registrar);
    registerService(context, gpmlReaderFactory, GpmlReaderFactory.class);

    final GpmlCyReaderTaskFactory gpmlCyReaderTaskFactory = new GpmlCyReaderTaskFactory( gpmlReaderFactory, streamUtil);
    registerService(context, gpmlCyReaderTaskFactory, InputStreamTaskFactory.class);

    final WPCyGUIClient guiClient = new WPCyGUIClient( taskMgr, client, openBrowser, gpmlReaderFactory);
    registerAllServices(context, guiClient);

    final ToggleShapesTaskFactory toggleShapesTF = new ToggleShapesTaskFactory();
    registerService(context, toggleShapesTF, NetworkViewTaskFactory.class, ezProps(
      ServiceProperties.TITLE, "Toggle Pathway Shapes",
      ServiceProperties.PREFERRED_MENU, "View" ));

    final OpenLinkedPathwayAsNewTaskFactory openLinkedPathwayAsNewTF = new OpenLinkedPathwayAsNewTaskFactory(client, gpmlReaderFactory);
    registerService(context, openLinkedPathwayAsNewTF, NodeViewTaskFactory.class, ezProps(
      ServiceProperties.TITLE, "Open Linked Pathway",
      ServiceProperties.PREFERRED_MENU, "Apps.WikiPathways",
      ServiceProperties.IN_MENU_BAR, "false"  ));

    ImageIcon icon = null;
 	try
 	{
 		  icon = new ImageIcon(getClass().getClassLoader().getResource("logo_150.png"));
 	}
 	catch (NullPointerException e)				// icon with that name not found
 	{
	}
 	registerAllServices(context, new WPNetworkSearchTaskFactory(registrar, client, icon));		//		support NetworkSearchBar

// These are additional sample classes that you could mock up as other services
 	
//	registerAllServices(context, new CustomOptionsTaskFactory());
//	registerAllServices(context, new CustomQueryTaskFactory(registrar));
//	registerAllServices(context, new TunableOptionsTaskFactory(1));
   
    reg(context,  new GpmlImportCmdTaskFactory(gpmlReaderFactory, GpmlConversionMethod.PATHWAY),"import-as-pathway", "gpml");
    reg(context,  new GpmlImportCmdTaskFactory(gpmlReaderFactory, GpmlConversionMethod.NETWORK),"import-as-network", "gpml");
    reg(context,  new WPSpeciesCmdTaskFactory(client), "get-species", "wikipathways");
    reg(context,  new WPImportCmdTaskFactory(client, gpmlReaderFactory, GpmlConversionMethod.PATHWAY),"import-as-pathway", "wikipathways");
    reg(context,  new WPImportCmdTaskFactory(client, gpmlReaderFactory, GpmlConversionMethod.NETWORK),"import-as-network", "wikipathways");
 }
//-----------------------------------------------------

	private void reg(BundleContext context, Object service, String cmd, String namespace)
    {
        registerService(context, service,
        	 TaskFactory.class, ezProps( ServiceProperties.COMMAND,cmd,  ServiceProperties.COMMAND_NAMESPACE, namespace ));
    }

	private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
       props.put(vals[i], vals[i + 1]);
    return props;
  }
}
