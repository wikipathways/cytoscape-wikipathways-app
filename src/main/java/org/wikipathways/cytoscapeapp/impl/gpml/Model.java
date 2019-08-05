package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.Range;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.Bend;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.Handle;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.wikipathways.cytoscapeapp.CellShapes;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.WPManager;

public class Model
{
/*
 *  Model - the set of record lists	
 *  
 *  We need to keep track of species, nodes, edges, genesets, pathways, references, comments, groups
 */
	private Controller controller;
	public Controller getController() { return controller; } 
	private Map<String, DataNode> dataNodeMap = new HashMap<String, DataNode>();
	private int nodeCounter = 0;
	public Collection<DataNode> getNodes()			{ return dataNodeMap.values();	}
	public Map<String, DataNode> getDataNodeMap() {		return dataNodeMap;	}

	private Map<String, DataNodeGroup> groupMap = new HashMap<String, DataNodeGroup> ();
	public Collection<DataNodeGroup> getGroups()			{ return groupMap.values();	}
	public Map<String, DataNodeGroup> getGroupMap() {		return groupMap;	}

	private Map<String, DataNodeState> stateMap = new HashMap<String, DataNodeState>();
	public Collection<DataNodeState> getStates()			{ return stateMap.values();	}
	public Map<String, DataNodeState> getStateMap() {		return stateMap;	}
	public void addState(String graphRef, DataNodeState statenode) {		stateMap.put(graphRef, statenode);	}

	public Collection<Interaction> getEdges()			{ return interactionMap.values();	}
	private Map<String, Interaction> interactionMap = new HashMap<String, Interaction>();
	public Map<String, Interaction> getInteractions()			{ return interactionMap;	}
	public List<Interaction> getInteractionList(String nodeId)			
	{ 
		List<Interaction> hits = new ArrayList<Interaction>();
		for (Interaction e : interactionMap.values())
			if (e.touches(nodeId))
					hits.add(e);
		return hits;	
	}
	public Interaction getInteraction(String edgeId)	{ 	return interactionMap.get(edgeId);	}
	
	private Map<String, DataNode> shapes = new HashMap<String,DataNode>();
	public Map<String, DataNode> getShapes()	{ return shapes; }
	public DataNode findShape(String s ) 		{ return shapes.get(s);	}
	public void addShape(DataNode s ) 			{ shapes.put(s.getId(),s);	}

	private Map<String, DataNode> labels = new HashMap<String,DataNode>();
	public Map<String, DataNode> getLabels()	{ return labels; }
	public DataNode findLabel(Integer s ) 		{ return labels.get(s);	}
	public void addLabel(DataNode d)			{ labels.put(d.getId(),  d); }


	private String title = "PathVisio Mockup";
	public void setTitle(String val) {		title = val;		}
	public String getTitle() 		{		return title;		}
	
	WPManager       manager;
	private  CyNetworkView 	networkView = null;	
	  private CyNetwork cyNet = null;
		private CyTable cyNetworkTbl = null;
		private CyTable cyNodeTbl = null;
		private CyTable cyEdgeTbl = null;
		private CyEventHelper helper = null;
		private GpmlConversionMethod method = GpmlConversionMethod.PATHWAY;
	// **-------------------------------------------------------------------------------

	public Model()
	{
//		controller = ct;
	}
	
	public void writePathway(WPManager mgr, CyNetwork net, CyEventHelper ev, GpmlConversionMethod meth)
	{
		cyNet = net;
		manager = mgr;
		helper = ev;
		method = meth;
		networkView = manager.getNetworkViewMgr().getNetworkViews(cyNet).iterator().next();
		cyNetworkTbl = cyNet.getTable(CyNetwork.class, CyNetwork.DEFAULT_ATTRS);
		cyNodeTbl = cyNet.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
		cyEdgeTbl = cyNet.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
//		MIMShapes.registerShapes();
		manager.setUpTableRefs(cyNet);
		writeNetworkTable();
		writeNodeTable();
		writeAnchors();
		addGroupsToNodeTable();
		writeEdgeTable();
		boolean annot = true;
		if (method == GpmlConversionMethod.PATHWAY && annot)
			writeAnnotations();
	}
	//--------------------------------------------------------------------------------
	private void writeNetworkTable() {
		String organism = getSpecies().common();
		CyRow row = cyNet.getRow(cyNet);
		final String name = getComment("Name");
		String title = name + " - " + organism;
		final String description = getComment("WikiPathways-description");
		final String nonConflictingName = name;  //netNaming.getSuggestedNetworkTitle(name);
		row.set(CyNetwork.NAME, nonConflictingName);
		addNetworkTableColumns(row, "organism", organism, "description", description, "title", title);
		if (cyNet instanceof CySubNetwork) {
			final CyRootNetwork root = ((CySubNetwork) cyNet).getRootNetwork();
			root.getRow(root).set(CyNetwork.NAME, nonConflictingName);
		}
	}

	private void addNetworkTableColumns(CyRow row, String... strs)
	{
		row.getAllValues();
		int len = strs.length;
		for (int i=0; i<len; i+=2)
		{
			CyColumn col = cyNetworkTbl.getColumn(strs[i]);
			if (col == null)
				cyNetworkTbl.createColumn(strs[i], String.class, false);
			row.set(strs[i],  strs[i+1]);
		}
	}

	//--------------------------------------------------------------------------------
	private void writeNodeTable() {
		for (String i : dataNodeMap.keySet())
		{
			DataNode node = dataNodeMap.get(i);
			if (isNode(node))
			{
				CyNode cyNode = cyNet.addNode();
				node.setCyNode(cyNode);
			}
		}
		helper.flushPayloadEvents();
		for (String i : dataNodeMap.keySet())
		{
			DataNode node = dataNodeMap.get(i);
			if (isNode(node))
			{
				CyNode cyNode = node.getCyNode();
				View<CyNode> nodeView = networkView.getNodeView(cyNode);
				if (nodeView != null)
				{
					copyNodeAttribues(node, cyNode, nodeView);
//					if (isShape(node))
//					{
//						nodeView.setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, true);	
////						nodeView.setVisualProperty(BasicVisualLexicon.NODE_HEIGHT, 3.0);	
////						node.putDouble("Width", 3.0);
////						node.putDouble("Height", 3.0);
//					}
				}
				
			}
		}
	}
	
	private boolean isShape(DataNode node)
	{
		String type = node.getType();
		if ("Shape".equals(type))	return true;
		return false;
	}
	
	private boolean isNode(DataNode node)
	{
		if (node.isAnchor()) return true;
		String type = node.getType();
		if ("GeneProduct".equals(type))	return true;
		if ("Metabolite".equals(type))	return true;
		if ("Protein".equals(type))	return true;
//		if ("Shape".equals(type))	return true;
		return false;
	}

	// This will be used to for String VP which accepts any string values.
	protected static final Range<String> ARBITRARY_STRING_RANGE = new DiscreteRange<String>(String.class, new HashSet<>()) {
		// Takes any String as valid value.
		@Override
		public boolean inRange(String value) {
			return true;
		}
	};
	private void copyNodeAttribues(DataNode node, CyNode cyNode, View<CyNode> cyNodeView) {
		
		if (node == null) 
		{
			System.out.println("NULL");
			return;
		}
		 node.setCyNode(cyNode);
		CyRow row = cyNodeTbl.getRow(cyNode.getSUID());
		set(row, "name", node.getLabel());
		set(row, "shared name", node.getName());
		set(row, "GraphID", "" + node.getGraphId());
		set(row, "Xrefid", "" + node.getDbid());
		set(row, "XrefDatasource", "" + node.getDatabase());
		set(row, "Type", node.getType());
		set(row, "GroupRef", node.get("GroupRef"));

		String val = node.get("org.pathvisio.model.BackpageHead");
		if (!StringUtil.isEmpty(val))
			cyNodeView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP,val );
		
		set(row, "CenterX", node.get("CenterX"));		
		double x = node.getDouble("CenterX");
		cyNodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);//
		
		set(row, "CenterY", node.get("CenterY"));	
		double y = node.getDouble("CenterY");
		cyNodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);//
		
		set(row, "Width", node.get("Width"));		
		double w = node.getDouble("Width");
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, w);//

		set(row, "Height", node.get("Height"));		
		double h = node.getDouble("Height");
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, h);//

		set(row, "ZOrder", node.get("ZOrder"));
		double z = node.getDouble("ZOrder");
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_Z_LOCATION, z);//

		
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.RECTANGLE );//
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_PAINT,Color.white );//
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT,Color.black );//
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH,2.0 );//
		
		set(row, "LabelColor", node.get("LabelColor"));				//  BasicVisualLexicon.NODE_FILL_COLOR);   //BasicVisualLexicon.NODE_BORDER_PAINT
		Color lc = node.getColor("LabelColor");
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_COLOR,lc );//

		String fill = node.get("Color");
		set(row, "Color", fill);				//  BasicVisualLexicon.NODE_FILL_COLOR);   //BasicVisualLexicon.NODE_BORDER_PAINT
		Color c = Color.WHITE;
		if (!StringUtil.isEmpty(fill))
		{
			c = node.getColor("Color");
			cyNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_COLOR,c );
			cyNodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT,c );
		}

		set(row, "Shape", node.get("Shape"));
		NodeShape sh = getNodeShape(node.get("Shape"));
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_SHAPE,sh );//

		String shType = node.get("ShapeType");
		Shape path = getCustomShape(shType);
//		set(row, "LineType", node.get("LineType"));
//		LineType lin = getLineType(node.get("LineType"));
//		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_LINE_TYPE,lin );//
//

		set(row, "FontSize", node.get("FontSize"));
		int sz = node.getInteger("FontSize");
		cyNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, sz );//

		set(row, "Transparency", node.get("Transparency"));		//BasicVisualLexicon.NODE_TRANSPARENCY
		set(row, "Resizable", node.get("Resizable"));
		set(row, "VAlign", node.get("VAlign"));
	}
                     
//    public static final VizTableStore NODE_BORDER_THICKNESS = new BasicVizTableStore("BorderThickness", Double.class, BasicExtracter.NODE_LINE_THICKNESS,             BasicVisualLexicon.NODE_BORDER_WIDTH);
//    public static final VizTableStore NODE_SHAPE            = new BasicVizTableStore("Shape",                         BasicExtracter.SHAPE, PV_SHAPE_MAP,             BasicVisualLexicon.NODE_SHAPE);
 
	void set(CyRow row, String colName, String val)
	{
		try
		{
			CyColumn c = cyNodeTbl.getColumn(colName);
			if (c == null)
				cyNodeTbl.createColumn(colName, String.class, false);
			row.set(colName, val);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	//--------------------------------------------------------------------------------
	// GROUP

	boolean bypass = false;
	
	private void addGroupsToNodeTable() {
		
	if (bypass) return;	
		CyGroupFactory groupFactory = manager.getRegistrar().getService(CyGroupFactory.class);
		List<CyNode> nodeList = new ArrayList<CyNode>();
		for (String g : groupMap.keySet())
		{
			nodeList.clear();
			DataNodeGroup group = groupMap.get(g);
			group.assignMembers();
			for (DataNode member : group.getMembers())
				if (member.getCyNode() != null)
					nodeList.add(member.getCyNode());
			CyGroup group1 = groupFactory.createGroup(cyNet, true);
			group1.addNodes(nodeList);
			CyNode groupCyNode = group1.getGroupNode();
			group.setCyNode(groupCyNode);
			
		}
		helper.flushPayloadEvents();
		for (String g : groupMap.keySet())
		{
			DataNodeGroup group = groupMap.get(g);
			CyNode cyNode = group.getCyNode();
			View<CyNode> nodeView = networkView.getNodeView(cyNode);
			if (nodeView != null)
			{
				CyRow row = cyNodeTbl.getRow(cyNode.getSUID());
//				row.set(CyRootNetwork.SHARED_NAME, String.format("GROUP OF %d", group.getMembers().size()));
				set(row, "name", group.getLabel());
				set(row, "shared name", String.format("Group of %d", group.getMembers().size() ));
				set(row, "GraphID", "" + group.getGraphId());
				String groupStyle = group.get("Style");
				set(row, "Style", groupStyle);
			    Color groupColor =  Color.WHITE;
			    if   ("Pathway".equals(groupStyle)) 
			    	groupColor =  Color.GREEN;
			    else if ("Complex".equals(groupStyle) || "Group".equals(groupStyle) || StringUtil.isEmpty(groupStyle))
			    	groupColor =  new Color(236, 236, 200);

				NodeShape sh = "Complex".equals(groupStyle) ?  NodeShapeVisualProperty.OCTAGON : NodeShapeVisualProperty.RECTANGLE; 
				nodeView.setLockedValue(BasicVisualLexicon.NODE_SHAPE,sh );

				set(row, "CenterX", group.get("CenterX"));		
//				double x = group.getDouble("CenterX") - group.getLeft();
//				nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
				
				set(row, "CenterY", group.get("CenterY"));	
//				double y =group.getDouble("CenterY") - group.getTop();
//				nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
				
				set(row, "Width", group.get("Width"));		
//				double w = group.getDouble("Width");
//				nodeView.setVisualProperty(BasicVisualLexicon.NODE_WIDTH, w);

				set(row, "Height", group.get("Height"));		
//				double h = group.getDouble("Height");
//				nodeView.setVisualProperty(BasicVisualLexicon.NODE_HEIGHT, h);//
				
				set(row, "ZOrder", "1");		
				nodeView.setLockedValue(BasicVisualLexicon.NODE_Z_LOCATION, 1.0);

				nodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 1.0);//
				nodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.black);//
				nodeView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, groupColor);//
				set(row, "Color", AttributeMap.colorToString(groupColor));
			}
		}		
		helper.flushPayloadEvents();
	}
	//--------------------------------------------------------------------------------
	// EDGE
	
	private void writeAnchors() {
		for (Interaction i : getEdges())
		{
			Edge e = i.getEdge();
			String id = e.getSourceid();
			String tid = e.getTargetid();
			DataNode targ = dataNodeMap.get(tid);
			if (targ != null && targ.isAnchor())
			{
				CyNode cyTarg = targ.getCyNode();
				if (cyTarg == null)
				{	
					System.err.println("ADDING TARGET");
					cyTarg =cyNet.addNode();
				
				}
				targ.setCyNode(cyTarg);
				helper.flushPayloadEvents();

				String parent = ((Anchor)targ).getInteractionId();
				Interaction inter = findInteractionById(parent);
				SegmentList segments = new SegmentList(this,  inter.getEdge().getPoints());
				double d = ((Anchor) targ).getAnchorPosition();
				Point2D.Double position =  segments.fromLineCoordinate(d);

				CyRow row = cyNodeTbl.getRow(cyTarg.getSUID());
				set(row, "name", "Anchor in " + i.getName());
				set(row, "shared name", "" + cyTarg.getSUID());
				set(row, "GraphID",  tid);
				set(row, "Position", String.format("%.2f", d));		
				set(row, "CenterX", String.format("%.2f", position.getX()));		
				set(row, "CenterY", String.format("%.2f", position.getY()));	
				set(row, "Width", targ.get("Width"));		
				set(row, "Height", targ.get("Height"));		
				set(row, "ZOrder", targ.get("ZOrder"));
				set(row, "Shape", targ.get("Shape"));
				
				System.out.println(String.format("Anchor at: (%.1f, %.1f)",position.getX(), position.getY()));
				View<CyNode> nodeView = networkView.getNodeView(cyTarg);
				nodeView.setVisualProperty( BasicVisualLexicon.NODE_X_LOCATION, position.getX());
				nodeView.setVisualProperty( BasicVisualLexicon.NODE_Y_LOCATION, position.getY());
				nodeView.setLockedValue( BasicVisualLexicon.NODE_Z_LOCATION,  -10001.5);
				nodeView.setLockedValue( BasicVisualLexicon.NODE_FILL_COLOR, Color.MAGENTA);
				nodeView.setLockedValue( BasicVisualLexicon.NODE_BORDER_WIDTH,1.0);
				nodeView.setLockedValue( BasicVisualLexicon.NODE_BORDER_PAINT,Color.BLACK);
				nodeView.setVisualProperty( BasicVisualLexicon.NODE_BORDER_WIDTH, 1.0);
				nodeView.setVisualProperty( BasicVisualLexicon.NODE_WIDTH, 25.0);
				nodeView.setVisualProperty( BasicVisualLexicon.NODE_HEIGHT,  25.0);
				nodeView.setLockedValue( BasicVisualLexicon.NODE_SIZE,  25.0);
				nodeView.setLockedValue( BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.HEXAGON);

			}

		}
		System.out.println("Nodes: " + getDataNodeMap().size());
	}
	
	private void writeEdgeTable() {
		System.out.println("Interactions: " + getEdges().size());
		for (Interaction i : getEdges())
		{
			Edge e = i.getEdge();
			String id = e.getSourceid();
			String tid = e.getTargetid();
			DataNode src = dataNodeMap.get(id);
			if (src == null)
			{
				System.out.println(e.getSourceid() + " SOURCE NOT FOUND");
//				GPMLPoint pt = e.firstGPMLPoint();
//				CyNode cyNode = cyNet.addNode();
//				AttributeMap am = new AttributeMap();
//				am.put("x", String.format("%.2f", pt.getX()));
//				am.put("y", String.format("%.2f", pt.getY()));
//				am.put("width", "3");
//				am.put("height", "3");
//				src = new DataNode(am, this);
//				src.setCyNode(cyNode);
//				helper.flushPayloadEvents();
//				setPosition(cyNode,pt);
//				View<CyNode> nodeView = networkView.getNodeView(cyNode);
//				if (nodeView != null)
//				{
//					nodeView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, pt.getX());
//					nodeView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, pt.getY());
//					nodeView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 3.0);
//					nodeView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, 3.0);
//				}
//				i.getEdge().setSourceid(src.getGraphId());
			}
			
			DataNode targ = dataNodeMap.get(tid);
			if (targ == null)
			{
				System.out.println(e.getTargetid() + " TARGET NOT FOUND");
			}
			CyNode cyTarg = targ.getCyNode();
			if (cyTarg != null && src.getCyNode() != null)
			{
				final CyEdge cyEdge = cyNet.addEdge(src.getCyNode(), cyTarg, true);
				i.setCyEdge(cyEdge);
			}
		}
	
		helper.flushPayloadEvents();
//		System.out.println("Interactions: " + getEdges().size());
		int x = 1;
		for (Interaction i : getEdges())
		{
			Edge e = i.getEdge();
			if (e == null)
			{
				System.err.println("NO EDGE");
				continue;
			}
			CyEdge cyEdge = i.getCyEdge();
			if (cyEdge == null)
			{
				System.err.println("NO CYEDGE");
				continue;
			}
			View<CyEdge> edgeView = networkView.getEdgeView(cyEdge);
			if (edgeView == null)
			{
				System.err.println("NO VIEW");
				continue;
			}
			else	copyEdgeAttribues(i, cyEdge, edgeView);
			System.out.println(x++ + " " + e.getGraphId() + " " + cyEdge.getSUID() + " " + e.getInteraction() + "  " + e.getNPoints());
			
			if (cyEdge != null)
			{
				int n = e.getNPoints();
				String edgeType = e.get("ConnectorType");  
				if ("Elbow".equals(edgeType))
				{
					SegmentList segments = new SegmentList(this,  e.getPoints());
					makeElbowEdgeBend(e, cyEdge, segments);
				} else if ("Curved".equals(edgeType))
				{
					SegmentList segments = new SegmentList(this,  e.getPoints());
						
				}
			}
			else
				System.err.println("CyEdge = null");
		}
	}
	
	private void setPosition(CyNode cyNode, GPMLPoint pt) {
		// TODO Auto-generated method stub
		
	}
	private void makeElbowEdgeBend(Edge e, CyEdge cyEdge, SegmentList segments) {
	   BendFactory factory = manager.getBendFactory();	
		if (networkView == null)    	{
			System.out.println("networkView == null"); 
			return;
		}
		
		
		View<CyEdge> edgeView = networkView.getEdgeView(cyEdge);
		if (edgeView == null)    	{
			System.out.println("edgeView == null"); 
			return;
		}
		DataNode start = getDataNode(e.getSourceid());
		DataNode target = getDataNode(e.getTargetid());
		if (target instanceof DataNodeGroup)
		{
			System.err.println("TARGET IS GROUP");
			return;
		}
		double sx = start.getDouble("CenterX");
		double sy = start.getDouble("CenterY");
		System.out.println(String.format("(%.0f, %.0f)", sx, sy));
		System.out.println(String.format("(%.0f, %.0f)", e.getStartX(), e.getStartY()));
		segments.dump();
		System.out.println(String.format("(%.0f, %.0f)", e.getEndX(), e.getEndY()));
		double ex = target.getDouble("CenterX");
		double ey = target.getDouble("CenterY");
		System.out.println(String.format("(%.0f, %.0f)", ex, ey));

		Bend bend = factory.createBend();
	    HandleFactory facto = manager.getHandleFactory();
	    
	    segments.dump();
	    int sz = segments.size();
	    for (int i=0; i<sz-1; i++)
	    {
	    	Segment seg = segments.get(i);
	    	double x = seg.end.getX();
	    	double y = seg.end.getY();
			System.out.println(String.format("(%.0f, %.0f)", x, y));
			try {
				Handle h = facto.createHandle(networkView, edgeView,  x, y);		// put in 2 handles for a angled bend
			    Handle j = facto.createHandle(networkView, edgeView,  x, y);
			    bend.getAllHandles().add(h);
			    bend.getAllHandles().add(j);
				edgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, bend);
			
			}
			catch (IllegalStateException ex2)
			{
				System.err.println(String.format("(%.0f, %.0f)", x, y));
				System.err.println(networkView + " " + edgeView);
//				ex2.printStackTrace();
//Invalid angle: NaN. Cuased by cos(theta) = NaN
			}
				    
	   }
//    	Handle h = facto.createHandle(networkView, edgeView, ex, ey);		// put in 2 handles for a angled bend
//	    Handle j = facto.createHandle(networkView, edgeView,  ex, ey);
//	    bend.getAllHandles().add(h);
//	    bend.getAllHandles().add(j);
	}



	void setEdge(CyRow row, String colName, String val)
	{
		try
		{
			CyColumn c = cyEdgeTbl.getColumn(colName);
			if (c == null)
				cyEdgeTbl.createColumn(colName, String.class, false);
			row.set(colName, val);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void copyEdgeAttribues(Interaction i, CyEdge edge, View<CyEdge> cyEdgeView) {
		if (i == null) {	System.err.println("NULL INTERACTION");		return;		}
		
		Edge e = i.getEdge();
		AttributeMap attr = e.getAttributes();
		CyRow row = cyEdgeTbl.getRow(edge.getSUID());
		setEdge(row, "GraphId", "" + e.getGraphId());
		setEdge(row, "Xrefid", "" + e.getDbid());
		setEdge(row, "XrefDatasource", "" + e.getDatabase());
		setEdge(row, "Type", e.getType());
		setEdge(row, "ZOrder", e.get("ZOrder"));
		
		setEdge(row, "Color", e.get("Color")); 
		Color c = e.getColor("Color");
		cyEdgeView.setLockedValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT,c );//
		cyEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT,c );//

		setEdge(row, "LineType", e.get("LineStyle"));
		LineType lin = getLineType(e.get("LineStyle"));
		cyEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LINE_TYPE,lin );//

		setEdge(row, "Thickness", e.get("LineThickness"));
		double th = e.getDouble("LineThickness");
		cyEdgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH,th );//

		String arrow = i.getInterType();
		cyEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE,getArrowShape(arrow) );//
		
		setEdge(row, "Source", e.getSourceid());	//BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE;
		setEdge(row, "Target", e.getTargetid());		//BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE)
		setEdge(row, "Interaction", i.getInterType());
		
		int npts = e.getNPoints();
		String p = e.getPointsStr();
		String fstPt = e.pointString(e.firstPoint());
		String lstPt = e.pointString(e.lastPoint());
		setEdge(row, "SourcePt", fstPt);
		setEdge(row, "TargetPt", lstPt);
		setEdge(row, "NPoints", "" + npts);

		setEdge(row, "name", e.toString());
		setEdge(row, "shared name", i.getName());
	}

	//--------------------------------------------------------------------------------
	private void	writeAnnotations()
	{
		AnnotationFactory<ShapeAnnotation> shapeFactory;
		AnnotationFactory<TextAnnotation> textFactory;
		CyServiceRegistrar registrar = manager.getRegistrar();
		AnnotationManager annotationMgr = registrar.getService(AnnotationManager.class);
		if (annotationMgr == null) 
			System.err.println("AnnotationManager is null");
		shapeFactory =  (AnnotationFactory<ShapeAnnotation>)registrar.getService( AnnotationFactory.class,"(type=ShapeAnnotation.class)");
		if (shapeFactory == null) 
			System.err.println("shapeFactory is null");
		textFactory =  (AnnotationFactory<TextAnnotation>)registrar.getService( AnnotationFactory.class,"(type=TextAnnotation.class)");
		if (textFactory == null) 
			System.err.println("textFactory is null");
boolean debug = true;
if (debug)
	debugSquare(annotationMgr, shapeFactory, 400);
		
		
		for (String i : shapes.keySet())
		{
			DataNode shape = dataNodeMap.get(i);
			String type = shape.getType();
			System.out.println(type + " " + shape.attributes.get("Name"));
			double x = 0;   //shape.getDouble("CenterX");
			double y = 0; //shape.getDouble("CenterY");
			System.out.println(x + " " + y);
			if ("Shape".equals(type))
			{
				final AttributeMap args = new AttributeMap();
				String shapeType = shape.get("ShapeType");
				String text = shape.get("TextLabel");
				args.put("shapeType" ,shapeType);
				if ("GraphicalLine".equals(shapeType))
				{
					List<GPMLPoint> pts = shape.getGPMLPoints();
					int nPts = pts.size();
					if (nPts == 2)
					{
						GPMLPoint a = pts.get(0);
						GPMLPoint b = pts.get(1);
						double w = Math.abs(b.getX() - a.getX());
						double h = Math.abs(b.getY() - a.getY());
						if (h < 1) h = 1;
						x = a.getX();
						y = a.getY();
						args.put("x", String.format("%.2f", x)); 
						args.put("y", String.format("%.2f", y));
						args.put("width", String.format("%.2f", w));
						args.put("height", String.format("%.2f", h));
					}
					args.put("edgeThickness" ,shape.get("LineThickness"));
					args.put("shapeType" , "Line");
				}
				else
				{
					x = shape.getDouble("CenterX");
					y = shape.getDouble("CenterY");
					double w = shape.getDouble("Width");
					double h = shape.getDouble("Height");
					x -= w /2.;
					y -= h /2.;
					args.put("x", String.format("%.2f", x)); 
					args.put("y", String.format("%.2f", y));
					args.put("width", String.format("%.2f", w));
					args.put("height", String.format("%.2f", h));

					args.put("text",text);
					args.put("rotation" ,shape.get("Rotation"));
					args.put("edgeThickness" ,shape.get("LineThickness"));
					args.put("fontweight" ,"bold");		//shape.get("FontWeight")
					args.put("vertAlign" ,shape.get("VAlign"));
					args.put("fontsize" ,shape.get("FontSize"));
//					
//					AttributeMap m = new AttributeMap();
//					m.put("GraphId", "2");
//					m.put("width", "2");
//					m.put("height", "2");
//					m.put("x", String.format("%.2f", x)); 
//					m.put("y", String.format("%.2f", y));
				
//					DataNode dummyNode = new DataNode(m, this);
//					CyNode cyNode = cyNet.addNode();
//					shape.setCyNode(cyNode);
//					addNode(shape);
//					String shType = shape.get("ShapeType");
//					Shape path = getCustomShape(shType);
				}
				ShapeAnnotation anno = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, args);
				anno.setCanvas("background");
				String shType = shape.get("ShapeType");
				Shape path = getCustomShape(shType);
				boolean custom =  (path != null);
				String colString = shape.get("Color");
				Color col = AttributeMap.colorFromString(colString, Color.white);
				if (col != null)
				{
//					if ("GraphicalLine".equals(shapeType) || custom)
						anno.setBorderColor(col);
//					else anno.setFillColor(col);
				}
				
				if (custom)	anno.setCustomShape(path);

				annotationMgr.addAnnotation(anno);
				
				if (!StringUtil.isEmpty(text))
					addTextAnnotation(shape, textFactory, annotationMgr);
			}
		}
		for (String i : labels.keySet())
		{
			DataNode label = dataNodeMap.get(i);
			if ("Label".equals(label.getType()))
				addTextAnnotation(label, textFactory, annotationMgr);
		}
	}
	
	private void debugSquare(AnnotationManager annotationMgr, AnnotationFactory<ShapeAnnotation> shapeFactory, double width)
	{
		final AttributeMap args = new AttributeMap();
		args.put("x", "0"); 
		args.put("y", "0");
		args.put(ShapeAnnotation.SHAPETYPE, "Rectangle");
		ShapeAnnotation anno = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, args);
		anno.setCanvas("background");
		anno.setSize(width, width);
		anno.setFillColor(Color.white);
		anno.setBorderColor(Color.green);
		annotationMgr.addAnnotation(anno);
		for (double i=100; i<= width; i+=100)
		{
			makeLine(annotationMgr, shapeFactory, 0, i, width, i);
			makeLine(annotationMgr, shapeFactory,  i,0, i, width);
		}
	}
	
	private void makeLine(AnnotationManager annotationMgr, AnnotationFactory<ShapeAnnotation> shapeFactory, 
				double x1, double y1, double x2, double y2) {
		final AttributeMap args = new AttributeMap();
		args.put("x", "" + x1); 
		args.put("y", "" + y1);
		args.put("width", "" + (1 + x2 - x1) );
		args.put("height", "" + (1 + y2 - y1) );
		args.put(ShapeAnnotation.SHAPETYPE, "Rectangle");
		ShapeAnnotation anno = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, args);
		anno.setCanvas("foreground");
		anno.setBorderColor(Color.red);
		anno.setBorderWidth(2);
		annotationMgr.addAnnotation(anno);
	}
	
	private void addTextAnnotation(DataNode label,AnnotationFactory<TextAnnotation> textFactory, AnnotationManager annotationMgr) {
		final Map<String,String> args = new HashMap<String,String>();
		double x = label.getDouble("CenterX");
		double y = label.getDouble("CenterY");
		double w = label.getDouble("Width");
		double h = label.getDouble("Height");
		x -= w /2.;
		y -= h /2.;
		args.put("x", String.format("%.2f", x)); 
		args.put("y", String.format("%.2f", y));
		args.put("width", String.format("%.2f", w)); 
		args.put("height", String.format("%.2f", h));

		Color c = AttributeMap.colorFromString(label.get("Color"), Color.white);
		String colString = AttributeMap.colorToString(c);
		args.put("fillColor", colString);
//		args.put("text", label.get("TextLabel")); 	 doesnt convert CRs
		args.put("fontSize", label.get("FontSize"));
		args.put("canvas", "foreground");

		//				strs.put("fontFamily", label.getFontName());
		args.put("fontStyle", "1");
		String s = label.get("TextLabel");
		TextAnnotation textBox = textFactory.createAnnotation(TextAnnotation.class, networkView, args);
		textBox.setCanvas("foreground");
		textBox.setText(s);
		annotationMgr.addAnnotation(textBox);
	}
	private Shape getCustomShape(String type) {

		Shape sh = CellShapes.getPath(type);
		if (sh != null) 	return sh;
		if ("Oval".equals(type))
			return CellShapes.getShape(type);
		return null;
	}
	//-------------------------------------------------------------------------
	

	
String name;
String descriptions;
String pmids;

String getName()			{ return name;	}
String getDescriptions()	{ return descriptions;	}
String getPMIDs()			{ return pmids;	}

	
	private Species species = Species.Unspecified;
	public Species getSpecies() {
		if (species == null) 
			species = Species.Unspecified;
		return species;
	}
	public void setSpecies(Species s) {			species = s;	}
	// **-------------------------------------------------------------------------------
	public String saveState()
	{
		String header = docHeader();
		StringBuilder saver = new StringBuilder(header);
		serializeComments(saver);
		serializeNodes(saver);
		serializeStates(saver);
		serializeEdges(saver);
		serializeGroups(saver);
		serializeReferences(saver);
		saver.append("</Pathway>\n");
		return saver.toString();
	}
	String[] pathwayAttributes = {"Name", "Organism", "License"};
	private static String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n";
	private static String namespace = "xmlns=\"http://pathvisio.org/GPML/2013a\"\n";
	
	private String docHeader() {		return xmlHeader +  "<Pathway>\n";	}
	
	private void serializeComments(StringBuilder saver) {
//		for (CommentRecord rec : getComments())
//			saver.append(rec.toGPML());
	}
	private void serializeReferences(StringBuilder saver) {
//		for (BiopaxRecord rec : getReferences())
//			saver.append(rec.toGPML());
	}
	private void serializeNodes(StringBuilder saver) {
		for (DataNode node : getNodes())
		{	
			if (node instanceof DataNodeGroup) continue;
			saver.append(node.toGPML());
		}
	}
	private void serializeStates(StringBuilder saver) {
		for (DataNodeState s : getStates())
			saver.append(s.toGPML());
		}
	private void serializeEdges(StringBuilder saver) {
		for (Interaction edge : getEdges())
			saver.append(edge.toGPML());
	}
	public void serializeGroups(StringBuilder bldr)
	{
		for (String key : groupMap.keySet())
			bldr.append(groupMap.get(key).toGPML());
	}
	// **-------------------------------------------------------------------------------
	
	List<CommentRecord> comments = new ArrayList<CommentRecord>();
	public void addComment(String source, String text) {		comments.add(new CommentRecord(source, text));	}
	public void clearComments() {		comments.clear();			}
	public String getCommentsStr() {
		StringBuilder b = new StringBuilder();
		for (CommentRecord c : comments)
			b.append(c.getText()).append("\n");
		return b.toString();
	}
	public List<CommentRecord> getComments() {		return comments;	}
	public String getComment(String src) 
	{	
		for (CommentRecord cr : comments)
			if (cr.getSource().equals(src))
				return cr.getText();
		return "";
	}

	//--------------------------------------------------------------------
	//--------------------------------------------------------------------
	List<BiopaxRecord> references = new ArrayList<BiopaxRecord>();
	public void addRef(BiopaxRecord ref) 	{	if (!refExists(ref.getId()))	references.add(ref);		}
	private boolean refExists(String ref) 	{	return findRef(ref) != null;	}
	private BiopaxRecord findRef(String ref) {
		if (ref == null) return null;
		for (BiopaxRecord r : references)
			if (ref.equals(r.getRdfid())) return r;
		return null;
	}
	public int getNReferences() 			{	return references.size();	}
	public BiopaxRecord getReference(String ref) 	{	return findRef(ref);		}
	public BiopaxRecord getReference(int i) {	return references.get(i);	}
	public void clearRefs() 				{	references.clear();	}
	public List<BiopaxRecord> getReferences() { return references;	}
	// **-------------------------------------------------------------------------------
	public void addNode(DataNode node)	{		addResource(node);	}
	
	public void addResource(DataNode mnode)		
	{  
		if (mnode != null) 
			addResource(mnode.getId(), mnode);
	}
	public void addResource(String key, DataNode n)		
	{  
		if (key == null) System.err.println("NULL KEY");
		else if (dataNodeMap.get(key) == null)
			dataNodeMap.put(key, n);
	}
	public String getNodeName(int id)
	{
		if (id <= 0) return "???";
		DataNode node = dataNodeMap.get(id);
		return (node == null) ? "??" : node.getName();
	}
//	public DataNode findDataNode(String nameOrId)
//	{
//		if (nameOrId == null) return null;
//		String name = nameOrId.trim();
//		for (DataNode g : getNodes())
//		{
//			if (name.equalsIgnoreCase(g.getName())) return g;
////			if (name.equalsIgnoreCase(g.getGraphId())) return g;
//		}
//		return null;
//	}
	public DataNode findDataNode(String id)
	{
		if (id == null) return null;
		for (DataNode g : getNodes())
		{
			if (id.equals(g.getId())) return g;
//			if (name.equalsIgnoreCase(g.getGraphId())) return g;
		}
		return null;
	}
	public List<DataNode> findByDBID(String db, String id)
	{
		if (db == null || id == null) return null;
		List<DataNode> hits = new ArrayList<DataNode>();
		
		for (DataNode g : getNodes())
		{
			if (db.equals(g.get("Database"))) 
				if (id.equals(g.get("ID"))) 
					hits.add(g);
		}
		return hits;
	}
	// **-------------------------------------------------------------------------------
	public List<DataNode> search(String txt)
	{
		txt = txt.toUpperCase();
		List<DataNode> hits = new ArrayList<DataNode>();
		for (DataNode g : getNodes())
			if (g.contains(txt))
				hits.add(g);
		return hits;
	}
	// **-------------------------------------------------------------------------------
//	public Interaction addInteraction(DataNode start, DataNode end, String activeLayer)		
//	{  
//		AttributeMap attributes = new AttributeMap();
//		Interaction edge = new Interaction(this, start, end, attributes);
//		addEdge(edge);
//		return edge;
//	}
	// comes from user dragging / connecting
//	public Interaction addIteraction(VNode start, VNode end)		
//	{  
//		if (start == null || end == null) return null;
//		AttributeMap attributes = new AttributeMap();
//		attributes.put("Color", Color.RED.toString());
//		attributes.put("Layer", start.getLayerName());
//		List<GPMLPoint> pts = new ArrayList<GPMLPoint>();
//		pts.add(new GPMLPoint(start.center()));
//		pts.add(new GPMLPoint(end.center()));
//		Interaction edge = new Interaction(this, start, end, attributes);
//		addEdge(edge);
//		return edge;
//	}
//	private void readEdges(String state) {
//	}
	// **-------------------------------------------------------------------------------
	public Anchor findAnchorByRef(String graphRef)
	{
		Anchor a = (Anchor) find(graphRef);
		if (a != null) return a;
		
		for (Interaction inter : interactionMap.values())
		{
			a = inter.getEdge().findAnchor(graphRef);
			if (a != null) 
				return a;
		}
		return null;
	}
	public Interaction findInteractionById(String graphId)
	{
		for (Interaction inter : interactionMap.values())
			if (graphId.equals(inter.getEdge().getId()))
					return inter;
		return null;
	}
	
	
	public List< Interaction> findInteractionsByNode(DataNode node)
	{
		List< Interaction> hits = new ArrayList<Interaction>();
		for (Interaction inter : interactionMap.values())
			if (inter.getEdge().isStart(node) || inter.getEdge().isEnd(node))
				hits.add(inter);
		return hits;
	}
	
	public List< Interaction> findInteractionsByNodes(DataNode src, DataNode target)
	{
		List< Interaction> hits = new ArrayList<Interaction>();
		for (Interaction inter : interactionMap.values())
			if (inter.getEdge().isStart(src) || inter.getEdge().isEnd(target))
				hits.add(inter);
		return hits;
	}

	public void removeEdges(DataNode node)		
	{  
		List<String> edgesToRemove = new ArrayList<String>();
		for (Interaction e : getEdges())
		{
			if (e == null) continue;
			if (e.getEdge().isStart(node) || e.getEdge().isEnd(node))
				edgesToRemove.add(e.getEdge().getId());
		}
		for (String id : edgesToRemove)
			interactionMap.remove(id);
//		List<Edge> okEdges = edgeTable.stream().filter(new TouchingNodeFilter(node)).collect(Collectors.toList());
//		edgeTable.clear();
//		edgeTable.addAll(okEdges);
	}
	// **-------------------------------------------------------------------------------
	
	public void removeNode(DataNode node)		
	{  
		if (node == null) return;
//		if ("Marquee".equals(node.getId())) return;
		
//		DataNode mNode = node.modelNode();
		String id = node.getId();
		removeEdges(node);
		
		dataNodeMap.remove(id);
		shapes.remove(id);
		labels.remove(id);
		groupMap.remove(id);
	}

	public List<DataNode> getResourceByKey(String key)				
	{
		List<DataNode> hits = new ArrayList<DataNode>();
		if (key == null) return null;
		 for (DataNode n : dataNodeMap.values())
		 {
			 String name = "" +n.get("TextLabel");
			if (name.equals(key)) hits.add(n);
		 }
//		 DataNode n = dataNodeMap.get(key);	
		 return hits;
	}

	public DataNode getDataNode(String key)				
	{
		 if (key == null) return null;
		 if (key.startsWith("\""))  // if its in quotes, strip them
		 {
			 int len = key.length();
			 key = key.substring(1,len-1);
		 }
		 DataNode n = dataNodeMap.get(key);	
		 return n;
	}
	
	public DataNode find(String key)				
	{
		 DataNode n = getDataNode(key);	
		 if (n != null) return n;

		System.out.println("failed to find node: " + key);
//		dumpKeys();
		
		return null;
	}
	private void dumpKeys() {
	for (	String key : dataNodeMap.keySet())
		System.out.println(key);
		
	}
//	
//	public DataNode find(int key)				
//	{
//		 DataNode n = dataNodeMap.get(key);	
//		return n;
//	}
//	
//	public int cloneResourceId(String oldId)	{		return gensym();	}
	//------------------------------------------------------------------------- GROUPS
//	Map<String, GPMLGroup> groups = new HashMap<String, GPMLGroup>();
	// move to GPML
	public void addGroup(DataNodeGroup grp) {
		String id = grp.getGraphId();
		groupMap.put(id,grp);
		dataNodeMap.put(grp.getId(),grp);
	}
//	public Collection<GPMLGroup> getGroups() { return groups.values();	}
	
	// **-------------------------------------------------------------------------------
	public boolean containsEdge(Edge a) {
		for (Interaction i : getEdges())
		{
			if (a == i.getEdge()) return true;
			if (a.getSourceid() == i.getEdge().getSourceid())
				if (a.getTargetid() == i.getEdge().getTargetid())
					return true;
		}
		return false;
	}

	public void addEdge(Interaction e)			
	{  
		String id = e.getEdge().get("GraphId");
				interactionMap.put(id, e);	
	}

	//--------------------------------------------------------------------------------
	
	static LineType getLineType(String s)
	  {
			if ("Solid".equals(s))	return LineTypeVisualProperty.SOLID;
			if ("Double".equals(s))	return LineTypeVisualProperty.LONG_DASH;
			if ("Broken".equals(s))	return LineTypeVisualProperty.EQUAL_DASH;
			if ("Dashed".equals(s))	return LineTypeVisualProperty.EQUAL_DASH;
			if ("Dots".equals(s))	return LineTypeVisualProperty.DOT;
			return LineTypeVisualProperty.SOLID;
		  
	  }

	static NodeShape getNodeShape(String s)
	{
		if ("Rectangle".equals(s))	return NodeShapeVisualProperty.RECTANGLE;
		if ("Triangle".equals(s))	return NodeShapeVisualProperty.TRIANGLE;
		if ("RoundRectangle".equals(s))	return NodeShapeVisualProperty.ROUND_RECTANGLE;
		if ("RoundedRectangle".equals(s))	return NodeShapeVisualProperty.ROUND_RECTANGLE;
		if ("Hexagon".equals(s))	return NodeShapeVisualProperty.HEXAGON;
		if ("Pentagon".equals(s))	return NodeShapeVisualProperty.HEXAGON;
		if ("Ellipse".equals(s))	return NodeShapeVisualProperty.ELLIPSE;
		if ("Oval".equals(s))	return NodeShapeVisualProperty.ELLIPSE;
		if ("Octagon".equals(s))	return NodeShapeVisualProperty.OCTAGON;
		return NodeShapeVisualProperty.RECTANGLE;
	}
	
	static ArrowShape getArrowShape(String s)
	{
		if ("Arrow".equals(s))	 return ArrowShapeVisualProperty.DELTA;
		if ("Line".equals(s))	 return ArrowShapeVisualProperty.NONE;
		if ("tbar".equals(s))	 return ArrowShapeVisualProperty.T;
		if ("mim-binding".equals(s))	 return ArrowShapeVisualProperty.ARROW;
		if ("mim-conversion".equals(s))	 return ArrowShapeVisualProperty.DELTA;
		if ("mim-modification".equals(s))	 return ArrowShapeVisualProperty.DELTA;
		if ("mim-catalysis".equals(s))	 return ArrowShapeVisualProperty.OPEN_CIRCLE;
		if ("mim-inhibition".equals(s))	 return ArrowShapeVisualProperty.T;
		if ("mim-necessary-stimulation".equals(s))	 return ArrowShapeVisualProperty.CROSS_OPEN_DELTA;
		if ("mim-stimulation".equals(s))	 return ArrowShapeVisualProperty.OPEN_DELTA;
		if ("mim-cleavage".equals(s))	 return ArrowShapeVisualProperty.DIAMOND;
		if ("mim-branching-left".equals(s))	 return ArrowShapeVisualProperty.CROSS_DELTA;
		if ("mim-branching-right".equals(s))	 return ArrowShapeVisualProperty.CROSS_OPEN_DELTA;
		if ("mim-transcription-translation".equals(s))	 return ArrowShapeVisualProperty.DELTA;
		if ("mim-mim-gap".equals(s))	 return ArrowShapeVisualProperty.DELTA;
		if ("mim-mim-covalent-bond".equals(s))	 return ArrowShapeVisualProperty.CROSS_DELTA;
		return ArrowShapeVisualProperty.DELTA;
	}
	
	// **-------------------------------------------------------------------------------
	static public String describe(DataNode node)	{	return node.toGPML();	}
	static String getBoundsString(double x, double y, double w, double h)	{
	 return String.format("x=%.1f, y=%.1f, width=%.1f, height=%.1f", x, y, w, h);
	}
	
	Map<String, Integer> oldIds = new HashMap<String, Integer>();
	
//	public int gensym()	{		return ++nodeCounter;	}
//	public int gensym(String oldSymbol)	
//	{		
//		Integer extant = oldIds.get(oldSymbol);
//		if  (extant != null && extant > 0) return extant;
//		Integer newId =	gensym();
//		oldIds.put(oldSymbol, newId);
//		return newId;
//	}

	public String getXRefs() {
		
		StringBuilder bldr = new StringBuilder();
		for (DataNode node : getDataNodeMap().values())
		{
			String db = node.get("Database");
			String id = node.get("ID");
			if((StringUtil.isEmpty(db) && StringUtil.isEmpty(id)))
			{
				db = "HGNC";
				id = node.getName();
			}

			
			if(!(StringUtil.isEmpty(db) || StringUtil.isEmpty(id)))
				bldr.append(db).append('\t').append(id).append('\n');
		}
		return bldr.toString();
	}
		// **-------------------------------------------------------------------------------
	// Polygons and polylines are stored the same, but have different base types
//	private void parsePolygonPoints(Polygon poly, String string)
//	{
//		parsePolyPoints(poly.getPoints(), string);
//	}	
	private void parsePolylinePoints(Path2D poly, String string)
	{
//		parsePolyPoints(poly.getPoints(), string);
	}	
	private void parsePolyPoints(List<Double> pts, String string)
	{
		String s = string.trim();
		s = s.substring(1, s.length()-1);
		String[] doubles = s.split(",");
		for (String d : doubles)
			pts.add(StringUtil.toDouble(d));
	}
	//---------------------------------------------------------------------------------------------
	public void addFields(List<QuadValue> records) {
		Set<String> targets = new HashSet<String>();
		for (QuadValue r : records)
			targets.add(r.getTarget());
		
		for (QuadValue r : records)
		{
			List<DataNode> nodes = findByDBID(r.getSource(), r.getAttribute());
			for (DataNode node : nodes)
			{
				String val = r.getValue();
				String extant = node.get(r.getTarget());
				if (extant != null && !extant.contains(val)) 
					val += extant + ", " + r.getValue();
				node.put( r.getTarget(), val);
			}
		}
	}
	
}



