package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.ArchetypeStatement;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.persistence.json.JsonEntityRepositoryAdapter;
import com.bic.burble.ontology.persistence.json.JsonFileOntologyGraphStore;
import com.bic.burble.ontology.service.impl.DefaultEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsonTimedArchetypeTest extends TimedArchetypeContract {

    @TempDir
    Path tempDir;

    private Path file;

    @Override
    protected EntityRepository freshRepository() {
        file = tempDir.resolve("ontology-" + System.nanoTime() + ".json");
        return new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file));
    }

    @Test
    void untimedIsAInOlderFilesHoldsAlways() throws Exception {
        Files.writeString(file, """
                {"worlds": [], "entities": [
                  {"guid": "sword", "worldId": "w", "name": "Sword", "statements": [
                    {"verb": "IS_A", "object": "Item", "properties": {"portable": true}}]},
                  {"guid": "hero", "worldId": "w", "name": "Hero", "statements": [{"verb": "IS_A", "object": "Character"}]},
                  {"guid": "old", "worldId": "w", "name": "Old", "statements": [
                    {"verb": "IS_A", "object": "Character", "time": {"start": {"tick": 5, "empty": false}, "open": false}}]}
                ]}
                """);
        DefaultEntityService reloaded = new DefaultEntityService(
                new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(file)));
        assertThat(reloaded.findArchetypes("w", "hero", 1L).orElseThrow())
                .containsExactly(ArchetypeStatement.always("Character"));
        assertThat(reloaded.findArchetypes("w", "old", null).orElseThrow())
                .containsExactly(new ArchetypeStatement("Character", StatementTime.from(5), 1.0));
        assertThat(reloaded.addRelationship("w", "sword", "CARRIED_BY", "hero")).isPresent();
    }
}
