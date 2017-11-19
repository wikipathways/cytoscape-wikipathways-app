
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

  public WPPathway(final String id, final String revision,
      final String name, final String species, final String url) {
    this.id = id;
    this.revision = revision;
    this.name = name;
    this.species = species;
    this.url = url;
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

  public String toString() {
    return String.format("%s (%s) [ID: %s]", name, species, id);
  }
}