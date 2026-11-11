package local.lito381.server;

/**
 * 精靈魔法施放時的屬性檢查（Issue #308，T108）。
 *
 * <p>依 1-伺服器 {@code L1SkillUse.isAttrAgrees}（{@code L1SkillUse.java:1850-1863}）另寫：
 * 技能等級 17..22（精靈魔法）且 SQL {@code skills.attr} 不為 0 時，玩家的 ElfAttr 位元遮罩
 * （1 地、2 火、4 水、8 風）必須包含該屬性，否則不能施放；GM 例外。由
 * {@code isNormalSkillUsable}（{@code :554-556}）在施放前呼叫，所以不扣 MP、不扣材料。</p>
 */
final class SourceElfAttributeGate {
    private SourceElfAttributeGate() {}

    /** 1-伺服器 skills.skill_level：每 8 個技能一級。 */
    static int skillLevel(int skillId) {
        if (skillId <= 0) throw new IllegalArgumentException("invalid skill id " + skillId);
        return ((skillId - 1) >> 3) + 1;
    }

    static boolean agrees(int skillId, int skillAttribute, int elfAttribute, boolean administrator) {
        int level = skillLevel(skillId);
        if (level < 17 || level > 22 || skillAttribute == 0) return true;
        return (elfAttribute & skillAttribute) != 0 || administrator;
    }
}
