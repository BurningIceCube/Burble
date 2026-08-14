# Services Overview

Burble is split into focused services:

## Ontology Service
Authoritative source of world definition and long-lived truth.  
Owns formal archetypes, statements, and composite mappings.

## Story Service
Narrative structure (campaigns, sessions, scenes, arcs).  
References ontology entities but does not own world truth.

## State Service
Live mechanical and situational state (HP, inventory, conditions, positions, etc.).  
Shields the Ontology service from high-frequency updates.  
Promotes significant changes back to the Ontology at save points.

## TTRPG / Game Service(s)
Rules, resolution, and play experience.  
Primarily reads and writes to the State service.