package com.bic.burble.ontology.domain;

import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.ArchetypeFacets;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeBound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * An entity. {@code facets} holds one facet per archetype the entity ever
 * has, with that archetype's own properties (item flags, period ticks).
 * {@code archetypes} holds the timed {@code IS_A} statements. The two are
 * kept in step: an archetype statement with no facet gets a default facet,
 * and a facet with no statement gets an always-true statement, so code that
 * only sets facets keeps working.
 */
public record Entity(
    String guid,
    String worldId,
    String name,
    List<String> aliases,
    String description,
    Set<ArchetypeFacet> facets,
    List<ArchetypeStatement> archetypes) {

  private static final Comparator<ArchetypeStatement> ORDER = Comparator
      .comparing(ArchetypeStatement::archetype)
      .thenComparing(statement -> startTick(statement.time()), Comparator.nullsFirst(Comparator.naturalOrder()));

  public Entity {
    Set<ArchetypeFacet> facetSet = new HashSet<>(facets == null ? Set.of() : facets);
    List<ArchetypeStatement> statements = new ArrayList<>(archetypes == null ? List.of() : archetypes);
    Set<String> facetNames = new HashSet<>();
    facetSet.forEach(facet -> facetNames.add(ArchetypeFacets.nameOf(facet)));
    for (ArchetypeStatement statement : statements) {
      if (statement.archetype() != null && !facetNames.contains(statement.archetype())) {
        ArchetypeFacet facet = ArchetypeFacets.of(statement.archetype(), null, null);
        if (facet != null) {
          facetSet.add(facet);
          facetNames.add(statement.archetype());
        }
      }
    }
    for (String facetName : facetNames) {
      if (statements.stream().noneMatch(statement -> facetName.equals(statement.archetype()))) {
        statements.add(ArchetypeStatement.always(facetName));
      }
    }
    statements.removeIf(statement -> !facetNames.contains(statement.archetype()));
    statements.sort(ORDER);
    facets = Set.copyOf(facetSet);
    archetypes = List.copyOf(statements);
  }

  public Entity(String guid, String worldId, String name, List<String> aliases, String description,
                Set<ArchetypeFacet> facets) {
    this(guid, worldId, name, aliases, description, facets, null);
  }

  public boolean hasFacet(Class<? extends ArchetypeFacet> type) {
    return facets.stream().anyMatch(type::isInstance);
  }

  public <T extends ArchetypeFacet> Optional<T> getFacet(Class<T> type) {
    return facets.stream().filter(type::isInstance).map(type::cast).findFirst();
  }

  private static Long startTick(StatementTime time) {
    TimeBound start = time == null ? null : time.start();
    return start == null ? null : start.tick();
  }
}
