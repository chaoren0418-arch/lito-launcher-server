package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleSpellCatalogTest {
    @Test void shapeChangePinsNativeObjectTargetResourcesAndCircleMpRules() {
        BattleSpellCatalog.PolymorphSpell spell =
                BattleSpellCatalog.polymorphSpell(BattleSpellCatalog.SHAPE_CHANGE);
        assertEquals(67, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(230, spell.effectGfx());
        assertEquals(80, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(7_200, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(280, spell.failureMessage());
        assertEquals("shape-change", spell.key());
        assertEquals(80, spell.effectiveMpCost(12, 0));
        assertEquals(73, spell.effectiveMpCost(19, 0));
        assertEquals(71, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.polymorphSpell(68));
    }

    @Test void immuneToHarmPinsNativeProtectionResourcesMessagesAndMpRules() {
        BattleSpellCatalog.DamageProtectionSpell spell =
                BattleSpellCatalog.damageProtectionSpell(
                        BattleSpellCatalog.IMMUNE_TO_HARM);
        assertEquals(68, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(228, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(1_500, spell.reuseDelayMillis());
        assertEquals(32, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(4, spell.targetTo());
        assertEquals(2, spell.damageDivisor());
        assertEquals(314, spell.happenMessage());
        assertEquals(315, spell.stopMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals("immune-to-harm", spell.key());
        assertEquals(30, spell.effectiveMpCost(12, 0));
        assertEquals(23, spell.effectiveMpCost(19, 0));
        assertEquals(21, spell.effectiveMpCost(19, 2));
        assertEquals(2, spell.damageAfterProtection(5));
        assertEquals(0, spell.damageAfterProtection(1));
        assertNull(BattleSpellCatalog.damageProtectionSpell(67));
    }

    @Test void massTeleportPinsNativeResourcesInviteGeometryAndMpRules() {
        BattleSpellCatalog.MassTeleportSpell spell =
                BattleSpellCatalog.massTeleportSpell(BattleSpellCatalog.MASS_TELEPORT);
        assertEquals(69, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2236, spell.effectGfx());
        assertEquals(60, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(4, spell.targetTo());
        assertEquals(200, spell.randomRange());
        assertEquals(3, spell.inviteRange());
        assertEquals(748, spell.questionId());
        assertEquals(276, spell.failureMessage());
        assertEquals("mass-teleport", spell.key());
        assertEquals(60, spell.effectiveMpCost(12, 0));
        assertEquals(53, spell.effectiveMpCost(19, 0));
        assertEquals(51, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.massTeleportSpell(68));
    }

    @Test void callClanPinsNameRequestQuestionAndRoyalMpRules() {
        BattleSpellCatalog.CallClanSpell spell =
                BattleSpellCatalog.callClanSpell(BattleSpellCatalog.CALL_CLAN);
        assertEquals(116, spell.skillId());
        assertEquals(14, spell.row());
        assertEquals(3, spell.column());
        assertEquals(15, spell.skillLevel());
        assertEquals(19, spell.actionId());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.targetTo());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(8, spell.nativeId());
        assertEquals("", spell.nameId());
        assertEquals(0, spell.effectGfx());
        assertEquals(729, spell.questionId());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals("call-clan", spell.key());
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(29, spell.effectiveMpCost(1));
        assertEquals(28, spell.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.callClanSpell(115));
    }

    @Test void runClanPinsIntendedNameRequestAndRoyalMpRules() {
        BattleSpellCatalog.RunClanSpell spell =
                BattleSpellCatalog.runClanSpell(BattleSpellCatalog.RUN_CLAN);
        assertEquals(118, spell.skillId());
        assertEquals(14, spell.row());
        assertEquals(5, spell.column());
        assertEquals(15, spell.skillLevel());
        assertEquals(19, spell.actionId());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.targetTo());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(32, spell.nativeId());
        assertEquals("", spell.nameId());
        assertEquals(0, spell.effectGfx());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals("run-clan", spell.key());
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(29, spell.effectiveMpCost(1));
        assertEquals(28, spell.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.runClanSpell(116));
    }

    @Test void tornadoPinsSelectorCenteredAreaResourcesAndCircleMpRules() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.TORNADO);
        assertEquals(53, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(4, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(758, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(1_500, spell.reuseDelayMillis());
        assertEquals(0, spell.maximumRange());
        assertEquals(4, spell.areaRadius());
        assertEquals(16, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals("tornado", spell.key());
        assertEquals(40, spell.effectiveMpCost(12, 0));
        assertEquals(34, spell.effectiveMpCost(18, 0));
        assertEquals(32, spell.effectiveMpCost(18, 2));
    }

    @Test void blizzardPinsSelectorCenteredAreaResourcesCostsAndCircleMpRules() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.BLIZZARD);
        assertEquals(59, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(2, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(757, spell.effectGfx());
        assertEquals(60, spell.mpCost());
        assertEquals(10, spell.hpCost());
        assertEquals(1_500, spell.reuseDelayMillis());
        assertEquals(0, spell.maximumRange());
        assertEquals(10, spell.areaRadius());
        assertEquals(2, spell.baseDamage());
        assertEquals(3, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(-10, spell.lawfulDelta());
        assertEquals("blizzard", spell.key());
        assertEquals(60, spell.effectiveMpCost(12, 0));
        assertEquals(53, spell.effectiveMpCost(19, 0));
        assertEquals(51, spell.effectiveMpCost(19, 2));
    }

    @Test void earthquakePinsSelectorCenteredAreaResourcesAndCircleMpRules() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.EARTHQUAKE);
        assertEquals(62, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(1812, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(1_500, spell.reuseDelayMillis());
        assertEquals(0, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(32, spell.baseDamage());
        assertEquals(6, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_EARTH, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals("earthquake", spell.key());
        assertEquals(40, spell.effectiveMpCost(12, 0));
        assertEquals(33, spell.effectiveMpCost(19, 0));
        assertEquals(31, spell.effectiveMpCost(19, 2));
    }

    @Test void fireStormPinsSelectorCenteredAreaResourcesAndCircleMpRules() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.FIRE_STORM);
        assertEquals(70, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(1819, spell.effectGfx());
        assertEquals(48, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(0, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(24, spell.baseDamage());
        assertEquals(8, spell.diceCount());
        assertEquals(15, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals("fire-storm", spell.key());
        assertEquals(48, spell.effectiveMpCost(12, 0));
        assertEquals(41, spell.effectiveMpCost(19, 0));
        assertEquals(39, spell.effectiveMpCost(19, 2));
    }

    @Test void lightningStormPinsTargetCenteredAreaResourcesAndCircleMpRules() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.LIGHTNING_STORM);
        assertEquals(65, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(0, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(3924, spell.effectGfx());
        assertEquals(48, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(20, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals("lightning-storm", spell.key());
        assertEquals(48, spell.effectiveMpCost(12, 0));
        assertEquals(41, spell.effectiveMpCost(19, 0));
        assertEquals(39, spell.effectiveMpCost(19, 2));
    }

    @Test void meteorStrikePinsTargetCenteredAreaResourcesAndCircleMpRules() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.METEOR_STRIKE);
        assertEquals(74, spell.skillId());
        assertEquals(9, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(762, spell.effectGfx());
        assertEquals(60, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(110, spell.baseDamage());
        assertEquals(8, spell.diceCount());
        assertEquals(15, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals("meteor-strike", spell.key());
        assertEquals(60, spell.effectiveMpCost(12, 0));
        assertEquals(53, spell.effectiveMpCost(19, 0));
        assertEquals(51, spell.effectiveMpCost(19, 2));
    }

    @Test void holyWalkPinsSelectorResourcesModeAndMovementOnlyCadence() {
        BattleSpellCatalog.MovementBuffSpell spell =
                BattleSpellCatalog.movementBuffSpell(BattleSpellCatalog.HOLY_WALK);
        assertEquals(52, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3936, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(32, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, spell.braveMode());
        assertEquals(0.665D, spell.movementMultiplier());
        assertEquals(665, spell.movementIntervalMillis(1_000));
        assertEquals(15, spell.effectiveMpCost(12, 0));
        assertEquals(9, spell.effectiveMpCost(18, 0));
        assertEquals(7, spell.effectiveMpCost(18, 2));
        assertNull(BattleSpellCatalog.movementBuffSpell(51));
    }

    @Test void windWalkPinsExactSelectorModeAndMovementOnlyCadence() {
        BattleSpellCatalog.MovementBuffSpell spell =
                BattleSpellCatalog.movementBuffSpell(
                        BattleSpellCatalog.WIND_WALK);
        assertEquals(150, spell.skillId());
        assertEquals(18, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2247, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, spell.braveMode());
        assertEquals(0.665D, spell.movementMultiplier());
        assertEquals(665, spell.movementIntervalMillis(1_000));
        assertEquals(15, spell.effectiveMpCost(18, 0));
        assertEquals(13, spell.effectiveMpCost(18, 2));
        assertEquals("wind-walk", spell.key());
    }

    @Test void greaterHastePinsSelectorResourcesMessagesAndWindHelmMpRules() {
        BattleSpellCatalog.SelfHasteSpell spell =
                BattleSpellCatalog.selfHasteSpell(BattleSpellCatalog.GREATER_HASTE);
        assertEquals(54, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3104, spell.effectGfx());
        assertEquals(60, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(2_400, spell.durationSeconds());
        assertEquals(184, spell.happenMessage());
        assertEquals(185, spell.stopMessage());
        assertEquals("greater-haste", spell.key());
        assertEquals(60, spell.effectiveMpCost(12, 0, false));
        assertEquals(54, spell.effectiveMpCost(18, 0, false));
        assertEquals(27, spell.effectiveMpCost(18, 0, true));
        assertEquals(25, spell.effectiveMpCost(18, 2, true));
        assertNull(BattleSpellCatalog.selfHasteSpell(53));
    }

    @Test void berserkersPinsObjectTargetResourcesModifiersAndCircleMpRules() {
        BattleSpellCatalog.CombatBuffSpell spell =
                BattleSpellCatalog.combatBuffSpell(BattleSpellCatalog.BERSERKERS);
        assertEquals(55, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3943, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(320, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(12, spell.targetTo());
        assertEquals(10, spell.armorClass());
        assertEquals(2, spell.meleeHit());
        assertEquals(5, spell.meleeDamage());
        assertEquals(280, spell.failureMessage());
        assertEquals("berserkers", spell.key());
        assertEquals(40, spell.effectiveMpCost(12, 0));
        assertEquals(34, spell.effectiveMpCost(18, 0));
        assertEquals(32, spell.effectiveMpCost(18, 2));
        assertNull(BattleSpellCatalog.combatBuffSpell(54));
    }

    @Test void summonMonsterPinsNativeSelectorResourcesAndAllControlChoices() {
        BattleSpellCatalog.SummonSpell spell =
                BattleSpellCatalog.summonSpell(BattleSpellCatalog.SUMMON_MONSTER);
        assertEquals(51, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2510, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(30, spell.hpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(3, spell.itemConsumeCount());
        assertEquals(-10, spell.lawfulDelta());
        assertEquals(353, spell.mapFailureMessage());
        assertEquals(319, spell.capacityFailureMessage());
        assertEquals(743, spell.levelFailureMessage());
        assertEquals(755, spell.spawnEffectGfx());
        assertEquals(1_800, spell.lifetimeSeconds());
        assertEquals(7, spell.directGrade(1));
        assertEquals(11, spell.directGrade(47));
        assertEquals(13, spell.directGrade(99));
        assertTrue(spell.permitsControlSelection(47, 523));
        assertFalse(spell.permitsControlSelection(47, 12));
        assertTrue(spell.permitsControlSelection(52, 525));
        assertTrue(spell.permitsControlSelection(99, 274));
        assertFalse(spell.permitsControlSelection(47, 0xFFFF_FFFFL));
        assertEquals(30, spell.effectiveMpCost(12, 0));
        assertEquals(25, spell.effectiveMpCost(17, 0));
        assertEquals(23, spell.effectiveMpCost(17, 2));

        assertEquals(31, BattleSpellCatalog.summonChoices().size());
        BattleSpellCatalog.SummonChoice direct =
                BattleSpellCatalog.summonChoice(11);
        assertEquals(81222, direct.npcId());
        assertEquals(44, direct.requiredLevel());
        assertEquals(8, direct.petCost());
        assertTrue(direct.directChoice());
        assertFalse(direct.exclusive());
        BattleSpellCatalog.SummonChoice alternate =
                BattleSpellCatalog.summonChoice(523);
        assertEquals(81224, alternate.npcId());
        assertEquals(11, alternate.selection() & 0xFF);
        assertFalse(alternate.directChoice());
        BattleSpellCatalog.SummonChoice blackPanther =
                BattleSpellCatalog.summonChoice(274);
        assertEquals(81240, blackPanther.npcId());
        assertEquals(72, blackPanther.requiredLevel());
        assertEquals(44, blackPanther.petCost());
        assertEquals(51, blackPanther.effectivePetCost());
        assertTrue(blackPanther.exclusive());
        assertNull(BattleSpellCatalog.summonChoice(0));
        assertNull(BattleSpellCatalog.summonSpell(50));
    }

    @Test void counterMagicPinsExactSelectorResourceAndOneShotWardMetadata() {
        BattleSpellCatalog.CounterMagicSpell spell =
                BattleSpellCatalog.counterMagicSpell(BattleSpellCatalog.COUNTER_MAGIC);
        assertEquals(31, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(10701, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(16, spell.durationSeconds());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(10701, spell.consumeEffectGfx());
        assertEquals(20, spell.effectiveMpCost(0));
        assertEquals(19, spell.effectiveMpCost(1));
        assertEquals(18, spell.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.counterMagicSpell(30));
    }

    @Test void absoluteBarrierPinsNativeCase77AndRetainedResources() {
        BattleSpellCatalog.AbsoluteBarrierSpell spell =
                BattleSpellCatalog.absoluteBarrierSpell(
                        BattleSpellCatalog.ABSOLUTE_BARRIER);
        assertEquals(78, spell.skillId());
        assertEquals(9, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2234, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(12_000, spell.reuseDelayMillis());
        assertEquals(12, spell.durationSeconds());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals("absolute-barrier", spell.key());
        assertEquals(30, spell.effectiveMpCost(8, 0));
        assertEquals(23, spell.effectiveMpCost(19, 0));
        assertEquals(21, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.absoluteBarrierSpell(77));
    }

    @Test void advanceSpiritPinsNativeCase78AndRetainedMaximumSemantics() {
        BattleSpellCatalog.AdvanceSpiritSpell spell =
                BattleSpellCatalog.advanceSpiritSpell(
                        BattleSpellCatalog.ADVANCE_SPIRIT);
        assertEquals(79, spell.skillId());
        assertEquals(9, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3935, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(5, spell.baseMaximumDivisor());
        assertEquals(280, spell.failureMessage());
        assertEquals("advance-spirit", spell.key());
        assertEquals(20, spell.maximumBonus(100));
        assertEquals(1, spell.maximumBonus(9));
        assertEquals(130, spell.maximumWithBonus(110, 100));
        assertEquals(120, spell.maximumWithBonus(100, 100));
        assertEquals(0x7FFF, spell.maximumWithBonus(0x7FFF, 100));
        assertEquals(20, spell.effectiveMpCost(8, 0));
        assertEquals(13, spell.effectiveMpCost(19, 0));
        assertEquals(11, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.advanceSpiritSpell(78));
    }

    @Test void advanceSpiritUsesNegotiatedWideVitalLimit() {
        String previous = System.getProperty("lito.numeric.breakthrough");
        try {
            System.setProperty("lito.numeric.breakthrough", "1");
            BattleSpellCatalog.AdvanceSpiritSpell spell =
                    BattleSpellCatalog.advanceSpiritSpell(
                            BattleSpellCatalog.ADVANCE_SPIRIT);
            assertEquals(180_000, spell.maximumBonus(900_000));
            assertEquals(999_999, spell.maximumWithBonus(900_000, 900_000));
        } finally {
            if (previous == null) {
                System.clearProperty("lito.numeric.breakthrough");
            } else {
                System.setProperty("lito.numeric.breakthrough", previous);
            }
        }
    }

    @Test void lightPinsNativeSelectorAndSameFamilyResources() {
        BattleSpellCatalog.LightSpell light =
                BattleSpellCatalog.lightSpell(BattleSpellCatalog.LIGHT);
        assertEquals(2, light.skillId());
        assertEquals(0, light.row());
        assertEquals(1, light.column());
        assertEquals(19, light.actionId());
        assertEquals(2510, light.effectGfx());
        assertEquals(4, light.mpCost());
        assertEquals(10, light.reuseDelayMillis());
        assertEquals(3_600, light.durationSeconds());
        assertEquals(0x0E, light.lightRange());
        assertEquals(145, light.soundId());
        assertNull(BattleSpellCatalog.lightSpell(3));
    }

    @Test void iceArrowAndWindCutterShareTheNativeObjectCoordinateProducer() {
        BattleSpellCatalog.AttackSpell ice =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.ICE_ARROW);
        assertEquals(6, ice.skillId());
        assertEquals(0, ice.row());
        assertEquals(5, ice.column());
        assertEquals(18, ice.actionId());
        assertEquals(1797, ice.effectGfx());
        assertEquals(4, ice.mpCost());
        assertEquals(10, ice.maximumRange());
        assertEquals(1, ice.diceCount());
        assertEquals(10, ice.dieSides());
        assertEquals(8, ice.baseDamage());
        assertEquals(4, ice.attribute());

        BattleSpellCatalog.AttackSpell wind =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.WIND_CUTTER);
        assertEquals(7, wind.skillId());
        assertEquals(0, wind.row());
        assertEquals(6, wind.column());
        assertEquals(18, wind.actionId());
        assertEquals(1799, wind.effectGfx());
        assertEquals(4, wind.mpCost());
        assertEquals(10, wind.maximumRange());
        assertEquals(1, wind.diceCount());
        assertEquals(10, wind.dieSides());
        assertEquals(8, wind.baseDamage());
        assertEquals(8, wind.attribute());
    }

    @Test void chillTouchPinsSelectedResourcesAndRetainedLifeDrainMeaning() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.CHILL_TOUCH);
        assertEquals(10, spell.skillId());
        assertEquals(1, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(252, spell.effectGfx());
        assertEquals(8, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(8, spell.baseDamage());
        assertEquals(2, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertTrue(spell.drainsLife());
        assertFalse(BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.LIGHT_ARROW).drainsLife());
    }

    @Test void vampiricTouchPinsSelectedResourcesAndRetainedLifeDrainMeaning() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.VAMPIRIC_TOUCH);
        assertEquals(28, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(3, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(236, spell.effectGfx());
        assertEquals(12, spell.mpCost());
        assertEquals(50, spell.reuseDelayMillis());
        assertEquals(4, spell.maximumRange());
        assertEquals(12, spell.baseDamage());
        assertEquals(3, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertTrue(spell.drainsLife());
        assertEquals(9, spell.effectiveMpCost(15, 0));
    }

    @Test void secondCircleArrowsPinExactSelectedDatabaseRows() {
        BattleSpellCatalog.AttackSpell fire =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.FIRE_ARROW);
        assertEquals(15, fire.skillId());
        assertEquals(1, fire.row());
        assertEquals(6, fire.column());
        assertEquals(18, fire.actionId());
        assertEquals(1583, fire.effectGfx());
        assertEquals(6, fire.mpCost());
        assertEquals(10, fire.maximumRange());
        assertEquals(8, fire.baseDamage());
        assertEquals(2, fire.diceCount());
        assertEquals(10, fire.dieSides());
        assertEquals(1, fire.attribute());

        BattleSpellCatalog.AttackSpell stalac =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.STALAC);
        assertEquals(16, stalac.skillId());
        assertEquals(1, stalac.row());
        assertEquals(7, stalac.column());
        assertEquals(18, stalac.actionId());
        assertEquals(1801, stalac.effectGfx());
        assertEquals(6, stalac.mpCost());
        assertEquals(10, stalac.maximumRange());
        assertEquals(8, stalac.baseDamage());
        assertEquals(2, stalac.diceCount());
        assertEquals(10, stalac.dieSides());
        assertEquals(2, stalac.attribute());
    }

    @Test void lightningPinsExactLineAttackSourceRow() {
        BattleSpellCatalog.LineAttackSpell lightning =
                BattleSpellCatalog.lineAttackSpell(BattleSpellCatalog.LIGHTNING);
        assertEquals(17, lightning.skillId());
        assertEquals(2, lightning.row());
        assertEquals(0, lightning.column());
        assertEquals(18, lightning.actionId());
        assertEquals(170, lightning.effectGfx());
        assertEquals(20, lightning.mpCost());
        assertEquals(500, lightning.reuseDelayMillis());
        assertEquals(6, lightning.maximumRange());
        assertEquals(3, lightning.sourceAreaValue());
        assertEquals(0, lightning.baseDamage());
        assertEquals(4, lightning.diceCount());
        assertEquals(1, lightning.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, lightning.attribute());
        assertNull(BattleSpellCatalog.lineAttackSpell(18));
    }

    @Test void turnUndeadPinsExactProbabilitySourceRow() {
        BattleSpellCatalog.TurnUndeadSpell spell =
                BattleSpellCatalog.turnUndeadSpell(BattleSpellCatalog.TURN_UNDEAD);
        assertEquals(18, spell.skillId());
        assertEquals(2, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(754, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(7, spell.probabilityDice());
        assertEquals(1, spell.probabilityValue());
        assertEquals(280, spell.failureMessage());
        assertNull(BattleSpellCatalog.turnUndeadSpell(19));
    }

    @Test void curseBlindPinsSelectorSourceProbabilityAndMpRules() {
        BattleSpellCatalog.BlindSpell spell =
                BattleSpellCatalog.blindSpell(BattleSpellCatalog.CURSE_BLIND);
        assertEquals(20, spell.skillId());
        assertEquals(2, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(746, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(1, spell.maximumRange());
        assertEquals(16, spell.durationSeconds());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.35D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertEquals(false, spell.includeMagicLevel());
        assertEquals(true, spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());

        assertEquals(59, spell.probability(50, 40, 13, 10, 4, 20, 3));
        assertEquals(0, spell.probabilityForTarget(
                50, 40, 13, 10, 4, 20, 3, true));
        assertEquals(59, spell.probabilityForTarget(
                50, 40, 13, 10, 4, 20, 3, false));
        assertEquals(1, spell.probability(1, 99, 1, 0, -100, 999, 127));
        assertEquals(90, spell.probability(99, 1, 254, 10, 100, 0, -128));
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(19, spell.effectiveMpCost(13, 0));
        assertEquals(18, spell.effectiveMpCost(14, 0));
        assertEquals(16, spell.effectiveMpCost(14, 2));
        assertNull(BattleSpellCatalog.blindSpell(19));
    }

    @Test void darknessPinsSelectorReagentDurationBossGateAndCircleFiveMpRules() {
        BattleSpellCatalog.BlindSpell spell =
                BattleSpellCatalog.blindSpell(BattleSpellCatalog.DARKNESS);
        assertEquals(40, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2175, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(32, spell.durationSeconds());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.35D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertEquals(false, spell.includeMagicLevel());
        assertEquals(true, spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("darkness", spell.key());

        assertEquals(59, spell.probability(50, 40, 13, 10, 4, 20, 3));
        assertEquals(0, spell.probabilityForTarget(
                50, 40, 13, 10, 4, 20, 3, true));
        assertEquals(15, spell.effectiveMpCost(12, 0));
        assertEquals(14, spell.effectiveMpCost(13, 0));
        assertEquals(13, spell.effectiveMpCost(14, 0));
        assertEquals(12, spell.effectiveMpCost(15, 0));
        assertEquals(11, spell.effectiveMpCost(16, 0));
        assertEquals(9, spell.effectiveMpCost(16, 2));
    }

    @Test void darkBlindPinsNativeCase102MaterialAndNegativeControlRows() {
        BattleSpellCatalog.BlindSpell spell =
                BattleSpellCatalog.blindSpell(BattleSpellCatalog.DARK_BLIND);
        assertEquals(103, spell.skillId());
        assertEquals(12, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2947, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(990, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(64, spell.durationSeconds());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.35D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertEquals(false, spell.includeMagicLevel());
        assertEquals(true, spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertEquals(InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("dark-blind", spell.key());

        assertEquals(46, spell.probability(26, 26, 8, 0, 0, 27, 0));
        assertEquals(56, spell.probability(52, 26, 8, 0, 0, 27, 0));
        assertEquals(0, spell.probabilityForTarget(
                52, 26, 8, 0, 0, 27, 0, true));
        assertEquals(20, spell.effectiveMpCost(8, 0));
        assertEquals(18, spell.effectiveMpCost(16, 2));
    }

    @Test void createZombiePinsCorpseEnvelopeCostsAndNativeLevelTemplates() {
        BattleSpellCatalog.CreateZombieSpell spell =
                BattleSpellCatalog.createZombieSpell(BattleSpellCatalog.CREATE_ZOMBIE);
        assertEquals(41, spell.skillId());
        assertEquals(5, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(226, spell.effectGfx());
        assertEquals(5, spell.mpCost());
        assertEquals(80, spell.hpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(6, spell.petCost());
        assertEquals(5, spell.effectiveMpCost(12, 0));
        assertEquals(4, spell.effectiveMpCost(13, 0));
        assertEquals(1, spell.effectiveMpCost(17, 0));

        assertEquals(45065, BattleSpellCatalog.zombieTemplate(0, 99).npcId());
        assertEquals(45065, BattleSpellCatalog.zombieTemplate(1, 47).npcId());
        assertEquals(81183, BattleSpellCatalog.zombieTemplate(3, 24).npcId());
        assertEquals(81184, BattleSpellCatalog.zombieTemplate(3, 32).npcId());
        assertEquals(81185, BattleSpellCatalog.zombieTemplate(3, 40).npcId());
        BattleSpellCatalog.ZombieTemplate mage47 =
                BattleSpellCatalog.zombieTemplate(3, 47);
        assertEquals(81186, mage47.npcId());
        assertEquals(52, mage47.gfx());
        assertEquals("$57", mage47.name());
        assertEquals(16, mage47.level());
        assertEquals(500, mage47.maxHp());
        assertEquals(81187, BattleSpellCatalog.zombieTemplate(3, 48).npcId());
        assertEquals(81188, BattleSpellCatalog.zombieTemplate(3, 52).npcId());
        assertEquals(81183, BattleSpellCatalog.zombieTemplate(2, 48).npcId());
        assertNull(BattleSpellCatalog.createZombieSpell(42));
    }

    @Test void resurrectionPinsNativeSelectorCostsAndCantResurrectRows() {
        BattleSpellCatalog.ResurrectionSpell spell =
                BattleSpellCatalog.resurrectionSpell(BattleSpellCatalog.RESURRECTION);
        assertEquals(61, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3944, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(280, spell.failureMessage());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());
        assertEquals(321, spell.questionId());
        assertEquals(2, spell.playerHpDivisor());
        assertEquals(4, spell.npcHpDivisor());
        assertFalse(spell.restoresExperience());
        assertEquals(0, spell.requiredElfAttribute());
        assertEquals(25, spell.restoredPlayerHp(50));
        assertEquals(12, spell.restoredNpcHp(50));
        assertEquals(50, spell.effectiveMpCost(12, 0));
        assertEquals(45, spell.effectiveMpCost(17, 0));
        assertEquals(43, spell.effectiveMpCost(17, 2));

        BattleSpellCatalog.ResurrectionSpell greater =
                BattleSpellCatalog.resurrectionSpell(
                        BattleSpellCatalog.GREATER_RESURRECTION);
        assertEquals(75, greater.skillId());
        assertEquals(9, greater.row());
        assertEquals(2, greater.column());
        assertEquals(19, greater.actionId());
        assertEquals(3944, greater.effectGfx());
        assertEquals(100, greater.mpCost());
        assertEquals(10, greater.reuseDelayMillis());
        assertEquals(-1, greater.maximumRange());
        assertEquals(280, greater.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, greater.itemConsumeId());
        assertEquals(1, greater.itemConsumeCount());
        assertEquals(322, greater.questionId());
        assertEquals(1, greater.playerHpDivisor());
        assertEquals(4, greater.npcHpDivisor());
        assertTrue(greater.restoresExperience());
        assertEquals(0, greater.requiredElfAttribute());
        assertEquals(50, greater.restoredPlayerHp(50));
        assertEquals(12, greater.restoredNpcHp(50));
        assertEquals(100, greater.effectiveMpCost(12, 0));
        assertEquals(95, greater.effectiveMpCost(17, 0));
        assertEquals(93, greater.effectiveMpCost(17, 2));

        BattleSpellCatalog.ResurrectionSpell nature =
                BattleSpellCatalog.resurrectionSpell(
                        BattleSpellCatalog.CALL_OF_NATURE);
        assertEquals(165, nature.skillId());
        assertEquals(20, nature.row());
        assertEquals(4, nature.column());
        assertEquals(19, nature.actionId());
        assertEquals(2245, nature.effectGfx());
        assertEquals(50, nature.mpCost());
        assertEquals(0, nature.reuseDelayMillis());
        assertEquals(10, nature.maximumRange());
        assertEquals(280, nature.failureMessage());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, nature.itemConsumeId());
        assertEquals(1, nature.itemConsumeCount());
        assertEquals(322, nature.questionId());
        assertEquals(1, nature.playerHpDivisor());
        assertEquals(1, nature.npcHpDivisor());
        assertFalse(nature.restoresExperience());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER,
                nature.requiredElfAttribute());
        assertEquals(50, nature.restoredPlayerHp(50));
        assertEquals(50, nature.restoredNpcHp(50));
        assertEquals(50, nature.effectiveMpCost(18, 0));
        assertEquals(48, nature.effectiveMpCost(18, 2));
        assertTrue(BattleSpellCatalog.npcAllowsResurrection(45262));
        assertTrue(BattleSpellCatalog.npcAllowsResurrection(45182));
        assertFalse(BattleSpellCatalog.npcAllowsResurrection(45601));
        assertFalse(BattleSpellCatalog.npcAllowsResurrection(45584));
        assertFalse(BattleSpellCatalog.npcAllowsResurrection(45610));
        assertFalse(BattleSpellCatalog.npcAllowsResurrection(99999));
        assertNull(BattleSpellCatalog.resurrectionSpell(76));
    }

    @Test void fogOfSleepingPinsAreaProbabilityReagentAndFullCircleMpRules() {
        BattleSpellCatalog.SleepSpell spell = BattleSpellCatalog.sleepSpell(
                BattleSpellCatalog.FOG_OF_SLEEPING);
        assertEquals(66, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(760, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(1_500, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(32, spell.durationSeconds());
        assertEquals(297, spell.targetFailureMessage());
        assertEquals(280, spell.casterFailureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(54, spell.probability(50, 40, 13, 0, 4, 20, 3));
        assertEquals(1, spell.probability(1, 99, 1, 0, -100, 999, 127));
        assertEquals(90, spell.probability(99, 1, 254, 0, 100, 0, -128));
        assertEquals(50, spell.effectiveMpCost(12, 0));
        assertEquals(43, spell.effectiveMpCost(19, 0));
        assertEquals(41, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.sleepSpell(65));
    }

    @Test void earthBindPinsNativeProducerResourcesAndSustainFormula() {
        BattleSpellCatalog.BindSpell spell =
                BattleSpellCatalog.bindSpell(BattleSpellCatalog.EARTH_BIND);
        assertEquals(157, spell.skillId());
        assertEquals(19, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2251, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(600, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(16, spell.durationSeconds());
        assertEquals(280, spell.failureMessage());
        assertEquals(50, spell.probabilityHigh());
        assertEquals(42, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.4D, spell.intelligenceMultiplier());
        assertEquals(false, spell.includeMagicLevel());
        assertEquals(true, spell.includeMagicHit());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals(52, spell.probability(50, 40, 13, 99, 4, 7));
        assertEquals(1, spell.probability(1, 99, 1, 0, -100, 127));
        assertEquals(90, spell.probability(99, 1, 254, 0, 100, -128));
        assertEquals(50, spell.effectiveMpCost(0));
        assertEquals(48, spell.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.bindSpell(156));
    }

    @Test void iceLancePinsNativeAttackFreezeProbabilityAndSixCircleMpRules() {
        BattleSpellCatalog.FreezeAttackSpell spell =
                BattleSpellCatalog.freezeAttackSpell(BattleSpellCatalog.ICE_LANCE);
        assertEquals(50, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(756, spell.effectGfx());
        assertEquals(100, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(1_000, spell.clientCastClockMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(0, spell.areaRadius());
        assertFalse(spell.lineTargeted());
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                spell.requestShape());
        assertEquals(16, spell.durationSeconds());
        assertEquals(40, spell.baseDamage());
        assertEquals(6, spell.diceCount());
        assertEquals(5, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.4D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertEquals(false, spell.includeMagicLevel());
        assertEquals(true, spell.includeMagicHit());
        assertEquals(60, spell.probability(50, 40, 13, 0, 4, 20, 3));
        assertEquals(1, spell.probability(1, 99, 1, 0, -100, 999, 127));
        assertEquals(90, spell.probability(99, 1, 254, 0, 100, 0, -128));
        assertEquals(100, spell.effectiveMpCost(12, 0));
        assertEquals(94, spell.effectiveMpCost(18, 0));
        assertEquals(94, spell.effectiveMpCost(19, 0));
        assertEquals(92, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.freezeAttackSpell(49));
    }

    @Test void freezingBlizzardPinsSelectorOnlyCenteredFreezeContract() {
        BattleSpellCatalog.FreezeAttackSpell spell =
                BattleSpellCatalog.freezeAttackSpell(
                        BattleSpellCatalog.FREEZING_BLIZZARD);
        assertEquals(80, spell.skillId());
        assertEquals(9, spell.row());
        assertEquals(7, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(3933, spell.effectGfx());
        assertEquals(60, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(0, spell.maximumRange());
        assertEquals(10, spell.areaRadius());
        assertFalse(spell.lineTargeted());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(32, spell.durationSeconds());
        assertEquals(2, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(-10, spell.lawfulDelta());
        assertEquals(52, spell.probabilityHigh());
        assertEquals(44, spell.probabilityEqual());
        assertEquals(36, spell.probabilityLow());
        assertEquals(0.38D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertEquals(false, spell.includeMagicLevel());
        assertEquals(true, spell.includeMagicHit());
        assertEquals(56, spell.probability(50, 40, 13, 0, 4, 20, 3));
        assertEquals(60, spell.effectiveMpCost(12, 0));
        assertEquals(53, spell.effectiveMpCost(19, 0));
        assertEquals(51, spell.effectiveMpCost(19, 2));
    }

    @Test void frozenCloudPinsNativeTargetedAreaContract() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.FROZEN_CLOUD);
        assertEquals(22, spell.skillId());
        assertEquals(2, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(1_804, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(300, spell.reuseDelayMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(0, spell.baseDamage());
        assertEquals(2, spell.diceCount());
        assertEquals(2, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals("frozen-cloud", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                spell.requestShape());
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(18, spell.effectiveMpCost(14, 0));
        assertEquals(16, spell.effectiveMpCost(14, 2));
    }

    @Test void earthJailPinsNativeTargetedAreaContractAndCircleMpRules() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.EARTH_JAIL);
        assertEquals(30, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(1_805, spell.effectGfx());
        assertEquals(24, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(8, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(0, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(8, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_EARTH, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals("earth-jail", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                spell.requestShape());
        assertEquals(24, spell.effectiveMpCost(12, 0));
        assertEquals(21, spell.effectiveMpCost(15, 0));
        assertEquals(19, spell.effectiveMpCost(15, 2));
        assertNull(BattleSpellCatalog.areaAttackSpell(29));
    }

    @Test void revealWeaknessPinsNativeTargetAndWeakAttributeEffects() {
        BattleSpellCatalog.RevealWeaknessSpell spell =
                BattleSpellCatalog.revealWeaknessSpell(
                        BattleSpellCatalog.REVEAL_WEAKNESS);
        assertEquals(23, spell.skillId());
        assertEquals(2, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2_510, spell.effectGfx());
        assertEquals(8, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(2, spell.targetTo());
        assertEquals(0, spell.areaRadius());
        assertEquals(128, spell.sourceType());
        assertEquals(64, spell.nativeId());
        assertEquals(280, spell.failureMessage());
        assertEquals("reveal-weakness", spell.key());
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                spell.requestShape());
        assertArrayEquals(new int[0], spell.weaknessEffects(0));
        assertArrayEquals(new int[] {2_169}, spell.weaknessEffects(1));
        assertArrayEquals(new int[] {2_167}, spell.weaknessEffects(2));
        assertArrayEquals(new int[] {2_166}, spell.weaknessEffects(4));
        assertArrayEquals(new int[] {2_168}, spell.weaknessEffects(8));
        assertArrayEquals(new int[] {2_169, 2_167, 2_166, 2_168},
                spell.weaknessEffects(-1));
        assertEquals(8, spell.effectiveMpCost(12, 0));
        assertEquals(7, spell.effectiveMpCost(13, 0));
        assertEquals(6, spell.effectiveMpCost(14, 0));
        assertEquals(4, spell.effectiveMpCost(15, 2));
        assertNull(BattleSpellCatalog.revealWeaknessSpell(24));
    }

    @Test void freezingBreathPinsSelectedSelectorLineHpDamageAndClock() {
        BattleSpellCatalog.FreezeAttackSpell spell =
                BattleSpellCatalog.freezeAttackSpell(
                        BattleSpellCatalog.FREEZING_BREATH);
        assertEquals(194, spell.skillId());
        assertEquals(24, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(6_988, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(50, spell.hpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(1_800, spell.clientCastClockMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(0, spell.areaRadius());
        assertTrue(spell.lineTargeted());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(3, spell.durationSeconds());
        assertEquals(45, spell.baseDamage());
        assertEquals(0, spell.diceCount());
        assertEquals(0, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals(52, spell.probabilityHigh());
        assertEquals(44, spell.probabilityEqual());
        assertEquals(36, spell.probabilityLow());
        assertEquals(0.38D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(56, spell.probability(50, 40, 13, 0, 4, 20, 3));
        assertEquals(0, spell.effectiveMpCost(12, 0));
        assertEquals(0, spell.effectiveMpCost(19, 2));
    }

    @Test void tamingPinsNativeEnvelopeCostsProbabilityAndMpRules() {
        BattleSpellCatalog.TamingSpell spell =
                BattleSpellCatalog.tamingSpell(BattleSpellCatalog.TAMING_MONSTER);
        assertEquals(36, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(228, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(20, spell.hpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(3_600, spell.durationSeconds());
        assertEquals(280, spell.failureMessage());
        assertEquals(40, spell.probabilityHigh());
        assertEquals(35, spell.probabilityEqual());
        assertEquals(30, spell.probabilityLow());
        assertEquals(0.5D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertEquals(true, spell.includeMagicLevel());
        assertEquals(true, spell.includeMagicHit());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(54, spell.probability(47, 26, 8, 10, 1, 15));
        assertEquals(1, spell.probability(1, 99, 1, 0, -100, 999));
        assertEquals(90, spell.probability(99, 1, 254, 10, 100, 0));
        assertEquals(30, spell.effectiveMpCost(12, 0));
        assertEquals(26, spell.effectiveMpCost(16, 0));
        assertEquals(24, spell.effectiveMpCost(16, 2));
        assertNull(BattleSpellCatalog.tamingSpell(37));
    }

    @Test void curePoisonPinsNativeCase8TargetOnlyEnvelopeAndMpRules() {
        BattleSpellCatalog.CurseRemovalSpell spell =
                BattleSpellCatalog.curseRemovalSpell(BattleSpellCatalog.CURE_POISON);
        assertEquals(9, spell.skillId());
        assertEquals(1, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(871, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertFalse(spell.curesCurseParalysis());
        assertEquals("cure-poison", spell.key());
        assertEquals(15, spell.effectiveMpCost(12, 0));
        assertEquals(14, spell.effectiveMpCost(19, 0));
        assertEquals(12, spell.effectiveMpCost(19, 2));
    }

    @Test void removeCursePinsNativeTargetOnlyEnvelopeAndMpRules() {
        BattleSpellCatalog.CurseRemovalSpell spell =
                BattleSpellCatalog.curseRemovalSpell(BattleSpellCatalog.REMOVE_CURSE);
        assertEquals(37, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(227, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertTrue(spell.curesCurseParalysis());
        assertEquals("remove-curse", spell.key());
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(16, spell.effectiveMpCost(16, 0));
        assertEquals(14, spell.effectiveMpCost(16, 2));
        assertNull(BattleSpellCatalog.curseRemovalSpell(38));
    }

    @Test void coneOfColdPinsNativeProducerResourceAndMpRules() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.CONE_OF_COLD);
        assertEquals(38, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(11738, spell.effectGfx());
        assertEquals(18, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(32, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(6, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals("cone-of-cold", spell.key());
        assertEquals(18, spell.effectiveMpCost(12, 0));
        assertEquals(12, spell.effectiveMpCost(16, 2));
    }

    @Test void eruptionPinsNativeProducerResourceAndMpRules() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.ERUPTION);
        assertEquals(45, spell.skillId());
        assertEquals(5, spell.row());
        assertEquals(4, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(129, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(100, spell.reuseDelayMillis());
        assertEquals(8, spell.maximumRange());
        assertEquals(40, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(3, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_EARTH, spell.attribute());
        assertEquals("eruption", spell.key());
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(13, spell.effectiveMpCost(17, 2));
    }

    @Test void sunburstPinsNativeProducerResourceAndMpRules() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.SUNBURST);
        assertEquals(46, spell.skillId());
        assertEquals(5, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(1811, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(32, spell.baseDamage());
        assertEquals(16, spell.diceCount());
        assertEquals(5, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals("sunburst", spell.key());
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(13, spell.effectiveMpCost(17, 2));
    }

    @Test void weaknessPinsNativeSelectorProbabilityResourceAndMeleeModifiers() {
        BattleSpellCatalog.WeaknessSpell spell =
                BattleSpellCatalog.weaknessSpell(BattleSpellCatalog.WEAKNESS);
        assertEquals(47, spell.skillId());
        assertEquals(5, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2228, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(64, spell.durationSeconds());
        assertEquals(12, spell.probabilityDice());
        assertEquals(5, spell.bossModifier());
        assertEquals(-5, spell.meleeDamageModifier());
        assertEquals(-1, spell.meleeHitModifier());
        assertEquals(0, spell.armorClassModifier());
        assertEquals(0, spell.lawfulDelta());
        assertEquals(692, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("weakness", spell.key());

        assertEquals(10, spell.probability(
                3, 40, 8, 0, 0, 0, -1, sides -> 1));
        assertEquals(2, spell.probability(
                3, 40, 8, 0, 0, 0, 5, sides -> 1));
        assertEquals(90, spell.probability(
                3, 40, 8, 0, 0, 0, -1, sides -> sides));
        assertEquals(18, spell.probability(
                3, 40, 8, 0, 0, 0, 5, sides -> sides));
        assertEquals(10, spell.effectiveMpCost(12, 0));
        assertEquals(5, spell.effectiveMpCost(17, 0));
        assertEquals(3, spell.effectiveMpCost(17, 2));
        assertNull(BattleSpellCatalog.weaknessSpell(48));
    }

    @Test void weaponBreakPinsNativeSelectorProbabilityCostsAndBossDivisor() {
        BattleSpellCatalog.WeaponBreakSpell spell =
                BattleSpellCatalog.weaponBreakSpell(BattleSpellCatalog.WEAPON_BREAK);
        assertEquals(27, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(172, spell.effectGfx());
        assertEquals(32, spell.mpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(4, spell.maximumRange());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("weapon-break", spell.key());

        assertEquals(62, spell.probabilityForTarget(
                50, 40, 13, 99, 5, 20, false));
        assertEquals(20, spell.probabilityForTarget(
                50, 40, 13, 99, 5, 20, true));
        assertEquals(32, spell.effectiveMpCost(12, 0));
        assertEquals(29, spell.effectiveMpCost(15, 0));
        assertEquals(27, spell.effectiveMpCost(15, 2));
        assertNull(BattleSpellCatalog.weaponBreakSpell(28));
    }

    @Test void diseasePinsNativeSelectorProbabilityResourcesAbilitiesAndLawfulDelta() {
        BattleSpellCatalog.WeaknessSpell spell =
                BattleSpellCatalog.weaknessSpell(BattleSpellCatalog.DISEASE);
        assertEquals(56, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2230, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(64, spell.durationSeconds());
        assertEquals(10, spell.probabilityDice());
        assertEquals(5, spell.bossModifier());
        assertEquals(-6, spell.meleeDamageModifier());
        assertEquals(0, spell.meleeHitModifier());
        assertEquals(12, spell.armorClassModifier());
        assertEquals(-10, spell.lawfulDelta());
        assertEquals(0, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("disease", spell.key());

        assertEquals(10, spell.probability(
                3, 40, 8, 0, 0, 0, -1, sides -> 1));
        assertEquals(2, spell.probability(
                3, 40, 8, 0, 0, 0, 5, sides -> 1));
        assertEquals(90, spell.probability(
                3, 40, 8, 0, 0, 0, -1, sides -> sides));
        assertEquals(18, spell.probability(
                3, 40, 8, 0, 0, 0, 5, sides -> sides));
        assertEquals(10, spell.effectiveMpCost(12, 0));
        assertEquals(5, spell.effectiveMpCost(17, 0));
        assertEquals(3, spell.effectiveMpCost(17, 2));
    }

    @Test void silencePinsNativeSelectorNegativeControlBossImmunityAndResources() {
        BattleSpellCatalog.SilenceSpell spell =
                BattleSpellCatalog.silenceSpell(BattleSpellCatalog.SILENCE);
        assertEquals(64, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2177, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(16, spell.durationSeconds());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.35D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("silence", spell.key());

        assertEquals(56, spell.probability(47, 15, 8, 0, 0, 36));
        assertEquals(47, spell.probability(20, 20, 8, 0, 0, 0));
        assertEquals(37, spell.probability(15, 20, 8, 0, 0, 0));
        assertEquals(0, spell.probabilityForTarget(
                47, 15, 8, 0, 0, 0, true));
        assertEquals(50, spell.effectiveMpCost(12, 0));
        assertEquals(43, spell.effectiveMpCost(19, 0));
        assertEquals(41, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.silenceSpell(65));
    }

    @Test void decayPotionPinsNativeSelectorProbabilityResourcesAndMpRules() {
        BattleSpellCatalog.DecayPotionSpell spell =
                BattleSpellCatalog.decayPotionSpell(
                        BattleSpellCatalog.DECAY_POTION);
        assertEquals(71, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2232, spell.effectGfx());
        assertEquals(80, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(500, spell.clientCastClockMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(16, spell.durationSeconds());
        assertEquals(8, spell.probabilityDice());
        assertEquals(0, spell.bossModifier());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals("decay-potion", spell.key());

        assertEquals(10, spell.probability(
                3, 52, 8, 0, 0, 0, false, sides -> 1));
        assertEquals(80, spell.probability(
                3, 52, 8, 0, 0, 0, false, sides -> sides));
        assertEquals(67, spell.probability(
                3, 52, 8, 2, 3, 20, false, sides -> sides));
        assertEquals(1, spell.probability(
                3, 52, 8, 0, 0, 100, false, sides -> 1));
        assertEquals(0, spell.probability(
                3, 52, 8, 0, 0, 0, true, sides -> sides));
        assertEquals(80, spell.effectiveMpCost(12, 0));
        assertEquals(73, spell.effectiveMpCost(19, 0));
        assertEquals(71, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.decayPotionSpell(72));
    }

    @Test void polluteWaterPinsNativeSelectorSourceProbabilityAndBypass() {
        BattleSpellCatalog.PolluteWaterSpell spell =
                BattleSpellCatalog.polluteWaterSpell(
                        BattleSpellCatalog.POLLUTE_WATER);
        assertEquals(173, spell.skillId());
        assertEquals(21, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(5830, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(1_000, spell.clientCastClockMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(192, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.30D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertTrue(spell.counterMagicBypass());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("pollute-water", spell.key());

        assertEquals(55, spell.probability(47, 15, 8, 0, 0, 36));
        assertEquals(47, spell.probability(20, 20, 8, 0, 0, 0));
        assertEquals(37, spell.probability(15, 20, 8, 0, 0, 0));
        assertEquals(66, spell.probability(47, 15, 8, 0, 10, 20));
        assertEquals(0, spell.probabilityForTarget(
                47, 15, 8, 0, 0, 0, true));
        assertEquals(50, spell.effectiveMpCost(0));
        assertEquals(48, spell.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.polluteWaterSpell(174));
    }

    @Test void strikerGalePinsNativeSelectorScreenProbabilityAndBypass() {
        BattleSpellCatalog.StrikerGaleSpell spell =
                BattleSpellCatalog.strikerGaleSpell(
                        BattleSpellCatalog.STRIKER_GALE);
        assertEquals(174, spell.skillId());
        assertEquals(21, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(5826, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(3_000, spell.clientCastClockMillis());
        assertEquals(10, spell.clientClickRange());
        assertEquals(-1, spell.serverRange());
        assertEquals(19, spell.maximumRange());
        assertEquals(192, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.30D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertTrue(spell.counterMagicBypass());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(5, spell.itemConsumeCount());
        assertEquals("striker-gale", spell.key());

        assertEquals(55, spell.probability(47, 15, 8, 0, 0, 36));
        assertEquals(47, spell.probability(20, 20, 8, 0, 0, 0));
        assertEquals(37, spell.probability(15, 20, 8, 0, 0, 0));
        assertEquals(66, spell.probability(47, 15, 8, 0, 10, 20));
        assertEquals(0, spell.probabilityForTarget(
                47, 15, 8, 0, 0, 0, true));
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(8, spell.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.strikerGaleSpell(173));
    }

    @Test void detectionPinsNativeSelectorScreenRevealAndMpRules() {
        BattleSpellCatalog.DetectionSpell spell =
                BattleSpellCatalog.detectionSpell(BattleSpellCatalog.DETECTION);
        assertEquals(13, spell.skillId());
        assertEquals(1, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(749, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(-1, spell.nativeArea());
        assertEquals(2, spell.sourceType());
        assertEquals(16, spell.nativeId());
        assertEquals("detection", spell.key());

        assertEquals(15, spell.effectiveMpCost(12, 0, false));
        assertEquals(14, spell.effectiveMpCost(13, 0, false));
        assertEquals(7, spell.effectiveMpCost(13, 0, true));
        assertEquals(5, spell.effectiveMpCost(13, 2, true));
        assertEquals(6, spell.effectiveMpCost(12, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> spell.effectiveMpCost(0, 0, false));
        assertNull(BattleSpellCatalog.detectionSpell(12));
    }

    @Test void counterDetectionPinsNativeSelectorScreenDamageAndMpRules() {
        BattleSpellCatalog.CounterDetectionSpell spell =
                BattleSpellCatalog.counterDetectionSpell(
                        BattleSpellCatalog.COUNTER_DETECTION);
        assertEquals(72, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3934, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(500, spell.clientCastClockMillis());
        assertEquals(-1, spell.nativeArea());
        assertEquals(24, spell.baseDamage());
        assertEquals(4, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(0, spell.attribute());
        assertEquals("counter-detection", spell.key());

        assertEquals(28, spell.rollDamage(sides -> 1));
        assertEquals(64, spell.rollDamage(sides -> sides));
        assertThrows(IllegalArgumentException.class,
                () -> spell.rollDamage(sides -> 0));
        assertEquals(30, spell.effectiveMpCost(12, 0));
        assertEquals(23, spell.effectiveMpCost(19, 0));
        assertEquals(21, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.counterDetectionSpell(73));
    }

    @Test void createMagicalWeaponPinsNativeInventorySelectorCostsAndMessages() {
        BattleSpellCatalog.MagicalWeaponSpell spell =
                BattleSpellCatalog.magicalWeaponSpell(
                        BattleSpellCatalog.CREATE_MAGICAL_WEAPON);
        assertEquals(73, spell.skillId());
        assertEquals(9, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(763, spell.effectGfx());
        assertEquals(200, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(10, spell.itemConsumeCount());
        assertEquals(685, spell.happenMessage());
        assertEquals(79, spell.failureMessage());
        assertEquals(161, spell.successMessage());
        assertEquals("create-magical-weapon", spell.key());
        assertEquals(200, spell.effectiveMpCost(12, 0));
        assertEquals(193, spell.effectiveMpCost(19, 0));
        assertEquals(191, spell.effectiveMpCost(19, 2));
        assertNull(BattleSpellCatalog.magicalWeaponSpell(72));
    }

    @Test void holyWeaponPinsNativeCase7ResourcesAndWeaponAbilityRows() {
        BattleSpellCatalog.WeaponBuffSpell spell =
                BattleSpellCatalog.weaponBuffSpell(BattleSpellCatalog.HOLY_WEAPON);
        assertEquals(8, spell.skillId());
        assertEquals(0, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2165, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(1, spell.meleeHit());
        assertEquals(1, spell.meleeDamage());
        assertEquals(0, spell.rangedHit());
        assertEquals(0, spell.rangedDamage());
        assertEquals(79, spell.missingWeaponMessage());
        assertEquals(161, spell.successMessage());
        assertEquals("holy-weapon", spell.key());
        assertEquals(10, spell.effectiveMpCost(12, 0));
        assertEquals(10, spell.effectiveMpCost(19, 0));
        assertEquals(8, spell.effectiveMpCost(19, 2));
    }

    @Test void blessWeaponPinsNativeCase47ResourcesAndWeaponAbilityRows() {
        BattleSpellCatalog.WeaponBuffSpell spell =
                BattleSpellCatalog.weaponBuffSpell(BattleSpellCatalog.BLESS_WEAPON);
        assertEquals(48, spell.skillId());
        assertEquals(5, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2176, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(2, spell.meleeHit());
        assertEquals(2, spell.meleeDamage());
        assertEquals(2, spell.rangedHit());
        assertEquals(0, spell.rangedDamage());
        assertEquals(79, spell.missingWeaponMessage());
        assertEquals(161, spell.successMessage());
        assertEquals("bless-weapon", spell.key());
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(15, spell.effectiveMpCost(17, 0));
        assertEquals(13, spell.effectiveMpCost(17, 2));
        assertNull(BattleSpellCatalog.weaponBuffSpell(47));
    }

    @Test void manaDrainPinsNativeCase38ResourceProbabilityAndTransferRules() {
        BattleSpellCatalog.ManaDrainSpell spell =
                BattleSpellCatalog.manaDrainSpell(BattleSpellCatalog.MANA_DRAIN);
        assertEquals(39, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2171, spell.effectGfx());
        assertEquals(1, spell.mpCost());
        assertEquals(50, spell.hpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(4, spell.maximumRange());
        assertEquals(12, spell.probabilityDice());
        assertEquals(5, spell.bossModifier());
        assertEquals(280, spell.failureMessage());
        assertEquals("mana-drain", spell.key());
        assertEquals(1, spell.effectiveMpCost(8, 0));
        assertNull(BattleSpellCatalog.manaDrainSpell(40));
    }

    @Test void weightSpellsPinExactDatabaseSelectorsCostsAndEffects() {
        BattleSpellCatalog.WeightSpell decrease =
                BattleSpellCatalog.weightSpell(BattleSpellCatalog.DECREASE_WEIGHT);
        assertEquals(1, decrease.row());
        assertEquals(5, decrease.column());
        assertEquals(19, decrease.actionId());
        assertEquals(2170, decrease.effectGfx());
        assertEquals(10, decrease.mpCost());
        assertEquals(50, decrease.hpCost());
        assertEquals(1_800, decrease.durationSeconds());
        assertEquals(BattleSpellCatalog.WeightEffect.DECREASE_WEIGHT,
                decrease.effect());

        BattleSpellCatalog.ExoticVitalizeSpell exotic =
                BattleSpellCatalog.exoticVitalizeSpell(
                        BattleSpellCatalog.EXOTIC_VITALIZE);
        assertEquals(169, exotic.skillId());
        assertEquals(21, exotic.row());
        assertEquals(0, exotic.column());
        assertEquals(22, exotic.level());
        assertEquals(0, exotic.number());
        assertEquals(19, exotic.actionId());
        assertEquals(4400, exotic.effectGfx());
        assertEquals(30, exotic.mpCost());
        assertEquals(100, exotic.reuseDelayMillis());
        assertEquals(0, exotic.secondaryReuseDelayMillis());
        assertEquals(100, exotic.clientCastClockMillis());
        assertEquals(960, exotic.durationSeconds());
        assertEquals(0, exotic.maximumRange());
        assertEquals(0, exotic.area());
        assertEquals(0, exotic.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                exotic.attribute());
        assertEquals(2, exotic.type());
        assertEquals(1, exotic.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, exotic.itemConsumeId());
        assertEquals(1, exotic.itemConsumeCount());
        assertEquals(0, exotic.happenMessage());
        assertEquals(0, exotic.stopMessage());
        assertEquals(0, exotic.failureMessage());
        assertTrue(exotic.counterMagicBypass());
        assertEquals(30, exotic.effectiveMpCost(0));
        assertEquals(28, exotic.effectiveMpCost(2));
        assertEquals("exotic-vitalize", exotic.key());
        assertNull(BattleSpellCatalog.weightSpell(
                BattleSpellCatalog.EXOTIC_VITALIZE));
        assertNull(BattleSpellCatalog.exoticVitalizeSpell(168));

        BattleSpellCatalog.WaterLifeSpell waterLife =
                BattleSpellCatalog.waterLifeSpell(BattleSpellCatalog.WATER_LIFE);
        assertEquals(170, waterLife.skillId());
        assertEquals(21, waterLife.row());
        assertEquals(1, waterLife.column());
        assertEquals(22, waterLife.level());
        assertEquals(1, waterLife.number());
        assertEquals(19, waterLife.actionId());
        assertEquals(4401, waterLife.effectGfx());
        assertEquals(1, waterLife.mpCost());
        assertEquals(100, waterLife.reuseDelayMillis());
        assertEquals(0, waterLife.secondaryReuseDelayMillis());
        assertEquals(400, waterLife.clientCastClockMillis());
        assertEquals(64, waterLife.durationSeconds());
        assertEquals(-1, waterLife.maximumRange());
        assertEquals(0, waterLife.area());
        assertEquals(1, waterLife.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER,
                waterLife.attribute());
        assertEquals(2, waterLife.type());
        assertEquals(2, waterLife.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL,
                waterLife.itemConsumeId());
        assertEquals(1, waterLife.itemConsumeCount());
        assertEquals(0, waterLife.happenMessage());
        assertEquals(0, waterLife.stopMessage());
        assertEquals(280, waterLife.failureMessage());
        assertTrue(waterLife.counterMagicBypass());
        assertEquals(1, waterLife.effectiveMpCost(0));
        assertEquals(0, waterLife.effectiveMpCost(2));
        assertEquals("water-life", waterLife.key());
        assertNull(BattleSpellCatalog.waterLifeSpell(169));

        BattleSpellCatalog.AdditionalFireSpell additional =
                BattleSpellCatalog.additionalFireSpell(
                        BattleSpellCatalog.ADDITIONAL_FIRE);
        assertEquals(176, additional.skillId());
        assertEquals(21, additional.row());
        assertEquals(7, additional.column());
        assertEquals(22, additional.level());
        assertEquals(7, additional.number());
        assertEquals(19, additional.actionId());
        assertEquals(5825, additional.effectGfx());
        assertEquals(30, additional.mpCost());
        assertEquals(100, additional.reuseDelayMillis());
        assertEquals(0, additional.secondaryReuseDelayMillis());
        assertEquals(100, additional.clientCastClockMillis());
        assertEquals(960, additional.durationSeconds());
        assertEquals(0, additional.maximumRange());
        assertEquals(0, additional.area());
        assertEquals(0, additional.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                additional.attribute());
        assertEquals(2, additional.type());
        assertEquals(128, additional.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL,
                additional.itemConsumeId());
        assertEquals(1, additional.itemConsumeCount());
        assertEquals(0, additional.happenMessage());
        assertEquals(0, additional.stopMessage());
        assertEquals(0, additional.failureMessage());
        assertTrue(additional.counterMagicBypass());
        assertEquals(30, additional.effectiveMpCost(0));
        assertEquals(28, additional.effectiveMpCost(2));
        assertEquals("additional-fire", additional.key());
        assertNull(BattleSpellCatalog.additionalFireSpell(169));
        assertNull(BattleSpellCatalog.weightSpell(
                BattleSpellCatalog.ADDITIONAL_FIRE));
        assertNull(BattleSpellCatalog.weightSpell(190));
    }

    @Test void hastePinsNativeCase42ObjectTargetAndQualifiedHelmMpRules() {
        BattleSpellCatalog.HasteSpell spell =
                BattleSpellCatalog.hasteSpell(BattleSpellCatalog.HASTE);
        assertEquals(43, spell.skillId());
        assertEquals(5, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(755, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(280, spell.failureMessage());
        assertEquals("haste", spell.key());
        assertEquals(40, spell.effectiveMpCost(12, 0, false));
        assertEquals(36, spell.effectiveMpCost(16, 0, false));
        assertEquals(18, spell.effectiveMpCost(16, 0, true));
        assertEquals(15, spell.effectiveMpCost(17, 2, true));
        assertNull(BattleSpellCatalog.hasteSpell(44));
    }

    @Test void cancellationPinsNativeCase43ObjectTargetCostAndMaterials() {
        BattleSpellCatalog.CancellationSpell spell =
                BattleSpellCatalog.cancellationSpell(BattleSpellCatalog.CANCELLATION);
        assertEquals(44, spell.skillId());
        assertEquals(5, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(870, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals(40, spell.effectiveMpCost(12, 0));
        assertEquals(35, spell.effectiveMpCost(17, 0));
        assertEquals(33, spell.effectiveMpCost(17, 2));
        assertNull(BattleSpellCatalog.cancellationSpell(43));
    }

    @Test void massSlowPinsNativeCase75AreaProbabilityAndResources() {
        BattleSpellCatalog.MassSlowSpell spell =
                BattleSpellCatalog.massSlowSpell(BattleSpellCatalog.MASS_SLOW);
        assertEquals(76, spell.skillId());
        assertEquals(9, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3932, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(64, spell.durationSeconds());
        assertEquals(7, spell.probabilityDice());
        assertEquals(3, spell.bossModifier());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals("mass-slow", spell.key());
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(13, spell.effectiveMpCost(19, 0));
        assertEquals(11, spell.effectiveMpCost(19, 2));
        int normal = spell.probability(3, 52, 18, 1, 4, 20, false,
                sides -> sides);
        int boss = spell.probability(3, 52, 18, 1, 4, 20, true,
                sides -> sides);
        assertEquals(Math.max(1, normal / 3), boss);
        assertNull(BattleSpellCatalog.massSlowSpell(75));
    }

    @Test void entanglePinsNativeCase151TargetProbabilityAndResources() {
        BattleSpellCatalog.EntangleSpell spell =
                BattleSpellCatalog.entangleSpell(BattleSpellCatalog.ENTANGLE);
        assertEquals(152, spell.skillId());
        assertEquals(18, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.level());
        assertEquals(7, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2250, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.targetTo());
        assertEquals(10, spell.maximumRange());
        assertEquals(0, spell.areaRadius());
        assertEquals(0, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(1, spell.type());
        assertEquals(128, spell.nativeId());
        assertEquals(711, spell.stopMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.30D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertFalse(spell.counterMagicBypass());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(20, spell.effectiveMpCost(0));
        assertEquals(18, spell.effectiveMpCost(2));
        assertEquals(50, spell.probabilityForTarget(
                50, 50, 20, 0, 4, 100, false));
        assertEquals(0, spell.probabilityForTarget(
                50, 50, 20, 0, 4, 100, true));
        assertNull(BattleSpellCatalog.entangleSpell(151));
    }

    @Test void slowPinsNativeCase28SelectedReuseAndNegativeControl() {
        BattleSpellCatalog.EntangleSpell spell =
                BattleSpellCatalog.entangleSpell(BattleSpellCatalog.SLOW);
        assertEquals(29, spell.skillId());
        assertEquals(3, spell.row());
        assertEquals(4, spell.column());
        assertEquals(4, spell.level());
        assertEquals(4, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(752, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.targetTo());
        assertEquals(8, spell.maximumRange());
        assertEquals(0, spell.areaRadius());
        assertEquals(1, spell.type());
        assertEquals(16, spell.nativeId());
        assertEquals(277, spell.stopMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(5, spell.bossModifier());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals("slow", spell.key());
        assertEquals(15, spell.effectiveMpCost(15, 2));
        assertEquals(50, spell.probabilityForTarget(
                50, 50, 20, 0, 4, 100, false));
        assertEquals(10, spell.probabilityForTarget(
                50, 50, 20, 0, 4, 100, true));
    }

    @Test void eraseMagicPinsNativeCase152TargetProbabilityAndResources() {
        BattleSpellCatalog.EraseMagicSpell spell =
                BattleSpellCatalog.eraseMagicSpell(BattleSpellCatalog.ERASE_MAGIC);
        assertEquals(153, spell.skillId());
        assertEquals(19, spell.row());
        assertEquals(0, spell.column());
        assertEquals(20, spell.level());
        assertEquals(0, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(10_706, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(600, spell.reuseDelayMillis());
        assertEquals(32, spell.durationSeconds());
        assertEquals(3, spell.targetTo());
        assertEquals(-1, spell.maximumRange());
        assertEquals(10, spell.selectionRange());
        assertEquals(0, spell.areaRadius());
        assertEquals(0, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(1, spell.type());
        assertEquals(1, spell.nativeId());
        assertEquals(0, spell.happenMessage());
        assertEquals(0, spell.stopMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.35D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertTrue(spell.counterMagicBypass());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(8, spell.effectiveMpCost(2));
        assertEquals(51, spell.probabilityForTarget(
                50, 50, 20, 0, 4, 100, false));
        assertEquals(0, spell.probabilityForTarget(
                50, 50, 20, 0, 4, 100, true));
        assertNull(BattleSpellCatalog.eraseMagicSpell(152));
    }

    @Test void lesserElementalPinsNativeCase153SelectorResourcesAndAttributeNpc() {
        BattleSpellCatalog.ElementalSummonSpell spell =
                BattleSpellCatalog.elementalSummonSpell(
                        BattleSpellCatalog.LESSER_ELEMENTAL);
        assertEquals(154, spell.skillId());
        assertEquals(19, spell.row());
        assertEquals(1, spell.column());
        assertEquals(20, spell.level());
        assertEquals(1, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2510, spell.effectGfx());
        assertEquals(20, spell.effectiveMpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.targetTo());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.areaRadius());
        assertEquals(0, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                spell.attribute());
        assertEquals(128, spell.type());
        assertEquals(2, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals(353, spell.mapFailureMessage());
        assertEquals(755, spell.spawnEffectGfx());
        assertEquals(1_800, spell.lifetimeSeconds());
        assertTrue(spell.counterMagicBypass());
        assertEquals(45306, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_EARTH));
        assertEquals(45303, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_FIRE));
        assertEquals(45304, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_WATER));
        assertEquals(45305, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_WIND));
        assertEquals(45306, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_EARTH
                        | BattleSpellCatalog.ATTRIBUTE_WIND));
        assertEquals(0, spell.summonNpcId(0));
        assertEquals(25, spell.petCost(18));
        assertNull(BattleSpellCatalog.elementalSummonSpell(153));
    }

    @Test void greaterElementalPinsNativeCase161SelectorResourcesAndAttributeNpc() {
        BattleSpellCatalog.ElementalSummonSpell spell =
                BattleSpellCatalog.elementalSummonSpell(
                        BattleSpellCatalog.GREATER_ELEMENTAL);
        assertEquals(162, spell.skillId());
        assertEquals(20, spell.row());
        assertEquals(1, spell.column());
        assertEquals(21, spell.level());
        assertEquals(1, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2510, spell.effectGfx());
        assertEquals(20, spell.effectiveMpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.targetTo());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.areaRadius());
        assertEquals(0, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                spell.attribute());
        assertEquals(128, spell.type());
        assertEquals(2, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(4, spell.itemConsumeCount());
        assertEquals(353, spell.mapFailureMessage());
        assertEquals(755, spell.spawnEffectGfx());
        assertEquals(1_800, spell.lifetimeSeconds());
        assertTrue(spell.counterMagicBypass());
        assertEquals(81053, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_EARTH));
        assertEquals(81050, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_FIRE));
        assertEquals(81051, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_WATER));
        assertEquals(81052, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_WIND));
        assertEquals(81053, spell.summonNpcId(
                BattleSpellCatalog.ATTRIBUTE_EARTH
                        | BattleSpellCatalog.ATTRIBUTE_WIND));
        assertEquals(0, spell.summonNpcId(0));
        assertEquals(25, spell.petCost(18));
    }

    @Test void disintegratePinsNativeCase76LawfulGateAndRetainedCritical() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.DISINTEGRATE);
        assertEquals(77, spell.skillId());
        assertEquals(9, spell.row());
        assertEquals(4, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(11746, spell.effectGfx());
        assertEquals(70, spell.mpCost());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(190, spell.baseDamage());
        assertEquals(8, spell.diceCount());
        assertEquals(15, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_NONE, spell.attribute());
        assertEquals(500, spell.minimumLawful());
        assertEquals(40, spell.specialCriticalChancePercent());
        assertEquals(11748, spell.specialCriticalGfx());
        assertEquals(60, BattleSpellCatalog.DISINTEGRATE_EFFECT_FRAMES);
        assertEquals(24, BattleSpellCatalog.DISINTEGRATE_EFFECT_FPS);
        assertEquals(2_500,
                BattleSpellCatalog.DISINTEGRATE_EFFECT_COMPLETION_MILLIS);
        assertEquals("disintegrate", spell.key());
        assertFalse(spell.canCastAtLawful(499));
        assertTrue(spell.canCastAtLawful(500));
        assertTrue(spell.specialCritical(sides -> 40));
        assertFalse(spell.specialCritical(sides -> 41));
        assertEquals(11748, spell.effectGfx(true));
        assertEquals(11746, spell.effectGfx(false));
        assertEquals(600, spell.damageAfterSpecialCritical(300, true));
        assertEquals(300, spell.damageAfterSpecialCritical(300, false));
        assertEquals(Integer.MAX_VALUE,
                spell.damageAfterSpecialCritical(Integer.MAX_VALUE, true));
        assertEquals(70, spell.effectiveMpCost(12, 0));
        assertEquals(61, spell.effectiveMpCost(21, 0));
        assertEquals(59, spell.effectiveMpCost(21, 2));
    }

    @Test void regenSpellsPinNativeSelectorsCostsTargetsAndMprModifiers() {
        BattleSpellCatalog.RegenSpell meditation =
                BattleSpellCatalog.regenSpell(BattleSpellCatalog.MEDITATION);
        assertEquals(3, meditation.row());
        assertEquals(7, meditation.column());
        assertEquals(19, meditation.actionId());
        assertEquals(2173, meditation.effectGfx());
        assertEquals(5, meditation.mpCost());
        assertEquals(30, meditation.hpCost());
        assertEquals(10, meditation.reuseDelayMillis());
        assertEquals(640, meditation.durationSeconds());
        assertEquals(BattleSpellCatalog.Targeting.SELF_SELECTOR, meditation.targeting());
        assertEquals(5, meditation.mprBonus());
        assertEquals(true, meditation.breaksOnActivity());
        assertEquals(5, meditation.effectiveMpCost(12, 0));
        assertEquals(2, meditation.effectiveMpCost(15, 0));
        assertEquals(0, meditation.effectiveMpCost(15, 2));

        BattleSpellCatalog.RegenSpell concentration =
                BattleSpellCatalog.regenSpell(BattleSpellCatalog.CONCENTRATION);
        assertEquals(25, concentration.row());
        assertEquals(5, concentration.column());
        assertEquals(6527, concentration.effectGfx());
        assertEquals(20, concentration.mpCost());
        assertEquals(0, concentration.hpCost());
        assertEquals(1_000, concentration.reuseDelayMillis());
        assertEquals(1_200, concentration.clientCastClockMillis());
        assertEquals(600, concentration.durationSeconds());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                concentration.requestShape());
        assertEquals(BattleSpellCatalog.Targeting.PLAYER_OBJECT, concentration.targeting());
        assertEquals(3, concentration.maximumRange());
        assertEquals(2, concentration.mprBonus());
        assertEquals(false, concentration.breaksOnActivity());
        assertEquals(20, concentration.effectiveMpCost(18, 0));
        assertEquals(18, concentration.effectiveMpCost(18, 2));
        assertNull(BattleSpellCatalog.regenSpell(190));
    }

    @Test void shockStunPinsNativeCase86WeaponGateDamageProbabilityAndTimer() {
        BattleSpellCatalog.ShockStunSpell spell =
                BattleSpellCatalog.shockStunSpell(BattleSpellCatalog.SHOCK_STUN);
        assertEquals(87, spell.skillId());
        assertEquals(10, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.databaseActionId());
        assertEquals(4434, spell.impactGfx());
        assertEquals(81162, spell.effectNpcId());
        assertEquals(4183, spell.effectNpcGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(8_000, spell.reuseDelayMillis());
        assertEquals(1, spell.maximumRange());
        assertEquals(10, spell.baseDamage());
        assertEquals(0, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(0, spell.attribute());
        assertTrue(spell.supportsWalkAction(BattleActionCodes.TWO_HAND_SWORD_WALK));
        assertTrue(spell.supportsWalkAction(BattleActionCodes.EDORYU_WALK));
        assertFalse(spell.supportsWalkAction(BattleActionCodes.SWORD_WALK));
        assertEquals(15, spell.effectiveMpCost(12, 0));
        assertEquals(14, spell.effectiveMpCost(13, 0));
        assertEquals(0, spell.effectiveMpCost(26, 2));
        assertEquals(57, spell.probability(60, 50, 5, 20, 10, 3, false));
        assertEquals(5, spell.probability(60, 50, 5, 20, 10, 3, true));
        assertEquals(90, spell.probability(60, 50, 99, 0, -10, 99, false));
        assertEquals(2, spell.rollStunSeconds(sides -> 1));
        assertEquals(4, spell.rollStunSeconds(sides -> sides));
        assertEquals(4, spell.maximumStunSeconds());
        assertEquals("shock-stun", spell.key());
        assertNull(BattleSpellCatalog.shockStunSpell(86));
    }

    @Test void reductionArmorPinsNativeCase87ResourcesAndAsymmetricDamageRules() {
        BattleSpellCatalog.ReductionArmorSpell spell =
                BattleSpellCatalog.reductionArmorSpell(
                        BattleSpellCatalog.REDUCTION_ARMOR);
        assertEquals(88, spell.skillId());
        assertEquals(10, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(4824, spell.effectGfx());
        assertEquals(5, spell.mpCost());
        assertEquals(50, spell.hpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(192, spell.durationSeconds());
        assertEquals(50, spell.levelThreshold());
        assertEquals(5, spell.levelDivisor());
        assertEquals(10, spell.playerPhysicalBaseReduction());
        assertEquals(1, spell.otherBaseReduction());
        assertEquals(5, spell.effectiveMpCost(12, 0));
        assertEquals(4, spell.effectiveMpCost(13, 0));
        assertEquals(0, spell.effectiveMpCost(18, 2));
        assertEquals(10, spell.reduction(50, true));
        assertEquals(1, spell.reduction(50, false));
        assertEquals(12, spell.reduction(60, true));
        assertEquals(3, spell.reduction(60, false));
        assertEquals(8, spell.applyTo(20, 60, true));
        assertEquals(17, spell.applyTo(20, 60, false));
        assertEquals(0, spell.applyTo(2, 60, false));
        assertEquals("reduction-armor", spell.key());
        assertNull(BattleSpellCatalog.reductionArmorSpell(87));
    }

    @Test void bounceAttackPinsNativeCase88ResourcesAndCombatRules() {
        BattleSpellCatalog.BounceAttackSpell spell =
                BattleSpellCatalog.bounceAttackSpell(
                        BattleSpellCatalog.BOUNCE_ATTACK);
        assertEquals(89, spell.skillId());
        assertEquals(11, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(4648, spell.effectGfx());
        assertEquals(5, spell.mpCost());
        assertEquals(120, spell.hpCost());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(-2, spell.armorClassModifier());
        assertEquals(6, spell.meleeHitModifier());
        assertEquals(10, spell.weaponDamageChance());
        assertEquals(5, spell.effectiveMpCost(12, 0));
        assertEquals(4, spell.effectiveMpCost(13, 0));
        assertEquals(0, spell.effectiveMpCost(18, 2));
        assertEquals("bounce-attack", spell.key());
        assertNull(BattleSpellCatalog.bounceAttackSpell(88));
    }

    @Test void solidCarriagePinsNativeCase89EquipmentGateAndErLease() {
        Protocol.SpellSelector selector = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(11).c(1).bytes());
        assertEquals(BattleSpellCatalog.SOLID_CARRIAGE, selector.skillId());

        BattleSpellCatalog.SolidCarriageSpell spell =
                BattleSpellCatalog.solidCarriageSpell(
                        BattleSpellCatalog.SOLID_CARRIAGE);
        assertEquals(90, spell.skillId());
        assertEquals(11, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(5831, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(100, spell.hpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(15, spell.evasionRateModifier());
        assertEquals(10, spell.effectiveMpCost(12, 0));
        assertEquals(9, spell.effectiveMpCost(13, 0));
        assertEquals(2, spell.effectiveMpCost(18, 2));
        assertEquals("solid-carriage", spell.key());
        assertNull(BattleSpellCatalog.solidCarriageSpell(89));
    }

    @Test void counterBarrierPinsNativeCase90WeaponGateAndReflectionRules() {
        Protocol.SpellSelector selector = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(11).c(2).bytes());
        assertEquals(BattleSpellCatalog.COUNTER_BARRIER, selector.skillId());

        BattleSpellCatalog.CounterBarrierSpell spell =
                BattleSpellCatalog.counterBarrierSpell(
                        BattleSpellCatalog.COUNTER_BARRIER);
        assertEquals(91, spell.skillId());
        assertEquals(11, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(5832, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(100, spell.hpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(3, spell.requiredWeaponType());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertEquals(90, spell.maximumProbability());
        assertEquals(0, spell.bossModifier());
        assertEquals(10710, spell.triggerEffectGfx());
        assertEquals(1.0D, spell.damageMultiplier());
        assertEquals(10, spell.effectiveMpCost(12, 0));
        assertEquals(9, spell.effectiveMpCost(13, 0));
        assertEquals(2, spell.effectiveMpCost(18, 2));
        assertEquals("counter-barrier", spell.key());
        assertNull(BattleSpellCatalog.counterBarrierSpell(90));
    }

    @Test void skills81Through86AreReservedNativeSelectorSlotsNotEffects() {
        int[] durations = {0, 0, 0, 0, 900, 30};
        int[] graphics = {0, 0, 0, 0, 2059, 760};
        for (int skillId = 81; skillId <= 86; skillId++) {
            BattleSpellCatalog.ReservedSkillSlot slot =
                    BattleSpellCatalog.reservedSkillSlot(skillId);
            int index = skillId - 81;
            assertEquals(skillId, slot.skillId());
            assertEquals(10, slot.row());
            assertEquals(index, slot.column());
            assertEquals(0, slot.skillLevel());
            assertEquals(durations[index], slot.durationSeconds());
            assertEquals(19, slot.actionId());
            assertEquals(graphics[index], slot.castGfx());
            assertTrue(slot.templatePresent());
            assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                    slot.requestShape());
        }
        assertNull(BattleSpellCatalog.reservedSkillSlot(80));
        assertNull(BattleSpellCatalog.reservedSkillSlot(87));
    }

    @Test void skill24IsReservedWizardSelectorSlotNotGfx189Effect() {
        BattleSpellCatalog.ReservedSkillSlot slot =
                BattleSpellCatalog.reservedSkillSlot(24);
        assertEquals(24, slot.skillId());
        assertEquals(2, slot.row());
        assertEquals(7, slot.column());
        assertEquals(0, slot.skillLevel());
        assertEquals(0, slot.durationSeconds());
        assertEquals(19, slot.actionId());
        assertEquals(189, slot.castGfx());
        assertTrue(slot.templatePresent());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                slot.requestShape());
        assertNull(BattleSpellCatalog.reservedSkillSlot(23));
        assertNull(BattleSpellCatalog.reservedSkillSlot(25));
    }

    @Test void skills92Through96AreReservedLevelZeroSlotsNotEffects() {
        for (int skillId = 92; skillId <= 96; skillId++) {
            BattleSpellCatalog.ReservedSkillSlot slot =
                    BattleSpellCatalog.reservedSkillSlot(skillId);
            assertEquals(skillId, slot.skillId());
            assertEquals(11, slot.row());
            assertEquals(skillId - 89, slot.column());
            assertEquals(0, slot.skillLevel());
            assertEquals(1_200, slot.durationSeconds());
            assertEquals(19, slot.actionId());
            assertEquals(5906 + skillId - 92, slot.castGfx());
            assertTrue(slot.templatePresent());
            assertEquals(skillId == 93
                            ? BattleSpellCatalog.RequestShape.OBJECT_TARGET
                            : BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                    slot.requestShape());
        }
        assertNull(BattleSpellCatalog.reservedSkillSlot(91));
        assertNull(BattleSpellCatalog.reservedSkillSlot(97));
    }

    @Test void skills119Through128AreExplicitReservedNativeSelectorSlots() {
        for (int skillId = 119; skillId <= 128; skillId++) {
            BattleSpellCatalog.ReservedSkillSlot slot =
                    BattleSpellCatalog.reservedSkillSlot(skillId);
            assertEquals(skillId, slot.skillId());
            assertEquals((skillId - 1) / 8, slot.row());
            assertEquals((skillId - 1) % 8, slot.column());
            assertEquals(skillId <= 120 ? 15 : 0, slot.skillLevel());
            assertEquals(0, slot.durationSeconds());
            assertEquals(19, slot.actionId());
            assertEquals(skillId == 120 ? 2280 : 0, slot.castGfx());
            assertEquals(skillId != 122, slot.templatePresent());
            assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                    slot.requestShape());
        }
        assertNull(BattleSpellCatalog.reservedSkillSlot(129));
    }

    @Test void resistMagicPinsNativeSelectorResourceAndMrContract() {
        BattleSpellCatalog.MagicResistanceSpell spell =
                BattleSpellCatalog.magicResistanceSpell(
                        BattleSpellCatalog.RESIST_MAGIC);
        assertEquals(129, spell.skillId());
        assertEquals(16, spell.row());
        assertEquals(0, spell.column());
        assertEquals(17, spell.skillLevel());
        assertEquals(0, spell.skillNumber());
        assertEquals(19, spell.actionId());
        assertEquals(2186, spell.effectGfx());
        assertEquals(5, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(1, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(10, spell.magicResistance());
        assertEquals(719, spell.stopMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(5, spell.effectiveMpCost(8, 0));
        assertEquals(3, spell.effectiveMpCost(8, 2));
        assertEquals("resist-magic", spell.key());
        assertNull(BattleSpellCatalog.magicResistanceSpell(128));
    }

    @Test void bodyToMindPinsNativeConversionAndCrossSourceReuseContract() {
        BattleSpellCatalog.BodyToMindSpell spell =
                BattleSpellCatalog.bodyToMindSpell(
                        BattleSpellCatalog.BODY_TO_MIND);
        assertEquals(130, spell.skillId());
        assertEquals(16, spell.row());
        assertEquals(1, spell.column());
        assertEquals(17, spell.skillLevel());
        assertEquals(1, spell.skillNumber());
        assertEquals(19, spell.actionId());
        assertEquals(2179, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(8, spell.hpCost());
        assertEquals(100, spell.reuseDelayMillis());
        assertEquals(400, spell.secondaryReuseDelayMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(2, spell.nativeId());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());
        assertEquals(702, spell.happenMessage());
        assertEquals(0, spell.stopMessage());
        assertEquals(0, spell.failureMessage());
        assertEquals(2, spell.manaGain());
        assertTrue(spell.counterMagicBypass());
        assertTrue(spell.hasEnoughHitPoints(9));
        assertFalse(spell.hasEnoughHitPoints(8));
        assertEquals(1, spell.hitPointsAfter(9));
        assertEquals(42, spell.hitPointsAfter(50));
        assertEquals(97, spell.manaAfter(95, 100));
        assertEquals(100, spell.manaAfter(99, 100));
        assertEquals(100, spell.manaAfter(100, 100));
        assertThrows(IllegalArgumentException.class,
                () -> spell.hitPointsAfter(8));
        assertThrows(IllegalArgumentException.class,
                () -> spell.manaAfter(101, 100));
        assertEquals("body-to-mind", spell.key());
        assertNull(BattleSpellCatalog.bodyToMindSpell(129));
    }

    @Test void bloodySoulPinsNativeHpFortyMpTwentyInstantConversion() {
        BattleSpellCatalog.BodyToMindSpell spell =
                BattleSpellCatalog.bodyToMindSpell(
                        BattleSpellCatalog.BLOODY_SOUL);
        assertEquals(146, spell.skillId());
        assertEquals(18, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.skillLevel());
        assertEquals(1, spell.skillNumber());
        assertEquals(19, spell.actionId());
        assertEquals(2178, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(40, spell.hpCost());
        assertEquals(1_200, spell.reuseDelayMillis());
        assertEquals(1_200, spell.secondaryReuseDelayMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(2, spell.nativeId());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());
        assertEquals(0, spell.happenMessage());
        assertEquals(0, spell.stopMessage());
        assertEquals(0, spell.failureMessage());
        assertEquals(20, spell.manaGain());
        assertTrue(spell.counterMagicBypass());
        assertTrue(spell.hasEnoughHitPoints(41));
        assertFalse(spell.hasEnoughHitPoints(40));
        assertEquals(1, spell.hitPointsAfter(41));
        assertEquals(60, spell.hitPointsAfter(100));
        assertEquals(95, spell.manaAfter(75, 100));
        assertEquals(100, spell.manaAfter(95, 100));
        assertEquals("bloody-soul", spell.key());
        assertThrows(IllegalArgumentException.class,
                () -> spell.hitPointsAfter(40));
    }

    @Test void worldTreeCallPinsNativeClassicDestinationAndRejectedMapContract() {
        BattleSpellCatalog.WorldTreeCallSpell spell =
                BattleSpellCatalog.worldTreeCallSpell(
                        BattleSpellCatalog.WORLD_TREE_CALL);
        assertEquals(131, spell.skillId());
        assertEquals(16, spell.row());
        assertEquals(2, spell.column());
        assertEquals(17, spell.skillLevel());
        assertEquals(2, spell.skillNumber());
        assertEquals(19, spell.actionId());
        assertEquals(169, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(128, spell.sourceType());
        assertEquals(4, spell.nativeId());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());
        assertEquals(4, spell.destinationMapId());
        assertEquals(33_051, spell.destinationX());
        assertEquals(32_337, spell.destinationY());
        assertEquals(5, spell.destinationHeading());
        assertEquals(276, spell.mapRejectedMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(10, spell.effectiveMpCost(8, 0));
        assertEquals(8, spell.effectiveMpCost(8, 2));
        assertEquals("world-tree-call", spell.key());
        assertNull(BattleSpellCatalog.worldTreeCallSpell(130));
    }

    @Test void tripleArrowPinsNativeThreePhysicalShotsAndCrossSourceReuseContract() {
        BattleSpellCatalog.TripleArrowSpell spell =
                BattleSpellCatalog.tripleArrowSpell(
                        BattleSpellCatalog.TRIPLE_ARROW);
        assertEquals(132, spell.skillId());
        assertEquals(16, spell.row());
        assertEquals(3, spell.column());
        assertEquals(17, spell.level());
        assertEquals(3, spell.number());
        assertEquals(18, spell.databaseActionId());
        assertEquals(4394, spell.effectGfx());
        assertEquals(11, spell.mpCost());
        assertEquals(100, spell.reuseDelayMillis());
        assertEquals(400, spell.secondaryReuseDelayMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(3, spell.targetTo());
        assertEquals(0, spell.areaRadius());
        assertEquals(-1, spell.maximumRange());
        assertEquals(0, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(64, spell.type());
        assertEquals(8, spell.nativeId());
        assertEquals(3, spell.hitCount());
        assertTrue(spell.counterMagicBypass());
        assertEquals(11, spell.effectiveMpCost(12, 0));
        assertEquals(8, spell.effectiveMpCost(15, 0));
        assertEquals(6, spell.effectiveMpCost(15, 2));
        assertEquals("triple-arrow", spell.key());
        assertNull(BattleSpellCatalog.tripleArrowSpell(131));
    }

    @Test void elementalFallDownPinsNativeResistanceProbabilityAndLeaseContract() {
        BattleSpellCatalog.ElementalFallDownSpell spell =
                BattleSpellCatalog.elementalFallDownSpell(
                        BattleSpellCatalog.ELEMENTAL_FALL_DOWN);
        assertEquals(133, spell.skillId());
        assertEquals(16, spell.row());
        assertEquals(4, spell.column());
        assertEquals(17, spell.level());
        assertEquals(4, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(4396, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(100, spell.reuseDelayMillis());
        assertEquals(32, spell.durationSeconds());
        assertEquals(3, spell.targetTo());
        assertEquals(10, spell.maximumRange());
        assertEquals(0, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(1, spell.type());
        assertEquals(16, spell.nativeId());
        assertEquals(280, spell.failureMessage());
        assertEquals(50, spell.resistanceDebuff());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.30D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertFalse(spell.counterMagicBypass());
        assertEquals(56, spell.probability(45, 40, 8, 0, 0, 20));
        assertEquals(0, spell.probabilityForTarget(
                45, 40, 8, 0, 0, 20, true));
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(8, spell.effectiveMpCost(2));
        assertEquals("elemental-fall-down", spell.key());
        assertNull(BattleSpellCatalog.elementalFallDownSpell(132));
    }

    @Test void counterMirrorPinsNativeSelfWardMaterialAndReflectionContract() {
        BattleSpellCatalog.CounterMirrorSpell spell =
                BattleSpellCatalog.counterMirrorSpell(
                        BattleSpellCatalog.COUNTER_MIRROR);
        assertEquals(134, spell.skillId());
        assertEquals(16, spell.row());
        assertEquals(5, spell.column());
        assertEquals(17, spell.level());
        assertEquals(5, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(4395, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(1000, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(16, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                spell.attribute());
        assertEquals(2, spell.type());
        assertEquals(32, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(280, spell.failureMessage());
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(8, spell.effectiveMpCost(2));
        assertEquals("counter-mirror", spell.key());
        assertNull(BattleSpellCatalog.counterMirrorSpell(133));
    }

    @Test void clearMindAndPrecedingReservedGapPinExactNativeContract() {
        for (int skillId : new int[] {135, 136}) {
            BattleSpellCatalog.ReservedSkillSlot slot =
                    BattleSpellCatalog.reservedSkillSlot(skillId);
            assertEquals(skillId, slot.skillId());
            assertEquals(16, slot.row());
            assertEquals(skillId - 129, slot.column());
            assertEquals(0, slot.skillLevel());
            assertEquals(0, slot.durationSeconds());
            assertEquals(19, slot.actionId());
            assertEquals(0, slot.castGfx());
            assertTrue(slot.templatePresent());
            assertEquals(BattleSpellCatalog.RequestShape.NO_PRODUCER,
                    slot.requestShape());
        }

        BattleSpellCatalog.ClearMindSpell spell =
                BattleSpellCatalog.clearMindSpell(BattleSpellCatalog.CLEAR_MIND);
        assertEquals(137, spell.skillId());
        assertEquals(17, spell.row());
        assertEquals(0, spell.column());
        assertEquals(18, spell.level());
        assertEquals(0, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2180, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(1200, spell.durationSeconds());
        assertEquals(2, spell.type());
        assertEquals(1, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(3, spell.wisdomModifier());
        assertEquals(722, spell.happenMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(8, spell.effectiveMpCost(2));
        assertEquals("clear-mind", spell.key());
        assertNull(BattleSpellCatalog.clearMindSpell(136));
    }

    @Test void resistElementalPinsExactNativeContract() {
        BattleSpellCatalog.ResistElementalSpell spell =
                BattleSpellCatalog.resistElementalSpell(
                        BattleSpellCatalog.RESIST_ELEMENTAL);
        assertEquals(138, spell.skillId());
        assertEquals(17, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, spell.level());
        assertEquals(1, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2184, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(1200, spell.durationSeconds());
        assertEquals(2, spell.type());
        assertEquals(2, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(10, spell.resistanceModifier());
        assertEquals(721, spell.happenMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(8, spell.effectiveMpCost(2));
        assertEquals("resist-elemental", spell.key());
        assertNull(BattleSpellCatalog.resistElementalSpell(137));
    }

    @Test void skills139Through144AreNoProducerLevelZeroSlots() {
        for (int skillId = 139; skillId <= 144; skillId++) {
            BattleSpellCatalog.ReservedSkillSlot slot =
                    BattleSpellCatalog.reservedSkillSlot(skillId);
            assertEquals(skillId, slot.skillId());
            assertEquals(17, slot.row());
            assertEquals(skillId - 137, slot.column());
            assertEquals(0, slot.skillLevel());
            assertEquals(0, slot.durationSeconds());
            assertEquals(19, slot.actionId());
            assertEquals(0, slot.castGfx());
            assertTrue(slot.templatePresent());
            assertEquals(BattleSpellCatalog.RequestShape.NO_PRODUCER,
                    slot.requestShape());
        }
        assertNull(BattleSpellCatalog.reservedSkillSlot(138));
        assertNull(BattleSpellCatalog.reservedSkillSlot(145));
    }

    @Test void elementalProtectionPinsExactNativeContract() {
        BattleSpellCatalog.ElementalProtectionSpell spell =
                BattleSpellCatalog.elementalProtectionSpell(
                        BattleSpellCatalog.ELEMENTAL_PROTECTION);
        assertEquals(147, spell.skillId());
        assertEquals(18, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.level());
        assertEquals(2, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2285, spell.effectGfx());
        assertEquals(6, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(2, spell.type());
        assertEquals(4, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(50, spell.resistanceModifier());
        assertEquals(0, spell.happenMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(6, spell.effectiveMpCost(0));
        assertEquals(4, spell.effectiveMpCost(2));
        assertEquals("elemental-protection", spell.key());
        assertNull(BattleSpellCatalog.elementalProtectionSpell(146));
    }

    @Test void fireWeaponPinsExactNativeContract() {
        BattleSpellCatalog.FireWeaponSpell spell =
                BattleSpellCatalog.fireWeaponSpell(
                        BattleSpellCatalog.FIRE_WEAPON);
        assertEquals(148, spell.skillId());
        assertEquals(18, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.level());
        assertEquals(3, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2182, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.type());
        assertEquals(8, spell.nativeId());
        assertEquals(0, spell.meleeHitModifier());
        assertEquals(4, spell.meleeDamageModifier());
        assertEquals(723, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(15, spell.effectiveMpCost(0));
        assertEquals(13, spell.effectiveMpCost(2));
        assertEquals("fire-weapon", spell.key());
        assertNull(BattleSpellCatalog.fireWeaponSpell(147));
    }

    @Test void burningWeaponPinsExactNativeContract() {
        BattleSpellCatalog.FireWeaponSpell spell =
                BattleSpellCatalog.fireWeaponSpell(
                        BattleSpellCatalog.BURNING_WEAPON);
        assertEquals(163, spell.skillId());
        assertEquals(20, spell.row());
        assertEquals(2, spell.column());
        assertEquals(21, spell.level());
        assertEquals(2, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2242, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE,
                spell.attribute());
        assertEquals(2, spell.type());
        assertEquals(4, spell.nativeId());
        assertEquals(3, spell.meleeHitModifier());
        assertEquals(6, spell.meleeDamageModifier());
        assertEquals(723, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(28, spell.effectiveMpCost(2));
        assertEquals("burning-weapon", spell.key());
    }

    @Test void windShotPinsExactNativeContract() {
        BattleSpellCatalog.WindShotSpell spell =
                BattleSpellCatalog.windShotSpell(BattleSpellCatalog.WIND_SHOT);
        assertEquals(149, spell.skillId());
        assertEquals(18, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.level());
        assertEquals(4, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2246, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(1, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.type());
        assertEquals(16, spell.nativeId());
        assertEquals(6, spell.rangedHitModifier());
        assertEquals(724, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(15, spell.effectiveMpCost(0));
        assertEquals(13, spell.effectiveMpCost(2));
        assertEquals("wind-shot", spell.key());
        assertNull(BattleSpellCatalog.windShotSpell(148));
    }

    @Test void stormShotPinsExactNativeContract() {
        BattleSpellCatalog.StormShotSpell spell =
                BattleSpellCatalog.stormShotSpell(BattleSpellCatalog.STORM_SHOT);
        assertEquals(166, spell.skillId());
        assertEquals(20, spell.row());
        assertEquals(5, spell.column());
        assertEquals(21, spell.level());
        assertEquals(5, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2248, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.type());
        assertEquals(32, spell.nativeId());
        assertEquals(-1, spell.rangedHitModifier());
        assertEquals(5, spell.rangedDamageModifier());
        assertEquals(724, spell.happenMessage());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(28, spell.effectiveMpCost(2));
        assertEquals("storm-shot", spell.key());
        assertNull(BattleSpellCatalog.stormShotSpell(165));
    }

    @Test void windShacklePinsNativeTargetClockMaterialAndProbabilityContract() {
        BattleSpellCatalog.WindShackleSpell spell =
                BattleSpellCatalog.windShackleSpell(
                        BattleSpellCatalog.WIND_SHACKLE);
        assertEquals(167, spell.skillId());
        assertEquals(20, spell.row());
        assertEquals(6, spell.column());
        assertEquals(21, spell.level());
        assertEquals(6, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(1799, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(100, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(600, spell.clientCastClockMillis());
        assertEquals(16, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(8, spell.clientClickRange());
        assertEquals(-1, spell.area());
        assertEquals(3, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(1, spell.type());
        assertEquals(64, spell.nativeId());
        assertEquals(1001, spell.happenMessage());
        assertEquals(0, spell.stopMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.30D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertFalse(spell.counterMagicBypass());
        assertEquals(56, spell.probabilityForTarget(
                45, 40, 8, 0, 0, 20, false));
        assertEquals(46, spell.probabilityForTarget(
                40, 40, 8, 0, 0, 20, false));
        assertEquals(36, spell.probabilityForTarget(
                35, 40, 8, 0, 0, 20, false));
        assertEquals(0, spell.probabilityForTarget(
                45, 40, 8, 0, 0, 20, true));
        assertEquals(50, spell.effectiveMpCost(0));
        assertEquals(48, spell.effectiveMpCost(2));
        assertEquals("wind-shackle", spell.key());
        assertNull(BattleSpellCatalog.windShackleSpell(166));
    }

    @Test void ironSkinPinsNativeSelfAcMessagesAndCoexistContract() {
        BattleSpellCatalog.IronSkinSpell spell =
                BattleSpellCatalog.ironSkinSpell(BattleSpellCatalog.IRON_SKIN);
        assertEquals(168, spell.skillId());
        assertEquals(20, spell.row());
        assertEquals(7, spell.column());
        assertEquals(21, spell.level());
        assertEquals(7, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2252, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.secondaryReuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(0, spell.area());
        assertEquals(0, spell.targetTo());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.type());
        assertEquals(128, spell.nativeId());
        assertEquals(-10, spell.armorClassModifier());
        assertEquals(10, spell.legacyIconType());
        assertEquals(528, spell.coexistGraphic());
        assertEquals(714, spell.happenMessage());
        assertEquals(725, spell.stopMessage());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(28, spell.effectiveMpCost(2));
        assertEquals("iron-skin", spell.key());
        assertNull(BattleSpellCatalog.ironSkinSpell(167));
    }

    @Test void returnToNaturePinsNativePetTargetMaterialAndOwnerLevelFormula() {
        BattleSpellCatalog.ReturnToNatureSpell spell =
                BattleSpellCatalog.returnToNatureSpell(
                        BattleSpellCatalog.RETURN_TO_NATURE);
        assertEquals(145, spell.skillId());
        assertEquals(18, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.level());
        assertEquals(0, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2245, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(16, spell.targetTo());
        assertEquals(-1, spell.maximumRange());
        assertEquals(0, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(1, spell.type());
        assertEquals(1, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());
        assertEquals(280, spell.failureMessage());
        assertEquals(79, spell.invalidTargetMessage());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.30D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertFalse(spell.includeMagicLevel());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertFalse(spell.counterMagicBypass());
        assertEquals(56, spell.probability(45, 40, 8, 0, 0, 20));
        assertEquals(36, spell.probability(40, 45, 8, 0, 0, 20));
        assertEquals(30, spell.effectiveMpCost(0));
        assertEquals(28, spell.effectiveMpCost(2));
        assertEquals("return-to-nature", spell.key());
        assertNull(BattleSpellCatalog.returnToNatureSpell(144));
    }

    @Test void enchantWeaponPinsNativeItemObjectLeaseAndHelmMpContract() {
        BattleSpellCatalog.ItemWeaponEnchantSpell spell =
                BattleSpellCatalog.itemWeaponEnchantSpell(
                        BattleSpellCatalog.ENCHANT_WEAPON);
        assertEquals(12, spell.skillId());
        assertEquals(1, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(747, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_800, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(8, spell.nativeId());
        assertEquals(2, spell.meleeDamage());
        assertEquals(2, spell.rangedDamage());
        assertEquals(79, spell.invalidItemMessage());
        assertEquals(161, spell.successMessage());
        assertEquals(308, spell.expirationMessage());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());
        assertFalse(spell.consumesItem());
        assertEquals(20, spell.effectiveMpCost(8, 0, false));
        assertEquals(18, spell.effectiveMpCost(8, 2, false));
        assertEquals(19, spell.effectiveMpCost(13, 0, false));
        assertEquals(9, spell.effectiveMpCost(13, 0, true));
        assertEquals(7, spell.effectiveMpCost(13, 2, true));
        assertEquals("enchant-weapon", spell.key());
        assertEquals("Enchant-Weapon", spell.displayName());
    }

    @Test void blessedArmorPinsNativeItemObjectLeaseAndCircleMpContract() {
        BattleSpellCatalog.ItemArmorEnchantSpell spell =
                BattleSpellCatalog.itemArmorEnchantSpell(
                        BattleSpellCatalog.BLESSED_ARMOR);
        assertEquals(21, spell.skillId());
        assertEquals(2, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(748, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_800, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(16, spell.nativeId());
        assertEquals(-3, spell.armorClass());
        assertEquals(79, spell.invalidItemMessage());
        assertEquals(161, spell.successMessage());
        assertEquals(308, spell.expirationMessage());
        assertEquals(20, spell.effectiveMpCost(12, 0));
        assertEquals(19, spell.effectiveMpCost(13, 0));
        assertEquals(18, spell.effectiveMpCost(14, 0));
        assertEquals(16, spell.effectiveMpCost(14, 2));
        assertEquals("blessed-armor", spell.key());
        assertEquals("Blessed-Armor", spell.displayName());
        assertNull(BattleSpellCatalog.itemArmorEnchantSpell(20));
    }

    @Test void shadowFangPinsNativeItemObjectMaterialAndDamageContract() {
        BattleSpellCatalog.ItemWeaponEnchantSpell spell =
                BattleSpellCatalog.itemWeaponEnchantSpell(
                        BattleSpellCatalog.SHADOW_FANG);
        assertEquals(107, spell.skillId());
        assertEquals(13, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2951, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(192, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(4, spell.nativeId());
        assertEquals(5, spell.meleeDamage());
        assertEquals(5, spell.rangedDamage());
        assertEquals(79, spell.invalidItemMessage());
        assertEquals(0, spell.successMessage());
        assertEquals(308, spell.expirationMessage());
        assertEquals(InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(30, spell.effectiveMpCost(8, 0));
        assertEquals(28, spell.effectiveMpCost(8, 2));
        assertTrue(spell.consumesItem());
        assertEquals("Shadow-Fang", spell.displayName());
        assertEquals("shadow-fang", spell.key());
        assertNull(BattleSpellCatalog.itemWeaponEnchantSpell(106));
    }

    @Test void finalBurnPinsNativeTargetEnvelopeAndExceptionalResourceContract() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.FINAL_BURN);
        assertEquals(108, spell.skillId());
        assertEquals(13, spell.row());
        assertEquals(3, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(2952, spell.effectGfx());
        assertEquals(1, spell.mpCost());
        assertEquals(1, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(1, spell.maximumRange());
        assertEquals(0, spell.baseDamage());
        assertEquals(0, spell.diceCount());
        assertEquals(0, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_NONE, spell.attribute());
        assertTrue(spell.finalBurn());
        assertEquals("final-burn", spell.key());
        assertEquals(1, spell.effectiveMpCost(8, 0));
        assertEquals(0, spell.effectiveMpCost(8, 1));
        assertNull(BattleSpellCatalog.attackSpell(109));
    }

    @Test void areaSilencePinsNativeScreenMaterialAndProbabilityContract() {
        BattleSpellCatalog.AreaSilenceSpell spell =
                BattleSpellCatalog.areaSilenceSpell(
                        BattleSpellCatalog.AREA_OF_SILENCE);
        assertEquals(161, spell.skillId());
        assertEquals(20, spell.row());
        assertEquals(0, spell.column());
        assertEquals(21, spell.level());
        assertEquals(0, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(2241, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(9000, spell.reuseDelayMillis());
        assertEquals(16, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(-1, spell.area());
        assertEquals(3, spell.targetTo());
        assertEquals(1, spell.through());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                spell.attribute());
        assertEquals(1, spell.type());
        assertEquals(1, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(8, spell.itemConsumeCount());
        assertEquals(55, spell.probabilityHigh());
        assertEquals(45, spell.probabilityEqual());
        assertEquals(35, spell.probabilityLow());
        assertEquals(0.30D, spell.intelligenceMultiplier());
        assertEquals(0.05D, spell.magicResistanceMultiplier());
        assertTrue(spell.includeMagicHit());
        assertEquals(0, spell.bossModifier());
        assertEquals(56, spell.probability(45, 40, 8, 0, 20, false));
        assertEquals(46, spell.probability(40, 40, 8, 0, 20, false));
        assertEquals(36, spell.probability(35, 40, 8, 0, 20, false));
        assertEquals(0, spell.probability(45, 40, 8, 0, 20, true));
        assertEquals(40, spell.effectiveMpCost(0));
        assertEquals(38, spell.effectiveMpCost(2));
        assertEquals("area-of-silence", spell.key());
        assertNull(BattleSpellCatalog.areaSilenceSpell(160));
    }

    @Test void thunderGrabPinsNativeSelectorDamageProbabilityAndBook() {
        BattleSpellCatalog.ThunderGrabSpell thunder =
                BattleSpellCatalog.thunderGrabSpell(
                        BattleSpellCatalog.THUNDER_GRAB);
        BattleSpellCatalog.AttackSpell attack = thunder.attackSpell();
        assertEquals(192, attack.skillId());
        assertEquals(23, attack.row());
        assertEquals(7, attack.column());
        assertEquals(18, attack.actionId());
        assertEquals(6512, attack.effectGfx());
        assertEquals(0, attack.mpCost());
        assertEquals(45, thunder.hpCost());
        assertEquals(4_000, attack.reuseDelayMillis());
        assertEquals(1_800, thunder.clientCastClockMillis());
        assertEquals(8, attack.maximumRange());
        assertEquals(80, attack.baseDamage());
        assertEquals(2, attack.diceCount());
        assertEquals(10, attack.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, attack.attribute());
        assertEquals(50, thunder.successChancePercent(25, 24, 0, false));
        assertEquals(30, thunder.successChancePercent(24, 24, 0, false));
        assertEquals(15, thunder.successChancePercent(23, 24, 0, false));
        assertEquals(20, thunder.successChancePercent(25, 24, 30, false));
        assertEquals(1, thunder.successChancePercent(25, 24, 100, false));
        assertEquals(0, thunder.successChancePercent(25, 24, 0, true));
        assertTrue(thunder.succeeds(25, 24, 0, false, 50));
        assertFalse(thunder.succeeds(25, 24, 0, false, 51));
        assertFalse(thunder.succeeds(25, 24, 0, true, 1));
        assertEquals(2, thunder.bindDurationSeconds(0));
        assertEquals(4, thunder.bindDurationSeconds(2));
        assertTrue(thunder.counterMagicBypass());
        assertEquals(49_113L, thunder.skillBookItemId());
        assertEquals("$5711", thunder.skillBookNameId());
        assertEquals(attack, BattleSpellCatalog.attackSpell(192));
        assertNull(BattleSpellCatalog.thunderGrabSpell(191));
        assertThrows(IllegalArgumentException.class,
                () -> thunder.succeeds(25, 24, 0, false, 0));
        assertThrows(IllegalArgumentException.class,
                () -> thunder.bindDurationSeconds(3));
    }
}
