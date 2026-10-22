package com.bic.burble.ontology.domain.statement;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One end of a statement's time range. {@code tick} is a sortable number on
 * the world's timeline (for example year 300). {@code period} is the id of an
 * entity that IS_A Period; when there is no tick, the bound uses that
 * period's own start (for a start bound) or end (for an end bound). When both
 * are set, the tick is used and the period is kept as a label.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One end of a time range: a tick, a Period entity id, or both")
public record TimeBound(
        @Schema(description = "Sortable world time, such as a year", example = "300")
        Long tick,
        @Schema(description = "Id of an entity that IS_A Period", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String period
) {
    public static TimeBound atTick(long tick) {
        return new TimeBound(tick, null);
    }

    public static TimeBound inPeriod(String periodId) {
        return new TimeBound(null, periodId);
    }

    public boolean isEmpty() {
        return tick == null && (period == null || period.isBlank());
    }
}
