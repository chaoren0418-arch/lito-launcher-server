package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Match-owned, monotonic leases. No source buff is imported and no client clock is consulted. */
final class ModernTimedEffects {
    static final long MAX_OBSERVED_NANOS = TimeUnit.MINUTES.toNanos(30);
    enum Action { MOVE, ATTACK, SPELL }
    enum HasteOrigin { NONE, POTION, GREATER_HASTE, HASTE }
    enum Kind {
        HASTE(40013, 300, 191, 127), BRAVE(40014, 300, 751, 127),
        ELF_BRAVE(40068, 480, 751, 4), WISDOM(40016, 300, 750, 8);
        final long itemId, durationNanos;
        final int durationSeconds, effect, classMask;
        Kind(long itemId, int seconds, int effect, int classMask) {
            this.itemId = itemId; this.durationSeconds = seconds;
            this.durationNanos = TimeUnit.SECONDS.toNanos(seconds);
            this.effect = effect; this.classMask = classMask;
        }
        boolean eligible(int characterClass) {
            return characterClass >= 0 && characterClass <= 6 && (classMask & (1 << characterClass)) != 0;
        }
        long itemId() { return itemId; }
        long durationMillis() { return TimeUnit.SECONDS.toMillis(durationSeconds); }
        long durationNanos() { return durationNanos; }
        int effect() { return effect; }
        int classMask() { return classMask; }
        static Kind find(long itemId) {
            for (var kind : values()) if (kind.itemId == itemId) return kind;
            return null;
        }
    }
    /** observedNanos is fixed at this state mutation, NOT sampled anew for every read. */
    record State(long entityId, long revision, long observedNanos, long hasteUntilNanos,
                 long braveUntilNanos, long wisdomUntilNanos, int braveKind, HasteOrigin hasteOrigin, long slowUntilNanos, long slowRevision) {
        State(long entityId,long revision,long observedNanos,long hasteUntilNanos,
                long braveUntilNanos,long wisdomUntilNanos,int braveKind,HasteOrigin hasteOrigin) {
            this(entityId,revision,observedNanos,hasteUntilNanos,braveUntilNanos,wisdomUntilNanos,braveKind,hasteOrigin,0,0);
        }
        // Frozen potion-only constructors/decoders retain their 300-second bound.
        State(long entityId,long revision,long observedNanos,long hasteUntilNanos,
                long braveUntilNanos,long wisdomUntilNanos,int braveKind) {
            this(entityId,revision,observedNanos,hasteUntilNanos,braveUntilNanos,wisdomUntilNanos,braveKind,
                    hasteUntilNanos==0?HasteOrigin.NONE:HasteOrigin.POTION);
        }
        State {
            Objects.requireNonNull(hasteOrigin);
            if(slowRevision<0||slowRevision>revision||slowUntilNanos!=0&&(slowRevision==0||hasteUntilNanos!=0))throw bad();
            lease(slowUntilNanos,observedNanos,TimeUnit.SECONDS.toNanos(ModernSlow.DURATION_SECONDS));
            if (entityId < 1 || revision < 0 || observedNanos < 0 || observedNanos > MAX_OBSERVED_NANOS
                    || braveKind != 0 && braveKind != 1 && braveKind != 3
                    || (braveKind == 0) != (braveUntilNanos == 0)) throw bad();
            if ((hasteOrigin == HasteOrigin.NONE) != (hasteUntilNanos == 0)) throw bad();
            lease(hasteUntilNanos, observedNanos, hasteOrigin == HasteOrigin.GREATER_HASTE
                    ? TimeUnit.SECONDS.toNanos(ModernGreaterHaste.DURATION_SECONDS) : hasteOrigin == HasteOrigin.HASTE
                    ? TimeUnit.SECONDS.toNanos(ModernHaste.DURATION_SECONDS) : Kind.HASTE.durationNanos);
            lease(braveUntilNanos, observedNanos, braveKind == 3 ? Kind.ELF_BRAVE.durationNanos : Kind.BRAVE.durationNanos);
            lease(wisdomUntilNanos, observedNanos, Kind.WISDOM.durationNanos);
            if (revision == 0 && (observedNanos != 0 || hasteUntilNanos != 0
                    || braveUntilNanos != 0 || wisdomUntilNanos != 0 || braveKind != 0)) throw bad();
        }
        boolean hasEffects() { return slowUntilNanos != 0 || hasteUntilNanos != 0 || braveUntilNanos != 0 || wisdomUntilNanos != 0; }
        int spBonus() { return wisdomUntilNanos == 0 ? 0 : 2; }
        State wireRevision() {
            return revision == 0 ? this : new State(entityId, Math.addExact(revision, 1), observedNanos,
                    hasteUntilNanos, braveUntilNanos, wisdomUntilNanos, braveKind, hasteOrigin,
                    slowUntilNanos,slowRevision==0?0:Math.incrementExact(slowRevision));
        }
        boolean holds(Kind kind) {
            return switch (Objects.requireNonNull(kind)) {
                case HASTE -> hasteUntilNanos != 0;
                case BRAVE -> braveKind == 1;
                case ELF_BRAVE -> braveKind == 3;
                case WISDOM -> wisdomUntilNanos != 0;
            };
        }
        long until(Kind kind) {
            return switch (Objects.requireNonNull(kind)) {
                case HASTE -> hasteUntilNanos;
                case BRAVE, ELF_BRAVE -> braveUntilNanos;
                case WISDOM -> wisdomUntilNanos;
            };
        }
    }
    private ModernTimedEffects() {}
    static State initial(long entityId) { return new State(entityId, 0, 0, 0, 0, 0, 0); }

    /** Only a qualified source template and a live engine guard may authorize this prospective value. */
    static State apply(State old, Kind kind, long elapsedNanos, long nextRevision, int characterClass) {
        Objects.requireNonNull(kind); mutation(old, elapsedNanos, nextRevision);
        if (!kind.eligible(characterClass)) throw bad();
        long haste = alive(old.hasteUntilNanos(), elapsedNanos);
        long slow = alive(old.slowUntilNanos(), elapsedNanos);
        var origin = haste == 0 ? HasteOrigin.NONE : old.hasteOrigin();
        long brave = alive(old.braveUntilNanos(), elapsedNanos);
        long wisdom = alive(old.wisdomUntilNanos(), elapsedNanos);
        int braveKind = brave == 0 ? 0 : old.braveKind();
        long until = Math.addExact(elapsedNanos, kind.durationNanos);
        switch (kind) {
            case HASTE -> { haste = until; slow = 0; origin = HasteOrigin.POTION; }
            case BRAVE -> { brave = until; braveKind = 1; }
            case ELF_BRAVE -> { brave = until; braveKind = 3; }
            case WISDOM -> wisdom = Math.max(wisdom, until);
        }
        return new State(old.entityId(), nextRevision, elapsedNanos, haste, brave, wisdom, braveKind, origin,slow,old.slowRevision());
    }
    /** Candidate only: the engine must atomically commit its qualified MP receipt. */
    static State applyGreaterHaste(State old,long elapsedNanos,long nextRevision) {
        return applySpellHaste(old, elapsedNanos, nextRevision, ModernGreaterHaste.DURATION_SECONDS, HasteOrigin.GREATER_HASTE);
    }
    /** Target-side skill43 candidate; no caster authorization, MP debit or wire capability. */
    static State applyHaste(State old,long elapsedNanos,long nextRevision) {
        return applySpellHaste(old, elapsedNanos, nextRevision, ModernHaste.DURATION_SECONDS, HasteOrigin.HASTE);
    }
    private static State applySpellHaste(State old,long elapsedNanos,long nextRevision,int seconds,HasteOrigin origin) {
        mutation(old,elapsedNanos,nextRevision);
        long brave=alive(old.braveUntilNanos(),elapsedNanos);
        boolean neutralized=alive(old.slowUntilNanos(),elapsedNanos)!=0;
        return new State(old.entityId(),nextRevision,elapsedNanos,
                neutralized?0:Math.addExact(elapsedNanos,TimeUnit.SECONDS.toNanos(seconds)),
                brave,alive(old.wisdomUntilNanos(),elapsedNanos),brave==0?0:old.braveKind(),
                neutralized?HasteOrigin.NONE:origin,0,old.slowRevision());
    }
    /** Target-side admitted Slow29 only. Caller must qualify source, probability
     * and immunity before atomically publishing this with material/MP costs. */
    static State applySlow(State old,long elapsedNanos,long nextRevision) {
        mutation(old,elapsedNanos,nextRevision);
        long brave=alive(old.braveUntilNanos(),elapsedNanos);
        boolean neutralized=alive(old.hasteUntilNanos(),elapsedNanos)!=0;
        return new State(old.entityId(),nextRevision,elapsedNanos,0,brave,
                alive(old.wisdomUntilNanos(),elapsedNanos),brave==0?0:old.braveKind(),HasteOrigin.NONE,
                neutralized?0:Math.addExact(elapsedNanos,TimeUnit.SECONDS.toNanos(ModernSlow.DURATION_SECONDS)),nextRevision);
    }
    /** Even expired/cleared Slow history is not representable by the old wire. */
    static void requireNoSlow(State state) {
        if(Objects.requireNonNull(state).slowRevision()!=0)throw bad();
    }
    /** No expiry means the exact immutable state (including revision and sampled time) is retained. */
    static State expire(State old, long elapsedNanos, long nextRevision) {
        mutation(old, elapsedNanos, nextRevision);
        long haste = alive(old.hasteUntilNanos(), elapsedNanos);
        long brave = alive(old.braveUntilNanos(), elapsedNanos);
        long wisdom = alive(old.wisdomUntilNanos(), elapsedNanos);
        long slow = alive(old.slowUntilNanos(), elapsedNanos);
        if (slow == old.slowUntilNanos() && haste == old.hasteUntilNanos() && brave == old.braveUntilNanos()
                && wisdom == old.wisdomUntilNanos()) return old;
        return new State(old.entityId(), nextRevision, elapsedNanos, haste, brave, wisdom, brave == 0 ? 0 : old.braveKind(),
                haste==0?HasteOrigin.NONE:old.hasteOrigin(),slow,old.slowRevision());
    }
    static State clear(State old, long elapsedNanos, long nextRevision) {
        mutation(old, elapsedNanos, nextRevision);
        return old.hasEffects() ? new State(old.entityId(), nextRevision, elapsedNanos, 0, 0, 0, 0,HasteOrigin.NONE,0,old.slowRevision()) : old;
    }
    /**
     * Explicit current-classic policy: ActionIntervalCalculator's binary64 multipliers,
     * each truncated in milliseconds. Elf's executor ALSO sets BraveSpeed=1.
     * The original frozen base is never replaced by an already-scaled interval.
     */
    static long intervalNanos(long baseNanos, Action action, State state) {
        Objects.requireNonNull(action); Objects.requireNonNull(state);
        if (baseNanos < 1_000_000 || baseNanos > 10_000_000_000L) throw bad();
        if (state.slowUntilNanos() == 0 && state.hasteUntilNanos() == 0 && state.braveKind() == 0) return baseNanos;
        if (baseNanos % 1_000_000 != 0) throw bad();
        long millis = baseNanos / 1_000_000;
        if (state.slowUntilNanos() != 0) millis = Math.multiplyExact(millis,2);
        else if (state.hasteUntilNanos() != 0) millis = (long) (millis * 0.755);
        if (state.braveKind() != 0) millis = (long) (millis * 0.755);
        if (state.braveKind() == 3) {
            millis = (long) (millis * 0.855);
            if (action == Action.ATTACK) millis = (long) (millis * 0.9);
        }
        return Math.multiplyExact(Math.max(1, millis), 1_000_000);
    }
    /** Wisdom enters temporary item SP before the native minimum term clamp. Saved INT/SP are unchanged. */
    static long intelligenceTerm(long effectiveIntelligence, long itemSpellPower, State state) {
        Objects.requireNonNull(state);
        if (effectiveIntelligence < 0) throw bad();
        return Math.max(1, Math.subtractExact(Math.addExact(
                Math.addExact(effectiveIntelligence, itemSpellPower), state.spBonus()), 12));
    }
    private static void mutation(State old, long elapsedNanos, long nextRevision) {
        Objects.requireNonNull(old);
        if (elapsedNanos < old.observedNanos() || elapsedNanos > MAX_OBSERVED_NANOS
                || nextRevision <= old.revision()) throw bad();
    }
    private static long alive(long until, long now) { return until != 0 && now < until ? until : 0; }
    private static void lease(long until, long observed, long maximum) {
        if (until < 0 || until != 0 && (until <= observed || until - observed > maximum)) throw bad();
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern timed effect state"); }
}
