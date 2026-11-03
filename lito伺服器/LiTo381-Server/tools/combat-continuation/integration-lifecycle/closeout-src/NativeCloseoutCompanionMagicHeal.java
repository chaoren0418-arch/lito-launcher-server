package local.lito381.server;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/** Actual controlled caster of an accepted NPC drain event; never substitutes the master. */
final class NativeCloseoutCompanionMagicHeal {
    record Proof(GameHub hub, Object room, NativePlayerLifetime lifetime, ClientSession owner,
                 NativePlayerCommit.Binding binding, Object ownerLife, WeakReference<List<BattleCompanion>> bag,
                 BattleCompanion pet, Object life, BattleCompanion.Profile profile,
                 NativeSourceNpcFactoryType.Stamp factory, NativeCompanionPlacement.State placement,
                 Object scene, Object master, long id) {}
    // Output retains no target roster, NPC scope, plan or damage frame.
    record Output(Proof proof, int ratio) {}
    static final ThreadLocal<Output> output = new ThreadLocal<>();

    static boolean controlled(NativeSourceNpcPlayer.Scope scope) { return scope.attacker().kind() == 2; }
    static boolean handles(NativeSourceNpcMagicEffects.Source source) { return source != null && source.kind() == 2; }
    static boolean ownerCurrent(Proof p) {
        return p.lifetime().mode != NativePlayerLifetime.Mode.STOPPED
                && !(boolean) CombatAccess.field(p.hub(), "nativeStaticMotionClosed")
                && CombatAccess.field(p.owner(), "hub") == p.hub()
                && p.binding().phase().equals("BATTLE")
                && p.room() != null
                && NativeAcceptedFreezeMutation.roleFrame(p.binding(), p.owner())
                && !(boolean) CombatAccess.field(p.owner(), "sourceItemDisconnected")
                && !(boolean) CombatAccess.field(p.owner(), "observer")
                && NativeSourceControlledDisplay.rawLife(p.owner()) == p.ownerLife();
    }
    static boolean current(Proof p) {
        return ownerCurrent(p) && NativeCloseoutCompanionRecovery.petCurrent(p.pet(), p.life(), p.profile(),
                p.factory(), p.placement(), p.scene(), p.master(), p.owner());
    }
    static boolean currentSource(NativeSourceNpcMagicEffects.Source source) {
        return source.generation() instanceof Proof p && source.actor() == p.pet() && source.owner() == p.owner()
                && source.hub() == p.hub() && source.lifetime() == p.lifetime() && source.life() == p.life()
                && source.id() == p.id() && source.room() == p.binding().room() && current(p);
    }
    static NativeSourceNpcMagicEffects.Source capture(NativeSourceNpcPlayer.Scope scope) {
        var ref = scope.attacker();
        if (!(ref.nativeSourceNpcTickView instanceof NativeSourceCompanionFirstActor.View view)
                || !(ref.nativeSourceNpcMasterProof instanceof NativeSourceNpcOrigins.Master master)) return null;
        var owner = master.pc().get(); var pet = view.actor().get(); var hub = scope.origin().hub();
        if (owner == null || pet == null || owner != scope.origin().primary() || ref.master() == null
                || !ref.master().equals(master.binding()) || !ref.master().phase().equals("BATTLE")
                || ref.room() != scope.origin().room() || ref.room() != ref.master().room()
                || ref.id() != view.id() || ref.owner() != view.life()) return null;
        // Capture only: no owner action lock is acquired while the target's admission lock is held.
        synchronized (hub) {
            var room = ((Map<?, ?>) CombatAccess.field(hub, "rooms")).get(ref.room());
            var bag = NativeBattleTamedLifecycle.pets(owner);
            synchronized (bag) { synchronized (pet) { synchronized ((BattleTimedStatus) CombatAccess.field(pet, "timedStatus")) {
                var factory = view.stamp(); var profile = view.profile().get();
                if (factory == null || (factory.kind() != 5 && factory.kind() != 6)
                        || !(profile instanceof BattleCompanion.Profile actual) || actual.maxHp() <= 0
                        || !bag.contains(pet) || !NativeSourceCompanionFirstActor.same(view, pet)) return null;
                var placement = NativeCompanionPlacement.state(pet);
                var p = new Proof(hub, room, scope.lifetime(), owner, ref.master(), master.life(), new WeakReference<>(bag),
                        pet, view.life(), actual, factory, placement, placement == null ? null : placement.scene,
                        placement == null ? null : placement.master, view.id());
                if (!current(p)) return null;
                return new NativeSourceNpcMagicEffects.Source(hub, ref.room(), owner, pet, view.life(), p,
                        view.id(), 2, scope.lifetime(), actual.maxHp());
            }}}
        }
    }
    static void heal(NativeSourceNpcMagicEffects.Source source, int amount) {
        if (amount <= 0 || !(source.generation() instanceof Proof p)) return;
        Output[] result = {null};
        // The original accepted HP completion runs after its action/vital locks release.
        NativeSourcePlayerActionLock.runPairOrdered(p.owner(), p.owner(), () -> {
            synchronized (p.lifetime().admission) { synchronized (p.hub()) {
                if (!currentSource(source) || !p.hub().connectedSessionsSnapshot().contains(p.owner())
                        || ((Map<?, ?>) CombatAccess.field(p.hub(), "rooms")).get(p.binding().room()) != p.room()) return;
                var bag = p.bag().get();
                if (bag == null || NativeBattleTamedLifecycle.pets(p.owner()) != bag) return;
                synchronized (bag) { synchronized (p.pet()) { synchronized ((BattleTimedStatus) CombatAccess.field(p.pet(), "timedStatus")) {
                    if (!bag.contains(p.pet()) || !currentSource(source)) return;
                    int hp = NativeSourceRawDamage.number(p.pet(), "hp");
                    int next = NativeSourceNpcMagicEffects.capped(p.profile().maxHp(), hp, amount);
                    if (next <= hp) return;
                    CombatAccess.set(p.pet(), "hp", next);
                    result[0] = new Output(p, NativeCloseoutCompanionRecovery.ratio(next, p.profile().maxHp()));
                }}}
            }}
        });
        send(result[0]);
    }
    static boolean current(Output out) {
        var p = out.proof();
        return current(p) && NativeCloseoutCompanionRecovery.ratio(NativeSourceRawDamage.number(p.pet(), "hp"),
                p.profile().maxHp()) == out.ratio();
    }
    static void send(Output out) {
        if (out == null || !current(out)) return;
        var previous = output.get(); var context = NativeAreaMaterialAction.current.get();
        NativeAreaMaterialAction.current.remove(); output.set(out);
        try { NativePlayerCommit.completionFrame(null, () -> {
            try { synchronized (out.proof().owner()) {
                if (current(out)) out.proof().owner().nativeSelfOriginalSend(Protocol.hitRatio(out.proof().id(), out.ratio(), 100),
                        "S_HIT_RATIO Source accepted companion drain heal");
            }} catch (IOException | RuntimeException failure) { NativeSourcePhysicalNpc.failed(out.proof().lifetime()); }
        }); }
        finally {
            if (previous == null) output.remove(); else output.set(previous);
            if (context == null) NativeAreaMaterialAction.current.remove(); else NativeAreaMaterialAction.current.set(context);
        }
    }
    static boolean allowFrame(boolean original, ClientSession pc, byte[] packet) {
        if (!original) return false;
        var out = output.get();
        return out == null || out.proof().owner() == pc && current(out) && packet != null && packet.length == 6
                && (packet[0] & 255) == Protocol.S_HIT_RATIO && NativeSourcePhysicalNpc.object(packet) == out.proof().id()
                && (packet[5] & 255) == out.ratio();
    }
    private NativeCloseoutCompanionMagicHeal() {}
}
