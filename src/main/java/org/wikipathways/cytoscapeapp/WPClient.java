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

package org.wikipathways.cytoscapeapp;

import java.io.Reader;
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
   * Return a task that retrieves pathway info for a given pathway ID.
   * The result of the task is null if {@code id} is invalid.
   * @param id Pathway IDs have this regular form: {@code WP\d+}.
   */
  ResultTask<WPPathway> newPathwayInfoTask(final String id);

  /**
   * Return a task that provides the {@code Reader} containing the GPML contents of {@code pathway}.
   */
  ResultTask<Reader> newGPMLContentsTask(final WPPathway pathway);
}