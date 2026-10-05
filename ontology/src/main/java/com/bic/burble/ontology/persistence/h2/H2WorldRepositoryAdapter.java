package com.bic.burble.ontology.persistence.h2;

import com.bic.burble.ontology.domain.world.WorldRecord;
import com.bic.burble.ontology.persistence.WorldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * H2-backed implementation of {@link WorldRepository}, using Spring Data JPA.
 * Activated via the {@code burble.ontology.persistance.world=h2} configuration
 * property (see {@code application.yml}).
 */
@Component
@ConditionalOnProperty(prefix = "burble.ontology.persistance", name = "world", havingValue = "h2")
public class H2WorldRepositoryAdapter implements WorldRepository {

    private static final Logger log = LoggerFactory.getLogger(H2WorldRepositoryAdapter.class);

    private final WorldJpaRepository repository;
    private final WorldEntityMapper mapper;

    public H2WorldRepositoryAdapter(WorldJpaRepository repository, WorldEntityMapper mapper) {
        log.debug("H2WorldRepositoryAdapter activated (burble.ontology.persistance.world=h2)");
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public WorldRecord save(WorldRecord world) {
        log.debug("[H2] save() - mapping WorldRecord to WorldEntity: {}", world);
        WorldEntity entity = mapper.toEntity(world);
        log.debug("[H2] save() - persisting WorldEntity: {}", entity);
        WorldEntity saved = repository.save(entity);
        log.debug("[H2] save() - persisted WorldEntity: {}", saved);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WorldRecord> findById(String id) {
        log.debug("[H2] findById() - id={}", id);
        Optional<WorldRecord> result = repository.findById(id).map(mapper::toDomain);
        log.debug("[H2] findById() - id={} result={}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public List<WorldRecord> findAll() {
        log.debug("[H2] findAll()");
        List<WorldRecord> results = repository.findAll().stream().map(mapper::toDomain).toList();
        log.debug("[H2] findAll() - returned {} record(s)", results.size());
        return results;
    }

    @Override
    public boolean deleteById(String id) {
        log.debug("[H2] deleteById() - id={}", id);
        if (!repository.existsById(id)) {
            log.debug("[H2] deleteById() - id={} does not exist, skipping delete", id);
            return false;
        }
        repository.deleteById(id);
        log.debug("[H2] deleteById() - id={} deleted", id);
        return true;
    }
}
