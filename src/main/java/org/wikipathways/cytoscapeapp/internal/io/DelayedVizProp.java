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

package org.wikipathways.cytoscapeapp.internal.io;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;

/**
 * Temporarily stores visual property values of nodes and edges until
 * their views have been created.
 *
 * In Cytoscape, first the network topology is created (via CyNetwork.add{Node|Edge}),
 * then the view objects are created.
 * Once that's done, the network's visual style can be created (via View.setVisualProperty)
 * once all the view objects exist (ensured by CyEventHelper.flushPayloadEvents).
 *
 * However, while we're reading GPML, we need to create the network's visual style
 * while we are creating the network toplogy. Otherwise we'd have to read the GPML twice,
 * once for topology and again for the visual style.
 *
 * How do we get around this problem? While we're reading GPML, we create the network topology
 * and store our desired visual style in DelayedVizProp instances.
 * After we finish reading GPML, we ensure that view objects have been created for
 * all our new nodes and edges (via CyEventHelper.flushPayloadEvents). Finally we apply
 * the visual style stored in the DelayedVizProp objects.
 */
class DelayedVizProp {
  final CyIdentifiable netObj;
  final VisualProperty<?> prop;
  final Object value;
  final boolean isLocked;

  public DelayedVizProp(final CyIdentifiable netObj, final VisualProperty<?> prop, final Object value, final boolean isLocked) {
    this.netObj = netObj;
    this.prop = prop;
    this.value = value;
    this.isLocked = isLocked;
  }

  public static void applyAll(final CyNetworkView netView, final Iterable<DelayedVizProp> delayedProps) {
    for (final DelayedVizProp delayedProp : delayedProps) {
      final Object value = delayedProp.value;
      if (value == null)
        continue;
      
      View<?> view = null;
      if (delayedProp.netObj instanceof CyNode) {
        final CyNode node = (CyNode) delayedProp.netObj;
        view = netView.getNodeView(node);
      } else if (delayedProp.netObj instanceof CyEdge) {
        final CyEdge edge = (CyEdge) delayedProp.netObj;
        view = netView.getEdgeView(edge);
      }

      if (delayedProp.isLocked) {
        view.setLockedValue(delayedProp.prop, value);
      } else {
        view.setVisualProperty(delayedProp.prop, value);
      }
    }
  }
}
