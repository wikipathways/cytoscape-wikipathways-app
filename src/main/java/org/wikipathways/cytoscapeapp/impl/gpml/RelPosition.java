package org.wikipathways.cytoscapeapp.impl.gpml;

public class RelPosition //extends Pair<Double, Double>
{
	double relX, relY;
	
	public RelPosition(double k, double v)
	{
		relX = k;
		relY = v;
	}

	public double x()	{ return relX;	}
	public double y()	{ return relY;	}
	
	public String toString()	{ return String.format("(%.02f, %.02f)" , relX,relY);	}
	public static RelPosition ZERO = new RelPosition(0,0);
	
	// values "1" ... "9" coming from port ids
	public static RelPosition idToRelPosition(String id)
	{
		double rX, rY;
		if (!StringUtil.isInteger(id)) return RelPosition.ZERO;
		int i = StringUtil.toInteger(id) - 1;

		if (i % 3 == 0) rX = -1;
		else if (i % 3 == 1) rX = 0;
		else rX= 1;

		if (i < 3) rY = -1;
		else if (i < 6) rY = 0;
		else rY = 1;
		
		return new RelPosition(rX,rY);
	}
	
	enum Pos			// TODO Hack
	{
		CENTER
	};
	
	public static Pos idToPosition(String id)
	{
		if (StringUtil.isInteger(id))
		{
			int i = StringUtil.toInteger(id);
			if (i >= 0 && i < Pos.values().length)
				return Pos.values()[i];
		}
		return Pos.CENTER;
	}

	
	public static RelPosition toRelPos(Pos pos) {
		String name = pos.name();
		double rx = 0, ry = 0;

		if (name.contains("LEFT")) rx = -1;
		else if (name.contains("RIGHT")) rx = 1;
		
		if (name.contains("TOP")) ry = -1;
		else if (name.contains("BOTTOM")) ry = 1;

		return new RelPosition(rx, ry);
	}

	public boolean isInside()
	{
		return Math.abs(relX) < 1 && Math.abs(relY) < 1;
	}

	public RelPosition moveToEdge()
	{
		double x = Math.abs(relX);
		double y = Math.abs(relY);
		if (Math.abs(relX) > Math.abs(relY))
			x = relX < 0 ? Math.floor(relX) : Math.ceil(relX);
		else
			y = relY < 0 ? Math.floor(relY) : Math.ceil(relY);
		return new RelPosition(x, y);
	}
//
//
//	private double getAdjustmentX(Pos srcPosition, double nodeWidth) {
//	
//		String s = srcPosition.name();
//		if (s.contains("LEFT")) 	return -nodeWidth / 2;
//		if (s.contains("RIGHT")) 	return nodeWidth / 2;
//		return 0;
//}
//	private double getAdjustmentY(Pos srcPosition, double nodeHeight) {
//		
//		String s = srcPosition.name();
//		if (s.contains("TOP")) 		return -nodeHeight / 2;
//		if (s.contains("BOTTOM")) 	return nodeHeight / 2;
//		return 0;
//}
}
