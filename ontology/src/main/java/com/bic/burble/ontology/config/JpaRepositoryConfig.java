package com.bic.burble.ontology.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Scopes Spring Data JPA repository scanning to the {@code persistence.h2}
 * subpackage. Without this, JPA and Neo4j repository scanning both evaluate
 * every candidate repository interface in the module (since both modules
 * are on the classpath), which is harmless but logs noisy "not mine" info
 * messages for repositories belonging to the other store.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.bic.burble.ontology.persistence.h2")
public class JpaRepositoryConfig {
}
