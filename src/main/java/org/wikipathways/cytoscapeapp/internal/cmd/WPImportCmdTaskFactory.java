package org.wikipathways.cytoscapeapp.internal.cmd;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;

public class WPImportCmdTaskFactory extends AbstractTaskFactory {
  final WPClient client;
  final GpmlReaderFactory factory;
  final GpmlConversionMethod method;
  TaskManager<?,?> taskMgr;

  public WPImportCmdTaskFactory(
      final WPClient client,
      final GpmlReaderFactory factory,
      final GpmlConversionMethod method,
      final TaskManager<?,?> inTaskMgr
    ) {
    this.client = client;
    this.factory = factory;
    this.method = method;
    taskMgr = inTaskMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new WPImportCmdTask(client, factory, method, taskMgr));
  }
}
