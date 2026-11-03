package local.lito381.server;

/** Source costs for the two additional TYPE_MAGIC_ATTACK consumers. */
final class NativeCloseoutNpcAuxCost {
    static NativeCloseoutNpcSkillValues.Values captureCost(Object actor, Object definition) {
        if (!(definition instanceof BattleTimeAttackMagicSkillCatalog.MobskillRow row)
                || row.npcId() != NativeSourceNpcFactoryMemory.npc(actor)) return null;
        int skill, legacyMp;
        if (definition instanceof BattleTimeAttackMagicSkillCatalog.VisualDefinition d) {
            skill = d.skillId(); legacyMp = d.mpCost();
        } else if (definition instanceof BattleTimeAttackMagicSkillCatalog.GroundDefinition d) {
            skill = d.skillId(); legacyMp = d.mpCost();
        } else throw new IllegalArgumentException("unreviewed auxiliary NPC skill definition");
        var snapshot = CombatDatabase.nativeCloseoutSnapshot();
        if (snapshot == null) return new NativeCloseoutNpcSkillValues.Values(false, legacyMp, 0, 0, 0, 0);
        var value = snapshot.skills().get(skill);
        if (value == null) throw new IllegalStateException("missing skills snapshot row for NPC skill_id=" + skill);
        return new NativeCloseoutNpcSkillValues.Values(true, value.mp(), value.hp(), 0, 0, 0);
    }
    private NativeCloseoutNpcAuxCost() {}
}
