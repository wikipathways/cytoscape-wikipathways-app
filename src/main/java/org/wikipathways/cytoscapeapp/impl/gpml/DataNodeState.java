package org.wikipathways.cytoscapeapp.impl.gpml;

//import diagrams.pViz.view.VNode;

/*
 * Model Node
 * A data structure that contains all persistent attributes
 * of a node in our graph. 
 * 
 * It is in charge of the VNode which is a StackPane in the view system.
 */
public class DataNodeState extends XRefable {

//	protected VNode hoststack;
	protected Model model;
//	public DataNode(AttributeMap am, Controller c)
//	{
//		this(am,c.getModel());
//	}	
	public DataNodeState(Model m)
	{
		super();
		model = m;
	}	

	public DataNodeState(AttributeMap am, Model m)
	{
		super(am);
		model = m;
		copyAttributesToProperties();
		setName("State: " + get("TextLabel"));
		
//		stack = new VNode(this, m.getController().getPasteboard());
	}

	public Model getModel()					{		return model;	}
	public String getShapeType() 			{		return get("ShapeType");	}
	public String getType() 				{		return get("Type");	}
	public String getLabel() 				{		return get("TextLabel");	}

	public String getInfoStr()	{ return "HTML Template for " + getGraphId() + "\n" + toString();	}
//	@Override public String toString()	{ return getGraphId() + " = " + getName();	}
	//---------------------------------------------------------------------------------------
	public String toGPML()	{ 
		StringBuilder bldr = new StringBuilder();
		buildNodeOpen(bldr);
		buildGraphicsTag(bldr);
		buildXRefTag(bldr);
		buildNodeClose(bldr);
		return bldr.toString();
	}
	
		
	String elementType = "State";
	String[]  nodeAttrs = {  "TextLabel", "GraphId", "GraphRef", "Type"};
	private void buildNodeOpen(StringBuilder bldr) {
		bldr.append("<State " + attributeList(nodeAttrs) + " >\n");
	}

	String[] attributeNames = { "RelX", "RelY", "Width", "Height",  "ShapeType" };
	private void buildGraphicsTag(StringBuilder bldr)
	{
		String attributes = attributeList(attributeNames);
		if (StringUtil.hasText(attributes))
			bldr.append( "<Graphics ").append(attributes).append( " />\n");
	}	

	private void buildNodeClose(StringBuilder bldr) {
		bldr.append("</" + elementType + ">\n");
	}	
}
