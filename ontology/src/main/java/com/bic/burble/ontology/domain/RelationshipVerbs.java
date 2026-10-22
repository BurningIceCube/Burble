package com.bic.burble.ontology.domain;

/**
 * Names for relationship verbs. The allowed set is {@link RelationshipType};
 * {@code CARRIED_BY} lives on the thing being carried and points at the
 * carrier. {@code CARRYING} is the query for that statement, not a stored verb.
 */
public final class RelationshipVerbs {

    public static final String CARRIED_BY = RelationshipType.CARRIED_BY.name();

    private RelationshipVerbs() {
    }
}
