package org.cytoscape.wikipathways.app.internal.model;

import java.util.Collection;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;

public interface GPMLNetworkManager {
	/**
     * Add a dynamic network.
     * @param dynNetwork
     */
    public void addPathwayView(GPMLNetwork gpmlNetwork);
    
    public void addNetworkView(GPMLNetwork gpmlNetwork);

    /**
     * Get dynamic network.
     * @param network
     * @return dynNetwork
     */
    public GPMLNetwork getGPMLNetwork(CyNetwork network);
    
    /**
     * Get all dynamic networks.
     * @return networks
     */
    public Collection<GPMLNetwork> getGPMLNetworks();
    
    public CyNetworkFactory getCyNetworkFactory();
	
}
