package com.bic.burble.ontology.domain;

import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bic.burble.ontology.domain.RelationshipType.Cardinality.MANY;
import static com.bic.burble.ontology.domain.RelationshipType.Cardinality.SINGLE;
import static com.bic.burble.ontology.domain.RelationshipType.Names.*;

/**
 * The closed set of relationship types an entity can store, with which
 * archetypes each one may connect. {@code IS_A} is not here: archetypes are
 * set on the entity itself. See {@code docs/ontology/relationships.md}.
 *
 * <p>Every type is stored once, on the subject. The inverse (for example what
 * a character is carrying, or who is a member of an organization) is a query,
 * not a second statement.
 *
 * <p>A statement is allowed when one of the type's rules matches: the subject
 * has at least one of the rule's subject archetypes and the object has at
 * least one of its object archetypes. Archetypes count whenever the entity has
 * them, regardless of any time on its {@code IS_A} statements.
 *
 * <p>{@link Cardinality#SINGLE} types allow one object per subject at a time,
 * so their statements may not overlap. {@link Cardinality#MANY} types may
 * overlap; only an exact repeat (same object, same time) is merged.
 *
 * <p>Incoming verbs are matched case-insensitively after trimming and are
 * always stored and returned in upper case.
 */
public enum RelationshipType {

    // ----- the catalog -----            cardinality  subject archetypes ............ -> object archetypes
    CARRIED_BY(SINGLE, rule(of(ITEM), of(CHARACTER))),
    LOCATED_IN(SINGLE, rule(of(CHARACTER, ITEM, ORGANIZATION, EVENT), of(LOCATION))),
    PART_OF(SINGLE,
            rule(of(LOCATION), of(LOCATION)),
            rule(of(ORGANIZATION), of(ORGANIZATION)),
            rule(of(EVENT), of(EVENT)),
            rule(of(PERIOD), of(PERIOD))),
    OWNED_BY(SINGLE, rule(of(ITEM, LOCATION, TITLE), of(CHARACTER, ORGANIZATION))),
    MEMBER_OF(MANY, rule(of(CHARACTER), of(ORGANIZATION))),
    OF_SPECIES(SINGLE, rule(of(CHARACTER), of(SPECIES))),
    OF_LINEAGE(SINGLE, rule(of(CHARACTER), of(LINEAGE))),
    CHILD_OF(MANY, rule(of(CHARACTER), of(CHARACTER))),
    HELD_BY(SINGLE, rule(of(TITLE), of(CHARACTER, ORGANIZATION))),
    DURING(SINGLE, rule(of(EVENT), of(PERIOD))),
    PARTICIPATED_IN(MANY, rule(of(CHARACTER, ORGANIZATION, ITEM), of(EVENT))),
    CAUSED_BY(MANY, rule(of(EVENT), of(EVENT))),
    MADE_OF(MANY, rule(of(ITEM, LOCATION), of(SUBSTANCE))),
    HAS_TRAIT(MANY, rule(ANY, of(TRAIT))),
    CAN_PERFORM(MANY, rule(of(CHARACTER, SPECIES, ITEM), of(ACTION))),
    BELIEVES(MANY, rule(of(CHARACTER, ORGANIZATION), of(WORLDVIEW))),
    KNOWS(MANY, rule(of(CHARACTER), of(CONCEPT))),
    GOVERNED_BY(MANY, rule(of(LOCATION, ORGANIZATION), of(RULE, SYSTEM))),
    COMPONENT_OF(MANY, rule(of(RULE, LOGIC, CONCEPT, ACTION), of(SYSTEM, WORLDVIEW))),
    ENACTED_BY(SINGLE, rule(of(RULE), of(ORGANIZATION, CHARACTER))),
    SPEAKS(MANY, rule(of(CHARACTER, ORGANIZATION, SPECIES), of(CONCEPT, SYSTEM))),
    INHABITS(MANY, rule(of(SPECIES), of(LOCATION))),
    USES(MANY, rule(of(SYSTEM, ACTION, ITEM), of(SUBSTANCE))),
    EXISTS_DURING(MANY, rule(of(LOCATION, LOGIC, WORLDVIEW), of(PERIOD)));

    public enum Cardinality {
        /** One object per subject at a time; statements may not overlap. */
        SINGLE,
        /** Any number at once; only exact repeats are merged. */
        MANY
    }

    /** One allowed subject-to-object pairing. */
    public record Rule(Set<String> subjects, Set<String> objects) {
        boolean matches(Collection<String> subjectArchetypes, Collection<String> objectArchetypes) {
            return subjectArchetypes.stream().anyMatch(subjects::contains)
                    && objectArchetypes.stream().anyMatch(objects::contains);
        }

        @Override
        public String toString() {
            return (subjects.equals(ANY) ? "any archetype" : String.join(" or ", sorted(subjects)))
                    + " -> " + String.join(" or ", sorted(objects));
        }
    }

    private final Cardinality cardinality;
    private final List<Rule> rules;

    RelationshipType(Cardinality cardinality, Rule... rules) {
        this.cardinality = cardinality;
        this.rules = List.of(rules);
    }

    public Cardinality cardinality() {
        return cardinality;
    }

    /** Whether statements of this type on one subject must not overlap in time. */
    public boolean singleValued() {
        return cardinality == SINGLE;
    }

    public List<Rule> rules() {
        return rules;
    }

    /** Whether some rule allows a subject with these archetypes to point at an object with these. */
    public boolean allows(Collection<String> subjectArchetypes, Collection<String> objectArchetypes) {
        return rules.stream().anyMatch(rule -> rule.matches(subjectArchetypes, objectArchetypes));
    }

    /**
     * Throws {@link InvalidStatementException} naming the allowed archetypes
     * when no rule matches.
     */
    public void requireAllowed(Collection<String> subjectArchetypes, Collection<String> objectArchetypes) {
        if (!allows(subjectArchetypes, objectArchetypes)) {
            throw new InvalidStatementException(name() + " allows " + describeRules()
                    + ", but the subject is " + describe(subjectArchetypes)
                    + " and the object is " + describe(objectArchetypes));
        }
    }

    public String describeRules() {
        return rules.stream().map(Rule::toString).collect(Collectors.joining("; "));
    }

    // ----- parsing -----

    /** The matching type, or empty when {@code verb} is not one of them. */
    public static Optional<RelationshipType> find(String verb) {
        if (verb == null) {
            return Optional.empty();
        }
        String key = verb.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(type -> type.name().equals(key)).findFirst();
    }

    /** The matching type, or {@link InvalidStatementException} naming the allowed values. */
    public static RelationshipType parse(String verb) {
        return find(verb).orElseThrow(() -> new InvalidStatementException(
                "Unknown relationship verb \"" + verb + "\". Allowed values: " + allowed()));
    }

    /** {@code verb} in its stored form when it is known, otherwise as given. */
    public static String canonicalOrSelf(String verb) {
        return find(verb).map(Enum::name).orElse(verb);
    }

    public static String allowed() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    // ----- declaration helpers -----

    private static Rule rule(Set<String> subjects, Set<String> objects) {
        return new Rule(subjects, objects);
    }

    private static Set<String> of(String... archetypes) {
        for (String archetype : archetypes) {
            if (!ArchetypeFacets.isKnown(archetype)) {
                throw new IllegalArgumentException("Unknown archetype in relationship catalog: " + archetype);
            }
        }
        return Set.of(archetypes);
    }

    private static List<String> sorted(Set<String> names) {
        return names.stream().sorted().toList();
    }

    private static String describe(Collection<String> archetypes) {
        return archetypes.isEmpty() ? "no archetype" : String.join(", ", archetypes.stream().sorted().toList());
    }

    /** Archetype names, from docs/ontology/archetypes.md. Nested so the enum constants can use them. */
    static final class Names {

        static final String CHARACTER = "Character";
        static final String ITEM = "Item";
        static final String LOCATION = "Location";
        static final String SUBSTANCE = "Substance";
        static final String ORGANIZATION = "Organization";
        static final String SPECIES = "Species";
        static final String LINEAGE = "Lineage";
        static final String EVENT = "Event";
        static final String PERIOD = "Period";
        static final String ACTION = "Action";
        static final String TRAIT = "Trait";
        static final String TITLE = "Title";
        static final String SYSTEM = "System";
        static final String WORLDVIEW = "Worldview";
        static final String CONCEPT = "Concept";
        static final String LOGIC = "Logic";
        static final String RULE = "Rule";
        static final Set<String> ANY = Set.copyOf(ArchetypeFacets.NAMES);
        private Names() {
        }
    }
}
