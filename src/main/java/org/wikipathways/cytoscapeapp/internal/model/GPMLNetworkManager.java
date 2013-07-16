// WikiPathways App for Cytoscape
// opens pathways from WikiPathways as networks in Cytoscape
//
// Copyright 2013 WikiPathways.org
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.wikipathways.cytoscapeapp.internal.model;

import java.util.Collection;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;

/**
 * @author martinakutmon
 * Manager interface of the WikiPathways app
 * contains all GPMLNetworks 
 * 
 */
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
