package org.wikipathways.cytoscapeapp.impl.gpml;



import java.util.List;

import org.wikipathways.cytoscapeapp.impl.gpml.RelPosition.Pos;


public class Interaction extends Edge implements Comparable<Interaction>
{
	/**
	 * 		Interaction is the biological wrapper around Edge
	 */
	private static final long serialVersionUID = 1L;
//	GeneListRecord geneListRecord;
	protected XRefable source;
	protected XRefable target;

	private String interactionType = new String();		
	public void setInteractionType(String s) 	 	{ interactionType = s;}
	public  String getInterType()  					{ return interactionType;}

//	private SimpleStringProperty arrowType = new SimpleStringProperty();		
//	public StringProperty  arrowTypeProperty()  { return arrowType;}
//	public void setArrowType(String s) 	 	{ arrowType.set(s);}
//	public  String getArrowType()  					{ return arrowType.get();}

	public void dump()	{ System.out.println( toString());	}		//get("GraphId") +
	public Interaction(Model inModel)
	{
		super(inModel);
//		if (!inModel.containsEdge(this))
//			inModel.addEdge(this);
		interactionType ="arrow";
	}
	
	//------------------------------------------------------------------------------------------
	public Interaction(AttributeMap attr, Model inModel, List<GPMLPoint> pts, List<Anchor> anchors)
	{
		super(attr, inModel, pts, anchors);
		interactionType = attr.get("ArrowHead");
		GPMLPoint.setInteraction(pts, this);
	}

	public Interaction(Model model, DataNode src, Pos srcPosition, 
			DataNode targ, RelPosition targPosition, ArrowType arrow, EdgeType edge) 
	{
		this(model,src,targ, null);
		setInteractionType(arrow.toString());
		put("EdgeType", edge.toString());
//		setArrowType(arrow.toString());
		putDouble("LineThickness", 1.5);
		
//		String newId = newEdgeId();
//		put("GraphId", newId);
//		setGraphId(newId);
////		GPMLPoint  startPoint = new GPMLPoint(src.getPortPosition(srcPosition));
//		startPoint.setRelPosition(srcPosition);
//		startPoint.setGraphRef(src.getGraphId());
//		setStartPoint(startPoint.getPoint());
//		startPoint.setPos(srcPosition);
		
//		GPMLPoint endPoint = new GPMLPoint(targ.getRelativePosition(targPosition));
//		endPoint.setRelX(targPosition.x());
//		endPoint.setRelY(targPosition.y());
//		setEndPoint(endPoint.getPoint());
//		endPoint.setGraphRef(targ.getGraphId());
//		endPoint.setPos(targPosition);
//		endPoint.setArrowType(arrow);
	}	

	public Interaction(Model inModel, DataNode start, DataNode end, AttributeMap attr) 		//, List<GPMLPoint> pts, List<Anchor> anchors
	{
    	super( inModel, start, end,  attr);	
     }
	
 //------------------------------------------------------------------------------------------
	public int compareTo(Interaction other)
	{
		return getName().compareToIgnoreCase(other.getName());
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
		int id =  getId();
//		String str = (edgeLine == null) ? "X" : edgeLine.toString();
		return name + " [" + id + "] " + " " + getInterType();
	}

   public String toGPML()
   {
		StringBuffer b = new StringBuffer(String.format("<Interaction GraphId=\"%s\" >\n", getSafe("GraphId")));
		b.append("<Graphics ");
		b.append(attributeList(new String[]{"ConnectorType", "ZOrder","LineStyle","LineThickness"}));
		b.append (" >\n");
		
		b.append (getPointsStr());
		b.append (getAnchorsStr());
		b.append("</Graphics>\n");
		String db = get("database");
		String dbid =  get("dbid");
		if (db != null && dbid != null)
			b.append(String.format("<Xref Database=\"%s\" ID=\"%s\" />\n", db, dbid));
		b.append("</Interaction>\n");
		return b.toString();
	}
   
	public void toCX(CXObject cx)   	{	cx.addEdge(this);   }
	public MIM getInteractionType() {	return MIM.lookup(getInterType());	}

	public boolean isWellConnected() {
		return getStartNode() != null && getEndNode() != null;
	}
	public void setNameFromState() {
		String arrow = get("ArrowHead");
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

		int target = getInteger("targetid");
		String	targetName = ( target > 0) ? getModel().getNodeName(target) :  "??";
		setName("--" + arrow + " " + targetName);
	}
	
}
