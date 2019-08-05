package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/*
 *  Edge
 *  This is the entry in the edge table, not the actual Shapes on the screen
 *  see EdgeLine for the skin
 */
@SuppressWarnings("serial")
 public class Edge  {
	private Interaction parent;
	private Path2D polyline;
	private Line2D.Double line;
//	private Group curveGroup;
	private EdgeType type = EdgeType.simple;
	private List<GPMLPoint> points = new ArrayList<GPMLPoint>();
	private List<Anchor> anchors = new ArrayList<Anchor>();
	private XRefable attributes = new XRefable();
	public Anchor findAnchor(String graphRef) 
	{
		for (Anchor a : anchors)
			if (a.getId().equals(graphRef))
				return a;
		return null;
	}
	public double getStartX()	{  return  (firstPoint() != null) ? firstPoint().getX() : 0;	}
	public double getStartY()	{  return  (firstPoint() != null) ? firstPoint().getY() : 0;	}
	public double getEndX()		{  return  (lastPoint() != null) ? lastPoint().getX() : 0;	}
	public double getEndY()		{  return  (lastPoint() != null) ? lastPoint().getY() : 0;	}

	public List<GPMLPoint> getPoints() {	return points;	}
	public Point2D.Double firstPoint()
	{ 
		if (points.size() == 0) return null;
		return points.get(0).getPoint(); 
	}
	
	public String pointString(Point2D.Double pt)
	{ 
		if (pt == null) return "N/A";
		return String.format("(%.2f, %.2f)", pt.getX(), pt.getY());
	}
	
	
	public GPMLPoint firstGPMLPoint()
	{ 
		if (points.size() == 0) return null;
		return points.get(0); 
		}
	public void setFirstPoint(Point2D.Double pt)
	{ 
		if (points.size() == 0) return;
		GPMLPoint gpt = points.get(0); 
		gpt.setPoint(pt);
	}
	public void setStartPoint(Point2D.Double pt)	{ 		setFirstPoint(pt);	}
	public void setEndPoint(Point2D.Double pt)		{ 		setLastPoint(pt);	}
	public Point2D.Double forelastPoint()
	{ 
		if (points.size() < 2) return firstPoint();
		return points.get(points.size()-2).getPoint(); 
	}
	public Point2D.Double lastPoint()
	{ 
		if (points.size() == 0) return null;
		return points.get(points.size()-1).getPoint(); 
	}
	public GPMLPoint lastGPMLPoint()
	{ 
		if (points.size() == 0) return null;
		return points.get(points.size()-1); 
	}
	public void setLastPoint(Point2D.Double pt)
	{ 
		if (points.size() == 0) return;
		GPMLPoint gpt = points.get(points.size()-1); 
		gpt.setPoint(pt);
	}

	//----------------------------------------------------------------------
//	public Edge(VNode start, VNode end) 
//    {
//		this(start, end, null, null, null);
//    }
//	
	public Edge(Model inModel) 
    {
		model = inModel;
//		int id = inModel.gensym();
//		attributes.setGraphId(id);
//		putInteger("GraphId", id);
	}
	
	public void putInteger(String a, int i) { 	attributes.putInteger(a, i);	}
	public void put(String a, String i) { 	attributes.put(a, i);	}
	public String get(String a) { 	return attributes.get(a);	}
	public int getInteger(String a) { 	return attributes.getInteger(a);	}
	public Color getColor(String a) { 	return attributes.getColor(a);	}

	public String getId() { 	return attributes.get("GraphId");	}
	
	
	// from parser
    public Edge(Interaction parent, AttributeMap attr, Model inModel, List<GPMLPoint> pts) 	//, List<Anchor> anchors
    {
		this(inModel);
		attributes.addAll(attr);
		init(parent, pts);		//, anchors
		DataNode start = model.find(get("sourceid"));
    	if (start != null) 
         	setSourceid(start.getId());
    	DataNode target = model.find(get("targetid"));
    	if (target != null) 
    		setTargetid(target.getId());
    	
      }
 
//    public Edge(GPMLPoint startPt, GPMLPoint endPt, double thickness, Model inModel) 
//    {
//		model = inModel;
//    	endNode = startNode = null;
//		init(null, null);
//		edgeLine.addPoint(startPt);
//		edgeLine.addPoint(endPt);
////		model.addEdge(this);
//      }
//	public Edge(Interaction dad, Model inModel, DataNode start, DataNode end, AttributeMap attr) 		//, List<GPMLPoint> pts, List<Anchor> anchors
//	{
//		this(inModel);
//		parent = dad;
//		if (attr != null)    	attributes.addAll(attr);
////    	startNode = start.modelNode();  
////    	put("source", start.modelNode().getLabel());
//    	put("sourceid", start.getId());
//    	setSource(start.getLabel()); 
//    	setSourceid(start.getId());
//    	
//    	endNode = end;	
////    	put("target", end.modelNode().getLabel());
//    	put("targetid", end.getId());
//    	setTarget(endNode.getLabel()); 
//    	setTargetid(endNode.getId());
////		if (getId() > 0)
////		{	
////			int id = model.gensym();
////			attributes.setId(id);
////		}
//		init(parent, null);		//pts, anchors
//    }
	
//	abstract protected void init( List<GPMLPoint> pts, List<Anchor> anchors);

	//------------------------------------------------------------------------------------------
	public void init( Interaction parent, List<GPMLPoint> pts)  //, List<Anchor> anchors
    {
		
//		edgeLine = new EdgeLine(this, pts, anchors);
		this.parent = parent;
		setInteractionProperty(get("ArrowHead"));
	   EdgeType edgeType = EdgeType.simple;
	   System.out.println(pts.size() + " points");
		String type = get("ConnectorType");
		if (type != null)  
			edgeType = EdgeType.lookup(type);
		for (GPMLPoint pt : pts)
			points.add(pt);
//		edgeLine.setEdgeType(edgeType);
//		if (anchors != null && anchors.size() > 0)
//			System.out.println("ANCHORS");
//		edgeLine.addAnchors(anchors);
//		if (anchors != null)
//			for (Anchor a : anchors)
//				a.setInteraction(parent);
		String colStr = get("Color");
		if (colStr != null)
			setColor(colStr);
    }

//	public void connect() {
////		if (edgeLine == null)
////			return;  // error?
//		GPMLPoint gpt = firstGPMLPoint();
//		if (gpt == null) return;
//		Point2D.Double startPt = gpt.getPoint();
//		if (startNode != null)
//			startPt = startNode.getAdjustedPoint(gpt);
//		setFirstPoint(startPt);
//			
//		
//		GPMLPoint lastpt = lastGPMLPoint();
//		Point2D.Double endpt = lastpt.getPoint();
//		if (endNode != null)
//		{
//			endpt = endNode.getAdjustedPoint(lastpt);
//			setLastPoint(endpt);
//		}
//		setEndPoint(endpt);
////		edgeLine.setArrowType(lastpt.getArrowType());
//		connect(getArrowType());
//
//	}
//	
//	private ArrowType getArrowType() {
//	
//	GPMLPoint lastpt = lastGPMLPoint();
//	return lastpt.getArrowType();
//	}
//		
//		
//		if (endNode == null) {
//			String val = get("targetid");
//			DataNode mNode = startNode.getModel().getResourceByKey(val);
//			if (mNode != null)	
//				endNode = mNode;
//		if (endNode == null)
//		{
//			Anchor anch = startNode.getModel().findAnchorById(val);
//			System.out.println("anch " + anch);
//		}
//			Shape shape = getEdgeLine().getHead();  //getShape();  //endNode == null ? null : endNode.getStack().getFigure();
////					startNode.getModel().findShape(edgeLine.endGraphId()) : 
//			if (shape != null) 
//				pt = boundsCenter(shape);
//			else 
//				System.out.println("no shape");
//		} else
//		if (pt.getX() < 1)
//		{
////			edgeLine.setVisible(false);
//			System.out.println("zerro");
//		}
////		System.out.println(String.format("End: [ %.2f, %.2f]",pt.getX(), pt.getY()));
////		Shape head = edgeLine.makeArrowhead();
//
//
//		if (verbose)
//		{
//			String startStr = startNode == null ? "NULL" : startNode.getStack().getText();
//			String endStr = endNode == null ? "NULL" : endNode.getStack().getText();
//			System.out.println("connect " + startStr + " to " + endStr);
//		}
//	}
	boolean verbose = false;
	
//	public Point2D boundsCenter(Shape s)	{
//		Bounds b = s.getBoundsInParent();
//		double x = (b.getMinX() + b.getMaxX()) / 2;
//		double y = (b.getMinY() + b.getMaxY()) / 2;
//		return new Point2D(x, y);		
//	}

//   public void connect(boolean atEnd)
//   {
//	   if (startNode == null || endNode == null || edgeLine == null)
//		   return;
//		if (atEnd)		edgeLine.setEndPoint(endNode.getStack().center());
//   		else 			edgeLine.setStartPoint(startNode.getStack().center());
//   		edgeLine.connect();
//   		System.out.println("connect");
//   }
   public boolean references(String state)
   {
	   if (state.equals(get("GraphRef"))) return true;
	   for (GPMLPoint pt : points)
		   if (state.equals(pt.getGraphRef()))
			   return true;
	   return false;
   }
	public boolean touches(String graphId)
	{
		if (graphId == null) return false;
		if (graphId == get("sourceid"))	return true;
		if (graphId == get("targetid"))		return true;
		for (Anchor a : getAnchors())
			if (graphId == a.getGraphId())
				return true;
		return false;
	}
//
//    private void dumpPoints(GPMLPoint a, GPMLPoint b, Point2D c) {
//    	if (a != null) System.out.println(String.format("a: (%.2f, %.2f )", a.getX(), a.getY()));
//    	if (b != null) System.out.println(String.format("b: (%.2f, %.2f )", b.getX(), b.getY()));
//    	if (c != null) System.out.println(String.format("c: (%.2f, %.2f )", c.getX(), c.getY()));
//}
 //----------------------------------------------------------------------


    //------------------------------------------------------------------------------------------
   public boolean isStart(DataNode n)	{  return n != null && n.equals(getSourceid());	}
    public boolean isEnd(DataNode n)	{ return n != null && n.equals(getTargetid());	}
    public boolean isEndpoint(DataNode n)	{  return isStart(n) || isEnd(n);	}


	void addListeners()
	{
	}
	public void removeListeners() 
	{
	}
    //------------------------------------------------------------------------------------------
   @Override public String toString()
    {
    	return "Edge from " + getSourceid() +" to "  + getTargetid(); 
    }

    //------------------------------------------------------------------------------------------
    public int getNPoints()    {   return points.size();  }

    	public String getPointsStr()
	{
//		if (edgeLine == null) return "";
//		List<GPMLPoint> pts = getPoints();
//		if (pts == null) return "";
		StringBuilder builder = new StringBuilder();
		for (GPMLPoint pt : points)
			builder.append(pt.toGPML());
		return builder.toString();
	}
    
    public List<Anchor> getAnchors() { return getAnchors();  }

    public String getAnchorsStr ()
	{
		List<Anchor> anchors = getAnchors();
		StringBuilder builder = new StringBuilder();
		for (Anchor a : anchors)
			builder.append(a.toGPML());
		return builder.toString();
	}
//	public String getStartName() {
//		return getStartNode() == null ?  get("sourceid") : getStartNode().getName();
//	}
//	public String getEndName() {
//		return getEndNode() == null ? get("targetid") : getEndNode().getName();
//	}

	public Anchor findAnchorByRef(String targRef) {		return getModel().findAnchorByRef(targRef);	}
	
//	public Anchor addAnchorAt(Point2D hitPt) {			// comes from clicking on an edge while dragLining
//		
//		double startX = getStartX();		// TODO assuming simple line
//		double endX = getEndX();
//		double hitX = hitPt.getX();
//		double hitY = hitPt.getY();
//		double relVal = (hitX-startX) / (endX-startX);
//		
//		Model m = getModel();
//		AttributeMap map = new AttributeMap();
//		map.putDouble("Position", relVal);
//		map.putInteger("GraphId", m.gensym());
//		map.putDouble("CenterX", hitX);
//		map.putDouble("CenterY", hitY);
//		map.putDouble("Width", 10);
//		map.putDouble("Height", 10);
//		map.put("ShapeType", "Anchor");
//		String myId = getId();
//		Anchor a = new Anchor(map, m, myId);
//		addAnchor(a);
////		m.getPasteboard().connectTo(a.getStack(), RelPosition.ZERO);
////		m.getPasteboard().resetTool();
//		return a;
//	}
	public void addAnchor(Anchor a) 	{ 	anchors.add(a);	}

	//----------------------------------------------------------------------
//	protected AttributeMap attributes = new AttributeMap();
//	public AttributeMap getAttributes() {		return attributes;	}
//	
//	protected EdgeLine edgeLine;
//	public EdgeLine getEdgeLine() 	
//	{
//		if (edgeLine.getLine() != null)
//			return edgeLine;	
//		return null;
//	}
	
//	protected DataNode startNode=null, endNode=null;
//	public void setStartNode(DataNode dn)	{ 	startNode = dn;	}
//	public void setEndNode(DataNode dn)		{  endNode = dn;	}
//	public DataNode getStartNode()		{ 	return startNode;	}
//	public DataNode getEndNode()		{ 	return endNode;	}

	public Model getModel()		{ 	return model;	}
	protected Model model = null;
	protected int zOrder;
	public int getz() 				{	return zOrder;	}
	public void setz(int i) 		{	zOrder = i;	}
	
	
	public double getStrokeWidth() 
	{	String s =  get("LineThickness"); 
		if (s == null) return 1.4;
		if (StringUtil.isNumber(s)) return StringUtil.toDouble(s);	
		return 1.4;
	}
	public String getLayer()		{ 	return get("Layer");	}
	private Color color = Color.BLACK;
	public Color getColor() 		{	return color;	}
	public void setColor(Color c) 	{	color = c;	}
	public void setColor(String s) 	{	color = StringUtil.readColor(s);	}

//	private String source =  "";
//	public String getSource()  { return source;}
//	public void setSource(String s)  { source = s;}
//
//	private String target =  "";	
//	public String getTarget()  { return target;}
//	public void setTarget(String s)  { target = s;}
//	
//	private String sourceid = "";	
//	public String getSourceid()  { return sourceid;}
//	public void setSourceid(String s)  { sourceid = s;}
//	
//	private String targetid = "";	
	public String getTargetid()  { return attributes.get("targetid"); }
	public void setTargetid(String s)  {  attributes.put("targetid", s);	}
	public String getSourceid()  { return attributes.get("sourceid"); }
	public void setSourceid(String s)  {  attributes.put("sourceid", s);	}
//	
	private String interaction = new String();	
	public String getInteraction()  { return interaction;}
	public void setInteractionProperty(String s)  { interaction = s;}
	public XRefable getAttributes() {		return attributes;	}
	public String getGraphId() {		return attributes.getGraphId();	}
	public String getDbid() {		return attributes.getDbid();	}
	public String getDatabase() {		return attributes.getDatabase();	}
	public String getType() {return attributes.getType();	}
	public String getSafe(String s) {return attributes.getSafe(s);	}
	public String getName() {return attributes.getName();	}
	public double getDouble(String a) {return attributes.getDouble(a);	}
	public void putDouble(String a, double d) { attributes.putDouble(a,d);	}
}

