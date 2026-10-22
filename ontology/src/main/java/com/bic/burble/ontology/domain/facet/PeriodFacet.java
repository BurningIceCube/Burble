package com.bic.burble.ontology.domain.facet;

/**
 * Period archetype. {@code startTick} and {@code endTick} place the period on
 * the world timeline, so a statement that names this period as a time bound
 * resolves to these ticks. Either may be null when unknown.
 */
public record PeriodFacet(Long startTick, Long endTick) implements ArchetypeFacet {
    public PeriodFacet() {
        this(null, null);
    }
}
