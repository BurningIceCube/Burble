# Composites

Composites are user-facing concepts that sit on top of the formal ontology.  
They are not stored types and cannot be created, updated or deleted. A composite is a read-only query over formal archetypes and their [relationships](relationships.md): for example, a Religion is a Worldview together with the Organizations, Concepts and Rules connected to it.

## Core Composites

| Composite      | Primary Archetype(s)              | Common Supporting Archetypes              |
|----------------|-----------------------------------|-------------------------------------------|
| **Culture**    | Worldview                         | Organization, Species, Concept, Rule      |
| **Religion**   | Worldview                         | Organization, Concept, Rule, Character    |
| **Economy**    | System                            | Rule, Substance, Title, Organization      |
| **Ecology**    | Location + Species                | Substance, System                         |
| **Technology** | System                            | Item, Action, Logic, Substance            |
| **Magic System**| System                           | Logic, Action, Substance, Rule            |
| **Language**   | System or Concept                 | Worldview, Concept                        |
| **Government** | Organization + System             | Title, Rule, Worldview                    |
| **Cosmology**  | Location + Logic + Worldview      | Concept, Period                           |

Composites exist so that users can work with familiar concepts while the system maintains a clean and rigorous underlying model.

## Querying a composite

Composites are read-only. There is nothing to create, update or delete, and no composite type is stored on entities. Every request works the view out from the archetypes and relationships that are there at the time, on either store (JSON or Neo4j).

All paths are under `/api/v1/ontology/world/{worldId}/composite`:

| Request | What you get |
|---------|--------------|
| `GET /composite` | Every composite: name, label, a plain-language description, the archetypes it can start from, and its sections |
| `GET /composite/{name}?atTick=N` | Entities in the world it can start from (those with a root archetype, at `atTick` if given), so you can pick one without knowing ids |
| `GET /composite/{name}/{rootEntityId}?atTick=N` | The view: the root, then one section per row in the table below |

- Names ignore case, spaces, dashes and underscores: `culture`, `Magic System`, `magic-system` and `MAGIC_SYSTEM` all work.
- An unknown name returns 400 with the allowed names.
- A root without one of the root archetypes returns 400 saying which archetypes are needed.
- A root that isn't in the world returns 404.
- Each entity in a section has its `id`, `name`, `archetypes` (the ones active at the tick), the `certainty` of the statement that linked it in, and `via`: the statements followed from the root, as `{subject, verb, object}`.
- An entity reached by several paths is listed once, with the most certain link.
- **With `atTick`:** only relationships and archetypes that hold at that tick count, including the root's.
- **Without `atTick`:** every statement counts, whatever its time.

## How each composite is built

Each composite is one declaration in `Composite` in the ontology module. A section follows one or two relationship steps from the root:
- `<-VERB- (A)` means entities that store VERB pointing here and are an A.
- `-VERB->` means what this entity stores VERB pointing at.

A section that doesn't fit the root is just empty. For example, Ecology's species sections are empty on a Species root.

| Composite | Root | Section | Path from the root |
|-----------|------|---------|--------------------|
| **Culture** | Worldview | communities | `<-BELIEVES- (Organization)` |
| | | people | `<-BELIEVES- (Character)` |
| | | species | `<-BELIEVES- (Character) -OF_SPECIES->` |
| | | ideas | `<-COMPONENT_OF- (Concept)` |
| | | customs | `<-COMPONENT_OF- (Rule)` |
| | | traits | `-HAS_TRAIT->` |
| **Religion** | Worldview | followers | `<-BELIEVES- (Character)` |
| | | institutions | `<-BELIEVES- (Organization)` |
| | | clergy | `<-BELIEVES- (Organization) <-MEMBER_OF- (Character)` |
| | | doctrines | `<-COMPONENT_OF- (Concept)` |
| | | commandments | `<-COMPONENT_OF- (Rule)` |
| | | lawgivers | `<-COMPONENT_OF- (Rule) -ENACTED_BY->` |
| **Economy** | System | rules | `<-COMPONENT_OF- (Rule)` |
| | | participants | `<-GOVERNED_BY- (Organization)` |
| | | regions | `<-GOVERNED_BY- (Location)` |
| | | resources | `<-GOVERNED_BY- (Location) -MADE_OF->` |
| | | titles | `<-GOVERNED_BY- (Organization) <-HELD_BY- (Title)` |
| **Ecology** | Location or Species | inhabitants | `<-LOCATED_IN- (Character)` |
| | | species | `<-LOCATED_IN- (Character) -OF_SPECIES->` |
| | | materials | `-MADE_OF->` |
| | | systems | `-GOVERNED_BY-> (System)` |
| | | subregions | `<-PART_OF- (Location)` |
| | | members | `<-OF_SPECIES-` |
| | | habitats | `<-OF_SPECIES- -LOCATED_IN->` |
| **Technology** | System | techniques | `<-COMPONENT_OF- (Action)` |
| | | principles | `<-COMPONENT_OF- (Logic)` |
| | | devices | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Item)` |
| | | practitioners | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Character)` |
| **Magic System** | System | laws | `<-COMPONENT_OF- (Logic)` |
| | | spells | `<-COMPONENT_OF- (Action)` |
| | | rules | `<-COMPONENT_OF- (Rule)` |
| | | casters | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Character)` |
| | | gifted-species | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Species)` |
| | | artifacts | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Item)` |
| **Language** | System or Concept | vocabulary | `<-COMPONENT_OF- (Concept)` |
| | | grammar | `<-COMPONENT_OF- (Logic or Rule)` |
| | | speakers | `<-KNOWS- (Character)` |
| | | worldviews | `-COMPONENT_OF-> (Worldview)` |
| **Government** | Organization or System | officials | `<-MEMBER_OF- (Character)` |
| | | offices | `<-HELD_BY- (Title)` |
| | | office-holders | `<-OWNED_BY- (Title) -HELD_BY->` |
| | | laws | `<-ENACTED_BY- (Rule)` |
| | | system-rules | `<-COMPONENT_OF- (Rule)` |
| | | governed-by | `-GOVERNED_BY->` |
| | | governs | `<-GOVERNED_BY-` |
| | | territory | `<-OWNED_BY- (Location)` |
| | | ideology | `-BELIEVES->` |
| | | branches | `<-PART_OF- (Organization)` |
| **Cosmology** | Location, Logic or Worldview | realms | `<-PART_OF- (Location)` |
| | | within | `-PART_OF-> (Location)` |
| | | ages | `<-LOCATED_IN- (Event) -DURING->` |
| | | laws | `<-COMPONENT_OF- (Logic)` |
| | | ideas | `<-COMPONENT_OF- (Concept)` |
| | | frameworks | `-COMPONENT_OF->` |

### Gaps

Some supporting archetypes in the first table aren't directly linked by any of the current [relationship types](relationships.md). They are either reached indirectly or not at all:

- **Economy, Substance:** only through the regions it governs (`MADE_OF`). Nothing links a System or Organization to the goods it trades or produces.
- **Ecology, Species and Location:** linked only through characters (a character `LOCATED_IN` a place and `OF_SPECIES` a species). `LOCATED_IN` doesn't allow a Species subject, so a species can't have a habitat of its own.
- **Technology and Magic System, Substance:** no relationship links a System, Action or Item to the materials or reagents it uses. The path Action → Item → `MADE_OF` would take three steps, and sections allow at most two.
- **Language:** there is no "speaks" relationship. Speakers are characters who `KNOWS` the language, which only works when the root is a Concept. A System can't be `COMPONENT_OF` a Worldview, so the worldviews section only works on a Concept root.
- **Cosmology, Period:** only through events located in a place (`LOCATED_IN` then `DURING`). Nothing links a Location or Worldview directly to a Period.
