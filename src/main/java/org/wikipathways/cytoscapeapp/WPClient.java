package org.wikipathways.cytoscapeapp;

import java.io.Reader;
import java.util.List;

import org.wikipathways.cytoscapeapp.impl.ResultTask;
import org.wikipathways.cytoscapeapp.impl.WPManager;
import org.wikipathways.cytoscapeapp.impl.WPPathway;

/**
 * Issues requests to WikiPathways.
 * All results from these methods are wrapped in a {@code ResultTask}.
 * This is done to allow the user to cancel requests.
 */
public interface WPClient {
  /**
   * Return a task that retrieves the list of species recognized by WikiPathways.
   */
	ResultTask<List<String>> getSpeciesListTask();

  /**
   * Return a task that searches WikiPathways for pathways that match the given query.
   * @param query The query to be used to search WikiPathways; can be things like gene name or pathway name.
   * @param species The species to restrict the search; must be a value returned by {@code newSpeciesTask} or
   * null to specify no restriction.
   */
  	ResultTask<List<WPPathway>> freeTextSearchTask(final String query, final String species);

  /**
   * Return a task that retrieves pathway info for a given pathway ID.
   * The result of the task is null if {@code id} is invalid.
   * @param id Pathway IDs have this regular form: {@code WP\d+}.
   */
  	ResultTask<WPPathway> pathwayInfoTask(final String id);

  /**
   * Return a task that provides the {@code Reader} containing the GPML contents of {@code pathway}.
   */
	ResultTask<Reader> gpmlContentsTask(final WPPathway pathway);
	public WPManager getManager();

}