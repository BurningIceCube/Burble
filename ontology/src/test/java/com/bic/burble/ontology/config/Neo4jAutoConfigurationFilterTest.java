package com.bic.burble.ontology.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class Neo4jAutoConfigurationFilterTest {

    private static final String[] CLASSES = {
            "org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration",
            "org.springframework.boot.data.neo4j.autoconfigure.DataNeo4jAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"};

    private boolean[] match(MockEnvironment environment) {
        Neo4jAutoConfigurationFilter filter = new Neo4jAutoConfigurationFilter();
        filter.setEnvironment(environment);
        return filter.match(CLASSES, null);
    }

    @Test
    void jsonOrUnsetLeavesOutNeo4jOnly() {
        assertThat(match(new MockEnvironment())).containsExactly(false, false, true);
        assertThat(match(new MockEnvironment().withProperty("burble.ontology.persistance.entity", "json")))
                .containsExactly(false, false, true);
    }

    @Test
    void neo4jModeKeepsEverything() {
        assertThat(match(new MockEnvironment().withProperty("burble.ontology.persistance.entity", " Neo4j ")))
                .containsExactly(true, true, true);
    }
}
