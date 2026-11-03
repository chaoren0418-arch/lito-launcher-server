package local.lito381.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Success-only Source area action for the implemented empty-self-target execution branch. */
final class NativeCloseoutNpcAreaOutput {
    record Output(GameHub hub, Object room, long roomId, NativePlayerLifetime lifetime,
                  NativeSourceNpcFirstView.View view, int map, ClientSession pc,
                  NativePlayerCommit.Binding binding, Object life, byte[] packet) {}
    // Synchronous output marker only; no Selection, target roster, cast result or actor lookup authority.
    private static final ThreadLocal<Output> output = new ThreadLocal<>();

    static boolean known(GameHub.MonsterAiTick tick, Object snapshot) {
        if (!CombatRules.loaded()) return false;
        var d = tick.magicSkill(); var stamp = NativeSourceNpcFactoryMemory.proof(snapshot);
        if (d == null || stamp == null || stamp.npc() != d.npcId()
                || !(stamp.nativeSourceNpcSpellTiming instanceof NativeSourceNpcSpellTiming.Data data)) return false;
        var route = data.routes().get(d.actNo());
        return route != null && route.type() == NativeDeNpcMobSkill.TYPE_MAGIC_ATTACK && route.skill() == d.skillId()
                && (route.width() > 0 || route.height() > 0);
    }
    static boolean finish(GameHub.MonsterAiTick tick, Object snapshot, List<ClientSession> targets, List<?> hits) {
        // A nonempty original hit plan belongs to another still-unported execution branch.
        if (!hits.isEmpty() || !known(tick, snapshot)) return false;
        var selection = targets instanceof NativeCloseoutNpcAreaTargets.Selection proof ? proof : null;
        // Completed casts retain their original classification if metadata is reloaded before output.
        if ((selection == null || NativeCloseoutNpcAreaCast.attempts(selection).isEmpty())
                && !NativeCloseoutNpcAreaCast.emptyTarget(tick.magicSkill().skillId(), NativeSkillTargetMetadata.row(tick.magicSkill().skillId()))) return false;
        if (selection == null) return true;
        int successes = NativeCloseoutNpcAreaCast.successes(selection);
        CombatAccess.set(selection, NativeCloseoutNpcAreaCast.FIELD, null);
        if (successes == 0) return true;
        var scope = selection.scope; var view = selection.view; var hub = scope.origin().hub();
        Object room; int map; List<ClientSession> viewers;
        synchronized (hub) {
            room = ((Map<?, ?>) CombatAccess.field(hub, "rooms")).get(scope.origin().room());
            Object owner = view.owner().get(), actor = view.actor().get();
            if (room == null || owner == null || actor == null || !hub.battleStarted(scope.origin().room())
                    || CombatAccess.field(scope.origin().primary(), "hub") != hub
                    || !NativeCombatAttacker.alive(scope.origin().primary(), scope.attacker())
                    || CombatAccess.field(room, view.family() == 1 ? "questEncounter" : "timeAttackEncounter") != owner) return true;
            synchronized (owner) {
                if (!NativeSourceNpcTickView.same(view, owner, actor)) return true;
                var arena = NativeCloseoutNpcAreaScene.arena(owner, actor);
                if (arena == null || arena.mapId() == 93) return true;
                map = arena.mapId();
            }
            viewers = hub.roomMembers(scope.origin().room());
        }
        int action = tick.magicSkill().sourceActionId();
        byte[] packet = Protocol.objectAction(view.id(), action > 0 ? action : 19);
        for (var pc : viewers) {
            var first = scope.origin().members().get(pc);
            if (first == null) continue;
            var out = new Output(hub, room, scope.origin().room(), scope.lifetime(), view, map,
                    pc, first.binding, first.life, packet);
            send(out);
        }
        return true;
    }
    static int force(Object room, ClientSession pc) {
        Object value = ((Map<?, ?>) CombatAccess.field(room, "forces")).get(pc);
        if (value == null) value = ((Map<?, ?>) CombatAccess.field(room, "battleForces")).get(pc);
        return value instanceof Integer n ? n : 0;
    }
    static boolean current(Output out) {
        var v = out.view(); Object owner = v.owner().get(), actor = v.actor().get();
        if (out.lifetime().mode == NativePlayerLifetime.Mode.STOPPED
                || CombatAccess.field(out.hub(), "sourcePlayerCommitLifetime") != out.lifetime()
                || (boolean) CombatAccess.field(out.hub(), "nativeStaticMotionClosed")
                || ((Map<?, ?>) CombatAccess.field(out.hub(), "rooms")).get(out.roomId()) != out.room()
                || !(boolean) CombatAccess.field(out.room(), "battleStarted") || owner == null || actor == null
                || CombatAccess.field(out.room(), v.family() == 1 ? "questEncounter" : "timeAttackEncounter") != owner
                || CombatAccess.field(owner, "sourceProcEncounterGeneration") != v.generation()
                || NativeSourceNpcFactoryType.stamp(actor) != v.stamp()
                || NativeSourceArenaPoison.rawLife(actor) != v.life()
                || (long) CombatAccess.field(actor, "objectId") != v.id()
                || NativeSourceNpcFactoryMemory.npc(actor) != v.npc()
                || v.family() == 1 && NativeSourceRawDamage.number(actor, "force") != v.force()
                || NativeSourceRawDamage.number(actor, "hp") <= 0
                || NativeSourceRawDamage.number(actor, "x") != v.x() || NativeSourceRawDamage.number(actor, "y") != v.y()) return false;
        var pc = out.pc(); var binding = out.binding();
        if (binding == null || !binding.phase().equals("BATTLE") || binding.room() != out.roomId()
                || CombatAccess.field(pc, "hub") != out.hub() || !NativeAcceptedFreezeMutation.roleFrame(binding, pc)
                || (boolean) CombatAccess.field(pc, "sourceItemDisconnected")
                || NativeSourceControlledDisplay.rawLife(pc) != out.life()
                || !((Set<?>) CombatAccess.field(out.room(), "members")).contains(pc)) return false;
        var rules = ((BattleRules.MatchState) CombatAccess.field(out.room(), "match")).rules();
        return BattleRules.arenaMapForForce(rules, force(out.room(), pc)) == out.map()
                && Math.max(Math.abs(NativeSourceRawDamage.number(pc, "positionX") - v.x()),
                            Math.abs(NativeSourceRawDamage.number(pc, "positionY") - v.y())) <= 10;
    }
    static void send(Output out) {
        if (!current(out)) return;
        var previous = output.get(); var context = NativeAreaMaterialAction.current.get();
        NativeAreaMaterialAction.current.remove(); output.set(out);
        try {
            NativePlayerCommit.completionFrame(null, () -> {
                try { synchronized (out.pc()) {
                    if (current(out)) out.pc().nativeSelfOriginalSend(out.packet().clone(), "S_ACTION Source successful NPC area cast");
                }} catch (IOException | RuntimeException failure) { NativeSourcePhysicalNpc.failed(out.lifetime()); }
            });
        } finally {
            if (previous == null) output.remove(); else output.set(previous);
            if (context == null) NativeAreaMaterialAction.current.remove(); else NativeAreaMaterialAction.current.set(context);
        }
    }
    static boolean allowFrame(boolean original, ClientSession pc, byte[] packet) {
        if (!original) return false;
        var out = output.get();
        return out == null || out.pc() == pc && current(out) && Arrays.equals(packet, out.packet());
    }
    private NativeCloseoutNpcAreaOutput() {}
}
