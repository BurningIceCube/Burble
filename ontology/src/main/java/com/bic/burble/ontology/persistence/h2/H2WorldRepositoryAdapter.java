package com.bic.burble.ontology.persistence.h2;

import com.bic.burble.ontology.domain.world.WorldRecord;
import com.bic.burble.ontology.persistence.WorldRepository;
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

    private final WorldJpaRepository repository;
    private final WorldEntityMapper mapper;

    public H2WorldRepositoryAdapter(WorldJpaRepository repository, WorldEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public WorldRecord save(WorldRecord world) {
        WorldEntity saved = repository.save(mapper.toEntity(world));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WorldRecord> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WorldRecord> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean deleteById(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
