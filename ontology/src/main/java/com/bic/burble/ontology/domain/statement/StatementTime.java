package com.bic.burble.ontology.domain.statement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * When a statement holds. A missing {@code start} means it has held since
 * before anything is known; a missing {@code end} means it still holds or the
 * end is unknown. A statement with no time at all holds always.
 *
 * <p>The range is half-open: it includes the start tick and stops just
 * before the end tick, so one carrier can hand an item to the next at the
 * same tick.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "When a statement holds. Leave start or end out for open-ended.")
public record StatementTime(TimeBound start, TimeBound end) {

    public StatementTime {
        start = start == null || start.isEmpty() ? null : start;
        end = end == null || end.isEmpty() ? null : end;
    }

    public static StatementTime between(long startTick, long endTick) {
        return new StatementTime(TimeBound.atTick(startTick), TimeBound.atTick(endTick));
    }

    public static StatementTime from(long startTick) {
        return new StatementTime(TimeBound.atTick(startTick), null);
    }

    public static StatementTime during(String periodId) {
        return new StatementTime(TimeBound.inPeriod(periodId), TimeBound.inPeriod(periodId));
    }

    @JsonIgnore
    public boolean isOpen() {
        return start == null && end == null;
    }
}
