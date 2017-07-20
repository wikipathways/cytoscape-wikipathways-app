package org.wikipathways.cytoscapeapp.internal.cmd;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;

public class WPSearchCmdTaskFactory extends AbstractTaskFactory {
	  final WPClient client;
	  final String query;
	  
  public WPSearchCmdTaskFactory(
	      final WPClient client
	    ) {
	    this.client = client;
	    query = "";
	  }
  public WPSearchCmdTaskFactory(
	      final WPClient client, final String terms
	    ) {
	    this.client = client;
	    query = terms;
	  }

  public String getQuery()  { return query;	}
  public TaskIterator createTaskIterator() {
    return new TaskIterator(new WPSearchCmdTask(client, this));
  }
}
