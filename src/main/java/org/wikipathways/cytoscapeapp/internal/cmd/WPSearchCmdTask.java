package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;

public class WPSearchCmdTask extends AbstractTask {
   public String query;
  public String species = "";
  WPSearchCmdTaskFactory factory;
  
  final WPClient client;
  public WPSearchCmdTask( final WPClient client, WPSearchCmdTaskFactory factory) {
    this.client = client;
    this.factory = factory;
  }

  public void run(TaskMonitor monitor) {

	    query = factory.getQuery();
	    System.out.println("query: " + query);
	    System.out.println("species" + species);

  	if (query == null || query.length() == 0) {
    	// BEEP
    	return;  //System.out.println("DON'T LET EMPTY QUERY IN");
//      throw new IllegalArgumentException("query must be specified");
    }
    
    final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
    
    insertTasksAfterCurrentTask(searchTask, new ObservableTask() {
      public void run(TaskMonitor monitor) {
    	    System.out.println("SearchTask " + searchTask.toString());
    	  
      }
      public void cancel() {}
      
      public <R> R getResults(Class<? extends R> type) {
  	    System.out.println("getResults " + type.getClass().toString());
 if (List.class.equals(type))           
        	return (R) searchTask.get();
        if (String.class.equals(type))         return (R) searchTask.get().toString();
        return null;
      }
    });
    

  }
}