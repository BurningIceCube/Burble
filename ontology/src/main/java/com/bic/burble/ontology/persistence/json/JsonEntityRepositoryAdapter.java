package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.persistence.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON-file implementation of {@link EntityRepository}. The entity is one
 * record in the graph file. Its archetypes are {@code IS_A} statements on
 * that record, not a type field. Statements with any other verb are left in
 * place when the entity is saved again.
 *
 * <p>Activated via {@code burble.ontology.persistance.entity=json}.
 */
@Component
@ConditionalOnProperty(prefix = "burble.ontology.persistance", name = "entity", havingValue = "json")
public class JsonEntityRepositoryAdapter implements EntityRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonEntityRepositoryAdapter.class);

    private final OntologyGraphStore store;

    public JsonEntityRepositoryAdapter(OntologyGraphStore store) {
        log.debug("JsonEntityRepositoryAdapter activated (burble.ontology.persistance.entity=json)");
        this.store = store;
    }

    @Override
    public Entity save(Entity entity) {
        log.debug("[JSON] save() entity {}", entity);
        return store.update(graph -> {
            StoredEntity stored = toStored(entity, existingStatements(graph, entity.guid()));
            graph.entities().removeIf(existing -> entity.guid() != null && entity.guid().equals(existing.guid()));
            graph.entities().add(stored);
            return toDomain(stored);
        });
    }

    @Override
    public Optional<Entity> findById(String id) {
        log.debug("[JSON] findById() entity id={}", id);
        return findStored(store.load(), id).map(this::toDomain);
    }

    @Override
    public List<Entity> findAllByWorldId(String worldId) {
        log.debug("[JSON] findAllByWorldId() worldId={}", worldId);
        return store.load().entities().stream()
                .filter(entity -> worldId != null && worldId.equals(entity.worldId()))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean deleteById(String id) {
        log.debug("[JSON] deleteById() entity id={}", id);
        return store.update(graph -> graph.entities().removeIf(entity -> id != null && id.equals(entity.guid())));
    }

    @Override
    public Entity update(Entity entity) {
        log.debug("[JSON] update() entity guid={}", entity.guid());
        return save(entity);
    }

    private static Optional<StoredEntity> findStored(OntologyGraph graph, String id) {
        return graph.entities().stream()
                .filter(entity -> id != null && id.equals(entity.guid()))
                .findFirst();
    }

    private static List<StoredStatement> existingStatements(OntologyGraph graph, String id) {
        return findStored(graph, id).map(StoredEntity::statements).orElse(List.of());
    }

    private StoredEntity toStored(Entity entity, List<StoredStatement> existing) {
        List<StoredStatement> statements = new ArrayList<>();
        if (existing != null) {
            for (StoredStatement statement : existing) {
                if (statement != null && !FacetStatements.IS_A.equals(statement.verb())) {
                    statements.add(statement);
                }
            }
        }
        statements.addAll(FacetStatements.fromFacets(entity.facets()));
        List<String> aliases = entity.aliases() == null ? List.of() : entity.aliases();
        return new StoredEntity(
                entity.guid(),
                entity.worldId(),
                entity.name(),
                aliases,
                entity.description(),
                statements
        );
    }

    private Entity toDomain(StoredEntity stored) {
        List<String> aliases = stored.aliases() == null ? List.of() : stored.aliases();
        return new Entity(
                stored.guid(),
                stored.worldId(),
                stored.name(),
                aliases,
                stored.description(),
                FacetStatements.toFacets(stored.statements())
        );
    }
}
