package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterCatalogTest {
    @Test
    void cubeHarmonyRecoversWorldMonsterManaAndCapsAtNativeMaximum() {
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                ClassicWorldMonsterCatalog.definitions(4).stream()
                        .filter(value -> value.template().mp() >= 20)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | ordinal(definition.spawnId(), 1);
        ClassicWorldMonsterPopulation.ManaDrain drained =
                population.drainMana(4, objectId, 20);
        assertEquals(20, drained.actualDrain());
        assertEquals(5, population.recoverMana(4, objectId, 5));
        assertEquals(definition.template().mp() - 15,
                population.actor(4, objectId).currentMp());
    }

    @Test
    void ordinaryWorldNpcOwnsCancelableNonRefreshingBoneBreakLease() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot monster = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        long now = 2_000_000_000L;

        assertTrue(population.boneBreak(4, monster.objectId(), now, 2));
        assertFalse(population.boneBreak(4, monster.objectId(), now + 1, 2));
        assertTrue(population.boneBreakActive(
                4, monster.objectId(), now + 1));
        ClassicWorldMonsterPopulation.MagicCancellation cancellation =
                population.cancelMagicEffects(4, monster.objectId());
        assertTrue(cancellation.boneBreak());
        assertFalse(population.boneBreakActive(
                4, monster.objectId(), now + 2));

        assertTrue(population.boneBreak(
                4, monster.objectId(), now + 3, 2));
        assertTrue(population.expireBoneBreaks(
                4, now + 2_000_000_002L).isEmpty());
        assertEquals(1, population.expireBoneBreaks(
                4, now + 2_000_000_003L).size());
    }

    @Test
    void ordinaryWorldNpcOwnsCancelableHorrorOfDeathLease() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot monster = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        long now = 1_000_000_000L;

        assertFalse(population.horrorOfDeath(
                4, monster.objectId(), now, 60));
        assertTrue(population.horrorOfDeathActive(
                4, monster.objectId(), now));
        assertTrue(population.horrorOfDeath(
                4, monster.objectId(), now + 1, 60));
        assertTrue(population.expireHorrorOfDeaths(
                4, now + 59_999_999_999L).isEmpty());
        assertEquals(1, population.expireHorrorOfDeaths(
                4, now + 60_000_000_000L).size());
        assertFalse(population.horrorOfDeathActive(
                4, monster.objectId(), now + 60_000_000_000L));
        assertFalse(population.horrorOfDeath(
                4, monster.objectId(), now + 61_000_000_000L, 60));
        ClassicWorldMonsterPopulation.MagicCancellation cancelled =
                population.cancelMagicEffects(4, monster.objectId());
        assertTrue(cancelled.horrorOfDeath());
        assertFalse(population.horrorOfDeathActive(
                4, monster.objectId(), now + 2));
    }

    @Test
    void selectedMap4ResourceKeepsEveryEnabledMonsterSpawnGroup() throws Exception {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                ClassicWorldMonsterCatalog.definitions(4);
        assertEquals(ClassicWorldMonsterCatalog.MAP4_DEFINITION_COUNT,
                definitions.size());
        assertEquals(ClassicWorldMonsterCatalog.MAP4_POPULATION,
                definitions.stream().mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map4-monsters.lwm.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP4_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void worldTreeRegionKeepsExactSelectedOrcAndWildBoarGroups() {
        ClassicWorldMonsterCatalog.SpawnDefinition orc =
                ClassicWorldMonsterCatalog.definition(4, 3011);
        ClassicWorldMonsterCatalog.SpawnDefinition boar =
                ClassicWorldMonsterCatalog.definition(4, 3012);
        assertNotNull(orc);
        assertNotNull(boar);
        assertEquals(50, orc.count());
        assertEquals(45009, orc.template().npcId());
        assertEquals("$59", orc.template().nameId());
        assertEquals(56, orc.template().gfxId());
        assertEquals(2, orc.template().level());
        assertEquals(6, orc.template().hp());
        assertEquals(10, orc.template().armorClass());
        assertEquals(3, orc.heading());
        assertEquals(32851, orc.areaMinX());
        assertEquals(32200, orc.areaMinY());
        assertEquals(33200, orc.areaMaxX());
        assertEquals(32560, orc.areaMaxY());
        assertTrue(orc.respawnScreen());
        assertEquals(30, orc.minRespawnSeconds());
        assertEquals(60, orc.maxRespawnSeconds());

        assertEquals(50, boar.count());
        assertEquals(45023, boar.template().npcId());
        assertEquals("$931", boar.template().nameId());
        assertEquals(979, boar.template().gfxId());
        assertEquals(20, boar.template().hp());
    }

    @Test
    void deterministicPopulationUsesOnlyPassableUniqueSourceAreaCells() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        Set<String> cells = new HashSet<>();
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldMonsterCatalog.definitions(4)) {
            for (int slot = 1; slot <= definition.count(); slot++) {
                long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                        | ordinal(definition.spawnId(), slot);
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(4, objectId);
                assertNotNull(actor);
                assertEquals(definition.spawnId(), actor.spawnId());
                assertEquals(slot, actor.spawnNumber());
                assertTrue(ClassicWorldMapCatalog.forMap(4)
                        .standingPassable(actor.x(), actor.y()));
                assertTrue(cells.add(actor.x() + ":" + actor.y()));
                if (definition.areaSpawn()) {
                    assertTrue(actor.x() >= definition.areaMinX()
                            && actor.x() <= definition.areaMaxX());
                    assertTrue(actor.y() >= definition.areaMinY()
                            && actor.y() <= definition.areaMaxY());
                }
            }
        }
        ClassicWorldMonsterPopulation.Snapshot nearest = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(nearest);
        assertEquals(3011, nearest.spawnId());
        assertEquals(29, nearest.spawnNumber());
        assertEquals(0x6500_1300L, nearest.objectId());
        assertEquals(33051, nearest.x());
        assertEquals(32338, nearest.y());
        assertEquals(
                "0B1B81527E0013006538000003000005000000FEFF24353900"
                        + "000000000000000000FF000200FFFF00",
                HexFormat.of().withUpperCase().formatHex(
                        Protocol.classicWorldMonster(nearest)));
    }

    @Test
    void ordinaryWorldMonsterHasteOwnsMutualExclusionIntervalsAndExpiry() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 5_000_000_000L;
        assertTrue(population.slow(4, target.objectId(), now, 30));
        assertTrue(population.slowActive(4, target.objectId(), now));
        assertTrue(population.haste(4, target.objectId(), now, 2));
        assertFalse(population.slowActive(4, target.objectId(), now));
        assertTrue(population.hasteActive(4, target.objectId(), now));

        int targetX = 0;
        int targetY = 0;
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(4);
        outer: for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = target.x() + dx;
                int y = target.y() + dy;
                if (map.attackPosition(target.x(), target.y(), x, y, 1)) {
                    targetX = x;
                    targetY = y;
                    break outer;
                }
            }
        }
        assertTrue(targetX > 0 && targetY > 0);
        assertTrue(population.provoke(4, target.objectId(), 3));
        assertNotNull(population.attack(
                4, target.objectId(), 3, targetX, targetY, now));
        int hastedInterval = BattleActionInterval.npcSourceMillis(
                Math.max(100, target.template().atkspeed()), true, false, false);
        assertFalse(population.actionReady(
                4, target.objectId(), now + hastedInterval * 1_000_000L - 1));
        assertTrue(population.actionReady(
                4, target.objectId(), now + hastedInterval * 1_000_000L));

        assertTrue(population.expireHastes(
                4, now + 1_999_999_999L).isEmpty());
        assertEquals(List.of(target.objectId()), population.expireHastes(
                4, now + 2_000_000_000L).stream()
                .map(ClassicWorldMonsterPopulation.Snapshot::objectId).toList());
        assertFalse(population.hasteActive(
                4, target.objectId(), now + 2_000_000_000L));
    }

    @Test
    void cancellationAtomicallyClearsEveryImplementedWorldMonsterStatus() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        ClassicWorldMonsterPopulation.Snapshot paralysisTarget =
                population.nearest(
                        4, ClassicWorldMapCatalog.WORLD_TREE_X,
                        ClassicWorldMapCatalog.WORLD_TREE_Y, 45023);
        assertNotNull(target);
        assertNotNull(paralysisTarget);
        long now = 5_500_000_000L;
        assertTrue(population.haste(4, target.objectId(), now, 1_200));
        assertTrue(population.blind(4, target.objectId(), now, 64));
        assertTrue(population.weakness(4, target.objectId(), now, 64));
        assertTrue(population.disease(4, target.objectId(), now, 64));
        assertTrue(population.silence(4, target.objectId(), now, 16));
        assertTrue(population.decayPotion(4, target.objectId(), now, 16));
        assertTrue(population.poison(
                4, target.objectId(), 3, now, 30, 3_000));
        assertTrue(population.areaSilence(
                4, target.objectId(), now, 16));
        assertTrue(population.windShackle(
                4, target.objectId(), now, 16));
        assertTrue(population.polluteWater(
                4, target.objectId(), now, 192));
        assertTrue(population.strikerGale(
                4, target.objectId(), now, 192));
        assertTrue(population.elementalFallDown(
                4, target.objectId(), now, 32, 8));
        assertTrue(population.eraseMagic(
                4, target.objectId(), now, 32));
        assertEquals(1, population.addWeaponBreakStack(
                4, target.objectId()));

        ClassicWorldMonsterPopulation.MagicCancellation cancelled =
                population.cancelMagicEffects(4, target.objectId());
        assertNotNull(cancelled);
        assertTrue(cancelled.haste());
        assertFalse(cancelled.slow());
        assertTrue(cancelled.blind());
        assertTrue(cancelled.weakness());
        assertTrue(cancelled.disease());
        assertTrue(cancelled.silence());
        assertTrue(cancelled.decayPotion());
        assertTrue(cancelled.poison());
        assertFalse(cancelled.paralysis());
        assertTrue(cancelled.windShackle());
        assertTrue(cancelled.polluteWater());
        assertTrue(cancelled.strikerGale());
        assertTrue(cancelled.elementalFallDown().present());
        assertTrue(cancelled.eraseMagic());
        assertTrue(cancelled.weaponBreak());
        assertFalse(population.hasteActive(4, target.objectId(), now));
        assertFalse(population.blindActive(4, target.objectId(), now));
        assertFalse(population.weaknessActive(4, target.objectId(), now));
        assertFalse(population.diseaseActive(4, target.objectId(), now));
        assertFalse(population.silenceActive(4, target.objectId(), now));
        assertFalse(population.decayPotionActive(4, target.objectId(), now));
        assertEquals(0, population.poisonPalette(4, target.objectId(), now));
        assertFalse(population.areaSilenceActive(4, target.objectId(), now));
        assertFalse(population.windShackleActive(4, target.objectId(), now));
        assertFalse(population.polluteWaterActive(4, target.objectId(), now));
        assertFalse(population.strikerGaleActive(4, target.objectId(), now));
        assertEquals(0, population.elementalFallDownModifier(
                4, target.objectId(), 8, now));
        assertEquals(103, population.effectiveMagicResistance(
                4, target.objectId(), 103, now));
        assertEquals(0, population.weaponBreakStacks(4, target.objectId()));
        assertTrue(population.pollPoisons(
                4, now + 3_000_000_000L, 3_000, 5).isEmpty());

        assertTrue(population.slow(
                4, paralysisTarget.objectId(), now, 64));
        assertTrue(population.paralysis(
                4, paralysisTarget.objectId(), now, 16_000));
        ClassicWorldMonsterPopulation.MagicCancellation paralysisCancelled =
                population.cancelMagicEffects(
                        4, paralysisTarget.objectId());
        assertNotNull(paralysisCancelled);
        assertTrue(paralysisCancelled.slow());
        assertTrue(paralysisCancelled.paralysis());
        assertFalse(population.slowActive(
                4, paralysisTarget.objectId(), now));
        assertFalse(population.paralysisActive(
                4, paralysisTarget.objectId(), now));
        assertTrue(population.actor(
                4, paralysisTarget.objectId()).alive());
    }

    @Test void nativeElementalFallDownLeaseChangesMonsterResistanceAndDeathClearsIt() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 3_000_000_000L;
        assertTrue(population.elementalFallDown(
                4, target.objectId(), now, 32, 8));
        assertEquals(-50, population.elementalFallDownModifier(
                4, target.objectId(), 8, now));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.template().hp());
        assertEquals(0, population.elementalFallDownModifier(
                4, target.objectId(), 8, now));
    }

    @Test void nativePolluteWaterRefreshesExpiresAndDeathClearsClassicMonster() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 3_250_000_000L;
        assertTrue(population.polluteWater(
                4, target.objectId(), now, 192));
        assertTrue(population.polluteWaterActive(
                4, target.objectId(), now + 1));
        assertTrue(population.polluteWater(
                4, target.objectId(), now + 1_000_000_000L, 192));
        assertTrue(population.polluteWaterActive(
                4, target.objectId(), now + 192_999_999_999L));
        assertFalse(population.polluteWaterActive(
                4, target.objectId(), now + 193_000_000_000L));

        assertTrue(population.polluteWater(
                4, target.objectId(), now + 194_000_000_000L, 192));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.template().hp());
        assertFalse(population.polluteWaterActive(
                4, target.objectId(), now + 194_000_000_001L));
    }

    @Test void nativeStrikerGaleRefreshesExpiresAndDeathClearsClassicMonster() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 3_250_000_000L;
        assertTrue(population.strikerGale(
                4, target.objectId(), now, 192));
        assertTrue(population.strikerGaleActive(
                4, target.objectId(), now + 1));
        assertTrue(population.strikerGale(
                4, target.objectId(), now + 1_000_000_000L, 192));
        assertTrue(population.strikerGaleActive(
                4, target.objectId(), now + 192_999_999_999L));
        assertFalse(population.strikerGaleActive(
                4, target.objectId(), now + 193_000_000_000L));

        assertTrue(population.strikerGale(
                4, target.objectId(), now + 194_000_000_000L, 192));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.template().hp());
        assertFalse(population.strikerGaleActive(
                4, target.objectId(), now + 194_000_000_001L));
    }

    @Test void nativeEntangleLeaseSlowsExpiresAndDeathClearsClassicMonster() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_000_000_000L;
        assertTrue(population.slow(4, target.objectId(), now, 64));
        assertTrue(population.slowActive(4, target.objectId(), now));
        assertTrue(population.expireSlows(
                4, now + 63_999_999_999L).isEmpty());
        assertEquals(List.of(target.objectId()), population.expireSlows(
                        4, now + 64_000_000_000L).stream()
                .map(ClassicWorldMonsterPopulation.Snapshot::objectId).toList());
        assertFalse(population.slowActive(
                4, target.objectId(), now + 64_000_000_000L));

        assertTrue(population.slow(
                4, target.objectId(), now + 65_000_000_000L, 64));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.template().hp());
        assertFalse(population.slowActive(
                4, target.objectId(), now + 65_000_000_000L));
    }

    @Test void nativeCursePoisonTicksAttributeDamageAndDeathClearsLease() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        assertEquals(6, target.maximumHp());
        long now = 4_250_000_000L;
        long attacker = 0x10L;
        assertTrue(population.poison(
                4, target.objectId(), attacker, now, 30, 3_000));
        assertFalse(population.poison(
                4, target.objectId(), attacker, now + 1, 30, 3_000));
        assertTrue(population.poisonActive(
                4, target.objectId(), now + 2_999_999_999L));
        assertTrue(population.pollPoisons(
                4, now + 2_999_999_999L, 3_000, 5).isEmpty());

        List<ClassicWorldMonsterPopulation.PoisonUpdate> first =
                population.pollPoisons(
                        4, now + 3_000_000_000L, 3_000, 5);
        assertEquals(1, first.size());
        assertEquals(attacker, first.get(0).attackerObjectId());
        assertEquals(1, first.get(0).hits().size());
        assertEquals(5, first.get(0).hits().get(0).appliedDamage());
        assertEquals(1, first.get(0).target().currentHp());
        assertFalse(first.get(0).expired());

        List<ClassicWorldMonsterPopulation.PoisonUpdate> lethal =
                population.pollPoisons(
                        4, now + 6_000_000_000L, 3_000, 5);
        assertEquals(1, lethal.size());
        ClassicWorldMonsterPopulation.Hit death =
                lethal.get(0).hits().get(0);
        assertTrue(death.diedNow());
        assertEquals(1, death.appliedDamage());
        assertEquals(List.of(new ClassicWorldMonsterPopulation.Contribution(
                        attacker, 10)), death.contributions());
        assertFalse(population.poisonActive(
                4, target.objectId(), now + 6_000_000_001L));
        assertFalse(population.curePoison(4, target.objectId()));
    }

    @Test void nativeNpcPoisonKindsRetainCompanionOwnerAndNpcLifecycle() {
        long now = 4_375_000_000L;
        long companion = 0x7300_0801L;
        long owner = 0x4200_0801L;

        ClassicWorldMonsterPopulation damagePopulation =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot damageTarget =
                damagePopulation.nearest(4,
                        ClassicWorldMapCatalog.WORLD_TREE_X,
                        ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(damageTarget);
        ClassicWorldMonsterPopulation.NpcPoison damage =
                damagePopulation.npcPoison(
                        4, damageTarget.objectId(),
                        ClassicWorldMonsterPopulation.ContributorKind.PET,
                        companion, owner,
                        ClassicWorldMonsterPoisonAttack.Kind.DAMAGE, now);
        assertNotNull(damage);
        assertTrue(damage.applied());
        assertFalse(damage.existingPoison());
        assertEquals(1, damagePopulation.poisonPalette(
                4, damageTarget.objectId(), now));
        assertFalse(damagePopulation.npcPoison(
                4, damageTarget.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PET,
                companion, owner,
                ClassicWorldMonsterPoisonAttack.Kind.SILENCE, now + 1).applied());
        List<ClassicWorldMonsterPopulation.PoisonUpdate> firstTick =
                damagePopulation.pollPoisons(
                        4, now + 3_000_000_000L, 3_000, 5);
        assertEquals(1, firstTick.size());
        assertEquals(ClassicWorldMonsterPopulation.ContributorKind.PET,
                firstTick.get(0).contributorKind());
        assertEquals(companion, firstTick.get(0).attackerObjectId());
        assertEquals(owner, firstTick.get(0).playerObjectId());
        List<ClassicWorldMonsterPopulation.PoisonUpdate> lethal =
                damagePopulation.pollPoisons(
                        4, now + 6_000_000_000L, 3_000, 5);
        assertEquals(1, lethal.size());
        assertTrue(lethal.get(0).hits().get(0).diedNow());
        assertEquals(List.of(new ClassicWorldMonsterPopulation.Contribution(
                ClassicWorldMonsterPopulation.ContributorKind.PET,
                companion, owner, 10)),
                lethal.get(0).hits().get(0).contributions());

        ClassicWorldMonsterPopulation silencePopulation =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot silenceTarget =
                silencePopulation.nearest(4,
                        ClassicWorldMapCatalog.WORLD_TREE_X,
                        ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertTrue(silencePopulation.npcPoison(
                4, silenceTarget.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.SUMMON,
                companion, owner,
                ClassicWorldMonsterPoisonAttack.Kind.SILENCE, now).applied());
        assertTrue(silencePopulation.poisonActive(
                4, silenceTarget.objectId(), now + 1));
        assertEquals(1, silencePopulation.poisonPalette(
                4, silenceTarget.objectId(), now + 1));
        assertTrue(silencePopulation.pollPoisons(
                4, now + 60_000_000_000L, 3_000, 5).isEmpty());
        assertTrue(silencePopulation.curePoison(
                4, silenceTarget.objectId()));
        assertEquals(0, silencePopulation.poisonPalette(
                4, silenceTarget.objectId(), now + 2));

        ClassicWorldMonsterPopulation paralysisPopulation =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot paralysisTarget =
                paralysisPopulation.nearest(4,
                        ClassicWorldMapCatalog.WORLD_TREE_X,
                        ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertTrue(paralysisPopulation.npcPoison(
                4, paralysisTarget.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PET,
                companion, owner,
                ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS, now).applied());
        assertEquals(1, paralysisPopulation.poisonPalette(
                4, paralysisTarget.objectId(), now + 19_999_999_999L));
        assertTrue(paralysisPopulation.actionReady(
                4, paralysisTarget.objectId(), now + 19_999_999_999L));
        List<ClassicWorldMonsterPopulation.ParalysisUpdate> activated =
                paralysisPopulation.pollParalyses(
                        4, now + 20_000_000_000L);
        assertEquals(1, activated.size());
        assertTrue(activated.get(0).activated());
        assertTrue(activated.get(0).poisonParalysis());
        assertEquals(2, paralysisPopulation.poisonPalette(
                4, paralysisTarget.objectId(), now + 20_000_000_000L));
        assertFalse(paralysisPopulation.actionReady(
                4, paralysisTarget.objectId(), now + 20_000_000_000L));
        List<ClassicWorldMonsterPopulation.ParalysisUpdate> expired =
                paralysisPopulation.pollParalyses(
                        4, now + 65_000_000_000L);
        assertEquals(1, expired.size());
        assertTrue(expired.get(0).expired());
        assertTrue(expired.get(0).poisonParalysis());
        assertEquals(0, paralysisPopulation.poisonPalette(
                4, paralysisTarget.objectId(), now + 65_000_000_000L));
        assertFalse(paralysisPopulation.poisonActive(
                4, paralysisTarget.objectId(), now + 65_000_000_000L));
    }

    @Test void nativeRemoveCurseCuresWorldMonsterPoisonBeforeFirstTick() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_400_000_000L;
        assertTrue(population.poison(
                4, target.objectId(), 0x10L, now, 30, 3_000));
        assertEquals(1, population.poisonPalette(
                4, target.objectId(), now + 1));
        assertTrue(population.curePoison(4, target.objectId()));
        assertEquals(0, population.poisonPalette(
                4, target.objectId(), now + 2));
        assertTrue(population.pollPoisons(
                4, now + 3_000_000_000L, 3_000, 5).isEmpty());
        assertEquals(target.maximumHp(), population.actor(
                4, target.objectId()).currentHp());
    }

    @Test void nativeCurseParalysisImmediatelyLocksWorldMonsterAiThenExpires() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_450_000_000L;
        long playerObjectId = 15;
        assertTrue(population.provoke(4, target.objectId(), playerObjectId));
        assertTrue(population.paralysis(
                4, target.objectId(), now, 16_000));
        assertFalse(population.paralysis(
                4, target.objectId(), now + 1, 16_000));
        assertTrue(population.paralysisActive(
                4, target.objectId(), now));
        assertEquals(2, population.poisonPalette(
                4, target.objectId(), now));
        assertFalse(population.actionReady(
                4, target.objectId(), now));
        assertNull(population.attack(
                4, target.objectId(), playerObjectId,
                ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, now));
        assertTrue(population.expireParalyses(
                4, now + 15_999_999_999L).isEmpty());
        assertEquals(List.of(target.objectId()), population.expireParalyses(
                        4, now + 16_000_000_000L).stream()
                .map(ClassicWorldMonsterPopulation.Snapshot::objectId).toList());
        assertFalse(population.paralysisActive(
                4, target.objectId(), now + 16_000_000_000L));
        assertEquals(0, population.poisonPalette(
                4, target.objectId(), now + 16_000_000_000L));
        assertNotNull(population.attack(
                4, target.objectId(), playerObjectId,
                ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y,
                now + 16_000_000_000L));
    }

    @Test void nativeRemoveCurseAndDeathClearWorldMonsterParalysis() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_475_000_000L;
        assertTrue(population.paralysis(
                4, target.objectId(), now, 16_000));
        BattleTimedStatus.ParalysisCancellation cured =
                population.cureParalysis(4, target.objectId());
        assertTrue(cured.present());
        assertFalse(cured.unlockRequired());
        assertEquals(0, population.poisonPalette(
                4, target.objectId(), now + 1));

        assertTrue(population.paralysis(
                4, target.objectId(), now + 2, 16_000));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.maximumHp());
        assertFalse(population.paralysisActive(
                4, target.objectId(), now + 3));
        assertFalse(population.cureParalysis(
                4, target.objectId()).present());
    }

    @Test void freezingBlizzardLocksOrdinaryWorldMonsterAiAndClearsExactly() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_490_000_000L;
        long playerObjectId = 16;
        assertTrue(population.provoke(4, target.objectId(), playerObjectId));

        assertTrue(population.freeze(4, target.objectId(), now, 32));
        assertTrue(population.freezeActive(4, target.objectId(), now));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                playerObjectId, playerObjectId, 1);
        assertTrue(population.freezeActive(4, target.objectId(), now + 1));
        assertEquals(2, population.poisonPalette(
                4, target.objectId(), now));
        assertFalse(population.actionReady(4, target.objectId(), now));
        assertNull(population.attack(
                4, target.objectId(), playerObjectId,
                ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, now));
        assertTrue(population.expireFreezes(
                4, now + 31_999_999_999L).isEmpty());
        assertEquals(List.of(target.objectId()), population.expireFreezes(
                        4, now + 32_000_000_000L).stream()
                .map(ClassicWorldMonsterPopulation.Snapshot::objectId).toList());
        assertFalse(population.freezeActive(
                4, target.objectId(), now + 32_000_000_000L));
        assertEquals(0, population.poisonPalette(
                4, target.objectId(), now + 32_000_000_000L));
        assertTrue(population.actionReady(
                4, target.objectId(), now + 32_000_000_000L));

        assertTrue(population.freeze(
                4, target.objectId(), now + 33_000_000_000L, 32));
        ClassicWorldMonsterPopulation.MagicCancellation cancellation =
                population.cancelMagicEffects(4, target.objectId());
        assertNotNull(cancellation);
        assertTrue(cancellation.freeze());
        assertFalse(population.freezeActive(
                4, target.objectId(), now + 33_000_000_001L));

        assertTrue(population.freeze(
                4, target.objectId(), now + 34_000_000_000L, 32));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                playerObjectId, playerObjectId, target.maximumHp());
        assertFalse(population.freezeActive(
                4, target.objectId(), now + 34_000_000_001L));
    }

    @Test void shockStunLocksOrdinaryWorldMonsterAiAndIsCancellationImmune() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_870_000_000L;
        long playerObjectId = 17;
        assertTrue(population.provoke(4, target.objectId(), playerObjectId));

        BattleTimedStatus.ShockStunActivation activation =
                population.shockStun(
                        4, target.objectId(), now, 4, 4);
        assertNotNull(activation);
        assertEquals(4, activation.durationSeconds());
        assertTrue(population.shockStunActive(
                4, target.objectId(), now));
        assertFalse(population.actionReady(4, target.objectId(), now));
        assertNull(population.attack(
                4, target.objectId(), playerObjectId,
                ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, now));

        ClassicWorldMonsterPopulation.MagicCancellation cancellation =
                population.cancelMagicEffects(4, target.objectId());
        assertNotNull(cancellation);
        assertFalse(cancellation.freeze());
        assertTrue(population.shockStunActive(
                4, target.objectId(), now + 1));
        assertTrue(population.expireShockStuns(
                4, now + 3_999_999_999L).isEmpty());
        assertEquals(List.of(target.objectId()), population.expireShockStuns(
                        4, now + 4_000_000_000L).stream()
                .map(ClassicWorldMonsterPopulation.Snapshot::objectId).toList());
        assertFalse(population.shockStunActive(
                4, target.objectId(), now + 4_000_000_000L));
        assertTrue(population.actionReady(
                4, target.objectId(), now + 4_000_000_000L));
    }

    @Test void thunderGrabLocksOrdinaryWorldMonsterAndCancellationRemovesIt() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_920_000_000L;
        long playerObjectId = 19;
        assertTrue(population.provoke(4, target.objectId(), playerObjectId));
        assertTrue(population.thunderGrab(
                4, target.objectId(), now, 4));
        assertTrue(population.thunderGrabActive(
                4, target.objectId(), now));
        assertFalse(population.actionReady(4, target.objectId(), now));

        ClassicWorldMonsterPopulation.MagicCancellation cancellation =
                population.cancelMagicEffects(4, target.objectId());
        assertNotNull(cancellation);
        assertTrue(cancellation.thunderGrab());
        assertFalse(population.thunderGrabActive(
                4, target.objectId(), now + 1));
        assertTrue(population.actionReady(4, target.objectId(), now + 1));
    }

    @Test void nativeEraseMagicQuartersMrUsesExactLeaseAndDeathClears() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 4_500_000_000L;
        assertTrue(population.eraseMagic(4, target.objectId(), now, 32));
        assertEquals(25, population.effectiveMagicResistance(
                4, target.objectId(), 103, now));
        BattleEraseMagicStatus.Lease stale = population.captureEraseMagic(
                4, target.objectId(), now);
        assertTrue(population.eraseMagic(4, target.objectId(), now + 1, 32));
        assertFalse(population.consumeEraseMagic(4, target.objectId(), stale));
        assertTrue(population.consumeEraseMagic(
                4, target.objectId(), now + 2));
        assertEquals(103, population.effectiveMagicResistance(
                4, target.objectId(), 103, now + 3));

        assertTrue(population.eraseMagic(4, target.objectId(), now + 4, 32));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.template().hp());
        assertEquals(103, population.effectiveMagicResistance(
                4, target.objectId(), 103, now + 5));
    }

    @Test
    void selectedMap303PopulationIsHashLockedPlacedAndIndependentFromMap4() {
        assertEquals(ClassicWorldMonsterCatalog.MAP303_DEFINITION_COUNT,
                ClassicWorldMonsterCatalog.definitions(303).size());
        assertEquals(ClassicWorldMonsterCatalog.MAP303_POPULATION,
                ClassicWorldMonsterCatalog.definitions(303).stream()
                        .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        ClassicWorldMonsterCatalog.SpawnDefinition lightning =
                ClassicWorldMonsterCatalog.definition(303, 2038);
        assertNotNull(lightning);
        assertEquals(20, lightning.count());
        assertEquals(45549, lightning.template().npcId());
        assertEquals("$2485", lightning.template().nameId());
        assertEquals(2727, lightning.template().gfxId());
        assertEquals(90, lightning.template().hp());

        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        Set<String> cells = new HashSet<>();
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldMonsterCatalog.definitions(303)) {
            for (int slot = 1; slot <= definition.count(); slot++) {
                long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                        | ordinal(303, definition.spawnId(), slot);
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(303, objectId);
                assertNotNull(actor);
                assertEquals(definition.spawnId(), actor.spawnId());
                assertEquals(slot, actor.spawnNumber());
                assertTrue(ClassicWorldMapCatalog.forMap(303)
                        .standingPassable(actor.x(), actor.y()));
                assertTrue(cells.add(actor.x() + ":" + actor.y()));
            }
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP303_POPULATION, cells.size());
        assertNull(population.actor(4,
                ClassicWorldMonsterPopulation.OBJECT_BASE
                        | ordinal(303, lightning.spawnId(), 1)));
    }

    @Test
    void selectedMap21PopulationKeepsAllSixSourceGroupsAndThirtyActors()
            throws Exception {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                ClassicWorldMonsterCatalog.definitions(21);
        assertEquals(ClassicWorldMonsterCatalog.MAP21_DEFINITION_COUNT,
                definitions.size());
        assertEquals(ClassicWorldMonsterCatalog.MAP21_POPULATION,
                definitions.stream().mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(List.of(1016, 1017, 1022, 1023, 1024, 1025),
                definitions.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::spawnId).toList());

        ClassicWorldMonsterCatalog.SpawnDefinition blob =
                ClassicWorldMonsterCatalog.definition(21, 1016);
        ClassicWorldMonsterCatalog.SpawnDefinition ogre =
                ClassicWorldMonsterCatalog.definition(21, 1022);
        ClassicWorldMonsterCatalog.SpawnDefinition troll =
                ClassicWorldMonsterCatalog.definition(21, 1024);
        assertNotNull(blob);
        assertNotNull(ogre);
        assertNotNull(troll);
        assertEquals(45032, blob.template().npcId());
        assertEquals(20, blob.template().hp());
        assertEquals(45183, ogre.template().npcId());
        assertEquals(200, ogre.template().hp());
        assertTrue(ogre.template().aggressive());
        assertEquals(45186, troll.template().npcId());
        assertEquals(250, troll.template().hp());

        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        assertEquals(ClassicWorldMonsterCatalog.MAP21_POPULATION,
                definitions.stream().mapToInt(definition -> {
                    int found = 0;
                    for (int slot = 1; slot <= definition.count(); slot++) {
                        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                                | ordinal(21, definition.spawnId(), slot);
                        ClassicWorldMonsterPopulation.Snapshot actor =
                                population.actor(21, objectId);
                        assertNotNull(actor);
                        assertTrue(ClassicWorldMapCatalog.forMap(21)
                                .standingPassable(actor.x(), actor.y()));
                        found++;
                    }
                    return found;
                }).sum());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map21-monsters.lwm.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP21_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedSleepingDragonFloorsRetainExactMonsterGroupsAndPopulation()
            throws Exception {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> first =
                ClassicWorldMonsterCatalog.definitions(19);
        assertEquals(ClassicWorldMonsterCatalog.MAP19_DEFINITION_COUNT,
                first.size());
        assertEquals(ClassicWorldMonsterCatalog.MAP19_POPULATION,
                first.stream().mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(1007, first.get(0).spawnId());
        assertEquals(45103, first.get(0).template().npcId());
        assertEquals("$4005", first.get(0).template().nameId());
        assertEquals(80, first.get(0).template().hp());
        assertEquals(1, first.get(0).template().poisonAttack());

        List<ClassicWorldMonsterCatalog.SpawnDefinition> second =
                ClassicWorldMonsterCatalog.definitions(20);
        assertEquals(ClassicWorldMonsterCatalog.MAP20_DEFINITION_COUNT,
                second.size());
        assertEquals(ClassicWorldMonsterCatalog.MAP20_POPULATION,
                second.stream().mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(List.of(45032, 45103, 45151, 45152, 45150),
                second.stream().map(row -> row.template().npcId()).toList());
        assertEquals(List.of(10, 10, 15, 10, 25),
                second.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).toList());

        int[] mapIds = {19, 20};
        String[] resourceHashes = {
            ClassicWorldMonsterCatalog.MAP19_RESOURCE_SHA256,
            ClassicWorldMonsterCatalog.MAP20_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapIds[index] + "-monsters.lwm.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
    }

    @Test
    void selectedMap32RetainsSevenSourceGroupsAndSeventyActors()
            throws Exception {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                ClassicWorldMonsterCatalog.definitions(32);
        assertEquals(ClassicWorldMonsterCatalog.MAP32_DEFINITION_COUNT,
                definitions.size());
        assertEquals(ClassicWorldMonsterCatalog.MAP32_POPULATION,
                definitions.stream().mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(List.of(1089, 1090, 1091, 1092, 1093, 1094, 1095),
                definitions.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::spawnId).toList());
        assertEquals(List.of(45060, 45269, 45270, 45280, 45286, 45289, 45373),
                definitions.stream().map(row -> row.template().npcId()).toList());
        assertEquals(List.of(5, 15, 15, 5, 15, 10, 5),
                definitions.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).toList());
        assertEquals(List.of(60, 270, 250, 270, 280, 350, 500),
                definitions.stream().map(row -> row.template().hp()).toList());

        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        int actors = 0;
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition : definitions) {
            for (int slot = 1; slot <= definition.count(); slot++) {
                long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                        | ordinal(32, definition.spawnId(), slot);
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(32, objectId);
                assertNotNull(actor);
                assertTrue(ClassicWorldMapCatalog.forMap(32)
                        .standingPassable(actor.x(), actor.y()));
                assertFalse(ClassicWorldNpcCatalog.fixedMerchantOccupies(
                        32, actor.x(), actor.y()));
                actors++;
            }
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP32_POPULATION, actors);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map32-monsters.lwm.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP32_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedDragonValleyFloorsAndLairRetainExactGroupsPopulationAndPlacement()
            throws Exception {
        int[] mapIds = {30, 31, 33, 35, 36, 37};
        int[] groupCounts = {ClassicWorldMonsterCatalog.MAP30_DEFINITION_COUNT,
                ClassicWorldMonsterCatalog.MAP31_DEFINITION_COUNT,
                ClassicWorldMonsterCatalog.MAP33_DEFINITION_COUNT,
                ClassicWorldMonsterCatalog.MAP35_DEFINITION_COUNT,
                ClassicWorldMonsterCatalog.MAP36_DEFINITION_COUNT,
                ClassicWorldMonsterCatalog.MAP37_DEFINITION_COUNT};
        int[] populations = {ClassicWorldMonsterCatalog.MAP30_POPULATION,
                ClassicWorldMonsterCatalog.MAP31_POPULATION,
                ClassicWorldMonsterCatalog.MAP33_POPULATION,
                ClassicWorldMonsterCatalog.MAP35_POPULATION,
                ClassicWorldMonsterCatalog.MAP36_POPULATION,
                ClassicWorldMonsterCatalog.MAP37_POPULATION};
        List<List<Integer>> spawnIds = List.of(
                List.of(1076, 1077, 1079, 1080, 1081, 1082),
                List.of(1083, 1084, 1085, 1086, 1087, 1088),
                List.of(1096, 1097, 1098, 1099, 1100, 1101, 1102, 1103),
                List.of(1104, 1105, 1106, 1107, 1108),
                List.of(1109, 1110, 1111, 1112),
                List.of(1113));
        String[] resourceHashes = {
                ClassicWorldMonsterCatalog.MAP30_RESOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP31_RESOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP33_RESOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP35_RESOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP36_RESOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP37_RESOURCE_SHA256};
        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                    ClassicWorldMonsterCatalog.definitions(mapId);
            assertEquals(groupCounts[index], definitions.size());
            assertEquals(populations[index], definitions.stream()
                    .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
            assertEquals(spawnIds.get(index), definitions.stream().map(
                    ClassicWorldMonsterCatalog.SpawnDefinition::spawnId).toList());
            int actors = 0;
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition : definitions) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                            | ordinal(mapId, definition.spawnId(), slot);
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            population.actor(mapId, objectId);
                    assertNotNull(actor);
                    assertTrue(ClassicWorldMapCatalog.forMap(mapId)
                            .standingPassable(actor.x(), actor.y()));
                    actors++;
                }
            }
            assertEquals(populations[index], actors);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapId + "-monsters.lwm.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
    }

    @Test
    void selectedFairyDungeonsPreserveTheirExplicitlyEmptyMonsterResources()
            throws Exception {
        int[] mapIds = {209, 213};
        String[] resourceHashes = {
            ClassicWorldMonsterCatalog.MAP209_RESOURCE_SHA256,
            ClassicWorldMonsterCatalog.MAP213_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            assertTrue(ClassicWorldMonsterCatalog.definitions(mapId).isEmpty());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapId + "-monsters.lwm.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP213_SOURCE_SHA256);
    }

    @Test
    void selectedMap445PreservesItsExplicitlyEmptyMonsterResource()
            throws Exception {
        assertTrue(ClassicWorldMonsterCatalog.definitions(445).isEmpty());
        assertEquals(ClassicWorldMonsterCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP445_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map445-monsters.lwm.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP445_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap447PreservesItsExplicitlyEmptyMonsterResource()
            throws Exception {
        assertTrue(ClassicWorldMonsterCatalog.definitions(447).isEmpty());
        assertEquals(ClassicWorldMonsterCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP447_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map447-monsters.lwm.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP447_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap17PreservesItsExplicitlyEmptyMonsterResource()
            throws Exception {
        assertTrue(ClassicWorldMonsterCatalog.definitions(17).isEmpty());
        assertEquals(ClassicWorldMonsterCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldMonsterCatalog.MAP17_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map17-monsters.lwm.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldMonsterCatalog.MAP17_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void monsterDamageDeathRemovalAndSameObjectRespawnAreAuthoritative() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        ClassicWorldMonsterPopulation.Hit hit = population.damage(
                4, target.objectId(), target.maximumHp());
        assertNotNull(hit);
        assertTrue(hit.diedNow());
        assertEquals(0, hit.after().currentHp());
        assertFalse(hit.after().alive());
        assertFalse(population.occupies(4, target.x(), target.y()));

        ClassicWorldMonsterPopulation.Snapshot corpse =
                population.removeCorpse(4, target.objectId());
        assertNotNull(corpse);
        assertFalse(corpse.present());
        ClassicWorldMonsterPopulation.Respawn respawn =
                population.respawn(4, target.objectId());
        assertNotNull(respawn);
        assertEquals(target.objectId(), respawn.after().objectId());
        assertEquals(target.maximumHp(), respawn.after().currentHp());
        assertTrue(respawn.after().alive());
        assertTrue(respawn.after().present());
        assertTrue(population.occupies(
                4, respawn.after().x(), respawn.after().y()));
    }

    @Test
    void turnUndeadEligibleWorldMonsterUsesFullHpPlayerContributionDeath() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45065);
        assertNotNull(target);
        SourceNpcPhysicalStats source =
                SourceNpcPhysicalStats.memoryFixture(target.npcId());
        assertTrue(BattleTurnUndead.eligible(source));
        assertEquals(1, source.undead());
        assertTrue(source.turnUndeadEligible());

        ClassicWorldMonsterPopulation.Hit death = population.damage(
                4, target.objectId(), ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.currentHp());
        assertNotNull(death);
        assertTrue(death.diedNow());
        assertEquals(target.currentHp(), death.appliedDamage());
        assertEquals(0, death.after().currentHp());
        assertEquals(List.of(new ClassicWorldMonsterPopulation.Contribution(
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.currentHp() + target.maximumHp() / 10L)),
                death.contributions());
    }

    @Test
    void manaDrainDebitsSelectedWorldMonsterMpAtomicallyAndPersists() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45065);
        assertNotNull(target);
        SourceNpcPhysicalStats source =
                SourceNpcPhysicalStats.memoryFixture(target.npcId());
        assertTrue(BattleManaDrain.eligibleNpc(source));
        assertEquals(source.maxMp(), target.maximumMp());
        assertEquals(12, target.currentMp());

        ClassicWorldMonsterPopulation.ManaDrain first =
                population.drainMana(4, target.objectId(), 19);
        assertNotNull(first);
        assertEquals(19, first.requestedDrain());
        assertEquals(12, first.actualDrain());
        assertEquals(12, first.before().currentMp());
        assertEquals(0, first.after().currentMp());
        assertEquals(target.currentHp(), first.after().currentHp());
        assertTrue(first.after().alive());

        ClassicWorldMonsterPopulation.ManaDrain second =
                population.drainMana(4, target.objectId(), 19);
        assertNotNull(second);
        assertEquals(0, second.actualDrain());
        assertEquals(0, second.after().currentMp());
    }

    @Test
    void mindBreakStrictWorldMonsterDebitRequiresMoreThanRequested() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45065);
        assertNotNull(target);
        assertEquals(12, target.currentMp());
        ClassicWorldMonsterPopulation.ManaDrain first =
                population.drainManaIfStrictlyAbove(
                        4, target.objectId(), 5);
        assertEquals(5, first.actualDrain());
        assertEquals(7, first.after().currentMp());
        ClassicWorldMonsterPopulation.ManaDrain equal =
                population.drainManaIfStrictlyAbove(
                        4, target.objectId(), 7);
        assertEquals(0, equal.actualDrain());
        assertEquals(7, equal.after().currentMp());
    }

    @Test
    void livingMonsterHealingClampsAtTemplateMaximumAndRejectsDeadActors() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);

        int nonFatalDamage = Math.max(1, target.maximumHp() - 1);
        ClassicWorldMonsterPopulation.Hit damaged = population.damage(
                4, target.objectId(), nonFatalDamage);
        assertNotNull(damaged);
        assertFalse(damaged.diedNow());
        ClassicWorldMonsterPopulation.Healing first = population.heal(
                4, target.objectId(), 1);
        assertNotNull(first);
        assertEquals(1, first.appliedHealing());
        assertEquals(damaged.after().currentHp() + 1, first.after().currentHp());

        ClassicWorldMonsterPopulation.Healing clamped = population.heal(
                4, target.objectId(), target.maximumHp());
        assertNotNull(clamped);
        assertEquals(target.maximumHp(), clamped.after().currentHp());
        assertEquals(target.maximumHp() - first.after().currentHp(),
                clamped.appliedHealing());

        ClassicWorldMonsterPopulation.Hit death = population.damage(
                4, target.objectId(), target.maximumHp());
        assertNotNull(death);
        assertTrue(death.diedNow());
        assertNull(population.heal(4, target.objectId(), 1));
    }

    @Test
    void tamingDetachesLivingSpawnAndReleaseOrAbandonOwnsOneLifecycle() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot source = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45065);
        assertNotNull(source);
        assertTrue(SourceNpcPhysicalStats.memoryFixture(source.npcId()).tamable());

        ClassicWorldMonsterPopulation.Capture capture =
                population.captureTamed(4, source.objectId());
        assertNotNull(capture);
        assertEquals(source, capture.captured());
        assertTrue(capture.hidden().alive());
        assertFalse(capture.hidden().present());
        assertFalse(population.occupies(4, source.x(), source.y()));
        assertNull(population.captureTamed(4, source.objectId()));
        assertNull(population.respawn(4, source.objectId()));

        BattleCompanion companion = BattleCompanion.tamedFromClassicWorld(
                0x6000_0A36L, source, -1,
                SourceNpcPhysicalStats.memoryFixture(source.npcId()));
        assertTrue(companion.snapshot().kind().classicWorldEligible());
        assertEquals(source, companion.classicWorldTamedOrigin());
        companion.relocate(source.x() + 1, source.y());
        ClassicWorldMonsterPopulation.TamedRelease release =
                population.releaseTamed(4, source.objectId(), companion.snapshot());
        assertNotNull(release);
        assertTrue(release.released().present());
        assertTrue(release.released().alive());
        assertEquals(source.x() + 1, release.released().x());
        assertTrue(population.occupies(
                4, release.released().x(), release.released().y()));

        ClassicWorldMonsterPopulation.Capture second =
                population.captureTamed(4, source.objectId());
        assertNotNull(second);
        ClassicWorldMonsterPopulation.Snapshot abandoned =
                population.abandonTamed(4, source.objectId());
        assertNotNull(abandoned);
        assertFalse(abandoned.alive());
        assertFalse(abandoned.present());
        assertNotNull(population.respawn(4, source.objectId()));
    }

    @Test
    void callOfNatureResurrectsVisibleCorpseInPlaceAtFullHp() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        ClassicWorldMonsterPopulation.Hit death = population.damage(
                4, target.objectId(), target.maximumHp());
        assertNotNull(death);
        assertTrue(death.diedNow());
        ClassicWorldMonsterPopulation.Resurrection restored =
                population.resurrect(
                        4, target.objectId(), target.maximumHp());
        assertNotNull(restored);
        assertEquals(target.objectId(), restored.after().objectId());
        assertEquals(target.x(), restored.after().x());
        assertEquals(target.y(), restored.after().y());
        assertEquals(target.maximumHp(), restored.after().currentHp());
        assertTrue(restored.after().alive());
        assertTrue(restored.after().present());
        assertTrue(population.occupies(4, target.x(), target.y()));
        assertNull(population.removeCorpse(4, target.objectId()));
        assertNull(population.resurrect(
                4, target.objectId(), target.maximumHp()));
    }

    @Test
    void classicWorldWeaponBreakStacksCapAndResetAcrossDeathRespawn() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        for (int index = 0; index < 12; index++) {
            assertEquals(Math.min(index + 1, BattleWeaponBreak.MAX_STACKS),
                    population.addWeaponBreakStack(4, target.objectId()));
        }
        assertEquals(BattleWeaponBreak.MAX_STACKS,
                population.weaponBreakStacks(4, target.objectId()));
        assertEquals(50, BattleWeaponBreak.damageAfterStacks(
                100, population.weaponBreakStacks(4, target.objectId())));

        ClassicWorldMonsterPopulation.Hit death = population.damage(
                4, target.objectId(), target.maximumHp());
        assertNotNull(death);
        assertTrue(death.diedNow());
        assertEquals(0, population.weaponBreakStacks(4, target.objectId()));
        assertEquals(-1,
                population.addWeaponBreakStack(4, target.objectId()));
        assertNotNull(population.removeCorpse(4, target.objectId()));
        ClassicWorldMonsterPopulation.Respawn respawn = population.respawn(
                4, target.objectId());
        assertNotNull(respawn);
        assertEquals(0, population.weaponBreakStacks(4, target.objectId()));
        assertEquals(1,
                population.addWeaponBreakStack(4, target.objectId()));
    }

    @Test
    void monsterDeathSnapshotsRetainedRawHateAndFirstAttackBonusOnce() {
        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45270);
        assertNotNull(target);
        ClassicWorldMonsterPopulation.Hit first = population.damage(
                4, target.objectId(), 101, 2);
        ClassicWorldMonsterPopulation.Hit death = population.damage(
                4, target.objectId(), 202, 999);
        assertNotNull(first);
        assertTrue(first.contributions().isEmpty());
        assertNotNull(death);
        assertTrue(death.diedNow());
        assertEquals(248, death.appliedDamage());
        assertEquals(List.of(
                        new ClassicWorldMonsterPopulation.Contribution(101, 27),
                        new ClassicWorldMonsterPopulation.Contribution(202, 999)),
                death.contributions());
        assertEquals(1_026, death.contributions().stream()
                .mapToLong(ClassicWorldMonsterPopulation.Contribution::hate).sum());
        assertEquals(730, target.template().experience());
        assertEquals(-30, target.template().lawful());
        assertEquals(19, (730L * 27) / 1_026);
        assertEquals(710, (730L * 999) / 1_026);
        assertEquals(0, (-30L * 27) / 1_026);
        assertEquals(-29, (-30L * 999) / 1_026);
    }

    @Test
    void contributionKeepsSummonActorDistinctFromDropOwner() {
        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45270);
        assertNotNull(target);
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.SUMMON,
                0x6000_0049L, 3, 2);
        ClassicWorldMonsterPopulation.Hit death = population.damage(
                4, target.objectId(), 3, 999);
        assertNotNull(death);
        assertTrue(death.diedNow());
        assertEquals(new ClassicWorldMonsterPopulation.Contribution(
                        ClassicWorldMonsterPopulation.ContributorKind.SUMMON,
                        0x6000_0049L, 3, 27),
                death.contributions().get(0));
        assertEquals(new ClassicWorldMonsterPopulation.Contribution(3, 999),
                death.contributions().get(1));
    }

    @Test
    void retainedHateAttackAndActionSpeedGateAreAuthoritative() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot orc = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(orc);
        long playerObjectId = 15;
        long now = 1_000_000_000L;
        assertTrue(population.provoke(4, orc.objectId(), playerObjectId));
        assertEquals(playerObjectId,
                population.targetObjectId(4, orc.objectId()));
        population.transferTargets(playerObjectId, 16);
        assertEquals(16, population.targetObjectId(4, orc.objectId()));
        population.transferTargets(16, playerObjectId);
        ClassicWorldMonsterPopulation.Attack attack = population.attack(
                4, orc.objectId(), playerObjectId,
                ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, now);
        assertNotNull(attack);
        assertEquals(playerObjectId, attack.targetObjectId());
        assertEquals(0, attack.attacker().heading());
        assertFalse(population.actionReady(4, orc.objectId(),
                now + orc.template().atkspeed() * 1_000_000L - 1));
        assertTrue(population.actionReady(4, orc.objectId(),
                now + orc.template().atkspeed() * 1_000_000L));

        population.clearTargetsForPlayer(playerObjectId);
        assertEquals(0, population.targetObjectId(4, orc.objectId()));
    }

    @Test
    void retainedMobMagicConsumesMpAndOwnActionClockBeforePhysicalAttack() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot mage = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45224);
        assertNotNull(mage);
        assertEquals(0x6500_1593L, mage.objectId());
        assertEquals(33953, mage.x());
        assertEquals(32437, mage.y());

        ClassicWorldMonsterPopulation.GridPoint target =
                visibleTargetAtDistance(mage, 1);
        assertNotNull(target);
        long targetObjectId = 15;
        long now = 3_000_000_000L;
        assertTrue(population.provoke(4, mage.objectId(), targetObjectId));
        ClassicWorldMonsterPopulation.MagicAttack magic = population.magicAttack(
                4, mage.objectId(), targetObjectId, target.x(), target.y(), now,
                1, 1);
        assertNotNull(magic);
        assertEquals(BattleSpellCatalog.CONE_OF_COLD, magic.skill().skillId());
        assertEquals(82, magic.attacker().currentMp());
        assertFalse(population.actionReady(4, mage.objectId(),
                now + 1_919_999_999L));
        assertTrue(population.actionReady(4, mage.objectId(),
                now + 1_920_000_000L));
        assertNull(population.attack(4, mage.objectId(), targetObjectId,
                target.x(), target.y(), now));

        long next = now + 1_920_000_000L;
        assertNull(population.magicAttack(
                4, mage.objectId(), targetObjectId, target.x(), target.y(), next,
                2, 1));
        assertNotNull(population.attack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), next));
    }

    @Test
    void areaSilenceBlocksEveryWorldMonsterMagicAttemptButNotPhysicalAttack() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot mage = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45224);
        assertNotNull(mage);
        ClassicWorldMonsterPopulation.GridPoint target =
                visibleTargetAtDistance(mage, 1);
        assertNotNull(target);
        long targetObjectId = 17;
        long now = 3_000_000_000L;
        assertTrue(population.provoke(4, mage.objectId(), targetObjectId));
        assertTrue(population.areaSilence(
                4, mage.objectId(), now, 16));
        assertTrue(population.areaSilenceActive(
                4, mage.objectId(), now + 1));
        assertNull(population.magicAttack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), now + 1, 1, 1));
        assertNull(population.magicAttack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(),
                now + java.util.concurrent.TimeUnit.SECONDS.toNanos(15), 1, 1));
        assertNotNull(population.attack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), now + 1));

        long expiry = now + java.util.concurrent.TimeUnit.SECONDS.toNanos(16);
        assertFalse(population.areaSilenceActive(4, mage.objectId(), expiry));
        assertNotNull(population.magicAttack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), expiry, 1, 1));
    }

    @Test
    void ordinarySilenceBlocksAndConsumesExactlyOneEligibleMonsterMagicAttempt() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot mage = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45224);
        assertNotNull(mage);
        ClassicWorldMonsterPopulation.GridPoint target =
                visibleTargetAtDistance(mage, 1);
        assertNotNull(target);
        long targetObjectId = 18;
        long now = 3_200_000_000L;
        assertTrue(population.provoke(4, mage.objectId(), targetObjectId));
        assertTrue(population.silence(4, mage.objectId(), now, 16));
        assertTrue(population.silenceActive(4, mage.objectId(), now + 1));

        assertNotNull(population.attack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), now + 1));
        assertTrue(population.silenceActive(4, mage.objectId(), now + 1));
        long spellAttempt = now + 2_000_000_000L;
        assertNull(population.magicAttack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), spellAttempt, 1, 1));
        assertFalse(population.silenceActive(
                4, mage.objectId(), spellAttempt + 1));
        assertNotNull(population.magicAttack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), spellAttempt + 1, 1, 1));
    }

    @Test
    void windShackleDoesNotRefreshAndExtendsWorldMonsterAttackClock() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot mage = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45224);
        assertNotNull(mage);
        ClassicWorldMonsterPopulation.GridPoint target =
                visibleTargetAtDistance(mage, 1);
        assertNotNull(target);
        long targetObjectId = 171;
        long now = 3_500_000_000L;
        assertTrue(population.provoke(4, mage.objectId(), targetObjectId));
        assertTrue(population.windShackle(4, mage.objectId(), now, 16));
        assertFalse(population.windShackle(4, mage.objectId(), now + 1, 16));
        assertTrue(population.windShackleActive(4, mage.objectId(), now + 1));

        assertNotNull(population.attack(
                4, mage.objectId(), targetObjectId,
                target.x(), target.y(), now));
        int nativeDelay = BattleActionInterval.npcSourceMillis(
                Math.max(100, mage.template().atkspeed()), false, false);
        int shackledDelay = BattleActionInterval.npcSourceMillis(
                Math.max(100, mage.template().atkspeed()), false, false, true);
        assertNull(population.attack(
                4, mage.objectId(), targetObjectId, target.x(), target.y(),
                now + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(nativeDelay)));
        assertNotNull(population.attack(
                4, mage.objectId(), targetObjectId, target.x(), target.y(),
                now + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(shackledDelay)));
        assertFalse(population.windShackleActive(
                4, mage.objectId(),
                now + java.util.concurrent.TimeUnit.SECONDS.toNanos(16)));
    }

    @Test
    void retainedOutOfMeleeBranchAttemptsMagicWithoutTheHalfPhysicalCoin() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot mage = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45224);
        assertNotNull(mage);
        ClassicWorldMonsterPopulation.GridPoint target =
                visibleTargetAtDistance(mage, 6);
        assertNotNull(target);
        long targetObjectId = 16;
        assertTrue(population.provoke(4, mage.objectId(), targetObjectId));
        ClassicWorldMonsterPopulation.MagicAttack magic = population.magicAttack(
                4, mage.objectId(), targetObjectId, target.x(), target.y(),
                4_000_000_000L, 2, 1);
        assertNotNull(magic);
        assertEquals(82, magic.attacker().currentMp());
    }

    @Test
    void retainedType1OgreUsesStrictChanceDoubleLeverageMetadataAndAttackClock() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot ogre = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45278);
        assertNotNull(ogre);
        assertEquals(0x6500_1012L, ogre.objectId());
        assertEquals(33314, ogre.x());
        assertEquals(32339, ogre.y());
        ClassicWorldMonsterPopulation.GridPoint target =
                visibleTargetAtDistance(ogre, 2);
        assertNotNull(target);
        long targetObjectId = 18;
        long now = 4_500_000_000L;
        assertTrue(population.provoke(4, ogre.objectId(), targetObjectId));
        assertNull(population.physicalSkillAttack(
                4, ogre.objectId(), targetObjectId, target.x(), target.y(),
                now, 1, 30));
        ClassicWorldMonsterPopulation.PhysicalSkillAttack physical =
                population.physicalSkillAttack(
                        4, ogre.objectId(), targetObjectId,
                        target.x(), target.y(), now, 1, 29);
        assertNotNull(physical);
        assertEquals(20, physical.skill().effectiveLeverage());
        assertEquals(30, physical.skill().actionId());
        assertTrue(physical.skill().hasArea());
        assertFalse(population.actionReady(4, ogre.objectId(),
                now + ogre.template().atkspeed() * 1_000_000L - 1));
        assertTrue(population.actionReady(4, ogre.objectId(),
                now + ogre.template().atkspeed() * 1_000_000L));
    }

    @Test
    void retainedFireBeastDistanceRowsAndTwoUseLimitShareOneLife() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot fire = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45341);
        assertNotNull(fire);
        assertEquals(0x6500_11D9L, fire.objectId());
        assertEquals(33568, fire.x());
        assertEquals(32330, fire.y());
        ClassicWorldMonsterPopulation.GridPoint far =
                visibleTargetAtDistance(fire, 2);
        ClassicWorldMonsterPopulation.GridPoint near =
                visibleTargetAtDistance(fire, 1);
        assertNotNull(far);
        assertNotNull(near);
        long targetObjectId = 19;
        long now = 6_000_000_000L;
        assertTrue(population.provoke(4, fire.objectId(), targetObjectId));
        ClassicWorldMonsterPopulation.PhysicalSkillAttack first =
                population.physicalSkillAttack(
                        4, fire.objectId(), targetObjectId,
                        far.x(), far.y(), now, 1, 1);
        assertNotNull(first);
        assertEquals(0, first.skill().actNo());
        assertEquals(954, first.skill().gfxId());
        assertEquals(3, first.skill().actionId());
        long secondAt = now + fire.template().atkspeed() * 1_000_000L;
        assertNotNull(population.physicalSkillAttack(
                4, fire.objectId(), targetObjectId,
                far.x(), far.y(), secondAt, 1, 1));
        long thirdAt = secondAt + fire.template().atkspeed() * 1_000_000L;
        assertNull(population.physicalSkillAttack(
                4, fire.objectId(), targetObjectId,
                far.x(), far.y(), thirdAt, 1, 1));
        ClassicWorldMonsterPopulation.PhysicalSkillAttack close =
                population.physicalSkillAttack(
                        4, fire.objectId(), targetObjectId,
                        near.x(), near.y(), thirdAt, 1, 1);
        assertNotNull(close);
        assertEquals(1, close.skill().actNo());
        assertEquals(30, close.skill().actionId());
    }

    @Test
    void tornadoRetainsMpTriggerChanceAndCasterCenteredMetadata() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot darkElf = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45265);
        assertNotNull(darkElf);
        assertEquals(0x6500_0EA6L, darkElf.objectId());
        assertEquals(33379, darkElf.x());
        assertEquals(32211, darkElf.y());
        ClassicWorldMonsterPopulation.GridPoint target =
                visibleTargetAtDistance(darkElf, 4);
        assertNotNull(target);
        long targetObjectId = 17;
        assertTrue(population.provoke(4, darkElf.objectId(), targetObjectId));
        assertNull(population.magicAttack(
                4, darkElf.objectId(), targetObjectId, target.x(), target.y(),
                5_000_000_000L, 1, 80));
        ClassicWorldMonsterPopulation.MagicAttack magic = population.magicAttack(
                4, darkElf.objectId(), targetObjectId, target.x(), target.y(),
                5_000_000_000L, 1, 79);
        assertNotNull(magic);
        assertEquals(BattleSpellCatalog.TORNADO, magic.skill().skillId());
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED,
                magic.skill().targeting());
        assertEquals(70, magic.attacker().currentMp());
    }

    private static ClassicWorldMonsterPopulation.GridPoint visibleTargetAtDistance(
            ClassicWorldMonsterPopulation.Snapshot actor, int distance) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(actor.mapId());
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int heading = 0; heading < dx.length; heading++) {
            int x = actor.x() + dx[heading] * distance;
            int y = actor.y() + dy[heading] * distance;
            if (map.attackPosition(actor.x(), actor.y(), x, y, distance)) {
                return new ClassicWorldMonsterPopulation.GridPoint(x, y);
            }
        }
        return null;
    }

    @Test
    void retainedHatePursuitUsesMapEdgesOccupancyAndPassiveSpeed() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot boar = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45023);
        assertNotNull(boar);
        assertEquals(3012, boar.spawnId());
        assertEquals(34, boar.spawnNumber());
        assertEquals(0x6500_1337L, boar.objectId());
        assertEquals(33078, boar.x());
        assertEquals(32341, boar.y());
        assertEquals(640, boar.template().passispeed());
        assertEquals(800, boar.template().atkspeed());

        long playerObjectId = 15;
        long now = 2_000_000_000L;
        assertTrue(population.provoke(4, boar.objectId(), playerObjectId));
        Set<ClassicWorldMonsterPopulation.GridPoint> players = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(33075, 32338));
        ClassicWorldMonsterPopulation.Movement first = population.moveToward(
                4, boar.objectId(), playerObjectId, 33075, 32338,
                players, now);
        assertNotNull(first);
        assertEquals(new ClassicWorldMonsterPopulation.GridPoint(33078, 32341),
                new ClassicWorldMonsterPopulation.GridPoint(
                        first.before().x(), first.before().y()));
        assertEquals(new ClassicWorldMonsterPopulation.GridPoint(33077, 32340),
                new ClassicWorldMonsterPopulation.GridPoint(
                        first.after().x(), first.after().y()));
        assertEquals(7, first.after().heading());
        assertFalse(population.actionReady(4, boar.objectId(),
                now + 639_999_999L));

        ClassicWorldMonsterPopulation.Movement second = population.moveToward(
                4, boar.objectId(), playerObjectId, 33075, 32338,
                players, now + 640_000_000L);
        assertNotNull(second);
        assertEquals(33076, second.after().x());
        assertEquals(32339, second.after().y());
        assertEquals(7, second.after().heading());
        assertNotNull(population.attack(
                4, boar.objectId(), playerObjectId, 33075, 32338,
                now + 1_280_000_000L));
    }

    @Test
    void retainedBlindStopsPursuitButAllowsAdjacentAttackAndExpires() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot boar = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45023);
        assertNotNull(boar);
        long playerObjectId = 15;
        long now = 7_000_000_000L;
        assertTrue(population.provoke(4, boar.objectId(), playerObjectId));
        assertTrue(population.blind(4, boar.objectId(), now, 1));
        assertTrue(population.blindActive(4, boar.objectId(), now));

        int farX = boar.x() - 3;
        int farY = boar.y() - 3;
        Set<ClassicWorldMonsterPopulation.GridPoint> farPlayer = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(farX, farY));
        assertNull(population.moveToward(
                4, boar.objectId(), playerObjectId, farX, farY,
                farPlayer, now));

        ClassicWorldMonsterPopulation.GridPoint adjacent =
                visibleTargetAtDistance(boar, 1);
        assertNotNull(adjacent);
        long blindRetryAt = now + ClassicWorldMonsterPopulation
                .TRANSIENT_PATH_RETRY_MILLIS * 1_000_000L;
        assertNotNull(population.attack(
                4, boar.objectId(), playerObjectId,
                adjacent.x(), adjacent.y(), blindRetryAt));

        long expired = now + 1_000_000_000L;
        assertFalse(population.blindActive(4, boar.objectId(), expired));
        assertNotNull(population.moveToward(
                4, boar.objectId(), playerObjectId, farX, farY,
                farPlayer, expired));

        assertTrue(population.blind(
                4, boar.objectId(), expired, 64));
        ClassicWorldMonsterPopulation.Snapshot current =
                population.actor(4, boar.objectId());
        assertNotNull(current);
        population.damage(4, boar.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                playerObjectId, playerObjectId, current.currentHp());
        assertFalse(population.blindActive(
                4, boar.objectId(), expired + 1));
    }

    @Test
    void retainedWeaknessAndDiseaseExpireAndDeathClearsBothLeases() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45065);
        assertNotNull(target);
        long now = 8_000_000_000L;
        assertTrue(population.weakness(4, target.objectId(), now, 64));
        assertTrue(population.disease(4, target.objectId(), now, 64));
        assertTrue(population.weaknessActive(4, target.objectId(), now));
        assertTrue(population.diseaseActive(4, target.objectId(), now));
        assertFalse(population.weaknessActive(
                4, target.objectId(), now + 64_000_000_000L));
        assertFalse(population.diseaseActive(
                4, target.objectId(), now + 64_000_000_000L));

        long refreshed = now + 65_000_000_000L;
        assertTrue(population.weakness(
                4, target.objectId(), refreshed, 64));
        assertTrue(population.disease(
                4, target.objectId(), refreshed, 64));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                3, 3, target.currentHp());
        assertFalse(population.weaknessActive(
                4, target.objectId(), refreshed + 1));
        assertFalse(population.diseaseActive(
                4, target.objectId(), refreshed + 1));
    }

    @Test
    void retainedOrdinarySilenceExpiresAndDeathClearsItsLease() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45065);
        assertNotNull(target);
        long now = 9_000_000_000L;
        assertTrue(population.silence(4, target.objectId(), now, 16));
        assertTrue(population.silenceActive(4, target.objectId(), now));
        assertFalse(population.silenceActive(
                4, target.objectId(), now + 16_000_000_000L));

        long refreshed = now + 17_000_000_000L;
        assertTrue(population.silence(
                4, target.objectId(), refreshed, 16));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                3, 3, target.currentHp());
        assertFalse(population.silenceActive(
                4, target.objectId(), refreshed + 1));
    }

    @Test
    void retainedOrdinaryDecayPotionIsNonRefreshingAndDeathClearsItsLease() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45065);
        assertNotNull(target);
        long now = 9_000_000_000L;
        assertTrue(population.decayPotion(4, target.objectId(), now, 16));
        assertTrue(population.decayPotionActive(4, target.objectId(), now));
        assertTrue(population.decayPotion(
                4, target.objectId(), now + 1_000_000_000L, 16));
        assertFalse(population.decayPotionActive(
                4, target.objectId(), now + 16_000_000_000L));

        long restarted = now + 17_000_000_000L;
        assertTrue(population.decayPotion(
                4, target.objectId(), restarted, 16));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                3, 3, target.currentHp());
        assertFalse(population.decayPotionActive(
                4, target.objectId(), restarted + 1));
    }

    @Test
    void selectedProjectileTemplatesAndNearestRuntimeSlotsRemainDiscoverable() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        List<ClassicWorldMonsterCatalog.SpawnDefinition> projectileGroups =
                ClassicWorldMonsterCatalog.definitions(4).stream()
                        .filter(definition -> definition.template().ranged() > 1)
                        .filter(definition -> definition.template().bowActionId() > 0)
                        .toList();
        assertEquals(33, projectileGroups.size());
        assertEquals(505, projectileGroups.stream().mapToInt(
                ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(8, projectileGroups.stream().map(definition ->
                definition.template().npcId()).distinct().count());

        ClassicWorldMonsterPopulation.Snapshot actor = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45270);
        assertNotNull(actor);
        assertEquals(559, actor.spawnId());
        assertEquals(27, actor.spawnNumber());
        assertEquals(0x6500_0F88L, actor.objectId());
        assertEquals(33266, actor.x());
        assertEquals(32345, actor.y());
        assertEquals(1130, actor.template().gfxId());
        assertEquals(10, actor.template().attackRange());
        assertEquals(66, actor.template().bowActionId());
        assertEquals(1, actor.template().projectileAttackAction());

        ClassicWorldMonsterCatalog.MonsterTemplate orcArcher =
                ClassicWorldMonsterCatalog.definition(4, 190).template();
        assertEquals(3860, orcArcher.gfxId());
        assertEquals(66, orcArcher.bowActionId());
        assertEquals(21, orcArcher.projectileAttackAction());
        ClassicWorldMonsterCatalog.MonsterTemplate stationary =
                ClassicWorldMonsterCatalog.definition(4, 536).template();
        assertEquals(6, stationary.attackRange());
        assertEquals(1991, stationary.bowActionId());
        assertEquals(0, stationary.passispeed());
    }

    @Test
    void selectedMapKeepsExactPoisonAndUnimplementedParalysisPopulations() {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                ClassicWorldMonsterCatalog.definitions(4);
        assertEquals(14, definitions.stream().filter(definition ->
                definition.template().poisonAttack() == 1).count());
        assertEquals(260, definitions.stream().filter(definition ->
                definition.template().poisonAttack() == 1).mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(5, definitions.stream().filter(definition ->
                definition.template().poisonAttack() == 2).count());
        assertEquals(7, definitions.stream().filter(definition ->
                definition.template().poisonAttack() == 2).mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(0, definitions.stream().filter(definition ->
                definition.template().poisonAttack() == 4).count());

        assertEquals(9, definitions.stream().filter(definition ->
                definition.template().paralysisAttack() == 1).count());
        assertEquals(72, definitions.stream().filter(definition ->
                definition.template().paralysisAttack() == 1).mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(9, definitions.stream().filter(definition ->
                definition.template().paralysisAttack() == 2).count());
        assertEquals(42, definitions.stream().filter(definition ->
                definition.template().paralysisAttack() == 2).mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());

        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot scorpion =
                population.actor(4, 0x6500_0D8EL);
        assertNotNull(scorpion);
        assertEquals(524, scorpion.spawnId());
        assertEquals(2, scorpion.spawnNumber());
        assertEquals(45259, scorpion.npcId());
        assertEquals(1, scorpion.template().poisonAttack());
        assertEquals(33307, scorpion.x());
        assertEquals(32347, scorpion.y());
    }

    @Test
    void retainedProjectileRangeAndStaticArrowLineOfSightGateTheAttack() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot actor = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45270);
        assertNotNull(actor);
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(4);
        int[] clear = null;
        int[] blocked = null;
        for (int x = actor.x() - 10; x <= actor.x() + 10; x++) {
            for (int y = actor.y() - 10; y <= actor.y() + 10; y++) {
                int manhattan = Math.abs(x - actor.x()) + Math.abs(y - actor.y());
                int chebyshev = Math.max(Math.abs(x - actor.x()),
                        Math.abs(y - actor.y()));
                if (manhattan < 2 || manhattan > actor.template().attackRange()
                        || !map.standingPassable(x, y)
                        || population.occupies(4, x, y)) continue;
                if (map.attackPosition(actor.x(), actor.y(), x, y,
                        actor.template().attackRange())) {
                    if (clear == null) clear = new int[] {x, y};
                } else if (chebyshev <= actor.template().attackRange()
                        && !map.hasLineOfSight(actor.x(), actor.y(), x, y)) {
                    if (blocked == null) blocked = new int[] {x, y};
                }
            }
        }
        assertNotNull(clear);
        assertNotNull(blocked);
        assertEquals(33264, clear[0]);
        assertEquals(32337, clear[1]);
        assertEquals(33256, blocked[0]);
        assertEquals(32345, blocked[1]);
        assertFalse(map.attackPosition(actor.x(), actor.y(),
                actor.x() - 8, actor.y() - 8, actor.template().attackRange()));

        long playerObjectId = 15;
        assertTrue(population.provoke(4, actor.objectId(), playerObjectId));
        ClassicWorldMonsterPopulation.Attack attack = population.attack(
                4, actor.objectId(), playerObjectId, clear[0], clear[1],
                3_000_000_000L);
        assertNotNull(attack);
        assertTrue(attack.projectile());
        assertEquals(clear[0], attack.targetX());
        assertEquals(clear[1], attack.targetY());

        ClassicWorldMonsterPopulation blockedPopulation =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot blockedActor =
                blockedPopulation.actor(4, actor.objectId());
        assertNotNull(blockedActor);
        assertTrue(blockedPopulation.provoke(
                4, blockedActor.objectId(), playerObjectId));
        assertEquals(null, blockedPopulation.attack(
                4, blockedActor.objectId(), playerObjectId,
                blocked[0], blocked[1], 3_000_000_000L));
    }

    @Test
    void liberatedPetReservationBecomesOneShotWildMonsterWithItsBag() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        int x = ClassicWorldMapCatalog.WORLD_TREE_X;
        int y = ClassicWorldMapCatalog.WORLD_TREE_Y;
        while (population.occupies(4, x, y)) x++;
        GameStateStore.PetState durable = new GameStateStore.PetState(
                "legacy\u001f10231", 0x7000_3810L, 0x9000_0001L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 30, 120, 24, 60_000, 0);
        BattleCompanion pet = BattleCompanion.pet(
                durable, SourcePetTemplate.memoryFixture(45034), x, y);
        RuntimeInventory.Item fruit = new RuntimeInventory.Item(
                0x7000_7777L, InventoryCatalog.require(
                        InventoryCatalog.EVOLUTION_FRUIT), 2, false);
        assertTrue(pet.receiveItem(fruit, 2));

        ClassicWorldMonsterPopulation.Snapshot reserved =
                population.reserveReleasedPet(
                        4, pet.snapshot(), SourcePetTemplate.memoryFixture(45034),
                        pet.carriedItems());
        assertNotNull(reserved);
        assertFalse(reserved.present());
        assertTrue(population.occupies(4, x, y));
        ClassicWorldMonsterPopulation.Snapshot wild =
                population.commitReleasedPet(reserved.objectId());
        assertNotNull(wild);
        assertTrue(wild.present());
        assertEquals(durable.level(), wild.template().level());
        assertEquals(durable.maxHp(), wild.maximumHp());
        assertEquals(durable.maxMp(), wild.maximumMp());
        assertEquals(pet.snapshot().mp(), wild.currentMp());

        ClassicWorldMonsterPopulation.Hit death = population.damage(
                4, wild.objectId(), Integer.MAX_VALUE);
        assertTrue(death.diedNow());
        List<RuntimeInventory.Item> drops = population.takeReleasedPetItems(
                4, wild.objectId());
        assertEquals(1, drops.size());
        assertEquals(fruit.objectId(), drops.get(0).objectId());
        assertEquals(2, drops.get(0).quantity());
        assertTrue(population.takeReleasedPetItems(4, wild.objectId()).isEmpty());
        assertNotNull(population.removeCorpse(4, wild.objectId()));
        assertEquals(null, population.actor(4, wild.objectId()));
        assertEquals(-1, population.nextRespawnDelaySeconds(4, wild.objectId()));
    }

    @Test
    void deterministicProjectileTcpRouteAvoidsStaticActors() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(4);
        int startX = ClassicWorldMapCatalog.WORLD_TREE_X;
        int startY = ClassicWorldMapCatalog.WORLD_TREE_Y;
        int targetX = 33264;
        int targetY = 32337;
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        long start = ((long) startX << 32) | Integer.toUnsignedLong(startY);
        long target = ((long) targetX << 32) | Integer.toUnsignedLong(targetY);
        ArrayDeque<Long> pending = new ArrayDeque<>();
        Map<Long, Long> previous = new HashMap<>();
        Map<Long, Integer> direction = new HashMap<>();
        pending.add(start);
        previous.put(start, start);
        while (!pending.isEmpty() && !previous.containsKey(target)) {
            long current = pending.removeFirst();
            int x = (int) (current >> 32);
            int y = (int) current;
            for (int heading = 0; heading < 8; heading++) {
                int nextX = x + dx[heading];
                int nextY = y + dy[heading];
                long next = ((long) nextX << 32) | Integer.toUnsignedLong(nextY);
                if (previous.containsKey(next)
                        || !map.canStep(x, y, nextX, nextY)
                        || population.occupies(4, nextX, nextY)
                        || ClassicWorldNpcCatalog.fixedMerchantOccupies(
                                4, nextX, nextY)) continue;
                previous.put(next, current);
                direction.put(next, heading);
                pending.addLast(next);
            }
        }
        assertTrue(previous.containsKey(target));
        ArrayList<Integer> reversed = new ArrayList<>();
        for (long cell = target; cell != start; cell = previous.get(cell)) {
            reversed.add(direction.get(cell));
        }
        java.util.Collections.reverse(reversed);
        StringBuilder encoded = new StringBuilder();
        for (int heading : reversed) encoded.append(heading);
        assertEquals(221, reversed.size());
        assertEquals("00111111111111111111111322111111111131122111111111111111111111111111111111111111111111111111111111111111133333333333333333333333333333333333332222212113333333333333333333333333233333333333333333333333333333323212233333555",
                encoded.toString());
    }

    @Test
    void deterministicDamagePoisonTcpRouteAvoidsStaticActors() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(4);
        int startX = ClassicWorldMapCatalog.WORLD_TREE_X;
        int startY = ClassicWorldMapCatalog.WORLD_TREE_Y;
        int targetX = 33253;
        int targetY = 32415;
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        long start = ((long) startX << 32) | Integer.toUnsignedLong(startY);
        long target = ((long) targetX << 32) | Integer.toUnsignedLong(targetY);
        ArrayDeque<Long> pending = new ArrayDeque<>();
        Map<Long, Long> previous = new HashMap<>();
        Map<Long, Integer> direction = new HashMap<>();
        pending.add(start);
        previous.put(start, start);
        while (!pending.isEmpty() && !previous.containsKey(target)) {
            long current = pending.removeFirst();
            int x = (int) (current >> 32);
            int y = (int) current;
            for (int heading = 0; heading < 8; heading++) {
                int nextX = x + dx[heading];
                int nextY = y + dy[heading];
                long next = ((long) nextX << 32) | Integer.toUnsignedLong(nextY);
                if (previous.containsKey(next)
                        || !map.canStep(x, y, nextX, nextY)
                        || population.occupies(4, nextX, nextY)
                        || ClassicWorldNpcCatalog.fixedMerchantOccupies(
                                4, nextX, nextY)) continue;
                previous.put(next, current);
                direction.put(next, heading);
                pending.addLast(next);
            }
        }
        assertTrue(previous.containsKey(target));
        ArrayList<Integer> reversed = new ArrayList<>();
        for (long cell = target; cell != start; cell = previous.get(cell)) {
            reversed.add(direction.get(cell));
        }
        java.util.Collections.reverse(reversed);
        StringBuilder encoded = new StringBuilder();
        for (int heading : reversed) encoded.append(heading);
        assertEquals(204, reversed.size());
        assertEquals("001111111111111111111113221111111111311221111111111113332333332333333331333332333333332333333333323333333333333333333333333333333333333333332332121333333333333333333333333333333333333332223222332311123333",
                encoded.toString());
    }

    @Test
    void deterministicDamagePoisonFixtureHasBoundedAggressiveScreen() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot actor =
                population.actor(4, 0x6500_0DBDL);
        assertNotNull(actor);
        assertEquals(525, actor.spawnId());
        assertEquals(13, actor.spawnNumber());
        assertEquals(45259, actor.npcId());
        assertEquals(1, actor.template().poisonAttack());
        assertTrue(actor.template().aggressive());
        assertEquals(33254, actor.x());
        assertEquals(32415, actor.y());
        assertEquals(6, population.inScreen(4, actor.x(), actor.y()).stream()
                .filter(row -> row.template().aggressive()).count());
    }

    @Test void armorBreakLeaseRefreshesExpiresCancelsAndDiesWithWorldMonster() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot target = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        assertNotNull(target);
        long now = 9_100_000_000L;

        assertTrue(population.armorBreak(
                4, target.objectId(), now, 8));
        assertTrue(population.armorBreakActive(
                4, target.objectId(), now + 7_999_999_999L));
        assertFalse(population.armorBreakActive(
                4, target.objectId(), now + 8_000_000_000L));

        assertTrue(population.armorBreak(
                4, target.objectId(), now + 9_000_000_000L, 8));
        assertTrue(population.armorBreak(
                4, target.objectId(), now + 10_000_000_000L, 8));
        assertTrue(population.armorBreakActive(
                4, target.objectId(), now + 17_999_999_999L));
        ClassicWorldMonsterPopulation.MagicCancellation cancellation =
                population.cancelMagicEffects(4, target.objectId());
        assertNotNull(cancellation);
        assertTrue(cancellation.armorBreak());
        assertFalse(population.armorBreakActive(
                4, target.objectId(), now + 18_000_000_000L));

        assertTrue(population.armorBreak(
                4, target.objectId(), now + 19_000_000_000L, 8));
        population.damage(4, target.objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.PLAYER,
                1, 1, target.maximumHp());
        assertFalse(population.armorBreakActive(
                4, target.objectId(), now + 19_000_000_001L));
    }

    private static int ordinal(int spawnId, int slot) {
        return ordinal(4, spawnId, slot);
    }

    private static int ordinal(int mapId, int spawnId, int slot) {
        int result = slot;
        for (int candidateMapId : ClassicWorldMapCatalog.supportedMapIds()) {
            if (candidateMapId == mapId) {
                for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                        : ClassicWorldMonsterCatalog.definitions(candidateMapId)) {
                    if (definition.spawnId() == spawnId) return result;
                    result += definition.count();
                }
                break;
            }
            result += ClassicWorldMonsterCatalog.definitions(candidateMapId).stream()
                    .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum();
        }
        throw new IllegalArgumentException("unknown map/spawn " + mapId + ":" + spawnId);
    }

}
