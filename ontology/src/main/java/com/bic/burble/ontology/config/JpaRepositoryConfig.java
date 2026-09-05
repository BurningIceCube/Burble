package com.bic.burble.ontology.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Scopes Spring Data JPA repository scanning to the {@code persistence.h2}
 * subpackage. Without this, JPA and Neo4j repository scanning both evaluate
 * every candidate repository interface in the module (since both modules
 * are on the classpath), which is harmless but logs noisy "not mine" info
 * messages for repositories belonging to the other store.
 *
 * <p>Also explicitly declares the JPA {@code transactionManager} bean.
 * {@code JpaBaseConfiguration}'s own transaction manager bean is
 * {@code @ConditionalOnMissingBean(TransactionManager.class)}; since
 * {@link Neo4jRepositoryConfig} explicitly registers a {@code Neo4jTransactionManager}
 * bean (needed so {@code Neo4jTemplate} has a transaction manager wired in —
 * see that class's javadoc), user configuration classes are processed before
 * autoconfiguration, so Boot sees a {@code TransactionManager} bean already
 * present and skips creating the default JPA one entirely. That leaves
 * {@code @Transactional} methods on JPA repositories unable to resolve a bean
 * named {@code transactionManager}. Declaring it here explicitly (and marking
 * it {@link Primary @Primary} so it remains the default for plain
 * {@code @Transactional} usages) keeps both stores' transaction managers
 * available side by side.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.bic.burble.ontology.persistence.h2")
public class JpaRepositoryConfig {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
