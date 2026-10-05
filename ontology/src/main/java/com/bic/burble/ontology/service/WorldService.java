package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.world.CreateWorldRequest;
import com.bic.burble.ontology.domain.world.WorldRecord;

import java.util.List;
import java.util.Optional;

/**
 * Programmatic API for CRUD on World entities. This is the public contract
 * of the {@code ontology} module's world management capability: any Spring
 * context that component-scans {@code com.bic.burble.ontology} and provides
 * a {@link com.bic.burble.ontology.persistence.WorldRepository} bean (e.g.
 * via the H2 or Neo4j adapters) can inject this interface and use it
 * directly, with or without the orchestrator's REST layer.
 */
public interface WorldService {

    /**
     * Looks up a world by its GUID.
     *
     * @param worldId the world's unique identifier
     * @return the world record, or {@link Optional#empty()} if not found
     */
    Optional<WorldRecord> getWorld(String worldId);

    /**
     * @return all world records currently stored.
     */
    List<WorldRecord> getAllWorlds();

    /**
     * Creates a new world.
     *
     * @param request the requested name/description; may be {@code null}
     * @return the generated GUID of the created world
     */
    String createWorld(CreateWorldRequest request);

    /**
     * Deletes a world by its GUID.
     *
     * @param worldId the world's unique identifier
     * @return {@code true} if a world was deleted, {@code false} if none existed
     */
    boolean deleteWorld(String worldId);
}


