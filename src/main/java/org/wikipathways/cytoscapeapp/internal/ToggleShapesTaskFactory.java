package org.wikipathways.cytoscapeapp.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;

import org.cytoscape.task.NetworkViewTaskFactory;

import org.cytoscape.work.TaskIterator;

public class ToggleShapesTaskFactory implements NetworkViewTaskFactory {
  public ToggleShapesTaskFactory() {}

  public TaskIterator createTaskIterator(final CyNetworkView netView) {
    return new TaskIterator(new ToggleShapesTask(netView));
  }

  public boolean isReady(final CyNetworkView netView) {
    if (netView == null)
      return false;
    final CyNetwork network = netView.getModel();
    if (network == null)
      return false;
    return network.getDefaultNodeTable().getColumn("IsGPMLShape") != null;
  }
}