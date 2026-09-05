package com.bic.burble.ontology.persistence.neo4j;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.persistence.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapts the domain {@link Entity} to the Neo4j storage method: the entity
 * itself is a single {@code Entity} node, and every archetype it belongs to
 * is stored as its own node connected via an outgoing {@code IS_A}
 * relationship (see {@link EntityNode} and {@link ArchetypeNode}).
 *
 * <p>Activated via the {@code burble.ontology.persistance.entity=neo4j}
 * configuration property (see {@code application.yml}), mirroring how
 * {@code H2WorldRepositoryAdapter} is activated for world storage.
 */
@Component
@ConditionalOnProperty(prefix = "burble.ontology.persistance", name = "entity", havingValue = "neo4j")
public class Neo4jEntityRepositoryAdapter implements EntityRepository {

    private static final Logger log = LoggerFactory.getLogger(Neo4jEntityRepositoryAdapter.class);

    private final EntityNeo4jRepository repository;
    private final Neo4jEntityMapper mapper;

    public Neo4jEntityRepositoryAdapter(EntityNeo4jRepository repository, Neo4jEntityMapper mapper) {
        log.debug("Neo4jEntityRepositoryAdapter activated (burble.ontology.persistance.entity=neo4j)");
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Entity save(Entity entity) {
        log.debug("[Neo4j] save() - mapping Entity to EntityNode: {}", entity);
        EntityNode node = mapper.toNode(entity);
        Set<ArchetypeNode> archetypeNodes = toArchetypeNodes(entity.facets());
        node.setArchetypes(archetypeNodes);
        log.debug("[Neo4j] save() - persisting EntityNode: {} with archetypes={}", node, archetypeNodes);

        EntityNode saved = repository.save(node);
        log.debug("[Neo4j] save() - persisted EntityNode: {}", saved);
        return toDomainWithFacets(saved);
    }

    @Override
    public Optional<Entity> findById(String id) {
        log.debug("[Neo4j] findById() - id={}", id);
        Optional<Entity> result = repository.findById(id).map(this::toDomainWithFacets);
        log.debug("[Neo4j] findById() - id={} result={}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public List<Entity> findAllByWorldId(String worldId) {
        log.debug("[Neo4j] findAllByWorldId() - worldId={}", worldId);
        List<Entity> results = repository.findByWorldId(worldId).stream()
                .map(this::toDomainWithFacets)
                .toList();
        log.debug("[Neo4j] findAllByWorldId() - worldId={} returned {} record(s)", worldId, results.size());
        return results;
    }

    @Override
    public boolean deleteById(String id) {
        log.debug("[Neo4j] deleteById() - id={}", id);
        if (!repository.existsById(id)) {
            log.debug("[Neo4j] deleteById() - id={} does not exist, skipping delete", id);
            return false;
        }
        repository.deleteById(id);
        log.debug("[Neo4j] deleteById() - id={} deleted", id);
        return true;
    }

    @Override
    public Entity update(Entity entity) {
        // Neo4j SDN's save() merges on the node's @Id, so it acts as an
        // upsert; updating uses the same create-or-replace semantics as save.
        log.debug("[Neo4j] update() - delegating to save() for entity guid={}", entity.guid());
        return save(entity);
    }

    // ----- archetype <-> node conversions -----

    private Entity toDomainWithFacets(EntityNode node) {
        Entity entity = mapper.toDomain(node);
        Set<ArchetypeFacet> facets = toArchetypeFacets(node.getArchetypes());
        log.debug("[Neo4j] Mapped EntityNode id={} to domain Entity with facets={}", node.getId(), facets);
        return new Entity(entity.guid(), entity.worldId(), entity.name(), entity.aliases(), entity.description(), facets);
    }

    private Set<ArchetypeNode> toArchetypeNodes(Set<ArchetypeFacet> facets) {
        if (facets == null) return Set.of();

        Set<ArchetypeNode> archetypeNodes = new HashSet<>();
        for (ArchetypeFacet facet : facets) {
            if (facet instanceof ItemFacet item) {
                archetypeNodes.add(new ItemArchetypeNode(item.portable(), item.unique()));
            } else if (facet instanceof CharacterFacet) {
                archetypeNodes.add(new CharacterArchetypeNode());
            }
        }
        log.debug("[Neo4j] Converted {} facet(s) to {} archetype node(s)", facets.size(), archetypeNodes.size());
        return archetypeNodes;
    }

    private Set<ArchetypeFacet> toArchetypeFacets(Set<ArchetypeNode> archetypeNodes) {
        if (archetypeNodes == null) return Set.of();

        Set<ArchetypeFacet> facets = new HashSet<>();
        for (ArchetypeNode archetypeNode : archetypeNodes) {
            if (archetypeNode instanceof ItemArchetypeNode item) {
                facets.add(new ItemFacet(item.isPortable(), item.isUnique()));
            } else if (archetypeNode instanceof CharacterArchetypeNode) {
                facets.add(new CharacterFacet());
            }
        }
        return facets;
    }
}
