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

public class FileImportCmdTask extends AbstractTask {
//  static final Pattern WP_ID_REGEX = Pattern.compile("WP\\d+");

	@ContainsTunables
  @Tunable(
			description = "File name",
			longDescription="The full path to the import file, in quotes", 
			exampleStringValue = "file=\"/Users/Joe/ThePathway.gpml\""
	)
  public String file;

  final WPClient client;
  final GpmlReaderFactory factory;
//	@Tunable(
//			description = "GPML Conversion Method",
//			gravity = 2.0,
//			longDescription="Whether the import produces a pathway or network ", 
//			exampleStringValue = "\"Pathway\""
//	)
	  final GpmlConversionMethod method;
  TaskManager<?,?> taskMgr;

  public FileImportCmdTask( final WPClient client, final GpmlReaderFactory factory,  final GpmlConversionMethod method,  TaskManager<?,?> inTaskMgr) {
    this.client = client;
    this.factory = factory;
    this.method = method;
    taskMgr = inTaskMgr;
  }

  public void run(TaskMonitor monitor) {
    if (file == null || file.length() == 0) {
      throw new IllegalArgumentException("file must be specified");
    }
//    filename = filename.trim().toUpperCase();
    if (file.startsWith("file://"))   file=file.substring(6);	// strip front off a url, leaving /

    final ResultTask<WPPathway> infoTask = client.pathwayInfoTask(file);
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