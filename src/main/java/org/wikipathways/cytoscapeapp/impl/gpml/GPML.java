package org.wikipathways.cytoscapeapp.impl.gpml;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.pathvisio.core.model.Pathway;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.wikipathways.cytoscapeapp.impl.WPManager;


public class GPML {

	private Controller controller;
	private Model model;
	private Pathway pathway;	
	private CyNetwork network;
	
	public GPML(WPManager mgr, Pathway path, CyNetwork net)
	{
		this();
		pathway = path;
		network = net;
	}
	public GPML() {
		model = new Model();
		controller = new Controller(model);
	}
//	private Controller getController() { 	return model.getController();	}
	//----------------------------------------------------------------------------
//----------------------------------------------------------------------------
	public void read(String s)
	{
		try {
			Document doc = FileUtil.convertStringToDocument(s);
			read(doc);
		} 
		catch (Exception e) {	e.printStackTrace();	}
	}
	public void read(org.w3c.dom.Document doc)
	{
//		if (doc != null) return;
		model.clearComments();
		NodeList nodes = doc.getElementsByTagName("Pathway");
		System.out.println(nodes.getLength());
		for (int i=0; i<nodes.getLength(); i++)
		{
			org.w3c.dom.Node domNode = nodes.item(i);
			System.out.println(domNode.toString());
			NamedNodeMap nodemap = domNode.getAttributes();
			String key = "", val = "";
			for (int j=0; j<nodemap.getLength(); j++)
			{
				org.w3c.dom.Node grandchild = nodemap.item(j);
				if (grandchild == null) continue;
				key = grandchild.getNodeName();
				val = grandchild.getNodeValue();
				if ("Name".equals(key) || "Organism".equals(key))
					model.addComment(key, val);
				if ("Name".equals(key)) 
					model.setTitle(val);
				if ("Organism".equals(key)) 
					model.setSpecies(Species.lookup(val));
				System.out.println(key + ": " + val);
			}
		
		}
//		Label labl = new Label(model.getCommentsStr());
//		controller.addExternalNode(labl);
//		labl.setLayoutX(10);
//		labl.setLayoutY(10);
		
		parseDataNodes(doc.getElementsByTagName("DataNode"));
		parseLabels(doc.getElementsByTagName("Label"));
		parseBiopax(doc.getElementsByTagName("Biopax"));
		parseShapes(doc.getElementsByTagName("Shape"), "Shape");
		parseShapes(doc.getElementsByTagName("GraphicalLine"),"GraphicalLine");
		parseStateNodes(doc.getElementsByTagName("State"));
		parseGroups(doc.getElementsByTagName("Group"));
		parseInteractions(doc.getElementsByTagName("Interaction"));
//		List<Node> sorted = getController().getPasteboard().getChildren().stream()
//        	.sorted(Comparator.comparing(null).reversed())
//        	.peek(System.out::println)
//        	.collect(Collectors.toCollection(()->FXCollections.observableArrayList()));
	}
//	
//	Comparator<DataNode> c = new Comparator<DataNode>()
//	{
//		@Override public int compare(DataNode o1, DataNode o2) {
//				return ((DataNode)o2).compareTo((DataNode)o1);
//			return 0;
//		}
//	};
//	
	private void parseDataNodes(NodeList nodes) {
		for (int i=0; i<nodes.getLength(); i++)
		{
			org.w3c.dom.Node child = nodes.item(i);
			DataNode node = parseGPMLDataNode(child, model);
			addDataNode(node);
			System.out.println("adding: " + node + "\n");
		}
	}
	
	private void addDataNode(DataNode node) {
//		controller.addDataNode(node);

		
	}

	private void parseStateNodes(NodeList nodes) {
		for (int i=0; i<nodes.getLength(); i++)
		{
			org.w3c.dom.Node child = nodes.item(i);
			DataNodeState sstate = parseGPMLDataNodeState(child, model);
			addStateNode(sstate);
			System.out.println("adding: " + child + "\n");
		
		}
	}

	private void parseInteractions(NodeList edges) 
	{
		for (int i=0; i<edges.getLength(); i++)
		{
			org.w3c.dom.Node xml = edges.item(i);
			Interaction edge = parseGPMLInteraction(xml, model);
			controller.addInteraction(edge);
		}
	}
	boolean verbose = true;
	private void parseShapes(NodeList shapes, final String shapeType) {
		for (int i=0; i<shapes.getLength(); i++)
		{
			org.w3c.dom.Node child = shapes.item(i);
			if (verbose)
				System.out.println("");
			DataNode node = parseGPMLDataNode(child, model);
			String s= shapeType;
			if ("Shape".equals(shapeType))
				s = node.get("ShapeType");
			if (node != null)
				addShapeNode(node,s);
		}
	}
	
	// TODO should be override of DataNode
	public DataNodeState parseGPMLDataNodeState(org.w3c.dom.Node datanode, Model m) {
		DataNodeState node = new DataNodeState(m);
		node.add(datanode.getAttributes());
		
		NodeList elems = datanode.getChildNodes();
		for (int i=0; i<elems.getLength(); i++)
		{
			org.w3c.dom.Node child = elems.item(i);
			String name = child.getNodeName();
			if ("#text".equals(name)) continue;
			if ("BiopaxRef".equals(name))
			{
				String ref = child.getTextContent();
				if (!StringUtil.isEmpty(ref))
					node.put("BiopaxRef", ref);
			}
			if ("Comment".equals(name))
			{
				String ref = child.getTextContent();
				if (!StringUtil.isEmpty(ref))
					node.put("Comment", ref);
			}
			if ("Attribute".equals(name))
			{
				String key = null;
				String val = null;
				for (int j=0; j<child.getAttributes().getLength(); j++)
				{
					org.w3c.dom.Node grandchild = child.getAttributes().item(j);
					if ("Key".equals(grandchild.getNodeName()))
						key = grandchild.getTextContent();
					else 
						val = grandchild.getTextContent();
					System.out.println(grandchild.getNodeName() + " " + key + val );
				}
				if (key != null && val != null)
					 node.put(key, val);
					
			}
			if ("Graphics".equals(name))
			{
				node.add(child.getAttributes());
			}
		node.add(child.getAttributes());			// NOTE: multiple Attribute elements will get overridden!
//			System.out.println(name);
//			node.copyAttributesToProperties();
		}
		String type = node.get("Type");
		if (isFixed(type))  node.put("Resizable", "false");
		node.copyAttributesToProperties();
		return node;
	}

	// **-------------------------------------------------------------------------------
	/*
	 * 	convert an org.w3c.dom.Node to a local MNode.  
	 * 
	 */
	public DataNode parseGPMLDataNode(org.w3c.dom.Node datanode, Model m) {
		DataNode node = new DataNode(m);
//		node.put("Layer", activeLayer);
		node.add(datanode.getAttributes());
		String inId = node.get("GraphId");
		if (!StringUtil.isNumber(inId))
			node.putInteger("GraphId", m.gensym(inId));
		NodeList elems = datanode.getChildNodes();
		for (int i=0; i<elems.getLength(); i++)
		{
			org.w3c.dom.Node child = elems.item(i);
			String name = child.getNodeName();
			if ("#text".equals(name)) continue;
			if ("BiopaxRef".equals(name))
			{
				String ref = child.getTextContent();
				if (!StringUtil.isEmpty(ref))
					node.put("BiopaxRef", ref);
			}
			if ("Comment".equals(name))
			{
				String ref = child.getTextContent();
				if (!StringUtil.isEmpty(ref))
					node.put("Comment", ref);
			}
			if ("Attribute".equals(name))
			{
				String key = null;
				String val = null;
				for (int j=0; j<child.getAttributes().getLength(); j++)
				{
					org.w3c.dom.Node grandchild = child.getAttributes().item(j);
					if ("Key".equals(grandchild.getNodeName()))
						key = grandchild.getTextContent();
					else 
						val = grandchild.getTextContent();
					System.out.println(grandchild.getNodeName() + " " + key + ": " + val );
				}
				if (key != null && val != null)
					 node.put(key, val);
			}
			if ("Graphics".equals(name))
			{
				node.add(child.getAttributes());
				NodeList pts = child.getChildNodes();
				List<GPMLPoint> points = new ArrayList<GPMLPoint>();
				for (int j=0; j<pts.getLength(); j++)
				{
					org.w3c.dom.Node pt = pts.item(j);
					if ("Point".equals(pt.getNodeName()))
					{
						GPMLPoint gpt = new GPMLPoint(pt, m);
						points.add(gpt);
						ArrowType type = gpt.getArrowType();
						if (type != null)
							node.put("ArrowHead", type.toString());
					}
				}
				if (!points.isEmpty())
					node.addPointArray(points);
			}
			node.add(child.getAttributes());			// NOTE: multiple Attribute elements will get overridden!
//			System.out.println(name);
//			node.copyAttributesToProperties();
		}
		String type = node.get("Type");
		if (isFixed(type))  node.put("Resizable", "false");
		node.copyAttributesToProperties();
		return node;
	}
	
	boolean isFixed(String s)
	{
		if (s == null) return false;
		return "GeneProduct Metabolite Protein RNA Pathway".contains(s);
	}
		//----------------------------------------------------------------------------
	public Interaction parseGPMLInteraction(org.w3c.dom.Node edgeML, Model m) {
	try
	{
		List<GPMLPoint> points = new ArrayList<GPMLPoint>();
		List<Anchor> anchors = new ArrayList<Anchor>();
		AttributeMap attrib = new AttributeMap(edgeML.getAttributes());
		NodeList elems = edgeML.getChildNodes();
		int graphId = -1;
		String val = attrib.get("GraphId");
		if (StringUtil.isNumber(val))
			graphId = StringUtil.toInteger(val);
		else 
		{
			DataNode nod = model.find(val);
			if (nod != null)
				graphId = nod.getId(); 
			graphId = m.gensym(val);
			attrib.putInteger("GraphId",graphId);
		}

		for (int i=0; i<elems.getLength(); i++)
		{
			org.w3c.dom.Node n = elems.item(i);
			String name = n.getNodeName();
			if ("Graphics".equals(name))
			{
				attrib.add(n.getAttributes());
				NodeList pts = n.getChildNodes();
				for (int j=0; j<pts.getLength(); j++)
				{
					org.w3c.dom.Node pt = pts.item(j);
					if ("Point".equals(pt.getNodeName()))
					{
						GPMLPoint gpt = new GPMLPoint(pt, m);
						points.add(gpt);
						String key = j > 0 ? "targetid" : "sourceid";
						attrib.putInteger(key, gpt.getGraphRef());
						ArrowType type = gpt.getArrowType();
						if (type != null)
							attrib.put("ArrowHead", type.toString());
					}
					if ("Anchor".equals(pt.getNodeName()))
					{
						
						Anchor anchor = new Anchor(pt, m, graphId);
						String oldid = anchor.get("GraphId");
						int id;
						if (StringUtil.isNumber(oldid))
							id = StringUtil.toInteger(oldid);
						else id = m.gensym(oldid);
						anchor.putInteger("GraphId", id);
						anchor.setId(id);
						anchors.add(anchor);
//						getController().addAnchor(anchor);
					}
				}
			}
			else if ("Xref".equals(name))	// suck the Xref element into our attributes
				attrib.add(n.getAttributes());
			else if ("BiopaxRef".equals(name))	// suck the BiopaxRef element into our attributes
				attrib.put("BiopaxRef", n.getTextContent());
		}
		//post parsing
		int z = points.size();
		DataNode endNode = null, startNode = null;
		if (z > 1)
		{
			GPMLPoint startPt = points.get(0);
			int strtId = startPt.getGraphRef();
			startNode = m.find(strtId);
			attrib.putInteger("sourceid", strtId);
		
			GPMLPoint lastPt = points.get(z-1);
			int ending = lastPt.getGraphRef();
			endNode = m.find(ending);
			attrib.putInteger("targetid", ending);
			ArrowType arwType = lastPt.getArrowType();
			if (arwType != null)
				attrib.put("ArrowHead", arwType.toString());
		}
		else
		{
			System.err.println("z = " + z);
			return null;
		}
		Interaction interaction = new Interaction(attrib, m, points, anchors);
//		interaction.put("Layer", "Content");
		interaction.add(edgeML.getAttributes());
		interaction.putInteger("GraphId", graphId);
		interaction.copyAttributesToProperties();		// copy attributes into properties for tree table editing
		interaction.setStartNode(startNode);
		interaction.setEndNode(endNode);
		
		return interaction;
	}
	catch(Exception e)
	{
		e.printStackTrace();
		return null;
	}
	}	
	
	public DataNode parseGPMLLabel(org.w3c.dom.Node labelNode) {
		DataNode label = new DataNode(model);
		label.setType("Label");
		NodeList elems = labelNode.getChildNodes();
		label.add(labelNode.getAttributes());
		String idval = label.get("GraphId");
		int graphId = -1;
		if (StringUtil.isNumber(idval))
			graphId = StringUtil.toInteger(idval);
		else 
		{
			DataNode nod = model.find(idval);
			if (nod != null)
				graphId = nod.getId(); 
			graphId = model.gensym(idval);
			label.putInteger("GraphId",graphId);
			label.setId(graphId);
		}
		String txt = label.get("TextLabel");
		if (txt == null) txt = "Undefined";
		label.setName(txt);
		String name = "";
		for (int i=0; i<elems.getLength(); i++)
		{
			org.w3c.dom.Node child = elems.item(i);
			name = child.getNodeName();
			if (name != null && name.equals("Attribute")) 
			{
				NamedNodeMap attrs = child.getAttributes();
				String key = "", val = "";
				for (int j=0; j<attrs.getLength(); j++)
				{
					org.w3c.dom.Node grandchild = attrs.item(j);
					String grandname = grandchild.getNodeName();
					{
						if ("Key".equals(grandname))	key = grandchild.getNodeValue();
						if ("Value".equals(grandname))	val = grandchild.getNodeValue();
					}
				}
				if (StringUtil.hasText(key) && StringUtil.hasText(val))
				{
					if (key.startsWith("org.pathvisio."))
						key = key.substring(14);
//					label.setText(key + ":\n" + val);
//					label.setTextFill(Color.CHOCOLATE);
					label.put("TextLabel", key + ":\n" + val);
					
				}
			}
			if ("Graphics".equals(name))
				label.add(child.getAttributes());
		}
		if (txt.length() > 10)
		{
			double wid = label.getDouble("Width");
			label.putDouble("Width", wid + 100);
		}
//		attrMap.put("ShapeType", "Label");
		String shapeType = label.get("ShapeType");
		if (shapeType == null)
			label.put("ShapeType", "None");
				
		return label;
	}
	//----------------------------------------------------------------------------

	private void parseBiopax(NodeList elements) {
		for (int i=0; i<elements.getLength(); i++)
		{
			org.w3c.dom.Node child = elements.item(i);
			String name = child.getNodeName();
			System.out.println(name);
			
			for (int j=0; j<child.getChildNodes().getLength(); j++)
			{
				org.w3c.dom.Node gchild = child.getChildNodes().item(j);
				String jname = gchild.getNodeName();
//				System.out.println(name);
				if ("bp:openControlledVocabulary".equals(jname))
				{
//					VocabRecord ref = new VocabRecord(gchild);
//					model.addRef(ref);
					System.out.println("TODO: VocabRecord");					//TODO
				}
				if ("bp:PublicationXref".equals(jname))
				{
					BiopaxRecord ref = new BiopaxRecord(gchild);
					model.addRef(ref);
					System.out.println(ref);					//TODO
				}
			}
		}
	}
	//----------------------------------------------------------------------------
	private void parseLabels(NodeList elements) {
		for (int i=0; i<elements.getLength(); i++)
		{
			org.w3c.dom.Node child = elements.item(i);
			String name = child.getNodeName();
			System.out.println(name);
			DataNode label = parseGPMLLabel(child);
			if (label != null)
			{
				addLabel(label);
//				label.getStack().toBack();
			}
		}
	}

	//----------------------------------------------------------------------------
	
	private void parseGroups(NodeList elements) {			//TODO
		for (int i=0; i<elements.getLength(); i++)
		{
//if (i >= 0) continue;			//SKIP
			org.w3c.dom.Node child = elements.item(i);
			NamedNodeMap attrs = child.getAttributes();
			String name = child.getNodeName();
			System.out.println(name);
			if ("#text".equals(name)) continue;
			if ("Group".equals(name))
			{
				try 
				{
					controller.addGroup(parseGPMLGroup(child, attrs));	
				}
				catch (Exception e) {
					System.err.println("parseGroups");
				}
			
			}
		}
	}
	
	DataNodeGroup parseGPMLGroup(org.w3c.dom.Node child, NamedNodeMap attrs)
	{
		String groupId = "";
		if (attrs.getNamedItem("GroupId") != null)
			groupId = attrs.getNamedItem("GroupId").getNodeValue();
		String style = "";
		if (attrs.getNamedItem("Style") != null)
			style = attrs.getNamedItem("Style").getNodeValue();
		String graphId = "NONE";
		if (attrs.getNamedItem("GraphId") != null)
		{
			graphId = attrs.getNamedItem("GraphId").getNodeValue();
		}
		AttributeMap attrMap = new AttributeMap();
		String shapeType = "Complex".equals(style) ? "ComplexComponent" : "GroupComponent";
		attrMap.putAll("GraphId", graphId, "GroupId", groupId, "ShapeType", shapeType, "Style", style, "Fill", "F7F7F0", "LineStyle", "Broken");
		DataNodeGroup newGroup = new DataNodeGroup(attrMap,model, true);

		newGroup.copyAttributesToProperties();
//		newGroup.setName(newGroup.get("Style") + " [" + newGroup.get("GroupId") + "]");
//		System.out.println("Making group with name = " + newGroup.getName());
		return newGroup;
	}
	

//	
	//----------------------------------------------------------------------------
//	http://fxexperience.com/2011/12/styling-fx-buttons-with-css/
	// TODO -- CYTOSCAPE PROPERTIES	
		public static String asFxml(String gpmlTag) {
		
		if (gpmlTag == null) return null;
		if ("Color".equals(gpmlTag))		return "-fx-border-color";
		if ("FillColor".equals(gpmlTag))	return "-fx-background-color";
		if ("LineThickness".equals(gpmlTag)) return "-fx-stroke-width";
		if ("Opacity".equals(gpmlTag))		return "-fx-opacity";
		if ("FontSize".equals(gpmlTag))		return "-fx-font-size";
		if ("Valign".equals(gpmlTag))		return "-fx-row-valignment";
		
		return null;
	}
	
		//----------------------------------------------------------------------------
		//	moved from Controller


		public void addShapeNode(DataNode shapeNode, String shapeType) {
//		pasteboard.setActiveLayer("Background");	
			shapeNode.setType("Shape");
			shapeNode.put("Type", "Shape");
			shapeNode.put("ShapeType", shapeType);
			shapeNode.setName(shapeNode.get("ShapeType"));
//			VNode vnode = new VNode(shapeNode, pasteboard);
//			pasteboard.add(vnode);
			model.addResource(shapeNode);
			model.addShape(shapeNode);
//			vnode.setStyle("double-border");
			
		}
		public void addLabel(DataNode label) {
//			pasteboard.setActiveLayer("Content");	
			label.put("Layer", "Background");
			label.setType("Label");
			label.put("Type", "Label");
//			VNode vnode = new VNode(label, pasteboard);
//			vnode.setMouseTransparent(false);
//			pasteboard.add(vnode);
			model.addResource(label);
			model.addLabel(label);
		}


		public void addStateNode(DataNodeState statenode) {
//			pasteboard.setActiveLayer("Content");	
			Integer graphRef = statenode.getInteger("GraphRef");
			DataNode host = model.findDataNode(graphRef);
			if (host != null)
			{
				host.addState(statenode);
				model.addState(graphRef, statenode);
			}
			
		}
	
		//----------------------------------------------------------------------------
		//----------------------------------------------------------------------------
	// UNUSED ??
//	
//	public Shape shapeFromGPML(String gpmlStr,  AttributeMap attrMap, boolean addHandlers) {
//		String txt = gpmlStr.trim();
//		if (txt.startsWith("<DataNode "))
//		{
//			String attrs = txt.substring(10, txt.indexOf(">"));
//			attrMap.addGPML(attrs);
//			String graphics =  txt.substring(10 + txt.indexOf("<Graphics "), txt.indexOf("</Graphics>"));
//			String xref = txt.substring(10 + txt.indexOf(6 + "<Xref "), txt.indexOf("</Xref>"));
//			attrMap.addGPML(graphics);
//			attrMap.addGPML(xref);
//		}
//		String shapeType = attrMap.get("ShapeType");
//		Shape newShape = controller.getNodeFactory().getShapeFactory().makeNewShape(shapeType, attrMap, addHandlers); 
//		return newShape;
//	}

//	public static Edge createEdge(String txt, AttributeMap attrMap, Model model) 
//	{
//		String graphics =  txt.substring(10 + txt.indexOf("<Graphics "), txt.indexOf("</Graphics>"));
//		String xref = txt.substring(10 + txt.indexOf(6 + "<Xref "), txt.indexOf("</Xref>"));
//		attrMap.addGPMLEdgeInfo(graphics);
//		attrMap.addGPML(xref);
//		return new Edge(attrMap, model);
//	}

	//----------------------------------------------------------------------------
//	public Node[] makeTestItems() {
//		Node a,b,c,d,e,f,g,h,i,j,k, l;
//		ShapeFactory factory = controller.getNodeFactory().getShapeFactory();
//		AttributeMap attrMap = new AttributeMap();
//		
//		attrMap.putCircle(new Circle(150, 150, 60, Color.AQUA));
//		attrMap.put("TextLabel", "Primary");
//		Label label = factory.createLabel("Primary", Color.AQUA);
//		a = label;
//		Circle cir = (Circle) factory.makeNewShape(Tool.Circle, attrMap);
//		b = cir;
//		label.layoutXProperty().bind(cir.centerXProperty().subtract(label.widthProperty().divide(2.)));
//		label.layoutYProperty().bind(cir.centerYProperty().subtract(label.heightProperty().divide(2.)));
//		
//		attrMap = new AttributeMap();
//		attrMap.putCircle(new Circle(180, 450, 60, Color.AQUA));
//		attrMap.put("TextLabel", "Secondary");
//		c = factory.makeNewShape(Tool.Circle, attrMap);
//		attrMap = new AttributeMap();
//		attrMap.putCircle(new Circle(150, 300, 20, Color.BEIGE));
//		attrMap.put("TextLabel", "Tertiary");
//		d = factory.makeNewShape(Tool.Circle, attrMap);
//		attrMap = new AttributeMap();
//		attrMap.putRect(new Rectangle(250, 50, 30, 30));
//		e = factory.makeNewShape(Tool.Rectangle, attrMap);
//		attrMap = new AttributeMap();
//		attrMap.putRect(new Rectangle(250, 450, 30, 50));
//		f = factory.makeNewShape(Tool.Rectangle, attrMap);
//		
//		new Edge(b, c);
//		new Edge(d, b);
//		new Edge(f, b);
//		new Edge(f, c);
//		new Edge(e, f);
//		new Edge(e, d);
//		return new Node[] { a,b,c,d,e,f };
//	}


//		private void applyGraphicsNode(Label label, org.w3c.dom.Node child) {
//			NamedNodeMap attrs = child.getAttributes();
//			String name = "";
//			for (int i=0; i<attrs.getLength(); i++)
//			{
//				org.w3c.dom.Node item = attrs.item(i);
//				String val = item.getNodeValue();
//				double d = StringUtil.toDouble(val);
//				name = item.getNodeName();
//				
//				if ("CenterX".equals(name)) 		 label.setLayoutX(d);
//				else if ("CenterY".equals(name)) 	 label.setLayoutY(d);
//				else if ("Width".equals(name)) 		 {	label.maxWidth(d); label.prefWidth(d);}
//				else if ("Height".equals(name)) 	{	label.maxHeight(d); label.prefHeight(d);}
//				else if ("ZOrder".equals(name)) 	{}
////				else if ("Color".equals(name)) {	label.setBorder(Borders.coloredBorder(val));}
//				else if ("Color".equals(name)) 		label.setTextFill(Color.web(val));
//				else if ("FillColor".equals(name)) 	label.setBackground(Backgrounds.colored(val));
//				else if ("FontSize".equals(name)) 	{}
//				else if ("FontWeight".equals(name)) {}
//				else if ("Valign".equals(name)) 	{}
////				else if ("ShapeType".equals(name)) 	
////				{	if ("RoundedRectangle".equals(val)) {}		}
//			}
//			double w = StringUtil.toDouble(attrs.getNamedItem("Width").getNodeValue());
//			double h = StringUtil.toDouble(attrs.getNamedItem("Height").getNodeValue());
////			label.getWidth();
////			double h = label.getWidth();
//			label.setLayoutX(label.getLayoutX() - w / 2.);
//			label.setLayoutY(label.getLayoutY() - h / 2.);
//		}
	//	
//		private void put(AttributeMap attrMap, String key, NamedNodeMap map)
//		{
//			org.w3c.dom.Node  named = map.getNamedItem(key);
//			if (named != null)
//				attrMap.put(named.getNodeName(), named.getNodeValue());
//		}
//		public static GeneSetRecord readGeneList(File file, Species inSpecies)
//		{
//			org.w3c.dom.Document doc = FileUtil.openXML(file);
//			if (doc == null) return null;
//			List<Gene> list = FXCollections.observableArrayList();
//			GeneSetRecord record = new GeneSetRecord(file.getName());
//			record.setSpecies(inSpecies.common());
//			record.setName(file.getName());
//			
//			NodeList nodes = doc.getElementsByTagName("DataNode");
//			int len = nodes.getLength();
//			for (int i=0; i<len; i++)
//			{
//				org.w3c.dom.Node domNode = nodes.item(i);
//				NamedNodeMap nodemap = domNode.getAttributes();
//				org.w3c.dom.Node type = nodemap.getNamedItem("Type");
//				String val = type == null ? "" : type.getNodeValue();
//				org.w3c.dom.Node id = nodemap.getNamedItem("GraphId");
//				String graphid = id == null ? "" : id.getNodeValue();
////				if ("GeneProduct".equals(val) || "Protein".equals(val))
////				{
////					String textLabel = nodemap.getNamedItem("TextLabel").getNodeValue();
////					Gene existing = Model.findInList(list, textLabel);
////					if (existing == null)
////						list.add(new Gene(record, textLabel, graphid));
////					System.out.println(textLabel + " " + ((existing == null) ? "unique" : "found"));
////				}
//			}
//			record.setGeneSet(list);
//			return record;
//		}
			
}
