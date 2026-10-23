package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.composite.CompositeView;
import com.bic.burble.ontology.domain.composite.CompositeView.CompositeInfo;
import com.bic.burble.ontology.domain.composite.CompositeView.EntitySummary;

import java.util.List;
import java.util.Optional;

/**
 * Read-only composite queries. Composites are never stored; every call
 * computes the view from archetypes and relationships.
 */
public interface CompositeService {

    /** Every composite with its root archetypes and sections. */
    List<CompositeInfo> listComposites();

    /**
     * Entities in the world that can be the root of {@code name}: those with
     * one of its root archetypes (active at {@code atTick} when given).
     *
     * @throws com.bic.burble.ontology.domain.composite.UnknownCompositeException for an unknown name
     */
    List<EntitySummary> findRoots(String worldId, String name, Long atTick);

    /**
     * The composite rooted at {@code rootId}. Empty when the root is not in
     * the world.
     *
     * @throws com.bic.burble.ontology.domain.composite.UnknownCompositeException for an unknown name
     * @throws com.bic.burble.ontology.domain.statement.InvalidStatementException when the root
     *         does not have one of the composite's root archetypes
     */
    Optional<CompositeView> query(String worldId, String name, String rootId, Long atTick);
}
