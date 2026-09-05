package com.bic.burble.ontology.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request payload for creating or updating an Entity")
public record CreateEntityRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Schema(description = "Name of the entity", example = "Aldric the Bold", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(description = "Alternate names/aliases for the entity", example = "[\"The Bold\"]")
    List<String> aliases,

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Detailed description of the entity", example = "A wandering knight.")
    String description,

    @Schema(description = "Whether this entity is a Character archetype", example = "true")
    Boolean character,

    @Valid
    @Schema(description = "Item archetype details; presence implies this entity is an Item")
    ItemFacetRequest item
) {}
