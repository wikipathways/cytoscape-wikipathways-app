package org.wikipathways.cytoscapeapp;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.EdgeBendVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.AbstractVisualPropertyValue;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.Bend;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.Handle;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.pathvisio.core.model.ConnectorRestrictions;
import org.pathvisio.core.model.ConnectorShape;
import org.pathvisio.core.model.GraphLink;
import org.pathvisio.core.model.GraphLink.GraphIdContainer;
import org.pathvisio.core.model.GroupStyle;
import org.pathvisio.core.model.IShape;
import org.pathvisio.core.model.LineStyle;
import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.Comment;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.model.PathwayElement.MPoint;
import org.pathvisio.core.model.ShapeType;
import org.pathvisio.core.model.StaticProperty;
import org.pathvisio.core.view.MIMShapes;

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
  final CyNetwork         cyNet;
  final CyTable           cyNodeTbl;
  final CyTable           cyEdgeTbl;
  final WPManager         manager;
  final CyNetworkView 	networkView;
  /**
   * Create a converter from the given pathway and store it in the given network.
   * Constructing this object will not start the conversion and will not modify
   * the given network in any way.
   *
   * @param eventHelper The {@code CyEventHelper} service -- used to flush network object creation events
   * @param annots A wrapper around the Cytoscape Annotations API
   * @param pvPathway The GPML pathway object from which to convert
   * @param cyNetView The Cytoscape network to contain the converted GPML pathway
   */
	public GpmlToPathway(
	final WPManager     inManager,
	final CyEventHelper     cyEventHelper,
      final Annots            cyAnnots,
      final Pathway           pvPathway,
      final CyNetwork         cyNet) {
		manager = inManager;
		this.cyEventHelper = cyEventHelper;
		this.cyAnnots = cyAnnots;
		this.pvPathway = pvPathway;
		this.cyNet = cyNet;
		networkView = getNetworkView(cyNet);
		this.cyNodeTbl = cyNet.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
		this.cyEdgeTbl = cyNet.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
//		System.out.println("GpmlToPathway");
		MIMShapes.registerShapes();
	}

  /**
   * Convert the pathway given in the constructor.
   */
	public List<DelayedVizProp> convert() {
	MIMShapes.registerShapes();
    setupCyTables();

    // convert by each pathway element type
    convertDataNodes();
    convertShapes();
    convertStates();
    convertGroups();
    convertLabels();
    convertAnchors();
    convertLines();

    // clear our data structures just to be nice to the GC
    pvToCyNodes.clear();
    return cyDelayedVizProps;
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
//        BasicVizTableStore.NODE_ROTATION,
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
        BasicVizTableStore.EDGE_CONNECTOR_TYPE,
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

	  static final Converter Z_CONVERT = new Converter() {		// AST
		    public Object toCyValue(Object[] pvValues) {
//		    	System.out.println("Z_CONVERT: " + pvValues[0] + " @ " + pvValues[0].getClass());
		     double d = (Integer) pvValues[0] * 1.0;
//		    	System.out.println(d);
		    		 return new Double( d);
		    }
		  };

  static final Converter PV_ARROW_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
     String name =   ((org.pathvisio.core.model.LineType) pvValues[0]).getName();
     return name;
    }
  };

  /** AST
   * Hard coded array of Connector Types because there is no StaticProperty.CONNECTORTYPE
   */
  static final Converter PV_CONNECTOR_TYPE_CONVERTER = new Converter() {
    final String[] PV_CONNECTOR_TYPE_NAMES ={"Straight","Curved","Elbow","Segmented"};
    public Object toCyValue(Object[] pvValues) {
      final int lineStyle = (Integer) pvValues[0];
      return PV_CONNECTOR_TYPE_NAMES[lineStyle];
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
    	try {
      return ((Font) PV_FONT_CONVERTER.toCyValue(pvValues)).getFontName();
    	}
    	catch (ClassCastException e)  	{  return null;  	} 
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
    	try {
    	      final ShapeType pvShapeType = (ShapeType) pvValues[0];
    	      final Double pvLineThickness = (Double) pvValues[1];
    	      if (ShapeType.NONE.equals(pvShapeType)) 
    	        return 0.0;  
    	      return pvLineThickness;
  		
    	} catch (ClassCastException e)
    	{
        return 0.0;  
    	}
    }
  };

  /**
   * Uses PathVisio's {@code StaticProperty.COLOR} or {@code StaticProperty.FILLCOLOR}
   * and returns a hex string of the color.
   */
  static final Converter PV_COLOR_STRING_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
    	try {
      final Color c = (Color) pvValues[0];
      final int r = c.getRed();
      final int g = c.getGreen();
      final int b = c.getBlue();
      return String.format("#%02x%02x%02x", r, g, b);
    	} catch (ClassCastException e)
    	{
        return "#ff0000";  
    	}
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
    public static final Extracter Z                   = new BasicExtracter(Z_CONVERT, StaticProperty.ZORDER);
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
    public static final Extracter LINE_CONNECTOR_TYPE  = new BasicExtracter(PV_CONNECTOR_TYPE_CONVERTER, StaticProperty.LINESTYLE);
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
    BasicExtracter(StaticProperty ... pvProps) {  this(NO_CONVERT, pvProps);   }

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
//      System.out.println("Extracting...");
      for (int i = 0; i < pvValues.length; i++) {
        pvValues[i] = pvElem.getStaticProperty(pvProps[i]);
//        System.out.println("Extracting..." + pvProps[i] + " = " + pvValues[i]);
      }
      if (pvValues.length == 1 && pvValues[0] == null)
        return null;
     try
     {
    	  Object obj = converter.toCyValue(pvValues);
//          System.out.println("Extracted: " + obj);

    	  return obj;
     }
     catch (Exception e) { return null;	}
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
    public DefaultExtracter(final Object cyValue) {   this.cyValue = cyValue; }
    public Object extract(final PathwayElement pvElem) { return cyValue;  }
  }

  /**
   * Stores values produced by an {@code Extractor} into a Cytoscape table column.
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
    PV_ARROW_MAP.put("mim-branching-left",  ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("mim-branching-right", ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("mim-translation",     ArrowShapeVisualProperty.DELTA);
    PV_ARROW_MAP.put("mim-gap",    			ArrowShapeVisualProperty.NONE);
    PV_ARROW_MAP.put("mim-covalent-bond",  	ArrowShapeVisualProperty.CROSS_DELTA);
  }

  static Map<String,NodeShape> PV_SHAPE_MAP = new HashMap<String,NodeShape>();
  static {
    PV_SHAPE_MAP.put("Rectangle",        NodeShapeVisualProperty.RECTANGLE);
    PV_SHAPE_MAP.put("Triangle",         NodeShapeVisualProperty.TRIANGLE);			// Note: triangle is different shape than PV's
    PV_SHAPE_MAP.put("RoundRectangle", NodeShapeVisualProperty.ROUND_RECTANGLE);
    PV_SHAPE_MAP.put("RoundedRectangle", NodeShapeVisualProperty.ROUND_RECTANGLE);
    PV_SHAPE_MAP.put("Hexagon",          NodeShapeVisualProperty.HEXAGON);
    PV_SHAPE_MAP.put("Pentagon",         NodeShapeVisualProperty.HEXAGON);			// TODO
    PV_SHAPE_MAP.put("Ellipse",             NodeShapeVisualProperty.ELLIPSE);
    PV_SHAPE_MAP.put("Oval",             NodeShapeVisualProperty.ELLIPSE);
    PV_SHAPE_MAP.put("Octagon",          NodeShapeVisualProperty.OCTAGON);
    PV_SHAPE_MAP.put("Cell",          NodeShapeVisualProperty.ELLIPSE);
    PV_SHAPE_MAP.put("Nucleus",          NodeShapeVisualProperty.ELLIPSE);
    PV_SHAPE_MAP.put("Organelle",          NodeShapeVisualProperty.ROUND_RECTANGLE);
    PV_SHAPE_MAP.put("Octagon",          NodeShapeVisualProperty.OCTAGON);
    PV_SHAPE_MAP.put("Mitochondria",     new NodeShapeImpl("Mitochondria", "Mitochondria"));	
    PV_SHAPE_MAP.put("Sarcoplasmic Reticulum", new NodeShapeImpl("Sarcoplasmic Reticulum", "Sarcoplasmic Reticulum"));	
    PV_SHAPE_MAP.put("Endoplasmic Reticulum", new NodeShapeImpl("Endoplasmic Reticulum", "Endoplasmic Reticulum"));	
    PV_SHAPE_MAP.put("Golgi Apparatus", new NodeShapeImpl("Golgi Apparatus", "Golgi Apparatus"));	
    PV_SHAPE_MAP.put("Brace",     		new NodeShapeImpl("Brace", "Brace"));		
    PV_SHAPE_MAP.put("Arc",     		new NodeShapeImpl("Arc", "Arc"));		
    

  }
	private static final class NodeShapeImpl extends AbstractVisualPropertyValue implements NodeShape {
		public NodeShapeImpl(final String displayName, final String serializableString) {
			super(displayName, serializableString);
		}
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
  // AST			
  static Map<String,String> PV_CONNECTORTYPE_MAP = new HashMap<String,String>();
  static {
	  PV_CONNECTORTYPE_MAP.put("Straight",  "Straight");
	  PV_CONNECTORTYPE_MAP.put("Curved", "Curved");
	  PV_CONNECTORTYPE_MAP.put("Elbow", "Elbow");
	  PV_CONNECTORTYPE_MAP.put("Segmented",  "Segmented");
  }

  static class BasicVizTableStore extends BasicTableStore implements VizTableStore {
//    public static final VizTableStore NODE_ROTATION         = new BasicVizTableStore("Rotation", Double.class,        BasicExtracter.ROTATION,                        BasicVisualLexicon.NODE_ROTATION);

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

    //AST
    public static final VizTableStore EDGE_CONNECTOR_TYPE     = new BasicVizTableStore("ConnectorType",               BasicExtracter.LINE_CONNECTOR_TYPE, PV_CONNECTORTYPE_MAP,   BasicVisualLexicon.EDGE_BEND);
    

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

    public String getCyColumnName() 			{      return super.cyColName;    }
    public Class<?> getCyColumnType() 			{      return super.cyColType;    }
    public VisualProperty<?>[] getCyVizProps() 	{      return vizProps;    }
    public Map<?,?> getMapping() 				{      return mapping;    }
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
//    	      BasicVizTableStore.NODE_ROTATION,
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
      BasicVizTableStore.EDGE_CONNECTOR_TYPE,		//AST
      BasicVizTableStore.EDGE_LINE_THICKNESS,
      BasicVizTableStore.EDGE_START_ARROW,
      BasicVizTableStore.EDGE_END_ARROW
      );
  }

  void store(final CyTable cyTable, final CyIdentifiable cyNetObj, final PathwayElement pvElem, final TableStore ... tableStores) {
    for (final TableStore tableStore : tableStores) 
      tableStore.store(cyTable, cyNetObj, pvElem);
    
  }

  /**
   * Takes a PathVisio element value and stores
   * the equivalent Cytoscape visual bypass value in a {@code DelayedVizProp}.
   */
  static interface VizPropStore {
    DelayedVizProp[] store(final CyIdentifiable cyNetObj, final PathwayElement pvElem);
  }

  static class BasicVizPropStore implements VizPropStore {
    public static final VizPropStore NODE_X   = new BasicVizPropStore(BasicExtracter.X,  BasicVisualLexicon.NODE_X_LOCATION);
    public static final VizPropStore NODE_Y   = new BasicVizPropStore(BasicExtracter.Y,  BasicVisualLexicon.NODE_Y_LOCATION);
    public static final VizPropStore NODE_Z   = new BasicVizPropStore(BasicExtracter.Z,  BasicVisualLexicon.NODE_Z_LOCATION);
    public static final VizPropStore NODE_LABEL    = new BasicVizPropStore(BasicExtracter.TEXT_LABEL,   BasicVisualLexicon.NODE_LABEL);
    public static final VizPropStore NODE_WIDTH    = new BasicVizPropStore(BasicExtracter.WIDTH,   BasicVisualLexicon.NODE_WIDTH);
    public static final VizPropStore NODE_HEIGHT   = new BasicVizPropStore(BasicExtracter.HEIGHT,  BasicVisualLexicon.NODE_HEIGHT);
    public static final VizPropStore NODE_FILL_COLOR  = new BasicVizPropStore(BasicExtracter.FILL_COLOR,  BasicVisualLexicon.NODE_FILL_COLOR);
    public static final VizPropStore NODE_COLOR       = new BasicVizPropStore(BasicExtracter.COLOR,       BasicVisualLexicon.NODE_LABEL_COLOR, BasicVisualLexicon.NODE_BORDER_PAINT);
    public static final VizPropStore NODE_LABEL_FONT  = new BasicVizPropStore(BasicExtracter.FONT,        BasicVisualLexicon.NODE_LABEL_FONT_FACE);
    public static final VizPropStore NODE_LABEL_SIZE  = new BasicVizPropStore(BasicExtracter.FONT_SIZE,   BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
    public static final VizPropStore NODE_TRANSPARENT = new BasicVizPropStore(BasicExtracter.TRANSPARENT, PV_TRANSPARENT_MAP, BasicVisualLexicon.NODE_TRANSPARENCY);
    public static final VizPropStore NODE_ALWAYS_TRANSPARENT = new BasicVizPropStore(new DefaultExtracter(0),      BasicVisualLexicon.NODE_TRANSPARENCY);
    public static final VizPropStore NODE_BORDER_STYLE = new BasicVizPropStore(BasicExtracter.LINE_STYLE_NAME, PV_LINE_STYLE_MAP, BasicVisualLexicon.NODE_BORDER_LINE_TYPE);
    public static final VizPropStore NODE_BORDER_THICKNESS  = new BasicVizPropStore(BasicExtracter.NODE_LINE_THICKNESS,             BasicVisualLexicon.NODE_BORDER_WIDTH);
    public static final VizPropStore NODE_SHAPE       = new BasicVizPropStore(BasicExtracter.SHAPE, PV_SHAPE_MAP, BasicVisualLexicon.NODE_SHAPE);

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
    for (final VizPropStore vizPropStore : vizPropStores) 
    {
      final DelayedVizProp[] props = vizPropStore.store(cyNetObj, pvElem);
      for (DelayedVizProp prop : props) 
        cyDelayedVizProps.add(prop);
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
      BasicVizPropStore.NODE_Y,
      BasicVizPropStore.NODE_Z
    );
    if (ZERO.equals(BasicExtracter.NODE_LINE_THICKNESS.extract(pvDataNode))) 			// already done above!
      store(cyNode, pvDataNode,  BasicVizPropStore.NODE_BORDER_THICKNESS );
    
  }

  /*
   ========================================================
     Shapes
   ========================================================
  */
  static final TableStore IS_GPML_SHAPE = new BasicTableStore("IsGPMLShape", Boolean.class, new DefaultExtracter(true));
  // TODO: refactor this as an annotation

  private void convertShapes() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) 
      if (pvElem.getObjectType().equals(ObjectType.SHAPE))
    	  convertShape(pvElem);
  }

  private void convertShape(final PathwayElement pvShape) {
    final CyNode cyNode = cyNet.addNode();
    Long id = cyNode.getSUID();
//    pvShape.setLineThickness(10);
    pvToCyNodes.put(pvShape, cyNode);
    IShape shtype = pvShape.getShapeType();
    if (shtype == null) 	return;
//    if (verbose)  
//    {
//    	System.out.println("convertShape: " + (shtype == null ? "NONE" : shtype.getName()) + " " + id + " " + pvShape.getFillColor());
//    	System.out.println("at: " + (int) pvShape.getMCenterX() + ", " +  (int) pvShape.getMCenterY());
//    }
    store(cyNodeTbl, cyNode, pvShape, BasicTableStore.GRAPH_ID, BasicTableStore.TEXT_LABEL, IS_GPML_SHAPE);
    store(cyNode, pvShape,
      BasicVizPropStore.NODE_X, BasicVizPropStore.NODE_Y, BasicVizPropStore.NODE_Z,
      BasicVizPropStore.NODE_WIDTH, 
      BasicVizPropStore.NODE_HEIGHT,
      BasicVizPropStore.NODE_FILL_COLOR,  BasicVizPropStore.NODE_COLOR,
      BasicVizPropStore.NODE_LABEL_FONT, BasicVizPropStore.NODE_LABEL_SIZE,
      BasicVizPropStore.NODE_ALWAYS_TRANSPARENT,
      BasicVizPropStore.NODE_BORDER_STYLE,  
      BasicVizPropStore.NODE_BORDER_THICKNESS, 
      BasicVizPropStore.NODE_SHAPE, 
      SELECTED_COLOR 
    );
    double rotation =  pvShape.getRotation();
    DelayedVizProp.putRotation(cyNode, rotation);
    DelayedVizProp.putPathwayElement(cyNode, pvShape);

   }
  
  /*
   ========================================================
     States
   ========================================================
  */

  final Extracter STATE_X_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvState) {
      final PathwayElement pvParent = (PathwayElement) pvPathway.getGraphIdContainer(pvState.getGraphRef());
      return pvParent.getMCenterX() + (pvState.getRelX() * pvParent.getMWidth() / 2.0);
    }
  };
  
  final Extracter STATE_Y_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvState) {
      final PathwayElement pvParent = (PathwayElement) pvPathway.getGraphIdContainer(pvState.getGraphRef());
      return pvParent.getMCenterY() + (pvState.getRelY() * pvParent.getMHeight() / 2.0);
    }
  };

  final VizPropStore STATE_X_STORE = new BasicVizPropStore(STATE_X_EXTRACTER, BasicVisualLexicon.NODE_X_LOCATION);
  final VizPropStore STATE_Y_STORE = new BasicVizPropStore(STATE_Y_EXTRACTER, BasicVisualLexicon.NODE_Y_LOCATION);

  private void convertStates() {
	  DelayedVizProp.clearStateList();
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) 
      if (pvElem.getObjectType().equals(ObjectType.STATE))
      convertState(pvElem);
  }

  private void convertState(final PathwayElement pvState) {

    final CyNode cyNode = cyNet.addNode();

    pvToCyNodes.put(pvState, cyNode);
    store(cyNodeTbl, cyNode, pvState, BasicTableStore.TEXT_LABEL );
    
//    CyRow row0 = cyNodeTbl.getRow(cyNode.getSUID());
//	System.out.println("stored: " + row0.get("name", String.class));

    store(cyNode, pvState,
      STATE_X_STORE,
      STATE_Y_STORE,
      BasicVizPropStore.NODE_Z,
      BasicVizPropStore.NODE_WIDTH,
      BasicVizPropStore.NODE_HEIGHT,
      BasicVizPropStore.NODE_FILL_COLOR,
      BasicVizPropStore.NODE_COLOR,
      BasicVizPropStore.NODE_LABEL,
      BasicVizPropStore.NODE_LABEL_FONT,
      BasicVizPropStore.NODE_LABEL_SIZE,
      BasicVizPropStore.NODE_TRANSPARENT,
      BasicVizPropStore.NODE_BORDER_STYLE,
      BasicVizPropStore.NODE_BORDER_THICKNESS,
      BasicVizPropStore.NODE_SHAPE
    );
//    DelayedVizProp fillColorProp = new DelayedVizProp(cyNode,
//    		BasicVisualLexicon.NODE_FILL_COLOR, new Color(250, 0, 0), true);
//    cyDelayedVizProps.add(fillColorProp);
    CyRow row = cyNodeTbl.getRow(cyNode.getSUID());
    if (row == null) 
	{
    	System.err.println("NO ROW " + cyNode.getSUID());
    	return;
	}
    DelayedVizProp.saveState(cyNode.getSUID());
    row.getAllValues().put("State", 1);
//    row.set(CyNetwork.NODE_LABEL, pvState.getTextLabel());
//    String graphRef = pvState.getGraphRef();
//System.out.println("state = " + row.get(CyNetwork.NODE_LABEL, String.class));
    CyColumn col;
    List<Comment> comments = pvState.getComments();
    for (Comment c : comments)
    {
    	String s = c.getComment();
    	if (s != null && s.trim().length() > 0)
    	{
        	String[] tokens = s.split(";");
        	for (String token : tokens)
        	{
        		int sep = token.indexOf("=");
        		if (sep < 0) continue;
        		String attr = token.substring(0, sep).trim();
        		String val = token.substring(sep+1).trim();
        		col = cyNodeTbl.getColumn(attr);
        		if (col == null)
        		{
        			cyNodeTbl.createColumn(attr, String.class, false, "");
        			col = cyNodeTbl.getColumn(attr);
        			
        		}
        		row.set(attr, val);
//        		if (graphRef != null)
//        		{
//        			cyNodeTbl.getRow(primaryKey)Collection<CyRow> nodeRows = cyNodeTbl.getMatchingRows("GraphId", graphRef);
//        			if (nodeRows.size() != 0)
//        			{
//        				CyRow aRow =  nodeRows.iterator().next();
//        				aRow.set(attr, val);
//        			}
//        		}
        	}
    	}
    }    
  }
  
  /*
   ========================================================
     Groups
   ========================================================
  */

  static final Extracter GROUP_X_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) { return pvGroup.getMCenterX(); }
  };

  static final Extracter GROUP_Y_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) { return pvGroup.getMCenterY(); }
  };
  
  static final Extracter GROUP_W_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) { return pvGroup.getMWidth(); }
  };

  static final Extracter GROUP_H_EXTRACTER = new Extracter() {
    public Object extract(final PathwayElement pvGroup) { return pvGroup.getMHeight(); }
  };

  static Color GRAY = new Color(0xefefef);
  
  static final Converter GROUP_FILL_COLOR_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final String styleName = (String) pvValues[0];
      final GroupStyle style = styleName != null ? GroupStyle.fromName(styleName) : null;
      return (GroupStyle.GROUP.equals(style)) ? Color.WHITE : GRAY;
    }
  };

  static final Extracter GROUP_FILL_COLOR_EXTRACTER = new BasicExtracter(GROUP_FILL_COLOR_CONVERTER, StaticProperty.GROUPSTYLE);

  static final Converter GROUP_BORDER_THICKNESS_CONVERTER = new Converter() {
    public Object toCyValue(Object[] pvValues) {
      final String styleName = (String) pvValues[0];
      final GroupStyle style = styleName != null ? GroupStyle.fromName(styleName) : null;
      if (GroupStyle.COMPLEX.equals(style))
          return 3.0;
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
  static final VizPropStore GROUP_BORDER_STYLE      = new BasicVizPropStore(new DefaultExtracter("Dash"), PV_LINE_STYLE_MAP,      BasicVisualLexicon.NODE_BORDER_LINE_TYPE);
  static final VizPropStore GROUP_SHAPE             = new BasicVizPropStore(new DefaultExtracter("Octagon"), PV_SHAPE_MAP,      BasicVisualLexicon.NODE_SHAPE);

  private void convertGroups() {
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) {
      if (!pvElem.getObjectType().equals(ObjectType.GROUP)) continue;
      final int numChildren = pvPathway.getGroupElements(pvElem.getGroupId()).size();
      if (numChildren > 0) // ignore groups with 0 children
    	  convertGroup(pvElem);
    }
  }

  private void convertGroup(final PathwayElement pvGroup) {
    final CyNode cyGroupNode = cyNet.addNode();
    pvToCyNodes.put(pvGroup, cyGroupNode);
//    if (verbose)  System.out.println("convertGroup: " + pvGroup.getLineThickness());
    store(cyGroupNode, pvGroup,
      GROUP_X,
      GROUP_Y,
      BasicVizPropStore.NODE_Z,
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
    for (final PathwayElement pvElem : pvPathway.getDataObjects()) 
      if (pvElem.getObjectType().equals(ObjectType.LABEL))
    	  convertLabel(pvElem);
  }

  private void convertLabel(final PathwayElement pvLabel) {
    // TODO: refactor this as an annotation
	// comment Tina: not sure if they can all be replaced by annotations because they are often connected with data nodes

    final CyNode cyNode = cyNet.addNode();
    pvToCyNodes.put(pvLabel, cyNode);
    store(cyNodeTbl, cyNode, pvLabel, BasicTableStore.TEXT_LABEL);
    store(cyNode, pvLabel,
      BasicVizPropStore.NODE_X,
      BasicVizPropStore.NODE_Y,
      BasicVizPropStore.NODE_Z,
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
//      if (pvElem != null) continue;
      if (!pvElem.getMAnchors().isEmpty())
    	  convertAnchorsInLine(pvElem);
    }
  }

  private void assignAnchorVizStyle(final CyNode node, final Point2D position) {
    assignAnchorVizStyle(node, position, Color.WHITE);
  }

	private void convertAnchorsInLine(final PathwayElement pvElem) {
		final MLine pvLine = (MLine) pvElem;
		for (final MAnchor pvAnchor : pvElem.getMAnchors()) {
			final CyNode cyNode = cyNet.addNode();
			final Point2D position = pvLine.getConnectorShape().fromLineCoordinate(pvAnchor.getPosition());
//			System.out.println("\nAnchor at " + cyNode.getSUID() + "  --------");
			pvToCyNodes.put(pvAnchor, cyNode);
			assignAnchorVizStyle(cyNode, position, pvLine.getColor());
//			System.out.println("--------");
		}
	 }

  private void assignAnchorVizStyle(final CyNode node, final Point2D position, final Color color) {
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_X_LOCATION, position.getX(), false));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_Y_LOCATION, position.getY(), false));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_Z_LOCATION, 10001.5, false));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_FILL_COLOR, color, true));
//    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_BORDER_WIDTH, 20.0, true));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_WIDTH, 1.0, true));
    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_HEIGHT, 1.0, true));
//    cyDelayedVizProps.add(new DelayedVizProp(node, BasicVisualLexicon.NODE_TRANSPARENCY, 128, true));  // AST
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
//    System.out.println("\n--------------------\nconvertLine");

    Point2D startPt = pvLine.getStartPoint();
    Point2D endPt = pvLine.getEndPoint();
    GraphIdContainer startref = pvPathway.getGraphIdContainer(pvStartRef);
    CyNode cyStartNode = pvToCyNodes.get(startref);
    if (cyStartNode == null) {
      cyStartNode = cyNet.addNode();
//      String nodeName = "" + cyStartNode.getSUID();
//      View<CyNode> view = networkView.getNodeView(cyStartNode);
//      view.getVisualProperty();
//      view.getModel();
      assignAnchorVizStyle(cyStartNode, startPt);
    }
    GraphIdContainer endref = pvPathway.getGraphIdContainer(pvEndRef);
    CyNode cyEndNode = pvToCyNodes.get(endref);
    if (cyEndNode == null) {
      cyEndNode = cyNet.addNode();
      assignAnchorVizStyle(cyEndNode, endPt);
    }    
//    final List<MAnchor> pvAnchors = pvElem.getMAnchors();
//	System.out.println("NAnchors: " + pvAnchors.size());
//    for (int i = 0; i < pvAnchors.size(); i++) {
//    	MAnchor anchor = pvAnchors.get(i);
//    	System.out.println("anchor at " + anchor.getPosition());
//      }

//    if (pvAnchors.isEmpty()) 
      newEdge(pvLine, cyStartNode, cyEndNode, true, true);
//    else
//    {
//      // this is for multiple segmented lines
////    	System.out.println("this is for multiple segmented lines: " + pvAnchors.size());
//      newEdge(pvLine, cyStartNode, pvToCyNodes.get(pvAnchors.get(0)), true, false);
//      for (int i = 1; i < pvAnchors.size(); i++) {
//        newEdge(pvLine, pvToCyNodes.get(pvAnchors.get(i - 1)), pvToCyNodes.get(pvAnchors.get(i)), false, false);
//      }
//      newEdge(pvLine, pvToCyNodes.get(pvAnchors.get(pvAnchors.size() - 1)), cyEndNode, false, true);
//    }
  }
//------------------------------------------
// Changes start here
  
  private void newEdge(final PathwayElement pvLine, final CyNode cySourceNode, 
		  final CyNode cyTargetNode, final boolean isStart, final boolean isEnd) {

	final CyEdge cyEdge = cyNet.addEdge(cySourceNode, cyTargetNode, true);
    
    store(cyEdgeTbl, cyEdge, pvLine, 
        	      BasicVizTableStore.EDGE_LINE_THICKNESS,
        	      BasicVizTableStore.EDGE_LINE_STYLE,
        	      BasicVizTableStore.EDGE_COLOR,
        	      BasicVizTableStore.EDGE_CONNECTOR_TYPE
        	      );
    
    storeEdgeBend(cyEdge, pvLine);
	DelayedVizProp prop = new DelayedVizProp(cyEdge, BasicVisualLexicon.EDGE_WIDTH, 1.0, true);		// #44
	cyDelayedVizProps.add(prop);

    // Arrow heads defined here
    if (isStart) 
      store(cyEdgeTbl, cyEdge, pvLine, BasicVizTableStore.EDGE_START_ARROW);
    
    if (isEnd) 
      store(cyEdgeTbl, cyEdge, pvLine, BasicVizTableStore.EDGE_END_ARROW);
    
    
  }
  
  
	private void storeEdgeBend(final CyEdge cyEdge, final PathwayElement pvLine) 
	{
		String connectorType = pvLine.getConnectorType().toString();
//		org.pathvisio.core.model.LineType endLineType = pvLine.getEndLineType();
//		org.pathvisio.core.model.LineType startLineType = pvLine.getStartLineType();
//		if (verbose)
//		{
//			System.out.println(connectorType + " from: " + getNodeNameWithId(cyEdge.getSource()) + "[" + startLineType.getMappName() + "] " + pvLine.getStartGraphRef() + 
//					" to: " + getNodeNameWithId(cyEdge.getTarget()) + "[" + endLineType.getMappName() + "] " + pvLine.getEndGraphRef());
//		}

		makeSegments(pvLine, cyEdge);
		Bend bend = EdgeBendVisualProperty.DEFAULT_EDGE_BEND;
		if ("Curved".equals(connectorType))
			bend = makeCurvedEdgeBend(networkView, cyEdge, pvLine);
		else if ("Elbow".equals(connectorType))
			bend = makeElbowEdgeBend(networkView, cyEdge, makeSegments(pvLine, cyEdge));

		DelayedVizProp prop = new DelayedVizProp(cyEdge, BasicVisualLexicon.EDGE_BEND, bend, true);
		cyDelayedVizProps.add(prop);
	}
	
  
  
	private List<Segment> makeSegments(final PathwayElement pvLine, final CyEdge cyEdge)
	{
//if (verbose)
//		{
//	  	System.out.println("makeSegments: " + pvLine.getStartGraphRef() + " -> " + pvLine.getEndGraphRef() );
//	  	System.out.println("");
//}
//		
		List<MPoint> pts = pvLine.getMPoints();
	  	MPoint mStart = pts.get(0);
	  	int len = pts.size();
	  	MPoint mEnd = pts.get(len-1);
//	  	if (verbose)	  	System.out.println("length: " + len + " points");
	  	
		double startRelX = mStart.getRelX();
		double startRelY = mStart.getRelY();
		double endRelX = mEnd.getRelX();
		double endRelY = mEnd.getRelY();
	  	Point2D startPt = mStart.toPoint2D();
	  	Point2D endPt = mEnd.toPoint2D();
  	
		int startSide = getSide(startRelX, startRelY);
		if (startSide < 0)
			startSide = getNearestSide(startPt, endPt);
		int endSide = getSide(endRelX, endRelY);
		if (endSide < 0)
			endSide = getNearestSide(endPt, startPt);

//if (verbose)
//		{
//	System.out.println("Source: " + getNodeNameWithId(cyEdge.getSource()) + printPoint(startPt) + " on " + sides[startSide] + " side");
//  	System.out.println("Target: "  + getNodeNameWithId(cyEdge.getTarget()) + printPoint(endPt) + " on " + sides[endSide] + " side");
//		}
		
	  	Point2D[] wps = calculateWayPoints(startPt, endPt, startSide, endSide);
	    segments = calculateSegments(startPt, endPt, startSide, endSide, wps);
	    return segments;
	}
	
	private int getNearestSide(Point2D startPt, Point2D endPt) {
		double dx = endPt.getX() - startPt.getX();
		double dy= endPt.getY() - startPt.getY();
		if (Math.abs(dy) > Math.abs(dx) ) 
			return (dy > 0) ? ConnectorRestrictions.SIDE_SOUTH : ConnectorRestrictions.SIDE_NORTH;
		return (dx > 0) ? ConnectorRestrictions.SIDE_EAST : ConnectorRestrictions.SIDE_WEST;
	}

	String[] sides = {  "North", "East", "South", "West" };
static boolean verbose = true;
//--------------------------------------------------------------------
	private Bend makeCurvedEdgeBend(CyNetworkView networkView, CyEdge edge, PathwayElement pvLine) {
	    BendFactory factory = manager.getBendFactory();		
		View<CyEdge> ev = networkView.getEdgeView(edge);
	    Bend bend = factory.createBend();
	    HandleFactory facto = manager.getHandleFactory();
//	    MPoint start = pvLine.getMStart();
//	    MPoint end = pvLine.getMEnd();
//		if (verbose)	
//			System.out.println("makeCurvedEdgeBend start at " + printPoint(start) + ", end at " + printPoint(end));
 
	    int sz = segments.size();
	    for (int i=0; i<sz; i++)
	    {
	    	Segment seg = segments.get(i);
	    	Point2D centerPoint  = centerPoint(seg.start, seg.end);	
	    	Handle h = facto.createHandle(networkView, ev,  centerPoint.getX(), centerPoint.getY());
//		  if (verbose)	
//			  System.out.println("adding handle at " + printPoint( centerPoint));
	    	bend.getAllHandles().add(h);
	    }
		return bend;
	}



	private Bend makeElbowEdgeBend(CyNetworkView networkView, CyEdge edge, List<Segment> segments) {
//		System.out.println("makeElbowEdgeBend"); 
   BendFactory factory = manager.getBendFactory();	
	if (networkView == null)    	{
//		System.out.println("networkView == null"); 
		return null;
	}
	View<CyEdge> ev = networkView.getEdgeView(edge);
    Bend bend = factory.createBend();
    HandleFactory facto = manager.getHandleFactory();
    
    int sz = segments.size();
    for (int i=0; i<sz-1; i++)
    {
    	Segment seg = segments.get(i);
    	Handle h = facto.createHandle(networkView, ev,  seg.end.getX(), seg.end.getY());		// put in 2 handles for a angled bend
	    Handle j = facto.createHandle(networkView, ev,  seg.end.getX(), seg.end.getY());
	    bend.getAllHandles().add(h);
	    bend.getAllHandles().add(j);
//	    
//	    System.out.println("adding two handles at " + seg);
    }
	return bend;
}
//--------------------------------------------------------------------

	public static final int AXIS_X = 0;
	public static final int AXIS_Y = 1;

	private CyNetworkView getNetworkView(CyNetwork cyNet) {
		return manager.getNetworkViewMgr().getNetworkViews(cyNet).iterator().next();
	}
	
//	private View<CyNode> getNodeView(CyNetworkView networkView, CyNode source) {
//		return networkView.getNodeView(source);
//}
	
	private String getNodeName(CyNode source) {
		CyTable nodeTable = networkView.getModel().getDefaultNodeTable();
		CyRow row = nodeTable.getRow(source.getSUID());
		String name = row.get("name", String.class);
		return name;
}
	private String getNodeNameWithId(CyNode source) {
//		CyTable nodeTable = networkView.getModel().getDefaultNodeTable();
//		CyRow row = nodeTable.getRow(source.getSUID());
//		String name = row.get("name", String.class);
		return getNodeName(source) + " (" + source.getSUID() + ")";
}
	 
//	
	private String printPoint(Point2D pt)
	{
		return ("(" + (int) pt.getX() + ", " + (int) pt.getY() + ")");
	}
	
	private String printPoint(MPoint pt)
	{
		return ("(" + (int) pt.getX() + ", " + (int) pt.getY() + ")");
	}
	
//	private Point2D.Double getNodePosition(View<CyNode> nodeView) {
//	    Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
//	    Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
//		return new Point2D.Double(x,y);
//	}
//=================================================================================
	// EDGES
	
	List<Segment> segments;
	List<Segment> getSegments()	{ return segments;	}
	private final static double SEGMENT_OFFSET = 40;


	protected Point2D[] calculateWayPoints(Point2D start, Point2D end, int startSide, int endSide) {
		int nrSegments = getNrSegments( start, end, startSide, endSide);

		//Else, calculate the default waypoints
		Point2D[] waypoints = new Point2D[nrSegments - 2];

		int startAxis = getSegmentAxis(startSide);
		int startDirection = getSegmentDirection(startSide);
		int endAxis = getSegmentAxis(endSide);
		int endDirection = getSegmentDirection(endSide);


		if(nrSegments - 2 == 1) {
			/*
			 * [S]---
			 * 		|
			 * 		---[S]
			 */
			waypoints[0] = calculateWayPoint(start, end, startAxis, startDirection);
		} else if(nrSegments - 2 == 2) {
			/*
			* [S]---
			* 		| [S]
			* 		|  |
			* 		|---
			*/
			double offset = SEGMENT_OFFSET * endDirection;
			Point2D pt = new Point2D.Double( end.getX() + offset, end.getY() + offset );
			waypoints[0] = calculateWayPoint(start, pt, startAxis, startDirection);

			waypoints[1] = calculateWayPoint(end, waypoints[0], endAxis, endDirection);
		} else if(nrSegments - 2 == 3) {
			/*  -----
			 *  |   |
			 * [S]  | [S]
			 *      |  |
			 *      |---
			 */
			//Start with middle waypoint
			waypoints[1] = centerPoint(start, end); 
			waypoints[0] = calculateWayPoint(start, waypoints[1], startAxis, startDirection);
			waypoints[2] = calculateWayPoint(end, waypoints[1], endAxis, endDirection);
		}
		 else if(nrSegments - 2 == 4) {			// NEW code UNTESTED
		/*   ---------
		 *   |       |
		 *   ---[S]  | [S]
		 *           |  |
		 *           |---
		 */
				waypoints[2] = centerPoint(start, end); 
				waypoints[1] = calculateWayPoint(start, waypoints[2], startAxis, startDirection);
				waypoints[0] = calculateWayPoint(start, waypoints[1], startAxis, startDirection);
				waypoints[3] = calculateWayPoint(end, waypoints[1], endAxis, endDirection);
			}
	return waypoints;
	}

	protected Point2D calculateWayPoint(Point2D start, Point2D end, int axis, int direction) {
		double x,y = 0;
		if(axis == AXIS_Y) {
			x = start.getX() + (end.getX() - start.getX()) / 2;
			y = start.getY() + SEGMENT_OFFSET * direction;
		} else {
			x = start.getX() + SEGMENT_OFFSET * direction;
			y = start.getY() + (end.getY() - start.getY()) / 2;
		}
		return new Point2D.Double(x, y);
	}

	protected List<Segment> calculateSegments(Point2D start, Point2D end, int startSide, int endSide, Point2D[] waypts) {
		int nrSegments = getNrSegments(start, end, startSide, endSide);
		Segment[] segments = new Segment[nrSegments];

		int startAxis = getSegmentAxis(startSide);
		if(nrSegments == 2) { //No waypoints
			segments[0] = createStraightSegment(start, end, startAxis);
			segments[1] = createStraightSegment(segments[0].getMEnd(), end, getOppositeAxis(startAxis));
		} else 
		{
			segments[0] = createStraightSegment(start, waypts[0], startAxis );
			int axis = 1 - startAxis;
			for(int i = 0; i < waypts.length - 1; i++) {
				segments[i + 1] = createStraightSegment( segments[i].getMEnd(), waypts[i + 1], axis );
				axis = getOppositeAxis(axis);
		}
		segments[segments.length - 2] = createStraightSegment( segments[segments.length -3].getMEnd(), end, axis);
		segments[segments.length - 1] = createStraightSegment(
					segments[segments.length - 2].getMEnd(), end, getSegmentAxis(endSide));
		}
		return Arrays.asList(segments);
	}
	//-----------------------------------------------------------
	class Segment
	{
		Point2D start, end;
		public Segment(Point2D a, Point2D b)
		{
			start = a;
			end = b;
		}
		public Point2D getMEnd() { return end;	}
		public Point2D getMStart() { return start;	}
		public Point2D getMCenter()	{ return new Point2D.Double((end.getX() + start.getX()) / 2., (end.getY() + start.getY()) / 2.); }
		double length() { return distance(end, start);	}
		public String toString() { return "(" + (int) getMStart().getX() + ", " +  (int) getMStart().getY() +  " -> " + (int) getMEnd().getX() + ", " +  (int) getMStart().getY() + ")"; }
	}
	
	protected Segment createStraightSegment(Point2D start, Point2D end, int axis) {
		double ex = end.getX();
		double ey = end.getY();
		if(axis == AXIS_X) 		ey = start.getY();
		else 					ex = start.getX();
		return new Segment(start, new Point2D.Double(ex, ey));
	}

	private int getOppositeAxis(int axis) {
		return axis == ConnectorShape.AXIS_X ? AXIS_Y : AXIS_X;
	}
	static private int getSide(double relX, double relY)
	{
			if (relX < 0 && Math.abs(relX) > Math.abs(relY))  		return ConnectorRestrictions.SIDE_WEST;
			if (relX > 0 && Math.abs(relX) > Math.abs(relY))  		return ConnectorRestrictions.SIDE_EAST;
			if (relY < 0)  		return ConnectorRestrictions.SIDE_NORTH;
			if (relY > 0)  		return ConnectorRestrictions.SIDE_SOUTH;
			return -1;
	}

	static private int getSegmentDirection(int side) {
		switch(side) {
		case ConnectorRestrictions.SIDE_EAST:
		case ConnectorRestrictions.SIDE_SOUTH: 		return 1;
		case ConnectorRestrictions.SIDE_NORTH:
		case ConnectorRestrictions.SIDE_WEST: 		return -1;
		}
		return 0;
	}

	static private int getSegmentAxis(int side) {
		switch(side) {
			case ConnectorRestrictions.SIDE_EAST:
			case ConnectorRestrictions.SIDE_WEST: 		return AXIS_X;
			case ConnectorRestrictions.SIDE_NORTH:
			case ConnectorRestrictions.SIDE_SOUTH: 		return AXIS_Y;
		}
		return 0;
	}
// R=RIGHT, L=LEFT, T=TOP, B=BOTTOM
// N=NORTH, E=EAST, S=SOUTH, W=WEST
	/* The number of connector for each side and relative position
		RN	RE	RS	RW
BLN		1	2	1	0
TLN		1	2	3	2

BLE		3	1	0	1
TLE		0	1	2	1

BLS		3	2	1	2
TLS		1	2	1	0

BLW		2	3	2	1
TLW		2	3	2	1
	There should be some logic behind this, but hey, it's Friday...
	(so we just hard code the array)
	
BUG:  	There should be some cases where 4 is returned !!  
	 */
	private int[][][] waypointNumbers;

	private int getNrWaypoints(int x, int y, int z) {
		waypointNumbers = new int[][][] {
			{	{ 1, 1 },	{ 2, 2 },	{ 1, 3 },	{ 0, 2 }	},
			{	{ 2, 0 }, 	{ 1, 1 }, 	{ 0, 2 }, 	{ 1, 1 },	},
			{	{ 3, 1 },	{ 2, 2 },	{ 1, 1 },	{ 2, 0 },	},
			{ 	{ 2, 2 },	{ 3, 3 },	{ 2, 2 },	{ 1, 1 },	}
		};
		return waypointNumbers[x][y][z];
	}

	/**
	 * Get the direction of the line on the x axis
	 * @param start The start point of the line
	 * @param end The end point of the line
	 * @return 1 if the direction is positive (from left to right),
	 * -1 if the direction is negative (from right to left)
	 */
	static int getDirectionX(Point2D start, Point2D end) {
		return (int)Math.signum(end.getX() - start.getX());
	}

	/**
	 * Get the direction of the line on the y axis
	 * @param start The start point of the line
	 * @param end The end point of the line
	 * @return 1 if the direction is positive (from top to bottom),
	 * -1 if the direction is negative (from bottom to top)
	 */
	protected int getDirectionY(Point2D start, Point2D end) {
		return (int)Math.signum(end.getY() - start.getY());
	}

	protected int getNrSegments(Point2D start, Point2D end, int startSide, int endSide) {

		boolean leftToRight = getDirectionX(start, end) > 0;

		Point2D left = leftToRight ? start : end;
		Point2D right = leftToRight ? end : start;
		boolean leftBottom = getDirectionY(left, right) < 0;

		int z = leftBottom ? 0 : 1;
		int x = leftToRight ? startSide : endSide;
		int y = leftToRight ? endSide : startSide;
		return getNrWaypoints(x, y, z) + 2;
	}

    protected Point2D fromLineCoordinate(double l, List<Segment> segments) 
    {
		double totalLength = getTotalLength(segments);
		double pixelsRemaining = totalLength * l;
		if (pixelsRemaining < 0) pixelsRemaining = 0;
		if (pixelsRemaining > totalLength) pixelsRemaining = totalLength;

		// count off each segment from pixelsRemaining, until there aren't enough pixels left
		Segment segment = null;
		double slength = 0.0;
		for(Segment s : segments) 
		{
			slength = s.length();
			segment = s;
			if (pixelsRemaining < slength) 		break; // not enough pixels left, we found our segment.
			pixelsRemaining -= slength;
		}

		//Find the location on the segment
		Point2D s = segment.getMStart();
		Point2D e = segment.getMEnd();

		// protection against division by 0
		if (slength == 0)
			return new Point2D.Double(s.getX(), s.getY());
			// start from s, in the direction of e, for pixelRemaining pixels.
		double deltax = e.getX() - s.getX();
		double deltay = e.getY() - s.getY();

		return new Point2D.Double(s.getX() + deltax / slength * pixelsRemaining,
				s.getY() + deltay / slength * pixelsRemaining );
		}


	/** @returns sum of the lengths of the segments */
	static private double getTotalLength (List<Segment> segments) 
	{
		double totLength = 0.0;
		for (Segment seg : segments)
			totLength += seg.length();
		return totLength;
	}
	//-----------------------------------------------
	static private Point2D centerPoint(Point2D start, Point2D end) 
	{
		return new Point2D.Double( (start.getX() + end.getX() ) / 2, (start.getY() + end.getY()) / 2 );
	}
	
	static private double distance(Point2D a, Point2D b)	
	{
		double dx = a.getX() - b.getX();
		double dy = a.getY() - b.getY();
		return (Math.sqrt(dx*dx + dy* dy));
	}

}
