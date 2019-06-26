package org.wikipathways.cytoscapeapp.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bridgedb.Xref;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.LineType;
import org.pathvisio.core.model.GraphLink;
import org.pathvisio.core.model.GraphLink.GraphIdContainer;
import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.model.ShapeType;
import org.pathvisio.core.model.StaticProperty;
import org.wikipathways.cytoscapeapp.impl.GpmlToPathway.BasicExtracter;
import org.wikipathways.cytoscapeapp.impl.GpmlToPathway.BasicTableStore;
import org.wikipathways.cytoscapeapp.impl.GpmlToPathway.Extracter;
import org.wikipathways.cytoscapeapp.impl.GpmlToPathway.TableStore;

public class GpmlToNetwork {
	/**
	 * Maps a GPML pathway element to its representative CyNode in the network.
	 */
	final Map<GraphLink.GraphIdContainer, CyNode> nodes = new HashMap<GraphLink.GraphIdContainer, CyNode>();

	List<DelayedVizProp> delayedVizProps = new ArrayList<DelayedVizProp>();

  final CyEventHelper eventHelper;
	final Pathway pathway;
	final CyNetwork network;
	
	private List<PathwayElement> edges;
	private List<MAnchor> anchors;
	private CyTable nodeTable;
	private CyTable edgeTable;
	/**
	 * Create a converter from the given pathway and store it in the given
	 * network. Constructing this object will not start the conversion and will
	 * not modify the given network in any way.
	 */
	public GpmlToNetwork(final CyEventHelper eventHelper, final Pathway pathway, final CyNetwork network) {
		this.eventHelper = eventHelper;
		this.pathway = pathway;
		this.network = network;
		nodeTable = network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
		edgeTable = network.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
	}
	/**
	 * Convert the pathway given in the constructor.
	 */
	public List<DelayedVizProp> convert() {
		 delayedVizProps = new ArrayList<DelayedVizProp>();
		 nodeTable.createColumn("GraphID", String.class, false);
		nodeTable.createColumn("Type", String.class, false);
		nodeTable.createColumn("XrefId", String.class, false);
		nodeTable.createColumn("XrefDatasource", String.class, false);
		nodeTable.createColumn("Color", String.class, false);
		nodeTable.createColumn("Border Width", Double.class, false);
		nodeTable.createColumn("Node Size", Double.class, false);
		nodeTable.createColumn("Label Font Size", Double.class, false);
		
		edgeTable.createColumn("WP.type", String.class, false);
		edgeTable.createColumn("Width", Double.class, false);
		edgeTable.createColumn("LineStyle", String.class, false);
		edgeTable.createColumn("Source Arrow Shape", String.class, false);
		edgeTable.createColumn("Target Arrow Shape", String.class, false);
		edgeTable.createColumn("Color", String.class, false);

		// convert by each pathway element type
//		System.out.println("convert data nodes");
		convertDataNodes();
//		System.out.println("convert groups");
		convertGroups();
//		System.out.println("convert labels");
		convertLabels();
		
//		System.out.println("find edges");
		findEdges();
//		System.out.println("convert anchors");
		convertAnchors();
//		System.out.println("convert lines");
		convertLines();


		// clear our data structures just to be nice to the GC
		nodes.clear();

		return delayedVizProps;
	}

	/**
	 * filters out all edges that are not connected in the pathway
	 * only those edges and anchor nodes will be created
	 * important in network view
	 */
	private void findEdges() {
		Map<String, MLine> map = new HashMap<String, MLine>();
		edges = new ArrayList<PathwayElement>();
		anchors = new ArrayList<MAnchor>();
		for (PathwayElement elem : pathway.getDataObjects()) {
			if (elem.getObjectType().equals(ObjectType.LINE)) {
				if(isEdge(elem)) {
					final MLine line = (MLine) elem;
					final String startRef = line.getMStart().getGraphRef();
					final String endRef = line.getMEnd().getGraphRef();
					edges.add(line);
					map.put(startRef, line);
					map.put(endRef, line);
				} else {
//					unconnectedLines = true;
				}
			}
		}
		
		for (PathwayElement elem : pathway.getDataObjects()) {
			if (elem.getObjectType().equals(ObjectType.LINE)) {
				final MLine line = (MLine) elem;
				if(edges.contains(line)) {
					for(MAnchor a : line.getMAnchors()) {
						if(map.containsKey(a.getGraphId())) {
							anchors.add(a);
						}
	 				}
				}
			}
		}
	}
	
	private boolean isEdge(PathwayElement e) {
		GraphIdContainer start = pathway.getGraphIdContainer(e.getMStart().getGraphRef());
		GraphIdContainer end = pathway.getGraphIdContainer(e.getMEnd().getGraphRef());
		return isNode(start) && isNode(end);
	}

	private boolean isNode(GraphIdContainer idc) {
		if(idc instanceof MAnchor) {
			//only valid if the parent line is an edge
			return isEdge(((MAnchor)idc).getParent());
		} else if(idc instanceof PathwayElement) {
			ObjectType ot = ((PathwayElement)idc).getObjectType();
			return
				ot == ObjectType.DATANODE ||
				ot == ObjectType.GROUP ||
				ot == ObjectType.LABEL;
		} else {
			return false;
		}
	}

	/*
	 * ================================ Static property conversion =====================
	 */

	/**
	 * Converts a GPML static property to a Cytoscape visual property.
	 */
	static interface StaticPropConverter<S, V> {
		public V convert(S staticPropValue);
	}

	/**
	 * Copies a GPML pathway element's static properties to a CyTable's row of
	 * elements.
	 * 
	 * @param elem
	 *            The GPML pathway element from which static properties are
	 *            copied.
	 * @param staticProps
	 *            A map of the GPML pathway element's static properties and
	 *            their corresponding Cytoscape CyTable column names.
	 * @param table
	 *            A CyTable to which static properties are copied; this table
	 *            must have the columns specified in {@code staticProps} already
	 *            created with the correct types.
	 * @param key
	 *            The row in the CyTable to which static properties are copied.
	 */
	private void convertStaticProps(final PathwayElement elem,final Map<StaticProperty, String> staticProps, final CyTable table,final Object key) {
		for (final Map.Entry<StaticProperty, String> staticPropEntry : staticProps.entrySet()) {
			final StaticProperty staticProp = staticPropEntry.getKey();
			Object value = elem.getStaticProperty(staticProp);
			if (value == null)
				continue;
			if (value instanceof Color)
				value = colorToString((Color)value);
			final String column = staticPropEntry.getValue();
			table.getRow(key).set(column, value);
		}
	}

	/**
	 * GPML start/end line types and their corresponding Cytoscape ArrowShape
	 * names.
	 */
	static Map<String, ArrowShape> GPML_ARROW_SHAPES = new HashMap<String, ArrowShape>();
	static {
		GPML_ARROW_SHAPES.put("Arrow", ArrowShapeVisualProperty.DELTA);
		GPML_ARROW_SHAPES.put("TBar", ArrowShapeVisualProperty.T);
		GPML_ARROW_SHAPES.put("Line", ArrowShapeVisualProperty.NONE);
		GPML_ARROW_SHAPES.put("mim-binding", ArrowShapeVisualProperty.ARROW);
		GPML_ARROW_SHAPES.put("mim-conversion", ArrowShapeVisualProperty.ARROW);
		GPML_ARROW_SHAPES.put("mim-modification",ArrowShapeVisualProperty.ARROW);
		GPML_ARROW_SHAPES.put("mim-catalysis", ArrowShapeVisualProperty.CIRCLE);
		GPML_ARROW_SHAPES.put("mim-inhibition", ArrowShapeVisualProperty.T);
		GPML_ARROW_SHAPES.put("mim-covalent-bond", ArrowShapeVisualProperty.CROSS_DELTA);
		GPML_ARROW_SHAPES.put("mim-branching-right", ArrowShapeVisualProperty.CROSS_OPEN_DELTA);
		GPML_ARROW_SHAPES.put("mim-transcription-translation", ArrowShapeVisualProperty.DELTA);
		GPML_ARROW_SHAPES.put("mim-cleavage", ArrowShapeVisualProperty.DIAMOND);
		GPML_ARROW_SHAPES.put("mim-gap", ArrowShapeVisualProperty.DELTA);
		GPML_ARROW_SHAPES.put("mim-modification", ArrowShapeVisualProperty.DELTA);
		GPML_ARROW_SHAPES.put("mim-conversion", ArrowShapeVisualProperty.DELTA);
		GPML_ARROW_SHAPES.put("mim-necessary-stimulation", ArrowShapeVisualProperty.CROSS_OPEN_DELTA);
	}
 
	/**
	 * Converts a GPML start/end line type to a Cytoscape ArrowShape object.
	 * This uses the {@code GPML_ARROW_NAME_TO_CYTOSCAPE} map to do the
	 * converstion.
	 */
	private static StaticPropConverter<org.pathvisio.core.model.LineType, ArrowShape> ARROW_SHAPE_CONVERTER = new StaticPropConverter<org.pathvisio.core.model.LineType, ArrowShape>() {
		public ArrowShape convert(org.pathvisio.core.model.LineType lineType) {
			final String gpmlArrowName = lineType.getName();
			final ArrowShape arrowShape = GPML_ARROW_SHAPES.get(gpmlArrowName);
			if (arrowShape == null)
				return ArrowShapeVisualProperty.NONE;
			return arrowShape;
		}
	};


	/**
	 * A map of converters from GPML static properties to Cytoscape visual
	 * properties.
	 */
	private static Map<StaticProperty, StaticPropConverter> VIZ_STATIC_PROP_CONVERTERS = new HashMap<StaticProperty, StaticPropConverter>();
	static {
		VIZ_STATIC_PROP_CONVERTERS.put(StaticProperty.STARTLINETYPE,ARROW_SHAPE_CONVERTER);
		VIZ_STATIC_PROP_CONVERTERS.put(StaticProperty.ENDLINETYPE,ARROW_SHAPE_CONVERTER);
	}


	/**
	 * For a GPML pathway element, convert one of its static properties to a
	 * Cytoscape View's VisualProperty value and store it in
	 * {@code delayedVizProps}. If {@code staticProp} is a key in
	 * {@code STATIC_PROP_CONVERTERS}, its converter will be invoked before it
	 * is set as a visual property value.
	 * 
	 * @param elem
	 *            The GPML pathway element that contains a static property.
	 * @param netObj
	 *            Either a Cytoscape CyNode or CyEdge whose corresponding View
	 *            should have a new VisualProperty.
	 * @param staticProp
	 *            The static property whose value is to be converted to a
	 *            VisualProperty value.
	 * @param vizProp
	 *            The visual property to which to convert the static property's
	 *            value.
	 */
	private void convertViewStaticProp(final PathwayElement elem, final CyIdentifiable netObj, final StaticProperty staticProp, final VisualProperty vizProp) {
		Object value = elem.getStaticProperty(staticProp);
		if (value == null)
			return;
		if (VIZ_STATIC_PROP_CONVERTERS.containsKey(staticProp)) {
			value = VIZ_STATIC_PROP_CONVERTERS.get(staticProp).convert(value);
		}
		delayedVizProps.add(new DelayedVizProp(netObj, vizProp, value, true));
	}



	/**
	 * Overrides a node's border width by setting it to zero if the pathway
	 * element has a shape of type NONE.
	 */
	private void convertShapeTypeNone(final CyNode node, final PathwayElement elem) {
		if (ShapeType.NONE.equals(elem.getShapeType())) {
			delayedVizProps.add(new DelayedVizProp(node,BasicVisualLexicon.NODE_BORDER_WIDTH, 2.0, true));
			delayedVizProps.add(new DelayedVizProp(node,BasicVisualLexicon.NODE_BORDER_TRANSPARENCY, 0, true));
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

	  static LineType getCyNodeLineType(final String displayName) {
	    for (final LineType lineType : ((DiscreteRange<LineType>) BasicVisualLexicon.NODE_BORDER_LINE_TYPE.getRange()).values()) 
	    {
	      final String lineTypeName = lineType.getDisplayName();
	      if (displayName.equals(lineTypeName)) 
	        return lineType;
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

	  static Map<String,Double> PV_BORDER_WIDTH_MAP = new HashMap<String,Double>();
	  static {
		  PV_BORDER_WIDTH_MAP.put("Anchor",  0.0);
		  PV_BORDER_WIDTH_MAP.put("Group",  0.0);
		  PV_BORDER_WIDTH_MAP.put("Label",  0.0);
		  }

	  static Map<String,Double> PV_SIZE_MAP = new HashMap<String,Double>();
	  static {
		  PV_SIZE_MAP.put("Anchor",  1.0);
		  PV_SIZE_MAP.put("Group",  25.0);
		  PV_SIZE_MAP.put("Label",  25.0);
		  }


	  static class BasicVizTableStore extends BasicTableStore implements VizTableStore {
//	    public static final VizTableStore NODE_ROTATION         = new BasicVizTableStore("Rotation", Double.class,        BasicExtracter.ROTATION,                        BasicVisualLexicon.NODE_ROTATION);

//			public static final VizTableStore NODE_SIZE            = new BasicVizTableStore("Size", Double.class,     BasicExtracter.WIDTH,                           BasicVisualLexicon.NODE_WIDTH);
		public static final VizTableStore NODE_SIZE            = new BasicVizTableStore("Type", BasicExtracter.NODE_TYPE,  PV_SIZE_MAP,                           BasicVisualLexicon.NODE_SIZE);
	    public static final VizTableStore NODE_FILL_COLOR       = new BasicVizTableStore("FillColor",             BasicExtracter.FILL_COLOR_STRING,               BasicVisualLexicon.NODE_FILL_COLOR);
	    public static final VizTableStore NODE_COLOR            = new BasicVizTableStore("Color",                 BasicExtracter.COLOR_STRING,                    BasicVisualLexicon.NODE_LABEL_COLOR, BasicVisualLexicon.NODE_BORDER_PAINT);
//	    public static final VizTableStore NODE_BORDER_STYLE     = new BasicVizTableStore("BorderStyle",           BasicExtracter.LINE_STYLE_NAME, PV_LINE_STYLE_MAP, BasicVisualLexicon.NODE_BORDER_LINE_TYPE);
	    public static final VizTableStore NODE_LABEL_SIZE       = new BasicVizTableStore("Label Font Size", Double.class, BasicExtracter.FONT_SIZE,                       BasicVisualLexicon.NODE_LABEL_FONT_SIZE);
//	    public static final VizTableStore NODE_BORDER_THICKNESS = new BasicVizTableStore("Border Width", Double.class, BasicExtracter.NODE_LINE_THICKNESS,             BasicVisualLexicon.NODE_BORDER_WIDTH);
	    public static final VizTableStore NODE_BORDER_THICKNESS = new BasicVizTableStore("Type", BasicExtracter.NODE_TYPE,  PV_BORDER_WIDTH_MAP,           BasicVisualLexicon.NODE_BORDER_WIDTH);
	    
	    public static final VizTableStore EDGE_COLOR            = new BasicVizTableStore("Color",                 BasicExtracter.COLOR_STRING,                    BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
	    public static final VizTableStore EDGE_LINE_STYLE       = new BasicVizTableStore("LineStyle",             BasicExtracter.LINE_STYLE_NAME, PV_LINE_STYLE_MAP, BasicVisualLexicon.EDGE_LINE_TYPE);
	    public static final VizTableStore EDGE_LINE_THICKNESS   = new BasicVizTableStore("Width", Double.class,   BasicExtracter.EDGE_LINE_THICKNESS,             BasicVisualLexicon.EDGE_WIDTH);
	    public static final VizTableStore EDGE_END_ARROW        = new BasicVizTableStore("Target Arrow Shape",    BasicExtracter.END_ARROW_STYLE, PV_ARROW_MAP,   BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
	    public static final VizTableStore EDGE_START_ARROW        = new BasicVizTableStore("Source Arrow Shape",    BasicExtracter.START_ARROW_STYLE, PV_ARROW_MAP,   BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE);
	    

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
	  
	  // this is the list of properties that will be pass thru maps in our defined style
	  public static List<VizTableStore> getAllVizTableStores() {
	    return Arrays.asList(
//	    	      BasicVizTableStore.NODE_ROTATION,
	      BasicVizTableStore.NODE_SIZE,
//	      BasicVizTableStore.NODE_FILL_COLOR,
//	      BasicVizTableStore.NODE_LABEL_SIZE,
	      BasicVizTableStore.NODE_COLOR,
	      BasicVizTableStore.NODE_BORDER_THICKNESS,
	      BasicVizTableStore.EDGE_COLOR,
	      BasicVizTableStore.EDGE_LINE_STYLE,
	      BasicVizTableStore.EDGE_LINE_THICKNESS,
	      BasicVizTableStore.EDGE_START_ARROW,
	      BasicVizTableStore.EDGE_END_ARROW
	      );
	  }
	/*
	 * ================================== Data nodes =====================================
	 */

	private static Map<StaticProperty, String> dataNodeStaticProps = new HashMap<StaticProperty, String>();
	static {
		dataNodeStaticProps.put(StaticProperty.GRAPHID, "GraphID");
		dataNodeStaticProps.put(StaticProperty.TEXTLABEL, CyNetwork.NAME);
		dataNodeStaticProps.put(StaticProperty.TYPE, "Type");
		dataNodeStaticProps.put(StaticProperty.COLOR, "Color");
		dataNodeStaticProps.put(StaticProperty.WIDTH, "Node Size");
		dataNodeStaticProps.put(StaticProperty.LINETHICKNESS, "Border Width");
	}

	private Map<Xref, PathwayElement> elements;

	private void convertDataNodes() {
		dataNodeStaticProps.put(StaticProperty.GENEID, "XrefId");
		elements = new HashMap<Xref, PathwayElement>();
		for (final PathwayElement elem : pathway.getDataObjects()) {
			if (elem.getObjectType().equals(ObjectType.DATANODE)) {
				if (elements.containsKey(elem.getXref())) {
					nodes.put(elem, nodes.get(elements.get(elem.getXref())));
				} else {
					convertDataNode(elem);
					if (elem.getXref() != null && !elem.getXref().getId().equals("")) {
						elements.put(elem.getXref(), elem);
					}
				}
			}
		}
		dataNodeStaticProps.remove(StaticProperty.GENEID);
	}

	private void convertDataNode(final PathwayElement dataNode) {
		final CyNode node = network.addNode();
		if (dataNode.getDataSource() != null && dataNode.getDataSource().getFullName() != null) {
			nodeTable.getRow(node.getSUID()).set("XrefDatasource", dataNode.getDataSource().getFullName());
		}
		convertStaticProps(dataNode, dataNodeStaticProps, nodeTable, node.getSUID());
		convertShapeTypeNone(node, dataNode);
		nodes.put(dataNode, node);
	}

	/*
	 * ==================================== Groups  ==================================
	 */
	
	private void convertGroups() {
		for (final PathwayElement elem : pathway.getDataObjects()) 
			if (elem.getObjectType().equals(ObjectType.GROUP))
				convertGroup(elem);
	}

	private void convertGroup(final PathwayElement group) {
		final CyNode groupNode = network.addNode();
		nodes.put(group, groupNode);

		String groupName = "group";
		for (final PathwayElement elem : pathway.getGroupElements(group.getGroupId())) {
			final CyNode node = nodes.get(elem);
			if (node == null)
				continue;
			network.addEdge(node, groupNode, false);
			groupName = groupName + "_" + elem.getTextLabel();
		}
		nodeTable.getRow(groupNode.getSUID()).set("Type", "Group");
		nodeTable.getRow(groupNode.getSUID()).set("GraphID", group.getGraphId());
		nodeTable.getRow(groupNode.getSUID()).set("name", groupName);

		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_FILL_COLOR, Color.blue, true));
		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_BORDER_WIDTH, 1.0, true));
		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_WIDTH, 10.0, true));
		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_HEIGHT, 10.0, true));
	}

	/*
	 * ============================================= Labels  =============================================
	 */

	private static Map<StaticProperty, String> labelStaticProps = new HashMap<StaticProperty, String>();
	static {
		labelStaticProps.put(StaticProperty.TEXTLABEL, CyNetwork.NAME);
		labelStaticProps.put(StaticProperty.GRAPHID, "GraphID");
	}

	private void convertLabels() {
		for (final PathwayElement elem : pathway.getDataObjects()) {
			if (elem.getObjectType().equals(ObjectType.LABEL) && isConnected(elem)) {
				convertLabel(elem);
			}
		}
	}

	private boolean isConnected(PathwayElement element) {
		for (final PathwayElement elem : pathway.getDataObjects()) {
			if (elem.getObjectType().equals(ObjectType.LINE) || elem.getObjectType().equals(ObjectType.GRAPHLINE)) {
				MLine line = (MLine) elem;
				String startRef = line.getMStart().getGraphRef();
				String endRef = line.getMEnd().getGraphRef();

				if (element.getGraphId().equals(startRef) || element.getGraphId().equals(endRef)) 
					return true;
			}
		}
		return false;
	}

	private void convertLabel(final PathwayElement label) {
		final CyNode node = network.addNode();
		nodeTable.getRow(node.getSUID()).set("Type", "Label");
		convertStaticProps(label, labelStaticProps, nodeTable, node.getSUID());
		convertShapeTypeNone(node, label);
		nodes.put(label, node);
	}

	/*
	 * =================================== Anchors==========================================
	 */
	private void convertAnchors() {
		for(MAnchor anchor : anchors) {
			convertAnchor(anchor);
		}
	}
	
	private void assignAnchorVizStyle(final CyNode node, final Color color) {
		delayedVizProps.add(new DelayedVizProp(node,BasicVisualLexicon.NODE_FILL_COLOR, color, true));
		delayedVizProps.add(new DelayedVizProp(node,BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0, true));
		delayedVizProps.add(new DelayedVizProp(node,BasicVisualLexicon.NODE_WIDTH, 5.0, true));
		delayedVizProps.add(new DelayedVizProp(node,BasicVisualLexicon.NODE_HEIGHT, 5.0, true));
	}
	
	private void convertAnchor(MAnchor anchor) {
		final CyNode node = network.addNode();
		nodes.put(anchor, node);
		assignAnchorVizStyle(node, Color.gray);
		nodeTable.getRow(node.getSUID()).set("GraphID", anchor.getGraphId());
		nodeTable.getRow(node.getSUID()).set("Type", "Anchor");
	}

	/*
	 * =========================== Lines ================================
	 */

	private static Map<StaticProperty, String> lineStaticProps = new HashMap<StaticProperty, String>();
	static {
		lineStaticProps.put(StaticProperty.TYPE, "WP.type");
	}
	
	private void convertLines() {
		for(PathwayElement line : edges) {
			convertLine(line);
		}
	}

	private void convertLine(final PathwayElement elem) {
		final MLine line = (MLine) elem;
		final String startRef = line.getMStart().getGraphRef();
		final String endRef = line.getMEnd().getGraphRef();
		
		// don't draw unconnected lines without anchors
		boolean createLine = true;
		if (startRef == null || endRef == null) {
			if (line.getMAnchors().size() == 0) {
				createLine = false;
			}
		}
		
		if (createLine) {
			CyNode startNode = nodes.get(pathway.getGraphIdContainer(startRef));
			if (startNode == null) {
				System.out.println("ERROR");
				startNode = network.addNode();
				assignAnchorVizStyle(startNode, Color.white);
			}
			CyNode endNode = nodes.get(pathway.getGraphIdContainer(endRef));
			if (endNode == null) {
				System.out.println("ERROR");
				endNode = network.addNode();
				assignAnchorVizStyle(endNode, Color.white);
			}

			final MAnchor[] anchors = elem.getMAnchors().toArray(new MAnchor[0]);
			if (anchors.length > 0) {
				List<MAnchor> existingAnchors = new ArrayList<PathwayElement.MAnchor>();
				for (int i = 0; i < anchors.length; i++) {
					if(nodes.get(anchors[i]) != null) {
						existingAnchors.add(anchors[i]);
					}
				}
				if(existingAnchors.size() > 0) {
					final CyEdge firstEdge = network.addEdge(startNode,nodes.get(existingAnchors.get(0)), true);
					assignEdgeVizStyle(firstEdge, line, true, false);
					
					for (int i = 1; i < existingAnchors.size(); i++) {
						final CyEdge edge = network.addEdge(nodes.get(existingAnchors.get(i-1)), nodes.get(existingAnchors.get(i)),true);
						assignEdgeVizStyle(edge, line, false, false);
					}
					
					final CyEdge lastEdge = network.addEdge(nodes.get(existingAnchors.get(existingAnchors.size()-1)), endNode, true);
					assignEdgeVizStyle(lastEdge, line, false, true);
				}
			} else {
				final CyEdge edge = network.addEdge(startNode, endNode, true);
				assignEdgeVizStyle(edge, line, true, true);
			}
		}

	}

	private void assignEdgeVizStyle(final CyEdge edge, final PathwayElement line, final boolean isFirst, final boolean isLast) 
	{
		if (edge == null)  return;
		CyRow edgeRow = edgeTable.getRow(edge.getSUID());
		edgeRow.set("WP.Type", "" + line.getEndLineType());
		edgeRow.set("Source Arrow Shape", "" + line.getStartLineType());
		edgeRow.set("Target Arrow Shape", "" + line.getEndLineType());
		edgeRow.set("Width", (line.getLineThickness() + 0.01 * Math.random()));  // TODO hack to give a sortable value
		edgeRow.set("LineStyle", line.getLineStyle() == 0 ? "Solid" : "Dots");
		edgeRow.set("Color", colorToString(line.getColor()));

		 
	    // #107  Edge Table is missing name, shared name, interaction, shared interaction
	    CyNode src = edge.getSource();
	    CyNode targ = edge.getTarget();
	    CyRow srcRow = nodeTable.getRow(src.getSUID());
	    CyRow targRow = nodeTable.getRow(targ.getSUID());
	    String srcName = srcRow.get("name", String.class);
	    String interaction = edgeRow.get("Target Arrow Shape", String.class);
	    String targName = targRow.get("name", String.class);
	    String edgeName = srcName + " (" + interaction + ") " + targName;
	    edgeRow.set("name", edgeName);
	    edgeRow.set("shared name", edgeName);
	    edgeRow.set("interaction", interaction);
	    edgeRow.set("shared interaction", interaction);
		if (isFirst)
			convertViewStaticProp(line, edge, StaticProperty.STARTLINETYPE, BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE);
		if (isLast)
			convertViewStaticProp(line, edge, StaticProperty.ENDLINETYPE, BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
	}
	private Object colorToString(Color color) {
		String str = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
		
		return str;
	}
}