package com.bic.burble.controller.ontology;

import com.bic.burble.ontology.domain.composite.CompositeView;
import com.bic.burble.ontology.domain.composite.CompositeView.CompositeInfo;
import com.bic.burble.ontology.domain.composite.CompositeView.EntitySummary;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.service.CompositeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Read-only composite views such as Culture or Government. Composites are
 * not stored, so there is nothing to create, update or delete; each request
 * computes the view from archetypes and relationships.
 */
@Tag(name = "Composites", description = "Read-only views like Culture or Government, computed from archetypes and relationships")
@RestController
@RequestMapping("/api/v1/ontology/world/{worldId}/composite")
public class CompositeController {

    private static final Logger log = LoggerFactory.getLogger(CompositeController.class);

    private final CompositeService composites;

    public CompositeController(CompositeService composites) {
        this.composites = composites;
    }

    @Operation(summary = "List the composites", description = "Every composite with a plain-language description, the archetypes it can start from and its sections.")
    @GetMapping
    public List<CompositeInfo> listComposites(@PathVariable("worldId") String worldId) {
        log.info("GET  /api/v1/ontology/world/{}/composite", worldId);
        return composites.listComposites();
    }

    @Operation(summary = "Find entities a composite can start from",
            description = "Entities in this world that have one of the composite's root archetypes, so you can pick one without knowing ids. Names ignore case, spaces and dashes: culture, Magic System, magic-system.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Candidate root entities"),
        @ApiResponse(responseCode = "400", description = "Unknown composite name")
    })
    @GetMapping("/{name}")
    public List<EntitySummary> findRoots(
            @PathVariable("worldId") String worldId,
            @PathVariable("name") String name,
            @Parameter(description = "Only roots that have the archetype at this tick. Leave out for any time.")
            @RequestParam(value = "atTick", required = false) Long atTick) {
        log.info("GET  /api/v1/ontology/world/{}/composite/{} atTick={}", worldId, name, atTick);
        return composites.findRoots(worldId, name, atTick);
    }

    @Operation(summary = "Show a composite",
            description = "The composite rooted at one entity, grouped into sections. Each entity lists its archetypes, the certainty of the statement that linked it in, and the statements followed to reach it. With atTick, only statements and archetypes that hold at that tick count.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The computed view"),
        @ApiResponse(responseCode = "400", description = "Unknown composite name, or the root does not have a root archetype"),
        @ApiResponse(responseCode = "404", description = "Root entity was not found in this world")
    })
    @GetMapping("/{name}/{rootEntityId}")
    public ResponseEntity<CompositeView> query(
            @PathVariable("worldId") String worldId,
            @PathVariable("name") String name,
            @PathVariable("rootEntityId") String rootEntityId,
            @Parameter(description = "Compute the view at this tick. Leave out for all time.")
            @RequestParam(value = "atTick", required = false) Long atTick) {
        log.info("GET  /api/v1/ontology/world/{}/composite/{}/{} atTick={}", worldId, name, rootEntityId, atTick);
        return composites.query(worldId, name, rootEntityId, atTick)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(InvalidStatementException.class)
    public ResponseEntity<Map<String, String>> invalid(InvalidStatementException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
