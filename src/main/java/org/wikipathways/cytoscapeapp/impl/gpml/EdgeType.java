package org.wikipathways.cytoscapeapp.impl.gpml;

public enum EdgeType {

	simple,
	polyline,
	elbow,
	curved,
	cubic,
	polycubic;
	
	
	public static EdgeType lookup(String s)
	{
		if (null == s) return simple;
		String text = s.toLowerCase();
		for (EdgeType e : values())
			if (e.toString().equals(text))
				return e;
		return simple;
	}
}
