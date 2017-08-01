package org.wikipathways.cytoscapeapp.internal.cmd;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JTextField;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.WebServiceClient;
import org.cytoscape.io.webservice.swing.WebServiceGUI;
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
import org.wikipathways.cytoscapeapp.impl.search.WPSearchCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.WPCyGUIClient;

public class WPSearchCmdTask extends AbstractTask {
   public String query;
  public String species = "";
  WPSearchCmdTaskFactory factory;
	final TaskManager<?,?> dialogTaskManager;
	final	CyServiceRegistrar registrar;
	WPCyGUIClient guiClient;
	
  final WPClient client;
  public WPSearchCmdTask(WPClient client, CyServiceRegistrar r, WPSearchCmdTaskFactory factory, WPCyGUIClient guiClient) {
    this.client = client;
    this.factory = factory;
    registrar = r;
	dialogTaskManager = r.getService(TaskManager.class);
	this.guiClient = guiClient;
  }

  public void run(TaskMonitor monitor) {

		monitor.setTitle("Searching Wikipathways.org");
//		System.out.println("Searching Wikipathways.org");
	    query = factory.getQuery();
//	    System.out.println("query: " + query);
//	    System.out.println("species" + species);

  	if (query == null || query.length() == 0)  	return;     	// BEEP
    
    final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
    
    
//    final WebServiceImportDialog<?> dialog = (WebServiceImportDialog) registrar.getService(WebServiceImportDialog.class);
//    final CyAction openPanelTask = new ShowImportDialogAction(dialogTaskManager, "", 1.0, "menuLabel", null, registrar);
//    insertTasksAfterCurrentTask(searchTask);
    dialogTaskManager.execute(new TaskIterator(searchTask), 			//openPanelTask, 
    		new TaskObserver() {
    			public void taskFinished(ObservableTask t) {}
    			public void allFinished(FinishStatus status) {
//    				 System.out.println("sub run all Finished");
    				setPathwaysInResultsTable(searchTask.get());
    			}        
      });  }
  
  void setPathwaysInResultsTable(final List<WPPathway> pathways) {
	  
//	  System.out.println("-- - -- --");
//	  guiClient.bringToFront();
//	  JTable resultsTable = guiClient.getResultsTable();
		guiClient.setPathwaysInResultsTable(pathways);
		Container queryGui = guiClient.getQueryBuilderGUI();
		Component comp = queryGui.getComponent(0);
		// JTextField is three layers down from QueryBuilderGUI, inject the query text
		if (comp instanceof Container)		
		{
			Component subComp = ((Container)comp).getComponent(0);
			if (subComp instanceof Container)
			{
				Component subsubComp = ((Container)subComp).getComponent(0);
			    if (subsubComp instanceof JTextField)
			    {
			    	JTextField fld = (JTextField) subsubComp;
			    	fld.setText(query);
			    }
			}
		}
		
		// show the dialog
		WebServiceGUI wsGui = registrar.getService(WebServiceGUI.class);
		if (wsGui != null) {
			Window w = wsGui.getWindow(NetworkImportWebServiceClient.class);
			if (w != null) {
				w.toFront();
				w.setVisible(true);
				if (w instanceof JDialog)		// CANT FIGURE OUT HOW TO INSTALL OUR PANEL
				{
					Container parent = queryGui.getParent();
					JDialog dlog = (JDialog) w;
					Container content = dlog.getContentPane();
//					org.cytoscape.webservice.internal.ui.WebServiceImportDialog d;
				}
			}
		}
		// System.out.println("RESULTS " + (pathways == null ? "NULL"
		// :pathways.toString()));
		//
//	for (WPPathway path : pathways)
//	{
//		System.out.println(path.getId() + '\t' + path.getName() + '\t' + path.getSpecies());
//	}
	// attempting to get the table to populate, and to bring its window to the front
  }
  
}