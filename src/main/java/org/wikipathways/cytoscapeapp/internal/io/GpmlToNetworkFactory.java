package org.wikipathways.cytoscapeapp.internal.io;

import org.pathvisio.core.model.Pathway;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;

public class GpmlToNetworkFactory implements ConverterFactory {
  final CyEventHelper     cyEventHelper;

  public GpmlToNetworkFactory(
      final CyEventHelper     cyEventHelper) {
    this.cyEventHelper = cyEventHelper;
  }

  public Converter create(final Pathway pvPathway, final CyNetwork cyNet) {
    return new GpmlToNetwork(
      cyEventHelper,
      pvPathway,
      cyNet);
  }
}
