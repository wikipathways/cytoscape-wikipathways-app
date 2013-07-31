package org.wikipathways.cytoscapeapp.internal.webclient;

import org.cytoscape.work.TaskIterator;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

public class CyWPClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
  public CyWPClient() {
    super("http://www.wikipathways.org", "WikiPathways", "WikiPathways");
  }

  public TaskIterator createTaskIterator(Object query) {
    System.out.println(query);
    return new TaskIterator();
  }
}