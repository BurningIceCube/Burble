package com.bic.burble.ontology.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after creating a new entity")
public record CreateEntityResponse(
        @Schema(description = "Unique GUID of the newly created entity", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String entityId
) {}
