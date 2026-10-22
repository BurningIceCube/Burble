package com.bic.burble.ontology.domain.entity;

import com.bic.burble.ontology.domain.statement.StatementTime;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One archetype in a request. Either a plain name such as {@code "Character"}
 * (always, certain) or an object with an optional time and certainty.
 */
@Schema(description = "An archetype name, or {archetype, time, certainty} for a timed IS_A statement")
public record ArchetypeRequest(
        @Schema(description = "Archetype name from the closed list", example = "Character")
        String archetype,
        @Schema(description = "When the entity has this archetype. Leave out for always.")
        StatementTime time,
        @Schema(description = "How sure the world is, 0 to 1. Defaults to 1.", example = "0.8")
        Double certainty
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ArchetypeRequest {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ArchetypeRequest of(String archetype) {
        return new ArchetypeRequest(archetype, null, null);
    }

    /** True when only a name was given, as in requests from before archetypes had time. */
    public boolean nameOnly() {
        return (time == null || time.isOpen()) && certainty == null;
    }
}
