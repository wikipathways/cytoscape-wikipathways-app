
package org.wikipathways.cytoscapeapp.impl;

import java.awt.Color;
import java.awt.Shape;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.EdgeBendVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.AbstractVisualPropertyValue;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.NodeShape;
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
	  
	    vizStyle = vizStyleFactory.createVisualStyle(vizMapMgr.getDefaultVisualStyle());	
//	  vizStyle =  vizMapMgr.addVisualStyle(vizMapMgr.getDefaultVisualStyle());
	  if (vizMapMgr != null)   return vizStyle;
  
	  
    removeOldVizStyle();

    vizStyle = vizStyleFactory.createVisualStyle(vizMapMgr.getDefaultVisualStyle());			//vizMapMgr.getDefaultVisualStyle()

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
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.RECTANGLE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, new Double(1.0));
//    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_BEND, EdgeBendVisualProperty.DEFAULT_EDGE_BEND);
    vizStyle.setDefaultValue(BasicVisualLexicon.EDGE_PAINT, Color.BLACK);

  final VisualMappingFunctionFactory fnFactory = discFnFactory;
   
  final VisualMappingFunction<?,?> fn = fnFactory.createVisualMappingFunction("Shape", String.class,   BasicVisualLexicon.NODE_SHAPE);
  vizStyle.addVisualMappingFunction(fn);

    // create viz mappings
//    for (final GpmlToPathway.VizTableStore vizTableStore : GpmlToPathway.getAllVizTableStores()) {
//      final Map<?,?> mapping = vizTableStore.getMapping();
//      final VisualMappingFunctionFactory fnFactory = (mapping == null) ? passFnFactory : discFnFactory;
//      for (final VisualProperty<?> vizProp : vizTableStore.getCyVizProps()) {
//        final VisualMappingFunction<?,?> fn = fnFactory.createVisualMappingFunction(
//        				vizTableStore.getCyColumnName(), vizTableStore.getCyColumnType(),  vizProp);
//        if (mapping != null) {
//          final DiscreteMapping discreteFn = (DiscreteMapping) fn;
//          discreteFn.putAll(mapping);
//        }
//        vizStyle.addVisualMappingFunction(fn);
//      }
//    }
    
    
    addMapping("Width", Double.class, BasicVisualLexicon.NODE_WIDTH);
    addMapping("Height", Double.class, BasicVisualLexicon.NODE_HEIGHT);
    vizMapMgr.addVisualStyle(vizStyle);
    return vizStyle;
  }

  static Map<String,ArrowShape> PV_ARROW_MAP = new HashMap<String,ArrowShape>();
  static {
    PV_ARROW_MAP.put("Arrow",              ArrowShapeVisualProperty.DELTA);	
    PV_ARROW_MAP.put("Line",               ArrowShapeVisualProperty.NONE);
    PV_ARROW_MAP.put("TBar",               ArrowShapeVisualProperty.T);
    PV_ARROW_MAP.put("mim-binding",        ArrowShapeVisualProperty.ARROW);
    PV_ARROW_MAP.put("mim-conversion",     ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("mim-modification",   ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("mim-catalysis",      ArrowShapeVisualProperty.OPEN_CIRCLE);
    PV_ARROW_MAP.put("mim-inhibition",     ArrowShapeVisualProperty.T);
    PV_ARROW_MAP.put("mim-necessary-stimulation",     ArrowShapeVisualProperty.CROSS_OPEN_DELTA);
    PV_ARROW_MAP.put("mim-stimulation",     ArrowShapeVisualProperty.OPEN_DELTA);
    PV_ARROW_MAP.put("mim-cleavage",     	ArrowShapeVisualProperty.DIAMOND);
    PV_ARROW_MAP.put("mim-branching-left",  ArrowShapeVisualProperty.CROSS_DELTA);
    PV_ARROW_MAP.put("mim-branching-right", ArrowShapeVisualProperty.CROSS_OPEN_DELTA);
    PV_ARROW_MAP.put("mim-transcription-translation",     ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("mim-gap",    			ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("mim-covalent-bond",  	ArrowShapeVisualProperty.CROSS_DELTA);
  }

  static Map<String,NodeShape> PV_SHAPE_MAP = new HashMap<String,NodeShape>();
  static {
    PV_SHAPE_MAP.put("Rectangle",        NodeShapeVisualProperty.RECTANGLE);
    PV_SHAPE_MAP.put("Triangle",         NodeShapeVisualProperty.TRIANGLE);			// Note: triangle is different shape than PV's
    PV_SHAPE_MAP.put("RoundRectangle", 	NodeShapeVisualProperty.ROUND_RECTANGLE);
    PV_SHAPE_MAP.put("RoundedRectangle", NodeShapeVisualProperty.ROUND_RECTANGLE);
    PV_SHAPE_MAP.put("Hexagon",          NodeShapeVisualProperty.HEXAGON);
    PV_SHAPE_MAP.put("Pentagon",         NodeShapeVisualProperty.HEXAGON);			// TODO
    PV_SHAPE_MAP.put("Ellipse",          NodeShapeVisualProperty.ELLIPSE);
    PV_SHAPE_MAP.put("Oval",             NodeShapeVisualProperty.ELLIPSE);
    PV_SHAPE_MAP.put("Octagon",          NodeShapeVisualProperty.OCTAGON);
    
    
    PV_SHAPE_MAP.put("Cell",          	new NodeShapeImpl("Cell")); 
    PV_SHAPE_MAP.put("Nucleus",          new NodeShapeImpl("Nucleus"));
    PV_SHAPE_MAP.put("Organelle",        NodeShapeVisualProperty.ROUND_RECTANGLE);
    PV_SHAPE_MAP.put("Mitochondria",     new NodeShapeImpl("Mitochondria"));	
    PV_SHAPE_MAP.put("Sarcoplasmic Reticulum", new NodeShapeImpl("Sarcoplasmic Reticulum"));	
    PV_SHAPE_MAP.put("Endoplasmic Reticulum", new NodeShapeImpl("Endoplasmic Reticulum"));	
    PV_SHAPE_MAP.put("Golgi Apparatus", new NodeShapeImpl("Golgi Apparatus"));	
    PV_SHAPE_MAP.put("Brace",     		new NodeShapeImpl("Brace"));		
    PV_SHAPE_MAP.put("Arc",     			new NodeShapeImpl("Arc"));		
    

  }
	private static final class NodeShapeImpl extends AbstractVisualPropertyValue implements NodeShape {
		public NodeShapeImpl(final String displayName) {
			super(displayName, displayName);
		}
	}

  void addMapping(String colname, Class type, VisualProperty<?> vizProp)
  {
    VisualMappingFunction<?,?> fn = passFnFactory.createVisualMappingFunction( colname, type,  vizProp);
    vizStyle.addVisualMappingFunction(fn);
	  
  }
  
  public void apply(final CyNetworkView view) {
 if (vizStyle == null || !vizMapMgr.getAllVisualStyles().contains(vizStyle)) {
      vizStyle = create();
    }
    vizMapMgr.setVisualStyle(vizStyle, view);
    vizStyle.apply(view);
  }
}
