package com.bic.burble.ontology.domain.statement;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A statement's time as ticks. The range is half-open, {@code [start, end)}:
 * it includes the start tick and stops just before the end tick. An open
 * start is {@link Long#MIN_VALUE} and an open end is {@link Long#MAX_VALUE}.
 *
 * <p>Stores resolve period bounds through a lookup that returns the
 * period's own ticks, or empty when the id is not an entity that IS_A Period.
 */
public record TimeRange(long start, long end) {

    /** A Period entity's own ticks; either may be null when unknown. */
    public record PeriodTicks(Long startTick, Long endTick) {
    }

    public static final TimeRange ALWAYS = new TimeRange(Long.MIN_VALUE, Long.MAX_VALUE);

    public boolean contains(long tick) {
        return start <= tick && tick < end;
    }

    public boolean overlaps(TimeRange other) {
        return start < other.end && other.start < end;
    }

    /**
     * The first part of {@code want} that none of {@code have} covers, or
     * empty when their union covers all of it. Ranges that touch, such as
     * [0, 10) and [10, 20), cover as one.
     */
    public static Optional<TimeRange> firstGap(TimeRange want, Collection<TimeRange> have) {
        List<TimeRange> sorted = have.stream().sorted(Comparator.comparingLong(TimeRange::start)).toList();
        long cursor = want.start();
        for (TimeRange range : sorted) {
            if (cursor >= want.end()) {
                break;
            }
            if (range.start() > cursor) {
                return Optional.of(new TimeRange(cursor, Math.min(range.start(), want.end())));
            }
            cursor = Math.max(cursor, range.end());
        }
        return cursor >= want.end() ? Optional.empty() : Optional.of(new TimeRange(cursor, want.end()));
    }

    /** Readable form, for example "from tick 10 until tick 20" or "always". */
    public String describe() {
        if (start == Long.MIN_VALUE && end == Long.MAX_VALUE) {
            return "always";
        }
        String from = start == Long.MIN_VALUE ? "from the beginning" : "from tick " + start;
        String until = end == Long.MAX_VALUE ? "onward" : "until tick " + end;
        return from + " " + until;
    }

    public static TimeRange resolve(StatementTime time, Function<String, Optional<PeriodTicks>> periods) {
        if (time == null) {
            return ALWAYS;
        }
        return new TimeRange(bound(time.start(), true, periods), bound(time.end(), false, periods));
    }

    /**
     * Checks every period bound names a Period and the range is not empty or
     * backwards, then returns the resolved range.
     */
    public static TimeRange validate(StatementTime time, Function<String, Optional<PeriodTicks>> periods) {
        if (time != null) {
            requirePeriod(time.start(), periods);
            requirePeriod(time.end(), periods);
        }
        TimeRange range = resolve(time, periods);
        if (range.start() >= range.end()) {
            throw new InvalidStatementException(
                    "Time range must start before it ends, got start " + range.start() + " and end " + range.end());
        }
        return range;
    }

    private static long bound(TimeBound bound, boolean isStart, Function<String, Optional<PeriodTicks>> periods) {
        long open = isStart ? Long.MIN_VALUE : Long.MAX_VALUE;
        if (bound == null) {
            return open;
        }
        if (bound.tick() != null) {
            return bound.tick();
        }
        if (bound.period() == null) {
            return open;
        }
        return periods.apply(bound.period())
                .map(ticks -> isStart ? ticks.startTick() : ticks.endTick())
                .orElse(open);
    }

    private static void requirePeriod(TimeBound bound, Function<String, Optional<PeriodTicks>> periods) {
        if (bound == null || bound.period() == null || bound.period().isBlank()) {
            return;
        }
        if (periods.apply(bound.period()).isEmpty()) {
            throw new InvalidStatementException(
                    "Time bound period " + bound.period() + " is not an entity that IS_A Period");
        }
    }
}
