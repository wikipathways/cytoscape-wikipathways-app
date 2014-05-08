package org.wikipathways.cytoscapeapp.internal.io;

import org.pathvisio.core.model.Pathway;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;

public class GpmlToPathwayFactory implements ConverterFactory {
  final CyEventHelper     cyEventHelper;
  final Annots            cyAnnots;

  public GpmlToPathwayFactory(
      final CyEventHelper     cyEventHelper,
      final Annots            cyAnnots) {
    this.cyEventHelper = cyEventHelper;
    this.cyAnnots = cyAnnots;
  }

  public Converter create(final Pathway pvPathway, final CyNetwork cyNet) {
    return new GpmlToPathway(
      cyEventHelper,
      cyAnnots,
      pvPathway,
      cyNet);
  }
}
