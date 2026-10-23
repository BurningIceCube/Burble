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
| | | resources | `-USES->` |
| | | regional-resources | `<-GOVERNED_BY- (Location) -MADE_OF->` |
| | | titles | `<-GOVERNED_BY- (Organization) <-HELD_BY- (Title)` |
| **Ecology** | Location or Species | inhabitants | `<-LOCATED_IN- (Character)` |
| | | species | `<-INHABITS-` |
| | | visiting-species | `<-LOCATED_IN- (Character) -OF_SPECIES->` |
| | | materials | `-MADE_OF->` |
| | | systems | `-GOVERNED_BY-> (System)` |
| | | subregions | `<-PART_OF- (Location)` |
| | | members | `<-OF_SPECIES-` |
| | | habitats | `-INHABITS->` |
| | | member-locations | `<-OF_SPECIES- -LOCATED_IN->` |
| **Technology** | System | techniques | `<-COMPONENT_OF- (Action)` |
| | | principles | `<-COMPONENT_OF- (Logic)` |
| | | devices | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Item)` |
| | | practitioners | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Character)` |
| | | materials | `-USES->` |
| | | technique-materials | `<-COMPONENT_OF- (Action) -USES->` |
| **Magic System** | System | laws | `<-COMPONENT_OF- (Logic)` |
| | | spells | `<-COMPONENT_OF- (Action)` |
| | | rules | `<-COMPONENT_OF- (Rule)` |
| | | casters | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Character)` |
| | | gifted-species | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Species)` |
| | | artifacts | `<-COMPONENT_OF- (Action) <-CAN_PERFORM- (Item)` |
| | | reagents | `-USES->` |
| | | spell-reagents | `<-COMPONENT_OF- (Action) -USES->` |
| **Language** | System or Concept | vocabulary | `<-COMPONENT_OF- (Concept)` |
| | | grammar | `<-COMPONENT_OF- (Logic or Rule)` |
| | | speakers | `<-SPEAKS-` |
| | | knowers | `<-KNOWS- (Character)` |
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
| | | ages | `-EXISTS_DURING->` |
| | | event-ages | `<-LOCATED_IN- (Event) -DURING->` |
| | | laws | `<-COMPONENT_OF- (Logic)` |
| | | ideas | `<-COMPONENT_OF- (Concept)` |
| | | frameworks | `-COMPONENT_OF->` |

### Gaps

`SPEAKS`, `INHABITS`, `USES` and `EXISTS_DURING` were added so each supporting archetype in the first table can be reached directly:

- Language speakers, for either a System or a Concept root, use `SPEAKS`.
- Ecology links species and places with `INHABITS`.
- Economy, Technology and Magic System reach Substance with `USES`.
- Cosmology reaches Period with `EXISTS_DURING`.

The older indirect paths are kept as extra sections, such as the species of the characters located in a place, or the periods of events that happened there.

One gap is left. The Language worldviews section only works on a Concept root. `COMPONENT_OF` does not allow a System subject, and widening it would let any System be part of a Worldview, which is a bigger change to the catalog than this needs.
