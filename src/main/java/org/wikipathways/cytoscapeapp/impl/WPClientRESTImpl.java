// WikiPathways App for Cytoscape
//
// Copyright 2013-2014 WikiPathways
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wikipathways.cytoscapeapp.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.ProxySelector;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.work.TaskMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wikipathways.cytoscapeapp.WPClient;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.json.JSONArray;
import org.json.JSONObject;

public class WPClientRESTImpl implements WPClient {
	protected static final String BASE_URL = "http://webservice.wikipathways.org/";
	protected static final String NEW_BASE_URL = "https://www.wikipathways.org/json/";


	final private CyApplicationConfiguration appConf;		//  gives access to a species cache file
	final private DocumentBuilder xmlParser;
	final private CloseableHttpClient httpClient;
	final private WPManager manager;
	public WPManager getManager() 	{ return manager;	}
	//----------------------------
	public WPClientRESTImpl(final CyApplicationConfiguration appConf, WPManager mgr) {
		manager = mgr;
		this.appConf = appConf;
		try {
			xmlParser = makeXmlParser();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("Failed to build XML parser", e);
		}
		SystemDefaultRoutePlanner planner = new SystemDefaultRoutePlanner(ProxySelector.getDefault());
		httpClient = HttpClientBuilder.create().setRoutePlanner(planner).build();
	}
	
	protected static DocumentBuilder makeXmlParser() throws ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		final DocumentBuilder builder = factory.newDocumentBuilder();
		return builder;
	}

	//----------------------------
/*
 * We have four different queries of wikipathways
 *  	getSpeciesList -- the list of options to present
 *  freeTextSearch -- instigate a server side search for text found in pathways
 *  pathwayInfo -- build a WPPathway record of pathway, version, revision, etc
 *  pathwayContents -- get the GPML (XML) for one specified pathway
 */
	List<String> species = null;


	public ResultTask<List<String>> getSpeciesListTask() {
        return new ReqTask<List<String>>() {
            protected List<String> checkedRun(final TaskMonitor monitor) throws Exception {
                monitor.setTitle("Retrieve list of organisms from WikiPathways");

                if (species == null) 
                    species = retrieveSpeciesFromCache();
                if (species != null) return species;

                // Fetch the JSON content
                final String jsonString = jsonGet(NEW_BASE_URL+ "listOrganisms.json");
                if (super.cancelled) return null;
                
                // Parse the JSON content
                final JSONObject jsonObject = new JSONObject(jsonString);
                final JSONArray organismArray = jsonObject.getJSONArray("organisms");
                final List<String> species = new ArrayList<>();
                for (int i = 0; i < organismArray.length(); i++) {
                    species.add(organismArray.getString(i));
                }

                storeSpeciesToCache(species);
                return species;
            }
        };
    }
    private String jsonGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();
        return content.toString();
    }

	@Override
    public ResultTask<List<WPPathway>> freeTextSearchTask(final String query, final String species) {
        return new ReqTask<List<WPPathway>>() {
            protected List<WPPathway> checkedRun(final TaskMonitor monitor) throws Exception {
                monitor.setTitle("Search WikiPathways for '" + query + "'");
                final List<WPPathway> result = new ArrayList<>();
                if (query.trim().isEmpty()) return result;

                // Fetch the JSON content
                final String jsonString = jsonGet(NEW_BASE_URL + "findPathwaysByText.json");
                if (super.cancelled) return result;
                if (jsonString == null) return result;

                // Parse the JSON content
                final JSONObject jsonObject = new JSONObject(jsonString);
                final JSONArray pathwayArray = jsonObject.getJSONArray("pathwayInfo");

                for (int i = 0; i < pathwayArray.length(); i++) {
                    final JSONObject pathwayObject = pathwayArray.getJSONObject(i);
                    final WPPathway pathway = parsePathwayInfo(pathwayObject);

                    if (pathway != null) {
                        result.add(pathway);
                    }
                }

                // Filter results based on query and field
                List<WPPathway> filteredResults = filterPathways(result, query, species);
                return filteredResults;
            }
        };
    }
	private List<WPPathway> filterPathways(List<WPPathway> pathways, String query, String species) {
		List<WPPathway> filteredResults = new ArrayList<>();
		String lowerQuery = query.toLowerCase();
	
		for (WPPathway pathway : pathways) {
			boolean matches = false;
	
			// Ensure exact match for 'id' field
			if (pathway.getId().equalsIgnoreCase(query)) {
				matches = true;
			} else {
				// Check other fields for partial matches
				matches = pathway.getName().toLowerCase().contains(lowerQuery) ||
						  pathway.getSpecies().toLowerCase().contains(lowerQuery) ||
						  pathway.getDescription().toLowerCase().contains(lowerQuery) ||
						  pathway.getAuthors().toLowerCase().contains(lowerQuery) ||
						  pathway.getDatanodes().toLowerCase().contains(lowerQuery) ||
						  pathway.getAnnotations().toLowerCase().contains(lowerQuery) ||
						  pathway.getCitedIn().toLowerCase().contains(lowerQuery);
			}
	
			if (species != null && !species.isEmpty()) {
				matches = matches && pathway.getSpecies().equalsIgnoreCase(species);
			}
	
			if (matches) {
				filteredResults.add(pathway);
			}
		}
	
		// Filter again to ensure only exact id match if query is id
		if (query.toLowerCase().startsWith("wp")) {
			filteredResults.removeIf(p -> !p.getId().equalsIgnoreCase(query));
		}
	
		return filteredResults;
	}
	
	public ResultTask<WPPathway> pathwayInfoTask(final String id) {
		return new ReqTask<WPPathway>() {
			protected WPPathway checkedRun(final TaskMonitor monitor) throws Exception {
				monitor.setTitle("Retrieve info for '" + id + "'");
				
				// Fetch the JSON content
				final String jsonString = jsonGet(NEW_BASE_URL + "getPathwayInfo.json");
				if (super.cancelled) return null;
				if (jsonString == null) return null;
	
				// Parse the JSON content
				final JSONObject jsonObject = new JSONObject(jsonString);
				final JSONArray pathwayArray = jsonObject.getJSONArray("pathwayInfo");
	
				for (int i = 0; i < pathwayArray.length(); i++) {
					final JSONObject pathwayObject = pathwayArray.getJSONObject(i);
					if (pathwayObject.getString("id").equalsIgnoreCase(id)) {
						// Create and return the WPPathway object
						return parsePathwayInfo(pathwayObject);
					}
				}
	
				return null; // Return null if no matching pathway is found
			}
		};
	}
	


	private WPPathway parsePathwayInfo(final JSONObject pathwayObject) {
        String id = pathwayObject.optString("id", "");
        String revision = pathwayObject.optString("revision", "");
        String name = pathwayObject.optString("name", "");
        String species = pathwayObject.optString("species", "");
        String url = pathwayObject.optString("url", "");
        String description = pathwayObject.optString("description", "");
        String authors = pathwayObject.optString("authors", "");
        String datanodes = pathwayObject.optString("datanodes", "");
        String annotations = pathwayObject.optString("annotations", "");
        String citedIn = pathwayObject.optString("citedIn", "");

        if (name.isEmpty()) return null;

        return new WPPathway(id, revision, name, species, url, description, authors, datanodes, annotations, citedIn);
    }
    
	public ResultTask<Reader> gpmlContentsTask(final WPPathway pathway) {
		return new ReqTask<Reader>() {
			protected Reader checkedRun(final TaskMonitor monitor) throws Exception {
				String title = "Get \'" + pathway.getName() + "\' from WikiPathways";
//				System.out.println(title);
				monitor.setTitle(title);
				Document doc = null;
				try {
					String url = BASE_URL + "getPathway?pwId=" + pathway.getId();
//					System.out.println(url);
					doc = xmlGet(url, "pwId", pathway.getId(), "revision", "0"); //0 = latest revision  //pathway.getRevision());
				} catch (SAXParseException e) {
					throw new Exception(String.format("'%s' is not available -- invalid GPML", pathway.getName()), e);
				}
				if (super.cancelled)
					return null;

//				NodeList nodes = doc.getChildNodes();
//				for (int i=0; i<nodes.getLength(); i++)
//					System.out.println(nodes.item(i));

				docPeek(doc);
				final Node responseNode = doc.getFirstChild();
				final Node pathwayNode = findChildNode(responseNode, "ns1:pathway");
				final Node gpmlNode = findChildNode(pathwayNode, "ns2:gpml");
				final String gpmlContents = new String(Base64.decodeBase64(gpmlNode.getTextContent()), "UTF-8");
				return new StringReader(gpmlContents);
			}
		};
	}
	//----------------------------
	private File getSpeciesCacheFile() {
		final File confDir = appConf.getAppConfigurationDirectoryLocation(this.getClass());
		if (!confDir.exists())
			if (!confDir.mkdirs())
				return null;

		return new File(confDir, "species-cache");
	}

	private void storeSpeciesToCache(final List<String> species) {
		final File speciesCacheFile = getSpeciesCacheFile();
		if (speciesCacheFile == null)
			return;
		try {
			final FileOutputStream outStream = new FileOutputStream(speciesCacheFile);
			final ObjectOutputStream output = new ObjectOutputStream(outStream);
			output.writeObject(species);
			outStream.close();
		} catch (Exception e) {
			System.err.println("Failed to write species cache");
			e.printStackTrace();
		}
	}

	private List<String> retrieveSpeciesFromCache() {
		final File speciesCacheFile = getSpeciesCacheFile();
		if (speciesCacheFile == null || !speciesCacheFile.exists()) 
			return null;

		try {
			final FileInputStream inStream = new FileInputStream(speciesCacheFile);
			final ObjectInputStream input = new ObjectInputStream(inStream);
			final Object object = input.readObject();
			inStream.close();
			final List<String> result = (List<String>) object;
			return result;
		} catch (Exception e) {
			System.err.println("Failed to read species cache");
			e.printStackTrace();
		}
		return null;
	}

	/****************************************************************************
	 * A convenience class for issuing REST calls.
	 */
	protected abstract class ReqTask<T> extends ResultTask<T> {
		protected volatile boolean cancelled = false;
		protected volatile HttpRequestBase request = null;
		protected volatile CloseableHttpResponse resp = null;
		protected volatile InputStream stream = null;

		protected Document xmlGet(final String url, final String... args) throws IOException, SAXException {
			// build the request
			URI uri = null;
			try {
				final URIBuilder uriBuilder = new URIBuilder(url);
				for (int i = 0; i < args.length; i += 2) {
					uriBuilder.addParameter(args[i], args[i + 1]);
				}
				uri = uriBuilder.build();
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid URL request", e);
			}
			request = new HttpGet(uri);
//			System.out.println(uri);
//			System.out.println(request);
			// issue the request
			try {
				resp = httpClient.execute(request);
//				System.out.println(resp);
				final HttpEntity entity = resp.getEntity();
				final String encoding = entity.getContentEncoding() != null ? entity.getContentEncoding().getValue()
						: null;
				stream = entity.getContent();
				final InputSource inputSource = new InputSource(stream);
				if (encoding != null)
					inputSource.setEncoding(encoding);
				return xmlParser.parse(inputSource);
			} catch (Exception e) {
				if (!cancelled) {
					throw e; // ignore exceptions thrown during cancellation
				}
			} finally {
				request.releaseConnection();
				if (resp != null) 
					resp.close();
				request = null;
				resp = null;
				stream = null;
			}
			return null;
		}

		public void cancel() {
			cancelled = true; // this must be set before calling abort() or
								// close()
			final HttpRequestBase req2 = request; // copy the ref to req so that it
												// doesn't become null when
												// trying to abort it
			if (req2 != null)
				req2.abort();

			final InputStream stream2 = stream;
			if (stream2 != null) {
				try {
					stream2.close();
				} catch (IOException e) {
					// don't bother with the exception when trying to close the
					// stream
				}
			}
		}
	}
	private void docPeek(Document doc)
	{
		elemPeek(doc.getDocumentElement());
	}
	
	private void elemPeek(Node parent )
	{
       try { // get the first element
//        Element element = doc.getDocumentElement();

        // get all child nodes
        NodeList nodes = parent.getChildNodes();

        // print the text content of each child
        for (int i = 0; i < nodes.getLength(); i++) {
        	Node node = nodes.item(i);
           //System.out.println("" +node.getTextContent());
           elemPeek(node);
        }
     } catch (Exception ex) {
        ex.printStackTrace();
     }
	}

	protected static Node findChildNode(final Node parentNode, final String nodeName) {
		final NodeList nodes = parentNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			final Node node = nodes.item(i);
			if (node.getNodeName().equals(nodeName))
				return node;
		}
		return null;
	}
}