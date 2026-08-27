package com.bic.burble.ontology.persistence.neo4j;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.persistence.EntityRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Adapts the domain {@link Entity} to the Neo4j storage method: the entity
 * itself is a single {@code Entity} node, and every archetype it belongs to
 * is stored as its own node connected via an outgoing {@code IS_A}
 * relationship (see {@link EntityNode} and {@link ArchetypeNode}).
 */
@Component
@Profile("neo4j")
public class Neo4jEntityRepositoryAdapter implements EntityRepository {

    private final EntityNeo4jRepository repository;
    private final Neo4jEntityMapper mapper;

    public Neo4jEntityRepositoryAdapter(EntityNeo4jRepository repository, Neo4jEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Entity save(Entity entity) {
        EntityNode node = mapper.toNode(entity);
        node.setArchetypes(toArchetypeNodes(entity.facets()));

        EntityNode saved = repository.save(node);
        return toDomainWithFacets(saved);
    }

    @Override
    public Optional<Entity> findById(String worldId, String id) {
        return repository.findByWorldIdAndId(worldId, id).map(this::toDomainWithFacets);
    }

    // ----- archetype <-> node conversions -----

    private Entity toDomainWithFacets(EntityNode node) {
        Entity entity = mapper.toDomain(node);
        Set<ArchetypeFacet> facets = toArchetypeFacets(node.getArchetypes());
        return new Entity(entity.guid(), entity.name(), entity.aliases(), entity.description(), facets);
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
