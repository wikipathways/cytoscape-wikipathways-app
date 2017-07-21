package org.wikipathways.cytoscapeapp.internal.cmd;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;

public class WPSearchCmdTaskFactory extends AbstractTaskFactory 
{
	final WPClient client;
	final String query;
	final CyServiceRegistrar registrar;

  public WPSearchCmdTaskFactory(   final WPClient client, CyServiceRegistrar r) 
  {
	   this(client, r, "");
  }
  
  public WPSearchCmdTaskFactory(WPClient client,  CyServiceRegistrar r, String terms) {
	    this.client = client;
	    query = terms;
	    registrar = r;
	  }

  public String getQuery()  { return query;	}
  
  public TaskIterator createTaskIterator() {
    return new TaskIterator(new WPSearchCmdTask(client, registrar, this));
  }
}
