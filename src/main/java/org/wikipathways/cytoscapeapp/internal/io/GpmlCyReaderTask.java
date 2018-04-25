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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;

/**
 * 
 * @author martinakutmon
 * Reads the GPML file and creates a GPMLNetwork 
 * TODO: currently network and pathway view are initialized --> setting!
 */
public class GpmlCyReaderTask extends AbstractTask {
    public static final String PATHWAY_DESC = "Pathway";
    public static final String NETWORK_DESC = "Network";

    final GpmlReaderFactory gpmlReaderFactory;
	InputStream input = null;
    final String fileName;

    @Tunable(description="Import as:", groups={"WikiPathways"})
    public ListSingleSelection<String> importMethod = new ListSingleSelection<String>(PATHWAY_DESC, NETWORK_DESC);

	public GpmlCyReaderTask( final GpmlReaderFactory factory, final InputStream input, final String fileName) {
        gpmlReaderFactory = factory;
        this.input = input;
        this.fileName = fileName;
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("Read GPML file " + fileName);
		monitor.setProgress(-1.0);
		int index = fileName.indexOf("_");			
		String id = (index > 0) ? fileName.substring(0, index) : fileName;
        final Reader reader = new InputStreamReader(input);
        final GpmlConversionMethod method = importMethod.getSelectedValue().equals(PATHWAY_DESC)  ? GpmlConversionMethod.PATHWAY  : GpmlConversionMethod.NETWORK;
        super.insertTasksAfterCurrentTask(gpmlReaderFactory.createReaderAndViewBuilder(id, reader, method));
    }

    public void cancel() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {}
        }
    }
}
