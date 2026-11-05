// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

final class NativeSkillConsumptionBridges {
   static int PolymorphSpell_effectiveMpCost_III(BattleSpellCatalog.PolymorphSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ShieldSpell_effectiveMpCost_II(BattleSpellCatalog.ShieldSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int CounterMagicSpell_effectiveMpCost_II(BattleSpellCatalog.CounterMagicSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int AbsoluteBarrierSpell_effectiveMpCost_III(BattleSpellCatalog.AbsoluteBarrierSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int AdvanceSpiritSpell_effectiveMpCost_III(BattleSpellCatalog.AdvanceSpiritSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ArmorBreakSpell_effectiveMpCost_II(BattleSpellCatalog.ArmorBreakSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int GuardBrakeSpell_effectiveMpCost_II(BattleSpellCatalog.GuardBrakeSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int ResistFearSpell_effectiveMpCost_II(BattleSpellCatalog.ResistFearSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int ReductionArmorSpell_effectiveMpCost_III(BattleSpellCatalog.ReductionArmorSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int BounceAttackSpell_effectiveMpCost_III(BattleSpellCatalog.BounceAttackSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int SolidCarriageSpell_effectiveMpCost_III(BattleSpellCatalog.SolidCarriageSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int CounterBarrierSpell_effectiveMpCost_III(BattleSpellCatalog.CounterBarrierSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int CubeIgnitionSpell_effectiveMpCost_II(BattleSpellCatalog.CubeIgnitionSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int CubeQuakeSpell_effectiveMpCost_II(BattleSpellCatalog.CubeQuakeSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int CubeShockSpell_effectiveMpCost_II(BattleSpellCatalog.CubeShockSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int CubeHarmonySpell_effectiveMpCost_II(BattleSpellCatalog.CubeHarmonySpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int PatienceSpell_effectiveMpCost_II(BattleSpellCatalog.PatienceSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int PhantasmSpell_effectiveMpCost_II(BattleSpellCatalog.PhantasmSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int PanicSpell_effectiveMpCost_II(BattleSpellCatalog.PanicSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int JoyOfPainSpell_effectiveMpCost_II(BattleSpellCatalog.JoyOfPainSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int IllusionOgreSpell_effectiveMpCost_II(BattleSpellCatalog.IllusionOgreSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int DiamondGolemSpell_effectiveMpCost_II(BattleSpellCatalog.DiamondGolemSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int InsightSpell_effectiveMpCost_II(BattleSpellCatalog.InsightSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int IllusionLichSpell_effectiveMpCost_II(BattleSpellCatalog.IllusionLichSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int IllusionAvatarSpell_effectiveMpCost_II(BattleSpellCatalog.IllusionAvatarSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int AbilityBuffSpell_effectiveMpCost_IIZI(BattleSpellCatalog.AbilityBuffSpell var0, int var1, int var2, boolean var3) {
      var0.effectiveMpCost(var1, var2, var3);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, var3);
   }

   static int WeaponBuffSpell_effectiveMpCost_III(BattleSpellCatalog.WeaponBuffSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int SelfHasteSpell_effectiveMpCost_IIZI(BattleSpellCatalog.SelfHasteSpell var0, int var1, int var2, boolean var3) {
      var0.effectiveMpCost(var1, var2, var3);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, var3);
   }

   static int CombatBuffSpell_effectiveMpCost_III(BattleSpellCatalog.CombatBuffSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int DamageProtectionSpell_effectiveMpCost_III(BattleSpellCatalog.DamageProtectionSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int MassTeleportSpell_effectiveMpCost_III(BattleSpellCatalog.MassTeleportSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int MovementBuffSpell_effectiveMpCost_III(BattleSpellCatalog.MovementBuffSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int PhysicalProcBuffSpell_effectiveMpCost_III(BattleSpellCatalog.PhysicalProcBuffSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int OtherNoEffectSpell_effectiveMpCost_III(BattleSpellCatalog.OtherNoEffectSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int DodgeBuffSpell_effectiveMpCost_III(BattleSpellCatalog.DodgeBuffSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int RangedEvasionBuffSpell_effectiveMpCost_III(BattleSpellCatalog.RangedEvasionBuffSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int PoisonResistanceSpell_effectiveMpCost_III(BattleSpellCatalog.PoisonResistanceSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int MagicResistanceSpell_effectiveMpCost_III(BattleSpellCatalog.MagicResistanceSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int TripleArrowSpell_effectiveMpCost_III(BattleSpellCatalog.TripleArrowSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int WorldTreeCallSpell_effectiveMpCost_III(BattleSpellCatalog.WorldTreeCallSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int CancellationSpell_effectiveMpCost_III(BattleSpellCatalog.CancellationSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ExoticVitalizeSpell_effectiveMpCost_II(BattleSpellCatalog.ExoticVitalizeSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int AdditionalFireSpell_effectiveMpCost_II(BattleSpellCatalog.AdditionalFireSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int WeightSpell_mpCost_I(BattleSpellCatalog.WeightSpell var0, int var1, int var2) {
      var0.mpCost();
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int RegenSpell_effectiveMpCost_III(BattleSpellCatalog.RegenSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int TrueTargetSpell_effectiveMpCost_II(BattleSpellCatalog.TrueTargetSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int RoyalAuraSpell_effectiveMpCost_II(BattleSpellCatalog.RoyalAuraSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int CallClanSpell_effectiveMpCost_II(BattleSpellCatalog.CallClanSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int RunClanSpell_effectiveMpCost_II(BattleSpellCatalog.RunClanSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int InvisibilitySpell_effectiveMpCost_III(BattleSpellCatalog.InvisibilitySpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int EnchantVenomSpell_effectiveMpCost_II(BattleSpellCatalog.EnchantVenomSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int BringStoneSpell_effectiveMpCost_II(BattleSpellCatalog.BringStoneSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int TurnUndeadSpell_mpCost_I(BattleSpellCatalog.TurnUndeadSpell var0, int var1, int var2) {
      var0.mpCost();
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ManaDrainSpell_effectiveMpCost_III(BattleSpellCatalog.ManaDrainSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int BlindSpell_effectiveMpCost_III(BattleSpellCatalog.BlindSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int SilenceSpell_effectiveMpCost_III(BattleSpellCatalog.SilenceSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ElementalFallDownSpell_effectiveMpCost_II(BattleSpellCatalog.ElementalFallDownSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int ReturnToNatureSpell_effectiveMpCost_II(BattleSpellCatalog.ReturnToNatureSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int CounterMirrorSpell_effectiveMpCost_II(BattleSpellCatalog.CounterMirrorSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int ClearMindSpell_effectiveMpCost_II(BattleSpellCatalog.ClearMindSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int ResistElementalSpell_effectiveMpCost_II(BattleSpellCatalog.ResistElementalSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int ElementalProtectionSpell_effectiveMpCost_II(BattleSpellCatalog.ElementalProtectionSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int FireWeaponSpell_effectiveMpCost_II(BattleSpellCatalog.FireWeaponSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int WindShotSpell_effectiveMpCost_II(BattleSpellCatalog.WindShotSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int FireBlessSpell_effectiveMpCost_II(BattleSpellCatalog.FireBlessSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int StormEyeSpell_effectiveMpCost_II(BattleSpellCatalog.StormEyeSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int StormShotSpell_effectiveMpCost_II(BattleSpellCatalog.StormShotSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int EarthBlessSpell_effectiveMpCost_II(BattleSpellCatalog.EarthBlessSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int IronSkinSpell_effectiveMpCost_II(BattleSpellCatalog.IronSkinSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int AwakenSpell_mpCost_I(BattleSpellCatalog.AwakenSpell var0, int var1, int var2) {
      var0.mpCost();
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int BloodlustSpell_mpCost_I(BattleSpellCatalog.BloodlustSpell var0, int var1, int var2) {
      var0.mpCost();
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int FoeSlayerSpell_mpCost_I(BattleSpellCatalog.FoeSlayerSpell var0, int var1, int var2) {
      var0.mpCost();
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int NaturesTouchSpell_effectiveMpCost_II(BattleSpellCatalog.NaturesTouchSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int WaterLifeSpell_effectiveMpCost_II(BattleSpellCatalog.WaterLifeSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int AquaProtectSpell_effectiveMpCost_II(BattleSpellCatalog.AquaProtectSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int AreaSilenceSpell_effectiveMpCost_II(BattleSpellCatalog.AreaSilenceSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int WindShackleSpell_effectiveMpCost_II(BattleSpellCatalog.WindShackleSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int TargetNegativeStatusSpell_effectiveMpCost_II(BattleSpellCatalog.TargetNegativeStatusSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int DecayPotionSpell_effectiveMpCost_III(BattleSpellCatalog.DecayPotionSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ItemWeaponEnchantSpell_effectiveMpCost_IIZI(BattleSpellCatalog.ItemWeaponEnchantSpell var0, int var1, int var2, boolean var3) {
      var0.effectiveMpCost(var1, var2, var3);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, var3);
   }

   static int ItemArmorEnchantSpell_effectiveMpCost_III(BattleSpellCatalog.ItemArmorEnchantSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int MagicalWeaponSpell_effectiveMpCost_III(BattleSpellCatalog.MagicalWeaponSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int RevealWeaknessSpell_effectiveMpCost_III(BattleSpellCatalog.RevealWeaknessSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int DetectionSpell_effectiveMpCost_IIZI(BattleSpellCatalog.DetectionSpell var0, int var1, int var2, boolean var3) {
      var0.effectiveMpCost(var1, var2, var3);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, var3);
   }

   static int CounterDetectionSpell_effectiveMpCost_III(BattleSpellCatalog.CounterDetectionSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int WeaponBreakSpell_effectiveMpCost_III(BattleSpellCatalog.WeaponBreakSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int WeaknessSpell_effectiveMpCost_III(BattleSpellCatalog.WeaknessSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int BindSpell_effectiveMpCost_II(BattleSpellCatalog.BindSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int MassSlowSpell_effectiveMpCost_III(BattleSpellCatalog.MassSlowSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int SleepSpell_effectiveMpCost_III(BattleSpellCatalog.SleepSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int DirectionalLineSpell_effectiveMpCost_III(BattleSpellCatalog.DirectionalLineSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int FreezeAttackSpell_effectiveMpCost_III(BattleSpellCatalog.FreezeAttackSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ShockStunSpell_effectiveMpCost_III(BattleSpellCatalog.ShockStunSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int LineAttackSpell_effectiveMpCost_III(BattleSpellCatalog.LineAttackSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int AreaAttackSpell_effectiveMpCost_III(BattleSpellCatalog.AreaAttackSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int AttackSpell_effectiveMpCost_III(BattleSpellCatalog.AttackSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int PoisonSpell_effectiveMpCost_III(BattleSpellCatalog.PoisonSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ParalysisSpell_effectiveMpCost_III(BattleSpellCatalog.ParalysisSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int CurseRemovalSpell_effectiveMpCost_III(BattleSpellCatalog.CurseRemovalSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ElementalSummonSpell_effectiveMpCost_I(BattleSpellCatalog.ElementalSummonSpell var0, int var1, int var2) {
      var0.effectiveMpCost();
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int SummonSpell_effectiveMpCost_III(BattleSpellCatalog.SummonSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int CreateZombieSpell_effectiveMpCost_III(BattleSpellCatalog.CreateZombieSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int ResurrectionSpell_effectiveMpCost_III(BattleSpellCatalog.ResurrectionSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int TamingSpell_effectiveMpCost_III(BattleSpellCatalog.TamingSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int GroundSpell_effectiveMpCost_III(BattleSpellCatalog.GroundSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int NaturesBlessingSpell_effectiveMpCost_II(BattleSpellCatalog.NaturesBlessingSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int GroupHealingSpell_effectiveMpCost_III(BattleSpellCatalog.GroupHealingSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int HealingSpell_effectiveMpCost_IIZI(BattleSpellCatalog.HealingSpell var0, int var1, int var2, boolean var3) {
      var0.effectiveMpCost(var1, var2, var3);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, var3);
   }

   static int EntangleSpell_effectiveMpCost_III(BattleSpellCatalog.EntangleSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1, var2);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }

   static int EraseMagicSpell_effectiveMpCost_II(BattleSpellCatalog.EraseMagicSpell var0, int var1, int var2) {
      var0.effectiveMpCost(var1);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var2, var1, false);
   }

   static int HasteSpell_effectiveMpCost_IIZI(BattleSpellCatalog.HasteSpell var0, int var1, int var2, boolean var3) {
      var0.effectiveMpCost(var1, var2, var3);
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, var3);
   }

   static int LightSpell_mpCost_I(BattleSpellCatalog.LightSpell var0, int var1, int var2) {
      var0.mpCost();
      return NativeSourceSkillConsumptionMath.mpCost(NativeSkillConsumption.skillId(var0), NativeSkillConsumption.baseMp(var0), var1, var2, false);
   }
}
