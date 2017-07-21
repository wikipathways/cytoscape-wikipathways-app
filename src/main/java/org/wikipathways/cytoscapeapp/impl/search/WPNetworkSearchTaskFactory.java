package org.wikipathways.cytoscapeapp.impl.search;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTask;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.QueryBar;

public class WPNetworkSearchTaskFactory extends AbstractNetSearchTestTaskFactory  implements TaskObserver {

	private final WPClient client;
	private final ImageIcon ICON = new ImageIcon(getClass().getClassLoader().getResource("logo_150.png"));
	@Override public Icon getIcon() 		{ return ICON; }
	private URL website;
	@Override public URL getWebsite() { return website;	}
	private final CyServiceRegistrar serviceRegistrar;
	
	static String ID = "org.wikipathways.3";
	static String URL = "http://wikipathways.org";
	static String NAME = "Wikipathways query";
	static String DESC = "A user-curated pathway collection";
	static String DESC_LONG = "<html>WikiPathways is a database of biological pathways maintained by and for the scientific community. It was established to facilitate the contribution and maintenance of pathway information by the biology community. </p></html>";


	//----------------------------------------------
	public WPNetworkSearchTaskFactory(CyServiceRegistrar reggie, WPClient clnt, ImageIcon icon) {
		super( ID,	NAME, DESC);
		serviceRegistrar = reggie;
		client = clnt;
//		getQueryComponent();		// make sure queryBar gets defined early
		try {
			website = new URL(URL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	

	@Override public TaskIterator createTaskIterator() 
	{
		String terms = getQuery();

		if (terms == null) {
			throw new NullPointerException("Query string is null.");
		}
//		String terms = getQuery();    // queryBar.getQueryFromUI();
		System.out.println("createTaskIterator: " + terms);
		WPSearchCmdTaskFactory factory = new WPSearchCmdTaskFactory(client, serviceRegistrar, terms);
		return factory.createTaskIterator();
	}
	
	@Override public TaskObserver getTaskObserver() { return this; }
	
	@Override public void taskFinished(ObservableTask task) {
		System.out.println("taskFinished - " + task.getClass());
		if (!(task instanceof WPSearchCmdTask)) 
			return;
		WPSearchCmdTask searchTask = (WPSearchCmdTask) task;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog(null, "Your query returned no results",
							                        "No results", JOptionPane.ERROR_MESSAGE); 
			} });
		System.out.println("taskFinished: " + searchTask.query);
}

	@Override public void allFinished(FinishStatus finishStatus) {
		System.out.println("allFinished: " + finishStatus.getType().toString());
	}

	// this is the panel that contains a search text field			(used to have a species pop-up too )
//	private QueryBar queryBar = null;
	@Override public JComponent getQueryComponent() { 
	return null;
//	if (queryBar == null)
//			queryBar = new QueryBar(serviceRegistrar);
//		return queryBar;
	}
//public String getQuery() { return queryBar.getQueryFromUI();	}
//	@Override	public boolean isReady() { 	 return queryBar != null && queryBar.isReady();	}
	public boolean isReady() { return true; }

}