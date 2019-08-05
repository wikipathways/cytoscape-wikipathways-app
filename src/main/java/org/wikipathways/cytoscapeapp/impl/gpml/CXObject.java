package org.wikipathways.cytoscapeapp.impl.gpml;

import java.util.List;
//-------------------------------------------------------------------
class CartesianLayout
{
	int node;	
	String x;
	String y;
	String z;
	int view;

	public String toString()
	{
		return CXObject.iline("node", node)+ CXObject.iline("view", view) + CXObject.line("x", x) + CXObject.line("y", y) ;
	}
}

//-------------------------------------------------------------------
class NodeAttribute
{
	int propOf;	
	String name;
	String val;
	String type;
	String subnet;

	public String toString()
	{
		return CXObject.line("n", name) + CXObject.line("po", propOf) + CXObject.line("v", val) + CXObject.line("d", type);
	}
}

//-------------------------------------------------------------------
class EdgeAttribute
{
	String propOf;	
	String name;
	String val;
	String type;
	String subnet;

	EdgeAttribute(String pr, String nam, String v, String typ, String sub)
	{
		propOf = pr;
		name = nam;
		val = v;
		type = typ;
		subnet = sub;
	}
	public String toString()
	{
		return CXObject.line("n", name) + CXObject.line("po", propOf) + CXObject.line("v", val) + CXObject.line("d", type) + CXObject.line("s", subnet);
	}
}


class AnchorAttribute
{
	int propOf;	
	String position;

	public String toString()
	{
		return "?" + CXObject.line("n", position) + CXObject.line("po", propOf) + CXObject.line("v", position);		// TODO
	}
}

//-------------------------------------------------------------------
public class CXObject {
	
	private List<NodeAttribute> nodeAttributes;
	private List<EdgeAttribute> edgeAttributes;
	private List<DataNode> nodes;
	private List<Edge> edges;
	private List<CartesianLayout> cartesianLayout;
	
	public void addNode(DataNode node)
	{
		
	}	
	//-------------------------------------------------------------------
	class Node
	{
		int id;	
		String name;
		String rep;
		Node(int i, String n, String r)
		{
			id = i;
			name = n;
			rep = r;
		}
	
		public String toString()
		{
			return line("@id", id) + line("n", name) + line("r", rep);
		}
	}
	
	//-------------------------------------------------------------------
	class Edge
	{
		String id;	
		String src;
		String targ;
		String type;
		public String toString()
		{
			return line("@id", id) + line("s", src) + line("t", targ) + line("i", type);
		}
		public Edge(String i, String s, String t, String typ)
		{
			id = i;
			src = s;
			targ = t;
			type = typ;
		}
	}
	public void addEdge(Interaction e) {
		
		String id = e.getEdge().getId();
		Edge edge = new Edge(id, e.getEdge().getSourceid(), e.getEdge().getTargetid(), e.getInterType());
		edges.add(edge);
		for (String s : e.getEdge().getAttributes().keySet())
		{
			EdgeAttribute ea = new EdgeAttribute(id, s, e.getEdge().get(s), "string", "");
			edgeAttributes.add(ea);
		}
	}
	public void addAnchor(Anchor a)
	{
		
	}
	//-------------------------------------------------------------------
	class NetworkAttribute
	{
		String name;
		String val;
		String type;
		String subnet;
	
		public String toString()
		{
			return line("n", name) + line("v", val) + line("d", type) + line("s", subnet);
		}
		
	}
	//-------------------------------------------------------------------
	public static String line(String attr, int val)
	{
		return q(attr) + ": " + val + ",\n";
	}

	public static String iline(String attr, int val)
	{
		return q(attr) + ": " + val + ",\n";
	}

	public static String line(String attr, String val)
	{
		if (StringUtil.isEmpty(val)) return "";
		return q(attr) + ": " + q(val) + ",\n";
	}

	public static String line(String attr, double val)
	{
		return q(attr) + ": " + val + ",\n";
	}
	
	public static String q(String a)		{			return '"' + a + '"';		}
	public static String b(String a)		{			return '{' + a + '}';		}


}
