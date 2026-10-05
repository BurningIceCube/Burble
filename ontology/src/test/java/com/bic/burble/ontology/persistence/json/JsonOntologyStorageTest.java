package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.RelationshipVerbs;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.domain.world.WorldRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JsonOntologyStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void worldsAndEntityArchetypesSurviveANewStore() throws Exception {
        Path file = tempDir.resolve("ontology.json");
        JsonFileOntologyGraphStore store = new JsonFileOntologyGraphStore(file);
        JsonWorldRepositoryAdapter worlds = new JsonWorldRepositoryAdapter(store);
        JsonEntityRepositoryAdapter entities = new JsonEntityRepositoryAdapter(store);

        Date created = new Date(1_700_000_000_000L);
        WorldRecord world = new WorldRecord("world-1", "Aethelgard", "A high-fantasy world.", "system", created, created);
        worlds.save(world);

        Entity sword = new Entity(
                "entity-1",
                "world-1",
                "Sung Sword",
                List.of("The Blade"),
                "A sword that woke up.",
                Set.of(new ItemFacet(true, false), new CharacterFacet())
        );
        entities.save(sword);

        JsonNode entityNode = JsonMapper.builder().build().readTree(file).get("entities").get(0);
        assertThat(entityNode.has("type")).isFalse();
        assertThat(entityNode.has("facets")).isFalse();
        assertThat(entityNode.has("archetypes")).isFalse();
        assertThat(entityNode.get("worldId").asString()).isEqualTo("world-1");
        assertThat(entityNode.get("statements")).hasSize(2);
        assertThat(entityNode.get("statements")).anySatisfy(statement -> {
            assertThat(statement.get("verb").asString()).isEqualTo("IS_A");
            assertThat(statement.get("object").asString()).isEqualTo("Item");
            assertThat(statement.get("properties").get("portable").asBoolean()).isTrue();
            assertThat(statement.get("properties").get("unique").asBoolean()).isFalse();
        });
        assertThat(entityNode.get("statements")).anySatisfy(statement -> {
            assertThat(statement.get("verb").asString()).isEqualTo("IS_A");
            assertThat(statement.get("object").asString()).isEqualTo("Character");
        });

        JsonFileOntologyGraphStore reloaded = new JsonFileOntologyGraphStore(file);
        JsonWorldRepositoryAdapter reloadedWorlds = new JsonWorldRepositoryAdapter(reloaded);
        JsonEntityRepositoryAdapter reloadedEntities = new JsonEntityRepositoryAdapter(reloaded);

        WorldRecord loadedWorld = reloadedWorlds.findById("world-1").orElseThrow();
        assertThat(loadedWorld.name()).isEqualTo("Aethelgard");
        assertThat(loadedWorld.owner()).isEqualTo("system");
        assertThat(loadedWorld.createdAt()).isEqualTo(created);

        Entity loaded = reloadedEntities.findById("entity-1").orElseThrow();
        assertThat(loaded.aliases()).containsExactly("The Blade");
        assertThat(loaded.hasFacet(ItemFacet.class)).isTrue();
        assertThat(loaded.hasFacet(CharacterFacet.class)).isTrue();
        ItemFacet item = loaded.getFacet(ItemFacet.class).orElseThrow();
        assertThat(item.portable()).isTrue();
        assertThat(item.unique()).isFalse();
        assertThat(reloadedEntities.findAllByWorldId("world-1")).containsExactly(loaded);
    }

    @Test
    void savingAgainKeepsNonIsAStatementsAndReplacesArchetypes() {
        Path file = tempDir.resolve("ontology.json");
        JsonFileOntologyGraphStore store = new JsonFileOntologyGraphStore(file);
        store.save(new OntologyGraph(List.of(), List.of(new StoredEntity(
                "entity-1",
                "world-1",
                "Sung Sword",
                List.of(),
                "old",
                List.of(new StoredStatement("FORGED_BY", "entity-smith", Map.of("certainty", "told")))
        ))));

        JsonEntityRepositoryAdapter entities = new JsonEntityRepositoryAdapter(store);
        entities.update(new Entity(
                "entity-1",
                "world-1",
                "Sung Sword",
                List.of(),
                "A sword that woke up.",
                Set.of(new CharacterFacet())
        ));

        OntologyGraph graph = new JsonFileOntologyGraphStore(file).load();
        StoredEntity stored = graph.entities().get(0);
        assertThat(stored.statements()).anySatisfy(statement -> {
            assertThat(statement.verb()).isEqualTo("FORGED_BY");
            assertThat(statement.object()).isEqualTo("entity-smith");
        });
        assertThat(stored.statements()).anySatisfy(statement -> {
            assertThat(statement.verb()).isEqualTo("IS_A");
            assertThat(statement.object()).isEqualTo("Character");
        });
        assertThat(stored.statements()).noneMatch(statement -> "Item".equals(statement.object()));

        Entity domain = entities.findById("entity-1").orElseThrow();
        assertThat(domain.hasFacet(CharacterFacet.class)).isTrue();
        assertThat(domain.hasFacet(ItemFacet.class)).isFalse();
    }

    @Test
    void deletingAWorldRemovesItsEntitiesOnly() {
        Path file = tempDir.resolve("ontology.json");
        JsonFileOntologyGraphStore store = new JsonFileOntologyGraphStore(file);
        JsonWorldRepositoryAdapter worlds = new JsonWorldRepositoryAdapter(store);
        JsonEntityRepositoryAdapter entities = new JsonEntityRepositoryAdapter(store);

        Date now = new Date(1_700_000_000_000L);
        worlds.save(new WorldRecord("world-1", "One", null, "system", now, now));
        worlds.save(new WorldRecord("world-2", "Two", null, "system", now, now));
        entities.save(new Entity("entity-1", "world-1", "Sword", List.of(), null, Set.of(new ItemFacet(true, true))));
        entities.save(new Entity("entity-2", "world-2", "Town", List.of(), null, Set.of()));

        assertThat(worlds.deleteById("missing")).isFalse();
        assertThat(worlds.deleteById("world-1")).isTrue();

        OntologyGraph graph = new JsonFileOntologyGraphStore(file).load();
        assertThat(graph.worlds()).extracting(StoredWorld::guid).containsExactly("world-2");
        assertThat(graph.entities()).extracting(StoredEntity::guid).containsExactly("entity-2");
        assertThat(entities.deleteById("entity-2")).isTrue();
        assertThat(entities.findById("entity-2")).isEmpty();
    }

    @Test
    void oneEntityCanHoldEveryFormalArchetypeAsIsAStatements() {
        Path file = tempDir.resolve("ontology.json");
        JsonFileOntologyGraphStore store = new JsonFileOntologyGraphStore(file);
        JsonEntityRepositoryAdapter entities = new JsonEntityRepositoryAdapter(store);

        Set<ArchetypeFacet> facets = new HashSet<>();
        for (String name : ArchetypeFacets.NAMES) {
            if ("Item".equals(name)) {
                facets.add(new ItemFacet(true, true));
            } else {
                facets.add(ArchetypeFacets.of(name, null, null));
            }
        }
        assertThat(facets).hasSize(ArchetypeFacets.NAMES.size());

        entities.save(new Entity("entity-all", "world-1", "Everything", List.of(), null, facets));

        JsonFileOntologyGraphStore reloaded = new JsonFileOntologyGraphStore(file);
        StoredEntity stored = reloaded.load().entities().get(0);
        assertThat(stored.statements()).hasSize(ArchetypeFacets.NAMES.size());
        assertThat(stored.statements()).allMatch(statement -> "IS_A".equals(statement.verb()));
        assertThat(stored.statements()).extracting(StoredStatement::object)
                .containsExactlyInAnyOrderElementsOf(ArchetypeFacets.NAMES);
        assertThat(stored.statements()).anySatisfy(statement -> {
            assertThat(statement.object()).isEqualTo("Item");
            assertThat(statement.properties()).containsEntry("portable", true);
            assertThat(statement.properties()).containsEntry("unique", true);
        });

        Entity loaded = new JsonEntityRepositoryAdapter(reloaded).findById("entity-all").orElseThrow();
        assertThat(loaded.facets()).hasSize(ArchetypeFacets.NAMES.size());
        assertThat(loaded.hasFacet(ItemFacet.class)).isTrue();
        assertThat(loaded.hasFacet(CharacterFacet.class)).isTrue();
        assertThat(loaded.getFacet(ItemFacet.class).orElseThrow().portable()).isTrue();
        for (String name : ArchetypeFacets.NAMES) {
            if ("Item".equals(name)) {
                continue;
            }
            assertThat(loaded.facets()).anyMatch(facet -> name.equals(ArchetypeFacets.nameOf(facet)));
        }
    }

    @Test
    void carriedByIsStoredOnceOnTheSubjectAndSurvivesReload() {
        Path file = tempDir.resolve("ontology.json");
        JsonFileOntologyGraphStore store = new JsonFileOntologyGraphStore(file);
        JsonEntityRepositoryAdapter entities = new JsonEntityRepositoryAdapter(store);

        entities.save(new Entity(
                "sword", "world-1", "Sung Sword", List.of(), "A blade.",
                Set.of(new ItemFacet(true, false), new CharacterFacet())));
        entities.save(new Entity(
                "hero", "world-1", "Aldric", List.of(), "A knight.",
                Set.of(new CharacterFacet())));

        Relationship stored = entities.addRelationship("sword", RelationshipVerbs.CARRIED_BY, "hero").orElseThrow();
        assertThat(stored.verb()).isEqualTo(RelationshipVerbs.CARRIED_BY);
        assertThat(stored.objectId()).isEqualTo("hero");

        JsonFileOntologyGraphStore reloadedStore = new JsonFileOntologyGraphStore(file);
        JsonEntityRepositoryAdapter reloaded = new JsonEntityRepositoryAdapter(reloadedStore);
        OntologyGraph graph = reloadedStore.load();

        StoredEntity sword = graph.entities().stream().filter(entity -> "sword".equals(entity.guid())).findFirst().orElseThrow();
        StoredEntity hero = graph.entities().stream().filter(entity -> "hero".equals(entity.guid())).findFirst().orElseThrow();

        assertThat(sword.statements()).filteredOn(statement -> RelationshipVerbs.CARRIED_BY.equals(statement.verb()))
                .singleElement()
                .satisfies(statement -> assertThat(statement.object()).isEqualTo("hero"));
        assertThat(hero.statements()).noneMatch(statement -> RelationshipVerbs.CARRIED_BY.equals(statement.verb()));
        assertThat(hero.statements()).noneMatch(statement -> "CARRYING".equals(statement.verb()));
        assertThat(graph.entities().stream().flatMap(entity -> entity.statements().stream()))
                .filteredOn(statement -> "CARRYING".equals(statement.verb()))
                .isEmpty();
        assertThat(graph.entities().stream().flatMap(entity -> entity.statements().stream()))
                .filteredOn(statement -> RelationshipVerbs.CARRIED_BY.equals(statement.verb()))
                .hasSize(1);

        assertThat(reloaded.findRelationships("sword"))
                .containsExactly(new Relationship(RelationshipVerbs.CARRIED_BY, "hero"));
        assertThat(reloaded.findRelationships("hero")).isEmpty();
        assertThat(reloaded.findByRelationship(RelationshipVerbs.CARRIED_BY, "hero"))
                .extracting(Entity::guid)
                .containsExactly("sword");

        reloaded.update(new Entity(
                "sword", "world-1", "Sung Sword", List.of(), "A blade that woke up.",
                Set.of(new ItemFacet(true, false))));

        OntologyGraph afterArchetypeUpdate = new JsonFileOntologyGraphStore(file).load();
        StoredEntity swordAfter = afterArchetypeUpdate.entities().stream()
                .filter(entity -> "sword".equals(entity.guid())).findFirst().orElseThrow();
        StoredEntity heroAfter = afterArchetypeUpdate.entities().stream()
                .filter(entity -> "hero".equals(entity.guid())).findFirst().orElseThrow();
        assertThat(swordAfter.statements()).filteredOn(statement -> RelationshipVerbs.CARRIED_BY.equals(statement.verb()))
                .singleElement()
                .satisfies(statement -> assertThat(statement.object()).isEqualTo("hero"));
        assertThat(heroAfter.statements()).extracting(StoredStatement::verb).containsExactly("IS_A");
        assertThat(afterArchetypeUpdate.entities().stream().flatMap(entity -> entity.statements().stream()))
                .filteredOn(statement -> "CARRYING".equals(statement.verb()))
                .isEmpty();
    }
}
