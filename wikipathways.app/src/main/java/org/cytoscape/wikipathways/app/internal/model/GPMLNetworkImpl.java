package org.cytoscape.wikipathways.app.internal.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

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
			
			// TODO: fill up network with network view!
			
			// only for testing: add all pathway element with a text label
			for(PathwayElement element : pathway.getDataObjects()) {
				if(element.getTextLabel() != null && !element.getTextLabel().equals("")) {
					CyNode node1 = pathwayView.addNode();
					// Set name for new nodes
					pathwayView.getRow(node1).set(CyNetwork.NAME, element.getTextLabel());
				}
			}
			
			// TODO: fill up network with pathway view!
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
