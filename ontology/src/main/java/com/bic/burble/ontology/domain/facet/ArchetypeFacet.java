package com.bic.burble.ontology.domain.facet;

public sealed interface ArchetypeFacet permits
        CharacterFacet,
        ItemFacet,
        LocationFacet,
        SubstanceFacet,
        OrganizationFacet,
        SpeciesFacet,
        LineageFacet,
        EventFacet,
        PeriodFacet,
        InstanceFacet,
        ActionFacet,
        TraitFacet,
        TitleFacet,
        SystemFacet,
        WorldviewFacet,
        ConceptFacet,
        LogicFacet,
        RuleFacet {
}
