package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPClientFactory;
import org.wikipathways.cytoscapeapp.impl.WPClientRESTFactoryImpl;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTaskFactory;

@SuppressWarnings("serial")
public class QueryBar extends JPanel {
	
	private JComboBox<String> organismCombo;
	private JTextField searchTextField;
	CyServiceRegistrar serviceRegistrar;
	private WPClient client;
	Font font = null; 
	
	public QueryBar(CyServiceRegistrar reg) 
	{
		serviceRegistrar = reg;
		font = serviceRegistrar.getService(IconManager.class).getIconFont(14.0f);
	    final CyApplicationConfiguration appConf = reg.getService(CyApplicationConfiguration.class);
	    final WPClientFactory clientFactory = new WPClientRESTFactoryImpl(appConf);
	    client = clientFactory.create();
		final BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
		setLayout(layout);
		add(getOrganismCombo());
		add(getSearchTextField());
//		add(getGoButton());
	}
	
	public String getQueryFromUI() {
		return getSearchTextField().getText();
	}
	
	public boolean isReady() {	
		String query = getQueryFromUI();
//		System.out.println("isReady: "+ getQueryFromUI().trim());
		// Let's pretend the query string must have at least 3 characters
		boolean ready = query != null && query.trim().length() > 2 && getOrganismCombo().getSelectedItem() != null;
		return ready;
	}
	

	static final String BASE = "http://wikipath/etc/";
//  This is left over from earlier when the task was run from here.
	
	//	private JButton getGoButton()
//	{
//		JButton b = new JButton(IconManager.ICON_SEARCH);
//		b.setFont(font);
//		b.setMaximumSize(new Dimension(24, 24));
//		b.setActionCommand("Search");
//		b.addActionListener(new ActionListener() {
//			@Override public void actionPerformed(ActionEvent e) 
//			{
//				String text =  getQueryFromUI().trim();
//				System.out.println("Execute query: "+ BASE + "search?" + text);
//				WPSearchCmdTaskFactory factory = new WPSearchCmdTaskFactory(client);
//				DialogTaskManager taskManager = serviceRegistrar.getService(DialogTaskManager.class);
//				taskManager.execute(factory.createTaskIterator());
//				//open the import from database dialog and fill in fields.
//			}
//		});
//		return b;
//	}
//	
	private JComboBox<String> getOrganismCombo() {
		if (organismCombo == null) {
			DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
			model.addElement(IconManager.ICON_ASTERISK);
			model.addElement(IconManager.ICON_MALE);
			model.addElement(IconManager.ICON_PAW);
			model.addElement(IconManager.ICON_LEAF);
			model.addElement(IconManager.ICON_BUG);
			
			// this doesn't work because you can't mix fonts in the same combobox
//			 Font iconFont = null;
//					try {
//				iconFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("icomoon.ttf"));
//			} catch (FontFormatException e) {
//				throw new RuntimeException();
//			} catch (IOException e) {
//				throw new RuntimeException();
//			}
		
			final Font font = serviceRegistrar.getService(IconManager.class).getIconFont(14.0f);
			
			organismCombo = new JComboBox<String>(model);
			organismCombo.setFont(font);
			organismCombo.setRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index,
						boolean isSelected, boolean cellHasFocus) {
					super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					setFont(font);
					setHorizontalAlignment(SwingConstants.CENTER);
					return this;
				}
			});
		}
		
		return organismCombo;
	}
	
	private JTextField getSearchTextField() {
		if (searchTextField == null) {
			searchTextField = new JTextField();
			searchTextField.setMinimumSize(getOrganismCombo().getPreferredSize());
			
			// Since we provide our own search component, it should let Cytoscape know
			// when it has been updated by the user, so Cytoscape can give a better
			// feedback to the user of whether or not the whole search component is ready
			// (e.g. Cytoscape may enable or disable the search button)
			searchTextField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent e) {
					fireQueryChanged();
				}
				@Override
				public void insertUpdate(DocumentEvent e) {
					fireQueryChanged();
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
					// Nothing to do here...
				}
			});
		}
		
		return searchTextField;
	}
	
	private void fireQueryChanged() {
		firePropertyChange(NetworkSearchTaskFactory.QUERY_PROPERTY, null, null);
	}
}
