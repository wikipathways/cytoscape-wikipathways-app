package org.cytoscape.wikipathways.app.internal;

import java.util.HashSet;
import java.util.Properties;

import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.wikipathways.app.internal.io.GpmlFileReaderTaskFactory;

public class WikiPathwaysAppHandler {
	
	public WikiPathwaysAppHandler() { 
	}

	public void registerImporter(StreamUtil streamUtil, CyNetworkViewFactory networkViewFactory, CyNetworkFactory networkFactory, CyServiceRegistrar serviceRegistrar) {
		HashSet<String> extensions = new HashSet<String>();
		extensions.add("gpml");
		HashSet<String> contentTypes = new HashSet<String>();
		contentTypes.add("xml");
		String description = "GPML files";
		DataCategory category = DataCategory.NETWORK;
//		System.out.println(swingAdapter);
		BasicCyFileFilter filter = new BasicCyFileFilter(extensions,contentTypes, description, category, streamUtil);
		
		// Create an instance of the ReaderFactory
		GpmlFileReaderTaskFactory factory = new GpmlFileReaderTaskFactory(filter, networkFactory, networkViewFactory);
		
		//register the ReaderFactory as an InputStreamTaskFactory.
		Properties props = new Properties();
		props.setProperty("readerDescription","GPML file reader");
		props.setProperty("readerId","gpmlNetworkReader");
		serviceRegistrar.registerService(factory, InputStreamTaskFactory.class, props);
	}
}
