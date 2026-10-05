package com.bic.burble.ontology.persistence.h2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;
import java.util.Objects;

/**
 * JPA entity backing the H2 storage method for worlds. Field names mirror
 * {@link com.bic.burble.ontology.domain.world.WorldRecord} so that
 * {@link WorldEntityMapper} can map between them without explicit rules.
 */
@Entity
@Table(name = "worlds")
public class WorldEntity {

    @Id
    private String guid;

    private String name;

    @Column(length = 1000)
    private String description;

    private String owner;

    private Date createdAt;

    private Date updatedAt;

    public WorldEntity() {
    }

    public WorldEntity(String guid, String name, String description, String owner, Date createdAt, Date updatedAt) {
        this.guid = guid;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldEntity that = (WorldEntity) o;
        return Objects.equals(guid, that.guid) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(guid, name, description, owner, createdAt, updatedAt);
    }
}
