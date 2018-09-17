package org.wikipathways.cytoscapeapp.internal.cmd.mapping;

import java.util.Set;

/**
 * This is used to express one-to-one relationships of biological identifiers 

 * @author adam treister
 *
 */
public interface IdMapping {

    /**
     * This returns the source id(s) of this id mapping relationship.
     *
     * @return a set of source ids
     */
    public Set<String> getSourceIds();

    /**
     * This returns the target id(s) of this id mapping relationship.
     *
     * @return a set of target ids
     */
    public Set<String> getTargetIds();

    /**
     * This returns the source id type (e.g. "UniProt") of this id mapping
     * relationship.
     *
     * @return the source type
     */
    public MappingSource getSourceType();

    /**
     * This returns the species of the source ids (e.g. "Homo sapiens") of this
     * id mapping relationship. In most cases, source and target species are the
     * same.
     *
     * @return the source species.
     */
    public String getSourceSpecies();

    /**
     * This returns the target id type (e.g. "Ensembl") of this id mapping
     * relationship.
     *
     * @return the target type
     */
    public MappingSource getTargetType();


}
