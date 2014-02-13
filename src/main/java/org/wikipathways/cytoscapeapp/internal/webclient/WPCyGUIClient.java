package org.wikipathways.cytoscapeapp.internal.webclient;

import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import org.pathvisio.core.model.Pathway;

import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.internal.CyActivator;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToNetwork;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToPathway;
import org.wikipathways.cytoscapeapp.internal.io.GpmlVizStyle;

public class WPCyGUIClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
  final String PATHWAY_IMG = getClass().getResource("/pathway.png").toString();
  final String NETWORK_IMG = getClass().getResource("/network.png").toString();

  final JTextField searchField = new JTextField();
  final JCheckBox speciesCheckBox = new JCheckBox("Only: ");
  final JComboBox speciesComboBox = new JComboBox();
  final JButton searchButton = new JButton("Search");
  final PathwayRefsTableModel tableModel = new PathwayRefsTableModel();
  final JTable resultsTable = new JTable(tableModel);
  final JRadioButton pathwayButton = new JRadioButton("<html>Pathway<br><br><img src=\"" + PATHWAY_IMG + "\"></html>", true);
  final JRadioButton networkButton = new JRadioButton("<html>Network<br><br><img src=\"" + NETWORK_IMG + "\"></html>", false);
  final WPClient client;

  public WPCyGUIClient(final WPClient client) {
    super("http://www.wikipathways.org", "WikiPathways", "WikiPathways");
    this.client = client;

    speciesCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        speciesComboBox.setEnabled(speciesCheckBox.isSelected());
      }
    });

    speciesCheckBox.setSelected(false);
    speciesComboBox.setEnabled(false);

    final ActionListener performSearch = new SearchForPathways();
    searchButton.addActionListener(performSearch);
    searchField.addActionListener(performSearch);

    resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
    resultsTable.addMouseListener(new LoadPathway());

    final JPanel resultsPanel = newResultsPanel();

    super.gui = new JPanel(new GridBagLayout());
    EasyGBC c = new EasyGBC();
    super.gui.add(searchField, c.expandHoriz().insets(0, 10, 5, 0));
    super.gui.add(speciesCheckBox, c.noExpand().right().insets(0, 5, 5, 0));
    super.gui.add(speciesComboBox, c.right().insets(0, 0, 5, 5));
    super.gui.add(searchButton, c.right().insets(0, 0, 5, 10));
    super.gui.add(resultsPanel, c.down().expandBoth().spanHoriz(4).insets(0, 10, 10, 10));

    final ResultTask<List<String>> speciesTask = client.newSpeciesTask();
    CyActivator.taskMgr.execute(new TaskIterator(speciesTask, new PopulateSpecies(speciesTask)));
  }

  private JPanel newResultsPanel() {
    final EasyGBC c = new EasyGBC();

    pathwayButton.setVerticalTextPosition(JRadioButton.TOP);
    networkButton.setVerticalTextPosition(JRadioButton.TOP);
    final ButtonGroup group = new ButtonGroup();
    group.add(pathwayButton);
    group.add(networkButton);

    final JPanel importPanel = new JPanel(new GridBagLayout());
    importPanel.add(new JLabel("Import as:"), c.spanHoriz(2).anchor("west").insets(10, 0, 10, 0));
    importPanel.add(pathwayButton, c.noSpan().down().insets(0, 0, 0, 20));
    importPanel.add(networkButton, c.right().noInsets());

    final JPanel resultsPanel = new JPanel(new GridBagLayout());
    resultsPanel.add(new JScrollPane(resultsTable), c.reset().expandBoth());
    resultsPanel.add(importPanel, c.anchor("west").noExpand().down());
    return resultsPanel;
  }

  public TaskIterator createTaskIterator(Object query) {
    return new TaskIterator();
  }

  class PopulateSpecies extends AbstractTask {
    final ResultTask<List<String>> speciesTask;
    public PopulateSpecies(final ResultTask<List<String>> speciesTask) {
      this.speciesTask = speciesTask;
    }
    public void run(TaskMonitor monitor) throws Exception {
      final List<String> allSpecies = speciesTask.get();
      monitor.setTitle("Obtain list of species from WikiPathways");
      for (final String species : allSpecies)
        speciesComboBox.addItem(species);
    }
  }

  class SearchForPathways implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      final String query = searchField.getText();
      final String species = speciesCheckBox.isSelected() ? speciesComboBox.getSelectedItem().toString() : null;
      final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
      CyActivator.taskMgr.execute(new TaskIterator(searchTask, new AbstractTask() {
        public void run(final TaskMonitor monitor) {
          tableModel.setPathwayRefs(searchTask.get());
        }
      }));
    }
  }

  class LoadPathway extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() != 2)
        return;
      final WPPathway pathway = tableModel.getSelectedPathwayRef();
      final ResultTask<InputStream> loadPathwayTask = client.newLoadPathwayTask(pathway);
      final LoadPathwayFromStreamTask fromStreamTask = new LoadPathwayFromStreamTask(loadPathwayTask);
      final TaskIterator taskIterator = new TaskIterator(loadPathwayTask, fromStreamTask);
      CyActivator.taskMgr.execute(taskIterator);
    }
  }

  class LoadPathwayFromStreamTask extends AbstractTask {
    final ResultTask<InputStream> streamTask;
    InputStream gpmlStream = null;

    public LoadPathwayFromStreamTask(final ResultTask<InputStream> streamTask) {
      this.streamTask = streamTask;
    }

    public void cancel() {
      final InputStream gpmlStream2 = gpmlStream;
      if (gpmlStream2 != null) {
        try {
          gpmlStream2.close();
        } catch (Exception e) {}
      }
    }

    public void run(final TaskMonitor monitor) throws Exception {
      gpmlStream = streamTask.get();

      monitor.setStatusMessage("Parsing pathways file");
      final Pathway pathway = new Pathway();
      pathway.readFromXml(gpmlStream, true);
      gpmlStream = null;

      monitor.setStatusMessage("Constructing network");
      final String name = pathway.getMappInfo().getMapInfoName();
      final CyNetworkView view = newNetwork(name);

      if (pathwayButton.isSelected()) {
        (new GpmlToPathway(pathway, view)).convert();
      } else {
       (new GpmlToNetwork(pathway, view)).convert();
        CyLayoutAlgorithm layout = CyActivator.layoutMgr.getLayout("force-directed");
        insertTasksAfterCurrentTask(layout.createTaskIterator(view, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null));
      }
      updateNetworkView(view);
    }
  }

  private static CyNetworkView newNetwork(final String name) {
    final CyNetwork net = CyActivator.netFactory.createNetwork();
    net.getRow(net).set(CyNetwork.NAME, name);
    CyActivator.netMgr.addNetwork(net);
    final CyNetworkView view = CyActivator.netViewFactory.createNetworkView(net);
    CyActivator.netViewMgr.addNetworkView(view);
    return view;
  }

  private static void updateNetworkView(final CyNetworkView netView) {
    GpmlVizStyle.get().apply(netView);
    netView.fitContent();
    netView.updateView();
  }

  class PathwayRefsTableModel extends AbstractTableModel {
    List<WPPathway> pathwayRefs = null;

    public void setPathwayRefs(List<WPPathway> pathwayRefs) {
      this.pathwayRefs = pathwayRefs;
      super.fireTableDataChanged();
    }

    public int getRowCount() {
      return (pathwayRefs == null) ? 0 : pathwayRefs.size();
    }

    public int getColumnCount() {
      return 2;
    }

    public Object getValueAt(int row, int col) {
      final WPPathway pathwayRef = pathwayRefs.get(row);
      switch(col) {
        case 0: return pathwayRef.getName();
        case 1: return pathwayRef.getSpecies();
        default: return null;
      }
    }

    public String getColumnName(int col) {
      switch(col) {
        case 0: return "Pathway";
        case 1: return "Species";
        default: return null;
      }
    }

    public boolean isCellEditable(int row, int col) {
      return false;
    }

    public WPPathway getSelectedPathwayRef() {
      final int row = resultsTable.getSelectedRow();
      if (row < 0)
        return null;
      return pathwayRefs.get(row);
    }
  }
}
