package org.wikipathways.cytoscapeapp;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.EdgeBendVisualProperty;
import org.cytoscape.view.presentation.property.values.Bend;
import org.cytoscape.view.presentation.property.values.Handle;
import org.cytoscape.view.presentation.property.values.HandleFactory;

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
class DelayedVizProp {
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
	static boolean verbose = true;
	public String toString() {  return prop.getDisplayName() + ": " + value.toString(); }
	public static void applyAll(final CyNetworkView netView,final Iterable<DelayedVizProp> delayedProps, WPManager mgr) 
	{
		for ( DelayedVizProp delayedProp : delayedProps) {
			final Object value = delayedProp.value;
			if (value == null) continue;

		String propName = delayedProp.prop.getDisplayName();
		if ("Node Shape".equals(propName))
			applyNodeShape(netView, delayedProps, mgr, delayedProp);

		if ("Edge Bend".equals(propName))
			applyEdgeBend(netView, mgr, delayedProp);
			
	
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
//		System.out.println("Node id: " + delayedProp.netObj.getSUID()  + " is setting " + propName + " to " + propvalue);

      String prop = delayedProp.prop.getIdString();
//      System.out.println(prop + " @ " + delayedProp.prop.getClass() + " # " + delayedProp.prop.getDisplayName() +  " = " + value + " @ " + value.getClass());
      boolean isPosition = prop.equals("NODE_X_LOCATION") || prop.equals("NODE_Y_LOCATION");
     if (delayedProp.isLocked && !isPosition)
        view.setLockedValue(delayedProp.prop, value);
       else 
        view.setVisualProperty(delayedProp.prop, value);
    }
  }

//	public String dump()
//	{
//		String propName = prop.getDisplayName();
//		String propvalue = value == null ? "EMPTY" : value.toString();
//		String propClass = value == null ? "EMPTY" : value.getClass().toString();
//		return("delayedProp: " + propName + " " + propvalue + " " + propClass);
//		
//	}
	//--------------------------------------------------------------------------------
	private static void applyNodeShape(final CyNetworkView netView,final Iterable<DelayedVizProp> delayedProps, WPManager mgr, DelayedVizProp delayedProp) 		{
		final Map<String,String> map = new HashMap<String,String>();
		CyNode src = (CyNode) delayedProp.netObj;
		List<DelayedVizProp> relatedProps = getPropsByID(delayedProps, src.getSUID());
		double wid = 0;
		double hght = 0;
		double x = Double.NaN;
		double y = Double.NaN;
//		double z = Double.NaN;
//		System.out.println("applyNodeShape");
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
				if ("x".equals(lookup))				x = Double.valueOf(propvalue1);
				if ("y".equals(lookup))				y = Double.valueOf(propvalue1);
//				if ("z".equals(lookup))				z = Double.valueOf(propvalue1);
			}
		}
		String propvalue = delayedProp.value.toString();
		
//		if ("Round Rectangle".equals(propvalue))
//		{
//			map.put("edgeThickness", "4.3");
//			propvalue = "Rounded Rectangle";
//		}
		if ("Octagon".equals(propvalue))			// HACK - should look for group node
			return;
		Shape thePath = getShapePath(propvalue);
		ShapeAnnotation mAnnotation = null;
		
//		map.put("canvas", "background");
		mAnnotation = mgr.getAnnots().newShape(netView, map);
//		mAnnotation.setCanvas("background");			BUG:  this will cause annotation to move to 0,0!
		if (thePath != null)
			mAnnotation.setCustomShape(thePath);
		else 
			mAnnotation.setShapeType(propvalue);  

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
			}
			// view.setLockedValue(prop, 0.);
		}
//		if (verbose) 
//		{
//			System.out.println("size: "+ (int) x + ",  "+ (int)  y +  " # " + (int) wid + ",  "+ (int)  hght);
//			System.out.println("mAnnotation: "+ mAnnotation);	
//			System.out.println("---> legal size: "+ legalSize + " legal pos: "+ legalXY);
//		}
//		
		boolean deleteNode = true;  //thePath != null;		
		if (deleteNode)
		{
			View<CyNode> view = netView.getNodeView(src);
			view.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
			netView.getModel().removeNodes(Collections.singletonList((src)));	
//			mAnnotation.setCanvas("background");		//	BUG:  this will cause annotation to move to 0,0!
		}

	}

	// --------------------------------------------------------------------------------
	private static void applyEdgeBend(final CyNetworkView netView, WPManager mgr, final DelayedVizProp delayedProp) {
		if (delayedProp.value == EdgeBendVisualProperty.DEFAULT_EDGE_BEND)
			return;

		try {
			HandleFactory handleFactory = mgr.getHandleFactory();
			// System.out.println("handleFactory: " + handleFactory.toString());
			if (delayedProp.netObj != null) {
				CyEdge edge = (CyEdge) delayedProp.netObj;
				CyNode src = edge.getSource();
				CyNode targ = edge.getTarget();
				// if (src == null || src != null) return;
				View<CyNode> srcView = netView.getNodeView(src);
				View<CyNode> targView = netView.getNodeView(targ);
				View<CyEdge> edgeView = netView.getEdgeView(edge);
				// System.out.println("srcView: " + srcView.getSUID());
				// System.out.println("targView: " + targView.getSUID());
				// System.out.println("edgeView: " + edgeView.getSUID());

				// edgeView.getVisualProperty(BasicVisualLexicon.C);

				Point2D.Double srcCenter = getNodePosition(srcView);
				Point2D.Double targCenter = getNodePosition(targView);

				Point2D.Double elbow = new Point2D.Double(srcCenter.getX(), targCenter.getY()); // TODO -- two  choices here!
				//
				// showPoint("src", srcCenter);
				// showPoint("target", targCenter);
				// showPoint("elbow", elbow);
				//

				// boolean isCurved = 1 == EdgeView.CURVED_LINES;
				if (edgeView != null) {
					Bend bend = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_BEND);
					// if (verbose)
					// System.out.println("bend: " + bend.getAllHandles().size()
					// + " handles " + (delayedProp.isLocked ? "LOCKED" : "UNLOCKED"));

					List<Handle> handles = bend.getAllHandles();
					if (handles.size() > 0) {
						try {
							handles.get(0).defineHandle(netView, edgeView, elbow.getX(), elbow.getY());
						} catch (IllegalStateException ex) {
							System.err.println(
									"IllegalStateException at " + (int) elbow.getX() + ", " + (int) elbow.getY());
						}
					} else
						handles.add(handleFactory.createHandle(netView, edgeView, elbow.getX(), elbow.getY()));
				}
			}
		} catch (ClassCastException ex) {
			System.out.println("->ClassCastException: " + delayedProp.netObj.getClass());
		}
	}

	//--------------------------------------------------------------------------------
	private static Shape getShapePath(String propvalue) {
		if (verbose) System.out.println("getShapePath: " + propvalue);
		if ("Rounded Rectangle".equals(propvalue)) 			  	return makeRoundRect();
		if ("Round Rectangle".equals(propvalue)) 			  	return makeRoundRect();
		if ("Cell".equals(propvalue)) 					  		return makeRoundRect();
		if ("Organelle".equals(propvalue)) 					  	return makeRoundRect();

		if ("Oval".equals(propvalue)) 					  		return makeEllipse();
		if ("Nucleus".equals(propvalue)) 					  	return makeEllipse();
		if ("Vesicle".equals(propvalue)) 					  	return makeEllipse();
		if ("Ellipse".equals(propvalue)) 					  	return makeEllipse();
	
		if ("Mitochondria".equals(propvalue)) 			  		return makeMitochondria();
		if ("Endoplasmic Reticulum".equals(propvalue))  		return makeER();
		if ("Sarcoplasmic Reticulum".equals(propvalue)) 		return makeSR();
		if ("Golgi Apparatus".equals(propvalue)) 				return makeGolgi();
		if ("Brace".equals(propvalue))  						return makeBrace();
		if ("Triangle".equals(propvalue))  						return makeTriangle();
		if ("Arc".equals(propvalue))  							return makeArc();
		return null;
	}

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
	  static public void showPoint(String name, Point2D pt)		// DEBUG
	  {
		System.out.println(String.format("%s: (%3.1f, %3.1f)" , name, pt.getX() , pt.getY()));
	  }
		static private Point2D.Double getNodePosition(View<CyNode> nodeView) {
		    Double x = 0.;
		    Double y = 0.;
		    if (nodeView != null)
		    {
		    	x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			    y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
		    }
			return new Point2D.Double(x,y);
		}
//
//		static private String getNodeName(View<CyNode> nodeView) {
//		    Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
//		    Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
//			return nodeView.getVisualProperty(BasicVisualLexicon.);
//		}
//
		//--------------------------------------------------------------------------------
//https://stackoverflow.com/questions/14169234/the-relation-of-the-bezier-curve-and-ellipse
	static private Shape makeEllipse() {
		GeneralPath path = new GeneralPath();

		double x = 0;
		double y = 0;
		double w = 100;
		double h = 100;

		double kappa = 0.5522848;
		double ox = (w / 2) * kappa; // control point offset horizontal
		double oy = (h / 2) * kappa; // control point offset vertical
		double xe = x + w; // x-end
		double ye = y + h; // y-end
		double xm = x + w / 2; // x-middle
		double ym = y + h / 2; // y-middle

		path.moveTo(x, ym);
		path.curveTo(x, ym - oy, xm - ox, y, xm, y);
		path.curveTo(xm + ox, y, xe, ym - oy, xe, ym);
		path.curveTo(xe, ym + oy, xm + ox, ye, xm, ye);
		path.curveTo(xm - ox, ye, x, ym + oy, x, ym);

		x = 2;
		y = 2;
		w = 96;
		h = 96;

		ox = (w / 2) * kappa; // control point offset horizontal
		oy = (h / 2) * kappa; // control point offset vertical
		xe = x + w; // x-end
		ye = y + h; // y-end
		xm = x + w / 2; // x-middle
		ym = y + h / 2; // y-middle

		path.moveTo(x, ym);
		path.curveTo(x, ym - oy, xm - ox, y, xm, y);
		path.curveTo(xm + ox, y, xe, ym - oy, xe, ym);
		path.curveTo(xe, ym + oy, xm + ox, ye, xm, ye);
		path.curveTo(xm - ox, ye, x, ym + oy, x, ym);
		path.closePath();
		return path;
	}
		//--------------------------------------------------------------------------------
		static private Shape makeRoundRect()
		{
			float width = 100.f;
			float height = 100.f;
			float x = 0.0f;
			float y = 0.0f;
			float curveRad = 8.0f;
			
			GeneralPath path = new GeneralPath();
//			path.moveTo(-100, -100);
			path.moveTo(x, curveRad);
			path.curveTo(x, y, curveRad, y, x+curveRad, y);
			path.lineTo(width - curveRad, y);
			path.curveTo(width, y, width, curveRad, width, curveRad);
			path.lineTo(width, height - curveRad);
			path.curveTo(width, height, width - curveRad, height, width - curveRad, height);
			path.lineTo(curveRad, height);
			path.curveTo(x, height, x, height - curveRad, x, height - curveRad);
			path.lineTo(x, curveRad);
			
			width = 98.f;
			height = 98.f;
			x = 1.0f;
			y = 1.0f;
			path.moveTo(x, curveRad);
			path.curveTo(x, y, curveRad, y, x+curveRad, y);
			path.lineTo(width - curveRad, y);
			path.curveTo(width, y, width, curveRad, width, curveRad);
			path.lineTo(width, height - curveRad);
			path.curveTo(width, height, width - curveRad, height, width - curveRad, height);
			path.lineTo(curveRad, height);
			path.curveTo(x, height, x, height - curveRad, x, height - curveRad);
			path.lineTo(x, curveRad);
			return path;
	    }
		
//		path.moveTo(0.0, 8.0);
//		path.curveTo(0.0, 0.0, 8.0, 0.0, 8.0, 0.0);
//		path.lineTo(width - 8.0, 0.0);
//		path.curveTo(width, 0.0, width, 8.0, width, 8.0);
//		path.lineTo(width, height - 8.0);
//		path.curveTo(width, height, width - 8.0, height, width - 8.0, height);
//		path.lineTo(8.0, height);
//		path.curveTo(0.0, .height, 0.0, height - 8.0, 0, height - 8.0);
//		path.closePath();

		static private GeneralPath makeArc()				// PLACEHOLDER -- arc needs start angle & rotation
		{
			GeneralPath path = new GeneralPath();
			path.moveTo (50f, 0f);
			path.quadTo (0f, 0f,50f, 50f);
//			path.quadTo (50f, 100f, 50f, 50f);

//			path.closePath();
			return path;
	  }
		
	static private GeneralPath makeMitochondria()
	  {
		GeneralPath path = new GeneralPath();
		path.moveTo (72.81f, 85.70f);
		path.curveTo (97.59f, 83.01f, 94.55f, 147.38f, 119.28f, 144.29f);
		path.curveTo (166.27f, 144.40f, 136.22f, 42.38f, 175.51f, 41.70f);
		path.curveTo (215.08f, 41.02f, 188.27f, 150.12f, 227.79f, 148.28f);
		path.curveTo (271.14f, 146.25f, 230.67f, 29.04f, 274.00f, 26.55f);
		path.curveTo (317.72f, 24.05f, 290.58f, 142.55f, 334.36f, 143.22f);
		path.curveTo (371.55f, 143.80f, 351.55f, 43.14f, 388.66f, 45.75f);
		path.curveTo (429.51f, 48.62f, 392.43f, 153.80f, 432.85f, 160.40f);
		path.curveTo (459.82f, 164.80f, 457.96f, 94.30f, 485.13f, 97.26f);
		path.curveTo (548.33f, 124.69f, 534.13f, 233.75f, 472.75f, 258.89f);
		path.curveTo (454.92f, 261.42f, 450.22f, 220.87f, 432.35f, 223.03f);
		path.curveTo (400.60f, 226.86f, 409.73f, 303.71f, 377.80f, 301.95f);
		path.curveTo (348.05f, 300.30f, 365.16f, 223.61f, 335.37f, 223.28f);
		path.curveTo (295.83f, 222.85f, 316.30f, 327.99f, 276.78f, 326.44f);
		path.curveTo (241.90f, 325.08f, 266.95f, 236.11f, 232.34f, 231.61f);
		path.curveTo (200.07f, 227.42f, 201.79f, 311.88f, 169.71f, 306.49f);
		path.curveTo (134.22f, 300.53f, 167.04f, 209.92f, 131.32f, 205.60f);
		path.curveTo (110.14f, 203.04f, 116.28f, 257.74f, 94.95f, 258.26f);
		path.curveTo (15.35f, 236.77f, 5.51f, 114.51f, 72.81f, 85.70f);
		path.closePath();
		path.moveTo (272.82f, 0.84f);
		path.curveTo (378.97f, 1.13f, 542.51f, 62.39f, 543.54f, 168.53f);
		path.curveTo (544.58f, 275.18f, 381.50f, 342.19f, 274.84f, 342.28f);
		path.curveTo (166.69f, 342.36f, 0.84f, 274.66f, 2.10f, 166.51f);
		path.curveTo (3.33f, 60.72f, 167.03f, 0.56f, 272.82f, 0.84f);
		path.closePath();
		return path;
	  }

	static private GeneralPath makeSR()
	{
//		System.out.println("--------------makeSR--------------" );
		GeneralPath path = new GeneralPath();

		path.moveTo(118.53f, 16.63f);
		path.curveTo(34.13f, 22.00f, 23.84f, 107.76f, 49.44f, 169.22f);
		path.curveTo(73.73f, 242.63f, 0.51f, 289.88f, 56.13f, 366.83f);
		path.curveTo(99.99f, 419.32f, 176.93f, 391.26f, 192.04f, 332.54f);
		path.curveTo(207.42f, 271.52f, 163.49f, 228.38f, 183.45f, 168.61f);
		path.curveTo(211.75f, 89.03f, 181.43f, 16.01f, 118.53f, 16.63f);
		path.lineTo(118.53f, 16.63f);
		path.closePath();
		return path;
	}

	static private GeneralPath makeER()
	{
//		System.out.println("--------------makeER--------------" );
		GeneralPath path = new GeneralPath();
		path.moveTo (115.62f, 170.76f);
		path.curveTo (106.85f, 115.66f, 152.29f , 74.72f, 152.11f , 37.31f);
		path.curveTo (151.57f, 22.91f, 135.75f , 10.96f, 123.59f , 21.51f);
		path.curveTo (97.02f, 44.83f, 99.19f , 108.29f, 90.52f , 146.58f);
		path.curveTo (89.97f, 157.27f, 79.04f , 153.89f, 78.44f , 145.14f);
		path.curveTo (69.32f, 111.41f, 105.16f , 72.62f, 87.74f , 58.00f);
		path.curveTo (57.12f, 33.80f, 42.90f , 120.64f, 53.32f , 143.34f);
		path.curveTo (65.01f, 185.32f, 49.93f , 215.62f, 42.80f , 189.23f);
		path.curveTo (39.00f, 173.52f, 52.26f , 156.40f, 41.55f , 141.32f);
		path.curveTo (34.82f, 133.03f, 23.22f , 139.41f, 16.36f , 150.49f);
		path.curveTo (0.00f, 182.29f, 23.74f , 271.85f, 49.05f , 257.53f);
		path.curveTo (56.38f, 251.73f, 44.01f , 231.76f, 55.14f , 229.10f);
		path.curveTo (66.52f, 226.70f, 63.22f , 247.43f, 67.13f , 256.43f);
		path.curveTo (70.73f, 268.42f, 74.67f , 281.17f, 83.91f , 290.85f);
		path.curveTo (91.38f, 298.36f, 107.76f , 297.10f, 110.06f , 285.05f);
		path.curveTo (113.23f, 257.62f, 69.35f , 201.07f, 93.40f , 192.41f);
		path.curveTo (122.33f, 184.37f, 100.80f , 263.03f, 131.30f , 280.35f);
		path.curveTo (146.12f, 286.36f, 155.69f , 278.51f, 154.40f , 268.41f);
		path.curveTo (150.12f, 235.05f, 115.21f , 201.24f, 115.47f , 170.24f);
		path.lineTo (115.62f, 170.76f);
		path.closePath();
		return path;
	}

	static private GeneralPath makeGolgi()
	{
//		System.out.println("--------------makeGolgi--------------" );
		GeneralPath path = new GeneralPath();
		path.moveTo (148.89f, 77.62f);
		path.curveTo (100.07f, 3.50f, 234.06f , 7.65f, 207.78f , 62.66f);
		path.curveTo (187.00f, 106.50f, 171.09f , 190.54f, 209.13f , 287.47f);
		path.curveTo (240.55f, 351.33f, 111.35f , 353.69f, 144.36f , 284.72f);
		path.curveTo (171.13f, 215.31f, 165.77f , 107.32f, 148.89f , 77.62f);
		path.lineTo (148.89f, 77.62f);
		path.closePath();
		path.moveTo (88.16f, 91.24f);
		path.curveTo (62.70f, 40.69f, 158.70f , 44.41f, 131.59f , 92.83f);
		path.curveTo (116.28f, 128.91f, 117.95f , 238.10f, 134.33f , 269.85f);
		path.curveTo (154.45f, 313.72f, 56.82f , 315.51f, 85.96f , 264.54f);
		path.curveTo (102.37f, 223.58f, 110.67f , 141.16f, 88.16f , 91.24f);
		path.lineTo (88.16f, 91.24f);
		path.closePath();
		path.moveTo (83.40f, 133.15f);
		path.curveTo (86.43f, 160.23f, 86.72f , 203.15f, 82.05f , 220.09f);
		path.curveTo (73.24f, 250.74f, 69.98f , 262.93f, 50.80f , 265.89f);
		path.curveTo (32.17f, 265.52f, 22.80f , 242.80f, 39.49f , 227.87f);
		path.curveTo (50.94f, 214.61f, 53.98f , 202.20f, 55.20f , 173.72f);
		path.curveTo (54.63f, 152.16f, 56.07f , 133.57f, 43.25f , 126.63f);
		path.curveTo (25.26f, 121.45f, 30.31f , 86.90f, 56.06f , 93.20f);
		path.curveTo (69.86f, 95.63f, 79.23f , 109.03f, 83.40f , 133.15f);
		path.lineTo (83.40f, 133.15f);
		path.closePath();
		return path;
	}

	static private GeneralPath makeBrace()
	{
//		System.out.println("--------------makeBrace--------------" );
		GeneralPath path = new GeneralPath();
		path.moveTo(0, 4);
		path.quadTo(0, 2, 3, 2);
		path.quadTo(6, 2, 6, 0);
		path.quadTo(6, 2, 9, 2);
		path.quadTo(12, 2, 12, 4);
		path.closePath();
		return path;
	}

	static private GeneralPath makeTriangle()
	{
//		System.out.println("--------------makeTriangle--------------" );
		GeneralPath path = new GeneralPath();
		path.moveTo(0, 4);
		path.lineTo(0, -4);
		path.lineTo(16, 0);
		path.lineTo(0, 4);
		path.closePath();
		return path;
	}

}
