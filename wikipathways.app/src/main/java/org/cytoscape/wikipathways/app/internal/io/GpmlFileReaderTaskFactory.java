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

import java.io.File;

import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManager;
import org.cytoscape.work.TaskIterator;

/**
 * 
 * @author martinakutmon
 * this class creates a TaskIterator to load a GPML file
 *
 */
public class GpmlFileReaderTaskFactory {
	
	private GPMLNetworkManager gpmlNetMgr;
	
	
	public GpmlFileReaderTaskFactory(GPMLNetworkManager gpmlNetMgr) {
		this.gpmlNetMgr = gpmlNetMgr;
	}
	
	
	public TaskIterator createTaskIterator(File file) {
		return new TaskIterator(new GpmlReaderImpl(file, gpmlNetMgr));
	}

}
