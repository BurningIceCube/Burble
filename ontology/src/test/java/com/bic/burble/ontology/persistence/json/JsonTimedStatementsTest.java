package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.RelationshipVerbs;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.domain.facet.PeriodFacet;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeBound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.bic.burble.ontology.domain.RelationshipVerbs.CARRIED_BY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonTimedStatementsTest {

    @TempDir
    Path tempDir;

    private Path file;
    private JsonEntityRepositoryAdapter entities;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("ontology.json");
        entities = new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file));
        entities.save(new Entity("sword", "w", "Sung Sword", List.of(), null, Set.of(new ItemFacet(true, true))));
        entities.save(new Entity("aldric", "w", "Aldric", List.of(), null, Set.of(new CharacterFacet())));
        entities.save(new Entity("mira", "w", "Mira", List.of(), null, Set.of(new CharacterFacet())));
    }

    private List<String> carriersAt(JsonEntityRepositoryAdapter repo, long tick) {
        return repo.findRelationships("sword", tick).stream().map(Relationship::objectId).toList();
    }

    private List<String> carriedAt(JsonEntityRepositoryAdapter repo, String carrier, long tick) {
        return repo.findByRelationship(CARRIED_BY, carrier, tick).stream().map(Entity::guid).toList();
    }

    @Test
    void timedCarryHistoryAnswersWhoHadItAtATick() throws Exception {
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(250, 300), null);
        entities.addRelationship("sword", CARRIED_BY, "mira", StatementTime.from(300), 0.6);

        JsonEntityRepositoryAdapter reloaded = new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file));
        assertThat(carriersAt(reloaded, 249)).isEmpty();
        assertThat(carriersAt(reloaded, 250)).containsExactly("aldric");
        assertThat(carriersAt(reloaded, 299)).containsExactly("aldric");
        assertThat(carriersAt(reloaded, 300)).containsExactly("mira");
        assertThat(carriersAt(reloaded, 10_000)).containsExactly("mira");
        assertThat(carriedAt(reloaded, "aldric", 280)).containsExactly("sword");
        assertThat(carriedAt(reloaded, "aldric", 300)).isEmpty();
        assertThat(carriedAt(reloaded, "mira", 300)).containsExactly("sword");

        List<Relationship> all = reloaded.findRelationships("sword");
        assertThat(all).hasSize(2);
        assertThat(all).anySatisfy(r -> {
            assertThat(r.objectId()).isEqualTo("mira");
            assertThat(r.certainty()).isEqualTo(0.6);
            assertThat(r.time().start().tick()).isEqualTo(300L);
            assertThat(r.time().end()).isNull();
        });
        assertThat(all).anySatisfy(r -> {
            assertThat(r.objectId()).isEqualTo("aldric");
            assertThat(r.certainty()).isEqualTo(1.0);
        });

        JsonNode aldric = null;
        for (JsonNode entity : JsonMapper.builder().build().readTree(file).get("entities")) {
            if (!"sword".equals(entity.get("guid").asString())) {
                continue;
            }
            for (JsonNode statement : entity.get("statements")) {
                if ("aldric".equals(statement.path("object").asString())) {
                    aldric = statement;
                }
            }
        }
        assertThat(aldric).isNotNull();
        assertThat(aldric.get("time").get("start").get("tick").asLong()).isEqualTo(250);
        assertThat(aldric.get("time").get("end").get("tick").asLong()).isEqualTo(300);
        assertThat(aldric.has("certainty")).isFalse();
        assertThat(aldric.has("properties")).isFalse();
    }

    @Test
    void periodOnlyBoundsResolveToThePeriodsTicks() {
        entities.save(new Entity("age-imperials", "w", "Age of Imperials", List.of(), null,
                Set.of(new PeriodFacet(200L, 400L))));
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.during("age-imperials"), 0.9);

        assertThat(carriersAt(entities, 199)).isEmpty();
        assertThat(carriersAt(entities, 200)).containsExactly("aldric");
        assertThat(carriersAt(entities, 399)).containsExactly("aldric");
        assertThat(carriersAt(entities, 400)).isEmpty();

        // A tick in the bound wins over the period, which stays as a label.
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "mira",
                new StatementTime(new TimeBound(350L, "age-imperials"), null), null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "mira",
                new StatementTime(new TimeBound(null, "age-imperials"), null), null))
                .isInstanceOf(OverlappingStatementException.class);
        // Starting where the period ends is a hand-off.
        assertThat(entities.addRelationship("sword", CARRIED_BY, "mira",
                new StatementTime(new TimeBound(400L, null), null), null)).isPresent();
        assertThat(carriersAt(entities, 400)).containsExactly("mira");
    }

    @Test
    void periodFacetTicksSurviveReload() {
        entities.save(new Entity("age", "w", "Age", List.of(), null, Set.of(new PeriodFacet(1L, 9L))));
        Entity loaded = new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file)).findById("age").orElseThrow();
        assertThat(loaded.getFacet(PeriodFacet.class)).contains(new PeriodFacet(1L, 9L));
    }

    @Test
    void unknownPeriodAndBackwardsRangesAreRejected() {
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric",
                StatementTime.during("aldric"), null))
                .isInstanceOf(InvalidStatementException.class)
                .hasMessageContaining("Period");
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric",
                StatementTime.between(300, 250), null))
                .isInstanceOf(InvalidStatementException.class);
        assertThat(entities.findRelationships("sword")).isEmpty();
    }

    @Test
    void certaintyMustBeBetweenZeroAndOne() {
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", null, 1.5))
                .isInstanceOf(InvalidStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", null, -0.1))
                .isInstanceOf(InvalidStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", null, Double.NaN))
                .isInstanceOf(InvalidStatementException.class);
        assertThat(entities.findRelationships("sword")).isEmpty();

        assertThat(entities.addRelationship("sword", CARRIED_BY, "aldric", null, 0.0)).isPresent();
        assertThat(entities.findRelationships("sword")).singleElement()
                .satisfies(r -> assertThat(r.certainty()).isEqualTo(0.0));
    }

    @Test
    void overlappingCarriesAreRejectedAndNothingIsWritten() {
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(250, 300), null);

        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "mira", StatementTime.between(299, 400), null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "mira", null, null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThatThrownBy(() -> entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(290, 310), null))
                .isInstanceOf(OverlappingStatementException.class);
        assertThat(entities.findRelationships("sword")).hasSize(1);

        // Same statement again only updates its certainty.
        entities.addRelationship("sword", CARRIED_BY, "aldric", StatementTime.between(250, 300), 0.5);
        assertThat(entities.findRelationships("sword")).singleElement()
                .satisfies(r -> assertThat(r.certainty()).isEqualTo(0.5));

        // Touching ranges are a hand-off, not an overlap.
        assertThat(entities.addRelationship("sword", CARRIED_BY, "mira", StatementTime.between(100, 250), null)).isPresent();
        assertThat(entities.findRelationships("sword")).hasSize(2);
    }

    @Test
    void untimedCarryStillHoldsAtEveryTick() {
        entities.addRelationship("sword", RelationshipVerbs.CARRIED_BY, "aldric");
        assertThat(carriersAt(entities, -5)).containsExactly("aldric");
        assertThat(carriersAt(entities, 300)).containsExactly("aldric");
        assertThat(entities.findRelationships("sword"))
                .containsExactly(new Relationship(CARRIED_BY, "aldric"));
    }

    @Test
    void archetypeTimeAndCertaintySurviveSavingTheEntityAgain() {
        new JsonFileOntologyGraphStore(file).update(graph -> {
            StoredEntity sword = graph.entities().stream().filter(e -> "sword".equals(e.guid())).findFirst().orElseThrow();
            List<StoredStatement> statements = sword.statements().stream()
                    .map(s -> s.withTiming(StatementTime.from(120), 0.7)).toList();
            graph.entities().remove(sword);
            graph.entities().add(new StoredEntity(sword.guid(), sword.worldId(), sword.name(), sword.aliases(),
                    sword.description(), statements));
            return null;
        });

        // The domain entity now carries the timed IS_A statements, so saving it back keeps them.
        Entity loaded = entities.findById("sword").orElseThrow();
        assertThat(loaded.archetypes()).singleElement().satisfies(statement -> {
            assertThat(statement.time()).isEqualTo(StatementTime.from(120));
            assertThat(statement.certainty()).isEqualTo(0.7);
        });
        entities.update(new Entity("sword", "w", "Sung Sword", List.of(), "renamed", Set.of(new ItemFacet(true, false)),
                loaded.archetypes()));

        StoredStatement isA = new JsonFileOntologyGraphStore(file).load().entities().stream()
                .filter(e -> "sword".equals(e.guid())).findFirst().orElseThrow()
                .statements().get(0);
        assertThat(isA.object()).isEqualTo("Item");
        assertThat(isA.time()).isEqualTo(StatementTime.from(120));
        assertThat(isA.certainty()).isEqualTo(0.7);
        assertThat(isA.properties()).containsEntry("unique", false);
    }

    @Test
    void jsonWrittenBeforeTimeAndCertaintyStillLoads() throws Exception {
        Path old = tempDir.resolve("old.json");
        Files.writeString(old, """
                {
                  "worlds": [],
                  "entities": [
                    { "guid": "sword", "worldId": "w", "name": "Sung Sword", "aliases": [], "description": null,
                      "statements": [
                        { "verb": "IS_A", "object": "Item", "properties": { "portable": true, "unique": false } },
                        { "verb": "IS_A", "object": "Period" },
                        { "verb": "CARRIED_BY", "object": "aldric", "properties": {} }
                      ] }
                  ]
                }
                """);
        JsonEntityRepositoryAdapter repo = new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(old));

        Entity sword = repo.findById("sword").orElseThrow();
        assertThat(sword.getFacet(ItemFacet.class)).contains(new ItemFacet(true, false));
        assertThat(sword.getFacet(PeriodFacet.class)).contains(new PeriodFacet());
        assertThat(repo.findRelationships("sword")).containsExactly(new Relationship(CARRIED_BY, "aldric"));
        assertThat(repo.findRelationships("sword", 300L)).hasSize(1);
        assertThat(repo.findByRelationship(CARRIED_BY, "aldric", 0L)).extracting(Entity::guid).containsExactly("sword");
    }
}
