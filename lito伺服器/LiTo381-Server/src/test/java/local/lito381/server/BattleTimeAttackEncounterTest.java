package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleTimeAttackEncounterTest {
    @Test
    void cubeHarmonyDoesNotInventManaForZeroMpMonster() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot monster =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.ManaDrain drained = encounter.drainMana(
                monster.objectId(), monster.maximumMp(), 20);
        assertEquals(0, monster.maximumMp());
        assertEquals(0, drained.drained());
        assertEquals(0, encounter.recoverMana(monster.objectId(), 5));
    }

    @Test
    void ordinaryTimeAttackNpcOwnsCancelableHorrorOfDeathLease() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot monster =
                encounter.startAfterTowerDestroyed().get(0);
        long now = 1_000_000_000L;

        assertFalse(encounter.horrorOfDeath(monster.objectId(), now, 60));
        assertTrue(encounter.horrorOfDeathActive(monster.objectId(), now));
        assertTrue(encounter.horrorOfDeath(monster.objectId(), now + 1, 60));
        assertTrue(encounter.expireHorrorOfDeaths(
                now + TimeUnit.SECONDS.toNanos(59)).isEmpty());
        assertEquals(1, encounter.expireHorrorOfDeaths(
                now + TimeUnit.SECONDS.toNanos(60)).size());
        assertFalse(encounter.horrorOfDeathActive(
                monster.objectId(), now + TimeUnit.SECONDS.toNanos(60)));
        assertFalse(encounter.horrorOfDeath(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(61), 60));
        BattleTimeAttackEncounter.MagicCancellation cancelled =
                encounter.cancelMagicEffects(monster.objectId());
        assertTrue(cancelled.horrorOfDeath());
        assertFalse(encounter.horrorOfDeathActive(monster.objectId(), now + 2));
    }

    @Test
    void preservesEvidencedFukuokaFinalBossPairWithoutInventingNormalWaves() {
        List<BattleTimeAttackEncounter.Template> bosses =
                BattleTimeAttackEncounter.finalBosses();
        assertEquals(List.of(45584, 45610),
                bosses.stream().map(BattleTimeAttackEncounter.Template::npcId).toList());
        assertEquals(List.of(2402, 2421),
                bosses.stream().map(BattleTimeAttackEncounter.Template::gfx).toList());
        assertEquals(List.of(6_500, 6_500),
                bosses.stream().map(BattleTimeAttackEncounter.Template::maxHp).toList());
        assertFalse(BattleTimeAttackEncounter.ordinaryWavePatternRecovered());
        assertTrue(BattleTimeAttackEncounter.documentedOrdinaryFragmentEnabled());
        assertEquals("unit-mechanics-fixture-not-production-pattern",
                BattleTimeAttackEncounter.ORDINARY_WAVE_SOURCE);
    }

    @Test
    void retainsTwoSeparateOfficialMinimumPatternsWithoutHybridizingThem() {
        List<BattleTimeAttackEncounter.DocumentedPattern> patterns =
                BattleTimeAttackEncounter.documentedPatterns();
        assertEquals(2, patterns.size());

        BattleTimeAttackEncounter.DocumentedPattern fukuoka = patterns.get(0);
        assertEquals("fukuoka-black-knight-chief", fukuoka.id());
        assertEquals(List.of(81321), fukuoka.ordinaryFragments().stream()
                .map(BattleTimeAttackEncounter.DocumentedOrdinaryFragment::npcId)
                .toList());
        assertEquals(List.of(45584, 45610), fukuoka.finalBosses().stream()
                .map(BattleTimeAttackEncounter.Template::npcId).toList());
        assertFalse(fukuoka.completePattern());

        BattleTimeAttackEncounter.DocumentedPattern tokyo = patterns.get(1);
        assertEquals("tokyo-owlbear", tokyo.id());
        assertEquals(List.of(45147, 45182, 45812),
                tokyo.ordinaryFragments().stream()
                        .map(BattleTimeAttackEncounter.DocumentedOrdinaryFragment::npcId)
                        .toList());
        assertEquals(List.of(45601, 45650), tokyo.finalBosses().stream()
                .map(BattleTimeAttackEncounter.Template::npcId).toList());
        assertFalse(tokyo.completePattern());
    }

    @Test
    void fixedOrderWholePatternSelectionKeepsUnrecoveredWeightsExplicit() {
        assertEquals(
                "4Gamer-Fukuoka-2005-fixed-order+organizer-unknown-until-start",
                BattleTimeAttackEncounter.PATTERN_SELECTION_MODEL_SOURCE);
        assertEquals("unrecovered",
                BattleTimeAttackEncounter.PATTERN_SELECTION_WEIGHTS_SOURCE);
        assertEquals("selected-LiTo-sub_46F9C0-map-metadata-only+no-wave-table",
                BattleTimeAttackEncounter.CLIENT_WAVE_TABLE_SOURCE);
        assertEquals("Lineage-Compendium-Aden-Arena-Deathmatch-not-LiTo-map540",
                BattleTimeAttackEncounter.EXCLUDED_ADEN_ARENA_DM_SOURCE);
        assertFalse(BattleTimeAttackEncounter.ordinaryWavePatternRecovered());
        assertFalse(BattleTimeAttackEncounter.patternSelectionWeightsRecovered());
    }

    @Test
    void eachProductionPatternRunsOnlyItsDocumentedActorsAndBossPair() {
        assertProductionPattern(0,
                List.of(81321), List.of(45584, 45610),
                BattleTimeAttackEncounter.FUKUOKA_PATTERN_SOURCE);
        assertProductionPattern(1,
                List.of(45147, 45182, 45812), List.of(45601, 45650),
                BattleTimeAttackEncounter.TOKYO_PATTERN_SOURCE);
    }

    @Test
    void classicDragonCandidatesArePinnedToSelectedSpritesAndSourceRows() {
        List<BattleTimeAttackEncounter.Template> dragons =
                BattleTimeAttackEncounter.dragonCandidates();
        assertEquals(List.of(45682, 45683, 45684), dragons.stream()
                .map(BattleTimeAttackEncounter.Template::npcId).toList());
        assertEquals(List.of(1245, 1590, 2001), dragons.stream()
                .map(BattleTimeAttackEncounter.Template::gfx).toList());
        assertEquals(List.of(33_500, 35_500, 38_800), dragons.stream()
                .map(BattleTimeAttackEncounter.Template::maxHp).toList());
        assertEquals("provisional-equal-none+antharas+fafurion+valakas",
                BattleTimeAttackEncounter.DRAGON_SELECTION_SOURCE);
    }

    @Test
    void documentedNoDragonOutcomeCompletesAfterBossPair() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        List<BattleTimeAttackEncounter.Snapshot> bosses =
                reachFinalBosses(encounter, 0);
        encounter.damage(bosses.get(0).objectId(), bosses.get(0).maxHp());
        BattleTimeAttackEncounter.Hit terminal = encounter.damage(
                bosses.get(1).objectId(), bosses.get(1).maxHp());
        assertTrue(terminal.finalBossesCleared());
        assertTrue(terminal.spawned().isEmpty());
        assertEquals(BattleTimeAttackEncounter.Phase.COMPLETE, encounter.phase());
    }

    @Test
    void eachDragonOutcomeSpawnsAfterBossPairAndOnlyDragonDeathCompletes() {
        for (int outcome = 1; outcome <= 3; outcome++) {
            BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
            List<BattleTimeAttackEncounter.Snapshot> bosses =
                    reachFinalBosses(encounter, outcome);
            encounter.damage(bosses.get(0).objectId(), bosses.get(0).maxHp());
            BattleTimeAttackEncounter.Hit bossClear = encounter.damage(
                    bosses.get(1).objectId(), bosses.get(1).maxHp());
            assertFalse(bossClear.finalBossesCleared());
            assertEquals(1, bossClear.actorsRemaining());
            BattleTimeAttackEncounter.Snapshot dragon = bossClear.spawned().get(0);
            assertEquals(BattleTimeAttackEncounter.dragonCandidates()
                    .get(outcome - 1).npcId(), dragon.npcId());
            assertEquals(BattleTimeAttackEncounter.DRAGON_IDENTITY_SOURCE,
                    dragon.identitySource());
            assertTrue(dragon.ordinaryWaveSource().contains(
                    BattleTimeAttackEncounter.DRAGON_STAGE_SOURCE));

            BattleTimeAttackEncounter.Hit terminal = encounter.damage(
                    dragon.objectId(), dragon.maxHp());
            assertTrue(terminal.finalBossesCleared());
            assertEquals(BattleTimeAttackEncounter.Phase.COMPLETE, encounter.phase());
        }
    }

    @Test
    void nonLeaderTowerDestroyerStartsEncounterButUsesConservativeNoDragonBranch() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540), 0, 1);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed(false).get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned();
        encounter.damage(bosses.get(0).objectId(), bosses.get(0).maxHp());
        BattleTimeAttackEncounter.Hit terminal = encounter.damage(
                bosses.get(1).objectId(), bosses.get(1).maxHp());

        assertTrue(terminal.finalBossesCleared());
        assertTrue(terminal.spawned().isEmpty());
        assertEquals(BattleTimeAttackEncounter.Phase.COMPLETE, encounter.phase());
        assertTrue(BattleTimeAttackEncounter.NON_LEADER_DRAGON_BOUNDARY_SOURCE
                .contains("conservative-no-dragon"));
    }

    @Test
    void rejectsDragonOutcomeOutsideExplicitNonePlusThreeCandidateDomain() {
        assertThrows(IllegalArgumentException.class, () ->
                new BattleTimeAttackEncounter().reset(
                        rules(), ArenaMapCatalog.forMap(540), 0, -1));
        assertThrows(IllegalArgumentException.class, () ->
                new BattleTimeAttackEncounter().reset(
                        rules(), ArenaMapCatalog.forMap(540), 0, 4));
    }

    @Test
    void antharasAiSelectsExactDamageVisualPhysicalAndInertActRows() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        BattleTimeAttackEncounter.Snapshot dragon = dragon(encounter, 1);
        assertEquals(45682, dragon.npcId());
        assertDragonMagic(encounter, dragon, 4,
                new int[] {1, 100, 100, 100, 100, 100, 100, 100}, 31001, 795);

        encounter = new BattleTimeAttackEncounter();
        dragon = dragon(encounter, 1);
        assertDragonMagic(encounter, dragon, 3,
                new int[] {100, 1, 100, 100, 100, 100, 100, 100}, 10027, 795);

        encounter = new BattleTimeAttackEncounter();
        dragon = dragon(encounter, 1);
        assertDragonMagic(encounter, dragon, 3,
                new int[] {100, 100, 1, 100, 100, 100, 100, 100}, 31004, 795);

        encounter = new BattleTimeAttackEncounter();
        dragon = dragon(encounter, 1);
        assertDragonMagic(encounter, dragon, 5,
                new int[] {100, 100, 100, 1, 100, 100, 100, 100}, 31002, 795);

        encounter = new BattleTimeAttackEncounter();
        dragon = dragon(encounter, 1);
        BattleTimeAttackEncounter.AiAttack visual = encounter.aiAttackFor(
                dragon.objectId(),
                dragon.x() + 4, dragon.y(), 0,
                new int[] {100, 100, 100, 100, 1, 100, 100, 100}, 0, 0);
        assertNotNull(visual);
        assertEquals(31003, visual.visualSkill().skillId());
        assertEquals(795, visual.actor().currentMp());

        encounter = new BattleTimeAttackEncounter();
        dragon = dragon(encounter, 1);
        BattleTimeAttackEncounter.AiAttack wide = encounter.aiAttackFor(
                dragon.objectId(), dragon.x() + 3, dragon.y(), 0,
                new int[] {100, 100, 100, 100, 100, 1, 100, 100}, 0, 0);
        assertNotNull(wide.physicalSkill());
        assertEquals(5, wide.physicalSkill().actNo());
        assertEquals(3, wide.physicalSkill().areaWidth());

        encounter = new BattleTimeAttackEncounter();
        dragon = dragon(encounter, 1);
        BattleTimeAttackEncounter.AiAttack line = encounter.aiAttackFor(
                dragon.objectId(), dragon.x() + 3, dragon.y(), 0,
                new int[] {100, 100, 100, 100, 100, 100, 1, 100}, 0, 0);
        assertNotNull(line.physicalSkill());
        assertEquals(6, line.physicalSkill().actNo());
        assertEquals(0, line.physicalSkill().areaWidth());

        encounter = new BattleTimeAttackEncounter();
        dragon = dragon(encounter, 1);
        assertNull(encounter.aiAttackFor(dragon.objectId(),
                dragon.x() + 8, dragon.y(), 0,
                new int[] {100, 100, 100, 100, 100, 100, 100, 1}, 0, 0));
    }

    @Test
    void fafurionAndValakasAiUseExactSelectedRowsAndMpCosts() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        BattleTimeAttackEncounter.Snapshot fafurion = dragon(encounter, 2);
        assertEquals(45683, fafurion.npcId());
        assertDragonMagic(encounter, fafurion, 4,
                new int[] {1, 100, 100, 100, 100}, 33001, 4_995);

        encounter = new BattleTimeAttackEncounter();
        fafurion = dragon(encounter, 2);
        assertDragonMagic(encounter, fafurion, 5,
                new int[] {100, 100, 1, 100, 100}, 33002, 4_995);

        encounter = new BattleTimeAttackEncounter();
        BattleTimeAttackEncounter.Snapshot valakas = dragon(encounter, 3);
        assertEquals(45684, valakas.npcId());
        assertDragonMagic(encounter, valakas, 4,
                new int[] {1, 100, 100, 100, 100, 100, 100, 100}, 31001, 4_495);

        encounter = new BattleTimeAttackEncounter();
        valakas = dragon(encounter, 3);
        BattleTimeAttackEncounter.AiAttack firePrison = encounter.aiAttackFor(
                valakas.objectId(),
                valakas.x() + 3, valakas.y(), 0,
                new int[] {100, 100, 1, 100, 100, 100, 100, 100}, 0, 0);
        assertNotNull(firePrison);
        assertFalse(firePrison.magic());
        assertEquals(30004, firePrison.groundSkill().skillId());
        assertEquals(4_495, firePrison.actor().currentMp());

        encounter = new BattleTimeAttackEncounter();
        valakas = dragon(encounter, 3);
        assertDragonMagic(encounter, valakas, 5,
                new int[] {100, 100, 100, 1, 100, 100, 100, 100}, 30002, 4_495);

        encounter = new BattleTimeAttackEncounter();
        valakas = dragon(encounter, 3);
        assertDragonMagic(encounter, valakas, 4,
                new int[] {100, 100, 100, 100, 1, 100, 100, 100}, 30003, 4_495);

        encounter = new BattleTimeAttackEncounter();
        valakas = dragon(encounter, 3);
        long inertValakasId = valakas.objectId();
        assertNull(encounter.aiAttackFor(valakas.objectId(),
                valakas.x() + 8, valakas.y(), 0,
                new int[] {100, 100, 100, 100, 100, 100, 100, 1}, 0, 0));
        assertEquals(4_500, encounter.snapshots().stream()
                .filter(actor -> actor.objectId() == inertValakasId)
                .findFirst().orElseThrow().currentMp());
    }

    @Test
    void dragonActNoFixtureSelectsOnlyRequestedExecutableSourceRow() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        BattleTimeAttackEncounter.Snapshot antharas = dragon(encounter, 1);
        BattleTimeAttackEncounter.AiAttack magic = encounter.aiAttackForActNoFixture(
                antharas.objectId(), antharas.x() + 4, antharas.y(),
                0, 0, 0);
        assertNotNull(magic);
        assertEquals(31001, magic.magicSkill().skillId());

        encounter = new BattleTimeAttackEncounter();
        antharas = dragon(encounter, 1);
        BattleTimeAttackEncounter.AiAttack physical =
                encounter.aiAttackForActNoFixture(
                        antharas.objectId(), antharas.x() + 3, antharas.y(),
                        0, 5, 0);
        assertNotNull(physical);
        assertEquals(5, physical.physicalSkill().actNo());

        encounter = new BattleTimeAttackEncounter();
        antharas = dragon(encounter, 1);
        BattleTimeAttackEncounter.AiAttack visual =
                encounter.aiAttackForActNoFixture(
                antharas.objectId(), antharas.x() + 4, antharas.y(),
                0, 4, 0);
        assertNotNull(visual);
        assertTrue(visual.magic());
        assertNull(visual.magicSkill());
        assertEquals(31003, visual.visualSkill().skillId());
        assertEquals(18, visual.visualSkill().actionId());
        assertEquals(2510, visual.visualSkill().effectGfx());
        assertEquals(795, visual.actor().currentMp());

        encounter = new BattleTimeAttackEncounter();
        BattleTimeAttackEncounter.Snapshot valakas = dragon(encounter, 3);
        BattleTimeAttackEncounter.AiAttack ground =
                encounter.aiAttackForActNoFixture(
                        valakas.objectId(), valakas.x() + 3, valakas.y(),
                        0, 2, 0);
        assertNotNull(ground);
        assertEquals(30004, ground.groundSkill().skillId());
        assertEquals(18, ground.groundSkill().actionId());
    }

    @Test
    void recordsTokyoIceGolemThenSeaDancerAsIncompleteEnabledFragments() {
        List<BattleTimeAttackEncounter.DocumentedOrdinaryFragment> fragments =
                BattleTimeAttackEncounter.documentedOrdinaryFragments();
        assertEquals(2, fragments.size());
        BattleTimeAttackEncounter.DocumentedOrdinaryFragment iceGolem =
                fragments.get(0);
        assertEquals(45182, iceGolem.npcId());
        assertEquals(1525, iceGolem.gfx());
        assertEquals("$1395", iceGolem.name());
        assertEquals(18, iceGolem.level());
        assertEquals(220, iceGolem.maxHp());
        assertEquals(373, iceGolem.observedSecondsRemaining());
        assertEquals(List.of(0, 1, 30), iceGolem.nativeTimedActions());
        assertEquals(BattleTimeAttackEncounter.TOKYO_ICE_GOLEM_FRAGMENT_SOURCE,
                iceGolem.source());
        assertFalse(iceGolem.completePattern());
        assertTrue(BattleAnimationCatalog.get().supportsTimedAction(
                iceGolem.gfx(), BattleActionCodes.ATTACK));

        BattleTimeAttackEncounter.DocumentedOrdinaryFragment seaDancer =
                fragments.get(1);
        assertEquals(45812, seaDancer.npcId());
        assertEquals(1603, seaDancer.gfx());
        assertEquals("$1429", seaDancer.name());
        assertEquals(27, seaDancer.level());
        assertEquals(210, seaDancer.maxHp());
        assertEquals(166, seaDancer.observedSecondsRemaining());
        assertEquals(List.of(0, 18, 19), seaDancer.nativeTimedActions());
        assertEquals(BattleTimeAttackEncounter.TOKYO_ORDINARY_FRAGMENT_SOURCE,
                seaDancer.source());
        assertFalse(seaDancer.completePattern());
        assertFalse(BattleAnimationCatalog.get().supportsTimedAction(
                seaDancer.gfx(), BattleActionCodes.ATTACK));
        assertFalse(BattleTimeAttackEncounter.ordinaryWavePatternRecovered());
        assertTrue(BattleTimeAttackEncounter.documentedOrdinaryFragmentEnabled());
    }

    @Test
    void waitsForStartTowerThenSpawnsOnlyDocumentedOrdinaryFragment() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));

        assertEquals(BattleTimeAttackEncounter.Phase.WAITING_FOR_START_TOWER,
                encounter.phase());
        assertTrue(encounter.snapshots().isEmpty());
        List<BattleTimeAttackEncounter.Snapshot> spawned =
                encounter.startAfterTowerDestroyed();
        assertEquals(1, spawned.size());
        assertEquals(List.of("$1395"),
                spawned.stream().map(BattleTimeAttackEncounter.Snapshot::name).toList());
        assertFalse(spawned.get(0).finalBoss());
        assertTrue(spawned.get(0).activeAi());
        assertEquals(BattleTimeAttackEncounter.Phase.ORDINARY_DOCUMENTED_FRAGMENT,
                encounter.phase());
        assertTrue(encounter.startAfterTowerDestroyed().isEmpty());
        assertTrue(encounter.canAttack(spawned.get(0).objectId()));
        assertTrue(encounter.blocks(spawned.get(0).x(), spawned.get(0).y()));
    }

    @Test
    void nativeHealingRestoresLivingTimeAttackActorAndCapsAtMaxHp() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor = encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Hit hit = encounter.damage(actor.objectId(), 30);
        assertEquals(actor.maxHp() - 30, hit.damage().after().hp());

        BattleTimeAttackEncounter.Healing healing = encounter.heal(actor.objectId(), 50);
        assertEquals(actor.maxHp(), healing.after().hp());
        assertEquals(30, healing.restoredHp());
        assertEquals(0, encounter.heal(actor.objectId(), 0).restoredHp());
    }

    @Test void elementalFallDownAppliesToOrdinaryActorAndCancellationRestores() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        long now = 2_000_000_000L;
        assertTrue(encounter.elementalFallDown(
                actor.objectId(), now, 32, 4));
        assertEquals(-50, encounter.elementalFallDownModifier(
                actor.objectId(), 4, now));
        BattleTimeAttackEncounter.MagicCancellation cancelled =
                encounter.cancelMagicEffects(actor.objectId());
        assertTrue(cancelled.elementalFallDown().present());
        assertEquals(4, cancelled.elementalFallDown().attributeKind());
        assertEquals(0, encounter.elementalFallDownModifier(
                actor.objectId(), 4, now));
    }

    @Test void eraseMagicQuartersMrConsumesAndFinalBossIsImmune() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        long now = 2_500_000_000L;
        assertTrue(encounter.eraseMagic(actor.objectId(), now, 32));
        assertEquals(25, encounter.effectiveMagicResistance(
                actor.objectId(), 103, now));
        assertTrue(encounter.consumeEraseMagic(actor.objectId(), now + 1));
        assertEquals(103, encounter.effectiveMagicResistance(
                actor.objectId(), 103, now + 2));

        BattleTimeAttackEncounter.Snapshot next = encounter.damage(
                actor.objectId(), actor.maxHp()).spawned().get(0);
        while (!next.finalBoss()) {
            next = encounter.damage(next.objectId(), next.maxHp()).spawned().get(0);
        }
        assertFalse(encounter.eraseMagic(next.objectId(), now, 32));
    }

    @Test
    void weaponBreakStacksPersistOnLivingActorAndResetOnDeath() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        assertEquals(1, encounter.addWeaponBreakStack(actor.objectId()));
        assertEquals(2, encounter.addWeaponBreakStack(actor.objectId()));
        assertEquals(2, encounter.weaponBreakStacks(actor.objectId()));
        encounter.damage(actor.objectId(), actor.maxHp());
        assertEquals(0, encounter.weaponBreakStacks(actor.objectId()));
    }

    @Test
    void manaDrainUsesSelectedSourceMpAndPersistsPerTimeAttackActor() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Snapshot actor = encounter.damage(
                ice.objectId(), ice.maxHp()).spawned().get(0);
        assertEquals(500, actor.maximumMp());

        BattleTimeAttackEncounter.ManaDrain first =
                encounter.drainMana(actor.objectId(), 500, 5);
        assertEquals(500, first.maximumMp());
        assertEquals(500, first.beforeMp());
        assertEquals(495, first.afterMp());
        assertEquals(5, first.drained());
        BattleTimeAttackEncounter.ManaDrain second =
                encounter.drainMana(actor.objectId(), 500, 10);
        assertEquals(10, second.drained());
        assertEquals(485, second.afterMp());
        assertNull(encounter.drainMana(1, 500, 1));
    }

    @Test
    void mindBreakStrictManaDebitDoesNotDebitAnEqualBalance() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Snapshot actor = encounter.damage(
                ice.objectId(), ice.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.ManaDrain first =
                encounter.drainManaIfStrictlyAbove(actor.objectId(), 500, 50);
        assertEquals(500, first.beforeMp());
        assertEquals(450, first.afterMp());
        assertEquals(50, first.drained());
        BattleTimeAttackEncounter.ManaDrain equal =
                encounter.drainManaIfStrictlyAbove(actor.objectId(), 500, 450);
        assertEquals(450, equal.beforeMp());
        assertEquals(450, equal.afterMp());
        assertEquals(0, equal.drained());
    }

    @Test
    void ordinaryFragmentsGateBossesAndOnlyLastBossCompletesEncounter() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot iceGolem =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Hit iceHit = encounter.damage(
                iceGolem.objectId(), iceGolem.maxHp());
        assertTrue(iceHit.damage().diedNow());
        assertFalse(iceHit.finalBossesCleared());
        assertEquals(1, iceHit.actorsRemaining());
        assertEquals(List.of("$1429"), iceHit.spawned().stream()
                .map(BattleTimeAttackEncounter.Snapshot::name).toList());
        assertTrue(iceHit.spawned().stream()
                .allMatch(BattleTimeAttackEncounter.Snapshot::activeAi));

        BattleTimeAttackEncounter.Snapshot seaDancer = iceHit.spawned().get(0);
        BattleTimeAttackEncounter.Hit ordinaryHit = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp());
        assertTrue(ordinaryHit.damage().diedNow());
        assertFalse(ordinaryHit.finalBossesCleared());
        assertEquals(2, ordinaryHit.actorsRemaining());
        assertEquals(List.of("$2106", "$2109"), ordinaryHit.spawned().stream()
                .map(BattleTimeAttackEncounter.Snapshot::name).toList());
        assertTrue(ordinaryHit.spawned().stream()
                .allMatch(BattleTimeAttackEncounter.Snapshot::finalBoss));
        List<BattleTimeAttackEncounter.Snapshot> bosses = ordinaryHit.spawned();

        BattleTimeAttackEncounter.Hit first = encounter.damage(
                bosses.get(0).objectId(), bosses.get(0).maxHp());
        assertTrue(first.damage().diedNow());
        assertFalse(first.finalBossesCleared());
        assertEquals(1, first.actorsRemaining());
        assertTrue(first.spawned().isEmpty());
        assertNull(encounter.find(bosses.get(0).objectId()));

        BattleTimeAttackEncounter.Hit second = encounter.damage(
                bosses.get(1).objectId(), bosses.get(1).maxHp());
        assertTrue(second.finalBossesCleared());
        assertEquals(0, second.actorsRemaining());
        assertEquals(BattleTimeAttackEncounter.Phase.COMPLETE, encounter.phase());
        assertTrue(encounter.snapshots().isEmpty());
    }

    @Test
    void captureAdvancesDocumentedFragmentsButNeverCapturesFinalBoss() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);

        BattleTimeAttackEncounter.Capture iceCapture = encounter.capture(ice.objectId());
        assertEquals(ice, iceCapture.captured());
        assertEquals(List.of(45812), iceCapture.spawned().stream()
                .map(BattleTimeAttackEncounter.Snapshot::npcId).toList());
        BattleTimeAttackEncounter.Capture seaCapture = encounter.capture(
                iceCapture.spawned().get(0).objectId());
        assertEquals(List.of(45584, 45610), seaCapture.spawned().stream()
                .map(BattleTimeAttackEncounter.Snapshot::npcId).toList());
        assertNull(encounter.capture(seaCapture.spawned().get(0).objectId()));
        assertEquals(BattleTimeAttackEncounter.Phase.FINAL_BOSSES, encounter.phase());
    }

    @Test
    void deadOrdinaryActorRemainsAConsumableCorpseAcrossFragmentAdvance() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);

        BattleTimeAttackEncounter.Hit hit = encounter.damage(
                ice.objectId(), ice.maxHp());
        assertTrue(hit.damage().diedNow());
        assertEquals(45812, hit.spawned().get(0).npcId());
        long now = System.nanoTime();
        BattleTimeAttackEncounter.Snapshot corpse =
                encounter.findCorpse(ice.objectId(), now);
        assertEquals(ice.objectId(), corpse.objectId());
        assertFalse(corpse.alive());
        assertEquals(corpse, encounter.consumeCorpse(ice.objectId(), now));
        assertNull(encounter.consumeCorpse(ice.objectId(), now));
    }

    @Test
    void returnToNatureLiberatesTamedTimeAttackActorAsFreshWildObject() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot source =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Capture capture = encounter.capture(source.objectId());
        assertNotNull(capture);
        BattleTimeAttackEncounter.Snapshot captured = capture.captured();
        BattleCompanion tamed = BattleCompanion.tamed(
                0x6810_1145L, captured.npcId(), captured.gfx(), captured.name(),
                captured.level(), Math.max(1, captured.hp() - 1), captured.maxHp(),
                captured.maximumMp(), 0, 12, 12,
                captured.x() + 1, captured.y());
        assertTrue(tamed.receiveItem(InventoryCatalog.require(
                InventoryCatalog.EXTRA_HEALING_POTION), 3));

        BattleTimeAttackEncounter.Snapshot released = encounter.releaseTamed(
                captured, tamed.snapshot(), tamed.carriedItems());
        assertNotNull(released);
        assertTrue(released.alive());
        assertFalse(released.finalBoss());
        assertEquals(captured.npcId(), released.npcId());
        assertEquals(tamed.snapshot().x(), released.x());
        assertEquals(tamed.snapshot().y(), released.y());
        assertEquals(tamed.snapshot().hp(), released.hp());
        assertEquals(tamed.snapshot().mp(), released.currentMp());
        assertTrue(released.objectId() != captured.objectId());
        assertTrue(released.objectId() != tamed.snapshot().objectId());
        assertEquals(released, encounter.find(released.objectId()));
        assertEquals(3, encounter.releasedInventory(released.objectId())
                .catalogCounts().getOrDefault(
                        InventoryCatalog.EXTRA_HEALING_POTION, 0));
    }

    @Test
    void resurrectionRestoresEligibleTimeAttackCorpseAtQuarterHp() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);
        encounter.damage(ice.objectId(), ice.maxHp());

        BattleTimeAttackEncounter.Resurrection result = encounter.resurrectCorpse(
                ice.objectId(), System.nanoTime());
        assertNotNull(result);
        assertFalse(result.before().alive());
        assertEquals(ice.objectId(), result.after().objectId());
        assertEquals(ice.maxHp() / 4, result.after().hp());
        assertTrue(result.after().alive());
        assertEquals(ice.objectId(), encounter.find(ice.objectId()).objectId());
        assertNull(encounter.findCorpse(ice.objectId(), System.nanoTime()));
    }

    @Test
    void callOfNatureRestoresEligibleTimeAttackCorpseAtFullHp() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);
        encounter.damage(ice.objectId(), ice.maxHp());

        BattleTimeAttackEncounter.Resurrection result = encounter.resurrectCorpse(
                ice.objectId(), System.nanoTime(), 1);
        assertNotNull(result);
        assertFalse(result.before().alive());
        assertTrue(result.after().alive());
        assertEquals(ice.maxHp(), result.after().hp());
        assertEquals(ice.maxHp(), result.after().maxHp());
    }

    @Test
    void timeAttackBossCantResurrectSourceFlagIsEnforced() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice = encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Snapshot sea = encounter.damage(
                ice.objectId(), ice.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                sea.objectId(), sea.maxHp()).spawned();
        BattleTimeAttackEncounter.Snapshot first = bosses.get(0);
        encounter.damage(first.objectId(), first.maxHp());
        assertNull(encounter.resurrectCorpse(first.objectId(), System.nanoTime()));
    }

    @Test
    void timeAttackCorpseExpiresAtTheSelectedThreeSecondDeletionBoundary() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);
        encounter.damage(ice.objectId(), ice.maxHp());
        assertNull(encounter.findCorpse(ice.objectId(), Long.MAX_VALUE));
    }

    @Test
    void nativePutObjectCarriesBossIdentityAndFullHp() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot iceGolem =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                iceGolem.objectId(), iceGolem.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);

        Protocol.Reader packet = new Protocol.Reader(Protocol.timeAttackActor(boss));
        assertEquals(Protocol.S_PUT_OBJECT, packet.c());
        assertEquals(boss.x(), packet.h());
        assertEquals(boss.y(), packet.h());
        assertEquals(boss.objectId(), packet.d());
        assertEquals(boss.gfx(), packet.h());
        assertEquals(BattleActionCodes.WALK, packet.c());
        packet.c();
        packet.c();
        assertEquals(BattleTimedStatus.NORMAL, packet.c());
        packet.d();
        packet.h();
        assertEquals(boss.name(), packet.s());
        assertEquals("", packet.s());
        assertEquals(0, packet.c());
        packet.d();
        packet.s();
        packet.s();
        packet.c();
        assertEquals(100, packet.c());
        packet.c();
        assertEquals(boss.level(), packet.c());
    }

    @Test
    void nativePutObjectCarriesSpellCastingOrdinaryFragmentWithoutMeleeAction() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot iceGolem =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Snapshot ordinary = encounter.damage(
                iceGolem.objectId(), iceGolem.maxHp()).spawned().get(0);

        assertFalse(ordinary.finalBoss());
        assertTrue(ordinary.activeAi());
        assertEquals(500, ordinary.currentMp());
        assertEquals(18, ordinary.intelligence());
        assertEquals(BattleTimeAttackEncounter.TOKYO_ORDINARY_FRAGMENT_SOURCE,
                ordinary.identitySource());
        Protocol.Reader packet = new Protocol.Reader(Protocol.timeAttackActor(ordinary));
        assertEquals(Protocol.S_PUT_OBJECT, packet.c());
        assertEquals(ordinary.x(), packet.h());
        assertEquals(ordinary.y(), packet.h());
        assertEquals(ordinary.objectId(), packet.d());
        assertEquals(1603, packet.h());
        assertEquals(BattleActionCodes.WALK, packet.c());
        assertFalse(BattleAnimationCatalog.get().supportsTimedAction(
                ordinary.gfx(), BattleActionCodes.ATTACK));
    }

    @Test
    void iceGolemAndSelectedMobskillActorsUseNativeActions() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        assertNull(encounter.moveForAi(1, arena.featurePoint().x(),
                arena.featurePoint().y(), 0));

        BattleTimeAttackEncounter.Snapshot iceGolem =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point iceDestination = adjacentFloor(
                arena, iceGolem.x(), iceGolem.y());
        BattleTimeAttackEncounter.Snapshot movedIce = encounter.moveForAi(
                iceGolem.objectId(), iceDestination.x(), iceDestination.y(), 0);
        assertEquals(iceDestination.x(), movedIce.x());
        assertEquals(iceDestination.y(), movedIce.y());
        assertNull(encounter.attackForAi(iceGolem.objectId(), 0));
        assertEquals(iceGolem.objectId(), encounter.attackForAi(
                iceGolem.objectId(), TimeUnit.SECONDS.toNanos(10)).objectId());

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                iceGolem.objectId(), iceGolem.maxHp()).spawned().get(0);
        assertNull(encounter.moveForAi(seaDancer.objectId(), seaDancer.x(),
                seaDancer.y(), TimeUnit.SECONDS.toNanos(20)));
        assertNull(encounter.attackForAi(
                seaDancer.objectId(), TimeUnit.SECONDS.toNanos(20)));
        BattleTimeAttackEncounter.AiAttack seaMagic = encounter.aiAttackFor(
                seaDancer.objectId(), seaDancer.x() + 6, seaDancer.y(),
                TimeUnit.SECONDS.toNanos(20), 1, 1, 0);
        assertTrue(seaMagic.magic());
        assertEquals(BattleSpellCatalog.CONE_OF_COLD,
                seaMagic.magicSkill().skillId());
        assertEquals(482, seaMagic.actor().currentMp());
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, boss.x(), boss.y());
        BattleTimeAttackEncounter.Snapshot moved = encounter.moveForAi(
                boss.objectId(), destination.x(), destination.y(), 0);
        assertEquals(destination.x(), moved.x());
        assertEquals(destination.y(), moved.y());
        assertNull(encounter.attackForAi(boss.objectId(), 0));
        assertEquals(boss.objectId(), encounter.attackForAi(
                boss.objectId(), TimeUnit.SECONDS.toNanos(10)).objectId());
        assertNull(encounter.moveForAi(boss.objectId(),
                arena.minX() - 1, arena.minY() - 1,
                TimeUnit.SECONDS.toNanos(20)));
    }

    @Test
    void selectedBossMobskillsPreserveTriggerRangesAndMagicRows() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Snapshot sea = encounter.damage(
                ice.objectId(), ice.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                sea.objectId(), sea.maxHp()).spawned();

        BattleTimeAttackEncounter.Snapshot minotaur = bosses.get(0);
        BattleTimeAttackEncounter.AiAttack earthquake = encounter.aiAttackFor(
                minotaur.objectId(), minotaur.x() + 3, minotaur.y(),
                0, 1, 1, 0);
        assertTrue(earthquake.magic());
        assertEquals(BattleSpellCatalog.EARTHQUAKE,
                earthquake.magicSkill().skillId());
        assertEquals(160, earthquake.actor().currentMp());

        BattleTimeAttackEncounter.Snapshot ancient = bosses.get(1);
        BattleTimeAttackEncounter.AiAttack ancientEarthquake = encounter.aiAttackFor(
                ancient.objectId(), ancient.x() + 15, ancient.y(),
                0, 1, 1, 0);
        assertTrue(ancientEarthquake.magic());
        assertEquals(10019, ancientEarthquake.magicSkill().skillId());
        assertEquals(BattleTimeAttackMagicSkillCatalog.Targeting.CASTER_SCREEN,
                ancientEarthquake.magicSkill().targeting());
        assertEquals(345, ancientEarthquake.actor().currentMp());

        BattleTimeAttackEncounter encounter2 = new BattleTimeAttackEncounter();
        encounter2.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice2 =
                encounter2.startAfterTowerDestroyed().get(0);
        BattleTimeAttackEncounter.Snapshot sea2 = encounter2.damage(
                ice2.objectId(), ice2.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot ancient2 = encounter2.damage(
                sea2.objectId(), sea2.maxHp()).spawned().get(1);
        BattleTimeAttackEncounter.AiAttack physical = encounter2.aiAttackFor(
                ancient2.objectId(), ancient2.x() + 2, ancient2.y(),
                0, 100, 1, 0);
        assertFalse(physical.magic());
        assertEquals(1, physical.triggerRoll());
    }

    @Test
    void tokyoDeathKnightSelectsEachIndependentActNoAndConsumesExactMp() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        List<BattleTimeAttackEncounter.Snapshot> bosses = tokyoBosses(encounter);
        BattleTimeAttackEncounter.Snapshot deathKnight = bosses.get(0);
        assertEquals(45601, deathKnight.npcId());

        BattleTimeAttackEncounter.AiAttack vampire = encounter.aiAttackFor(
                deathKnight.objectId(), deathKnight.x() + 3, deathKnight.y(),
                0, new int[] {1, 100, 100, 100}, 0, 0);
        assertTrue(vampire.magic());
        assertEquals(28, vampire.magicSkill().skillId());
        assertTrue(vampire.magicSkill().drainsLife());
        assertEquals(238, vampire.actor().currentMp());

        BattleTimeAttackEncounter encounter2 = new BattleTimeAttackEncounter();
        deathKnight = tokyoBosses(encounter2).get(0);
        BattleTimeAttackEncounter.AiAttack earthquake = encounter2.aiAttackFor(
                deathKnight.objectId(), deathKnight.x() + 6, deathKnight.y(),
                0, new int[] {100, 1, 100, 100}, 0, 0);
        assertTrue(earthquake.magic());
        assertEquals(10056, earthquake.magicSkill().skillId());
        assertEquals(245, earthquake.actor().currentMp());

        BattleTimeAttackEncounter encounter3 = new BattleTimeAttackEncounter();
        deathKnight = tokyoBosses(encounter3).get(0);
        BattleTimeAttackEncounter.AiAttack physical = encounter3.aiAttackFor(
                deathKnight.objectId(), deathKnight.x() + 1, deathKnight.y(),
                0, new int[] {100, 100, 100, 1}, 0, 0);
        assertFalse(physical.magic());
        assertNotNull(physical.physicalSkill());
        assertEquals(3, physical.physicalSkill().actNo());
        assertEquals(20, physical.physicalSkill().leverage());
    }

    @Test
    void deathKnightSummonUsesOneToThreeSpartoiAndStopsAfterTwoUses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        BattleTimeAttackEncounter.Snapshot deathKnight =
                tokyoBosses(encounter).get(0);

        BattleTimeAttackEncounter.AiAttack first = encounter.aiAttackFor(
                deathKnight.objectId(), deathKnight.x() + 3, deathKnight.y(),
                0, new int[] {100, 100, 1, 100}, 0, 2);
        assertTrue(first.summon());
        assertEquals(3, first.spawned().size());
        assertEquals(19, first.summonSkill().actionId());
        assertEquals(761, first.summonSkill().effectGfx());
        assertTrue(first.spawned().stream().allMatch(spawned ->
                spawned.npcId() == 45161 && spawned.gfx() == 145
                        && spawned.level() == 16 && spawned.maxHp() == 120
                        && spawned.maximumMp() == 30 && spawned.activeAi()
                        && !spawned.finalBoss()
                        && BattleTimeAttackEncounter.DEATH_KNIGHT_SUMMON_SOURCE
                            .equals(spawned.identitySource())));

        BattleTimeAttackEncounter.AiAttack second = encounter.aiAttackFor(
                deathKnight.objectId(), deathKnight.x() + 3, deathKnight.y(),
                TimeUnit.SECONDS.toNanos(1),
                new int[] {100, 100, 1, 100}, 0, 0);
        assertTrue(second.summon());
        assertEquals(1, second.spawned().size());

        assertNull(encounter.aiAttackFor(
                deathKnight.objectId(), deathKnight.x() + 3, deathKnight.y(),
                TimeUnit.SECONDS.toNanos(2),
                new int[] {100, 100, 1, 100}, 0, 0));
    }

    @Test
    void zombieLordUsesExactWideMagicAndTwoTilePhysicalRows() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        BattleTimeAttackEncounter.Snapshot zombie = tokyoBosses(encounter).get(1);
        assertEquals(45650, zombie.npcId());

        BattleTimeAttackEncounter.AiAttack magic = encounter.aiAttackFor(
                zombie.objectId(), zombie.x() + 6, zombie.y(), 0,
                new int[] {1, 100}, 0, 0);
        assertTrue(magic.magic());
        assertEquals(10000, magic.magicSkill().skillId());
        assertEquals(95, magic.actor().currentMp());

        BattleTimeAttackEncounter encounter2 = new BattleTimeAttackEncounter();
        zombie = tokyoBosses(encounter2).get(1);
        BattleTimeAttackEncounter.AiAttack physical = encounter2.aiAttackFor(
                zombie.objectId(), zombie.x() + 2, zombie.y(), 0,
                new int[] {100, 1}, 0, 0);
        assertFalse(physical.magic());
        assertNotNull(physical.physicalSkill());
        assertEquals(2, physical.physicalSkill().attackRange());
        assertEquals(30, physical.physicalSkill().leverage());
    }

    @Test
    void hasteShortensTimeAttackNpcAuthoritativeMoveAndAttackIntervals() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, actor.x(), actor.y());
        int moveMillis = BattleActionInterval.npcMillis(
                actor.gfx(), BattleActionCodes.WALK, true);
        int attackMillis = BattleActionInterval.npcMillis(
                actor.gfx(), BattleActionCodes.ATTACK, true);

        assertTrue(encounter.haste(actor.objectId(), 0, 1_200));
        assertNotNull(encounter.moveForAi(
                actor.objectId(), destination.x(), destination.y(), 0));
        assertNull(encounter.attackForAi(
                actor.objectId(), TimeUnit.MILLISECONDS.toNanos(moveMillis) - 1));
        assertNotNull(encounter.attackForAi(
                actor.objectId(), TimeUnit.MILLISECONDS.toNanos(moveMillis)));
        long attackReady = TimeUnit.MILLISECONDS.toNanos(moveMillis + attackMillis);
        assertNull(encounter.attackForAi(actor.objectId(), attackReady - 1));
        assertNotNull(encounter.attackForAi(actor.objectId(), attackReady));
    }

    @Test
    void darknessSupportsThirtyTwoSecondLeaseAndRejectsFinalBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, actor.x(), actor.y());

        assertTrue(encounter.blind(actor.objectId(), 0, 32));
        assertTrue(encounter.blindActive(
                actor.objectId(), TimeUnit.SECONDS.toNanos(31)));
        assertNull(encounter.moveForAi(actor.objectId(), destination.x(), destination.y(),
                TimeUnit.SECONDS.toNanos(1)));
        assertEquals(actor.objectId(), encounter.attackForAi(
                actor.objectId(), TimeUnit.SECONDS.toNanos(1)).objectId());
        assertFalse(encounter.blindActive(
                actor.objectId(), TimeUnit.SECONDS.toNanos(32)));
        assertEquals(destination.x(), encounter.moveForAi(
                actor.objectId(), destination.x(), destination.y(),
                TimeUnit.SECONDS.toNanos(33)).x());

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                actor.objectId(), actor.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        assertFalse(encounter.blind(boss.objectId(),
                TimeUnit.SECONDS.toNanos(34), 32));
        assertFalse(encounter.blindActive(
                boss.objectId(), TimeUnit.SECONDS.toNanos(35)));
        assertTrue(encounter.weakness(
                boss.objectId(), TimeUnit.SECONDS.toNanos(35), 64));
        assertTrue(encounter.weaknessActive(
                boss.objectId(), TimeUnit.SECONDS.toNanos(36)));
        assertTrue(encounter.disease(
                boss.objectId(), TimeUnit.SECONDS.toNanos(35), 64));
        assertTrue(encounter.diseaseActive(
                boss.objectId(), TimeUnit.SECONDS.toNanos(36)));
    }

    @Test
    void sleepBlocksOrdinaryAiDamageWakesAndFinalBossesAreImmune() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, ordinary.x(), ordinary.y());

        assertTrue(encounter.sleep(ordinary.objectId(), 0, 32));
        assertTrue(encounter.sleepActive(ordinary.objectId(), 1));
        assertNull(encounter.moveForAi(
                ordinary.objectId(), destination.x(), destination.y(), 1));
        assertNull(encounter.attackForAi(ordinary.objectId(), 1));
        assertFalse(encounter.damage(ordinary.objectId(), 1).damage().diedNow());
        assertFalse(encounter.sleepActive(ordinary.objectId(), 2));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        assertFalse(encounter.sleep(boss.objectId(), 3, 32));
        assertFalse(encounter.sleepActive(boss.objectId(), 4));
    }

    @Test
    void freezeBlocksOrdinaryAiSurvivesDamageExpiresAndFinalBossesAreImmune() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, ordinary.x(), ordinary.y());
        long expiry = TimeUnit.SECONDS.toNanos(16);

        assertTrue(encounter.freeze(ordinary.objectId(), 0, 16));
        assertFalse(encounter.freeze(ordinary.objectId(), 1, 16));
        assertTrue(encounter.freezeActive(ordinary.objectId(), 1));
        assertNull(encounter.moveForAi(
                ordinary.objectId(), destination.x(), destination.y(), 1));
        assertNull(encounter.attackForAi(ordinary.objectId(), 1));
        assertFalse(encounter.damage(ordinary.objectId(), 1).damage().diedNow());
        assertTrue(encounter.freezeActive(ordinary.objectId(), 2));
        assertEquals(List.of(ordinary.objectId()), encounter.expireFreezes(expiry)
                .stream().map(BattleTimeAttackEncounter.Snapshot::objectId).toList());
        assertTrue(encounter.expireFreezes(expiry).isEmpty());
        assertEquals(destination.x(), encounter.moveForAi(
                ordinary.objectId(), destination.x(), destination.y(), expiry).x());

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        assertFalse(encounter.freeze(boss.objectId(), expiry, 16));
        assertFalse(encounter.freezeActive(boss.objectId(), expiry + 1));
    }

    @Test
    void curseParalysisImmediatelyBlocksOrdinaryAiAndFinalBossesAreImmune() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(
                arena, ordinary.x(), ordinary.y());
        long start = TimeUnit.SECONDS.toNanos(3);
        long expiry = start + TimeUnit.SECONDS.toNanos(16);

        assertTrue(encounter.paralysis(ordinary.objectId(), start, 16_000));
        assertFalse(encounter.paralysis(ordinary.objectId(), start + 1, 16_000));
        assertTrue(encounter.paralysisActive(ordinary.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                ordinary.objectId(), destination.x(), destination.y(), start + 1));
        assertNull(encounter.attackForAi(ordinary.objectId(), start + 1));
        assertFalse(encounter.damage(ordinary.objectId(), 1).damage().diedNow());
        assertTrue(encounter.cancelParalysis(ordinary.objectId()));
        assertFalse(encounter.cancelParalysis(ordinary.objectId()));
        assertFalse(encounter.paralysisActive(ordinary.objectId(), start + 2));
        assertEquals(ordinary.objectId(), encounter.attackForAi(
                ordinary.objectId(), start + 2).objectId());

        assertTrue(encounter.paralysis(ordinary.objectId(), start + 3, 16_000));
        assertEquals(List.of(ordinary.objectId()), encounter.expireParalyses(expiry + 3)
                .stream().map(BattleTimeAttackEncounter.Snapshot::objectId).toList());
        assertTrue(encounter.expireParalyses(expiry + 3).isEmpty());
        assertEquals(destination.x(), encounter.moveForAi(
                ordinary.objectId(), destination.x(), destination.y(), expiry + 3).x());

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        assertFalse(encounter.paralysis(boss.objectId(), expiry, 16_000));
        assertFalse(encounter.paralysisActive(boss.objectId(), expiry + 1));
    }

    @Test
    void earthBindBlocksOrdinaryAiAndDamageWhileFinalBossesAreImmune() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, ordinary.x(), ordinary.y());
        long start = System.nanoTime();
        long expiry = start + TimeUnit.SECONDS.toNanos(16);

        assertTrue(encounter.earthBind(ordinary.objectId(), start, 16));
        assertFalse(encounter.earthBind(ordinary.objectId(), start + 1, 16));
        assertTrue(encounter.earthBindActive(ordinary.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                ordinary.objectId(), destination.x(), destination.y(), start + 1));
        assertNull(encounter.attackForAi(ordinary.objectId(), start + 1));
        assertNull(encounter.damage(ordinary.objectId(), 1).damage());
        assertEquals(List.of(ordinary.objectId()), encounter.expireEarthBinds(expiry)
                .stream().map(BattleTimeAttackEncounter.Snapshot::objectId).toList());
        assertFalse(encounter.damage(ordinary.objectId(), 1).damage().diedNow());

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        assertFalse(encounter.earthBind(boss.objectId(), expiry, 16));
        assertFalse(encounter.earthBindActive(boss.objectId(), expiry + 1));
    }

    @Test
    void shockStunBlocksTimeAttackAiSurvivesCancellationAndAllowsFinalBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(
                arena, ordinary.x(), ordinary.y());
        long start = TimeUnit.SECONDS.toNanos(3);
        long expiry = start + TimeUnit.SECONDS.toNanos(4);

        assertTrue(encounter.shockStun(ordinary.objectId(), start, 4));
        assertTrue(encounter.shockStunActive(ordinary.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                ordinary.objectId(), destination.x(), destination.y(), start + 1));
        assertNull(encounter.attackForAi(ordinary.objectId(), start + 1));
        assertNotNull(encounter.cancelMagicEffects(ordinary.objectId()));
        assertTrue(encounter.shockStunActive(ordinary.objectId(), start + 2));
        assertFalse(encounter.shockStunActive(ordinary.objectId(), expiry));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        long bossStart = expiry + 1;
        assertTrue(encounter.shockStun(boss.objectId(), bossStart, 2));
        assertTrue(encounter.shockStunActive(boss.objectId(), bossStart + 1));
        assertNull(encounter.attackForAi(boss.objectId(), bossStart + 1));
        assertFalse(encounter.shockStunActive(
                boss.objectId(), bossStart + TimeUnit.SECONDS.toNanos(2)));
    }

    @Test
    void thunderGrabBlocksOrdinaryTimeActorIsCancelledAndRejectsFinalBoss() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long start = TimeUnit.SECONDS.toNanos(5);
        assertTrue(encounter.thunderGrab(ordinary.objectId(), start, 4));
        assertTrue(encounter.thunderGrabActive(ordinary.objectId(), start + 1));
        BattleTimeAttackEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(ordinary.objectId());
        assertNotNull(cancellation);
        assertTrue(cancellation.thunderGrab());
        assertFalse(encounter.thunderGrabActive(ordinary.objectId(), start + 2));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        assertFalse(encounter.thunderGrab(
                boss.objectId(), start + 3, 4));
        assertFalse(encounter.thunderGrabActive(
                boss.objectId(), start + 4));
    }

    @Test
    void boneBreakLocksOrdinaryAndFinalBossAndCancellationRemovesIt() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        encounter.reset(rules(), arena);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long start = TimeUnit.SECONDS.toNanos(7);
        assertTrue(encounter.boneBreak(ordinary.objectId(), start, 2));
        assertFalse(encounter.boneBreak(ordinary.objectId(), start + 1, 2));
        assertTrue(encounter.boneBreakActive(ordinary.objectId(), start + 1));
        BattleTimeAttackEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(ordinary.objectId());
        assertNotNull(cancellation);
        assertTrue(cancellation.boneBreak());
        assertFalse(encounter.boneBreakActive(ordinary.objectId(), start + 2));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        BattleTimeAttackEncounter.Snapshot boss = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned().get(0);
        assertTrue(boss.finalBoss());
        assertTrue(encounter.boneBreak(boss.objectId(), start + 3, 2),
                "retained Bone Break has no boss immunity gate in its TYPE_ATTACK mode");
        assertTrue(encounter.boneBreakActive(boss.objectId(), start + 4));
    }

    @Test
    void hasteRefreshesTimeAttackActorAndPublishesOneExactExpiry() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long start = TimeUnit.SECONDS.toNanos(3);
        long refresh = start + TimeUnit.SECONDS.toNanos(100);
        long firstExpiry = start + TimeUnit.SECONDS.toNanos(1_200);
        long refreshedExpiry = refresh + TimeUnit.SECONDS.toNanos(1_200);

        assertTrue(encounter.haste(ordinary.objectId(), start, 1_200));
        assertTrue(encounter.hasteActive(ordinary.objectId(), start + 1));
        assertTrue(encounter.haste(ordinary.objectId(), refresh, 1_200));
        assertTrue(encounter.expireHastes(firstExpiry).isEmpty());
        assertEquals(List.of(ordinary.objectId()), encounter.expireHastes(refreshedExpiry)
                .stream().map(BattleTimeAttackEncounter.Snapshot::objectId).toList());
        assertFalse(encounter.hasteActive(ordinary.objectId(), refreshedExpiry));
        assertTrue(encounter.expireHastes(refreshedExpiry).isEmpty());
    }

    @Test
    void massSlowNeutralizesHasteAndPublishesOneTimeAttackExpiry() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        long start = TimeUnit.SECONDS.toNanos(3);

        assertTrue(encounter.haste(actor.objectId(), start, 1_200));
        assertTrue(encounter.slow(actor.objectId(), start + 1, 64));
        assertFalse(encounter.hasteActive(actor.objectId(), start + 1));
        assertFalse(encounter.slowActive(actor.objectId(), start + 1));
        assertTrue(encounter.slow(actor.objectId(), start + 2, 64));
        assertTrue(encounter.slowActive(actor.objectId(), start + 2));
        long expiry = start + 2 + TimeUnit.SECONDS.toNanos(64);
        assertTrue(encounter.expireSlows(expiry - 1).isEmpty());
        assertEquals(List.of(actor.objectId()), encounter.expireSlows(expiry)
                .stream().map(BattleTimeAttackEncounter.Snapshot::objectId).toList());
        assertFalse(encounter.slowActive(actor.objectId(), expiry));
        assertTrue(encounter.expireSlows(expiry).isEmpty());

        assertTrue(encounter.slow(actor.objectId(), expiry + 1, 64));
        BattleTimeAttackEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(actor.objectId());
        assertTrue(cancellation.slow());
        assertFalse(cancellation.haste());
        assertFalse(encounter.slowActive(actor.objectId(), expiry + 2));
    }

    @Test
    void cancellationClearsEveryImplementedTimeAttackNpcMagicEffect() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        long now = TimeUnit.SECONDS.toNanos(3);

        assertTrue(encounter.haste(actor.objectId(), now, 1_200));
        assertTrue(encounter.blind(actor.objectId(), now, 32));
        assertTrue(encounter.weakness(actor.objectId(), now, 64));
        assertTrue(encounter.disease(actor.objectId(), now, 64));
        assertTrue(encounter.silence(actor.objectId(), now, 16));
        assertTrue(encounter.decayPotion(actor.objectId(), now, 16));
        assertTrue(encounter.polluteWater(actor.objectId(), now, 192));
        assertTrue(encounter.sleep(actor.objectId(), now, 32));
        assertTrue(encounter.paralysis(actor.objectId(), now, 16_000));
        assertTrue(encounter.freeze(actor.objectId(), now, 16));
        assertTrue(encounter.earthBind(actor.objectId(), now, 16));
        assertTrue(encounter.windShackle(actor.objectId(), now, 16));
        assertFalse(encounter.windShackle(actor.objectId(), now + 1, 16));

        BattleTimeAttackEncounter.MagicCancellation result =
                encounter.cancelMagicEffects(actor.objectId());
        assertNotNull(result);
        assertEquals(actor.objectId(), result.target().objectId());
        assertTrue(result.haste());
        assertFalse(result.slow());
        assertTrue(result.blind());
        assertTrue(result.weakness());
        assertTrue(result.disease());
        assertTrue(result.silence());
        assertTrue(result.decayPotion());
        assertTrue(result.polluteWater());
        assertTrue(result.sleep());
        assertTrue(result.paralysis());
        assertTrue(result.freeze());
        assertTrue(result.earthBind());
        assertTrue(result.windShackle());
        assertFalse(encounter.hasteActive(actor.objectId(), now + 1));
        assertFalse(encounter.blindActive(actor.objectId(), now + 1));
        assertFalse(encounter.weaknessActive(actor.objectId(), now + 1));
        assertFalse(encounter.diseaseActive(actor.objectId(), now + 1));
        assertFalse(encounter.silenceActive(actor.objectId(), now + 1));
        assertFalse(encounter.decayPotionActive(actor.objectId(), now + 1));
        assertFalse(encounter.polluteWaterActive(actor.objectId(), now + 1));
        assertFalse(encounter.sleepActive(actor.objectId(), now + 1));
        assertFalse(encounter.paralysisActive(actor.objectId(), now + 1));
        assertFalse(encounter.freezeActive(actor.objectId(), now + 1));
        assertFalse(encounter.earthBindActive(actor.objectId(), now + 1));
        assertFalse(encounter.windShackleActive(actor.objectId(), now + 1));
    }

    @Test
    void timeAttackSilenceRejectsBossesAndIsConsumedByOrdinarySpellAttempt() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.silence(ordinary.objectId(), now, 16));
        assertTrue(encounter.consumeSilenceForSpell(ordinary.objectId(), now + 1));
        assertFalse(encounter.silenceActive(ordinary.objectId(), now + 1));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned();
        for (BattleTimeAttackEncounter.Snapshot boss : bosses) {
            assertTrue(boss.finalBoss());
            assertFalse(encounter.silence(boss.objectId(), now + 3, 16));
        }
    }

    @Test
    void timeAttackAreaSilencePersistsForOrdinaryActorsAndRejectsBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.areaSilence(ordinary.objectId(), now, 16));
        assertTrue(encounter.consumeSilenceForSpell(ordinary.objectId(), now + 1));
        assertTrue(encounter.consumeSilenceForSpell(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(15)));
        assertFalse(encounter.consumeSilenceForSpell(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(16)));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned();
        for (BattleTimeAttackEncounter.Snapshot boss : bosses) {
            assertTrue(boss.finalBoss());
            assertFalse(encounter.areaSilence(boss.objectId(), now + 20, 16));
        }
    }

    @Test
    void timeAttackWindShackleExpiresWithoutRefreshAndRejectsFinalBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.windShackle(ordinary.objectId(), now, 16));
        assertFalse(encounter.windShackle(
                ordinary.objectId(), now + TimeUnit.SECONDS.toNanos(8), 16));
        assertTrue(encounter.windShackleActive(
                ordinary.objectId(), now + TimeUnit.SECONDS.toNanos(15)));
        assertFalse(encounter.windShackleActive(
                ordinary.objectId(), now + TimeUnit.SECONDS.toNanos(16)));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned();
        for (BattleTimeAttackEncounter.Snapshot boss : bosses) {
            assertTrue(boss.finalBoss());
            assertFalse(encounter.windShackle(
                    boss.objectId(), now + TimeUnit.SECONDS.toNanos(20), 16));
        }
    }

    @Test
    void timeAttackDecayPotionDoesNotRefreshAndRejectsFinalBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.decayPotion(ordinary.objectId(), now, 16));
        assertTrue(encounter.decayPotion(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(5), 16));
        assertTrue(encounter.decayPotionActive(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(15)));
        assertFalse(encounter.decayPotionActive(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(16)));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned();
        for (BattleTimeAttackEncounter.Snapshot boss : bosses) {
            assertTrue(boss.finalBoss());
            assertFalse(encounter.decayPotion(boss.objectId(), now + 3, 16));
        }
    }

    @Test
    void timeAttackPolluteWaterRefreshesAndRejectsFinalBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.polluteWater(ordinary.objectId(), now, 192));
        assertTrue(encounter.polluteWater(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(100), 192));
        assertTrue(encounter.polluteWaterActive(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(291)));
        assertFalse(encounter.polluteWaterActive(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(292)));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned();
        for (BattleTimeAttackEncounter.Snapshot boss : bosses) {
            assertTrue(boss.finalBoss());
            assertFalse(encounter.polluteWater(boss.objectId(), now + 3, 192));
        }
    }

    @Test
    void timeAttackStrikerGaleRefreshesCancelsAndRejectsFinalBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.strikerGale(ordinary.objectId(), now, 192));
        assertTrue(encounter.strikerGale(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(100), 192));
        assertTrue(encounter.strikerGaleActive(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(291)));
        BattleTimeAttackEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(ordinary.objectId());
        assertNotNull(cancellation);
        assertTrue(cancellation.strikerGale());
        assertFalse(encounter.strikerGaleActive(ordinary.objectId(),
                now + TimeUnit.SECONDS.toNanos(291)));

        BattleTimeAttackEncounter.Snapshot seaDancer = encounter.damage(
                ordinary.objectId(), ordinary.maxHp()).spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned();
        for (BattleTimeAttackEncounter.Snapshot boss : bosses) {
            assertTrue(boss.finalBoss());
            assertFalse(encounter.strikerGale(boss.objectId(), now + 3, 192));
        }
    }

    @Test
    void retainedDamagePoisonTicksThroughEarthBindExpiresAndCanBeCured() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        long start = System.nanoTime();

        assertTrue(encounter.poison(actor.objectId(), 15L,
                start, 30, 3_000));
        assertFalse(encounter.poison(actor.objectId(), 16L,
                start + 1, 30, 3_000));
        assertTrue(encounter.earthBind(actor.objectId(), start + 1, 16));
        assertNull(encounter.damage(actor.objectId(), 1).damage());

        List<BattleTimeAttackEncounter.PoisonUpdate> firstTick =
                encounter.pollPoisons(
                        start + TimeUnit.SECONDS.toNanos(3), 3_000, 5);
        assertEquals(1, firstTick.size());
        assertEquals(15L, firstTick.get(0).attackerObjectId());
        assertEquals(actor.maxHp() - 5,
                firstTick.get(0).hits().get(0).damage().after().hp());
        assertTrue(encounter.poisonActive(actor.objectId(),
                start + TimeUnit.SECONDS.toNanos(29)));

        List<BattleTimeAttackEncounter.PoisonUpdate> expired =
                encounter.pollPoisons(
                        start + TimeUnit.SECONDS.toNanos(30), 3_000, 5);
        assertEquals(1, expired.size());
        assertTrue(expired.get(0).expired());
        assertTrue(expired.get(0).hits().isEmpty());
        assertFalse(encounter.poisonActive(actor.objectId(),
                start + TimeUnit.SECONDS.toNanos(30)));

        long secondStart = start + TimeUnit.SECONDS.toNanos(31);
        assertTrue(encounter.poison(actor.objectId(), 15L,
                secondStart, 30, 3_000));
        assertTrue(encounter.curePoison(actor.objectId()));
        assertFalse(encounter.curePoison(actor.objectId()));
        assertTrue(encounter.pollPoisons(
                secondStart + TimeUnit.SECONDS.toNanos(3), 3_000, 5).isEmpty());
    }

    @Test
    void retainedDamagePoisonKillAdvancesFragmentsAndAcceptsFinalBosses() {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot ice =
                encounter.startAfterTowerDestroyed().get(0);
        encounter.damage(ice.objectId(), ice.maxHp() - 5);
        long start = System.nanoTime();
        assertTrue(encounter.poison(ice.objectId(), 15L,
                start, 30, 3_000));

        BattleTimeAttackEncounter.PoisonUpdate update = encounter.pollPoisons(
                start + TimeUnit.SECONDS.toNanos(3), 3_000, 5).get(0);
        BattleTimeAttackEncounter.Hit lethal = update.hits().get(0);
        assertTrue(lethal.damage().diedNow());
        assertEquals(1, lethal.actorsRemaining());
        assertEquals(1, lethal.spawned().size());
        assertEquals(45812, lethal.spawned().get(0).npcId());

        BattleTimeAttackEncounter.Snapshot seaDancer = lethal.spawned().get(0);
        List<BattleTimeAttackEncounter.Snapshot> bosses = encounter.damage(
                seaDancer.objectId(), seaDancer.maxHp()).spawned();
        assertEquals(2, bosses.size());
        assertTrue(bosses.stream().allMatch(BattleTimeAttackEncounter.Snapshot::finalBoss));
        for (BattleTimeAttackEncounter.Snapshot boss : bosses) {
            assertTrue(encounter.poison(boss.objectId(), 15L,
                    start + TimeUnit.SECONDS.toNanos(4), 30, 3_000));
        }
    }

    private static ArenaMapCatalog.Point adjacentFloor(
            ArenaMapCatalog.Arena arena, int x, int y) {
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int heading = 0; heading < 8; heading++) {
            int candidateX = x + dx[heading];
            int candidateY = y + dy[heading];
            if (arena.nativeCanStep(x, y, candidateX, candidateY)) {
                return new ArenaMapCatalog.Point(candidateX, candidateY);
            }
        }
        throw new AssertionError("native safe anchor has no adjacent legal step");
    }

    private static List<BattleTimeAttackEncounter.Snapshot> tokyoBosses(
            BattleTimeAttackEncounter encounter) {
        encounter.reset(rules(), ArenaMapCatalog.forMap(540), 1);
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        for (int index = 0; index < 3; index++) {
            List<BattleTimeAttackEncounter.Snapshot> spawned = encounter.damage(
                    actor.objectId(), actor.maxHp()).spawned();
            if (spawned.get(0).finalBoss()) return spawned;
            actor = spawned.get(0);
        }
        throw new AssertionError("Tokyo boss stage was not reached");
    }

    private static List<BattleTimeAttackEncounter.Snapshot> reachFinalBosses(
            BattleTimeAttackEncounter encounter, int dragonOutcome) {
        encounter.reset(rules(), ArenaMapCatalog.forMap(540), 0, dragonOutcome);
        BattleTimeAttackEncounter.Snapshot ordinary =
                encounter.startAfterTowerDestroyed().get(0);
        return encounter.damage(ordinary.objectId(), ordinary.maxHp()).spawned();
    }

    private static BattleTimeAttackEncounter.Snapshot dragon(
            BattleTimeAttackEncounter encounter, int outcome) {
        List<BattleTimeAttackEncounter.Snapshot> bosses =
                reachFinalBosses(encounter, outcome);
        encounter.damage(bosses.get(0).objectId(), bosses.get(0).maxHp());
        return encounter.damage(bosses.get(1).objectId(), bosses.get(1).maxHp())
                .spawned().get(0);
    }

    private static void assertDragonMagic(
            BattleTimeAttackEncounter encounter,
            BattleTimeAttackEncounter.Snapshot dragon,
            int distance, int[] rolls, int skillId, int expectedMp) {
        BattleTimeAttackEncounter.AiAttack attack = encounter.aiAttackFor(
                dragon.objectId(), dragon.x() + distance, dragon.y(),
                0, rolls, 0, 0);
        assertNotNull(attack);
        assertTrue(attack.magic());
        assertEquals(skillId, attack.magicSkill().skillId());
        assertEquals(expectedMp, attack.actor().currentMp());
    }

    private static void assertProductionPattern(
            int patternIndex, List<Integer> ordinaryNpcIds,
            List<Integer> bossNpcIds, String waveSource) {
        BattleTimeAttackEncounter encounter = new BattleTimeAttackEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(540), patternIndex);
        assertEquals(BattleTimeAttackEncounter.documentedPatterns()
                .get(patternIndex).id(), encounter.selectedPatternId());

        List<Integer> actualOrdinary = new java.util.ArrayList<>();
        BattleTimeAttackEncounter.Snapshot actor =
                encounter.startAfterTowerDestroyed().get(0);
        List<BattleTimeAttackEncounter.Snapshot> spawned = List.of(actor);
        while (!spawned.get(0).finalBoss()) {
            actor = spawned.get(0);
            actualOrdinary.add(actor.npcId());
            assertEquals(waveSource, actor.ordinaryWaveSource());
            spawned = encounter.damage(actor.objectId(), actor.maxHp()).spawned();
        }
        assertEquals(ordinaryNpcIds, actualOrdinary);
        assertEquals(bossNpcIds, spawned.stream()
                .map(BattleTimeAttackEncounter.Snapshot::npcId).toList());
        assertTrue(spawned.stream().allMatch(snapshot ->
                waveSource.equals(snapshot.ordinaryWaveSource())));
    }

    private static BattleRules.RuleSet rules() {
        return BattleRules.decode(new GameHub.RoomSettings(
                "time", "", 60, BattleRules.Mode.TIME_ATTACK.wireId(),
                0, 0, 0, 0, List.of()));
    }
}
