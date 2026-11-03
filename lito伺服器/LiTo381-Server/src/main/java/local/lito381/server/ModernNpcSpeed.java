package local.lito381.server;

import java.util.Objects;

/** Match-owned target speed leases, independent of frozen source templates.
 * Slow candidates grant no caster authorization and are not on the old wire. */
final class ModernNpcSpeed {
    static final long HASTE_MILLIS = ModernHaste.DURATION_SECONDS * 1000L;
    static final long SLOW_MILLIS = ModernSlow.DURATION_SECONDS * 1000L;
    record State(long revision, long observedMillis, long hasteUntilMillis, long slowUntilMillis, long slowRevision) {
        State(long revision, long observedMillis, long hasteUntilMillis) {
            this(revision, observedMillis, hasteUntilMillis, 0, 0);
        }
        State {
            if (revision < 0 || observedMillis < 0 || observedMillis > ModernNpcRoster.MAX_ELAPSED_MILLIS
                    || slowRevision < 0 || slowRevision > revision
                    || slowUntilMillis != 0 && (slowRevision == 0 || hasteUntilMillis != 0)
                    || revision == 0 && (observedMillis != 0 || hasteUntilMillis != 0)) throw bad();
            lease(hasteUntilMillis, observedMillis, HASTE_MILLIS);
            lease(slowUntilMillis, observedMillis, SLOW_MILLIS);
        }
        boolean hasted() { return hasteUntilMillis != 0; }
        boolean slowed() { return slowUntilMillis != 0; }
        boolean hasEffects() { return hasted() || slowed(); }
        void at(long elapsedMillis) {
            if (elapsedMillis < observedMillis || elapsedMillis > ModernNpcRoster.MAX_ELAPSED_MILLIS
                    || hasted() && elapsedMillis >= hasteUntilMillis
                    || slowed() && elapsedMillis >= slowUntilMillis) throw bad();
        }
    }
    static State initial() { return new State(0, 0, 0); }
    /** Native spell haste cancels live Slow without leaving either lease. */
    static State haste(State before, long elapsedMillis, long nextRevision) {
        mutation(before, elapsedMillis, nextRevision);
        return new State(nextRevision, elapsedMillis,
                alive(before.slowUntilMillis(), elapsedMillis) ? 0 : Math.addExact(elapsedMillis, HASTE_MILLIS),
                0, before.slowRevision());
    }
    /** Native Slow cancels live haste; a second admitted cast can then slow. */
    static State slow(State before, long elapsedMillis, long nextRevision) {
        mutation(before, elapsedMillis, nextRevision);
        return new State(nextRevision, elapsedMillis, 0,
                alive(before.hasteUntilMillis(), elapsedMillis) ? 0 : Math.addExact(elapsedMillis, SLOW_MILLIS),
                nextRevision);
    }
    static State expire(State before, long elapsedMillis, long nextRevision) {
        mutation(before, elapsedMillis, nextRevision);
        return before.hasted() && elapsedMillis >= before.hasteUntilMillis()
                || before.slowed() && elapsedMillis >= before.slowUntilMillis()
                ? new State(nextRevision, elapsedMillis, 0, 0, before.slowRevision()) : before;
    }
    static State clear(State before, long elapsedMillis, long nextRevision) {
        mutation(before, elapsedMillis, nextRevision);
        return before.hasEffects() ? new State(nextRevision, elapsedMillis, 0, 0, before.slowRevision()) : before;
    }
    /** The qualified template remains the base. Never feed a scaled pace back
     * into it or rewrite a previously scheduled action. */
    static ModernNpcMotion.Pace pace(ModernNpcMotion.Pace base, State state) {
        Objects.requireNonNull(base); Objects.requireNonNull(state);
        requireBase(base);
        return state.slowed() ? new ModernNpcMotion.Pace(Math.multiplyExact(base.walkMillis(), 2), Math.multiplyExact(base.attackMillis(), 2))
                : state.hasted() ? new ModernNpcMotion.Pace(scale(base.walkMillis()), scale(base.attackMillis())) : base;
    }
    static void requireBase(ModernNpcMotion.Pace base) {
        if (base.walkMillis() > 60_000 || base.attackMillis() > 60_000) throw bad();
    }
    static void requireNoSlow(State state) {
        if (Objects.requireNonNull(state).slowRevision() != 0) throw bad();
    }
    private static int scale(int millis) { return Math.max(1, (int) (millis * 0.755d)); }
    private static boolean alive(long until, long now) { return until != 0 && now < until; }
    private static void lease(long until, long now, long maximum) {
        if (until < 0 || until != 0 && (until <= now || until - now > maximum)) throw bad();
    }
    private static void mutation(State before, long elapsedMillis, long nextRevision) {
        Objects.requireNonNull(before);
        if (elapsedMillis < before.observedMillis() || elapsedMillis > ModernNpcRoster.MAX_ELAPSED_MILLIS
                || nextRevision <= before.revision()) throw bad();
    }
    /** Existing public projections cannot silently discard even an expired
     * speed mutation or a past changed step. A new contract is required. */
    static void requireFrozen(ModernNpcRoster.Actor actor) {
        if (actor.speed().revision() != 0 || actor.walkSpeed().revision() != 0) throw bad();
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern NPC speed state"); }
    private ModernNpcSpeed() {}
}
