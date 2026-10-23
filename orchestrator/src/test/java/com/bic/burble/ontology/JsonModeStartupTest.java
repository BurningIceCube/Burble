package com.bic.burble.ontology;

import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.persistence.json.JsonEntityRepositoryAdapter;
import org.junit.jupiter.api.Test;
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

/** The whole app context boots in JSON mode with no Neo4j beans at all. */
@SpringBootTest
class JsonModeStartupTest {

    @Autowired
    private ApplicationContext context;

    @DynamicPropertySource
    static void jsonMode(DynamicPropertyRegistry registry) throws Exception {
        Path file = Files.createTempFile("burble-json-startup-", ".json");
        Files.deleteIfExists(file);
        registry.add("burble.ontology.persistance.world", () -> "json");
        registry.add("burble.ontology.persistance.entity", () -> "json");
        registry.add("burble.ontology.persistance.json.path", () -> file.toAbsolutePath().toString());
    }

    @Test
    void startsWithoutNeo4j() {
        assertThat(context.getBean(EntityRepository.class)).isInstanceOf(JsonEntityRepositoryAdapter.class);
        assertThat(context.getBeanNamesForType(Driver.class)).isEmpty();
        assertThat(context.containsBean("neo4jClient")).isFalse();
        assertThat(context.containsBean("neo4jHealthContributor")).isFalse();
        assertThat(context.getBean(HealthEndpoint.class).health().getStatus().getCode()).isEqualTo("UP");
    }
}
