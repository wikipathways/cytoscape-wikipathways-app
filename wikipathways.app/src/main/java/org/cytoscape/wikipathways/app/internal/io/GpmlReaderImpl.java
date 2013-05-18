package org.cytoscape.wikipathways.app.internal.io;

import java.io.File;
import java.io.IOException;

import org.cytoscape.wikipathways.app.internal.model.GPMLNetwork;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkImpl;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManager;
import org.cytoscape.work.TaskMonitor;
import org.pathvisio.core.model.Pathway;

public class GpmlReaderImpl extends AbstractGpmlReader  {

	private GPMLNetworkManager gpmlMgr;
	
	private File inputFile;
	
	protected GpmlReaderImpl(File inputFile, GPMLNetworkManager gpmlMgr) {
		super(inputFile);
		this.gpmlMgr = gpmlMgr;
		this.inputFile = inputFile;
	}

	@Override
	public void run(TaskMonitor monitor) throws IOException {
		monitor.setProgress(0.0);
		System.out.println("Import GPML file");
		try {
			Pathway pathway = new Pathway();
			pathway.readFromXml(inputFile, true);
			
			System.out.println(pathway.getDataObjects().size());
			GPMLNetwork net = new GPMLNetworkImpl(pathway, gpmlMgr.getCyNetworkFactory());
			
			// TODO: decide which view should be created
			// currently both are initialized!
			gpmlMgr.addNetworkView(net);
			gpmlMgr.addPathwayView(net);
			
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}
		
		monitor.setProgress(1.0);
	}
}
