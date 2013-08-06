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
package org.wikipathways.cytoscapeapp.internal.io;

import java.io.InputStream;
import java.io.IOException;

import org.wikipathways.cytoscapeapp.internal.CyActivator;
import org.wikipathways.cytoscapeapp.internal.model.GPMLNetwork;
import org.wikipathways.cytoscapeapp.internal.model.GPMLNetworkImpl;
import org.wikipathways.cytoscapeapp.internal.model.GPMLNetworkManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.pathvisio.core.model.Pathway;

/**
 * 
 * @author martinakutmon
 * Reads the GPML file and creates a GPMLNetwork 
 * TODO: currently network and pathway view are initialized --> setting!
 */
public class GpmlNetworkReader implements CyNetworkReader {

	InputStream input = null;
    final String fileName;
    CyNetwork net = null;

	
	protected GpmlNetworkReader(InputStream input, String fileName) {
        this.input = input;
        this.fileName = fileName;
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("Read GPML file " + fileName);
		monitor.setProgress(-1.0);

        monitor.setStatusMessage("Parsing GPML file");
        final Pathway pathway = new Pathway();
        pathway.readFromXml(input, true);
        input = null;

        monitor.setStatusMessage("Constructing network");
        net = CyActivator.netFactory.createNetwork();
        final String name = pathway.getMappInfo().getMapInfoName();
        net.getRow(net).set(CyNetwork.NAME, name);
        CyActivator.netMgr.addNetwork(net);
        CyNetworkView view = CyActivator.netViewFactory.createNetworkView(net);
        CyActivator.netViewMgr.addNetworkView(view);
        (new GpmlToPathway(pathway, view)).convert();
	}

    public void cancel() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {}
        }
    }

    public CyNetworkView buildCyNetworkView(CyNetwork network) {
        return null;
    }

    public CyNetwork[] getNetworks() {
        return new CyNetwork[0];
    }
}
