package local.lito381.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Bounded, private target provenance. A caster's last-per-lane receipt is
 * insufficient when two different victims still hold its previous casts.
 * NPCs may additionally retain ONE old grant for their last committed walk;
 * this is not an unbounded combat log or a second resource authority. */
final class ModernHasteTargets {
    sealed interface Key permits Player, Npc {}
    record Player(ModernTargetHistory.Participant participant) implements Key {
        Player { Objects.requireNonNull(participant); }
    }
    record Npc(ModernNpcRoster.Id id) implements Key { Npc { Objects.requireNonNull(id); } }
    record Entry(ModernSpellRuntime.Receipt active, ModernSpellRuntime.Receipt walk) {
        Entry {
            if (active == null && walk == null) throw bad();
            if (active != null) detail(active);
            if (walk != null && !(detail(walk).target() instanceof ModernHasteReceipt.Npc)) throw bad();
            if (active != null && walk != null && (!key(active).equals(key(walk))
                    || active.batch().use().revision() < walk.batch().use().revision()
                    || active.batch().use().revision() == walk.batch().use().revision() && !active.equals(walk)
                    || detail(active).target().life() < detail(walk).target().life())) throw bad();
        }
    }
    record State(ModernTargetHistory.World world, Map<Key, Entry> targets) {
        State {
            Objects.requireNonNull(world); targets = Map.copyOf(targets);
            int players = 0, npcs = 0;
            var revisions = new HashMap<Long, ModernSpellRuntime.Receipt>();
            for (var row : targets.entrySet()) {
                if (row.getKey() instanceof Player) players++; else npcs++;
                var e = row.getValue();
                for (var r : receipts(e)) {
                    if (!world.equals(r.batch().world()) || !row.getKey().equals(key(r))) throw bad();
                    var prior = revisions.putIfAbsent(r.batch().use().revision(), r);
                    if (prior != null && !prior.equals(r)) throw bad();
                }
            }
            if (players > ModernNpcRoster.MAX_PLAYERS || npcs > ModernNpcRoster.MAX_RESIDENT) throw bad();
        }
        static State empty(ModernTargetHistory.World world) { return new State(world, Map.of()); }
        State cast(ModernSpellRuntime.Receipt receipt) {
            if (!world.equals(receipt.batch().world())) throw bad();
            var key = key(receipt); var prior = targets.get(key);
            if (prior != null && receipts(prior).stream().anyMatch(r -> r.batch().use().revision() >= receipt.batch().use().revision())) throw bad();
            var next = new HashMap<>(targets);
            next.put(key, new Entry(receipt, prior == null ? null : prior.walk()));
            return new State(world, next);
        }
        /** Keep exactly the facts still required by the captured world. This
         * never synthesizes a receipt from a current duration or later cast. */
        State reconcile(long revision, ModernBattleEngine.Phase phase, ModernBattleObservation.Clock clock, long loadingOrigin,
                        List<ModernBattleObservation.CapturedActor> actors, ModernBattleObservation.Npcs npcs) {
            var next = new HashMap<Key, Entry>();
            for (var a : actors) {
                var b = a.buffs();
                if (b == null || b.hasteOrigin() != ModernTimedEffects.HasteOrigin.HASTE) continue;
                var key = new Player(new ModernTargetHistory.Participant(a.pose().entityId(), a.pose().member().identity()));
                var old = targets.get(key);
                if (phase != ModernBattleEngine.Phase.ACTIVE || a.vital().values().hp() == 0 || old == null || old.active() == null) throw bad();
                var r = old.active(); var d = detail(r); var p = (ModernHasteReceipt.Player) d.target();
                if (p.life() != Math.incrementExact(a.resources().life()) || p.equipmentRevision() > a.resources().equipmentRevision()
                        || b.hasteUntilNanos() != p.after().hasteUntilNanos() || b.revision() < p.after().revision()
                        || b.revision() > revision || b.observedNanos() < d.activeElapsedNanos()
                        || b.observedNanos() > clock.activeElapsedNanos() || b.hasteUntilNanos() <= clock.activeElapsedNanos()) throw bad();
                next.put(key, new Entry(r, null));
            }
            if (npcs != null) {
                if (!world.contains(npcs.roster().scope())) throw bad();
                for (var a : npcs.roster().actors()) {
                    var key = new Npc(a.id()); var old = targets.get(key);
                    ModernSpellRuntime.Receipt active = null, walk = null;
                    if (a.speed().hasted()) {
                        if (phase != ModernBattleEngine.Phase.ACTIVE || !a.alive() || old == null || old.active() == null
                                || !npcMatches(old.active(), a.life(), a.speed())) throw bad();
                        a.speed().at(npcs.roster().elapsedMillis()); active = old.active();
                    }
                    if (a.walkSpeed().hasted()) {
                        if (a.walk() == null || old == null) throw bad();
                        // A new step can use the current grant; a refresh or
                        // later incarnation must keep the old step's grant.
                        for (var candidate : receipts(old)) if (npcMatches(candidate, a.walk().life(), a.walkSpeed())) walk = candidate;
                        if (walk == null || a.walkSpeed().revision() >= a.walk().rosterRevision()) throw bad();
                        a.walkSpeed().at(a.walk().elapsedMillis());
                    }
                    if (active != null && ((ModernHasteReceipt.Npc)detail(active).target()).lane()!=a.lane()
                            || walk != null && ((ModernHasteReceipt.Npc)detail(walk).target()).lane()!=a.lane()) throw bad();
                    if (active != null || walk != null) next.put(key, new Entry(active, walk));
                }
            }
            var retained = new State(world, next);
            retained.validateCasters(revision, clock, loadingOrigin, actors, npcs);
            return retained.equals(this) ? this : retained;
        }
        void validate(ModernBattleObservation.Capture c) {
            validate(c.identity(), c.revision(), c.phase(), c.clock(), c.loadingOriginNanos(), c.actors(), c.npcs());
        }
        void validate(ModernTargetHistory.World identity, long revision, ModernBattleEngine.Phase phase,
                      ModernBattleObservation.Clock clock, long loadingOrigin,
                      List<ModernBattleObservation.CapturedActor> actors, ModernBattleObservation.Npcs npcs) {
            if (!world.equals(identity) || !equals(reconcile(revision, phase, clock, loadingOrigin, actors, npcs))) throw bad();
        }
        private void validateCasters(long revision, ModernBattleObservation.Clock clock, long loadingOrigin,
                                     List<ModernBattleObservation.CapturedActor> actors, ModernBattleObservation.Npcs npcs) {
            var unique = new HashMap<Long, ModernSpellRuntime.Receipt>();
            for (var entry : targets.values()) for (var r : receipts(entry)) unique.put(r.batch().use().revision(), r);
            var latestTargets = new HashMap<Key, Long>();
            for (var a : actors) if (a.runtime() != null) {
                var r = a.runtime().receipts().get(ModernUseHistory.Lane.HASTE);
                if (r != null) latestTargets.merge(key(r), r.batch().use().revision(), Math::max);
            }
            for (var row : targets.entrySet()) if (row.getValue().active() != null
                    && latestTargets.getOrDefault(row.getKey(), 0L) > row.getValue().active().batch().use().revision()) throw bad();
            for (var r : unique.values()) {
                var b = r.batch(); var u = b.use(); var d = detail(r); long entity = b.caster().entityId();
                if (entity > actors.size()) throw bad(); var caster = actors.get(Math.toIntExact(entity - 1));
                var runtime = caster.runtime(); var lane = caster.histories().get(ModernUseHistory.Lane.HASTE);
                if (!b.caster().identity().equals(caster.pose().member().identity()) || !b.source().equals(caster.scope())
                        || runtime == null || lane == null || !runtime.identity().world().equals(world)
                        || u.revision() > revision || u.before().life() > Math.incrementExact(caster.resources().life())
                        || u.before().equipmentRevision() > caster.resources().equipmentRevision()
                        || r.atNanos() - loadingOrigin < 0 || r.atNanos() - loadingOrigin > clock.loadingElapsedNanos()
                        || d.activeElapsedNanos() > clock.activeElapsedNanos()
                        || r.atNanos() - loadingOrigin - d.activeElapsedNanos() != clock.loadingElapsedNanos() - clock.activeElapsedNanos()) throw bad();
                var latest = runtime.receipts().get(ModernUseHistory.Lane.HASTE);
                if (latest == null || !latest.batch().use().equals(lane.last()) || latest.casts() != lane.current().casts()
                        || latest.spent() != lane.current().spent()) throw bad();
                follows(r, latest);
                if (npcs != null && npcs.roster().isolatedForces()) {
                    int targetTeam;
                    if (d.target() instanceof ModernHasteReceipt.Player p) targetTeam = actors.get(Math.toIntExact(p.participant().entityId() - 1)).pose().member().team();
                    else { var id = ((ModernHasteReceipt.Npc) d.target()).id();
                        targetTeam = npcs.roster().actors().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow(ModernHasteTargets::bad).lane(); }
                    if (targetTeam != caster.pose().member().team()) throw bad();
                }
            }
            // Independent victims can retain several events by one caster.
            // Their counts, gear/lives, clock and spending must be coherent too.
            var ordered = unique.values().stream().sorted(java.util.Comparator.comparingLong(r -> r.batch().use().revision())).toList();
            var prior = new HashMap<ModernTargetHistory.Participant, ModernSpellRuntime.Receipt>();
            for (var r : ordered) { var before = prior.put(r.batch().caster(), r); if (before != null) follows(before, r); }
        }
        @Override public String toString() { return "HasteTargets[redacted]"; }
    }
    private static void follows(ModernSpellRuntime.Receipt before, ModernSpellRuntime.Receipt after) {
        long count = after.casts() - before.casts(), spent = after.spent() - before.spent();
        if (count < 0 || spent < 0 || count == 0 && !before.equals(after)) throw bad();
        if (count > 0 && (!before.batch().world().equals(after.batch().world()) || !before.batch().source().equals(after.batch().source())
                || !before.batch().caster().equals(after.batch().caster()) || before.batch().use().revision() >= after.batch().use().revision()
                || before.batch().use().sequence() >= after.batch().use().sequence() || after.atNanos() - before.atNanos() < 0
                || before.batch().use().before().life() > after.batch().use().before().life()
                || before.batch().use().before().equipmentRevision() > after.batch().use().before().equipmentRevision()
                || spent < Math.multiplyExact(count, 15) || spent > Math.multiplyExact(count, ModernHaste.BASE_MP))) throw bad();
    }
    private static boolean npcMatches(ModernSpellRuntime.Receipt r, long life, ModernNpcSpeed.State speed) {
        return detail(r).target() instanceof ModernHasteReceipt.Npc n && n.life() == life && n.after().equals(speed);
    }
    static Key key(ModernSpellRuntime.Receipt r) {
        var target = detail(r).target();
        return target instanceof ModernHasteReceipt.Player p ? new Player(p.participant()) : new Npc(((ModernHasteReceipt.Npc) target).id());
    }
    private static ModernHasteReceipt detail(ModernSpellRuntime.Receipt r) {
        if (r.batch().lane() != ModernUseHistory.Lane.HASTE || !(r.detail() instanceof ModernHasteReceipt h)) throw bad();
        return h;
    }
    private static List<ModernSpellRuntime.Receipt> receipts(Entry e) {
        if (e.active() == null) return List.of(e.walk());
        if (e.walk() == null || e.walk().equals(e.active())) return List.of(e.active());
        return List.of(e.active(), e.walk());
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid haste target provenance"); }
    private ModernHasteTargets() {}
}
