package com.bic.burble.ontology.persistence.json;

import java.util.Map;

/**
 * A statement whose subject is the enclosing entity. {@code verb} is the
 * relationship ({@code IS_A} for archetypes). {@code object} is what it points
 * at (an archetype name such as {@code Item} or {@code Character}, or another
 * entity id for future relationships). {@code properties} holds values that
 * belong to that statement, such as an item's {@code portable} and
 * {@code unique} flags.
 */
public record StoredStatement(
        String verb,
        String object,
        Map<String, Object> properties
) {
    public StoredStatement {
        properties = properties == null || properties.isEmpty() ? Map.of() : Map.copyOf(properties);
    }
}
