package com.bic.burble.ontology.persistence.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Lean Neo4j node for an {@code Entity}. Per the Neo4j storage method, each
 * archetype the entity belongs to is stored as its own node (see
 * {@link ArchetypeNode}) linked from this entity via an outgoing
 * {@code IS_A} relationship (e.g. {@code (Entity)-[:IS_A]->(Character)}),
 * rather than as a property or dynamic label on the entity node itself.
 */
@Node("Entity")
public class EntityNode {
    @Id
    private String id;
    private String worldId;
    private String name;
    private String description;
    private List<String> aliases;

    @Relationship(type = "IS_A", direction = Relationship.Direction.OUTGOING)
    private Set<ArchetypeNode> archetypes = new HashSet<>();

    public EntityNode() {
    }

    public EntityNode(String id, String worldId, String name, String description, List<String> aliases) {
        this.id = id;
        this.worldId = worldId;
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public Set<ArchetypeNode> getArchetypes() {
        return archetypes;
    }

    public void setArchetypes(Set<ArchetypeNode> archetypes) {
        this.archetypes = archetypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityNode that = (EntityNode) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(worldId, that.worldId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(archetypes, that.archetypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, worldId, name, description, aliases, archetypes);
    }

    @Override
    public String toString() {
        return "EntityNode{" +
                "id='" + id + '\'' +
                ", worldId='" + worldId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", aliases=" + aliases +
                ", archetypes=" + archetypes +
                '}';
    }
}
