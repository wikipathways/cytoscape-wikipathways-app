package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.wikipathways.cytoscapeapp.impl.WPManager;
import org.wikipathways.cytoscapeapp.internal.cmd.mapping.BridgeDbIdMapper;
import org.wikipathways.cytoscapeapp.internal.cmd.mapping.MappingSource;

/**
 * This is used to express one-to-one relationships of biological identifiers 

 * @author Denise Slenter Github:DeniseSl22
 *
 */

//-----------------------------------------------------------------------

public class ChEBIIdTask extends AbstractTask {
//   private CyNetwork network;
   private String species;
   private String prefix;
//   private CyServiceRegistrar registrar;
   private CyTable table;
   boolean verbose = false;
   
	public ChEBIIdTask(final CyNetwork network, final CyServiceRegistrar reg, String organism) {
//		this.network = network;
		table = network.getDefaultNodeTable();
//		registrar = reg;
		if (verbose)  System.out.println("create ChEBIIdTask");
		species = organism;
		prefix = "CHEBI:";
	}

	static String CHEBI_COLUMN = "ChEBI";

	public void run(TaskMonitor monitor) {
		if (verbose) System.out.println("running the ChEBIIdTask " + species);
		if (bridgeDbAvailable()) // chebiColumn == null &&
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
		CyColumn chebiColumn  = table.getColumn(CHEBI_COLUMN);
		if  (chebiColumn == null)
			table.createColumn(CHEBI_COLUMN, String.class, false);
		if (verbose) System.out.println("\nbuildIdMapBatch_ChEBI\n");
		List<CyRow> rows = table.getAllRows();
		if (rows.isEmpty()) 
		{
			if (verbose) System.out.println("rows.isEmpty() ");
			return;
		}
		CyRow first = rows.get(0);
		String firstSource = first.get("XRefDataSource", String.class);
		if (verbose) System.out.println("firstXRefDataSource: " + firstSource);
	
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
			if (verbose) System.out.println(record);
			map.put(suid, record);
			map2.put(suid, type);
			if (!sources.contains(src))
				sources.add(src);
	//			System.out.println(suid + ": " + id + "  \t" + src + "\t" + type + "\t" + name);
		}
		String[] goodTypeArray = { "Metabolite", "Metabolite	" }; //Only convert ChEBI IDs for type Metabolite DataNode
		List<String> goodTypes = Arrays.asList(goodTypeArray);
		for (String s : sources)
		{
//			if (s.equals("ChEBI")) continue;   // no need to translate those, but we can't ignore files that only have ChEBI
			Set<String> metaboliteset = new HashSet<String>();
			boolean isEmpty = true;
			for (Long suid : map.keySet())
			{
				String fields = map.get(suid);
				String wpType = map2.get(suid);
				boolean hit = fields.contains(s) && goodTypes.contains(wpType);
				if (hit)
				{
					String[] flds = fields.split("\t");
					metaboliteset.add(flds[0]);
					isEmpty = false;
				}
			}
			if (!isEmpty)
			{
				Map<String, String> resultsmap = translateSet(s, metaboliteset);
				if (resultsmap != null)
					applyIdMap(resultsmap);
			}
		}
	}
//-------------------------------------------------------------------
	private Map<String, String> translateSet(String src, Set<String> metaboliteset) {
		
		Map<String, String> result = new HashMap<String, String>();
		//Map<String, String> result_CHEBI = new HashMap<String, String>();
		try {
			MappingSource source = MappingSource.nameLookup(src);
			if ("ChEBI".equals(src))
			{
				for (String metabolite : metaboliteset)
					result.put(metabolite,  metabolite); 
			}
			else if (source != null)
			{
				
				final BridgeDbIdMapper map = new BridgeDbIdMapper();
				result = map.map(metaboliteset, source.system(), "Ce", species, species);
				
				//Resolving inconsistency in ChEBI ID system (with our without 'CHEBI:' prefix):

					for (Map.Entry<String, String> entry : result.entrySet()){
						String key = entry.getKey();
						String individual_values = entry.getValue();
						if(individual_values.startsWith(prefix)){continue;}
						else{
							String updated_values = "CHEBI:" + individual_values;
							result.put(key, updated_values);
				    	//}  
					}
				}

			}
		} catch (final NullPointerException e){
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() { 
					String msg = e.getMessage();
					if (msg == null || msg.length() == 0)
						msg = "Unrecognized Source: " + src;
					JOptionPane.showMessageDialog(null, msg, "Mapping Error", JOptionPane.ERROR_MESSAGE); }
			});
		}
		return result;
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
				if ("ChEBI".equals(src))
					val = orig;
				else val = resultsmap.get(orig);
				if (val != null)
					row.set("ChEBI", val);
			}
		}
		
	}
}
