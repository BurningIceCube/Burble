package com.bic.burble.ontology.persistence.json;

import java.util.function.Function;

/**
 * Storage interface for the ontology graph: worlds, the entities they
 * contain, and the statements attached to each entity (including {@code IS_A}
 * archetype statements).
 *
 * <p>A JSON-file implementation is {@link JsonFileOntologyGraphStore}. It is
 * selected when {@code burble.ontology.persistance.world} and/or
 * {@code burble.ontology.persistance.entity} is {@code json}.
 */
public interface OntologyGraphStore {

    OntologyGraph load();

    void save(OntologyGraph graph);

    /**
     * Loads the graph, applies {@code action}, and writes the same instance
     * back. The read-modify-write is atomic with respect to other callers of
     * this store.
     */
    <T> T update(Function<OntologyGraph, T> action);
}
