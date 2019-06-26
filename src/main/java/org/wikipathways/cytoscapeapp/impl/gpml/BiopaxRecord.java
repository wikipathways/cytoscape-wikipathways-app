package org.wikipathways.cytoscapeapp.impl.gpml;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.NamedNodeMap;

@SuppressWarnings("serial")
public class BiopaxRecord extends HashMap<String, String> {

	private String rdfid = new String();
	public String getRdfid()  { return rdfid;}
	public void setRdfid(String s)  { rdfid = s;}
	
	private String id = new String();
	public String getId()  { return id;}
	public void setId(String s)  { id = s;}
	
	private String db = new String();
	public String getDb()  { return db;}
	public void setDb(String s)  { db = s;}
	
	private String title = new String();
	public String getTitle()  { return title;}
	public void setTitle(String s)  { title = s;}
	
	private String source = new String();
	public String getSource()  { return source;}
	public void setSource(String s)  { source = s;}
	
	private String year = new String();
	public String getYear()  { return year;}
	public void setYear(String s)  { year = s;}
	
	private List<String> authors = new ArrayList<String>();
	public List<String> getAuthors()  { return authors;}
	public void addAuthors(String s)  { authors.add(s);}
	

	public String getFirstAuthor()  {
		if (authors.isEmpty()) return "";
		String auths = authors.get(0);
		if (auths == null) return "";
		return auths.split(" ")[0];
	}
	
	
	public BiopaxRecord(org.w3c.dom.Node elem) {
		
//		for (int i=0; i<elem.getChildNodes().getLength(); i++)
//		{
//			org.w3c.dom.Node child = elem.getChildNodes().item(i);
//			String name = child.getNodeName();
////			System.out.println(name);
//			if ("bp:PublicationXref".equals(name))
//			{
				NamedNodeMap attrs = elem.getAttributes();
				setRdfid(attrs.getNamedItem("rdf:id").getNodeValue());
				for (int j=0; j<elem.getChildNodes().getLength(); j++)
				{
					org.w3c.dom.Node grandchild = elem.getChildNodes().item(j);
					if (grandchild == null) continue;
					org.w3c.dom.Node kid =  grandchild.getFirstChild();
					if (kid == null) continue;
					
					String subname = grandchild.getNodeName();
					if ("#text".equals(subname)) continue;
					if ("bp:ID".equals(subname))			id = kid.getNodeValue();
					else if ("bp:DB".equals(subname))		db = kid.getTextContent();
					else if ("bp:TITLE".equals(subname))	title = kid.getTextContent();
					else if ("bp:SOURCE".equals(subname))	source = kid.getTextContent();
					else if ("bp:YEAR".equals(subname))		year = kid.getTextContent();
					else if ("bp:AUTHORS".equals(subname))	authors.add(kid.getTextContent());
				}
//			}
//		}
	}
	static String pubHeaderCtrl = "<bp:PublicationXref xmlns:bp=\"http://www.biopax.org/release/biopax-level3.owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" rdf:id=\"%s\">\n";
	static String pubHeaderClose = "</bp:PublicationXref>\n";
	
	public String toString()
	{
		String firstAuthor =  getFirstAuthor(); 
		return db + " " +  id + ": " + firstAuthor + ", [" + year + ", " + source + ", " + title + "].";
	}
	public String toGPML()		// TODO --save original text so we can pass external fields thru
	{
		StringBuilder bldr = new StringBuilder();
		String s = String.format(pubHeaderCtrl, getRdfid());
		bldr.append(s);
		addTag(bldr, "bp:ID", id);
		addTag(bldr, "bp:DB", db);
		addTag(bldr, "bp:TITLE", title);
		addTag(bldr, "bp:SOURCE", source);
		addTag(bldr, "bp:YEAR", year);
		for (String auth : getAuthors())
			addTag(bldr, "bp:AUTHORS", auth);
		
		bldr.append(pubHeaderClose);
		return bldr.toString();
	}
	
	void addTag(StringBuilder bldr, String key, String val)
	{
		bldr.append("<").append(key).append(">");
		bldr.append(val);
		bldr.append("</").append(key).append(">\n");
	}
	

//
//<bp:ID >16374s430</bp:ID>
//<bp:DB >PubMed</bp:DB>
//<bp:TITLE >Renin increases mesangial cell transforming growth factor-beta1 and matrix proteins through receptor-mediated, angiotensin II-independent mechanisms.</bp:TITLE>
//<bp:SOURCE >Kidney Int</bp:SOURCE>
//<bp:YEAR >2006</bp:YEAR>
//<bp:AUTHORS >Huang Y</bp:AUTHORS>
//<bp:AUTHORS >Wongamorntham S</bp:AUTHORS>
//<bp:AUTHORS >Kasting J</bp:AUTHORS>
//<bp:AUTHORS >McQuillan D</bp:AUTHORS>
//<bp:AUTHORS >Owens RT</bp:AUTHORS>
//<bp:AUTHORS >Yu L</bp:AUTHORS>
//<bp:AUTHORS >Noble NA</bp:AUTHORS>
//<bp:AUTHORS >Border W</bp:AUTHORS>



}
