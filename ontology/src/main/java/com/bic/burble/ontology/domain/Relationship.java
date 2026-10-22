package com.bic.burble.ontology.domain;

/**
 * One relationship statement whose subject is the entity it is stored on.
 * {@code objectId} is the other entity. The inverse is not a second statement.
 */
public record Relationship(String verb, String objectId) {
}
