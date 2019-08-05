package org.wikipathways.cytoscapeapp.impl.gpml;

public class Anchor extends DataNode {

	/**
	 *  An Anchor is a DataNode that lives along an edge of a line.  
	 *  Catalysis and Inhibition interactions connect to anchors
	 */
	
	private static final long serialVersionUID = 1L;
//	private Circle myShape;
	private String myInteractionId;

	
	public boolean isAnchor() 					{		return true;}
	public String getInteraction() 		{  	return myInteractionId;	}
	public void setInteraction(String id) 		
	{  
//		myInteraction = (Interaction) e;  
		setInteractionId (id);	
	}
	public String getInteractionId() 				{  	return  get("InteractionId");	}
	public void setInteractionId(String e) 			{  	put("InteractionId", e);	}
//	
	public double getAnchorPosition()				{  	return getDouble("Position");	}
	public void setPosition(double d)				{   putDouble("Position", d);	}
	//=========================================================================

	public Anchor(org.w3c.dom.Node node, Model m, String interactionId)
	{
		this(new AttributeMap(node.getAttributes()), m, interactionId);	
	}	
	
	public Anchor(AttributeMap attr, Model m, String interactionId)
	{
		super(attr, m);
		setName(String.format("Anchor @ %.2f", getAnchorPosition()));
		setInteractionId(interactionId);
	}

	//=========================================================================
//	public void resetPosition(Edge caller)
//	{
//		Shape myShape = getStack().getFigure();
//		double position = getAnchorPosition();
//		if (Double.isNaN(position))
//			position = 0.5;
//		if (caller == null) return;
//		EdgeLine edgeLine = caller.getEdgeLine();
//		Point2D.Double pt = edgeLine.getPointAlongLine(position);
//		if (!edgeLine.getChildren().contains(myShape))
//			edgeLine.getChildren().add(myShape);
////		String myId = get("GraphId");
////		System.out.println(String.format("position: %.2f id: %s @ [ %.2f, %.2f] ",position, myId,  pt.getX(), pt.getY()));
//		if (myShape instanceof Circle) 
//		{
//			Circle c = ((Circle)(myShape));
//			c.setCenterX(pt.getX());
//			c.setCenterY(pt.getY());
//			putDouble("CenterX", pt.getX());
//			putDouble("CenterY", pt.getY());
//			putDouble("X", pt.getX()-(c.getRadius()/2));
//			putDouble("Y", pt.getY()-(c.getRadius()/2));
//		}
//		List< Interaction>  links = model.findInteractionsByNode(this);
//		for (Interaction e : links)
//			if (e != caller)
//				e.connect();
//	}
//	
//=========================================================================
	public String toString()
	{
		return getAttributes().toString();
		
	}
	
	public String toGPML()
	{
		String shape = get("Shape");
		if (shape == null) shape = "Oval";
		return String.format("<Anchor Position=\"%.2f\" Shape=\"%s\" GraphId=\"%s\" />\n", getAnchorPosition(), shape, getGraphId());
	}
	
	public void toCX(CXObject cx)	{		cx.addAnchor(this);	}

}
