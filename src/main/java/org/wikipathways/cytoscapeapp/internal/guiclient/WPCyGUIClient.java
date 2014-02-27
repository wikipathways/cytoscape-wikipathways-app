package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.CardLayout;
import java.awt.Color;
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
import javax.swing.SwingUtilities;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.FinishStatus;

import org.pathvisio.core.model.Pathway;

import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.internal.io.Annots;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToNetwork;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToPathway;
import org.wikipathways.cytoscapeapp.internal.io.GpmlVizStyle;

public class WPCyGUIClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
  final String PATHWAY_IMG = getClass().getResource("/pathway.png").toString();
  final String NETWORK_IMG = getClass().getResource("/network.png").toString();

  final CyEventHelper eventHelper;
  final TaskManager taskMgr;
  final CyNetworkFactory netFactory;
  final CyNetworkManager netMgr;
  final CyNetworkViewFactory netViewFactory;
  final CyNetworkViewManager netViewMgr;
  final CyLayoutAlgorithmManager layoutMgr;
  final Annots annots;
  final GpmlVizStyle vizStyle;
  final WPClient client;

  final JTextField searchField = new JTextField();
  final JCheckBox speciesCheckBox = new JCheckBox("Only: ");
  final JComboBox speciesComboBox = new JComboBox();
  final JButton searchButton = new JButton("Search");
  final PathwayRefsTableModel tableModel = new PathwayRefsTableModel();
  final JTable resultsTable = new JTable(tableModel);
  final JRadioButton pathwayButton = new JRadioButton("<html>Pathway<br><br><img src=\"" + PATHWAY_IMG + "\"></html>", true);
  final JRadioButton networkButton = new JRadioButton("<html>Network<br><br><img src=\"" + NETWORK_IMG + "\"></html>", false);
  final JLabel noResultsLabel = new JLabel();

  public WPCyGUIClient(
      final CyEventHelper eventHelper,
      final TaskManager taskMgr,
      final CyNetworkFactory netFactory,
      final CyNetworkManager netMgr,
      final CyNetworkViewFactory netViewFactory,
      final CyNetworkViewManager netViewMgr,
      final CyLayoutAlgorithmManager layoutMgr,
      final Annots annots,
      final GpmlVizStyle vizStyle,
      final WPClient client) {
    super("http://www.wikipathways.org", "WikiPathways", "WikiPathways");
    this.eventHelper = eventHelper;
    this.taskMgr = taskMgr;
    this.client = client;
    this.netFactory = netFactory;
    this.netMgr = netMgr;
    this.netViewFactory = netViewFactory;
    this.netViewMgr = netViewMgr;
    this.annots = annots;
    this.vizStyle = vizStyle;
    this.layoutMgr = layoutMgr;

    speciesCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        speciesComboBox.setEnabled(speciesCheckBox.isSelected());
      }
    });

    speciesCheckBox.setSelected(false);
    speciesCheckBox.setEnabled(false);
    speciesComboBox.setEnabled(false);

    final ActionListener performSearch = new SearchForPathways();
    searchButton.addActionListener(performSearch);
    searchField.addActionListener(performSearch);

    noResultsLabel.setVisible(false);
    noResultsLabel.setForeground(new Color(0x802020));

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
    taskMgr.execute(new TaskIterator(speciesTask, new PopulateSpecies(speciesTask)));
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
    resultsPanel.add(noResultsLabel, c.reset().expandHoriz());
    resultsPanel.add(new JScrollPane(resultsTable), c.down().expandBoth());
    resultsPanel.add(importPanel, c.anchor("west").noExpand().down());
    return resultsPanel;
  }

  public TaskIterator createTaskIterator(Object query) {
    return new TaskIterator();
  }

  private void searchButtonEnable() {
    searchButton.setText("Search");
    searchButton.setEnabled(true);
  }

  private void searchButtonDisable() {
    searchButton.setText("Searching...");
    searchButton.setEnabled(false);
  }

  private void executeLater(final TaskIterator iterator, final TaskObserver observer) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        taskMgr.execute(iterator, observer);
      }
    });
  }

  class PopulateSpecies extends AbstractTask {
    final ResultTask<List<String>> speciesTask;
    public PopulateSpecies(final ResultTask<List<String>> speciesTask) {
      this.speciesTask = speciesTask;
    }
    public void run(TaskMonitor monitor) throws Exception {
      final List<String> allSpecies = speciesTask.get();
      for (final String species : allSpecies)
        speciesComboBox.addItem(species);
      speciesCheckBox.setEnabled(true);
    }
  }

  class SearchForPathways implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      searchButtonDisable();
      final String query = searchField.getText();
      final String species = speciesCheckBox.isSelected() ? speciesComboBox.getSelectedItem().toString() : null;
      final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
      executeLater(new TaskIterator(searchTask, new AbstractTask() {
        public void run(final TaskMonitor monitor) {
          final List<WPPathway> results = searchTask.get();
          if (results.isEmpty()) {
            noResultsLabel.setText(String.format("<html><b>No results for \'%s\'.</b></html>", query));
            noResultsLabel.setVisible(true);
          } else {
            noResultsLabel.setVisible(false);
          }
          tableModel.setPathwayRefs(results);
        }
      }),
      new TaskObserver() {
        public void allFinished(final FinishStatus status) {
          searchButtonEnable();
        }

        public void taskFinished(final ObservableTask task) {}
      });
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
      taskMgr.execute(taskIterator);
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
      try {
        pathway.readFromXml(gpmlStream, true);
      } catch (Exception e) {
        throw new Exception("Pathway not available -- invalid GPML", e);
      }
      gpmlStream = null;

      monitor.setStatusMessage("Constructing network");
      final String name = pathway.getMappInfo().getMapInfoName();
      final CyNetworkView view = newNetwork(name);

      if (pathwayButton.isSelected()) {
        (new GpmlToPathway(eventHelper, annots, pathway, view)).convert();
      } else {
       (new GpmlToNetwork(eventHelper, pathway, view)).convert();
        CyLayoutAlgorithm layout = layoutMgr.getLayout("force-directed");
        insertTasksAfterCurrentTask(layout.createTaskIterator(view, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null));
      }
      updateNetworkView(view);
    }
  }

  private CyNetworkView newNetwork(final String name) {
    final CyNetwork net = netFactory.createNetwork();
    net.getRow(net).set(CyNetwork.NAME, name);
    netMgr.addNetwork(net);
    final CyNetworkView view = netViewFactory.createNetworkView(net);
    netViewMgr.addNetworkView(view);
    return view;
  }

  private void updateNetworkView(final CyNetworkView netView) {
    vizStyle.apply(netView);
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
