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
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.pathvisio.core.model.Pathway;
import org.wikipathways.cytoscapeapp.internal.CyActivator;

/**
 * 
 * @author martinakutmon
 * Reads the GPML file and creates a GPMLNetwork 
 * TODO: currently network and pathway view are initialized --> setting!
 */
public class GpmlNetworkReader extends AbstractTask implements CyNetworkReader {

	InputStream input = null;
    final String fileName;
	
    @Tunable(description="Import as:", groups={"WikiPathways"})
    public ListSingleSelection<String> importMethod = new ListSingleSelection<String>("Pathway", "Network");
    
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
        final String name = pathway.getMappInfo().getMapInfoName();
        final CyNetworkView view = newNetwork(name);
        if(importMethod.getSelectedValue().equals("Pathway")) {
        	(new GpmlToPathway(pathway, view)).convert();
        } else {
        	Boolean unconnected = (new GpmlToNetwork(pathway, view)).convert();
        	if(unconnected) {
	        	JOptionPane.showMessageDialog(CyActivator.cySwingApp.getJFrame(),
	        		    "<html>Some of the lines in the pathways are not connected.<br>Therefore some nodes might not be connected.</html>",
	        		    "Unconnected lines warning",
	        		    JOptionPane.WARNING_MESSAGE);
        	}
        	CyLayoutAlgorithm layout = CyActivator.layoutMgr.getLayout("force-directed");
        	insertTasksAfterCurrentTask(layout.createTaskIterator(view, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null));
        }
        updateNetworkView(view);
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

    private static CyNetworkView newNetwork(final String name) {
        final CyNetwork net = CyActivator.netFactory.createNetwork();
        net.getRow(net).set(CyNetwork.NAME, name);
        CyActivator.netMgr.addNetwork(net);
        final CyNetworkView view = CyActivator.netViewFactory.createNetworkView(net);
        CyActivator.netViewMgr.addNetworkView(view);
        return view;
    }

    private static void updateNetworkView(final CyNetworkView netView) {
        GpmlVizStyle.get().apply(netView);
        netView.fitContent();
        netView.updateView();
    }
}
