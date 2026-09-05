package com.bic.burble.ontology.service.impl;

import com.bic.burble.ontology.domain.world.CreateWorldRequest;
import com.bic.burble.ontology.domain.world.WorldRecord;
import com.bic.burble.ontology.persistence.WorldRepository;
import com.bic.burble.ontology.service.WorldService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link WorldService} implementation, backed by whichever
 * {@link WorldRepository} implementation is active (e.g. the H2-backed
 * adapter enabled via {@code burble.ontology.persistance.world=h2}).
 *
 * <p>This is an internal implementation detail of the {@code ontology}
 * module; consumers should depend on the {@link WorldService} interface
 * rather than this class.
 */
@Service
public class DefaultWorldService implements WorldService {

    private final WorldRepository worldRepository;

    public DefaultWorldService(WorldRepository worldRepository) {
        this.worldRepository = worldRepository;
    }

    @Override
    public Optional<WorldRecord> getWorld(String worldId) {
        return worldRepository.findById(worldId);
    }

    @Override
    public List<WorldRecord> getAllWorlds() {
        return worldRepository.findAll();
    }

    @Override
    public String createWorld(CreateWorldRequest request) {
        String guid = UUID.randomUUID().toString();
        Date now = new Date();
        String name = request != null ? request.name() : null;
        String description = request != null ? request.description() : null;
        String owner = "system";

        WorldRecord world = new WorldRecord(
                guid,
                name,
                description,
                owner,
                now,
                now
        );
        worldRepository.save(world);
        return guid;
    }

    @Override
    public boolean deleteWorld(String worldId) {
        return worldRepository.deleteById(worldId);
    }
}
