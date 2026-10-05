package com.bic.burble.ontology.persistence.neo4j;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link Neo4jEntityRepositoryAdapter#save} builds the graph
 * per the Neo4j storage method: the entity is a single {@code Entity} node,
 * and each {@link ArchetypeFacet} it carries is attached as its own
 * archetype node via an outgoing {@code IS_A} relationship (modeled here by
 * {@link EntityNode#getArchetypes()}, which Spring Data Neo4j persists as
 * {@code IS_A} edges per {@link EntityNode}'s {@code @Relationship} field).
 */
@ExtendWith(MockitoExtension.class)
class Neo4jEntityRepositoryAdapterTest {

    @Mock
    private EntityNeo4jRepository repository;

    @Mock
    private Neo4jEntityMapper mapper;

    @Test
    void saveAttachesNewArchetypeNodesViaIsARelationship() {
        Neo4jEntityRepositoryAdapter adapter = new Neo4jEntityRepositoryAdapter(repository, mapper);

        Entity entity = new Entity(
                "entity-1",
                "world-1",
                "Sword of Testing",
                List.of(),
                "A blade forged for unit tests.",
                Set.of(new ItemFacet(true, false), new CharacterFacet())
        );

        EntityNode nodeFromMapper = new EntityNode("entity-1", "world-1", "Sword of Testing", "A blade forged for unit tests.", List.of());
        when(mapper.toNode(entity)).thenReturn(nodeFromMapper);

        // Simulate the Neo4j driver assigning IDs and persisting relationships:
        // repository.save returns the same node with archetypes attached.
        when(repository.save(any(EntityNode.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDomain(any(EntityNode.class))).thenReturn(
                new Entity("entity-1", "world-1", "Sword of Testing", List.of(), "A blade forged for unit tests.", Set.of())
        );

        Entity saved = adapter.save(entity);

        // The node passed to save() must carry brand-new archetype nodes,
        // one per facet, connected via the IS_A relationship field.
        ArgumentCaptor<EntityNode> nodeCaptor = ArgumentCaptor.forClass(EntityNode.class);
        org.mockito.Mockito.verify(repository).save(nodeCaptor.capture());

        Set<ArchetypeNode> persistedArchetypes = nodeCaptor.getValue().getArchetypes();
        assertThat(persistedArchetypes).hasSize(2);
        assertThat(persistedArchetypes).anySatisfy(node -> {
            assertThat(node).isInstanceOf(ItemArchetypeNode.class);
            ItemArchetypeNode item = (ItemArchetypeNode) node;
            assertThat(item.isPortable()).isTrue();
            assertThat(item.isUnique()).isFalse();
        });
        assertThat(persistedArchetypes).anySatisfy(node -> assertThat(node).isInstanceOf(CharacterArchetypeNode.class));

        // The domain result reflects the facets restored from the saved node's archetypes.
        assertThat(saved.facets()).hasSize(2);
        assertThat(saved.hasFacet(ItemFacet.class)).isTrue();
        assertThat(saved.hasFacet(CharacterFacet.class)).isTrue();
    }

    @Test
    void saveWithNoFacetsAttachesNoArchetypeNodes() {
        Neo4jEntityRepositoryAdapter adapter = new Neo4jEntityRepositoryAdapter(repository, mapper);

        Entity entity = new Entity("entity-2", "world-1", "Plain Rock", List.of(), "Just a rock.", Set.of());
        EntityNode nodeFromMapper = new EntityNode("entity-2", "world-1", "Plain Rock", "Just a rock.", List.of());
        when(mapper.toNode(entity)).thenReturn(nodeFromMapper);
        when(repository.save(any(EntityNode.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDomain(any(EntityNode.class))).thenReturn(entity);

        adapter.save(entity);

        ArgumentCaptor<EntityNode> nodeCaptor = ArgumentCaptor.forClass(EntityNode.class);
        org.mockito.Mockito.verify(repository).save(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getArchetypes()).isEmpty();
    }
}
