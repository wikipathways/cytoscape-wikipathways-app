package org.wikipathways.cytoscapeapp.impl.search;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.CENTER;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

public class CustomQueryTaskFactory implements NetworkSearchTaskFactory {

	private static final String ID = "netsearchtest.test-c";
	private static final String NAME = "C. Custom Query UI";
	private static final String DESCRIPTION = "Provides its own Query UI component";
	private final Icon icon;
	private URL website;
	
	private QueryBar queryBar;
	
	private final CyServiceRegistrar serviceRegistrar;
	
	public CustomQueryTaskFactory(CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
		icon = new TextIcon();
		
		try {
			website = new URL("https://github.com/chrtannus/netsearch-test/blob/master/src/main/java/org/cytoscape/netsearchtest/internal/task/CustomQueryTaskFactory.java");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public JComponent getQueryComponent() {
		if (queryBar == null) {
			queryBar = new QueryBar();
		}
		
		return queryBar;
	}
	
	@Override
	public JComponent getOptionsComponent() {
		return null;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AbstractTask() {
			@Override
			public void run(TaskMonitor tm) throws Exception {
				System.out.println(
						"- Network Search [" + getName() + "]: " + ((QueryBar) getQueryComponent()).getQuery());
			}
		});
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() {
		return website;
	}

	@Override
	public TaskObserver getTaskObserver() {
		return null;
	}

	@Override
	public boolean isReady() {
		return ((QueryBar) getQueryComponent()).isReady();
	}
	
	@SuppressWarnings("serial")
	private class QueryBar extends JPanel {
		
		private JComboBox<String> organismCombo;
		private JTextField searchTextField;
		
		public QueryBar() {
			final GroupLayout layout = new GroupLayout(this);
			this.setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(false);
			
			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addComponent(getOrganismCombo(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(getSearchTextField(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createParallelGroup(CENTER, true)
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(getOrganismCombo(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(getSearchTextField(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addGap(0, 0, Short.MAX_VALUE)
			);
		}
		
		String getQuery() {
			return getSearchTextField().getText();
		}
		
		boolean isReady() {
			String query = getQuery();
			// Let's pretend the query string must have at least 3 characters
			return query != null && query.trim().length() > 2 && getOrganismCombo().getSelectedItem() != null;
		}
		
		private JComboBox<String> getOrganismCombo() {
			if (organismCombo == null) {
				DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
				model.addElement(IconManager.ICON_MALE);
				model.addElement(IconManager.ICON_PAW);
				model.addElement(IconManager.ICON_LEAF);
				model.addElement(IconManager.ICON_BUG);
				
				final Font font = serviceRegistrar.getService(IconManager.class).getIconFont(18.0f);
				
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
			firePropertyChange(QUERY_PROPERTY, null, null);
		}
	}
	
	private class TextIcon implements Icon {
		
		private final int W = 32;
		private final int H = 32;
		private final Font FONT = serviceRegistrar.getService(IconManager.class).getIconFont(28.0f);
		private final String TEXT = IconManager.ICON_HEARTBEAT;
		
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
	        Graphics2D g2d = (Graphics2D) g;
	        g2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
	        
	        int xx = c.getWidth();
	        int yy = c.getHeight();
	        int h2 = g.getFontMetrics().getDescent();
	        g2d.setPaint(new Color(255, 255, 255, 0)); // Transparent
	        g2d.fillRect(0, 0, xx, yy);
	        
	        g2d.setPaint(UIManager.getColor("CyColor.secondary2(-1)"));
	        g2d.setFont(FONT);
	        g2d.drawString(TEXT, x + 2, y + 7 + yy / 2 + h2);
		}
		
		@Override
		public int getIconWidth() {
			return W;
		}
		
		@Override
		public int getIconHeight() {
			return H;
		}
	}
}