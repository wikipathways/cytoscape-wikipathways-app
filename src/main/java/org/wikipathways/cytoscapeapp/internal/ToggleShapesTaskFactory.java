package org.wikipathways.cytoscapeapp.internal;

import org.cytoscape.view.model.CyNetworkView;

import org.cytoscape.task.NetworkViewTaskFactory;

import org.cytoscape.work.TaskIterator;

public class ToggleShapesTaskFactory implements NetworkViewTaskFactory {
  public ToggleShapesTaskFactory() {}

  public TaskIterator createTaskIterator(final CyNetworkView netView) {
    return new TaskIterator(new ToggleShapesTask(netView));
  }

  public boolean isReady(final CyNetworkView netView) {
    return netView.getModel().getDefaultNodeTable().getColumn("IsGPMLShape") != null;
  }
}