package com.bic.burble.ontology.domain.world;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

@Schema(description = "Representation of a World entity")
public record WorldRecord(
        @Schema(description = "Unique identifier (UUID) of the world", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", accessMode = Schema.AccessMode.READ_ONLY)
        String guid,

        @Schema(description = "Name of the world", example = "Aethelgard")
        String name,

        @Schema(description = "Detailed description or overview of the world", example = "A high-fantasy world of shattered floating continents.")
        String description,

        @Schema(description = "Owner or creator identifier", example = "system", accessMode = Schema.AccessMode.READ_ONLY)
        String owner,

        @Schema(description = "Timestamp when the world was created", example = "2026-08-18T21:02:33.699Z", accessMode = Schema.AccessMode.READ_ONLY)
        Date createdAt,

        @Schema(description = "Timestamp when the world was last updated", example = "2026-08-18T21:02:33.699Z", accessMode = Schema.AccessMode.READ_ONLY)
        Date updatedAt
) {}

