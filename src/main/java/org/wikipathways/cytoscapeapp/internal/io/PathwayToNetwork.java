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

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;

import org.wikipathways.cytoscapeapp.internal.CyActivator;

import java.awt.Font;

class PathwayToNetwork {
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
    convertLabels();
    convertAnchors();

    CyActivator.eventHelper.flushPayloadEvents();
    DelayedVizProp.applyAll(networkView, delayedVizProps);
    delayedVizProps.clear(); // garbage collect DelayedVizProps

    CyActivator.vizMapMgr.getDefaultVisualStyle().apply(networkView);
    networkView.fitContent();
    networkView.updateView();
	}

  /*
  private static Map<StaticPropertyType,Class<?>> staticPropTypeClasses = new EnumMap<StaticPropertyType,Class<?>>(StaticPropertyType.class);
  static {
    staticPropTypeClasses.put(StaticPropertyType.STRING, String.class);
    staticPropTypeClasses.put(StaticPropertyType.DOUBLE, Double.class);
  }
  */

  private static Map<StaticProperty,String> dataNodeStaticProps = ezMap(StaticProperty.class, String.class,
    StaticProperty.TEXTLABEL, CyNetwork.NAME);

  private static Map<StaticProperty,VisualProperty> dataNodeViewStaticProps = ezMap(StaticProperty.class, VisualProperty.class,
    StaticProperty.CENTERX, BasicVisualLexicon.NODE_X_LOCATION,
    StaticProperty.CENTERY, BasicVisualLexicon.NODE_Y_LOCATION,
    StaticProperty.WIDTH,   BasicVisualLexicon.NODE_WIDTH,
    StaticProperty.HEIGHT,  BasicVisualLexicon.NODE_HEIGHT,
    StaticProperty.COLOR, BasicVisualLexicon.NODE_BORDER_PAINT,
    StaticProperty.FILLCOLOR, BasicVisualLexicon.NODE_FILL_COLOR,
    StaticProperty.FONTSIZE, BasicVisualLexicon.NODE_LABEL_FONT_SIZE,
    StaticProperty.TRANSPARENT, BasicVisualLexicon.NODE_TRANSPARENCY,
    StaticProperty.LINETHICKNESS, BasicVisualLexicon.NODE_BORDER_WIDTH
    );

  private static Set<VisualProperty> lockedVizProps = new HashSet<VisualProperty>(Arrays.asList(
    BasicVisualLexicon.NODE_WIDTH,
    BasicVisualLexicon.NODE_HEIGHT,
    BasicVisualLexicon.NODE_BORDER_PAINT,
    BasicVisualLexicon.NODE_FILL_COLOR,
    BasicVisualLexicon.NODE_LABEL_FONT_SIZE,
    BasicVisualLexicon.NODE_TRANSPARENCY,
    BasicVisualLexicon.NODE_BORDER_WIDTH
    ));

  private void convertStaticProps(final PathwayElement elem, final Map<StaticProperty,String> staticProps, final CyTable table, final Object key) {
    for (final Map.Entry<StaticProperty,String> staticProp : staticProps.entrySet()) {
      final Object value = elem.getStaticProperty(staticProp.getKey());
      if (value == null) continue;
      final String column = staticProp.getValue();
      table.getRow(key).set(column, value);
    }
  }

  private void convertViewStaticProps(final PathwayElement elem, final Map<StaticProperty,VisualProperty> props, CyNode node) {
    //System.out.print(elem.getGraphId() + ": ");
    //System.out.flush();
    for (final Map.Entry<StaticProperty,VisualProperty> prop : props.entrySet()) {
      final StaticProperty staticProp = prop.getKey();
      Object value = elem.getStaticProperty(prop.getKey());
      //System.out.print(staticProp + " = " + value + ", ");
      //System.out.flush();
      if (value == null) continue;
      if (staticPropConverters.containsKey(staticProp)) {
        //System.out.print("[CONVERT] " + value + ", ");
        //System.out.flush();
        value = staticPropConverters.get(staticProp).convert(value);
      }
      final VisualProperty vizProp = prop.getValue();
      final boolean locked = lockedVizProps.contains(vizProp);
      delayedVizProps.add(new DelayedVizProp(node, vizProp, value, locked));
    }
    //System.out.println();

    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_LABEL_FONT_FACE, convertFontFromStaticProps(elem), true));
  }

  private void convertDataNodes() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.DATANODE))
        continue;
      convertDataNode(elem);
    }
  }

  private void convertLabels() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (!elem.getObjectType().equals(ObjectType.LABEL))
        continue;
      convertLabel(elem);
    }
  }

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

  private void convertDataNode(final PathwayElement dataNode) {
    final CyNode node = network.addNode();
    convertStaticProps(dataNode, dataNodeStaticProps, network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS), node.getSUID());
    convertViewStaticProps(dataNode, dataNodeViewStaticProps, node);
  }

  private void convertLabel(final PathwayElement label) {
    /*
    final Map<String,String> args = ezMap(
      Annotation.CANVAS, Annotation.FOREGROUND,
      Annotation.X, (String) label.getStaticProperty(StaticProperty.CENTERX),
      Annotation.Y, (String) label.getStaticProperty(StaticProperty.CENTERY)
      );
    final TextAnnotation annotation = CyActivator.annotationFactory.createAnnotation(TextAnnotation.class, args);
    CyActivator.annotationMgr.addAnnotation(annotation, networkView);
    */
  }

  private void convertAnchor(final PathwayElement elem) {
    //System.out.println(String.format("Anchor: %s @ [%.2f, %.2f] [%.2f x %.2f]", elem.getGraphId(), elem.getMCenterX(), elem.getMCenterY(), elem.getMWidth(), elem.getMHeight()));
  }

  private boolean areStartAndEndNodes(final PathwayElement elem) {
    final GraphLink.GraphIdContainer start = pathway.getGraphIdContainer(elem.getMStart().getGraphRef());
    final GraphLink.GraphIdContainer end = pathway.getGraphIdContainer(elem.getMEnd().getGraphRef());
    return isNode(start) && isNode(end);
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

  private static Map<StaticProperty,StaticPropConverter> staticPropConverters = ezMap(StaticProperty.class, StaticPropConverter.class,
    StaticProperty.TRANSPARENT, new TransparencyConverter()
    );

  private static Font convertFontFromStaticProps(final PathwayElement elem) {
    String fontFace = (String) elem.getStaticProperty(StaticProperty.FONTNAME);
    if (fontFace == null)
      fontFace = Font.SANS_SERIF;
    int style = Font.PLAIN;
    if (Boolean.TRUE.equals(elem.getStaticProperty(StaticProperty.FONTWEIGHT)))
      style |= Font.BOLD;
    if (Boolean.TRUE.equals(elem.getStaticProperty(StaticProperty.FONTSTYLE)))
      style |= Font.ITALIC;
    return new Font(fontFace, style, 12);
  }
}

interface StaticPropConverter<S,V> {
  public V convert(S staticPropValue);
}

class TransparencyConverter implements StaticPropConverter<Boolean,Integer> {
  public Integer convert(Boolean transparent) {
    if (transparent)
      return 0;
    else
      return 255;
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

  public String toString() {
    return String.format("[%s %d] %s -> %s [%s]%s", isNode ? "node":"edge", netObjSUID, prop.getDisplayName(), value, value.getClass(), isLocked ? " [locked]" : "");
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

      /*
      if (delayedProp.prop.equals(BasicVisualLexicon.NODE_TRANSPARENCY)) {
        System.out.println("Applying: " + delayedProp.toString());
        System.out.flush();
      }
      */

      if (delayedProp.isLocked) {
        view.setLockedValue(delayedProp.prop, delayedProp.value);
      } else {
        view.setVisualProperty(delayedProp.prop, delayedProp.value);
      }
    }
  }
}