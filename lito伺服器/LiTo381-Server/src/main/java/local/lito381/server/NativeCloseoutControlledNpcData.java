package local.lito381.server;

import java.util.ArrayList;
import java.util.List;

/**
 * T135 (#386), ported from candidate 046c58c5: one read-only template snapshot for controlled
 * (pet / summon) recovery and the complete mobskill rows.
 *
 * <p>Captured once per actor birth from the already-loaded Source NPC template snapshot; no
 * database access, no registry.
 */
final class NativeCloseoutControlledNpcData {
  /** One {@code mobskill} row; field order follows {@code templates/L1MobSkill.java}. */
  record Row(
      int actNo,
      int type,
      int triggerRandom,
      int triggerHp,
      int triggerCompanionHp,
      int triggerRange,
      int triggerCount,
      int changeTarget,
      int range,
      int areaWidth,
      int areaHeight,
      int leverage,
      int skill,
      int gfx,
      int action,
      int summon,
      int summonMin,
      int summonMax,
      int poly,
      int reuse) {}

  record Data(
      String database,
      int npc,
      int intelligence,
      int wisdom,
      int family,
      int attackMagicMillis,
      int supportMagicMillis,
      List<Row> rows) {
    Data {
      rows = List.copyOf(rows);
    }
  }

  /** {@code recovery} is a {@link NativeCloseoutCompanionRecoveryData.Data}, {@code skills} a {@link Data}. */
  record Birth(Object recovery, Object skills) {}

  static final Birth UNKNOWN = new Birth(null, null);

  static Birth capture(int npcId) {
    final NativeDeNpcTemplateResources.Snapshot snapshot;
    try {
      snapshot = NativeDeNpcTemplateResources.snapshot();
    } catch (IllegalStateException unloaded) {
      return UNKNOWN;
    }
    return capture(snapshot, npcId);
  }

  static Birth capture(NativeDeNpcTemplateResources.Snapshot snapshot, int npcId) {
    if (snapshot == null || snapshot.lifecycleFailure() || snapshot.npcs() == null) return UNKNOWN;
    var npc = snapshot.npcs().get(npcId);
    if (npc == null || npc.get_npcId() != npcId) return UNKNOWN;
    var rows = new ArrayList<Row>();
    var mob = snapshot.mobSkills() == null ? null : snapshot.mobSkills().get(npcId);
    if (mob != null && mob.get_mobid() == npcId) {
      for (int i = 0; i < mob.getSkillSize(); i++)
        rows.add(
            new Row(
                i,
                mob.getType(i),
                mob.getTriggerRandom(i),
                mob.getTriggerHp(i),
                mob.getTriggerCompanionHp(i),
                mob.getTriggerRange(i),
                mob.getTriggerCount(i),
                mob.getChangeTarget(i),
                mob.getRange(i),
                mob.getAreaWidth(i),
                mob.getAreaHeight(i),
                mob.getLeverage(i),
                mob.getSkillId(i),
                mob.getGfxid(i),
                mob.getActid(i),
                mob.getSummon(i),
                mob.getSummonMin(i),
                mob.getSummonMax(i),
                mob.getPolyId(i),
                mob.getReuseDelay(i)));
    }
    var skills =
        new Data(
            snapshot.database(),
            npcId,
            npc.get_int(),
            npc.get_wis(),
            npc.get_family(),
            npc.getAtkMagicSpeed(),
            npc.getSubMagicSpeed(),
            rows);
    return new Birth(NativeCloseoutCompanionRecoveryData.of(npc), skills);
  }

  private NativeCloseoutControlledNpcData() {}
}
