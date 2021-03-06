package org.wikipathways.cytoscapeapp.impl.search;

import java.util.List;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.impl.ResultTask;

public class WPSpeciesCmdTaskFactory extends AbstractTaskFactory {
  final WPClient client;
 
  public WPSpeciesCmdTaskFactory(final WPClient client) {
    this.client = client;
  }

  public TaskIterator createTaskIterator() {
    final ResultTask<List<String>> speciesTask = client.getSpeciesListTask();
    return new TaskIterator(speciesTask, new ObservableTask() {
      public void run(TaskMonitor monitor) {}
      public void cancel() {}
      public <R> R getResults(Class<? extends R> type) {
        if (List.class.equals(type))           return (R) speciesTask.get();
        if (String.class.equals(type))         return (R) speciesTask.get().toString();
       return null;
      }  });
  }
}
