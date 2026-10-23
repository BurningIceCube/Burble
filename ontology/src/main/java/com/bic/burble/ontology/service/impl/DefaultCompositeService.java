package com.bic.burble.ontology.service.impl;

import com.bic.burble.ontology.domain.ArchetypeStatement;
import com.bic.burble.ontology.domain.Entity;
import com.bic.burble.ontology.domain.Relationship;
import com.bic.burble.ontology.domain.composite.Composite;
import com.bic.burble.ontology.domain.composite.Composite.Direction;
import com.bic.burble.ontology.domain.composite.Composite.Section;
import com.bic.burble.ontology.domain.composite.Composite.Step;
import com.bic.burble.ontology.domain.composite.CompositeView;
import com.bic.burble.ontology.domain.composite.CompositeView.CompositeInfo;
import com.bic.burble.ontology.domain.composite.CompositeView.EntitySummary;
import com.bic.burble.ontology.domain.composite.CompositeView.Hop;
import com.bic.burble.ontology.domain.composite.CompositeView.SectionInfo;
import com.bic.burble.ontology.domain.composite.CompositeView.SectionView;
import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.service.CompositeService;
import com.bic.burble.ontology.service.EntityService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Computes composites with the ordinary {@link EntityService} queries, so it
 * works the same on every store. Nothing is written.
 */
@Service
public class DefaultCompositeService implements CompositeService {

    private final EntityService entities;

    public DefaultCompositeService(EntityService entities) {
        this.entities = entities;
    }

    @Override
    public List<CompositeInfo> listComposites() {
        return Arrays.stream(Composite.values())
                .map(composite -> new CompositeInfo(composite.key(), composite.label(), composite.description(),
                        composite.roots().stream().sorted().toList(),
                        composite.sections().stream()
                                .map(section -> new SectionInfo(section.key(), section.label(), section.description(), path(section)))
                                .toList()))
                .toList();
    }

    @Override
    public List<EntitySummary> findRoots(String worldId, String name, Long atTick) {
        Composite composite = Composite.parse(name);
        List<EntitySummary> roots = new ArrayList<>();
        for (Entity entity : entities.findAllByWorldId(worldId)) {
            List<String> active = activeArchetypes(worldId, entity.guid(), atTick);
            if (active.stream().anyMatch(composite.roots()::contains)) {
                roots.add(new EntitySummary(entity.guid(), entity.name(), active, null, null));
            }
        }
        roots.sort((a, b) -> String.valueOf(a.name()).compareToIgnoreCase(String.valueOf(b.name())));
        return roots;
    }

    @Override
    public Optional<CompositeView> query(String worldId, String name, String rootId, Long atTick) {
        Composite composite = Composite.parse(name);
        Optional<Entity> root = entities.findById(rootId).filter(entity -> Objects.equals(worldId, entity.worldId()));
        if (root.isEmpty()) {
            return Optional.empty();
        }
        List<String> rootArchetypes = activeArchetypes(worldId, rootId, atTick);
        if (rootArchetypes.stream().noneMatch(composite.roots()::contains)) {
            throw new InvalidStatementException("A " + composite.label() + " starts at an entity that is "
                    + String.join(" or ", composite.roots().stream().sorted().toList()) + ", but " + root.get().name()
                    + " is " + (rootArchetypes.isEmpty() ? "no archetype" : String.join(", ", rootArchetypes))
                    + (atTick == null ? "" : " at tick " + atTick));
        }
        List<SectionView> sections = new ArrayList<>();
        for (Section section : composite.sections()) {
            sections.add(new SectionView(section.key(), section.label(), section.description(), path(section),
                    walk(worldId, rootId, section.steps(), atTick)));
        }
        EntitySummary rootSummary = new EntitySummary(rootId, root.get().name(), rootArchetypes, null, null);
        return Optional.of(new CompositeView(composite.key(), composite.label(), atTick, rootSummary, sections));
    }

    /** A partial path: where it ended, the hops so far, and the certainty of the last hop. */
    private record Reached(String id, List<Hop> via, double certainty) {
    }

    private List<EntitySummary> walk(String worldId, String rootId, List<Step> steps, Long atTick) {
        List<Reached> frontier = List.of(new Reached(rootId, List.of(), 1.0));
        for (Step step : steps) {
            List<Reached> next = new ArrayList<>();
            for (Reached from : frontier) {
                next.addAll(follow(worldId, from, step, atTick));
            }
            frontier = next;
        }
        // One row per entity; when several paths reach it, keep the most certain link.
        Map<String, Reached> best = new LinkedHashMap<>();
        for (Reached reached : frontier) {
            if (reached.id().equals(rootId)) {
                continue;
            }
            best.merge(reached.id(), reached, (a, b) -> b.certainty() > a.certainty() ? b : a);
        }
        List<EntitySummary> out = new ArrayList<>();
        for (Reached reached : best.values()) {
            entities.findById(reached.id()).ifPresent(entity -> out.add(new EntitySummary(entity.guid(), entity.name(),
                    activeArchetypes(worldId, entity.guid(), atTick), reached.certainty(), reached.via())));
        }
        out.sort((a, b) -> String.valueOf(a.name()).compareToIgnoreCase(String.valueOf(b.name())));
        return out;
    }

    private List<Reached> follow(String worldId, Reached from, Step step, Long atTick) {
        String verb = step.verb().name();
        List<Reached> out = new ArrayList<>();
        if (step.direction() == Direction.OUT) {
            for (Relationship relationship : entities.findRelationships(from.id(), atTick)) {
                if (verb.equals(relationship.verb()) && passes(worldId, relationship.objectId(), step, atTick)) {
                    out.add(extend(from, new Hop(from.id(), verb, relationship.objectId()),
                            relationship.objectId(), relationship.certainty()));
                }
            }
        } else {
            for (Entity subject : entities.findByRelationship(verb, from.id(), atTick)) {
                if (!passes(worldId, subject.guid(), step, atTick)) {
                    continue;
                }
                entities.findRelationships(subject.guid(), atTick).stream()
                        .filter(relationship -> verb.equals(relationship.verb()) && from.id().equals(relationship.objectId()))
                        .mapToDouble(Relationship::certainty)
                        .max()
                        .ifPresent(certainty -> out.add(extend(from, new Hop(subject.guid(), verb, from.id()),
                                subject.guid(), certainty)));
            }
        }
        return out;
    }

    private static Reached extend(Reached from, Hop hop, String to, double certainty) {
        List<Hop> via = new ArrayList<>(from.via());
        via.add(hop);
        return new Reached(to, List.copyOf(via), certainty);
    }

    private boolean passes(String worldId, String id, Step step, Long atTick) {
        if (step.archetypes().isEmpty()) {
            return entities.findById(id).map(entity -> Objects.equals(worldId, entity.worldId())).orElse(false);
        }
        return activeArchetypes(worldId, id, atTick).stream().anyMatch(step.archetypes()::contains);
    }

    private List<String> activeArchetypes(String worldId, String id, Long atTick) {
        return entities.findArchetypes(worldId, id, atTick).orElse(List.of()).stream()
                .map(ArchetypeStatement::archetype)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static String path(Section section) {
        return section.steps().stream().map(Step::toString).collect(Collectors.joining(" "));
    }
}
