
package org.wikipathways.cytoscapeapp.impl;

/**
 * Represents a pathway on WikiPathways.
 */
public final class WPPathway {
  final String id;
  final String revision;
  final String name;
  final String species;
  final String url;
  final String description;
  final String authors;
  final String datanodes;
  final String annotations;
  final String citedIn;

  public WPPathway(final String id, final String revision, final String name, final String species, final String url, 
                   final String description, final String authors, final String datanodes, final String annotations, final String citedIn) {
      this.id = id;
      this.revision = revision;
      this.name = name;
      this.species = species;
      this.url = url;
      this.description = description;
      this.authors = authors;
      this.datanodes = datanodes;
      this.annotations = annotations;
      this.citedIn = citedIn;
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

  public String getUrl() {
      return url;
  }

  public String getDescription() {
      return description;
  }

  public String getAuthors() {
      return authors;
  }

  public String getDatanodes() {
      return datanodes;
  }

  public String getAnnotations() {
      return annotations;
  }

  public String getCitedIn() {
      return citedIn;
  }

  @Override
  public String toString() {
      return String.format("%s (%s) [ID: %s]", name, species, id);
  }
}