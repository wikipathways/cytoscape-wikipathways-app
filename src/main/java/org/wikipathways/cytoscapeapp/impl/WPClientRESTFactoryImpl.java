package org.wikipathways.cytoscapeapp.impl;

import org.cytoscape.application.CyApplicationConfiguration;


public class WPClientRESTFactoryImpl implements WPClientFactory {
  final CyApplicationConfiguration appConf;
  final WPManager manager;
  
  public WPClientRESTFactoryImpl(final CyApplicationConfiguration appConf, WPManager mgr) {
    this.appConf = appConf;
    manager = mgr;
  }
  public WPClient create() {
    return new WPClientRESTImpl(appConf, manager);
  }
}
