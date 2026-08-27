package com.bic.burble.ontology.domain.world;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating a new world")
public record CreateWorldRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Schema(description = "Name of the world", example = "Aethelgard", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Detailed description or overview of the world", example = "A high-fantasy world of shattered floating continents.")
    String description
) {}
