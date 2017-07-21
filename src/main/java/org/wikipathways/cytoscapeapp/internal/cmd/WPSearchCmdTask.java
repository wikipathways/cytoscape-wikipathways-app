package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.List;

import org.cytoscape.io.webservice.WebServiceClient;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.internal.guiclient.WPCyGUIClient;

public class WPSearchCmdTask extends AbstractTask {
   public String query;
  public String species = "";
  WPSearchCmdTaskFactory factory;
	final TaskManager<?,?> dialogTaskManager;
	final	CyServiceRegistrar registrar;
	
  final WPClient client;
  public WPSearchCmdTask(WPClient client, CyServiceRegistrar r, WPSearchCmdTaskFactory factory) {
    this.client = client;
    this.factory = factory;
    registrar = r;
	dialogTaskManager = r.getService(TaskManager.class);
  }

  public void run(TaskMonitor monitor) {

		monitor.setTitle("Searching Wikipathways.org");
		System.out.println("Searching Wikipathways.org");
	    query = factory.getQuery();
	    System.out.println("query: " + query);
	    System.out.println("species" + species);

  	if (query == null || query.length() == 0)  	return;     	// BEEP
    
    final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
    
    
//    final WebServiceImportDialog<?> dialog = (WebServiceImportDialog) registrar.getService(WebServiceImportDialog.class);
//    final CyAction openPanelTask = new ShowImportDialogAction(dialogTaskManager, "", 1.0, "menuLabel", null, registrar);
//    insertTasksAfterCurrentTask(searchTask);
    dialogTaskManager.execute(new TaskIterator(searchTask), 			//openPanelTask, 
    		new TaskObserver() {
    			public void taskFinished(ObservableTask t) {}
    			public void allFinished(FinishStatus status) {
    				 System.out.println("sub run all Finished");
    				setPathwaysInResultsTable(searchTask.get());
    			}        
      });  }
  
  void setPathwaysInResultsTable(final List<WPPathway> pathways) {
	  
		 System.out.println("-- - --");
//		 System.out.println("RESULTS " + (pathways == null ? "NULL" :pathways.toString()));

	for (WPPathway path : pathways)
	{
		System.out.println(path.getId() + '\t' + path.getName() + '\t' + path.getSpecies());
	}
	// attempting to get the table to populate, and to bring its window to the front
	if (client instanceof WPClient)
	{
		WebServiceClient gui = null;
		try
		{
			gui = registrar.getService(WebServiceClient.class, "Wikipathways");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (gui != null)
		{
			System.out.println("gui  returned");
		}
		else System.out.println("gui not returned");
//		((WPClient)client).notify();  //setPathwaysInResultsTable(pathways);
	}
	else System.out.println("Class cast error");
  }
  
}