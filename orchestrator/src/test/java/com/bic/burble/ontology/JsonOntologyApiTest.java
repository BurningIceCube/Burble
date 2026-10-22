package com.bic.burble.ontology;

import com.bic.burble.ontology.persistence.json.JsonFileOntologyGraphStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * World and entity HTTP API against the JSON-file store. A second store
 * opened on the same file stands in for a process restart.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JsonOntologyApiTest {

    private static final Path FILE = createFile();

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static Path createFile() {
        try {
            Path path = Files.createTempFile("burble-ontology-api-", ".json");
            Files.deleteIfExists(path);
            return path;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @DynamicPropertySource
    static void jsonStorage(DynamicPropertyRegistry registry) {
        registry.add("burble.ontology.persistance.world", () -> "json");
        registry.add("burble.ontology.persistance.entity", () -> "json");
        registry.add("burble.ontology.persistance.json.path", () -> FILE.toAbsolutePath().toString());
    }

    @Test
    void createWorldAndArchetypedEntityThenReadThemBackFromTheFile() throws Exception {
        Files.deleteIfExists(FILE);
        String createWorld = mockMvc.perform(post("/api/v1/ontology/world")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aethelgard\",\"description\":\"A high-fantasy world.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.worldId").exists())
                .andReturn().getResponse().getContentAsString();
        String worldId = objectMapper.readTree(createWorld).get("worldId").asText();

        String createEntity = mockMvc.perform(post("/api/v1/ontology/world/{worldId}/entity", worldId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sung Sword","aliases":["The Blade"],"description":"A sword that woke up.","character":true,"item":{"portable":true,"unique":false}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entityId").exists())
                .andReturn().getResponse().getContentAsString();
        String entityId = objectMapper.readTree(createEntity).get("entityId").asText();

        mockMvc.perform(get("/api/v1/ontology/world/{worldId}", worldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aethelgard"))
                .andExpect(jsonPath("$.owner").value("system"));

        mockMvc.perform(get("/api/v1/ontology/world/{worldId}/entity/{entityId}", worldId, entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sung Sword"))
                .andExpect(jsonPath("$.worldId").value(worldId))
                .andExpect(jsonPath("$.facets.length()").value(2));

        JsonNode root = JsonMapper.builder().build().readTree(FILE);
        assertThat(root.get("worlds")).hasSize(1);
        assertThat(root.get("entities")).hasSize(1);
        assertThat(root.get("entities").get(0).get("statements")).hasSize(2);
        assertThat(new JsonFileOntologyGraphStore(FILE).load().entities()).hasSize(1);
    }

    @Test
    void carriedByIsStoredOnTheSwordAndNotOnTheCharacter() throws Exception {
        Files.deleteIfExists(FILE);
        String createWorld = mockMvc.perform(post("/api/v1/ontology/world")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aethelgard\",\"description\":\"A high-fantasy world.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String worldId = objectMapper.readTree(createWorld).get("worldId").asText();

        String createHero = mockMvc.perform(post("/api/v1/ontology/world/{worldId}/entity", worldId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aldric\",\"character\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String heroId = objectMapper.readTree(createHero).get("entityId").asText();

        String createSword = mockMvc.perform(post("/api/v1/ontology/world/{worldId}/entity", worldId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sung Sword\",\"item\":{\"portable\":true,\"unique\":false},\"archetypes\":[\"Character\"]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String swordId = objectMapper.readTree(createSword).get("entityId").asText();

        mockMvc.perform(post("/api/v1/ontology/world/{worldId}/entity/{entityId}/relationship", worldId, swordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verb\":\"CARRIED_BY\",\"objectId\":\"%s\"}".formatted(heroId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verb").value("CARRIED_BY"))
                .andExpect(jsonPath("$.objectId").value(heroId));

        mockMvc.perform(get("/api/v1/ontology/world/{worldId}/entity/{entityId}/relationship", worldId, swordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].verb").value("CARRIED_BY"))
                .andExpect(jsonPath("$[0].objectId").value(heroId));

        mockMvc.perform(get("/api/v1/ontology/world/{worldId}/entity/{entityId}/relationship", worldId, heroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/ontology/world/{worldId}/entity/relationship", worldId)
                        .param("verb", "CARRIED_BY")
                        .param("objectId", heroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guid").value(swordId));

        mockMvc.perform(put("/api/v1/ontology/world/{worldId}/entity/{entityId}", worldId, swordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sung Sword\",\"item\":{\"portable\":true,\"unique\":true}}"))
                .andExpect(status().isOk());

        JsonNode root = JsonMapper.builder().build().readTree(FILE);
        int carriedBy = 0;
        int carrying = 0;
        for (JsonNode entity : root.get("entities")) {
            for (JsonNode statement : entity.get("statements")) {
                String verb = statement.get("verb").asString();
                if ("CARRIED_BY".equals(verb)) {
                    carriedBy++;
                    assertThat(entity.get("guid").asString()).isEqualTo(swordId);
                    assertThat(statement.get("object").asString()).isEqualTo(heroId);
                }
                if ("CARRYING".equals(verb)) {
                    carrying++;
                }
            }
        }
        assertThat(carriedBy).isEqualTo(1);
        assertThat(carrying).isEqualTo(0);
    }
}
