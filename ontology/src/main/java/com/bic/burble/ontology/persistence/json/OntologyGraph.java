package com.bic.burble.ontology.persistence.json;

import java.util.ArrayList;
import java.util.List;

/**
 * One ontology JSON document.
 *
 * <pre>
 * {
 *   "worlds": [
 *     { "guid", "name", "description", "owner", "createdAt", "updatedAt" }
 *   ],
 *   "entities": [
 *     {
 *       "guid", "worldId", "name", "aliases", "description",
 *       "statements": [
 *         { "verb": "IS_A", "object": "Item", "properties": { "portable": true, "unique": false } },
 *         { "verb": "IS_A", "object": "Character" }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * Worlds contain entities by {@code worldId}. An entity has no type field.
 * Each archetype is an {@code IS_A} statement on that entity, so one entity
 * can be an Item and a Character at the same time. Other statement verbs, if
 * present, sit in the same {@code statements} list.
 */
public record OntologyGraph(List<StoredWorld> worlds, List<StoredEntity> entities) {

    public OntologyGraph {
        worlds = mutable(worlds);
        entities = mutable(entities);
    }

    public static OntologyGraph empty() {
        return new OntologyGraph(List.of(), List.of());
    }

    private static <T> List<T> mutable(List<T> values) {
        return new ArrayList<>(values == null ? List.of() : values);
    }
}
