package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.RelationshipType;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.persistence.json.JsonEntityRepositoryAdapter;
import com.bic.burble.ontology.persistence.json.JsonFileOntologyGraphStore;
import com.bic.burble.ontology.service.impl.DefaultEntityService;
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

/** The closed relationship type list, through the service and the JSON store. */
class RelationshipVerbRuleTest {

    @TempDir
    Path tempDir;

    private Path file;
    private JsonEntityRepositoryAdapter repository;
    private DefaultEntityService service;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("ontology.json");
        repository = new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file));
        service = new DefaultEntityService(repository);
        repository.save(new Entity("sword", "w", "Sword", List.of(), null, Set.of(new ItemFacet(true, true))));
        repository.save(new Entity("hero", "w", "Hero", List.of(), null, Set.of(new CharacterFacet())));
        repository.save(new Entity("mira", "w", "Mira", List.of(), null, Set.of(new CharacterFacet())));
    }

    @Test
    void typesAreMatchedCaseInsensitivelyAfterTrimming() {
        assertThat(RelationshipType.parse("CARRIED_BY")).isEqualTo(RelationshipType.CARRIED_BY);
        assertThat(RelationshipType.parse("carried_by")).isEqualTo(RelationshipType.CARRIED_BY);
        assertThat(RelationshipType.parse("  Carried_By ")).isEqualTo(RelationshipType.CARRIED_BY);
        assertThat(RelationshipType.find("IS_A")).isEmpty();
        assertThat(RelationshipType.find("carried by")).isEmpty();
        assertThat(RelationshipType.canonicalOrSelf("carried_by")).isEqualTo(CARRIED_BY);
        assertThat(RelationshipType.canonicalOrSelf("FORGED_BY")).isEqualTo("FORGED_BY");
    }

    @Test
    void lowerAndMixedCaseInputIsStoredAsCarriedBy() throws Exception {
        assertThat(service.addRelationship("w", "sword", "carried_by", "hero", StatementTime.between(1, 5), null))
                .hasValueSatisfying(r -> assertThat(r.verb()).isEqualTo(CARRIED_BY));
        assertThat(repository.addRelationship("sword", " Carried_By ", "mira", StatementTime.from(5), null))
                .hasValueSatisfying(r -> assertThat(r.verb()).isEqualTo(CARRIED_BY));

        for (JsonNode entity : JsonMapper.builder().build().readTree(file).get("entities")) {
            for (JsonNode statement : entity.get("statements")) {
                if (!"IS_A".equals(statement.get("verb").asString())) {
                    assertThat(statement.get("verb").asString()).isEqualTo(CARRIED_BY);
                }
            }
        }
        assertThat(service.findByRelationship("carried_BY", "hero", 2L)).extracting(Entity::guid).containsExactly("sword");
        assertThat(service.findRelationships("sword")).extracting(Relationship::verb).containsOnly(CARRIED_BY);
        assertThatThrownBy(() -> service.addRelationship("w", "sword", "CARRIED_by", "mira", StatementTime.between(2, 3), null))
                .isInstanceOf(OverlappingStatementException.class);
    }

    @Test
    void unknownVerbsAreRejectedWithTheAllowedList() {
        for (String verb : List.of("MEMBER_OF", "IS_A", "is_a", "carried by", "")) {
            assertThatThrownBy(() -> service.addRelationship("w", "sword", verb, "hero"))
                    .as(verb)
                    .isInstanceOf(InvalidStatementException.class)
                    .hasMessageContaining("Allowed values: CARRIED_BY");
        }
        assertThatThrownBy(() -> repository.addRelationship("sword", "MEMBER_OF", "hero"))
                .isInstanceOf(InvalidStatementException.class);
        assertThatThrownBy(() -> service.findByRelationship("MEMBER_OF", "hero"))
                .isInstanceOf(InvalidStatementException.class);
        // Checked before looking up the entities.
        assertThatThrownBy(() -> service.addRelationship("w", "missing", "MEMBER_OF", "hero"))
                .isInstanceOf(InvalidStatementException.class);
        assertThat(repository.findRelationships("sword")).isEmpty();
    }

    @Test
    void olderFilesLoadWithKnownVerbsUpperCasedAndUnknownVerbsKept() throws Exception {
        Files.writeString(file, """
                {"worlds": [], "entities": [
                  {"guid": "sword", "worldId": "w", "name": "Sword", "statements": [
                    {"verb": "IS_A", "object": "Item"},
                    {"verb": "carried_by", "object": "hero", "time": {"end": {"tick": 10}}},
                    {"verb": "FORGED_BY", "object": "mira"}
                  ]},
                  {"guid": "hero", "worldId": "w", "name": "Hero", "statements": []},
                  {"guid": "mira", "worldId": "w", "name": "Mira", "statements": []}
                ]}
                """);
        JsonEntityRepositoryAdapter reloaded = new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file));
        DefaultEntityService reloadedService = new DefaultEntityService(reloaded);

        assertThat(reloaded.findRelationships("sword")).extracting(Relationship::verb)
                .containsExactly(CARRIED_BY, "FORGED_BY");
        assertThat(reloadedService.findByRelationship("CARRIED_BY", "hero", 5L)).extracting(Entity::guid)
                .containsExactly("sword");
        // The old lower-case carry still counts for overlaps.
        assertThatThrownBy(() -> reloadedService.addRelationship("w", "sword", "CARRIED_BY", "mira",
                StatementTime.between(5, 20), null))
                .isInstanceOf(OverlappingStatementException.class);

        // Saving the entity again keeps both old statements.
        reloaded.update(new Entity("sword", "w", "Sword", List.of(), "d", Set.of(new ItemFacet(true, false))));
        assertThat(reloaded.findRelationships("sword")).extracting(Relationship::verb)
                .containsExactly(CARRIED_BY, "FORGED_BY");
    }
}
