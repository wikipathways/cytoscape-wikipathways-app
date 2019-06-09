//package org.wikipathways.cytoscapeapp.impl.gpml;
//
//import java.awt.Color;
//import java.awt.Font;
//import java.awt.Label;
//import java.awt.Rectangle;
//import java.awt.Shape;
//import java.awt.event.MouseEvent;
//import java.awt.geom.Line2D;
//import java.awt.geom.Path2D;
//import java.awt.geom.Point2D;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.tools.Tool;
//
//import org.pathvisio.core.view.Group;
//import org.pathvisio.core.view.Line;
//import org.w3c.dom.Node;
//import org.wikipathways.cytoscapeapp.impl.gpml.RelPosition.Pos;
//
//
//
///* 
// * EdgeLine 
// * Isolate the drawing elements from the nodes/attribute definition
// * so that the edge can be switched from simple line to curve to elbow
// * without having to destroy the edge at the model level.
// * 
// * Anchors are endpoints of other interactions, specified by a 0-1 number 
// * representing how far along the path from source to target the anchor is positioned
// *
// *  All edges are directional. We support arrow heads but not tails (or double headed arrows). 
// *  points is the array of all segment ends.  The straight segments are used to derive the curves.
// *  There is also a centerPoint object per segment to allow the user to add segments by moving centerPoint.
// *  There are Shapes defined for head and tail, but only head is used.
// *  Interaction is the model  that corresponds to this view.
// */
//public class EdgeLine extends Group {
//
//	 //----------------------------------------------------------------------
//	private EdgeLine()
//	{
//	   	polyline = null;
//	   	line = null;
//	   	head =  tail = null;
//		interaction = null;			
////		arrowType = ArrowType.arrow;			// kept in last GPMLPoint
//
//		startPointShape = new Circle(50, 50, 10);
//		startPointShape.setFill(Color.HOTPINK);
//
//		endPointShape = new Circle(50, 50, 10);
//		endPointShape.setFill(Color.HOTPINK);
//		
//		getChildren().addAll(startPointShape, endPointShape);
//
//		centerPoints = new ArrayList<Circle>();
//	}
////	public EdgeLine(EdgeType edgeType, Point2D start, ArrowType arrow)
////	{
////		this(edgeType, start);
//////		arrowType = arrow;
////	}
//	private Pasteboard canvas;
//	public EdgeLine(EdgeType edgeType, Point2D start, Pasteboard board)
//	
//	{
//	   this();
//	   canvas = board;
////	   srcX = startX;
////	   	srcY = startY;
////	   	targX = endX;
////	    targY = endY;
//	   Line line = getLine();
//	   line.setStartX(start.getX());
//	   line.setStartY(start.getY());
//	   line.setEndX(start.getX());
//	   line.setEndY(start.getY());
//	   setStartPoint(start);
//	    type = edgeType;
//		setMouseTransparent(true);
//	}
//
//	private Tool getTool() { return  canvas.getTool(); }
//	Point2D hitPt = null;
//	
//	public EdgeLine(Edge inter, List<GPMLPoint> pts, List<Anchor> anchorList) 
//	{
//	   	this();
//		if (pts != null) 
//			points.addAll(pts);
//		interaction = (Interaction) inter;
//		if (inter != null)
//			   canvas = inter.getModel().getController().getPasteboard();
//
//		if (anchorList != null)
//			anchors.addAll(anchorList);
////		for (Anchor anchor : anchors)
////			System.out.println("A: " +anchor.toString());
//		addGraphIdDisplay();
//		setMouseTransparent(false);
//		addCenterPoint();
//		addEventHandler(MouseEvent.MOUSE_ENTERED, e -> 
//		{ 	
//			boolean hiliteEdge = canvas.isDraggingLine();
//			Tool tool = getTool();
//			hiliteEdge &= tool.connectsToEdges();
//			if (line != null && hiliteEdge)
//			{
//				line.getStyleClass().add("fatLine");
//				line.getStyleClass().remove("thinLine");
//				hitPt = new Point2D(e.getX(), e.getY());
//			}
//		} );
//		addEventHandler(MouseEvent.MOUSE_MOVED, e -> 
//		{ 	
//			boolean hiliteEdge = canvas.isDraggingLine();
//			Tool tool = getTool();
//			hiliteEdge &= tool.connectsToEdges();
//			if (line != null && hiliteEdge)
//			{
//				line.getStyleClass().add("fatLine");
//				line.getStyleClass().remove("thinLine");
//				hitPt = new Point2D(e.getX(), e.getY());
//			}
//	} );
//		addEventHandler(MouseEvent.MOUSE_EXITED, e -> 
//		{ 	
//			if (line != null)
//			{
//				line.getStyleClass().add("thinLine");
//				line.getStyleClass().remove("fatLine");
//				hitPt = null;
//		}
//		});
//		
//		addEventHandler(MouseEvent.MOUSE_PRESSED, e -> 
//		{ 	
//			if (hitPt != null)
//			{
//				System.out.println("addAnchor");
//				Anchor anchor = interaction.addAnchorAt(hitPt);
//			}
//		});
//		
//	}
//
//	private void addCenterPoint()
//	{
//		Circle c = new Circle(3);
//		centerPoints.add(c);
//		addCenterPointListeners(c);
//		getChildren().add(c);
//		
//	}
//	 //----------------------------------------------------------------------
////	private double srcX, srcY, targX, targY;
////	
////	public void setStart(Point2D pt) { srcX = pt.getX(); srcY = pt.getY(); }
////	public void setEnd(Point2D pt) { targX = pt.getX(); targY = pt.getY(); }
//
////	public void setEndX(double x) { targX = x; }
////	public void setEndY(double y) { targY = y; }
////	public void setStartX(double x) { srcX = x; }
////	public void setStartY(double y) { srcY = y; }
//	
//private void addCenterPointListeners(Circle c) {
//	ObservableMap<Object, Object> properties = getProperties(); 
//	BooleanProperty selectedProperty = (BooleanProperty) properties.get("selected"); 
//	if (selectedProperty != null) 
//		for (Shape centerPoint : centerPoints)
//			centerPoint.visibleProperty().bind(interaction.getModel().getController().getInspector().centerpointVisibleProperty());
//	c.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> 
//	{ 	
//		if (polyline == null)
//		{
//			polyline =getPolyline();
//			if (line != null)
//			{
//				getChildren().remove(line); 
//				line = null;
//			}
//		}
//		GPMLPoint pt = findGPMLPoint(e.getX(), e.getY());
//		if (pt == null)
//		{
//			pt = new GPMLPoint(e.getX(), e.getY());
//			pt.setInteraction(interaction);
//			points.add(1,pt);			/// TODO  fix index
//			// add two more midpoint circles
//		}
//		setEdgeType(EdgeType.polyline);
//		connect(null);
//		e.consume();
//	} );
//	c.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> 
//	{ 	
//		points.get(1).setX(e.getX());
//		points.get(1).setY(e.getY());				// TODO only works once
//		
//		c.setCenterX(e.getX());
//		c.setCenterY(e.getY());
//		connect(null);
//		select(true);
//		e.consume();
//	} );
//
//	
//}
//
//	private GPMLPoint findGPMLPoint(double x, double y) {
//	for (GPMLPoint pt : points)
//		if (nearby(pt, x, y))
//			return pt;
//	return null;
//	}
//
//	boolean nearby(GPMLPoint pt, double x, double y)
//	{
//		double epsilon = 12;
//		return Math.abs(pt.getX() - x) < epsilon && Math.abs(pt.getY() - y) < epsilon;
//	}
//	
//	//	public double getEndX(double x) { return targX; }
////	public double getEndY(double y) { return targY; }
////	public double getStartX(double x) { return srcX; }
////	public double getStartY(double y) { return srcY; }
//	Color stroke = Color.AQUA;
//	double strokeWidth = 2;
//	public void setStroke(Color c)
//	{ 
//		stroke = c; 
//		getLine().setStroke(c);
//	}
//	public void setStrokeWidth(double w) { strokeWidth = w; getLine().setStrokeWidth(w);}
//	public double getStrokeWidth() { return strokeWidth; }
//	 //----------------------------------------------------------------------
//
//	private Label graphIdLabel;
//	public void addGraphIdDisplay()
//	{
//		if (interaction == null) return;
//		int id = interaction.getInteger("GraphId"); // ("GraphId");
//		if (id == 0) return;
////		System.out.println("addGraphIdDisplay: " + id);
//		graphIdLabel = new Label("" + id);
//		graphIdLabel.setPrefWidth(100);
//		graphIdLabel.setTextFill(Color.RED);
//		graphIdLabel.setPrefHeight(20);
//		graphIdLabel.setMouseTransparent(true);
//		graphIdLabel.setBackground(Backgrounds.transparent());   
//		graphIdLabel.setFont(new Font(9));
//		graphIdLabel.setAlignment(Pos.TOP_LEFT);
////		graphIdLabel.setTranslateY(-20);
//		graphIdLabel.visibleProperty().bind(interaction.getModel().getController().getInspector().graphIdsVisibleProperty());
//		
//		getChildren().add(graphIdLabel);
//	}	
//	private void setLabelPosition(Point2D pt)
//	{
//		if (graphIdLabel != null)
//		{
//			graphIdLabel.setTranslateX(pt.getX());
//			graphIdLabel.setTranslateY(pt.getY());
//		}
//	}
//	 //----------------------------------------------------------------------
//	private Circle startPointShape;
//	private Circle endPointShape;
//	private List<Circle> centerPoints;
//	private void setStartAndEndMarkers()
//	{
//		startPointShape.setCenterX(getStartX());
//		startPointShape.setCenterY(getStartY());
//		endPointShape.setCenterX(getEndX());
//		endPointShape.setCenterY(getEndY());
//		startPointShape.toFront();
//		endPointShape.toFront();
//	}
//	
//	private void setCenterPosition(int index, Point2D pt)
//	{
//		Circle cpt;
//		if (index <= centerPoints.size())
//		{
//			cpt = new Circle(3);
//			centerPoints.add(cpt);
//		}
//		else cpt = centerPoints.get(index);
//			
//		cpt.setCenterX(pt.getX());
//		cpt.setCenterY(pt.getY());
//	}
//	
//	public void setCenterpointVis(boolean visible)
//	{
//		endPointShape.setVisible(visible);
//		startPointShape.setVisible(visible);
//		for (Circle c : centerPoints)
//			c.setVisible(visible);
//	}
//		 //----------------------------------------------------------------------
//	private Interaction interaction;		// the model corresponding to this geometry
////	private ArrowType arrowType;
//	private Path2D polyline;
//	private Line2D.Double line;
//	private Group curveGroup;
//	private EdgeType type = EdgeType.simple;
//	private List<GPMLPoint> points = new ArrayList<GPMLPoint>();
//	private List<Anchor> anchors = new ArrayList<Anchor>();
// 
//	public Anchor findAnchor(int graphRef)
//    {
//	  if (graphRef <= 0) return null;
////	  System.out.println("searching anchors " + getAnchors().size() + " " + graphRef);
//	  for (Anchor a : getAnchors())
//	  {
////		  System.out.println("anchor " + a.getGraphId());
//		  if (graphRef == a.getGraphId())
//			   return a;
//	  }
//	   return null;
//    }
//
//	private Point2D arrowPt = new Point2D(0,0);			// the arrowhead is drawn to stop short of the node edge
//	public double getArrowX()		{ return arrowPt.getX();	}
//	public double getArrowY()		{ return arrowPt.getY();	}
//	public void setArrowPt(double x, double y) { arrowPt = new Point2D(x,y); }
//	public void setArrowPt(Point2D pt) { arrowPt = pt; }
//	public void setAnchorVis(boolean visible)
//	{
//		for (Anchor a: anchors) 
//			a.getShape().setVisible(visible);
//	}
//
//	public Polyline getPolyline()	
//	{
//		if (polyline == null)
//		{
//			polyline  = new Polyline();
//			polyline.setStyle("");		// TODO setStyleClass
//			getChildren().add(polyline);
//		}
//		return polyline;	
//	}
//	
//	public Line2D.Double getLine()			
//	{
//		if (line == null)
//		{
//			line  = new Line2D.Double();
////			line.setStyle("");		// TODO setStyleClass
//			getChildren().add(line);
//			line.setOnMouseClicked(e -> { interaction.getEdgeLine().select(true); });
//		}
//		return line;	
//	}
//
////	public CubicCurve getCurve()			
////	{
////		if (curve == null)
////		{
////			curve  = new CubicCurve();
////			curve.setStyle("");		// TODO setStyleClass
////			getChildren().add(curve);
////		}
////		return curve;	
////	}
//	//----------------------------------------------------------------------
//	public   Point2D getStartPoint()	{ return new Point2D(getStartX(), getStartY());  }
//	public   Point2D getEndPoint()		{ return new Point2D(getEndX(), getEndY());  }
//
//
//	private Shape head, tail;
//	public Shape getHead()			{ return head;	}
//	public Shape getTail()			{ return tail;	}
////	public Shape getCenter()		{ return centerPoint;	}
//	public EdgeType getEdgeType()	{ return type;	}
//	public void setEdgeType(EdgeType t)	{ type = t;	}
//	public String getLayer()			{ 	return interaction.getLayer();	}
//
////	double relX = 0;		// center + this * width/2  1= right, -1= left
////	double relY = 0;
////	public double getRelX()		{ return relX;	}
////	public double getRelY()		{ return relY;	}
////	public void setRelX(double x)		{  relX = x;	}
////	public void setRelY(double y)		{  relY = y;	}
//	public List<GPMLPoint> getPoints() 	{ 	return points;	}
//	
//	public List<Anchor> getAnchors() 	{ 	return anchors;	}
//	public void addAnchor(Anchor a) 	{ 	anchors.add(a);	}
////	public void addAnchors(List<Anchor> a) 	{ 	if (a != null) anchors.addAll(a);	}
//	public void removeAnchor(Anchor a) 	{ 	anchors.remove(a);	}
//
//	private boolean selected;
//	public boolean isSelected()		{ 	return selected;	}
//
//	public void select(boolean b)		
//	{ 	
//		selected = b;	
//		Color c = lineColor(selected);
//		if (line != null) line.setStroke(c);
//		if (polyline != null) polyline.setStroke(c);
////		if (curveGroup != null) curveGroup.getChildren().forEach(setStroke(c));
//		if (head != null) {		head.setStroke(c);  head.setFill(c);  }
//	}
//	
//	Color lineColor(boolean selected)
//	{
//		return selected ? Color.RED : Color.BLACK; 
//	}
//	
//	//----------------------------------------------------------------------
//	public Point2D.Double getPointAlongLine(double position) {
//		Point2D.Double startPt = getStartPoint();   //firstPoint();
//		Point2D.Double endPt = getEndPoint();  //lastPoint();
//		if (type == EdgeType.simple) 
//			return getPointAlongSegment(position, startPt, endPt);
//	
//		int size = points.size();
//		if (size < 2) return startPt;
//		startPt = points.get(size-2).getPoint();
//		return getPointAlongSegment(position, startPt, endPt);
//
////		SegmentList segments = new SegmentList(interaction.getModel(), points);
////		
////		return segments.fromLineCoordinate(position);
//		}
//	
//	
//	public Point2D.Double getPointAlongSegment(double position, Point2D startPt, Point2D endPt) {
//		double x = startPt.getX() + ((endPt.getX() - startPt.getX()) * position);
//		double y = startPt.getY() + ((endPt.getY() - startPt.getY()) * position);
//		return new Point2D(x,y);
//	}
//	//-----------------------------------------------------
//	Double[] strokeDashArray;
//	public void setStrokeDashArray(Double[] vals)	{	strokeDashArray = vals;	}
//
//	//----------------------------------------------------------------------
//	private void setPoint(Point2D src, GPMLPoint targ)
//	{
//		targ.setX(src.getX());
//		targ.setY(src.getY());
//	}
//
//	//----------------------------------------------------------------------
//	public void setStartPoint(GPMLPoint startPt) {		setStartPoint(startPt.getPoint());	}
//	
//	public void setStartPoint(Point2D startPt) {
//		if (startPt == null) return;
//		if (points.size() < 1)
//			points.add(new GPMLPoint(startPt, interaction));
//		setPoint(startPt, points.get(0));
//		if (line != null)
//		{
//			line.setStartX(startPt.getX());
//			line.setStartY(startPt.getY());
//		}
//		startPointShape.setCenterX(startPt.getX());
//		startPointShape.setCenterY(startPt.getY());
//	}
//	
//	public void setEndPoint(GPMLPoint startPt) {		setEndPoint(startPt.getPoint());	}
//
//	public void setEndPoint(Point2D endPt) {
//		if (endPt == null) 								return;
//		if (endPt.getX() == 0 && endPt.getY() == 0)		return;
//		if (points.size() < 2)
//			points.add(new GPMLPoint(endPt));
//		setPoint(endPt, points.get(points.size()-1));
//		if (line != null)
//		{
//			line.setEndX(endPt.getX());
//			line.setEndY(endPt.getY());
//		}
//		endPointShape.setCenterX(endPt.getX());
//		endPointShape.setCenterY(endPt.getY());
//	}
//	//----------------------------------------------------------------------
//	public void dispose()
//	{
//		if (head != null) 	getChildren().remove(head);
//		getChildren().clear();	
//	}
//	
//  //----------------------------------------------------------------------
//	public void connect(ArrowType arrowType)
//	{
////		for (GPMLPoint pt : points)
////			pt.setXYFromNode();
//
//		if (head != null) 	
//			getChildren().remove(head);
//		
//		for (Anchor a : anchors)
//			a.resetPosition(interaction);
//	
//		setStartAndEndMarkers();
//		
//		switch (type)
//		{
//			case polyline:	polylineConnect(); 		break;
//			case elbow:		elbowConnect(); 		break;
//			case curved: 	curveConnect(); 	 	break;
//			default: 		linearConnect();
//		}
//		
//		head = makeArrowhead(arrowType);
//	 	
//
////		addTail();			not implemented here
//	}
//	
//	//----------------------------------------------------------------------
//	public int startGraphId()
//	{
//		if (points.size() == 0) return 0;
//		GPMLPoint pt = points.get(0);
//		return pt.getGraphRef();
//	}
//	public int endGraphId()
//	{
//		if (points.size() == 0) return 0;
//		GPMLPoint pt = points.get(points.size()-1);
//		return pt.getGraphRef();
//	}
//	public double getStartX()	{  return  (firstPoint() != null) ? firstPoint().getX() : 0;	}
//	public double getStartY()	{  return  (firstPoint() != null) ? firstPoint().getY() : 0;	}
//	public double getEndX()		{  return  (lastPoint() != null) ? lastPoint().getX() : 0;	}
//	public double getEndY()		{  return  (lastPoint() != null) ? lastPoint().getY() : 0;	}
//
//	public Point2D firstPoint()
//	{ 
//		if (points.size() == 0) return null;
//		return points.get(0).getPoint(); 
//		}
//	public GPMLPoint firstGPMLPoint()
//	{ 
//		if (points.size() == 0) return null;
//		return points.get(0); 
//		}
//	public Point2D forelastPoint()
//	{ 
//		if (points.size() < 2) return firstPoint();
//		return points.get(points.size()-2).getPoint(); 
//	}
//	public Point2D lastPoint()
//	{ 
//		if (points.size() == 0) return null;
//		return points.get(points.size()-1).getPoint(); 
//	}
//	public GPMLPoint lastGPMLPoint()
//	{ 
//		if (points.size() == 0) return null;
//		return points.get(points.size()-1); 
//	}
//	public void setLastPoint(Point2D pt)
//	{ 
//		if (points.size() == 0) return;
//		GPMLPoint gpt = points.get(points.size()-1); 
//		gpt.setPoint(pt);
//	}
//	double length()		// TODO assumes straight edges
//	{ 
//		int sz = points.size();
//		double len = 0;
//		if (sz < 2) return 0;
//		for (int i=0; i<sz-1; i++)
//			len += LineUtil.distance(points.get(i).getPoint(), points.get(i+i).getPoint());
//		return len;
//	}
//	public double getClosestPosition(double evX, double evY) {// TODO assumes straight edges
//		return LineUtil.toLineCoordinates(getStartPoint(), getEndPoint(), new Point2D(evX, evY));
//	}
//	//----------------------------------------------------------------------
//	boolean BADPOINT(Point2D pt)
//	{
//		return pt == null || Double.isNaN(pt.getX()) || Double.isNaN(pt.getY());
//	}
//
//	private void linearConnect() {
//		Point2D lastPt = lastPoint();
//		int shorten = SHORTEN ? 10 : 0;
//		
//		if (shorten > 0) {
//			Point2D prev = forelastPoint();
//			if (prev != null)
//			{
//				if (BADPOINT(prev))			return;
//				if (BADPOINT(lastPt)) 		return;
//				Line refline = new Line(prev.getX(), prev.getY(), lastPt.getX(), lastPt.getY());
//				if (interaction != null)
//				{
//					VNode endNode = interaction.getEndNode() == null ? null : interaction.getEndNode().getStack();
//					lastPt= LineUtil.getIntersection(refline, endNode, shorten);
//				}
//				setLastPoint(lastPt);
//			}
//		}     
//		setArrowPt(lastPt);
//
//		if (isZero(lastPt) || isZero(getStartPoint()))
//		{
//			System.out.println("zeroooo ");			//  TODO DEBUG
//		}
//		LineUtil.set(line, getStartPoint(), lastPt);
//		double width = 3;
//		if (interaction != null)
//		{
//			line.setStroke(interaction.getColor());
//			width = interaction.getStrokeWidth();
//		}
//		line.setStrokeWidth(width);
//		if (strokeDashArray != null)
//			line.getStrokeDashArray().setAll(strokeDashArray);
//		
//		Point2D pt = getPointAlongLine(0.5);
//		setLabelPosition(pt);
////		GPMLPoint midPt = points.get(1);
////		pt = midPt.getPoint();
//		setCenterPosition(0, pt);
//	}
//	
//	  private boolean isZero(Point2D pt) {
//		return pt.getX() == 0 && pt.getY() == 0;
//	}
//	//----------------------------------------------------------------------
//
//	private void curveConnect() {
//		if (curveGroup != null) 	getChildren().remove(curveGroup);
//		
//		if (line != null) line.setVisible(false);
//		
//		SegmentList segments = new SegmentList(canvas.getModel(), points);
//		curveGroup  = segments.makeCurveGroup(interaction, strokeDashArray);
//		setArrowPt(lastPoint());
//		getChildren().add(curveGroup);
//	}
//	
//	boolean SHORTEN = true;
//	  //----------------------------------------------------------------------
//	private void polylineConnect() {
//		int sz = points.size();
//		polyline.getPoints().clear();
//		GPMLPoint last = points.get(sz - 1);
//		for (int i = 0; i < sz - 1; i++) {
//			GPMLPoint pt = points.get(i);
//			GPMLPoint nextPt = points.get(i+1);
//			polyline.getPoints().addAll(pt.getX(), pt.getY());
//			Point2D midPoint = LineUtil.midPoint(pt.getPoint(), nextPt.getPoint(), 0.5);
//			centerPoints.get(i).setLayoutX(midPoint.getX());
//			centerPoints.get(i).setLayoutY(midPoint.getY());
//		}
//		// shorten the last segment if endNode is defined
//		Node endNode = (interaction == null || interaction.getEndNode() == null) ? null : interaction.getEndNode().getStack();
//		boolean shorten = SHORTEN && endNode != null;			// TODO -- and arrowhead??
//		if (shorten) {
//			GPMLPoint prev = points.get(sz - 2);
//			Line line = new Line(prev.getX(), prev.getY(), last.getX(), last.getY()+ last.getRelY());
//			Point2D shortStopPt = LineUtil.getIntersection(line, endNode);
//
//			polyline.getPoints().addAll(shortStopPt.getX()+ last.getRelX(), shortStopPt.getY()+ last.getRelY());
//			setArrowPt(shortStopPt);
//		} else
//		{
//			polyline.getPoints().addAll(last.getX(), last.getY());
//			setArrowPt(last.getX(), last.getY());
//		}
//		polyline.setStroke(interaction.getColor());
//		polyline.setStrokeWidth(interaction.getStrokeWidth());
//		if (strokeDashArray != null)
//			polyline.getStrokeDashArray().setAll(strokeDashArray);
//		
//	}
//
//	private boolean isOrthog(GPMLPoint a, GPMLPoint b)
//	{
//		double epsilon = 0.01;
//		if (Math.abs(a.getX() - b.getX()) < epsilon) return true;
//		if (Math.abs(a.getY() - b.getY()) < epsilon) return true;
//		return false;
//	}
////----------------------------------------------------------------------
//	private void elbowConnect() {
//		Polyline poly = getPolyline();
//		poly.getPoints().clear();
//		if (line != null) line.setVisible(false);
//		int sz  = points.size();
//		GPMLPoint startPt = points.get(0);
//		GPMLPoint prevPt = null;
//		boolean sidewaysFirst = Math.abs(startPt.getRelX()) > Math.abs(startPt.getRelY());
//		
//		for (int i = 0; i < sz ; i++) {
//			GPMLPoint currPt = points.get(i);
//			if (prevPt != null)
//			{
//				if (! isOrthog(prevPt, currPt))
//				{
//					double x = (sidewaysFirst ) ? currPt.getX() : prevPt.getX();
//					double y = (sidewaysFirst ) ? prevPt.getY() : currPt.getY();
//					poly.getPoints().addAll(x, y);
//				}
//				sidewaysFirst = !sidewaysFirst;
//			}
//			poly.getPoints().addAll(currPt.getX(), currPt.getY());
//			prevPt = currPt;
//		}
//		
//		Point2D last = lastPoint();
//		Node endNode = interaction.getEndNode() == null ? null : interaction.getEndNode().getStack();
//		boolean shorten = SHORTEN && endNode != null;			// TODO -- and arrowhead != null??
//		if (shorten) {
//			GPMLPoint prev = points.get(sz - 2);
//			Line line = new Line(prev.getX(), prev.getY(), last.getX(), last.getY());
//			Point2D shortStopPt = LineUtil.getIntersection(line, endNode);
//			Point2D correctedPt = new Point2D(last.getX(), shortStopPt.getY());			//  hack
//			setArrowPt(correctedPt);
//			last = correctedPt;
//		} 
//			
//		poly.getPoints().addAll(last.getX(), last.getY());
//		poly.setStroke(interaction.getColor());
//	}
//
//   //----------------------------------------------------------------------
//	public void setArrowhead(ArrowType inType)
//	{
//		GPMLPoint last = lastGPMLPoint();
//		if (last != null) 	
//			last.setArrowType(inType);
//	}
//		
//	public Shape makeArrowhead(ArrowType arrowhead)
//	{	
//		GPMLPoint last = lastGPMLPoint();
//		if (last == null) 		return null;
//
//		Color strokeColor = interaction == null ? Color.AQUAMARINE : interaction.getColor();
//		if (arrowhead == null)	return null;
//		if (arrowhead == ArrowType.none) return null;
//		Point2D prev = forelastPoint();			// the arrowhead rotation is based only on the last segment
//		if (type == EdgeType.elbow)
//			prev = new Point2D(last.getX(), prev.getY());
////		System.out.println("makeArrowhead: " + prev.toString() + " --> " + last.getPoint().toString());
//		Shape aHead = makeArrowHead(arrowhead.toString(), prev, last.getPoint(), strokeColor);
//		getChildren().add(aHead);
//		return aHead;
//	}
//
//   private Shape makeArrowHead(String shape, Point2D mid, Point2D last, Color color)
//   {
//		Shape arrowhead;
//		double[] arrowShape = ArrowType.getArrowShape(shape);
//		if (ArrowType.isShape(shape))
//		{
//			arrowhead = new Circle(6);
//			arrowhead.setFill(Color.WHITE);
//			arrowhead.setStroke(Color.BLACK);
//		}
//		else 
//		{
//			Line line = new Line(mid.getX(), mid.getY(), last.getX(), last.getY());
//			line.setStrokeWidth(6);
//			arrowhead = new Arrow(line, 1.0f, color, arrowShape);
////			arrowhead.setFill(color);
//		}
//		arrowhead.setTranslateX(getArrowX());
//		arrowhead.setTranslateY(getArrowY());
//		return arrowhead;
//   }
//	//----------------------------------------------------------------------
//	public ArrowType getArrowType()
//	{
//		if (points == null || points.size() < 1) return ArrowType.arrow;
//		GPMLPoint last = points.get(points.size()-1);
//		return last.getArrowType();
//	}
//	//-----------------------------------------------------------------------------------
//	// does any segment intersect the rectangle
//	public boolean sectRect(Rectangle r) {
//		for (int i = 0; i < points.size() - 1; i++) {
//			Point2D a = points.get(i).getPoint();
//			Point2D b = points.get(i + 1).getPoint();
//			double left = r.getX();
//			double top = r.getY();
//			Point2D r1 = new Point2D(left, top);
//			Point2D r2 = new Point2D(left + r.getWidth(), top);
//			Point2D r3 = new Point2D(left + r.getWidth(), top + r.getHeight());
//			Point2D r4 = new Point2D(left, top + r.getHeight());
//
//			if (doIntersect(a, b, r1, r2))				return true;
//			if (doIntersect(a, b, r2, r3))				return true;
//			if (doIntersect(a, b, r3, r4))				return true;
//			if (doIntersect(a, b, r4, r1))				return true;
//		}
//		return false;
//	}
//	  
//	// The main function that returns true if line segment 'p1q1' and 'p2q2' intersect. 
//	boolean doIntersect(Point2D p1, Point2D q1, Point2D p2, Point2D q2) 
//	{ 
//	    // Find the four orientations needed for general and special cases 
//	    int o1 = orientation(p1, q1, p2); 
//	    int o2 = orientation(p1, q1, q2); 
//	    int o3 = orientation(p2, q2, p1); 
//	    int o4 = orientation(p2, q2, q1); 
//	  
//	    // General case 
//	    if (o1 != o2 && o3 != o4)  		     return true; 
//	  
//	    // Special Cases 
//	    if (o1 == 0 && onSegment(p1, p2, q1)) return true;  // p1, q1 and p2 are colinear and p2 lies on segment p1q1 
//	    if (o2 == 0 && onSegment(p1, q2, q1)) return true;  // p1, q1 and q2 are colinear and q2 lies on segment p1q1 
//	    if (o3 == 0 && onSegment(p2, p1, q2)) return true;  // p2, q2 and p1 are colinear and p1 lies on segment p2q2 
//	    if (o4 == 0 && onSegment(p2, q1, q2)) return true;  // p2, q2 and q1 are colinear and q1 lies on segment p2q2 
//	    return false; 	// Doesn't fall in any of the above cases 
//	} 	
//	
//	// To find orientation of ordered triplet (p, q, r). 
//	// The function returns following values 
//	// 0 --> p, q and r are colinear 
//	// 1 --> Clockwise 
//	// 2 --> Counterclockwise 
//	int orientation(Point2D p, Point2D q, Point2D r) 
//	{ 
//	    double val = (q.getY() - p.getY()) * (r.getX() - q.getX()) - 
//	              (q.getX() - p.getX()) * (r.getY() - q.getY()); 
//	  
//	    if (Math.abs(val) < 0.01) return 0;  // colinear 
//	    return (val > 0) ? 1: 2; // clock or counterclock wise 
//	} 
//	// check if point q lies on line segment 'p- r' 
//	boolean onSegment(Point2D p, Point2D q, Point2D r) 
//	{ 
//	    if (q.getX() <= Math.max(p.getX(), r.getX()) && q.getX() >= Math.min(p.getX(), r.getX()) && 
//	        q.getY() <= Math.max(p.getY(), r.getY()) && q.getY() >= Math.min(p.getY(), r.getY())) 
//	       return true; 
//	    return false; 
//	} 
//	//----------------------------------------------------------------------
//	public String toString()
//	{
//		int startID;
//		if (interaction == null || interaction.getStartNode() == null) startID =  0;
//		else 
//		{
//			VNode start = interaction.getStartNode().getStack();
//			startID = start == null ? startGraphId() : start.getGraphId();
//		}
//		int endID;
//		if (interaction == null || interaction.getEndNode() == null) endID = 0;
//		else 
//		{
//			VNode end = interaction.getEndNode().getStack();
//			endID = end == null ? endGraphId() : end.getGraphId();
//		}
//		String s=  String.format(" %s \t(%4.1f, %4.1f) --> %s (%4.1f, %4.1f) %d pts", 
//				startID, getStartX(), getStartY(), 
//				endID, getEndX(), getEndY(), getPoints().size());
//				
//		return s;
//	}
//	  
//}
