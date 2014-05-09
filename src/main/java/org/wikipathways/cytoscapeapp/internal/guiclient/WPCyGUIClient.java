package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.SwingUtilities;

import javax.swing.border.AbstractBorder;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.FinishStatus;

import org.cytoscape.util.swing.OpenBrowser;

import org.cytoscape.task.NetworkTaskFactory;

import org.pathvisio.core.model.Pathway;

import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.internal.io.Annots;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToNetwork;
import org.wikipathways.cytoscapeapp.internal.io.GpmlToPathway;
import org.wikipathways.cytoscapeapp.internal.io.GpmlVizStyle;

public class WPCyGUIClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
  static final Pattern WP_ID_REGEX = Pattern.compile("WP\\d+");
  static final String APP_DESCRIPTION
    = "<html>"
    + "This app imports community-curated pathways from "
    + "the <a href=\"http://wikipathways.org\">WikiPathways</a> website. "
    + "Pathways can be imported in two ways: "
    + "<ul>"
    + "<li><i>Pathway mode</i>: Complete graphical annotations; "
    + "ideal for custom visualizations of pathways.</li>"
    + "<li><i>Network mode</i>: Simple network without graphical annotations; "
    + "ideal for algorithmic analysis."
    + "</ul>"
    + "This app also supports importing GPML files from "
    + "WikiPathways or PathVisio into Cytoscape."
    + "</html>";

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
  final NetworkTaskFactory showLODTF;
  final WPClient client;
  final OpenBrowser openBrowser;
  final CyNetworkNaming netNaming;

  final JTextField searchField = new JTextField();
  final JCheckBox speciesCheckBox = new JCheckBox("Only: ");
  final JComboBox speciesComboBox = new JComboBox();
  final PathwayRefsTableModel tableModel = new PathwayRefsTableModel();
  final JTable resultsTable = new JTable(tableModel);
  final JLabel noResultsLabel = new JLabel();
  final SplitButton importButton = new SplitButton("Import as Pathway");
  final JButton openUrlButton = new JButton("Open in Web Browser");
  final CheckMarkMenuItem pathwayMenuItem = new CheckMarkMenuItem("Pathway", PATHWAY_IMG, true);
  final CheckMarkMenuItem networkMenuItem = new CheckMarkMenuItem("Network", NETWORK_IMG);

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
      final NetworkTaskFactory showLODTF,
      final WPClient client,
      final OpenBrowser openBrowser,
      final CyNetworkNaming netNaming) {
    super("http://www.wikipathways.org", "WikiPathways", APP_DESCRIPTION);
    this.eventHelper = eventHelper;
    this.taskMgr = taskMgr;
    this.client = client;
    this.netFactory = netFactory;
    this.netMgr = netMgr;
    this.netViewFactory = netViewFactory;
    this.netViewMgr = netViewMgr;
    this.annots = annots;
    this.vizStyle = vizStyle;
    this.showLODTF = showLODTF;
    this.layoutMgr = layoutMgr;
    this.openBrowser = openBrowser;
    this.netNaming = netNaming;

    speciesCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        speciesComboBox.setEnabled(speciesCheckBox.isSelected());
      }
    });

    speciesCheckBox.setSelected(false);
    speciesCheckBox.setVisible(false);
    speciesComboBox.setEnabled(false);
    speciesComboBox.setVisible(false);

    final JPanel searchPanel = newSearchPanel();

    noResultsLabel.setVisible(false);
    noResultsLabel.setForeground(new Color(0x802020));

    resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
    resultsTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() != 2)
          return;
        loadSelectedPathway();
      }
    });

    final JPanel resultsPanel = newResultsPanel();

    super.gui = new JPanel(new GridBagLayout());
    EasyGBC c = new EasyGBC();
    super.gui.add(searchPanel, c.expandHoriz().insets(0, 10, 5, 10));
    super.gui.add(speciesCheckBox, c.noExpand().right().insets(0, 0, 5, 0));
    super.gui.add(speciesComboBox, c.right().insets(0, 0, 5, 10));
    super.gui.add(resultsPanel, c.down().expandBoth().spanHoriz(3).insets(0, 10, 10, 10));

    final ResultTask<List<String>> speciesTask = client.newSpeciesTask();
    taskMgr.execute(new TaskIterator(speciesTask, new PopulateSpecies(speciesTask)));
  }

  private JPanel newResultsPanel() {
    final EasyGBC c = new EasyGBC();

    pathwayMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pathwayMenuItem.setSelected(true);
        networkMenuItem.setSelected(false);
        importButton.setLabel("Import as Pathway");
      }
    });

    networkMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pathwayMenuItem.setSelected(false);
        networkMenuItem.setSelected(true);
        importButton.setLabel("Import as Network");
      }
    });

    final JPopupMenu menu = new JPopupMenu();
    menu.add(pathwayMenuItem);
    menu.addSeparator();
    menu.add(networkMenuItem);

    importButton.setMenu(menu);
    importButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        loadSelectedPathway();
      }
    });
    importButton.setEnabled(false);

    openUrlButton.setEnabled(false);
    openUrlButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final WPPathway pathway = tableModel.getSelectedPathwayRef();
        openBrowser.openURL(pathway.getUrl());
      }
    });

    final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonsPanel.add(openUrlButton);
    buttonsPanel.add(importButton);

    final JPanel resultsPanel = new JPanel(new GridBagLayout());
    resultsPanel.add(noResultsLabel, c.reset().expandHoriz());
    resultsPanel.add(new JScrollPane(resultsTable), c.down().expandBoth());
    resultsPanel.add(buttonsPanel, c.anchor("northeast").expandHoriz().down());
    return resultsPanel;
  }

  private JPanel newSearchPanel() {
    final ActionListener performSearch = new SearchForPathways();

    final JButton searchButton = new JButton(new ImageIcon(getClass().getResource("/search-icon.png")));
    searchButton.setBorder(BorderFactory.createEmptyBorder());
    searchButton.setContentAreaFilled(false);
    searchButton.addActionListener(performSearch);

    searchField.addActionListener(performSearch);
    searchField.setOpaque(false);
    searchField.setBorder(BorderFactory.createEmptyBorder());

    final JPanel searchPanel = new JPanel(new GridBagLayout());
    searchPanel.setBorder(new SearchPanelBorder());
    final EasyGBC e = new EasyGBC();
    searchPanel.add(searchField, e.expandHoriz().insets(6, 12, 6, 0));
    searchPanel.add(searchButton, e.noExpand().right().insets(6, 8, 6, 8));

    return searchPanel;
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
      for (final String species : allSpecies)
        speciesComboBox.addItem(species);
      speciesCheckBox.setVisible(true);
      speciesComboBox.setVisible(true);
    }
  }

  class SearchForPathways implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      final String query = searchField.getText();
      if (WP_ID_REGEX.matcher(query).matches()) {
        getPathwayFromId(query);
      } else {
        final String species = speciesCheckBox.isSelected() ? speciesComboBox.getSelectedItem().toString() : null;
        performSearch(query, species);
      }
    }
  }

  void setPathwaysInResultsTable(final List<WPPathway> pathways) {
    tableModel.setPathwayRefs(pathways);
    resultsTable.getColumnModel().getColumn(2).setMaxWidth(180);
    if (pathways == null || pathways.size() == 0) {
      importButton.setEnabled(false);
      openUrlButton.setEnabled(false);
    } else {
      importButton.setEnabled(true);
      openUrlButton.setEnabled(true);
      resultsTable.setRowSelectionInterval(0, 0);
    }
  }

  void performSearch(final String query, final String species) {
    final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
    taskMgr.execute(new TaskIterator(searchTask, new AbstractTask() {
      public void run(final TaskMonitor monitor) {
        final List<WPPathway> results = searchTask.get();
        if (results.isEmpty()) {
          noResultsLabel.setText(String.format("<html><b>No results for \'%s\'.</b></html>", query));
          noResultsLabel.setVisible(true);
        } else {
          noResultsLabel.setVisible(false);
        }
        setPathwaysInResultsTable(results);
      }
    }));
  }

  void getPathwayFromId(final String id) {
    final ResultTask<WPPathway> infoTask = client.newPathwayInfoTask(id);
    taskMgr.execute(new TaskIterator(infoTask, new AbstractTask() {
      public void run(final TaskMonitor monitor) {
        final WPPathway pathway = infoTask.get();
        if (pathway == null) {
          noResultsLabel.setText(String.format("<html><b>No such pathway \'%s\'.</b></html>", id));
          noResultsLabel.setVisible(true);
          setPathwaysInResultsTable(null);
        } else {
          noResultsLabel.setVisible(false);
          setPathwaysInResultsTable(Arrays.asList(pathway));
        }
      }
    }));
  }

  void loadSelectedPathway() {
    final WPPathway pathway = tableModel.getSelectedPathwayRef();
    final ResultTask<Reader> loadPathwayTask = client.newGPMLContentsTask(pathway);
    final LoadPathwayFromStreamTask fromStreamTask = new LoadPathwayFromStreamTask(loadPathwayTask);
    final TaskIterator taskIterator = new TaskIterator(loadPathwayTask, fromStreamTask);
    taskMgr.execute(taskIterator);
  }

  class LoadPathwayFromStreamTask extends AbstractTask {
    final ResultTask<Reader> streamTask;
    Reader gpmlStream = null;

    public LoadPathwayFromStreamTask(final ResultTask<Reader> streamTask) {
      this.streamTask = streamTask;
    }

    public void cancel() {
      final Reader gpmlStream2 = gpmlStream;
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

      if (pathwayMenuItem.isSelected()) {
        (new GpmlToPathway(eventHelper, annots, pathway, view)).convert();
      } else {
       (new GpmlToNetwork(eventHelper, pathway, view)).convert();
        CyLayoutAlgorithm layout = layoutMgr.getLayout("force-directed");
        insertTasksAfterCurrentTask(layout.createTaskIterator(view, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null));
      }
      insertTasksAfterCurrentTask(showLODTF.createTaskIterator(view.getModel()));
      insertTasksAfterCurrentTask(new AbstractTask() {
        public void run(TaskMonitor monitor) {
          updateNetworkView(view);
        }

        public void cancel() {}
      });
    }
  }

  private CyNetworkView newNetwork(final String name) {
    final CyNetwork net = netFactory.createNetwork();
    net.getRow(net).set(CyNetwork.NAME, netNaming.getSuggestedNetworkTitle(name));
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
      return 3;
    }

    public Object getValueAt(int row, int col) {
      final WPPathway pathwayRef = pathwayRefs.get(row);
      switch(col) {
        case 0: return pathwayRef.getName();
        case 1: return pathwayRef.getSpecies();
        case 2: return pathwayRef.getId();
        default: return null;
      }
    }

    public String getColumnName(int col) {
      switch(col) {
        case 0: return "Pathway";
        case 1: return "Species";
        case 2: return "ID";
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

class SearchPanelBorder extends AbstractBorder {
  final static float ARC = 25.0f;
  final static Color BORDER_COLOR = new Color(0x909090);
  final static Color BKGND_COLOR = Color.WHITE;
  final static Stroke BORDER_STROKE = new BasicStroke(1.0f);

  final RoundRectangle2D.Float borderShape = new RoundRectangle2D.Float();
  public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
    final Graphics2D g2d = (Graphics2D) g;

    final boolean aa = RenderingHints.VALUE_ANTIALIAS_ON.equals(g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
    final Paint oldPaint = g2d.getPaint();
    final Stroke oldStroke = g2d.getStroke();

    borderShape.setRoundRect((float) x, (float) y, (float) (w - 1), (float) (h - 1), ARC, ARC);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2d.setColor(BKGND_COLOR);
    g2d.fill(borderShape);

    g2d.setColor(BORDER_COLOR);
    g2d.setStroke(BORDER_STROKE);
    g2d.draw(borderShape);

    g2d.setPaint(oldPaint);
    g2d.setStroke(oldStroke);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
  }
}

class SplitButton extends JButton {
  static final int GAP = 5;
  final JLabel mainText;
  volatile boolean actionListenersEnabled = true;
  JPopupMenu menu = null;

  public SplitButton(final String text) {
    mainText = new JLabel(text);
    final JLabel menuIcon = new JLabel("\u25be");
    super.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    super.add(mainText);
    super.add(Box.createRigidArea(new Dimension(GAP, 0)));
    super.add(new JSeparator(JSeparator.VERTICAL));
    super.add(Box.createRigidArea(new Dimension(GAP, 0)));
    super.add(menuIcon);

    super.addMouseListener(new MouseAdapter() {
      public void mousePressed(final MouseEvent e) {
        if (!SplitButton.this.isEnabled())
          return;
        final int x = e.getX();
        final int w = e.getComponent().getWidth();
        if (x >= (2 * w / 3)) {
          actionListenersEnabled = false;
          if (menu != null) {
            menu.show(e.getComponent(), e.getX(), e.getY());
          }
        } else {
          actionListenersEnabled = true;
        }
      }
    });
  }

  public void setLabel(final String label) {
    mainText.setText(label);
  }

  protected void fireActionPerformed(final ActionEvent e) {
    if (actionListenersEnabled) {
      super.fireActionPerformed(e);
    }
  }

  public void setMenu(final JPopupMenu menu) {
    this.menu = menu;
  }
}

class CheckMarkMenuItem extends JMenuItem {
  static final String CHECKED_STATE_TEXT_FMT = "<html><font size=\"+1\"><b>\u2714</b></font> %s<br><img src=\"%s\"></html>";
  static final String NORMAL_STATE_TEXT_FMT = "<html>%s<br><img src=\"%s\"></html>";
  final String text;
  final String imgUrl;

  public CheckMarkMenuItem(final String text, final String imgUrl) {
    this(text, imgUrl, false);
  }

  public CheckMarkMenuItem(final String text, final String imgUrl, final boolean state) {
    this.text = text;
    this.imgUrl = imgUrl;
    setSelected(state);
  }

  public void setSelected(final boolean state) {
    super.setSelected(state);
    updateText();
  }

  private void updateText() {
    super.setText(String.format(super.isSelected() ? CHECKED_STATE_TEXT_FMT : NORMAL_STATE_TEXT_FMT, text, imgUrl));
  }
}