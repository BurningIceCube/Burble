package com.bic.burble.ontology.persistence.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;
import java.util.Optional;

public interface EntityNeo4jRepository extends Neo4jRepository<EntityNode, String> {
    List<EntityNode> findByWorldId(String worldId);
    Optional<EntityNode> findByWorldIdAndId(String worldId, String id);
}