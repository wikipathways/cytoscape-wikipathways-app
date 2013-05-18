package org.cytoscape.wikipathways.app.internal.io;

import java.io.File;

import org.cytoscape.work.AbstractTask;

public abstract class AbstractGpmlReader extends AbstractTask implements GpmlReader {

	protected File inputFile;
	
	protected AbstractGpmlReader(File inputFile) {
		this.inputFile = inputFile;
	}
	
}
