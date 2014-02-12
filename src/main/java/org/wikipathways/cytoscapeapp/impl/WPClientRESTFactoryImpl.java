package org.wikipathways.cytoscapeapp.impl;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPClientFactory;

public class WPClientRESTFactoryImpl implements WPClientFactory {
  public WPClient create() {
    return new WPClientRESTImpl();
  }
}
