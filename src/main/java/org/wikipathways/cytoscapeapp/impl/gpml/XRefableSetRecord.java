//package model.bio;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Consumer;
//
//import gui.DropUtil;
//import javafx.beans.property.DoubleProperty;
//import javafx.beans.property.ReadOnlyObjectWrapper;
//import javafx.beans.property.SimpleDoubleProperty;
//import javafx.beans.property.SimpleStringProperty;
//import javafx.beans.property.StringProperty;
//import javafx.beans.value.ObservableValue;
//import javafx.scene.chart.LineChart;
//import javafx.scene.chart.NumberAxis;
//import javafx.scene.chart.ScatterChart;
//import javafx.scene.chart.XYChart;
//import javafx.scene.control.TableColumn;
//import javafx.scene.control.TableColumn.CellDataFeatures;
//import javafx.scene.input.DragEvent;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.paint.Color;
//import javafx.scene.shape.Rectangle;
//import javafx.util.Callback;
//import model.chart.DimensionRecord;
//import model.stat.Range;
//import services.bridgedb.BridgeDbIdMapper;
//import services.bridgedb.MappingSource;
//import util.FileUtil;
//import util.StringUtil;
//
//public class XRefableSetRecord extends TableRecord<XRefable> {
//	
//	public XRefableSetRecord(String n, List<TableColumn<XRefable, String>> cols)
//	{
//		this(n);
//		allColumns.addAll(cols);
//	}
//	public XRefableSetRecord(String n)
//	{
//		super(n);
//		xrefSet = new ArrayList<XRefable>();
//	}
////	
////	public XRefableSetRecord(File f)
////	{
////		this(f.getName());
////		
////		List<String> lines = FileUtil.readFileIntoStringList(f);
////		int sz = lines.size();
////		String DELIM = "\t";
////		String firstRow = lines.get(0);
////		if (firstRow.startsWith("[GeneSet]"))
////		{
////			TableColumn<Gene, String> primary = new TableColumn<Gene, String>("Name");
////			getAllColumns().add(primary);
////			int size = lines.size();
////			int parserState = 0;
////			for (int i=0; i<size; i++)	// iterator.next()	
////			{
////				String line = lines.get(i).trim();
////				if (StringUtil.isEmpty(line)) continue;
////				if (line.startsWith("[")) 
////				{
////					if (line.startsWith("[GeneSet]"))	parserState = 1;
//////					if (line.startsWith("[Columns"))	addColumns(line);	
////					if (line.startsWith("[Genes]"))		parserState = 2;
////					continue;
////				}
////				if (parserState == 1) addHeader(line);
////				else if (parserState == 2) addGene(line);
////				else System.err.println("bad parser state: " + line);
////			}
////
////		}
////		else
////		{
////			int nCols = firstRow.split(DELIM).length;
////			for (int i = 1; i< sz; i++)
////			{
////				String row = lines.get(i);
////				String[] tokens = row.split(DELIM);
////				geneSet.add(new Gene(this, tokens[0], ""));
////			}
////		}
////	}
//	public XRefableSetRecord(XRefableSetRecord parent)
//	{
//		this("Subset of " + parent.getName());
//		type.set(parent.getType());
//		species.set(parent.getSpecies());
//		history.set(parent.getHistory());
//		copyColumns(parent);
//	}
//
//	private void copyColumns(XRefableSetRecord parent)
//	{
//		List<TableColumn<XRefable, ?>> parentColumns = parent.getAllColumns();
//		boolean separatorSeen = false;
//		for (TableColumn<XRefable, ?> col : parentColumns)
//		{
//			String text = col.getText();
//			if (text.startsWith("---") )  separatorSeen = true;
//			
//			TableColumn<XRefable, ?> newColumn = new TableColumn(col.getText());
//			newColumn.setEditable(col.isEditable());
//			newColumn.setVisible(col.isVisible());
//			newColumn.setMinWidth(col.getMinWidth());
//			newColumn.setMaxWidth(col.getMaxWidth());
//			newColumn.setPrefWidth(col.getWidth());
//
//			boolean numeric = "TRUE".equals(col.getProperties().get("Numeric"));  
//			if (numeric)
//				setupNumericColumn(col.getText());
//			else //if (typ.equals("T"))
//				setupTextColumn(col.getText());
//			
//		}
//		if (!separatorSeen) 
//			makeSeparator();
//	}
//	private void makeSeparator()
//	{
//		TableColumn<XRefable, String> separatorColumn = new TableColumn<XRefable, String>();
//		separatorColumn.setPrefWidth(0);  
//		separatorColumn.setVisible(false);  
//		separatorColumn.setMaxWidth(0);  
//		separatorColumn.setText("--------");
//		allColumns.add(separatorColumn); 
//	}
//
//	DoubleProperty score = new SimpleDoubleProperty(0);
//	DoubleProperty size = new SimpleDoubleProperty(0);
//	StringProperty comments = new SimpleStringProperty();
//	StringProperty tissue = new SimpleStringProperty();
//	boolean windowState = false;
//	public void setWindowState(boolean b)	{ windowState =b; }
//	public boolean getWindowState()	{ return windowState; }
//	
//	StringProperty history = new SimpleStringProperty();
//	public StringProperty  historyProperty()  { return history;}
//	public String getHistory()  { return history.get();}
//	public void setHistory(String s)  { history.set(s);}
//
//	StringProperty species = new SimpleStringProperty();
//	public StringProperty  speciesProperty()  { return species;}
//	public String getSpecies()  { return species.get();}
//	public void setSpecies(String s)  { species.set(s);}
//
//	private List<XRefable> xrefSet ;		// observableList created in Doc.readCDT or GPML.readXRefableList
//
//	public void setXRefableSet(List<XRefable> g) {  xrefSet = g; 	}
//	public List<XRefable>  getXRefableSet() {	return xrefSet; }
//
//	Map<String, DimensionRecord> dimensions = new HashMap<String, DimensionRecord>();
//	public VBox buildHypercube(List<String> headers)
//	{
//		VBox vbox = new VBox(12);
//		try
//		{
//			int nCols = headers.size();
//			for (int col = 0; col < nCols; col++)
//			{
//				String title = headers.get(col);
//				int index = getValueIndex(title);
//				if (index < 0) continue;
//				List<Double> vals = new ArrayList<Double>();
//				for (XRefable g : getXRefableSet())
//					vals.add(new Double(g.getValue()));		// TODO
//				DimensionRecord rec = new DimensionRecord(title, vals);
//				dimensions.put(title, rec);
//				rec.build1DChart();
//	//				vbox.getChildren().add(rec.getChart());
//			}
//			for (int col = 0; col < nCols; col += 2)
//			{
//				String xDim = headers.get(col);
//				String yDim = headers.get(col+1);
//				DimensionRecord xRec = dimensions.get(xDim);
//				DimensionRecord yRec = dimensions.get(yDim);
//				if (xRec != null && yRec != null)
//				{
//					LineChart<Number, Number> x1D = xRec.getChart();
//					LineChart<Number, Number> y1D = yRec.getChart();
//					ScatterChart<Number, Number> xy2D = buildScatterChart(xRec, yRec);
//					HBox conglom = new HBox(xy2D, new VBox(x1D, y1D));
//					vbox.getChildren().add(conglom);
//				}
//	//				break;  //  when debugging, quit after first 2D chart
//			}
//		}
//		catch (Exception ex) 	{ ex.printStackTrace();  return null;	}
//		return vbox;
//	}
//
//	private ScatterChart<Number, Number> buildScatterChart(DimensionRecord xRec, DimensionRecord yRec) {
//		final NumberAxis xAxis = new NumberAxis();
//		Range xRange = xRec.getRange();
//		xAxis.setLowerBound(xRange.min);
//		xAxis.setUpperBound(xRange.max);
//		xAxis.setLabel(xRec.getTitle());
//		final NumberAxis yAxis = new NumberAxis();
//		Range yRange = yRec.getRange();
//		yAxis.setLowerBound(yRange.min);
//		yAxis.setUpperBound(yRange.max);
//		yAxis.setLabel(yRec.getTitle());
//
//		ScatterChart<Number, Number>	scatter = new ScatterChart<Number, Number>(xAxis, yAxis);
//		scatter.setTitle(xRec.getTitle() + " x " + yRec.getTitle());
//		XYChart.Series<Number, Number> dataSeries = new XYChart.Series<Number, Number>();
//		scatter.getStyleClass().add("custom-chart");
//		dataSeries.setName("XRefables");
//		int sz = Math.min(xRec.getNValues(), yRec.getNValues());
//		for (int i=0; i< sz; i++)
//		{
//			double x = xRec.getValue(i);
//			double y = yRec.getValue(i);
//			if (Double.isNaN(x) || Double.isNaN(y)) continue;
//			XYChart.Data<Number, Number> data = new XYChart.Data<Number, Number>(x, y);
//			Rectangle r = new Rectangle(2,2);
//			r.setFill(i<2000 ? Color.FIREBRICK : Color.YELLOW);
//	        data.setNode(r);
//	        
//			dataSeries.getData().add(data);
//		}
////			Shape circle = new Circle(1);
////			circle.setFill(Color.RED);
//		dataSeries.setNode(new Rectangle(1,1));
//		scatter.getData().addAll(dataSeries);
//		return scatter;
//	}
//	public void setColumnList() {
//		if (headers == null || headers.size() == 0) return;
//		String header = headers.get(0);
//		boolean separatorSeen = false;
//		int skipColumns = 0;
//		String[] fields = header.split("\t");
//		for (int i=skipColumns; i<fields.length; i++)
//		{
//			String fld = fields[i];
//			if (fld.startsWith("---"))
//			{
//				makeSeparator();
//				separatorSeen = true;
//			}
//			else if (isNumeric(fld))
//				setupNumericColumn(fld);
//			else setupTextColumn(fld);
//		}
//		if (!separatorSeen) 
//			makeSeparator();
//	}
//	
//	private void setupTextColumn(String fld)
//	{
//		TableColumn<XRefable, String> column = new TableColumn<XRefable, String>(fld);
//		column.setUserData("T");
//		column.getProperties().put("Numeric", "FALSE");
//		column.setPrefWidth(200);
//		column.setCellValueFactory(new Callback<CellDataFeatures<XRefable, String>, ObservableValue<String>>() {
//		     public ObservableValue<String> call(CellDataFeatures<XRefable, String> p) {
//		         XRefable gene = p.getValue();
//		         String str = "" + gene.getValue();
//		         return new ReadOnlyObjectWrapper(str);
//		     }
//		  });
//		addColumn(column, fld); 
//	}
//
//	private void setupNumericColumn(String fld)
//	{
//		String format =  "%4.2f";
//		TableColumn<XRefable, Double> column = new TableColumn<XRefable, Double>(fld);
//		column.setUserData("N");
//		column.getProperties().put("Numeric", "TRUE");
//		column.getProperties().put("Format", format);
//		column.setCellValueFactory(new Callback<CellDataFeatures<XRefable, Double>, ObservableValue<Double>>() {
//		     public ObservableValue<Double> call(CellDataFeatures<XRefable, Double> p) {
//		         XRefable gene = p.getValue();
//		         double d = gene.getValue();		//fld
//		         if (Double.isNaN(d))
//		        	 return new ReadOnlyObjectWrapper(gene.getValue());		//fld
//		         return new ReadOnlyObjectWrapper(String.format(format, d));
//		     }
//		  });
//		addColumn(column, fld);  //TODO
//
//	}
//
//	private boolean isNumeric(String fld)
//	{
//		if ("logFC".equals(fld)) return true;
//		if ("P.Value".equals(fld)) return true;
//		if ("adj.P.Val".equals(fld)) return true;
//		return false;
//	}
//
//	static String TAB = "\t";
//	static String NL = "\n";
//	public static String BDB = "http://webservice.bridgedb.org/";
//	public void fillIdlist()
//	{
//		Species spec = Species.lookup(species.get());
//		if (spec == null) 
//			spec = Species.Human;
//		StringBuilder str = new StringBuilder();
//		for (XRefable g : xrefSet)
//		{
////			if (StringUtil.hasText(g.getIdlist())) continue;
//			String name = g.getName();
//			MappingSource sys = MappingSource.guessSource(spec, name);
//			str.append(name + TAB + sys.system() + NL);
//		}
//		try
//		{
//			List<String> output = BridgeDbIdMapper.post(BDB, spec.common(), "xrefsBatch", "", str.toString());
//			for (String line : output)
//			{
//				String [] flds = line.split("\t");
//				String name = flds[0];
//				String allrefs = flds[2];
//				int ct = 0;
//				for (XRefable g : xrefSet)
//				{
//					if (!g.getName().equals(name)) continue;
////					System.out.println(ct++ + ": setting ids for " + name );	
////					g.setIdlist(allrefs);
////					g.setEnsembl(BridgeDbIdMapper.getEnsembl(allrefs));
//				}
//			}
//		}
//		catch(Exception ex) 
//		{ 
//			System.err.println(ex.getMessage());	
//		}
//	}
//	public int getRowCount()	{ return xrefSet.size();}
//	public void addXRefable(String line)
//	{
//		String[] tokens = line.split(",");
//		int len = tokens.length;
//		if (StringUtil.isEmpty(tokens[0].trim())) return;		//error
//		if (StringUtil.isEmpty(tokens[len-1].trim())) len--;
//		if (len == 1)
//		{
//			XRefable g = new DataNodeDeprecated(this, tokens[0]);
//			xrefSet.add(g);
//			System.out.println(g.getName());
//		}
//	}
//	public void addColumns(String line)
//	{
////		String[] tokens = line.split(",");
//		System.out.println("addColumns "+ line);
//	}
//	public void addHeader(String line)
//	{
//		System.out.println("addHeader "+ line);
//	}
//
//}
