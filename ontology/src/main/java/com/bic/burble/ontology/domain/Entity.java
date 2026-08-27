package com.bic.burble.ontology.domain;

import com.bic.burble.ontology.domain.facet.ArchetypeFacet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record Entity(
    String guid,
    String name,
    List<String> aliases,
    String description,
    Set<ArchetypeFacet> facets) {
  public boolean hasFacet(Class<? extends ArchetypeFacet> type) {
    return facets.stream().anyMatch(type::isInstance);
  }

  public <T extends ArchetypeFacet> Optional<T> getFacet(Class<T> type) {
    return facets.stream().filter(type::isInstance).map(type::cast).findFirst();
  }
}
