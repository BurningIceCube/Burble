package com.bic.burble.ontology.service;

import com.bic.burble.ontology.domain.world.CreateWorldRequest;
import com.bic.burble.ontology.domain.world.WorldRecord;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorldService {

    /**
     * Services for CRUD on World entities.
     */
    private final Map<String, WorldRecord> worlds = new ConcurrentHashMap<>();

    public Optional<WorldRecord> getWorld(String worldId) {
        return Optional.ofNullable(worlds.get(worldId));
    }

    public List<WorldRecord> getAllWorlds() {
        return new ArrayList<>(worlds.values());
    }

    public String createWorld(CreateWorldRequest request) {
        String guid = UUID.randomUUID().toString();
        Date now = new Date();
        String name = request != null ? request.name() : null;
        String description = request != null ? request.description() : null;
        String owner = "system";

        WorldRecord saved = new WorldRecord(
                guid,
                name,
                description,
                owner,
                now,
                now
        );
        worlds.put(guid, saved);
        return guid;
    }

    public boolean deleteWorld(String worldId) {
        return worlds.remove(worldId) != null;
    }
}


