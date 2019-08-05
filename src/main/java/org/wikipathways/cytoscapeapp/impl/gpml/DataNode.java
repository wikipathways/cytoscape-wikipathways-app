package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNode;
import org.w3c.dom.NamedNodeMap;

/*
 * Model Node
 * A data structure that contains all persistent attributes
 * of a node in our graph. 
 * 
 * It is in charge of the VNode which is a StackPane in the view system.
 * Parent class XRefable is an AttributeMap with added properties to support binding in tables
 */
@SuppressWarnings("serial")
public class DataNode {

	protected Model model;
	protected CyNode stack;
	protected XRefable attributes;
//	public DataNode(AttributeMap am, Controller c)
//	{
//		this(am,c.getModel());
//	}	
	public DataNode(Model m)
	{
		super();
		model = m;
		attributes = new XRefable();
	}	
	static int counter = 4000;
	static String getNextId()	{ return "id" + counter++; }
	public DataNode(AttributeMap am, Model m)
	{
		super();
		attributes = new XRefable(am);
		model = m;
//		int id = getId();
//		if (id <= 0) 
//			id = model.gensym();
//		setId(id);
//		putInteger("GraphId", id );
	}
	public AttributeMap getAttributes()	{ return attributes;	}
	public String getId()  { return attributes.get("GraphId"); } 
	public void setId(String i)  { attributes.setId(i); } 
	public String getGraphId()  { return attributes.getGraphId(); } 
	public double getDouble(String s)  { return attributes.getDouble( s); } 
	public double getDouble(String s, double deft)  { return attributes.getDouble( s, deft); } 
	public void putDouble(String s, double d)  { attributes.putDouble( s, d); } 
	public void put(String s, String d)  { attributes.put( s, d); } 
	public String get(String s)  { return attributes.get( s); } 
	public int getInteger(String s)  { return attributes.getInteger( s); } 
	public void putInteger(String s, int i)  { attributes.putInteger( s, i); } 
	public boolean getBool(String s) { return attributes.getBool(s);	}
	public boolean getBool(String s, boolean b) { return attributes.getBool(s, b);	}
//	public void copyPropertiesToAttributes() {attributes.copyAttributesToProperties(); } 
//	public void copyAttributesToProperties() {attributes.copyAttributesToProperties(); } 
	public void putBool(String s, boolean i)  { attributes.putBool( s, i); } 
	public void add(NamedNodeMap n) { attributes.add(n);  }
	public String getDatabase() {		return attributes.getDatabase();	}
	public String getDbid() {		return attributes.getDbid();	}
	public String getName() {		return attributes.getName();	}
	public void setName(String s) {		attributes.setName(s);	}
	public void setType(String s) {		attributes.setType(s);	}
	public Color getColor(String string) {		return attributes.getColor(string);	}
//	public VNode getStack()					{		return stack;	}
//	public void setStack(VNode st)			{		 stack = st;	}
	public Model getModel()					{		return model;	}
	public Object getResource(String id) 	{		return model.getDataNode(id);	}
//	public Shape getShape() 				{		return getStack().getFigure();	}
//	public String getGraphId() 				{		return get("GraphId");	}
	public String getShapeType() 			{		return get("ShapeType");	}
	public String getType() 				{		return get("Type");	}
	public String getLabel() 				{		return get("TextLabel");	}
	public void rememberPosition() 			
	{	
//		double width =  stack.getWidth();
//		double height =  stack.getHeight();
//		
//		putDouble("X",  stack.getLayoutX());	
//		putDouble("Y",  stack.getLayoutY());	
//		putDouble("CenterX",  stack.getLayoutX() + width / 2);	
//		putDouble("CenterY",  stack.getLayoutY() + height / 2);	
//		putDouble("Width", width);	
//		putDouble("Height",  height);	
	}
	public void removeSelf() {	}		// this allows groups to do more disposal

	
	public String toString()	{ return "[" + getGraphId() + "] " + getLabel() + ' ' + getShapeType();  }
	public String getInfoStr()	{ return "HTML Template for " + getGraphId() + "\n" + toString();	}
//	@Override public String toString()	{ return getGraphId() + " = " + getName();	}
	
	public DataNodeGroup getGroup() {
		int ref = getInteger("GroupRef");
		if (ref <= 0) return null;
		Map<String, DataNodeGroup> map = model.getGroupMap();
		DataNodeGroup gp = map.get(ref);
		return gp;
	}	
	//---------------------------------------------------------------------------------------
	public void toCX(CXObject cx )	{ 		cx.addNode(this);	}

	
	public String toGPML()	{ 
//		copyPropertiesToAttributes();
		StringBuilder bldr = new StringBuilder();
		buildNodeOpen(bldr);
		buildAttributeTag(bldr);
		buildGraphicsTag(bldr);
		attributes.buildXRefTag(bldr);
		buildNodeClose(bldr);
		return bldr.toString();
	}
	String elementType;
	String[]  nodeAttrs = {  "TextLabel", "GraphId", "GroupRef", "Type"};
	String[]  dataNodeTypes = {  "gene", "geneproduct", "protein", "metabolite", "rna"};
	boolean isDataNode(String typ) {
		if (typ == null) return false;
		typ = typ.toLowerCase();
		for (String t : dataNodeTypes)
			if (t.equals(typ))  return true;
		return false;
	}
	private void buildNodeOpen(StringBuilder bldr) {
		String typ = get("Type");
		String shapetyp = get("ShapeType");
		String label = get("TextLabel");
		if ("GraphicalLine".equals(shapetyp)) elementType = "GraphicalLine";
		else if ("Shape".equals(typ)) elementType = "Shape";
		else if ("Label".equals(typ)) elementType = "Label";
//		else if ("Pathway".equals(typ)) elementType = "Pathway";
		else if (isDataNode(typ))		elementType =  "DataNode";
		else
			elementType ="DataNode";
		
		bldr.append("<" + elementType + " " + attributes.attributeList(nodeAttrs) + ">\n");
	}
	private void buildNodeClose(StringBuilder bldr) {
		bldr.append("</" + elementType + ">\n");
	}

	String[]  attrs = {  "Key", "Value"};
	void buildAttributeTag(StringBuilder bldr)
	{
		String attribs = attributes.attributeList(attrs);
		if (StringUtil.hasText(attribs))
			bldr.append( "<Attribute ").append(attribs).append( "/>\n");
	}
	
	String[] attributeNames = { "CenterX", "CenterY", "Width", "Height", 
			"ShapeType", "ZOrder", "Valign", "FillColor", "Color", 
			"FontSize", "FontWeight", "FontStyle" };
	void buildGraphicsTag(StringBuilder bldr)
	{
		
		
		if (get("CenterX") == null && get("X") != null)
		{
			double x = getDouble("X");
			double y = getDouble("Y");
			double w = getDouble("Width");
			double h = getDouble("Height");
			putDouble("CenterX", x + w /2);
			putDouble("CenterY", y + h /2);
		}
		String attribs = attributes.attributeList(attributeNames);
		if (StringUtil.hasText(attribs))
			bldr.append( "<Graphics ").append(attributes).append( " />\n");
		if (points != null && !points.isEmpty())
		{	// TODO stream points if its not empty
			//must change lines above to terminate after the Points
		}
	}
	// **-------------------------------------------------------------------------------
	public Point2D.Double  getAdjustedPoint(GPMLPoint gpmlPt)
	{
		if (gpmlPt == null) return new Point2D.Double(0,0);
		double relX = gpmlPt.getRelX();
		double relY = gpmlPt.getRelY();
		double cx = getDouble("CenterX");
		double cy = getDouble("CenterY");
		double width = getDouble("Width", 0);
		double height = getDouble("Height", 0);
		double x = cx + relX * width / 2;
		double y = cy + relY * height / 2;
		return new Point2D.Double(x, y);
	}
	// **-------------------------------------------------------------------------------
	public boolean isResizable() 	{	return getBool("Resizable", true);	}
	public boolean isConnectable() 	{	return getBool("Connectable", true);	}
	public boolean isSelectable() 	{	return getBool("Selectable", true);	}
	public boolean isMovable() 		{	return getBool("Movable", true);	}
	public boolean isLocked() 		{	return !getBool("Movable", true);	}
	
	public void setResizable(boolean b) {		 putBool("Resizable",b);	}
	public void setConnectable(boolean b) {		 putBool("Connectable",b);	}
	public void setSelectable(boolean b) {		 putBool("Selectable",b);	}
	public void setMovable(boolean b)	 {		 putBool("Movable",b);	}

	public void applyEditable(boolean mov, boolean resiz, boolean edit, boolean connect) {
		setMovable(mov);
		setResizable(resiz);
		setSelectable(edit);
		setConnectable(connect);
	}
	public boolean isAnchor() {		return false;}
	// **-------------------------------------------------------------------------------
	// side case:  shapes are dataNodes, but they may be a path or GraphicalLine
	
	List<GPMLPoint> points = null;
	public void addPointArray(List<GPMLPoint> pts) {
		points = new ArrayList<GPMLPoint>();
		points.addAll(pts);
	}
	public List<GPMLPoint> getGPMLPoints()	{ return points;	}
	// **-------------------------------------------------------------------------------
	public boolean contains(String s)
	{
		if (StringUtil.isEmpty(s)) return false;
//		String str = s.toUpperCase();		done by caller
		for (String val : attributes.values())
			if (val.toUpperCase().indexOf(s) >= 0)
				return true;
		return false;
	}
	public Point2D getRelativePosition(double relX, double relY) {
		// TODO Auto-generated method stub
		return null;
	}
	public void addState(DataNodeState statenode) {
		// TODO Auto-generated method stub
		
	}
	CyNode cyNode;
	public void setCyNode(CyNode c) 	{		cyNode = c; 	}
	public CyNode getCyNode() 			{ 		return cyNode; 	}

}
