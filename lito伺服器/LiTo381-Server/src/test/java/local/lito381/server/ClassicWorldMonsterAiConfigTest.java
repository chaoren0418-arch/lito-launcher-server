package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterAiConfigTest {
    @Test
    void productionDisablesClassicAiWhileDedicatedFixturesRetainIt()
            throws Exception {
        ServerConfig development = ServerConfig.load(
                Path.of("config/server.properties"));
        ServerConfig dedicated = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-monster-ai-selftest.properties"));
        ServerConfig previousAudit = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-tree-call-selftest.properties"));
        ServerConfig poison = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-monster-poison-selftest.properties"));
        ServerConfig magic = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-monster-magic-selftest.properties"));
        ServerConfig physical = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-monster-physical-selftest.properties"));
        ServerConfig tamingLifecycle = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-taming-lifecycle-selftest.properties"));
        ServerConfig tamingDeath = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-taming-death-selftest.properties"));
        ServerConfig turnUndead = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-turn-undead-selftest.properties"));
        ServerConfig manaDrain = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-mana-drain-selftest.properties"));
        ServerConfig blind = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-blind-selftest.properties"));
        ServerConfig weakness = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-weakness-selftest.properties"));
        ServerConfig silence = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-silence-selftest.properties"));
        ServerConfig decayPotion = ServerConfig.load(
                Path.of("src/test/resources/server-config/root/server.world-decay-potion-selftest.properties"));

        assertFalse(development.classicWorldMonsterAiEnabled());
        assertTrue(development.classicWorldMonsterAggressiveAiEnabled());
        assertFalse(development.worldTamingLifecycleFixtureEnabled());
        assertEquals(0, development.returnToNatureFixturePercentileRoll());
        assertEquals(0, development.turnUndeadFixtureDieRoll());
        assertEquals(0, development.turnUndeadFixturePercentileRoll());
        assertEquals(0, development.manaDrainFixtureDieRoll());
        assertEquals(0, development.manaDrainFixturePercentileRoll());
        assertEquals(0, development.manaDrainFixtureTransferRoll());
        assertFalse(development.weaknessSpellFixtureEnabled());
        assertEquals(0, development.weaknessFixtureDieRoll());
        assertEquals(0, development.weaknessFixturePercentileRoll());
        assertFalse(development.silenceSpellFixtureEnabled());
        assertEquals(0, development.silenceFixturePercentileRoll());
        assertFalse(development.decayPotionSpellFixtureEnabled());
        assertEquals(0, development.decayPotionFixtureDieRoll());
        assertEquals(0, development.decayPotionFixturePercentileRoll());
        assertEquals(100, development.classicWorldMonsterAiTickMs());
        assertTrue(dedicated.classicWorldMonsterAiEnabled());
        assertEquals(50, dedicated.classicWorldMonsterAiTickMs());
        assertFalse(previousAudit.classicWorldMonsterAiEnabled());
        assertTrue(poison.classicWorldMonsterAiEnabled());
        assertEquals(50, poison.classicWorldMonsterAiTickMs());
        assertEquals(1, poison.classicWorldMonsterPoisonFixtureRoll());
        assertEquals(0, development.classicWorldMonsterPoisonFixtureRoll());
        assertEquals(0, development.classicWorldMonsterPhysicalSkillFixtureBranch());
        assertEquals(0, development.classicWorldMonsterPhysicalSkillFixtureTriggerRoll());
        assertEquals(0, development.classicWorldMonsterPhysicalSkillFixtureLevelRoll());
        assertEquals(0, development.classicWorldMonsterPhysicalSkillFixtureCriticalRoll());
        assertEquals(0, development.classicWorldMonsterMagicFixtureBranch());
        assertEquals(0, development.classicWorldMonsterMagicFixtureTriggerRoll());
        assertEquals(0, development.classicWorldMonsterMagicFixtureDieRoll());
        assertEquals(-1, development.timeAttackDocumentedPatternFixtureIndex());
        assertEquals(-1, development.timeAttackDragonOutcomeFixtureIndex());
        assertFalse(development.timeAttackDragonCombatFixtureOnly());
        assertEquals(1,
                development.timeAttackDragonCombatFixtureMinimumTargets());
        assertEquals(-1, development.timeAttackMonsterActNoFixture());
        assertEquals(0, development.timeAttackMonsterMagicFixtureTriggerRoll());
        assertEquals(0, development.timeAttackMonsterPhysicalFixtureTriggerRoll());
        assertEquals(0, development.timeAttackMonsterCandidateFixtureSelector());
        assertEquals(-1, development.timeAttackMonsterPhysicalFixtureLevelRoll());
        assertEquals(-1, development.timeAttackMonsterPhysicalFixtureCriticalRoll());
        assertEquals(-1, development.timeAttackMonsterPhysicalFixtureAcRoll());
        ServerConfig timeAttack = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.pve-ai-selftest.properties"));
        assertEquals(1, timeAttack.timeAttackMonsterMagicFixtureTriggerRoll());
        assertEquals(100, timeAttack.timeAttackMonsterPhysicalFixtureTriggerRoll());
        assertEquals(0, timeAttack.timeAttackMonsterCandidateFixtureSelector());
        assertEquals(1, timeAttack.timeAttackMonsterPhysicalFixtureLevelRoll());
        assertEquals(99, timeAttack.timeAttackMonsterPhysicalFixtureCriticalRoll());
        assertEquals(0, timeAttack.timeAttackMonsterPhysicalFixtureAcRoll());
        assertEquals(10_000, timeAttack.timeAttackPlayerPhysicalDamageFixture());
        ServerConfig dedicatedTimeAttack = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-magic-selftest.properties"));
        assertEquals(1, dedicatedTimeAttack.timeAttackMonsterMagicFixtureTriggerRoll());
        assertEquals(1, dedicatedTimeAttack.timeAttackDocumentedPatternFixtureIndex());
        assertEquals(0, dedicatedTimeAttack.timeAttackDragonOutcomeFixtureIndex());
        assertEquals(10_000, dedicatedTimeAttack.timeAttackPlayerPhysicalDamageFixture());
        ServerConfig physicalTimeAttack = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-physical-selftest.properties"));
        assertEquals(1, physicalTimeAttack.battleAiDamage());
        assertEquals(100, physicalTimeAttack.timeAttackMonsterMagicFixtureTriggerRoll());
        assertEquals(1, physicalTimeAttack.timeAttackDocumentedPatternFixtureIndex());
        assertEquals(1, physicalTimeAttack.timeAttackMonsterPhysicalFixtureTriggerRoll());
        assertEquals(1, physicalTimeAttack.timeAttackMonsterPhysicalFixtureLevelRoll());
        assertEquals(99, physicalTimeAttack.timeAttackMonsterPhysicalFixtureCriticalRoll());
        assertEquals(0, physicalTimeAttack.timeAttackMonsterPhysicalFixtureAcRoll());
        ServerConfig summonTimeAttack = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-summon-selftest.properties"));
        assertEquals(1, summonTimeAttack.timeAttackDocumentedPatternFixtureIndex());
        assertEquals(1, summonTimeAttack.timeAttackMonsterMagicFixtureTriggerRoll());
        assertEquals(2, summonTimeAttack.timeAttackMonsterCandidateFixtureSelector());
        assertEquals(10_000, summonTimeAttack.timeAttackPlayerPhysicalDamageFixture());
        ServerConfig flowTimeAttack = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-flow-selftest.properties"));
        assertFalse(flowTimeAttack.battleAiEnabled());
        assertEquals(1, flowTimeAttack.timeAttackDocumentedPatternFixtureIndex());
        assertEquals(0, flowTimeAttack.timeAttackDragonOutcomeFixtureIndex());
        assertEquals(10_000, flowTimeAttack.timeAttackPlayerPhysicalDamageFixture());
        ServerConfig dragonTimeAttack = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-dragon-selftest.properties"));
        assertEquals(0, dragonTimeAttack.timeAttackDocumentedPatternFixtureIndex());
        assertEquals(1, dragonTimeAttack.timeAttackDragonOutcomeFixtureIndex());
        assertFalse(dragonTimeAttack.timeAttackDragonCombatFixtureOnly());
        assertEquals(1,
                dragonTimeAttack.timeAttackDragonCombatFixtureMinimumTargets());
        assertEquals(-1, dragonTimeAttack.timeAttackMonsterActNoFixture());
        ServerConfig dragonMagic = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-dragon-magic-selftest.properties"));
        assertTrue(dragonMagic.timeAttackDragonCombatFixtureOnly());
        assertEquals(2, dragonMagic.timeAttackDragonCombatFixtureMinimumTargets());
        assertEquals(0, dragonMagic.timeAttackMonsterActNoFixture());
        ServerConfig dragonPhysical = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-dragon-physical-selftest.properties"));
        assertTrue(dragonPhysical.timeAttackDragonCombatFixtureOnly());
        assertEquals(2, dragonPhysical.timeAttackDragonCombatFixtureMinimumTargets());
        assertEquals(5, dragonPhysical.timeAttackMonsterActNoFixture());
        ServerConfig dragonGround = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-dragon-ground-selftest.properties"));
        assertTrue(dragonGround.timeAttackDragonCombatFixtureOnly());
        assertEquals(3, dragonGround.timeAttackDragonOutcomeFixtureIndex());
        assertEquals(2, dragonGround.timeAttackDragonCombatFixtureMinimumTargets());
        assertEquals(2, dragonGround.timeAttackMonsterActNoFixture());
        ServerConfig dragonVisual = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.time-attack-dragon-visual-selftest.properties"));
        assertTrue(dragonVisual.timeAttackDragonCombatFixtureOnly());
        assertEquals(1, dragonVisual.timeAttackDragonOutcomeFixtureIndex());
        assertEquals(2, dragonVisual.timeAttackDragonCombatFixtureMinimumTargets());
        assertEquals(4, dragonVisual.timeAttackMonsterActNoFixture());
        assertEquals(50_000, dragonTimeAttack.timeAttackPlayerPhysicalDamageFixture());
        assertTrue(magic.classicWorldMonsterAiEnabled());
        assertEquals(1, magic.classicWorldMonsterMagicFixtureBranch());
        assertEquals(1, magic.classicWorldMonsterMagicFixtureTriggerRoll());
        assertEquals(1, magic.classicWorldMonsterMagicFixtureDieRoll());
        assertTrue(physical.classicWorldMonsterAiEnabled());
        assertEquals(1, physical.classicWorldMonsterPhysicalSkillFixtureBranch());
        assertEquals(1, physical.classicWorldMonsterPhysicalSkillFixtureTriggerRoll());
        assertEquals(1, physical.classicWorldMonsterPhysicalSkillFixtureLevelRoll());
        assertEquals(99, physical.classicWorldMonsterPhysicalSkillFixtureCriticalRoll());
        assertFalse(tamingLifecycle.classicWorldMonsterAiEnabled());
        assertTrue(tamingLifecycle.classicWorldMonsterAggressiveAiEnabled());
        assertTrue(tamingLifecycle.worldTamingLifecycleFixtureEnabled());
        assertEquals(1, tamingLifecycle.tamingMonsterFixturePercentileRoll());
        assertEquals(1, tamingLifecycle.returnToNatureFixturePercentileRoll());
        assertTrue(tamingDeath.classicWorldMonsterAiEnabled());
        assertFalse(tamingDeath.classicWorldMonsterAggressiveAiEnabled());
        assertTrue(tamingDeath.worldTamingLifecycleFixtureEnabled());
        assertFalse(turnUndead.classicWorldMonsterAiEnabled());
        assertEquals(7, turnUndead.turnUndeadFixtureDieRoll());
        assertEquals(1, turnUndead.turnUndeadFixturePercentileRoll());
        assertFalse(manaDrain.classicWorldMonsterAiEnabled());
        assertEquals(12, manaDrain.manaDrainFixtureDieRoll());
        assertEquals(1, manaDrain.manaDrainFixturePercentileRoll());
        assertEquals(10, manaDrain.manaDrainFixtureTransferRoll());
        assertFalse(blind.classicWorldMonsterAiEnabled());
        assertTrue(blind.darkBlindSpellFixtureEnabled());
        assertEquals(1, blind.darkBlindFixtureRoll());
        assertFalse(weakness.classicWorldMonsterAiEnabled());
        assertTrue(weakness.weaknessSpellFixtureEnabled());
        assertEquals(10, weakness.weaknessFixtureDieRoll());
        assertEquals(1, weakness.weaknessFixturePercentileRoll());
        assertFalse(silence.classicWorldMonsterAiEnabled());
        assertTrue(silence.silenceSpellFixtureEnabled());
        assertEquals(1, silence.silenceFixturePercentileRoll());
        assertFalse(decayPotion.classicWorldMonsterAiEnabled());
        assertTrue(decayPotion.decayPotionSpellFixtureEnabled());
        assertEquals(8, decayPotion.decayPotionFixtureDieRoll());
        assertEquals(1, decayPotion.decayPotionFixturePercentileRoll());
    }
}
