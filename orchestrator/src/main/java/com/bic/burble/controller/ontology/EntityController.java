package com.bic.burble.controller.ontology;

import com.bic.burble.ontology.domain.ArchetypeStatement;
import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.entity.ArchetypeRequest;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeBound;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.entity.AddRelationshipRequest;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;
import com.bic.burble.ontology.domain.entity.CreateEntityResponse;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
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
import java.util.Map;

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

    @Operation(summary = "Link this entity to another",
            description = "Stores one relationship statement on this entity. The other entity is not updated, so the inverse is a query rather than a second statement.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Relationship stored on the subject entity"),
        @ApiResponse(responseCode = "400", description = "Certainty outside 0..1, unknown period, or a range that ends before it starts"),
        @ApiResponse(responseCode = "409", description = "A CARRIED_BY statement on this entity already covers part of that time"),
        @ApiResponse(responseCode = "404", description = "Subject or object entity was not found in this world")
    })
    @PostMapping("/{entityId}/relationship")
    public ResponseEntity<?> addRelationship(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId,
            @Parameter(name = "entityId", description = "Subject entity the statement is stored on", required = true)
            @PathVariable("entityId") String entityId,
            @Valid @RequestBody AddRelationshipRequest request) {
        log.info("POST /api/v1/ontology/world/{}/entity/{}/relationship - {} -> {}",
                worldId, entityId, request.verb(), request.objectId());
        try {
            return entityService.addRelationship(worldId, entityId, request.verb(), request.objectId(),
                            request.time(), request.certainty())
                    .<ResponseEntity<?>>map(relationship -> ResponseEntity.status(HttpStatus.CREATED).body(relationship))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (OverlappingStatementException ex) {
            log.debug("Relationship rejected, overlapping time: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (InvalidStatementException ex) {
            log.debug("Relationship rejected: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "List relationships stored on an entity",
            description = "Returns relationship statements whose subject is this entity. IS_A archetype statements are not included.")
    @ApiResponse(responseCode = "200", description = "Relationships stored on the entity")
    @GetMapping("/{entityId}/relationship")
    public List<Relationship> getRelationships(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId,
            @Parameter(name = "entityId", description = "Subject entity", required = true)
            @PathVariable("entityId") String entityId,
            @Parameter(name = "atTick", description = "Only statements that hold at this tick. Leave out for all.")
            @RequestParam(value = "atTick", required = false) Long atTick) {
        log.info("GET  /api/v1/ontology/world/{}/entity/{}/relationship atTick={}", worldId, entityId, atTick);
        return entityService.findRelationships(entityId, atTick);
    }

    @Operation(summary = "Query the inverse of a stored relationship",
            description = "Finds entities that store verb pointing at objectId. Carrying is this query for CARRIED_BY, not a stored statement.")
    @ApiResponse(responseCode = "200", description = "Entities with a matching outgoing statement")
    @GetMapping("/relationship")
    public List<Entity> findByRelationship(
            @Parameter(name = "worldId", description = "Unique GUID of the world", required = true)
            @PathVariable("worldId") String worldId,
            @Parameter(name = "verb", description = "Relationship verb, matched case-insensitively. Allowed: CARRIED_BY", required = true)
            @RequestParam("verb") String verb,
            @Parameter(name = "objectId", description = "Entity id the statement points at", required = true)
            @RequestParam("objectId") String objectId,
            @Parameter(name = "atTick", description = "Only statements that hold at this tick. Leave out for all.")
            @RequestParam(value = "atTick", required = false) Long atTick) {
        log.info("GET  /api/v1/ontology/world/{}/entity/relationship?verb={}&objectId={}&atTick={}",
                worldId, verb, objectId, atTick);
        return entityService.findByRelationship(verb, objectId, atTick).stream()
                .filter(entity -> worldId.equals(entity.worldId()))
                .toList();
    }

    @Operation(summary = "List an entity's archetypes",
            description = "Returns the entity's IS_A statements with their time and certainty. Untimed statements hold always.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "IS_A statements on the entity"),
        @ApiResponse(responseCode = "404", description = "Entity was not found in this world")
    })
    @GetMapping("/{entityId}/archetype")
    public ResponseEntity<List<ArchetypeStatement>> getArchetypes(
            @PathVariable("worldId") String worldId,
            @PathVariable("entityId") String entityId,
            @Parameter(name = "atTick", description = "Only archetypes that hold at this tick. Leave out for all.")
            @RequestParam(value = "atTick", required = false) Long atTick) {
        log.info("GET  /api/v1/ontology/world/{}/entity/{}/archetype atTick={}", worldId, entityId, atTick);
        return entityService.findArchetypes(worldId, entityId, atTick)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Add one archetype statement",
            description = "Adds an IS_A statement with an optional time and certainty. The same archetype may be added again for a time that does not overlap. The same archetype and time again updates the certainty.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "IS_A statement stored"),
        @ApiResponse(responseCode = "400", description = "Unknown archetype, certainty outside 0..1, unknown period, or a range that ends before it starts"),
        @ApiResponse(responseCode = "409", description = "The entity already has this archetype during part of that time"),
        @ApiResponse(responseCode = "404", description = "Entity was not found in this world")
    })
    @PostMapping("/{entityId}/archetype")
    public ResponseEntity<ArchetypeStatement> addArchetype(
            @PathVariable("worldId") String worldId,
            @PathVariable("entityId") String entityId,
            @RequestBody ArchetypeRequest request) {
        log.info("POST /api/v1/ontology/world/{}/entity/{}/archetype - {}", worldId, entityId, request);
        return entityService.addArchetype(worldId, entityId, request)
                .map(statement -> ResponseEntity.status(HttpStatus.CREATED).body(statement))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Remove one archetype statement",
            description = "Removes the IS_A statement with this archetype and exactly this time. Leave all time parameters out to remove the untimed one.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "IS_A statement removed"),
        @ApiResponse(responseCode = "404", description = "Entity or matching statement was not found")
    })
    @DeleteMapping("/{entityId}/archetype/{archetype}")
    public ResponseEntity<Void> removeArchetype(
            @PathVariable("worldId") String worldId,
            @PathVariable("entityId") String entityId,
            @PathVariable("archetype") String archetype,
            @RequestParam(value = "startTick", required = false) Long startTick,
            @RequestParam(value = "startPeriod", required = false) String startPeriod,
            @RequestParam(value = "endTick", required = false) Long endTick,
            @RequestParam(value = "endPeriod", required = false) String endPeriod) {
        log.info("DELETE /api/v1/ontology/world/{}/entity/{}/archetype/{}", worldId, entityId, archetype);
        StatementTime time = new StatementTime(new TimeBound(startTick, startPeriod), new TimeBound(endTick, endPeriod));
        return entityService.removeArchetype(worldId, entityId, archetype, time)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** Unknown relationship verbs, bad certainty or bad time ranges are client errors. */
    @ExceptionHandler(InvalidStatementException.class)
    public ResponseEntity<Map<String, String>> invalidStatement(InvalidStatementException ex) {
        HttpStatus status = ex instanceof OverlappingStatementException ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }
}
