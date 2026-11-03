package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleAbilityStatusTest {
    @Test void shieldUsesCeilingSecondsRefreshesAndExpiresExactly() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateShield(start, 1_800, -2);

        assertEquals(new BattleAbilityStatus.ArmorProtection(
                        BattleSpellCatalog.SHIELD, -2, 1_800),
                status.armorProtection(start));
        assertEquals(new BattleAbilityStatus.Modifier(-2, 1_800), status.shield(start));
        assertEquals(new BattleAbilityStatus.Modifier(-2, 1),
                status.shield(start + TimeUnit.SECONDS.toNanos(1_799) + 1));
        assertFalse(status.expireShield(start + TimeUnit.SECONDS.toNanos(1_800) - 1));
        assertTrue(status.expireShield(start + TimeUnit.SECONDS.toNanos(1_800)));
        assertEquals(new BattleAbilityStatus.Modifier(0, 0), status.shield(start));

        status.activateShield(start, 1_800, -2);
        status.activateShield(start + TimeUnit.SECONDS.toNanos(100), 1_800, -2);
        assertEquals(1_800,
                status.shield(start + TimeUnit.SECONDS.toNanos(100)).remainingSeconds());
    }

    @Test void shadowArmorReplacesShieldAndPreservesItsSkillIdThroughExpiry() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(15);
        status.activateShield(start, 1_800, -2);
        status.activateArmorProtection(BattleSpellCatalog.SHADOW_ARMOR,
                start + 1, 960, -3);

        assertEquals(new BattleAbilityStatus.ArmorProtection(
                        BattleSpellCatalog.SHADOW_ARMOR, -3, 960),
                status.armorProtection(start + 1));
        assertEquals(new BattleAbilityStatus.Modifier(-3, 960),
                status.shield(start + 1));
        assertEquals(0, status.expireArmorProtection(
                start + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleSpellCatalog.SHADOW_ARMOR,
                status.expireArmorProtection(
                        start + TimeUnit.SECONDS.toNanos(960) + 1));
        assertEquals(new BattleAbilityStatus.ArmorProtection(0, 0, 0),
                status.armorProtection(start + 1));
    }

    @Test void earthSkinOccupiesTheNativeShieldReplacementSlotWithMinusSixAc() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(17);
        status.activateArmorProtection(BattleSpellCatalog.SHADOW_ARMOR,
                start, 960, -3);

        assertEquals(BattleSpellCatalog.SHADOW_ARMOR,
                status.armorProtection(start).skillId());
        status.activateArmorProtection(BattleSpellCatalog.EARTH_SKIN,
                start + 1, 960, -6);
        assertEquals(new BattleAbilityStatus.ArmorProtection(
                        BattleSpellCatalog.EARTH_SKIN, -6, 960),
                status.armorProtection(start + 1));
        assertEquals(new BattleAbilityStatus.Modifier(-6, 960),
                status.shield(start + 1));
        assertEquals(BattleSpellCatalog.EARTH_SKIN,
                status.cancelMagicEffects().armorProtectionSkillId());
        assertEquals(new BattleAbilityStatus.ArmorProtection(0, 0, 0),
                status.armorProtection(start + 1));
    }

    @Test void strengthAndDexterityAreIndependentAuthoritativeLeases() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(20);
        status.activateStrength(start, 1_200, 5);
        status.activateDexterity(start, 1_200, 5);

        assertEquals(new BattleAbilityStatus.Modifier(5, 1_200), status.strength(start));
        assertEquals(new BattleAbilityStatus.Modifier(5, 1_200), status.dexterity(start));
        assertTrue(status.expireStrength(start + TimeUnit.SECONDS.toNanos(1_200)));
        assertEquals(new BattleAbilityStatus.Modifier(0, 0), status.strength(start));
        assertEquals(5, status.dexterity(start).amount());
        assertTrue(status.expireDexterity(start + TimeUnit.SECONDS.toNanos(1_200)));
    }

    @Test void strengthFamilyTracksReplacementSourceRefreshAndExactExpiry() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(25);
        assertEquals(0, status.activateStrength(
                BattleSpellCatalog.PHYSICAL_ENCHANT_STR, start, 1_200, 5));
        assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                status.strengthSkillId(start));

        long dressStart = start + 1;
        assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                status.activateStrength(BattleSpellCatalog.DRESS_MIGHTY,
                        dressStart, 960, 2));
        assertEquals(BattleSpellCatalog.DRESS_MIGHTY,
                status.strengthSkillId(dressStart));
        assertEquals(new BattleAbilityStatus.Modifier(2, 960),
                status.strength(dressStart));
        assertEquals(0, status.activateStrength(BattleSpellCatalog.DRESS_MIGHTY,
                dressStart + 1, 960, 2));
        assertEquals(0, status.expireStrengthSkillId(
                dressStart + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleSpellCatalog.DRESS_MIGHTY,
                status.expireStrengthSkillId(
                        dressStart + TimeUnit.SECONDS.toNanos(960) + 1));
        assertEquals(0, status.strengthSkillId(dressStart));
    }

    @Test void dexterityFamilyTracksReplacementSourceRefreshAndExactExpiry() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(27);
        assertEquals(0, status.activateDexterity(
                BattleSpellCatalog.PHYSICAL_ENCHANT_DEX, start, 1_200, 5));
        assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
                status.dexteritySkillId(start));

        long dressStart = start + 1;
        assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
                status.activateDexterity(BattleSpellCatalog.DRESS_DEXTERITY,
                        dressStart, 960, 2));
        assertEquals(BattleSpellCatalog.DRESS_DEXTERITY,
                status.dexteritySkillId(dressStart));
        assertEquals(new BattleAbilityStatus.Modifier(2, 960),
                status.dexterity(dressStart));
        assertEquals(0, status.activateDexterity(
                BattleSpellCatalog.DRESS_DEXTERITY,
                dressStart + 1, 960, 2));
        assertEquals(0, status.expireDexteritySkillId(
                dressStart + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleSpellCatalog.DRESS_DEXTERITY,
                status.expireDexteritySkillId(
                        dressStart + TimeUnit.SECONDS.toNanos(960) + 1));
        assertEquals(0, status.dexteritySkillId(dressStart));
    }

    @Test void berserkUsesExactModifiersCeilingSecondsAndExpiry() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(30);
        status.activateBerserk(start, 320, 10, 2, 5);

        assertEquals(new BattleAbilityStatus.Berserk(10, 2, 5, 320),
                status.berserk(start));
        assertEquals(new BattleAbilityStatus.Berserk(10, 2, 5, 1),
                status.berserk(start + TimeUnit.SECONDS.toNanos(319) + 1));
        assertFalse(status.expireBerserk(
                start + TimeUnit.SECONDS.toNanos(320) - 1));
        assertTrue(status.expireBerserk(start + TimeUnit.SECONDS.toNanos(320)));
        assertEquals(new BattleAbilityStatus.Berserk(0, 0, 0, 0),
                status.berserk(start));
    }

    @Test void clearRemovesEveryBuffFamily() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = 1;
        status.activateShield(start, 1, -2);
        status.activateStrength(start, 1, 5);
        status.activateDexterity(start, 1, 5);
        status.activateBerserk(start, 320, 10, 2, 5);
        status.activateImmuneToHarm(start, 32, 2);
        status.activateReductionArmor(start, 192);
        status.activateBounceAttack(start, 64, -2, 6, 10);
        status.activateSolidCarriage(start, 64, 15);
        status.activateCounterBarrier(start, 64);
        status.activateRoyalAura(BattleSpellCatalog.GLOWING_AURA,
                start, 640, 0, 5, 5, 20);
        status.activateBraveAura(start, 640, 5);
        status.activateIllusionOgre(start, 64, 4, 4);
        status.activateIllusionLich(start, 64, 2);
        status.activateIllusionAvatar(start, 64, 10, 10, 8, 1.5D);
        status.activateDiamondGolem(start, 64, -20);
        status.activateMagicResistance(start, 1_200, 10);
        status.clear();

        assertEquals(new BattleAbilityStatus.Modifier(0, 0), status.shield(start));
        assertEquals(new BattleAbilityStatus.Modifier(0, 0), status.strength(start));
        assertEquals(new BattleAbilityStatus.Modifier(0, 0), status.dexterity(start));
        assertEquals(new BattleAbilityStatus.Berserk(0, 0, 0, 0),
                status.berserk(start));
        assertEquals(new BattleAbilityStatus.Protection(0, 0),
                status.immuneToHarm(start));
        assertEquals(new BattleAbilityStatus.ReductionArmor(0),
                status.reductionArmor(start));
        assertEquals(new BattleAbilityStatus.BounceAttack(0, 0, 0, 0),
                status.bounceAttack(start));
        assertEquals(new BattleAbilityStatus.SolidCarriage(0, 0),
                status.solidCarriage(start));
        assertEquals(new BattleAbilityStatus.CounterBarrier(0),
                status.counterBarrier(start));
        assertEquals(new BattleAbilityStatus.RoyalAura(0, 0, 0, 0, 0, 0),
                status.royalAura(start));
        assertEquals(new BattleAbilityStatus.BraveAura(0, 0),
                status.braveAura(start));
        assertEquals(new BattleAbilityStatus.IllusionOgre(0, 0, 0),
                status.illusionOgre(start));
        assertEquals(new BattleAbilityStatus.IllusionLich(0, 0),
                status.illusionLich(start));
        assertEquals(new BattleAbilityStatus.IllusionAvatar(
                        0, 0, 0, 1.0D, 0),
                status.illusionAvatar(start));
        assertEquals(new BattleAbilityStatus.DiamondGolem(0, 0),
                status.diamondGolem(start));
        assertEquals(new BattleAbilityStatus.MagicResistance(0, 0),
                status.magicResistance(start));
    }

    @Test void invalidNativeLeaseInputsAreRejected() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activateShield(-1, 1_800, -2));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateStrength(0, 0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateDexterity(0, 1_200, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateBerserk(0, 320, 0, 2, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateImmuneToHarm(0, 32, 1));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateReductionArmor(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateBounceAttack(0, 64, 0, 6, 10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateSolidCarriage(0, 64, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateCounterBarrier(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateRoyalAura(
                        BattleSpellCatalog.GLOWING_AURA, 0, 640, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateBraveAura(0, 640, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateMagicResistance(0, 1_200, -10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateIllusionOgre(0, 64, 0, 4));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateIllusionLich(0, 64, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateIllusionAvatar(
                        0, 64, 10, 10, 8, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateDiamondGolem(0, 64, 0));
    }

    @Test void immuneToHarmRefreshesExpiresAndTruncatesPositiveDamage() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(40);
        status.activateImmuneToHarm(start, 32, 2);

        BattleAbilityStatus.Protection active = status.immuneToHarm(start);
        assertEquals(new BattleAbilityStatus.Protection(2, 32), active);
        assertEquals(2, active.applyTo(5));
        assertEquals(0, active.applyTo(1));
        assertEquals(0, active.applyTo(0));
        assertFalse(status.expireImmuneToHarm(
                start + TimeUnit.SECONDS.toNanos(32) - 1));
        status.activateImmuneToHarm(
                start + TimeUnit.SECONDS.toNanos(10), 32, 2);
        assertEquals(32, status.immuneToHarm(
                start + TimeUnit.SECONDS.toNanos(10)).remainingSeconds());
        assertTrue(status.expireImmuneToHarm(
                start + TimeUnit.SECONDS.toNanos(42)));
        assertEquals(new BattleAbilityStatus.Protection(0, 0),
                status.immuneToHarm(start));
        assertThrows(IllegalArgumentException.class, () -> active.applyTo(-1));
    }

    @Test void magicCancellationReturnsExactFamiliesAndClearsThem() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        status.activateShield(0, 1_800, -2);
        status.activateStrength(0, 1_200, 5);
        status.activateBerserk(0, 320, 10, 2, 5);
        status.activateImmuneToHarm(0, 32, 2);
        status.activateReductionArmor(0, 192);
        status.activateBounceAttack(0, 64, -2, 6, 10);
        status.activateSolidCarriage(0, 64, 15);
        assertTrue(status.activateUncannyDodge(0, 192, 5));
        assertTrue(status.activateMirrorImage(0, 1_200, 5));
        assertFalse(status.activateIllusionOgre(0, 64, 4, 4));
        assertFalse(status.activateIllusionLich(0, 64, 2));
        assertFalse(status.activateIllusionAvatar(
                0, 64, 10, 10, 8, 1.5D));
        assertFalse(status.activateDiamondGolem(0, 64, -20));
        assertTrue(status.activatePanic(0, 64, -1));
        status.activateDressEvasion(0, 32, 12);
        status.activateRoyalAura(BattleSpellCatalog.GLOWING_AURA,
                0, 640, 0, 5, 5, 20);
        status.activateBraveAura(0, 640, 5);
        status.activateMagicResistance(0, 1_200, 10);
        status.activateMortalBody(0, 300);

        assertEquals(new BattleAbilityStatus.Cancellation(
                        BattleSpellCatalog.SHIELD,
                        BattleSpellCatalog.PHYSICAL_ENCHANT_STR, 0,
                        BattleSpellCatalog.GLOWING_AURA,
                        BattleSpellCatalog.BRAVE_AURA,
                        true,  // magic resistance
                        true,  // berserkers
                        true,  // immune to harm
                        true,  // bounce attack
                        true,  // uncanny dodge
                        true,  // mirror image
                        true,  // illusion ogre
                        true,  // illusion lich
                        true,  // illusion avatar
                        true,  // diamond golem
                        false, // insight
                        true,  // panic
                        true,  // dress evasion
                        true), // mortal body
                status.cancelMagicEffects());
        assertEquals(new BattleAbilityStatus.Modifier(0, 0), status.shield(0));
        assertEquals(new BattleAbilityStatus.Modifier(0, 0), status.strength(0));
        assertEquals(new BattleAbilityStatus.Berserk(0, 0, 0, 0), status.berserk(0));
        assertEquals(new BattleAbilityStatus.Protection(0, 0), status.immuneToHarm(0));
        assertEquals(new BattleAbilityStatus.ReductionArmor(192),
                status.reductionArmor(0));
        assertEquals(new BattleAbilityStatus.BounceAttack(0, 0, 0, 0),
                status.bounceAttack(0));
        assertEquals(new BattleAbilityStatus.SolidCarriage(15, 64),
                status.solidCarriage(0));
        assertEquals(new BattleAbilityStatus.UncannyDodge(0, 0),
                status.uncannyDodge(0));
        assertEquals(new BattleAbilityStatus.MirrorImage(0, 0),
                status.mirrorImage(0));
        assertEquals(new BattleAbilityStatus.IllusionOgre(0, 0, 0),
                status.illusionOgre(0));
        assertEquals(new BattleAbilityStatus.IllusionLich(0, 0),
                status.illusionLich(0));
        assertEquals(new BattleAbilityStatus.IllusionAvatar(
                        0, 0, 0, 1.0D, 0),
                status.illusionAvatar(0));
        assertEquals(new BattleAbilityStatus.DiamondGolem(0, 0),
                status.diamondGolem(0));
        assertEquals(new BattleAbilityStatus.DressEvasion(0, 0),
                status.dressEvasion(0));
        assertEquals(new BattleAbilityStatus.RoyalAura(0, 0, 0, 0, 0, 0),
                status.royalAura(0));
        assertEquals(new BattleAbilityStatus.BraveAura(0, 0),
                status.braveAura(0));
        assertEquals(new BattleAbilityStatus.MagicResistance(0, 0),
                status.magicResistance(0));
        assertEquals(new BattleAbilityStatus.MortalBody(0),
                status.mortalBody(0));
        assertEquals(new BattleAbilityStatus.Cancellation(
                        0, 0, 0, 0, 0,
                        false,
                        false, false, false, false, false, false, false, false, false,
                        false, false, false, false),
                status.cancelMagicEffects());
    }

    @Test void resistMagicRefreshesExpiresAndIsCancelable() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(105);
        status.activateMagicResistance(start, 1_200, 10);
        assertEquals(new BattleAbilityStatus.MagicResistance(10, 1_200),
                status.magicResistance(start));
        assertFalse(status.expireMagicResistance(
                start + TimeUnit.SECONDS.toNanos(1_200) - 1));

        long refresh = start + TimeUnit.SECONDS.toNanos(100);
        status.activateMagicResistance(refresh, 1_200, 10);
        assertEquals(new BattleAbilityStatus.MagicResistance(10, 1_200),
                status.magicResistance(refresh));
        assertTrue(status.cancelMagicEffects().magicResistance());
        assertEquals(new BattleAbilityStatus.MagicResistance(0, 0),
                status.magicResistance(refresh));

        status.activateMagicResistance(start, 1_200, 10);
        assertTrue(status.expireMagicResistance(
                start + TimeUnit.SECONDS.toNanos(1_200)));
    }

    @Test void royalAuraRefreshesReplacesExpiresAndIsRemovedByCancellation() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(85);
        assertEquals(0, status.activateRoyalAura(
                BattleSpellCatalog.GLOWING_AURA, start, 640, 0, 5, 5, 20));
        assertEquals(new BattleAbilityStatus.RoyalAura(
                        BattleSpellCatalog.GLOWING_AURA, 0, 5, 5, 20, 640),
                status.royalAura(start));
        assertEquals(0, status.activateRoyalAura(
                BattleSpellCatalog.GLOWING_AURA, start + 1, 640, 0, 5, 5, 20));

        int shiningAura = BattleSpellCatalog.SHINING_AURA;
        long replacement = start + 2;
        assertEquals(BattleSpellCatalog.GLOWING_AURA,
                status.activateRoyalAura(
                        shiningAura, replacement, 640, -8, 0, 0, 0));
        assertEquals(shiningAura, status.royalAura(replacement).skillId());
        assertEquals(-8, status.royalAura(replacement).armorClass());
        assertEquals(shiningAura,
                status.cancelMagicEffects().royalAuraSkillId());
        assertEquals(new BattleAbilityStatus.RoyalAura(0, 0, 0, 0, 0, 0),
                status.royalAura(replacement));

        status.activateRoyalAura(shiningAura,
                replacement + 1, 640, -8, 0, 0, 0);
        assertEquals(shiningAura, status.activateRoyalAura(
                BattleSpellCatalog.GLOWING_AURA,
                replacement + 2, 640, 0, 5, 5, 20));
        assertEquals(new BattleAbilityStatus.RoyalAura(
                        BattleSpellCatalog.GLOWING_AURA, 0, 5, 5, 20, 640),
                status.royalAura(replacement + 2));

        status.activateRoyalAura(BattleSpellCatalog.GLOWING_AURA,
                start, 640, 0, 5, 5, 20);
        assertEquals(0, status.expireRoyalAura(
                start + TimeUnit.SECONDS.toNanos(640) - 1));
        assertEquals(BattleSpellCatalog.GLOWING_AURA,
                status.expireRoyalAura(start + TimeUnit.SECONDS.toNanos(640)));
    }

    @Test void braveAuraCoexistsWithRoyalAuraRefreshesExpiresAndCancels() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(95);
        status.activateRoyalAura(BattleSpellCatalog.SHINING_AURA,
                start, 640, -8, 0, 0, 0);
        status.activateBraveAura(start, 640, 5);

        assertEquals(new BattleAbilityStatus.RoyalAura(
                        BattleSpellCatalog.SHINING_AURA, -8, 0, 0, 0, 640),
                status.royalAura(start));
        assertEquals(new BattleAbilityStatus.BraveAura(5, 640),
                status.braveAura(start));
        status.activateBraveAura(start + 1, 640, 5);
        assertEquals(640, status.braveAura(start + 1).remainingSeconds());
        assertFalse(status.expireBraveAura(
                start + 1 + TimeUnit.SECONDS.toNanos(640) - 1));
        assertTrue(status.expireBraveAura(
                start + 1 + TimeUnit.SECONDS.toNanos(640)));
        assertEquals(BattleSpellCatalog.SHINING_AURA,
                status.royalAura(start + 1).skillId());

        status.activateBraveAura(start + 2, 640, 5);
        assertEquals(BattleSpellCatalog.BRAVE_AURA,
                status.cancelMagicEffects().braveAuraSkillId());
        assertEquals(new BattleAbilityStatus.BraveAura(0, 0),
                status.braveAura(start + 2));
    }

    @Test void reductionArmorRefreshesExpiresAndSurvivesCancellation() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(50);
        status.activateReductionArmor(start, 192);
        assertEquals(new BattleAbilityStatus.ReductionArmor(192),
                status.reductionArmor(start));
        assertEquals(new BattleAbilityStatus.ReductionArmor(1),
                status.reductionArmor(
                        start + TimeUnit.SECONDS.toNanos(191) + 1));
        assertFalse(status.expireReductionArmor(
                start + TimeUnit.SECONDS.toNanos(192) - 1));

        status.activateReductionArmor(
                start + TimeUnit.SECONDS.toNanos(100), 192);
        status.cancelMagicEffects();
        assertEquals(new BattleAbilityStatus.ReductionArmor(192),
                status.reductionArmor(
                        start + TimeUnit.SECONDS.toNanos(100)));
        assertTrue(status.expireReductionArmor(
                start + TimeUnit.SECONDS.toNanos(292)));
        assertEquals(new BattleAbilityStatus.ReductionArmor(0),
                status.reductionArmor(
                        start + TimeUnit.SECONDS.toNanos(292)));
        assertFalse(status.expireReductionArmor(
                start + TimeUnit.SECONDS.toNanos(292)));
    }

    @Test void bounceAttackRefreshesExpiresAndIsRemovedByCancellation() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(60);
        status.activateBounceAttack(start, 64, -2, 6, 10);
        assertEquals(new BattleAbilityStatus.BounceAttack(-2, 6, 10, 64),
                status.bounceAttack(start));
        assertEquals(new BattleAbilityStatus.BounceAttack(-2, 6, 10, 1),
                status.bounceAttack(start + TimeUnit.SECONDS.toNanos(63) + 1));
        assertFalse(status.expireBounceAttack(
                start + TimeUnit.SECONDS.toNanos(64) - 1));

        status.activateBounceAttack(
                start + TimeUnit.SECONDS.toNanos(20), 64, -2, 6, 10);
        assertEquals(64, status.bounceAttack(
                start + TimeUnit.SECONDS.toNanos(20)).remainingSeconds());
        assertTrue(status.cancelMagicEffects().bounceAttack());
        assertEquals(new BattleAbilityStatus.BounceAttack(0, 0, 0, 0),
                status.bounceAttack(start + TimeUnit.SECONDS.toNanos(20)));
        assertFalse(status.expireBounceAttack(
                start + TimeUnit.SECONDS.toNanos(84)));
    }

    @Test void solidCarriageRefreshesExpiresSurvivesCancellationAndClearsOnUnequip() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(70);
        status.activateSolidCarriage(start, 64, 15);
        assertEquals(new BattleAbilityStatus.SolidCarriage(15, 64),
                status.solidCarriage(start));
        assertEquals(new BattleAbilityStatus.SolidCarriage(15, 1),
                status.solidCarriage(start + TimeUnit.SECONDS.toNanos(63) + 1));
        assertFalse(status.expireSolidCarriage(
                start + TimeUnit.SECONDS.toNanos(64) - 1));

        status.activateSolidCarriage(
                start + TimeUnit.SECONDS.toNanos(20), 64, 15);
        status.cancelMagicEffects();
        assertEquals(new BattleAbilityStatus.SolidCarriage(15, 64),
                status.solidCarriage(start + TimeUnit.SECONDS.toNanos(20)));
        assertTrue(status.removeSolidCarriage());
        assertFalse(status.removeSolidCarriage());
        assertEquals(new BattleAbilityStatus.SolidCarriage(0, 0),
                status.solidCarriage(start + TimeUnit.SECONDS.toNanos(20)));
    }

    @Test void uncannyDodgeDoesNotRefreshAndIsRemovedByCancellation() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(75);
        assertTrue(status.activateUncannyDodge(start, 192, 5));
        assertEquals(new BattleAbilityStatus.UncannyDodge(5, 192),
                status.uncannyDodge(start));

        long recast = start + TimeUnit.SECONDS.toNanos(50);
        assertFalse(status.activateUncannyDodge(recast, 192, 5));
        assertEquals(new BattleAbilityStatus.UncannyDodge(5, 142),
                status.uncannyDodge(recast));
        assertFalse(status.expireUncannyDodge(
                start + TimeUnit.SECONDS.toNanos(192) - 1));
        assertTrue(status.cancelMagicEffects().uncannyDodge());
        assertEquals(new BattleAbilityStatus.UncannyDodge(0, 0),
                status.uncannyDodge(recast));
        assertFalse(status.expireUncannyDodge(
                start + TimeUnit.SECONDS.toNanos(192)));

        assertTrue(status.activateUncannyDodge(start, 192, 5));
        assertTrue(status.expireUncannyDodge(
                start + TimeUnit.SECONDS.toNanos(192)));
    }

    @Test void mirrorImageIsIndependentDoesNotRefreshAndIsCancelable() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(80);
        assertTrue(status.activateUncannyDodge(start, 192, 5));
        assertTrue(status.activateMirrorImage(start, 1_200, 5));
        assertEquals(new BattleAbilityStatus.UncannyDodge(5, 192),
                status.uncannyDodge(start));
        assertEquals(new BattleAbilityStatus.MirrorImage(5, 1_200),
                status.mirrorImage(start));

        long recast = start + TimeUnit.SECONDS.toNanos(50);
        assertFalse(status.activateMirrorImage(recast, 1_200, 5));
        assertEquals(new BattleAbilityStatus.MirrorImage(5, 1_150),
                status.mirrorImage(recast));
        assertFalse(status.expireMirrorImage(
                start + TimeUnit.SECONDS.toNanos(1_200) - 1));
        BattleAbilityStatus.Cancellation cancellation = status.cancelMagicEffects();
        assertTrue(cancellation.uncannyDodge());
        assertTrue(cancellation.mirrorImage());
        assertEquals(new BattleAbilityStatus.MirrorImage(0, 0),
                status.mirrorImage(recast));

        assertTrue(status.activateMirrorImage(start, 1_200, 5));
        assertTrue(status.expireMirrorImage(
                start + TimeUnit.SECONDS.toNanos(1_200)));
    }

    @Test void illusionOgreRefreshesExactlyExpiresAndIsCancelable() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(82);
        assertFalse(status.activateIllusionOgre(start, 64, 4, 4));
        assertEquals(new BattleAbilityStatus.IllusionOgre(4, 4, 64),
                status.illusionOgre(start));
        assertEquals(new BattleAbilityStatus.IllusionOgre(4, 4, 1),
                status.illusionOgre(
                        start + TimeUnit.SECONDS.toNanos(63) + 1));

        long recast = start + TimeUnit.SECONDS.toNanos(20);
        assertTrue(status.activateIllusionOgre(recast, 64, 4, 4));
        assertEquals(new BattleAbilityStatus.IllusionOgre(4, 4, 64),
                status.illusionOgre(recast));
        assertFalse(status.expireIllusionOgre(
                recast + TimeUnit.SECONDS.toNanos(64) - 1));
        assertTrue(status.cancelMagicEffects().illusionOgre());
        assertEquals(new BattleAbilityStatus.IllusionOgre(0, 0, 0),
                status.illusionOgre(recast));

        assertFalse(status.activateIllusionOgre(start, 64, 4, 4));
        assertTrue(status.expireIllusionOgre(
                start + TimeUnit.SECONDS.toNanos(64)));
    }

    @Test void illusionLichRefreshesExactlyExpiresAndIsCancelable() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(83);
        assertFalse(status.activateIllusionLich(start, 64, 2));
        assertEquals(new BattleAbilityStatus.IllusionLich(2, 64),
                status.illusionLich(start));
        assertEquals(new BattleAbilityStatus.IllusionLich(2, 1),
                status.illusionLich(
                        start + TimeUnit.SECONDS.toNanos(63) + 1));

        long recast = start + TimeUnit.SECONDS.toNanos(20);
        assertTrue(status.activateIllusionLich(recast, 64, 2));
        assertEquals(new BattleAbilityStatus.IllusionLich(2, 64),
                status.illusionLich(recast));
        assertFalse(status.expireIllusionLich(
                recast + TimeUnit.SECONDS.toNanos(64) - 1));
        assertTrue(status.cancelMagicEffects().illusionLich());
        assertEquals(new BattleAbilityStatus.IllusionLich(0, 0),
                status.illusionLich(recast));

        assertFalse(status.activateIllusionLich(start, 64, 2));
        assertTrue(status.expireIllusionLich(
                start + TimeUnit.SECONDS.toNanos(64)));
    }

    @Test void illusionAvatarRefreshesTruncatesDamageExpiresAndIsCancelable() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(83);
        assertFalse(status.activateIllusionAvatar(
                start, 64, 10, 10, 8, 1.5D));
        BattleAbilityStatus.IllusionAvatar active = status.illusionAvatar(start);
        assertEquals(new BattleAbilityStatus.IllusionAvatar(
                10, 10, 8, 1.5D, 64), active);
        assertEquals(1, active.applyIncomingDamage(1));
        assertEquals(4, active.applyIncomingDamage(3));
        assertThrows(IllegalArgumentException.class,
                () -> active.applyIncomingDamage(-1));

        long recast = start + TimeUnit.SECONDS.toNanos(20);
        assertTrue(status.activateIllusionAvatar(
                recast, 64, 10, 10, 8, 1.5D));
        assertEquals(new BattleAbilityStatus.IllusionAvatar(
                        10, 10, 8, 1.5D, 64),
                status.illusionAvatar(recast));
        assertFalse(status.expireIllusionAvatar(
                recast + TimeUnit.SECONDS.toNanos(64) - 1));
        assertTrue(status.cancelMagicEffects().illusionAvatar());
        assertEquals(new BattleAbilityStatus.IllusionAvatar(
                        0, 0, 0, 1.0D, 0),
                status.illusionAvatar(recast));

        assertFalse(status.activateIllusionAvatar(
                start, 64, 10, 10, 8, 1.5D));
        assertTrue(status.expireIllusionAvatar(
                start + TimeUnit.SECONDS.toNanos(64)));
    }

    @Test void diamondGolemRefreshesExactlyExpiresAndIsCancelable() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(84);
        assertFalse(status.activateDiamondGolem(start, 64, -20));
        assertEquals(new BattleAbilityStatus.DiamondGolem(-20, 64),
                status.diamondGolem(start));
        assertEquals(new BattleAbilityStatus.DiamondGolem(-20, 1),
                status.diamondGolem(
                        start + TimeUnit.SECONDS.toNanos(63) + 1));

        long recast = start + TimeUnit.SECONDS.toNanos(20);
        assertTrue(status.activateDiamondGolem(recast, 64, -20));
        assertEquals(new BattleAbilityStatus.DiamondGolem(-20, 64),
                status.diamondGolem(recast));
        assertFalse(status.expireDiamondGolem(
                recast + TimeUnit.SECONDS.toNanos(64) - 1));
        assertTrue(status.cancelMagicEffects().diamondGolem());
        assertEquals(new BattleAbilityStatus.DiamondGolem(0, 0),
                status.diamondGolem(recast));

        assertFalse(status.activateDiamondGolem(start, 64, -20));
        assertTrue(status.expireDiamondGolem(
                start + TimeUnit.SECONDS.toNanos(64)));
    }

    @Test void insightDoesNotStackOrRefreshAndExactlyExpiresOrCancels() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(85);
        assertTrue(status.activateInsight(start, 600, 1));
        assertEquals(new BattleAbilityStatus.Insight(1, 600),
                status.insight(start));
        assertEquals(new BattleAbilityStatus.Insight(1, 1),
                status.insight(start + TimeUnit.SECONDS.toNanos(599) + 1));

        long recast = start + TimeUnit.SECONDS.toNanos(20);
        assertFalse(status.activateInsight(recast, 600, 1));
        assertEquals(new BattleAbilityStatus.Insight(1, 580),
                status.insight(recast));
        assertFalse(status.expireInsight(
                start + TimeUnit.SECONDS.toNanos(600) - 1));
        assertTrue(status.expireInsight(
                start + TimeUnit.SECONDS.toNanos(600)));
        assertEquals(new BattleAbilityStatus.Insight(0, 0),
                status.insight(start + TimeUnit.SECONDS.toNanos(600)));

        assertTrue(status.activateInsight(start, 600, 1));
        assertTrue(status.cancelMagicEffects().insight());
        assertEquals(new BattleAbilityStatus.Insight(0, 0),
                status.insight(start));
    }

    @Test void dressEvasionRefreshesExpiresAndIsRemovedByCancellation() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(77);
        status.activateDressEvasion(start, 32, 12);
        assertEquals(new BattleAbilityStatus.DressEvasion(12, 32),
                status.dressEvasion(start));
        assertEquals(new BattleAbilityStatus.DressEvasion(12, 1),
                status.dressEvasion(start + TimeUnit.SECONDS.toNanos(31) + 1));
        assertFalse(status.expireDressEvasion(
                start + TimeUnit.SECONDS.toNanos(32) - 1));

        long recast = start + TimeUnit.SECONDS.toNanos(20);
        status.activateDressEvasion(recast, 32, 12);
        assertEquals(new BattleAbilityStatus.DressEvasion(12, 32),
                status.dressEvasion(recast));
        assertTrue(status.cancelMagicEffects().dressEvasion());
        assertEquals(new BattleAbilityStatus.DressEvasion(0, 0),
                status.dressEvasion(recast));
        assertFalse(status.expireDressEvasion(
                recast + TimeUnit.SECONDS.toNanos(32)));

        status.activateDressEvasion(start, 32, 12);
        assertTrue(status.expireDressEvasion(
                start + TimeUnit.SECONDS.toNanos(32)));
    }

    @Test void counterBarrierRefreshesExpiresSurvivesCancellationAndClearsOnUnequip() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(80);
        status.activateCounterBarrier(start, 64);
        assertEquals(new BattleAbilityStatus.CounterBarrier(64),
                status.counterBarrier(start));
        assertEquals(new BattleAbilityStatus.CounterBarrier(1),
                status.counterBarrier(start + TimeUnit.SECONDS.toNanos(63) + 1));
        assertFalse(status.expireCounterBarrier(
                start + TimeUnit.SECONDS.toNanos(64) - 1));

        status.activateCounterBarrier(
                start + TimeUnit.SECONDS.toNanos(20), 64);
        status.cancelMagicEffects();
        assertEquals(new BattleAbilityStatus.CounterBarrier(64),
                status.counterBarrier(start + TimeUnit.SECONDS.toNanos(20)));
        assertTrue(status.removeCounterBarrier());
        assertFalse(status.removeCounterBarrier());
        assertEquals(new BattleAbilityStatus.CounterBarrier(0),
                status.counterBarrier(start + TimeUnit.SECONDS.toNanos(20)));

        status.activateCounterBarrier(start, 64);
        assertTrue(status.expireCounterBarrier(
                start + TimeUnit.SECONDS.toNanos(64)));
    }

    @Test void namedRemovalClearsOnlyTheRequestedAutoSkillLease() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long now = TimeUnit.SECONDS.toNanos(90);
        status.activateArmorProtection(BattleSpellCatalog.SHIELD,
                now, 1_800, -2);
        status.activateStrength(BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                now, 1_200, 5);
        status.activateDexterity(BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
                now, 1_200, 5);
        status.activateBerserk(now, 320, 10, 2, 5);

        assertFalse(status.removeSkill(999));
        assertTrue(status.removeSkill(BattleSpellCatalog.SHIELD));
        assertEquals(0, status.armorProtection(now).skillId());
        assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                status.strengthSkillId(now));
        assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
                status.dexteritySkillId(now));
        assertTrue(status.removeSkill(BattleSpellCatalog.PHYSICAL_ENCHANT_STR));
        assertTrue(status.removeSkill(BattleSpellCatalog.PHYSICAL_ENCHANT_DEX));
        assertTrue(status.removeSkill(BattleSpellCatalog.BERSERKERS));
        assertEquals(0, status.berserk(now).remainingSeconds());
    }
}
