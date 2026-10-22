package com.bic.burble.ontology.persistence.json;

import com.bic.burble.ontology.domain.statement.StatementTime;
import com.bic.burble.ontology.domain.statement.TimeRange;
import com.bic.burble.ontology.domain.statement.TimeRange.PeriodTicks;

import java.util.Optional;

/** Resolves statement times against the Period entities in a JSON graph. */
final class StatementTimes {

    private StatementTimes() {
    }

    static TimeRange resolve(OntologyGraph graph, StatementTime time) {
        return TimeRange.resolve(time, id -> periodTicks(graph, id));
    }

    static TimeRange validate(OntologyGraph graph, StatementTime time) {
        return TimeRange.validate(time, id -> periodTicks(graph, id));
    }

    private static Optional<PeriodTicks> periodTicks(OntologyGraph graph, String periodId) {
        if (periodId == null) {
            return Optional.empty();
        }
        return graph.entities().stream()
                .filter(entity -> periodId.equals(entity.guid()))
                .flatMap(entity -> entity.statements().stream())
                .filter(statement -> statement != null
                        && FacetStatements.IS_A.equals(statement.verb())
                        && FacetStatements.PERIOD.equals(statement.object()))
                .findFirst()
                .map(statement -> new PeriodTicks(
                        FacetStatements.tick(statement.properties(), FacetStatements.START_TICK),
                        FacetStatements.tick(statement.properties(), FacetStatements.END_TICK)));
    }
}
