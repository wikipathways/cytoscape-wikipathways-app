package org.wikipathways.cytoscapeapp.impl.gpml;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;



@SuppressWarnings("serial")
public class DataNodeGroup extends DataNode {

	private Set<DataNode> members = new HashSet<DataNode>();
//	public List<DataNode> getChildren() { return children ; } 
	public void addMember(DataNode child)	{		members.add(child);	}
	public void clearMembers() 				{		members.clear();	}
	public Set<DataNode> getMembers() 		{		return members;	}
	final boolean compoundNode;			// a compound node leaves its children on the canvas, a group moves them into its own Group
	public boolean isCompoundNode() { return compoundNode;	}
	public Integer getGroupId()	{ return getInteger("GroupId");	}
	public DataNodeGroup(AttributeMap map, Model m, boolean isCompound)
	{
		super(map,m);
		compoundNode = isCompound;
//		System.out.println(model.traverseSceneGraph(getStack()));
	}	

	// go thru all children and get the enclosing rectangle
	// set the models attributes, and tell the stack to resize its shape
	public void calcBounds()
	{
		System.out.print("Group " + getGraphId() + " has membership of " + members.size());
//		groupView.getChildren().clear();
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
		if (members.isEmpty())
		{
			minX = minY = maxX = maxY = 0;
		}
		for (DataNode child : members)
		{
			double centerX =child.getDouble("CenterX");
			double centerY =child.getDouble("CenterY");
			double width =child.getDouble("Width");
			double height =child.getDouble("Height");
			double halfwidth = width / 2;
			double halfheight = height / 2;
			double left = centerX - halfwidth;
			double right = centerX + halfwidth;
			double top = centerY - halfheight;
			double bottom = centerY + halfheight;
			if (left < minX)  	minX = left;
			if (right > maxX)  	maxX = right;
			if (top < minY)  	minY = top;
			if (bottom > maxY)  maxY = bottom;
		}
		minX += 12;			// TODO FUDGE
		maxX += 12;
		putDouble("X", minX);
		putDouble("Y", minY);
		putDouble("CenterX", (minX + maxX) / 2.0);
		putDouble("CenterY", (minY + maxY) / 2.0);
		putDouble("Width",  maxX - minX);
		putDouble("Height",  maxY - minY);
		
//		getStack().setRect(minX, minY, maxX - minX, maxY - minY);
//		ShapeFactory.resizeFigureToNode(getStack());
		System.out.println(String.format(" and Bounds: [ ( %.2f,  %.2f )  %.2f x  %.2f]", minX, minY, maxX - minX, maxY - minY)); 
	}
//---------------------------------------------------------------------------
	public void assignMembers() {
		String groupId = get("GroupId");
		if (groupId == null) 		return;		//ERROR
		clearMembers();
		Map<Integer, DataNode> nodes = model.getDataNodeMap();
//		Pasteboard pasteboard = model.getController().getPasteboard();
		for (int nodeKey : nodes.keySet())
		{
			DataNode nod = nodes.get(nodeKey);
			String groupRef = nod.get("GroupRef");
			if (groupId.equals(groupRef))
			{
				addMember(nod);
//				pasteboard.bringInFront(nod, getStack());
			}
		}
		calcBounds();
		double minX = getDouble("X");
		double minY = getDouble("Y");
//		int i = -1 * getMembers().size() / 2;
//		double myHeight = getDouble("Height");
		for (DataNode node : getMembers())
		{
			if (compoundNode)
			{
				
			}
			else
			{
//				VNode childstack = node.getStack();
//				childstack.setId("" + node.getGraphId());
//	//			pasteboard.getContentLayer().remove(childstack);
//				stack.getChildren().add(childstack);
//				double x = node.getDouble("X");
//				double y = node.getDouble("Y");
//				double w = node.getDouble("Width");
//				double h = node.getDouble("Height");
//				childstack.setTranslateX(x-minX);
//				childstack.setTranslateY(y-minY-h);
//				childstack.setMouseTransparent(true);
			}
		}
//		System.out.println("inGroup: " + stack.getChildren().size());
		updateView();
		
	}

//	public BoundingBox getBounds() {
//		double padding = 5;
//		return new BoundingBox(minX-padding,minY-padding,0, maxX-minX + 2 * padding, maxY-minY+ 2 * padding, 0);
//	}

	public void updateView() {
//		if (stack != null) 
//			stack.setRect(getDouble("X"), getDouble("Y"), getDouble("Width"), getDouble("Height"));		
	}


	public void moveMembers(double dx, double dy) {
		for (DataNode node : members)
//			if (node.getStack() != null) 
			{
				node.putDouble("X", node.getDouble("X")+dx);
				node.putDouble("Y", node.getDouble("Y")+dy);
				node.putDouble("CenterX", node.getDouble("CenterX")+dx);
				node.putDouble("CenterY", node.getDouble("CenterY")+dy);
//				node.getStack().setRect(node.getDouble("X"), node.getDouble("Y"), node.getDouble("Width"), node.getDouble("Height"));		
			}
//		for (DataNode node : members)
//			node.getModel().getController().redrawMyEdges(node.getStack());
	}

	public void collapse() {
		System.out.println("collapse");	
	}

	public void expand() {
		System.out.println("expand");	
	}

	public String getName()					
	{	 
		String s = super.getName();
		if (StringUtil.isEmpty(s))
			s = get("Style");
		if (StringUtil.isEmpty(s))
			s = get("GroupId");
		return s;
	}
	
//	public VNode getStack()					{		return stack;	}
//	public void setStack(VNode st)			{		stack = st;	}
	public Model getModel()					{		return model;	}
	public String getShapeType() 			{		return "GroupComponent";	}
	public String getType() 				{		return get("Type");	}
	public String getLabel() 				{		return get("TextLabel");	}

	public String getInfoStr()	{ return "GROUP HTML Template for " + getGraphId() + "\n" + toString();	}
//	@Override public String toString()	{ return getGraphId() + " = " + getName();	}
	//---------------------------------------------------------------------------------------
	public String toGPML()	{ 
		StringBuilder bldr = new StringBuilder();
		buildNodeOpen(bldr);
		buildXRefTag(bldr);
//		buildNodeClose(bldr);
		return bldr.toString();
	}
	String elementType = "Group";
	String[]  nodeAttrs = {  "TextLabel", "GroupId", "GraphId", "Style", "GroupRef"};
	private void buildNodeOpen(StringBuilder bldr) {
		bldr.append("<Group " + attributeList(nodeAttrs) + " />\n");
	}

}
