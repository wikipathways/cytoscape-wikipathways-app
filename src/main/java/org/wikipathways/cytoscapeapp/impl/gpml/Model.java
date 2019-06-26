package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Model
{
/*
 *  Model - the set of record lists	
 *  
 *  We need to keep track of species, nodes, edges, genesets, pathways, references, comments, groups
 */
	private Controller controller;
	public Controller getController() { return controller; } 
//	public Pasteboard getPasteboard() { return controller.getPasteboard(); } 
	private Map<Integer, DataNode> dataNodeMap = new HashMap<Integer, DataNode>();
	private int nodeCounter = 0;
	public Collection<DataNode> getNodes()			{ return dataNodeMap.values();	}
	public Map<Integer, DataNode> getDataNodeMap() {		return dataNodeMap;	}

	private Map<Integer, DataNodeGroup> groupMap = new HashMap<Integer, DataNodeGroup> ();
	public Collection<DataNodeGroup> getGroups()			{ return groupMap.values();	}
	public Map<Integer, DataNodeGroup> getGroupMap() {		return groupMap;	}

	private Map<Integer, DataNodeState> stateMap = new HashMap<Integer, DataNodeState>();
	public Collection<DataNodeState> getStates()			{ return stateMap.values();	}
	public Map<Integer, DataNodeState> getStateMap() {		return stateMap;	}
	public void addState(Integer graphRef, DataNodeState statenode) {		stateMap.put(graphRef, statenode);	}

	public Collection<Interaction> getEdges()			{ return interactionMap.values();	}
	private Map<String, Interaction> interactionMap = new HashMap<String, Interaction>();
	public Map<String, Interaction> getInteractions()			{ return interactionMap;	}
	public List<Interaction> getInteractionList(int nodeId)			
	{ 
		List<Interaction> hits = new ArrayList<Interaction>();
		for (Interaction e : interactionMap.values())
			if (e.touches(nodeId))
					hits.add(e);
		return hits;	
	}
	public Interaction getInteraction(String edgeId)	{ 	return interactionMap.get(edgeId);	}
	
	private Map<Integer, DataNode> shapes = new HashMap<Integer,DataNode>();
	public Map<Integer, DataNode> getShapes()	{ return shapes; }
	public DataNode findShape(Integer s ) 		{ return shapes.get(s);	}
	public void addShape(DataNode s ) 			{ shapes.put(s.getId(),s);	}

	private Map<Integer, DataNode> labels = new HashMap<Integer,DataNode>();
	public Map<Integer, DataNode> getLabels()	{ return labels; }
	public DataNode findLabel(Integer s ) 		{ return labels.get(s);	}
	public void addLabel(DataNode d)			{ labels.put(d.getId(),  d); }


	private String title = "PathVisio Mockup";
	public void setTitle(String val) {		title = val;		}
	public String getTitle() 		{		return title;		}
	// **-------------------------------------------------------------------------------

	public Model()
	{
//		controller = ct;
	}
	//-------------------------------------------------------------------------
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
		for (Integer key : groupMap.keySet())
			bldr.append(groupMap.get(key).toGPML());
	}
//	//---------------------------------------------------------
//	public void setState(String s)
//	{
//		try
//		{
//		org.w3c.dom.Document doc = FileUtil.convertStringToDocument(s);	//  parse string to XML
//		if (doc != null)
//			controller.addXMLDoc(doc);	
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
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
	public void addResource(DataNode mnode)		
	{  
		if (mnode != null) 
			addResource(mnode.getId(), mnode);
	}
	public void addResource(Integer key, DataNode n)		
	{  
		if (key == null) System.err.println("NULL KEY");
		if (dataNodeMap.get(key) == null)
			dataNodeMap.put(key, n);
	}
	public String getNodeName(int id)
	{
		if (id <= 0) return "???";
		DataNode node = dataNodeMap.get(id);
		return (node == null) ? "??" : node.getName();
	}
	public DataNode findDataNode(String nameOrId)
	{
		if (nameOrId == null) return null;
		String name = nameOrId.trim();
		for (DataNode g : getNodes())
		{
			if (name.equalsIgnoreCase(g.getName())) return g;
//			if (name.equalsIgnoreCase(g.getGraphId())) return g;
		}
		return null;
	}
	public DataNode findDataNode(int id)
	{
		if (id <= 0) return null;
		for (DataNode g : getNodes())
		{
			if (id == g.getId()) return g;
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
	public Interaction addInteraction(DataNode start, DataNode end, String activeLayer)		
	{  
		AttributeMap attributes = new AttributeMap();
		attributes.put("Layer", activeLayer);
		Interaction edge = new Interaction(this, start, end, attributes);
		addEdge(edge);
		return edge;
	}
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
	private void readEdges(String state) {
	}
	// **-------------------------------------------------------------------------------
	public Anchor findAnchorByRef(int graphRef)
	{
		for (Interaction inter : interactionMap.values())
		{
			Anchor a = inter.findAnchor(graphRef);
			if (a != null) 
				return a;
		}
		return null;
	}
	public Interaction findInteractionById(int graphId)
	{
		for (Interaction inter : interactionMap.values())
			if (graphId ==inter.getId())
					return inter;
		return null;
	}
	
	
	public List< Interaction> findInteractionsByNode(DataNode node)
	{
		List< Interaction> hits = new ArrayList<Interaction>();
		for (Interaction inter : interactionMap.values())
			if (inter.isStart(node) || inter.isEnd(node))
				hits.add(inter);
		return hits;
	}
	
	public List< Interaction> findInteractionsByNodes(DataNode src, DataNode target)
	{
		List< Interaction> hits = new ArrayList<Interaction>();
		for (Interaction inter : interactionMap.values())
			if (inter.isStart(src) || inter.isEnd(target))
				hits.add(inter);
		return hits;
	}

	public void removeEdges(DataNode node)		
	{  
		List<Integer> edgesToRemove = new ArrayList<Integer>();
		for (Interaction e : getEdges())
		{
			if (e == null) continue;
			if (e.isStart(node) || e.isEnd(node))
				edgesToRemove.add(e.getId());
		}
		for (int id : edgesToRemove)
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
		int id = node.getId();
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
		 Integer id = oldIds.get(key);
		 if (id != null)
			 return find(id);
		 gensym(key);
		return null;
	}
	
	public DataNode find(int key)				
	{
		 DataNode n = dataNodeMap.get(key);	
		return n;
	}
	
	public int cloneResourceId(String oldId)	{		return gensym();	}
	//------------------------------------------------------------------------- GROUPS
//	Map<String, GPMLGroup> groups = new HashMap<String, GPMLGroup>();
	// move to GPML
	public void addGroup(DataNodeGroup grp) {
		int id = grp.getGraphId();
		groupMap.put(id,grp);
		dataNodeMap.put(grp.getId(),grp);
	}
//	public Collection<GPMLGroup> getGroups() { return groups.values();	}
	
	// **-------------------------------------------------------------------------------
//	public List<Interaction> connectSelectedNodes()		
//	{  
//		List<Interaction> edges = new ArrayList<Interaction>();
//		List<VNode> selection = controller.getSelection();
//		for (int i=0; i<selection.size()-1; i++)
//		{
//			VNode start = selection.get(i);
//			if (start.getShape() instanceof Line) continue;
//			for (int j=0; j < selection.size(); j++)
//			{
//				if (i == j) continue;
//				VNode end = selection.get(j);
//				if (end.getShape() instanceof Line) continue;		//TODO add anchor
//				
//				if (downRightAndClose(start, end) || selection.size() == 2)
//					edges.add(new Interaction(this, start, end, null));
//			}
//		}
//		return edges;
//	}
//	private boolean downRightAndClose(VNode start, VNode end) {
//		double startX = start.getLayoutX();
//		double startY = start.getLayoutY();
//		double endX = end.getLayoutX();
//		double endY = end.getLayoutY();
//
//		double SLOP = 20;
//		if (endY < startY - SLOP) return false;
//		if (endX < startX - SLOP) return false;
//		if (endY - startY > 2 * start.getHeight()) return false;
//		if (endX - startX > 2 * start.getWidth()) return false;
//
//		return true;
//	}
	// **-------------------------------------------------------------------------------
	public boolean containsEdge(Edge a) {
		for (Edge ed : getEdges())
		{
			if (a == ed) return true;
			if (a.getSourceid() == ed.getSourceid())
				if (a.getTargetid() == ed.getTargetid())
					return true;
		}
		return false;
	}

//	public Edge addEdge(DataNode start, DataNode end)		
//	{  
//		AttributeMap attributes = new AttributeMap();
//		String linetype = controller.getActiveLineType();
//		ArrowType arrow = controller.getActiveArrowType();
//		attributes.put("ArrowType", arrow.toString());
//		attributes.put("LineType", linetype);
//		Interaction edge = new Interaction(this, start, end, attributes);
//		controller.addInteraction(edge);
//		edge.connect();
//		return edge;
//	}
	
	public void addEdge(Interaction e)			{  interactionMap.put(e.get("GraphId"), e);	}
	// **-------------------------------------------------------------------------------
//	public void removeEdge(Edge edge)			
//	{  						
//		edge.removeListeners();
//		edge.getEdgeLine().dispose();
//		interactionMap.remove(edge.getGraphId());	
//	}
//	
//	public void connectAllEdges() {
//		for (int z = interactionMap.size()-1; z >= 0; z--)
//		{
//			Edge e = interactionMap.get(z);
//			e.connect();
//		}
//	}
	

	// **-------------------------------------------------------------------------------
	static public String describe(DataNode node)	{	return node.toGPML();	}
//	static public String describe(Layer node)	{	return node.getName();	}
//	static public String describe(Node node)	{	return node.getClass().getSimpleName() + ": " + node.getId() + " " +
//				StringUtil.asString(node.getBoundsInParent());	}
	static String getBoundsString(double x, double y, double w, double h)	{
	 return String.format("x=%.1f, y=%.1f, width=%.1f, height=%.1f", x, y, w, h);
	}
	
	Map<String, Integer> oldIds = new HashMap<String, Integer>();
	
	public int gensym()	{		return ++nodeCounter;	}
	public int gensym(String oldSymbol)	
	{		
		Integer extant = oldIds.get(oldSymbol);
		if  (extant != null && extant > 0) return extant;
		Integer newId =	gensym();
		oldIds.put(oldSymbol, newId);
		return newId;
	}
	
	
	int verbose = 0;
	public void setAttributes(DataNode shape, AttributeMap map)
	{
		if (verbose>0) System.out.println(map.toString());
		for (String k : map.keySet())
		{
//			String val = map.get(k);
//			if (k.equals("GraphId"))			shape.setId(val);
//			double d = StringUtil.toDouble(val);			// exception safe:  comes back NaN if val is not a number
//			if (shape instanceof Rectangle2D.Double)
//			{
//				Rectangle2D.Double r = (Rectangle2D.Double) shape;
//				if (k.equals("x"))				r.setX(d);
//				else if (k.equals("y"))			r.setY(d);
//				else if (k.equals("width"))		r.setWidth(d);
//				else if (k.equals("height"))	r.setHeight(d);
//			}
//			if (shape instanceof Circle2D)
//			{
//				Circle2D circ = (Circle2D) shape;
//				if (k.equals("centerX"))		circ.setCenterX(d);
//				else if (k.equals("centerY"))	circ.setCenterY(d);
//				else if (k.equals("radius"))	circ.setRadius(d);
//			}
//			if (shape instanceof Polygon)
//			{
//				Polygon poly = (Polygon) shape;
//				if (k.equals("points"))			parsePolygonPoints(poly, map.get(k));
//			}
//			if (shape instanceof Polyline)
//			{
//				Polyline poly = (Polyline) shape;
//				if (k.equals("points"))			parsePolylinePoints(poly, map.get(k));
//			}
//			if (shape instanceof Line)
//			{
//				Line line = (Line) shape;
//				if (k.equals("startX"))			line.setStartX(d);
//				else if (k.equals("startY"))	line.setStartY(d);
//				else if (k.equals("endX"))		line.setEndX(d);
//				else if (k.equals("endY"))		line.setEndY(d);
//			}
//			if (shape instanceof StackPane)
//			{
//				StackPane r = (StackPane) shape;
//				if (k.equals("x"))				r.setLayoutX(d);
//				else if (k.equals("y"))			r.setLayoutY(d);
//				else if (k.equals("width"))		{ r.setMinWidth(d); r.setMaxWidth(d); r.prefWidth(d); }
//				else if (k.equals("height"))	{ r.setMinHeight(d); r.setMaxHeight(d); r.prefHeight(d); }
//				else if (k.equals("rotate"))	{ r.setRotate(d); }
//				else if (k.equals("fill"))				
//				{
//					Background b = new Background(new BackgroundFill(Color.web(val), CornerRadii.EMPTY, Insets.EMPTY));
//					r.setBackground(b);
//				}
//			}
//			if (shape instanceof Shape)
//			try
//			{
//				Shape sh = (Shape) shape;
//				if (k.equals("fill") || k.equals("-fx-fill"))				
//				{
//					sh.setFill(Color.web(val));
//					String lastTwoChars = val.substring(val.length()-2);
//					int opac = Integer.parseInt(lastTwoChars, 16);
//					shape.setOpacity(opac / 255.);
//				}
//				else if (k.equals("stroke")  || k.equals("-fx-stroke"))		sh.setStroke(Color.web(val));
//				else if (k.equals("strokeWidth")  || k.equals("-fx-stroke-width"))	sh.setStrokeWidth(d);
////				else if (k.equals("selected"))		shape.setSelected(val);
//			}
//			catch (Exception e) { System.err.println("Parse errors: " + k); }
		}	
	}

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

	public void setColorByValue() {
//		clearColors();
//		for (DataNode node : getDataNodeMap().values())
//		{
//			Object val = node.get("value");
//			if (val != null)
//			{
//				double d = StringUtil.toDouble("" + val);
//				if (!Double.isNaN(d) && 0 <= d && 1 >= d)
//				{
//					Color gray = new Color(d,d,d, 1);
//					Shape shape = node.getStack().getFigure();
//					if (shape != null)
//					{
//						shape.setFill(gray);			// TODO set the attribute
//						if (gray.getRed() < 0.4 || gray.getBlue() < 0.4 || gray.getGreen() < 0.4)
//						{
//							node.getStack().getTextField().setStyle("-fx-text-fill: white");
//						}
//					}
//				}
//			}
//		}
	}	
	public void clearColors() {
//			
//		for (DataNode node : getDataNodeMap().values())
//		{
//			node.remove("value");
//			Shape shape = node.getStack().getFigure();
//			if (shape != null)
//			{	
//				shape.setFill(Color.WHITE);
//				node.getStack().getTextField().setStyle("-fx-text-fill: black");
//			}
//		}
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
	//-------------------------------------------------------------------------
//
//	public void resetEdgeTable()
//	{
//		System.out.println("resetEdgeTable: " + getEdges().size());
//		for (Interaction inter : getEdges())
//		{
//			inter.dump();
//			inter.connect();		
//		}
//	}
//	public XRefableSetRecord getXRec() {
//		XRefableSetRecord set = new XRefableSetRecord("XREFS");
//		set.getXRefableSet().addAll(getNodes());
//		return set;
//	}

	//---------------------------------------------------------------------------------------------
	public void addFields(List<QuadValue> records) {
		Set<String> targets = new HashSet<String>();
		for (QuadValue r : records)
			targets.add(r.getTarget());
		
		if (verbose>0)
		{
			System.out.println("Targets: ");
			for (String t : targets)
				System.out.println(t);
		}
//		
//		GPMLTreeTableView table = controller.getTreeTableView();
//		
//		for (String s : targets)
//			if (!table.columnExists(s))
//				table.addColumn(s);

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



