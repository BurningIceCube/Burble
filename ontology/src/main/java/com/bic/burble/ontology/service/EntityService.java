package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;

import java.util.List;
import java.util.Optional;

/**
 * Programmatic API for CRUD on Entity records. This is the public contract
 * of the {@code ontology} module's entity management capability: any Spring
 * context that component-scans {@code com.bic.burble.ontology} and provides
 * a {@link com.bic.burble.ontology.persistence.EntityRepository} bean (e.g.
 * via the Neo4j adapter) can inject this interface and use it directly,
 * with or without the orchestrator's REST layer.
 */
public interface EntityService {

    /**
     * Creates a new entity within a world.
     *
     * @param worldId the ID of the world the entity belongs to
     * @param request the requested name/description/aliases/facets
     * @return the generated GUID of the created entity
     */
    String createEntity(String worldId, CreateEntityRequest request);

    /**
     * Looks up an entity by its globally unique GUID.
     *
     * @param id the entity's unique identifier
     * @return the entity, or {@link Optional#empty()} if not found
     */
    Optional<Entity> findById(String id);

    /**
     * Looks up all entities belonging to a given world.
     *
     * @param worldId the ID of the world
     * @return all entities in that world, or an empty list if none exist
     */
    List<Entity> findAllByWorldId(String worldId);

    /**
     * Deletes an entity by its globally unique GUID.
     *
     * @param id the entity's unique identifier
     * @return {@code true} if an entity was deleted, {@code false} if none existed
     */
    boolean deleteById(String id);

    /**
     * Updates an existing entity in place.
     *
     * @param worldId  the ID of the world the entity belongs to
     * @param entityId the entity's unique identifier
     * @param request  the requested name/description/aliases/facets
     * @return the updated entity, or {@link Optional#empty()} if no entity with that ID exists
     */
    Optional<Entity> updateEntity(String worldId, String entityId, CreateEntityRequest request);
}

