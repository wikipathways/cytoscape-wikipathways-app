package org.wikipathways.cytoscapeapp.impl.search;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.internal.guiclient.GUI;

public class WPNetworkSearchTaskFactory extends AbstractNetworkSearchTaskFactory  implements TaskObserver {

	
	static String ID = "org.wikipathways.3";
	static String URL = "https://wikipathways.org";
	static String NAME = "WikiPathways";
	static String DESC = "A user-curated pathway collection";
	static String DESC_LONG = "<html>"+
		"WikiPathways is a user-curated collection of pathway diagrams.</p>"+
		"<ul><li>Enter one or more keywords, gene symbols or molecular names</li>"+
		"<li>Separate terms by space, comma or newline</li>"+
		"<li>Use && between terms to restrict the results</li>"+
		"<li>Use ? and * for one or more wildcard characters</li>"+
				"</html>";       
	private final static ImageIcon ICON = new ImageIcon(WPNetworkSearchTaskFactory.class.getClassLoader().getResource("logo_150.png"));
	private static final boolean VERBOSE = false;


	private final WPClient client;
	@Override public Icon getIcon() 		{ return ICON; }
	@Override public URL getWebsite() 
	{ 
		try { return new URL(URL);	}
		catch (MalformedURLException e) {
		e.printStackTrace();
		return null;
	}
	}
	private final CyServiceRegistrar serviceRegistrar;
	private final GUI guiClient;

	private final static URL getURL()
	{
		try
		{
			return new URL(URL);
		}
		catch (Exception e)		{
			return null;
		}
	}
	//----------------------------------------------
	public WPNetworkSearchTaskFactory(CyServiceRegistrar reggie, WPClient clnt, ImageIcon icon, GUI gui) {
		super( ID,	NAME, DESC_LONG, null, null);  // ICON, getURL()
		serviceRegistrar = reggie;
		client = clnt;
		guiClient = gui;
//		getQueryComponent();		// make sure queryBar gets defined early
//		try {
//			website = new URL(URL);
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//		System.out.println("WPNetworkSearchTaskFactory");
	}
	

	@Override public TaskIterator createTaskIterator() throws NullPointerException
	{
		String terms = getQuery();

		if (terms == null) 
			throw new NullPointerException("Query string is null.");
//		System.out.println("createTaskIterator: " + terms);
		WPSearchCmdTaskFactory factory = new WPSearchCmdTaskFactory(client, serviceRegistrar, terms, guiClient);
		return factory.createTaskIterator();
	}
	
	@Override public TaskObserver getTaskObserver() { return this; }
	
	@Override public void taskFinished(ObservableTask task) {
//		if (VERBOSE) System.out.println("taskFinished - " + task.getClass());			// not called (?)
//		if (!(task instanceof WPSearchCmdTask)) 
//			return;
//		WPSearchCmdTask searchTask = (WPSearchCmdTask) task;
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				JOptionPane.showMessageDialog(null, "Your query returned no results",
//						"No results", JOptionPane.ERROR_MESSAGE);  	} });
//		System.out.println("taskFinished: " + searchTask.query);
}

	@Override public void allFinished(FinishStatus finishStatus) {
//		if (VERBOSE) System.out.println("allFinished: " + finishStatus.getType().toString());
	}

	// this is the panel that contains a search text field			(used to have a species pop-up too )
//	private QueryBar queryBar = null;
//	@Override public JComponent getQueryComponent() { 
//	return null;
//	if (queryBar == null)
//			queryBar = new QueryBar(serviceRegistrar);
//		return queryBar;
//	}
//public String getQuery() { return queryBar.getQueryFromUI();	}
//	@Override	public boolean isReady() { 	 return queryBar != null && queryBar.isReady();	}
	public boolean isReady() { return true; }

}
