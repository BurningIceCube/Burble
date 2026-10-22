package com.bic.burble.ontology.persistence.neo4j;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.RelationshipType;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.domain.facet.PeriodFacet;
import com.bic.burble.ontology.domain.statement.Certainty;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.OverlappingStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeBound;
import com.bic.burble.ontology.domain.statement.TimeRange;
import com.bic.burble.ontology.domain.statement.TimeRange.PeriodTicks;
import com.bic.burble.ontology.persistence.EntityRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Neo4j implementation of {@link EntityRepository}, with the same behaviour
 * as the JSON store.
 *
 * <p>Graph shape:
 * <pre>
 * (:Entity {id, worldId, name, description, aliases})
 *   -[:IS_A {time..., certainty}]->(:Archetype:Character {name: "Character"})        shared node
 *   -[:IS_A {time..., certainty}]->(:Archetype:Item {name, owner, portable, unique}) one per entity
 *   -[:IS_A {time..., certainty}]->(:Archetype:Period {name, owner, startTick, endTick}) one per entity
 *   -[:CARRIED_BY {time..., certainty}]->(:Entity)
 * </pre>
 * Time is stored as relationship properties {@code startTick},
 * {@code startPeriod}, {@code endTick} and {@code endPeriod}; each is left
 * off when not set. {@code certainty} is left off when it is 1.
 *
 * <p>A relationship is stored once, on the subject, as an outgoing edge.
 * The inverse (for example what a character is carrying) is a query over
 * incoming edges, not a second edge.
 *
 * <p>Activated via {@code burble.ontology.persistance.entity=neo4j}.
 */
@Component
@ConditionalOnProperty(prefix = "burble.ontology.persistance", name = "entity", havingValue = "neo4j")
public class Neo4jEntityRepositoryAdapter implements EntityRepository {

    private static final Logger log = LoggerFactory.getLogger(Neo4jEntityRepositoryAdapter.class);

    static final String IS_A = "IS_A";
    /** Archetypes with their own properties get a node per entity instead of a shared node. */
    private static final Set<String> OWNED_ARCHETYPES = Set.of("Item", "Period");

    private final Driver driver;

    public Neo4jEntityRepositoryAdapter(Driver driver) {
        log.debug("Neo4jEntityRepositoryAdapter activated (burble.ontology.persistance.entity=neo4j)");
        this.driver = driver;
        try (Session session = driver.session()) {
            session.run("CREATE CONSTRAINT entity_id IF NOT EXISTS FOR (e:Entity) REQUIRE e.id IS UNIQUE").consume();
        } catch (RuntimeException ex) {
            log.warn("[Neo4j] Could not create the Entity id constraint: {}", ex.getMessage());
        }
    }

    // ----- entity CRUD -----

    @Override
    public Entity save(Entity entity) {
        log.debug("[Neo4j] save() entity {}", entity);
        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> write(tx, entity));
        }
        return findById(entity.guid()).orElseThrow();
    }

    @Override
    public Entity update(Entity entity) {
        return save(entity);
    }

    @Override
    public Optional<Entity> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> readEntities(tx, "MATCH (e:Entity {id: $id})", Map.of("id", id)))
                    .stream().findFirst();
        }
    }

    @Override
    public List<Entity> findAllByWorldId(String worldId) {
        if (worldId == null) {
            return List.of();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx ->
                    readEntities(tx, "MATCH (e:Entity {worldId: $worldId})", Map.of("worldId", worldId)));
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                boolean exists = tx.run("MATCH (e:Entity {id: $id}) RETURN count(e) AS n", Map.of("id", id))
                        .single().get("n").asLong() > 0;
                if (exists) {
                    tx.run("""
                            MATCH (e:Entity {id: $id})
                            OPTIONAL MATCH (e)-[:IS_A]->(owned:Archetype {owner: $id})
                            DETACH DELETE owned, e
                            """, Map.of("id", id)).consume();
                }
                return exists;
            });
        }
    }

    private void write(TransactionContext tx, Entity entity) {
        String id = entity.guid();
        Map<String, Map<String, Object>> previousTiming = new HashMap<>();
        tx.run("MATCH (:Entity {id: $id})-[r:IS_A]->(a:Archetype) RETURN a.name AS name, properties(r) AS props",
                Map.of("id", id)).list().forEach(row ->
                previousTiming.put(row.get("name").asString(), row.get("props").asMap()));

        Map<String, Object> props = new HashMap<>();
        props.put("id", id);
        props.put("worldId", entity.worldId());
        props.put("name", entity.name());
        props.put("description", entity.description());
        props.put("aliases", entity.aliases() == null ? List.of() : entity.aliases());
        tx.run("MATCH (:Entity {id: $id})-[:IS_A]->(a:Archetype {owner: $id}) DETACH DELETE a", Map.of("id", id)).consume();
        tx.run("MATCH (:Entity {id: $id})-[r:IS_A]->() DELETE r", Map.of("id", id)).consume();
        tx.run("""
                MERGE (e:Entity {id: $id})
                SET e.worldId = $worldId, e.name = $name, e.description = $description, e.aliases = $aliases
                """, props).consume();

        if (entity.facets() == null) {
            return;
        }
        for (ArchetypeFacet facet : entity.facets()) {
            String name = ArchetypeFacets.nameOf(facet);
            Map<String, Object> archetype = new HashMap<>();
            archetype.put("name", name);
            if (facet instanceof ItemFacet item) {
                archetype.put("portable", item.portable());
                archetype.put("unique", item.unique());
            } else if (facet instanceof PeriodFacet period) {
                if (period.startTick() != null) {
                    archetype.put("startTick", period.startTick());
                }
                if (period.endTick() != null) {
                    archetype.put("endTick", period.endTick());
                }
            }
            Map<String, Object> timing = previousTiming.getOrDefault(name, Map.of());
            Map<String, Object> params = Map.of("id", id, "props", archetype, "timing", timing, "name", name);
            // name is from the closed archetype list, so it is safe as a label.
            if (OWNED_ARCHETYPES.contains(name)) {
                tx.run("MATCH (e:Entity {id: $id}) CREATE (e)-[r:IS_A]->(a:Archetype:" + name + ") "
                        + "SET a = $props, a.owner = $id, r = $timing", params).consume();
            } else {
                tx.run("MATCH (e:Entity {id: $id}) MERGE (a:Archetype:" + name + " {name: $name}) "
                        + "CREATE (e)-[r:IS_A]->(a) SET r = $timing", params).consume();
            }
        }
    }

    private List<Entity> readEntities(TransactionContext tx, String match, Map<String, Object> params) {
        return tx.run(match + """
                 OPTIONAL MATCH (e)-[:IS_A]->(a:Archetype)
                 RETURN e, collect(properties(a)) AS archetypes
                 ORDER BY e.id
                """, params).list().stream().map(Neo4jEntityRepositoryAdapter::toEntity).toList();
    }

    private static Entity toEntity(Record row) {
        Value e = row.get("e");
        Set<ArchetypeFacet> facets = new HashSet<>();
        for (Value archetype : row.get("archetypes").values()) {
            String name = archetype.get("name").asString(null);
            if (name == null) {
                continue;
            }
            ArchetypeFacet facet = switch (name) {
                case "Item" -> new ItemFacet(
                        archetype.get("portable").asBoolean(false), archetype.get("unique").asBoolean(false));
                case "Period" -> new PeriodFacet(longOrNull(archetype.get("startTick")), longOrNull(archetype.get("endTick")));
                default -> ArchetypeFacets.of(name, null, null);
            };
            if (facet != null) {
                facets.add(facet);
            }
        }
        List<String> aliases = e.get("aliases").isNull() ? List.of() : e.get("aliases").asList(Value::asString);
        return new Entity(e.get("id").asString(), e.get("worldId").asString(null), e.get("name").asString(null),
                aliases, e.get("description").asString(null), facets);
    }

    // ----- relationships -----

    @Override
    public Optional<Relationship> addRelationship(String subjectId, String verb, String objectId) {
        return addRelationship(subjectId, verb, objectId, null, null);
    }

    @Override
    public Optional<Relationship> addRelationship(
            String subjectId, String verb, String objectId, StatementTime time, Double certainty) {
        log.debug("[Neo4j] addRelationship() subject={} verb={} object={} time={} certainty={}",
                subjectId, verb, objectId, time, certainty);
        if (subjectId == null || verb == null || verb.isBlank() || objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        RelationshipType relationshipType = RelationshipType.parse(verb);
        // The edge type is always an enum name, never free-form input.
        String type = relationshipType.name();
        double checkedCertainty = Certainty.validate(certainty);
        StatementTime checkedTime = time == null || time.isOpen() ? null : time;
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                boolean found = tx.run("""
                        MATCH (s:Entity {id: $s}) OPTIONAL MATCH (o:Entity {id: $o})
                        RETURN o IS NOT NULL AS hasObject
                        """, Map.of("s", subjectId, "o", objectId)).list().stream()
                        .anyMatch(row -> row.get("hasObject").asBoolean());
                if (!found) {
                    return Optional.<Relationship>empty();
                }
                TimeRange range = TimeRange.validate(checkedTime, id -> periodTicks(tx, id));

                String sameId = null;
                for (Record row : tx.run("MATCH (:Entity {id: $s})-[r:" + type + "]->(o:Entity) "
                        + "RETURN elementId(r) AS rid, o.id AS object, properties(r) AS props",
                        Map.of("s", subjectId)).list()) {
                    String existingObject = row.get("object").asString();
                    StatementTime existingTime = timeOf(row.get("props").asMap());
                    if (objectId.equals(existingObject) && Objects.equals(checkedTime, existingTime)) {
                        sameId = row.get("rid").asString();
                        continue;
                    }
                    if (relationshipType.singleValued()
                            && TimeRange.resolve(existingTime, id -> periodTicks(tx, id)).overlaps(range)) {
                        throw new OverlappingStatementException(
                                subjectId + " already has " + type + " " + existingObject + " during part of that time");
                    }
                }
                Map<String, Object> props = edgeProperties(checkedTime, checkedCertainty);
                if (sameId != null) {
                    tx.run("MATCH ()-[r]->() WHERE elementId(r) = $rid SET r = $props",
                            Map.of("rid", sameId, "props", props)).consume();
                } else {
                    tx.run("MATCH (s:Entity {id: $s}), (o:Entity {id: $o}) CREATE (s)-[r:" + type + "]->(o) SET r = $props",
                            Map.of("s", subjectId, "o", objectId, "props", props)).consume();
                }
                return Optional.of(new Relationship(type, objectId, checkedTime, checkedCertainty));
            });
        }
    }

    @Override
    public List<Relationship> findRelationships(String subjectId) {
        return findRelationships(subjectId, null);
    }

    @Override
    public List<Relationship> findRelationships(String subjectId, Long atTick) {
        if (subjectId == null) {
            return List.of();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                List<Relationship> out = new ArrayList<>();
                for (Record row : tx.run("""
                        MATCH (:Entity {id: $s})-[r]->(o:Entity)
                        WHERE type(r) <> 'IS_A'
                        RETURN type(r) AS verb, o.id AS object, properties(r) AS props
                        ORDER BY verb, object
                        """, Map.of("s", subjectId)).list()) {
                    Map<String, Object> props = row.get("props").asMap();
                    StatementTime time = timeOf(props);
                    if (holdsAt(tx, time, atTick)) {
                        out.add(new Relationship(RelationshipType.canonicalOrSelf(row.get("verb").asString()), row.get("object").asString(),
                                time, certaintyOf(props)));
                    }
                }
                return out;
            });
        }
    }

    @Override
    public List<Entity> findByRelationship(String verb, String objectId) {
        return findByRelationship(verb, objectId, null);
    }

    @Override
    public List<Entity> findByRelationship(String verb, String objectId, Long atTick) {
        Optional<RelationshipType> relationshipType = RelationshipType.find(verb);
        if (relationshipType.isEmpty() || objectId == null) {
            return List.of();
        }
        String type = relationshipType.get().name();
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Set<String> subjects = new LinkedHashSet<>();
                for (Record row : tx.run("MATCH (s:Entity)-[r:" + type + "]->(:Entity {id: $o}) "
                        + "RETURN s.id AS subject, properties(r) AS props ORDER BY subject",
                        Map.of("o", objectId)).list()) {
                    if (holdsAt(tx, timeOf(row.get("props").asMap()), atTick)) {
                        subjects.add(row.get("subject").asString());
                    }
                }
                if (subjects.isEmpty()) {
                    return List.<Entity>of();
                }
                return readEntities(tx, "MATCH (e:Entity) WHERE e.id IN $ids", Map.of("ids", List.copyOf(subjects)));
            });
        }
    }

    // ----- helpers -----

    private boolean holdsAt(TransactionContext tx, StatementTime time, Long atTick) {
        return atTick == null || TimeRange.resolve(time, id -> periodTicks(tx, id)).contains(atTick);
    }

    private static Optional<PeriodTicks> periodTicks(TransactionContext tx, String periodId) {
        return tx.run("MATCH (:Entity {id: $id})-[:IS_A]->(p:Archetype:Period) RETURN p LIMIT 1",
                        Map.of("id", periodId)).list().stream().findFirst()
                .map(row -> new PeriodTicks(longOrNull(row.get("p").get("startTick")),
                        longOrNull(row.get("p").get("endTick"))));
    }

    static Map<String, Object> edgeProperties(StatementTime time, double certainty) {
        Map<String, Object> props = new LinkedHashMap<>();
        if (time != null) {
            putBound(props, "start", time.start());
            putBound(props, "end", time.end());
        }
        if (certainty != Certainty.CERTAIN) {
            props.put("certainty", certainty);
        }
        return props;
    }

    private static void putBound(Map<String, Object> props, String prefix, TimeBound bound) {
        if (bound == null) {
            return;
        }
        if (bound.tick() != null) {
            props.put(prefix + "Tick", bound.tick());
        }
        if (bound.period() != null) {
            props.put(prefix + "Period", bound.period());
        }
    }

    static StatementTime timeOf(Map<String, Object> props) {
        StatementTime time = new StatementTime(boundOf(props, "start"), boundOf(props, "end"));
        return time.isOpen() ? null : time;
    }

    private static TimeBound boundOf(Map<String, Object> props, String prefix) {
        Object tick = props.get(prefix + "Tick");
        Object period = props.get(prefix + "Period");
        Long tickValue = tick instanceof Number n ? n.longValue() : null;
        TimeBound bound = new TimeBound(tickValue, period == null ? null : period.toString());
        return bound.isEmpty() ? null : bound;
    }

    private static double certaintyOf(Map<String, Object> props) {
        Object value = props.get("certainty");
        return value instanceof Number n ? n.doubleValue() : Certainty.CERTAIN;
    }

    private static Long longOrNull(Value value) {
        return value == null || value.isNull() ? null : value.asLong();
    }
}
