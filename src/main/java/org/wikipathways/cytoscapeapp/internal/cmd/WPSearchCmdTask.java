package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.ObservableTask;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.ResultTask;

public class WPSearchCmdTask extends AbstractTask {
  @Tunable
  public String query;

  @Tunable
  public String species = null;

  final WPClient client;
  public WPSearchCmdTask( final WPClient client ) {
    this.client = client;
  }

  public void run(TaskMonitor monitor) {

  	System.out.println("query");

  	if (query == null || query.length() == 0) {
    	// BEEP
    	System.out.println("DON'T LET EMPTY QUERY IN");
      throw new IllegalArgumentException("query must be specified");
    }
    
    final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
    
    insertTasksAfterCurrentTask(searchTask, new ObservableTask() {
      public void run(TaskMonitor monitor) {
    	  
    	  // RUN
    	    System.out.println("SearchTask " + searchTask.toString());
    	  
      }
      public void cancel() {}
      
      public <R> R getResults(Class<? extends R> type) {
        if (List.class.equals(type))           
        	return (R) searchTask.get();
        if (String.class.equals(type))         return (R) searchTask.get().toString();
        return null;
      }
    });
  }
}