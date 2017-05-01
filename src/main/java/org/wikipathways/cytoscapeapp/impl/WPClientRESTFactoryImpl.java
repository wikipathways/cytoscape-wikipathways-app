package org.wikipathways.cytoscapeapp.impl;

import org.cytoscape.application.CyApplicationConfiguration;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPClientFactory;


public class WPClientRESTFactoryImpl implements WPClientFactory {
  final CyApplicationConfiguration appConf;
  public WPClientRESTFactoryImpl(final CyApplicationConfiguration appConf) {
    this.appConf = appConf;
  }
  public WPClient create() {
    return new WPClientRESTImpl(appConf);
  }
}
