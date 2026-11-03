package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/** Original born-actor mobskill delay, using the existing actor cooldown map. */
final class NativeCloseoutNpcReuse {
    static NativeSourceNpcSpellTiming.Route route(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        var stamp = NativeSourceNpcFactoryType.stamp(actor);
        if (row == null || stamp == null || stamp.npc() != row.npcId()
                || !(stamp.nativeSourceNpcSpellTiming instanceof NativeSourceNpcSpellTiming.Data data)) return null;
        var route = data.routes().get(row.actNo());
        if (route == null || route.reuse() < 0) return null;
        if (row instanceof BattleTimeAttackMagicSkillCatalog.PhysicalDefinition)
            return route.type() == NativeDeNpcMobSkill.TYPE_PHYSICAL_ATTACK ? route : null;
        if (row instanceof BattleTimeAttackMagicSkillCatalog.SummonDefinition)
            return route.type() == NativeDeNpcMobSkill.TYPE_SUMMON ? route : null;
        int skill = row instanceof BattleTimeAttackMagicSkillCatalog.Definition d ? d.skillId()
                : row instanceof BattleTimeAttackMagicSkillCatalog.GroundDefinition d ? d.skillId()
                : row instanceof BattleTimeAttackMagicSkillCatalog.VisualDefinition d ? d.skillId() : 0;
        return skill > 0 && route.type() == NativeDeNpcMobSkill.TYPE_MAGIC_ATTACK && route.skill() == skill ? route : null;
    }
    @SuppressWarnings("unchecked")
    static Map<Integer, Long> cooldowns(Object actor) { return (Map<Integer, Long>) CombatAccess.field(actor, "skillReadyNanos"); }
    static boolean ready(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row, long now) {
        return route(actor, row) == null || now >= cooldowns(actor).getOrDefault(row.actNo(), 0L);
    }
    static boolean distance(boolean original, Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row, long now) {
        return original && ready(actor, row, now);
    }
    static void scheduled(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row, long now) {
        var route = route(actor, row); if (route == null) return;
        cooldowns(actor).put(row.actNo(), Math.addExact(now, TimeUnit.MILLISECONDS.toNanos(route.reuse())));
    }
    static int roll(ThreadLocalRandom random, int origin, int bound, Object actor, List<?> rows, int index, long now) {
        var row = (BattleTimeAttackMagicSkillCatalog.MobskillRow) rows.get(index);
        return ready(actor, row, now) ? random.nextInt(origin, bound) : bound - 1;
    }
    static int summonRoll(ThreadLocalRandom random, int bound, Object actor, BattleTimeAttackMagicSkillCatalog.SummonDefinition row, long now) {
        return ready(actor, row, now) ? random.nextInt(bound) : 0;
    }
    private NativeCloseoutNpcReuse() {}
}
