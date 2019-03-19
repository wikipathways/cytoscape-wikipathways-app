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
package org.wikipathways.cytoscapeapp;

import java.util.Properties;

import javax.swing.ImageIcon;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.pathvisio.core.view.MIMShapes;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactoryImpl;
import org.wikipathways.cytoscapeapp.impl.WPClientRESTFactoryImpl;
import org.wikipathways.cytoscapeapp.impl.search.WPNetworkSearchTaskFactory;
import org.wikipathways.cytoscapeapp.internal.cmd.FileImportCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.cmd.GpmlImportCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.cmd.WPImportCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.GUI;
import org.wikipathways.cytoscapeapp.internal.io.GpmlFileReaderTaskFactory;
/**
 * 
 * @author martinakutmon
 * @author adamtreister
 * gets the required services provided by Cytoscape
 * initializes the GpmlReaderFactory, WPCyGUIClient and Network Search Bar
 *
 */
public class CyActivator extends AbstractCyActivator {
	@Override
	public void start(BundleContext context) throws Exception {
	final CyServiceRegistrar registrar = getService(context, CyServiceRegistrar.class);
    // --- get the GpmlReaderFactory and the GpmlCyReaderTaskFactory to manage imports
    final GpmlReaderFactory gpmlReaderFactory = new GpmlReaderFactoryImpl(registrar);
    registerService(context, gpmlReaderFactory, GpmlReaderFactory.class);
    final StreamUtil streamUtil = getService(context,StreamUtil.class);
    final GpmlFileReaderTaskFactory gpmlCyReaderTaskFactory = new GpmlFileReaderTaskFactory( gpmlReaderFactory, streamUtil);
    registerService(context, gpmlCyReaderTaskFactory, InputStreamTaskFactory.class);

    // --- analogous GpmlWriterFactory and the GpmlCyWriterTaskFactory to manage exports go here  
    // TODO
     
	MIMShapes.registerShapes();

	// ---- get all the services necessary to build the GUI and then build and register it
	final CyApplicationConfiguration appConf = getService(context, CyApplicationConfiguration.class);
	final WPClientFactory clientFactory = new WPClientRESTFactoryImpl(appConf, gpmlReaderFactory.getWPManager());
	registerService(context, clientFactory, WPClientFactory.class);
	final TaskManager<?, ?> taskMgr = getService(context, SynchronousTaskManager.class);
	final WPClient client = clientFactory.create();
	gpmlReaderFactory.setClient(client);
	final OpenBrowser openBrowser = getService(context, OpenBrowser.class);
	final GUI guiClient = new GUI(taskMgr, client, openBrowser, gpmlReaderFactory);
	registerAllServices(context, guiClient);

		// ---- create and register a bunch of CommandTaskFactories
//	reg(context,  new WPSpeciesCmdTaskFactory(client), "get-species", "wikipathways");
//  reg(context,  new WPSearchCmdTaskFactory(client, registrar, guiClient), "search", "wikipathways");
	
	final  String longDesc1 = "Import a GPML object from WikiPathways and translate it into a pathway diagram";
	final  String longDesc2 = "Import a GPML object from WikiPathways and translate it into a network";
	final  String longDesc3 = "Import a GPML object from WikiPathways and translate it into a pathway diagram";
	final  String longDesc4 = "Import a GPML object from WikiPathways and translate it into a network";
	final  String longDesc5 = "Import a GPML file from a file path and translate it into a pathway diagram";
	final  String longDesc6 = "Import a GPML file from a file path translate it into a network";
    reg(context,  new GpmlImportCmdTaskFactory(gpmlReaderFactory, GpmlConversionMethod.PATHWAY),"import-as-pathway", "gpml", longDesc1);
    reg(context,  new GpmlImportCmdTaskFactory(gpmlReaderFactory, GpmlConversionMethod.NETWORK),"import-as-network", "gpml", longDesc2);
    reg(context,  new WPImportCmdTaskFactory(client, gpmlReaderFactory, GpmlConversionMethod.PATHWAY, taskMgr),"import-as-pathway", "wikipathways", longDesc3);
    reg(context,  new WPImportCmdTaskFactory(client, gpmlReaderFactory, GpmlConversionMethod.NETWORK, taskMgr),"import-as-network", "wikipathways", longDesc4);
    reg(context,  new FileImportCmdTaskFactory(client, gpmlReaderFactory, GpmlConversionMethod.PATHWAY, taskMgr),"import-file-as-pathway", "wikipathways", longDesc5);
    reg(context,  new FileImportCmdTaskFactory(client, gpmlReaderFactory, GpmlConversionMethod.NETWORK, taskMgr),"import-file-as-network", "wikipathways", longDesc6);

    // --- analogous export commands go here   TODO
   
    //  ----- support NetworkSearchBar
    ImageIcon icon = null;
 	try
 	{
 		icon = new ImageIcon(getClass().getClassLoader().getResource("logo_150.png"));
 	}
 	catch (NullPointerException e)			// report icon with that name not found, but null is okay
 	{
 		System.err.println("Icon not found"); 
 	}
 	 
 	registerAllServices(context, new WPNetworkSearchTaskFactory(registrar, client, icon, guiClient));		
 
 }
//-----------------------------------------------------
	String JSON_EXAMPLE = "{\"SUID\":1234}";

//	private void reg(BundleContext context, Object service, String cmd, String namespace)
//    {
//        registerService(context, service, TaskFactory.class, ezProps( 
//			 ServiceProperties.COMMAND,cmd,  ServiceProperties.COMMAND_NAMESPACE, namespace, 
//			 ServiceProperties.COMMAND_SUPPORTS_JSON, "true",  ServiceProperties.COMMAND_EXAMPLE_JSON, JSON_EXAMPLE 
//		 ));
// }
	private void reg(BundleContext context, Object service, String cmd, String namespace, String desc)
    {
        registerService(context, service, TaskFactory.class, ezProps( 
			 ServiceProperties.COMMAND,cmd,  ServiceProperties.COMMAND_NAMESPACE, namespace, 
			 ServiceProperties.COMMAND_DESCRIPTION, desc, 
			 ServiceProperties.COMMAND_SUPPORTS_JSON, "true",  ServiceProperties.COMMAND_EXAMPLE_JSON, JSON_EXAMPLE 
		 ));
 }

	private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
       props.put(vals[i], vals[i + 1]);
    return props;
  }
}
