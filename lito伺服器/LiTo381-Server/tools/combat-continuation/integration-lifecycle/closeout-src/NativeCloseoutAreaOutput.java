package local.lito381.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/** Independent PC/NPC outputs retain only first participants, never the Aim/Context. */
final class NativeCloseoutAreaOutput {
    record Npc(NativeSourceWeaponFinalFrame.Npc identity,
               NativeSourceWeaponFinalFrame.Participant owner, int x, int y,
               NativeCompanionPlacement.State placement, Object pose, Object scene, Object master,
               Object profile, Object factory, int templateNpc) {}

    static final class Scope {
        final NativeSourceSelfFinalFrame.First first;
        final NativeSourceWeaponFinalFrame.Participant source;
        final Map<ClientSession, NativeSourceSelfFinalFrame.Participant> players = new IdentityHashMap<>();
        final Map<Long, NativeSourceWeaponFinalFrame.Participant> addressed = new HashMap<>();
        final Map<Long, Npc> npcs = new HashMap<>();
        final Map<Object, NativeSourceSelfFinalFrame.Participant> mutationPlayers = new IdentityHashMap<>();
        final Map<Object, Npc> mutationNpcs = new IdentityHashMap<>();
        final Map<Object, String> mutationFields = new IdentityHashMap<>();

        Scope(NativeAreaMaterialAction.Context c, NativeSourceAreaPairFirst.First value) {
            first = value.source();
            var proof = value.proof();
            source = proof == null ? null : NativeSourceWeaponFinalFrame.participant(proof.source(), proof.sourceTimed(), false, true);
            if (!(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot)) return;
            for (var entry : snapshot.members().entrySet()) {
                var pc = entry.getKey(); var member = entry.getValue(); Object timed = value.playerStatuses().get(pc);
                if (timed == null || member.nativeSourceNpcHpObserver == null || !member.binding.equals(c.bindings.get(pc))) continue;
                players.put(pc, new NativeSourceSelfFinalFrame.Participant(pc, member.binding, timed, member.acceptedLife, member.nativeSourceNpcHpObserver, member.dead));
                addressed.put(member.binding.object(), NativeSourceWeaponFinalFrame.participant(member, timed, member.nativeSourceNpcHpObserver, true));
                mutationPlayers.put(timed, players.get(pc));
                mutationFields.put(timed, "timedStatus");
                for (var field : new String[]{"eraseMagicStatus", "windShackleStatus"}) {
                    Object status = CombatAccess.field(pc, field);
                    if (status != null) { mutationPlayers.put(status, players.get(pc)); mutationFields.put(status, field); }
                }
            }
            for (var n : c.npcs.values()) {
                if (n.kind() < 1 || n.kind() > 3) continue;
                Object timed = NativeSourceTargetNegativeLife.timed(n.actor(), n.kind() != 3);
                Object life = NativeSourceNpcFirstLife.first(n.actor(), n, c);
                if (timed == null || life == null || NativeSourceTargetNegativeLife.raw(timed) != life) continue;
                NativeSourceWeaponFinalFrame.Participant owner = null;
                NativeCompanionPlacement.State placement = null;
                Object generation = null;
                if (n.kind() == 3) {
                    if (!(n.owner() instanceof ClientSession pc) || !players.containsKey(pc)) continue;
                    var member = snapshot.members().get(pc);
                    owner = NativeSourceWeaponFinalFrame.participant(member, value.playerStatuses().get(pc), false, false);
                    placement = NativeCompanionPlacement.state((BattleCompanion) n.actor());
                } else generation = CombatAccess.field(n.owner(), "sourceProcEncounterGeneration");
                var identity = new NativeSourceWeaponFinalFrame.Npc(n.kind(), n.owner(), n.actor(), timed, life, generation);
                var target = new Npc(identity, owner, NativeSourceRawDamage.number(n.actor(), "x"), NativeSourceRawDamage.number(n.actor(), "y"),
                        placement, placement == null ? null : placement.pose, placement == null ? null : placement.scene, placement == null ? null : placement.master,
                        profile(n.actor(), n.kind()), NativeSourceNpcFactoryType.stamp(n.actor()), NativeSourceNpcFactoryMemory.npc(n.actor()));
                if (current(target, c.hub)) {
                    npcs.put(n.id(), target);
                    if (n.kind() != 3) {
                        Object erase = CombatAccess.field(n.actor(), "eraseMagicStatus");
                        if (erase != null) { mutationNpcs.put(erase, target); mutationFields.put(erase, "eraseMagicStatus"); }
                    }
                }
            }
        }
        void clear() { players.clear(); addressed.clear(); npcs.clear(); mutationPlayers.clear(); mutationNpcs.clear(); mutationFields.clear(); }
    }

    record Value(NativeSourceSelfFinalFrame.First first,
                 NativeSourceWeaponFinalFrame.Participant source,
                 NativeSourceSelfFinalFrame.Participant receiver,
                 NativeSourceWeaponFinalFrame.Participant addressed,
                 Npc npc, boolean ownImpact, byte[] packet, String note) {
        Value { packet = packet.clone(); }
    }

    static Scope scope(NativeAreaMaterialAction.Context c) {
        return c != null && c.nativeCloseoutAreaOutput instanceof Scope s ? s : null;
    }
    static void install(NativeAreaMaterialAction.Context c) {
        if (c.nativeSourceAreaPairFirst instanceof NativeSourceAreaPairFirst.First first && "BATTLE".equals(c.phase))
            c.nativeCloseoutAreaOutput = new Scope(c, first);
    }
    static void install(NativeSourceMassSlowTargets.Aim a) { install(a.c); }
    static void install(NativeSourceWindTargets.Aim a) { install(a.c); }
    static void close(NativeAreaMaterialAction.Context c) {
        var s = scope(c); if (s != null) s.clear(); c.nativeCloseoutAreaOutput = null;
    }
    static void close(NativeSourceMassSlowTargets.Aim a) { close(a.c); }
    static void close(NativeSourceWindTargets.Aim a) { close(a.c); }
    static boolean register(NativeSourceMassSlowTargets.Aim a) {
        if (scope(a.c) == null) return false; a.registered = true; return true;
    }
    static boolean register(NativeSourceWindTargets.Aim a) {
        if (scope(a.c) == null) return false; a.registered = true; return true;
    }

    static Object profile(Object actor, int kind) {
        return kind == 1 ? CombatAccess.field(actor, "template") : kind == 3 ? CombatAccess.field(actor, "profile") : null;
    }
    static boolean current(Npc n, GameHub hub) {
        if (n == null || n.owner() != null && !NativeSourceWeaponFinalFrame.member(n.owner(), hub)
                || !NativeSourceWeaponFinalFrame.npc(n.identity(), n.owner())) return false;
        Object actor = n.identity().actor();
        if (profile(actor, n.identity().kind()) != n.profile() || NativeSourceNpcFactoryType.stamp(actor) != n.factory()
                || NativeSourceNpcFactoryMemory.npc(actor) != n.templateNpc()) return false;
        if (NativeSourceRawDamage.number(actor, "x") != n.x() || NativeSourceRawDamage.number(actor, "y") != n.y()) return false;
        if (n.identity().kind() != 3) return true;
        var placement = NativeCompanionPlacement.state((BattleCompanion) actor);
        return placement == n.placement() && (placement == null || placement.owner.get() == n.owner().pc()
                && placement.pose == n.pose() && placement.scene == n.scene() && placement.master == n.master());
    }

    static boolean npc(boolean original, NativeAreaMaterialAction.Context c, long id) {
        if (!original) return false;
        var s = scope(c); return s == null || current(s.npcs.get(id), c.hub);
    }
    static Iterator<?> iterator(Iterator<?> original) {
        var c = NativeAreaMaterialAction.current.get(); var s = scope(c);
        return s == null ? original : new Cursor(original, s);
    }
    static final class Cursor implements Iterator<Object> {
        final Iterator<?> original; final Scope scope; Object next; boolean ready;
        Cursor(Iterator<?> original, Scope scope) { this.original = original; this.scope = scope; }
        boolean current(Object raw) {
            if (!NativeSourceSelfFinalFrame.current(scope.first)) return false;
            Object node = NativeSourceAreaClassSelection.node(raw);
            if (node instanceof ClientSession pc) {
                var participant = scope.players.get(pc);
                return participant != null && NativeSourceSelfFinalFrame.member(participant, scope.first.hub())
                        && NativeSourceWeaponFinalFrame.member(scope.addressed.get(participant.binding().object()), scope.first.hub());
            }
            Long id = null;
            if (node instanceof BattleQuestEncounter.Snapshot s) id = s.objectId();
            else if (node instanceof BattleTimeAttackEncounter.Snapshot s) id = s.objectId();
            else if (node instanceof BattleCompanion.Snapshot s) id = s.objectId();
            else if (node instanceof BattleCompanion pet) id = (Long) CombatAccess.field(pet, "objectId");
            return id == null || NativeCloseoutAreaOutput.current(scope.npcs.get(id), scope.first.hub());
        }
        public boolean hasNext() {
            while (!ready && original.hasNext()) { var value = original.next(); if (current(value)) { next = value; ready = true; } }
            return ready;
        }
        public Object next() { if (!hasNext()) throw new NoSuchElementException(); var value = next; next = null; ready = false; return value; }
    }

    static boolean capture(ClientSession receiver, byte[] bytes, String note) {
        var c = NativeAreaMaterialAction.current.get();
        var scope = scope(c);
        if (scope == null
                || !"BATTLE".equals(c.phase)) return false;
        var slow = NativeSourceMassSlowTargets.aim();
        var wind = NativeSourceWindTargets.aim();
        if (slow != null && slow.outbound != null || wind != null && wind.outbound != null) return false;
        // Actor packets name their own affected entity. Other status/message packets
        // belong to their actual recipient, never to an unrelated selected primary.
        int op = bytes.length == 0 ? -1 : bytes[0] & 255;
        boolean ownImpact = receiver == c.pc && (op == Protocol.S_MANA_POINT
                || op == Protocol.S_HIT_POINT || op == Protocol.S_CHANGE_DIRECTION);
        NativeSourceWeaponFinalFrame.Participant addressed = null;
        Npc npc = null;
        if (!ownImpact && bytes.length >= 5
                && Set.of(Protocol.S_EFFECT, Protocol.S_SPEED, Protocol.S_ACTION, Protocol.S_POISON).contains(op)) {
            long id = NativeStaticNpcMotion.id(bytes, 1);
            addressed = scope.addressed.get(id); npc = scope.npcs.get(id);
            if (addressed == null && npc == null) return true;
        } else if (!ownImpact && receiver != c.pc) {
            var binding = c.bindings.get(receiver);
            if (binding == null) return true;
            addressed = scope.addressed.get(binding.object());
            if (addressed == null) return true;
        }
        var audience = scope.players.get(receiver);
        var value = new Value(scope.first, scope.source, audience, addressed, npc, ownImpact, bytes, note);
        if (current(value)) NativeLightAction.deferScoped(() -> send(value));
        return true;
    }

    static boolean current(Value value) {
        return value != null && NativeSourceSelfFinalFrame.current(value.first())
                && (value.ownImpact() || NativeSourceWeaponFinalFrame.member(value.source(), value.first().hub()))
                && NativeSourceSelfFinalFrame.member(value.receiver(), value.first().hub())
                && (value.addressed() == null || NativeSourceWeaponFinalFrame.member(value.addressed(), value.first().hub()))
                && (value.npc() == null || current(value.npc(), value.first().hub()));
    }

    static void send(Value value) throws IOException {
        if (current(value)) value.receiver().pc().nativeSourceWeaponSelfSend(value.packet(), value.note(), value);
    }

    static boolean kind(Object value) { return value instanceof Value; }

    static boolean frame(ClientSession pc, byte[] bytes, String note, Object value) throws IOException {
        if (!(value instanceof Value v)) return false;
        if (v.receiver().pc() == pc) {
            var actual = new Value(v.first(), v.source(), v.receiver(), v.addressed(), v.npc(), v.ownImpact(), bytes, note);
            pc.nativeSourceWeaponStaticFrame(actual.packet(), actual.note(), actual);
        }
        return true;
    }

    static boolean allow(ClientSession pc, byte[] bytes, Object value) {
        if (!Thread.holdsLock(pc)) throw new IllegalStateException("area output requires original receiver monitor");
        return value instanceof Value v && v.receiver().pc() == pc && Arrays.equals(bytes, v.packet()) && current(v);
    }

    static void release(NativeCompanionTargets.Scope scope, Throwable primary, Throwable first) throws IOException {
        Throwable failure = first;
        try { scope.close(); } catch (RuntimeException | Error e) { failure = NativeSourceNegativeFinalFrame.collect(failure, e); }
        try {
            for (var receipt : new ArrayList<>(scope.receipts.values())) if (!receipt.released.get()) {
                try { receipt.close(); } catch (RuntimeException | Error e) { failure = NativeSourceNegativeFinalFrame.collect(failure, e); }
            }
        } finally { scope.receipts.clear(); scope.bindings.clear(); scope.caster.clear(); }
        if (failure != null) {
            if (primary != null) { NativeSourceNegativeFinalFrame.collect(primary, failure); return; }
            if (failure instanceof RuntimeException e) throw e;
            if (failure instanceof Error e) throw e;
            throw new IOException(failure);
        }
    }
    static void produce(NativeSourceMassSlowTargets.Aim a, NativeAreaMaterialAction.Context c, NativeCompanionTargets.Scope scope,
                        boolean world, ClientSession pc, byte[] packet, BattleSpellCatalog.MassSlowSpell spell) throws IOException {
        boolean scoped = scope(c) != null; Throwable primary = null;
        try { NativeSourceMassSlowTargets.nativeCloseoutAreaOriginalProducer(a, c, scope, world, pc, packet, spell); }
        catch (IOException | RuntimeException | Error e) { primary = e; throw e; }
        finally { if (scoped) { Throwable failure = null; try { a.close(); } catch (RuntimeException | Error e) { failure = e; } release(scope, primary, failure); } }
    }
    static void produce(NativeSourceWindTargets.Aim a, NativeAreaMaterialAction.Context c, NativeCompanionTargets.Scope scope,
                        ClientSession pc, byte[] packet, BattleSpellCatalog.WindShackleSpell spell, int fixture) throws IOException {
        boolean scoped = scope(c) != null; Throwable primary = null;
        try { NativeSourceWindTargets.nativeCloseoutAreaOriginalProducer(a, c, scope, pc, packet, spell, fixture); }
        catch (IOException | RuntimeException | Error e) { primary = e; throw e; }
        finally { if (scoped) { Throwable failure = null; try { a.close(); } catch (RuntimeException | Error e) { failure = e; } release(scope, primary, failure); } }
    }

    private NativeCloseoutAreaOutput() {}
}
