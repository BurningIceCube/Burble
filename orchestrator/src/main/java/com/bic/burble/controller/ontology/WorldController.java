package com.bic.burble.controller.ontology;

import com.bic.burble.ontology.domain.world.CreateWorldRequest;
import com.bic.burble.ontology.domain.world.CreateWorldResponse;
import com.bic.burble.ontology.domain.world.WorldRecord;
import com.bic.burble.ontology.service.WorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for world management, backed by the {@code ontology}
 * module's {@link WorldService}. Orchestrator hosts all API endpoints;
 * ontology remains the library providing the underlying domain logic.
 */
@Tag(name = "World Management", description = "Endpoints for creating, reading, and deleting world records")
@RestController
@RequestMapping("/api/v1/ontology/world")
public class WorldController {

    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    @Operation(summary = "Get all worlds", description = "Retrieves a list of all world records stored in the ontology service.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of worlds")
    @GetMapping
    public List<WorldRecord> getAllWorlds() {
        return worldService.getAllWorlds();
    }

    @Operation(summary = "Get a world by ID", description = "Retrieves the full world record for a given world ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "World record found"),
        @ApiResponse(responseCode = "404", description = "World not found")
    })
    @GetMapping("/{worldId}")
    public ResponseEntity<WorldRecord> getWorld(
            @Parameter(name = "worldId", description = "Unique GUID of the world", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable("worldId") String worldId) {
        return worldService.getWorld(worldId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new world", description = "Creates a new world entity and returns its generated GUID.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "World successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<CreateWorldResponse> createWorld(@Valid @RequestBody(required = false) CreateWorldRequest request) {
        String id = worldService.createWorld(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateWorldResponse(id));
    }

    @Operation(summary = "Delete a world by ID", description = "Deletes a world record from the ontology service by its GUID.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "World successfully deleted"),
        @ApiResponse(responseCode = "404", description = "World not found")
    })
    @DeleteMapping("/{worldId}")
    public ResponseEntity<Void> deleteWorld(
            @Parameter(name = "worldId", description = "Unique GUID of the world to delete", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable("worldId") String worldId) {
        if (worldService.deleteWorld(worldId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
