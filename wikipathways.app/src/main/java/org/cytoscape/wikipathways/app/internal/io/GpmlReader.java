package org.cytoscape.wikipathways.app.internal.io;

import java.io.InputStream;
import java.io.StringWriter;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.pathvisio.core.model.Pathway;

public class GpmlReader extends AbstractTask implements CyNetworkReader  {

//	reads the file and creates a CyNetwork.
	
	private final InputStream stream;
	private final CyNetworkFactory networkFactory;
	private final CyNetworkViewFactory viewFactory;
	private CyNetwork network;
	
	public GpmlReader(InputStream stream, CyNetworkFactory networkFactory, CyNetworkViewFactory viewFactory) {
		this.stream = stream;
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork arg0) {
		return viewFactory.createNetworkView(network);
	}

	@Override
	public CyNetwork[] getNetworks() {
		return new CyNetwork[] { network };
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		System.out.println("Import GPML file");
//		System.out.println(stream.toString());
		
		java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A");
		System.out.println(s.hasNext() ? s.next() : "");
		
		Pathway pathway = new Pathway();
		pathway.readFromXml(stream, true);
		
		System.out.println(pathway.getDataObjects().size() + " pathway elements");
		// TODO create network
	}
}
