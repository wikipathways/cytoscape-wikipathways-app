package org.wikipathways.cytoscapeapp.internal.cmd.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An client for the Id Mapping service BridgeDB.
 *
 * See
 *
 * http://developers.bridgedb.org/wiki/BridgeWebservice
 *
 * http://www.bridgedb.org/swagger/#!/
 *
 * @author cmzmasek
 *
 */

public class BridgeDbIdMapper implements IdMapper {

    public static final String              DEFAULT_MAP_SERVICE_URL_STR = "http://webservice.bridgedb.org";

    public static final boolean             DEBUG                       = true;

    private final String                    _url;
    private Set<String>                     _unmatched_ids;
    private Set<String>                     _matched_ids;

    /**
     * Constructor, takes the URL of the service as parameter
     *
     * @param url
     *            the URL of the service
     */
    public BridgeDbIdMapper(final String url) {       _url = url;    }			// System.out.println("BridgeDbIdMapper " + url);
    public BridgeDbIdMapper() 	{   this(DEFAULT_MAP_SERVICE_URL_STR);    }

    @Override    public Set<String> getUnmatchedIds() {       return _unmatched_ids;    }
    @Override    public Set<String> getMatchedIds() {        return _matched_ids;    }

    @Override
    public Map<String, String> map(final Collection<String> query_ids,
                                      final String source_type,
                                      final String target_type,
                                      final String source_species,
                                      final String target_species) {
        List<String> response = null;
        _matched_ids = new TreeSet<String>();
        _unmatched_ids = new TreeSet<String>();
        try {
//          System.out.println("map: " + target_species + ", " + source_type);
            response = BridgeDbIdMapper.runQuery(query_ids, target_species, "xrefsBatch", source_type,  _url);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        if (response != null) {
//            for (final String l : query_ids) {
//                System.out.println(l);
//            }

            try {
                final Map<String, String> res = parseResponse(response, source_species, source_type, target_species, target_type);
                return res;

            }
            catch (final IOException e) {  e.printStackTrace();      }

        }
        return null;
    }

    public Map<String, IdMapping> mapList(final Collection<String> query_ids,
                                      final List<MappingSource> source_types,
                                      final List<MappingSource> target_types,
                                      final String source_species,
                                      final String target_species) {
         return null;
    }

    @Override
    public Map<String, IdGuess> guess(final Collection<String> query_ids,
                                      final String source_species) {
        return null;
    }

    /**
     * This parses the response (List of String).
     *
     *
     * @param res_list 			to response to be parsed
     * @param source_species 	the source species
     * @param source_type 		the source type
     * @param target_species     the target species
     * @param target_type 		the target type
     * @return the result of the parsing as Map of String to IdMapping
     * @throws IOException
     */
//    final Map<String, String> details = new TreeMap<String, String>();
   private final Map<String, String> parseResponse(final List<String> res_list,
                                                       final String source_species,
                                                       final String source_type,
                                                       final String target_species,
                                                       final String target_type) throws IOException {

        final Map<String, String> res = new TreeMap<String, String>();
        
        for (final String s : res_list) {
            final String[] s1 = s.split("\t");
            if (s1.length != 3) {
                throw new IOException("illegal format: " + s);
            }
//            final IdMappingImpl idmap = new IdMappingImpl();
////            idmap.setTargetSpecies(target_species);
//            idmap.setSourceSpecies(source_species);
//            idmap.setTargetType(MappingSource.systemLookup(target_type));
//            idmap.setSourceType(MappingSource.systemLookup(s1[1]));
//            idmap.addSourceId(s1[0]);

            final String[] s2 = s1[2].split(",");
            String target =  "";
            for (final String s2_str : s2) {
                if ((s2_str != null) && !s2_str.toLowerCase().equals("n/a")) {
                    // System.out.println(s2_str);
                    final String[] tokens = s2_str.split(":", 2);
                    if (tokens.length != 2) 
                        throw new IOException("illegal format: " + s);
                    
                    if (tokens[0].equals(target_type)) 
                        target = tokens[1];   //idmap.addTargetId(s3[1]);
                }
            }
//            System.out.println(idmap);
            if (!target.isEmpty()) {				//idmap.getTargetIds().size() > 0 && 
                res.put(s1[0], target);
                _matched_ids.add(s1[0]);
            }
            else   _unmatched_ids.add(s1[0]);
//            details.put(s1[0], s1[1]);
        }
        return res;
    }

    /**
     * This posts a query to a URL
     *
     * @param url_str 	 	the URL to post to
     * @param species 		the species
     * @param command 		the target type
     * @param database 		the database
     * @param query 			the query
     * @return the response as List of String
     * @throws IOException
     */
    private static final List<String> post(final String url_str,
                                           final String species,
                                           final String command,
                                           final String database,
                                           final String query) throws IOException {
        
    	String link = url_str + "/" + species + "/" + command;
    	if (!"Unspecified".equals(database))
			link += "/" + database;
    	final URL url = new URL(link);
        

//System.out.println("POSTING:  " + url.toString());
//System.out.println(query + "\n\n\n");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        final OutputStream os = conn.getOutputStream();
        os.write(query.getBytes());
        os.flush();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) 
            throw new IOException("HTTP error code : " + conn.getResponseCode());

        final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        final List<String> res = new ArrayList<String>();
        String line;
        while ((line = br.readLine()) != null) 
            res.add(line);

        br.close();
        conn.disconnect();
        os.close();
        return res;
    }

    /**
     * Runs a query against a URL.
     *
     * @param ids
     * @param species
     * @param command
     * @param sourceType
     * @param url_str
     * @return
     * @throws IOException
     */
    private final static List<String> runQuery(final Collection<String> ids,
                                               final String species,
                                               final String command,
                                               final String sourceType,
                                               final String url_str) throws IOException {
        final String query = makeQuery(ids);
        return post(url_str, species, command, sourceType, query);
    }

    /**
     * To make the query String as a list of one id per line
     *
     *
     * @param ids
     * @return
     */
    private final static String makeQuery(final Collection<String> ids) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String id : ids) {
            if (first)   first = false;
            else  sb.append("\n");
            sb.append(id);
        }
        return sb.toString();
    }

    public static void main(final String[] args) throws IOException {
        final Collection<String> ids = new ArrayList<String>();

        ids.add("ENSMUSG00000063455");
        ids.add("ENSMUSG00000073823");
        ids.add("ENSMUSG00000037031");

        final BridgeDbIdMapper map = new BridgeDbIdMapper();

        final String source_type = "En";
        final String target_type = "S";
        final String source_species = "Mouse";
        final String target_species = "Mouse";

        final Map<String, String> x = map.map(ids, source_type,
                                    target_type, source_species, target_species);

//        for (final Entry<String, IdMapping> entry : x.entrySet()) {
//            System.out.println(entry.getKey() + "=>" + entry.getValue());
//        }

    }

}
