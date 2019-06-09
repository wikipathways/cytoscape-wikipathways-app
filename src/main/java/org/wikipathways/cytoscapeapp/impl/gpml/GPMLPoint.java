package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.geom.Point2D;
import java.util.List;


public class GPMLPoint {

	private double x = 0;
	private double y = 0;
	private double relX = 0;
	private double relY = 0;
	private ArrowType head = ArrowType.none;
	private int graphRef;
	private Interaction interaction;
	public void setInteraction(Interaction s)	{ interaction = s;	}

	public double getX()				{ return x;	}
	public void setX(double d)			{ x = d;	}

	public double getY()				{ return y;	}
	public void setY(double s)			{ y = s;	}

	public Point2D.Double getPoint()			{ return new Point2D.Double(x,y);	}
	public void setPoint(Point2D.Double p)		{ if (p != null) { setX(p.getX()); setY(p.getY()); }  }

	public int getGraphRef()			{ return graphRef;	}
	public void setGraphRef(int s)		{ graphRef = s;	}

	public ArrowType getArrowType()		{ return head;	}
	public void setArrowType(ArrowType s)	{ head = s;	}

	public void setPos(RelPosition.Pos pos)			{ setPos(RelPosition.toRelPos(pos)); }
	public void setPos(RelPosition pos)	{ setRelX(pos.x()); setRelY(pos.y()); }
	public double getRelX()				{ return relX;	}
	public void setRelX(double s)		{ relX = s;	}
	public double getRelY()				{ return relY;	}
	public void setRelY(double s)		{ relY = s;	}
	//-----------------------------------------------------------------------
	
	public GPMLPoint(Point2D.Double pt, Interaction i) {
		this(pt.getX(), pt.getY());
		interaction = i;
		graphRef = i == null ? 0 : i.getSourceid();
	}
	
	public GPMLPoint(Point2D.Double pt) {
		this(pt.getX(), pt.getY());
	}
	
	public GPMLPoint(double X, double Y) {
		x = X;
		y = Y;
	}

//	public GPMLPoint(double X, double Y, String ref, ArrowType h) {
//		x = X;
//		y = Y;
//		head = h;
//		graphRef = ref;
//	}
	
	public GPMLPoint(org.w3c.dom.Node node, Model model) {
		for (int i=0; i<node.getAttributes().getLength(); i++)
		{
			org.w3c.dom.Node child = node.getAttributes().item(i);
			String name = child.getNodeName();
			String val = child.getNodeValue();
			if ("X".equals(name))  				x = StringUtil.toDouble(val);
			else if ("Y".equals(name))  		y = StringUtil.toDouble(val);
			else if ("RelX".equals(name))  		relX = StringUtil.toDouble(val);
			else if ("RelY".equals(name))  		relY = StringUtil.toDouble(val);
			else if ("ArrowHead".equals(name))  head = ArrowType.lookup(val);
			else if ("GraphRef".equals(name))  
			{
				if (StringUtil.isNumber(val))
					graphRef = StringUtil.toInteger(val);
				else 
				{
					DataNode nod = model.find(val);
					if (nod != null)
						graphRef = nod.getId(); 
			}	}
			
		}
		if (graphRef == 0)
			System.err.println(String.format("%s %d (%.2f, %.2f)",  head.toString(),graphRef, x ,y )); 
		System.out.println(String.format("%s (%.2f, %.2f)",  head.toString(),x ,y )); 
	}
//	else if ("org.pathvisio.DoubleLineProperty".equals(name))  
//	{
//		relY = StringUtil.toDouble(val);
//	}

//	Pair<Double, Double > portToRelXY(String portId)
//	{
//		int id = StringUtil.toInteger(portId);
//		double relX = -1;
//		if ((id % 3)== 2) relX = 0;
//		if ((id % 3)== 0) relX = 1;
//		double relY = -1;
//		if (id > 3) relY = 0;
//		if (id > 6) relY = 1;
//		return new Pair<Double, Double >(relX, relY);
//	}
//	
	
	public void setXYFromNode()
	{
		if (interaction != null)		// the dragLine has no interaction defined
		{
			DataNode dataNode = interaction.getModel().findDataNode(graphRef);
			if (dataNode != null)
				setXY(dataNode, relX, relY);
		}
	}
	
	public void setXY(DataNode dataNode, double relX, double relY)
	{
		Point2D position = dataNode.getRelativePosition(relX, relY);
		x = position.getX();
		y = position.getY();
	}
	
	public String toGPML()
	{
		String firstPart = String.format("<Point X=\"%.2f\" Y=\"%.2f\" ", x, y);
		String secondPart = String.format("GraphRef=\"%s\" RelX=\"%.2f\" RelY=\"%.2f\" ArrowHead=\"%s\" />\n", getGraphRef(), getRelX(), getRelY(), getArrowType());
		return firstPart + secondPart;
	}

	
	public String toString()
	{
		return String.format("<Point X=\"%.2f\" Y=\"%.2f\' GraphRef=\"%s\" ArrowHead=\"%s\"", x, y, getGraphRef(), getArrowType());
	
	}

	public static void setInteraction(List<GPMLPoint> pts, Interaction edge) {
		for (GPMLPoint pt : pts)
			pt.setInteraction(edge);
	}
	
	public void setRelPosition(RelPosition.Pos srcPosition) {
		RelPosition pos = RelPosition.toRelPos(srcPosition);
		setRelX(pos.x());
		setRelY(pos.y());
		
	}

}
