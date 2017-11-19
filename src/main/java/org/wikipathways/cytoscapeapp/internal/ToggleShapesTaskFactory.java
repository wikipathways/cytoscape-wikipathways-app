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