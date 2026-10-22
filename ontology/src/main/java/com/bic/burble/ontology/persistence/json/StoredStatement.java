package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.statement.StatementTime;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * A statement whose subject is the enclosing entity. {@code verb} is the
 * relationship ({@code IS_A} for archetypes). {@code object} is what it points
 * at (an archetype name such as {@code Item} or {@code Character}, or another
 * entity id). {@code properties} holds values that belong to that statement,
 * such as an item's {@code portable} and {@code unique} flags.
 *
 * <p>{@code time} is when the statement holds; missing means always or
 * unknown. {@code certainty} is 0..1; missing means 1. Both are left out of
 * the file when not set, so files written before they existed still load.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoredStatement(
        String verb,
        String object,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, Object> properties,
        StatementTime time,
        Double certainty
) {
    public StoredStatement {
        properties = properties == null || properties.isEmpty() ? Map.of() : Map.copyOf(properties);
        if (time != null && time.isOpen()) {
            time = null;
        }
        if (certainty != null && certainty == 1.0) {
            certainty = null;
        }
    }

    public StoredStatement(String verb, String object, Map<String, Object> properties) {
        this(verb, object, properties, null, null);
    }

    StoredStatement withTiming(StatementTime newTime, Double newCertainty) {
        return new StoredStatement(verb, object, properties, newTime, newCertainty);
    }
}
