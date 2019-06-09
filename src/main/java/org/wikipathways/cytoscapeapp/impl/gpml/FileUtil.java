package org.wikipathways.cytoscapeapp.impl.gpml;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

//import com.opencsv.CSVReader;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.stage.FileChooser;
//import model.AttributeValue;
//import model.dao.CSVTableData;
//import model.dao.MixedDataRow;
//import services.SystemInfo;
//import xml.XMLTreeItem;

public class FileUtil
{
	static public void openFile(Application app, File f)
	{
		try
		{
			app.getHostServices().showDocument(f.toURI().toURL().toExternalForm());
		}
		catch (Exception e){}
	}

	static public void openFile(Application app, Path f)
	{
		try
		{
			app.getHostServices().showDocument(f.toUri().toURL().toExternalForm());
		}
		catch (Exception e){}
	}

	static public Document openXML(File f) 
	{
		try{
		StringBuilder buff = new StringBuilder();
		readFileIntoBuffer(f, buff);
		return convertStringToDocument(buff.toString());
		}
		catch (Exception e) {	return null;	}
	}
	
	static public String openXMLfile(File f)
	{
		try{
			Document parseddoc = openXML(f);
			StringBuilder xmlOut = new StringBuilder();
			if (parseddoc != null)
			{
				NodeList nodes = parseddoc.getChildNodes();
				int z = nodes.getLength();
				for (int i=0; i<z; i++) 
					readNode(nodes.item(i), xmlOut, 0);
			}
			return xmlOut.toString();
		}
		catch (Exception e) {	return "";	}
	}
	
//	//-------------------------------------------------------------
//	static public XMLTreeItem getXMLtree(File f) throws Exception	{		return getXMLtree(f, null);	}
//	//-------------------------------------------------------------
//	static public XMLTreeItem getXMLtree(File f, String[] suppressNames) throws Exception
//	{
//		StringBuilder buff = new StringBuilder();
//		readFileIntoBuffer(f, buff);
//		return getXMLtree(buff.toString(), suppressNames);
//	}
//	//-------------------------------------------------------------
//	static public XMLTreeItem getXMLtree(String rawtext, String[] suppressNames) throws Exception
//	{
//		XMLTreeItem root = new XMLTreeItem();
//		Document parseddoc = convertStringToDocument(rawtext);
//		if (parseddoc != null)
//			addKids(root, parseddoc.getChildNodes(), suppressNames);
//		return root;
//	}
//	
//	static private void addKids(XMLTreeItem parent, NodeList kids, String[] suppressNames)
//	{
//		int n = kids.getLength();
//		for (int i=0; i<n; i++) 
//		{
//			Node node = kids.item(i);
//			if (node == null)  continue;
//			String nodeName = node.getNodeName();
////			String text = node.getTextContent();
//			if (nodeName == null || nodeName.startsWith("#"))  continue;
//		
////			System.out.println("adding: " + nodeName);
//			XMLTreeItem kid = new XMLTreeItem();
//			kid.setValue(node);
//			
//			if (suppressNames != null)
//			{
//				boolean exp = true;
//				for (String s : suppressNames)
//					if (s.equals(nodeName)) exp = false;
//				kid.setExpanded(exp);
//			}
//			parent.getChildren().add(kid);
//			addKids(kid,node.getChildNodes(), suppressNames);
//		}
//	}
	//-------------------------------------------------------------	
	static public void findkeys(Node node, ObservableList<AttributeValue> list)
	{
		String type = node.getNodeName();
		if ("key".equals(type))
			list.add(new AttributeValue());
		NodeList kids = node.getChildNodes();			// recurse
		int sie = kids.getLength();
		for (int i=0; i<sie; i++)
			findkeys(kids.item(i), list);
	}
	//-------------------------------------------------------------
	//
//	@SuppressWarnings("unchecked") @Deprecated  // use CSVTableData.readCSVFile
//	static public CSVTableData openCSVfile(String absPath, TableView<ObservableList<StringProperty>> table)
//	{
//		if (absPath == null) return null;				// || table == null
//		CSVTableData output = new CSVTableData(absPath.substring(1+absPath.lastIndexOf(File.pathSeparator)));
//		try
//		{
//			String[] row = null;
//			CSVReader csvReader = new CSVReader(new FileReader(absPath));
//			List<String[]> content = csvReader.readAll();
////			ObservableList<StringProperty> props = FXCollections.observableArrayList();
//			csvReader.close();
//			int nCols = -1;
//			 
//			row = (String[]) content.get(0);
//			nCols = row.length;
//			System.out.println(nCols + " columns");
//			boolean isHeader = true;
//			List<MixedDataRow> data = output.getData();
//			int idx = 0;
//			for (String fld : row)
//			{
//				StringUtil.TYPES type = StringUtil.inferType(fld);
//				isHeader &= StringUtil.isString(type) || StringUtil.isEmpty(type);  
//				output.getColumnNames().add(fld);
//				data.add(new MixedDataRow(nCols));
//				System.out.println("Column Name: " + fld);
//			    if (table != null) table.getColumns().add(TableUtil.createColumn(idx++, fld));
//			}
//			output.setTypes(StringUtil.inferTypes((isHeader) ? row : (String[]) content.get(1)));
//			
//			ObservableList<ObservableList<StringProperty>> list = FXCollections.observableArrayList();
//			for (Object object : content)
//			{
//				row = (String[]) object;
//				if (isHeader) { isHeader = false; continue;  }
//				if (row.length != nCols) throw new NumberFormatException();
//				ObservableList<StringProperty> colData = FXCollections.observableArrayList();
//				for (String s : row)
//					colData.add(new SimpleStringProperty(s));
//				list.add(colData);
//			}
//	        if (table != null) table.setItems(list);
//		} 
//		catch (NumberFormatException e)	
//		{		 
//			System.err.print("Wrong number of columns in row"); 
//			e.printStackTrace();	
//			return null;
//		}
//		catch (Exception e)		{	e.printStackTrace();	return null;	}
//		return output;
//	}
	
		//--------------------------------------------------------------------------------------
	static public File compress(File fileSrc)
	{
		String source = fileSrc.getPath();
		String targetZipPath = source + ".zip";
          
		File extant = new File(targetZipPath);
		if (extant.exists()) extant.delete();
       try 
       {              
           pack(source, targetZipPath);
           return new File(targetZipPath);
       } 
       catch (FileAlreadyExistsException ex) 
       {   
    	   System.out.println("FileAlreadyExistsException: " + ex.getMessage()); 
    	   return null;
       }
       catch (IOException ex) 
           {   
    	   System.out.println("compress failed: " + ex.getMessage()); 
    	   return null;
       } 				// Logger.getLogger(null).log(Level.SEVERE, );    }

   }
	
	public static void pack(String sourceDirPath, String zipFilePath) throws IOException {
	    Path p = Files.createFile(Paths.get(zipFilePath));

	    ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p));
	    try {
	        Path pp = Paths.get(sourceDirPath);
	        Files.walk(pp)
	          .filter(path -> !Files.isDirectory(path))
	          .forEach(path -> {
	              String parent = path.toAbsolutePath().toString().replace(pp.toAbsolutePath().toString(), "").replace(path.getFileName().toString(), "");
	              ZipEntry zipEntry = new ZipEntry(parent + "/" + path.getFileName().toString());
	              try {
	                  zs.putNextEntry(zipEntry);
	                  zs.write(Files.readAllBytes(path));
	                  zs.closeEntry();
	            } catch (Exception e)    {   System.err.println(e);   }
	          });
	    } 
	    finally 	 {   zs.close();	}
	}
	
// this creates a sibling folder with the same name as the zip file (without .zip)
	
	static public String decompress(File fileSrc)
	{
		String source = fileSrc.getPath();
		File destFolder = new File(StringUtil.chopExtension(fileSrc.getAbsolutePath()));
		if (destFolder.exists()) return ("Target Folder Exists");
		StringBuilder entryList = new StringBuilder();
		if (destFolder.mkdirs())
		{
			String targetUnZipPath = destFolder.getAbsolutePath();
			try
			{
				FileInputStream fileInputStream = new FileInputStream(source);
				BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				ZipInputStream zipInputStream = new ZipInputStream(bufferedInputStream);

				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null)
				{
					try
					{
						byte[] buffer = new byte[(int) zipEntry.getSize()];
						String unzippedFile = targetUnZipPath + "/" + zipEntry.getName();
						FileOutputStream fileOutputStream = new FileOutputStream(unzippedFile);
						int size;
						while ((size = zipInputStream.read(buffer)) != -1)
							fileOutputStream.write(buffer, 0, size);
						fileOutputStream.flush();
						fileOutputStream.close();
						entryList.append(unzippedFile + "\n");
					} catch (Exception ex)	
					{	
						ex.printStackTrace();		//  FileNotFoundExceptions seen here
					}
					
				}
				zipInputStream.close();
			} catch (IOException ex)			{	Logger.getLogger(null).log(Level.SEVERE, null, "decompress failed");	}
		}
		else 	System.out.println("Failed to create directory.  Nothing unzipped.");
		return entryList.toString();
	}
//--------------------------------------------------------------------------------------
	static private void readNode(Node node, StringBuilder buff, int indent)
	{
//		Element elem = node.getOwnerDocument().getDocumentElement();  //ChildNodes();
		String text = node.getTextContent();
		String type = node.getNodeName();
		if ("Keyword".equals(type)) return;
		if ("#text".equals(type)) return;
		buff.append(doublespaced(indent)).append(type);
		if (text != null && text.length() > 0)
			buff.append(": ").append(text);
		buff.append("\n");
		
		NodeList kids = node.getChildNodes();
		int sie = kids.getLength();
		for (int i=0; i<sie; i++)
			readNode(kids.item(i), buff, indent+1);
	}
	//--------------------------------------------------------------------------------------
	static public String spaces = "                                   ";
	static public String doublespaced(int indent)
	{
		if (indent > 12)  return spaces;
		return spaces.substring(0, 2*indent);
	}
	//--------------------------------------------------------------------------------------
	public static Document convertStringToDocument(String xmlStr) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
        DocumentBuilder builder;  
        try 
        {  
            builder = factory.newDocumentBuilder();  
            Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) ); 
            return doc;
        } catch (Exception e) {  	throw e;    } 
    }
	//--------------------------------------------------------------------------------------
	// @formatter:off
static public boolean hasXMLFiles(Dragboard db)	{	return db.getFiles().stream().filter(f -> isXML(f)).count() > 0;	}

	public static boolean isImageFile(File f){		return isPNG(f) || isJPEG(f);	}
	public static boolean isTextFile(File f){		return isTXT(f) || isCSV(f);	}
	
	static public boolean isXML(File f)		{ 		return fileEndsWith(f,".xml");	}
	static public boolean isJPEG(File f)	{ 		return fileEndsWith(f,".jpg", ".jpeg");	}
	static public boolean isPNG(File f)		{ 		return fileEndsWith(f,".png");	}
	static public boolean isTXT(File f)		{ 		return fileEndsWith(f,".txt");	}
	static public boolean isCSV(Path f)		{ 		return pathEndsWith(f,".csv");	}
	static public boolean isCSV(File f)		{ 		return fileEndsWith(f,".csv");	}
	static public boolean isCSS(File f)		{ 		return fileEndsWith(f,".css");	}
	static public boolean isWebloc(File f)	{ 		return fileEndsWith(f,".webloc", ".url");	}
	static public boolean isFCS(File f)		{ 		return fileEndsWith(f,".fcs", ".lmd");	}
	static public boolean isZip(File f)		{ 		return fileEndsWith(f,".zip", ".gz", ".acs");	}
	static public boolean isSVG(File f)		{ 		return fileEndsWith(f,".svg");	}
	static public boolean isGPML(File f)	{ 		return fileEndsWith(f,".gpml");	}
	static public boolean isCX(File f)		{ 		return fileEndsWith(f,".cx");	}
	static public boolean isOBO(File f)		{ 		return fileEndsWith(f,".obo");	}
	public static boolean isDataFile(File f){		return fileEndsWith(f,".data");	}
	public static boolean isCDT(File f)		{		return fileEndsWith(f,".cdt");	}

	static public FileChooser.ExtensionFilter zipFilter = new FileChooser.ExtensionFilter("Zip files (*.zip)", "*.zip", "*.gz", "*.acs");
	static public FileChooser.ExtensionFilter fcsFilter = new FileChooser.ExtensionFilter("FCS files", "*.fcs", "*.lmd");
	// @formatter:om
	
	static private boolean fileEndsWith(File f, String ...extensions)
	{
		if (f == null) return false;
		String path = f.getAbsolutePath().toLowerCase();  
		for (String ext : extensions)
			if (path.endsWith(ext.toLowerCase())) return true;
		return false;
	}
	static private boolean pathEndsWith(Path p, String ...extensions)
	{
		return fileEndsWith(p.toFile(), extensions);
	}
	
	static public String readFiles(Dragboard db)
	{
		StringBuilder buff = new StringBuilder();
		db.getFiles().forEach(f ->readFile(f, buff));
		return buff.toString();
	}

	static public void readFile(File inFile, StringBuilder buff)
	{
		if (inFile.isDirectory())
		{
			for (File f : inFile.listFiles())
				readFile(f, buff);
		} else 	readFileIntoBuffer(inFile, buff);
	}

	
	static public void readFileIntoBuffer(File f, StringBuilder buff) { 	readFileIntoBuffer(f.getAbsolutePath(), buff); 	}
	
	
	static public String readFileIntoString(String absolutePath)
	{
		StringBuilder buffer = new StringBuilder();
		readFileIntoBuffer(absolutePath, buffer);
		return buffer.toString();
	}
	static public void readFileIntoBuffer(String absolutePath, StringBuilder buff)
	{
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(absolutePath)));
            String line = null;
            String nl = System.getProperty("line.separator", "\n");

            while((line = br.readLine()) != null)
            	buff.append(line + nl);

        } catch (Exception e) {          System.err.println("Error while reading content from selected file");      } 
        finally
        {
            if(br != null)
                try {   br.close();   } catch (Exception e) {}
        }
	}
	static public List<String> readFileIntoStringList(File f)
	{
		return readFileIntoStringList(f.getAbsolutePath(), 1000000);
	}
	
	static public List<String> readFileIntoStringList(String absolutePath)
	{
		return readFileIntoStringList(absolutePath, 1000000);
	}
	
	static public List<String> readFileIntoStringList(String absolutePath, int maxLines)
	{
        BufferedReader br = null;
        int lineCt = 0;
        List<String> strs = new ArrayList<String>();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(absolutePath)));
            String line = null;
//            String nl = System.getProperty("line.separator", "\n");

            while((line = br.readLine()) != null && lineCt++ < maxLines)
            	strs.add(line);

        } catch (Exception e) {          System.err.println("Error while reading content from selected file");      } 
        finally
        {
            if(br != null)
                try {   br.close();   } catch (Exception e) {}
        }
     return strs;
	}

	public static void writeTextFile(File parentFile, String childName, String content)
	{
		try
		{
			File target = new File(parentFile.getAbsolutePath(), childName);
			if (target.exists()) 		target.delete();
			target = createFileFromByteArray(content.getBytes(), target);
		}
		catch (Exception e)		{	e.printStackTrace();	}
	}

	public static File createFileFromByteArray(final byte[] data, final File target) throws IOException
	{
		final File parent = target.getParentFile();
		if (parent != null && !parent.exists())
		{
			if (!parent.mkdirs())	throw new IOException("Unable to create directory '" + parent.getPath());
		}
		final OutputStream fos = new FileOutputStream(target);
		try
		{
			fos.write(data);
		}
		finally	{		fos.close();}
		return target;
	}

	public static byte[] readAsBytes(final File file) throws IOException
	{
		final FileInputStream fis = new FileInputStream(file);
		try
		{
			return inputStreamToByteArray(fis, 8192);
		}
		finally		{	fis.close();	}
	}

	public static byte[] inputStreamToByteArray(final InputStream is, final int bufferSize) throws IOException
	{
		final byte[] buffer = new byte[bufferSize];
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		int length = is.read(buffer);
		while (length > 0)
		{
			os.write(buffer, 0, length);
			length = is.read(buffer);
		}
		return os.toByteArray();
	}

	public static String getHTMLDescription(File f)
	{
		String name = f.getName();
		String path = f.getParent();
		String x = f.isDirectory() ? "DIR/" : "FILE";
		String y = f.lastModified() + "";
		String len = f.length() + " bytes";
		
		return ("<html> "  + name + " <p> " + path + " <p> " + len +  " <p> " + x + " <p> " + y +" <p> </html>");
	}

	public static String getTextDescription(File f)
	{
		String name = f.getName();
		String path = f.getParent();
		String x = f.isDirectory() ? "DIR/" : "FILE";
		String y = f.lastModified() + "";
		String len = f.length() + " bytes";
		
		return (name + "\n" + path + "\n" + len +  "\n" + x + "\n" + y + "\n");
	}

	//--------------------------------------------------------------------------------
	// keep a cache of the images for extensions we've seen
	static HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<String, Image>();

	public static String getFileExt(String fname)
	{
		String ext = ".";
		int p = fname.lastIndexOf('.');
		if (p >= 0) ext = fname.substring(p);
		return ext.toLowerCase();
	}
	//--------------------------------------------------------------------------------
	public static javax.swing.Icon getJSwingIconFromFileSystem(File file)
	{
		javax.swing.Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
//		if (SystemInfo.isMacOSX())
//		{
//			final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
//			icon = fc.getUI().getFileView(fc).getIcon(file);
//		}
		return icon;
	}

	public static Image getFileIcon(String fname)
	{
		final String ext = getFileExt(fname);

		Image fileIcon = mapOfFileExtToSmallIcon.get(ext);
		if (fileIcon == null)
		{
			javax.swing.Icon jswingIcon = null;
			File file = new File(fname);
			if (file.exists()) jswingIcon = getJSwingIconFromFileSystem(file);
			else
			{
				File tempFile = null;
				try
				{
					tempFile = File.createTempFile("icon", ext);
					jswingIcon = getJSwingIconFromFileSystem(tempFile);
				} catch (IOException ignored)
				{} // Cannot create temporary file.
				finally
				{
					if (tempFile != null) tempFile.delete();
				}
			}
			if (jswingIcon != null)
			{
				fileIcon = jswingIconToImage(jswingIcon);
				mapOfFileExtToSmallIcon.put(ext, fileIcon);
			}
		}
		return fileIcon;
	}

	public static Image jswingIconToImage(javax.swing.Icon jswingIcon)
	{
		BufferedImage bufferedImage = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		jswingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
		return SwingFXUtils.toFXImage(bufferedImage, null);
	}

	public static void moveToTrash(File file)
	{
		// TODO -- implement a trash folder to save the cache for analysis
		try
		{
			file.delete();
		}
		catch (Exception e) { e.printStackTrace(); }
		
	}

/* <p>
	 * General file manipulation utilities.
	 * <p>
	 * Facilities are provided in the following areas:
	 * <ul>
	 * <li>writing to a file
	 * <li>reading from a file
	 * <li>make a directory including parent directories
	 * <li>copying files and directories
	 * <li>deleting files and directories
	 * <li>converting to and from a URL
	 * <li>listing files and directories by filter and extension
	 * <li>comparing file content
	 * <li>file last changed date
	 * <li>calculating a checksum
	 * </ul>
	 * <p>
	 * Origin of code: Excalibur, Alexandria, Commons-Utils
*/
	    private static final long ONE_KB = 1024;			//The number of bytes in a kilobyte.
	    private static final long ONE_MB = ONE_KB * ONE_KB;  //The number of bytes in a megabyte.
	    private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;		//The file copy buffer size (30 MB)

	    //-----------------------------------------------------------------------
	    /**
	     * Copies a file to a directory preserving the file date.
	     */
	    public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {	   copyFileToDirectory(srcFile, destDir, true);	    }

	    /**
	     * Copies a file to a directory optionally preserving the file date.
	     */
	    private static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
	        if (destDir == null) 	            throw new NullPointerException("Destination must not be null");
	        if (destDir.exists() && destDir.isDirectory() == false)          throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
	        File destFile = new File(destDir, srcFile.getName());
	        copyFile(srcFile, destFile, preserveFileDate);
	    }

	    /**
	     * Copies a file to a new location preserving the file date.
	     */
	    public static void copyFile(File srcFile, File destFile) throws IOException {	        copyFile(srcFile, destFile, true);    }

	    /**
	     * Copies a file to a new location.
	     */
	    private static void copyFile(File srcFile, File destFile,
	            boolean preserveFileDate) throws IOException {
	        if (srcFile == null)             throw new NullPointerException("Source must not be null");
	        if (destFile == null)            throw new NullPointerException("Destination must not be null");
	        if (srcFile.exists() == false)   throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
	        
	        if (srcFile.isDirectory())       throw new IOException("Source '" + srcFile + "' exists but is a directory");
	      
	        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) 
	            							throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
	        File parentFile = destFile.getParentFile();
	        if (parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory()) 
	                						throw new IOException("Destination '" + parentFile + "' directory cannot be created");
	          
	        if (destFile.exists() && destFile.canWrite() == false) 
	            							throw new IOException("Destination '" + destFile + "' exists but is read-only");
	        doCopyFile(srcFile, destFile, preserveFileDate);
	    }
	    
	    /**
	     * Internal copy file method.
	     * 
	     * @param srcFile  the validated source file, must not be <code>null</code>
	     * @param destFile  the validated destination file, must not be <code>null</code>
	     * @param preserveFileDate  whether to preserve the file date
	     * @throws IOException if an error occurs
	     */
	    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
	        if (destFile.exists() && destFile.isDirectory()) 
	            throw new IOException("Destination '" + destFile + "' exists but is a directory");

	        FileInputStream fis = null;
	        FileOutputStream fos = null;
	        FileChannel input = null;
	        FileChannel output = null;
	        try {
	            fis = new FileInputStream(srcFile);		 input  = fis.getChannel();
	            fos = new FileOutputStream(destFile);	 output = fos.getChannel();
		       long size = input.size();
	            long pos = 0, count = 0;
	            while (pos < size) {
	                count = (size - pos) > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : (size - pos);
	                pos += output.transferFrom(input, pos, count);
	            }
	        } finally {
	            closeQuietly(output);
	            closeQuietly(fos);
	            closeQuietly(input);
	            closeQuietly(fis);
	        }

	        if (srcFile.length() != destFile.length()) 
	            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
	        
	        if (preserveFileDate) 
	            destFile.setLastModified(srcFile.lastModified());
	    }

	    //-----------------------------------------------------------------------
	    /**
	     * Copies a directory to within another directory preserving the file dates.
	     */
	    public static void copyDirectoryToDirectory(File srcDir, File destDir) throws IOException {
	        if (srcDir == null) 	            throw new NullPointerException("Source must not be null");
	        if (srcDir.exists() && srcDir.isDirectory() == false) 	    throw new IllegalArgumentException("Source '" + destDir + "' is not a directory");
	        
	        if (destDir == null) 	            throw new NullPointerException("Destination must not be null");
	        if (destDir.exists() && destDir.isDirectory() == false)      throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
	        copyDirectory(srcDir, new File(destDir, srcDir.getName()), true);
	    }

	    /**
	     * Copies a whole directory to a new location preserving the file dates.
	     */
	    public static void copyDirectory(File srcDir, File destDir) throws IOException {
	        copyDirectory(srcDir, destDir, true);
	    }

	    /**
	     * Copies a whole directory to a new location.
	     */
	    private static void copyDirectory(File srcDir, File destDir, boolean preserveFileDate) throws IOException {
	        copyDirectory(srcDir, destDir, null, preserveFileDate);
	    }

	    /**
	     * Copies a filtered directory to a new location.
  	     */
	    private static void copyDirectory(File srcDir, File destDir, FileFilter filter, boolean preserveFileDate) throws IOException {
	        if (srcDir == null) 	            throw new NullPointerException("Source must not be null");
	        if (destDir == null) 	            throw new NullPointerException("Destination must not be null");
	        if (!srcDir.exists())       		throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
	        if (!srcDir.isDirectory())         	throw new IOException("Source '" + srcDir + "' exists but is not a directory");
	        if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) 
	            								throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
	      

	        // Cater for destination being directory within the source directory (see IO-141)
	        List<String> exclusionList = null;
	        if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
	            File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
	            if (srcFiles != null && srcFiles.length > 0) {
	                exclusionList = new ArrayList<String>(srcFiles.length);
	                for (File srcFile : srcFiles) {
	                    File copiedFile = new File(destDir, srcFile.getName());
	                    exclusionList.add(copiedFile.getCanonicalPath());
	                }
	            }
	        }
	        doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList);
	    }

	    /**
	     * Internal copy directory method.
	     * 
	     * @param srcDir  the validated source directory, must not be <code>null</code>
	     * @param destDir  the validated destination directory, must not be <code>null</code>
	     * @param filter  the filter to apply, null means copy all directories and files
	     * @param preserveFileDate  whether to preserve the file date
	     * @param exclusionList  List of files and directories to exclude from the copy, may be null
	     * @throws IOException if an error occurs
	     */
	    private static void doCopyDirectory(File srcDir, File destDir, FileFilter filter,
	            boolean preserveFileDate, List<String> exclusionList) throws IOException {
	        // recurse
	        File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
	        if (srcFiles == null) {  // null if abstract pathname does not denote a directory, or if an I/O error occurs
	            throw new IOException("Failed to list contents of " + srcDir);
	        }
	        if (destDir.exists()) {
	            if (destDir.isDirectory() == false) 	                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
	           
	        } else if (!destDir.mkdirs() && !destDir.isDirectory())     throw new IOException("Destination '" + destDir + "' directory cannot be created");
	        if (!destDir.canWrite()) 	      						    throw new IOException("Destination '" + destDir + "' cannot be written to");
	        
	        for (File srcFile : srcFiles) {
	            File dstFile = new File(destDir, srcFile.getName());
	            if (exclusionList == null || !exclusionList.contains(srcFile.getCanonicalPath())) {
	                if (srcFile.isDirectory())  doCopyDirectory(srcFile, dstFile, filter, preserveFileDate, exclusionList);
	                 else 	                    doCopyFile(srcFile, dstFile, preserveFileDate);
	            }
	        }
	        // Do this last, as the above has probably affected directory metadata
	        if (preserveFileDate) 
	            destDir.setLastModified(srcDir.lastModified());
	    }

	    //-----------------------------------------------------------------------
	    /**
	     * Deletes a directory recursively. 
	     */
	    public static void deleteDirectory(File directory) throws IOException {
	        if (!directory.exists())          return;
	        if (!directory.delete()) 
	            throw new IOException("Unable to delete directory " + directory + ".");
	    }

	    /**
	     * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
	     * <p>
	     * The difference between File.delete() and this method are:
	     * <ul>
	     * <li>A directory to be deleted does not have to be empty.</li>
	     * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
	     */
	    public static boolean deleteQuietly(File file) {
	        if (file == null)         return false;
	        try {
	            if (file.isDirectory())     cleanDirectory(file);
	        } catch (Exception ignored) {    }

	        try {
	            return file.delete();
	        } 
	        catch (Exception ignored) {          return false;      }
	    }

	    /**
	     * Cleans a directory without deleting it.
	     *
	     * @param directory directory to clean
	     * @throws IOException in case cleaning is unsuccessful
	     */
	    private static void cleanDirectory(File directory) throws IOException {
	        if (!directory.exists()) 	          throw new IllegalArgumentException(directory + " does not exist");
	        

	        if (!directory.isDirectory())        throw new IllegalArgumentException(directory + " is not a directory");
	        File[] files = directory.listFiles();
	        if (files == null)   throw new IOException("Failed to list contents of " + directory);  // null if security restricted
	
	        IOException exception = null;
	        for (File file : files) {
	            try {
	                forceDelete(file);
	            } catch (IOException ioe) {     exception = ioe;   }
	        }
	        if (null != exception) 	            throw exception;
	     }

	    //-----------------------------------------------------------------------
	    /**
	     * Deletes a file. If file is a directory, delete it and all sub-directories.
	     * <p>
	     * The difference between File.delete() and this method are:
	     * <ul>
	     * <li>A directory to be deleted does not have to be empty.</li>
	     * <li>You get exceptions when a file or directory cannot be deleted.
	     *      (java.io.File methods returns a boolean)</li>
	     */
	    public static void forceDelete(File file) throws IOException {
	        if (file.isDirectory())           deleteDirectory(file);
	       else {
	            boolean filePresent = file.exists();
	            if (!file.delete()) {
	                if (!filePresent)      throw new FileNotFoundException("File does not exist: " + file);
	                throw new IOException("Unable to delete file: " + file);
	            }
	        }
	    }

	    /**
	     * Schedules a file to be deleted when JVM exits.
	     * If file is directory delete it and all sub-directories.
	     */
	    public static void forceDeleteOnExit(File file) throws IOException {
	        if (file.isDirectory())       deleteDirectoryOnExit(file);
	         else          				  file.deleteOnExit();
	    }

	    /**
	     * Schedules a directory recursively for deletion on JVM exit.
	     */
	    private static void deleteDirectoryOnExit(File directory) throws IOException {
	        if (!directory.exists())          return;
	         directory.deleteOnExit();
	    }
	    /**
	     * Makes a directory, including any necessary but nonexistent parent
	     * directories. If a file already exists with specified name but it is
	     * not a directory then an IOException is thrown.
	     * If the directory cannot be created (or does not already exist)
	     * then an IOException is thrown.
	     */
	    public static void forceMkdir(File directory) throws IOException {
	        if (directory.exists()) {
	            if (!directory.isDirectory())         throw new IOException("File " + directory + " exists and is not a directory. Unable to create directory.");
	            
	        } else if (!directory.mkdirs())  // Double-check that some other thread or process hasn't made the directory in the background
	                if (!directory.isDirectory())    throw new IOException("Unable to create directory " + directory);
	    }
	    /**
	     * Moves a directory.
	     * When the destination directory is on another file system, do a "copy and delete".
		     */
	    public static void moveDirectory(File srcDir, File destDir) throws IOException {
	        if (srcDir == null)         throw new NullPointerException("Source must not be null");
	        if (destDir == null)        throw new NullPointerException("Destination must not be null");
	        if (!srcDir.exists())       throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
	        if (!srcDir.isDirectory())  throw new IOException("Source '" + srcDir + "' is not a directory");
	        if (destDir.exists())       throw new IOException("Destination '" + destDir + "' already exists");

	        boolean rename = srcDir.renameTo(destDir);
	        if (!rename) {
	            copyDirectory( srcDir, destDir );
	            deleteDirectory( srcDir );
	            if (srcDir.exists())    throw new IOException("Failed to delete original directory '" + srcDir + "' after copy to '" + destDir + "'");
	        }
	    }

	    /**
	     * Moves a directory to another directory.
	     */
	    private static void moveDirectoryToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
	        if (src == null) 			throw new NullPointerException("Source must not be null");
	        if (destDir == null)    	throw new NullPointerException("Destination directory must not be null");
	        
	        if (!destDir.exists() && createDestDir) 
	            destDir.mkdirs();
	        
	        if (!destDir.exists())       throw new FileNotFoundException("Destination directory '" + destDir + "' does not exist [createDestDir=" + createDestDir +"]");
	        if (!destDir.isDirectory())  throw new IOException("Destination '" + destDir + "' is not a directory");
	        moveDirectory(src, new File(destDir, src.getName()));
	    }

	    /**
	     * Moves a file.
	     * When the destination file is on another file system, do a "copy and delete".
	     */
	    public static void moveFile(File srcFile, File destFile) throws IOException {
	        if (srcFile == null) 	   throw new NullPointerException("Source must not be null");
	        if (destFile == null)      throw new NullPointerException("Destination must not be null");
	        if (!srcFile.exists())     throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
	        if (srcFile.isDirectory()) throw new IOException("Source '" + srcFile + "' is a directory");
	        if (destFile.exists())     throw new IOException("Destination '" + destFile + "' already exists");
	        if (destFile.isDirectory()) throw new IOException("Destination '" + destFile + "' is a directory");
	       
	        boolean rename = srcFile.renameTo(destFile);
	        if (!rename) {
	            copyFile( srcFile, destFile );
	            if (!srcFile.delete()) {
	                deleteQuietly(destFile);
	                throw new IOException("Failed to delete original file '" + srcFile + "' after copy to '" + destFile + "'");
	            }
	        }
	    }

	    /**
	     * Moves a file to a directory.
	     */
	    private static void moveFileToDirectory(File srcFile, File destDir, boolean createDestDir) throws IOException {
	        if (srcFile == null)           throw new NullPointerException("Source must not be null");
	        if (destDir == null)           throw new NullPointerException("Destination directory must not be null");
	        if (!destDir.exists() && createDestDir)           destDir.mkdirs();
	        if (!destDir.exists()) 
	            throw new FileNotFoundException("Destination directory '" + destDir + "' does not exist [createDestDir=" + createDestDir +"]");
	        if (!destDir.isDirectory()) 
	            throw new IOException("Destination '" + destDir + "' is not a directory");
	        moveFile(srcFile, new File(destDir, srcFile.getName()));
	    }

	    /**
	     * Moves a file or directory to the destination directory.
	     * <p>
	     * When the destination is on another file system, do a "copy and delete".
	     */
	    public static void moveToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
	        if (src == null)        throw new NullPointerException("Source must not be null");
	        if (destDir == null)    throw new NullPointerException("Destination must not be null");
	        if (!src.exists())      throw new FileNotFoundException("Source '" + src + "' does not exist");
	        
	        if (src.isDirectory())  moveDirectoryToDirectory(src, destDir, createDestDir);
	         else 		            moveFileToDirectory(src, destDir, createDestDir);
	    }

	    /**
	     * Unconditionally close an <code>InputStream</code>.
	     * <p>
	     * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored.
	     * This is typically used in finally blocks.
	     *
	     * @param input  the InputStream to close, may be null or already closed
	     */
	    public static void closeQuietly(InputStream input) {	        closeQuietly((Closeable)input);	    }

	    /**
	     * Unconditionally close an <code>OutputStream</code>.
	     * <p>
	     * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
	     * This is typically used in finally blocks.
	     * @param output  the OutputStream to close, may be null or already closed
	     */
	    public static void closeQuietly(OutputStream output) {       closeQuietly((Closeable)output);   }
	    
	    /**
	     * Unconditionally close a <code>Closeable</code>.
	     */
	    private static void closeQuietly(Closeable closeable) {
	        try {
	            if (closeable != null) 
	                closeable.close();
	        } catch (IOException ioe) { }
	    }
	}
