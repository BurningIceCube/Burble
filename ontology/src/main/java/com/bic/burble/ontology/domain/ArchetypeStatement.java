package com.bic.burble.ontology.domain;

import com.bic.burble.ontology.domain.statement.Certainty;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One {@code IS_A} statement: the entity is {@code archetype} during
 * {@code time} (null means always) with {@code certainty} 0..1.
 *
 * <p>An entity can have the same archetype more than once as long as the
 * ranges do not overlap. For a Period entity this time is when the entity is
 * a Period; it is not the period's own {@code startTick}/{@code endTick},
 * which is what other statements resolve a period bound to.
 */
@Schema(description = "An IS_A statement: the archetype, when it holds and how certain it is")
public record ArchetypeStatement(
        @Schema(description = "Archetype name from the closed list", example = "Character")
        String archetype,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "When the entity has this archetype. Missing means always.")
        StatementTime time,
        @Schema(description = "How sure the world is, 0 to 1", example = "1.0")
        double certainty
) {
    public ArchetypeStatement {
        if (time != null && time.isOpen()) {
            time = null;
        }
    }

    public static ArchetypeStatement always(String archetype) {
        return new ArchetypeStatement(archetype, null, Certainty.CERTAIN);
    }
}
