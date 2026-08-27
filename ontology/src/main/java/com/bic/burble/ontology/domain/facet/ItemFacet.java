package com.bic.burble.ontology.domain.facet;

public record ItemFacet(
        boolean portable,
        boolean unique
) implements ArchetypeFacet {}
