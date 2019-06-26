package org.wikipathways.cytoscapeapp.impl.gpml;

import java.util.List;

public class QuadValue {

	    private String attribute = new String();
	    private String value = new String();
	    private String source = new String();
	    private String target = new String();

	    public QuadValue()  					{ 	}
//	    public QuadValue(String rawString)  	{ 	
//	    	String[] duo = rawString.split(":");
//	    	if (duo.length == 2) { setAttribute(duo[0].trim()); setValue(duo[1].trim());	}}
	    
	    public QuadValue(String src, String a, String targ, String v)  	{ setSource(src); setAttribute(a); setTarget(targ); setValue(v);	}
	    public String getSource() 					{	        return source;	    }
	    public void setSource(String s) 			{	    	source = s;	    }
	
	    public String getTarget() 					{	        return target;	    }
	    public void setTarget(String s) 			{	    	target = s;	    }
	
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
		@Override public String toString()	{ return getSource() + ": " + getAttribute() + " | " + getTarget() + ": " + getValue();	}

}
