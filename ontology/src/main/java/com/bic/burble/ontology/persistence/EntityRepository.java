package com.bic.burble.ontology.persistence;

import com.bic.burble.ontology.domain.Entity;

import java.util.List;
import java.util.Optional;

public interface EntityRepository {
    Entity save(Entity entity);
    Optional<Entity> findById(String id);
    List<Entity> findAllByWorldId(String worldId);
    boolean deleteById(String id);
    Entity update(Entity entity);
}
