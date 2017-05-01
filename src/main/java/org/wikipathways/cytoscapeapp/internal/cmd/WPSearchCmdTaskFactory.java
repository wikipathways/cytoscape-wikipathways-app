package org.wikipathways.cytoscapeapp.internal.cmd;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;

public class WPSearchCmdTaskFactory extends AbstractTaskFactory {
  final WPClient client;
  public WPSearchCmdTaskFactory(
      final WPClient client
    ) {
    this.client = client;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new WPSearchCmdTask(client));
  }
}
