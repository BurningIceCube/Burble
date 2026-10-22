package com.bic.burble.ontology.domain;

import com.bic.burble.ontology.domain.statement.InvalidStatementException;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Relationship verbs stored once, on the subject entity. {@code CARRIED_BY}
 * lives on the thing being carried and points at the carrier. {@code CARRYING}
 * is the query for that statement, not a stored verb.
 *
 * <p>Every store uses the same verb rule ({@link #requireValid}): upper-case
 * letters, digits and underscores, starting with a letter, and not
 * {@code IS_A} (archetypes are set on the entity, not as relationships).
 * Verbs are rejected rather than silently rewritten. The rule is checked
 * when a statement is written; statements already in a store load as they are.
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

    public static final String IS_A = "IS_A";

    private static final Pattern VALID = Pattern.compile("[A-Z][A-Z0-9_]*");

    public static boolean isValid(String verb) {
        return verb != null && VALID.matcher(verb).matches() && !IS_A.equals(verb);
    }

    /**
     * Returns {@code verb} unchanged when it follows the rule, otherwise
     * throws {@link InvalidStatementException} with a message that suggests
     * the upper-case form when there is one.
     */
    public static String requireValid(String verb) {
        if (verb == null || verb.isBlank()) {
            throw new InvalidStatementException("Verb is required");
        }
        if (IS_A.equals(verb)) {
            throw new InvalidStatementException(
                    "IS_A is not a relationship verb; set archetypes on the entity instead");
        }
        if (VALID.matcher(verb).matches()) {
            return verb;
        }
        String suggestion = verb.trim().replaceAll("[\\s-]+", "_").toUpperCase(Locale.ROOT);
        String message = "Verb must be upper-case letters, digits and underscores, starting with a letter, got \""
                + verb + "\"";
        if (VALID.matcher(suggestion).matches() && !IS_A.equals(suggestion)) {
            message += ". Did you mean \"" + suggestion + "\"?";
        }
        throw new InvalidStatementException(message);
    }

    private RelationshipVerbs() {
    }
}
