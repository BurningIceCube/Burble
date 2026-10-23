package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.ArchetypeStatement;
import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.entity.ArchetypeRequest;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;
import com.bic.burble.ontology.domain.statement.StatementTime;

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

    /**
     * Stores one relationship on the subject entity. The object entity is not
     * modified, so the inverse is a query via {@link #findByRelationship}.
     *
     * @return empty when either entity is missing or they are not in {@code worldId}
     */
    Optional<Relationship> addRelationship(String worldId, String subjectId, String verb, String objectId);

    /**
     * Relationship statements stored on the subject. Does not include {@code IS_A}.
     */
    List<Relationship> findRelationships(String subjectId);

    /**
     * Entities that store {@code verb} pointing at {@code objectId}.
     */
    List<Entity> findByRelationship(String verb, String objectId);

    /**
     * Stores one relationship with when it holds and how certain it is.
     * {@code time} null means always or unknown; {@code certainty} null means 1.
     *
     * @throws com.bic.burble.ontology.domain.statement.InvalidStatementException
     *         when certainty is outside 0..1, a period bound is not a Period
     *         entity, or the range ends before it starts
     * @throws com.bic.burble.ontology.domain.statement.OverlappingStatementException
     *         when a single-valued verb such as CARRIED_BY would overlap
     */
    Optional<Relationship> addRelationship(String worldId, String subjectId, String verb, String objectId,
                                           StatementTime time, Double certainty);

    /** Relationships on the subject that hold at {@code atTick}; null means all. */
    List<Relationship> findRelationships(String subjectId, Long atTick);

    /** Entities storing {@code verb} at {@code objectId} at {@code atTick}; null ignores time. */
    List<Entity> findByRelationship(String verb, String objectId, Long atTick);

    /**
     * IS_A statements on the entity, only those that hold at {@code atTick}
     * when it is given. Empty when the entity is not in {@code worldId}.
     */
    Optional<List<ArchetypeStatement>> findArchetypes(String worldId, String entityId, Long atTick);

    /**
     * Adds one IS_A statement. The same archetype and time again updates the
     * certainty. Empty when the entity is not in {@code worldId}.
     *
     * @throws com.bic.burble.ontology.domain.statement.OverlappingStatementException
     *         when the same archetype already holds during part of that time
     */
    Optional<ArchetypeStatement> addArchetype(String worldId, String entityId, ArchetypeRequest request);

    /**
     * Removes the IS_A statement with exactly this archetype and time
     * (null time is the untimed one). False when there is no such statement.
     */
    boolean removeArchetype(String worldId, String entityId, String archetype, StatementTime time);
}
