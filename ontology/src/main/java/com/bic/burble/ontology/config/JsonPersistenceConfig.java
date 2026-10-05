package com.bic.burble.ontology.config;

import com.bic.burble.ontology.persistence.json.JsonFileOntologyGraphStore;
import com.bic.burble.ontology.persistence.json.OntologyGraphStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Wires the JSON-file ontology graph when world storage, entity storage, or
 * both are set to {@code json}. H2 and Neo4j adapters stay in place and are
 * selected by their own property values.
 */
@Configuration
@ConditionalOnExpression("'${burble.ontology.persistance.world:}' == 'json' || '${burble.ontology.persistance.entity:}' == 'json'")
public class JsonPersistenceConfig {

    @Bean
    public OntologyGraphStore ontologyGraphStore(
            @Value("${burble.ontology.persistance.json.path:data/ontology.json}") String path) {
        return new JsonFileOntologyGraphStore(Path.of(path));
    }
}
