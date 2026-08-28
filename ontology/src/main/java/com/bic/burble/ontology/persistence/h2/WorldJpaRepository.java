package com.bic.burble.ontology.persistence.h2;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldJpaRepository extends JpaRepository<WorldEntity, String> {
}
