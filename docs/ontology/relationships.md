# Relationships

Relationships connect two entities. The list of relationship types is fixed. It is declared in one place in the code, `RelationshipType` in the ontology module. `IS_A` is not a relationship type. Archetypes are set on the entity itself (see [archetypes](archetypes.md)).

## How relationships are stored

- **Stored once, on the subject.** `CARRIED_BY` is stored on the item and points at the character. The other direction ("what is this character carrying?") is a query, not a second statement.
- **Verbs ignore case.** `carried_by` and ` Carried_By ` both mean `CARRIED_BY`, and are always stored and returned upper case. An unknown verb is rejected and the error lists the allowed values.
- **Time and certainty.** Every relationship can have a time range and a certainty from 0 to 1. See the `time` and `certainty` fields on the relationship API.
- **Single or many.**
  - *Single*: a subject points at one object at a time. A second statement whose time overlaps an existing one is rejected (HTTP 409). An item can't be carried by two characters at once.
  - *Many*: any number at once, and overlapping times are fine. Repeating exactly the same statement (same object, same time) only updates its certainty.
- **No self-reference.** An entity can't point at itself.

## Which archetypes each type connects

A relationship is allowed when the subject has at least one of the allowed subject archetypes and the object has at least one of the allowed object archetypes. An entity with several archetypes can use any of them.

Types with several rows only allow those exact pairs. For example, `PART_OF` connects a Location to a Location or an Organization to an Organization, but not a Location to an Organization.

Archetypes count whenever the entity has them. Time on `IS_A` statements is not considered yet.

A relationship that breaks these rules is rejected with HTTP 400, and the message names the allowed archetypes.

| Type | Subject | Object | Cardinality |
|------|---------|--------|-------------|
| `CARRIED_BY` | Item | Character | single |
| `LOCATED_IN` | Character, Item, Organization, Event | Location | single |
| `PART_OF` | Location | Location | single |
| | Organization | Organization | |
| | Event | Event | |
| | Period | Period | |
| `OWNED_BY` | Item, Location, Title | Character, Organization | single |
| `MEMBER_OF` | Character | Organization | many |
| `OF_SPECIES` | Character | Species | single |
| `OF_LINEAGE` | Character | Lineage | single |
| `CHILD_OF` | Character | Character | many |
| `HELD_BY` | Title | Character, Organization | single |
| `DURING` | Event | Period | single |
| `PARTICIPATED_IN` | Character, Organization, Item | Event | many |
| `CAUSED_BY` | Event | Event | many |
| `MADE_OF` | Item, Location | Substance | many |
| `HAS_TRAIT` | any archetype | Trait | many |
| `CAN_PERFORM` | Character, Species, Item | Action | many |
| `BELIEVES` | Character, Organization | Worldview | many |
| `KNOWS` | Character | Concept | many (certainty is how sure the character is) |
| `GOVERNED_BY` | Location, Organization | Rule, System | many |
| `COMPONENT_OF` | Rule, Logic, Concept, Action | System, Worldview | many |
| `ENACTED_BY` | Rule | Organization, Character | single |

## Older data

These rules apply when a relationship is written. Statements that are already stored, including ones that break the table, still load unchanged.
