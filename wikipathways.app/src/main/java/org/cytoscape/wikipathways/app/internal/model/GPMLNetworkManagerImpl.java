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
package org.cytoscape.wikipathways.app.internal.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;

/**
 * @author martinakutmon
 * Manager class of the WikiPathways app
 * contains all GPMLNetworks 
 *
 */
public class GPMLNetworkManagerImpl implements GPMLNetworkManager {

	private final Map<CyNetwork, GPMLNetwork> gpmlNetworkMap;
	private CyNetworkManager cyNetMgr;
	private CyNetworkFactory cyNetFactory;
	private CyNetworkViewFactory cyNetViewFactory;
	private CyNetworkViewManager cyNetViewMgr;
	
	public GPMLNetworkManagerImpl(CyNetworkManager cyNetMgr, CyNetworkFactory cyNetFactory, CyNetworkViewFactory cyNetViewFactory,
			CyNetworkViewManager cyNetViewMgr) {
		this.cyNetMgr = cyNetMgr;
		gpmlNetworkMap = new HashMap<CyNetwork, GPMLNetwork>();
		this.cyNetFactory = cyNetFactory;
		this.cyNetViewFactory = cyNetViewFactory;
		this.cyNetViewMgr = cyNetViewMgr;
	}

	@Override
	public GPMLNetwork getGPMLNetwork(CyNetwork network) {
		return gpmlNetworkMap.get(network);
	}

	@Override
	public Collection<GPMLNetwork> getGPMLNetworks() {
		return gpmlNetworkMap.values();
	}

	@Override
	public void addPathwayView(GPMLNetwork gpmlNetwork) {
		CyNetwork network = gpmlNetwork.getPathwayView();
		cyNetMgr.addNetwork(network);
		gpmlNetworkMap.put(network, gpmlNetwork);
		CyNetworkView myView = cyNetViewFactory.createNetworkView(network);
		cyNetViewMgr.addNetworkView(myView);
	}

	@Override
	public void addNetworkView(GPMLNetwork gpmlNetwork) {
		CyNetwork network = gpmlNetwork.getNetworkView();
		cyNetMgr.addNetwork(network);
		gpmlNetworkMap.put(network, gpmlNetwork);
		CyNetworkView myView = cyNetViewFactory.createNetworkView(network);
		cyNetViewMgr.addNetworkView(myView);
	}

	@Override
	public CyNetworkFactory getCyNetworkFactory() {
		return cyNetFactory;
	}

}
