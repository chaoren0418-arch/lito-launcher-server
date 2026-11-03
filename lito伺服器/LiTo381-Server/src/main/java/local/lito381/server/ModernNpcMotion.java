package local.lito381.server;

import java.util.Objects;
import java.util.List;

/** Bounded presentation facts, never a movement proposal or source catalog.
 * Times are roster-relative milliseconds, not a raw System.nanoTime value.
 * Wire schemas 1-3 intentionally omit these v43/schema4 facts. */
final class ModernNpcMotion {
    static final int VERSION = 43;
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    record Pace(int walkMillis, int attackMillis) {
        Pace {
            if (walkMillis < 1 || walkMillis > 120_000 || attackMillis < 1 || attackMillis > 120_000) throw bad();
        }
        static Pace from(ModernNpcRoster.Template template) {
            return new Pace(template.walkMillis(), template.attackMillis());
        }
    }

    /** Public presentation facts, separately encoded by schema4 only. */
    record View(ModernNpcRoster.Id actor, long life, Pace pace, Walk walk) {
        View {
            Objects.requireNonNull(actor); Objects.requireNonNull(pace);
            ModernNpcSpeed.requireBase(pace); // Frozen schema4 cannot represent a slowed interval.
            if (life < 1 || walk != null && (!actor.equals(walk.actor()) || walk.life() > life
                    || pace.walkMillis() != walk.durationMillis())) throw bad();
        }
        static View from(ModernNpcRoster.Actor actor) {
            ModernNpcSpeed.requireFrozen(actor);
            return new View(actor.id(), actor.life(), Pace.from(actor.template()), actor.walk());
        }
    }
    static List<View> project(ModernNpcRoster.Snapshot snapshot, int lane) {
        Objects.requireNonNull(snapshot);
        if (lane < -1 || lane > 1 || snapshot.isolatedForces() != (lane >= 0)) throw bad();
        return snapshot.actors().stream().filter(a -> lane < 0 || a.lane() == lane).map(View::from).toList();
    }

    record State(ModernNpcRoster.Scope scope, long revision, long elapsedMillis, List<View> actors) {
        State { Objects.requireNonNull(scope); actors = List.copyOf(actors); }
    }
    static State state(ModernNpcRoster.Snapshot roster, int lane) {
        return new State(roster.scope(),roster.revision(),roster.elapsedMillis(),project(roster,lane));
    }
    /** A single publication and its exact selected audience. No caller can
     * splice another capture's movement into an otherwise valid owner view.
     * v43 BATTLE_DATA selects this complete extension; older versions omit it. */
    static final class Payload {
        private final ModernBattleAudience.View audience;
        private final State state;
        private Payload(ModernBattleAudience.View view) {
            audience = Objects.requireNonNull(view);
            state = view.npcMotion();
        }
        static Payload select(ModernBattlePublication publication, ModernRooms.Scope owner) {
            return new Payload(Objects.requireNonNull(publication).audience(owner));
        }
        static Payload selected(ModernBattleAudience.View view) { return new Payload(view); }
        ModernBattleAudience.View audience() { return audience; }
        State state() { return state; }
        @Override public String toString() { return "NpcMotionPayload[redacted]"; }
    }

    /** Only the latest committed step is retained. Sequence spans NPC lives;
     * life identifies the incarnation that actually performed this step. */
    record Walk(ModernNpcRoster.Id actor, long sequence, long life, long rosterRevision,
                long elapsedMillis, int durationMillis, ModernWorldMap.Cell from,
                ModernWorldMap.Cell to, int heading) {
        Walk {
            Objects.requireNonNull(actor); Objects.requireNonNull(from); Objects.requireNonNull(to);
            if (sequence < 1 || life < 1 || rosterRevision < 2 || sequence >= rosterRevision
                    || elapsedMillis < 0 || elapsedMillis > ModernNpcRoster.MAX_ELAPSED_MILLIS
                    || durationMillis < 1 || durationMillis > 120_000 || heading < 0 || heading > 7
                    || (long) to.x() - from.x() != DX[heading] || (long) to.y() - from.y() != DY[heading]) throw bad();
        }
        static Walk next(ModernNpcRoster.Actor before, long revision, long elapsedMillis,
                         ModernWorldMap.Cell to, int heading) {
            return new Walk(before.id(), before.walk() == null ? 1 : Math.incrementExact(before.walk().sequence()),
                    before.life(), revision, elapsedMillis, before.pace().walkMillis(), before.cell(), to, heading);
        }
        void validate(ModernNpcRoster.Id id, Pace pace, long actorLife, long bornRevision,
                      long changedRevision, ModernWorldMap.Cell currentCell) {
            if (!actor.equals(id) || life > actorLife || rosterRevision <= bornRevision
                    || rosterRevision > changedRevision || sequence > rosterRevision - bornRevision
                    || durationMillis != pace.walkMillis() || !to.equals(currentCell)) throw bad();
        }
    }

    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid NPC motion facts"); }
    private ModernNpcMotion() {}
}
