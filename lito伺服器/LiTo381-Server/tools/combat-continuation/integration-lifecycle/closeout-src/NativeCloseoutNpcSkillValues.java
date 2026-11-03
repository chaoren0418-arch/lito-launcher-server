package local.lito381.server;

/** One existing immutable skills row per cost check or original damage calculation. */
final class NativeCloseoutNpcSkillValues {
    record Values(boolean sourced, int mp, int hp, int baseDamage, int diceCount, int dieSides) {}
    static Values captureCost(Object actor, BattleTimeAttackMagicSkillCatalog.Definition definition) {
        if (definition == null || definition.npcId() != NativeSourceNpcFactoryMemory.npc(actor)) return null;
        return capture(definition);
    }
    static Values capture(BattleTimeAttackMagicSkillCatalog.Definition definition) {
        if (definition == null) return null;
        var snapshot = CombatDatabase.nativeCloseoutSnapshot();
        if (snapshot == null) return new Values(false, definition.mpCost(), 0, definition.baseDamage(), definition.diceCount(), definition.dieSides());
        var row = snapshot.skills().get(definition.skillId());
        if (row == null) throw new IllegalStateException("missing skills snapshot row for NPC skill_id=" + definition.skillId());
        return new Values(true, row.mp(), row.hp(), row.damage(), row.dice(), row.die());
    }
    static boolean ready(boolean original, Object actor, Values values) {
        return original && values != null && (!values.sourced() || NativeSourceRawDamage.number(actor, "hp") > values.hp());
    }
    static void payHp(Object actor, Values values) {
        if (values.sourced() && values.hp() != 0)
            CombatAccess.set(actor, "hp", NativeSourceRawDamage.number(actor, "hp") - values.hp());
    }
    private NativeCloseoutNpcSkillValues() {}
}
