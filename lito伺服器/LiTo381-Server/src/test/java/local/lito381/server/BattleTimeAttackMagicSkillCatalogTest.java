package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

final class BattleTimeAttackMagicSkillCatalogTest {
    @Test
    void selectedMagicRowsPreserveExactMobskillAndSkillFields() {
        BattleTimeAttackMagicSkillCatalog.Definition sea =
                BattleTimeAttackMagicSkillCatalog.magicForNpc(45812);
        assertNotNull(sea);
        assertEquals(100, sea.triggerChance());
        assertEquals(-8, sea.triggerRange());
        assertEquals(10, sea.leverage());
        assertEquals(BattleSpellCatalog.CONE_OF_COLD, sea.skillId());
        assertEquals(1_440, sea.actionDelayMillis());
        assertEquals(18, sea.mpCost());
        assertEquals(18, sea.actionId());
        assertEquals(11738, sea.effectGfx());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, sea.attribute());

        BattleTimeAttackMagicSkillCatalog.Definition minotaur =
                BattleTimeAttackMagicSkillCatalog.magicForNpc(45584);
        assertEquals(20, minotaur.triggerChance());
        assertEquals(-3, minotaur.triggerRange());
        assertEquals(40, minotaur.leverage());
        assertEquals(BattleSpellCatalog.EARTHQUAKE, minotaur.skillId());
        assertEquals(3, minotaur.areaRadius());
        assertEquals(1_520, minotaur.actionDelayMillis());

        BattleTimeAttackMagicSkillCatalog.Definition ancient =
                BattleTimeAttackMagicSkillCatalog.magicForNpc(45610);
        assertEquals(30, ancient.triggerChance());
        assertEquals(-15, ancient.triggerRange());
        assertEquals(30, ancient.leverage());
        assertEquals(10019, ancient.skillId());
        assertEquals(5, ancient.mpCost());
        assertEquals(100, ancient.baseDamage());
        assertEquals(10, ancient.diceCount());
        assertEquals(10, ancient.dieSides());
        assertEquals(2483, ancient.effectGfx());
        assertEquals(BattleTimeAttackMagicSkillCatalog.Targeting.CASTER_SCREEN,
                ancient.targeting());
    }

    @Test
    void attackPositionCoinMatchesRetainedL1NpcInstanceAttack() {
        // 1-伺服器 L1NpcInstance.java:894-897: nextInt(2) >= 1 -> ordinary attack.
        assertTrue(BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(1, 1));
        assertTrue(BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(0, 1));
        assertFalse(BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(1, 0));
        // Not an adjacent attack position: the mobskill roll always runs.
        assertFalse(BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(2, 1));
        assertFalse(BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(8, 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(-1, 0));
        // Roll 100 used by the coin never passes any strict TriRnd row.
        for (int npcId : new int[] {45278, 45584, 45601, 45610, 45650,
                45682, 45683, 45684, 45812}) {
            for (BattleTimeAttackMagicSkillCatalog.MobskillRow row
                    : BattleTimeAttackMagicSkillCatalog.rowsForNpc(npcId)) {
                assertFalse(row.triggerRoll(100), npcId + "/" + row.actNo());
            }
        }
    }

    @Test
    void selectedPhysicalRowsAndStrictTriRndBoundaryRemainExact() {
        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition ogre =
                BattleTimeAttackMagicSkillCatalog.physicalForNpc(45278);
        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition minotaur =
                BattleTimeAttackMagicSkillCatalog.physicalForNpc(45584);
        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition ancient =
                BattleTimeAttackMagicSkillCatalog.physicalForNpc(45610);
        assertEquals(0, ogre.actNo());
        assertEquals(30, ogre.triggerChance());
        assertEquals(-2, ogre.triggerRange());
        assertEquals(2, ogre.attackRange());
        assertEquals(20, ogre.leverage());
        // 1-伺服器 db/380_db.sql:34296 ActId=30; L1MobSkillUse.java:660-661.
        assertEquals(30, ogre.actionOverride());
        assertEquals(30, ogre.effectiveActionId());
        assertEquals(1, ogre.areaWidth());
        assertEquals(2, ogre.areaHeight());
        assertTrue(ogre.areaAttack());
        assertTrue(ogre.triggerRoll(29));
        assertFalse(ogre.triggerRoll(30));
        assertEquals(-1, minotaur.triggerRange());
        assertEquals(-2, ancient.triggerRange());
        assertEquals(1, minotaur.attackRange());
        assertEquals(2, ancient.attackRange());
        assertEquals(30, minotaur.leverage());
        assertEquals(30, ancient.leverage());
        assertEquals(0, minotaur.actionOverride());
        assertEquals(0, ancient.actionOverride());
        assertEquals(BattleActionCodes.ATTACK, minotaur.effectiveActionId());
        assertEquals(BattleActionCodes.ATTACK, ancient.effectiveActionId());
        assertTrue(minotaur.triggerRoll(99));
        assertFalse(minotaur.triggerRoll(100));

        BattleTimeAttackMagicSkillCatalog.Definition sea =
                BattleTimeAttackMagicSkillCatalog.magicForNpc(45812);
        assertTrue(sea.triggerRoll(99));
        assertFalse(sea.triggerRoll(100));
        assertTrue(sea.triggerDistance(8));
        assertFalse(sea.triggerDistance(9));
    }

    @Test
    void everySelectedCasterSpriteHasItsExactMagicAction() {
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(1603, 18));
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(2402, 18));
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(2421, 18));
        assertFalse(BattleAnimationCatalog.get().supportsTimedAction(
                1603, BattleActionCodes.ATTACK));
    }

    @Test
    void tokyoFinalBossRowsPreserveEverySelectedActNumber() {
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> deathKnight =
                BattleTimeAttackMagicSkillCatalog.rowsForNpc(45601);
        assertEquals(List.of(0, 1, 2, 3),
                deathKnight.stream().map(
                        BattleTimeAttackMagicSkillCatalog.MobskillRow::actNo).toList());

        List<BattleTimeAttackMagicSkillCatalog.Definition> deathKnightMagic =
                BattleTimeAttackMagicSkillCatalog.magicForNpcDefinitions(45601);
        assertEquals(2, deathKnightMagic.size());
        BattleTimeAttackMagicSkillCatalog.Definition vampire =
                deathKnightMagic.get(0);
        assertEquals(20, vampire.triggerChance());
        assertEquals(-3, vampire.triggerRange());
        assertEquals(50, vampire.leverage());
        assertEquals(28, vampire.skillId());
        assertEquals(900, vampire.actionDelayMillis());
        assertEquals(12, vampire.mpCost());
        assertEquals(50, vampire.reuseDelayMillis());
        assertEquals(18, vampire.actionId());
        assertEquals(236, vampire.effectGfx());
        assertEquals(4, vampire.maximumRange());
        assertEquals(12, vampire.baseDamage());
        assertEquals(3, vampire.diceCount());
        assertEquals(10, vampire.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, vampire.attribute());
        assertTrue(vampire.drainsLife());

        BattleTimeAttackMagicSkillCatalog.Definition earthquake =
                deathKnightMagic.get(1);
        assertEquals(60, earthquake.triggerChance());
        assertEquals(-6, earthquake.triggerRange());
        assertEquals(30, earthquake.leverage());
        assertEquals(10056, earthquake.skillId());
        assertEquals(960, earthquake.actionDelayMillis());
        assertEquals(5, earthquake.mpCost());
        assertEquals(3_000, earthquake.reuseDelayMillis());
        assertEquals(19, earthquake.actionId());
        assertEquals(245, earthquake.effectGfx());
        assertEquals(4, earthquake.areaRadius());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, earthquake.attribute());

        BattleTimeAttackMagicSkillCatalog.SummonDefinition summon =
                BattleTimeAttackMagicSkillCatalog.summonForNpc(45601);
        assertEquals(5, summon.triggerChance());
        assertEquals(-3, summon.triggerRange());
        assertEquals(2, summon.triggerCount());
        assertEquals(45161, summon.summonNpcId());
        assertEquals(1, summon.summonCount(0));
        assertEquals(3, summon.summonCount(2));
        assertEquals(960, summon.actionDelayMillis());
        assertEquals(19, summon.actionId());
        assertEquals(761, summon.effectGfx());

        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physical =
                BattleTimeAttackMagicSkillCatalog.physicalForNpc(45601);
        assertEquals(3, physical.actNo());
        assertEquals(100, physical.triggerChance());
        assertEquals(-1, physical.triggerRange());
        assertEquals(1, physical.attackRange());
        assertEquals(20, physical.leverage());
    }

    @Test
    void zombieLordRowsPreserveWideMagicAndTwoTilePhysicalAttack() {
        BattleTimeAttackMagicSkillCatalog.Definition magic =
                BattleTimeAttackMagicSkillCatalog.magicForNpc(45650);
        assertEquals(30, magic.triggerChance());
        assertEquals(-6, magic.triggerRange());
        assertEquals(20, magic.leverage());
        assertEquals(10000, magic.skillId());
        assertEquals(1_800, magic.actionDelayMillis());
        assertEquals(5, magic.mpCost());
        assertEquals(18, magic.sourceActionId());
        assertEquals(19, magic.actionId());
        assertEquals(2806, magic.effectGfx());
        assertEquals(6, magic.areaRadius());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, magic.attribute());

        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physical =
                BattleTimeAttackMagicSkillCatalog.physicalForNpc(45650);
        assertEquals(40, physical.triggerChance());
        assertEquals(-2, physical.triggerRange());
        assertEquals(2, physical.attackRange());
        assertEquals(30, physical.leverage());

        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(240, 18));
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(240, 19));
        assertFalse(BattleAnimationCatalog.get().supportsTimedAction(2803, 18));
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(2803, 19));
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(145, 0));
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(145, 1));
    }

    @Test
    void tokyoBossAndSummonPhysicalSnapshotsMatchSelectedNpcRows() {
        assertEquals(new SourceNpcPhysicalStats(
                        45601, -75, false, 0, true, 3, 2, 150,
                        false, -120, false, true, 250, 77, 17, false, 1, 0),
                SourceNpcPhysicalStats.memoryFixture(45601));
        assertEquals(new SourceNpcPhysicalStats(
                        45650, -62, true, 0, true, 3, 0, 50,
                        false, -180, false, true, 100, 67, 62, false, 2, 0),
                SourceNpcPhysicalStats.memoryFixture(45650));
        assertEquals(new SourceNpcPhysicalStats(
                        45161, -3, false, 0, false, 1, 2, 18,
                        true, -18, true, false, 30, 17, 16, true, 1, 0),
                SourceNpcPhysicalStats.memoryFixture(45161));
    }

    @Test
    void dragonDamageMagicRowsPreserveEveryExecutableSelectedField() {
        List<BattleTimeAttackMagicSkillCatalog.Definition> antharas =
                BattleTimeAttackMagicSkillCatalog.magicForNpcDefinitions(45682);
        assertEquals(List.of(0, 1, 2, 3), antharas.stream()
                .map(BattleTimeAttackMagicSkillCatalog.Definition::actNo).toList());
        assertMagic(antharas.get(0), 60, -5, 40, 31001, 2_240,
                BattleTimeAttackMagicSkillCatalog.Targeting.TARGET_RADIUS,
                5, 30, 4156, 4, 6, 200, 8, 10,
                BattleSpellCatalog.ATTRIBUTE_WATER);
        assertMagic(antharas.get(1), 10, -3, 40, 10027, 2_240,
                BattleTimeAttackMagicSkillCatalog.Targeting.DIRECTIONAL,
                5, 30, 4156, 20, 0, 1_500, 50, 10,
                BattleSpellCatalog.ATTRIBUTE_EARTH);
        assertMagic(antharas.get(2), 20, -3, 40, 31004, 2_240,
                BattleTimeAttackMagicSkillCatalog.Targeting.DIRECTIONAL,
                5, 25, 129, 6, 0, 75, 3, 5,
                BattleSpellCatalog.ATTRIBUTE_EARTH);
        assertMagic(antharas.get(3), 60, -5, 40, 31002, 2_240,
                BattleTimeAttackMagicSkillCatalog.Targeting.CASTER_RADIUS,
                5, 19, 2510, 0, 17, 150, 3, 5,
                BattleSpellCatalog.ATTRIBUTE_EARTH);
        List<BattleTimeAttackMagicSkillCatalog.Definition> fafurion =
                BattleTimeAttackMagicSkillCatalog.magicForNpcDefinitions(45683);
        assertEquals(List.of(0, 1, 2, 3), fafurion.stream()
                .map(BattleTimeAttackMagicSkillCatalog.Definition::actNo).toList());
        assertMagic(fafurion.get(0), 60, -5, 29, 33001, 1_600,
                BattleTimeAttackMagicSkillCatalog.Targeting.TARGET_RADIUS,
                5, 30, 4163, 4, 6, 200, 8, 10,
                BattleSpellCatalog.ATTRIBUTE_WATER);
        assertMagic(fafurion.get(1), 10, -3, 29, 10029, 1_600,
                BattleTimeAttackMagicSkillCatalog.Targeting.DIRECTIONAL,
                5, 30, 4163, 20, 0, 1_500, 50, 10,
                BattleSpellCatalog.ATTRIBUTE_WATER);
        assertMagic(fafurion.get(2), 60, -5, 29, 33002, 1_600,
                BattleTimeAttackMagicSkillCatalog.Targeting.CASTER_RADIUS,
                5, 19, 2510, 0, 17, 200, 3, 5,
                BattleSpellCatalog.ATTRIBUTE_FIRE);
        assertMagic(fafurion.get(3), 45, -4, 29, 33003, 1_600,
                BattleTimeAttackMagicSkillCatalog.Targeting.TARGET_RADIUS,
                5, 18, 2510, 4, 3, 75, 3, 5,
                BattleSpellCatalog.ATTRIBUTE_WIND);

        List<BattleTimeAttackMagicSkillCatalog.Definition> valakas =
                BattleTimeAttackMagicSkillCatalog.magicForNpcDefinitions(45684);
        assertEquals(List.of(0, 1, 3, 4), valakas.stream()
                .map(BattleTimeAttackMagicSkillCatalog.Definition::actNo).toList());
        assertMagic(valakas.get(0), 60, -5, 40, 31001, 1_840,
                BattleTimeAttackMagicSkillCatalog.Targeting.TARGET_RADIUS,
                5, 30, 4156, 4, 6, 200, 8, 10,
                BattleSpellCatalog.ATTRIBUTE_WATER);
        assertMagic(valakas.get(1), 10, -3, 40, 10028, 1_840,
                BattleTimeAttackMagicSkillCatalog.Targeting.DIRECTIONAL,
                5, 30, 4160, 20, 0, 1_500, 50, 10,
                BattleSpellCatalog.ATTRIBUTE_FIRE);
        assertMagic(valakas.get(2), 60, -5, 40, 30002, 1_840,
                BattleTimeAttackMagicSkillCatalog.Targeting.CASTER_RADIUS,
                5, 19, 2510, 0, 17, 150, 3, 5,
                BattleSpellCatalog.ATTRIBUTE_FIRE);
        assertMagic(valakas.get(3), 45, -4, 40, 30003, 1_840,
                BattleTimeAttackMagicSkillCatalog.Targeting.TARGET_RADIUS,
                5, 18, 2510, 4, 3, 75, 3, 5,
                BattleSpellCatalog.ATTRIBUTE_FIRE);
    }

    @Test
    void dragonInertRowsRemainExplicitAndCannotDisplaceActNumbers() {
        List<BattleTimeAttackMagicSkillCatalog.InertDefinition> inert =
                BattleTimeAttackMagicSkillCatalog.inertDefinitions();
        assertEquals(2, inert.size());
        BattleTimeAttackMagicSkillCatalog.InertDefinition antharas = inert.get(0);
        assertEquals(45682, antharas.npcId());
        assertEquals(7, antharas.actNo());
        assertEquals(31001, antharas.skillId());
        assertEquals(8, antharas.triggerRange());
        assertEquals(4, antharas.maximumRange());
        assertEquals("retained-trigger-and-skill-range-intersection-empty",
                antharas.reason());
        for (int distance = 0; distance <= 64; distance++) {
            assertFalse(antharas.executableDistance(distance));
        }

        BattleTimeAttackMagicSkillCatalog.InertDefinition valakas = inert.get(1);
        assertEquals(45684, valakas.npcId());
        assertEquals(7, valakas.actNo());
        assertEquals(31000, valakas.skillId());
        assertEquals(8, valakas.triggerRange());
        assertEquals(0, valakas.maximumRange());
        assertEquals("selected-skill-template-absent"
                        + "+retained-L1SkillUse-checkUseSkill-false",
                valakas.reason());
        for (int distance = 0; distance <= 64; distance++) {
            assertFalse(valakas.executableDistance(distance));
        }

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7),
                BattleTimeAttackMagicSkillCatalog.rowsForNpc(45682).stream()
                        .map(BattleTimeAttackMagicSkillCatalog.MobskillRow::actNo)
                        .toList());
        assertEquals(List.of(0, 1, 2, 3, 4),
                BattleTimeAttackMagicSkillCatalog.rowsForNpc(45683).stream()
                        .map(BattleTimeAttackMagicSkillCatalog.MobskillRow::actNo)
                        .toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7),
                BattleTimeAttackMagicSkillCatalog.rowsForNpc(45684).stream()
                        .map(BattleTimeAttackMagicSkillCatalog.MobskillRow::actNo)
                        .toList());
    }

    @Test
    void antharasActFourPreservesExactVisualOnlySelectedContract() {
        BattleTimeAttackMagicSkillCatalog.VisualDefinition visual =
                BattleTimeAttackMagicSkillCatalog.visualForNpc(45682);
        assertNotNull(visual);
        assertEquals(4, visual.actNo());
        assertEquals(45, visual.triggerChance());
        assertEquals(-4, visual.triggerRange());
        assertEquals(40, visual.leverage());
        assertEquals(31003, visual.skillId());
        assertEquals(2_240, visual.actionDelayMillis());
        assertEquals(5, visual.mpCost());
        assertEquals(0, visual.reuseDelayMillis());
        assertEquals(18, visual.actionId());
        assertEquals(2510, visual.effectGfx());
        assertEquals(10, visual.maximumRange());
        assertEquals(0, visual.durationMillis());
        assertTrue(visual.triggerDistance(4));
        assertTrue(visual.castDistance(10));
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(1245, 18));
    }

    @Test
    void everyExecutableDragonMagicActionExistsInSelectedSpriteTable() {
        for (BattleTimeAttackMagicSkillCatalog.Definition definition
                : BattleTimeAttackMagicSkillCatalog.magicDefinitions()) {
            if (definition.npcId() < 45682 || definition.npcId() > 45684) continue;
            int gfx = switch (definition.npcId()) {
                case 45682 -> 1245;
                case 45683 -> 1590;
                case 45684 -> 2001;
                default -> throw new AssertionError();
            };
            assertTrue(BattleAnimationCatalog.get().supportsTimedAction(
                    gfx, definition.actionId()),
                    "missing action " + definition.actionId() + " for gfx " + gfx);
        }
        assertFalse(BattleAnimationCatalog.get().supportsTimedAction(2001, 25));
    }

    @Test
    void valakasFirePrisonPinsSelectedIdentityAndExecutableGroundContract() {
        BattleTimeAttackMagicSkillCatalog.GroundDefinition fire =
                BattleTimeAttackMagicSkillCatalog.groundForNpc(45684);
        assertNotNull(fire);
        assertEquals(2, fire.actNo());
        assertEquals(30004, fire.skillId());
        assertEquals(5, fire.mpCost());
        assertEquals(18, fire.actionId());
        assertEquals(25, fire.sourceActionId());
        assertEquals(81157, fire.effectNpcId());
        assertEquals(168, fire.effectGfx());
        assertEquals(10_000, fire.durationMillis());
        assertEquals(1_200, fire.pulseIntervalMillis());
        assertEquals(8, fire.pulseCount());
        assertEquals(25, fire.fixedDamage());
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(2001, 18));
        assertFalse(BattleAnimationCatalog.get().supportsTimedAction(2001, 25));

        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        ArenaMapCatalog.Point center = new ArenaMapCatalog.Point(32737, 32829);
        List<ArenaMapCatalog.Point> points = fire.points(
                center.x(), center.y(), arena);
        assertEquals(9, points.size());
        assertTrue(points.contains(center));
    }

    @Test
    void dragonPhysicalRowsPreserveDirectionalBoxFieldsAndGeometry() {
        List<BattleTimeAttackMagicSkillCatalog.PhysicalDefinition> antharas =
                BattleTimeAttackMagicSkillCatalog.physicalForNpcDefinitions(45682);
        assertEquals(List.of(5, 6), antharas.stream()
                .map(BattleTimeAttackMagicSkillCatalog.PhysicalDefinition::actNo)
                .toList());
        assertPhysical(antharas.get(0), 50, 5, 3, 3);
        assertPhysical(antharas.get(1), 80, 1, 0, 3);
        assertPhysical(BattleTimeAttackMagicSkillCatalog.physicalForNpc(45683),
                50, 5, 3, 3);
        List<BattleTimeAttackMagicSkillCatalog.PhysicalDefinition> valakas =
                BattleTimeAttackMagicSkillCatalog.physicalForNpcDefinitions(45684);
        assertPhysical(valakas.get(0), 50, 5, 3, 3);
        assertPhysical(valakas.get(1), 80, 1, 0, 3);

        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition wide = antharas.get(0);
        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition line = antharas.get(1);
        // Heading 2 is east in the retained headingRotate table.
        assertTrue(wide.areaContains(100, 100, 2, 103, 100));
        assertTrue(wide.areaContains(100, 100, 2, 102, 102));
        assertFalse(wide.areaContains(100, 100, 2, 99, 100));
        assertTrue(line.areaContains(100, 100, 2, 103, 100));
        assertFalse(line.areaContains(100, 100, 2, 102, 101));
    }

    private static void assertMagic(
            BattleTimeAttackMagicSkillCatalog.Definition actual,
            int chance, int triggerRange, int leverage, int skillId,
            int delay, BattleTimeAttackMagicSkillCatalog.Targeting targeting,
            int mp, int action, int gfx, int range, int area,
            int baseDamage, int diceCount, int dieSides, int attribute) {
        assertEquals(chance, actual.triggerChance());
        assertEquals(triggerRange, actual.triggerRange());
        assertEquals(leverage, actual.leverage());
        assertEquals(skillId, actual.skillId());
        assertEquals(delay, actual.actionDelayMillis());
        assertEquals(targeting, actual.targeting());
        assertEquals(mp, actual.mpCost());
        assertEquals(0, actual.reuseDelayMillis());
        assertEquals(action, actual.actionId());
        assertEquals(gfx, actual.effectGfx());
        assertEquals(range, actual.maximumRange());
        assertEquals(area, actual.areaRadius());
        assertEquals(baseDamage, actual.baseDamage());
        assertEquals(diceCount, actual.diceCount());
        assertEquals(dieSides, actual.dieSides());
        assertEquals(attribute, actual.attribute());
    }

    private static void assertPhysical(
            BattleTimeAttackMagicSkillCatalog.PhysicalDefinition actual,
            int chance, int action, int areaWidth, int areaHeight) {
        assertEquals(chance, actual.triggerChance());
        assertEquals(-3, actual.triggerRange());
        assertEquals(3, actual.attackRange());
        assertEquals(15, actual.leverage());
        assertEquals(action, actual.actionOverride());
        assertEquals(action, actual.effectiveActionId());
        assertEquals(areaWidth, actual.areaWidth());
        assertEquals(areaHeight, actual.areaHeight());
        assertTrue(actual.areaAttack());
    }
}
