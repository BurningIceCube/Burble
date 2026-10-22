# Composites

Composites are user-facing concepts that sit on top of the formal ontology.  
They are not stored types and do not have their own API endpoints. A composite is a query over formal archetypes and their [relationships](relationships.md): for example, a Religion is a Worldview together with the Organizations, Concepts and Rules connected to it.

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