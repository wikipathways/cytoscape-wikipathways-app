package org.wikipathways.cytoscapeapp.internal.io;

import java.awt.Color;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.StaticProperty;
import org.pathvisio.core.model.GraphLink;
import org.pathvisio.core.model.ShapeType;
import org.pathvisio.core.model.LineStyle;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyIdentifiable;

import org.cytoscape.event.CyEventHelper;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.annotations.Annotation;


import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;

public class GpmlToPathway {
  /**
   * Maps a GPML pathway element to its representative CyNode in the network.
   */
  final Map<GraphLink.GraphIdContainer,CyNode> nodes = new HashMap<GraphLink.GraphIdContainer,CyNode>();

  //final Map<GraphLink.GraphIdContainer,Annotation> annots = new HashMap<GraphLink.GraphIdContainer,Annotation>();

  /**
   * In Cytoscape, first the network topology is created (via CyNetwork.add{Node|Edge}),
   * then the view objects are created.
   * Once that's done, the network's visual style can be created (via View.setVisualProperty)
   * once all the view objects exist (ensured by CyEventHelper.flushPayloadEvents).
   *
   * However, while we're reading GPML, we need to create the network's visual style
   * while we are creating the network toplogy. Otherwise we'd have to read the GPML twice,
   * once for topology and again for the visual style.
   *
   * How do we get around this problem? While we're reading GPML, we create the network topology
   * and store our desired visual style in DelayedVizProp objects (defined below).
   * After we finish reading GPML, we ensure that view objects have been created for
   * all our new nodes and edges (via CyEventHelper.flushPayloadEvents). Finally we apply
   * the visual style stored in the DelayedVizProp objects.
   */
  final List<DelayedVizProp> delayedVizProps = new ArrayList<DelayedVizProp>();

  final CyEventHelper     eventHelper;
  final Annots            annots;
	final Pathway           pathway;
  final CyNetworkView     networkView;
	final CyNetwork         network;
  final CyTable           nodeTbl;

  /**
   * Create a converter from the given pathway and store it in the given network.
   * Constructing this object will not start the conversion and will not modify
   * the given network in any way.
   * @param eventHelper The {@code CyEventHelper} service -- used to flush network object creation events
   * @param annotMgr The {@code AnnotationManager} service -- used for registering pathway annotations
   * @param annotFactory The {@code AnnotationFactory} service -- used for creating pathway annotations
   * @param pathway The GPML pathway object from which to convert
   * @param networkView The Cytoscape network to contain the GPML pathway
   */
	public GpmlToPathway(
      final CyEventHelper     eventHelper,
      final Annots            annots,
      final Pathway           pathway,
      final CyNetworkView     networkView) {
    this.eventHelper = eventHelper;
    this.annots = annots;
		this.pathway = pathway;
    this.networkView = networkView;
		this.network = networkView.getModel();
    this.nodeTbl = network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
	}

  /**
   * Convert the pathway given in the constructor.
   */
	public void convert() {
    setupTables();

    // convert by each pathway element type
    convertDataNodes();
    convertShapes();
    convertStates();
    convertGroups();
    convertLabels();
    convertAnchors();
    convertLines();

    eventHelper.flushPayloadEvents(); // guarantee that all node and edge views have been created
    DelayedVizProp.applyAll(networkView, delayedVizProps); // apply our visual style

    // clear our data structures just to be nice to the GC
    nodes.clear();
    delayedVizProps.clear();
	}

  /**
   * Ensure that the network's tables have the right columns.
   */
  private void setupTables() {
    nodeTbl.createColumn("GraphID", String.class, false);
    nodeTbl.createColumn("GeneID", String.class, false);
    nodeTbl.createColumn("Datasource", String.class, false);
  }

  /*
   ========================================================
     Static property conversion
   ========================================================
  */

  /**
   * Converts a GPML static property to a Cytoscape visual property.
   */
  static interface StaticPropConverter<S,V> {
    public V convert(S staticPropValue);
  }

  /**
   * Copies a GPML pathway element's static properties to a CyTable's row of elements.
   * @param elem The GPML pathway element from which static properties are copied.
   * @param staticProps A map of the GPML pathway element's static properties and their corresponding Cytoscape CyTable column names.
   * @param table A CyTable to which static properties are copied; this table must have the columns specified in {@code staticProps} already created with the correct types.
   * @param key The row in the CyTable to which static properties are copied.
   */
  private void convertStaticProps(final PathwayElement elem, final Map<StaticProperty,String> staticProps, final CyTable table, final Object key) {
    for (final Map.Entry<StaticProperty,String> staticPropEntry : staticProps.entrySet()) {
      final StaticProperty staticProp = staticPropEntry.getKey();
      Object value = elem.getStaticProperty(staticProp);
      if (value == null) continue;
      final String column = staticPropEntry.getValue();
      table.getRow(key).set(column, value);
    }
  }

  /**
   * Visual properties that should not be locked.
   */
  private static Set<VisualProperty<?>> unlockedVizProps = new HashSet<VisualProperty<?>>(Arrays.asList(
    BasicVisualLexicon.NODE_X_LOCATION,
    BasicVisualLexicon.NODE_Y_LOCATION
    ));

  /**
   * GPML start/end line types and their corresponding Cytoscape ArrowShape names.
   */
  static Map<String,ArrowShape> GPML_ARROW_SHAPES = new HashMap<String,ArrowShape>();
  static {
    GPML_ARROW_SHAPES.put("Arrow",              ArrowShapeVisualProperty.DELTA);
    GPML_ARROW_SHAPES.put("TBar",               ArrowShapeVisualProperty.T);
    GPML_ARROW_SHAPES.put("mim-binding",        ArrowShapeVisualProperty.ARROW);
    GPML_ARROW_SHAPES.put("mim-conversion",     ArrowShapeVisualProperty.ARROW);
    GPML_ARROW_SHAPES.put("mim-modification",   ArrowShapeVisualProperty.ARROW);
    GPML_ARROW_SHAPES.put("mim-catalysis",      ArrowShapeVisualProperty.CIRCLE);
    GPML_ARROW_SHAPES.put("mim-inhibition",     ArrowShapeVisualProperty.T);
    GPML_ARROW_SHAPES.put("mim-covalent-bond",  ArrowShapeVisualProperty.T);
  }

  /**
   * Converts a GPML start/end line type to a Cytoscape ArrowShape object.
   * This uses the {@code GPML_ARROW_NAME_TO_CYTOSCAPE} map to do the converstion.
   */
  private static StaticPropConverter<org.pathvisio.core.model.LineType,ArrowShape> ARROW_SHAPE_CONVERTER = new StaticPropConverter<org.pathvisio.core.model.LineType,ArrowShape>() {
    public ArrowShape convert(org.pathvisio.core.model.LineType lineType) {
      final String gpmlArrowName = lineType.getName();
      final ArrowShape arrowShape = GPML_ARROW_SHAPES.get(gpmlArrowName);
      if (arrowShape == null)
        return ArrowShapeVisualProperty.NONE;
      return arrowShape;
    }
  };

  /**
   * A map of all Cytoscape's LineType instances.
   * We can't use static fields in LineTypeVisualProperty because it doesn't contain all the
   * LineTypes we need, like "Parallel Lines".
   */
  static Map<String,LineType> LINE_TYPES = new HashMap<String,LineType>();
  static {
    for (final LineType lineType : ((DiscreteRange<LineType>) BasicVisualLexicon.EDGE_LINE_TYPE.getRange()).values()) {
      LINE_TYPES.put(lineType.getDisplayName(), lineType);
    }
  }

  /**
   * Converts a GPML line style (specified as an int) to a Cytoscape LineType object.
   */
  private static StaticPropConverter<Integer,LineType> LINE_TYPE_CONVERTER = new StaticPropConverter<Integer,LineType>() {
   public LineType convert(Integer lineStyle) {
    switch(lineStyle) {
      case LineStyle.DOUBLE:
        return LINE_TYPES.get("Parallel Lines");
      case LineStyle.DASHED:
        return LINE_TYPES.get("Dash");
      default:
        return LINE_TYPES.get("Solid");
      }
    } 
  };

  /**
   * A map of converters from GPML static properties to Cytoscape visual properties.
   */
  private static Map<StaticProperty,StaticPropConverter> VIZ_STATIC_PROP_CONVERTERS = new HashMap<StaticProperty,StaticPropConverter>();
  static {
    VIZ_STATIC_PROP_CONVERTERS.put(StaticProperty.TRANSPARENT,
      new StaticPropConverter<Boolean,Integer>() {
      public Integer convert(Boolean transparent) { return transparent ? 0 : 255; }
    });
    VIZ_STATIC_PROP_CONVERTERS.put(StaticProperty.SHAPETYPE,
      new StaticPropConverter<ShapeType,NodeShape>() {
      public NodeShape convert(ShapeType shape) {
        switch (shape) {
          case OVAL:
            return NodeShapeVisualProperty.ELLIPSE;
          case HEXAGON:
            return NodeShapeVisualProperty.HEXAGON;
          case ROUNDED_RECTANGLE:
            return NodeShapeVisualProperty.ROUND_RECTANGLE;
          case TRIANGLE:
            return NodeShapeVisualProperty.TRIANGLE;
          default:
            return NodeShapeVisualProperty.RECTANGLE;
        }
      }
    });
    VIZ_STATIC_PROP_CONVERTERS.put(StaticProperty.LINESTYLE,     LINE_TYPE_CONVERTER);
    VIZ_STATIC_PROP_CONVERTERS.put(StaticProperty.STARTLINETYPE, ARROW_SHAPE_CONVERTER);
    VIZ_STATIC_PROP_CONVERTERS.put(StaticProperty.ENDLINETYPE,   ARROW_SHAPE_CONVERTER);
  }

  private static Color DEFAULT_SELECTED_NODE_COLOR = new Color(255, 255, 204, 127);

  /**
   * For a GPML pathway element, convert one of its static properties to a Cytoscape View's VisualProperty value and store it in {@code delayedVizProps}.
   * If {@code staticProp} is a key in {@code STATIC_PROP_CONVERTERS}, its converter will be invoked before it is set as a visual property value.
   *
   * @param elem The GPML pathway element that contains a static property.
   * @param netObj Either a Cytoscape CyNode or CyEdge whose corresponding View should have a new VisualProperty.
   * @param staticProp The static property whose value is to be converted to a VisualProperty value.
   * @param vizProp The visual property to which to convert the static property's value.
   */
  private void convertViewStaticProp(final PathwayElement elem, final CyIdentifiable netObj, final StaticProperty staticProp, final VisualProperty vizProp) {
    Object value = elem.getStaticProperty(staticProp);
    if (value == null) return;
    if (VIZ_STATIC_PROP_CONVERTERS.containsKey(staticProp)) {
      value = VIZ_STATIC_PROP_CONVERTERS.get(staticProp).convert(value);
    }
    final boolean locked = !unlockedVizProps.contains(vizProp);
    delayedVizProps.add(new DelayedVizProp(netObj, vizProp, value, locked));
  }

  /**
   * Converts a series of a GPML pathway element's static proeprties to Cytoscape visual property values.
   * @param elem The GPML pathway element whose static properties are to be converted to visual property values.
   * @param props GPML static properties and their corresponding Cytoscape visual properties of which to convert.
   * @param netObj Either a CyNode or a CyEdge whose View would contain the visual properties.
   */
  private void convertViewStaticProps(final PathwayElement elem, final Map<StaticProperty,VisualProperty<?>> props, final CyIdentifiable netObj) {
    for (final Map.Entry<StaticProperty,VisualProperty<?>> prop : props.entrySet()) {
      final StaticProperty staticProp = prop.getKey();
      final VisualProperty<?> vizProp = prop.getValue();
      convertViewStaticProp(elem, netObj, staticProp, vizProp);
    }

    if (netObj instanceof CyNode) {
      delayedVizProps.add(new DelayedVizProp(netObj, BasicVisualLexicon.NODE_LABEL_FONT_FACE, convertFontFromStaticProps(elem), true));
      delayedVizProps.add(new DelayedVizProp(netObj, BasicVisualLexicon.NODE_SELECTED_PAINT, DEFAULT_SELECTED_NODE_COLOR, true));
    }
  }

  /**
   * Reads the FONTNAME, FONTWEIGHT, and FONTSTYLE GPML static properties and converts them to a Font object.
   */
  private static Font convertFontFromStaticProps(final PathwayElement elem) {
    String fontFace = (String) elem.getStaticProperty(StaticProperty.FONTNAME);
    if (fontFace == null)
      fontFace = Font.SANS_SERIF;
    int style = Font.PLAIN;
    if (Boolean.TRUE.equals(elem.getStaticProperty(StaticProperty.FONTWEIGHT)))
      style |= Font.BOLD;
    if (Boolean.TRUE.equals(elem.getStaticProperty(StaticProperty.FONTSTYLE)))
      style |= Font.ITALIC;
    return new Font(fontFace, style, 12 /* size doesn't matter here -- there's another viz prop for font size */);
  }

  /**
   * Overrides a node's border width by setting it to zero if the pathway element has a shape of type NONE.
   */
  private void convertShapeTypeNone(final CyNode node, final PathwayElement elem) {
    if (ShapeType.NONE.equals(elem.getShapeType())) {
      delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0, true));
    }
  }

  /*
   ========================================================
     Data nodes
   ========================================================
  */

  private static Map<StaticProperty,String> dataNodeStaticProps = new HashMap<StaticProperty,String>();
  static {
    dataNodeStaticProps.put(StaticProperty.GRAPHID,  "GraphID");
    dataNodeStaticProps.put(StaticProperty.TEXTLABEL, CyNetwork.NAME);
  }

  private static Map<StaticProperty,VisualProperty<?>> dataNodeViewStaticProps = new HashMap<StaticProperty,VisualProperty<?>>();
  static {
    dataNodeViewStaticProps.put(StaticProperty.CENTERX,       BasicVisualLexicon.NODE_X_LOCATION);
    dataNodeViewStaticProps.put(StaticProperty.CENTERY,       BasicVisualLexicon.NODE_Y_LOCATION);
    dataNodeViewStaticProps.put(StaticProperty.WIDTH,         BasicVisualLexicon.NODE_WIDTH);
    dataNodeViewStaticProps.put(StaticProperty.HEIGHT,        BasicVisualLexicon.NODE_HEIGHT);
    //dataNodeViewStaticProps.put(StaticProperty.COLOR,         BasicVisualLexicon.NODE_BORDER_PAINT);
    dataNodeViewStaticProps.put(StaticProperty.FILLCOLOR,     BasicVisualLexicon.NODE_FILL_COLOR);
    dataNodeViewStaticProps.put(StaticProperty.FONTSIZE,      BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
    dataNodeViewStaticProps.put(StaticProperty.TRANSPARENT,   BasicVisualLexicon.NODE_TRANSPARENCY);
    dataNodeViewStaticProps.put(StaticProperty.LINETHICKNESS, BasicVisualLexicon.NODE_BORDER_WIDTH);
    dataNodeViewStaticProps.put(StaticProperty.SHAPETYPE,     BasicVisualLexicon.NODE_SHAPE);
    dataNodeViewStaticProps.put(StaticProperty.LINESTYLE,     BasicVisualLexicon.NODE_BORDER_LINE_TYPE);
  }

  private void convertDataNodes() {
	dataNodeStaticProps.put(StaticProperty.GENEID, "GeneID");
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.DATANODE))
        continue;
      convertDataNode(elem);
    }
    dataNodeStaticProps.remove(StaticProperty.GENEID);
  }

  private void convertDataNode(final PathwayElement dataNode) {
    final CyNode node = network.addNode();
    
    if(dataNode.getDataSource() != null && dataNode.getDataSource().getFullName() != null) {
    	nodeTbl.getRow(node.getSUID()).set("Datasource", dataNode.getDataSource().getFullName());
    }
    convertStaticProps(dataNode, dataNodeStaticProps, nodeTbl, node.getSUID());
    convertViewStaticProps(dataNode, dataNodeViewStaticProps, node);
    convertShapeTypeNone(node, dataNode);
    nodes.put(dataNode, node);
  }

  /*
   ========================================================
     Shapes
   ========================================================
  */

  private void convertShapes() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.SHAPE))
        continue;
      convertDataNode(elem); // shapes are treated just like data nodes, but this will change in the future with annotations
    }
  }

  /*
  private void convertShape(final PathwayElement shape) {
    final CyNode node = network.addNode();
    convertStaticProps(shape, dataNodeStaticProps, network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS), node.getSUID());
    convertViewStaticProps(shape, dataNodeViewStaticProps, node);
    convertShapeTypeNone(node, shape);
    nodes.put(shape, node);
  }
  */
  
  /*
   ========================================================
     States
   ========================================================
  */

  private static Map<StaticProperty,String> stateStaticProps = new HashMap<StaticProperty,String>();
  static {
    stateStaticProps.put(StaticProperty.TEXTLABEL, CyNetwork.NAME);
  }

  private static Map<StaticProperty,VisualProperty<?>> stateViewStaticProps = new HashMap<StaticProperty,VisualProperty<?>>();
  static {
    stateViewStaticProps.put(StaticProperty.WIDTH,         BasicVisualLexicon.NODE_WIDTH);
    stateViewStaticProps.put(StaticProperty.HEIGHT,        BasicVisualLexicon.NODE_HEIGHT);
    stateViewStaticProps.put(StaticProperty.COLOR,         BasicVisualLexicon.NODE_BORDER_PAINT);
    stateViewStaticProps.put(StaticProperty.FILLCOLOR,     BasicVisualLexicon.NODE_FILL_COLOR);
    stateViewStaticProps.put(StaticProperty.FONTSIZE,      BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
    stateViewStaticProps.put(StaticProperty.TRANSPARENT,   BasicVisualLexicon.NODE_TRANSPARENCY);
    stateViewStaticProps.put(StaticProperty.SHAPETYPE,     BasicVisualLexicon.NODE_SHAPE);
    stateViewStaticProps.put(StaticProperty.LINETHICKNESS, BasicVisualLexicon.NODE_BORDER_WIDTH);
  }


  private void convertStates() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.STATE))
        continue;
      convertState(elem);
    }
  }

  private void convertState(final PathwayElement state) {
    final GraphLink.GraphIdContainer parentGIdC = pathway.getGraphIdContainer(state.getGraphRef());
    if (!(parentGIdC instanceof PathwayElement))
      return; // this should not happen
    final PathwayElement parentElem = (PathwayElement) parentGIdC;

    // TODO: refactor this as an annotation
    final CyNode node = network.addNode();
    convertStaticProps(state, stateStaticProps, nodeTbl, node.getSUID());
    convertViewStaticProps(state, stateViewStaticProps, node);
    convertShapeTypeNone(node, state);

    final double x = parentElem.getMCenterX() + state.getRelX() * parentElem.getMWidth() / 2.0;
    final double y = parentElem.getMCenterY() + state.getRelY() * parentElem.getMHeight() / 2.0;

    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_X_LOCATION, x, true));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_Y_LOCATION, y, true));
  }
  
  /*
   ========================================================
     Groups
   ========================================================
  */
 
  private void convertGroups() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.GROUP))
        continue;
      convertGroup(elem);
    }
  }

  private void convertGroup(final PathwayElement group) {
    final CyNode groupNode = network.addNode();
    nodes.put(group, groupNode);

    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_X_LOCATION, group.getMCenterX(), false));
    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_Y_LOCATION, group.getMCenterY(), false));
    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_WIDTH, group.getMWidth(), true));
    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_HEIGHT, group.getMHeight(), true));
    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_BORDER_WIDTH, 1.0, true));
    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_BORDER_LINE_TYPE, LINE_TYPES.get("Dots"), true));
    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_TRANSPARENCY, 0, true));
    delayedVizProps.add(new DelayedVizProp(groupNode, BasicVisualLexicon.NODE_SELECTED_PAINT, DEFAULT_SELECTED_NODE_COLOR, true));
  }

  /*
   ========================================================
     Labels
   ========================================================
  */

  private static Map<StaticProperty,String> labelStaticProps = new HashMap<StaticProperty,String>();
  static {
    labelStaticProps.put(StaticProperty.TEXTLABEL, CyNetwork.NAME);
  }

  private static Map<StaticProperty,VisualProperty<?>> labelViewStaticProps = new HashMap<StaticProperty,VisualProperty<?>>();
  static {
    labelViewStaticProps.put(StaticProperty.CENTERX,       BasicVisualLexicon.NODE_X_LOCATION);
    labelViewStaticProps.put(StaticProperty.CENTERY,       BasicVisualLexicon.NODE_Y_LOCATION);
    labelViewStaticProps.put(StaticProperty.WIDTH,         BasicVisualLexicon.NODE_WIDTH);
    labelViewStaticProps.put(StaticProperty.HEIGHT,        BasicVisualLexicon.NODE_HEIGHT);
    labelViewStaticProps.put(StaticProperty.COLOR,         BasicVisualLexicon.NODE_LABEL_COLOR);
    labelViewStaticProps.put(StaticProperty.FILLCOLOR,     BasicVisualLexicon.NODE_FILL_COLOR);
    labelViewStaticProps.put(StaticProperty.FONTSIZE,      BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
    labelViewStaticProps.put(StaticProperty.SHAPETYPE,     BasicVisualLexicon.NODE_SHAPE);
    labelViewStaticProps.put(StaticProperty.LINETHICKNESS, BasicVisualLexicon.NODE_BORDER_WIDTH);

  }

  private void convertLabels() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.LABEL))
        continue;
      convertLabel(elem);
    }
  }

  private void convertLabel(final PathwayElement label) {
    /*
    // TODO: refactor this as an annotation
	// comment Tina: not sure if they can all be replaced by annotations because they are often connected with data nodes
    final CyNode node = network.addNode();
    convertStaticProps(label, labelStaticProps, nodeTbl, node.getSUID());
    convertViewStaticProps(label, labelViewStaticProps, node);
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_TRANSPARENCY, 0, true)); // labels are always transparent
    convertShapeTypeNone(node, label);
    nodes.put(label, node);
    */
    annots.newText(networkView,
        "canvas",     Annotation.BACKGROUND,
        "x",          label.getStaticProperty(StaticProperty.CENTERX),
        "y",          label.getStaticProperty(StaticProperty.CENTERY),
        "zoom",       networkView.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR),
        "text",       label.getStaticProperty(StaticProperty.TEXTLABEL),
        "color",      ((Color) label.getStaticProperty(StaticProperty.COLOR)).getRGB(),
        "fontFamily", "Helvetica",
        "fontSize",   ((Number) label.getStaticProperty(StaticProperty.FONTSIZE)).intValue(),
        "fontStyle",  Font.PLAIN
      );
  }
  
  /*
   ========================================================
     Anchors
   ========================================================
  */

  private void convertAnchors() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.LINE))
        continue;
      if (elem.getMAnchors().size() == 0)
        continue;
      convertAnchorsInLine(elem);
    }
  }

  private void assignAnchorVizStyle(final CyNode node, final Point2D position) {
    assignAnchorVizStyle(node, position, Color.WHITE);
  }

  private void assignAnchorVizStyle(final CyNode node, final Point2D position, final Color color) {
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_X_LOCATION, position.getX(), false));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_Y_LOCATION, position.getY(), false));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_FILL_COLOR, color, true));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0, true));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_WIDTH, 5.0, true));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_HEIGHT, 5.0, true));
  }

  private void convertAnchorsInLine(final PathwayElement elem) {
    final MLine line = (MLine) elem;
    for (final MAnchor anchor : elem.getMAnchors()) {
      final CyNode node = network.addNode();
      final Point2D position = line.getConnectorShape().fromLineCoordinate(anchor.getPosition());
      nodes.put(anchor, node);
      assignAnchorVizStyle(node, position, line.getColor());
    }
  }
  
  /*
   ========================================================
     Lines
   ========================================================
  */

  private void convertLines() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!(elem.getObjectType().equals(ObjectType.LINE) || elem.getObjectType().equals(ObjectType.GRAPHLINE)))
        continue;
      convertLine(elem);
    }
  }

  private void convertLine(final PathwayElement elem) {
    final MLine line = (MLine) elem;
    final String startRef = line.getMStart().getGraphRef();
    final String endRef = line.getMEnd().getGraphRef();
    CyNode startNode = nodes.get(pathway.getGraphIdContainer(startRef));
    if (startNode == null) {
      startNode = network.addNode();
      assignAnchorVizStyle(startNode, line.getStartPoint());
    }
    CyNode endNode = nodes.get(pathway.getGraphIdContainer(endRef));
    if (endNode == null) {
      endNode = network.addNode();
      assignAnchorVizStyle(endNode, line.getEndPoint());
    }

    final MAnchor[] anchors = elem.getMAnchors().toArray(new MAnchor[0]);
    if (anchors.length > 0) {
      final CyEdge firstEdge = network.addEdge(startNode, nodes.get(anchors[0]), true);
      assignEdgeVizStyle(firstEdge, line, true, false);
      for (int i = 1; i < anchors.length; i++) {
        final CyEdge edge = network.addEdge(nodes.get(anchors[i - 1]), nodes.get(anchors[i]), true);
        assignEdgeVizStyle(edge, line, false, false);
      }
      final CyEdge lastEdge = network.addEdge(nodes.get(anchors[anchors.length - 1]), endNode, true);
      assignEdgeVizStyle(lastEdge, line, false, true);
    }
    else {
      final CyEdge edge = network.addEdge(startNode, endNode, true);
      assignEdgeVizStyle(edge, line, true, true);
    }
  }

  private static Map<StaticProperty,VisualProperty<?>> lineViewStaticProps = new HashMap<StaticProperty,VisualProperty<?>>();
  static {
    lineViewStaticProps.put(StaticProperty.COLOR,         BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
    lineViewStaticProps.put(StaticProperty.LINESTYLE,     BasicVisualLexicon.EDGE_LINE_TYPE);
    lineViewStaticProps.put(StaticProperty.LINETHICKNESS, BasicVisualLexicon.EDGE_WIDTH);
  }

  private void assignEdgeVizStyle(final CyEdge edge, final PathwayElement line, final boolean isFirst, final boolean isLast) {
    if (edge == null) return;
    convertViewStaticProps(line, lineViewStaticProps, edge);

    if (isFirst)
      convertViewStaticProp(line, edge, StaticProperty.STARTLINETYPE, BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE);
    if (isLast)
      convertViewStaticProp(line, edge, StaticProperty.ENDLINETYPE, BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);   
  }
}

class DelayedVizProp {
  final CyIdentifiable netObj;
  final VisualProperty<?> prop;
  final Object value;
  final boolean isLocked;

  public DelayedVizProp(final CyIdentifiable netObj, final VisualProperty<?> prop, final Object value, final boolean isLocked) {
    this.netObj = netObj;
    this.prop = prop;
    this.value = value;
    this.isLocked = isLocked;
  }

  public static void applyAll(final CyNetworkView netView, final Iterable<DelayedVizProp> delayedProps) {
    for (final DelayedVizProp delayedProp : delayedProps) {
      View<?> view = null;
      if (delayedProp.netObj instanceof CyNode) {
        final CyNode node = (CyNode) delayedProp.netObj;
        view = netView.getNodeView(node);
      } else if (delayedProp.netObj instanceof CyEdge) {
        final CyEdge edge = (CyEdge) delayedProp.netObj;
        view = netView.getEdgeView(edge);
      }

      if (delayedProp.isLocked) {
        view.setLockedValue(delayedProp.prop, delayedProp.value);
      } else {
        view.setVisualProperty(delayedProp.prop, delayedProp.value);
      }
    }
  }
}
