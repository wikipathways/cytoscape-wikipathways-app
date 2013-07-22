package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.StaticProperty;
import org.pathvisio.core.model.StaticPropertyType;
import org.pathvisio.core.model.GraphLink;
import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.ShapeType;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.group.CyGroup;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;

import org.wikipathways.cytoscapeapp.internal.CyActivator;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;

class PathwayToNetwork {
  /**
   * Maps a GPML pathway element to its representative CyNode in the network.
   */
  final Map<GraphLink.GraphIdContainer,CyNode> nodes = new HashMap<GraphLink.GraphIdContainer,CyNode>();

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

	final Pathway pathway;
  final CyNetworkView networkView;
	final CyNetwork network;

	public PathwayToNetwork(final Pathway pathway, final CyNetworkView networkView) {
		this.pathway = pathway;
    this.networkView = networkView;
		this.network = networkView.getModel();
	}

	public void convert() {
    convertDataNodes();
    convertStates();
    convertGroups();
    convertLabels();
    convertAnchors();
    convertLines();

    CyActivator.eventHelper.flushPayloadEvents();
    DelayedVizProp.applyAll(networkView, delayedVizProps);

    CyActivator.vizMapMgr.getDefaultVisualStyle().apply(networkView);
    networkView.fitContent();
    networkView.updateView();

    nodes.clear();
    delayedVizProps.clear();
	}

  /*
   ========================================================
     Static property conversion
   ========================================================
  */

  private static Set<VisualProperty> unlockedVizProps = new HashSet<VisualProperty>(Arrays.asList(
    BasicVisualLexicon.NODE_X_LOCATION,
    BasicVisualLexicon.NODE_Y_LOCATION
    ));

  static interface StaticPropConverter<S,V> {
    public V convert(S staticPropValue);
  }

  private static Map<StaticProperty,StaticPropConverter> staticPropConverters = ezMap(StaticProperty.class, StaticPropConverter.class,
    StaticProperty.TRANSPARENT, new StaticPropConverter<Boolean,Integer>() {
      public Integer convert(Boolean transparent) { return transparent ? 0 : 255; }
    },
    StaticProperty.SHAPETYPE, new StaticPropConverter<ShapeType,NodeShape>() {
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
        }
        return NodeShapeVisualProperty.RECTANGLE;
      }
    }
    );

  private void convertStaticProps(final PathwayElement elem, final Map<StaticProperty,String> staticProps, final CyTable table, final Object key) {
    for (final Map.Entry<StaticProperty,String> staticProp : staticProps.entrySet()) {
      final Object value = elem.getStaticProperty(staticProp.getKey());
      if (value == null) continue;
      final String column = staticProp.getValue();
      table.getRow(key).set(column, value);
    }
  }

  private void convertViewStaticProps(final PathwayElement elem, final Map<StaticProperty,VisualProperty> props, CyNode node) {
    for (final Map.Entry<StaticProperty,VisualProperty> prop : props.entrySet()) {
      final StaticProperty staticProp = prop.getKey();
      Object value = elem.getStaticProperty(prop.getKey());
      if (value == null) continue;
      if (staticPropConverters.containsKey(staticProp)) {
        value = staticPropConverters.get(staticProp).convert(value);
      }
      final VisualProperty vizProp = prop.getValue();
      final boolean locked = !unlockedVizProps.contains(vizProp);
      delayedVizProps.add(new DelayedVizProp(node, vizProp, value, locked));
    }

    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_LABEL_FONT_FACE, convertFontFromStaticProps(elem), true));
  }

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

  private void convertShapeTypeNone(final CyNode node, final PathwayElement elem) {
    if (ShapeType.NONE.equals(elem.getShapeType())) {
      delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0, true));
    }
  }

  /*
   ========================================================
     GPML edge util methods
   ========================================================
  */

  private GraphLink.GraphIdContainer getStartOfLine(final PathwayElement line) {
    return pathway.getGraphIdContainer(line.getMStart().getGraphRef());
  }

  private GraphLink.GraphIdContainer getEndOfLine(final PathwayElement line) {
    return pathway.getGraphIdContainer(line.getMEnd().getGraphRef());
  }

  private boolean areStartAndEndNodes(final PathwayElement elem) {
    return isNode(getStartOfLine(elem)) && isNode(getEndOfLine(elem));
  }

  private boolean isNode(final GraphLink.GraphIdContainer elem) {
    if (elem instanceof PathwayElement.MAnchor) {
      return areStartAndEndNodes(((PathwayElement.MAnchor) elem).getParent());
    } else if (elem instanceof PathwayElement) {
      switch(((PathwayElement) elem).getObjectType()) {
        case DATANODE: return true;
        case GROUP: return true;
      }
    }
    return false;
  }

  /*
   ========================================================
     Data nodes
   ========================================================
  */

  private static Map<StaticProperty,String> dataNodeStaticProps = ezMap(StaticProperty.class, String.class,
    StaticProperty.TEXTLABEL, CyNetwork.NAME);

  private static Map<StaticProperty,VisualProperty> dataNodeViewStaticProps = ezMap(StaticProperty.class, VisualProperty.class,
    StaticProperty.CENTERX,       BasicVisualLexicon.NODE_X_LOCATION,
    StaticProperty.CENTERY,       BasicVisualLexicon.NODE_Y_LOCATION,
    StaticProperty.WIDTH,         BasicVisualLexicon.NODE_WIDTH,
    StaticProperty.HEIGHT,        BasicVisualLexicon.NODE_HEIGHT,
    StaticProperty.COLOR,         BasicVisualLexicon.NODE_BORDER_PAINT,
    StaticProperty.FILLCOLOR,     BasicVisualLexicon.NODE_FILL_COLOR,
    StaticProperty.FONTSIZE,      BasicVisualLexicon.NODE_LABEL_FONT_SIZE,
    StaticProperty.TRANSPARENT,   BasicVisualLexicon.NODE_TRANSPARENCY,
    StaticProperty.LINETHICKNESS, BasicVisualLexicon.NODE_BORDER_WIDTH,
    StaticProperty.SHAPETYPE,     BasicVisualLexicon.NODE_SHAPE
    );

  private void convertDataNodes() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.DATANODE))
        continue;
      convertDataNode(elem);
    }
  }

  private void convertDataNode(final PathwayElement dataNode) {
    final CyNode node = network.addNode();
    convertStaticProps(dataNode, dataNodeStaticProps, network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS), node.getSUID());
    convertViewStaticProps(dataNode, dataNodeViewStaticProps, node);
    convertShapeTypeNone(node, dataNode);
    nodes.put(dataNode, node);
  }
  
  /*
   ========================================================
     States
   ========================================================
  */

  private static Map<StaticProperty,String> stateStaticProps = ezMap(StaticProperty.class, String.class,
    StaticProperty.TEXTLABEL, CyNetwork.NAME);

  private static Map<StaticProperty,VisualProperty> stateViewStaticProps = ezMap(StaticProperty.class, VisualProperty.class,
    StaticProperty.WIDTH,         BasicVisualLexicon.NODE_WIDTH,
    StaticProperty.HEIGHT,        BasicVisualLexicon.NODE_HEIGHT,
    StaticProperty.COLOR,         BasicVisualLexicon.NODE_BORDER_PAINT,
    StaticProperty.FILLCOLOR,     BasicVisualLexicon.NODE_FILL_COLOR,
    StaticProperty.FONTSIZE,      BasicVisualLexicon.NODE_LABEL_FONT_SIZE,
    StaticProperty.TRANSPARENT,   BasicVisualLexicon.NODE_TRANSPARENCY,
    StaticProperty.LINETHICKNESS, BasicVisualLexicon.NODE_BORDER_WIDTH,
    StaticProperty.SHAPETYPE,     BasicVisualLexicon.NODE_SHAPE
    );


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
    convertStaticProps(state, stateStaticProps, network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS), node.getSUID());
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
    List<CyNode> groupNodes = new ArrayList<CyNode>();
    for (final PathwayElement elem : pathway.getGroupElements(group.getGroupId())) {
      final CyNode node = nodes.get(elem);
      if (node == null)
        continue;
      groupNodes.add(node);
    }

    final CyGroup cyGroup = CyActivator.groupFactory.createGroup(network, groupNodes, null, true);
    nodes.put(group, cyGroup.getGroupNode());
  }
  
  /*
   ========================================================
     Labels
   ========================================================
  */

  private static Map<StaticProperty,String> labelStaticProps = ezMap(StaticProperty.class, String.class,
    StaticProperty.TEXTLABEL, CyNetwork.NAME);

  private static Map<StaticProperty,VisualProperty> labelViewStaticProps = ezMap(StaticProperty.class, VisualProperty.class,
    StaticProperty.CENTERX,       BasicVisualLexicon.NODE_X_LOCATION,
    StaticProperty.CENTERY,       BasicVisualLexicon.NODE_Y_LOCATION,
    StaticProperty.WIDTH,         BasicVisualLexicon.NODE_WIDTH,
    StaticProperty.HEIGHT,        BasicVisualLexicon.NODE_HEIGHT,
    StaticProperty.COLOR,         BasicVisualLexicon.NODE_LABEL_COLOR,
    StaticProperty.FILLCOLOR,     BasicVisualLexicon.NODE_FILL_COLOR,
    StaticProperty.FONTSIZE,      BasicVisualLexicon.NODE_LABEL_FONT_SIZE,
    StaticProperty.LINETHICKNESS, BasicVisualLexicon.NODE_BORDER_WIDTH,
    StaticProperty.SHAPETYPE,     BasicVisualLexicon.NODE_SHAPE
    );

  private void convertLabels() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.LABEL))
        continue;
      convertLabel(elem);
    }
  }

  private void convertLabel(final PathwayElement label) {
    // TODO: refactor this as an annotation
    final CyNode node = network.addNode();
    convertStaticProps(label, labelStaticProps, network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS), node.getSUID());
    convertViewStaticProps(label, labelViewStaticProps, node);
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_TRANSPARENCY, 0, true)); // labels are always transparent
    convertShapeTypeNone(node, label);
    nodes.put(label, node);
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
      if (!areStartAndEndNodes(elem))
        continue;
      convertAnchor(elem);
    }
  }

  private void convertAnchor(final PathwayElement elem) {
    final CyNode node = network.addNode();
    assignAnchorVizStyle(node, (MLine) elem);

    nodes.put(elem, node);
    for (final PathwayElement.MAnchor anchor : elem.getMAnchors()) {
      nodes.put(anchor, node);
    }
  }

  private void assignAnchorVizStyle(final CyNode node, final MLine line) {
    final PathwayElement.MAnchor firstAnchor = line.getMAnchors().get(0);
    final Point2D firstAnchorPoint = line.getConnectorShape().fromLineCoordinate(firstAnchor.getPosition());
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_X_LOCATION, firstAnchorPoint.getX(), false));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_Y_LOCATION, firstAnchorPoint.getY(), false));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE, true));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_BORDER_WIDTH, 1.0, true));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_WIDTH, 5.0, true));
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_HEIGHT, 5.0, true));
  }
  
  /*
   ========================================================
     Lines
   ========================================================
  */

  private void convertLines() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!(elem.getObjectType().equals(ObjectType.LINE)))
        continue;
      if (!areStartAndEndNodes(elem))
        continue;
      if (elem.getMAnchors().size() > 0) {
        convertLineWithAnchor(elem);
      } else {
        convertLineWithoutAnchor(elem);
      }
    }
  }

  private void convertLineWithAnchor(final PathwayElement line) {
    final CyNode start = nodes.get(getStartOfLine(line));
    final CyNode middle = nodes.get(line); // this node was created in convertAnchor()
    final CyNode end = nodes.get(getEndOfLine(line));
    network.addEdge(start, middle, true);
    network.addEdge(middle, end, true);
  }

  private void convertLineWithoutAnchor(final PathwayElement line) {
    final CyNode start = nodes.get(getStartOfLine(line));
    final CyNode end = nodes.get(getEndOfLine(line));
    network.addEdge(start, end, true);
  }
  
  /*
   ========================================================
     Collection util methods
   ========================================================
  */

  private static <E> Map<E,E> ezMap(E ... elems) {
    final Map<E,E> map = new HashMap<E,E>();
    for (int i = 0; i < elems.length; i += 2) {
      map.put(elems[i], elems[i+1]);
    }
    return map;
  }

  private static <K,V> Map<K,V> ezMap(Class<? extends K> keyType, Class<? extends V> valueType, Object ... elems) {
    final Map<K,V> map = new HashMap<K,V>();
    for (int i = 0; i < elems.length; i += 2) {
      map.put(keyType.cast(elems[i]), valueType.cast(elems[i+1]));
    }
    return map;
  }
}

class DelayedVizProp {
  final long netObjSUID;
  final boolean isNode;
  final VisualProperty<?> prop;
  final Object value;
  final boolean isLocked;

  public DelayedVizProp(final CyNode node, final VisualProperty<?> prop, final Object value, final boolean isLocked) {
    this.netObjSUID = node.getSUID();
    this.isNode = true;
    this.prop = prop;
    this.value = value;
    this.isLocked = isLocked;
  }

  public DelayedVizProp(final CyEdge edge, final VisualProperty<?> prop, final Object value, final boolean isLocked) {
    this.netObjSUID = edge.getSUID();
    this.isNode = false;
    this.prop = prop;
    this.value = value;
    this.isLocked = isLocked;
  }

  public static void applyAll(final CyNetworkView netView, final Iterable<DelayedVizProp> delayedProps) {
    final CyNetwork net = netView.getModel();
    for (final DelayedVizProp delayedProp : delayedProps) {
      View<?> view;
      if (delayedProp.isNode) {
        final CyNode node = net.getNode(delayedProp.netObjSUID);
        view = netView.getNodeView(node);
      } else {
        final CyEdge edge = net.getEdge(delayedProp.netObjSUID);
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