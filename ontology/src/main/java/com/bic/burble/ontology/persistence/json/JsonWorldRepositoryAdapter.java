package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.world.WorldRecord;
import com.bic.burble.ontology.persistence.WorldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * JSON-file implementation of {@link WorldRepository}. Worlds are stored in
 * the shared ontology graph file. Deleting a world also deletes the entities
 * it contains.
 *
 * <p>Activated via {@code burble.ontology.persistance.world=json}.
 */
@Component
@ConditionalOnProperty(prefix = "burble.ontology.persistance", name = "world", havingValue = "json")
public class JsonWorldRepositoryAdapter implements WorldRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonWorldRepositoryAdapter.class);

    private final OntologyGraphStore store;

    public JsonWorldRepositoryAdapter(OntologyGraphStore store) {
        log.debug("JsonWorldRepositoryAdapter activated (burble.ontology.persistance.world=json)");
        this.store = store;
    }

    @Override
    public WorldRecord save(WorldRecord world) {
        log.debug("[JSON] save() world {}", world);
        return store.update(graph -> {
            graph.worlds().removeIf(existing -> world.guid() != null && world.guid().equals(existing.guid()));
            StoredWorld stored = toStored(world);
            graph.worlds().add(stored);
            return toDomain(stored);
        });
    }

    @Override
    public Optional<WorldRecord> findById(String id) {
        log.debug("[JSON] findById() world id={}", id);
        return store.load().worlds().stream()
                .filter(world -> id != null && id.equals(world.guid()))
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public List<WorldRecord> findAll() {
        log.debug("[JSON] findAll() worlds");
        return store.load().worlds().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean deleteById(String id) {
        log.debug("[JSON] deleteById() world id={}", id);
        return store.update(graph -> {
            boolean removed = graph.worlds().removeIf(world -> id != null && id.equals(world.guid()));
            if (removed) {
                int entities = graph.entities().size();
                graph.entities().removeIf(entity -> id.equals(entity.worldId()));
                log.debug("[JSON] deleteById() world id={} removed {} contained entity(ies)",
                        id, entities - graph.entities().size());
            }
            return removed;
        });
    }

    private StoredWorld toStored(WorldRecord world) {
        return new StoredWorld(
                world.guid(),
                world.name(),
                world.description(),
                world.owner(),
                format(world.createdAt()),
                format(world.updatedAt())
        );
    }

    private WorldRecord toDomain(StoredWorld world) {
        return new WorldRecord(
                world.guid(),
                world.name(),
                world.description(),
                world.owner(),
                parse(world.createdAt()),
                parse(world.updatedAt())
        );
    }

    private static String format(Date date) {
        return date == null ? null : date.toInstant().toString();
    }

    private static Date parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.from(Instant.parse(value));
    }
}
