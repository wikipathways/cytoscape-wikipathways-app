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
package org.cytoscape.wikipathways.app.internal.io;

import java.io.InputStream;
import java.io.IOException;

import org.cytoscape.wikipathways.app.internal.model.GPMLNetwork;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkImpl;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.pathvisio.core.model.Pathway;

/**
 * 
 * @author martinakutmon
 * Reads the GPML file and creates a GPMLNetwork 
 * TODO: currently network and pathway view are initialized --> setting!
 */
public class GpmlNetworkReader implements CyNetworkReader {

	final InputStream input;
    final String fileName;

	
	protected GpmlNetworkReader(InputStream input, String fileName) {
        this.input = input;
        this.fileName = fileName;
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("Read GPML file " + fileName);
		monitor.setProgress(-1.0);
        Pathway pathway = new Pathway();
        pathway.readFromXml(input, true);
	}

    public void cancel() {}

    public CyNetworkView buildCyNetworkView(CyNetwork network) {
        return null;
    }

    public CyNetwork[] getNetworks() {
        return new CyNetwork[0];
    }
}
