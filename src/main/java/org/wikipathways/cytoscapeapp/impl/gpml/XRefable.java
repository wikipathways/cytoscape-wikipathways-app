package org.wikipathways.cytoscapeapp.impl.gpml;



abstract public class XRefable extends AttributeMap
{
	public XRefable()
	{
		this("", 0,"","","",0);
	}
	public XRefable(String inName)
	{
		this(inName, 0,"","","",0);
	}
	public XRefable(AttributeMap other)
	{
		this();
		if (other != null)
		for(String s: other.keySet())
			put(s,other.get(s));

		}
	public void copyAttributesToProperties()
	{
		String s;
		s = get("Name"); 		if (s != null) setName(s);
		int i = getInteger("GraphId");		if (i > 0) setGraphId(i);
		s = get("Database"); 	if (s != null) setDatabase(s);
		s = get("ID"); 			if (s != null) setDbid(s);
		s = get("Type"); 		if (s != null) setType(s);
		s = get("GroupRef"); 	if (s != null) setGroupRef(getInteger("GroupRef"));
	}
	public void copyPropertiesToAttributes()
	{
		put("Name", getName());
		putInteger("GraphId", getGraphId());
		put("Database", getDatabase());
		put("ID", getDbid());
		put("Type", getType());
		putInteger("GroupRef", getGroupRef());
	}
	public XRefable(String inName, int inId, String inDb, String inDbid, String inType, int inGroupRef)
	{
		setName(inName);
		setGraphId(inId);
		setDatabase(inDb);
		setDbid(inDbid);
		setType(inType);
		setGroupRef(inGroupRef);
	}
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

	protected Double valuePropety = new Double(0.);
	public Double getValue()  { return valuePropety;}
	public void setValue(Double s)  { valuePropety = s;}
	
	protected String name = new String();		// HGNC
	public String getName()  { return name;}
	public void setName(String s)  { name = s;}
	
	protected Integer graphid = new Integer(0);
	public int getGraphId()  { return graphid;}
	public void setGraphId(int s)  { graphid = s;}
	public void setId(int s)  { setGraphId(s);}

	Integer groupRef = new Integer(0);
	public int getGroupRef()  { return groupRef;}
	public void setGroupRef(int s)  { groupRef = s;}

	protected String database = "";
	public String getDatabase()  { return database;}	
	public void setDatabase(String s)  { database = s;}

	protected String dbid = "";
	public String getDbid()  { return dbid;}
	public void setDbid(String s)  { dbid = s;}

	protected String type = "";
	public String getType()  { return type;}
	public void setType(String s)  { type = s;}

	public String toString()	{ return getName() + " (" + getGraphId() + ")"; }
}
