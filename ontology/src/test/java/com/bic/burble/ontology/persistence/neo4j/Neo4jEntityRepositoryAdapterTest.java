package com.bic.burble.ontology.persistence.neo4j;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.domain.facet.PeriodFacet;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeBound;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bic.burble.ontology.domain.RelationshipVerbs.CARRIED_BY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a real Neo4j (see {@link Neo4jTestServer}); skipped when none
 * is available. Mirrors the JSON store's timed-statement tests.
 */
class Neo4jEntityRepositoryAdapterTest {

    private Driver driver;
    private Neo4jEntityRepositoryAdapter entities;

    @BeforeEach
    void setUp() {
        driver = Neo4jTestServer.driver();
        run("MATCH (n) DETACH DELETE n");
        entities = new Neo4jEntityRepositoryAdapter(driver);
        entities.save(new Entity("sword", "w", "Sung Sword", List.of("The Blade"), "A blade.",
                Set.of(new ItemFacet(true, true))));
        entities.save(new Entity("aldric", "w", "Aldric", List.of(), null, Set.of(new CharacterFacet())));
        entities.save(new Entity("mira", "w", "Mira", List.of(), null, Set.of(new CharacterFacet())));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.close();
        }
    }

    private List<Record> run(String cypher) {
        try (Session session = driver.session()) {
            return session.run(cypher, new HashMap<String, Object>()).list();
        }
    }

    private List<String> carriersAt(long tick) {
        return entities.findRelationships("sword", tick).stream().map(Relationship::objectId).toList();
    }

    private List<String> carriedAt(String carrier, long tick) {
        return entities.findByRelationship(CARRIED_BY, carrier, tick).stream().map(Entity::guid).toList();
    }

    @Test
    void everyArchetypeRoundTripsWithItemAndPeriodProperties() {
        Set<ArchetypeFacet> facets = new HashSet<>();
        for (String name : ArchetypeFacets.NAMES) {
            facets.add(switch (name) {
                case "Item" -> new ItemFacet(true, false);
                case "Period" -> new PeriodFacet(200L, 400L);
                default -> ArchetypeFacets.of(name, null, null);
            });
        }
        entities.save(new Entity("all", "w", "Everything", List.of("x"), "d", facets));

        Entity loaded = entities.findById("all").orElseThrow();
        assertThat(loaded.facets()).containsExactlyInAnyOrderElementsOf(facets);
        assertThat(loaded.aliases()).containsExactly("x");
        assertThat(entities.findAllByWorldId("w")).extracting(Entity::guid)
                .containsExactlyInAnyOrder("all", "sword", "aldric", "mira");

        // Saving again replaces archetypes and leaves no orphan per-entity nodes.
        entities.update(new Entity("all", "w", "Everything", List.of(), null, Set.of(new CharacterFacet())));
        assertThat(entities.findById("all").orElseThrow().facets()).containsExactly(new CharacterFacet());
        assertThat(run("MATCH (a:Archetype {owner: 'all'}) RETURN a")).isEmpty();
        // Character is one shared node.
        assertThat(run("MATCH (c:Archetype:Character) RETURN c")).hasSize(1);

        assertThat(entities.deleteById("all")).isTrue();
        assertThat(entities.deleteById("all")).isFalse();
        assertThat(entities.findById("all")).isEmpty();
    }

    @Test
    void carriedByIsOneEdgeFromItemToCharacterWithTimeAndCertainty() {
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(250, 300), 0.8);

        List<Record> edges = run("MATCH (s)-[r:CARRIED_BY]->(o) RETURN s.id AS s, o.id AS o, properties(r) AS p");
        assertThat(edges).singleElement().satisfies(row -> {
            assertThat(row.get("s").asString()).isEqualTo("sword");
            assertThat(row.get("o").asString()).isEqualTo("aldric");
            assertThat(row.get("p").asMap()).isEqualTo(Map.of("startTick", 250L, "endTick", 300L, "certainty", 0.8));
        });
        assertThat(run("MATCH ()-[r:CARRYING]->() RETURN r")).isEmpty();
        assertThat(entities.findRelationships("aldric")).isEmpty();

        // Re-saving the sword's archetypes keeps the carry edge.
        entities.update(new Entity("sword", "w", "Sung Sword", List.of(), null, Set.of(new ItemFacet(true, false))));
        assertThat(entities.findRelationships("sword")).singleElement().satisfies(r -> {
            assertThat(r.objectId()).isEqualTo("aldric");
            assertThat(r.certainty()).isEqualTo(0.8);
            assertThat(r.time()).isEqualTo(StatementTime.between(250, 300));
        });

        // An untimed, certain edge has no properties at all.
        entities.addRelationship("mira", CARRIED_BY, "aldric");
        assertThat(run("MATCH (:Entity {id:'mira'})-[r:CARRIED_BY]->() RETURN properties(r) AS p"))
                .singleElement().satisfies(row -> assertThat(row.get("p").asMap()).isEmpty());
        assertThat(entities.findRelationships("mira")).containsExactly(new Relationship(CARRIED_BY, "aldric"));
    }

    @Test
    void timedCarryHistoryAnswersBothDirectionsAtATick() {
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(250, 300), null);
        entities.addRelationship("sword", CARRIED_BY, "mira", StatementTime.from(300), 0.6);

        assertThat(carriersAt(249)).isEmpty();
        assertThat(carriersAt(250)).containsExactly("aldric");
        assertThat(carriersAt(299)).containsExactly("aldric");
        assertThat(carriersAt(300)).containsExactly("mira");
        assertThat(carriedAt("aldric", 280)).containsExactly("sword");
        assertThat(carriedAt("aldric", 300)).isEmpty();
        assertThat(carriedAt("mira", 9_000)).containsExactly("sword");
        assertThat(entities.findRelationships("sword")).hasSize(2);
        assertThat(entities.findByRelationship(CARRIED_BY, "aldric")).extracting(Entity::guid).containsExactly("sword");
    }

    @Test
    void periodOnlyBoundsResolveToThePeriodsTicks() {
        entities.save(new Entity("age", "w", "Age of Imperials", List.of(), null, Set.of(new PeriodFacet(200L, 400L))));
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.during("age"), 0.9);

        assertThat(carriersAt(199)).isEmpty();
        assertThat(carriersAt(200)).containsExactly("aldric");
        assertThat(carriersAt(399)).containsExactly("aldric");
        assertThat(carriersAt(400)).isEmpty();
        assertThat(carriedAt("aldric", 300)).containsExactly("sword");

        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "mira",
                new StatementTime(new TimeBound(null, "age"), null), null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThat(entities.addRelationship("sword", CARRIED_BY, "mira", StatementTime.from(400), null)).isPresent();
        assertThat(carriersAt(400)).containsExactly("mira");
    }

    @Test
    void overlapsAreRejectedAndNothingIsWritten() {
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(250, 300), null);

        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "mira", StatementTime.between(299, 400), null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "mira", null, null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThat(entities.findRelationships("sword")).hasSize(1);

        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(250, 300), 0.5);
        assertThat(entities.findRelationships("sword")).singleElement()
                .satisfies(r -> assertThat(r.certainty()).isEqualTo(0.5));
        assertThat(run("MATCH ()-[r:CARRIED_BY]->() RETURN r")).hasSize(1);

        assertThat(entities.addRelationship("sword", CARRIED_BY, "mira", StatementTime.between(100, 250), null)).isPresent();
        assertThat(entities.findRelationships("sword")).hasSize(2);
    }

    @Test
    void certaintyPeriodsAndVerbsAreValidated() {
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", null, 1.5))
                .isInstanceOf(InvalidStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", null, -0.1))
                .isInstanceOf(InvalidStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.during("aldric"), null))
                .isInstanceOf(InvalidStatementException.class).hasMessageContaining("Period");
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(300, 250), null))
                .isInstanceOf(InvalidStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", "carried by; DROP", "aldric", null, null))
                .isInstanceOf(InvalidStatementException.class);
        assertThat(entities.findRelationships("sword")).isEmpty();
        assertThat(entities.addRelationship("missing", CARRIED_BY, "aldric")).isEmpty();
        assertThat(entities.addRelationship("sword", CARRIED_BY, "missing")).isEmpty();
    }

    @Test
    void verbsAreMatchedCaseInsensitivelyAndStoredUpperCase() {
        assertThat(entities.addRelationship("sword", " carried_by ", "aldric", StatementTime.between(1, 5), null))
                .hasValueSatisfying(r -> assertThat(r.verb()).isEqualTo(CARRIED_BY));
        assertThat(entities.addRelationship("sword", "Carried_By", "mira", StatementTime.from(5), null)).isPresent();
        assertThat(run("MATCH ()-[r]->(:Entity) WHERE type(r) <> 'IS_A' RETURN DISTINCT type(r) AS t"))
                .extracting(row -> row.get("t").asString()).containsExactly(CARRIED_BY);
        assertThat(entities.findRelationships("sword")).extracting(Relationship::verb).containsOnly(CARRIED_BY);
        assertThat(entities.findByRelationship("carried_by", "aldric", 2L)).extracting(Entity::guid).containsExactly("sword");
        // Mixed case still overlaps with the stored upper-case edge.
        assertThatThrownBy(() -> entities.addRelationship("sword", "carried_BY", "mira", StatementTime.between(2, 3), null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", "FLIES_WITH", "aldric"))
                .isInstanceOf(InvalidStatementException.class)
                .hasMessageContaining("Allowed values: CARRIED_BY");
        assertThat(entities.findByRelationship("FLIES_WITH", "aldric")).isEmpty();
    }

    @Test
    void isATimeAndCertaintySurviveResave() {
        run("MATCH (:Entity {id:'sword'})-[r:IS_A]->() SET r.startTick = 120, r.certainty = 0.7");

        // The domain entity now carries the timed IS_A statements, so saving it back keeps them.
        Entity loaded = entities.findById("sword").orElseThrow();
        assertThat(loaded.archetypes()).singleElement().satisfies(statement -> {
            assertThat(statement.time()).isEqualTo(StatementTime.from(120));
            assertThat(statement.certainty()).isEqualTo(0.7);
        });
        entities.update(new Entity("sword", "w", "Sung Sword", List.of(), "renamed", Set.of(new ItemFacet(true, false)),
                loaded.archetypes()));

        assertThat(run("MATCH (:Entity {id:'sword'})-[r:IS_A]->(a) RETURN properties(r) AS p, a.unique AS u"))
                .singleElement().satisfies(row -> {
                    assertThat(row.get("p").asMap()).isEqualTo(Map.of("startTick", 120L, "certainty", 0.7));
                    assertThat(row.get("u").asBoolean()).isFalse();
                });
        assertThat(entities.findById("sword").orElseThrow().description()).isEqualTo("renamed");
    }
}
