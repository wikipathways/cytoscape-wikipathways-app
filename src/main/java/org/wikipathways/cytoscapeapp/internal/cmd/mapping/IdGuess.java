package org.wikipathways.cytoscapeapp.internal.cmd.mapping;

import java.util.Set;

/**
 * Interface for an Id guessing service.
 *
 *
 * @author cmzmasek
 *
 */
public interface IdGuess {

    /**
     * This returns the source id(s) of this id mapping relationship.
     *
     * @return a set of source ids
     */
    public String getSourceIds();

    /**
     * This returns the potential source id types (e.g. "UniProt").
     *
     * @return the guessed source types
     */
    public Set<String> getPotentialSourceTypes();

    /**
     * This returns the otential species of the source ids (e.g.
     * "Homo sapiens").\
     *
     * @return the guessed source species.
     */
    public Set<String> getPotentialSourceSpecies();

}
