package com.bic.burble.ontology.service;

import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.persistence.json.JsonEntityRepositoryAdapter;
import com.bic.burble.ontology.persistence.json.JsonFileOntologyGraphStore;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class JsonCompositeTest extends CompositeContract {

    @TempDir
    Path tempDir;

    @Override
    protected EntityRepository freshRepository() {
        return new JsonEntityRepositoryAdapter(new JsonFileOntologyGraphStore(tempDir.resolve("ontology-" + System.nanoTime() + ".json")));
    }
}
