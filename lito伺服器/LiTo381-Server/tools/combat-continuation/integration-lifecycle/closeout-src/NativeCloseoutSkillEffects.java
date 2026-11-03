package local.lito381.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.IntFunction;

/** Source's optional effect identity belongs to the same immutable published target row. */
final class NativeCloseoutSkillEffects {
    static NativeSkillTargetMetadata.Row attach(NativeSkillTargetMetadata.Row row, ResultSet result) {
        int effect = 0;
        try { effect = result.getInt("effect_skill_id"); }
        catch (SQLException absent) { /* Source SkillsTable keeps older schemas loadable with effect id zero. */ }
        row.nativeCloseoutEffectSkillId = effect;
        return row;
    }
    static int effectSkillId(int skill, NativeSkillTargetMetadata.Row row) {
        return row != null && row.nativeCloseoutEffectSkillId > 0 ? row.nativeCloseoutEffectSkillId : skill;
    }
    /** Prefer a skill's own mode; only a missing mode may use one direct positive effect reference. */
    static <T> T mode(int skill, NativeSkillTargetMetadata.Row row, IntFunction<T> lookup) {
        T mode = lookup.apply(skill);
        if (mode == null && row != null && row.nativeCloseoutEffectSkillId > 0
                && row.nativeCloseoutEffectSkillId != skill)
            mode = lookup.apply(row.nativeCloseoutEffectSkillId);
        return mode;
    }
    private NativeCloseoutSkillEffects() {}
}
