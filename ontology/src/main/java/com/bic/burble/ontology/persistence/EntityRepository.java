package com.bic.burble.ontology.persistence;

import com.bic.burble.ontology.domain.Entity;

import java.util.Optional;

public interface EntityRepository {
    Entity save(Entity entity);
    Optional<Entity> findById(String worldId, String id);
}
