package com.bic.burble.ontology.persistence.json;

/**
 * World row inside {@link OntologyGraph}. Timestamps are ISO-8601 instants.
 */
public record StoredWorld(
        String guid,
        String name,
        String description,
        String owner,
        String createdAt,
        String updatedAt
) {}
