package org.wikipathways.cytoscapeapp.internal.cmd;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.api.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.api.GpmlReaderFactory;

public class GpmlImportCmdTaskFactory extends AbstractTaskFactory {
  final GpmlReaderFactory factory;
  final GpmlConversionMethod method;
  public GpmlImportCmdTaskFactory(
      final GpmlReaderFactory factory,
      final GpmlConversionMethod method
    ) {
    this.factory = factory;
    this.method = method;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new GpmlImportCmdTask(factory, method));
  }
}
