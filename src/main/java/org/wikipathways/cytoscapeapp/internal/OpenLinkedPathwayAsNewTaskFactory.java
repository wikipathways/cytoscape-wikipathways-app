package org.wikipathways.cytoscapeapp.internal;

import java.io.Reader;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyColumn;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.ResultTask;

public class OpenLinkedPathwayAsNewTaskFactory implements NodeViewTaskFactory {
  final GpmlConversionMethod method;
  final WPClient client;
  final GpmlReaderFactory gpmlReaderFactory;

  public OpenLinkedPathwayAsNewTaskFactory(final GpmlConversionMethod method, final WPClient client, final GpmlReaderFactory gpmlReaderFactory) {
    this.method = method;
    this.client = client;
    this.gpmlReaderFactory = gpmlReaderFactory;
  }

  public TaskIterator createTaskIterator(final View<CyNode> nodeView, final CyNetworkView netView) {
    final CyNetwork net = netView.getModel();
    final CyNode node = nodeView.getModel();
    final Long nodeId = node.getSUID();
    final CyTable nodeTbl = net.getDefaultNodeTable();
    final String xrefId = nodeTbl.getRow(nodeId).get("XrefId", String.class);

    final ResultTask<WPPathway> pathwayInfoTask = client.newPathwayInfoTask(xrefId);
    return new TaskIterator(
      pathwayInfoTask,
      new AbstractTask() {
        public void run(final TaskMonitor monitor) {
          final ResultTask<Reader> gpmlContentsTask = client.newGPMLContentsTask(pathwayInfoTask.get());
          super.insertTasksAfterCurrentTask(gpmlContentsTask, new AbstractTask() {
            public void run(final TaskMonitor monitor) {
              super.insertTasksAfterCurrentTask(gpmlReaderFactory.createReaderAndViewBuilder(gpmlContentsTask.get(), method));
            }
          });
        }
      }
    );
  }

  public boolean isReady(final View<CyNode> nodeView, final CyNetworkView netView) {
    if (nodeView == null || netView == null)
      return false;

    final CyNetwork net = netView.getModel();
    final CyNode node = nodeView.getModel();
    if (node == null || net == null)
      return false;


    final CyTable nodeTbl = net.getDefaultNodeTable();
    final CyColumn xrefIdCol = nodeTbl.getColumn("XrefId");
    final CyColumn xrefSrcCol = nodeTbl.getColumn("XrefDatasource");
    if (!(xrefIdCol != null &&
          xrefSrcCol != null &&
          xrefIdCol.getType().equals(String.class) &&
          xrefSrcCol.getType().equals(String.class))) {
      return false;
    }

    final Long nodeId = node.getSUID();
    final String xrefSrc = nodeTbl.getRow(nodeId).get("XrefDatasource", String.class);
    final String xrefId = nodeTbl.getRow(nodeId).get("XrefId", String.class);
    
    return (xrefSrc != null &&
            xrefSrc.equals("WikiPathways") &&
            xrefId != null &&
            xrefId.length() > 0);
  }
}