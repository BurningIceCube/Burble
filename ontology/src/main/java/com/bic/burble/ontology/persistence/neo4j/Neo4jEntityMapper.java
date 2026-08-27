package com.bic.burble.ontology.persistence.neo4j;

import com.bic.burble.ontology.domain.Entity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface Neo4jEntityMapper {

    // ---------- Domain → Node ----------
    // Archetypes are handled separately by the adapter, since they require
    // building/reading related archetype nodes linked via IS_A.
    @Mapping(target = "id", source = "guid")
    @Mapping(target = "worldId", ignore = true)
    @Mapping(target = "archetypes", ignore = true)
    EntityNode toNode(Entity entity);

    // ---------- Node → Domain ----------
    @Mapping(target = "guid", source = "id")
    @Mapping(target = "facets", ignore = true)
    Entity toDomain(EntityNode node);
}
