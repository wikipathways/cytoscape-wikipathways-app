package org.wikipathways.cytoscapeapp.internal.io;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bridgedb.Xref;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.pathvisio.core.model.GraphLink;
import org.pathvisio.core.model.GraphLink.GraphIdContainer;
import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.model.ShapeType;
import org.pathvisio.core.model.StaticProperty;

public class GpmlToNetwork implements Converter {
	/**
	 * Maps a GPML pathway element to its representative CyNode in the network.
	 */
	final Map<GraphLink.GraphIdContainer, CyNode> nodes = new HashMap<GraphLink.GraphIdContainer, CyNode>();
	
	/**
	 * In Cytoscape, first the network topology is created (via
	 * CyNetwork.add{Node|Edge}), then the view objects are created. Once that's
	 * done, the network's visual style can be created (via
	 * View.setVisualProperty) once all the view objects exist (ensured by
	 * CyEventHelper.flushPayloadEvents).
	 * 
	 * However, while we're reading GPML, we need to create the network's visual
	 * style while we are creating the network toplogy. Otherwise we'd have to
	 * read the GPML twice, once for topology and again for the visual style.
	 * 
	 * How do we get around this problem? While we're reading GPML, we create
	 * the network topology and store our desired visual style in DelayedVizProp
	 * objects (defined below). After we finish reading GPML, we ensure that
	 * view objects have been created for all our new nodes and edges (via
	 * CyEventHelper.flushPayloadEvents). Finally we apply the visual style
	 * stored in the DelayedVizProp objects.
	 */
	final List<DelayedVizProp> delayedVizProps = new ArrayList<DelayedVizProp>();

  final CyEventHelper eventHelper;
	final Pathway pathway;
	final CyNetwork network;
	
	private List<PathwayElement> edges;
	private List<MAnchor> anchors;
	
	/**
	 * Create a converter from the given pathway and store it in the given
	 * network. Constructing this object will not start the conversion and will
	 * not modify the given network in any way.
	 */
	public GpmlToNetwork(final CyEventHelper eventHelper, final Pathway pathway, final CyNetwork network) {
		this.eventHelper = eventHelper;
		this.pathway = pathway;
		this.network = network;
	}
	
	private Boolean unconnectedLines = false;

	/**
	 * Convert the pathway given in the constructor.
	 */
	public ViewBuilder convert() {
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).createColumn("GraphID", String.class, false);
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).createColumn("GeneID", String.class, false);
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).createColumn("Datasource", String.class, false);
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).createColumn("WP.type", String.class, false);
		network.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS).createColumn("WP.type", String.class, false);

		// convert by each pathway element type
		System.out.println("convert data nodes");
		convertDataNodes();
		System.out.println("convert groups");
		convertGroups();
		System.out.println("convert labels");
		convertLabels();
		
		System.out.println("find edges");
		findEdges();
		System.out.println("convert anchors");
		convertAnchors();
		System.out.println("convert lines");
		convertLines();


		// clear our data structures just to be nice to the GC
		nodes.clear();

		return new ViewBuilder() {
			public void build(final CyNetworkView cyNetView) {
				eventHelper.flushPayloadEvents(); // guarantee that all node and edge views have been created
				DelayedVizProp.applyAll(cyNetView, delayedVizProps); // apply our visual style
				delayedVizProps.clear();
			}
		};
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
					unconnectedLines = true;
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
	 * ======================================================== Static property
	 * conversion ========================================================
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
		GPML_ARROW_SHAPES.put("mim-binding", ArrowShapeVisualProperty.ARROW);
		GPML_ARROW_SHAPES.put("mim-conversion", ArrowShapeVisualProperty.ARROW);
		GPML_ARROW_SHAPES.put("mim-modification",ArrowShapeVisualProperty.ARROW);
		GPML_ARROW_SHAPES.put("mim-catalysis", ArrowShapeVisualProperty.CIRCLE);
		GPML_ARROW_SHAPES.put("mim-inhibition", ArrowShapeVisualProperty.T);
		GPML_ARROW_SHAPES.put("mim-covalent-bond", ArrowShapeVisualProperty.T);
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
			delayedVizProps.add(new DelayedVizProp(node,BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0, true));
		}
	}

	/*
	 * ======================================================== Data nodes
	 * ========================================================
	 */

	private static Map<StaticProperty, String> dataNodeStaticProps = new HashMap<StaticProperty, String>();
	static {
		dataNodeStaticProps.put(StaticProperty.GRAPHID, "GraphID");
		dataNodeStaticProps.put(StaticProperty.TEXTLABEL, CyNetwork.NAME);
		dataNodeStaticProps.put(StaticProperty.TYPE, "WP.type");
	}

	private Map<Xref, PathwayElement> elements;

	private void convertDataNodes() {
		dataNodeStaticProps.put(StaticProperty.GENEID, "GeneID");
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
			network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).getRow(node.getSUID()).set("Datasource", dataNode.getDataSource().getFullName());
		}
		convertStaticProps(dataNode, dataNodeStaticProps,network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS),node.getSUID());
		convertShapeTypeNone(node, dataNode);
		nodes.put(dataNode, node);
	}

	/*
	 * ======================================================== Groups
	 * ========================================================
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

		for (final PathwayElement elem : pathway.getGroupElements(group.getGroupId())) {
			final CyNode node = nodes.get(elem);
			if (node == null)
				continue;
			network.addEdge(node, groupNode, false);
		}
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).getRow(groupNode.getSUID()).set("WP.type", "Group");
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).getRow(groupNode.getSUID()).set("GraphID", group.getGraphId());

		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_FILL_COLOR, Color.blue, true));
		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0, true));
		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_WIDTH, 5.0, true));
		delayedVizProps.add(new DelayedVizProp(groupNode,BasicVisualLexicon.NODE_HEIGHT, 5.0, true));
		
	}

	/*
	 * ======================================================== Labels
	 * ========================================================
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
		// TODO: refactor this as an annotation 
		// comment Tina: not sure if they can all be replaced by annotations because they are often connected with data nodes
		final CyNode node = network.addNode();
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).getRow(node.getSUID()).set("WP.type", "Label");

		convertStaticProps(label, labelStaticProps, network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS), node.getSUID());
		convertShapeTypeNone(node, label);
		nodes.put(label, node);
	}

	/*
	 * ======================================================== Anchors
	 * ========================================================
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
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).getRow(node.getSUID()).set("GraphID", anchor.getGraphId());
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).getRow(node.getSUID()).set("WP.type", "Anchor");
	}

	/*
	 * ======================================================== Lines
	 * ========================================================
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

	private void assignEdgeVizStyle(final CyEdge edge, final PathwayElement line, final boolean isFirst, final boolean isLast) {
		if (edge == null)
			return;

		network.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS).getRow(edge.getSUID()).set("WP.type", "Line");

		if (isFirst)
			convertViewStaticProp(line, edge, StaticProperty.STARTLINETYPE, BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE);
		if (isLast)
			convertViewStaticProp(line, edge, StaticProperty.ENDLINETYPE, BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
	}
}
