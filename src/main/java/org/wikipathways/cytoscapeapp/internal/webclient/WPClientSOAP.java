package org.wikipathways.cytoscapeapp.internal.webclient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.bridgedb.bio.Organism;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.pathvisio.wikipathways.webservice.WSSearchResult;
import org.wikipathways.client.WikiPathwaysClient;

public class WPClientSOAP {

	protected static final String BASE_URL = "http://www.wikipathways.org/wpi/webservice/webservice.php/";
	private WikiPathwaysClient client;

	  
	public WPClientSOAP() throws ServiceException, MalformedURLException {
		client = new WikiPathwaysClient(new URL(BASE_URL));
	}


	public List<String> getSpecies() throws RemoteException {
		String [] organism = client.listOrganisms();		
		return Arrays.asList(organism);
	}
	
	public List<PathwayRef> freeTextSearch(final String query) throws RemoteException {
		WSSearchResult [] res = client.findPathwaysByText(query);
		List<PathwayRef> list = new ArrayList<PathwayRef>();
		for(WSSearchResult r : res) {
			PathwayRef ref = new PathwayRef(r.getId(), r.getRevision(), r.getName(), r.getSpecies());
			list.add(ref);
		}
	    return list;
	  }
	
	public List<PathwayRef> freeTextSearch(String query, String organism) throws RemoteException {
		WSSearchResult [] res = client.findPathwaysByText(query, Organism.valueOf(organism));
		List<PathwayRef> list = new ArrayList<PathwayRef>();
		for(WSSearchResult r : res) {
			PathwayRef ref = new PathwayRef(r.getId(), r.getRevision(), r.getName(), r.getSpecies());
			list.add(ref);
		}
	    return list;
	  }
	
	public InputStream loadPathway(PathwayRef pathwayRef) throws RemoteException, ConverterException {
		WSPathway pathway = client.getPathway(pathwayRef.getId());
		return new ByteArrayInputStream(pathway.getGpml().getBytes());
	}
}
