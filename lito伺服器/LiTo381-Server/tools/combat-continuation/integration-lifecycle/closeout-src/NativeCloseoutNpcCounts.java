package local.lito381.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Source count facts at birth, original actor-owned counts, and proven successful-cast/AI-finish consumers. */
final class NativeCloseoutNpcCounts {
    static final String FIELD = "nativeCloseoutNpcCountLimits";
    static final String COUNTED = "nativeCloseoutNpcCounted";
    static Object freeze(Object timing, NativeDeNpcTemplateResources.Snapshot snapshot, NativeDeNpcTemplate npc) {
        if (!(timing instanceof NativeSourceNpcSpellTiming.Data) || snapshot == null || npc == null) return timing;
        var limits = new HashMap<Integer, Integer>();
        var rows = snapshot.mobSkills() == null ? null : snapshot.mobSkills().get(npc.get_npcId());
        if (rows != null && rows.get_mobid() == npc.get_npcId())
            for (int i = 0; i < rows.getSkillSize(); i++) limits.put(i, rows.getTriggerCount(i));
        CombatAccess.set(timing, FIELD, Map.copyOf(limits));
        return timing;
    }
    @SuppressWarnings("unchecked")
    static Map<Integer, Integer> counts(Object actor) {
        return (Map<Integer, Integer>) CombatAccess.field(actor, "skillUseCounts");
    }
    static Integer limit(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        if (NativeCloseoutNpcReuse.route(actor, row) == null) return null;
        var stamp = NativeSourceNpcFactoryType.stamp(actor);
        Object raw = CombatAccess.field(stamp.nativeSourceNpcSpellTiming, FIELD);
        return raw instanceof Map<?, ?> limits && limits.get(row.actNo()) instanceof Integer limit ? limit : null;
    }
    static boolean within(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row, int limit) {
        return limit <= 0 || counts(actor).getOrDefault(row.actNo(), 0) < limit;
    }
    static boolean allowed(boolean original, Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        if (!original) return false;
        Integer limit = limit(actor, row);
        return limit == null || within(actor, row, limit);
    }
    static Boolean summon(Object actor, BattleTimeAttackMagicSkillCatalog.SummonDefinition row) {
        Integer limit = limit(actor, row);
        return limit == null ? null : within(actor, row, limit);
    }
    static void scheduled(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        // Summon already increments its original map. Other not-yet-ported execution modes are not invented successes.
        var context = NativeCloseoutNpcAreaAttempt.current.get();
        var receipt = context == null ? null : context.receipt;
        if (receipt == null || receipt.row != row || receipt.view.actor().get() != actor || limit(actor, row) == null
                || (boolean) CombatAccess.field(receipt, COUNTED)) return;
        Object owner = receipt.view.owner().get();
        if (owner == null || !Thread.holdsLock(owner) || !NativeSourceNpcTickView.same(receipt.view, owner, actor)) return;
        // This seam is after the successful zero-cost schedule, before Source reuse, under the same encounter monitor.
        counts(actor).merge(row.actNo(), 1, Integer::sum);
        CombatAccess.set(receipt, COUNTED, true);
    }
    static boolean canRun(Object owner, Object actor, long now) {
        if (owner instanceof BattleQuestEncounter) {
            if (!(boolean) CombatAccess.field(owner, "active")) return false;
        } else {
            var phase = CombatAccess.field(owner, "phase");
            if (phase != BattleTimeAttackEncounter.Phase.ORDINARY_DOCUMENTED_FRAGMENT
                    && phase != BattleTimeAttackEncounter.Phase.FINAL_BOSSES
                    || !(boolean) CombatAccess.field(actor, "activeAi")) return false;
        }
        if (!(boolean) CombatAccess.call(actor, "ready", "(J)Z", now)) return false;
        for (String status : new String[]{"sleepActive", "paralysisActive", "shockStunActive", "boneBreakActive",
                "thunderGrabActive", "freezeActive", "earthBindActive"})
            if ((boolean) CombatAccess.call(actor, status, "(J)Z", now)) return false;
        return true;
    }
    static ClientSession noTarget(ClientSession target, GameHub hub, Object room, Object snapshot, long now) {
        if (target != null || !CombatRules.loaded() || !Thread.holdsLock(hub)
                || !(NativeSourceNpcFirstView.raw(snapshot) instanceof NativeSourceNpcFirstView.View view)
                || view.stamp() == null || view.stamp().kind() != 1
                || !(view.stamp().nativeSourceNpcSpellTiming instanceof NativeSourceNpcSpellTiming.Data data)
                || !(CombatAccess.field(data, FIELD) instanceof Map<?, ?> limits) || limits.isEmpty()) return target;
        var lifetime = NativePlayerLifetime.of(hub);
        if (!NativeCloseoutNpcAreaAttempt.live(hub, lifetime)) return target;
        long roomId = (long) CombatAccess.field(room, "id");
        Object owner = view.owner().get(), actor = view.actor().get();
        if (((Map<?, ?>) CombatAccess.field(hub, "rooms")).get(roomId) != room || !hub.battleStarted(roomId)
                || owner == null || actor == null
                || CombatAccess.field(room, view.family() == 1 ? "questEncounter" : "timeAttackEncounter") != owner) return target;
        synchronized (owner) {
            if (!NativeSourceNpcTickView.same(view, owner, actor) || NativeSourceRawDamage.number(actor, "hp") <= 0
                    || NativeSourceRawDamage.number(actor, "x") != view.x() || NativeSourceRawDamage.number(actor, "y") != view.y()
                    || NativeSourceNpcMagicEffects.actor(owner, view.id(), view.family() + 2) != actor) return target;
            if (counts(actor).isEmpty() || !canRun(owner, actor, now)) return target;
            var arena = NativeCloseoutNpcAreaScene.arena(owner, actor);
            if (arena == null) return target;
            for (var pc : (Set<ClientSession>) CombatAccess.field(room, "members")) {
                var binding = NativePlayerCommit.Binding.capture(pc);
                // Source noTarget uses every visible PC, including dead/ghost/GM-invisible players, at radius 20.
                if (binding != null && binding.phase().equals("BATTLE") && binding.room() == roomId
                        && CombatAccess.field(pc, "hub") == hub && NativeAcceptedFreezeMutation.roleFrame(binding, pc)
                        && !(boolean) CombatAccess.field(pc, "sourceItemDisconnected")
                        && hub.battleMapId(roomId, pc) == arena.mapId()
                        && Math.max(Math.abs(NativeSourceRawDamage.number(pc, "positionX") - view.x()),
                                    Math.abs(NativeSourceRawDamage.number(pc, "positionY") - view.y())) <= 20) return target;
            }
            // The existing no-target discovery proved the no-visible-PC finish condition. Keep reuse deadlines intact.
            if (NativeCloseoutNpcAreaAttempt.live(hub, lifetime)) counts(actor).clear();
        }
        return target;
    }
    private NativeCloseoutNpcCounts() {}
}
