package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.statement.InvalidStatementException;
import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeBound;

import java.util.Optional;

/**
 * Turns a statement's time into a tick range using the graph's Period
 * entities. The range is half-open, {@code [start, end)}. An open start is
 * {@link Long#MIN_VALUE} and an open end is {@link Long#MAX_VALUE}.
 */
final class StatementTimes {

    record Range(long start, long end) {
        boolean contains(long tick) {
            return start <= tick && tick < end;
        }

        boolean overlaps(Range other) {
            return start < other.end && other.start < end;
        }
    }

    private StatementTimes() {
    }

    static Range resolve(OntologyGraph graph, StatementTime time) {
        if (time == null) {
            return new Range(Long.MIN_VALUE, Long.MAX_VALUE);
        }
        long start = bound(graph, time.start(), true);
        long end = bound(graph, time.end(), false);
        return new Range(start, end);
    }

    /** Checks the periods exist and the range is not empty or backwards. */
    static Range validate(OntologyGraph graph, StatementTime time) {
        if (time != null) {
            requirePeriod(graph, time.start());
            requirePeriod(graph, time.end());
        }
        Range range = resolve(graph, time);
        if (range.start() >= range.end()) {
            throw new InvalidStatementException(
                    "Time range must start before it ends, got start " + range.start() + " and end " + range.end());
        }
        return range;
    }

    private static long bound(OntologyGraph graph, TimeBound bound, boolean isStart) {
        long open = isStart ? Long.MIN_VALUE : Long.MAX_VALUE;
        if (bound == null) {
            return open;
        }
        if (bound.tick() != null) {
            return bound.tick();
        }
        return periodStatement(graph, bound.period())
                .map(statement -> FacetStatements.tick(statement.properties(),
                        isStart ? FacetStatements.START_TICK : FacetStatements.END_TICK))
                .orElse(open);
    }

    private static void requirePeriod(OntologyGraph graph, TimeBound bound) {
        if (bound == null || bound.period() == null || bound.period().isBlank()) {
            return;
        }
        if (periodStatement(graph, bound.period()).isEmpty()) {
            throw new InvalidStatementException(
                    "Time bound period " + bound.period() + " is not an entity that IS_A Period");
        }
    }

    private static Optional<StoredStatement> periodStatement(OntologyGraph graph, String periodId) {
        if (periodId == null) {
            return Optional.empty();
        }
        return graph.entities().stream()
                .filter(entity -> periodId.equals(entity.guid()))
                .flatMap(entity -> entity.statements().stream())
                .filter(statement -> statement != null
                        && FacetStatements.IS_A.equals(statement.verb())
                        && FacetStatements.PERIOD.equals(statement.object()))
                .findFirst();
    }
}
