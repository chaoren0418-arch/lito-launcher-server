// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.NativeSkillConsumption;
import local.lito381.server.NativeSourceSkillConsumptionMath;

final class NativeSkillConsumptionBridges {
    static int PolymorphSpell_effectiveMpCost_III(BattleSpellCatalog.PolymorphSpell polymorphSpell, int n, int n2) {
        polymorphSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(polymorphSpell), NativeSkillConsumption.baseMp(polymorphSpell), n, n2, false);
    }

    static int ShieldSpell_effectiveMpCost_II(BattleSpellCatalog.ShieldSpell shieldSpell, int n, int n2) {
        shieldSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(shieldSpell), NativeSkillConsumption.baseMp(shieldSpell), n2, n, false);
    }

    static int CounterMagicSpell_effectiveMpCost_II(BattleSpellCatalog.CounterMagicSpell counterMagicSpell, int n, int n2) {
        counterMagicSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(counterMagicSpell), NativeSkillConsumption.baseMp(counterMagicSpell), n2, n, false);
    }

    static int AbsoluteBarrierSpell_effectiveMpCost_III(BattleSpellCatalog.AbsoluteBarrierSpell absoluteBarrierSpell, int n, int n2) {
        absoluteBarrierSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(absoluteBarrierSpell), NativeSkillConsumption.baseMp(absoluteBarrierSpell), n, n2, false);
    }

    static int AdvanceSpiritSpell_effectiveMpCost_III(BattleSpellCatalog.AdvanceSpiritSpell advanceSpiritSpell, int n, int n2) {
        advanceSpiritSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(advanceSpiritSpell), NativeSkillConsumption.baseMp(advanceSpiritSpell), n, n2, false);
    }

    static int ArmorBreakSpell_effectiveMpCost_II(BattleSpellCatalog.ArmorBreakSpell armorBreakSpell, int n, int n2) {
        armorBreakSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(armorBreakSpell), NativeSkillConsumption.baseMp(armorBreakSpell), n2, n, false);
    }

    static int GuardBrakeSpell_effectiveMpCost_II(BattleSpellCatalog.GuardBrakeSpell guardBrakeSpell, int n, int n2) {
        guardBrakeSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(guardBrakeSpell), NativeSkillConsumption.baseMp(guardBrakeSpell), n2, n, false);
    }

    static int ResistFearSpell_effectiveMpCost_II(BattleSpellCatalog.ResistFearSpell resistFearSpell, int n, int n2) {
        resistFearSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(resistFearSpell), NativeSkillConsumption.baseMp(resistFearSpell), n2, n, false);
    }

    static int ReductionArmorSpell_effectiveMpCost_III(BattleSpellCatalog.ReductionArmorSpell reductionArmorSpell, int n, int n2) {
        reductionArmorSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(reductionArmorSpell), NativeSkillConsumption.baseMp(reductionArmorSpell), n, n2, false);
    }

    static int BounceAttackSpell_effectiveMpCost_III(BattleSpellCatalog.BounceAttackSpell bounceAttackSpell, int n, int n2) {
        bounceAttackSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(bounceAttackSpell), NativeSkillConsumption.baseMp(bounceAttackSpell), n, n2, false);
    }

    static int SolidCarriageSpell_effectiveMpCost_III(BattleSpellCatalog.SolidCarriageSpell solidCarriageSpell, int n, int n2) {
        solidCarriageSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(solidCarriageSpell), NativeSkillConsumption.baseMp(solidCarriageSpell), n, n2, false);
    }

    static int CounterBarrierSpell_effectiveMpCost_III(BattleSpellCatalog.CounterBarrierSpell counterBarrierSpell, int n, int n2) {
        counterBarrierSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(counterBarrierSpell), NativeSkillConsumption.baseMp(counterBarrierSpell), n, n2, false);
    }

    static int CubeIgnitionSpell_effectiveMpCost_II(BattleSpellCatalog.CubeIgnitionSpell cubeIgnitionSpell, int n, int n2) {
        cubeIgnitionSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(cubeIgnitionSpell), NativeSkillConsumption.baseMp(cubeIgnitionSpell), n2, n, false);
    }

    static int CubeQuakeSpell_effectiveMpCost_II(BattleSpellCatalog.CubeQuakeSpell cubeQuakeSpell, int n, int n2) {
        cubeQuakeSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(cubeQuakeSpell), NativeSkillConsumption.baseMp(cubeQuakeSpell), n2, n, false);
    }

    static int CubeShockSpell_effectiveMpCost_II(BattleSpellCatalog.CubeShockSpell cubeShockSpell, int n, int n2) {
        cubeShockSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(cubeShockSpell), NativeSkillConsumption.baseMp(cubeShockSpell), n2, n, false);
    }

    static int CubeHarmonySpell_effectiveMpCost_II(BattleSpellCatalog.CubeHarmonySpell cubeHarmonySpell, int n, int n2) {
        cubeHarmonySpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(cubeHarmonySpell), NativeSkillConsumption.baseMp(cubeHarmonySpell), n2, n, false);
    }

    static int PatienceSpell_effectiveMpCost_II(BattleSpellCatalog.PatienceSpell patienceSpell, int n, int n2) {
        patienceSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(patienceSpell), NativeSkillConsumption.baseMp(patienceSpell), n2, n, false);
    }

    static int PhantasmSpell_effectiveMpCost_II(BattleSpellCatalog.PhantasmSpell phantasmSpell, int n, int n2) {
        phantasmSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(phantasmSpell), NativeSkillConsumption.baseMp(phantasmSpell), n2, n, false);
    }

    static int PanicSpell_effectiveMpCost_II(BattleSpellCatalog.PanicSpell panicSpell, int n, int n2) {
        panicSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(panicSpell), NativeSkillConsumption.baseMp(panicSpell), n2, n, false);
    }

    static int JoyOfPainSpell_effectiveMpCost_II(BattleSpellCatalog.JoyOfPainSpell joyOfPainSpell, int n, int n2) {
        joyOfPainSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(joyOfPainSpell), NativeSkillConsumption.baseMp(joyOfPainSpell), n2, n, false);
    }

    static int IllusionOgreSpell_effectiveMpCost_II(BattleSpellCatalog.IllusionOgreSpell illusionOgreSpell, int n, int n2) {
        illusionOgreSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(illusionOgreSpell), NativeSkillConsumption.baseMp(illusionOgreSpell), n2, n, false);
    }

    static int DiamondGolemSpell_effectiveMpCost_II(BattleSpellCatalog.DiamondGolemSpell diamondGolemSpell, int n, int n2) {
        diamondGolemSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(diamondGolemSpell), NativeSkillConsumption.baseMp(diamondGolemSpell), n2, n, false);
    }

    static int InsightSpell_effectiveMpCost_II(BattleSpellCatalog.InsightSpell insightSpell, int n, int n2) {
        insightSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(insightSpell), NativeSkillConsumption.baseMp(insightSpell), n2, n, false);
    }

    static int IllusionLichSpell_effectiveMpCost_II(BattleSpellCatalog.IllusionLichSpell illusionLichSpell, int n, int n2) {
        illusionLichSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(illusionLichSpell), NativeSkillConsumption.baseMp(illusionLichSpell), n2, n, false);
    }

    static int IllusionAvatarSpell_effectiveMpCost_II(BattleSpellCatalog.IllusionAvatarSpell illusionAvatarSpell, int n, int n2) {
        illusionAvatarSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(illusionAvatarSpell), NativeSkillConsumption.baseMp(illusionAvatarSpell), n2, n, false);
    }

    static int AbilityBuffSpell_effectiveMpCost_IIZI(BattleSpellCatalog.AbilityBuffSpell abilityBuffSpell, int n, int n2, boolean bl) {
        abilityBuffSpell.effectiveMpCost(n, n2, bl);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(abilityBuffSpell), NativeSkillConsumption.baseMp(abilityBuffSpell), n, n2, bl);
    }

    static int WeaponBuffSpell_effectiveMpCost_III(BattleSpellCatalog.WeaponBuffSpell weaponBuffSpell, int n, int n2) {
        weaponBuffSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(weaponBuffSpell), NativeSkillConsumption.baseMp(weaponBuffSpell), n, n2, false);
    }

    static int SelfHasteSpell_effectiveMpCost_IIZI(BattleSpellCatalog.SelfHasteSpell selfHasteSpell, int n, int n2, boolean bl) {
        selfHasteSpell.effectiveMpCost(n, n2, bl);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(selfHasteSpell), NativeSkillConsumption.baseMp(selfHasteSpell), n, n2, bl);
    }

    static int CombatBuffSpell_effectiveMpCost_III(BattleSpellCatalog.CombatBuffSpell combatBuffSpell, int n, int n2) {
        combatBuffSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(combatBuffSpell), NativeSkillConsumption.baseMp(combatBuffSpell), n, n2, false);
    }

    static int DamageProtectionSpell_effectiveMpCost_III(BattleSpellCatalog.DamageProtectionSpell damageProtectionSpell, int n, int n2) {
        damageProtectionSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(damageProtectionSpell), NativeSkillConsumption.baseMp(damageProtectionSpell), n, n2, false);
    }

    static int MassTeleportSpell_effectiveMpCost_III(BattleSpellCatalog.MassTeleportSpell massTeleportSpell, int n, int n2) {
        massTeleportSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(massTeleportSpell), NativeSkillConsumption.baseMp(massTeleportSpell), n, n2, false);
    }

    static int MovementBuffSpell_effectiveMpCost_III(BattleSpellCatalog.MovementBuffSpell movementBuffSpell, int n, int n2) {
        movementBuffSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(movementBuffSpell), NativeSkillConsumption.baseMp(movementBuffSpell), n, n2, false);
    }

    static int PhysicalProcBuffSpell_effectiveMpCost_III(BattleSpellCatalog.PhysicalProcBuffSpell physicalProcBuffSpell, int n, int n2) {
        physicalProcBuffSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(physicalProcBuffSpell), NativeSkillConsumption.baseMp(physicalProcBuffSpell), n, n2, false);
    }

    static int OtherNoEffectSpell_effectiveMpCost_III(BattleSpellCatalog.OtherNoEffectSpell otherNoEffectSpell, int n, int n2) {
        otherNoEffectSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(otherNoEffectSpell), NativeSkillConsumption.baseMp(otherNoEffectSpell), n, n2, false);
    }

    static int DodgeBuffSpell_effectiveMpCost_III(BattleSpellCatalog.DodgeBuffSpell dodgeBuffSpell, int n, int n2) {
        dodgeBuffSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(dodgeBuffSpell), NativeSkillConsumption.baseMp(dodgeBuffSpell), n, n2, false);
    }

    static int RangedEvasionBuffSpell_effectiveMpCost_III(BattleSpellCatalog.RangedEvasionBuffSpell rangedEvasionBuffSpell, int n, int n2) {
        rangedEvasionBuffSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(rangedEvasionBuffSpell), NativeSkillConsumption.baseMp(rangedEvasionBuffSpell), n, n2, false);
    }

    static int PoisonResistanceSpell_effectiveMpCost_III(BattleSpellCatalog.PoisonResistanceSpell poisonResistanceSpell, int n, int n2) {
        poisonResistanceSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(poisonResistanceSpell), NativeSkillConsumption.baseMp(poisonResistanceSpell), n, n2, false);
    }

    static int MagicResistanceSpell_effectiveMpCost_III(BattleSpellCatalog.MagicResistanceSpell magicResistanceSpell, int n, int n2) {
        magicResistanceSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(magicResistanceSpell), NativeSkillConsumption.baseMp(magicResistanceSpell), n, n2, false);
    }

    static int TripleArrowSpell_effectiveMpCost_III(BattleSpellCatalog.TripleArrowSpell tripleArrowSpell, int n, int n2) {
        tripleArrowSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(tripleArrowSpell), NativeSkillConsumption.baseMp(tripleArrowSpell), n, n2, false);
    }

    static int WorldTreeCallSpell_effectiveMpCost_III(BattleSpellCatalog.WorldTreeCallSpell worldTreeCallSpell, int n, int n2) {
        worldTreeCallSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(worldTreeCallSpell), NativeSkillConsumption.baseMp(worldTreeCallSpell), n, n2, false);
    }

    static int CancellationSpell_effectiveMpCost_III(BattleSpellCatalog.CancellationSpell cancellationSpell, int n, int n2) {
        cancellationSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(cancellationSpell), NativeSkillConsumption.baseMp(cancellationSpell), n, n2, false);
    }

    static int ExoticVitalizeSpell_effectiveMpCost_II(BattleSpellCatalog.ExoticVitalizeSpell exoticVitalizeSpell, int n, int n2) {
        exoticVitalizeSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(exoticVitalizeSpell), NativeSkillConsumption.baseMp(exoticVitalizeSpell), n2, n, false);
    }

    static int AdditionalFireSpell_effectiveMpCost_II(BattleSpellCatalog.AdditionalFireSpell additionalFireSpell, int n, int n2) {
        additionalFireSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(additionalFireSpell), NativeSkillConsumption.baseMp(additionalFireSpell), n2, n, false);
    }

    static int WeightSpell_mpCost_I(BattleSpellCatalog.WeightSpell weightSpell, int n, int n2) {
        weightSpell.mpCost();
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(weightSpell), NativeSkillConsumption.baseMp(weightSpell), n, n2, false);
    }

    static int RegenSpell_effectiveMpCost_III(BattleSpellCatalog.RegenSpell regenSpell, int n, int n2) {
        regenSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(regenSpell), NativeSkillConsumption.baseMp(regenSpell), n, n2, false);
    }

    static int TrueTargetSpell_effectiveMpCost_II(BattleSpellCatalog.TrueTargetSpell trueTargetSpell, int n, int n2) {
        trueTargetSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(trueTargetSpell), NativeSkillConsumption.baseMp(trueTargetSpell), n2, n, false);
    }

    static int RoyalAuraSpell_effectiveMpCost_II(BattleSpellCatalog.RoyalAuraSpell royalAuraSpell, int n, int n2) {
        royalAuraSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(royalAuraSpell), NativeSkillConsumption.baseMp(royalAuraSpell), n2, n, false);
    }

    static int CallClanSpell_effectiveMpCost_II(BattleSpellCatalog.CallClanSpell callClanSpell, int n, int n2) {
        callClanSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(callClanSpell), NativeSkillConsumption.baseMp(callClanSpell), n2, n, false);
    }

    static int RunClanSpell_effectiveMpCost_II(BattleSpellCatalog.RunClanSpell runClanSpell, int n, int n2) {
        runClanSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(runClanSpell), NativeSkillConsumption.baseMp(runClanSpell), n2, n, false);
    }

    static int InvisibilitySpell_effectiveMpCost_III(BattleSpellCatalog.InvisibilitySpell invisibilitySpell, int n, int n2) {
        invisibilitySpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(invisibilitySpell), NativeSkillConsumption.baseMp(invisibilitySpell), n, n2, false);
    }

    static int EnchantVenomSpell_effectiveMpCost_II(BattleSpellCatalog.EnchantVenomSpell enchantVenomSpell, int n, int n2) {
        enchantVenomSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(enchantVenomSpell), NativeSkillConsumption.baseMp(enchantVenomSpell), n2, n, false);
    }

    static int BringStoneSpell_effectiveMpCost_II(BattleSpellCatalog.BringStoneSpell bringStoneSpell, int n, int n2) {
        bringStoneSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(bringStoneSpell), NativeSkillConsumption.baseMp(bringStoneSpell), n2, n, false);
    }

    static int TurnUndeadSpell_mpCost_I(BattleSpellCatalog.TurnUndeadSpell turnUndeadSpell, int n, int n2) {
        turnUndeadSpell.mpCost();
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(turnUndeadSpell), NativeSkillConsumption.baseMp(turnUndeadSpell), n, n2, false);
    }

    static int ManaDrainSpell_effectiveMpCost_III(BattleSpellCatalog.ManaDrainSpell manaDrainSpell, int n, int n2) {
        manaDrainSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(manaDrainSpell), NativeSkillConsumption.baseMp(manaDrainSpell), n, n2, false);
    }

    static int BlindSpell_effectiveMpCost_III(BattleSpellCatalog.BlindSpell blindSpell, int n, int n2) {
        blindSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(blindSpell), NativeSkillConsumption.baseMp(blindSpell), n, n2, false);
    }

    static int SilenceSpell_effectiveMpCost_III(BattleSpellCatalog.SilenceSpell silenceSpell, int n, int n2) {
        silenceSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(silenceSpell), NativeSkillConsumption.baseMp(silenceSpell), n, n2, false);
    }

    static int ElementalFallDownSpell_effectiveMpCost_II(BattleSpellCatalog.ElementalFallDownSpell elementalFallDownSpell, int n, int n2) {
        elementalFallDownSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(elementalFallDownSpell), NativeSkillConsumption.baseMp(elementalFallDownSpell), n2, n, false);
    }

    static int ReturnToNatureSpell_effectiveMpCost_II(BattleSpellCatalog.ReturnToNatureSpell returnToNatureSpell, int n, int n2) {
        returnToNatureSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(returnToNatureSpell), NativeSkillConsumption.baseMp(returnToNatureSpell), n2, n, false);
    }

    static int CounterMirrorSpell_effectiveMpCost_II(BattleSpellCatalog.CounterMirrorSpell counterMirrorSpell, int n, int n2) {
        counterMirrorSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(counterMirrorSpell), NativeSkillConsumption.baseMp(counterMirrorSpell), n2, n, false);
    }

    static int ClearMindSpell_effectiveMpCost_II(BattleSpellCatalog.ClearMindSpell clearMindSpell, int n, int n2) {
        clearMindSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(clearMindSpell), NativeSkillConsumption.baseMp(clearMindSpell), n2, n, false);
    }

    static int ResistElementalSpell_effectiveMpCost_II(BattleSpellCatalog.ResistElementalSpell resistElementalSpell, int n, int n2) {
        resistElementalSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(resistElementalSpell), NativeSkillConsumption.baseMp(resistElementalSpell), n2, n, false);
    }

    static int ElementalProtectionSpell_effectiveMpCost_II(BattleSpellCatalog.ElementalProtectionSpell elementalProtectionSpell, int n, int n2) {
        elementalProtectionSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(elementalProtectionSpell), NativeSkillConsumption.baseMp(elementalProtectionSpell), n2, n, false);
    }

    static int FireWeaponSpell_effectiveMpCost_II(BattleSpellCatalog.FireWeaponSpell fireWeaponSpell, int n, int n2) {
        fireWeaponSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(fireWeaponSpell), NativeSkillConsumption.baseMp(fireWeaponSpell), n2, n, false);
    }

    static int WindShotSpell_effectiveMpCost_II(BattleSpellCatalog.WindShotSpell windShotSpell, int n, int n2) {
        windShotSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(windShotSpell), NativeSkillConsumption.baseMp(windShotSpell), n2, n, false);
    }

    static int FireBlessSpell_effectiveMpCost_II(BattleSpellCatalog.FireBlessSpell fireBlessSpell, int n, int n2) {
        fireBlessSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(fireBlessSpell), NativeSkillConsumption.baseMp(fireBlessSpell), n2, n, false);
    }

    static int StormEyeSpell_effectiveMpCost_II(BattleSpellCatalog.StormEyeSpell stormEyeSpell, int n, int n2) {
        stormEyeSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(stormEyeSpell), NativeSkillConsumption.baseMp(stormEyeSpell), n2, n, false);
    }

    static int StormShotSpell_effectiveMpCost_II(BattleSpellCatalog.StormShotSpell stormShotSpell, int n, int n2) {
        stormShotSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(stormShotSpell), NativeSkillConsumption.baseMp(stormShotSpell), n2, n, false);
    }

    static int EarthBlessSpell_effectiveMpCost_II(BattleSpellCatalog.EarthBlessSpell earthBlessSpell, int n, int n2) {
        earthBlessSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(earthBlessSpell), NativeSkillConsumption.baseMp(earthBlessSpell), n2, n, false);
    }

    static int IronSkinSpell_effectiveMpCost_II(BattleSpellCatalog.IronSkinSpell ironSkinSpell, int n, int n2) {
        ironSkinSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(ironSkinSpell), NativeSkillConsumption.baseMp(ironSkinSpell), n2, n, false);
    }

    static int AwakenSpell_mpCost_I(BattleSpellCatalog.AwakenSpell awakenSpell, int n, int n2) {
        awakenSpell.mpCost();
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(awakenSpell), NativeSkillConsumption.baseMp(awakenSpell), n, n2, false);
    }

    static int BloodlustSpell_mpCost_I(BattleSpellCatalog.BloodlustSpell bloodlustSpell, int n, int n2) {
        bloodlustSpell.mpCost();
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(bloodlustSpell), NativeSkillConsumption.baseMp(bloodlustSpell), n, n2, false);
    }

    static int FoeSlayerSpell_mpCost_I(BattleSpellCatalog.FoeSlayerSpell foeSlayerSpell, int n, int n2) {
        foeSlayerSpell.mpCost();
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(foeSlayerSpell), NativeSkillConsumption.baseMp(foeSlayerSpell), n, n2, false);
    }

    static int NaturesTouchSpell_effectiveMpCost_II(BattleSpellCatalog.NaturesTouchSpell naturesTouchSpell, int n, int n2) {
        naturesTouchSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(naturesTouchSpell), NativeSkillConsumption.baseMp(naturesTouchSpell), n2, n, false);
    }

    static int WaterLifeSpell_effectiveMpCost_II(BattleSpellCatalog.WaterLifeSpell waterLifeSpell, int n, int n2) {
        waterLifeSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(waterLifeSpell), NativeSkillConsumption.baseMp(waterLifeSpell), n2, n, false);
    }

    static int AquaProtectSpell_effectiveMpCost_II(BattleSpellCatalog.AquaProtectSpell aquaProtectSpell, int n, int n2) {
        aquaProtectSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(aquaProtectSpell), NativeSkillConsumption.baseMp(aquaProtectSpell), n2, n, false);
    }

    static int AreaSilenceSpell_effectiveMpCost_II(BattleSpellCatalog.AreaSilenceSpell areaSilenceSpell, int n, int n2) {
        areaSilenceSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(areaSilenceSpell), NativeSkillConsumption.baseMp(areaSilenceSpell), n2, n, false);
    }

    static int WindShackleSpell_effectiveMpCost_II(BattleSpellCatalog.WindShackleSpell windShackleSpell, int n, int n2) {
        windShackleSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(windShackleSpell), NativeSkillConsumption.baseMp(windShackleSpell), n2, n, false);
    }

    static int TargetNegativeStatusSpell_effectiveMpCost_II(BattleSpellCatalog.TargetNegativeStatusSpell targetNegativeStatusSpell, int n, int n2) {
        targetNegativeStatusSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(targetNegativeStatusSpell), NativeSkillConsumption.baseMp(targetNegativeStatusSpell), n2, n, false);
    }

    static int DecayPotionSpell_effectiveMpCost_III(BattleSpellCatalog.DecayPotionSpell decayPotionSpell, int n, int n2) {
        decayPotionSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(decayPotionSpell), NativeSkillConsumption.baseMp(decayPotionSpell), n, n2, false);
    }

    static int ItemWeaponEnchantSpell_effectiveMpCost_IIZI(BattleSpellCatalog.ItemWeaponEnchantSpell itemWeaponEnchantSpell, int n, int n2, boolean bl) {
        itemWeaponEnchantSpell.effectiveMpCost(n, n2, bl);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(itemWeaponEnchantSpell), NativeSkillConsumption.baseMp(itemWeaponEnchantSpell), n, n2, bl);
    }

    static int ItemArmorEnchantSpell_effectiveMpCost_III(BattleSpellCatalog.ItemArmorEnchantSpell itemArmorEnchantSpell, int n, int n2) {
        itemArmorEnchantSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(itemArmorEnchantSpell), NativeSkillConsumption.baseMp(itemArmorEnchantSpell), n, n2, false);
    }

    static int MagicalWeaponSpell_effectiveMpCost_III(BattleSpellCatalog.MagicalWeaponSpell magicalWeaponSpell, int n, int n2) {
        magicalWeaponSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(magicalWeaponSpell), NativeSkillConsumption.baseMp(magicalWeaponSpell), n, n2, false);
    }

    static int RevealWeaknessSpell_effectiveMpCost_III(BattleSpellCatalog.RevealWeaknessSpell revealWeaknessSpell, int n, int n2) {
        revealWeaknessSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(revealWeaknessSpell), NativeSkillConsumption.baseMp(revealWeaknessSpell), n, n2, false);
    }

    static int DetectionSpell_effectiveMpCost_IIZI(BattleSpellCatalog.DetectionSpell detectionSpell, int n, int n2, boolean bl) {
        detectionSpell.effectiveMpCost(n, n2, bl);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(detectionSpell), NativeSkillConsumption.baseMp(detectionSpell), n, n2, bl);
    }

    static int CounterDetectionSpell_effectiveMpCost_III(BattleSpellCatalog.CounterDetectionSpell counterDetectionSpell, int n, int n2) {
        counterDetectionSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(counterDetectionSpell), NativeSkillConsumption.baseMp(counterDetectionSpell), n, n2, false);
    }

    static int WeaponBreakSpell_effectiveMpCost_III(BattleSpellCatalog.WeaponBreakSpell weaponBreakSpell, int n, int n2) {
        weaponBreakSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(weaponBreakSpell), NativeSkillConsumption.baseMp(weaponBreakSpell), n, n2, false);
    }

    static int WeaknessSpell_effectiveMpCost_III(BattleSpellCatalog.WeaknessSpell weaknessSpell, int n, int n2) {
        weaknessSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(weaknessSpell), NativeSkillConsumption.baseMp(weaknessSpell), n, n2, false);
    }

    static int BindSpell_effectiveMpCost_II(BattleSpellCatalog.BindSpell bindSpell, int n, int n2) {
        bindSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(bindSpell), NativeSkillConsumption.baseMp(bindSpell), n2, n, false);
    }

    static int MassSlowSpell_effectiveMpCost_III(BattleSpellCatalog.MassSlowSpell massSlowSpell, int n, int n2) {
        massSlowSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(massSlowSpell), NativeSkillConsumption.baseMp(massSlowSpell), n, n2, false);
    }

    static int SleepSpell_effectiveMpCost_III(BattleSpellCatalog.SleepSpell sleepSpell, int n, int n2) {
        sleepSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(sleepSpell), NativeSkillConsumption.baseMp(sleepSpell), n, n2, false);
    }

    static int DirectionalLineSpell_effectiveMpCost_III(BattleSpellCatalog.DirectionalLineSpell directionalLineSpell, int n, int n2) {
        directionalLineSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(directionalLineSpell), NativeSkillConsumption.baseMp(directionalLineSpell), n, n2, false);
    }

    static int FreezeAttackSpell_effectiveMpCost_III(BattleSpellCatalog.FreezeAttackSpell freezeAttackSpell, int n, int n2) {
        freezeAttackSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(freezeAttackSpell), NativeSkillConsumption.baseMp(freezeAttackSpell), n, n2, false);
    }

    static int ShockStunSpell_effectiveMpCost_III(BattleSpellCatalog.ShockStunSpell shockStunSpell, int n, int n2) {
        shockStunSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(shockStunSpell), NativeSkillConsumption.baseMp(shockStunSpell), n, n2, false);
    }

    static int LineAttackSpell_effectiveMpCost_III(BattleSpellCatalog.LineAttackSpell lineAttackSpell, int n, int n2) {
        lineAttackSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(lineAttackSpell), NativeSkillConsumption.baseMp(lineAttackSpell), n, n2, false);
    }

    static int AreaAttackSpell_effectiveMpCost_III(BattleSpellCatalog.AreaAttackSpell areaAttackSpell, int n, int n2) {
        areaAttackSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(areaAttackSpell), NativeSkillConsumption.baseMp(areaAttackSpell), n, n2, false);
    }

    static int AttackSpell_effectiveMpCost_III(BattleSpellCatalog.AttackSpell attackSpell, int n, int n2) {
        attackSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(attackSpell), NativeSkillConsumption.baseMp(attackSpell), n, n2, false);
    }

    static int PoisonSpell_effectiveMpCost_III(BattleSpellCatalog.PoisonSpell poisonSpell, int n, int n2) {
        poisonSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(poisonSpell), NativeSkillConsumption.baseMp(poisonSpell), n, n2, false);
    }

    static int ParalysisSpell_effectiveMpCost_III(BattleSpellCatalog.ParalysisSpell paralysisSpell, int n, int n2) {
        paralysisSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(paralysisSpell), NativeSkillConsumption.baseMp(paralysisSpell), n, n2, false);
    }

    static int CurseRemovalSpell_effectiveMpCost_III(BattleSpellCatalog.CurseRemovalSpell curseRemovalSpell, int n, int n2) {
        curseRemovalSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(curseRemovalSpell), NativeSkillConsumption.baseMp(curseRemovalSpell), n, n2, false);
    }

    static int ElementalSummonSpell_effectiveMpCost_I(BattleSpellCatalog.ElementalSummonSpell elementalSummonSpell, int n, int n2) {
        elementalSummonSpell.effectiveMpCost();
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(elementalSummonSpell), NativeSkillConsumption.baseMp(elementalSummonSpell), n, n2, false);
    }

    static int SummonSpell_effectiveMpCost_III(BattleSpellCatalog.SummonSpell summonSpell, int n, int n2) {
        summonSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(summonSpell), NativeSkillConsumption.baseMp(summonSpell), n, n2, false);
    }

    static int CreateZombieSpell_effectiveMpCost_III(BattleSpellCatalog.CreateZombieSpell createZombieSpell, int n, int n2) {
        createZombieSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(createZombieSpell), NativeSkillConsumption.baseMp(createZombieSpell), n, n2, false);
    }

    static int ResurrectionSpell_effectiveMpCost_III(BattleSpellCatalog.ResurrectionSpell resurrectionSpell, int n, int n2) {
        resurrectionSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(resurrectionSpell), NativeSkillConsumption.baseMp(resurrectionSpell), n, n2, false);
    }

    static int TamingSpell_effectiveMpCost_III(BattleSpellCatalog.TamingSpell tamingSpell, int n, int n2) {
        tamingSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(tamingSpell), NativeSkillConsumption.baseMp(tamingSpell), n, n2, false);
    }

    static int GroundSpell_effectiveMpCost_III(BattleSpellCatalog.GroundSpell groundSpell, int n, int n2) {
        groundSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(groundSpell), NativeSkillConsumption.baseMp(groundSpell), n, n2, false);
    }

    static int NaturesBlessingSpell_effectiveMpCost_II(BattleSpellCatalog.NaturesBlessingSpell naturesBlessingSpell, int n, int n2) {
        naturesBlessingSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(naturesBlessingSpell), NativeSkillConsumption.baseMp(naturesBlessingSpell), n2, n, false);
    }

    static int GroupHealingSpell_effectiveMpCost_III(BattleSpellCatalog.GroupHealingSpell groupHealingSpell, int n, int n2) {
        groupHealingSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(groupHealingSpell), NativeSkillConsumption.baseMp(groupHealingSpell), n, n2, false);
    }

    static int HealingSpell_effectiveMpCost_IIZI(BattleSpellCatalog.HealingSpell healingSpell, int n, int n2, boolean bl) {
        healingSpell.effectiveMpCost(n, n2, bl);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(healingSpell), NativeSkillConsumption.baseMp(healingSpell), n, n2, bl);
    }

    static int EntangleSpell_effectiveMpCost_III(BattleSpellCatalog.EntangleSpell entangleSpell, int n, int n2) {
        entangleSpell.effectiveMpCost(n, n2);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(entangleSpell), NativeSkillConsumption.baseMp(entangleSpell), n, n2, false);
    }

    static int EraseMagicSpell_effectiveMpCost_II(BattleSpellCatalog.EraseMagicSpell eraseMagicSpell, int n, int n2) {
        eraseMagicSpell.effectiveMpCost(n);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(eraseMagicSpell), NativeSkillConsumption.baseMp(eraseMagicSpell), n2, n, false);
    }

    static int HasteSpell_effectiveMpCost_IIZI(BattleSpellCatalog.HasteSpell hasteSpell, int n, int n2, boolean bl) {
        hasteSpell.effectiveMpCost(n, n2, bl);
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(hasteSpell), NativeSkillConsumption.baseMp(hasteSpell), n, n2, bl);
    }

    static int LightSpell_mpCost_I(BattleSpellCatalog.LightSpell lightSpell, int n, int n2) {
        lightSpell.mpCost();
        return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(lightSpell), NativeSkillConsumption.baseMp(lightSpell), n, n2, false);
    }
}
