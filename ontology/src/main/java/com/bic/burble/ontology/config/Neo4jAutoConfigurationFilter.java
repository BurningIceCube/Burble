package com.bic.burble.ontology.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Leaves out Spring Boot's Neo4j auto-configuration (driver, health check,
 * Spring Data Neo4j) unless entities are stored in Neo4j
 * ({@code burble.ontology.persistance.entity=neo4j}).
 *
 * <p>JSON mode then starts with no Neo4j beans and no connection attempt.
 * Neo4j mode needs only that property plus the standard {@code spring.neo4j.*}
 * connection settings.
 *
 * <p>This is an auto-configuration import filter rather than an exclusion in
 * application.yml: Spring Boot can only switch config-file documents on by
 * profile or cloud platform, not by a property value. The filter runs while
 * auto-configurations are chosen, after every property source (config files,
 * command line, environment variables and test properties) is in place.
 */
public class Neo4jAutoConfigurationFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    static final String ENTITY_PROPERTY = "burble.ontology.persistance.entity";

    static final List<String> NEO4J_PACKAGES = List.of(
            "org.springframework.boot.neo4j.",
            "org.springframework.boot.data.neo4j.");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata metadata) {
        boolean neo4j = usesNeo4j(environment);
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = neo4j || !isNeo4j(autoConfigurationClasses[i]);
        }
        return matches;
    }

    static boolean usesNeo4j(Environment environment) {
        String entity = environment == null ? null : environment.getProperty(ENTITY_PROPERTY);
        return entity != null && "neo4j".equalsIgnoreCase(entity.trim());
    }

    static boolean isNeo4j(String autoConfigurationClass) {
        return autoConfigurationClass != null && NEO4J_PACKAGES.stream().anyMatch(autoConfigurationClass::startsWith);
    }
}
