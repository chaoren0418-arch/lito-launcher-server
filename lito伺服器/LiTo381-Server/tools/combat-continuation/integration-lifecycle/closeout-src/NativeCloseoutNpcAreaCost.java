package local.lito381.server;

/** Cost side of Source L1MobSkillUse.areaMagicAttack; target-side TYPE_GMBUFF dispatch remains separate. */
final class NativeCloseoutNpcAreaCost {
    static boolean matches(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        if (!CombatRules.loaded() || row == null) return false;
        var stamp = NativeSourceNpcFactoryType.stamp(actor);
        if (stamp == null || stamp.npc() != row.npcId()
                || !(stamp.nativeSourceNpcSpellTiming instanceof NativeSourceNpcSpellTiming.Data data)) return false;
        var route = data.routes().get(row.actNo());
        int skill = NativeCloseoutNpcSpellAdmission.skill(row);
        return skill > 0 && route != null && route.type() == NativeDeNpcMobSkill.TYPE_MAGIC_ATTACK
                && route.skill() == skill && (route.width() > 0 || route.height() > 0);
    }
    static boolean ready(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row, long now) {
        return now >= NativeCloseoutNpcReuse.cooldowns(actor).getOrDefault(row.actNo(), 0L);
    }
    static boolean attempt(Object actor, long now, BattleTimeAttackMagicSkillCatalog.MobskillRow row) {
        // Source checks skill existence only after selecting this row. No NPC normal-cast admission here.
        try { NativeSkillTargetMetadata.row(NativeCloseoutNpcSpellAdmission.skill(row)); }
        catch (IllegalStateException absent) { return false; }
        String lane = NativeCloseoutNpcSpellAdmission.lane(row), type = NativeCloseoutNpcSpellAdmission.descriptor(row);
        String cost = "Llocal/lito381/server/NativeCloseoutNpcSkillValues$Values;";
        var values = new NativeCloseoutNpcSkillValues.Values(true, 0, 0, 0, 0, 0);
        if (!(boolean) CombatAccess.call(actor, "nativeCloseout" + lane + "Ready", "(" + type + "J" + cost + ")Z", row, now, values)) return false;
        // Original scalar payment writes now receive zeros; original timing/identity checks remain.
        CombatAccess.call(actor, "nativeCloseoutAdmitted" + lane, "(J" + type + cost + ")V", now, row, values);
        return true;
    }
    private NativeCloseoutNpcAreaCost() {}
}
