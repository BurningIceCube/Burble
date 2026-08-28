package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.world.CreateWorldRequest;
import com.bic.burble.ontology.domain.world.WorldRecord;
import com.bic.burble.ontology.persistence.WorldRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Services for CRUD on World entities, backed by whichever
 * {@link WorldRepository} implementation is active (e.g. the H2-backed
 * adapter enabled via {@code burble.ontology.persistance.world=h2}).
 */
@Service
public class WorldService {

    private final WorldRepository worldRepository;

    public WorldService(WorldRepository worldRepository) {
        this.worldRepository = worldRepository;
    }

    public Optional<WorldRecord> getWorld(String worldId) {
        return worldRepository.findById(worldId);
    }

    public List<WorldRecord> getAllWorlds() {
        return worldRepository.findAll();
    }

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

    public boolean deleteWorld(String worldId) {
        return worldRepository.deleteById(worldId);
    }
}


