package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.persistence.json.JsonEntityRepositoryAdapter;
import com.bic.burble.ontology.persistence.json.JsonFileOntologyGraphStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRelationshipCatalogTest extends RelationshipCatalogContract {

    @TempDir
    Path tempDir;

    private Path file;

    @Override
    protected EntityRepository freshRepository() {
        file = tempDir.resolve("ontology-" + System.nanoTime() + ".json");
        return new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file));
    }

    @Test
    void statementsThatBreakTheCatalogStillLoadFromOlderFiles() throws Exception {
        Files.writeString(file, """
                {"worlds": [], "entities": [
                  {"guid": "town", "worldId": "w", "name": "Town", "statements": [
                    {"verb": "IS_A", "object": "Location"},
                    {"verb": "CARRIED_BY", "object": "town"},
                    {"verb": "member_of", "object": "hero"}
                  ]},
                  {"guid": "hero", "worldId": "w", "name": "Hero", "statements": [{"verb": "IS_A", "object": "Character"}]}
                ]}
                """);
        EntityRepository reloaded = new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file));
        assertThat(reloaded.findRelationships("town"))
                .containsExactly(new Relationship("CARRIED_BY", "town"), new Relationship("MEMBER_OF", "hero"));
    }
}
