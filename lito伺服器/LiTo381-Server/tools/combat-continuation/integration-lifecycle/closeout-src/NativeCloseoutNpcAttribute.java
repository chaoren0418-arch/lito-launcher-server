package local.lito381.server;

/** Source attribute selection at the original target resistance read. */
final class NativeCloseoutNpcAttribute {
    static int attribute(BattleTimeAttackMagicSkillCatalog.Definition definition) {
        if (NativeSkillTargetMetadata.size() == 0) return definition.attribute();
        return NativeSkillTargetMetadata.row(definition.skillId()).attribute();
    }
    private NativeCloseoutNpcAttribute() {}
}
