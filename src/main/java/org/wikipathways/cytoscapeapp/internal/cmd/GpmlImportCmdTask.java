package org.wikipathways.cytoscapeapp.internal.cmd;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;


public class GpmlImportCmdTask extends AbstractTask implements ObservableTask {
	@ContainsTunables
  @Tunable(
			longDescription="GpmlImportCmdTask placeholder, put command HERE",
					exampleStringValue="true" )
  public File file;

  final GpmlReaderFactory factory;
	@Tunable(
			description = "GPML Conversion Method",
			gravity = 2.0,
			longDescription="Whether the import produces a pathway or network ", 
			exampleStringValue = "\"Pathway\""
	)
  final GpmlConversionMethod method;
	@Tunable(description = "XXXXXXXX Protein query", required = true, 
	         longDescription="XXXXXXXX Comma separated list of protein names or identifiers",
					 exampleStringValue="EGFR,BRCA1,BRCA2,TP53")
	public String query = null;

	@Tunable(description = "XXXXXXXX Species", 
	         longDescription="XXXXXXXX Species name.  This should be the actual "+
					                "taxonomic name (e.g. homo sapiens, not human)",
					 exampleStringValue="homo sapiens")
	public String species = null;

  public GpmlImportCmdTask( final GpmlReaderFactory factory, final GpmlConversionMethod method) {
    this.factory = factory;
    this.method = method;
  }

  public void run(TaskMonitor monitor) throws Exception {
	  System.out.println("Running ");
    final Reader reader = new FileReader(file);
    int idx = file.getName().indexOf("_");
    if (idx > 0)
    {		
    	String id = file.getName().substring(0, idx);
    	super.insertTasksAfterCurrentTask(factory.createReaderAndViewBuilder(id, reader, method, null));
    }
  }

@Override
public <R> R getResults(Class<? extends R> type) {
	System.out.println("DONE");
	return null;
}
}