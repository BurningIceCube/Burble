package com.bic.burble.ontology.domain;

import com.bic.burble.ontology.domain.statement.InvalidStatementException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The closed set of relationship types an entity can store. {@code IS_A} is
 * not here: archetypes are set on the entity itself.
 *
 * <p>Incoming verbs are matched case-insensitively after trimming, so
 * {@code "carried_by"} and {@code " Carried_By "} both mean
 * {@link #CARRIED_BY}. Stores always write and return the upper-case name,
 * and Neo4j uses that name as the edge type.
 */
public enum RelationshipType {

    /** Stored on the thing being carried, pointing at the carrier. One carrier at a time. */
    CARRIED_BY(true);

    private final boolean singleValued;

    RelationshipType(boolean singleValued) {
        this.singleValued = singleValued;
    }

    /**
     * Whether the subject can point at only one object at a time, so two
     * statements of this type on one subject must not overlap in time.
     */
    public boolean singleValued() {
        return singleValued;
    }

    /** The matching type, or empty when {@code verb} is not one of them. */
    public static Optional<RelationshipType> find(String verb) {
        if (verb == null) {
            return Optional.empty();
        }
        String key = verb.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(type -> type.name().equals(key)).findFirst();
    }

    /**
     * The matching type, or {@link InvalidStatementException} naming the
     * allowed values.
     */
    public static RelationshipType parse(String verb) {
        return find(verb).orElseThrow(() -> new InvalidStatementException(
                "Unknown relationship verb \"" + verb + "\". Allowed values: " + allowed()));
    }

    /** {@code verb} in its stored form when it is known, otherwise as given. */
    public static String canonicalOrSelf(String verb) {
        return find(verb).map(Enum::name).orElse(verb);
    }

    public static String allowed() {
        return String.join(", ", Arrays.stream(values()).map(Enum::name).toList());
    }
}
