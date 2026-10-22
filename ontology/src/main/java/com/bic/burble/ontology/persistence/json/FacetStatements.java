package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
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

    private FacetStatements() {
    }

    static List<StoredStatement> fromFacets(Set<ArchetypeFacet> facets) {
        List<StoredStatement> statements = new ArrayList<>();
        if (facets == null || facets.isEmpty()) {
            return statements;
        }
        for (ArchetypeFacet facet : facets) {
            if (facet instanceof ItemFacet item) {
                statements.add(new StoredStatement(IS_A, ITEM, Map.of(
                        "portable", item.portable(),
                        "unique", item.unique()
                )));
            } else {
                statements.add(new StoredStatement(IS_A, ArchetypeFacets.nameOf(facet), Map.of()));
            }
        }
        return statements;
    }

    static Set<ArchetypeFacet> toFacets(List<StoredStatement> statements) {
        if (statements == null || statements.isEmpty()) {
            return Set.of();
        }
        Set<ArchetypeFacet> facets = new HashSet<>();
        for (StoredStatement statement : statements) {
            if (statement == null || !IS_A.equals(statement.verb()) || statement.object() == null) {
                continue;
            }
            ArchetypeFacet facet = ITEM.equals(statement.object())
                    ? new ItemFacet(
                            bool(statement.properties(), "portable"),
                            bool(statement.properties(), "unique"))
                    : ArchetypeFacets.of(statement.object(), null, null);
            if (facet != null) {
                facets.add(facet);
            }
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
