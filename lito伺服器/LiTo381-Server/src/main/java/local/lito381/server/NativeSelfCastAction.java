// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatDatabase;
import local.lito381.server.NativeLightAction;

final class NativeSelfCastAction {
    static boolean supported(int n) {
        return n == 3 || n == 14 || n == 99 || n == 151 || n == 31 || n == 60 || n == 78 || n == 97 || n == 98 || n == 102 || n == 105 || n == 129 || n == 134 || n == 137 || n == 138 || n == 147 || n == 169 || n == 171 || n == 175 || n == 176 || n == 185 || n == 186 || n == 190 || n == 195;
    }

    static void shield(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ShieldSpell shieldSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeSelfShieldBody(byArray, shieldSpell));
    }

    static void weight(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.WeightSpell weightSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeSelfWeightBody(byArray, weightSpell));
    }

    static boolean admit(ClientSession clientSession, Object object) {
        int n = (Integer)CombatAccess.call(object, "skillId", "()I", new Object[0]);
        int n2 = (Integer)CombatAccess.call(object, "actionId", "()I", new Object[0]);
        if (!NativeSelfCastAction.supported(n) || n != 31 && n != 60 && n != 78 && n != 97 && n != 98 && n != 102 && n != 105 && n != 129 && n != 134 && n != 137 && n != 138 && n != 147 && n != 169 && n != 171 && n != 175 && n != 176 && n != 185 && n != 186 && n != 190 && n != 195 && CombatDatabase.value(0, n, "NativeSelfCast", "itemConsumeId") != 0) {
            throw new IllegalStateException("self cast requires reviewed no-material metadata");
        }
        return NativeLightAction.admitSelf(clientSession, n, n2, (Integer)CombatAccess.call(object, "reuseDelayMillis", "()I", new Object[0]));
    }

    private NativeSelfCastAction() {
    }

    static void counterMagic(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CounterMagicSpell counterMagicSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialCounterMagicBody(byArray, counterMagicSpell));
    }

    static void absoluteBarrier(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.AbsoluteBarrierSpell absoluteBarrierSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialAbsoluteBarrierBody(byArray, absoluteBarrierSpell));
    }

    static void physicalProcBuff(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.PhysicalProcBuffSpell physicalProcBuffSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialPhysicalProcBuffBody(byArray, physicalProcBuffSpell));
    }

    static void magicResistance(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.MagicResistanceSpell magicResistanceSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialMagicResistanceBody(byArray, magicResistanceSpell));
    }

    static void exoticVitalize(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ExoticVitalizeSpell exoticVitalizeSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialExoticVitalizeBody(byArray, exoticVitalizeSpell));
    }

    static void additionalFire(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.AdditionalFireSpell additionalFireSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialAdditionalFireBody(byArray, additionalFireSpell));
    }

    static void invisibility(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.InvisibilitySpell invisibilitySpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialInvisibilityBody(byArray, invisibilitySpell));
    }

    static void enchantVenom(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.EnchantVenomSpell enchantVenomSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialEnchantVenomBody(byArray, enchantVenomSpell));
    }

    static void counterMirror(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CounterMirrorSpell counterMirrorSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialCounterMirrorBody(byArray, counterMirrorSpell));
    }

    static void clearMind(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ClearMindSpell clearMindSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialClearMindBody(byArray, clearMindSpell));
    }

    static void resistElemental(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ResistElementalSpell resistElementalSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialResistElementalBody(byArray, resistElementalSpell));
    }

    static void elementalProtection(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ElementalProtectionSpell elementalProtectionSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialElementalProtectionBody(byArray, elementalProtectionSpell));
    }

    static void awaken(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.AwakenSpell awakenSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialAwakenBody(byArray, awakenSpell));
    }

    static void bloodlust(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.BloodlustSpell bloodlustSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialBloodlustBody(byArray, bloodlustSpell));
    }

    static interface Task {
        public void run() throws IOException;
    }
}
