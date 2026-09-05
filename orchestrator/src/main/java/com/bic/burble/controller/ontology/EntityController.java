package com.bic.burble.controller.ontology;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;
import com.bic.burble.ontology.domain.entity.CreateEntityResponse;
import com.bic.burble.ontology.service.EntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for entity management, backed by the {@code ontology}
 * module's {@link EntityService}. Orchestrator hosts all API endpoints;
 * ontology remains the library providing the underlying domain logic.
 */
@Tag(name = "Entity Management", description = "Endpoints for creating, reading, updating, and deleting entity records within a world")
@RestController
@RequestMapping("/api/v1/ontology/world/{worldId}/entity")
public class EntityController {

    private static final Logger log = LoggerFactory.getLogger(EntityController.class);

    private final EntityService entityService;

    public EntityController(EntityService entityService) {
        this.entityService = entityService;
    }

    @Operation(summary = "Get all entities in a world", description = "Retrieves a list of all entity records belonging to the given world.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of entities")
    @GetMapping
    public List<Entity> getAllEntities(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId) {
        log.info("GET  /api/v1/ontology/world/{}/entity - listing entities", worldId);
        List<Entity> entities = entityService.findAllByWorldId(worldId);
        log.debug("Found {} entity(ies) for worldId={}", entities.size(), worldId);
        return entities;
    }

    @Operation(summary = "Get an entity by ID", description = "Retrieves the full entity record for a given entity ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entity record found"),
        @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    @GetMapping("/{entityId}")
    public ResponseEntity<Entity> getEntity(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId,
            @Parameter(name = "entityId", description = "Unique GUID of the entity", required = true)
            @PathVariable("entityId") String entityId) {
        log.info("GET  /api/v1/ontology/world/{}/entity/{} - fetching entity", worldId, entityId);
        return entityService.findById(entityId)
                .map(entity -> {
                    log.debug("Entity found: {}", entity);
                    return ResponseEntity.ok(entity);
                })
                .orElseGet(() -> {
                    log.debug("No entity found for entityId={}", entityId);
                    return ResponseEntity.notFound().build();
                });
    }

    @Operation(summary = "Create a new entity", description = "Creates a new entity within a world and returns its generated GUID.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Entity successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<CreateEntityResponse> createEntity(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId,
            @Valid @RequestBody CreateEntityRequest request) {
        log.info("POST /api/v1/ontology/world/{}/entity - creating entity", worldId);
        log.debug("CreateEntityRequest payload: {}", request);
        String id = entityService.createEntity(worldId, request);
        log.info("Entity created with id={} in worldId={}", id, worldId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateEntityResponse(id));
    }

    @Operation(summary = "Update an entity", description = "Replaces an existing entity's fields and facets.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entity successfully updated"),
        @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    @PutMapping("/{entityId}")
    public ResponseEntity<Entity> updateEntity(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId,
            @Parameter(name = "entityId", description = "Unique GUID of the entity", required = true)
            @PathVariable("entityId") String entityId,
            @Valid @RequestBody CreateEntityRequest request) {
        log.info("PUT  /api/v1/ontology/world/{}/entity/{} - updating entity", worldId, entityId);
        log.debug("UpdateEntity payload: {}", request);
        return entityService.updateEntity(worldId, entityId, request)
                .map(entity -> {
                    log.info("Entity updated: entityId={}", entityId);
                    return ResponseEntity.ok(entity);
                })
                .orElseGet(() -> {
                    log.debug("Update failed, no entity found for entityId={}", entityId);
                    return ResponseEntity.notFound().build();
                });
    }

    @Operation(summary = "Delete an entity by ID", description = "Deletes an entity record from the ontology service by its GUID.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Entity successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    @DeleteMapping("/{entityId}")
    public ResponseEntity<Void> deleteEntity(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId,
            @Parameter(name = "entityId", description = "Unique GUID of the entity to delete", required = true)
            @PathVariable("entityId") String entityId) {
        log.info("DELETE /api/v1/ontology/world/{}/entity/{} - deleting entity", worldId, entityId);
        if (entityService.deleteById(entityId)) {
            log.info("Entity deleted: entityId={}", entityId);
            return ResponseEntity.noContent().build();
        }
        log.debug("Delete failed, no entity found for entityId={}", entityId);
        return ResponseEntity.notFound().build();
    }
}

