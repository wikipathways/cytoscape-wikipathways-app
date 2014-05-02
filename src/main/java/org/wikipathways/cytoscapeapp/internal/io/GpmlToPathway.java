package org.wikipathways.cytoscapeapp.internal.io;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;

import java.util.Map;
import java.util.HashMap;
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
import org.pathvisio.core.model.GroupStyle;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;

import org.cytoscape.event.CyEventHelper;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.LineType;

/**
 * Converts a GPML file contained in a PathVisio Pathway object to a
 * Cytoscape network view, and it tries to reproduce
 * the pathway's visual representation.
 */
public class GpmlToPathway {
  /*
    NOMENCLATURE:
    In order to help distinguish PathVisio data structures from
    Cytoscape ones, I've prefixed all variables with either
    "cy" or "pv".
   */

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
   * All {@code TableStore}s that are used in this class must be listed here.
   */
  private void setupCyTables() {
    for (final TableStore tableStore : Arrays.asList(
        BasicTableStore.GRAPH_ID,
        XREF_ID_STORE,
        XREF_DATA_SOURCE_STORE,
        IS_GPML_SHAPE,
        BasicVizTableStore.NODE_WIDTH,
        BasicVizTableStore.NODE_HEIGHT,
        BasicVizTableStore.NODE_FILL_COLOR,
        BasicVizTableStore.NODE_COLOR,
        BasicVizTableStore.NODE_LABEL_FONT,
        BasicVizTableStore.NODE_LABEL_SIZE,
        BasicVizTableStore.NODE_TRANSPARENT,
        BasicVizTableStore.NODE_BORDER_THICKNESS,
        BasicVizTableStore.NODE_BORDER_STYLE,
        BasicVizTableStore.NODE_SHAPE
        )) {
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

  /**
   * Converts a PathVisio static property value (or values) to a value
   * that Cytoscape can use. The Cytoscape value can then be stored
   * in a table or as a visual property.
   *
   * A converter isn't aware of the underlying pathway element nor of
   * the static properties it is converting. It is only aware of static
   * property <em>values</em>. This allows a single converter to be used
   * for several static properties. For example, {@code PV_COLOR_CONVERTER}
   * can be used for {@code StaticProperty.COLOR} and {@code StaticProperty.FILLCOLOR}.
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

  /**
   * Uses PathVisio's {@code StaticProperty.LINESTYLE} to the name of
   * a Cytoscape Border Line Type.
   */
  static final Converter PV_LINE_STYLE_NAME_CONVERTER = new Converter() {
    final String[] PV_LINE_STYLE_NAMES = LineStyle.getNames();
    public Object toCyValue(Object[] pvValues) {
      final int lineStyle = (Integer) pvValues[0];
      return PV_LINE_STYLE_NAMES[lineStyle];
    }
  };

  /**
   * Uses PathVisio's {@code StaticProperty.SHAPETYPE} and returns its name.
   */
  static final Converter PV_SHAPE_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final ShapeType pvShapeType = (ShapeType) pvValues[0];
      return pvShapeType.getName();
    }
  };

  /**
   * Uses PathVisio's {@code StaticProperty.FONTNAME}, {@code StaticProperty.FONTWEIGHT},
   * and {@code StaticProperty.FONTSTYLE} to return the font. 
   */
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
      return new Font(fontFace, style, 12);
    }
  };

  /**
   * Uses PathVisio's {@code StaticProperty.FONTNAME}, {@code StaticProperty.FONTWEIGHT},
   * and {@code StaticProperty.FONTSTYLE} to return the font name. 
   */
  static final Converter PV_FONT_NAME_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      return ((Font) PV_FONT_CONVERTER.toCyValue(pvValues)).getFontName();
    }
  };

  /**
   * Uses PathVisio's {@code StaticProperty.SHAPETYPE} and 
   * {@code StaticProperty.LINETHICKNESS}. Cytoscape has no equivalent
   * of PathVisio's ShapeType.NONE. ShapeType.NONE is reproduced by
   * setting the line thickness to 0.
   */
  static final Converter PV_LINE_THICKNESS_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final ShapeType pvShapeType = (ShapeType) pvValues[0];
      final Double pvLineThickness = (Double) pvValues[1];
      if (ShapeType.NONE.equals(pvShapeType)) {
        return 0.0; // TODO: change this to 0.0 when VizMapper bug #2542 is fixed 
      } else {
        return pvLineThickness;
      }
    }
  };

  /**
   * Uses PathVisio's {@code StaticProperty.COLOR} or {@code StaticProperty.FILLCOLOR}
   * and returns a hex string of the color.
   */
  static final Converter PV_COLOR_STRING_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final Color c = (Color) pvValues[0];
      final int r = c.getRed();
      final int g = c.getGreen();
      final int b = c.getBlue();
      return String.format("#%02x%02x%02x", r, g, b);
    }
  };

  /**
   * Uses PathVisio's {@code StaticProperty.TRANSPARENT} to a String.
   */
  static final Converter PV_TRANSPARENT_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final ShapeType pvShapeType = (ShapeType) pvValues[0];
      final Boolean pvTransparent = (Boolean) pvValues[1];
      if (ShapeType.NONE.equals(pvShapeType)) {
        return "true";
      } else {
        return pvTransparent.toString();
      }
    }
  };

  /**
   * Extracts values from a PathVisio pathway element
   * and returns a Cytoscape value.
   *
   * Most of the time, an {@code Extracter} pulls static property values
   * from a pathway element. It rarely doesn't use static property values.
   * Some non-static property values include data source values and X, Y
   * coordinates for PathVisio State elements.
   *
   * If an {@code Extracter} uses a static property value, it 
   * calls a {@code Converter} to convert the static property value
   * to a value Cytoscape can use.
   */
  static interface Extracter {
    Object extract(PathwayElement pvElem);
  }

  /**
   * Extracts static property values from a PathVisio element.
   */
  static class BasicExtracter implements Extracter {
    public static final Extracter GRAPH_ID            = new BasicExtracter(StaticProperty.GRAPHID);
    public static final Extracter TEXT_LABEL          = new BasicExtracter(StaticProperty.TEXTLABEL);
    public static final Extracter X                   = new BasicExtracter(StaticProperty.CENTERX);
    public static final Extracter Y                   = new BasicExtracter(StaticProperty.CENTERY);
    public static final Extracter WIDTH               = new BasicExtracter(StaticProperty.WIDTH);
    public static final Extracter HEIGHT              = new BasicExtracter(StaticProperty.HEIGHT);
    public static final Extracter COLOR_STRING        = new BasicExtracter(PV_COLOR_STRING_CONVERTER, StaticProperty.COLOR);
    public static final Extracter FILL_COLOR_STRING   = new BasicExtracter(PV_COLOR_STRING_CONVERTER, StaticProperty.FILLCOLOR);
    public static final Extracter COLOR               = new BasicExtracter(StaticProperty.COLOR);
    public static final Extracter FILL_COLOR          = new BasicExtracter(StaticProperty.FILLCOLOR);
    public static final Extracter FONT_SIZE           = new BasicExtracter(StaticProperty.FONTSIZE);
    public static final Extracter FONT                = new BasicExtracter(PV_FONT_CONVERTER, StaticProperty.FONTNAME, StaticProperty.FONTWEIGHT, StaticProperty.FONTSTYLE);
    public static final Extracter FONT_NAME           = new BasicExtracter(PV_FONT_NAME_CONVERTER, StaticProperty.FONTNAME, StaticProperty.FONTWEIGHT, StaticProperty.FONTSTYLE);
    public static final Extracter TRANSPARENT         = new BasicExtracter(PV_TRANSPARENT_CONVERTER, StaticProperty.SHAPETYPE, StaticProperty.TRANSPARENT);
    public static final Extracter NODE_LINE_THICKNESS = new BasicExtracter(PV_LINE_THICKNESS_CONVERTER, StaticProperty.SHAPETYPE, StaticProperty.LINETHICKNESS);
    public static final Extracter EDGE_LINE_THICKNESS = new BasicExtracter(StaticProperty.LINETHICKNESS);
    public static final Extracter SHAPE               = new BasicExtracter(PV_SHAPE_CONVERTER, StaticProperty.SHAPETYPE);
    public static final Extracter LINE_STYLE_NAME     = new BasicExtracter(PV_LINE_STYLE_NAME_CONVERTER, StaticProperty.LINESTYLE);
    public static final Extracter START_ARROW_STYLE   = new BasicExtracter(PV_ARROW_CONVERTER, StaticProperty.STARTLINETYPE);
    public static final Extracter END_ARROW_STYLE     = new BasicExtracter(PV_ARROW_CONVERTER, StaticProperty.ENDLINETYPE);

    final Converter converter;
    final StaticProperty[] pvProps;
    final Object[] pvValues;

    /**
     * Create an extracter that extracts the value of the first
     * provided static property; it performs no conversion from
     * the PathVisio value to Cytoscape.
     */
    BasicExtracter(StaticProperty ... pvProps) {
      this(NO_CONVERT, pvProps);
    }

    /**
     * Create an extracter that extracts the values of the given static
     * properties, but use the given {@code converter} before passing
     * the value on to Cytoscape.
     */
    BasicExtracter(final Converter converter, StaticProperty ... pvProps) {
      this.converter = converter;
      this.pvProps = pvProps;
      this.pvValues = new Object[pvProps.length];
    }

    public Object extract(final PathwayElement pvElem) {
      //System.out.println("Extracting...");
      for (int i = 0; i < pvValues.length; i++) {
        //System.out.println(pvProps[i]);
        pvValues[i] = pvElem.getStaticProperty(pvProps[i]);
      }
      if (pvValues.length == 1 && pvValues[0] == null)
        return null;
      return converter.toCyValue(pvValues);
    }
  }

  /**
   * An extracter that always returns the same Cytoscape value
   * regardless of the PathVisio element. This is useful
   * for things like group nodes whose visual style is
   * consistent and is not derived from PathVisio static property values.
   */
  static class DefaultExtracter implements Extracter {
    final Object cyValue;
    public DefaultExtracter(final Object cyValue) {
      this.cyValue = cyValue;
    }

    public Object extract(final PathwayElement pvElem) {
      return cyValue;
    }
  }

  /**
   * Stores values produced by an {@code Extractor}
   * into a Cytoscape table column.
   */
  public static interface TableStore {
    /**
     * Set up the {@code cyTable} so that the column is created.
     */
    void setup(final CyTable cyTable);

    /**
     * Pulls a value from a {@code pvElem} and stores it in {@code cyTable} under the row
     * whose key is {@code cyNetObj}.
     */
    void store(final CyTable cyTable, final CyIdentifiable cyNetObj, final PathwayElement pvElem);
  }

  /**
   * A {@code TableStore} that pulls values from PathVisio elements
   * using a given {@code Extracter}.
   */
  static class BasicTableStore implements TableStore {
    public static final TableStore GRAPH_ID = new BasicTableStore("GraphID", BasicExtracter.GRAPH_ID);
    public static final TableStore TEXT_LABEL = new BasicTableStore(CyNetwork.NAME, BasicExtracter.TEXT_LABEL);

    final String cyColName;
    final Class<?> cyColType;
    final Extracter extracter;

    /**
     * Create a {@code TableStore} for the given column of type String
     * using the given {@code extracter}
     */
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
          // This should never happen, because the network we are given should be new
          // without any conflicting columns.
          throw new IllegalStateException(String.format("Wrong column type. Column %s is type %s but expected %s", cyColName, cyCol.getType().toString(), cyColType.toString()));
        }
      }
    }

    public void store(final CyTable cyTable, final CyIdentifiable cyNetObj, final PathwayElement pvElem) {
      final Object cyValue = extracter.extract(pvElem);
      cyTable.getRow(cyNetObj.getSUID()).set(cyColName, cyValue);
    }
  }

  /**
   * A specific kind of {@code TableStore} whose column stores
   * visual property values. The WikiPathways visual style,
   * specified in {@code GpmlVizStyle}, looks at known {@code VizTableStore}s
   * (as returned by {@code getAllVizPropStores()})
   * to create a series of discrete and passthrough mappings for all 
   * {@code VizTableStore} columns.
   */
  public static interface VizTableStore extends TableStore {
    /**
     * Return the name of the column that contains the visual property value.
     */
    String getCyColumnName();

    /**
     * Return the type of the column that contains the visual property value.
     */
    Class<?> getCyColumnType();

    /**
     * Return the Cytoscape visual properties that should read from the column
     * returned by {@getCyColumnName()}.
     */
    VisualProperty<?>[] getCyVizProps();

    /**
     * Return a map containing the key-value pairs for
     * the discrete mapping; return null for a passthrough mapping.
     */
    Map<?,?> getMapping();
  }

  static Map<String,Integer> PV_TRANSPARENT_MAP = new HashMap<String,Integer>();
  static {
    PV_TRANSPARENT_MAP.put("true",  0);
    PV_TRANSPARENT_MAP.put("false", 255);
  }

  static Map<String,ArrowShape> PV_ARROW_MAP = new HashMap<String,ArrowShape>();
  static {
    PV_ARROW_MAP.put("Arrow",              ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("TBar",               ArrowShapeVisualProperty.T);
    PV_ARROW_MAP.put("mim-binding",        ArrowShapeVisualProperty.ARROW);
    PV_ARROW_MAP.put("mim-conversion",     ArrowShapeVisualProperty.ARROW);
    PV_ARROW_MAP.put("mim-modification",   ArrowShapeVisualProperty.ARROW);
    PV_ARROW_MAP.put("mim-catalysis",      ArrowShapeVisualProperty.CIRCLE);
    PV_ARROW_MAP.put("mim-inhibition",     ArrowShapeVisualProperty.T);
    PV_ARROW_MAP.put("mim-covalent-bond",  ArrowShapeVisualProperty.T);
  }

  static Map<String,NodeShape> PV_SHAPE_MAP = new HashMap<String,NodeShape>();
  static {
    PV_SHAPE_MAP.put("Rectangle",        NodeShapeVisualProperty.RECTANGLE);
    PV_SHAPE_MAP.put("Triangle",         NodeShapeVisualProperty.TRIANGLE);
    PV_SHAPE_MAP.put("RoundedRectangle", NodeShapeVisualProperty.ROUND_RECTANGLE);
    PV_SHAPE_MAP.put("Hexagon",          NodeShapeVisualProperty.HEXAGON);
    PV_SHAPE_MAP.put("Oval",             NodeShapeVisualProperty.ELLIPSE);
    PV_SHAPE_MAP.put("Octagon",          NodeShapeVisualProperty.OCTAGON);
  }

  static LineType getCyNodeLineType(final String displayName) {
    for (final LineType lineType : ((DiscreteRange<LineType>) BasicVisualLexicon.NODE_BORDER_LINE_TYPE.getRange()).values()) {
      final String lineTypeName = lineType.getDisplayName();
      if (displayName.equals(lineTypeName)) {
        return lineType;
      }
    }
    return null;
  }

  static Map<String,LineType> PV_LINE_STYLE_MAP = new HashMap<String,LineType>();
  static {
    PV_LINE_STYLE_MAP.put("Solid",  getCyNodeLineType("Solid"));
    PV_LINE_STYLE_MAP.put("Double", getCyNodeLineType("Parallel Lines"));
    PV_LINE_STYLE_MAP.put("Dashed", getCyNodeLineType("Dash"));
    PV_LINE_STYLE_MAP.put("Dots",   getCyNodeLineType("Dots"));
  }

  static class BasicVizTableStore extends BasicTableStore implements VizTableStore {
    public static final VizTableStore NODE_WIDTH            = new BasicVizTableStore("Width", Double.class,           BasicExtracter.WIDTH,                           BasicVisualLexicon.NODE_WIDTH);
    public static final VizTableStore NODE_HEIGHT           = new BasicVizTableStore("Height", Double.class,          BasicExtracter.HEIGHT,                          BasicVisualLexicon.NODE_HEIGHT);
    public static final VizTableStore NODE_FILL_COLOR       = new BasicVizTableStore("FillColor",                     BasicExtracter.FILL_COLOR_STRING,               BasicVisualLexicon.NODE_FILL_COLOR);
    public static final VizTableStore NODE_COLOR            = new BasicVizTableStore("Color",                         BasicExtracter.COLOR_STRING,                    BasicVisualLexicon.NODE_LABEL_COLOR, BasicVisualLexicon.NODE_BORDER_PAINT);
    public static final VizTableStore NODE_BORDER_STYLE     = new BasicVizTableStore("BorderStyle",                   BasicExtracter.LINE_STYLE_NAME, PV_LINE_STYLE_MAP, BasicVisualLexicon.NODE_BORDER_LINE_TYPE);
    public static final VizTableStore NODE_LABEL_FONT       = new BasicVizTableStore("LabelFont",                     BasicExtracter.FONT_NAME,                       BasicVisualLexicon.NODE_LABEL_FONT_FACE);
    public static final VizTableStore NODE_LABEL_SIZE       = new BasicVizTableStore("LabelSize", Double.class,       BasicExtracter.FONT_SIZE,                       BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
    public static final VizTableStore NODE_TRANSPARENT      = new BasicVizTableStore("Transparent",                   BasicExtracter.TRANSPARENT, PV_TRANSPARENT_MAP, BasicVisualLexicon.NODE_TRANSPARENCY);
    public static final VizTableStore NODE_BORDER_THICKNESS = new BasicVizTableStore("BorderThickness", Double.class, BasicExtracter.NODE_LINE_THICKNESS,             BasicVisualLexicon.NODE_BORDER_WIDTH);
    public static final VizTableStore NODE_SHAPE            = new BasicVizTableStore("Shape",                         BasicExtracter.SHAPE, PV_SHAPE_MAP,             BasicVisualLexicon.NODE_SHAPE);
    
    public static final VizTableStore EDGE_COLOR            = new BasicVizTableStore("Color",                         BasicExtracter.COLOR_STRING,                    BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
    public static final VizTableStore EDGE_LINE_STYLE       = new BasicVizTableStore("LineStyle",                     BasicExtracter.LINE_STYLE_NAME, PV_LINE_STYLE_MAP, BasicVisualLexicon.EDGE_LINE_TYPE);
    public static final VizTableStore EDGE_LINE_THICKNESS   = new BasicVizTableStore("LineThickness", Double.class,   BasicExtracter.EDGE_LINE_THICKNESS,             BasicVisualLexicon.EDGE_WIDTH);
    public static final VizTableStore EDGE_START_ARROW      = new BasicVizTableStore("StartArrow",                    BasicExtracter.START_ARROW_STYLE, PV_ARROW_MAP, BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE);
    public static final VizTableStore EDGE_END_ARROW        = new BasicVizTableStore("EndArrow",                      BasicExtracter.END_ARROW_STYLE, PV_ARROW_MAP,   BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);

    final VisualProperty<?>[] vizProps;
    final Map<?,?> mapping; // null means passthru

    BasicVizTableStore(final String cyColName, final Extracter extracter, final VisualProperty<?> ... vizProps) {
      this(cyColName, String.class, extracter, vizProps);
    }

    BasicVizTableStore(final String cyColName, final Class<?> cyColType, final Extracter extracter, final VisualProperty<?> ... vizProps) {
      this(cyColName, cyColType, extracter, null, vizProps);
    }

    BasicVizTableStore(final String cyColName, final Extracter extracter, final Map<?,?> mapping, final VisualProperty<?> ... vizProps) {
      this(cyColName, String.class, extracter, mapping, vizProps);
    }

    BasicVizTableStore(final String cyColName, final Class<?> cyColType, final Extracter extracter, final Map<?,?> mapping, final VisualProperty<?> ... vizProps) {
      super(cyColName, cyColType, extracter);
      this.vizProps = vizProps;
      this.mapping = mapping;
    }

    public String getCyColumnName() {
      return super.cyColName;
    }

    public Class<?> getCyColumnType() {
      return super.cyColType;
    }

    public VisualProperty<?>[] getCyVizProps() {
      return vizProps;
    }

    public Map<?,?> getMapping() {
      return mapping;
    }
  }

  /**
   * Overrides the extractor of an existing {@code VizTableStore}.
   */
  static class OverrideVizTableStore extends BasicVizTableStore {
    /**
     * @param store Use this {@code store}'s Cytoscape column, visual property, and mapping.
     * @param extracter Use this {@code Extracter} instead of the one provided by {@code store}.
     */
    public OverrideVizTableStore(final VizTableStore store, final Extracter extracter) {
      super(store.getCyColumnName(), store.getCyColumnType(), extracter, store.getMapping(), store.getCyVizProps());
    }
  }

  public static List<VizTableStore> getAllVizTableStores() {
    return Arrays.asList(
      BasicVizTableStore.NODE_WIDTH,
      BasicVizTableStore.NODE_HEIGHT,
      BasicVizTableStore.NODE_FILL_COLOR,
      BasicVizTableStore.NODE_LABEL_FONT,
      BasicVizTableStore.NODE_LABEL_SIZE,
      BasicVizTableStore.NODE_TRANSPARENT,
      BasicVizTableStore.NODE_COLOR,
      BasicVizTableStore.NODE_BORDER_STYLE,
      BasicVizTableStore.NODE_BORDER_THICKNESS,
      BasicVizTableStore.NODE_SHAPE,
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
   * Takes a PathVisio element value and stores
   * the equivalent Cytoscape visual bypass value in a {@code DelayedVizProp}.
   */
  static interface VizPropStore {
    DelayedVizProp[] store(final CyIdentifiable cyNetObj, final PathwayElement pvElem);
  }

  static class BasicVizPropStore implements VizPropStore {
    public static final VizPropStore NODE_X                 = new BasicVizPropStore(BasicExtracter.X,                               BasicVisualLexicon.NODE_X_LOCATION);
    public static final VizPropStore NODE_Y                 = new BasicVizPropStore(BasicExtracter.Y,                               BasicVisualLexicon.NODE_Y_LOCATION);
    public static final VizPropStore NODE_WIDTH             = new BasicVizPropStore(BasicExtracter.WIDTH,                           BasicVisualLexicon.NODE_WIDTH);
    public static final VizPropStore NODE_HEIGHT            = new BasicVizPropStore(BasicExtracter.HEIGHT,                          BasicVisualLexicon.NODE_HEIGHT);
    public static final VizPropStore NODE_FILL_COLOR        = new BasicVizPropStore(BasicExtracter.FILL_COLOR,                      BasicVisualLexicon.NODE_FILL_COLOR);
    public static final VizPropStore NODE_COLOR             = new BasicVizPropStore(BasicExtracter.COLOR,                           BasicVisualLexicon.NODE_LABEL_COLOR, BasicVisualLexicon.NODE_BORDER_PAINT);
    public static final VizPropStore NODE_LABEL_FONT        = new BasicVizPropStore(BasicExtracter.FONT,                            BasicVisualLexicon.NODE_LABEL_FONT_FACE);
    public static final VizPropStore NODE_LABEL_SIZE        = new BasicVizPropStore(BasicExtracter.FONT_SIZE,                       BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
    public static final VizPropStore NODE_TRANSPARENT       = new BasicVizPropStore(BasicExtracter.TRANSPARENT, PV_TRANSPARENT_MAP, BasicVisualLexicon.NODE_TRANSPARENCY);
    public static final VizPropStore NODE_ALWAYS_TRANSPARENT = new BasicVizPropStore(new DefaultExtracter(0),                       BasicVisualLexicon.NODE_TRANSPARENCY);
    public static final VizPropStore NODE_BORDER_STYLE      = new BasicVizPropStore(BasicExtracter.LINE_STYLE_NAME, PV_LINE_STYLE_MAP, BasicVisualLexicon.NODE_BORDER_LINE_TYPE);
    public static final VizPropStore NODE_BORDER_THICKNESS  = new BasicVizPropStore(BasicExtracter.NODE_LINE_THICKNESS,             BasicVisualLexicon.NODE_BORDER_WIDTH);
    public static final VizPropStore NODE_SHAPE             = new BasicVizPropStore(BasicExtracter.SHAPE, PV_SHAPE_MAP,             BasicVisualLexicon.NODE_SHAPE);

    final VisualProperty<?>[] cyVizProps;
    final Extracter extracter;
    final Map<?,?> map;

    BasicVizPropStore(final Extracter extracter, final VisualProperty<?> ... cyVizProps) {
      this(extracter, null, cyVizProps);
    }

    BasicVizPropStore(final Extracter extracter, final Map<?,?> map, final VisualProperty<?> ... cyVizProps) {
      this.extracter = extracter;
      this.cyVizProps = cyVizProps;
      this.map = map;
    }

    public DelayedVizProp[] store(final CyIdentifiable cyNetObj, final PathwayElement pvElem) {
      Object cyValue = extracter.extract(pvElem);
      if (map != null) {
        cyValue = map.get(cyValue);
      }
      final DelayedVizProp[] props = new DelayedVizProp[cyVizProps.length];
      for (int i = 0; i < cyVizProps.length; i++) {
        props[i] = new DelayedVizProp(cyNetObj, cyVizProps[i], cyValue, true);
      }
      return props;
    }
  }

  void store(final CyIdentifiable cyNetObj, final PathwayElement pvElem, final VizPropStore ... vizPropStores) {
    for (final VizPropStore vizPropStore : vizPropStores) {
      final DelayedVizProp[] props = vizPropStore.store(cyNetObj, pvElem);
      for (int i = 0; i < props.length; i++) {
        cyDelayedVizProps.add(props[i]);
      }
    }
  }

  /*
   ========================================================
     Data nodes
   ========================================================
  */

  static final Extracter XREF_DATA_SOURCE_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvElem) {
      if(pvElem.getDataSource() == null)
        return null;
      else
        return pvElem.getDataSource().getFullName();
    }
  };

  static final Extracter XREF_ID_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvElem) {
      if(pvElem.getXref() == null)
        return null;
      else
        return pvElem.getXref().getId();
    }
  };


  static final TableStore XREF_ID_STORE = new BasicTableStore("XrefId", XREF_ID_EXTRACTER);
  static final TableStore XREF_DATA_SOURCE_STORE = new BasicTableStore("XrefDatasource", XREF_DATA_SOURCE_EXTRACTER);

  private void convertDataNodes() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.DATANODE))
        continue;
      convertDataNode(pvElem);
    }
  }

  static final Double ZERO = new Double(0.0);

  private void convertDataNode(final PathwayElement pvDataNode) {
    final CyNode cyNode = cyNet.addNode();
    pvToCyNodes.put(pvDataNode, cyNode);
    store(cyNodeTbl, cyNode, pvDataNode,
      BasicTableStore.GRAPH_ID,
      XREF_ID_STORE,
      XREF_DATA_SOURCE_STORE,
      BasicTableStore.TEXT_LABEL,
      BasicVizTableStore.NODE_WIDTH,
      BasicVizTableStore.NODE_HEIGHT,
      BasicVizTableStore.NODE_FILL_COLOR,
      BasicVizTableStore.NODE_COLOR,
      BasicVizTableStore.NODE_LABEL_FONT,
      BasicVizTableStore.NODE_LABEL_SIZE,
      BasicVizTableStore.NODE_TRANSPARENT,
      BasicVizTableStore.NODE_BORDER_STYLE,
      BasicVizTableStore.NODE_BORDER_THICKNESS,
      BasicVizTableStore.NODE_SHAPE
    );
    store(cyNode, pvDataNode,
      BasicVizPropStore.NODE_X,
      BasicVizPropStore.NODE_Y
    );
    if (ZERO.equals(BasicExtracter.NODE_LINE_THICKNESS.extract(pvDataNode))) {
      store(cyNode, pvDataNode,
        BasicVizPropStore.NODE_BORDER_THICKNESS
      );
    }
  }

  /*
   ========================================================
     Shapes
   ========================================================
  */
  static final TableStore IS_GPML_SHAPE = new BasicTableStore("IsGPMLShape", Boolean.class, new DefaultExtracter(true));

  private void convertShapes() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.SHAPE))
        continue;
      convertShape(pvElem);
    }
  }

  private void convertShape(final PathwayElement pvShape) {
    final CyNode cyNode = cyNet.addNode();
    pvToCyNodes.put(pvShape, cyNode);
    store(cyNodeTbl, cyNode, pvShape,
      BasicTableStore.GRAPH_ID,
      BasicTableStore.TEXT_LABEL,
      IS_GPML_SHAPE
    );
    store(cyNode, pvShape,
      BasicVizPropStore.NODE_X,
      BasicVizPropStore.NODE_Y,
      BasicVizPropStore.NODE_WIDTH,
      BasicVizPropStore.NODE_HEIGHT,
      BasicVizPropStore.NODE_FILL_COLOR,
      BasicVizPropStore.NODE_COLOR,
      BasicVizPropStore.NODE_LABEL_FONT,
      BasicVizPropStore.NODE_LABEL_SIZE,
      BasicVizPropStore.NODE_ALWAYS_TRANSPARENT,
      BasicVizPropStore.NODE_BORDER_STYLE,
      BasicVizPropStore.NODE_BORDER_THICKNESS,
      BasicVizPropStore.NODE_SHAPE,
      SELECTED_COLOR
    );
  }

  
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
      return pvParent.getMCenterY() + pvState.getRelY() * pvParent.getMHeight() / 2.0;
    }
  };

  final VizPropStore STATE_X_STORE = new BasicVizPropStore(STATE_X_EXTRACTER, BasicVisualLexicon.NODE_X_LOCATION);
  final VizPropStore STATE_Y_STORE = new BasicVizPropStore(STATE_Y_EXTRACTER, BasicVisualLexicon.NODE_Y_LOCATION);

  private void convertStates() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.STATE))
        continue;
      convertState(pvElem);
    }
  }

  private void convertState(final PathwayElement pvState) {
    // TODO: refactor this as an annotation

    final CyNode cyNode = cyNet.addNode();
    pvToCyNodes.put(pvState, cyNode);

    store(cyNodeTbl, cyNode, pvState,
      BasicTableStore.TEXT_LABEL
    );
    store(cyNode, pvState,
      STATE_X_STORE,
      STATE_Y_STORE,
      BasicVizPropStore.NODE_WIDTH,
      BasicVizPropStore.NODE_HEIGHT,
      BasicVizPropStore.NODE_FILL_COLOR,
      BasicVizPropStore.NODE_COLOR,
      BasicVizPropStore.NODE_LABEL_FONT,
      BasicVizPropStore.NODE_LABEL_SIZE,
      BasicVizPropStore.NODE_TRANSPARENT,
      BasicVizPropStore.NODE_BORDER_STYLE,
      BasicVizPropStore.NODE_BORDER_THICKNESS,
      BasicVizPropStore.NODE_SHAPE
    );
  }
  
  /*
   ========================================================
     Groups
   ========================================================
  */

  static final Extracter GROUP_X_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) {
      return pvGroup.getMCenterX();
    }
  };

  static final Extracter GROUP_Y_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) {
      return pvGroup.getMCenterY();
    }
  };
  
  static final Extracter GROUP_W_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) {
      return pvGroup.getMWidth();
    }
  };

  static final Extracter GROUP_H_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) {
      return pvGroup.getMHeight();
    }
  };

  static final Converter GROUP_FILL_COLOR_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final String styleName = (String) pvValues[0];
      final GroupStyle style = styleName != null ? GroupStyle.fromName(styleName) : null;
      if (GroupStyle.GROUP.equals(style))
        return Color.WHITE;
      else
        return new Color(0xefefef);
    }
  };

  static final Extracter GROUP_FILL_COLOR_EXTRACTER = new BasicExtracter(GROUP_FILL_COLOR_CONVERTER, StaticProperty.GROUPSTYLE);

  static final Converter GROUP_BORDER_THICKNESS_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final String styleName = (String) pvValues[0];
      final GroupStyle style = styleName != null ? GroupStyle.fromName(styleName) : null;
      if (GroupStyle.GROUP.equals(style))
        return 0.0;
      else
        return 1.0;
    }
  };

  static final Extracter GROUP_BORDER_THICKNESS_EXTRACTER = new BasicExtracter(GROUP_BORDER_THICKNESS_CONVERTER, StaticProperty.GROUPSTYLE);

  static final VizPropStore GROUP_X                 = new BasicVizPropStore(GROUP_X_EXTRACTER,                                    BasicVisualLexicon.NODE_X_LOCATION);
  static final VizPropStore GROUP_Y                 = new BasicVizPropStore(GROUP_Y_EXTRACTER,                                    BasicVisualLexicon.NODE_Y_LOCATION);
  static final VizPropStore SELECTED_COLOR          = new BasicVizPropStore(new DefaultExtracter(new Color(255, 255, 204, 127)),  BasicVisualLexicon.NODE_SELECTED_PAINT);
  static final VizPropStore GROUP_WIDTH             = new BasicVizPropStore(GROUP_W_EXTRACTER,                                    BasicVisualLexicon.NODE_WIDTH);
  static final VizPropStore GROUP_HEIGHT            = new BasicVizPropStore(GROUP_H_EXTRACTER,                                    BasicVisualLexicon.NODE_HEIGHT);
  static final VizPropStore GROUP_FILL_COLOR        = new BasicVizPropStore(GROUP_FILL_COLOR_EXTRACTER,                           BasicVisualLexicon.NODE_FILL_COLOR);
  static final VizPropStore GROUP_COLOR             = new BasicVizPropStore(new DefaultExtracter(new Color(0xaaaaaa)),            BasicVisualLexicon.NODE_LABEL_COLOR, BasicVisualLexicon.NODE_BORDER_PAINT);
  static final VizPropStore GROUP_BORDER_THICKNESS  = new BasicVizPropStore(GROUP_BORDER_THICKNESS_EXTRACTER,                     BasicVisualLexicon.NODE_BORDER_WIDTH);
  static final VizPropStore GROUP_BORDER_STYLE      = new BasicVizPropStore(new DefaultExtracter("Dots"), PV_LINE_STYLE_MAP,      BasicVisualLexicon.NODE_BORDER_LINE_TYPE);
  static final VizPropStore GROUP_SHAPE             = new BasicVizPropStore(new DefaultExtracter("Rectangle"), PV_SHAPE_MAP,      BasicVisualLexicon.NODE_SHAPE);

  private void convertGroups() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.GROUP))
        continue;
      convertGroup(pvElem);
    }
  }

  private void convertGroup(final PathwayElement pvGroup) {
    final CyNode cyGroupNode = cyNet.addNode();
    pvToCyNodes.put(pvGroup, cyGroupNode);

    store(cyGroupNode, pvGroup,
      GROUP_X,
      GROUP_Y,
      SELECTED_COLOR,
      GROUP_WIDTH,
      GROUP_HEIGHT,
      GROUP_COLOR,
      GROUP_FILL_COLOR,
      GROUP_BORDER_THICKNESS,
      GROUP_BORDER_STYLE,
      BasicVizPropStore.NODE_ALWAYS_TRANSPARENT,
      GROUP_SHAPE 
    );
  }

  /*
   ========================================================
     Labels
   ========================================================
  */


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

    final CyNode cyNode = cyNet.addNode();
    pvToCyNodes.put(pvLabel, cyNode);
    store(cyNodeTbl, cyNode, pvLabel,
      BasicTableStore.TEXT_LABEL
    );
    store(cyNode, pvLabel,
      BasicVizPropStore.NODE_X,
      BasicVizPropStore.NODE_Y,
      BasicVizPropStore.NODE_WIDTH,
      BasicVizPropStore.NODE_HEIGHT,
      BasicVizPropStore.NODE_BORDER_STYLE,
      BasicVizPropStore.NODE_BORDER_THICKNESS,
      BasicVizPropStore.NODE_COLOR,
      BasicVizPropStore.NODE_SHAPE,
      BasicVizPropStore.NODE_LABEL_FONT,
      BasicVizPropStore.NODE_LABEL_SIZE,
      BasicVizPropStore.NODE_TRANSPARENT,
      SELECTED_COLOR
    );
  }
  
  /*
   ========================================================
     Anchors
   ========================================================
  */

  private void convertAnchors() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!(pvElem.getObjectType().equals(ObjectType.LINE) || pvElem.getObjectType().equals(ObjectType.GRAPHLINE)))
        continue;
      if (pvElem.getMAnchors().isEmpty())
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

    final List<MAnchor> pvAnchors = pvElem.getMAnchors();
    if (pvAnchors.isEmpty()) {
      newEdge(pvLine, cyStartNode, cyEndNode, true, true);
    } else {
      // this is for multiple segmented lines
      newEdge(pvLine, cyStartNode, pvToCyNodes.get(pvAnchors.get(0)), true, false);
      for (int i = 1; i < pvAnchors.size(); i++) {
        newEdge(pvLine, pvToCyNodes.get(pvAnchors.get(i - 1)), pvToCyNodes.get(pvAnchors.get(i)), false, false);
      }
      newEdge(pvLine, pvToCyNodes.get(pvAnchors.get(pvAnchors.size() - 1)), cyEndNode, false, true);
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
