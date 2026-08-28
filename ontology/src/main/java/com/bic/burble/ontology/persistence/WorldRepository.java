package com.bic.burble.ontology.persistence;

import com.bic.burble.ontology.domain.world.WorldRecord;

import java.util.List;
import java.util.Optional;

public interface WorldRepository {
    WorldRecord save(WorldRecord world);

    Optional<WorldRecord> findById(String id);

    List<WorldRecord> findAll();

    boolean deleteById(String id);
}
