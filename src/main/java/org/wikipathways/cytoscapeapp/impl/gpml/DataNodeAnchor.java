package org.wikipathways.cytoscapeapp.impl.gpml;
//
//import diagrams.pViz.view.VNode;
//import model.AttributeMap;
//
///*
// * DataNodeAnchor
// * A data structure that lets you put a endpoint along the path of a line 
// */
//public class DataNodeAnchor extends DataNode {
//	private static final long serialVersionUID = 1L;
//	public DataNodeAnchor(Model m)
//	{
//		super(m);
//	}	
//	static int counter = 4000;
//	static String getNextId()	{ return "id" + counter++; }
//	public DataNodeAnchor(AttributeMap am, Model m)
//	{
//		super(am,m);
//	}
//
//
//	public VNode getStack()					{		return stack;	}
//	public void setStack(VNode st)			{		 stack = st;	}
//	public Model getModel()					{		return model;	}
//	public Object getResource(String id) 	{		return model.getDataNode(id);	}
//	public Shape getShape() 				{		return getStack().getFigure();	}
//	public String getShapeType() 			{		return get("ShapeType");	}
//	public String getType() 				{		return get("Type");	}
//	public String getLabel() 				{		return get("TextLabel");	}
//
//	public String getInfoStr()	{ return "HTML Template for " + getGraphId() + "\n" + toString();	}
////	@Override public String toString()	{ return getGraphId() + " = " + getName();	}
//	//---------------------------------------------------------------------------------------
//	public String toGPML()	{ 
//		StringBuilder bldr = new StringBuilder();
//		buildNodeOpen(bldr);
//		buildXRefTag(bldr);
//		buildNodeClose(bldr);
//		return bldr.toString();
//	}
//	String elementType = "Anchor";
//	String[]  nodeAttrs = {  "TextLabel", "Position", "Shape", "GraphId"};
//	private void buildNodeOpen(StringBuilder bldr) {
//		bldr.append("<" + attributeList(nodeAttrs) + ">\n");
//	}
//	private void buildNodeClose(StringBuilder bldr) {
//		bldr.append("</" + elementType + ">\n");
//	}
//}
