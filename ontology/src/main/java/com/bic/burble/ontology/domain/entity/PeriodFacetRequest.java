package com.bic.burble.ontology.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Period archetype details; presence implies this entity is a Period")
public record PeriodFacetRequest(
    @Schema(description = "Tick the period starts at", example = "250")
    Long startTick,

    @Schema(description = "Tick the period ends at (exclusive)", example = "400")
    Long endTick
) {}
