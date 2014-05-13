package org.wikipathways.cytoscapeapp;

/**
 * Used in {@link GpmlReaderFactory} to specify how
 * GPML pathways are converted to Cytoscape networks.
 */
public enum GpmlConversionMethod {
  /**
   * Convert the GPML pathway such that the visual representation
   * of the pathway is preserved. This method is ideal for
   * visualizing a pathway with molecular data like gene expression profiles.
   */
  PATHWAY,

  /**
   * Convert the GPML pathway without visual annotations. This method
   * is ideal for topological analysis and network merging.
   */
  NETWORK
}
