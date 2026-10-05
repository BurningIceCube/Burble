package com.bic.burble.ontology.service.impl;

import com.bic.burble.ontology.domain.world.CreateWorldRequest;
import com.bic.burble.ontology.domain.world.WorldRecord;
import com.bic.burble.ontology.persistence.WorldRepository;
import com.bic.burble.ontology.service.WorldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DefaultWorldService.class);

    private final WorldRepository worldRepository;

    public DefaultWorldService(WorldRepository worldRepository) {
        log.debug("Wiring DefaultWorldService with repository implementation: {}", worldRepository.getClass().getName());
        this.worldRepository = worldRepository;
    }

    @Override
    public Optional<WorldRecord> getWorld(String worldId) {
        log.debug("Looking up world by id={}", worldId);
        Optional<WorldRecord> result = worldRepository.findById(worldId);
        log.debug("Lookup result for worldId={}: {}", worldId, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public List<WorldRecord> getAllWorlds() {
        log.debug("Looking up all worlds");
        List<WorldRecord> worlds = worldRepository.findAll();
        log.debug("Found {} world(s)", worlds.size());
        return worlds;
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
        log.debug("Saving world via repository: {}", world);
        worldRepository.save(world);
        log.info("World created: guid={}", guid);
        return guid;
    }

    @Override
    public boolean deleteWorld(String worldId) {
        log.debug("Deleting world by id={}", worldId);
        boolean deleted = worldRepository.deleteById(worldId);
        log.info("Delete world id={} result={}", worldId, deleted);
        return deleted;
    }
}
