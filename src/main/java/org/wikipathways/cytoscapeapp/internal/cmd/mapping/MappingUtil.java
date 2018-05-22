package org.wikipathways.cytoscapeapp.internal.cmd.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

/**
 * Utility methods for Id mapping clients.
 *
 */
public final class MappingUtil {

    /**
     * Adds Object v as trimmed String to List ids.
     *
     * @param ids 	 the list to add to
     * @param v 		 the item to add
     */
    public final static void addCleanedStrValueToList(final List<String> ids, final Object v) 
    {
       if (v instanceof String)
    	   if ((ids != null) && (v != null)) 
    	   {
            String v_str = (String) v;
            if (v_str != null) 
            {
                v_str = v_str.trim();
                if (v_str.length() > 0)    ids.add(v_str);
            }
        }
    }

    /**
     * This is to add mapping results to a table column.
     *
     *
     * @param source_is_list 	true if the the source column has list values
     * @param res 				the mapping result
     * @param table 				the table to use
     * @param column 			the column to add to
     * @param new_column_name 	the name for the new column
     * @param force_single 		to force to use only use one mapped id
     * 
     * @return true if many to one mapping occurred
     */
    public final static boolean fillNewColumn(final boolean source_is_list,
                                              final Map<String, IdMapping> res,
                                              final CyTable table,
                                              final CyColumn column,
                                              final String new_column_name,
                                              final boolean force_single) {
        final List<CyRow> rows = table.getAllRows();
        boolean many_to_one = false;
        if (source_is_list) {
            for (final CyRow row : rows) {
                @SuppressWarnings("unchecked")
                final List<String> in_vals = (List<String>) row.get(column.getName(),
                                                                    column.getType());
                if (in_vals != null) {
                    final TreeSet<String> ts = new TreeSet<String>();
                    for (final Object iv : in_vals) {
                        final String in_val = (String) iv;
                        if ((in_val != null) && (in_val.length() > 0)) {
                            if (res.containsKey(in_val)) {
                                final Set<String> matched = res.get(in_val).getTargetIds();
                                if (!matched.isEmpty()) {
                                    for (final String m : matched) {
                                        if ((m != null) && (m.length() > 0)) {
                                            if (ts.contains(m))      many_to_one = true;
                                            else    ts.add(m);
                                        }
                                    }
                                }
                            }
						}
					}
					final List<String> l = new ArrayList<String>(ts);
					if (!l.isEmpty()) 
						row.set(new_column_name, (force_single) ? l.get(0) : l);
				}
            }
        }
        else {
            for (final CyRow row : rows) {
                final String in_val = (String) row.get(column.getName(),
                                                       column.getType());
                if ((in_val != null) && (in_val.length() > 0)) {
                    if (res.containsKey(in_val)) {
                        final Set<String> matched = res.get(in_val).getTargetIds();
                        if (!matched.isEmpty()) {
                            if (force_single) 
                                row.set(new_column_name,  matched.iterator().next());
                            else {
                                final TreeSet<String> ts = new TreeSet<String>();
                                for (final String m : matched) {
                                    if ((m != null) && (m.length() > 0)) {
                                        if (ts.contains(m))    many_to_one = true;
                                        else                   ts.add(m);
                                        
                                    }
                                }
                                final List<String> l = new ArrayList<String>(ts);
                                row.set(new_column_name, l);
                            }
                        }
                    }
                }
            }
        }
        return many_to_one;
    }

    /**
     * To determine if all mappings are one/many-to-one
     *
     * @param source_is_list    	true if the the source column has list values
     * @param res 				the mapping result
     * @param column 			the column to add to
     * @param table 				the table to use
     * @return 					true if all mappings are one/many-to-one
     */
    @SuppressWarnings("unchecked")
    public final static boolean isAllSingle(final boolean source_is_list,
                                            final Map<String, IdMapping> res,
                                            final CyColumn column,
                                            final CyTable table) {
        final List<CyRow> rows = table.getAllRows();
        final ArrayList<Set<String>> list = new ArrayList<Set<String>>();
        if (source_is_list) {
            for (final CyRow row : rows) {
                final List<String> in_vals = (List<String>) row.get(column.getName(),
                                                                    column.getType());
                if (in_vals != null) {
                    final TreeSet<String> ts = new TreeSet<String>();
                    for (final Object iv : in_vals) {
                        final String in_val = (String) iv;
                        if ((in_val != null) && (in_val.length() > 0)) {
                            if (res.containsKey(in_val)) {
                                final IdMapping matched = res.get(in_val);
                                if (!matched.getTargetIds().isEmpty()) {
                                    for (final String m : matched.getTargetIds()) {
                                        if ((m != null) && (m.length() > 0)) {
                                            ts.add(m);

                                        }
                                    }
                                }
                            }
                        }
                    }
                    list.add(ts);
                }
            }
        }
        else {
            for (final CyRow row : rows) {
                final String in_val = (String) row.get(column.getName(),
                                                       column.getType());
                if ((in_val != null) && (in_val.length() > 0)) {
                    if (res.containsKey(in_val)) {
                        final Set<String> matched = res.get(in_val).getTargetIds();
                        if (!matched.isEmpty()) {
                            final TreeSet<String> ts = new TreeSet<String>();
                            for (final String m : matched) {
                                if ((m != null) && (m.length() > 0)) 
                                    ts.add(m);
                            }
                            list.add(ts);
                        }
                    }
                }
            }
        }
        boolean all_single = true;
        for (final Set<String> set : list) {
            if (set.size() > 1) {
                all_single = false;
                break;
            }
        }
        return all_single;
    }

    /**
     * To make a new column name, ensuring that it does not match an existing
     * name
     *
     *
     * @param target 		 	the target id type
	 * @param source 			the source id type
     * @param new_column_name 	a suggested new name, can be null or empty
     * @param column 			the source column
     * @return a new column name
     */
    public final static String makeNewColumnName(final String target,
                final String source,final String new_column_name,final CyColumn column) 
    {

        final String my_target = target;
        final String my_source = source;
        String my_col_name;
        if ((new_column_name == null) || (new_column_name.trim().length() < 1)) {
            my_col_name = column.getName() + ": " + my_source + "->" + my_target;
        }
        else {
            my_col_name = new_column_name.trim();
        }
        final CyTable table = column.getTable();
        if (table.getColumn(my_col_name) != null) {
            int counter = 1;
            String new_new_column_name = my_col_name + " (" + counter + ")";
            while (table.getColumn(new_new_column_name) != null) {
                ++counter;
                new_new_column_name = my_col_name + " (" + counter + ")";
            }
            my_col_name = new_new_column_name;
        }
        return my_col_name;
    }

    /**
     * To create a simple message informing the user about the success of the
     * mapping operation.
     *
     */
    public final static String createMsg(final String new_column_name,
                                         final String target, 	final String source,
                                         final List<String> ids, 	final Set<String> matched_ids,
                                         final boolean all_unique,
                                         final int non_unique, 	final int unique,
                                         final int min, 			final int max,
                                         final boolean many_to_one,  final boolean force_single) {
        final String msg;
        String srcTarget = "Mapped: " + source + " -> " + target + "\n" ;
        if (matched_ids.size() < 1) 
        	return "Failed to map any of " +ids.size() + " identifiers.\n" + srcTarget;
		
        String o2o;
		if (all_unique)
			o2o = "All mappings one-to-one." + "\n";
		else {
			String intro = force_single ? 			// Issue #16
					"Some mappings reduced to first value:" : 
					"Not all mappings one-to-one:";	
			
			o2o = intro + "\n" + "  one-to-one: " + unique + "\n" + "  one-to-many: " + non_unique;
			o2o += (min != max) ? " (range: 1-to-" + min + " ~ 1-to-" + max + ")\n" : " (1-to-" + min + ")\n";
		}

		final String m2o = (many_to_one) ? "Same/all mappings many-to-one\n" : "";

		msg = srcTarget + "Successfully mapped " + matched_ids.size() +  " of " + ids.size() + " identifiers.\n" 
				+ o2o + m2o + "New column: " + new_column_name;
        return msg;
    }

}
