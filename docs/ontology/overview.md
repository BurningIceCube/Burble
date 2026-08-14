# Ontology Overview

The Burble Ontology models **world state**: what exists, what is true, and when it was true.

Narrative structures (plots, arcs, scenes) belong in the separate Story layer.

## Two-Layer Design

1. **Formal Archetypes**  
   The real, stored types. Used for persistence, querying, relationships, and AI classification.

2. **Composites**  
   User-facing concepts (Culture, Economy, Ecology, etc.).  
   These are facades that map onto one or more formal archetypes.

This separation keeps the core model clean while matching how worldbuilders naturally think.

## Design Principles

- Nodes are nouns; statements are verbs
- An entity may have multiple archetypes
- Entities can change archetypes over time
- Time lives on statements, not on entities
- Composites are abstractions over the formal model