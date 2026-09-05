package com.bic.burble.ontology;

import com.bic.burble.ontology.domain.world.CreateWorldRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link com.bic.burble.controller.ontology.WorldController},
 * exercising the full stack (controller -> service -> H2-backed repository)
 * through {@link MockMvc}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorldControllerTest {

    private static final String BASE_URL = "/api/v1/ontology/world";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createGetListAndDeleteWorld() throws Exception {
        CreateWorldRequest request = new CreateWorldRequest("Aethelgard", "A high-fantasy world.");

        String createResponse = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.worldId").exists())
                .andReturn().getResponse().getContentAsString();

        String worldId = objectMapper.readTree(createResponse).get("worldId").asText();

        mockMvc.perform(get(BASE_URL + "/{worldId}", worldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guid").value(worldId))
                .andExpect(jsonPath("$.name").value("Aethelgard"))
                .andExpect(jsonPath("$.description").value("A high-fantasy world."))
                .andExpect(jsonPath("$.owner").value("system"));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.guid == '" + worldId + "')]", hasSize(1)));

        mockMvc.perform(delete(BASE_URL + "/{worldId}", worldId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/{worldId}", worldId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWorldWithNoBodyStillSucceeds() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.worldId").exists());
    }

    @Test
    void createWorldWithBlankNameReturnsBadRequest() throws Exception {
        CreateWorldRequest invalidRequest = new CreateWorldRequest("", "Missing name");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWorldWithUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{worldId}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWorldWithUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{worldId}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }
}
