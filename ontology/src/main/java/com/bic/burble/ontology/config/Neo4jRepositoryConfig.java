package com.bic.burble.ontology.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Scopes Spring Data Neo4j repository scanning to the {@code persistence.neo4j}
 * subpackage. Without this, JPA and Neo4j repository scanning both evaluate
 * every candidate repository interface in the module (since both modules
 * are on the classpath), which is harmless but logs noisy "not mine" info
 * messages for repositories belonging to the other store.
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.bic.burble.ontology.persistence.neo4j")
public class Neo4jRepositoryConfig {
}
