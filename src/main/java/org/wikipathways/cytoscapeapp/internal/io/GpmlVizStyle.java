package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
/*
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
*/
import java.awt.Color;

public class GpmlVizStyle {
  static final String VIZ_STYLE_NAME = "WikiPathways";

  final VisualStyleFactory vizStyleFactory;
  final VisualMappingManager vizMapMgr;
  final VisualMappingFunctionFactory contFnFactory;
  final VisualMappingFunctionFactory discFnFactory;
  final VisualMappingFunctionFactory passFnFactory;
  VisualStyle vizStyle = null;

  public GpmlVizStyle(
      final VisualStyleFactory vizStyleFactory,
      final VisualMappingManager vizMapMgr,
      final VisualMappingFunctionFactory contFnFactory,
      final VisualMappingFunctionFactory discFnFactory,
      final VisualMappingFunctionFactory passFnFactory) {
    this.vizStyleFactory = vizStyleFactory;
    this.vizMapMgr = vizMapMgr;
    this.contFnFactory = contFnFactory;
    this.discFnFactory = discFnFactory;
    this.passFnFactory = passFnFactory;
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

    final VisualStyle vizStyle = vizStyleFactory.createVisualStyle(vizMapMgr.getDefaultVisualStyle());

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
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);

    // create viz mappings
    for (final GpmlToPathway.VizTableStore vizTableStore : GpmlToPathway.getAllVizTableStores()) {
      final Map<?,?> mapping = vizTableStore.getMapping();
      final VisualMappingFunctionFactory fnFactory = (mapping == null) ? passFnFactory : discFnFactory;
      final VisualMappingFunction fn = fnFactory.createVisualMappingFunction(
          vizTableStore.getCyColumnName(),
          vizTableStore.getCyColumnType(),
          vizTableStore.getCyVizProp());
      if (mapping != null) {
        final DiscreteMapping discreteFn = (DiscreteMapping) fn;
        discreteFn.putAll(mapping);
      }
      vizStyle.addVisualMappingFunction(fn);
    }

    vizMapMgr.addVisualStyle(vizStyle);
    return vizStyle;
  }

  public void apply(final CyNetworkView view) {
    if (vizStyle == null || !vizMapMgr.getAllVisualStyles().contains(vizStyle)) {
      vizStyle = create();
    }
    vizMapMgr.setVisualStyle(vizStyle, view);
  }
}
