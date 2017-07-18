package org.wikipathways.cytoscapeapp.impl.search;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.internal.cmd.WPSearchCmdTaskFactory;
import org.wikipathways.cytoscapeapp.internal.guiclient.QueryBar;

public class WPNetworkSearchTaskFactory extends AbstractNetworkSearchTaskFactory {

	private final WPClient client;
//	private ImageIcon ICON;
	private final ImageIcon ICON = new ImageIcon(getClass().getClassLoader().getResource("logo_150.png"));
	@Override
	public Icon getIcon() 		{ return ICON; }
	private final CyServiceRegistrar serviceRegistrar;
	
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

	private QueryBar queryBar = null;
	@Override
	public JComponent getQueryComponent() { 
		if (queryBar == null)
			queryBar = new QueryBar(serviceRegistrar);
		
		return queryBar;

	}


	@Override
	public boolean isReady() {
		return queryBar.isReady();
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