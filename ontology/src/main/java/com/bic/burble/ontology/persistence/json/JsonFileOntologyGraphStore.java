package com.bic.burble.ontology.persistence.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

/**
 * {@link OntologyGraphStore} that reads and writes a single JSON file.
 * Missing or empty files load as an empty graph. Writes replace the file via
 * a temp file in the same directory so a crash mid-write does not truncate it.
 */
public class JsonFileOntologyGraphStore implements OntologyGraphStore {

    private static final Logger log = LoggerFactory.getLogger(JsonFileOntologyGraphStore.class);

    private final Path path;
    private final JsonMapper mapper;
    private final Object lock = new Object();

    public JsonFileOntologyGraphStore(Path path) {
        this.path = path.toAbsolutePath().normalize();
        this.mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .build();
        log.debug("JsonFileOntologyGraphStore using {}", this.path);
    }

    @Override
    public OntologyGraph load() {
        synchronized (lock) {
            return readUnlocked();
        }
    }

    @Override
    public void save(OntologyGraph graph) {
        synchronized (lock) {
            writeUnlocked(graph == null ? OntologyGraph.empty() : graph);
        }
    }

    @Override
    public <T> T update(Function<OntologyGraph, T> action) {
        synchronized (lock) {
            OntologyGraph graph = readUnlocked();
            T result = action.apply(graph);
            writeUnlocked(graph);
            return result;
        }
    }

    private OntologyGraph readUnlocked() {
        try {
            if (Files.notExists(path) || Files.size(path) == 0) {
                log.debug("[JSON] load() - no graph file at {}, returning empty", path);
                return OntologyGraph.empty();
            }
            OntologyGraph graph = mapper.readValue(path, OntologyGraph.class);
            if (graph == null) {
                return OntologyGraph.empty();
            }
            log.debug("[JSON] load() - {} world(s), {} entity(ies) from {}",
                    graph.worlds().size(), graph.entities().size(), path);
            return graph;
        } catch (IOException | JacksonException ex) {
            throw new IllegalStateException("Failed to read ontology graph " + path, ex);
        }
    }

    private void writeUnlocked(OntologyGraph graph) {
        Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp, graph);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("[JSON] save() - wrote {} world(s), {} entity(ies) to {}",
                    graph.worlds().size(), graph.entities().size(), path);
        } catch (IOException | JacksonException ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                log.debug("[JSON] save() - failed to delete temp file {}", temp);
            }
            throw new IllegalStateException("Failed to write ontology graph " + path, ex);
        }
    }
}
