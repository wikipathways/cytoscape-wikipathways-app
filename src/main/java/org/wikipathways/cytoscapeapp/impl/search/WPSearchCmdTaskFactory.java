package org.wikipathways.cytoscapeapp.impl.search;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTask;
import org.wikipathways.cytoscapeapp.internal.guiclient.WPCyGUIClient;

public class WPSearchCmdTaskFactory extends AbstractTaskFactory 
{
	final WPClient client;
	final String query;
	final CyServiceRegistrar registrar;
	WPCyGUIClient guiClient;

  public WPSearchCmdTaskFactory(   final WPClient client, CyServiceRegistrar r, WPCyGUIClient guiClient) 
  {
	   this(client, r, "", guiClient);
  }
  
  public WPSearchCmdTaskFactory(WPClient client,  CyServiceRegistrar r, String terms, WPCyGUIClient guiClient) {
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
