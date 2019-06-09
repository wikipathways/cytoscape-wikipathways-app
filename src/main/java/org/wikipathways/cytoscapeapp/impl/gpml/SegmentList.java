package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.pathvisio.core.view.Group;
import org.wikipathways.cytoscapeapp.impl.gpml.AttributeMap.Circle2D;



public class SegmentList extends ArrayList<Segment> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//----------------------------------------------------------------------
	public SegmentList(Model model, List<GPMLPoint> points)
	{
	  	int len = points.size();
	  	GPMLPoint mStart = points.get(0);
	  	GPMLPoint mEnd = points.get(len-1);
//	  	if (verbose)	  	System.out.println("length: " + len + " points");
	  	
		double startRelX = mStart.getRelX();
		double startRelY = mStart.getRelY();
		double endRelX = mEnd.getRelX();
		double endRelY = mEnd.getRelY();
	  	Point2D.Double startPt =  new Point2D.Double(mStart.getX(), mStart.getY());
	  	Point2D.Double endPt =  new Point2D.Double(mEnd.getX(), mEnd.getY());
  	
	  	if (model != null)
	  	{
	  		DataNode startNode = model.find(mStart.getGraphRef());
	  	  	DataNode endNode = model.find(mEnd.getGraphRef());
		  	if (startNode != null)
		  		startPt = startNode.getAdjustedPoint(mStart);
		  	if (endNode != null)
		  		endPt = endNode.getAdjustedPoint(mEnd);
	  	}
		  	

	  	int startSide = Segment.getSide(startRelX, startRelY);
		if (startSide < 0)
			startSide = getNearestSide(startPt, endPt);
		int endSide = Segment.getSide(endRelX, endRelY);
		if (endSide < 0)
			endSide = getNearestSide(endPt, startPt);
		
	  	Point2D.Double[] wps = calculateWayPoints(startPt, endPt, startSide, endSide);
	    calculateSegments(startPt, endPt, startSide, endSide, wps);
	    
//	    dump(wps);
	}
	
	
	private void dump(Point2D[] wps) {
		for (Point2D pt : wps)
			System.out.println(pt.toString());
		for (Segment seg : this)
			System.out.println(seg.toString());
		
	}


	//----------------------------------------------------------------------
	private int getNearestSide(Point2D startPt, Point2D endPt) {
		double dx = endPt.getX() - startPt.getX();
		double dy= endPt.getY() - startPt.getY();
		if (Math.abs(dy) > Math.abs(dx) ) 
			return (dy > 0) ? ConnectorRestrictions.SIDE_SOUTH : ConnectorRestrictions.SIDE_NORTH;
		return (dx > 0) ? ConnectorRestrictions.SIDE_EAST : ConnectorRestrictions.SIDE_WEST;
	}

		private final static double SEGMENT_OFFSET = 40;


	protected Point2D.Double[] calculateWayPoints(Point2D.Double start, Point2D.Double end, int startSide, int endSide) {
		int nrSegments = getNrSegments( start, end, startSide, endSide);
		int	nWaypoints = nrSegments - 2;

		//Else, calculate the default waypoints
		Point2D.Double[] waypoints = new Point2D.Double[nWaypoints];

		int startAxis = Segment.getSegmentAxis(startSide);
		int startDirection = Segment.getSegmentDirection(startSide);
		int endAxis = Segment.getSegmentAxis(endSide);
		int endDirection = Segment.getSegmentDirection(endSide);
		
		if(nWaypoints == 1) {
			/*
			 * [S]---
			 * 		|
			 * 		---[S]
			 */
			waypoints[0] = calculateWayPoint(start, end, startAxis, startDirection);
		} else if(nWaypoints == 2) {
			/*
			* [S]---
			* 		| [S]
			* 		|  |
			* 		|---
			*/
			double offset = SEGMENT_OFFSET * endDirection;
			Point2D.Double pt = new Point2D.Double( end.getX() + offset, end.getY() + offset );
			waypoints[0] = calculateWayPoint(start, pt, startAxis, startDirection);

			waypoints[1] = calculateWayPoint(end, waypoints[0], endAxis, endDirection);
		} else if(nWaypoints == 3) {
			/*  -----
			 *  |   |
			 * [S]  | [S]
			 *      |  |
			 *      |---
			 */
			//Start with middle waypoint
			waypoints[1] = Segment.centerPoint(start, end); 
			waypoints[0] = calculateWayPoint(start, waypoints[1], startAxis, startDirection);
			waypoints[2] = calculateWayPoint(end, waypoints[1], endAxis, endDirection);
		}
		 else if(nWaypoints == 4) {			// NEW code UNTESTED
		/*   ---------
		 *   |       |
		 *   ---[S]  | [S]
		 *           |  |
		 *           |---
		 */
				waypoints[2] = Segment.centerPoint(start, end); 
				waypoints[1] = calculateWayPoint(start, waypoints[2], startAxis, startDirection);
				waypoints[0] = calculateWayPoint(start, waypoints[1], startAxis, startDirection);
				waypoints[3] = calculateWayPoint(end, waypoints[1], endAxis, endDirection);
			}
			// this case should be 3
		/*           
		 *   |---[S]    [S]
		 *   |           |
		 *    -----------
		 */		
	return waypoints;
	}


	String[] sides = {  "North", "East", "South", "West" };
	protected Point2D.Double calculateWayPoint(Point2D start, Point2D end, int axis, int direction) {
		double x,y = 0;
		if(axis == Segment.AXIS_Y) {	
//			x = start.getX() + (end.getX() - start.getX()) / 2;			// AST hacked for WP4, is pbly conditional on if there are more waypoints
			x = end.getX();
			y = start.getY() + SEGMENT_OFFSET * direction;
		} else {
			x = start.getX() + SEGMENT_OFFSET * direction;
//			y = start.getY() + (end.getY() - start.getY()) / 2;
			y = end.getY();
		}
		return new Point2D.Double(x, y);
	}

	
	
	protected void calculateSegments(Point2D.Double start, Point2D.Double end, int startSide, int endSide, Point2D.Double[] waypts) {
		int nrSegments = getNrSegments(start, end, startSide, endSide);
		Segment[] segments = new Segment[nrSegments];
		Segment prevSegment;
		
		int startAxis = Segment.getSegmentAxis(startSide);
		if(nrSegments == 2) { //No waypoints
			segments[0] = Segment.createStraightSegment(start, end, startAxis);
			segments[1] = Segment.createStraightSegment(segments[0].getEnd(), end, Segment.getOppositeAxis(startAxis));
		} else 
		{
			prevSegment = segments[0] = Segment.createStraightSegment(start, waypts[0], startAxis );
			int otheraxis = Segment.getOppositeAxis(startAxis);
//			prevSegment = segments[1] = Segment.createStraightSegment(prevSegment.getEnd(), waypts[0], otheraxis );
//			axis = Segment.getOppositeAxis(axis);
			
//			prevSegment = segments[2] = Segment.createStraightSegment(prevSegment.getEnd(), end, Segment.getSegmentAxis(endSide) );

// AST hacked for WP4  -- won't work with more than 1 way point  TODO

			for(int i = 1; i < waypts.length; i++) {
				prevSegment = segments[2*i-1] = Segment.createStraightSegment( prevSegment.getEnd(), waypts[i], startAxis );
				prevSegment = segments[2*i] = Segment.createStraightSegment( prevSegment.getEnd(), waypts[i], otheraxis );
			}
			Point2D.Double lastWayPt =  waypts[waypts.length-1];
			prevSegment = segments[segments.length - 2] = Segment.createStraightSegment( prevSegment.getEnd(), lastWayPt, otheraxis);
			segments[segments.length - 1] = Segment.createStraightSegment( prevSegment.getEnd(), end, Segment.getSegmentAxis(endSide));
		}
		
		for (Segment s : segments) 
			if (s != null && s.length() > 0.01)
				add(s);
			else System.out.println("Bad Segment: " + s);
	}

	
	
	protected int getNrSegments(Point2D.Double start, Point2D.Double end, int startSide, int endSide) {

		boolean leftToRight = getDirectionX(start, end) > 0;

		Point2D.Double left = leftToRight ? start : end;
		Point2D.Double right = leftToRight ? end : start;
		boolean leftBottom = getDirectionY(left, right) < 0;

		int z = leftBottom ? 0 : 1;
		int x = leftToRight ? startSide : endSide;
		int y = leftToRight ? endSide : startSide;
		return getNrWaypoints(x, y, z) + 2;
	}
	/**
	 * Get the direction of the line on the x axis
	 * @param start The start point of the line
	 * @param end The end point of the line
	 * @return 1 if the direction is positive (from left to right),
	 * -1 if the direction is negative (from right to left)
	 */
	static int getDirectionX(Point2D.Double start, Point2D.Double end) {
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
		
	double getLength()
	{
		double totLength = 0;
		for (Segment seg : this)
			totLength += seg.length();
		return totLength;
	}

    protected Point2D.Double fromLineCoordinate(double l) 
    {
		double totalLength = getLength();
		double pixelsRemaining = totalLength * l;
		if (pixelsRemaining < 0) pixelsRemaining = 0;
		if (pixelsRemaining > totalLength) pixelsRemaining = totalLength;

		// count off each segment from pixelsRemaining, until there aren't enough pixels left
		Segment segment = null;
		double slength = 0.0;
		for(Segment s : this) 
		{
			slength = s.length();
			segment = s;
			if (pixelsRemaining < slength) 		break; // not enough pixels left, we found our segment.
			pixelsRemaining -= slength;
		}
		if (segment == null) 
		{
			return new Point2D.Double(0,0);
		}
		//Find the location on the segment
		Point2D.Double s = segment.getStart();
		Point2D.Double e = segment.getEnd();

		// protection against division by 0
		if (slength == 0)
			return new Point2D.Double(s.getX(), s.getY());
			// start from s, in the direction of e, for pixelRemaining pixels.
		double deltax = e.getX() - s.getX();
		double deltay = e.getY() - s.getY();

		return new Point2D.Double(s.getX() + deltax / slength * pixelsRemaining,
				s.getY() + deltay / slength * pixelsRemaining );
	}

//	 public Group makeCurveGroup(Interaction interaction, Double[] strokeDashArray)
//	{
//	    Group group = new Group();
//		Point2D.Double line2End = new Point2D.Double(0,0);
//		for (int i=0; i < size()-1; i++)
//		{
////			Point2D a = points.get(i).getPoint();
////			Point2D b = points.get(i+1).getPoint();
////			Line line1 = new Line(a.getX(), a.getY(), b.getX(), b.getY());
////			Line line2 = new Line(a.getX(), a.getY(), b.getX(), b.getY());
//	//		Point2D lastPt = lastPoint();
//	
//			Segment seg = get(i);
//			Point2D line1Start = seg.getStart();
//			Point2D line1End = seg.getEnd();
//			
//			Segment nextseg = get(i+1);
//	        Point2D line2Start = nextseg.getStart();
//	        line2End = nextseg.getEnd();
//
////	        getChildren().addAll(seg.getLine(), nextseg.getLine());
////	        double line1Length = line1End.subtract(line1Start).magnitude();
////	        double line2Length = line2End.subtract(line2Start).magnitude();
////	
////	        // average length:
////	        double averLength = (line1Length + line2Length) / 2 ;
//	
//	        // extend line1 in direction of line1 for aveLength:
////	        Point2D control1 = line1End.add(line1End.subtract(line1Start).normalize().multiply(2));
////	        
////	        // extend line2 in (reverse) direction of line2 for aveLength:
////	        Point2D control2 = line2Start.add(line2Start.subtract(line2End).normalize().multiply(averLength));
////		
////	        control1 = new Point2D(line1End.getX() + 10, line1End.getY() + 40);
////	        control2 = new Point2D(line2End.getX() - 10, line2End.getY() - 40);
////	        CubicCurve curve = new CubicCurve(
////	                line1End.getX(), line1End.getY(), 
////	                control1.getX(), control1.getY(), 
////	                control2.getX(), control2.getY(), 
////	                line2Start.getX(), line2Start.getY());
////	 2      CubicCurve curve = new CubicCurve(
////	                line1Start.getX(), line1Start.getY(), 
////	                line1End.getX(), line1End.getY(), 
////	                line2Start.getX(), line2Start.getY(), 
////	                line2End.getX(), line2End.getY());
//	
//	        Point2D.Double mid = new Point2D.Double((line1End.getX() + line2Start.getX()) /2, (line1End.getY() + line2Start.getY()) / 2);
//	        CubicCurve curve = new CubicCurve(
//	                line1Start.getX(), line1Start.getY(), 
//	                mid.getX(), mid.getY(), 
//	                line2Start.getX(), line2Start.getY(), 
//	                line2End.getX(), line2End.getY());
//	
////	        boolean showControlPoints = false;
////	        if (showControlPoints)
////	        {
////    			Circle2D controlPt1 = new Circle2D();
////    			controlPt1.setFill(Color.GRAY);
////    			controlPt1.setStroke(Color.GREEN);
////    			controlPt1.setCenterX(line2End.getX());
////    			controlPt1.setCenterY(line2End.getY());
////    			
////    			Circle controlPt2 = new Circle(13);
////    			controlPt2.setFill(Color.ROSYBROWN);
////    			controlPt2.setStroke(Color.DARKBLUE);
////    			controlPt2.setCenterX(line1Start.getX());
////    			controlPt2.setCenterY(line1Start.getY());
////    			group.getChildren().addAll(controlPt1,controlPt2);		//
////	        }
//	        curve.setFill(null);
//			if (interaction != null)
//			{
//				curve.setStroke(interaction.getColor());
//				double width = interaction.getStrokeWidth();
//				curve.setStrokeWidth(width);
//			}
//			if (strokeDashArray != null)
//				curve.getStrokeDashArray().setAll(strokeDashArray);
//			group.getChildren().add(curve);
//		}
//		return group;
//	}
}
