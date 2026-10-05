package com.bic.burble.ontology.persistence.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Objects;

/**
 * Archetype node for the {@code Item} archetype. Unlike shared archetype
 * nodes (e.g. {@link CharacterArchetypeNode}), Item carries per-entity
 * properties ({@code portable}, {@code unique}), so each entity gets its own
 * Item archetype node instance.
 */
@Node("Item")
public class ItemArchetypeNode implements ArchetypeNode {
    @Id
    @GeneratedValue
    private String id;

    private boolean portable;
    private boolean unique;

    public ItemArchetypeNode() {
    }

    public ItemArchetypeNode(boolean portable, boolean unique) {
        this.portable = portable;
        this.unique = unique;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isPortable() {
        return portable;
    }

    public void setPortable(boolean portable) {
        this.portable = portable;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemArchetypeNode that = (ItemArchetypeNode) o;
        return portable == that.portable &&
                unique == that.unique &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, portable, unique);
    }

    @Override
    public String toString() {
        return "ItemArchetypeNode{" +
                "id='" + id + '\'' +
                ", portable=" + portable +
                ", unique=" + unique +
                '}';
    }
}
