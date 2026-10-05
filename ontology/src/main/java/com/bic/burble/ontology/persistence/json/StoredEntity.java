package com.bic.burble.ontology.persistence.json;

import java.util.List;

/**
 * Entity cell inside {@link OntologyGraph}. Archetypes are not stored on this
 * record; they are {@code IS_A} entries in {@link #statements()}.
 */
public record StoredEntity(
        String guid,
        String worldId,
        String name,
        List<String> aliases,
        String description,
        List<StoredStatement> statements
) {
    public StoredEntity {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        statements = statements == null ? List.of() : List.copyOf(statements);
    }
}
