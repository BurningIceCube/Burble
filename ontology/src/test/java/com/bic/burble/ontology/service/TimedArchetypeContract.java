package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.ArchetypeStatement;
import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.entity.ArchetypeRequest;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;
import com.bic.burble.ontology.domain.entity.PeriodFacetRequest;
import com.bic.burble.ontology.domain.facet.PeriodFacet;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeBound;
import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.service.impl.DefaultEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Timed IS_A statements and time-aware relationship rules, run once per store. */
abstract class TimedArchetypeContract {

    protected static final String WORLD = "w";

    protected EntityRepository repository;
    protected DefaultEntityService service;

    protected abstract EntityRepository freshRepository();

    @BeforeEach
    void setUpService() {
        repository = freshRepository();
        service = new DefaultEntityService(repository);
    }

    private static ArchetypeRequest named(String archetype) {
        return ArchetypeRequest.of(archetype);
    }

    private static ArchetypeRequest timed(String archetype, Long start, Long end) {
        return new ArchetypeRequest(archetype, time(start, end), null);
    }

    private static StatementTime time(Long start, Long end) {
        return new StatementTime(start == null ? null : TimeBound.atTick(start), end == null ? null : TimeBound.atTick(end));
    }

    private String create(String name, ArchetypeRequest... archetypes) {
        return service.createEntity(WORLD, new CreateEntityRequest(name, null, null, null, null, List.of(archetypes)));
    }

    private List<ArchetypeStatement> archetypesAt(String id, Long tick) {
        return service.findArchetypes(WORLD, id, tick).orElseThrow();
    }

    // ----- storing timed IS_A -----

    @Test
    void untimedNamesWorkAsBefore() {
        String hero = create("Hero", named("Character"));
        assertThat(archetypesAt(hero, null)).containsExactly(ArchetypeStatement.always("Character"));
        assertThat(archetypesAt(hero, 5L)).containsExactly(ArchetypeStatement.always("Character"));
    }

    @Test
    void timedArchetypesAreStoredAndFilteredByTick() {
        String town = create("Town", timed("Location", 10L, 20L),
                new ArchetypeRequest("Organization", time(15L, null), 0.6));
        Entity loaded = service.findById(town).orElseThrow();
        assertThat(loaded.archetypes()).containsExactly(
                new ArchetypeStatement("Location", time(10L, 20L), 1.0),
                new ArchetypeStatement("Organization", time(15L, null), 0.6));
        assertThat(archetypesAt(town, 5L)).isEmpty();
        assertThat(archetypesAt(town, 12L)).extracting(ArchetypeStatement::archetype).containsExactly("Location");
        assertThat(archetypesAt(town, 17L)).extracting(ArchetypeStatement::archetype)
                .containsExactly("Location", "Organization");
        assertThat(archetypesAt(town, 20L)).extracting(ArchetypeStatement::archetype).containsExactly("Organization");
    }

    @Test
    void sameArchetypeTwiceWithoutOverlapIsAllowed() {
        String ghost = create("Ghost", timed("Character", 0L, 10L), timed("Character", 20L, 30L));
        assertThat(archetypesAt(ghost, null)).hasSize(2);
        assertThat(archetypesAt(ghost, 15L)).isEmpty();
        service.addArchetype(WORLD, ghost, timed("Character", 10L, 20L)).orElseThrow();
        assertThat(archetypesAt(ghost, 15L)).hasSize(1);
    }

    @Test
    void overlappingSameArchetypeIsRejected() {
        assertThatThrownBy(() -> create("Ghost", timed("Character", 0L, 10L), timed("Character", 5L, 30L)))
                .isInstanceOf(OverlappingStatementException.class)
                .hasMessageContaining("IS_A Character");
        String ghost = create("Ghost", timed("Character", 0L, 10L));
        assertThatThrownBy(() -> service.addArchetype(WORLD, ghost, named("Character")))
                .isInstanceOf(OverlappingStatementException.class);
        assertThat(archetypesAt(ghost, null)).hasSize(1);
    }

    @Test
    void sameArchetypeAndTimeAgainUpdatesCertainty() {
        String hero = create("Hero", timed("Character", 0L, 10L));
        service.addArchetype(WORLD, hero, new ArchetypeRequest("Character", time(0L, 10L), 0.4)).orElseThrow();
        assertThat(archetypesAt(hero, null)).containsExactly(new ArchetypeStatement("Character", time(0L, 10L), 0.4));
    }

    @Test
    void badArchetypeInputIsRejected() {
        String hero = create("Hero", named("Character"));
        assertThatThrownBy(() -> service.addArchetype(WORLD, hero, new ArchetypeRequest("Dragon", null, null)))
                .isInstanceOf(InvalidStatementException.class).hasMessageContaining("Unknown archetype");
        assertThatThrownBy(() -> service.addArchetype(WORLD, hero, new ArchetypeRequest("Trait", null, 1.5)))
                .isInstanceOf(InvalidStatementException.class).hasMessageContaining("Certainty");
        assertThatThrownBy(() -> service.addArchetype(WORLD, hero, timed("Trait", 10L, 5L)))
                .isInstanceOf(InvalidStatementException.class).hasMessageContaining("start before it ends");
        assertThatThrownBy(() -> service.addArchetype(WORLD, hero,
                new ArchetypeRequest("Trait", StatementTime.during(hero), null)))
                .isInstanceOf(InvalidStatementException.class).hasMessageContaining("is not an entity that IS_A Period");
        assertThat(service.addArchetype("other-world", hero, named("Trait"))).isEmpty();
    }

    @Test
    void removingAStatementNeedsTheExactTime() {
        String ghost = create("Ghost", timed("Character", 0L, 10L), timed("Character", 20L, 30L), named("Trait"));
        assertThat(service.removeArchetype(WORLD, ghost, "Character", null)).isFalse();
        assertThat(service.removeArchetype(WORLD, ghost, "Character", time(0L, 10L))).isTrue();
        assertThat(archetypesAt(ghost, null)).extracting(ArchetypeStatement::time).containsExactly(time(20L, 30L), null);
        assertThat(service.removeArchetype(WORLD, ghost, "Character", time(20L, 30L))).isTrue();
        assertThat(service.removeArchetype(WORLD, ghost, "Trait", new StatementTime(null, null))).isTrue();
        Entity loaded = service.findById(ghost).orElseThrow();
        assertThat(loaded.archetypes()).isEmpty();
        assertThat(loaded.facets()).isEmpty();
    }

    @Test
    void updateWithOnlyNamesKeepsExistingTimes() {
        String town = create("Town", timed("Location", 10L, 20L));
        service.updateEntity(WORLD, town, new CreateEntityRequest("Town", null, "renamed", null, null,
                List.of(named("Location"), named("Trait"))));
        assertThat(archetypesAt(town, null)).containsExactly(
                new ArchetypeStatement("Location", time(10L, 20L), 1.0), ArchetypeStatement.always("Trait"));
        service.updateEntity(WORLD, town, new CreateEntityRequest("Town", null, null, null, null,
                List.of(timed("Location", 30L, null))));
        assertThat(archetypesAt(town, null)).containsExactly(new ArchetypeStatement("Location", time(30L, null), 1.0));
    }

    @Test
    void periodIsATimeIsSeparateFromItsTicks() {
        String age = service.createEntity(WORLD, new CreateEntityRequest("Age", null, null, null, null,
                List.of(timed("Period", 0L, 50L)), new PeriodFacetRequest(100L, 200L)));
        Entity loaded = service.findById(age).orElseThrow();
        assertThat(loaded.getFacet(PeriodFacet.class)).contains(new PeriodFacet(100L, 200L));
        assertThat(loaded.archetypes()).containsExactly(new ArchetypeStatement("Period", time(0L, 50L), 1.0));

        // A bound that names the period still resolves to the period's own ticks.
        String hero = create("Hero", timed("Character", 100L, 200L));
        String town = create("Town", named("Location"));
        service.addRelationship(WORLD, hero, "LOCATED_IN", town, StatementTime.during(age), null).orElseThrow();
        assertThatThrownBy(() -> service.addRelationship(WORLD, hero, "LOCATED_IN", town, time(90L, 100L), null))
                .hasMessageContaining("from tick 90 until tick 100");
    }

    // ----- time-aware relationship rules -----

    private static List<Long[]> ranges(Long[]... ranges) {
        return java.util.Arrays.asList(ranges);
    }

    static Stream<Arguments> coverage() {
        return Stream.of(
                // subject IS_A Character times, relationship time, gap in the error or null when allowed
                Arguments.of("always covers untimed", ranges(new Long[]{null, null}), null, null, null),
                Arguments.of("timed archetype, untimed relationship", ranges(new Long[]{10L, null}), null, null,
                        "from the beginning until tick 10"),
                Arguments.of("inside", ranges(new Long[]{10L, 30L}), 15L, 20L, null),
                Arguments.of("same range", ranges(new Long[]{10L, 30L}), 10L, 30L, null),
                Arguments.of("runs past the end", ranges(new Long[]{10L, 30L}), 20L, 40L, "from tick 30 until tick 40"),
                Arguments.of("starts too early", ranges(new Long[]{10L, 30L}), 5L, 20L, "from tick 5 until tick 10"),
                Arguments.of("open-ended relationship", ranges(new Long[]{10L, 30L}), 20L, null, "from tick 30 onward"),
                Arguments.of("touching ranges cover as one", ranges(new Long[]{0L, 10L}, new Long[]{10L, 20L}), 5L, 15L, null),
                Arguments.of("gap between ranges", ranges(new Long[]{0L, 10L}, new Long[]{12L, 20L}), 5L, 15L,
                        "from tick 10 until tick 12"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coverage")
    void subjectArchetypeMustCoverTheWholeRelationship(String label, List<Long[]> ranges, Long start, Long end, String gap) {
        ArchetypeRequest[] archetypes = ranges.stream()
                .map(range -> timed("Character", range[0], range[1])).toArray(ArchetypeRequest[]::new);
        String hero = create("Hero", archetypes);
        String town = create("Town", named("Location"));
        StatementTime relationshipTime = start == null && end == null ? null : time(start, end);
        if (gap == null) {
            assertThat(service.addRelationship(WORLD, hero, "LOCATED_IN", town, relationshipTime, null)).isPresent();
        } else {
            assertThatThrownBy(() -> service.addRelationship(WORLD, hero, "LOCATED_IN", town, relationshipTime, null))
                    .isInstanceOf(InvalidStatementException.class)
                    .isNotInstanceOf(OverlappingStatementException.class)
                    .hasMessageContaining("LOCATED_IN needs the subject " + hero + " to be Character")
                    .hasMessageContaining("it is not Character " + gap);
            assertThat(service.findRelationships(hero)).isEmpty();
        }
    }

    @Test
    void objectArchetypeMustCoverTheWholeRelationshipToo() {
        String sword = create("Sword", named("Item"));
        String hero = create("Hero", timed("Character", 0L, 50L));
        service.addRelationship(WORLD, sword, "CARRIED_BY", hero, time(10L, 20L), null).orElseThrow();
        assertThatThrownBy(() -> service.addRelationship(WORLD, sword, "CARRIED_BY", hero, time(40L, 60L), null))
                .isInstanceOf(InvalidStatementException.class)
                .hasMessageContaining("the object " + hero + " to be Character, but it is not Character from tick 50 until tick 60");
    }

    @Test
    void anyOneAllowedArchetypeCanCover() {
        // Becomes an Organization at tick 10 after being only an Item before.
        String guild = create("Guild", timed("Item", null, 10L), timed("Organization", 10L, null));
        String town = create("Town", named("Location"));
        service.addRelationship(WORLD, guild, "LOCATED_IN", town, time(0L, 5L), null).orElseThrow();
        service.addRelationship(WORLD, guild, "LOCATED_IN", town, time(10L, null), null).orElseThrow();
        // Item until 10 then Organization from 10 is not one archetype covering 5..15.
        String other = create("Other", named("Location"));
        assertThatThrownBy(() -> service.addRelationship(WORLD, guild, "OWNED_BY", other, time(5L, 15L), null))
                .isInstanceOf(InvalidStatementException.class);
    }

    @Test
    void archetypeNeverHeldGivesTheOldMessage() {
        String sword = create("Sword", named("Item"));
        String town = create("Town", named("Location"));
        assertThatThrownBy(() -> service.addRelationship(WORLD, sword, "CARRIED_BY", town))
                .hasMessageContaining("CARRIED_BY allows Item -> Character");
    }
}
