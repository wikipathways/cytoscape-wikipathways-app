package org.wikipathways.cytoscapeapp.impl.search;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.api.WPClient;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTask;
import org.wikipathways.cytoscapeapp.internal.guiclient.GUI;

public class WPSearchCmdTaskFactory extends AbstractTaskFactory 
{
	final WPClient client;
	final String query;
	final CyServiceRegistrar registrar;
	GUI guiClient;

  public WPSearchCmdTaskFactory(   final WPClient client, CyServiceRegistrar r, GUI guiClient) 
  {
	   this(client, r, "", guiClient);
  }
  
  public WPSearchCmdTaskFactory(WPClient client,  CyServiceRegistrar r, String terms, GUI guiClient) {
	    this.client = client;
	    query = terms;
	    registrar = r;
	    this.guiClient = guiClient;
	  }

  public String getQuery()  { return query;	}
  
  public TaskIterator createTaskIterator() {
    return new TaskIterator(new WPSearchCmdTask(client, registrar, this, guiClient));
  }
}
