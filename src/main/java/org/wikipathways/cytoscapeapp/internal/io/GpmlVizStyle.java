package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;

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

  private VisualStyle create() {
    final VisualStyle vizStyle = vizStyleFactory.createVisualStyle(vizMapMgr.getDefaultVisualStyle());
    vizStyle.setTitle("WikiPathways");
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);
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
    for (final VisualPropertyDependency<?> dep : vizStyle.getAllVisualPropertyDependencies()) {
      System.out.println(dep.getIdString());
      final String id = dep.getIdString();
      if ("nodeSizeLocked".equals(id)) {
        dep.setDependency(false);
      } else if ("arrowColorMatchesEdge".equals(id)) {
        dep.setDependency(true);
      }
    }
    return vizStyle;
  }

  public void apply(final CyNetworkView view) {
    if (vizStyle == null) {
      vizStyle = create();
    }
    vizMapMgr.setVisualStyle(vizStyle, view);
  }
}
