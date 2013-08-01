package org.wikipathways.cytoscapeapp.internal.webclient;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.NameValuePair;

import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * WikiPathways' REST client
 */
class WPClient {
  protected static NameValuePair[] makeNameValuePairs(final String[] args) {
    final NameValuePair[] nvPairs = new NameValuePair[args.length / 2];
    for (int i = 0; i < args.length; i += 2) {
      nvPairs[i / 2] = new NameValuePair(args[i], args[i + 1]);
    }
    return nvPairs;
  }

  protected DocumentBuilder newXmlParser() throws ParserConfigurationException {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    final DocumentBuilder builder = factory.newDocumentBuilder();
    return builder;
  }

  protected final DocumentBuilder xmlParser;

  public WPClient() throws ParserConfigurationException {
    xmlParser = newXmlParser();
  }

  protected static final String BASE_URL = "http://www.wikipathways.org/wpi/webservice/webservice.php/";

  protected Document xmlRequest(final String restMethod, final String ... args) throws IOException, SAXException {
    // build our get request
    final GetMethod method = new GetMethod(BASE_URL + restMethod);
    method.setQueryString(makeNameValuePairs(args));

    // issue get request
    final HttpClient client = new HttpClient();
    client.executeMethod(method);

    // parse response
    return xmlParser.parse(method.getResponseBodyAsStream());
  }

  public List<String> getSpecies() throws IOException, SAXException {
    final Document doc = xmlRequest("listOrganisms");
    final Node responseNode = doc.getFirstChild();
    final NodeList organismNodes = responseNode.getChildNodes(); 
    final List<String> result = new ArrayList<String>();
    for (int i = 0; i < organismNodes.getLength(); i++) {
      final Node organismNode = organismNodes.item(i);
      result.add(organismNode.getTextContent());
    }
    return result;
  }

  public static class PathwayRef {
    final String id;
    final String revision;
    final String name;
    final String species;

    public PathwayRef(final String id, final String revision, final String name, final String species) {
      this.id = id;
      this.revision = revision;
      this.name = name;
      this.species = species;
    }

    public String getId() {
      return id;
    }

    public String getRevision() {
      return revision;
    }

    public String getName() {
      return name;
    }

    public String getSpecies() {
      return species;
    }

    public String toString() {
      return String.format("%s (%s) [%s %s]", name, species, id, revision);
    }
  }

  public List<PathwayRef> freeTextSearch(final String query) throws IOException, SAXException {
    return freeTextSearch(query, "");
  }

  public List<PathwayRef> freeTextSearch(final String query, final String speciesToSearch) throws IOException, SAXException {
    final Document doc = xmlRequest("findPathwaysByText", "query", query, "species", speciesToSearch == null ? "" : speciesToSearch);
    final Node responseNode = doc.getFirstChild();
    final NodeList resultNodes = responseNode.getChildNodes(); 
    final List<PathwayRef> result = new ArrayList<PathwayRef>();
    for (int i = 0; i < resultNodes.getLength(); i++) {
      String id = "", revision = "", name = "", species = "";
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
        }
      }
      result.add(new PathwayRef(id, revision, name, species));
    }
    return result;
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

  public InputStream loadPathway(final PathwayRef ref) throws IOException, SAXException {
    final Document doc = xmlRequest("getPathway", "pwId", ref.getId(), "revision", ref.getRevision());
    final Node responseNode = doc.getFirstChild();
    final Node pathwayNode = responseNode.getFirstChild(); 
    final Node gpmlNode = findChildNode(pathwayNode, "ns2:gpml");
    final String gpmlContents = gpmlNode.getTextContent();
    return new ByteArrayInputStream(gpmlContents.getBytes());
  }
}