# Formal Archetypes

These are the only first-class types in the ontology.

## Concrete

| Archetype   | Description |
|-------------|-------------|
| **Character** | Named cognizant entity (can want, know, decide) |
| **Item**      | Non-cognizant thing that can be moved as a whole |
| **Location**  | Fixed spatial extent at any scale |
| **Substance** | Material or elemental composition |

## Collective

| Archetype      | Description |
|----------------|-------------|
| **Organization** | Collective treated as a single unit |
| **Species**      | Inherent category of being |
| **Lineage**      | Specific hereditary line |

## Temporal

| Archetype   | Description |
|-------------|-------------|
| **Event**   | Occurrence with a before and after |
| **Period**  | Temporal extent that contains events |
| **Instance**| Single point in time |

## Attributive

| Archetype  | Description |
|------------|-------------|
| **Action** | Reusable capability to do something |
| **Trait**  | Passive capacity, condition, or status |
| **Title**  | Socially recognized standing that can be transferred |

## Abstract & Structural

| Archetype    | Description |
|--------------|-------------|
| **System**   | Framework of interacting rules and structures |
| **Worldview**| Religion, philosophy, ideology, or belief system |
| **Concept**  | Information that can be known, shared, or kept secret |
| **Logic**    | Definitional fact that constrains what is possible |
| **Rule**     | Constraint that exists by decree or agreement (can be broken) |
## Archetypes over time

An entity's archetypes are `IS_A` statements. Each one can have a time range and a certainty, the same as a relationship: `time` has an optional `start` and `end`, each a `tick`, a `period` (id of a Period entity), or both. The range includes its start and stops just before its end. Certainty is 0 to 1 and defaults to 1. A statement with no time holds always.

- The same archetype can appear more than once on one entity, as long as the times do not overlap. An overlapping one is rejected with HTTP 409. Adding the same archetype with exactly the same time again only updates its certainty.
- For a Period entity, the time on its `IS_A Period` statement is when the entity is a Period. It is separate from the period's own `startTick`/`endTick`, which is what other statements use when they name the period as a time bound.

API (under `/api/v1/ontology/world/{worldId}/entity`):

- `POST` and `PUT` take `archetypes` as a list. Each entry is either a name, `"Character"`, which works exactly as before, or an object, `{"archetype": "Character", "time": {"start": {"tick": 10}}, "certainty": 0.9}`. On update, a plain name keeps the times the entity already has for that archetype.
- `GET {entityId}/archetype?atTick=15` lists the `IS_A` statements, only those that hold at `atTick` when it is given.
- `POST {entityId}/archetype` with one `{"archetype", "time", "certainty"}` adds one statement (201).
- `DELETE {entityId}/archetype/{archetype}?startTick=&startPeriod=&endTick=&endPeriod=` removes the statement with exactly that time. Leave out the time to remove the untimed one (204, or 404 when there is no such statement).
- Entities returned by the API include `archetypes`, the timed statements, next to `facets`.

Relationships check that the allowed archetypes hold for the whole relationship time; see [relationships](relationships.md#archetypes-over-time).
