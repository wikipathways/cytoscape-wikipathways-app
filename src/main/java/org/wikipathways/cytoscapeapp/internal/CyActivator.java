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

import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.osgi.framework.BundleContext;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPClientFactory;
import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.impl.WPClientRESTFactoryImpl;
import org.wikipathways.cytoscapeapp.internal.io.Annots;
import org.wikipathways.cytoscapeapp.internal.io.GpmlVizStyle;
import org.wikipathways.cytoscapeapp.internal.io.GpmlCyReaderTaskFactory;
import org.wikipathways.cytoscapeapp.internal.io.GpmlReaderFactoryImpl;
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
    final CyNetworkManager netMgr = getService(context,CyNetworkManager.class);
    final CyNetworkViewManager netViewMgr = getService(context,CyNetworkViewManager.class);
    final CyNetworkViewFactory netViewFactory = getService(context,CyNetworkViewFactory.class);
    final CyNetworkFactory netFactory = getService(context,CyNetworkFactory.class);
    final StreamUtil streamUtil = getService(context,StreamUtil.class);
    final CyEventHelper eventHelper = getService(context,CyEventHelper.class);
    @SuppressWarnings("rawtypes")
    final TaskManager taskMgr = getService(context, DialogTaskManager.class);
    final CyLayoutAlgorithmManager layoutMgr = getService(context, CyLayoutAlgorithmManager.class);
    final NetworkTaskFactory showLODTF = getService(context, NetworkTaskFactory.class, String.format("(%s=Show\\/Hide Graphics Details)", ServiceProperties.TITLE));
    final OpenBrowser openBrowser = getService(context, OpenBrowser.class);
    final CyNetworkNaming netNaming = getService(context, CyNetworkNaming.class);

    final GpmlVizStyle gpmlStyle = new GpmlVizStyle(
              getService(context, VisualStyleFactory.class),
              getService(context, VisualMappingManager.class),
              getService(context, VisualMappingFunctionFactory.class, "(mapping.type=continuous)"),
              getService(context, VisualMappingFunctionFactory.class, "(mapping.type=discrete)"),
              getService(context, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)"));
    @SuppressWarnings("unchecked")
    final Annots annots = new Annots(
              getService(context, AnnotationManager.class),
              (AnnotationFactory<ArrowAnnotation>) getService(context, AnnotationFactory.class,"(type=ArrowAnnotation.class)"),
              (AnnotationFactory<ShapeAnnotation>) getService(context, AnnotationFactory.class,"(type=ShapeAnnotation.class)"),
              (AnnotationFactory<TextAnnotation>) getService(context, AnnotationFactory.class,"(type=TextAnnotation.class)"));

    final WPClientFactory clientFactory = new WPClientRESTFactoryImpl();
    registerService(context, clientFactory, WPClientFactory.class, new Properties());

    final WPClient client = clientFactory.create();

    final GpmlReaderFactory gpmlReaderFactory = new GpmlReaderFactoryImpl(
      eventHelper,
      netFactory,
      netMgr,
      netNaming,
      netViewFactory,
      netViewMgr,
      layoutMgr,
      showLODTF,
      annots,
      gpmlStyle
      );
    registerService(context, gpmlReaderFactory, GpmlReaderFactory.class, new Properties());

    final GpmlCyReaderTaskFactory gpmlCyReaderTaskFactory = new GpmlCyReaderTaskFactory(
      eventHelper,
      netFactory,
      netMgr,
      netViewFactory,
      netViewMgr,
      layoutMgr,
      streamUtil,
      annots,
      gpmlStyle,
      showLODTF,
      netNaming);
    registerService(context, gpmlCyReaderTaskFactory, InputStreamTaskFactory.class, new Properties());

    final WPCyGUIClient guiClient = new WPCyGUIClient(
      taskMgr,
      client,
      openBrowser,
      gpmlReaderFactory);
    registerAllServices(context, guiClient, new Properties());

    final ToggleShapesTaskFactory toggleShapesTF = new ToggleShapesTaskFactory();
    registerService(context, toggleShapesTF, NetworkViewTaskFactory.class, ezProps(
      ServiceProperties.TITLE, "Toggle Pathway Shapes",
      ServiceProperties.PREFERRED_MENU, "View"
      ));
  }

  private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
       props.put(vals[i], vals[i + 1]);
    return props;
  }
}
