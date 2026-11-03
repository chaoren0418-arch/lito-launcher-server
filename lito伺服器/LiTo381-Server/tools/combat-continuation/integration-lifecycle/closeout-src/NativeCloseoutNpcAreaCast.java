package local.lito381.server;

import java.util.ArrayList;
import java.util.List;

/** The Source TYPE_GMBUFF self-target branch with a provably empty target list. */
final class NativeCloseoutNpcAreaCast {
    static final String FIELD = "nativeCloseoutGmBuffResults";
    record Attempt(int skill, NativeSourceRawDamage.Member target, boolean succeeded) {}

    static boolean emptyTarget(int skill, NativeSkillTargetMetadata.Row row) {
        // These runSkill/sendGrfx entries have effects before the ordinary target-list loop.
        if (skill == 2 || skill == 58 || skill == 63 || skill == 205 || skill == 210 || skill == 215 || skill == 220)
            return false;
        // isTarget rejects TYPE_ATTACK on self; makeTargetList returns for any non-'none' target.
        return row.type() == 64 && row.target() != null && !row.target().equals("none");
    }
    static boolean calculate(List<ClientSession> targets, ClientSession pc,
                             BattleTimeAttackMagicSkillCatalog.Definition definition) {
        if (!(targets instanceof NativeCloseoutNpcAreaTargets.Selection selection)) return true;
        int skill = definition.skillId();
        if (!emptyTarget(skill, NativeSkillTargetMetadata.row(skill))) return true;
        var member = selection.scope.origin().members().get(pc);
        boolean succeeded = member != null && NativeCloseoutNpcAreaTargets.eligible(selection, pc);
        // With an empty list, the peace-target check, runSkill target loop and sendGrfx(false)
        // do no work. handleCommands still returns true. Keep that result separate from damage.
        var next = new ArrayList<Attempt>(attempts(selection));
        next.add(new Attempt(skill, member, succeeded));
        CombatAccess.set(selection, FIELD, List.copyOf(next));
        return false;
    }
    @SuppressWarnings("unchecked")
    static List<Attempt> attempts(NativeCloseoutNpcAreaTargets.Selection selection) {
        Object value = CombatAccess.field(selection, FIELD);
        return value == null ? List.of() : (List<Attempt>) value;
    }
    static int successes(NativeCloseoutNpcAreaTargets.Selection selection) {
        int count = 0;
        for (var attempt : attempts(selection)) if (attempt.succeeded()) count++;
        return count;
    }
    private NativeCloseoutNpcAreaCast() {}
}
