package org.wikipathways.cytoscapeapp.internal;

import java.io.Reader;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;

public class OpenLinkedPathwayAsNewTaskFactory implements NodeViewTaskFactory {
  final WPClient client;
  final GpmlReaderFactory gpmlReaderFactory;

  public OpenLinkedPathwayAsNewTaskFactory(final WPClient client, final GpmlReaderFactory gpmlReaderFactory) {
    this.client = client;
    this.gpmlReaderFactory = gpmlReaderFactory;
  }

  public TaskIterator createTaskIterator(final View<CyNode> nodeView, final CyNetworkView netView) {
    final CyNetwork net = netView.getModel();
    final boolean isPathway = net.getDefaultNodeTable().getColumn("IsGPMLShape") != null;
    final CyNode node = nodeView.getModel();
    final Long nodeId = node.getSUID();
    final CyTable nodeTbl = net.getDefaultNodeTable();
    final String xrefId = nodeTbl.getRow(nodeId).get(isPathway ? "XrefId" : "GeneID", String.class);
    final GpmlConversionMethod method = isPathway ? GpmlConversionMethod.PATHWAY : GpmlConversionMethod.NETWORK;

    final ResultTask<WPPathway> pathwayInfoTask = client.newPathwayInfoTask(xrefId);
    return new TaskIterator(
      pathwayInfoTask,
      new AbstractTask() {
        public void run(final TaskMonitor monitor) {
          final ResultTask<Reader> gpmlContentsTask = client.newGPMLContentsTask(pathwayInfoTask.get());
          super.insertTasksAfterCurrentTask(gpmlContentsTask, new AbstractTask() {
            public void run(final TaskMonitor monitor) {
            	String id = "";
              super.insertTasksAfterCurrentTask(gpmlReaderFactory.createReaderAndViewBuilder(id, gpmlContentsTask.get(), method));
            }
          });
        }
      }
    );
  }

  public boolean isReady(final View<CyNode> nodeView, final CyNetworkView netView) {
   
	if (nodeView == null || netView == null)      return false;
    final CyNetwork net = netView.getModel();
    final CyNode node = nodeView.getModel();
    if (node == null || net == null)      return false;

    final CyTable nodeTbl = net.getDefaultNodeTable();
    final Long nodeId = node.getSUID();

    return checkNode(nodeTbl, nodeId, "XrefId", "XrefDatasource") ||
           checkNode(nodeTbl, nodeId, "GeneID", "Datasource");
  }

  private boolean checkNode(final CyTable nodeTbl, final Long nodeId, final String idColName, final String dataSrcColName) {
    final CyColumn xrefIdCol = nodeTbl.getColumn(idColName);
    final CyColumn xrefSrcCol = nodeTbl.getColumn(dataSrcColName);
    if (!(xrefIdCol != null && xrefSrcCol != null &&
          xrefIdCol.getType().equals(String.class) && xrefSrcCol.getType().equals(String.class))  ) 
      return false;
    
    final String xrefSrc = nodeTbl.getRow(nodeId).get(dataSrcColName, String.class);
    final String xrefId = nodeTbl.getRow(nodeId).get(idColName, String.class);
    if (xrefSrc == null || xrefId == null) return false;
    return "WikiPathways".equals(xrefSrc) && (xrefId.length() > 0);
  }
}