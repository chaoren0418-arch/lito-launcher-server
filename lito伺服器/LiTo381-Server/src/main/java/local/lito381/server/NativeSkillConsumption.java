// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.CharacterSummary;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeSourceSkillConsumptionMath;

final class NativeSkillConsumption {
    static int skillId(Object object) {
        return (Integer)CombatAccess.call(object, "skillId", "()I", new Object[0]);
    }

    static int baseMp(Object object) {
        return (Integer)CombatAccess.call(object, "mpCost", "()I", new Object[0]);
    }

    static int originalReduction(CharacterSummary characterSummary) {
        return characterSummary == null ? 0 : NativeSourceSkillConsumptionMath.originalReduction(characterSummary.type(), Math.max(0, characterSummary.originalIntelligence()));
    }

    static int boundMp(ClientSession clientSession, int n) {
        int n2 = (Integer)CombatAccess.field(clientSession, "playerMaxMp");
        return Math.max(0, Math.min(Math.max(0, n2), n));
    }

    private NativeSkillConsumption() {
    }
}
