package com.bic.burble.ontology.domain.composite;

import com.bic.burble.ontology.domain.RelationshipType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bic.burble.ontology.domain.RelationshipType.BELIEVES;
import static com.bic.burble.ontology.domain.RelationshipType.EXISTS_DURING;
import static com.bic.burble.ontology.domain.RelationshipType.USES;
import static com.bic.burble.ontology.domain.RelationshipType.INHABITS;
import static com.bic.burble.ontology.domain.RelationshipType.SPEAKS;
import static com.bic.burble.ontology.domain.RelationshipType.CAN_PERFORM;
import static com.bic.burble.ontology.domain.RelationshipType.COMPONENT_OF;
import static com.bic.burble.ontology.domain.RelationshipType.DURING;
import static com.bic.burble.ontology.domain.RelationshipType.ENACTED_BY;
import static com.bic.burble.ontology.domain.RelationshipType.GOVERNED_BY;
import static com.bic.burble.ontology.domain.RelationshipType.HAS_TRAIT;
import static com.bic.burble.ontology.domain.RelationshipType.HELD_BY;
import static com.bic.burble.ontology.domain.RelationshipType.KNOWS;
import static com.bic.burble.ontology.domain.RelationshipType.LOCATED_IN;
import static com.bic.burble.ontology.domain.RelationshipType.MADE_OF;
import static com.bic.burble.ontology.domain.RelationshipType.MEMBER_OF;
import static com.bic.burble.ontology.domain.RelationshipType.OF_SPECIES;
import static com.bic.burble.ontology.domain.RelationshipType.OWNED_BY;
import static com.bic.burble.ontology.domain.RelationshipType.PART_OF;
import static com.bic.burble.ontology.domain.composite.Composite.Step.in;
import static com.bic.burble.ontology.domain.composite.Composite.Step.out;

/**
 * The composites from {@code docs/ontology/composites.md}. A composite is
 * not stored: it is a read-only query that starts at a root entity holding
 * one of the root archetypes and fills named sections by following
 * relationships from the root.
 *
 * <p>Each section is one or two steps. {@code in(BELIEVES, "Character")}
 * means "entities that store BELIEVES pointing at the current entity and are
 * a Character"; {@code out(MADE_OF)} means "entities the current entity
 * stores MADE_OF pointing at". A section that does not apply to the root
 * (for example a Species section on a Location root) is just empty.
 */
public enum Composite {

    CULTURE("Culture", "The people, groups, ideas and customs that share a worldview.",
            roots("Worldview"),
            section("communities", "Communities", "Organizations that hold this worldview", in(BELIEVES, "Organization")),
            section("people", "People", "Characters who hold this worldview", in(BELIEVES, "Character")),
            section("species", "Species", "Species of the characters who hold it", in(BELIEVES, "Character"), out(OF_SPECIES)),
            section("ideas", "Ideas", "Concepts that are part of this worldview", in(COMPONENT_OF, "Concept")),
            section("customs", "Customs", "Rules that are part of this worldview", in(COMPONENT_OF, "Rule")),
            section("traits", "Traits", "Traits of this worldview", out(HAS_TRAIT))),

    RELIGION("Religion", "A faith: its followers, institutions, doctrines and commandments.",
            roots("Worldview"),
            section("followers", "Followers", "Characters who believe it", in(BELIEVES, "Character")),
            section("institutions", "Institutions", "Organizations that believe it", in(BELIEVES, "Organization")),
            section("clergy", "Clergy", "Members of those institutions", in(BELIEVES, "Organization"), in(MEMBER_OF, "Character")),
            section("doctrines", "Doctrines", "Concepts that are part of it", in(COMPONENT_OF, "Concept")),
            section("commandments", "Commandments", "Rules that are part of it", in(COMPONENT_OF, "Rule")),
            section("lawgivers", "Lawgivers", "Who enacted those rules", in(COMPONENT_OF, "Rule"), out(ENACTED_BY))),

    ECONOMY("Economy", "How goods, wealth and standing move under a system.",
            roots("System"),
            section("rules", "Rules", "Rules that make up the system", in(COMPONENT_OF, "Rule")),
            section("participants", "Participants", "Organizations governed by it", in(GOVERNED_BY, "Organization")),
            section("regions", "Regions", "Locations governed by it", in(GOVERNED_BY, "Location")),
            section("resources", "Resources", "Substances the system uses", out(USES)),
            section("regional-resources", "Regional resources", "What the regions it governs are made of", in(GOVERNED_BY, "Location"), out(MADE_OF)),
            section("titles", "Titles", "Titles held by the participating organizations", in(GOVERNED_BY, "Organization"), in(HELD_BY, "Title"))),

    ECOLOGY("Ecology", "The living things of a place, or the places a species lives.",
            roots("Location", "Species"),
            section("inhabitants", "Inhabitants", "Characters located here", in(LOCATED_IN, "Character")),
            section("species", "Species", "Species that inhabit this place", in(INHABITS)),
            section("visiting-species", "Species of inhabitants", "Species of the characters located here", in(LOCATED_IN, "Character"), out(OF_SPECIES)),
            section("materials", "Materials", "Substances this place is made of", out(MADE_OF)),
            section("systems", "Systems", "Systems that govern this place", out(GOVERNED_BY, "System")),
            section("subregions", "Subregions", "Locations that are part of this one", in(PART_OF, "Location")),
            section("members", "Members", "Characters of this species", in(OF_SPECIES)),
            section("habitats", "Habitats", "Locations this species inhabits", out(INHABITS)),
            section("member-locations", "Where members are", "Where characters of this species are located", in(OF_SPECIES), out(LOCATED_IN))),

    TECHNOLOGY("Technology", "What a system lets people build and do, and the principles behind it.",
            roots("System"),
            section("techniques", "Techniques", "Actions that are part of the system", in(COMPONENT_OF, "Action")),
            section("principles", "Principles", "Logic that is part of the system", in(COMPONENT_OF, "Logic")),
            section("devices", "Devices", "Items that can perform those techniques", in(COMPONENT_OF, "Action"), in(CAN_PERFORM, "Item")),
            section("practitioners", "Practitioners", "Characters that can perform those techniques", in(COMPONENT_OF, "Action"), in(CAN_PERFORM, "Character")),
            section("materials", "Materials", "Substances the system uses", out(USES)),
            section("technique-materials", "Technique materials", "Substances its techniques use", in(COMPONENT_OF, "Action"), out(USES))),

    MAGIC_SYSTEM("Magic System", "How magic works: its laws, spells, limits and who can use it.",
            roots("System"),
            section("laws", "Laws", "Logic that is part of the system", in(COMPONENT_OF, "Logic")),
            section("spells", "Spells", "Actions that are part of the system", in(COMPONENT_OF, "Action")),
            section("rules", "Rules", "Rules that are part of the system", in(COMPONENT_OF, "Rule")),
            section("casters", "Casters", "Characters that can perform its spells", in(COMPONENT_OF, "Action"), in(CAN_PERFORM, "Character")),
            section("gifted-species", "Gifted species", "Species that can perform its spells", in(COMPONENT_OF, "Action"), in(CAN_PERFORM, "Species")),
            section("artifacts", "Artifacts", "Items that can perform its spells", in(COMPONENT_OF, "Action"), in(CAN_PERFORM, "Item")),
            section("reagents", "Reagents", "Substances the system uses", out(USES)),
            section("spell-reagents", "Spell reagents", "Substances its spells use", in(COMPONENT_OF, "Action"), out(USES))),

    LANGUAGE("Language", "A language: its words, grammar and who knows it.",
            roots("System", "Concept"),
            section("vocabulary", "Vocabulary", "Concepts that are part of it", in(COMPONENT_OF, "Concept")),
            section("grammar", "Grammar", "Rules and logic that are part of it", in(COMPONENT_OF, "Rule", "Logic")),
            section("speakers", "Speakers", "Characters, organizations and species that speak it", in(SPEAKS)),
            section("knowers", "Known by", "Characters who know it as a concept", in(KNOWS, "Character")),
            section("worldviews", "Worldviews", "Worldviews it is part of", out(COMPONENT_OF, "Worldview"))),

    GOVERNMENT("Government", "Who rules, by what offices and laws, and over what.",
            roots("Organization", "System"),
            section("officials", "Officials", "Characters who are members", in(MEMBER_OF, "Character")),
            section("offices", "Offices", "Titles held by it", in(HELD_BY, "Title")),
            section("office-holders", "Office holders", "Who holds the titles it owns", in(OWNED_BY, "Title"), out(HELD_BY)),
            section("laws", "Laws", "Rules it enacted", in(ENACTED_BY, "Rule")),
            section("system-rules", "System rules", "Rules that make up the system", in(COMPONENT_OF, "Rule")),
            section("governed-by", "Governed by", "Systems and rules it is governed by", out(GOVERNED_BY)),
            section("governs", "Governs", "Organizations and locations governed by the system", in(GOVERNED_BY)),
            section("territory", "Territory", "Locations it owns", in(OWNED_BY, "Location")),
            section("ideology", "Ideology", "Worldviews it believes", out(BELIEVES)),
            section("branches", "Branches", "Organizations that are part of it", in(PART_OF, "Organization"))),

    COSMOLOGY("Cosmology", "The shape of the world: its realms, laws of nature, beliefs about it and its ages.",
            roots("Location", "Logic", "Worldview"),
            section("realms", "Realms", "Locations that are part of this one", in(PART_OF, "Location")),
            section("within", "Within", "Locations this one is part of", out(PART_OF, "Location")),
            section("ages", "Ages", "Periods this place, law or belief exists during", out(EXISTS_DURING)),
            section("event-ages", "Ages of events", "Periods of events that happened here", in(LOCATED_IN, "Event"), out(DURING)),
            section("laws", "Laws of nature", "Logic that is part of this worldview", in(COMPONENT_OF, "Logic")),
            section("ideas", "Ideas", "Concepts that are part of this worldview", in(COMPONENT_OF, "Concept")),
            section("frameworks", "Frameworks", "Systems and worldviews this logic is part of", out(COMPONENT_OF)));

    /** Which way a step follows a relationship from the current entity. */
    public enum Direction {
        /** The current entity is the subject; go to the object. */
        OUT,
        /** The current entity is the object; go to the subjects. */
        IN
    }

    /** One relationship hop, keeping only entities with one of {@code archetypes} (empty means any). */
    public record Step(Direction direction, RelationshipType verb, Set<String> archetypes) {
        static Step in(RelationshipType verb, String... archetypes) {
            return new Step(Direction.IN, verb, Set.of(archetypes));
        }

        static Step out(RelationshipType verb, String... archetypes) {
            return new Step(Direction.OUT, verb, Set.of(archetypes));
        }

        @Override
        public String toString() {
            String filter = archetypes.isEmpty() ? "" : " (" + archetypes.stream().sorted().collect(Collectors.joining(" or ")) + ")";
            return (direction == Direction.IN ? "<-" + verb + "-" : "-" + verb + "->") + filter;
        }
    }

    /** A named group of entities reached from the root by one or two steps. */
    public record Section(String key, String label, String description, List<Step> steps) {
    }

    private final String label;
    private final String description;
    private final Set<String> roots;
    private final List<Section> sections;

    Composite(String label, String description, Set<String> roots, Section... sections) {
        this.label = label;
        this.description = description;
        this.roots = roots;
        this.sections = List.of(sections);
    }

    /** Lower-case, dash-separated name used in URLs, such as {@code magic-system}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public Set<String> roots() {
        return roots;
    }

    public List<Section> sections() {
        return sections;
    }

    /** Matches {@code culture}, {@code Magic System}, {@code magic_system} and {@code MAGIC-SYSTEM}. */
    public static Optional<Composite> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String key = name.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_]+", "-");
        return Arrays.stream(values()).filter(composite -> composite.key().equals(key)).findFirst();
    }

    public static Composite parse(String name) {
        return find(name).orElseThrow(() -> new UnknownCompositeException(name));
    }

    public static String allowed() {
        return Arrays.stream(values()).map(Composite::key).collect(Collectors.joining(", "));
    }

    private static Set<String> roots(String... archetypes) {
        return Set.of(archetypes);
    }

    private static Section section(String key, String label, String description, Step... steps) {
        if (steps.length < 1 || steps.length > 2) {
            throw new IllegalArgumentException("A composite section has one or two steps: " + key);
        }
        return new Section(key, label, description, List.of(steps));
    }
}
