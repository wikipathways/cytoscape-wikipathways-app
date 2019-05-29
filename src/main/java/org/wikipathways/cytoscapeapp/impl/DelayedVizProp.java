package org.wikipathways.cytoscapeapp.impl;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.Bend;
import org.pathvisio.core.model.PathwayElement;
import org.wikipathways.cytoscapeapp.CellShapes;


/**
 * Temporarily stores visual property values of nodes and edges until
 * their views have been created.
 *
 * In Cytoscape, first the network topology is created (via CyNetwork.add{Node|Edge}),
 * then the view objects are created.
 * Once that's done, the network's visual style can be created (via View.setVisualProperty)
 * once all the view objects exist (ensured by CyEventHelper.flushPayloadEvents).
 *
 * However, while we're reading GPML, we need to create the network's visual style
 * while we are creating the network toplogy. Otherwise we'd have to read the GPML twice,
 * once for topology and again for the visual style.
 *
 * How do we get around this problem? While we're reading GPML, we create the network topology
 * and store our desired visual style in DelayedVizProp instances.
 * After we finish reading GPML, we ensure that view objects have been created for
 * all our new nodes and edges (via CyEventHelper.flushPayloadEvents). 
 * Finally we apply the visual style stored in the DelayedVizProp objects.
 */
public class DelayedVizProp {
  final CyIdentifiable netObj;
  final VisualProperty<?> prop;
  final Object value;
  final boolean isLocked;

  public DelayedVizProp(final CyIdentifiable netObj, final VisualProperty<?> prop, final Object value, final boolean isLocked) {
    this.netObj = netObj;
    this.prop = prop;
    this.value = value;
    this.isLocked = isLocked;
  }
	static boolean verbose = false;
	public String toString() {  return netObj.getSUID() + " @ " + prop.getDisplayName() + ": " + value.toString(); }
	public static void applyAll(final CyNetworkView netView,final Iterable<DelayedVizProp> delayedProps, WPManager mgr) 
	{
		try 
		{
			mgr.turnOffEvents();
			for ( DelayedVizProp delayedProp : delayedProps) 
			{
				final Object value = delayedProp.value;
				if (value == null) continue;

				String propName = delayedProp.prop.getDisplayName();
//				System.out.println(propName);
				if ("Node Shape".equals(propName))
					applyNodeShape(netView, delayedProps, mgr, delayedProp);
		
				if ("Label".equals(propName))
					applyLabel(netView, delayedProps, mgr, delayedProp);
		
				if ("Edge Bend".equals(propName))
				{
//					applyEdgeBend(netView, mgr, delayedProp);
					CyEdge edge = (CyEdge) delayedProp.netObj;
					View<CyEdge> edgeView = netView.getEdgeView(edge);
					if (edgeView != null && ( delayedProp.value instanceof Bend))
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, (Bend) delayedProp.value);
				}
				
			      View<?> view = null;
			      if (delayedProp.netObj instanceof CyNode) 
			      {
			        final CyNode node = (CyNode) delayedProp.netObj;
			        view = netView.getNodeView(node);
			      } else if (delayedProp.netObj instanceof CyEdge) 
			      {
			        final CyEdge edge = (CyEdge) delayedProp.netObj;
			        view = netView.getEdgeView(edge);
			      }
			     if (view == null) continue;
			
			     String prop = delayedProp.prop.getIdString();
			      boolean isPosition = prop.equals("NODE_X_LOCATION") || prop.equals("NODE_Y_LOCATION");
			      boolean isDimension = prop.equals("WIDTH") || prop.equals("HEIGHT");
			      if (isDimension)
			      {
			    	  Double d = Double.parseDouble(value.toString());
			    	  if (d <= 0) continue;   // WP2848
			      }
			   try
			   {
				   if (isPosition)
					   System.out.println(delayedProp.netObj.toString() + ": " + propName + " = " + value);
					if (delayedProp.isLocked && !isPosition)
						view.setLockedValue(delayedProp.prop, value);
					else
						view.setVisualProperty(delayedProp.prop, value);
			  	}
			    catch (IllegalArgumentException ex)
				{ 
					System.err.println(delayedProp.prop + " - " + value);
					continue;
				}
			}
			wpManagerInstance = mgr;
//			postProcessShapes();
		}
		catch (Exception e) {		} 
		finally 
		{
			mgr.turnOnEvents();
//			netView.updateView();
		}

  }
	static WPManager wpManagerInstance = null;

//	static private void postProcessShapes()
//	{
//		System.out.println("Postprocessing");
//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//			while (shapes.size() > 0)
//			{
//				ShapeAnnotation shape = shapes.remove(shapes.size() - 1);
//				shape.setCanvas(Annotation.BACKGROUND);
//				// BUG -- this causes a ~ 1" vertical offset of all shapes
//				shape.removeAnnotation();			// remove and readd the annotation to register the canvas change 
//				wpManagerInstance.getAnnots().addShape(shape);
//			}
//		}}
//				);
//	}
//	static final List<ShapeAnnotation> shapes = new ArrayList<ShapeAnnotation>();
	//--------------------------------------------------------------------------------
	static final List<Long> states = new ArrayList<Long>();
	public static void saveState(Long suid) 	{		states.add(suid);	}
	public static boolean isState(Long suid) {		return states.contains(suid);	}
	public static void clearStateList() 		{ 		states.clear();}
	//--------------------------------------------------------------------------------
	static final Map<CyNode, Double>rotations = new HashMap<CyNode, Double>();
	public static void putRotation(CyNode node, double r) {	rotations.put(node, new Double(r));	}
	public static double getRotation(CyNode node) {
		Double d = rotations.get(node);
		return (d == null) ? Double.NaN : d.doubleValue();
	}

	//--------------------------------------------------------------------------------
	static final Map<CyNode, PathwayElement>pvShapes = new HashMap<CyNode, PathwayElement>();
	public static void putPathwayElement(CyNode node, PathwayElement pv) 	{	pvShapes.put(node, pv);	}
	public static PathwayElement getPathwayElement(CyNode node) 			{	return pvShapes.get(node);	}

	//--------------------------------------------------------------------------------
	static final Map<CyEdge, PathwayElement>pvEdges = new HashMap<CyEdge, PathwayElement>();
	public static void putPathwayElement(CyEdge e, PathwayElement pv) 		{		pvEdges.put(e, pv);	}
	public static PathwayElement getPathwayElement(CyEdge e)				{		return pvEdges.get(e);	}

	//--------------------------------------------------------------------------------

	private static void applyLabel(final CyNetworkView netView,final Iterable<DelayedVizProp> delayedProps, WPManager mgr, DelayedVizProp delayedProp) 		{
		final Map<String,String> map = new HashMap<String,String>();
		TextAnnotation mAnnotation = mgr.getAnnots().newText(netView, map);
		CyNode src = (CyNode) delayedProp.netObj;
		View<CyNode> view = netView.getNodeView(src);
		double x = 0; 
		double y = 0;
		double wid = 100;
		double hght = 30;
		String text = "UNDEFINED";
		List<DelayedVizProp> relatedProps = getPropsByID(delayedProps, src.getSUID());
		for (DelayedVizProp prop : relatedProps)			// we have to rescan all properties to find other attributes for the same shape
		{
			String propName1 = prop.prop.getDisplayName();
			String lookup = propTranslator(propName1);
			if (lookup != null && prop.value != null)
			{
				String propvalue1 = prop.value.toString();
//	 			  System.out.println(lookup + ": " + propvalue1);
//				int idx = propvalue1.indexOf('.');
//				if (idx > 0)
//					propvalue1 = propvalue1.substring(0, idx);
				map.put(lookup, propvalue1);
				if ("Width".equals(lookup))			wid = Double.valueOf(propvalue1);
				if ("Height".equals(lookup))		hght = Double.valueOf(propvalue1);
//				if ("Style".equals(lookup))			{ System.out.println("set style to: " + style);  style = propvalue1;   }
				if ("x".equals(lookup))				x = Double.valueOf(propvalue1);
				if ("y".equals(lookup))				y = Double.valueOf(propvalue1);
//				if ("z".equals(lookup))				z = Double.valueOf(propvalue1);
			}
		}
		if (mAnnotation != null) 
		{
//			if (verbose) System.out.println(String.format("moving annotation to : %4.1f , %4.1f", x, y));
			mAnnotation.moveAnnotation(new Point2D.Double(x, y));
			mAnnotation.setText(text);
			mAnnotation.setCanvas(Annotation.BACKGROUND);
//			mAnnotation.setName(propvalue);
			mAnnotation.setZoom(1.0);
			mgr.getAnnotationManager().addAnnotation(mAnnotation);
//			shapes.add(mAnnotation);
		}
		
	}
	
	private static void applyNodeShape(final CyNetworkView netView,final Iterable<DelayedVizProp> delayedProps, WPManager mgr, DelayedVizProp delayedProp) 		{
		
		final Map<String,String> map = new HashMap<String,String>();
		CyNode src = (CyNode) delayedProp.netObj;
		CyRow row = netView.getModel().getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).getRow(src.getSUID());
	    Object val = row.getAllValues().get("State");
//	    if (val != null)	    				return;
	    if (isState(src.getSUID()))    	return;

		
		CyNetwork network = netView.getModel();
		List<CyNode> neighbors = network.getNeighborList(src, CyEdge.Type.ANY);
		List<DelayedVizProp> relatedProps = getPropsByID(delayedProps, src.getSUID());
		double wid = 0;
		double hght = 0;
		double x = Double.NaN;
		double y = Double.NaN;

		View<CyNode> view = netView.getNodeView(src);
		for (DelayedVizProp prop : relatedProps)			// we have to rescan all properties to find other attributes for the same shape
		{
			String propName1 = prop.prop.getDisplayName();
			String lookup = propTranslator(propName1);
			if (lookup != null && prop.value != null)
			{
				String propvalue1 = prop.value.toString();
//	 			  System.out.println(lookup + ": " + propvalue1);
//				int idx = propvalue1.indexOf('.');
//				if (idx > 0)
//					propvalue1 = propvalue1.substring(0, idx);
				map.put(lookup, propvalue1);
				if ("Width".equals(lookup))			wid = Double.valueOf(propvalue1);
				if ("Height".equals(lookup))		hght = Double.valueOf(propvalue1);
//				if ("Style".equals(lookup))			{ System.out.println("set style to: " + style);  style = propvalue1;   }
				if ("x".equals(lookup))				x = Double.valueOf(propvalue1);
				if ("y".equals(lookup))				y = Double.valueOf(propvalue1);
//				if ("z".equals(lookup))				z = Double.valueOf(propvalue1);
			}
		}
		System.out.println("applyNodeShape " + src + ": " + x + ", " + y );	
		String propvalue = delayedProp.value.toString();
		if (verbose) System.out.println("propvalue: "+propvalue);
//		System.out.println(String.format("Size of %s: %.2f x %.2f", propvalue, wid ,hght));


		if ("Rectangle".equals(propvalue) || "Octagon".equals(propvalue))			// HACK - should look for group node
		{
			//Nothing to do here; Group style is set in GpmlToPathway.java
			
			return;
		}
		
		ShapeAnnotation mAnnotation = mgr.getAnnots().newShape(netView, map);
		
		if ("Arc".equals(propvalue))
		{
			if (verbose) System.out.println("Arc");
			double startRotation = getRotation(src);
			Arc2D.Float arc = CellShapes.makeArc(startRotation);
		
			mAnnotation.setCustomShape(arc);
			mAnnotation.setFillOpacity(0);
			double d = startRotation / (Math.PI / 2.0);
			double epsilon = 0.001;
			if ((d - Math.round(d) < epsilon) && ((int) Math.round(d) % 2 == 1))
			{
				double t = wid;
				wid = hght;
				hght = t;
			}
		}
		else
		{
			GeneralPath path = CellShapes.getPath(propvalue);
			if (path != null)
			{
				if (verbose) System.out.println("GeneralPath");
				double cx = path.getBounds2D().getCenterX();
				double cy = path.getBounds2D().getCenterY();
				double startRotation = getRotation(src);
//				if (Double.isNaN(startRotation) || Math.abs(startRotation) < 0.1)
//				{
//					
//				}
//				else
				{
					AffineTransform rotater = new AffineTransform();
					rotater.rotate(startRotation, cx, cy);
					path.transform(rotater);
					mAnnotation.setCustomShape(path);
					if ((Math.abs(startRotation - Math.PI / 2.0) < 0.1) || (Math.abs(startRotation + Math.PI / 2.0) < 0.1))  // #58 hack- just look for +/- 90 degree rotation, and switch height and width.
					{
						double t = wid;
						wid = hght;
						hght = t;
					}
				}
			}
			else				
			{
				Shape theShape = CellShapes.getShape(propvalue);
				if (theShape == null)		mAnnotation.setShapeType(propvalue);  
				else
				{
					if (verbose) System.out.println("\nShape");
					double startRotation = getRotation(src);
					AffineTransform rotater = new AffineTransform();
					double cx = theShape.getBounds2D().getCenterX();
					double cy = theShape.getBounds2D().getCenterY();
					rotater.rotate(startRotation, cx, cy);
					mAnnotation.setCustomShape(theShape);
//					Rectangle2D newBounds = theShape.getBounds2D();
//					wid = newBounds.getWidth();
//					hght = newBounds.getHeight();
			}
			}
		}
		
		double nodex = 0, nodey = 0;
		PathwayElement elem = getPathwayElement(src);
		if (elem != null)
		{
			Color stroke = elem.getColor();
			mAnnotation.setBorderColor(stroke);
			mAnnotation.setBorderWidth(elem.getLineThickness());
			Color fill = elem.getFillColor(); 
			if ("Brace".equals(propvalue))
				fill = Color.white;
			mAnnotation.setFillColor(fill);
			double relx = 0;
			double rely = -1;

			List<CyEdge> edges = network.getAdjacentEdgeList(src, CyEdge.Type.ANY);
			if (edges != null)
				for (CyEdge edge : edges)
				{
					PathwayElement edgeElement = getPathwayElement(edge);
					if (edgeElement != null)
					{
						relx = edgeElement.getRelX();
						rely = edgeElement.getRelY();
					}
				}
//			Rectangle2D bounds = elem.getMBounds();
//			double cx = bounds.getCenterX();
//			double cy = bounds.getCenterY();
//			double w = bounds.getWidth();
//			double h = bounds.getHeight();
			
			nodex = x + relx * wid/2;
			nodey = y + rely * hght/2;
		}
//		Color fill = (Color) view.getLockedProperty(BasicVisualLexicon.NODE_FILL_COLOR);
//		Color fill2 = (Color) view.getVisualProperty(BasicVisualLexicon.NODE_COLOR);
//		System.out.println("Filling annotation with " + fill.toString());
//		mAnnotation.setBorderColor(Color.green);
		
		boolean legalSize = (wid > 0 && hght > 0);
		if (legalSize)
			mAnnotation.setSize(wid, hght);
		boolean legalXY = (!(Double.isNaN(x) || Double.isNaN(y)));
		
		if (legalXY && legalSize) 
		{
			x -= (wid / 2.);
			y -= (hght / 2.);
			if (mAnnotation != null) 
			{
//				if (verbose) System.out.println(String.format("moving annotation to : %4.1f , %4.1f", x, y));
				mAnnotation.moveAnnotation(new Point2D.Double(x, y));
				mAnnotation.setCanvas(Annotation.BACKGROUND);
				mAnnotation.setName(propvalue);
				mAnnotation.setZoom(1.0);
				mgr.getAnnotationManager().addAnnotation(mAnnotation);
//				shapes.add(mAnnotation);
			}
			// view.setLockedValue(prop, 0.);
		}
//		if (verbose) 
//		{
//			System.out.println("size: "+ (int) x + ",  "+ (int)  y +  " # " + (int) wid + ",  "+ (int)  hght);
//			System.out.println("mAnnotation: "+ mAnnotation);	
//			System.out.println("---> legal size: "+ legalSize + " legal pos: "+ legalXY);
//		}
		
		boolean noNeighbors = neighbors.size() == 0;
		boolean deleteNode = noNeighbors;  //thePath != null;		
		if (deleteNode)
		{
			view.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
			netView.getModel().removeNodes(Collections.singletonList((src)));	
//			mAnnotation.setCanvas("background");		//	BUG:  this will cause annotation to move to 0,0!
		}
		else 
		{
			if (elem != null)
			{
				view.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, nodex);
				view.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, nodey);
			}
			view.setLockedValue(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY, 0);		// opacity
			view.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 2.0);
			view.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, 2.0);
		}
	}

	// --------------------------------------------------------------------------------
//	private static void applyEdgeBend(final CyNetworkView netView, WPManager mgr, final DelayedVizProp delayedProp) {
//
//		if (delayedProp.netObj == null) {
//			System.out.println("delayedProp.netObj == null");
//			return;
//		}
//
//		CyEdge edge = (CyEdge) delayedProp.netObj;
//		View<CyEdge> edgeView = netView.getEdgeView(edge);
//		if (edgeView == null) return;
//		if (!( delayedProp.value instanceof Bend))  return;
//		Bend bend = (Bend) delayedProp.value;
//		edgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, bend);
//		if (bend == EdgeBendVisualProperty.DEFAULT_EDGE_BEND) {
//			System.out.println("DEFAULT_EDGE_BEND");
//			return;
//		}
//		System.out.println("dont applyEdgeBend " + delayedProp.value);
//
//		CyNode src = edge.getSource();
//		CyNode targ = edge.getTarget();
//		System.out.println("src " + src);
//		System.out.println("targ " + targ);
//		// if (src == null || src != null) return;
//		View<CyNode> srcView = netView.getNodeView(src);
//		View<CyNode> targView = netView.getNodeView(targ);
//		Point2D.Double srcCenter = getNodePosition(srcView);
//		Point2D.Double targCenter = getNodePosition(targView);
//
//		Point2D.Double elbow = new Point2D.Double(srcCenter.getX(), targCenter.getY()); // TODO -- two choices here!
//
//		showPoint("src", srcCenter);
//		showPoint("target", targCenter);
//		showPoint("elbow", elbow);
//
//		if (verbose)
//			System.out.println("bend: " + bend.getAllHandles().size() + " handles "
//					+ (delayedProp.isLocked ? "LOCKED" : "UNLOCKED"));
//
//		HandleFactory handleFactory = mgr.getHandleFactory();
//		List<Handle> handles = bend.getAllHandles();
//		if (handles.size() == 0)
//			handles.add(handleFactory.createHandle(netView, edgeView, elbow.getX(), elbow.getY()));
//		else try {
//
//				for (Handle h : handles) {
//					h.defineHandle(netView, edgeView, elbow.getX(), elbow.getY());
//					System.out.println(" setting to (" + (int) elbow.getX() + ", " + (int) elbow.getY() + ")");
//				}
//			} catch (IllegalStateException ex) {
//				System.err.println("IllegalStateException " + ex.getMessage() + "  at  " + (int) elbow.getX() + ", " + (int) elbow.getY());
//				ex.printStackTrace();
//			}
//	(Bend) delayedProp.value	}

	//--------------------------------------------------------------------------------
	private static String propTranslator(String inName) {
		if ("Node X Location".equalsIgnoreCase(inName)) return "x";		
		if ("Node Y Location".equalsIgnoreCase(inName)) return "y";		
		if ("Node Width".equalsIgnoreCase(inName)) return "Width";		
		if ("Node Height".equalsIgnoreCase(inName)) return "Height";		
		return inName;
	}

	//--------------------------------------------------------------------------------
	static List<DelayedVizProp> getPropsByID(final Iterable<DelayedVizProp> allProps, final Long id)
	{
		List<DelayedVizProp> props = new ArrayList<DelayedVizProp>();
		for (DelayedVizProp p : allProps)
		{
			if (id == p.netObj.getSUID())
				props.add(p);
		}
		return props;
	}
//	public String dump()
//	{
//		String propName = prop.getDisplayName();
//		String propvalue = value == null ? "EMPTY" : value.toString();
//		String propClass = value == null ? "EMPTY" : value.getClass().toString();
//		return("delayedProp: " + propName + " " + propvalue + " " + propClass);
//		
//	}
//	static public void showPoint(String name, Point2D pt) // DEBUG
//	{
//		System.out.println(String.format("%s: (%3.1f, %3.1f)", name, pt.getX(), pt.getY()));
//	}
//	
//	static private Point2D.Double getNodePosition(View<CyNode> nodeView) {
//		Double x = 0., y = 0.;
//		if (nodeView != null) {
//			x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
//			y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
//		}
//		return new Point2D.Double(x, y);
//	}
}
