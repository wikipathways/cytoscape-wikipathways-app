package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.List;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;

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
