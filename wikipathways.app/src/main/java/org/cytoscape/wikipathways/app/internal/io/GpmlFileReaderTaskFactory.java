package org.cytoscape.wikipathways.app.internal.io;

import java.io.InputStream;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskIterator;

public class GpmlFileReaderTaskFactory extends AbstractInputStreamTaskFactory {

	private CyNetworkFactory networkFactory;
	private CyNetworkViewFactory viewFactory;
	
	public GpmlFileReaderTaskFactory(CyFileFilter filter, CyNetworkFactory networkFactory, CyNetworkViewFactory viewFactory) {
		super(filter);
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
	}
	
	@Override
	public TaskIterator createTaskIterator(InputStream stream, String arg1) {
		return new TaskIterator(new GpmlReader(stream, this.networkFactory, this.viewFactory));

	}

}
