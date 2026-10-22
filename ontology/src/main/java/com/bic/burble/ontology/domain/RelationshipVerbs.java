package com.bic.burble.ontology.domain;

/**
 * Relationship verbs stored once, on the subject entity. {@code CARRIED_BY}
 * lives on the thing being carried and points at the carrier. {@code CARRYING}
 * is the query for that statement, not a stored verb.
 */
public final class RelationshipVerbs {

    public static final String CARRIED_BY = "CARRIED_BY";

    private RelationshipVerbs() {
    }
}
