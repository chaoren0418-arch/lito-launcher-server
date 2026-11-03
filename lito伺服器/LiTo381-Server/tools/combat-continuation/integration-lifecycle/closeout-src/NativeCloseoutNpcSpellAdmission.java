package local.lito381.server;

/** Source normal NPC spell admission follows row selection; area-magic routes retain their separate path. */
final class NativeCloseoutNpcSpellAdmission {
    static final String P = "local/lito381/server/";
    static boolean normal(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        if (!CombatRules.loaded() || row == null) return false;
        var stamp = NativeSourceNpcFactoryType.stamp(actor);
        if (stamp == null || stamp.npc() != row.npcId()
                || !(stamp.nativeSourceNpcSpellTiming instanceof NativeSourceNpcSpellTiming.Data data)) return false;
        var route = data.routes().get(row.actNo());
        return route != null && route.type() == NativeDeNpcMobSkill.TYPE_MAGIC_ATTACK && route.skill() == skill(row)
                && route.width() <= 0 && route.height() <= 0;
    }
    static int skill(BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        if (row instanceof BattleTimeAttackMagicSkillCatalog.Definition d) return d.skillId();
        if (row instanceof BattleTimeAttackMagicSkillCatalog.VisualDefinition d) return d.skillId();
        if (row instanceof BattleTimeAttackMagicSkillCatalog.GroundDefinition d) return d.skillId();
        return 0;
    }
    static String lane(BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        if (row instanceof BattleTimeAttackMagicSkillCatalog.Definition) return "Magic";
        if (row instanceof BattleTimeAttackMagicSkillCatalog.VisualDefinition) return "Visual";
        if (row instanceof BattleTimeAttackMagicSkillCatalog.GroundDefinition) return "Ground";
        throw new IllegalArgumentException("unreviewed NPC spell lane");
    }
    static String descriptor(BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        return "L" + P + "BattleTimeAttackMagicSkillCatalog$" + (row instanceof BattleTimeAttackMagicSkillCatalog.Definition
                ? "Definition" : row instanceof BattleTimeAttackMagicSkillCatalog.VisualDefinition ? "VisualDefinition" : "GroundDefinition") + ";";
    }
    static boolean selectionSilence(Object actor, long now, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        if (!normal(actor, row)) return (boolean) CombatAccess.call(actor, "silenceActive", "(J)Z", now);
        // Retain the existing area-silence policy. Ordinary SILENCE belongs to the selected attempt.
        return now >= 0 && now < ((Number) CombatAccess.field(actor, "areaSilenceExpiresAtNanos")).longValue();
    }
    static boolean selectionReady(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row, long now) {
        if (!normal(actor, row)) {
            String name = lane(row); name = Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Ready";
            return (boolean) CombatAccess.call(actor, name, "(" + descriptor(row) + "J)Z", row, now);
        }
        // Source isSkillUseble checks row delay, not HP/MP affordability.
        return now >= NativeCloseoutNpcReuse.cooldowns(actor).getOrDefault(row.actNo(), 0L);
    }
    static boolean attempt(Object actor, long now, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        String lane = lane(row), type = descriptor(row);
        if (!normal(actor, row)) {
            CombatAccess.call(actor, "schedule" + lane, "(J" + type + ")V", now, row);
            return true;
        }
        // Original encounter monitor still owns actor lookup, selected row, costs and scheduling.
        if ((boolean) CombatAccess.call(actor, "consumeSilenceForSpell", "(J)Z", now)) return false;
        var values = row instanceof BattleTimeAttackMagicSkillCatalog.Definition d
                ? NativeCloseoutNpcSkillValues.captureCost(actor, d) : NativeCloseoutNpcAuxCost.captureCost(actor, row);
        String cost = "L" + P + "NativeCloseoutNpcSkillValues$Values;";
        if (!(boolean) CombatAccess.call(actor, "nativeCloseout" + lane + "Ready", "(" + type + "J" + cost + ")Z", row, now, values)) return false;
        CombatAccess.call(actor, "nativeCloseoutAdmitted" + lane, "(J" + type + cost + ")V", now, row, values);
        return true;
    }
    private NativeCloseoutNpcSpellAdmission() {}
}
