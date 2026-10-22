package com.bic.burble.ontology.domain;

import java.util.Set;

/**
 * Relationship verbs stored once, on the subject entity. {@code CARRIED_BY}
 * lives on the thing being carried and points at the carrier. {@code CARRYING}
 * is the query for that statement, not a stored verb.
 */
public final class RelationshipVerbs {

    public static final String CARRIED_BY = "CARRIED_BY";

    /**
     * Verbs where the subject can point at only one object at a time. Two
     * statements with one of these verbs on the same subject must not
     * overlap in time.
     */
    public static final Set<String> SINGLE_VALUED = Set.of(CARRIED_BY);

    public static boolean isSingleValued(String verb) {
        return SINGLE_VALUED.contains(verb);
    }

    private RelationshipVerbs() {
    }
}
