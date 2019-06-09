package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.pathvisio.core.view.Group;


/*
 *  Edge
 *  This is the entry in the edge table, not the actual Shapes on the screen
 *  see EdgeLine for the skin
 */
@SuppressWarnings("serial")
abstract public class Edge extends XRefable {
	private Path2D polyline;
	private Line2D.Double line;
	private Group curveGroup;
	private EdgeType type = EdgeType.simple;
	private List<GPMLPoint> points = new ArrayList<GPMLPoint>();
	private List<Anchor> anchors = new ArrayList<Anchor>();
 
	public Anchor findAnchor(int graphRef) 
	{
		for (Anchor a : anchors)
			if (a.getId() == graphRef)
				return a;
		return null;
	}
	public double getStartX()	{  return  (firstPoint() != null) ? firstPoint().getX() : 0;	}
	public double getStartY()	{  return  (firstPoint() != null) ? firstPoint().getY() : 0;	}
	public double getEndX()		{  return  (lastPoint() != null) ? lastPoint().getX() : 0;	}
	public double getEndY()		{  return  (lastPoint() != null) ? lastPoint().getY() : 0;	}

	public Point2D.Double firstPoint()
	{ 
		if (points.size() == 0) return null;
		return points.get(0).getPoint(); 
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
		int id = inModel.gensym();
		setGraphId(id);
		putInteger("GraphId", id);
	}
	
	// from parser
    public Edge(AttributeMap attr, Model inModel, List<GPMLPoint> pts, List<Anchor> anchors) 
    {
		this(inModel);
		addAll(attr);
		init(pts, anchors);
		DataNode start = model.find(getInteger("sourceid"));
    	if (start != null) 
    	{
    		startNode = start;
        	setSource(start.getLabel()); 
        	setSourceid(start.getId());
    	}
    	DataNode target = model.find(getInteger("targetid"));
    	if (target == null)
    	{
    		int ref = getInteger("targetid");
    		if (startNode != null)
    		{
    			Anchor anch = model.findAnchorByRef(ref);
        		if (anch != null)
        			setTargetid(anch.getId());
    		}
  		}
    	if (target != null) 
    	{
    		endNode = target;
    		setTarget(target.getLabel()); 
    		setTargetid(target.getId());
    	}
    	
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
 
	public Edge(Model inModel, DataNode start, DataNode end, AttributeMap attr) 		//, List<GPMLPoint> pts, List<Anchor> anchors
	{
		this(inModel);
		if (attr != null)    	addAll(attr);
//    	startNode = start.modelNode();  
//    	put("source", start.modelNode().getLabel());
    	putInteger("sourceid", start.getId());
    	setSource(start.getLabel()); 
    	setSourceid(start.getId());
    	
    	endNode = end;	
//    	put("target", end.modelNode().getLabel());
    	putInteger("targetid", end.getId());
    	setTarget(endNode.getLabel()); 
    	setTargetid(endNode.getId());
		if (getId() > 0)
		{	
			int id = model.gensym();
			setId(id);
		}
		init(null, null);		//pts, anchors
    }
	
//	abstract protected void init( List<GPMLPoint> pts, List<Anchor> anchors);

	//------------------------------------------------------------------------------------------
	public void init( List<GPMLPoint> pts, List<Anchor> anchors)
    {
		
//		edgeLine = new EdgeLine(this, pts, anchors);
		setInteractionProperty(get("ArrowHead"));
	   EdgeType edgeType = EdgeType.simple;
		String type = get("ConnectorType");
		if (type != null)  
			edgeType = EdgeType.lookup(type);
//		edgeLine.setEdgeType(edgeType);
//		if (anchors != null && anchors.size() > 0)
//			System.out.println("ANCHORS");
//		edgeLine.addAnchors(anchors);
		if (anchors != null)
			for (Anchor a : anchors)
				a.setInteraction(this);
		String colStr = get("Color");
		if (colStr != null)
		{
			setColor(colStr);
		}
		if ("Broken".equals(get("LineStyle")))		
		{
//			Double[] vals = {10.0, 5.0};
//			edgeLine.setStrokeDashArray(vals);
		}
		addListeners();
		
			//			connect(false);		// edgeLine.setStartPoint(startNode.center());
//			connect(true);		//edgeLine.setEndPoint(endNode.center());
			
			// TODO listen to layoutY too?
//		}
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
	
	private ArrowType getArrowType() {
	
	GPMLPoint lastpt = lastGPMLPoint();
	return lastpt.getArrowType();
	}
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
   public boolean references(int state)
   {
	   if (state == getInteger("GraphRef")) return true;
	   for (GPMLPoint pt : points)
		   if (state == pt.getGraphRef())
			   return true;
	   return false;
   }
	public boolean touches(int graphId)
	{
		if (graphId <= 0) return false;
		if (graphId == getInteger("sourceid"))	return true;
		if (graphId == getInteger("targetid"))		return true;
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
   public boolean isStart(DataNode n)	{  return n == startNode;	}
    public boolean isEnd(DataNode n)	{  return n == endNode;	}
    public boolean isEndpoint(DataNode n)	{  return isStart(n) || isEnd(n);	}

	//------------------------------------------------------------------------------------------
//	
//	ChangeListener<Bounds> startbounds = new ChangeListener<Bounds>()
//	{ 
//		@Override public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue)
//		{ connect(); }  //false
//	};
//
//	ChangeListener<Bounds> endbounds = new ChangeListener<Bounds>()
//	{ 
//		@Override public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue)
//		{ connect(); }   //true
//	};
//
//	ChangeListener<Number> startposition = new ChangeListener<Number>()
//	{ 
//		@Override public void changed(ObservableValue<? extends Number> observable,Number oldValue, Number newValue)
//		{ connect(); }   //false
//	};
//
//	ChangeListener<Number> endposition = new ChangeListener<Number>()
//	{ 
//		@Override public void changed(ObservableValue<? extends Number> observable,Number oldValue, Number newValue)
//		{ connect(); }   //true
//	};
//
//	
	void addListeners()
	{
//		if (startNode != null)
//		{
//			startNode.getStack().layoutBoundsProperty().addListener(startbounds);
//			startNode.getStack().layoutXProperty().addListener(startposition);
//		}
//		if (endNode == null){
//			String endId = get("end");
//			if (endId != null && getModel() != null)
//			{
//				DataNode mnode = getModel().getDataNode(endId);
//				if (mnode != null)
//					endNode = mnode;
//			}
//		}
//		if (endNode != null)
//		{	
//			endNode.getStack().layoutBoundsProperty().addListener(endbounds);
//			endNode.getStack().layoutXProperty().addListener(endposition);
//		}
	}
	public void removeListeners() 
	{
//		if (startNode != null)
//		{
//			startNode.getStack().layoutBoundsProperty().removeListener(startbounds);
//			startNode.getStack().layoutXProperty().removeListener(startposition);
//		}
//		if (endNode != null)
//		{	
//			endNode.getStack().layoutBoundsProperty().removeListener(endbounds);
//			endNode.getStack().layoutXProperty().removeListener(endposition);
//		}
	}
    //------------------------------------------------------------------------------------------
   @Override public String toString()
    {
//    	EdgeLine eLine = getEdgeLine();
//    	if (eLine == null) return "NO EDGELINE";
    	Point2D endpt = lastPoint();
    	return "Edge from " + (startNode == null ? "Null" : startNode.getName()) + //" @ " + StringUtil.asString(eLine.firstPoint())  ) +
    			" to "  + (endNode == null ? "Null" : endNode.getName());  //" @ " + StringUtil.asString(endpt));
    }

//   public boolean isSelected()  	 {	   return getEdgeLine().isSelected();   }
//   public void select(boolean on)   {	   getEdgeLine().select(on);   }
    //------------------------------------------------------------------------------------------
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
	public String getStartName() {
		return getStartNode() == null ?  get("sourceid") : getStartNode().getName();
	}
	public String getEndName() {
		return getEndNode() == null ? get("targetid") : getEndNode().getName();
	}

	public Anchor findAnchorByRef(int targRef) {		return getModel().findAnchorByRef(targRef);	}
	
	public Anchor addAnchorAt(Point2D hitPt) {			// comes from clicking on an edge while dragLining
		
		double startX = getStartX();		// TODO assuming simple line
		double endX = getEndX();
		double hitX = hitPt.getX();
		double hitY = hitPt.getY();
		double relVal = (hitX-startX) / (endX-startX);
		
		Model m = getModel();
		AttributeMap map = new AttributeMap();
		map.putDouble("Position", relVal);
		map.putInteger("GraphId", m.gensym());
		map.putDouble("CenterX", hitX);
		map.putDouble("CenterY", hitY);
		map.putDouble("Width", 10);
		map.putDouble("Height", 10);
		map.put("ShapeType", "Anchor");
		int myId = getId();
		Anchor a = new Anchor(map, m, myId);
		addAnchor(a);
//		m.getPasteboard().connectTo(a.getStack(), RelPosition.ZERO);
//		m.getPasteboard().resetTool();
		return a;
	}
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
	
	protected DataNode startNode=null, endNode=null;
	public void setStartNode(DataNode dn)	{ 	startNode = dn;	}
	public void setEndNode(DataNode dn)		{  endNode = dn;	}
	public DataNode getStartNode()		{ 	return startNode;	}
	public DataNode getEndNode()		{ 	return endNode;	}

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

	private String source = new String();
	public String getSource()  { return source;}
	public void setSource(String s)  { source = s;}

	private String target = new String();	
	public String getTarget()  { return target;}
	public void setTarget(String s)  { target = s;}
	
	private Integer sourceid = new Integer(0);	
	public int getSourceid()  { return sourceid;}
	public void setSourceid(int s)  { sourceid = s;}
	
	private Integer targetid = new Integer(0);	
	public Integer getTargetid()  { return targetid; }
	public void setTargetid(int s)  { targetid = s;}
	
	private String interaction = new String();	
	public String getInteraction()  { return interaction;}
	public void setInteractionProperty(String s)  { interaction = s;}

}

