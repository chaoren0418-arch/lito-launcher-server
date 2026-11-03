package local.lito381.server;

/** Existing readonly skills snapshot at the original WORLD cost and dice boundaries. */
final class NativeCloseoutWorldMagicValues {
    static NativeCloseoutNpcSkillValues.Values capture(ClassicWorldMonsterMagicSkillCatalog.Definition definition) {
        if (definition == null) return null;
        var snapshot = CombatDatabase.nativeCloseoutSnapshot();
        if (snapshot == null) return new NativeCloseoutNpcSkillValues.Values(false, definition.mpCost(), 0,
                definition.baseDamage(), definition.diceCount(), definition.dieSides());
        var row = snapshot.skills().get(definition.skillId());
        if (row == null) throw new IllegalStateException("missing skills snapshot row for WORLD skill_id=" + definition.skillId());
        return new NativeCloseoutNpcSkillValues.Values(true, row.mp(), row.hp(), row.damage(), row.dice(), row.die());
    }
    static NativeCloseoutNpcSkillValues.Values cost(Object actor, ClassicWorldMonsterMagicSkillCatalog.Definition definition) {
        if (definition == null || actor == null) return null;
        var spawn = (ClassicWorldMonsterCatalog.SpawnDefinition) CombatAccess.field(actor, "definition");
        return spawn.template().npcId() == definition.npcId() ? capture(definition) : null;
    }
    static NativeCloseoutNpcSkillValues.Values statusCost(Object actor, ClassicWorldMonsterStatusSkillCatalog.Definition definition) {
        if (definition == null || actor == null) return null;
        var spawn = (ClassicWorldMonsterCatalog.SpawnDefinition) CombatAccess.field(actor, "definition");
        if (spawn.template().npcId() != definition.npcId()) return null;
        var snapshot = CombatDatabase.nativeCloseoutSnapshot();
        if (snapshot == null) return new NativeCloseoutNpcSkillValues.Values(false, definition.mpCost(), 0, 0, 0, 0);
        var row = snapshot.skills().get(definition.skillId());
        if (row == null) throw new IllegalStateException("missing skills snapshot row for WORLD status skill_id=" + definition.skillId());
        return new NativeCloseoutNpcSkillValues.Values(true, row.mp(), row.hp(), 0, 0, 0);
    }
    static boolean ready(Object actor, NativeCloseoutNpcSkillValues.Values values) {
        return values != null && (!values.sourced() || NativeSourceRawDamage.number(actor, "currentHp") > values.hp());
    }
    static void payHp(Object actor, NativeCloseoutNpcSkillValues.Values values) {
        if (values.sourced() && values.hp() != 0)
            CombatAccess.set(actor, "currentHp", NativeSourceRawDamage.number(actor, "currentHp") - values.hp());
    }
    private NativeCloseoutWorldMagicValues() {}
}
