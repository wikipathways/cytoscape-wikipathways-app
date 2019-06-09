package org.wikipathways.cytoscapeapp.impl.gpml;

public class CommentRecord implements GPMLRecord {

	String source = new String();
	String text = new String();

	public String getSource()  { return source;}
	public void setSource(String s)  { source = s;}

	public String getText()  { return text;}
	public void setText(String s)  { text = s;}

	public CommentRecord(String src, String txt)
	{
		source = src;
		text = txt;
	}
	
	@Override
	public void getInfo(String mimetype, String a, String b) {
		
		System.out.println("CommentRecord.getInfo: " + a + " " + b);

	}
	public String toGPML() {
		return "<Comment Source=\"" + getSource() + "\">" + getText() + "</Comment>\n";
	}

}
