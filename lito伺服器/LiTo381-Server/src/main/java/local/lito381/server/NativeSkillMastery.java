package local.lito381.server;

/**
 * Classic skill-template admission used while importing read-only characters.
 *
 * <p>The retained login flow only turns a {@code character_skills} row into
 * both an S_ADD_SPELL bit and server-side mastery when its matching
 * {@code skills} template exists and has a level in the client's 28-byte
 * spell bitmap.  In particular, level-zero {@code none} rows are reserved
 * slots, even when they retain historical action or cast-gfx values.</p>
 */
final class NativeSkillMastery {
    static final int MIN_SKILL_ID = 1;
    static final int MAX_SKILL_ID = 220;
    static final int MAX_SKILL_LEVEL = 28;

    private NativeSkillMastery() {}

    static boolean importable(int skillId, Integer skillLevel) {
        return skillId >= MIN_SKILL_ID && skillId <= MAX_SKILL_ID
                && !reservedNativeGap(skillId)
                && skillLevel != null && skillLevel > 0
                && skillLevel <= MAX_SKILL_LEVEL;
    }

    private static boolean reservedNativeGap(int skillId) {
        return BattleSpellCatalog.reservedSkillSlot(skillId) != null;
    }
}
