package org.wikipathways.cytoscapeapp.internal;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class ToggleShapesTask extends AbstractTask {
  final CyNetworkView netView;
  public ToggleShapesTask(final CyNetworkView netView) {
    this.netView = netView;
  }

  public void run(TaskMonitor monitor) {
    final CyNetwork net = netView.getModel();
    final CyTable nodeTbl = net.getDefaultNodeTable();
    for (final View<CyNode> nodeView : netView.getNodeViews()) {
      final CyNode node = nodeView.getModel();
      final Boolean isShape = nodeTbl.getRow(node.getSUID()).get("IsGPMLShape", Boolean.class);
      if (!Boolean.TRUE.equals(isShape))
        continue;
      nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, Boolean.FALSE);
    }
    netView.updateView();
  }

  public void cancel() {}
}