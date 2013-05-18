// WikiPathways App for Cytoscape
// opens pathways from WikiPathways as networks in Cytoscape
//
// Copyright 2013 WikiPathways.org
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
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

/**
 * @author martinakutmon
 * Menu Item added in File -> Import -> WikiPathways
 * loads a local GPML file
 */
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
