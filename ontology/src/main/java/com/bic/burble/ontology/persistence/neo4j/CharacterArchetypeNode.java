package com.bic.burble.ontology.persistence.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Objects;

/**
 * Archetype node for the {@code Character} archetype. Carries no
 * entity-specific properties, so a single shared node (fixed id) is reused
 * across every entity that {@code IS_A} Character rather than creating a
 * duplicate node per entity.
 */
@Node("Character")
public class CharacterArchetypeNode implements ArchetypeNode {

    /** Fixed id so repeated saves reuse (merge onto) the same shared node. */
    public static final String SHARED_ID = "Character";

    @Id
    private String id = SHARED_ID;

    public CharacterArchetypeNode() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterArchetypeNode that = (CharacterArchetypeNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CharacterArchetypeNode{id='" + id + "'}";
    }
}
