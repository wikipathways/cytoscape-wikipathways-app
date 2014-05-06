package org.wikipathways.cytoscapeapp.impl;

import java.util.ArrayList;
import java.util.List;

import java.io.Reader;
import java.io.InputStream;
import java.io.StringReader;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.NameValuePair;

import org.cytoscape.work.TaskMonitor;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.ResultTask;

public class WPClientRESTImpl implements WPClient {
  protected static final String BASE_URL = "http://www.wikipathways.org/wpi/webservice/webservice.php/";

  final DocumentBuilder xmlParser;
  final HttpClient client;

  protected static DocumentBuilder newXmlParser() throws ParserConfigurationException {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    final DocumentBuilder builder = factory.newDocumentBuilder();
    return builder;
  }
  
  public WPClientRESTImpl() {
    try {
      xmlParser = newXmlParser();
    } catch (ParserConfigurationException e) {
      throw new IllegalArgumentException("Failed to build XML parser", e);
    }
    client = new HttpClient();
  }

  /**
   * A convenience class for issuing cancellable REST calls.
   */
  protected abstract class ReqTask<T> extends ResultTask<T> {
    protected NameValuePair[] makeNameValuePairs(final String[] args) {
      final NameValuePair[] nvPairs = new NameValuePair[args.length / 2];
      for (int i = 0; i < args.length; i += 2) {
        nvPairs[i / 2] = new NameValuePair(args[i], args[i + 1]);
      }
      return nvPairs;
    }

    protected HttpMethodBase req = null;
    protected InputStream stream = null;

    protected Document xmlGet(final String url, final String ... args) throws IOException, SAXException {
      // build our get request
      req = new GetMethod(url);
      req.setQueryString(makeNameValuePairs(args));

      try {
        client.executeMethod(req);
        final String encoding = req.getResponseCharSet();
        stream = req.getResponseBodyAsStream();
        final InputSource inputSource = new InputSource(stream);
        inputSource.setEncoding(encoding);
        return xmlParser.parse(inputSource);
      } finally {
        req.releaseConnection();
        req = null;
        stream = null;
      }  
    }

    public void cancel() {
      final HttpMethodBase req2 = req; // copy the ref to req so that it doesn't become null when trying to abort it
      if (req2 != null) {
        req2.abort();
      }
      final InputStream stream2 = stream;
      if (stream2 != null) {
        try {
          stream2.close();
        } catch (IOException e) {
          // don't bother with the exception when trying to close the stream
        }
      }
    }
  }

  public ResultTask<List<String>> newSpeciesTask() {
    return new ReqTask<List<String>>() {
      protected List<String> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Retrieve list of organisms from WikiPathways");
        final Document doc = xmlGet(BASE_URL + "listOrganisms");
        final Node responseNode = doc.getFirstChild();
        final NodeList organismNodes = responseNode.getChildNodes(); 
        final List<String> result = new ArrayList<String>();
        for (int i = 0; i < organismNodes.getLength(); i++) {
          final Node organismNode = organismNodes.item(i);
          result.add(organismNode.getTextContent());
        }
        return result;
      }
    };
  }

  public ResultTask<List<WPPathway>> newFreeTextSearchTask(final String query, final String species) {
    return new ReqTask<List<WPPathway>>() {
      protected List<WPPathway> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Search WikiPathways for \'" + query + "\'");
        final Document doc = xmlGet(BASE_URL + "findPathwaysByText", "query", query, "species", species == null ? "" : species);
        final Node responseNode = doc.getFirstChild();
        final NodeList resultNodes = responseNode.getChildNodes(); 
        final List<WPPathway> result = new ArrayList<WPPathway>();
        for (int i = 0; i < resultNodes.getLength(); i++) {
          String id = "", revision = "", name = "", species = "", url = "";
          final Node resultNode = resultNodes.item(i);
          final NodeList argNodes = resultNode.getChildNodes();
          for (int j = 0; j < argNodes.getLength(); j++) {
            final Node argNode = argNodes.item(j);
            final String argName = argNode.getNodeName();
            final String argVal = argNode.getTextContent();
            if (argName.equals("ns2:id")) {
              id = argVal;
            } else if (argName.equals("ns2:revision")) {
              revision = argVal;
            } else if (argName.equals("ns2:name")) {
              name = argVal;
            } else if (argName.equals("ns2:species")) {
              species = argVal;
            } else if (argName.equals("ns2:url")) {
              url = argVal;
            }
          }
          result.add(new WPPathway(id, revision, name, species, url));
        }
        return result;
      }
    };
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

  public ResultTask<Reader> newLoadPathwayTask(final WPPathway pathway) {
    return new ReqTask<Reader>() {
      protected Reader checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get \'" + pathway.getName() + "\' from WikiPathways");
        Document doc = null;
        try {
          doc = xmlGet(BASE_URL + "getPathway", "pwId", pathway.getId(), "revision", pathway.getRevision());
        } catch (SAXParseException e) {
          throw new Exception(String.format("'%s' is not available -- invalid GPML", pathway.getName()), e);
        }
        final Node responseNode = doc.getFirstChild();
        final Node pathwayNode = responseNode.getFirstChild(); 
        final Node gpmlNode = findChildNode(pathwayNode, "ns2:gpml");
        final String gpmlContents = gpmlNode.getTextContent();
        return new StringReader(gpmlContents);
      }
    };
  }
}