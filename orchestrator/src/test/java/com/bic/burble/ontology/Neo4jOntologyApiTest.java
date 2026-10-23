package com.bic.burble.ontology;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.neo4j.Neo4jContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The timed relationship API with entities in Neo4j. Uses NEO4J_TEST_URI
 * (NEO4J_TEST_USER, NEO4J_TEST_PASSWORD) when set, else a Testcontainers
 * Neo4j when Docker is available, else the class is skipped.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Neo4jOntologyApiTest {

    private static String uri;
    private static String user = "neo4j";
    private static String password = "burble-test-pw";
    private static Neo4jContainer container;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void findNeo4j() {
        if (uri != null) {
            return;
        }
        String envUri = System.getenv("NEO4J_TEST_URI");
        if (envUri != null && !envUri.isBlank()) {
            uri = envUri;
            user = System.getenv().getOrDefault("NEO4J_TEST_USER", "neo4j");
            password = System.getenv().getOrDefault("NEO4J_TEST_PASSWORD", "");
        } else {
            boolean docker;
            try {
                docker = DockerClientFactory.instance().isDockerAvailable();
            } catch (Throwable ex) {
                docker = false;
            }
            Assumptions.assumeTrue(docker, "No Neo4j: set NEO4J_TEST_URI or make Docker available for Testcontainers");
            container = new Neo4jContainer("neo4j:5").withAdminPassword(password);
            container.start();
            uri = container.getBoltUrl();
        }
        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
             Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
    }

    @DynamicPropertySource
    static void neo4jStorage(DynamicPropertyRegistry registry) throws IOException {
        Path worlds = Files.createTempFile("burble-neo4j-api-", ".json");
        Files.deleteIfExists(worlds);
        registry.add("burble.ontology.persistance.world", () -> "json");
        registry.add("burble.ontology.persistance.entity", () -> "neo4j");
        registry.add("burble.ontology.persistance.json.path", () -> worlds.toAbsolutePath().toString());
        // application.yml's JSON-only section switches Neo4j auto-configuration
        // off; it is matched before test properties apply, so clear it here.
        registry.add("spring.autoconfigure.exclude", () -> "");
        registry.add("spring.neo4j.uri", () -> uri);
        registry.add("spring.neo4j.authentication.username", () -> user);
        registry.add("spring.neo4j.authentication.password", () -> password);
    }

    private String create(String path, String body, String idField, Object... vars) throws Exception {
        String response = mockMvc.perform(post(path, vars).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get(idField).asText();
    }

    @Test
    void timedCarriesWithPeriodsAndCertaintyThroughTheApi() throws Exception {
        String worldId = create("/api/v1/ontology/world", "{\"name\":\"Aethelgard\"}", "worldId");
        String base = "/api/v1/ontology/world/{worldId}/entity";
        String heroId = create(base, "{\"name\":\"Aldric\",\"character\":true}", "entityId", worldId);
        String miraId = create(base, "{\"name\":\"Mira\",\"character\":true}", "entityId", worldId);
        String swordId = create(base, "{\"name\":\"Sung Sword\",\"item\":{\"portable\":true,\"unique\":true}}", "entityId", worldId);
        String ageId = create(base, "{\"name\":\"Age of Imperials\",\"period\":{\"startTick\":200,\"endTick\":300}}", "entityId", worldId);

        mockMvc.perform(get(base + "/{entityId}", worldId, ageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facets[0].startTick").value(200))
                .andExpect(jsonPath("$.facets[0].endTick").value(300));

        String rel = base + "/{entityId}/relationship";
        mockMvc.perform(post(rel, worldId, swordId).contentType(MediaType.APPLICATION_JSON).content("""
                        {"verb":"CARRIED_BY","objectId":"%s","time":{"start":{"period":"%s"},"end":{"period":"%s"}},"certainty":0.8}
                        """.formatted(heroId, ageId, ageId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certainty").value(0.8));

        mockMvc.perform(post(rel, worldId, swordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"CARRIED_BY\",\"objectId\":\"%s\",\"time\":{\"start\":{\"tick\":250}}}".formatted(miraId)))
                .andExpect(status().isConflict());
        mockMvc.perform(post(rel, worldId, swordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"CARRIED_BY\",\"objectId\":\"%s\",\"certainty\":1.2}".formatted(miraId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(rel, worldId, swordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"CARRIED_BY\",\"objectId\":\"%s\",\"time\":{\"start\":{\"period\":\"%s\"}}}".formatted(miraId, heroId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(rel, worldId, swordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\" carried_By \",\"objectId\":\"%s\",\"time\":{\"start\":{\"tick\":300}}}".formatted(miraId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verb").value("CARRIED_BY"));
        mockMvc.perform(post(rel, worldId, swordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"FLIES_WITH\",\"objectId\":\"%s\"}".formatted(miraId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Allowed values: CARRIED_BY")));

        mockMvc.perform(get(rel, worldId, swordId).param("atTick", "250"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objectId").value(heroId));
        mockMvc.perform(get(rel, worldId, swordId).param("atTick", "300"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objectId").value(miraId));
        mockMvc.perform(get(rel, worldId, heroId))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(base + "/relationship", worldId)
                        .param("verb", "carried_by").param("objectId", heroId).param("atTick", "299"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guid").value(swordId));
        mockMvc.perform(get(base + "/relationship", worldId)
                        .param("verb", "CARRIED_BY").param("objectId", heroId).param("atTick", "300"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void timedArchetypesThroughTheApi() throws Exception {
        String worldId = create("/api/v1/ontology/world", "{\"name\":\"Timed\"}", "worldId");
        String base = "/api/v1/ontology/world/{worldId}/entity";
        // Plain names still work exactly as before; objects add time and certainty.
        String hero = create(base, """
                {"name":"Hero","archetypes":["Trait", {"archetype":"Character","time":{"start":{"tick":10},"end":{"tick":50}},"certainty":0.9}]}
                """, "entityId", worldId);
        String town = create(base, "{\"name\":\"Town\",\"archetypes\":[\"Location\"]}", "entityId", worldId);

        mockMvc.perform(get(base + "/{id}/archetype", worldId, hero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].archetype").value("Character"))
                .andExpect(jsonPath("$[0].time.start.tick").value(10))
                .andExpect(jsonPath("$[0].time.open").doesNotExist())
                .andExpect(jsonPath("$[0].time.start.empty").doesNotExist())
                .andExpect(jsonPath("$[0].certainty").value(0.9))
                .andExpect(jsonPath("$[1].archetype").value("Trait"))
                .andExpect(jsonPath("$[1].time").doesNotExist());
        mockMvc.perform(get(base + "/{id}/archetype", worldId, hero).param("atTick", "60"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].archetype").value("Trait"));
        mockMvc.perform(get(base + "/{id}", worldId, hero))
                .andExpect(jsonPath("$.archetypes.length()").value(2));

        // The relationship must sit inside the Character range.
        mockMvc.perform(post(base + "/{id}/relationship", worldId, hero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"LOCATED_IN\",\"objectId\":\"" + town + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString(
                        "it is not Character from the beginning until tick 10")));
        mockMvc.perform(post(base + "/{id}/relationship", worldId, hero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"LOCATED_IN\",\"objectId\":\"" + town + "\",\"time\":{\"start\":{\"tick\":20},\"end\":{\"tick\":30}}}"))
                .andExpect(status().isCreated());

        // Add a later Character range, reject an overlapping one, then remove it again.
        mockMvc.perform(post(base + "/{id}/archetype", worldId, hero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"archetype\":\"Character\",\"time\":{\"start\":{\"tick\":50}}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certainty").value(1.0));
        mockMvc.perform(post(base + "/{id}/archetype", worldId, hero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"archetype\":\"Character\",\"time\":{\"start\":{\"tick\":40}}}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(base + "/{id}/archetype", worldId, hero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"archetype\":\"Dragon\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete(base + "/{id}/archetype/Character", worldId, hero).param("startTick", "50"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(base + "/{id}/archetype/Character", worldId, hero).param("startTick", "50"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(base + "/{id}/archetype", worldId, hero).param("atTick", "60"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void compositeQueriesThroughTheApi() throws Exception {
        String worldId = create("/api/v1/ontology/world", "{\"name\":\"Composites\"}", "worldId");
        String base = "/api/v1/ontology/world/{worldId}/entity";
        String faith = create(base, "{\"name\":\"Faith\",\"archetypes\":[\"Worldview\"]}", "entityId", worldId);
        String guild = create(base, "{\"name\":\"Guild\",\"archetypes\":[\"Organization\"]}", "entityId", worldId);
        String ann = create(base, "{\"name\":\"Ann\",\"archetypes\":[\"Character\"]}", "entityId", worldId);
        mockMvc.perform(post(base + "/{id}/relationship", worldId, guild).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"BELIEVES\",\"objectId\":\"" + faith + "\",\"certainty\":0.8}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post(base + "/{id}/relationship", worldId, ann).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"BELIEVES\",\"objectId\":\"" + faith + "\",\"time\":{\"start\":{\"tick\":0},\"end\":{\"tick\":100}}}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post(base + "/{id}/relationship", worldId, ann).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"MEMBER_OF\",\"objectId\":\"" + guild + "\"}"))
                .andExpect(status().isCreated());

        String composites = "/api/v1/ontology/world/{worldId}/composite";
        mockMvc.perform(get(composites, worldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[5].name").value("magic-system"))
                .andExpect(jsonPath("$[1].rootArchetypes[0]").value("Worldview"))
                .andExpect(jsonPath("$[1].sections[0].key").value("followers"));
        mockMvc.perform(get(composites + "/{name}", worldId, "Religion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(faith))
                .andExpect(jsonPath("$[0].name").value("Faith"));
        mockMvc.perform(get(composites + "/{name}", worldId, "Magic System"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get(composites + "/{name}/{root}", worldId, "religion", faith))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.composite").value("religion"))
                .andExpect(jsonPath("$.root.name").value("Faith"))
                .andExpect(jsonPath("$.atTick").doesNotExist())
                .andExpect(jsonPath("$.sections[0].key").value("followers"))
                .andExpect(jsonPath("$.sections[0].entities[0].name").value("Ann"))
                .andExpect(jsonPath("$.sections[1].entities[0].name").value("Guild"))
                .andExpect(jsonPath("$.sections[1].entities[0].certainty").value(0.8))
                .andExpect(jsonPath("$.sections[1].entities[0].archetypes[0]").value("Organization"))
                .andExpect(jsonPath("$.sections[2].entities[0].via.length()").value(2))
                .andExpect(jsonPath("$.sections[2].entities[0].via[1].verb").value("MEMBER_OF"));
        mockMvc.perform(get(composites + "/{name}/{root}", worldId, "RELIGION", faith).param("atTick", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atTick").value(150))
                .andExpect(jsonPath("$.sections[0].entities.length()").value(0));

        mockMvc.perform(get(composites + "/{name}/{root}", worldId, "folklore", faith))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Unknown composite")));
        mockMvc.perform(get(composites + "/{name}", worldId, "folklore"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(composites + "/{name}/{root}", worldId, "religion", guild))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("starts at an entity that is Worldview")));
        mockMvc.perform(get(composites + "/{name}/{root}", worldId, "religion", "missing"))
                .andExpect(status().isNotFound());
    }
}
