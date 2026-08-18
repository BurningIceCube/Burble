package com.bic.burble.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after creating a new world")
public record CreateWorldResponse(
        @Schema(description = "Unique GUID of the newly created world", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String worldId
) {}
