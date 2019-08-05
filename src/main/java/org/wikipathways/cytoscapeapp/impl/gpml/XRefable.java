package org.wikipathways.cytoscapeapp.impl.gpml;

public class XRefable extends AttributeMap
{
	public XRefable(String inName, String inId, String inDb, String inDbid, String inType, String inGroupRef)
	{
		setName(inName);
		setGraphId(inId);
		setDatabase(inDb);
		setDbid(inDbid);
		setType(inType);
		setGroupRef(inGroupRef);
	}
	public XRefable()
	{
		this("", "","","","","");
	}
	public XRefable(AttributeMap other)
	{
//		this();
		if (other != null)
		for(String s: other.keySet())
			put(s,other.get(s));

		}

	//--------------------------------------------------------
//	public void copyAttributesToProperties()
//	{
//		String s;
//		s = get("Name"); 		if (s != null) setName(s);
//		s = get("GraphId");		if (s != null) setGraphId(s);
//		s = get("Database"); 	if (s != null) setDatabase(s);
//		s = get("ID"); 			if (s != null) setDbid(s);
//		s = get("Type"); 		if (s != null) setType(s);
//		s = get("GroupRef"); 	if (s != null) setGroupRef(getInteger("GroupRef"));
//	}
//	public void copyPropertiesToAttributes()
//	{
//		put("Name", getName());
//		put("GraphId", getGraphId());
//		put("Database", getDatabase());
//		put("ID", getDbid());
//		put("Type", getType());
//		putInteger("GroupRef", getGroupRef());
//	}
//	public void setProperties() {
//		setName(get("TextLabel"));
//		setGraphId(get("GraphId"));
//		setDatabase(get("Database"));
//		setDbid(get("ID"));
//		setType(get("Type"));
//	}
	
	String[]  xrefattrs = {  "Database", "ID"};
	protected void buildXRefTag(StringBuilder bldr)
	{
		String attributes = attributeList(xrefattrs);
		if (StringUtil.hasText(attributes))
			bldr.append( "<Xref ").append(attributes).append( " />\n");
	}
	
	protected String attributeList(String[] strs)
	{
		StringBuilder bldr = new StringBuilder();
		for (String attr : strs)
		{
			String val = get(attr);
			if (val != null)
				bldr.append(attr + "=\"" + val + "\" ");
		}
		return bldr.toString();
	}

//	protected Double valuePropety = new Double(0.);
//	public Double getValue()  { return getDouble("Value");}
//	
//	public void setValue(Double s)  { valuePropety = s;}
//	
//	protected String name = new String();		// HGNC
	public String getName()  { return get("Name");}
	public void setName(String s)  { put("Name", s);}
	
	
//	protected String graphid = "-1";
	public String getGraphId()  { return get("GraphId");}
	public void setGraphId(String s)  {  put("GraphId", s); }
	public void setId(String s)  { setGraphId(s);}

//	String groupRef = new Integer(0);
	public String getGroupRef()  { return get("GroupRef");}
	public void setGroupRef(String s)  { put("GroupRef", s);}

//	protected String database = "";
	public String getDatabase()  { return get("Database");}	
	public void setDatabase(String s)  { put("Database", s);}

//	protected String dbid = "";
	public String getDbid()  { return get("DbId");}
	public void setDbid(String s)  { put("DbId", s);}

//	protected String type = "";
	public String getType()  { return get("Type");}
	public void setType(String s)  {put("Type", s);}

	public String toString()	{ return getName() + " (" + getGraphId() + ")"; }
}
