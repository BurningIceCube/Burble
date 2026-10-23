package com.bic.burble.ontology.domain.composite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** The result of a composite query, ready to render: the root and one group per section. */
@Schema(description = "A composite computed from archetypes and relationships. Nothing here is stored.")
public record CompositeView(
        @Schema(example = "culture") String composite,
        @Schema(example = "Culture") String label,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "The tick the view was computed at; missing means all time") Long atTick,
        EntitySummary root,
        List<SectionView> sections
) {
    /** One group of entities. */
    public record SectionView(String key, String label, String description,
                              @Schema(description = "How entities are reached from the root", example = "<-BELIEVES- (Character)")
                              String path,
                              List<EntitySummary> entities) {
    }

    /**
     * An entity in the view. {@code archetypes} are the ones active at the
     * tick. {@code certainty} is the certainty of the statement that linked
     * it in; {@code via} is how it was reached, root first.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EntitySummary(String id, String name, List<String> archetypes, Double certainty, List<Hop> via) {
    }

    /** One stored statement that was followed: {@code subject} {@code verb} {@code object}. */
    public record Hop(String subject, String verb, String object) {
    }

    /** One composite in the discovery list. */
    public record CompositeInfo(String name, String label, String description, List<String> rootArchetypes,
                                List<SectionInfo> sections) {
    }

    public record SectionInfo(String key, String label, String description, String path) {
    }
}
