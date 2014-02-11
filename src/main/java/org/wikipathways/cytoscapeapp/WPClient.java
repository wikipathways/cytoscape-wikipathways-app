package org.wikipathways.cytoscapeapp;

import java.io.InputStream;
import java.util.List;

/**
 * Issues requests to WikiPathways.
 * All results from these methods are wrapped in a {@code ResultTask}.
 * This is done to allow the user to cancel requests.
 */
public interface WPClient {
  /**
   * Return a task that retrieves the list of species recognized by WikiPathways.
   */
  ResultTask<List<String>> newSpeciesTask();

  /**
   * Return a task that searches WikiPathways for pathways that match the given query.
   * @param query The query to be used to search WikiPathways; can be things like gene name or pathway name.
   * @param species The species to restrict the search; must be a value returned by {@code newSpeciesTask} or
   * null to specify no restriction.
   */
  ResultTask<List<WPPathway>> newFreeTextSearchTask(final String query, final String species);

  /**
   * Return a task that provides the {@code InputStream} containing the GPML contents of {@code pathway}.
   */
  ResultTask<InputStream> newLoadPathwayTask(final WPPathway pathway);
}