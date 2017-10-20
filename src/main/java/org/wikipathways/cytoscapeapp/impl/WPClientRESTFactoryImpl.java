package org.wikipathways.cytoscapeapp.impl;

import org.cytoscape.application.CyApplicationConfiguration;
import org.wikipathways.cytoscapeapp.api.WPClient;
import org.wikipathways.cytoscapeapp.api.WPClientFactory;


public class WPClientRESTFactoryImpl implements WPClientFactory {
  final CyApplicationConfiguration appConf;
  public WPClientRESTFactoryImpl(final CyApplicationConfiguration appConf) {
    this.appConf = appConf;
  }
  public WPClient create() {
    return new WPClientRESTImpl(appConf);
  }
}
