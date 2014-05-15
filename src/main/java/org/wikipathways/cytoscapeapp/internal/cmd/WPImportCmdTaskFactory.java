package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.List;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;

public class WPImportCmdTaskFactory extends AbstractTaskFactory {
  final WPClient client;
  final GpmlReaderFactory factory;
  final GpmlConversionMethod method;

  public WPImportCmdTaskFactory(
      final WPClient client,
      final GpmlReaderFactory factory,
      final GpmlConversionMethod method
    ) {
    this.client = client;
    this.factory = factory;
    this.method = method;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new WPImportCmdTask(client, factory, method));
  }
}
