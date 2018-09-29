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

import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;

/**
 * 
 * @author martinakutmon
 * this class creates a TaskIterator to load a GPML file
 *
 */
public class GpmlCyReaderTaskFactory extends AbstractInputStreamTaskFactory {
  final GpmlReaderFactory gpmlReaderFactory;

  public GpmlCyReaderTaskFactory(
      final GpmlReaderFactory gpmlReaderFactory,
      final StreamUtil streamUtil) {
    super(new BasicCyFileFilter(new String[]{"gpml"}, new String[]{"text/xml"}, "GPML files", DataCategory.NETWORK, streamUtil));
    this.gpmlReaderFactory = gpmlReaderFactory;
  }
	
	public TaskIterator createTaskIterator(InputStream inputStream, String fileName) {
		Task t = new GpmlCyReaderTask( gpmlReaderFactory, inputStream, fileName);
		return new TaskIterator(t);
	}
}
