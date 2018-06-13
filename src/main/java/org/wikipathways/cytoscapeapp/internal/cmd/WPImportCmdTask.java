package org.wikipathways.cytoscapeapp.internal.cmd;

import java.io.Reader;
import java.util.regex.Pattern;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.Tunable;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.impl.ResultTask;
import org.wikipathways.cytoscapeapp.impl.WPPathway;

public class WPImportCmdTask extends AbstractTask {
  static final Pattern WP_ID_REGEX = Pattern.compile("WP\\d+");

	@ContainsTunables
  @Tunable
  public String id;

  final WPClient client;
  final GpmlReaderFactory factory;
	@Tunable(
			description = "GPML Conversion Method",
			gravity = 2.0,
			longDescription="Whether the import produces a pathway or network ", 
			exampleStringValue = "\"Pathway\""
	)
	  final GpmlConversionMethod method;
  TaskManager<?,?> taskMgr;

  public WPImportCmdTask( final WPClient client, final GpmlReaderFactory factory,  final GpmlConversionMethod method,  TaskManager<?,?> inTaskMgr) {
    this.client = client;
    this.factory = factory;
    this.method = method;
    taskMgr = inTaskMgr;
  }

  public void run(TaskMonitor monitor) {
    if (id == null || id.length() == 0) {
      throw new IllegalArgumentException("id must be specified");
    }
   id = id.trim().toUpperCase();
    if (id.startsWith("“"))   id=id.substring(1);			// somehow id has an extra quote to start, but doesn't carry the final quote thru


    if (!WP_ID_REGEX.matcher(id).matches()) {
      throw new IllegalArgumentException("id must follow this regular format: " + WP_ID_REGEX.pattern());
    }

    final ResultTask<WPPathway> infoTask = client.pathwayInfoTask(id);
    final TaskIterator taskIterator = new TaskIterator(infoTask);
      taskIterator.append(new AbstractTask() {
        public void run(TaskMonitor monitor) {
        	WPPathway pathway = infoTask.get();
        	if (pathway == null)
        	{
        		System.err.println("Something went wrong...");
        		return;
        }
        	taskIterator.append(new AbstractTask() {
              public void run(TaskMonitor monitor) {
          		System.out.println("loading!");
              	factory.getWPManager().loadPathway(method, pathway, taskMgr);
              }
           });
	     taskMgr.execute(taskIterator, new TaskObserver() {
	        public void taskFinished(ObservableTask t) { System.out.println("inner task finished " + t);}
	        public void allFinished(FinishStatus status) {  System.out.println("inner all done");}
	     });


        } });
      taskMgr.execute(taskIterator, new TaskObserver() {
          public void taskFinished(ObservableTask t) { System.err.println("outer finished! " + t);}
          public void allFinished(FinishStatus status) {  System.out.println("outer all done");}
   });

  }
}