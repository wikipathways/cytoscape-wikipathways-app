package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.Paint;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StringUtil
{

	static public String inject(String full, String after, String until, String insert)
	{
		int idx1 = full.indexOf(after) + after.length();
		int idx2 = full.indexOf(until, idx1);
		return full.substring(0,idx1) + insert + full.substring(idx2);
	}
	static public String insertAt(String full, int index, String insert)
	{
		return full.substring(0,index) + insert + full.substring(index);
	}

	public static String chop(String string, int i) { 	return string.substring(0,string.length()-i);	}
	static public String chopLast(String s)			{	return chop(s,1);	}
	static public String chopLast2(String s)		{	return chop(s,2);	}
	static public String chopExtension(String in)	{	int idx = in.lastIndexOf("."); return idx < 0 ? in : in.substring(0,idx);	}
//	static public ObservableList<String> lines(String in)
//	{
//		ObservableList<String> strs = FXCollections.observableArrayList();
//		return strs;
	// }

	public static void launchURL(String urlString)
	{
        try {
            java.awt.Desktop.getDesktop().browse(new URI(urlString));
        }   catch (Exception e) {}  // ignore
	}
	
	public static String callURL(String urlString, boolean addNL)
	{
		StringBuilder buffer = new StringBuilder();
		try
		{
			URL url = new URL(urlString);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String strTemp = "";
			String eol = addNL ? "\n" : "";
			while (null != (strTemp = br.readLine()))			// WARNING: DON"T READ ON MAIN THREAD
				buffer.append(strTemp).append(eol);  //
		} catch (Exception ex)
		{
//			ex.printStackTrace();
			System.err.println(ex.getMessage());
			System.out.println(urlString + " failed to load");
		}
		return buffer.toString();
	}
	
	public enum TYPES { EMPTY, DATE, DOUBLE, STRING, COLOR };

	public static boolean isEmpty(TYPES t)  { return t == TYPES.EMPTY; }
	public static boolean isString(TYPES t)  { return t == TYPES.STRING; }
	public static boolean isNumber(TYPES t)  { return t == TYPES.DOUBLE; }
	public static boolean iColor(TYPES t)  { return t == TYPES.COLOR; }
	
	static public TYPES inferType(String s)
	{
		if (s.isEmpty()) return TYPES.EMPTY;
		if (isColor(s)) return TYPES.COLOR;
		if (isDate(s)) return TYPES.DATE;
		if (isNumber(s)) return TYPES.DOUBLE;
		return TYPES.STRING;
	}
	private static int counter = 0;
	public static String gensym()
	{
		return (counter++ + "." + Math.random()).substring(0,8);
	}
	
	public static String decapitalize(final String s)
	{
		return s.substring(0, 1) + s.substring(1).toLowerCase();  
	}

	static public List<StringUtil.TYPES> inferTypes(String[] s)
	{
		List<StringUtil.TYPES> types = new ArrayList<StringUtil.TYPES>();
		for (String str : s)
			types.add(inferType(str));
		return types;
	}
	
	List<StringUtil.TYPES> types = new ArrayList<StringUtil.TYPES>();

	//-----------------------------------------------------------------------------------
	public static boolean isColor(String s)
	{
		if (s == null) return false;
		try
		{
			Color c = StringUtil.readColor(s);
		} 
		catch (IllegalArgumentException e)		{		return false;		}
		return true;
	}

	public static boolean isDate(String s)
	{
		try
		{
			DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
			LocalDate.parse(s, formatter);
			return true;
		} 
		catch (DateTimeParseException e)		{			}
		
		String[] sections = s.split("/");
		if (sections.length == 3)
		{
			int mon = toInteger(sections[0]);			if (!inclRange(mon, 1, 12))  return false;	
			int day = toInteger(sections[1]);			if (!inclRange(day, 1, 31))  return false;	
			int yr = toInteger(sections[2]);
			if (inclRange(yr, 0, 50) || inclRange(yr, 2000, 2050))  return true;	
		}
		return false;	
	}
	//-----------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------
	public static boolean isValidIPAddress (String s) 	
	{
		String[] sections = s.split("\\.");
		int len = sections.length;
		if (len == 4 || len == 6)
		{
			for (String section : sections)
				if (isNumber(section))
				{
					int num = toInteger(section);
					if (!inclRange(num,0,255)) return false;
				}
				else return false;
			return true;
		}
		return false;
	}	
	//-----------------------------------------------------------------------------------
	private static boolean inclRange(int num, int i, int j)	{		return num >= i && num <= j;	}

	public static boolean isInteger (String s) 		{		return isNumber(s) && s.indexOf(".") < 0;	}
	public static boolean isNumber (String s) 
	{
		if (isEmpty(s)) return false;
		try
		{
			Double.parseDouble(s);
			return true;
		}
		catch(NumberFormatException e)		{			return false;		}
	}
	//-----------------------------------------------------------------------------------
	public static double toDouble (String s) 
	{
		if (isEmpty(s)) return Double.NaN;
		try
		{
			return Double.parseDouble(s);
		}
		catch(NumberFormatException e)		{		return Double.NaN;		}
	}
	public static int toInteger (String s) 
	{
		if (isEmpty(s)) return -1;
		try
		{
			return Integer.parseInt(s);
		}
		catch(NumberFormatException e)		{			return -1;		}
	}
	
	public static int toInteger (String s, int deflt) 
	{
		if (isEmpty(s)) return deflt;
		try
		{
			return Integer.parseInt(s);
		}
		catch(NumberFormatException e)		{	return deflt;	}
	}
	//-----------------------------------------------------------------------------------
	private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\s*\\n\\s*");

	static char DEC_PT = '.';			// TODO -- locale dependent !!
	
//	public static boolean isKeyLegal(KeyEvent ev, TextField fld, ValidationType valType)
//	{
//		return true;
//		char c = getChar(ev);
//		if (valType == ValidationType.WHOLE)		return Character.isDigit(c);
//		if (valType == ValidationType.INT)	
//		{
//			if (Character.isDigit(c)) return true;
//			if (c == '-') return fld.getCaretPosition() == 0;
//			return false;
//		}
//		if (valType == ValidationType.ZIP)	
//		{
//			if (Character.isDigit(c)) return true;
//			if (c == '-') return fld.getCaretPosition() == 5;
//			return false;
//		}
//		if (valType == ValidationType.DOUBLE)	
//		{
//			if (Character.isDigit(c)) return true;
//			if (c == '-') return fld.getCaretPosition() == 0;
//			if (c == DEC_PT) return fld.getText().indexOf(DEC_PT) < 0;	
//			return false;
//		}
//		if (valType == ValidationType.STRING)	
//		{
//			if (Character.isLetterOrDigit(c)) return true;
//			if (Character.isSpaceChar(c)) return true;
//			if (c == DEC_PT) return true;	
//			return false;
//		}
//		if (valType == ValidationType.ISBN)			// either 10 or 13 digits
//		{
//			if (Character.isDigit(c)) return true;
//			if (c == '-') return true;
//			return false;
//		}
//		if (valType == ValidationType.DATE)			// digits or slashes
//		{
//			if (Character.isDigit(c)) return true;
//			if (c == '-') return true;		// for ISO dates
//			if (c == '/') return true;		// for MM/DD/YY or MM/DD/YYYY
//			return false;
//		}
//		if (valType == ValidationType.IP4 || valType == ValidationType.IP6)			// digits or decimals
//		{
//			if (Character.isDigit(c)) return true;
//			if (c == DEC_PT) return true;
//			return false;
//		}
//		return true;
//	
//	}
//	static char getChar(KeyEvent ev) 
//	{ 
//		String s = ev.getCharacter();
//		if (s.isEmpty()) return Character.CURRENCY_SYMBOL;
//		return s.charAt(0); 
//	}
	//-------------------------------------------------------
//
//	public static ValidationState validate(String s, ValidationType valType, boolean required)
//	{
//		if (s.trim().isEmpty() && required ) 		return ValidationState.REQUIRED;		// if empty and not required return true
//		if (isStateLegal(s, valType, required)) 	return ValidationState.OK;
//		return ValidationState.ERROR;
//	}
//
//	private static boolean isStateLegal(String s, ValidationType valType, boolean required)
//	{
//		if (s.trim().isEmpty()) return !required;		// if empty and not required return true
//		
//		if (valType == ValidationType.WHOLE)		return isInteger(s);
//		if (valType == ValidationType.INT)			return isInteger(s);
//		if (valType == ValidationType.DOUBLE)		return isNumber(s);
//		if (valType == ValidationType.DATE)			return isDate(s);
//		if (valType == ValidationType.URL)			return isValidUrl(s);
//		if (valType == ValidationType.EMAIL)		return isValidEmail(s);
//		if (valType == ValidationType.IP4)			return isValidIPAddress(s);
//		if (valType == ValidationType.IP6)			return isValidIPAddress(s);
//		if (valType == ValidationType.ZIP)			return isValidZipCode(s);
//		return true;
//	}
//-------------------------------------------------------
	/** @author Daniel Bechler */

	
	public static boolean hasText(final String s)	{	return s != null && s.trim().length() > 0;	}
	public static boolean isEmpty(final String s)	{		return !hasText(s);	}
	public static boolean anyEmpty(final String ... s)	
	{	for (String a : s) 
			if (isEmpty(a)) return true; 
		return false;
	}

	public static String capitalize(final String s)
	{
		if (s != null && s.length() > 0)
		{
			final char[] chars = s.toCharArray();
			chars[0] = Character.toUpperCase(chars[0]);
			return new String(chars).intern();
		}
		return s;
	}

	public static String toPropertyExpression(final String s)		// JSON property, not FX model property
	{
		final char[] chars = s.toCharArray();
		final StringBuilder sb = new StringBuilder();
		char previousChar = ' ';
		for (final char aChar : chars)
		{
			if (aChar != '_')
			{
				if (previousChar == '_')
					sb.append(Character.toUpperCase(aChar));
				else
					sb.append(Character.toLowerCase(aChar));
			}
			previousChar = aChar;
		}
		return sb.toString();
	}

	/**
	 * Converts an optionally camel-cased character sequence (e.g. ThisIsSparta) into underscore-case (e.g.
	 * this_is_sparta).
	 *
	 * @param s The text to convert.
	 *
	 * @return A underscore-cased version of the given text.
	 */
	public static String toUnderscoreCase(final String s)
	{
		final char[] chars = s.toCharArray();
		final StringBuilder sb = new StringBuilder();
		char previousChar = 0;
		for (final char aChar : chars)
		{
			if (Character.isUpperCase(aChar))
			{
				if (previousChar != 0)
					sb.append('_');
				sb.append(Character.toLowerCase(aChar));
			}
			else
				sb.append(aChar);
			previousChar = aChar;
		}
		return sb.toString();
	}

	/**
	 * Joins all non-null elements of the given <code>elements</code> into one String.
	 *
	 * @param delimiter Inserted as separator between consecutive elements.
	 * @param elements  The elements to join.
	 *
	 * @return A long string containing all non-null elements.
	 */
	public static String join(final String delimiter, final Object... elements)
	{
		final StringBuilder sb = new StringBuilder();
		for (final Object part : elements)
		{
			if (part == null)				continue;
			if (sb.length() > 0)		sb.append(delimiter);
			sb.append(part.toString());
		}
		return sb.toString();
	}

	/**
	 * Same as {@link #join(String, Object...)} but with a {@link java.util.Collection} instead of an Array
	 * for the elements.
	 *
	 * @see #join(String, java.util.Collection)
	 */
	public static String join(final String delimiter, final Collection<?> elements)
	{
		if (elements == null || elements.isEmpty())
			return "";
		return join(delimiter, elements.toArray(new Object[elements.size()]));
	}

	public static String toSingleLineString(final Object object)
	{
		if (object != null)
		{
			final String s = object.toString().trim();
			final Matcher matcher = LINE_BREAK_PATTERN.matcher(s);
			return matcher.replaceAll(" \\\\ ");
		}
		return null;
	}
//
//	public static String randomSequence(final int length)
//	{
//		return RandomSequenceGenerator.generate(length);
//	}
//
//	public static String randomSequence(final int length, final CharSequence alphabet)
//	{
//		return RandomSequenceGenerator.generate(length, alphabet);
//	}

	public static Object singleQuote(String content)	
	{	
		return "'" + content.replaceAll("'", "") + "'";	
	}

	public static String getSuperscript(char input) 
	{
		switch (input) 
		{
		    case '0':   return ("\u2070");
		    case '1':   return ("\u00B9");
		    case '2':   return ("\u00B2");
		    case '3':   return ("\u00B3");
		    case '4':   return ("\u2074");
		    case '5':   return ("\u2075");
		    case '6':   return ("\u2076");
		    case '7':   return ("\u2077");
		    case '8':   return ("\u2078");
		    case '9':   return ("\u2079");
		}
		return "";
	}

	public static String beforeColon(String rawString)
	{
		int idx = rawString.indexOf(":");
		if (idx < 0) return rawString;
		return rawString.substring(0, idx).trim();
	}
	public static String afterColon(String rawString)
	{
		int idx = rawString.indexOf(":");
		if (idx < 0) return "";
		return rawString.substring(idx+1).trim();
	}

	public static String clearQuotes(String s)
	{
		String t = s.trim();
		if (t.startsWith("\"") && t.endsWith("\""))
			return t.substring(1, t.length()-2);
		return t;
	}

	// a simple line wrapper based on character count and word boundaries
	public static String asciiWrap(String inputStr, int lineLen)
	{
		if (inputStr == null) return "";
		StringBuilder out = new StringBuilder();
		int curLineStart = 0;
		int inputLen = inputStr.length();
		int idx = 0;
		int prevSpace = 0;
		
		while (idx < inputLen)
		{
			int nextSpace = inputStr.indexOf(' ', idx);
			if (nextSpace < 0)
			{
				out.append(inputStr.substring(prevSpace)).append("\n");
				idx = inputLen;
			}
			else
			if (nextSpace - curLineStart > lineLen)
			{
				out.append(inputStr.substring(curLineStart, prevSpace)).append("\n");
				curLineStart = prevSpace + 1;
				prevSpace = curLineStart;
				idx = curLineStart;
			}
			else
			{
				prevSpace = nextSpace;
				idx = nextSpace + 1;
			}
		}
		
		return out.toString();
	}

	public static String firstWord(String value)
	{
		int idx = value.indexOf(' ');
		if (idx < 0) return value;
		return value.substring(0, idx);
	}

	public static Integer readClosestInt(String string)
	{
		if (!isNumber(string)) return 0;
		if (isInteger(string)) return toInteger(string); 
		return (int) (toDouble(string) + 0.5);
	}
	
	public static String readTag(String markup, String tagName)
	{
		String result = "";
		int idx = markup.indexOf(tagName);
		int idx2 = markup.indexOf(tagName,  idx + 1);
		if (idx > 0 && idx2 > 0)
		{
			int start = 1 + markup.indexOf(">", idx + tagName.length());
			int end = idx2-2;
			result = markup.substring(start, end);
		}
		return result;
	}
//	public static String asString(Point2D.Float endpt)
//	{
//		if (endpt == null) return "";
//		String s = String.format("(%.2f,  %.2f )", endpt.getX(), endpt.getY());  
//		return s;
//	}
//	public static String asString(Bounds b)
//	{
//		if (b == null) return "";
//		String s = String.format("@ %.2f, %.2f [ %.2f x %.2f ]", b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());  
//		return s;
//	}
	public static String asString(Color c)
	{
		if (c == null) return "#000000";
		String s = c.toString();  
		return s;
	}
	public static Color readColor(String c)
	{
		return Color.GRAY;
	}
	public static String spaces(int indent)
	{
		if (indent > 25) indent = 25;
		return "                           ".substring(0, indent);
	}
	public static boolean isXML(String s)
	{
		return s.trim().startsWith("<?xml ");
	}
	public static boolean anyNumbers(String[] strs)
	{
		for (String s : strs)
			if (isNumber(s)) return true;
		return false;
	}
	public static String stripQuotes(String string)
	{
		return string.trim().replaceAll("\"", "");
	}
	//---------------
	static public void removeEmptyLines(List<String> strs)
	{
		int count = strs.size();
		for (int i=count-1; i>0; i--)
		{
			if (isEmpty(strs.get(i)))
				strs.remove(i);
		}
	}
	public static Paint getPaint(String string) {
		// TODO Auto-generated method stub
		return null;
	}
}
