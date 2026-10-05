package com.bic.burble.ontology.persistence;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;

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
}
