package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.composite.Composite;
import com.bic.burble.ontology.domain.composite.CompositeView;
import com.bic.burble.ontology.domain.composite.CompositeView.EntitySummary;
import com.bic.burble.ontology.domain.composite.CompositeView.Hop;
import com.bic.burble.ontology.domain.composite.CompositeView.SectionView;
import com.bic.burble.ontology.domain.composite.UnknownCompositeException;
import com.bic.burble.ontology.domain.entity.ArchetypeRequest;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.service.impl.DefaultCompositeService;
import com.bic.burble.ontology.service.impl.DefaultEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Composite queries over a small world, run once per store. Composites are
 * read-only, so the same world is built with ordinary entity and
 * relationship calls.
 */
abstract class CompositeContract {

    protected static final String WORLD = "w";

    protected DefaultEntityService entities;
    protected DefaultCompositeService composites;

    protected String faith, guild, ann, bob, elf, tithe, prayer, town, magic, fireball, staff;
    protected String common, mana, iron, forest, firstAge;

    protected abstract EntityRepository freshRepository();

    @BeforeEach
    void buildWorld() {
        entities = new DefaultEntityService(freshRepository());
        composites = new DefaultCompositeService(entities);

        faith = create("Faith", ArchetypeRequest.of("Worldview"));
        guild = create("Guild", ArchetypeRequest.of("Organization"));
        ann = create("Ann", ArchetypeRequest.of("Character"));
        bob = create("Bob", new ArchetypeRequest("Character", StatementTime.from(50), null));
        elf = create("Elf", ArchetypeRequest.of("Species"));
        tithe = create("Tithe", ArchetypeRequest.of("Rule"));
        prayer = create("Prayer", ArchetypeRequest.of("Concept"));
        town = create("Town", ArchetypeRequest.of("Location"));
        magic = create("Arcana", ArchetypeRequest.of("System"));
        fireball = create("Fireball", ArchetypeRequest.of("Action"));
        staff = create("Staff", ArchetypeRequest.of("Item"));

        link(guild, "BELIEVES", faith, null, 0.8);
        link(ann, "BELIEVES", faith, StatementTime.between(0, 100), null);
        link(bob, "BELIEVES", faith, StatementTime.from(50), 0.6);
        link(ann, "MEMBER_OF", guild, null, null);
        link(ann, "OF_SPECIES", elf, null, null);
        link(ann, "LOCATED_IN", town, null, null);
        link(tithe, "COMPONENT_OF", faith, null, null);
        link(tithe, "ENACTED_BY", guild, null, null);
        link(prayer, "COMPONENT_OF", faith, null, 0.9);
        link(fireball, "COMPONENT_OF", magic, null, null);
        link(ann, "CAN_PERFORM", fireball, null, null);
        link(staff, "CAN_PERFORM", fireball, null, null);

        // Relationship types that close the composite gaps.
        common = create("Common", ArchetypeRequest.of("System"));
        mana = create("Mana", ArchetypeRequest.of("Substance"));
        iron = create("Iron", ArchetypeRequest.of("Substance"));
        forest = create("Forest", ArchetypeRequest.of("Location"));
        firstAge = create("First Age", ArchetypeRequest.of("Period"));
        link(ann, "SPEAKS", common, null, null);
        link(elf, "SPEAKS", common, null, 0.7);
        link(guild, "SPEAKS", common, StatementTime.from(200), null);
        link(elf, "INHABITS", forest, null, null);
        link(magic, "USES", iron, null, null);
        link(fireball, "USES", mana, null, null);
        link(forest, "EXISTS_DURING", firstAge, null, null);
    }

    private String create(String name, ArchetypeRequest archetype) {
        return entities.createEntity(WORLD, new CreateEntityRequest(name, null, null, null, null, List.of(archetype)));
    }

    private void link(String subject, String verb, String object, StatementTime time, Double certainty) {
        entities.addRelationship(WORLD, subject, verb, object, time, certainty).orElseThrow();
    }

    private CompositeView view(String name, String root, Long atTick) {
        return composites.query(WORLD, name, root, atTick).orElseThrow();
    }

    private static SectionView section(CompositeView view, String key) {
        return view.sections().stream().filter(section -> section.key().equals(key)).findFirst().orElseThrow();
    }

    private static List<String> names(CompositeView view, String key) {
        return section(view, key).entities().stream().map(EntitySummary::name).toList();
    }

    @Test
    void religionGroupsFollowersInstitutionsClergyAndTeachings() {
        CompositeView religion = view("religion", faith, null);
        assertThat(religion.composite()).isEqualTo("religion");
        assertThat(religion.root().name()).isEqualTo("Faith");
        assertThat(religion.root().archetypes()).containsExactly("Worldview");
        assertThat(names(religion, "followers")).containsExactly("Ann", "Bob");
        assertThat(names(religion, "institutions")).containsExactly("Guild");
        assertThat(names(religion, "clergy")).containsExactly("Ann");
        assertThat(names(religion, "doctrines")).containsExactly("Prayer");
        assertThat(names(religion, "commandments")).containsExactly("Tithe");
        assertThat(names(religion, "lawgivers")).containsExactly("Guild");

        EntitySummary institution = section(religion, "institutions").entities().get(0);
        assertThat(institution.certainty()).isEqualTo(0.8);
        assertThat(institution.archetypes()).containsExactly("Organization");
        assertThat(institution.via()).containsExactly(new Hop(guild, "BELIEVES", faith));

        EntitySummary clergy = section(religion, "clergy").entities().get(0);
        assertThat(clergy.via()).containsExactly(new Hop(guild, "BELIEVES", faith), new Hop(ann, "MEMBER_OF", guild));
        assertThat(section(religion, "clergy").path()).isEqualTo("<-BELIEVES- (Organization) <-MEMBER_OF- (Character)");
    }

    @Test
    void cultureReachesSpeciesInTwoSteps() {
        CompositeView culture = view("Culture", faith, null);
        assertThat(names(culture, "species")).containsExactly("Elf");
        assertThat(names(culture, "communities")).containsExactly("Guild");
        assertThat(names(culture, "ideas")).containsExactly("Prayer");
        assertThat(section(culture, "ideas").entities().get(0).certainty()).isEqualTo(0.9);
        assertThat(names(culture, "traits")).isEmpty();
    }

    @Test
    void atTickUsesTimedStatementsAndTimedArchetypes() {
        // Ann believes during [0, 100); Bob is a Character and believes from 50.
        CompositeView early = view("religion", faith, 10L);
        assertThat(early.atTick()).isEqualTo(10L);
        assertThat(names(early, "followers")).containsExactly("Ann");
        assertThat(names(view("religion", faith, 60L), "followers")).containsExactly("Ann", "Bob");
        CompositeView late = view("religion", faith, 150L);
        assertThat(names(late, "followers")).containsExactly("Bob");
        assertThat(section(late, "followers").entities().get(0).certainty()).isEqualTo(0.6);
        // Guild's belief and Ann's membership are untimed, so she stays clergy.
        assertThat(names(late, "clergy")).containsExactly("Ann");
        // Archetypes shown are the ones active at the tick: Bob is a Character only from 50.
        assertThat(section(view("religion", faith, 60L), "followers").entities())
                .filteredOn(entity -> entity.name().equals("Bob"))
                .singleElement().satisfies(entity -> assertThat(entity.archetypes()).containsExactly("Character"));
        assertThat(composites.findRoots(WORLD, "ecology", null)).extracting(EntitySummary::name)
                .containsExactly("Elf", "Forest", "Town");
    }

    @Test
    void otherCompositesUseTheirOwnTraversals() {
        assertThat(names(view("government", guild, null), "officials")).containsExactly("Ann");
        assertThat(names(view("government", guild, null), "laws")).containsExactly("Tithe");
        assertThat(names(view("government", guild, null), "ideology")).containsExactly("Faith");
        assertThat(names(view("ecology", town, null), "inhabitants")).containsExactly("Ann");
        assertThat(names(view("ecology", town, null), "visiting-species")).containsExactly("Elf");
        assertThat(names(view("ecology", elf, null), "member-locations")).containsExactly("Town");
        CompositeView arcana = view("Magic System", magic, null);
        assertThat(names(arcana, "spells")).containsExactly("Fireball");
        assertThat(names(arcana, "casters")).containsExactly("Ann");
        assertThat(names(arcana, "artifacts")).containsExactly("Staff");
        assertThat(names(view("technology", magic, null), "devices")).containsExactly("Staff");
    }

    @Test
    void newRelationshipTypesFillTheFormerGaps() {
        CompositeView language = view("language", common, null);
        assertThat(names(language, "speakers")).containsExactly("Ann", "Elf", "Guild");
        assertThat(section(language, "speakers").entities()).filteredOn(e -> e.name().equals("Elf"))
                .singleElement().satisfies(e -> assertThat(e.certainty()).isEqualTo(0.7));
        assertThat(names(view("language", common, 100L), "speakers")).containsExactly("Ann", "Elf");

        assertThat(names(view("ecology", forest, null), "species")).containsExactly("Elf");
        assertThat(names(view("ecology", elf, null), "habitats")).containsExactly("Forest");

        CompositeView arcana = view("magic-system", magic, null);
        assertThat(names(arcana, "reagents")).containsExactly("Iron");
        assertThat(names(arcana, "spell-reagents")).containsExactly("Mana");
        assertThat(section(arcana, "spell-reagents").entities().get(0).via())
                .containsExactly(new Hop(fireball, "COMPONENT_OF", magic), new Hop(fireball, "USES", mana));
        assertThat(names(view("technology", magic, null), "materials")).containsExactly("Iron");
        assertThat(names(view("economy", magic, null), "resources")).containsExactly("Iron");

        assertThat(names(view("cosmology", forest, null), "ages")).containsExactly("First Age");
    }

    @Test
    void discoveryListsRootsByArchetype() {
        assertThat(composites.findRoots(WORLD, "religion", null)).extracting(EntitySummary::name).containsExactly("Faith");
        assertThat(composites.findRoots(WORLD, "magic_system", null)).extracting(EntitySummary::name)
                .containsExactly("Arcana", "Common");
        assertThat(composites.findRoots(WORLD, "government", null)).extracting(EntitySummary::name)
                .containsExactly("Arcana", "Common", "Guild");
        assertThat(composites.findRoots("other", "religion", null)).isEmpty();
        assertThat(composites.listComposites()).extracting(info -> info.name()).containsExactly(
                "culture", "religion", "economy", "ecology", "technology", "magic-system", "language", "government", "cosmology");
    }

    @Test
    void wrongRootUnknownNameAndMissingRoot() {
        assertThatThrownBy(() -> composites.query(WORLD, "religion", guild, null))
                .isInstanceOf(InvalidStatementException.class)
                .hasMessageContaining("A Religion starts at an entity that is Worldview, but Guild is Organization");
        assertThatThrownBy(() -> composites.query(WORLD, "economy", bob, 10L))
                .hasMessageContaining("Bob is no archetype at tick 10");
        assertThatThrownBy(() -> composites.query(WORLD, "folklore", faith, null))
                .isInstanceOf(UnknownCompositeException.class)
                .hasMessageContaining("Allowed values: culture, religion");
        assertThat(composites.query(WORLD, "religion", "missing", null)).isEmpty();
        assertThat(composites.query("other", "religion", faith, null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"culture,CULTURE", "' Magic System ',MAGIC_SYSTEM", "magic-system,MAGIC_SYSTEM", "Magic_System,MAGIC_SYSTEM",
            "COSMOLOGY,COSMOLOGY"})
    void namesIgnoreCaseSpacesAndDashes(String name, Composite expected) {
        assertThat(Composite.parse(name)).isEqualTo(expected);
    }

    @Test
    void everyDeclarationIsWellFormed() {
        for (Composite composite : Composite.values()) {
            assertThat(composite.roots()).isNotEmpty();
            assertThat(composite.sections()).isNotEmpty();
            assertThat(composite.sections().stream().map(s -> s.key()).distinct().count())
                    .isEqualTo(composite.sections().size());
            composite.sections().forEach(section -> assertThat(section.steps()).hasSizeBetween(1, 2));
        }
        assertThat(Arrays.stream(Composite.values()).map(Composite::label))
                .containsExactly("Culture", "Religion", "Economy", "Ecology", "Technology", "Magic System", "Language",
                        "Government", "Cosmology");
    }
}
