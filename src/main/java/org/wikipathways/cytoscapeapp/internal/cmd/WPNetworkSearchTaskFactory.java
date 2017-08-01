package org.wikipathways.cytoscapeapp.internal.cmd;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;


public class WPNetworkSearchTaskFactory extends AbstractNetSearchTaskFactory {

	private final ImageIcon ICON = new ImageIcon(getClass().getClassLoader().getResource("images/star-96.png"));
	
	public WPNetworkSearchTaskFactory() {
		super(
				"netsearchtest.test-b",
				"Custom Options UI",
				"Wikipathways"
		);
	}
	
//	@Override
	public Icon getIcon() {
		return ICON;
	}
	
//	@Override
	public JComponent getOptionsComponent() {
		JCheckBox cb1 = new JCheckBox("Filter by status", true);
		JCheckBox cb2 = new JCheckBox("Sit Amet");
		cb1.setForeground(Color.WHITE);
		cb2.setForeground(Color.WHITE);
		
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.DARK_GRAY);
		
		p.add(cb1, BorderLayout.NORTH);
		p.add(cb2, BorderLayout.SOUTH);
		
		return p;
	}
}