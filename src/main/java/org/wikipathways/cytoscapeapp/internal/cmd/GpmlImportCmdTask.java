package org.wikipathways.cytoscapeapp.internal.cmd;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;


public class GpmlImportCmdTask extends AbstractTask {
  @Tunable
  public File file;

  final GpmlReaderFactory factory;
  final GpmlConversionMethod method;

  public GpmlImportCmdTask(
      final GpmlReaderFactory factory,
      final GpmlConversionMethod method
    ) {
    this.factory = factory;
    this.method = method;
  }

  public void run(TaskMonitor monitor) throws Exception {
    final Reader reader = new FileReader(file);
    String id = file.getName().substring(0, file.getName().indexOf("_"));
    super.insertTasksAfterCurrentTask(factory.createReaderAndViewBuilder(id, reader, method));
  }
}