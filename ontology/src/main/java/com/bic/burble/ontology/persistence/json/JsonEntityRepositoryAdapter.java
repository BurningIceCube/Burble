package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.RelationshipVerbs;
import com.bic.burble.ontology.domain.statement.Certainty;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.persistence.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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


    @Override
    public Optional<Relationship> addRelationship(String subjectId, String verb, String objectId) {
        return addRelationship(subjectId, verb, objectId, null, null);
    }

    @Override
    public Optional<Relationship> addRelationship(
            String subjectId, String verb, String objectId, StatementTime time, Double certainty) {
        log.debug("[JSON] addRelationship() subject={} verb={} object={} time={} certainty={}",
                subjectId, verb, objectId, time, certainty);
        if (subjectId == null || verb == null || verb.isBlank() || objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        double checkedCertainty = Certainty.validate(certainty);
        StatementTime checkedTime = time == null || time.isOpen() ? null : time;
        return store.update(graph -> {
            Optional<StoredEntity> found = findStored(graph, subjectId);
            if (found.isEmpty()) {
                return Optional.<Relationship>empty();
            }
            StatementTimes.Range range = StatementTimes.validate(graph, checkedTime);
            StoredEntity subject = found.get();
            List<StoredStatement> statements = new ArrayList<>(subject.statements());
            StoredStatement incoming = new StoredStatement(verb, objectId, Map.of(), checkedTime, checkedCertainty);

            int sameIndex = -1;
            for (int i = 0; i < statements.size(); i++) {
                StoredStatement statement = statements.get(i);
                if (statement == null || !verb.equals(statement.verb())) {
                    continue;
                }
                if (objectId.equals(statement.object()) && Objects.equals(checkedTime, statement.time())) {
                    sameIndex = i;
                    continue;
                }
                if (RelationshipVerbs.isSingleValued(verb)
                        && StatementTimes.resolve(graph, statement.time()).overlaps(range)) {
                    throw new OverlappingStatementException(
                            subjectId + " already has " + verb + " " + statement.object()
                                    + " during part of that time");
                }
            }
            if (sameIndex >= 0) {
                statements.set(sameIndex, incoming);
            } else {
                statements.add(incoming);
            }
            StoredEntity updated = new StoredEntity(
                    subject.guid(),
                    subject.worldId(),
                    subject.name(),
                    subject.aliases(),
                    subject.description(),
                    statements
            );
            graph.entities().removeIf(entity -> subjectId.equals(entity.guid()));
            graph.entities().add(updated);
            return Optional.of(toRelationship(incoming));
        });
    }

    @Override
    public List<Relationship> findRelationships(String subjectId) {
        return findRelationships(subjectId, null);
    }

    @Override
    public List<Relationship> findRelationships(String subjectId, Long atTick) {
        log.debug("[JSON] findRelationships() subject={} atTick={}", subjectId, atTick);
        OntologyGraph graph = store.load();
        return findStored(graph, subjectId)
                .map(entity -> entity.statements().stream()
                        .filter(statement -> statement != null && !FacetStatements.IS_A.equals(statement.verb()))
                        .filter(statement -> holdsAt(graph, statement, atTick))
                        .map(JsonEntityRepositoryAdapter::toRelationship)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public List<Entity> findByRelationship(String verb, String objectId) {
        return findByRelationship(verb, objectId, null);
    }

    @Override
    public List<Entity> findByRelationship(String verb, String objectId, Long atTick) {
        log.debug("[JSON] findByRelationship() verb={} object={} atTick={}", verb, objectId, atTick);
        if (verb == null || objectId == null) {
            return List.of();
        }
        OntologyGraph graph = store.load();
        return graph.entities().stream()
                .filter(entity -> entity.statements().stream()
                        .anyMatch(statement -> statement != null
                                && verb.equals(statement.verb())
                                && objectId.equals(statement.object())
                                && holdsAt(graph, statement, atTick)))
                .map(this::toDomain)
                .toList();
    }

    private static boolean holdsAt(OntologyGraph graph, StoredStatement statement, Long atTick) {
        return atTick == null || StatementTimes.resolve(graph, statement.time()).contains(atTick);
    }

    private static Relationship toRelationship(StoredStatement statement) {
        return new Relationship(statement.verb(), statement.object(), statement.time(),
                Certainty.orDefault(statement.certainty()));
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
        // Keep the time and certainty of archetypes the entity already had.
        for (StoredStatement archetype : FacetStatements.fromFacets(entity.facets())) {
            StoredStatement previous = existing == null ? null : existing.stream()
                    .filter(statement -> statement != null
                            && FacetStatements.IS_A.equals(statement.verb())
                            && Objects.equals(archetype.object(), statement.object()))
                    .findFirst()
                    .orElse(null);
            statements.add(previous == null
                    ? archetype
                    : archetype.withTiming(previous.time(), previous.certainty()));
        }
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
