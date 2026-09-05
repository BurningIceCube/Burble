package com.bic.burble.ontology.service.impl;

import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.entity.CreateEntityRequest;
import com.bic.burble.ontology.domain.facet.ArchetypeFacet;
import com.bic.burble.ontology.domain.facet.CharacterFacet;
import com.bic.burble.ontology.domain.facet.ItemFacet;
import com.bic.burble.ontology.persistence.EntityRepository;
import com.bic.burble.ontology.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link EntityService} implementation, backed by whichever
 * {@link EntityRepository} implementation is active (e.g. the Neo4j-backed
 * adapter enabled via {@code burble.ontology.persistance.entity=neo4j}).
 *
 * <p>This is an internal implementation detail of the {@code ontology}
 * module; consumers should depend on the {@link EntityService} interface
 * rather than this class.
 */
@Service
public class DefaultEntityService implements EntityService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEntityService.class);

    private final EntityRepository entityRepository;

    public DefaultEntityService(EntityRepository entityRepository) {
        log.debug("Wiring DefaultEntityService with repository implementation: {}", entityRepository.getClass().getName());
        this.entityRepository = entityRepository;
    }

    @Override
    public String createEntity(String worldId, CreateEntityRequest request) {
        String guid = UUID.randomUUID().toString();
        log.debug("Building new entity guid={} for worldId={} from request={}", guid, worldId, request);
        Entity entity = toEntity(guid, worldId, request);
        log.debug("Saving entity via repository: {}", entity);
        entityRepository.save(entity);
        log.info("Entity created: guid={}, worldId={}", guid, worldId);
        return guid;
    }

    @Override
    public Optional<Entity> findById(String id) {
        log.debug("Looking up entity by id={}", id);
        Optional<Entity> result = entityRepository.findById(id);
        log.debug("Lookup result for id={}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public List<Entity> findAllByWorldId(String worldId) {
        log.debug("Looking up all entities for worldId={}", worldId);
        List<Entity> entities = entityRepository.findAllByWorldId(worldId);
        log.debug("Found {} entity(ies) for worldId={}", entities.size(), worldId);
        return entities;
    }

    @Override
    public boolean deleteById(String id) {
        log.debug("Deleting entity by id={}", id);
        boolean deleted = entityRepository.deleteById(id);
        log.info("Delete entity id={} result={}", id, deleted);
        return deleted;
    }

    @Override
    public Optional<Entity> updateEntity(String worldId, String entityId, CreateEntityRequest request) {
        log.debug("Updating entity id={} in worldId={} with request={}", entityId, worldId, request);
        if (entityRepository.findById(entityId).isEmpty()) {
            log.debug("Update aborted, no entity found for entityId={}", entityId);
            return Optional.empty();
        }
        Entity entity = toEntity(entityId, worldId, request);
        Entity updated = entityRepository.update(entity);
        log.info("Entity updated: entityId={}, worldId={}", entityId, worldId);
        return Optional.of(updated);
    }

    private Entity toEntity(String guid, String worldId, CreateEntityRequest request) {
        String name = request != null ? request.name() : null;
        String description = request != null ? request.description() : null;
        List<String> aliases = request != null && request.aliases() != null ? request.aliases() : List.of();
        Set<ArchetypeFacet> facets = toFacets(request);
        log.debug("Mapped CreateEntityRequest to Entity: guid={}, worldId={}, name={}, facets={}", guid, worldId, name, facets);
        return new Entity(guid, worldId, name, aliases, description, facets);
    }

    private Set<ArchetypeFacet> toFacets(CreateEntityRequest request) {
        if (request == null) {
            return Set.of();
        }
        Set<ArchetypeFacet> facets = new HashSet<>();
        if (Boolean.TRUE.equals(request.character())) {
            facets.add(new CharacterFacet());
        }
        if (request.item() != null) {
            facets.add(new ItemFacet(request.item().portable(), request.item().unique()));
        }
        return facets;
    }
}

