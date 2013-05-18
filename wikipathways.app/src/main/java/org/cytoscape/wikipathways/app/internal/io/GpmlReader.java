package org.cytoscape.wikipathways.app.internal.io;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

public interface GpmlReader extends Task {

	@Override
    public void run(TaskMonitor tm) throws Exception;
}
