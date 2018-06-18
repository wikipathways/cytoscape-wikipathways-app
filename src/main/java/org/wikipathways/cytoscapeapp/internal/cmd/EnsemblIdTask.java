package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.wikipathways.cytoscapeapp.internal.cmd.mapping.BridgeDbIdMapper;
import org.wikipathways.cytoscapeapp.internal.cmd.mapping.IdMapping;
import org.wikipathways.cytoscapeapp.internal.cmd.mapping.MappingSource;

//-----------------------------------------------------------------------

public class EnsemblIdTask extends AbstractTask {
   private CyNetwork network;
   private String species;
   private CyServiceRegistrar registrar;
   private CyTable table;
   boolean verbose = false;

	public EnsemblIdTask(final CyNetwork network, final CyServiceRegistrar reg, String organism) {
		this.network = network;
		table = network.getDefaultNodeTable();
		registrar = reg;
		if (verbose)  System.out.println("create EnsemblIdTask");
		species = organism;
	}

	static String ENSEMBL_COLUMN = "Ensembl";

	public void run(TaskMonitor monitor) {
		if (verbose) System.out.println("running EnsemblIdColumnTask " + species);
		if (bridgeDbAvailable()) // ensemblColumn == null &&
			buildIdMapBatch();
	}

	private boolean bridgeDbAvailable() {

		// registrar.getService(null, "");
		return true;
	}

	private void buildIdMapBatch() {
		HashMap<Long, String> map = new HashMap<Long, String>();
		HashMap<Long, String> map2 = new HashMap<Long, String>();
		List<String> sources = new ArrayList<String>();
		CyColumn ensemblColumn  = table.getColumn(ENSEMBL_COLUMN);
		if  (ensemblColumn == null)
			table.createColumn(ENSEMBL_COLUMN, String.class, true);
		if (verbose) System.out.println("\nbuildIdMapBatch\n");
		List<CyRow> rows = table.getAllRows();
		if (rows.isEmpty()) 
		{
			if (verbose) System.out.println("rows.isEmpty() ");
			return;
		}
		CyRow first = rows.get(0);
		String firstSource = first.get("XRefDataSource", String.class);
		if (verbose) System.out.println("firstXRefDataSource: " + firstSource);
//		boolean homogenousSourced = true;
		for (CyRow row : table.getAllRows())
		{
			Long suid = row.get("SUID", Long.class);
			String id = row.get("XRefId", String.class);
			String src = row.get("XRefDataSource", String.class);
			String name = row.get("name", String.class);
//			String wptype = row.get("WP.type", String.class);
			String type = row.get("Type", String.class);
			
			if (suid == null || id == null || src == null) continue;
			if (map.get(suid) != null) continue;
			
//			if (!goodTypes.contains(wptype)) continue;
			
//			homogenousSourced &= src.equals(firstSource);
			String record = id + "\t" + src + "\t" + type  + "\t" + name;
			map.put(suid, record);
			map2.put(suid, type);
			if (!sources.contains(src))
				sources.add(src);
//			System.out.println(suid + ": " + id + "  \t" + src + "\t" + type + "\t" + name);
		}
		String[] goodTypeArray = { "Gene", "GeneProduct", "Rna	", "Protein" };
		List<String> goodTypes = Arrays.asList(goodTypeArray);
//		System.out.println("homogenousSourced: " + homogenousSourced + "\n\n");
//		if (!homogenousSourced)
//		{
			for (String s : sources)
			{
				if (s.equals("Ensembl")) continue;   // no need to translate those
				Set<String> geneset = new HashSet<String>();
				boolean isEmpty = true;
//				System.out.println(s);
				for (Long suid : map.keySet())
				{
					String fields = map.get(suid);
					String wpType = map2.get(suid);
					boolean hit = fields.contains(s) && goodTypes.contains(wpType);
					
//					System.out.println((hit ? "Hit  " : "Miss ") + fields + " contains  " + s);
					if (hit)
					{
						String[] flds = fields.split("\t");
						geneset.add(flds[0]);
						isEmpty = false;
					}
				}
				if (!isEmpty)
				{
					Map<String, String> resultsmap = translateSet(s, geneset);
					if (resultsmap != null)
						applyIdMap(resultsmap);
				}
		}
//	}
//  
}
//-------------------------------------------------------------------
	private Map<String, String> translateSet(String src, Set<String> geneset) {
		
		Map m = new HashMap<String, String>();
		if (verbose) 
		{
			System.out.println( "translateSet " + src + " " + species);
			 for (String a : geneset)
				 System.out.println(a);					
		}

			Set<String> matched_ids = new TreeSet<String>();
			Set<String> unmatched_ids = new TreeSet<String>();
			Map<String, String> res = new HashMap<String, String>();
	 
			try {
				final BridgeDbIdMapper map = new BridgeDbIdMapper();
				MappingSource source = MappingSource.nameLookup(src);
				res = map.map(geneset, source.system(), "En", species, species);
				matched_ids = map.getMatchedIds();
				unmatched_ids = map.getUnmatchedIds();
			} catch (final Exception e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(null, e.getMessage(), "ID Mapping Error", JOptionPane.ERROR_MESSAGE);
					}
				});
			}

//			System.out.println( "translated " + src + " to Ensembl: " + matched_ids.size() + " / " + ( matched_ids.size() + unmatched_ids.size()) + " matches");			
		try
			{
//these calls to registrar all work fine
//	final Font font = registrar.getService(IconManager.class).getIconFont(18.0f);
//	final CyNetworkManager networkManager = registrar.getService(CyNetworkManager.class);
//	TableColumnTaskFactory  factory = registrar.getService(TableColumnTaskFactory.class);
//	TaskFactory  factory3 = registrar.getService(TaskFactory.class);

//about to throw   java.lang.NoClassDefFoundError: org/cytoscape/idmapper/task/MapColumnCommandTask
//	MapColumnCommandTask  factory2 = registrar.getService(MapColumnCommandTask.class);

			
			
			
			
			
				
//				 System.out.println("factory: " + factory);					
				//						 System.out.println("factory2: " + factory2);					
//				if (factory != null)
				{
//					CyColumn col = new CyColumn();
//					factory.createTaskIterator(column)
				}
//				final BridgeDbIdMapper mapp = new BridgeDbIdMapper();
//				 Map<String, IdMapping>  res = mapp.map(builder, s, MappingSource.Ensembl.name(), species, species);
//				 System.out.println("Results");
//				 for (String a : res.keySet())
//					 System.out.println(a + ": " + res.get(a));					
			// System.out.println(a + ": " + res.get(a));
		} catch (Exception e) {
			System.out.println("exception in EnsemblIdColumnTask: " + e.getMessage());
			// e.printStackTrace();
		}
		return res;
	}

	private void applyIdMap(Map<String, String> resultsmap) {
		if (verbose) System.out.println( "applyIdMap " + resultsmap);
		if (resultsmap.size() > 0)
		{
			for (CyRow row : table.getAllRows())
			{
				String orig = row.get("XRefId", String.class);
				if (orig == null) continue;
				String src = row.get("XRefDatasource", String.class);
				String val = null;
				if ("Ensembl".equals(src))
					val = orig;
				else val = resultsmap.get(orig);
				if (val != null)
					row.set("Ensembl", val);
			}
		}
		
	}
}
