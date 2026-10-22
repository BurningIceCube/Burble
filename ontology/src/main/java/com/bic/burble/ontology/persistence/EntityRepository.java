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
     * an inverse statement on the object. Empty when the subject does not exist.
     * The JSON store implements this; other stores do not.
     */
    default Optional<Relationship> addRelationship(String subjectId, String verb, String objectId) {
        throw new UnsupportedOperationException(
                "Relationship statements are not supported by " + getClass().getName());
    }

    /**
     * Relationship statements stored on the subject. Archetype {@code IS_A}
     * statements are not included. Empty when this store cannot read them.
     */
    default List<Relationship> findRelationships(String subjectId) {
        return List.of();
    }

    /**
     * Entities that already store {@code verb} pointing at {@code objectId}.
     * This is how an inverse such as carrying is queried, not stored.
     */
    default List<Entity> findByRelationship(String verb, String objectId) {
        return List.of();
    }

    /**
     * Appends one relationship statement with a time range and certainty.
     * {@code time} null means always or unknown; {@code certainty} null means 1.
     * Throws {@link com.bic.burble.ontology.domain.statement.InvalidStatementException}
     * for a bad certainty or time range, and
     * {@link com.bic.burble.ontology.domain.statement.OverlappingStatementException}
     * when a single-valued verb such as {@code CARRIED_BY} would overlap an
     * existing statement. Only the JSON store supports a time or certainty.
     */
    default Optional<Relationship> addRelationship(
            String subjectId, String verb, String objectId, StatementTime time, Double certainty) {
        boolean plain = (time == null || time.isOpen()) && (certainty == null || certainty == 1.0);
        if (plain) {
            return addRelationship(subjectId, verb, objectId);
        }
        throw new UnsupportedOperationException(
                "Timed statements are not supported by " + getClass().getName());
    }

    /**
     * Relationship statements on the subject that hold at {@code atTick}.
     * Null {@code atTick} returns every statement, like {@link #findRelationships(String)}.
     */
    default List<Relationship> findRelationships(String subjectId, Long atTick) {
        if (atTick == null) {
            return findRelationships(subjectId);
        }
        throw new UnsupportedOperationException(
                "Timed queries are not supported by " + getClass().getName());
    }

    /**
     * Entities storing {@code verb} at {@code objectId} that hold at
     * {@code atTick}. Null {@code atTick} ignores time.
     */
    default List<Entity> findByRelationship(String verb, String objectId, Long atTick) {
        if (atTick == null) {
            return findByRelationship(verb, objectId);
        }
        throw new UnsupportedOperationException(
                "Timed queries are not supported by " + getClass().getName());
    }
}
