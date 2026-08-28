package com.bic.burble.ontology.persistence.h2;

import com.bic.burble.ontology.domain.world.WorldRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorldEntityMapper {

    WorldEntity toEntity(WorldRecord record);

    WorldRecord toDomain(WorldEntity entity);
}
