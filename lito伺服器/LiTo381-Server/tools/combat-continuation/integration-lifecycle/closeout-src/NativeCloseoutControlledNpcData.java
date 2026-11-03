package local.lito381.server;

import java.util.ArrayList;
import java.util.List;

/** One readonly template snapshot for controlled recovery and complete mobskill facts. */
final class NativeCloseoutControlledNpcData {
    record Row(int actNo, int type, int triggerRandom, int triggerHp, int triggerCompanionHp,
               int triggerRange, int triggerCount, int changeTarget, int range, int areaWidth, int areaHeight,
               int leverage, int skill, int gfx, int action, int summon, int summonMin, int summonMax,
               int poly, int reuse) {}
    record Data(String database, int npc, int intelligence, int wisdom, int family,
                int attackMagicMillis, int supportMagicMillis, List<Row> rows) {
        Data { rows = List.copyOf(rows); }
    }
    record Birth(Object recovery, Object skills) {}
    static Birth capture(int npcId) {
        final NativeDeNpcTemplateResources.Snapshot snapshot;
        try { snapshot = NativeDeNpcTemplateResources.snapshot(); }
        catch (IllegalStateException unloaded) { return new Birth(null, null); }
        if (snapshot == null || snapshot.lifecycleFailure() || snapshot.npcs() == null) return new Birth(null, null);
        var npc = snapshot.npcs().get(npcId);
        if (npc == null || npc.get_npcId() != npcId) return new Birth(null, null);
        var rows = new ArrayList<Row>();
        var mob = snapshot.mobSkills() == null ? null : snapshot.mobSkills().get(npcId);
        if (mob != null && mob.get_mobid() == npcId) {
            for (int i = 0; i < mob.getSkillSize(); i++) rows.add(new Row(i, mob.getType(i), mob.getTriggerRandom(i),
                    mob.getTriggerHp(i), mob.getTriggerCompanionHp(i), mob.getTriggerRange(i), mob.getTriggerCount(i),
                    mob.getChangeTarget(i), mob.getRange(i), mob.getAreaWidth(i), mob.getAreaHeight(i), mob.getLeverage(i),
                    mob.getSkillId(i), mob.getGfxid(i), mob.getActid(i), mob.getSummon(i), mob.getSummonMin(i),
                    mob.getSummonMax(i), mob.getPolyId(i), mob.getReuseDelay(i)));
        }
        var recovery = new NativeCloseoutNpcRecovery.Data(npc.get_hprinterval() > 0 ? npc.get_hprinterval() : 20,
                npc.get_hpr() > 0 ? npc.get_hpr() : 1, npc.get_mprinterval() > 0 ? npc.get_mprinterval() : 20,
                npc.get_mpr() > 0 ? npc.get_mpr() : 1);
        var skills = new Data(snapshot.database(), npcId, npc.get_int(), npc.get_wis(), npc.get_family(),
                npc.getAtkMagicSpeed(), npc.getSubMagicSpeed(), rows);
        return new Birth(recovery, skills);
    }
    private NativeCloseoutControlledNpcData() {}
}
