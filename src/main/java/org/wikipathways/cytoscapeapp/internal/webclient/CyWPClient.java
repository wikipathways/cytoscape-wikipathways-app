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
import org.wikipathways.cytoscapeapp.internal.CyActivator;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToNetwork;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToPathway;
import org.wikipathways.cytoscapeapp.internal.io.GpmlVizStyle;

public class CyWPClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
  final String PATHWAY_IMG = getClass().getResource("/pathway.png").toString();
  final String NETWORK_IMG = getClass().getResource("/network.png").toString();
  static final String[] RESULTS_TABLE_COLUMN_NAMES = {"Pathway Name", "Species"};
  final JTextField searchField = new JTextField();
  final JCheckBox speciesCheckBox = new JCheckBox("Only: ");
  final JComboBox speciesComboBox = new JComboBox();
  final JButton searchButton = new JButton("Search");
  final PathwayRefsTableModel tableModel = new PathwayRefsTableModel();
  final CardLayout cardLayout = new CardLayout();
  final JPanel cardPanel = new JPanel(cardLayout);
  final JLabel loadingLabel = new JLabel();
  final JTable resultsTable = new JTable(tableModel);
  final JRadioButton pathwayButton = new JRadioButton("<html>Pathway<br><br><img src=\"" + PATHWAY_IMG.toString() + "\"></html>", true);
  final JRadioButton networkButton = new JRadioButton("<html>Network<br><br><img src=\"" + NETWORK_IMG.toString() + "\"></html>", false);
  WPClientREST client;

  public CyWPClient() {
    super("http://www.wikipathways.org", "WikiPathways", "WikiPathways");

    try {
      client = new WPClientREST();
    } catch (Exception e) {
      // TODO: log this exception
      client = null;
      return;
    }

    speciesCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        speciesComboBox.setEnabled(speciesCheckBox.isSelected());
      }
    });

    speciesCheckBox.setSelected(false);
    speciesComboBox.setEnabled(false);

    final ActionListener performSearch = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CyActivator.taskMgr.execute(new TaskIterator(new PerformSearch()));
      }
    };
    searchButton.addActionListener(performSearch);
    searchField.addActionListener(performSearch);

    resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
    resultsTable.addMouseListener(new LoadPathway());

    cardPanel.add(newResultsPanel(), "results");
    cardPanel.add(newLoadingPanel(), "loading");

    super.gui = new JPanel(new GridBagLayout());
    EasyGBC c = new EasyGBC();
    super.gui.add(searchField, c.expandHoriz().insets(0, 10, 5, 0));
    super.gui.add(speciesCheckBox, c.noExpand().right().insets(0, 5, 5, 0));
    super.gui.add(speciesComboBox, c.right().insets(0, 0, 5, 5));
    super.gui.add(searchButton, c.right().insets(0, 0, 5, 10));
    super.gui.add(cardPanel, c.down().expandBoth().spanHoriz(4).insets(0, 10, 10, 10));
    cardLayout.show(cardPanel, "results");

    CyActivator.taskMgr.execute(new TaskIterator(new PopulateSpecies()));
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

  private JPanel newLoadingPanel() {
    final JPanel loadingPanel = new JPanel(new GridBagLayout());
    final JPanel innerLoadingPanel = new JPanel(new GridLayout(2, 1));
    loadingLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    loadingPanel.add(loadingLabel);
    final JProgressBar loadingBar = new JProgressBar();
    loadingBar.setIndeterminate(true);
    innerLoadingPanel.add(loadingLabel);
    innerLoadingPanel.add(loadingBar);
    loadingPanel.add(innerLoadingPanel, new GridBagConstraints());
    return loadingPanel;
  }

  public TaskIterator createTaskIterator(Object query) {
    return new TaskIterator();
  }

  abstract class InterruptableTask extends AbstractTask {
    public abstract void interruptableRun(TaskMonitor monitor) throws Exception;

    protected Thread thread = null;
    public void run(TaskMonitor monitor) throws Exception {
      thread = Thread.currentThread();
      interruptableRun(monitor);
      thread = null;
    }

    public void cancel() {
      if (thread != null)
        thread.interrupt();
    }
  }

  class PopulateSpecies extends InterruptableTask {
    public void interruptableRun(TaskMonitor monitor) throws Exception {
      monitor.setTitle("Obtain list of species from WikiPathways");
      for (final String species : client.getSpecies())
        speciesComboBox.addItem(species);
    }
  }

  class PerformSearch extends InterruptableTask {
    public void interruptableRun(TaskMonitor monitor) throws Exception {
      final String query = searchField.getText();
      if (query == null || query.length() == 0)
        return;
      monitor.setTitle(String.format("Search WikiPathways for '%s'", query));

      searchButton.setEnabled(false);
      searchButton.setText("Searching...");

      List<PathwayRef> pathwayRefs = null;
      if (speciesCheckBox.isSelected()) {
        final String species = (String) speciesComboBox.getSelectedItem();
        pathwayRefs = client.freeTextSearch(query, species);
      } else {
        pathwayRefs = client.freeTextSearch(query);
      }

      tableModel.setPathwayRefs(pathwayRefs);

      searchButton.setEnabled(true);
      searchButton.setText("Search");
    }
  }

  class LoadPathway extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() != 2)
        return;
      final TaskIterator taskIterator = new TaskIterator();
      taskIterator.append(new LoadPathwayTask());
      CyActivator.taskMgr.execute(taskIterator);
    }
  }

  class LoadPathwayTask extends InterruptableTask {
    InputStream gpmlStream = null;
    public void interruptableRun(TaskMonitor monitor) throws Exception {
      try {
        innerRun(monitor);
      } catch (Exception e) {
        cleanUIAfterPathwayLoading();
        throw e;
      }
    }

    public void cancel() {
      cleanUIAfterPathwayLoading();
      if (gpmlStream != null) {
        try {
          gpmlStream.close();
        } catch (Exception e) {}
      }
      super.cancel();
    }

    private void innerRun(TaskMonitor monitor) throws Exception {
      final PathwayRef pathwayRef = tableModel.getSelectedPathwayRef();
      if (pathwayRef == null)
        return;
      monitor.setTitle(String.format("Open '%s' from WikiPathways", pathwayRef.getName()));
      setupUIBeforePathwayLoading(pathwayRef.getName());

      monitor.setStatusMessage("Download pathways file");
      gpmlStream = client.loadPathway(pathwayRef);

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
      
      cleanUIAfterPathwayLoading();
    }
  }

  private void setupUIBeforePathwayLoading(final String pathwayName) {
    cardLayout.show(cardPanel, "loading");
    loadingLabel.setText(String.format("Loading '%s'", pathwayName));
  }

  private void cleanUIAfterPathwayLoading() {
    cardLayout.show(cardPanel, "results");
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
    List<PathwayRef> pathwayRefs = null;

    public void setPathwayRefs(List<PathwayRef> pathwayRefs) {
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
      final PathwayRef pathwayRef = pathwayRefs.get(row);
      switch(col) {
        case 0: return pathwayRef.getName();
        case 1: return pathwayRef.getSpecies();
        default: return null;
      }
    }

    public String getColumnName(int col) {
      switch(col) {
        case 0: return "Pathway Name";
        case 1: return "Species";
        default: return null;
      }
    }

    public boolean isCellEditable(int row, int col) {
      return false;
    }

    public PathwayRef getSelectedPathwayRef() {
      final int row = resultsTable.getSelectedRow();
      if (row < 0)
        return null;
      return pathwayRefs.get(row);
    }
  }
}
