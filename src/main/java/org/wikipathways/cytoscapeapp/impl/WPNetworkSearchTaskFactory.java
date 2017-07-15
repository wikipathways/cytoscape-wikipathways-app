package org.wikipathways.cytoscapeapp.impl;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.work.TaskIterator;

public class WPNetworkSearchTaskFactory extends AbstractNetworkSearchTaskFactory {

	private final ImageIcon ICON = new ImageIcon(getClass().getClassLoader().getResource("images/star-96.png"));
	
	public WPNetworkSearchTaskFactory() {
		super(
				"wikipathways-netsearchtest.test-b",
				"Custom Options UI",
				"Wikipathways", null
		);
	}
	
	@Override
	public Icon getIcon() 		{ return ICON; }
	
	@Override
	public JComponent getOptionsComponent() {
		JCheckBox cb1 = new JCheckBox("DWIM", true);
		JCheckBox cb2 = new JCheckBox("Custom");
		cb1.setForeground(Color.WHITE);
		cb2.setForeground(Color.WHITE);
		
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.DARK_GRAY);
		
		p.add(cb1, BorderLayout.NORTH);
		p.add(cb2, BorderLayout.SOUTH);
		
		return p;
	}

	@Override
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return null;
	}
}