package local.lito381.server;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;

/** Source-owned item lifetimes, separate from magic stones. No SQL, mutation,
 * timer renewal or interpretation of positive remaining seconds as permanence.
 * Absolute deadlines use the database's own local wall-clock convention. Their
 * remaining duration is anchored at query START, never extended by query time.
 * A frozen observation rejects a backwards JVM clock; cross-restart clock or
 * database rollback is not a durable monotonic-time guarantee. */
final class ModernItemLifetime {
    enum State {
        UNQUALIFIED, PERMANENT, ACTIVE, EXPIRED, UNSTARTED_OR_MISSING,
        RELATIVE_UNQUALIFIED, LIGHTING_UNQUALIFIED;
        boolean usable() { return this == PERMANENT || this == ACTIVE; }
    }
    record View(long itemObjectId, State state, long expiresAtMillis) {
        View {
            Objects.requireNonNull(state);
            if (itemObjectId <= 0 || expiresAtMillis < -1
                    || ((state == State.ACTIVE || state == State.EXPIRED) != (expiresAtMillis >= 0))) throw invalid();
        }
        boolean usable() { return state.usable(); }
    }
    record Fact(long itemObjectId, long catalogId, ModernCharacterData.ItemKind kind,
                boolean sourceEquipped, OptionalLong remainingSeconds, boolean timeRulePresent,
                OptionalLong timeRuleHours, Optional<String> absoluteLocalTime,
                long templateMaxUseSeconds, boolean lighting) {
        Fact {
            Objects.requireNonNull(kind); Objects.requireNonNull(remainingSeconds);
            Objects.requireNonNull(timeRuleHours); Objects.requireNonNull(absoluteLocalTime);
            if (itemObjectId <= 0 || catalogId <= 0 || templateMaxUseSeconds < 0
                    || remainingSeconds.isPresent() && remainingSeconds.getAsLong() < 0
                    || timeRuleHours.isPresent() && timeRuleHours.getAsLong() < 0
                    || !timeRulePresent && timeRuleHours.isPresent()
                    || lighting && kind != ModernCharacterData.ItemKind.ETC) throw invalid();
            absoluteLocalTime = absoluteLocalTime.map(ModernItemLifetime::canonical);
        }
        boolean verifiedPermanent() {
            return !lighting && remainingSeconds.isPresent() && remainingSeconds.getAsLong() == 0
                    && !timeRulePresent && absoluteLocalTime.isEmpty() && templateMaxUseSeconds == 0;
        }
        @Override public String toString() { return "ItemLifetimeFact[redacted]"; }
    }
    record Observation(int sourceId, long objectId, String dbNowLocal, long queryStartedEpochMillis,
                       long queryStartedNanos, Map<Long, Fact> items, long dbClockUncertaintyMillis) {
        /** Full-precision trusted in-memory observation; JDBC supplies its own precision bound. */
        Observation(int sourceId, long objectId, String dbNowLocal, long queryStartedEpochMillis,
                    long queryStartedNanos, Map<Long, Fact> items) {
            this(sourceId, objectId, dbNowLocal, queryStartedEpochMillis, queryStartedNanos, items, 0);
        }
        Observation {
            if (sourceId <= 0 || objectId <= 0 || queryStartedEpochMillis < 0 || items == null
                    || items.size() > ModernCharacterData.MAX_ITEMS
                    || dbClockUncertaintyMillis < 0 || dbClockUncertaintyMillis > 1000) throw invalid();
            dbNowLocal = canonical(dbNowLocal);
            var copy = new TreeMap<Long, Fact>();
            for (var entry : items.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null
                        || entry.getKey().longValue() != entry.getValue().itemObjectId()) throw invalid();
                copy.put(entry.getKey(), entry.getValue());
            }
            items = Map.copyOf(copy);
        }
        void validateSource(ModernCharacterData.Snapshot source) {
            if (source == null || source.sourceId() != sourceId || source.character().objectId() != objectId
                    || source.inventory().size() != items.size()) throw invalid();
            for (var item : source.inventory()) {
                Fact fact = items.get(item.objectId());
                if (fact == null || fact.catalogId() != item.template().catalogId()
                        || fact.kind() != item.template().kind() || fact.sourceEquipped() != item.equipped()) throw invalid();
            }
        }
        View view(long itemObjectId, long nowEpochMillis) {
            if (nowEpochMillis < 0) throw invalid();
            Fact fact = Objects.requireNonNull(items.get(itemObjectId), "unqualified lifetime identity");
            if (nowEpochMillis < queryStartedEpochMillis) return new View(itemObjectId, State.UNQUALIFIED, -1);
            if (fact.lighting()) return new View(itemObjectId, State.LIGHTING_UNQUALIFIED, -1);
            if (fact.absoluteLocalTime().isPresent()) {
                Duration duration = Duration.between(parse(dbNowLocal), parse(fact.absoluteLocalTime().get()));
                long micros = Math.addExact(Math.multiplyExact(duration.getSeconds(), 1_000_000L), duration.getNano() / 1_000);
                // MySQL 5.5 CURRENT_TIMESTAMP exposes whole seconds. Subtract
                // the entire unknown fraction rather than granting up to one
                // extra second; fractional-capable fixtures may prove zero.
                long durationMillis = Math.subtractExact(Math.floorDiv(micros, 1_000), dbClockUncertaintyMillis);
                long cutoff = Math.addExact(queryStartedEpochMillis, durationMillis);
                // A pre-epoch cutoff is already expired, not an unsigned wrap.
                cutoff = Math.max(0, cutoff);
                return new View(itemObjectId, nowEpochMillis < cutoff ? State.ACTIVE : State.EXPIRED, cutoff);
            }
            if (fact.remainingSeconds().isEmpty() || fact.timeRulePresent() && fact.timeRuleHours().isEmpty())
                return new View(itemObjectId, State.UNQUALIFIED, -1);
            if (fact.remainingSeconds().getAsLong() > 0)
                return new View(itemObjectId, State.RELATIVE_UNQUALIFIED, -1);
            if (fact.timeRulePresent() || fact.templateMaxUseSeconds() > 0)
                return new View(itemObjectId, State.UNSTARTED_OR_MISSING, -1);
            return new View(itemObjectId, State.PERMANENT, -1);
        }
        List<View> views(ModernCharacterData.Snapshot source, long nowEpochMillis) {
            validateSource(source);
            var result = new ArrayList<View>(items.size());
            for (var item : source.inventory()) result.add(view(item.objectId(), nowEpochMillis));
            return List.copyOf(result);
        }
        /** Explicit trusted in-memory fixture qualification. JDBC never uses
         * this factory: every real item requires all lifetime source columns. */
        static Observation verifiedPermanent(ModernCharacterData.Snapshot source) {
            return synthetic(source, true);
        }
        static Observation unqualified(ModernCharacterData.Snapshot source) {
            return synthetic(source, false);
        }
        private static Observation synthetic(ModernCharacterData.Snapshot source, boolean permanent) {
            Objects.requireNonNull(source);
            var facts = new TreeMap<Long, Fact>();
            for (var item : source.inventory()) facts.put(item.objectId(), new Fact(item.objectId(),
                    item.template().catalogId(), item.template().kind(), item.equipped(),
                    permanent ? OptionalLong.of(0) : OptionalLong.empty(), false, OptionalLong.empty(),
                    Optional.empty(), 0, false));
            return new Observation(source.sourceId(), source.character().objectId(),
                    "1970-01-01 00:00:00.000000", 0, 0, facts);
        }
        @Override public String toString() { return "ItemLifetimeObservation[redacted]"; }
    }
    private static final DateTimeFormatter INPUT = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 6, true).optionalEnd()
            .toFormatter(java.util.Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter CANONICAL = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSS", java.util.Locale.ROOT);
    static LocalDateTime parse(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,6})?")) throw invalid();
        try {
            LocalDateTime parsed = LocalDateTime.parse(value, INPUT);
            if (parsed.getYear() < 1 || parsed.getYear() > 9999) throw invalid();
            return parsed;
        } catch (java.time.DateTimeException failure) { throw invalid(); }
    }
    static String canonical(String value) { return CANONICAL.format(parse(value)); }
    private ModernItemLifetime() {}
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid qualified item lifetime"); }
}
