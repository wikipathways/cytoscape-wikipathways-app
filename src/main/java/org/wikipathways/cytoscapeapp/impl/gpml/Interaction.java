package org.wikipathways.cytoscapeapp.impl.gpml;



import java.util.List;

import org.cytoscape.model.CyEdge;


public class Interaction  implements Comparable<Interaction>  //extends Edge
{
	/**
	 * 		Interaction is the biological wrapper around Edge
	 */
	private static final long serialVersionUID = 1L;
//	GeneListRecord geneListRecord;
//	protected XRefable source;
//	protected XRefable target;

	private String interactionType = new String();		
	public void setInteractionType(String s) 	 	{ interactionType = s;}
	public  String getInterType()  					{ return interactionType;}
private Edge edge;
//	private SimpleStringProperty arrowType = new SimpleStringProperty();		
//	public StringProperty  arrowTypeProperty()  { return arrowType;}
//	public void setArrowType(String s) 	 	{ arrowType.set(s);}
//	public  String getArrowType()  					{ return arrowType.get();}

	public void dump()	{ System.out.println( toString());	}		//get("GraphId") +
	public Interaction(Model inModel)
	{
		edge = new Edge(inModel);
//		if (!inModel.containsEdge(this))
//			inModel.addEdge(this);
		interactionType ="arrow";
	}
	
	//------------------------------------------------------------------------------------------
	public Interaction(AttributeMap attr, Model inModel, List<GPMLPoint> pts)		//, List<Anchor> anchors
	{
		edge = new Edge(this, attr, inModel, pts); //, List<Anchor> anchors
		interactionType = attr.get("ArrowHead");
		GPMLPoint.setInteraction(pts, this);
	}

//	public Interaction(Model inModel, DataNode start, DataNode end, AttributeMap attr) 		//, List<GPMLPoint> pts, List<Anchor> anchors
//	{
//		edge = new Edge(this, inModel, start, end,  attr);	
//     }
	String getName() { return edge.getName();}
 //------------------------------------------------------------------------------------------
	public int compareTo(Interaction other)
	{
		return edge.getName().compareToIgnoreCase(other.getName());
	}
//	
//	public void rebind(String sourceId) {
//		if (!isWellConnected()) return; 
//		setSource(getStartNode().getName());
//		setTarget(getEndNode().getName());
//		setNameFromState();
//		model.getController().getTreeTableView().addBranch(this, sourceId);
////		repostionAnchors();
//	}
//------------------------------------------------------------------------------------------
	@Override public String toString()
	{
		String name =  getName();
		if (StringUtil.isEmpty(name)) name = "-"; 
		String id =  edge.getId();
//		String str = (edgeLine == null) ? "X" : edgeLine.toString();
		return name + " [" + id + "] " + " " + getInterType();
	}

   public String toGPML()
   {
		StringBuffer b = new StringBuffer(String.format("<Interaction GraphId=\"%s\" >\n", edge.getSafe("GraphId")));
		b.append("<Graphics ");
		b.append(edge.getAttributes().attributeList(new String[]{"ConnectorType", "ZOrder","LineStyle","LineThickness"}));
		b.append (" >\n");
		
		b.append (edge.getPointsStr());
		b.append (edge.getAnchorsStr());
		b.append("</Graphics>\n");
		String db = edge.get("database");
		String dbid =  edge.get("dbid");
		if (db != null && dbid != null)
			b.append(String.format("<Xref Database=\"%s\" ID=\"%s\" />\n", db, dbid));
		b.append("</Interaction>\n");
		return b.toString();
	}
   
	public void toCX(CXObject cx)   	{	cx.addEdge(this);   }
	public MIM getInteractionType() {	return MIM.lookup(getInterType());	}

	public boolean isWellConnected() {
		return !(StringUtil.isEmpty(edge.getSourceid()) || StringUtil.isEmpty(edge.getTargetid()));
	}
	public void setNameFromState() {
		String arrow = edge.get("ArrowHead");
		if (arrow == null) arrow = "arrow";
		else arrow = arrow.toLowerCase();
		setInteractionType(arrow);
		if ("arrow".equals(arrow)) 				arrow = "->";
		else if (arrow.contains("none")) 		arrow = "--";
		else if (arrow.contains("tbar")) 		arrow = "-|";
		else if (arrow.contains("inhibit")) 	arrow = "-|";
		else if (arrow.contains("activ")) 		arrow = "->";
		else if (arrow.contains("conver")) 		arrow = ">>";
		else if (arrow.contains("catal")) 		arrow = "-O";
		else if (arrow.contains("bind")) 		arrow = "-{";
		else if (arrow.contains("stimu")) 		arrow = "+>";
		else if (arrow.contains("modif")) 		arrow = "~>";
		else if (arrow.contains("cleav")) 		arrow = "-\\";
		else if (arrow.contains("coval")) 		arrow = "::";
		else if (arrow.contains("transcrip")) 	arrow = "-#";
		else if (arrow.contains("translat")) 	arrow = "-X";
		else if (arrow.contains("gap")) 		arrow = " >";
		else if (arrow.contains("reg")) 		arrow = "-R";

		int target = edge.getInteger("targetid");
		String	targetName = ( target > 0) ? edge.getModel().getNodeName(target) :  "??";
		edge.getAttributes().setName("--" + arrow + " " + targetName);
	}
	CyEdge cyEdge;
	public void setCyEdge(CyEdge e) {		cyEdge = e; }
	public CyEdge getCyEdge() 		{		return cyEdge;	}
	public boolean touches(String nodeId) {	return edge.touches(nodeId);	}
	public Edge getEdge() {		return edge;	}
	public AttributeMap getEdgeAttributes() {		return edge.getAttributes();	}
	
}
