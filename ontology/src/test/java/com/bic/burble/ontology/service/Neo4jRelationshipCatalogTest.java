package com.bic.burble.ontology.service;

import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.persistence.neo4j.Neo4jEntityRepositoryAdapter;
import com.bic.burble.ontology.persistence.neo4j.Neo4jTestServer;
import org.junit.jupiter.api.AfterEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

/** Same catalog checks with entities in Neo4j; skipped when no Neo4j is available. */
class Neo4jRelationshipCatalogTest extends RelationshipCatalogContract {

    private Driver driver;

    @Override
    protected EntityRepository freshRepository() {
        driver = Neo4jTestServer.driver();
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
        return new Neo4jEntityRepositoryAdapter(driver);
    }

    @AfterEach
    void closeDriver() {
        if (driver != null) {
            driver.close();
        }
    }
}
