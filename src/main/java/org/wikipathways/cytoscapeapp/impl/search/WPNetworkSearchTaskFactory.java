package org.wikipathways.cytoscapeapp.impl.search;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTaskFactory;

public class WPNetworkSearchTaskFactory extends AbstractNetworkSearchTaskFactory {

	private final WPClient client;
//	private ImageIcon ICON;
	private final ImageIcon ICON = new ImageIcon(getClass().getClassLoader().getResource("logo_150.png"));
	@Override
	public Icon getIcon() 		{ return ICON; }
	CyServiceRegistrar serviceRegistrar;
	public WPNetworkSearchTaskFactory(CyServiceRegistrar reggie, WPClient clnt, ImageIcon icon) {
		super(
				"wikipathways-netsearchtest.test-b",		// id
				"Wikipathways",								// name
				"A user-curated pathway collection", 		// description
				null										// icon
		);
		serviceRegistrar = reggie;
		client = clnt;
	//	ICON = icon;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		WPSearchCmdTaskFactory factory = new WPSearchCmdTaskFactory(client);
		return factory.createTaskIterator();
	}

	
	@Override
	public JComponent getQueryComponent() { 
		JPanel p = new QueryBar();
		return p;

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
			final BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
			this.setLayout(layout);
			add(getOrganismCombo());
			add(getSearchTextField());
			add(getGoButton());
			
//			layout.setAutoCreateContainerGaps(false);
//			layout.setAutoCreateGaps(false);
//			
//			layout.setHorizontalGroup(layout.createSequentialGroup()
//					.addComponent(getOrganismCombo(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
//					.addComponent(getSearchTextField(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
//			);
//			layout.setVerticalGroup(layout.createParallelGroup(CENTER, true)
//					.addGap(0, 0, Short.MAX_VALUE)
//					.addComponent(getOrganismCombo(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
//					.addComponent(getSearchTextField(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
//					.addGap(0, 0, Short.MAX_VALUE)
//			);
		}
		
		private String getQuery() {
			return getSearchTextField().getText();
		}
		
		private boolean isReady() {
			JTextField field = getSearchTextField();
					String query = field.getText();
			// Let's pretend the query string must have at least 3 characters
			boolean ready = query != null && query.trim().length() > 2 && getOrganismCombo().getSelectedItem() != null;
			return ready;
		}
		final Font font = serviceRegistrar.getService(IconManager.class).getIconFont(14.0f);
	
		static final String BASE = "http://wikipath/etc/";
		private JButton getGoButton()
		{
			JButton b = new JButton(IconManager.ICON_SEARCH);
			b.setFont(font);
			b.setMaximumSize(new Dimension(24, 24));
			b.setActionCommand("Search");
			b.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Execute query: "+ BASE + "foo?" + getQuery());

					//open the import from database dialog and fill in fields.
				}
			});
			return b;
		}
		
		private JComboBox<String> getOrganismCombo() {
			if (organismCombo == null) {
				DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
				model.addElement(IconManager.ICON_ASTERISK);
				model.addElement(IconManager.ICON_MALE);
				model.addElement(IconManager.ICON_PAW);
				model.addElement(IconManager.ICON_LEAF);
				model.addElement(IconManager.ICON_BUG);
				
				// this doesn't work because you can't mix fonts in the same combobox
//				 Font iconFont = null;
//						try {
//					iconFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("icomoon.ttf"));
//				} catch (FontFormatException e) {
//					throw new RuntimeException();
//				} catch (IOException e) {
//					throw new RuntimeException();
//				}
			
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
			firePropertyChange(QUERY_PROPERTY, null, null);
		}
	}
	
//	private class TextIcon implements Icon {
//		
//		private final int W = 32;
//		private final int H = 32;
//		private final Font FONT = serviceRegistrar.getService(IconManager.class).getIconFont(28.0f);
//		private final String TEXT = IconManager.ICON_HEARTBEAT;
//		
//		@Override
//		public void paintIcon(Component c, Graphics g, int x, int y) {
//	        Graphics2D g2d = (Graphics2D) g;
//	        g2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
//					RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
//	        
//	        int xx = c.getWidth();
//	        int yy = c.getHeight();
//	        int h2 = g.getFontMetrics().getDescent();
//	        g2d.setPaint(new Color(255, 255, 255, 0)); // Transparent
//	        g2d.fillRect(0, 0, xx, yy);
//	        
//	        g2d.setPaint(UIManager.getColor("CyColor.secondary2(-1)"));
//	        g2d.setFont(FONT);
//	        g2d.drawString(TEXT, x + 2, y + 7 + yy / 2 + h2);
//		}
//		
//		@Override
//		public int getIconWidth() {
//			return W;
//		}
//		
//		@Override
//		public int getIconHeight() {
//			return H;
//		}
//	}
//	@Override
//	public JComponent getOptionsComponent() {
//		JCheckBox cb1 = new JCheckBox("DWIM", true);
//		JCheckBox cb2 = new JCheckBox("Custom");
//		cb1.setForeground(Color.WHITE);
//		cb2.setForeground(Color.WHITE);
//		
//		JPanel p = new JPanel(new BorderLayout());
//		p.setBackground(Color.DARK_GRAY);
//		
//		p.add(cb1, BorderLayout.NORTH);
//		p.add(cb2, BorderLayout.SOUTH);
//		
//		return p;
//	}

}