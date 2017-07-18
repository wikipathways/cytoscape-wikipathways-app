package org.wikipathways.cytoscapeapp.impl.search;

import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public abstract class AbstractNetSearchTestTaskFactory extends AbstractNetworkSearchTaskFactory {

	protected AbstractNetSearchTestTaskFactory(String id, String name, String description) {
		super(id, name, description, null);
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AbstractTask() {
			@Override
			public void run(TaskMonitor tm) throws Exception {
				System.out.println("- Network Search [" + getName() + "]: " + getQuery());
			}
		});
	}
}