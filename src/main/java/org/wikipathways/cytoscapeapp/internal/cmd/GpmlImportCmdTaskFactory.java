package org.wikipathways.cytoscapeapp.internal.cmd;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.Tunable;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;

public class GpmlImportCmdTaskFactory extends AbstractTaskFactory {
  final GpmlReaderFactory factory;
  final GpmlConversionMethod method;
  
	@Tunable(description = "Protein query", required = true, 
	         longDescription="Comma separated list of protein names or identifiers",
					 exampleStringValue="EGFR,BRCA1,BRCA2,TP53")
	public String query = null;

	@Tunable(description = "Species", 
	         longDescription="Species name.  This should be the actual "+
					                "taxonomic name (e.g. homo sapiens, not human)",
					 exampleStringValue="homo sapiens")
	public String species = null;

	public GpmlImportCmdTaskFactory( final GpmlReaderFactory f, final GpmlConversionMethod m ) {
	  factory = f;
	  method = m;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new GpmlImportCmdTask(factory, method));
  }
}
