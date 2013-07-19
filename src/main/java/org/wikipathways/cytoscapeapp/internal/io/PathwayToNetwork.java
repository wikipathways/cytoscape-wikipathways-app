package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;

import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.StaticProperty;
import org.pathvisio.core.model.StaticPropertyType;

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

import org.wikipathways.cytoscapeapp.internal.CyActivator;

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
    CyActivator.eventHelper.flushPayloadEvents();
    DelayedVizProp.applyAll(networkView, delayedVizProps);
    delayedVizProps.clear();
	}

  private static Map<StaticPropertyType,Class<?>> staticPropTypeClasses = new EnumMap<StaticPropertyType,Class<?>>(StaticPropertyType.class);
  static {
    staticPropTypeClasses.put(StaticPropertyType.STRING, String.class);
    staticPropTypeClasses.put(StaticPropertyType.DOUBLE, Double.class);
  }

  private static Map<StaticProperty,String> dataNodeStaticProps = ezMap(StaticProperty.class, String.class,
    StaticProperty.TEXTLABEL, CyNetwork.NAME);

  private static Map<StaticProperty,VisualProperty> dataNodeViewStaticProps = ezMap(StaticProperty.class, VisualProperty.class,
    StaticProperty.CENTERX, BasicVisualLexicon.NODE_X_LOCATION,
    StaticProperty.CENTERY, BasicVisualLexicon.NODE_Y_LOCATION,
    StaticProperty.WIDTH,   BasicVisualLexicon.NODE_WIDTH,
    StaticProperty.HEIGHT,  BasicVisualLexicon.NODE_HEIGHT
    );

  private static Set<VisualProperty> lockedVizProps = new HashSet<VisualProperty>();
  static {
    lockedVizProps.add(BasicVisualLexicon.NODE_WIDTH);
    lockedVizProps.add(BasicVisualLexicon.NODE_HEIGHT);
  }

  private void convertStaticProps(final PathwayElement elem, final Map<StaticProperty,String> staticProps, final CyTable table, final Object key) {
    for (final Map.Entry<StaticProperty,String> staticProp : staticProps.entrySet()) {
      final Object value = elem.getStaticProperty(staticProp.getKey());
      final String column = staticProp.getValue();
      table.getRow(key).set(column, value);
    }
  }

  private void convertViewStaticProps(final PathwayElement elem, final Map<StaticProperty,VisualProperty> props, CyNode node) {
    for (final Map.Entry<StaticProperty,VisualProperty> prop : props.entrySet()) {
      final VisualProperty vizProp = prop.getValue();
      final boolean locked = lockedVizProps.contains(vizProp);
      delayedVizProps.add(new DelayedVizProp(node, vizProp, elem.getStaticProperty(prop.getKey()), locked));
    }
  }

  private void convertDataNodes() {
    for (final PathwayElement elem : pathway.getDataObjects()) {
      if (elem.getObjectType().equals(ObjectType.DATANODE)) {
        convertDataNode(elem);
      }
    }
  }

  private void convertDataNode(final PathwayElement dataNode) {
    final CyNode node = network.addNode();
    convertStaticProps(dataNode, dataNodeStaticProps, network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS), node.getSUID());
    convertViewStaticProps(dataNode, dataNodeViewStaticProps, node);
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