package com.bic.burble.ontology.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Scopes Spring Data Neo4j repository scanning to the {@code persistence.neo4j}
 * subpackage. Without this, JPA and Neo4j repository scanning both evaluate
 * every candidate repository interface in the module (since both modules
 * are on the classpath), which is harmless but logs noisy "not mine" info
 * messages for repositories belonging to the other store.
 *
 * <p>Also explicitly declares a {@link Neo4jTransactionManager} bean.
 * {@code Neo4jDataAutoConfiguration}'s own transaction manager bean is
 * {@code @ConditionalOnMissingBean(TransactionManager.class)}; since the JPA
 * module already registers a {@code transactionManager} bean (a
 * {@code JpaTransactionManager}), Spring Boot's autoconfiguration silently
 * skips creating a {@code Neo4jTransactionManager}. Without one,
 * {@code Neo4jTemplate} ends up with no transaction manager wired in and
 * throws a {@link NullPointerException} on every save/find call. Declaring
 * this bean explicitly (and wiring it into {@code @EnableNeo4jRepositories})
 * ensures Neo4j operations always have a dedicated transaction manager,
 * independent of what other stores are configured in the module.
 */
@Configuration
@EnableNeo4jRepositories(
        basePackages = "com.bic.burble.ontology.persistence.neo4j",
        transactionManagerRef = "neo4jTransactionManager")
public class Neo4jRepositoryConfig {

    @Bean
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver,
                                                            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}
