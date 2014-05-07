// WikiPathways App for Cytoscape
// opens pathways from WikiPathways as networks in Cytoscape
//
// Copyright 2013 WikiPathways.org
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.wikipathways.cytoscapeapp.internal.io;

import java.io.InputStream;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.task.NetworkTaskFactory;

/**
 * 
 * @author martinakutmon
 * this class creates a TaskIterator to load a GPML file
 *
 */
public class GpmlReaderTaskFactory extends AbstractInputStreamTaskFactory {
  final CyEventHelper             eventHelper;
  final CyNetworkFactory          netFactory;
  final CyNetworkManager          netMgr;
  final CyNetworkViewFactory      netViewFactory;
  final CyNetworkViewManager      netViewMgr;
  final CyLayoutAlgorithmManager  layoutMgr;
  final Annots                    annots;
  final GpmlVizStyle              vizStyle;
  final NetworkTaskFactory        showLODTF;
  final CyNetworkNaming           netNaming;

  public GpmlReaderTaskFactory(
      final CyEventHelper             eventHelper,
      final CyNetworkFactory          netFactory,
      final CyNetworkManager          netMgr,
      final CyNetworkViewFactory      netViewFactory,
      final CyNetworkViewManager      netViewMgr,
      final CyLayoutAlgorithmManager  layoutMgr,
      final StreamUtil                streamUtil,
      final Annots                    annots,
      final GpmlVizStyle              vizStyle,
      final NetworkTaskFactory        showLODTF,
      final CyNetworkNaming           netNaming) {
    super(new BasicCyFileFilter(new String[]{"gpml"}, new String[]{"text/xml"}, "GPML files", DataCategory.NETWORK, streamUtil));
    this.eventHelper = eventHelper;
    this.netFactory = netFactory;
    this.netMgr = netMgr;
    this.netViewFactory = netViewFactory;
    this.netViewMgr = netViewMgr;
    this.layoutMgr = layoutMgr;
    this.annots = annots;
    this.vizStyle = vizStyle;
    this.showLODTF = showLODTF;
    this.netNaming = netNaming;
  }
	
	
	public TaskIterator createTaskIterator(InputStream inputStream, String fileName) {
		return new TaskIterator(new GpmlReaderTask(
      eventHelper,
      netFactory,
      netMgr,
      netViewFactory,
      netViewMgr,
      layoutMgr,
      annots,
      vizStyle,
      showLODTF,
      netNaming,
      inputStream,
      fileName));
	}
}
