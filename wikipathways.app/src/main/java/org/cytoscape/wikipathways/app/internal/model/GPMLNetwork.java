package org.cytoscape.wikipathways.app.internal.model;

import org.cytoscape.model.CyNetwork;
import org.pathvisio.core.model.Pathway;

public interface GPMLNetwork {

	public Pathway getPathway();
	public CyNetwork getPathwayView();
	public CyNetwork getNetworkView();
	
}
