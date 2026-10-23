package com.bic.burble.ontology;

import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.persistence.neo4j.Neo4jEntityRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole app context boots in Neo4j mode with only the storage property
 * and the standard spring.neo4j settings. Needs a running server, given by
 * NEO4J_TEST_URI (and NEO4J_TEST_USER / NEO4J_TEST_PASSWORD); skipped otherwise.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "NEO4J_TEST_URI", matches = ".+")
class Neo4jModeStartupTest {

    @Autowired
    private ApplicationContext context;

    @DynamicPropertySource
    static void neo4jMode(DynamicPropertyRegistry registry) throws Exception {
        Path file = Files.createTempFile("burble-neo4j-startup-", ".json");
        Files.deleteIfExists(file);
        registry.add("burble.ontology.persistance.entity", () -> "neo4j");
        registry.add("burble.ontology.persistance.json.path", () -> file.toAbsolutePath().toString());
        registry.add("spring.neo4j.uri", () -> System.getenv("NEO4J_TEST_URI"));
        registry.add("spring.neo4j.authentication.username", () -> System.getenv().getOrDefault("NEO4J_TEST_USER", "neo4j"));
        registry.add("spring.neo4j.authentication.password", () -> System.getenv().getOrDefault("NEO4J_TEST_PASSWORD", ""));
    }

    @Test
    void startsAndConnectsToNeo4j() {
        assertThat(context.getBean(EntityRepository.class)).isInstanceOf(Neo4jEntityRepositoryAdapter.class);
        Driver driver = context.getBean(Driver.class);
        driver.verifyConnectivity();
        assertThat(context.containsBean("neo4jHealthContributor")).isTrue();
        assertThat(context.getBean(HealthEndpoint.class).health().getStatus().getCode()).isEqualTo("UP");
    }
}
