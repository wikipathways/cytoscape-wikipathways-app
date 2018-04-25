package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.wikipathways.cytoscapeapp.impl.ResultTask;
import org.wikipathways.cytoscapeapp.impl.WPClient;
import org.wikipathways.cytoscapeapp.impl.WPPathway;
import org.wikipathways.cytoscapeapp.impl.search.WPSearchCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.GUI;

public class WPSearchCmdTask extends AbstractTask {
   public String query;
   public String species = "";
   private WPSearchCmdTaskFactory factory;
   private final TaskManager<?,?> dialogTaskManager;
   private final	CyServiceRegistrar registrar;
   private GUI guiClient;
	
   final WPClient client;
   public WPSearchCmdTask(WPClient client, CyServiceRegistrar r, WPSearchCmdTaskFactory factory, GUI guiClient) {
	    this.client = client;
	    this.factory = factory;
	    registrar = r;
		dialogTaskManager = r.getService(TaskManager.class);
		this.guiClient = guiClient;
	}
  	public void setSpecies(String sp)	{ species = sp;	}
  	public void run(TaskMonitor monitor) {

	monitor.setTitle("Searching Wikipathways.org");
	query = factory.getQuery();
	species = guiClient.getSpecies();
  	if (query == null || query.length() == 0)  	return;     	// BEEP
    
    final ResultTask<List<WPPathway>> searchTask = client.freeTextSearchTask(query, species);
    dialogTaskManager.execute(new TaskIterator(searchTask), 			//openPanelTask, 
    		new TaskObserver() {
    			public void taskFinished(ObservableTask t) {}
    			public void allFinished(FinishStatus status) { setPathwaysInResultsTable(searchTask.get()); }        
      });  
    }
  
  void setPathwaysInResultsTable(final List<WPPathway> pathways) {
		JFrame frame = registrar.getService(CySwingApplication.class).getJFrame();
		guiClient.displayPathwaysInModal(frame, query, pathways);
  }
  
}