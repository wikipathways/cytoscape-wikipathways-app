package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.EnumMap;
import java.util.EnumSet;

import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.StaticProperty;
import org.pathvisio.core.model.StaticPropertyType;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
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
  final DataNodeConverter dataNodeConverter = new DataNodeConverter();

	final Pathway pathway;
  final CyNetworkView networkView;
	final CyNetwork network;

  final CyTable graphIds = newGraphIds();

	public PathwayToNetwork(final Pathway pathway, final CyNetworkView networkView) {
		this.pathway = pathway;
    this.networkView = networkView;
		this.network = networkView.getModel();
	}

	public void convertToTables() {
		for (final PathwayElement elem : pathway.getDataObjects()) {
			switch(elem.getObjectType()) {
        case DATANODE:
          convertDataNode(elem);
          break;
				case SHAPE:
				case GRAPHLINE:
				case LABEL:
				case LINE:
				case LEGEND:
				case INFOBOX:
				case MAPPINFO:
				case GROUP:
				case BIOPAX:
				case STATE:
        default:
          break;
			}
		}
    dump(dataNodeConverter.table());
    dump(graphIds);
    loadNodeProps();
    CyActivator.eventHelper.flushPayloadEvents();
    loadNodeViewProps();
	}

  private static CyTable newGraphIds() {
    final CyTable table = CyActivator.tableFactory.createTable("PathwayElement graphIds", "graphId", String.class, true, false);
    table.createColumn("SUID", Long.class, true);
    table.createColumn("type", String.class, true);
    return table;
  }

  private void addGraphId(final PathwayElement elem, final CyIdentifiable obj, final String type) {
    final CyRow row = graphIds.getRow(elem.getGraphId());
    row.set("SUID", obj.getSUID());
    row.set("type", type);
  }

  public void loadNodeProps() {
    final CyTable nodeTable = network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
    final CyTable dataNodeTable = dataNodeConverter.table();
    for (final CyNode node : network.getNodeList()) {
      final Long suid = node.getSUID();
      for (final Map.Entry<String,String> nodeStaticProp : nodeStaticProps.entrySet()) {
        final String staticPropName = nodeStaticProp.getKey();
        final Object value = dataNodeTable.getRow(suid).getRaw(staticPropName);
        if (value == null) continue;
        final String nodeColumn = nodeStaticProp.getValue();
        nodeTable.getRow(suid).set(nodeColumn, value);
      }
    }
  }

  public void loadNodeViewProps() {
    final CyTable dataNodeTable = dataNodeConverter.table();
    for (final View<CyNode> nodeView : networkView.getNodeViews()) {
      final Long suid = nodeView.getModel().getSUID();
      for (final Map.Entry<String,VisualProperty> nodeViewStaticProp : nodeViewStaticProps.entrySet()) {
        final String staticPropName = nodeViewStaticProp.getKey();
        final Object value = dataNodeTable.getRow(suid).getRaw(staticPropName);
        if (value == null) continue;
        final VisualProperty vizProp = nodeViewStaticProp.getValue();
        nodeView.setVisualProperty(vizProp, value);
      }
    }
  }

  private static Map<StaticPropertyType,Class<?>> staticPropTypeClasses = new EnumMap<StaticPropertyType,Class<?>>(StaticPropertyType.class);
  static {
    staticPropTypeClasses.put(StaticPropertyType.STRING, String.class);
    staticPropTypeClasses.put(StaticPropertyType.DOUBLE, Double.class);
  }

  private static Set<StaticProperty> staticProps = EnumSet.of(
    StaticProperty.GRAPHID,
    StaticProperty.TEXTLABEL,
    StaticProperty.CENTERX,
    StaticProperty.CENTERY,
    StaticProperty.WIDTH,
    StaticProperty.HEIGHT);

  private static Map<String,String> nodeStaticProps = ezMap(
    StaticProperty.TEXTLABEL.tag(), CyNetwork.NAME);

  private static Map<String,VisualProperty> nodeViewStaticProps = ezMap(String.class, VisualProperty.class,
    StaticProperty.CENTERX.tag(), BasicVisualLexicon.NODE_X_LOCATION,
    StaticProperty.CENTERY.tag(), BasicVisualLexicon.NODE_Y_LOCATION,
    StaticProperty.WIDTH.tag(),   BasicVisualLexicon.NODE_WIDTH,
    StaticProperty.HEIGHT.tag(),  BasicVisualLexicon.NODE_HEIGHT
    );

  private static void createStaticPropColumns(final CyTable table) {
    for (final StaticProperty staticProp : staticProps) {
      final Class<?> type = staticPropTypeClasses.get(staticProp.type());
      table.createColumn(staticProp.tag(), type, true);
    }
  }

  private static void addStaticProp(final PathwayElement elem, final CyTable table, final CyIdentifiable row) {
    for (final StaticProperty staticProp : elem.getStaticPropertyKeys()) {
      if (!staticProps.contains(staticProp)) continue;
      final Class<?> type = staticPropTypeClasses.get(staticProp.type());
      final Object value = elem.getStaticProperty(staticProp);
      if (value == null) continue;

      final Long key = row.getSUID();
      final String column = staticProp.tag();
      table.getRow(key).set(column, type.cast(value));
    }
  }

	private void convertDataNode(final PathwayElement elem) {
    dataNodeConverter.convert(elem);
	}

  class DataNodeConverter {
    final CyTable table;

    public DataNodeConverter() {
      table = CyActivator.tableFactory.createTable("PathwayElement DATANODES", "CyNode SUID", Long.class, true, false);
      createStaticPropColumns(table);
    }

    public void convert(final PathwayElement elem) {
      final CyNode node = network.addNode();
      addStaticProp(elem, table, node);
      addGraphId(elem, node, "node");
    }

    public CyTable table() {
      return table;
    }
  }

  private static void dump(final CyTable table) {
    final java.util.Collection<CyColumn> columns = table.getColumns();
    for (final CyColumn column : columns) {
      System.out.print(String.format("%s (%s)\t", column.getName(), column.getType().getSimpleName()));
    }
    System.out.println();
    for (final CyRow row : table.getAllRows()) {
      for (final CyColumn column : columns) {
        System.out.print(String.format("%s\t", row.getRaw(column.getName())));
      }
      System.out.println();
    }
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