package org.cytoscape.wikipathways.app.internal.io;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

public class MenuActionLoadGPML<T, C> extends AbstractCyAction {

	 private final CySwingApplication cySwingApp;
     private final TaskManager<T,C> taskMgr;
     private GPMLNetworkManager gpmlNetMgr;
     private final FileUtil fileUtil;
	
	public MenuActionLoadGPML(CySwingApplication cySwingApp, TaskManager<T, C> taskMgr,
			GPMLNetworkManager gpmlNetMgr, FileUtil fileUtil) {
		super("Local GPML file");
		this.setPreferredMenu("File.Import.WikiPathways");
		this.cySwingApp = cySwingApp;
		this.taskMgr = taskMgr;
		this.gpmlNetMgr = gpmlNetMgr;
		this.fileUtil = fileUtil;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		GpmlFileReaderTaskFactory fct = new GpmlFileReaderTaskFactory(gpmlNetMgr);
		File file = fileUtil.getFile(cySwingApp.getJFrame(), "Load GPML file", FileUtil.LOAD, getFilters());
		Task loadTask = fct.createTaskIterator(file).next();
		TaskIterator iter = new TaskIterator(loadTask);
		
		taskMgr.execute(iter);
	}

	private List<FileChooserFilter> getFilters() {
		List<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
		filters.add(new FileChooserFilter("GPML", "gpml"));
		return filters;
    }
}
