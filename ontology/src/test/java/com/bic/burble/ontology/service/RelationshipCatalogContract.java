package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.RelationshipType;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.service.impl.DefaultEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The relationship catalog through {@link DefaultEntityService}, run once per
 * store. Every allowed (type, subject archetype, object archetype)
 * combination in the catalog is a case of the happy-path table.
 */
abstract class RelationshipCatalogContract {

    protected static final String WORLD = "w";
    private final AtomicInteger ids = new AtomicInteger();

    protected EntityRepository repository;
    protected DefaultEntityService service;

    /** A clean, empty store for each test. */
    protected abstract EntityRepository freshRepository();

    @BeforeEach
    void setUpService() {
        repository = freshRepository();
        service = new DefaultEntityService(repository);
    }

    protected String entity(String... archetypes) {
        Set<ArchetypeFacet> facets = new HashSet<>();
        for (String archetype : archetypes) {
            facets.add(ArchetypeFacets.of(archetype, true, false));
        }
        String id = "e" + ids.incrementAndGet() + "-" + String.join("-", archetypes);
        repository.save(new Entity(id, WORLD, id, List.of(), null, facets));
        return id;
    }

    static Stream<Arguments> allowedCombinations() {
        List<Arguments> cases = new ArrayList<>();
        for (RelationshipType type : RelationshipType.values()) {
            for (RelationshipType.Rule rule : type.rules()) {
                for (String subject : rule.subjects().stream().sorted().toList()) {
                    for (String object : rule.objects().stream().sorted().toList()) {
                        cases.add(Arguments.of(type, subject, object));
                    }
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("allowedCombinations")
    void everyAllowedCombinationIsStoredOnTheSubject(RelationshipType type, String subjectArchetype, String objectArchetype) {
        String subject = entity(subjectArchetype);
        String object = entity(objectArchetype);

        assertThat(service.addRelationship(WORLD, subject, type.name().toLowerCase(), object, null, 0.9))
                .hasValueSatisfying(r -> assertThat(r.verb()).isEqualTo(type.name()));
        assertThat(service.findRelationships(subject)).singleElement().satisfies(r -> {
            assertThat(r.verb()).isEqualTo(type.name());
            assertThat(r.objectId()).isEqualTo(object);
            assertThat(r.certainty()).isEqualTo(0.9);
        });
        assertThat(service.findRelationships(object)).isEmpty();
        assertThat(service.findByRelationship(type.name(), object)).extracting(Entity::guid).containsExactly(subject);
    }

    static Stream<Arguments> disallowedSubjects() {
        List<Arguments> cases = new ArrayList<>();
        for (RelationshipType type : RelationshipType.values()) {
            Set<String> subjects = new HashSet<>();
            type.rules().forEach(rule -> subjects.addAll(rule.subjects()));
            ArchetypeFacets.NAMES.stream().filter(name -> !subjects.contains(name)).findFirst()
                    .ifPresent(wrong -> cases.add(Arguments.of(type, wrong,
                            type.rules().get(0).objects().iterator().next())));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}: wrong subject {1}")
    @MethodSource("disallowedSubjects")
    void wrongSubjectArchetypeIsRejected(RelationshipType type, String wrongSubject, String object) {
        String subject = entity(wrongSubject);
        String target = entity(object);
        assertThatThrownBy(() -> service.addRelationship(WORLD, subject, type.name(), target))
                .isInstanceOf(InvalidStatementException.class)
                .isNotInstanceOf(OverlappingStatementException.class)
                .hasMessageContaining(type.name() + " allows " + type.describeRules())
                .hasMessageContaining("the subject is " + wrongSubject);
        assertThat(service.findRelationships(subject)).isEmpty();
    }

    static Stream<Arguments> disallowedObjects() {
        List<Arguments> cases = new ArrayList<>();
        for (RelationshipType type : RelationshipType.values()) {
            RelationshipType.Rule rule = type.rules().get(0);
            Set<String> objects = new HashSet<>();
            type.rules().forEach(r -> objects.addAll(r.objects()));
            String subject = rule.subjects().stream().sorted().findFirst().orElseThrow();
            ArchetypeFacets.NAMES.stream().filter(name -> !objects.contains(name)).findFirst()
                    .ifPresent(wrong -> cases.add(Arguments.of(type, subject, wrong)));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}: wrong object {2}")
    @MethodSource("disallowedObjects")
    void wrongObjectArchetypeIsRejected(RelationshipType type, String subject, String wrongObject) {
        String from = entity(subject);
        String to = entity(wrongObject);
        assertThatThrownBy(() -> service.addRelationship(WORLD, from, type.name(), to))
                .isInstanceOf(InvalidStatementException.class)
                .hasMessageContaining("the object is " + wrongObject);
    }

    @Test
    void anEntityWithNoArchetypesCannotTakePart() {
        String nothing = entity();
        String location = entity("Location");
        assertThatThrownBy(() -> service.addRelationship(WORLD, nothing, "LOCATED_IN", location))
                .hasMessageContaining("the subject is no archetype");
    }

    @Test
    void partOfOnlyAllowsTheSameArchetypeOnBothSides() {
        String location = entity("Location");
        String organization = entity("Organization");
        String event = entity("Event");
        String period = entity("Period");
        for (String[] pair : new String[][]{{location, organization}, {organization, location},
                {event, period}, {period, event}, {location, event}}) {
            assertThatThrownBy(() -> service.addRelationship(WORLD, pair[0], "PART_OF", pair[1]))
                    .as(pair[0] + " -> " + pair[1])
                    .isInstanceOf(InvalidStatementException.class)
                    .hasMessageContaining("Location -> Location; Organization -> Organization; Event -> Event; Period -> Period");
        }
        // An entity with two archetypes can use whichever pair fits.
        String cityGuild = entity("Location", "Organization");
        assertThat(service.addRelationship(WORLD, cityGuild, "PART_OF", entity("Organization"))).isPresent();
        assertThat(service.addRelationship(WORLD, entity("Location"), "PART_OF", cityGuild)).isPresent();
    }

    @Test
    void singleTypesRejectOverlapsAndManyTypesAllowThem() {
        String hero = entity("Character");
        String town = entity("Location");
        String castle = entity("Location");
        service.addRelationship(WORLD, hero, "LOCATED_IN", town, StatementTime.between(100, 200), null);
        assertThatThrownBy(() -> service.addRelationship(WORLD, hero, "LOCATED_IN", castle, StatementTime.between(150, 250), null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThat(service.addRelationship(WORLD, hero, "LOCATED_IN", castle, StatementTime.from(200), null)).isPresent();
        assertThat(service.findRelationships(hero, 150L)).extracting(Relationship::objectId).containsExactly(town);
        assertThat(service.findRelationships(hero, 250L)).extracting(Relationship::objectId).containsExactly(castle);

        String guild = entity("Organization");
        String order = entity("Organization");
        service.addRelationship(WORLD, hero, "MEMBER_OF", guild, StatementTime.between(100, 300), null);
        service.addRelationship(WORLD, hero, "member_of", order, null, null);
        assertThat(service.findRelationships(hero, 150L)).filteredOn(r -> r.verb().equals("MEMBER_OF"))
                .extracting(Relationship::objectId).containsExactlyInAnyOrder(guild, order);
        // Same membership in an overlapping range is a second statement too.
        assertThat(service.addRelationship(WORLD, hero, "MEMBER_OF", guild, StatementTime.between(250, 400), null)).isPresent();

        // An exact repeat only updates certainty.
        service.addRelationship(WORLD, hero, "MEMBER_OF", order, null, 0.4);
        assertThat(service.findRelationships(hero)).filteredOn(r -> r.objectId().equals(order)).singleElement()
                .satisfies(r -> assertThat(r.certainty()).isEqualTo(0.4));
        assertThat(service.findByRelationship("MEMBER_OF", guild, 260L)).extracting(Entity::guid).containsExactly(hero);
    }

    @Test
    void childOfAllowsTwoParentsAtOnce() {
        String child = entity("Character");
        String mother = entity("Character");
        String father = entity("Character");
        service.addRelationship(WORLD, child, "CHILD_OF", mother);
        service.addRelationship(WORLD, child, "CHILD_OF", father);
        assertThat(service.findRelationships(child)).extracting(Relationship::objectId).containsExactlyInAnyOrder(mother, father);
        assertThat(service.findByRelationship("CHILD_OF", mother)).extracting(Entity::guid).containsExactly(child);
    }

    @Test
    void selfReferenceIsRejected() {
        String hero = entity("Character");
        String event = entity("Event");
        assertThatThrownBy(() -> service.addRelationship(WORLD, hero, "CHILD_OF", hero))
                .isInstanceOf(InvalidStatementException.class).hasMessageContaining("itself");
        assertThatThrownBy(() -> service.addRelationship(WORLD, event, "CAUSED_BY", event))
                .isInstanceOf(InvalidStatementException.class).hasMessageContaining("itself");
        assertThat(service.findRelationships(hero)).isEmpty();
    }

    @Test
    void knowsUsesCertaintyForHowSure() {
        String sage = entity("Character");
        String secret = entity("Concept");
        service.addRelationship(WORLD, sage, "KNOWS", secret, null, 0.3);
        assertThat(service.findRelationships(sage)).singleElement()
                .satisfies(r -> assertThat(r.certainty()).isEqualTo(0.3));
        assertThatThrownBy(() -> service.addRelationship(WORLD, sage, "KNOWS", secret, null, 1.3))
                .isInstanceOf(InvalidStatementException.class);
    }
}
