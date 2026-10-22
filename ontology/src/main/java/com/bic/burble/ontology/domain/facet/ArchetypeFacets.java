package com.bic.burble.ontology.domain.facet;

import java.util.List;

/**
 * The closed archetype list from {@code docs/ontology/archetypes.md}.
 * Item keeps {@code portable} and {@code unique}. Every other archetype is
 * an empty marker stored as an {@code IS_A} statement.
 */
public final class ArchetypeFacets {

    public static final List<String> NAMES = List.of(
            "Character",
            "Item",
            "Location",
            "Substance",
            "Organization",
            "Species",
            "Lineage",
            "Event",
            "Period",
            "Instance",
            "Action",
            "Trait",
            "Title",
            "System",
            "Worldview",
            "Concept",
            "Logic",
            "Rule"
    );

    private ArchetypeFacets() {
    }

    public static boolean isKnown(String name) {
        return NAMES.contains(name);
    }

    public static String nameOf(ArchetypeFacet facet) {
        return switch (facet) {
            case CharacterFacet ignored -> "Character";
            case ItemFacet ignored -> "Item";
            case LocationFacet ignored -> "Location";
            case SubstanceFacet ignored -> "Substance";
            case OrganizationFacet ignored -> "Organization";
            case SpeciesFacet ignored -> "Species";
            case LineageFacet ignored -> "Lineage";
            case EventFacet ignored -> "Event";
            case PeriodFacet ignored -> "Period";
            case InstanceFacet ignored -> "Instance";
            case ActionFacet ignored -> "Action";
            case TraitFacet ignored -> "Trait";
            case TitleFacet ignored -> "Title";
            case SystemFacet ignored -> "System";
            case WorldviewFacet ignored -> "Worldview";
            case ConceptFacet ignored -> "Concept";
            case LogicFacet ignored -> "Logic";
            case RuleFacet ignored -> "Rule";
        };
    }

    /**
     * @return the facet for a formal archetype name, or {@code null} when
     * {@code name} is not on the closed list. Item flags default to false
     * when the corresponding argument is null.
     */
    public static ArchetypeFacet of(String name, Boolean portable, Boolean unique) {
        return switch (name) {
            case "Character" -> new CharacterFacet();
            case "Item" -> new ItemFacet(Boolean.TRUE.equals(portable), Boolean.TRUE.equals(unique));
            case "Location" -> new LocationFacet();
            case "Substance" -> new SubstanceFacet();
            case "Organization" -> new OrganizationFacet();
            case "Species" -> new SpeciesFacet();
            case "Lineage" -> new LineageFacet();
            case "Event" -> new EventFacet();
            case "Period" -> new PeriodFacet();
            case "Instance" -> new InstanceFacet();
            case "Action" -> new ActionFacet();
            case "Trait" -> new TraitFacet();
            case "Title" -> new TitleFacet();
            case "System" -> new SystemFacet();
            case "Worldview" -> new WorldviewFacet();
            case "Concept" -> new ConceptFacet();
            case "Logic" -> new LogicFacet();
            case "Rule" -> new RuleFacet();
            default -> null;
        };
    }
}
