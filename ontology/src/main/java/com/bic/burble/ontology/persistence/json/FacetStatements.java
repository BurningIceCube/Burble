package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps the domain archetype facets onto {@code IS_A} statements and back.
 * {@code IS_A} is just a statement verb; it is not a type field on the entity.
 */
final class FacetStatements {

    static final String IS_A = "IS_A";
    static final String ITEM = "Item";
    static final String CHARACTER = "Character";

    private FacetStatements() {
    }

    static List<StoredStatement> fromFacets(Set<ArchetypeFacet> facets) {
        List<StoredStatement> statements = new ArrayList<>();
        if (facets == null || facets.isEmpty()) {
            return statements;
        }
        ItemFacet item = null;
        boolean character = false;
        for (ArchetypeFacet facet : facets) {
            if (facet instanceof ItemFacet itemFacet) {
                item = itemFacet;
            } else if (facet instanceof CharacterFacet) {
                character = true;
            }
        }
        if (item != null) {
            statements.add(new StoredStatement(IS_A, ITEM, Map.of(
                    "portable", item.portable(),
                    "unique", item.unique()
            )));
        }
        if (character) {
            statements.add(new StoredStatement(IS_A, CHARACTER, Map.of()));
        }
        return statements;
    }

    static Set<ArchetypeFacet> toFacets(List<StoredStatement> statements) {
        if (statements == null || statements.isEmpty()) {
            return Set.of();
        }
        Set<ArchetypeFacet> facets = new HashSet<>();
        ItemFacet item = null;
        boolean character = false;
        for (StoredStatement statement : statements) {
            if (statement == null || !IS_A.equals(statement.verb())) {
                continue;
            }
            if (ITEM.equals(statement.object())) {
                item = new ItemFacet(
                        bool(statement.properties(), "portable"),
                        bool(statement.properties(), "unique"));
            } else if (CHARACTER.equals(statement.object())) {
                character = true;
            }
        }
        if (item != null) {
            facets.add(item);
        }
        if (character) {
            facets.add(new CharacterFacet());
        }
        return facets;
    }

    private static boolean bool(Map<String, Object> properties, String key) {
        if (properties == null) {
            return false;
        }
        Object value = properties.get(key);
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }
}
