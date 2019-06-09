package org.wikipathways.cytoscapeapp.impl.gpml;

import java.util.List;

public class AttributeValue {

	    private String attribute = "";
	    private String value = "";

	    public AttributeValue()  					{ 	}
	    public AttributeValue(String rawString)  	{ 	
	    	String[] duo = rawString.split(":");
	    	if (duo.length == 2) { setAttribute(duo[0].trim()); setValue(duo[1].trim());	}}
	    
	    public AttributeValue(String a, String v)  	{ setAttribute(a); setValue(v);	}
	    public String getAttribute() 				{	        return attribute;	    }
	    public void setAttribute(String s) 			{	    	attribute = s;	    }

	    public String getValue() 					{	        return value;	    }
	    public void setValue(String s) 				{	    	value = s;	    }
		public String makeString()					{			return getAttribute() + ": " + getValue()  + "; ";		}
		public static List<AttributeValue> parseList(String string)
		{
			System.err.println(" TODO Auto-generated method stub");
			return null;
		}
		@Override public String toString()	{ return getAttribute() + ": " + getValue();	}

}
