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
}
