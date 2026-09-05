package com.bic.burble.ontology.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item archetype facet details")
public record ItemFacetRequest(
    @Schema(description = "Whether the item can be carried", example = "true")
    boolean portable,

    @Schema(description = "Whether only one instance of this item can exist", example = "false")
    boolean unique
) {}
