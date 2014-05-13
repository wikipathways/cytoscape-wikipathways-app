package org.wikipathways.cytoscapeapp.internal.io;

import java.io.Reader;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;

public class GpmlReaderFactoryImpl implements GpmlReaderFactory  {
  public TaskIterator createReader(final Reader gpmlContents, final CyNetwork network, final GpmlConversionMethod conversionMethod) {
    return null;
  }

  public TaskIterator createViewBuilder(final CyNetwork gpmlNetwork, final CyNetworkView networkView) {
    return null;
  }

  public TaskIterator createReaderAndViewBuilder(final Reader gpmlContents, final CyNetworkView networkView, final GpmlConversionMethod conversionMethod) {
    return null;
  }

  public TaskIterator createReaderAndViewBuilder(final Reader gpmlContents, final GpmlConversionMethod conversionMethod) {
    return null;
  }
}
