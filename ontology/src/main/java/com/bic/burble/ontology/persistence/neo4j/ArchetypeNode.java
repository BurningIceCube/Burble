package com.bic.burble.ontology.persistence.neo4j;

/**
 * Marker interface for archetype nodes attached to an {@link EntityNode} via
 * an {@code IS_A} relationship (e.g. {@link ItemArchetypeNode}, {@link CharacterArchetypeNode}).
 *
 * <p>Per the Neo4j storage method: the main entity is stored as a single
 * {@code Entity} node, while each archetype it belongs to (Character, Item,
 * Species, etc.) is stored as its own node with an {@code IS_A} relationship
 * pointing from the entity to the archetype. This keeps archetype-specific
 * properties and relationships on dedicated nodes, enabling richer traversal,
 * indexing, and querying across the graph.</p>
 */
public interface ArchetypeNode {
}
