# Composites

Composites are user-facing concepts that sit on top of the formal ontology.  
They have real API endpoints and can be used as if they were first-class, but internally they map to formal archetypes.

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