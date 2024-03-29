package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

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
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.impl.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.impl.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.impl.ResultTask;
import org.wikipathways.cytoscapeapp.impl.WPPathway;

public class GUI extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient {
//  static final Pattern WP_ID_REGEX = Pattern.compile("WP\\d+");		// AST   was: WP\\d+   "[wW][pP]\\d+"
  static final String APP_DESCRIPTION
    = "<html>"
    + "This app imports community-curated pathways from "
    + "the <a href=\"http://wikipathways.org\">WikiPathways</a> website. "
    + "Pathways can be imported in two ways: "
    + "<ul>"
    + "<li><i>Pathway mode</i>: Complete graphical annotations; "
    + "ideal for custom visualizations of pathways.</li>"
    + "<li><i>Network mode</i>: Simple network without graphical annotations; "
    + "suited for algorithmic analysis. "
    + "</ul>"
    + "This app supports importing GPML files from "
    + "WikiPathways or PathVisio into Cytoscape."
    + "</html>";

private static final boolean VERBOSE = false;

  final String PATHWAY_IMG = getClass().getResource("/pathway.png").toString();
  final String NETWORK_IMG = getClass().getResource("/network.png").toString();

  final TaskManager<?, ?> taskMgr;
  final WPClient client;
  final OpenBrowser openBrowser;
  final GpmlReaderFactory gpmlReaderFactory;

  final JTextField searchField = new JTextField();
  final JButton searchButton = new JButton(new ImageIcon(getClass().getResource("/search-icon.png")));
  final JCheckBox speciesCheckBox = new JCheckBox("Only: ");
  final JComboBox<String> speciesComboBox = new JComboBox<String>();
  final PathwayRefsTableModel tableModel = new PathwayRefsTableModel();
  final JTable resultsTable = new JTable(tableModel);
  public JTable getResultsTable()		{ return resultsTable;	}
  final JLabel noResultsLabel = new JLabel();
//  final SplitButton importButton = new SplitButton("Import as Pathway");
  final JButton importPathwayButton = new JButton("Import as Pathway");
  final JButton importNetworkButton = new JButton("Import as Network");
  final JButton openUrlButton = new JButton("Open in Web Browser");
  final JToggleButton previewButton = new JToggleButton("Preview \u2192");
  final ImagePreview imagePreview = new ImagePreview();
  final JSplitPane resultsPreviewPane = new JSplitPane();
  double lastDividerPosition = 0.25;
//  final CheckMarkMenuItem pathwayMenuItem = new CheckMarkMenuItem("Pathway", PATHWAY_IMG, true);
//  final CheckMarkMenuItem networkMenuItem = new CheckMarkMenuItem("Network", NETWORK_IMG);

  public GUI(
      final TaskManager<?, ?> taskMgr,
      final WPClient client,
      final OpenBrowser openBrowser,
      final GpmlReaderFactory gpmlReaderFactory) {
    super("https://www.wikipathways.org", "WikiPathways", APP_DESCRIPTION);
    this.taskMgr = taskMgr;
    this.client = client;
    this.openBrowser = openBrowser;
    this.gpmlReaderFactory = gpmlReaderFactory;

    speciesCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        boolean selected = speciesCheckBox.isSelected();
        speciesComboBox.setEnabled(selected);
        performSearch();
      }
    });

    speciesComboBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {  performSearch();  }
        });
    
//    speciesCheckBox.setSelected(false); speciesCheckBox.setVisible(false);
//    speciesComboBox.setEnabled(false); 	speciesComboBox.setVisible(false);
    speciesComboBox.setMaximumRowCount(30);
    noResultsLabel.setVisible(false);
    noResultsLabel.setForeground(new Color(0x802020));

    resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
    resultsTable.getSelectionModel().addListSelectionListener(new SharedListSelectionHandler());
    resultsTable.requestFocusInWindow();
    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(resultsTable.getModel());
    resultsTable.setRowSorter(sorter);
    List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>(25);
    sortKeys.add(new RowSorter.SortKey(2, SortOrder.ASCENDING));
    sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
    sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
    sorter.setSortKeys(sortKeys);

    resultsTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
          if (previewButton.isSelected())    updatePreview();
        } 
        else if (e.getClickCount() == 2)     
        	{
        	closeDialog(); 
  	    	loadSelectedPathway(GpmlConversionMethod.PATHWAY);
        	}
      }
    });
    openUrlButton.setToolTipText("View the original pathway on the WikiPathways site");

    importPathwayButton.setToolTipText("Import pathway annotations and labels");
    importPathwayButton.addActionListener(new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) 
		{
			closeDialog();
			loadSelectedPathway(GpmlConversionMethod.PATHWAY); }
	});
    importNetworkButton.setToolTipText("Import nodes only");
    importNetworkButton.addActionListener(new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) 
		{ 
			closeDialog();
			loadSelectedPathway(GpmlConversionMethod.NETWORK); 
		}
	});
    final JPanel searchPanel = makeSearchPanel();
    final JPanel resultsPanel = makeResultsPanel();

    super.gui = new JPanel(new GridBagLayout());
    EasyGridBagConstraints c = new EasyGridBagConstraints();
    super.gui.add(searchPanel, c.expandHoriz());
    super.gui.add(resultsPanel, c.down().expandBoth().insets(0, 10, 10, 10));

    setButtonStates(! resultsTable.getSelectionModel().isSelectionEmpty());
    noResultsLabel.setVisible(false);
    final ResultTask<List<String>> speciesTask = client.getSpeciesListTask();
    taskMgr.execute(new TaskIterator(speciesTask, new PopulateSpecies(speciesTask)));

  }
  static String speciesCache = null;
//----------------------------------------------------------------------
   private JDialog dlogCache = null;
  private void closeDialog()
  {
	  speciesCache = getSpecies();
//	 System.out.println("CloseDialog: " + dlogCache);
	  if (dlogCache != null)
		  dlogCache.dispose();
	  dlogCache = null;
  }

//----------------------------------------------------------------------
 class SharedListSelectionHandler implements ListSelectionListener {
      public void valueChanged(ListSelectionEvent e) { 
//          ListSelectionModel lsm = (ListSelectionModel)e.getSource();
//
//          int firstIndex = e.getFirstIndex();
//          int lastIndex = e.getLastIndex();
//          boolean isAdjusting = e.getValueIsAdjusting(); 
      	updatePreview(); 
      }
  }
//----------------------------------------------------------------------
 private JPanel makeResultsPanel() {
    final EasyGridBagConstraints c = new EasyGridBagConstraints();
    openUrlButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final WPPathway pathway = tableModel.getSelectedPathwayRef();
        openBrowser.openURL(pathway.getUrl());
        closeDialog();
      }
    });

    previewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (previewButton.isSelected())    	openPreview();
        else           						closePreview();
      }
    });

    final JPanel leftButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    leftButtonsPanel.add(importPathwayButton);
    leftButtonsPanel.add(importNetworkButton);
    leftButtonsPanel.add(openUrlButton);

    final JPanel rightButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    rightButtonsPanel.add(previewButton);

    imagePreview.setVisible(false);
//    imagePreview.setBorder(BorderFactory.createLineBorder(Color.green));
    final JPanel buttonsPanel = new JPanel(new GridBagLayout());
    buttonsPanel.add(leftButtonsPanel, c.reset().expandHoriz());
    buttonsPanel.add(rightButtonsPanel, c.right());

    final JPanel resultsPanel = new JPanel(new GridBagLayout());
    resultsPanel.add(noResultsLabel, c.reset().expandHoriz());
    resultsPanel.add(new JScrollPane(resultsTable), c.down().expandBoth());

    resultsPreviewPane.setLeftComponent(resultsPanel);
    resultsPreviewPane.setRightComponent(imagePreview);
    resultsPreviewPane.setDividerLocation(0.25);			// AST panel split
    resultsPreviewPane.setResizeWeight(0.25);
    resultsPreviewPane.setEnabled(false);

    final JPanel bottomPanel = new JPanel(new GridBagLayout());
    bottomPanel.add(resultsPreviewPane, c.reset().expandBoth());
    bottomPanel.add(buttonsPanel, c.expandHoriz().down());
    return bottomPanel;
  }

//----------------------------------------------------------------------
  private JPanel makeSearchBar() {
    final ActionListener performSearch = new SearchForPathways();

    searchButton.setBorder(BorderFactory.createEmptyBorder());
    searchButton.setContentAreaFilled(false);
    searchButton.addActionListener(performSearch);

    searchField.addActionListener(performSearch);
    searchField.setOpaque(false);
    searchField.setBorder(BorderFactory.createEmptyBorder());

    final JPanel searchBar = new JPanel(new GridBagLayout());
    searchBar.setBorder(new SearchBarBorder());
    final EasyGridBagConstraints e = new EasyGridBagConstraints();
    searchBar.add(searchField, e.expandHoriz().insets(6, 12, 6, 0));
    searchBar.add(searchButton, e.noExpand().right().insets(6, 8, 6, 8));

    return searchBar;
  }

//----------------------------------------------------------------------
  public JPanel makeSearchPanel() {
    final JPanel searchBar = makeSearchBar();
    final JPanel searchPanel = new JPanel(new GridBagLayout());
    EasyGridBagConstraints c = new EasyGridBagConstraints();
    searchPanel.add(searchBar, c.expandHoriz().insets(0, 10, 5, 10));
    searchPanel.add(speciesCheckBox, c.noExpand().right().insets(0, 0, 5, 0));
    searchPanel.add(speciesComboBox, c.right().insets(0, 0, 5, 10));
    return searchPanel;
  }

  public TaskIterator createTaskIterator(Object query) {    return new TaskIterator();  }

//----------------------------------------------------------------------
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

//----------------------------------------------------------------------
  class SearchForPathways implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      final String query = searchField.getText();
//      if (WP_ID_REGEX.matcher(query).matches()) 
//    	  		getPathwayFromId(query);
//       else   	
    	   performSearch(query, getSpecies());
    }
  }

//----------------------------------------------------------------------
  public String getSpecies()
  {
	  return speciesCheckBox.isSelected() ? speciesComboBox.getSelectedItem().toString() : null;
  }

//----------------------------------------------------------------------
  public void setPathwaysInResultsTable(final List<WPPathway> pathways) {
		tableModel.setPathwayRefs(pathways);
		resultsTable.getColumnModel().getColumn(2).setMaxWidth(180);
		boolean isEmpty = pathways == null || pathways.size() == 0;
		updateToContent (isEmpty);
  	}
  
  private void updateToContent(boolean isEmpty)
  {
  		setButtonStates(!isEmpty);
  		if (isEmpty)
  			previewButton.setSelected(false);
  		else 
  		{
  			resultsTable.setRowSelectionInterval(0, 0);
  			if (previewButton.isSelected()) 
  				updatePreview();
  		}
  }

	private void setButtonStates(boolean anySelection) {
		importPathwayButton.setEnabled(anySelection);
		importNetworkButton.setEnabled(anySelection);
		openUrlButton.setEnabled(anySelection);
		previewButton.setEnabled(anySelection);
		speciesComboBox.setEnabled(speciesCheckBox.isSelected());
	}

//----------------------------------------------------------------------
  void performSearch()
  {
     final String species = getSpecies();
     final String query = searchField.getText();
     performSearch(query, species);
  }
  
  void performSearch(final String query, final String species) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
//      	  System.out.println("performingSearch");
     
      	  final ResultTask<List<WPPathway>> searchTask = client.freeTextSearchTask(query, species);
      	  taskMgr.execute(new TaskIterator(searchTask, new AbstractTask() {
          public void run(final TaskMonitor monitor) {
            final List<WPPathway> results = searchTask.get();
            
            if (results == null || results.isEmpty()) {
              noResultsLabel.setText(String.format("<html><b>No results for \'%s\'.</b></html>", query));
              noResultsLabel.setVisible(true);
            } 
            else  noResultsLabel.setVisible(false);
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
//  void getPathwayFromId(final String id) {
//    final ResultTask<WPPathway> infoTask = client.pathwayInfoTask(id);
//    taskMgr.execute(new TaskIterator(infoTask, new AbstractTask() {
//      public void run(final TaskMonitor monitor) {
//        final WPPathway pathway = infoTask.get();
//        if (pathway == null) {
//          noResultsLabel.setText(String.format("<html><b>No such pathway \'%s\'.</b></html>", id));
//          noResultsLabel.setVisible(true);
//          setPathwaysInResultsTable(null);
//        } else {
//          noResultsLabel.setVisible(false);
//          setPathwaysInResultsTable(Arrays.asList(pathway));
//        }
//      }
//    }), new TaskObserver() {
//      public void taskFinished(ObservableTask t) {}
//      public void allFinished(FinishStatus status) {
//        searchField.setEnabled(true);
//        searchButton.setEnabled(true);
//      }
//    });
//  }
//
  
  //----------------------------------------------------------------------
  public void bringToFront() {
    Container parent = gui.getParent();
    while (parent != null & !(parent instanceof JRootPane))
    		parent = parent.getParent();
    if (parent instanceof JRootPane)
    {
    		Container contain = ((JRootPane)parent).getParent();
    		if (contain instanceof JFrame)
    			parent.setVisible(true);
    		((JFrame) parent).toFront();
    }
    else System.err.println("Parent not found");
  }
  //----------------------------------------------------------------------
  void openPreview() {
//	  System.out.println("lastDividerPosition read as: " + (int) (100 * lastDividerPosition));
    resultsPreviewPane.setEnabled(true);
    resultsPreviewPane.setDividerLocation(0.25);			//
    previewButton.setText("Preview \u2190");
    imagePreview.setVisible(true);
    updatePreview();
  }

  void closePreview() {
    lastDividerPosition = ((double) resultsPreviewPane.getDividerLocation()) / (resultsPreviewPane.getWidth() - resultsPreviewPane.getDividerSize());
//	  System.out.println("lastDividerPosition stored as: " + (int) (100 * lastDividerPosition));
   resultsPreviewPane.setDividerLocation(1.0);
    resultsPreviewPane.setEnabled(false);
    previewButton.setText("Preview \u2192");
    imagePreview.clearImage();
    imagePreview.setVisible(false);
  }

  void updatePreview() {
    final WPPathway pathway = tableModel.getSelectedPathwayRef();

    if (pathway == null) 
      imagePreview.clearImage();
     else 
     {
   	  	String url = "https://www.wikipathways.org//wpi/wpi.php?action=downloadFile&type=png&pwTitle=Pathway:" + pathway.getId();
   	  	if (VERBOSE) System.out.println("GUI: updatePreview: selected Pathway is " + pathway + ": " + url);
    	Rectangle bounds = imagePreview.getBounds();
    	if (VERBOSE) System.out.println(bounds.width + " x " + bounds.height );
    	imagePreview.setImage(url);
     }

  }

  //----------------------------------------------------------------------
  void loadSelectedPathway(final GpmlConversionMethod method) {
	  	if (VERBOSE) System.out.println("execute loadSelectedPathway");
      final WPPathway pathway = tableModel.getSelectedPathwayRef();
      loadSelectedPathway(method, pathway);
  }     
      
  void loadSelectedPathway(final GpmlConversionMethod method, WPPathway pathway) 
  {
	  gpmlReaderFactory.getWPManager().loadPathway(method, pathway, taskMgr);
  
  }

//----------------------------------------------------------------------
class PathwayRefsTableModel extends AbstractTableModel {
    List<WPPathway> pathwayRefs = null;
	private static final long serialVersionUID = 192L;

    public void setPathwayRefs(List<WPPathway> pathwayRefs) {
      this.pathwayRefs = pathwayRefs;
      super.fireTableDataChanged();
    }

    public int getRowCount() {
      return (pathwayRefs == null) ? 0 : pathwayRefs.size();
    }

    public int getColumnCount() {      return 3;    }

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

    public boolean isCellEditable(int row, int col) {  return false;   }

		public WPPathway getSelectedPathwayRef() {
			int rawRow = resultsTable.getSelectedRow();
			if (rawRow < 0)
				return null;
			final int row = resultsTable.convertRowIndexToModel(rawRow);
			if (row < 0)
				return null;
			return pathwayRefs.get(row);
		}
  }
  //--------------------------------------------------------------
	private JDialog dlog;

	public void displayPathwaysInModal(JFrame parent, String query, List<WPPathway> pathways) {
		EasyGridBagConstraints c = new EasyGridBagConstraints();
		if (dlog == null) {
			dlog = new JDialog(parent, "WikiPathways Search", false);
			JPanel searchPanel = makeSearchPanel();
			JPanel resultsPanel = makeResultsPanel();
			JPanel gui = new JPanel(new GridBagLayout());
			dlog.add(gui);
			gui.add(searchPanel, c.expandHoriz());
			gui.add(resultsPanel, c.down().expandBoth().insets(0, 10, 10, 10));
		}

	 	dlogCache = dlog;	   
	 	searchField.setText(query);
	 	noResultsLabel.setVisible(false); 
	 	if(speciesCache != null)
	 		speciesComboBox.setSelectedItem(speciesCache);
		setPathwaysInResultsTable(pathways);
		dlog.setSize(700, 500);
		dlog.setVisible(true); 
		
	}
}