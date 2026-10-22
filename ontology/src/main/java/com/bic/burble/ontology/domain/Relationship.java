package com.bic.burble.ontology.domain;

import com.bic.burble.ontology.domain.statement.Certainty;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One relationship statement whose subject is the entity it is stored on.
 * {@code objectId} is the other entity. The inverse is not a second statement.
 * {@code time} is when it holds (null means always or unknown) and
 * {@code certainty} is 0..1, 1 when not given.
 */
public record Relationship(
        String verb,
        String objectId,
        @JsonInclude(JsonInclude.Include.NON_NULL) StatementTime time,
        double certainty
) {
    public Relationship {
        if (time != null && time.isOpen()) {
            time = null;
        }
    }

    public Relationship(String verb, String objectId) {
        this(verb, objectId, null, Certainty.CERTAIN);
    }
}
