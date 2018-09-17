package org.wikipathways.cytoscapeapp.internal.cmd.mapping;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This interface defines a basic mapping service client.
 *
 */
public interface IdMapper {

    /**
     * This returns a set of query identifiers for which a mapping could not
     * have been established with the latest execution of method "run".
     *
     * @return a set of unmatched identifiers
     */
    public Set<String> getUnmatchedIds();

    /**
     * This returns a set of query identifiers for which a mapping was
     * established with the latest execution of method "run".
     *
     * @return a set of matched identifiers
     */
    public Set<String> getMatchedIds();

    /**
     * This is the main method for accessing and querying mapping services.
     *
     * @param query_ids 		the identifiers to be mapped
     * @param source_type 	the type/source (e.g. "UniProt") of the identifiers to be mapped from
     * @param target_type 	the type/source (e.g. "Ensembl")of the identifiers to be mapped to
     * @param source_species the species (e.g. "Homo sapiens") of the identifiers to be mapped from
     * @param target_species the species (e.g. "Homo sapiens") of the identifiers to be mapped to
     *
     * @return a map of identifiers (for which a mapping exists) to IdMapping objects
     */
    public Map<String, String> map(final Collection<String> query_ids,
                                      final String source_type,
                                      final String target_type,
                                      final String source_species,
                                      final String target_species);


    /**
     * This is the main method for accessing and querying mapping services.
     *
     * @param query_ids 		the identifiers to be mapped
     * @param source_type 	the type/source (e.g. "UniProt") of the identifiers to be mapped from
     * @param target_type 	the type/source (e.g. "Ensembl")of the identifiers to be mapped to
     * @param source_species the species (e.g. "Homo sapiens") of the identifiers to be mapped from
     * @param target_species the species (e.g. "Homo sapiens") of the identifiers to be mapped to
     *
     * @return a map of identifiers (for which a mapping exists) to IdMapping objects
     */
    public Map<String, IdMapping> mapList(final Collection<String> query_ids,
                                      final List<MappingSource> source_type,
                                      final List<MappingSource> target_type,
                                      final String source_species,
                                      final String target_species);

    /**
     * 
     * @param query_ids			the sampling of identifiers
     * @param source_species 	constrains guesses to datasources matching the species
     * @return
     */
    public Map<String, IdGuess> guess(final Collection<String> query_ids,
                                      final String source_species);

}
