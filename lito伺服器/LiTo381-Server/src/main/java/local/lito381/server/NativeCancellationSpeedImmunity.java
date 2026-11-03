// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatDatabase;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.SourceSkillControls;

final class NativeCancellationSpeedImmunity {
    NativeCancellationSpeedImmunity() {
    }

    private static boolean protectedSkill(int n) {
        SourceSkillControls.Snapshot snapshot = CombatDatabase.controlsOrNull();
        return n > 0 && snapshot != null && snapshot.cancellationImmune().contains(n);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static BattleTimedStatus.MagicCancellation magic(BattleTimedStatus battleTimedStatus, ClientSession clientSession, CombatPoisonLifecycle.Scope scope) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            BattleTimedStatus.MagicCancellation magicCancellation;
            int n = (Integer)CombatAccess.field(battleTimedStatus, "hasteSkillId");
            int n2 = (Integer)CombatAccess.field(battleTimedStatus, "slowSkillId");
            long l = (Long)CombatAccess.field(battleTimedStatus, "hasteExpiresAtNanos");
            long l2 = (Long)CombatAccess.field(battleTimedStatus, "slowExpiresAtNanos");
            boolean bl = l != 0L && NativeCancellationSpeedImmunity.protectedSkill(n);
            boolean bl2 = l2 != 0L && NativeCancellationSpeedImmunity.protectedSkill(n2);
            long l3 = (Long)CombatAccess.field(battleTimedStatus, "weaknessExpiresAtNanos");
            boolean bl3 = l3 != 0L && NativeCancellationSpeedImmunity.protectedSkill(47);
            long l4 = (Long)CombatAccess.field(battleTimedStatus, "diseaseExpiresAtNanos");
            boolean bl4 = l4 != 0L && NativeCancellationSpeedImmunity.protectedSkill(56);
            long l5 = (Long)CombatAccess.field(battleTimedStatus, "decayPotionExpiresAtNanos");
            boolean bl5 = l5 != 0L && NativeCancellationSpeedImmunity.protectedSkill(71);
            long l6 = (Long)CombatAccess.field(battleTimedStatus, "polluteWaterExpiresAtNanos");
            boolean bl6 = l6 != 0L && NativeCancellationSpeedImmunity.protectedSkill(173);
            long l7 = (Long)CombatAccess.field(battleTimedStatus, "strikerGaleExpiresAtNanos");
            boolean bl7 = l7 != 0L && NativeCancellationSpeedImmunity.protectedSkill(174);
            try {
                magicCancellation = CombatPoisonLifecycle.magic(battleTimedStatus, clientSession, scope);
            }
            finally {
                if (bl) {
                    CombatAccess.set(battleTimedStatus, "hasteExpiresAtNanos", l);
                    CombatAccess.set(battleTimedStatus, "hasteSkillId", n);
                }
                if (bl2) {
                    CombatAccess.set(battleTimedStatus, "slowExpiresAtNanos", l2);
                    CombatAccess.set(battleTimedStatus, "slowSkillId", n2);
                }
                if (bl3) {
                    CombatAccess.set(battleTimedStatus, "weaknessExpiresAtNanos", l3);
                }
                if (bl4) {
                    CombatAccess.set(battleTimedStatus, "diseaseExpiresAtNanos", l4);
                }
                if (bl5) {
                    CombatAccess.set(battleTimedStatus, "decayPotionExpiresAtNanos", l5);
                }
                if (bl6) {
                    CombatAccess.set(battleTimedStatus, "polluteWaterExpiresAtNanos", l6);
                }
                if (bl7) {
                    CombatAccess.set(battleTimedStatus, "strikerGaleExpiresAtNanos", l7);
                }
            }
            if (!(bl || bl2 || bl3 || bl4 || bl5 || bl6 || bl7)) {
                return magicCancellation;
            }
            // MONITOREXIT @DISABLED, blocks:[0, 2] lbl44 : MonitorExitStatement: MONITOREXIT : var3_3
            BattleTimedStatus.MagicCancellation magicCancellation2 = magicCancellation;
            return new BattleTimedStatus.MagicCancellation(magicCancellation2.haste() && !bl, magicCancellation2.slow() && !bl2, magicCancellation2.brave(), magicCancellation2.blind(), magicCancellation2.weakness() && !bl3, magicCancellation2.disease() && !bl4, magicCancellation2.silence(), magicCancellation2.decayPotion() && !bl5, magicCancellation2.polluteWater() && !bl6, magicCancellation2.strikerGale() && !bl7, magicCancellation2.light(), magicCancellation2.bluePotion(), magicCancellation2.wisdomPotion(), magicCancellation2.meditation(), magicCancellation2.concentration(), magicCancellation2.counterMagic(), magicCancellation2.breath(), magicCancellation2.poison(), magicCancellation2.sleep(), magicCancellation2.freeze(), magicCancellation2.earthBind(), magicCancellation2.burningSpirit(), magicCancellation2.elementalFire(), magicCancellation2.soulOfFlame(), magicCancellation2.venomResist(), magicCancellation2.doubleBrake(), magicCancellation2.horrorOfDeath(), magicCancellation2.thunderGrab(), magicCancellation2.boneBreak(), magicCancellation2.paralysisCancellation());
        }
    }
}
