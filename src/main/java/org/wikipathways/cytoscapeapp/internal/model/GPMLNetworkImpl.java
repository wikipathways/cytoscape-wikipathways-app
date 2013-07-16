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

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

/**
 * @author martinakutmon
 * Implementation class of GPMLNetwork
 * each GPMLNetwork contains a pathway object
 * and functions to create the pathway view network
 * and the network view object
 */
public class GPMLNetworkImpl implements GPMLNetwork {

	private Pathway pathway;
	private CyNetworkFactory cyNetFactory;
	private CyNetwork networkView;
	private CyNetwork pathwayView;
	
	
	public GPMLNetworkImpl(Pathway pathway, CyNetworkFactory cyNetFactory) {
		this.pathway = pathway;
		this.cyNetFactory = cyNetFactory;
	}

	
	@Override
	public Pathway getPathway() {
		return pathway;
	}

	@Override
	public CyNetwork getPathwayView() {
		if(pathway != null && pathwayView == null) {
			System.out.println("CREATE PATHWAY VIEW");
			pathwayView = cyNetFactory.createNetwork();
			pathwayView.getRow(pathwayView).set(CyNetwork.NAME, "PathwayView_" + pathway.getMappInfo().getMapInfoName());
			
			// TODO: fill up network with pathway view!
			
			// only for testing: add all pathway element with a text label
			for(PathwayElement element : pathway.getDataObjects()) {
				if(element.getTextLabel() != null && !element.getTextLabel().equals("")) {
					CyNode node1 = pathwayView.addNode();
					// Set name for new nodes
					pathwayView.getRow(node1).set(CyNetwork.NAME, element.getTextLabel());
				}
			}
			
			
		}
	
		return pathwayView;
	}

	@Override
	public CyNetwork getNetworkView() {
		if(pathway != null && networkView == null) {
			System.out.println("CREATE NETWORK VIEW");
			networkView = cyNetFactory.createNetwork();
			networkView.getRow(networkView).set(CyNetwork.NAME, "NetworkView_" + pathway.getMappInfo().getMapInfoName());
			
			// TODO: fill up network with network view!
			
			// only for testing: add all pathway element with a text label
			for(PathwayElement element : pathway.getDataObjects()) {
				if(element.getTextLabel() != null && !element.getTextLabel().equals("")) {
					CyNode node1 = networkView.addNode();
					// Set name for new nodes
					networkView.getRow(node1).set(CyNetwork.NAME, element.getTextLabel());
				}
			}
		}
	
		return networkView;
	}

}
