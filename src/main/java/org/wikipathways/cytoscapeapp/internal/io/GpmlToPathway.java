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
import org.cytoscape.model.CyColumn;
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
  // NOMENCLATURE:
  // In order to help distinguish PathVisio data structures from
  // Cytoscape ones, I've prefixed all variables with either
  // "cy" or "pv".

  /**
   * Maps a GPML pathway element to its representative CyNode in the network.
   */
  final Map<GraphLink.GraphIdContainer,CyNode> pvToCyNodes = new HashMap<GraphLink.GraphIdContainer,CyNode>();

  final List<DelayedVizProp> cyDelayedVizProps = new ArrayList<DelayedVizProp>();

  final CyEventHelper     cyEventHelper;
  final Annots            cyAnnots;
	final Pathway           pvPathway;
  final CyNetworkView     cyNetView;
	final CyNetwork         cyNet;
  final CyTable           cyNodeTbl;
  final CyTable           cyEdgeTbl;

  /**
   * Create a converter from the given pathway and store it in the given network.
   * Constructing this object will not start the conversion and will not modify
   * the given network in any way.
   *
   * @param eventHelper The {@code CyEventHelper} service -- used to flush network object creation events
   * @param annots A wrapper around the Cytoscape Annotations API
   * @param gpmlPathway The GPML pathway object from which to convert
   * @param cyNetView The Cytoscape network to contain the converted GPML pathway
   */
	public GpmlToPathway(
      final CyEventHelper     cyEventHelper,
      final Annots            cyAnnots,
      final Pathway           pvPathway,
      final CyNetworkView     cyNetView) {
    this.cyEventHelper = cyEventHelper;
    this.cyAnnots = cyAnnots;
		this.pvPathway = pvPathway;
    this.cyNetView = cyNetView;
		this.cyNet = cyNetView.getModel();
    this.cyNodeTbl = cyNet.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
    this.cyEdgeTbl = cyNet.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
	}

  /**
   * Convert the pathway given in the constructor.
   */
	public void convert() {
    setupCyTables();

    // convert by each pathway element type
    convertDataNodes();
    convertShapes();
    convertStates();
    convertGroups();
    convertLabels();
    convertAnchors();
    convertLines();

    cyEventHelper.flushPayloadEvents(); // guarantee that all node and edge views have been created
    DelayedVizProp.applyAll(cyNetView, cyDelayedVizProps); // apply our visual style

    // clear our data structures just to be nice to the GC
    pvToCyNodes.clear();
    cyDelayedVizProps.clear();
	}

  /**
   * Ensure that the network's tables have the right columns.
   */
  private void setupCyTables() {
    for (final TableStore tableStore : Arrays.asList(
        BasicTableStore.GRAPH_ID,
        DATA_SOURCE_STORE,
        BasicVizTableStore.NODE_WIDTH,
        BasicVizTableStore.NODE_HEIGHT)) {
      tableStore.setup(cyNodeTbl);
    }

    for (final TableStore tableStore : Arrays.asList(
        BasicVizTableStore.EDGE_COLOR,
        BasicVizTableStore.EDGE_LINE_STYLE,
        BasicVizTableStore.EDGE_LINE_THICKNESS,
        BasicVizTableStore.EDGE_START_ARROW,
        BasicVizTableStore.EDGE_END_ARROW)) {
      tableStore.setup(cyEdgeTbl);
    }
  }

  /*
   ========================================================
     Static property conversion
   ========================================================
  */

  /**
   * Converts a PathVisio static property value (or values) to a value
   * that Cytoscape can use. The Cytoscape value can then be stored
   * in a table or as a visual property.
   *
   * A converter isn't aware
   * of the underlying pathway element nor of the static properties
   * it is converting. It is only aware of static property values.
   * Thus a single converter can be used 
   * for several static properties. For example,
   * {@code PV_COLOR_CONVERTER} can be used for
   * {@code StaticProperty.COLOR} and 
   * {@code StaticProperty.FILLCOLOR}.
   */
  static interface Converter {
    Object toCyValue(Object[] pvValues);
  }

  /**
   * Passes the PathVisio static property value to Cytoscape
   * without any conversion.
   */
  static final Converter NO_CONVERT = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      return pvValues[0];
    }
  };

  static final Converter PV_ARROW_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      return ((org.pathvisio.core.model.LineType) pvValues[0]).getName();
    }
  };

  static final Converter PV_LINE_STYLE_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final int lineStyle = (Integer) pvValues[0];
      switch (lineStyle) {
      case LineStyle.DOUBLE:
        return "double";
      case LineStyle.DASHED:
        return "dashed";
      default:
        return "solid";
      }
    }
  };

  static final Converter PV_SHAPE_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final ShapeType pvShapeType = (ShapeType) pvValues[0];
      return pvShapeType.getName();
    }
  };

  static final Converter PV_FONT_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final String fontFace = (String) pvValues[0];
      final Boolean bold = (Boolean) pvValues[1];
      final Boolean italic = (Boolean) pvValues[2];
      int style = Font.PLAIN;
      if (bold)
        style |= Font.BOLD;
      if (italic)
        style |= Font.ITALIC;
      return (new Font(fontFace, style, 12)).getFontName();
    }
  };

  static final Converter PV_LINE_THICKNESS_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final ShapeType pvShapeType = (ShapeType) pvValues[0];
      final Double pvLineThickness = (Double) pvValues[1];
      if (ShapeType.NONE.equals(pvShapeType)) {
        return 0.0;
      } else {
        return pvLineThickness;
      }
    }
  };

  static final Converter PV_COLOR_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final int rgb = ((Color) pvValues[0]).getRGB();
      return String.format("#%06x", rgb);
    }
  };

  /**
   * Understands how to extract static property values from a PathVisio
   * pathway element and return a Cytoscape value.
   */
  static interface Extracter {
    Object extract(PathwayElement pvElem);
  }

  static class BasicExtracter implements Extracter {
    public static final Extracter GRAPH_ID = new BasicExtracter(StaticProperty.GRAPHID);
    public static final Extracter TEXT_LABEL = new BasicExtracter(StaticProperty.TEXTLABEL);
    public static final Extracter X = new BasicExtracter(StaticProperty.CENTERX);
    public static final Extracter Y = new BasicExtracter(StaticProperty.CENTERY);
    public static final Extracter WIDTH = new BasicExtracter(StaticProperty.WIDTH);
    public static final Extracter HEIGHT = new BasicExtracter(StaticProperty.HEIGHT);
    public static final Extracter COLOR = new BasicExtracter(PV_COLOR_CONVERTER, StaticProperty.COLOR);
    public static final Extracter FILL_COLOR = new BasicExtracter(PV_COLOR_CONVERTER, StaticProperty.FILLCOLOR);
    public static final Extracter FONT_SIZE = new BasicExtracter(StaticProperty.FONTSIZE);
    public static final Extracter TRANSPARENT = new BasicExtracter(StaticProperty.TRANSPARENT);
    public static final Extracter NODE_LINE_THICKNESS = new BasicExtracter(PV_LINE_THICKNESS_CONVERTER, StaticProperty.SHAPETYPE, StaticProperty.LINETHICKNESS);
    public static final Extracter EDGE_LINE_THICKNESS = new BasicExtracter(StaticProperty.LINETHICKNESS);
    public static final Extracter SHAPE = new BasicExtracter(PV_SHAPE_CONVERTER, StaticProperty.SHAPETYPE);
    public static final Extracter LINE_STYLE = new BasicExtracter(PV_LINE_STYLE_CONVERTER, StaticProperty.LINESTYLE);
    public static final Extracter START_ARROW_STYLE = new BasicExtracter(PV_ARROW_CONVERTER, StaticProperty.STARTLINETYPE);
    public static final Extracter END_ARROW_STYLE = new BasicExtracter(PV_ARROW_CONVERTER, StaticProperty.ENDLINETYPE);

    final Converter converter;
    final StaticProperty[] pvProps;
    final Object[] pvValues;

    BasicExtracter(StaticProperty ... pvProps) {
      this(NO_CONVERT, pvProps);
    }

    BasicExtracter(final Converter converter, StaticProperty ... pvProps) {
      this.converter = converter;
      this.pvProps = pvProps;
      this.pvValues = new Object[pvProps.length];
    }

    public Object extract(final PathwayElement pvElem) {
      for (int i = 0; i < pvValues.length; i++) {
        pvValues[i] = pvElem.getStaticProperty(pvProps[i]);
      }
      return converter.toCyValue(pvValues);
    }
  }


  /**
   * Describes how PathVisio PathwayElement's static property values
   * are stored in a Cytoscape table.
   */
  public static interface TableStore {
    void setup(final CyTable cyTable);
    void store(final CyTable cyTable, final CyIdentifiable cyNetObj, final PathwayElement pvElem);
  }

  static class BasicTableStore implements TableStore {
    public static final TableStore GRAPH_ID = new BasicTableStore("GraphID", BasicExtracter.GRAPH_ID);
    public static final TableStore TEXT_LABEL = new BasicTableStore(CyNetwork.NAME, BasicExtracter.TEXT_LABEL);

    final String cyColName;
    final Class<?> cyColType;
    final Extracter extracter;

    BasicTableStore(final String cyColName, final Extracter extracter) {
      this(cyColName, String.class, extracter);
    }

    BasicTableStore(final String cyColName, final Class<?> cyColType, final Extracter extracter) {
      this.cyColName = cyColName;
      this.cyColType = cyColType;
      this.extracter = extracter;
    }

    public void setup(final CyTable cyTable) {
      final CyColumn cyCol = cyTable.getColumn(cyColName);
      if (cyCol == null) {
        cyTable.createColumn(cyColName, cyColType, false);
      } else {
        if (!cyCol.getType().equals(cyColType)) {
          System.out.println(String.format("Wrong column type. Column %s is type %s but expected %s", cyColName, cyCol.getType().toString(), cyColType.toString()));
        }
      }
    }

    public void store(final CyTable cyTable, final CyIdentifiable cyNetObj, final PathwayElement pvElem) {
      final Object cyValue = extracter.extract(pvElem);
      cyTable.getRow(cyNetObj.getSUID()).set(cyColName, cyValue);
    }
  }

  public static interface VizTableStore extends TableStore {
    String getCyColumnName();
    Class<?> getCyColumnType();
    VisualProperty<?> getCyVizProp();
    Map<?,?> getMapping();
  }

  static Map<String,ArrowShape> PV_ARROW_TYPES = new HashMap<String,ArrowShape>();
  static {
    PV_ARROW_TYPES.put("Arrow",              ArrowShapeVisualProperty.DELTA);
    PV_ARROW_TYPES.put("TBar",               ArrowShapeVisualProperty.T);
    PV_ARROW_TYPES.put("mim-binding",        ArrowShapeVisualProperty.ARROW);
    PV_ARROW_TYPES.put("mim-conversion",     ArrowShapeVisualProperty.ARROW);
    PV_ARROW_TYPES.put("mim-modification",   ArrowShapeVisualProperty.ARROW);
    PV_ARROW_TYPES.put("mim-catalysis",      ArrowShapeVisualProperty.CIRCLE);
    PV_ARROW_TYPES.put("mim-inhibition",     ArrowShapeVisualProperty.T);
    PV_ARROW_TYPES.put("mim-covalent-bond",  ArrowShapeVisualProperty.T);
  }

  static class BasicVizTableStore extends BasicTableStore implements VizTableStore {
    public static final VizTableStore NODE_WIDTH = new BasicVizTableStore("Width", Double.class, BasicExtracter.WIDTH, BasicVisualLexicon.NODE_WIDTH);
    public static final VizTableStore NODE_HEIGHT = new BasicVizTableStore("Height", Double.class, BasicExtracter.HEIGHT, BasicVisualLexicon.NODE_HEIGHT);
    public static final VizTableStore NODE_COLOR = new BasicVizTableStore("Color", BasicExtracter.FILL_COLOR, BasicVisualLexicon.NODE_PAINT);
    
    public static final VizTableStore EDGE_COLOR = new BasicVizTableStore("Color", BasicExtracter.COLOR, BasicVisualLexicon.EDGE_PAINT);
    public static final VizTableStore EDGE_LINE_STYLE = new BasicVizTableStore("LineStyle", BasicExtracter.LINE_STYLE, BasicVisualLexicon.EDGE_LINE_TYPE);
    public static final VizTableStore EDGE_LINE_THICKNESS = new BasicVizTableStore("LineThickness", Double.class, BasicExtracter.EDGE_LINE_THICKNESS, BasicVisualLexicon.EDGE_WIDTH);
    public static final VizTableStore EDGE_START_ARROW = new BasicVizTableStore("StartArrow", BasicExtracter.START_ARROW_STYLE, BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE, PV_ARROW_TYPES);
    public static final VizTableStore EDGE_END_ARROW = new BasicVizTableStore("EndArrow", BasicExtracter.END_ARROW_STYLE, BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, PV_ARROW_TYPES);

    final VisualProperty<?> vizProp;
    final Map<?,?> mapping;

    BasicVizTableStore(final String cyColName, final Extracter extracter, final VisualProperty<?> vizProp) {
      this(cyColName, String.class, extracter, vizProp);
    }

    BasicVizTableStore(final String cyColName, final Class<?> cyColType, final Extracter extracter, final VisualProperty<?> vizProp) {
      this(cyColName, cyColType, extracter, vizProp, null);
    }

    BasicVizTableStore(final String cyColName, final Extracter extracter, final VisualProperty<?> vizProp, final Map<?,?> mapping) {
      this(cyColName, String.class, extracter, vizProp, mapping);
    }

    BasicVizTableStore(final String cyColName, final Class<?> cyColType, final Extracter extracter, final VisualProperty<?> vizProp, final Map<?,?> mapping) {
      super(cyColName, cyColType, extracter);
      this.vizProp = vizProp;
      this.mapping = mapping;
    }

    public String getCyColumnName() {
      return super.cyColName;
    }

    public Class<?> getCyColumnType() {
      return super.cyColType;
    }

    public VisualProperty<?> getCyVizProp() {
      return vizProp;
    }

    public Map<?,?> getMapping() {
      return mapping;
    }
  }

  public static List<VizTableStore> getAllVizTableStores() {
    return Arrays.asList(
      BasicVizTableStore.NODE_WIDTH,
      BasicVizTableStore.NODE_HEIGHT,
      BasicVizTableStore.EDGE_COLOR,
      BasicVizTableStore.EDGE_LINE_STYLE,
      BasicVizTableStore.EDGE_LINE_THICKNESS,
      BasicVizTableStore.EDGE_START_ARROW,
      BasicVizTableStore.EDGE_END_ARROW
      );
  }

  void store(final CyTable cyTable, final CyIdentifiable cyNetObj, final PathwayElement pvElem, final TableStore ... tableStores) {
    for (final TableStore tableStore : tableStores) {
      tableStore.store(cyTable, cyNetObj, pvElem);
    }
  }

  /**
   * Takes a PathVisio PathwayElement's static property values and stores
   * the equivalent Cytoscape visual property value in a {@code DelayedVizProp}.
   */
  static interface VizPropStore {
    DelayedVizProp store(final CyIdentifiable cyNetObj, final PathwayElement pvElem);
  }

  static class BasicVizPropStore implements VizPropStore {
    public static final VizPropStore NODE_X = new BasicVizPropStore(BasicVisualLexicon.NODE_X_LOCATION, BasicExtracter.X);
    public static final VizPropStore NODE_Y = new BasicVizPropStore(BasicVisualLexicon.NODE_Y_LOCATION, BasicExtracter.Y);

    final VisualProperty<?> cyVizProp;
    final Extracter extracter;

    BasicVizPropStore(final VisualProperty<?> cyVizProp, final Extracter extracter) {
      this.cyVizProp = cyVizProp;
      this.extracter = extracter;
    }

    public DelayedVizProp store(final CyIdentifiable cyNetObj, final PathwayElement pvElem) {
      final Object cyValue = extracter.extract(pvElem);
      return new DelayedVizProp(cyNetObj, cyVizProp, cyValue, false);
    }
  }

  void store(final CyIdentifiable cyNetObj, final PathwayElement pvElem, final VizPropStore ... vizPropStores) {
    for (final VizPropStore vizPropStore : vizPropStores) {
      cyDelayedVizProps.add(vizPropStore.store(cyNetObj, pvElem));
    }
  }

  /*
   ========================================================
     Data nodes
   ========================================================
  */

  static final Extracter DATA_SOURCE_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvElem) {
      if(pvElem.getDataSource() == null)
        return null;
      else
        return pvElem.getDataSource().getFullName();
    }
  };

  static final TableStore DATA_SOURCE_STORE = new BasicTableStore("Datasource", DATA_SOURCE_EXTRACTER);

  private void convertDataNodes() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.DATANODE))
        continue;
      convertDataNode(pvElem);
    }
  }

  private void convertDataNode(final PathwayElement pvDataNode) {
    final CyNode cyNode = cyNet.addNode();
    pvToCyNodes.put(pvDataNode, cyNode);
    store(cyNodeTbl, cyNode, pvDataNode,
      BasicTableStore.GRAPH_ID,
      DATA_SOURCE_STORE,
      BasicTableStore.TEXT_LABEL,
      BasicVizTableStore.NODE_WIDTH,
      BasicVizTableStore.NODE_HEIGHT);
    store(cyNode, pvDataNode,
      BasicVizPropStore.NODE_X,
      BasicVizPropStore.NODE_Y);
    
    /*
    if(dataNode.getDataSource() != null && dataNode.getDataSource().getFullName() != null) {
    	nodeTbl.getRow(node.getSUID()).set("Datasource", dataNode.getDataSource().getFullName());
    }
    convertStaticProps(dataNode, dataNodeStaticProps, nodeTbl, node.getSUID());
    convertViewStaticProps(dataNode, dataNodeViewStaticProps, node);
    convertShapeTypeNone(node, dataNode);
    nodes.put(dataNode, node);
    */
  }

  /*
   ========================================================
     Shapes
   ========================================================
  */

  private void convertShapes() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.SHAPE))
        continue;
      convertDataNode(pvElem); // shapes are treated just like data nodes, but this will change in the future with annotations
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

  final Extracter STATE_X_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvState) {
      final PathwayElement pvParent = (PathwayElement) pvPathway.getGraphIdContainer(pvState.getGraphRef());
      return pvParent.getMCenterX() + pvState.getRelX() * pvParent.getMWidth() / 2.0;
    }
  };
  
  final Extracter STATE_Y_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvState) {
      final PathwayElement pvParent = (PathwayElement) pvPathway.getGraphIdContainer(pvState.getGraphRef());
      return pvParent.getMCenterY() + pvState.getRelY() * pvParent.getMWidth() / 2.0;
    }
  };

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
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.STATE))
        continue;
      convertState(pvElem);
    }
  }

  private void convertState(final PathwayElement pvState) {
    /*
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
    */
  }
  
  /*
   ========================================================
     Groups
   ========================================================
  */
 
  private void convertGroups() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.GROUP))
        continue;
      convertGroup(pvElem);
    }
  }

  private void convertGroup(final PathwayElement pvGroup) {
    /*
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
    */
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
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.LABEL))
        continue;
      convertLabel(pvElem);
    }
  }

  private void convertLabel(final PathwayElement pvLabel) {
    // TODO: refactor this as an annotation
	// comment Tina: not sure if they can all be replaced by annotations because they are often connected with data nodes
    /*
    final CyNode node = network.addNode();
    convertStaticProps(label, labelStaticProps, nodeTbl, node.getSUID());
    convertViewStaticProps(label, labelViewStaticProps, node);
    delayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_TRANSPARENCY, 0, true)); // labels are always transparent
    convertShapeTypeNone(node, label);
    nodes.put(label, node);
    */
  }
  
  /*
   ========================================================
     Anchors
   ========================================================
  */

  private void convertAnchors() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.LINE))
        continue;
      if (pvElem.getMAnchors().size() == 0)
        continue;
      convertAnchorsInLine(pvElem);
    }
  }

  private void assignAnchorVizStyle(final CyNode node, final Point2D position) {
    assignAnchorVizStyle(node, position, Color.WHITE);
  }

  private void assignAnchorVizStyle(final CyNode node, final Point2D position, final Color color) {
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_X_LOCATION, position.getX(), false));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_Y_LOCATION, position.getY(), false));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_FILL_COLOR, color, true));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0, true));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_WIDTH, 5.0, true));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_HEIGHT, 5.0, true));
  }

  private void convertAnchorsInLine(final PathwayElement pvElem) {
    final MLine pvLine = (MLine) pvElem;
    for (final MAnchor pvAnchor : pvElem.getMAnchors()) {
      final CyNode cyNode = cyNet.addNode();
      final Point2D position = pvLine.getConnectorShape().fromLineCoordinate(pvAnchor.getPosition());
      pvToCyNodes.put(pvAnchor, cyNode);
      assignAnchorVizStyle(cyNode, position, pvLine.getColor());
    }
  }
  
  /*
   ========================================================
     Lines
   ========================================================
  */

  private void convertLines() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!(pvElem.getObjectType().equals(ObjectType.LINE) || pvElem.getObjectType().equals(ObjectType.GRAPHLINE)))
        continue;
      convertLine(pvElem);
    }
  }

  private void convertLine(final PathwayElement pvElem) {
    final MLine pvLine = (MLine) pvElem;
    final String pvStartRef = pvLine.getMStart().getGraphRef();
    final String pvEndRef = pvLine.getMEnd().getGraphRef();

    CyNode cyStartNode = pvToCyNodes.get(pvPathway.getGraphIdContainer(pvStartRef));
    if (cyStartNode == null) {
      cyStartNode = cyNet.addNode();
      assignAnchorVizStyle(cyStartNode, pvLine.getStartPoint());
    }
    CyNode cyEndNode = pvToCyNodes.get(pvPathway.getGraphIdContainer(pvEndRef));
    if (cyEndNode == null) {
      cyEndNode = cyNet.addNode();
      assignAnchorVizStyle(cyEndNode, pvLine.getEndPoint());
    }

    final MAnchor[] pvAnchors = pvElem.getMAnchors().toArray(new MAnchor[0]);
    if (pvAnchors.length > 0) {
      newEdge(pvLine, cyStartNode, pvToCyNodes.get(pvAnchors[0]), true, false);
      for (int i = 1; i < pvAnchors.length; i++) {
        newEdge(pvLine, pvToCyNodes.get(pvAnchors[i - 1]), pvToCyNodes.get(pvAnchors[i]), false, false);
      }
      newEdge(pvLine, pvToCyNodes.get(pvAnchors[pvAnchors.length - 1]), cyEndNode, false, true);
    } else {
      newEdge(pvLine, cyStartNode, cyEndNode, true, true);
    }
  }

  private void newEdge(final PathwayElement pvLine, final CyNode cySourceNode, final CyNode cyTargetNode, final boolean isStart, final boolean isEnd) {
    final CyEdge cyEdge = cyNet.addEdge(cySourceNode, cyTargetNode, true);
    store(cyEdgeTbl, cyEdge, pvLine, 
      BasicVizTableStore.EDGE_COLOR,
      BasicVizTableStore.EDGE_LINE_STYLE,
      BasicVizTableStore.EDGE_LINE_THICKNESS
      );
    if (isStart) {
      store(cyEdgeTbl, cyEdge, pvLine, BasicVizTableStore.EDGE_START_ARROW);
    }
    if (isEnd) {
      store(cyEdgeTbl, cyEdge, pvLine, BasicVizTableStore.EDGE_END_ARROW);
    }
  }
}
