package org.wikipathways.cytoscapeapp.internal.webclient;

public class PathwayRef {
	final String id;
	final String revision;
	final String name;
	final String species;

	public PathwayRef(final String id, final String revision,
			final String name, final String species) {
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