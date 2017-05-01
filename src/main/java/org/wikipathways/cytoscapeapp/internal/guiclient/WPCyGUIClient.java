package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.ImageObserver;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;

public class WPCyGUIClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
  static final Pattern WP_ID_REGEX = Pattern.compile("WP\\d+");
  static final String APP_DESCRIPTION
    = "<html>"
    + "This REVISED app imports community-curated pathways from "
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

  final TaskManager taskMgr;
  final WPClient client;
  final OpenBrowser openBrowser;
  final GpmlReaderFactory gpmlReaderFactory;

  final JTextField searchField = new JTextField();
  final JButton searchButton = new JButton(new ImageIcon(getClass().getResource("/search-icon.png")));
  final JCheckBox speciesCheckBox = new JCheckBox("Only: ");
  final JComboBox speciesComboBox = new JComboBox();
  final PathwayRefsTableModel tableModel = new PathwayRefsTableModel();
  final JTable resultsTable = new JTable(tableModel);
  final JLabel noResultsLabel = new JLabel();
//  final SplitButton importButton = new SplitButton("Import as Pathway");
  final JButton importPathwayButton = new JButton("Import as Pathway");
  final JButton importNetworkButton = new JButton("Import as Network");
  final JButton openUrlButton = new JButton("Open in Web Browser");
  final JToggleButton previewButton = new JToggleButton("Preview \u2192");
  final ImagePreview imagePreview = new ImagePreview();
  final JSplitPane resultsPreviewPane = new JSplitPane();
  double lastDividerPosition = 0.35;
//  final CheckMarkMenuItem pathwayMenuItem = new CheckMarkMenuItem("Pathway", PATHWAY_IMG, true);
//  final CheckMarkMenuItem networkMenuItem = new CheckMarkMenuItem("Network", NETWORK_IMG);

  public WPCyGUIClient(
      final TaskManager taskMgr,
      final WPClient client,
      final OpenBrowser openBrowser,
      final GpmlReaderFactory gpmlReaderFactory) {
    super("http://www.wikipathways.org", "WikiPathways", APP_DESCRIPTION);
    this.taskMgr = taskMgr;
    this.client = client;
    this.openBrowser = openBrowser;
    this.gpmlReaderFactory = gpmlReaderFactory;

    speciesCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        speciesComboBox.setEnabled(speciesCheckBox.isSelected());
      }
    });

    speciesCheckBox.setSelected(false); speciesCheckBox.setVisible(false);
    speciesComboBox.setEnabled(false); 	speciesComboBox.setVisible(false);
    speciesComboBox.setMaximumRowCount(30);
    noResultsLabel.setVisible(false);
    noResultsLabel.setForeground(new Color(0x802020));

    resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
    resultsTable.getSelectionModel().addListSelectionListener(new SharedListSelectionHandler());
    

    resultsTable.requestFocusInWindow();
    
    resultsTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
          if (previewButton.isSelected())    updatePreview();
        } 
        else if (e.getClickCount() == 2)     loadSelectedPathway(GpmlConversionMethod.PATHWAY);
      }
    });
    importPathwayButton.setToolTipText("Import pathway annotations and labels");
    importPathwayButton.addActionListener(new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) { loadSelectedPathway(GpmlConversionMethod.PATHWAY); }
	});
    importNetworkButton.setToolTipText("Import nodes only");
    importNetworkButton.addActionListener(new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) { loadSelectedPathway(GpmlConversionMethod.NETWORK); }
	});
    final JPanel searchPanel = newSearchPanel();
    final JPanel resultsPanel = newResultsPanel();

    super.gui = new JPanel(new GridBagLayout());
    EasyGBC c = new EasyGBC();
    super.gui.add(searchPanel, c.expandHoriz());
    super.gui.add(resultsPanel, c.down().expandBoth().insets(0, 10, 10, 10));

    final ResultTask<List<String>> speciesTask = client.newSpeciesTask();
    taskMgr.execute(new TaskIterator(speciesTask, new PopulateSpecies(speciesTask)));
  }
//
  

  class SharedListSelectionHandler implements ListSelectionListener {
      public void valueChanged(ListSelectionEvent e) { 
          ListSelectionModel lsm = (ListSelectionModel)e.getSource();

          int firstIndex = e.getFirstIndex();
          int lastIndex = e.getLastIndex();
          boolean isAdjusting = e.getValueIsAdjusting(); 
      	updatePreview(); 
      }
  }
  private JPanel newResultsPanel() {
    final EasyGBC c = new EasyGBC();
//
//    pathwayMenuItem.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        pathwayMenuItem.setSelected(true);
//        networkMenuItem.setSelected(false);
//        importButton.setText("Import as Pathway");
//      }
//    });
//
//    networkMenuItem.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        pathwayMenuItem.setSelected(false);
//        networkMenuItem.setSelected(true);
//        importButton.setText("Import as Network");
//      }
//    });
//
//    final JPopupMenu menu = new JPopupMenu();
//    menu.add(pathwayMenuItem);
//    menu.addSeparator();
//    menu.addSeparator();
//    menu.add(networkMenuItem);

//    importButton.setMenu(menu);
//    importButton.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        loadSelectedPathway();
//      }
//    });
//    importPathwayButton.setEnabled(false);
//    importNetworkButton.setEnabled(false);

    openUrlButton.setEnabled(false);
    openUrlButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final WPPathway pathway = tableModel.getSelectedPathwayRef();
        openBrowser.openURL(pathway.getUrl());
      }
    });

    previewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (previewButton.isSelected())    	openPreview();
        else           						closePreview();
      }
    });
    previewButton.setEnabled(false);

    final JPanel leftButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    leftButtonsPanel.add(importPathwayButton);
    leftButtonsPanel.add(importNetworkButton);
    leftButtonsPanel.add(openUrlButton);

    final JPanel rightButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    rightButtonsPanel.add(previewButton);

    imagePreview.setVisible(false);

    final JPanel buttonsPanel = new JPanel(new GridBagLayout());
    buttonsPanel.add(leftButtonsPanel, c.reset().expandHoriz());
    buttonsPanel.add(rightButtonsPanel, c.right());

    final JPanel resultsPanel = new JPanel(new GridBagLayout());
    resultsPanel.add(noResultsLabel, c.reset().expandHoriz());
    resultsPanel.add(new JScrollPane(resultsTable), c.down().expandBoth());

    resultsPreviewPane.setLeftComponent(resultsPanel);
    resultsPreviewPane.setRightComponent(imagePreview);
    resultsPreviewPane.setDividerLocation(1.0);
    resultsPreviewPane.setResizeWeight(0.25);
    resultsPreviewPane.setEnabled(false);

    final JPanel bottomPanel = new JPanel(new GridBagLayout());
    bottomPanel.add(resultsPreviewPane, c.reset().expandBoth());
    bottomPanel.add(buttonsPanel, c.expandHoriz().down());
    return bottomPanel;
  }

  private JPanel newSearchBar() {
    final ActionListener performSearch = new SearchForPathways();

    searchButton.setBorder(BorderFactory.createEmptyBorder());
    searchButton.setContentAreaFilled(false);
    searchButton.addActionListener(performSearch);

    searchField.addActionListener(performSearch);
    searchField.setOpaque(false);
    searchField.setBorder(BorderFactory.createEmptyBorder());

    final JPanel searchBar = new JPanel(new GridBagLayout());
    searchBar.setBorder(new SearchBarBorder());
    final EasyGBC e = new EasyGBC();
    searchBar.add(searchField, e.expandHoriz().insets(6, 12, 6, 0));
    searchBar.add(searchButton, e.noExpand().right().insets(6, 8, 6, 8));

    return searchBar;
  }

  private JPanel newSearchPanel() {
    final JPanel searchBar = newSearchBar();
    final JPanel searchPanel = new JPanel(new GridBagLayout());
    EasyGBC c = new EasyGBC();
    searchPanel.add(searchBar, c.expandHoriz().insets(0, 10, 5, 10));
    searchPanel.add(speciesCheckBox, c.noExpand().right().insets(0, 0, 5, 0));
    searchPanel.add(speciesComboBox, c.right().insets(0, 0, 5, 10));
    return searchPanel;
  }

  public TaskIterator createTaskIterator(Object query) {    return new TaskIterator();  }

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
//        importPathwayButton.setEnabled(false);
//        importNetworkButton.setEnabled(false);
      openUrlButton.setEnabled(false);
      previewButton.setSelected(false);
      previewButton.setEnabled(false);
      closePreview();
    } else {
    	importPathwayButton.setEnabled(true);
    	importNetworkButton.setEnabled(true);
    	openUrlButton.setEnabled(true);
    	previewButton.setEnabled(true);
    	resultsTable.setRowSelectionInterval(0, 0);
    	if (previewButton.isSelected()) 
    		updatePreview();
    }
  }

  void performSearch(final String query, final String species) {
//    searchField.setEnabled(false);
//    searchButton.setEnabled(false);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
        taskMgr.execute(new TaskIterator(searchTask, new AbstractTask() {
          public void run(final TaskMonitor monitor) {
            final List<WPPathway> results = searchTask.get();
            if (results.isEmpty()) {
              noResultsLabel.setText(String.format("<html><b>No results for \'%s\'.</b></html>", query));
              noResultsLabel.setVisible(true);
            } else  noResultsLabel.setVisible(false);
            
            setPathwaysInResultsTable(results);
          }
        }), new TaskObserver() {
          public void taskFinished(ObservableTask t) {}
          public void allFinished(FinishStatus status) {
            searchField.setEnabled(true);
            searchButton.setEnabled(true);
          }
        });
      }
    });
  }
  //----------------------------------------------------------------------
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
    }), new TaskObserver() {
      public void taskFinished(ObservableTask t) {}
      public void allFinished(FinishStatus status) {
        searchField.setEnabled(true);
        searchButton.setEnabled(true);
      }
    });
  }

  //----------------------------------------------------------------------
  void openPreview() {
    resultsPreviewPane.setEnabled(true);
    resultsPreviewPane.setDividerLocation(lastDividerPosition);
    previewButton.setText("Preview \u2190");
    imagePreview.setVisible(true);
    updatePreview();
  }

  void closePreview() {
    lastDividerPosition = ((double) resultsPreviewPane.getDividerLocation()) / (resultsPreviewPane.getWidth() - resultsPreviewPane.getDividerSize());
    resultsPreviewPane.setDividerLocation(1.0);
    resultsPreviewPane.setEnabled(false);
    previewButton.setText("Preview \u2192");
    imagePreview.clearImage();
    imagePreview.setVisible(false);
  }

  void updatePreview() {
    final WPPathway pathway = tableModel.getSelectedPathwayRef();
    if (pathway == null) {
      imagePreview.clearImage();
    } else {
      imagePreview.setImage("http://www.wikipathways.org//wpi/wpi.php?action=downloadFile&type=png&pwTitle=Pathway:" + pathway.getId());
    }
  }

  //----------------------------------------------------------------------
  void loadSelectedPathway(final GpmlConversionMethod method) {
	  System.out.println("\n\nloadSelectedPathway");
//	  importPathwayButton.setEnabled(false);
//	    importNetworkButton.setEnabled(false);
//    resultsTable.setEnabled(false);
//    SwingUtilities.invokeLater(new Runnable() { // wrap in a invokeLater() to let the setEnabled calls above take effect
//      public void run() {
        final WPPathway pathway = tableModel.getSelectedPathwayRef();
        final ResultTask<Reader> loadPathwayTask = client.newGPMLContentsTask(pathway);
//  	  System.out.println("loadPathwayTask");

//        final GpmlConversionMethod method = pathwayMenuItem.isSelected() ? GpmlConversionMethod.PATHWAY : GpmlConversionMethod.NETWORK;
        final TaskIterator taskIterator = new TaskIterator(loadPathwayTask);
        taskIterator.append(new AbstractTask() {
          public void run(TaskMonitor monitor) {
            super.insertTasksAfterCurrentTask(gpmlReaderFactory.createReaderAndViewBuilder(loadPathwayTask.get(), method));
          }
        });
    	  System.out.println("append");
       taskMgr.execute(taskIterator, new TaskObserver() {
          public void taskFinished(ObservableTask t) {}
          public void allFinished(FinishStatus status) {
//              importPathwayButton.setEnabled(true);
//              importNetworkButton.setEnabled(true);
//            resultsTable.setEnabled(true);
          	  System.out.println("allFinished");
        }
        });
 	  System.out.println("execute");

//      }
//    });
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

class SearchBarBorder extends AbstractBorder {
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

//class SplitButton extends JButton {
//  static final int GAP = 5;
//  final JLabel mainText;
//  volatile boolean actionListenersEnabled = true;
//  JPopupMenu menu = null;
//
//  public SplitButton(final String text) {
//    mainText = new JLabel(text);
//    final JLabel menuIcon = new JLabel("\u25be");
//    super.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
//    super.add(mainText);
//    super.add(Box.createRigidArea(new Dimension(GAP, 0)));
//    super.add(new JSeparator(JSeparator.VERTICAL));
//    super.add(Box.createRigidArea(new Dimension(GAP, 0)));
//    super.add(menuIcon);
//
//    super.addMouseListener(new MouseAdapter() {
//      public void mousePressed(final MouseEvent e) {
//        if (!SplitButton.this.isEnabled())
//          return;
//        final int x = e.getX();
//        final int w = e.getComponent().getWidth();
//        if (x >= (2 * w / 3)) {
//          actionListenersEnabled = false;
//          if (menu != null) {
//            menu.show(e.getComponent(), e.getX(), e.getY());
//          }
//        } else {
//          actionListenersEnabled = true;
//        }
//      }
//    });
//  }
//
//  public void setText(final String label) {
//    mainText.setText(label);
//  }
//
//  protected void fireActionPerformed(final ActionEvent e) {
//    if (actionListenersEnabled) {
//      super.fireActionPerformed(e);
//    }
//  }
//
//  public void setMenu(final JPopupMenu menu) {
//    this.menu = menu;
//  }
//}

//class CheckMarkMenuItem extends JMenuItem {
//  static final String CHECKED_STATE_TEXT_FMT = "<html><font size=\"+1\">\u2714</font> %s<br><img src=\"%s\"></html>";
//  static final String NORMAL_STATE_TEXT_FMT = "<html>%s<br><img src=\"%s\"></html>";
//  final String text;
//  final String imgUrl;
//
//  public CheckMarkMenuItem(final String text, final String imgUrl) {
//    this(text, imgUrl, false);
//  }
//
//  public CheckMarkMenuItem(final String text, final String imgUrl, final boolean state) {
//    this.text = text;
//    this.imgUrl = imgUrl;
//    setSelected(state);
//  }
//
//  public void setSelected(final boolean state) {
//    super.setSelected(state);
//    updateText();
//  }
//
//  private void updateText() {
//    super.setText(String.format(super.isSelected() ? CHECKED_STATE_TEXT_FMT : NORMAL_STATE_TEXT_FMT, text, imgUrl));
//  }
//}

class ImagePreview extends JComponent implements ImageObserver {
 	private static final long serialVersionUID = 1L;
 	ImageIcon img = null;

  public void setImage(final String urlPath) {
    URL url = null;
    try {
      url = new URL(urlPath);
      img = new ImageIcon(new URL(urlPath));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
    super.repaint();
  }

  public void clearImage() {
    img = null;
    super.repaint();
  }

  protected void paintComponent(final Graphics g) {
    if (img != null && img.getImage() != null) {
      final Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

      final int cw = super.getWidth();
      final int ch = super.getHeight();
      final double ca = (double) ch / cw;

      final int iw = img.getIconWidth();
      final int ih = img.getIconHeight();
      final double ia = (double) ih / iw;

      if (cw > iw && ch > ih) {
        final int x = (cw - iw) / 2;
        final int y = (ch - ih) / 2;
        g2d.drawImage(img.getImage(), x, y, null);
      } else if (ca > ia) {
        final int dw = cw;
        final int dh = cw * ih / iw;
        final int x = 0;
        final int y = (ch - dh) / 2;
        g.drawImage(img.getImage(), x, y, dw, dh, null);
      } else { // iw <= ih
        final int dw = iw * ch / ih;
        final int dh = ch;
        final int x = (cw - dw) / 2;
        final int y = 0;
        g.drawImage(img.getImage(), x, y, dw, dh, null);
      }
    }
  }
}