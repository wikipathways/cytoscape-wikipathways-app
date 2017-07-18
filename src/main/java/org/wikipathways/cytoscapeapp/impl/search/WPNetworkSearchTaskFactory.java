package org.wikipathways.cytoscapeapp.impl.search;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.QueryBar;

public class WPNetworkSearchTaskFactory extends AbstractNetSearchTestTaskFactory {

	private final WPClient client;
	private final ImageIcon ICON = new ImageIcon(getClass().getClassLoader().getResource("logo_150.png"));
	@Override public Icon getIcon() 		{ return ICON; }
	private final String link = "http://wikipathways.org";
	private URL website;
	@Override public URL getWebsite() { return website;	}
	private final CyServiceRegistrar serviceRegistrar;
	
	
	//----------------------------------------------
	public WPNetworkSearchTaskFactory(CyServiceRegistrar reggie, WPClient clnt, ImageIcon icon) {
		super(
				"wikipathways-netsearchtest.test-b",		// id
				"Wikipathways",								// name
				"A user-curated pathway collection" //,		// description
//				 null,									//icon
//				 link
		);
		serviceRegistrar = reggie;
		client = clnt;
		getQueryComponent();		// make sure queryBar gets defined early
		try {
			website = new URL(link);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	

	@Override public TaskIterator createTaskIterator() {
		WPSearchCmdTaskFactory factory = new WPSearchCmdTaskFactory(client);
		return factory.createTaskIterator();
	}

	// this is the panel that contains a species pop-up and a search text field
	private QueryBar queryBar = null;
	@Override public JComponent getQueryComponent() { 
		if (queryBar == null)
			queryBar = new QueryBar(serviceRegistrar);
		return queryBar;
	}


	@Override	public boolean isReady() { 	 return queryBar != null && queryBar.isReady();	}

}