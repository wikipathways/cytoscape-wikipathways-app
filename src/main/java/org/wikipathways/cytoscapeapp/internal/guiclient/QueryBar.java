package org.wikipathways.cytoscapeapp.internal.guiclient;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;

@SuppressWarnings("serial")
public class QueryBar extends JPanel {
	
	private JTextField searchTextField;
	CyServiceRegistrar serviceRegistrar;
	
	public QueryBar(CyServiceRegistrar reg) 
	{
		serviceRegistrar = reg;
		final BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
		setLayout(layout);
		add(getSearchTextField());
	}
	
	public String getQueryFromUI() {
		return getSearchTextField().getText();
	}
	
	public boolean isReady() {	
		String query = getQueryFromUI();
		// Let's pretend the query string must have at least 3 characters
		boolean ready = query != null && query.trim().length() > 2; //&& getOrganismCombo().getSelectedItem() != null;
		return ready;
	}

	private JTextField getSearchTextField() {
		if (searchTextField == null) {
			searchTextField = new JTextField();
//			searchTextField.setMinimumSize(getOrganismCombo().getPreferredSize());
			
			// Since we provide our own search component, it should let Cytoscape know
			// when it has been updated by the user, so Cytoscape can give a better
			// feedback to the user of whether or not the whole search component is ready
			// (e.g. Cytoscape may enable or disable the search button)
			searchTextField.getDocument().addDocumentListener(new DocumentListener() {
				@Override public void removeUpdate(DocumentEvent e) {		fireQueryChanged();		}
				@Override public void insertUpdate(DocumentEvent e) {		fireQueryChanged();		}
				@Override public void changedUpdate(DocumentEvent e) {}		// Nothing to do here...
				
			});
		}
		
		return searchTextField;
	}
	
	private void fireQueryChanged() {
		firePropertyChange(NetworkSearchTaskFactory.QUERY_PROPERTY, null, null);
	}
}
