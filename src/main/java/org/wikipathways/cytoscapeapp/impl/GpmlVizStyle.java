
package org.wikipathways.cytoscapeapp.impl;

import java.awt.Color;
import java.awt.Shape;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.EdgeBendVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

public class GpmlVizStyle {
  static final String VIZ_STYLE_NAME = "WikiPathways";

  final VisualStyleFactory vizStyleFactory;
  final VisualMappingManager vizMapMgr;
  final VisualMappingFunctionFactory contFnFactory;
  final VisualMappingFunctionFactory discFnFactory;
  final VisualMappingFunctionFactory passFnFactory;
  VisualStyle vizStyle = null;

  public GpmlVizStyle(CyServiceRegistrar registrar) {
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

    final VisualStyle vizStyle = vizStyleFactory.createVisualStyle(vizMapMgr.getDefaultVisualStyle());			//vizMapMgr.getDefaultVisualStyle()

    // set up viz style dependencies
    for (final VisualPropertyDependency<?> dep : vizStyle.getAllVisualPropertyDependencies()) {
      final String id = dep.getIdString();
      if ("nodeSizeLocked".equals(id)) {
        dep.setDependency(false);
      } else if ("arrowColorMatchesEdge".equals(id)) {
        dep.setDependency(true);
      }
    }

    vizStyle.setTitle(VIZ_STYLE_NAME);

    // set default visual properties
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, new Double(1.0));
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_BEND, EdgeBendVisualProperty.DEFAULT_EDGE_BEND);

    // create viz mappings
    for (final GpmlToPathway.VizTableStore vizTableStore : GpmlToPathway.getAllVizTableStores()) {
      final Map<?,?> mapping = vizTableStore.getMapping();
      final VisualMappingFunctionFactory fnFactory = (mapping == null) ? passFnFactory : discFnFactory;
      for (final VisualProperty<?> vizProp : vizTableStore.getCyVizProps()) {
        final VisualMappingFunction<?,?> fn = fnFactory.createVisualMappingFunction(
        				vizTableStore.getCyColumnName(), vizTableStore.getCyColumnType(),  vizProp);
        if (mapping != null) {
          final DiscreteMapping discreteFn = (DiscreteMapping) fn;
          discreteFn.putAll(mapping);
        }
        vizStyle.addVisualMappingFunction(fn);
      }
    }

    vizMapMgr.addVisualStyle(vizStyle);
    return vizStyle;
  }

  public void apply(final CyNetworkView view) {
    if (vizStyle == null || !vizMapMgr.getAllVisualStyles().contains(vizStyle)) {
      vizStyle = create();
    }
    vizMapMgr.setVisualStyle(vizStyle, view);
    vizStyle.apply(view);
  }
}
