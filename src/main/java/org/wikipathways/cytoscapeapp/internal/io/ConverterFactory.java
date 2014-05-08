package org.wikipathways.cytoscapeapp.internal.io;

import org.pathvisio.core.model.Pathway;
import org.cytoscape.model.CyNetwork;

public interface ConverterFactory {
  public Converter create(Pathway pvPathway, CyNetwork cyNet);
}