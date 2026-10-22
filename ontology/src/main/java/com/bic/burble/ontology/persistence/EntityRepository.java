package com.bic.burble.ontology.persistence;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.statement.StatementTime;

import java.util.List;
import java.util.Optional;

public interface EntityRepository {
    Entity save(Entity entity);
    Optional<Entity> findById(String id);
    List<Entity> findAllByWorldId(String worldId);
    boolean deleteById(String id);
    Entity update(Entity entity);

    /**
     * Appends one relationship statement on the subject entity. Does not write
     * an inverse statement on the object. Empty when the subject or object
     * does not exist.
     */
    Optional<Relationship> addRelationship(String subjectId, String verb, String objectId);

    /**
     * Relationship statements stored on the subject. Archetype {@code IS_A}
     * statements are not included.
     */
    List<Relationship> findRelationships(String subjectId);

    /**
     * Entities that already store {@code verb} pointing at {@code objectId}.
     * This is how an inverse such as carrying is queried, not stored.
     */
    List<Entity> findByRelationship(String verb, String objectId);

    /**
     * Appends one relationship statement with a time range and certainty.
     * {@code time} null means always or unknown; {@code certainty} null means 1.
     * Throws {@link com.bic.burble.ontology.domain.statement.InvalidStatementException}
     * for a bad certainty or time range, and
     * {@link com.bic.burble.ontology.domain.statement.OverlappingStatementException}
     * when a single-valued verb such as {@code CARRIED_BY} would overlap an
     * existing statement.
     */
    Optional<Relationship> addRelationship(
            String subjectId, String verb, String objectId, StatementTime time, Double certainty);

    /**
     * Relationship statements on the subject that hold at {@code atTick}.
     * Null {@code atTick} returns every statement.
     */
    List<Relationship> findRelationships(String subjectId, Long atTick);

    /**
     * Entities storing {@code verb} at {@code objectId} that hold at
     * {@code atTick}. Null {@code atTick} ignores time.
     */
    List<Entity> findByRelationship(String verb, String objectId, Long atTick);
}
