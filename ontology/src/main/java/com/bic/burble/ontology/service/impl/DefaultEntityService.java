package com.bic.burble.ontology.service.impl;

import com.bic.burble.ontology.domain.ArchetypeStatement;
import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.RelationshipType;
import com.bic.burble.ontology.domain.entity.ArchetypeRequest;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.domain.facet.PeriodFacet;
import com.bic.burble.ontology.domain.statement.Certainty;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeRange;
import com.bic.burble.ontology.domain.statement.TimeRange.PeriodTicks;
import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link EntityService} implementation, backed by whichever
 * {@link EntityRepository} implementation is active (e.g. the Neo4j-backed
 * adapter enabled via {@code burble.ontology.persistance.entity=neo4j}).
 *
 * <p>This is an internal implementation detail of the {@code ontology}
 * module; consumers should depend on the {@link EntityService} interface
 * rather than this class.
 */
@Service
public class DefaultEntityService implements EntityService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEntityService.class);

    private final EntityRepository entityRepository;

    public DefaultEntityService(EntityRepository entityRepository) {
        log.debug("Wiring DefaultEntityService with repository implementation: {}", entityRepository.getClass().getName());
        this.entityRepository = entityRepository;
    }

    @Override
    public String createEntity(String worldId, CreateEntityRequest request) {
        String guid = UUID.randomUUID().toString();
        log.debug("Building new entity guid={} for worldId={} from request={}", guid, worldId, request);
        Entity entity = toEntity(guid, worldId, request, null);
        log.debug("Saving entity via repository: {}", entity);
        entityRepository.save(entity);
        log.info("Entity created: guid={}, worldId={}", guid, worldId);
        return guid;
    }

    @Override
    public Optional<Entity> findById(String id) {
        log.debug("Looking up entity by id={}", id);
        Optional<Entity> result = entityRepository.findById(id);
        log.debug("Lookup result for id={}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public List<Entity> findAllByWorldId(String worldId) {
        log.debug("Looking up all entities for worldId={}", worldId);
        List<Entity> entities = entityRepository.findAllByWorldId(worldId);
        log.debug("Found {} entity(ies) for worldId={}", entities.size(), worldId);
        return entities;
    }

    @Override
    public boolean deleteById(String id) {
        log.debug("Deleting entity by id={}", id);
        boolean deleted = entityRepository.deleteById(id);
        log.info("Delete entity id={} result={}", id, deleted);
        return deleted;
    }

    @Override
    public Optional<Entity> updateEntity(String worldId, String entityId, CreateEntityRequest request) {
        log.debug("Updating entity id={} in worldId={} with request={}", entityId, worldId, request);
        Optional<Entity> existing = entityRepository.findById(entityId);
        if (existing.isEmpty()) {
            log.debug("Update aborted, no entity found for entityId={}", entityId);
            return Optional.empty();
        }
        Entity entity = toEntity(entityId, worldId, request, existing.get());
        Entity updated = entityRepository.update(entity);
        log.info("Entity updated: entityId={}, worldId={}", entityId, worldId);
        return Optional.of(updated);
    }

    private Entity toEntity(String guid, String worldId, CreateEntityRequest request, Entity existing) {
        String name = request != null ? request.name() : null;
        String description = request != null ? request.description() : null;
        List<String> aliases = request != null && request.aliases() != null ? request.aliases() : List.of();
        Set<ArchetypeFacet> facets = toFacets(request);
        List<ArchetypeStatement> statements = toStatements(request, existing);
        Entity entity = new Entity(guid, worldId, name, aliases, description, facets, statements);
        entity = new Entity(guid, worldId, name, aliases, description, entity.facets(),
                checkArchetypes(guid, entity.archetypes()));
        log.debug("Mapped CreateEntityRequest to Entity: guid={}, worldId={}, name={}, archetypes={}",
                guid, worldId, name, entity.archetypes());
        return entity;
    }

    private Set<ArchetypeFacet> toFacets(CreateEntityRequest request) {
        if (request == null) {
            return Set.of();
        }
        Set<ArchetypeFacet> facets = new HashSet<>();
        if (Boolean.TRUE.equals(request.character())) {
            facets.add(new CharacterFacet());
        }
        if (request.item() != null) {
            facets.add(new ItemFacet(request.item().portable(), request.item().unique()));
        }
        if (request.period() != null) {
            facets.add(new PeriodFacet(request.period().startTick(), request.period().endTick()));
        }
        if (request.archetypes() != null) {
            for (ArchetypeRequest archetype : request.archetypes()) {
                String name = archetype == null ? null : archetype.archetype();
                if (name == null || name.isBlank()) {
                    continue;
                }
                if ("Item".equals(name)) {
                    if (request.item() == null) {
                        facets.add(new ItemFacet(false, false));
                    }
                    continue;
                }
                if ("Period".equals(name) && request.period() != null) {
                    continue;
                }
                ArchetypeFacet facet = ArchetypeFacets.of(name, null, null);
                if (facet == null) {
                    throw new IllegalArgumentException("Unknown archetype: " + name);
                }
                facets.add(facet);
            }
        }
        return facets;
    }

    /**
     * IS_A statements from the request. A name given without time or
     * certainty keeps the statements the entity already had for that
     * archetype (or is always true for a new one), so an update that only
     * lists names behaves as before.
     */
    private List<ArchetypeStatement> toStatements(CreateEntityRequest request, Entity existing) {
        List<ArchetypeStatement> statements = new ArrayList<>();
        if (request == null || request.archetypes() == null) {
            return statements;
        }
        for (ArchetypeRequest archetype : request.archetypes()) {
            if (archetype == null || archetype.archetype() == null || archetype.archetype().isBlank()) {
                continue;
            }
            String name = archetype.archetype();
            if (archetype.nameOnly()) {
                List<ArchetypeStatement> kept = existing == null ? List.of() : existing.archetypes().stream()
                        .filter(statement -> name.equals(statement.archetype()))
                        .toList();
                if (kept.isEmpty()) {
                    statements.add(ArchetypeStatement.always(name));
                } else {
                    kept.stream().filter(statement -> !statements.contains(statement)).forEach(statements::add);
                }
            } else {
                statements.add(toStatement(archetype));
            }
        }
        return statements;
    }

    private static ArchetypeStatement toStatement(ArchetypeRequest request) {
        if (request.archetype() == null || !ArchetypeFacets.isKnown(request.archetype())) {
            throw new InvalidStatementException("Unknown archetype: " + request.archetype()
                    + ". Allowed values: " + String.join(", ", ArchetypeFacets.NAMES));
        }
        StatementTime time = request.time() == null || request.time().isOpen() ? null : request.time();
        return new ArchetypeStatement(request.archetype(), time, Certainty.validate(request.certainty()));
    }

    /**
     * Checks every IS_A statement has a valid time and certainty and that
     * statements for the same archetype do not overlap. Two statements with
     * the same archetype and the same time are one statement; the later one
     * wins.
     */
    private List<ArchetypeStatement> checkArchetypes(String entityId, List<ArchetypeStatement> statements) {
        Map<String, ArchetypeStatement> unique = new java.util.LinkedHashMap<>();
        for (ArchetypeStatement statement : statements) {
            Certainty.validate(statement.certainty());
            unique.put(statement.archetype() + "|" + statement.time(), statement);
        }
        List<ArchetypeStatement> checked = new ArrayList<>(unique.values());
        List<TimeRange> ranges = new ArrayList<>();
        for (ArchetypeStatement statement : checked) {
            ranges.add(TimeRange.validate(statement.time(), this::periodTicks));
        }
        for (int i = 0; i < checked.size(); i++) {
            for (int j = i + 1; j < checked.size(); j++) {
                if (checked.get(i).archetype().equals(checked.get(j).archetype())
                        && ranges.get(i).overlaps(ranges.get(j))) {
                    throw new OverlappingStatementException(entityId + " already IS_A " + checked.get(i).archetype()
                            + " " + ranges.get(i).describe() + ", which overlaps " + ranges.get(j).describe());
                }
            }
        }
        return checked;
    }

    private Optional<PeriodTicks> periodTicks(String periodId) {
        return entityRepository.findById(periodId)
                .flatMap(entity -> entity.getFacet(PeriodFacet.class))
                .map(period -> new PeriodTicks(period.startTick(), period.endTick()));
    }

    @Override
    public Optional<List<ArchetypeStatement>> findArchetypes(String worldId, String entityId, Long atTick) {
        return inWorld(worldId, entityId).map(entity -> entity.archetypes().stream()
                .filter(statement -> atTick == null
                        || TimeRange.resolve(statement.time(), this::periodTicks).contains(atTick))
                .toList());
    }

    @Override
    public Optional<ArchetypeStatement> addArchetype(String worldId, String entityId, ArchetypeRequest request) {
        Optional<Entity> found = inWorld(worldId, entityId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Entity entity = found.get();
        ArchetypeStatement incoming = toStatement(request);
        List<ArchetypeStatement> statements = new ArrayList<>(entity.archetypes());
        statements.removeIf(statement -> statement.archetype().equals(incoming.archetype())
                && Objects.equals(statement.time(), incoming.time()));
        statements.add(incoming);
        checkArchetypes(entityId, statements);
        entityRepository.update(new Entity(entity.guid(), entity.worldId(), entity.name(), entity.aliases(),
                entity.description(), entity.facets(), statements));
        log.info("Archetype stored on {}: IS_A {}", entityId, incoming.archetype());
        return Optional.of(incoming);
    }

    @Override
    public boolean removeArchetype(String worldId, String entityId, String archetype, StatementTime time) {
        Optional<Entity> found = inWorld(worldId, entityId);
        if (found.isEmpty()) {
            return false;
        }
        Entity entity = found.get();
        StatementTime wanted = time == null || time.isOpen() ? null : time;
        List<ArchetypeStatement> statements = new ArrayList<>(entity.archetypes());
        if (!statements.removeIf(statement -> statement.archetype().equals(archetype)
                && Objects.equals(statement.time(), wanted))) {
            return false;
        }
        Set<ArchetypeFacet> facets = new HashSet<>(entity.facets());
        if (statements.stream().noneMatch(statement -> statement.archetype().equals(archetype))) {
            facets.removeIf(facet -> ArchetypeFacets.nameOf(facet).equals(archetype));
        }
        entityRepository.update(new Entity(entity.guid(), entity.worldId(), entity.name(), entity.aliases(),
                entity.description(), facets, statements));
        log.info("Archetype removed from {}: IS_A {} {}", entityId, archetype, wanted);
        return true;
    }

    private Optional<Entity> inWorld(String worldId, String entityId) {
        return entityRepository.findById(entityId).filter(entity -> Objects.equals(worldId, entity.worldId()));
    }

    @Override
    public Optional<Relationship> addRelationship(String worldId, String subjectId, String verb, String objectId) {
        return addRelationship(worldId, subjectId, verb, objectId, null, null);
    }

    @Override
    public Optional<Relationship> addRelationship(String worldId, String subjectId, String verb, String objectId,
                                                  StatementTime time, Double certainty) {
        log.debug("Adding relationship in worldId={} subject={} verb={} object={}", worldId, subjectId, verb, objectId);
        RelationshipType type = RelationshipType.parse(verb);
        verb = type.name();
        if (subjectId != null && subjectId.equals(objectId)) {
            throw new InvalidStatementException("An entity cannot have " + verb + " pointing at itself");
        }
        Optional<Entity> subject = entityRepository.findById(subjectId);
        Optional<Entity> object = entityRepository.findById(objectId);
        if (subject.isEmpty() || object.isEmpty()) {
            log.debug("Relationship aborted, subject or object entity missing");
            return Optional.empty();
        }
        if (worldId == null || !worldId.equals(subject.get().worldId()) || !worldId.equals(object.get().worldId())) {
            log.debug("Relationship aborted, entities are not both in worldId={}", worldId);
            return Optional.empty();
        }
        type.requireAllowed(archetypeNames(subject.get()), archetypeNames(object.get()));
        requireActive(type, subject.get(), object.get(), time);
        Optional<Relationship> stored = entityRepository.addRelationship(subjectId, verb, objectId, time, certainty);
        stored.ifPresent(relationship -> log.info(
                "Relationship stored on subject {}: {} -> {}", subjectId, relationship.verb(), relationship.objectId()));
        return stored;
    }

    @Override
    public List<Relationship> findRelationships(String subjectId) {
        log.debug("Looking up relationships for subject={}", subjectId);
        return entityRepository.findRelationships(subjectId);
    }

    @Override
    public List<Entity> findByRelationship(String verb, String objectId) {
        log.debug("Querying relationship verb={} object={}", verb, objectId);
        return entityRepository.findByRelationship(RelationshipType.parse(verb).name(), objectId);
    }

    @Override
    public List<Relationship> findRelationships(String subjectId, Long atTick) {
        log.debug("Looking up relationships for subject={} atTick={}", subjectId, atTick);
        return entityRepository.findRelationships(subjectId, atTick);
    }

    @Override
    public List<Entity> findByRelationship(String verb, String objectId, Long atTick) {
        log.debug("Querying relationship verb={} object={} atTick={}", verb, objectId, atTick);
        return entityRepository.findByRelationship(RelationshipType.parse(verb).name(), objectId, atTick);
    }

    private static Set<String> archetypeNames(Entity entity) {
        Set<String> names = new HashSet<>();
        for (ArchetypeStatement statement : entity.archetypes()) {
            names.add(statement.archetype());
        }
        return names;
    }

    /**
     * The subject and object must each have an allowed archetype for the
     * whole relationship time. One archetype has to cover all of it, though
     * it may be several IS_A statements that touch end to end. An untimed
     * relationship needs an untimed (always) archetype.
     */
    private void requireActive(RelationshipType type, Entity subject, Entity object, StatementTime time) {
        StatementTime checkedTime = time == null || time.isOpen() ? null : time;
        TimeRange want = TimeRange.validate(checkedTime, this::periodTicks);
        Map<String, TimeRange> subjectGaps = gaps(subject, want);
        Map<String, TimeRange> objectGaps = gaps(object, want);
        String problem = null;
        for (RelationshipType.Rule rule : type.rules()) {
            String subjectProblem = sideProblem("subject", subject, rule.subjects(), subjectGaps);
            String objectProblem = sideProblem("object", object, rule.objects(), objectGaps);
            if (subjectProblem == null && objectProblem == null) {
                return;
            }
            boolean namesMatch = rule.subjects().stream().anyMatch(subjectGaps::containsKey)
                    && rule.objects().stream().anyMatch(objectGaps::containsKey);
            if (namesMatch && problem == null) {
                problem = subjectProblem != null ? subjectProblem : objectProblem;
            }
        }
        if (problem != null) {
            throw new InvalidStatementException(type.name() + " needs " + problem
                    + " for the whole relationship time (" + want.describe() + ")");
        }
    }

    /** Archetype name to the first uncovered part of {@code want}, or null when it covers all. */
    private Map<String, TimeRange> gaps(Entity entity, TimeRange want) {
        Map<String, List<TimeRange>> byName = new TreeMap<>();
        for (ArchetypeStatement statement : entity.archetypes()) {
            byName.computeIfAbsent(statement.archetype(), name -> new ArrayList<>())
                    .add(TimeRange.resolve(statement.time(), this::periodTicks));
        }
        Map<String, TimeRange> gaps = new TreeMap<>();
        byName.forEach((name, ranges) -> gaps.put(name, TimeRange.firstGap(want, ranges).orElse(null)));
        return gaps;
    }

    private static String sideProblem(String side, Entity entity, Set<String> allowed, Map<String, TimeRange> gaps) {
        String firstMiss = null;
        for (String name : allowed.stream().sorted().toList()) {
            if (!gaps.containsKey(name)) {
                continue;
            }
            TimeRange gap = gaps.get(name);
            if (gap == null) {
                return null;
            }
            if (firstMiss == null) {
                firstMiss = "the " + side + " " + entity.guid() + " to be " + name
                        + ", but it is not " + name + " " + gap.describe();
            }
        }
        return firstMiss == null ? "the " + side + " to have an allowed archetype" : firstMiss;
    }
}
