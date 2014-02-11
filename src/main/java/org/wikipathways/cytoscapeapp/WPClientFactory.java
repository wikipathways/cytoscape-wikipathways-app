package org.wikipathways.cytoscapeapp;

public interface WPClientFactory {
  /**
   * Create a new WikiPathways client.
   */
  WPClient create();
}
