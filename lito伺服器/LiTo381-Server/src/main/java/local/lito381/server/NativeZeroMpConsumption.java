// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeSkillConsumption;
import local.lito381.server.NativeSourceSkillConsumptionMath;
import local.lito381.server.Protocol;

final class NativeZeroMpConsumption {
    static int cost(ClientSession clientSession, Object object) {
        int n = NativeSkillConsumption.skillId(object);
        int n2 = NativeSkillConsumption.baseMp(object);
        if (n2 != 0 || n != 130 && n != 146 && n != 181 && n != 182 && n != 191) {
            throw new IllegalStateException("unreviewed zero-base skill consumption metadata");
        }
        return NativeSourceSkillConsumptionMath.mpCost(n, n2, clientSession.nativeSkillConsumptionIntelligence(), clientSession.nativeSkillConsumptionOriginalReduction(), false);
    }

    static byte[] apply(ClientSession clientSession, int n) {
        int n2 = (Integer)CombatAccess.field(clientSession, "playerMp");
        int n3 = Math.max(0, (Integer)CombatAccess.field(clientSession, "playerMaxMp"));
        int n4 = (int)Math.max(0L, Math.min((long)n3, (long)n2 - (long)n));
        if (n4 == n2) {
            return null;
        }
        CombatAccess.set(clientSession, "playerMp", n4);
        return Protocol.manaPoint(n4, n3);
    }

    private NativeZeroMpConsumption() {
    }
}
