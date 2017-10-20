package org.wikipathways.cytoscapeapp.api;

/**
 * Used as an OSGi service to create new WikiPathways clients.
 *
 * If your Cytoscape app needs to use the WikiPathways client,
 * get the {@code WPClientFactory} service through OSGi.
 * This can be done in your {@code CyActivator} class as follows:
 * <p><pre>
 * {@code 
 * final WPClientFactory wpClientFactory = super.getService(WPClientFactory.class);
 * final WPClient wpClient = wpClientFactory.create();
 * }
 * </pre></p>
 */
public interface WPClientFactory {
  /**
   * Create a new WikiPathways client.
   */
  WPClient create();
}
