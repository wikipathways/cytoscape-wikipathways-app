package org.cytoscape.wikipathways.app.internal.io;

import java.io.File;

import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManager;
import org.cytoscape.work.TaskIterator;

public class GpmlFileReaderTaskFactory {
	
	private GPMLNetworkManager gpmlNetMgr;
	
	
	public GpmlFileReaderTaskFactory(GPMLNetworkManager gpmlNetMgr) {
		this.gpmlNetMgr = gpmlNetMgr;
	}
	
	
	public TaskIterator createTaskIterator(File file) {
		return new TaskIterator(new GpmlReaderImpl(file, gpmlNetMgr));
	}

}
