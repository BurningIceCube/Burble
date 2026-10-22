package com.bic.burble.ontology.persistence.neo4j;

import org.junit.jupiter.api.Assumptions;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * Finds a Neo4j for tests, in this order:
 * <ol>
 *   <li>{@code NEO4J_TEST_URI} (with {@code NEO4J_TEST_USER}, default
 *       {@code neo4j}, and {@code NEO4J_TEST_PASSWORD}), for a server you
 *       already run;</li>
 *   <li>a Testcontainers {@code neo4j:5} container when Docker is available.</li>
 * </ol>
 * When neither is there the calling test is skipped, not failed.
 */
public final class Neo4jTestServer {

    private static Neo4jContainer container;
    private static String uri;
    private static String user;
    private static String password;

    private Neo4jTestServer() {
    }

    public static synchronized boolean available() {
        if (uri != null) {
            return true;
        }
        String envUri = System.getenv("NEO4J_TEST_URI");
        if (envUri != null && !envUri.isBlank()) {
            uri = envUri;
            user = System.getenv().getOrDefault("NEO4J_TEST_USER", "neo4j");
            password = System.getenv().getOrDefault("NEO4J_TEST_PASSWORD", "");
            return true;
        }
        boolean docker;
        try {
            docker = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ex) {
            docker = false;
        }
        if (!docker) {
            return false;
        }
        container = new Neo4jContainer("neo4j:5").withAdminPassword("burble-test-pw");
        container.start();
        uri = container.getBoltUrl();
        user = "neo4j";
        password = "burble-test-pw";
        return true;
    }

    /** Skips the test when no Neo4j is available. */
    public static void assumeAvailable() {
        Assumptions.assumeTrue(available(),
                "No Neo4j: set NEO4J_TEST_URI or make Docker available for Testcontainers");
    }

    public static String uri() {
        return uri;
    }

    public static String user() {
        return user;
    }

    public static String password() {
        return password;
    }

    public static Driver driver() {
        assumeAvailable();
        return GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }
}
