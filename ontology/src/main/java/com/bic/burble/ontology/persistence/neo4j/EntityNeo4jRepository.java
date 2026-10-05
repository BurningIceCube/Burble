package com.bic.burble.ontology.persistence.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface EntityNeo4jRepository extends Neo4jRepository<EntityNode, String> {
    List<EntityNode> findByWorldId(String worldId);
}