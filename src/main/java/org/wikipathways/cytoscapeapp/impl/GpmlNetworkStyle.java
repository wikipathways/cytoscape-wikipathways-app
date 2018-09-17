
package org.wikipathways.cytoscapeapp.impl;

import java.awt.Color;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;


public class GpmlNetworkStyle {
  static final String VIZ_STYLE_NAME = "WikiPathways-As-Network";

  final VisualStyleFactory vizStyleFactory;
  final VisualMappingManager vizMapMgr;
  final VisualMappingFunctionFactory contFnFactory;
  final VisualMappingFunctionFactory discFnFactory;
  final VisualMappingFunctionFactory passFnFactory;
  VisualStyle vizStyle = null;

  public GpmlNetworkStyle(CyServiceRegistrar registrar)
  {
    vizStyleFactory = registrar.getService( VisualStyleFactory.class);
    vizMapMgr = registrar.getService( VisualMappingManager.class);
    contFnFactory = registrar.getService( VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
    discFnFactory = registrar.getService( VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
    passFnFactory = registrar.getService( VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
   
  }

  private void removeOldVizStyle() {
    final Set<VisualStyle> styles = new HashSet<VisualStyle>(vizMapMgr.getAllVisualStyles()); // prevent concurrent modification exception
    for (final VisualStyle style : styles) {
      if (VIZ_STYLE_NAME.equals(style.getTitle())) {
        vizMapMgr.removeVisualStyle(style);
      }
    }
  }
 

  private VisualStyle create() {
    removeOldVizStyle();

    final VisualStyle vizStyle = vizStyleFactory.createVisualStyle(vizMapMgr.getDefaultVisualStyle());   //vizMapMgr.getDefaultVisualStyle()

    // set up viz style dependencies
    for (final VisualPropertyDependency<?> dep : vizStyle.getAllVisualPropertyDependencies()) {
      final String id = dep.getIdString();
      if ("nodeSizeLocked".equals(id)) {
        dep.setDependency(true);
      } else if ("arrowColorMatchesEdge".equals(id)) {
        dep.setDependency(true);
      }
    }

    vizStyle.setTitle(VIZ_STYLE_NAME);
    
    // set default visual properties
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_TITLE, "THIS IS THE TITLE");
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_HEIGHT, 400.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_SIZE, 550.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_SCALE_FACTOR, 1.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_WIDTH, 550.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, 0.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION, 0.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, 0.0);

    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_VISIBLE, true);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_SELECTED, true);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_DEPTH, 0.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL, "");
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, new Color(153,153,153));
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 12);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 2.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 50.0);

    //?? map node size to WP.Type:  Group = 10, Anchor = 5, Metabolite = 25, Pathway = 25 
    
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.LIGHT_GRAY);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_SELECTED_PAINT, Color.YELLOW);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);

   
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_SELECTED, false);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_TOOLTIP, "");
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, Color.RED);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SIZE, 6.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, Color.GRAY);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_LABEL, "");
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_VISIBLE, true);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 2.0);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, Color.GRAY);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_LABEL_COLOR, Color.BLACK);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, new Color(204,204,204));
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_PAINT, Color.BLACK);
//    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 255);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_BEND, null);
   
    
    // create viz mappings
    for (final GpmlToNetwork.VizTableStore vizTableStore : GpmlToNetwork.getAllVizTableStores()) 
    {
      final Map<?,?> mapping = vizTableStore.getMapping();
      String colName =  vizTableStore.getCyColumnName();
      Class<?> colType = vizTableStore.getCyColumnType();
      final VisualMappingFunctionFactory fnFactory = (mapping == null) ? passFnFactory : discFnFactory;
      for (final VisualProperty<?> vizProp : vizTableStore.getCyVizProps()) 
      {
        final VisualMappingFunction<?,?> fn = fnFactory.createVisualMappingFunction( colName, colType, vizProp);
        if (mapping != null && fn instanceof DiscreteMapping  ) 
          ((DiscreteMapping) fn).putAll(mapping);
        vizStyle.addVisualMappingFunction(fn);
      }
    }
    vizMapMgr.addVisualStyle(vizStyle);
    return vizStyle;
  }

  public void apply(final CyNetworkView view) {
    if (vizStyle == null || !vizMapMgr.getAllVisualStyles().contains(vizStyle)) 
      vizStyle = create();
    
    vizMapMgr.setVisualStyle(vizStyle, view);
    vizStyle.apply(view);
  }
}
