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
    ItemFacetRequest item,

    @Valid
    @Schema(description = "Archetypes stored as IS_A statements. Each entry is a name such as \"Location\" (always) or {archetype, time, certainty}. "
            + "The same archetype may appear more than once if the times do not overlap. Item properties still come from item. Character may also be set with character.",
            example = "[\"Location\", {\"archetype\": \"Rule\", \"time\": {\"start\": {\"tick\": 100}}}]")
    List<ArchetypeRequest> archetypes,

    @Valid
    @Schema(description = "Period archetype details; presence implies this entity is a Period whose ticks other statements can use")
    PeriodFacetRequest period
) {
    public CreateEntityRequest(String name, List<String> aliases, String description, Boolean character,
                               ItemFacetRequest item, List<ArchetypeRequest> archetypes) {
        this(name, aliases, description, character, item, archetypes, null);
    }
}
