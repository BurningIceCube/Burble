package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.domain.facet.PeriodFacet;

import java.util.ArrayList;
import java.util.HashMap;
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
    static final String PERIOD = "Period";
    static final String START_TICK = "startTick";
    static final String END_TICK = "endTick";

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
            } else if (facet instanceof PeriodFacet period) {
                Map<String, Object> properties = new HashMap<>();
                if (period.startTick() != null) {
                    properties.put(START_TICK, period.startTick());
                }
                if (period.endTick() != null) {
                    properties.put(END_TICK, period.endTick());
                }
                statements.add(new StoredStatement(IS_A, PERIOD, properties));
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
            ArchetypeFacet facet;
            if (ITEM.equals(statement.object())) {
                facet = new ItemFacet(
                        bool(statement.properties(), "portable"),
                        bool(statement.properties(), "unique"));
            } else if (PERIOD.equals(statement.object())) {
                facet = new PeriodFacet(
                        tick(statement.properties(), START_TICK),
                        tick(statement.properties(), END_TICK));
            } else {
                facet = ArchetypeFacets.of(statement.object(), null, null);
            }
            if (facet != null) {
                facets.add(facet);
            }
        }
        return facets;
    }

    static Long tick(Map<String, Object> properties, String key) {
        if (properties == null) {
            return null;
        }
        Object value = properties.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
