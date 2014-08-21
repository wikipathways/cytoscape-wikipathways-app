// WikiPathways App for Cytoscape
//
// Copyright 2013-2014 WikiPathways
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
    final boolean areGPMLShapesShown = areGPMLShapesShown();
    for (final View<CyNode> nodeView : netView.getNodeViews()) {
      final CyNode node = nodeView.getModel();
      final Boolean isShape = nodeTbl.getRow(node.getSUID()).get("IsGPMLShape", Boolean.class);
      if (!Boolean.TRUE.equals(isShape))
        continue;
      nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, !areGPMLShapesShown);
    }
    netView.updateView();
  }

  public void cancel() {}

  /**
   * Counts the number of shown GPML shapes vs hidden; if the number of shown is more,
   * return true.
   *
   * This is done because there is no reliable way to know if the toggle shapes task
   * was performed on a given network. Setting a network attribute may not work,
   * because the network view can be destroyed and all of the visual properties of its
   * nodes without affecting the state of the network attribute.
   */
  boolean areGPMLShapesShown() {
    final CyNetwork net = netView.getModel();
    final CyTable nodeTbl = net.getDefaultNodeTable();
    int shownCount = 0;
    int hiddenCount = 0;
    for (final View<CyNode> nodeView : netView.getNodeViews()) {
      final CyNode node = nodeView.getModel();
      final Boolean isShape = nodeTbl.getRow(node.getSUID()).get("IsGPMLShape", Boolean.class);
      if (!Boolean.TRUE.equals(isShape))
        continue;
      if (Boolean.FALSE.equals(nodeView.getVisualProperty(BasicVisualLexicon.NODE_VISIBLE))) {
        hiddenCount++;
      } else {
        shownCount++;
      }
    }
    return shownCount > hiddenCount;
  }
}