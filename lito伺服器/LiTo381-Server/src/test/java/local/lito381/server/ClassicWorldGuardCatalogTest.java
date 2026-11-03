package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class ClassicWorldGuardCatalogTest {
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    @Test
    void importsEveryLiveSupportedGuardAndLeavesZeroHpDecorationsStatic() {
        assertEquals(ClassicWorldGuardCatalog.POPULATION,
                ClassicWorldGuardCatalog.all().size());
        assertEquals(ClassicWorldGuardCatalog.POPULATION_BY_MAP,
                ClassicWorldGuardCatalog.all().stream().collect(
                        Collectors.groupingBy(
                                ClassicWorldNpcCatalog.StaticNpc::mapId,
                                Collectors.summingInt(ignored -> 1))));
        assertEquals(44, ClassicWorldGuardCatalog.all().stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::npcId)
                .distinct().count());
        assertTrue(ClassicWorldGuardCatalog.all().stream()
                .allMatch(npc -> npc.hp() > 0
                        && npc.implementation().equals("L1Guard")));

        long staticZeroHp = ClassicWorldMapCatalog.supportedMapIds().stream()
                .flatMap(mapId -> ClassicWorldNpcCatalog.all(mapId).stream())
                .filter(npc -> npc.implementation().equals("L1Guard"))
                .filter(npc -> npc.hp() == 0)
                .count();
        assertEquals(ClassicWorldGuardCatalog.STATIC_ZERO_HP_POPULATION,
                staticZeroHp);
        ClassicWorldNpcCatalog.StaticNpc decoration =
                ClassicWorldMapCatalog.supportedMapIds().stream()
                        .flatMap(mapId -> ClassicWorldNpcCatalog.all(mapId).stream())
                        .filter(npc -> npc.implementation().equals("L1Guard"))
                        .filter(npc -> npc.hp() == 0)
                        .findFirst().orElseThrow();
        assertFalse(ClassicWorldGuardCatalog.isGuard(decoration));
        assertTrue(ClassicWorldNpcCatalog.occupies(
                decoration.mapId(), decoration.x(), decoration.y()));
    }

    @Test
    void exactCommonStatsRangesProjectilesAndStableRuntimePopulationHold() {
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldGuardCatalog.definitions()) {
            ClassicWorldMonsterCatalog.MonsterTemplate npc =
                    definition.template();
            assertTrue(npc.guard());
            assertEquals(0, npc.armorClass());
            assertEquals(126, npc.strength());
            assertEquals(126, npc.constitution());
            assertEquals(126, npc.dexterity());
            assertEquals(126, npc.wisdom());
            assertEquals(126, npc.intelligence());
            assertEquals(0, npc.magicResistance());
            assertEquals(0, npc.experience());
            assertEquals("large", npc.size());
            assertEquals(0, definition.minRespawnSeconds());
            assertEquals(0, definition.maxRespawnSeconds());
            assertEquals(ClassicWorldNpcCatalog.objectId(definition.spawnId()),
                    ClassicWorldGuardCatalog.all().stream()
                            .filter(source -> source.spawnId()
                                    == definition.spawnId())
                            .findFirst().orElseThrow().objectId());
        }
        ClassicWorldMonsterCatalog.MonsterTemplate bow =
                template(60501);
        assertEquals(10, bow.attackRange());
        assertEquals(66, bow.bowActionId());
        assertFalse(bow.projectileAtDistance(1));
        assertTrue(bow.projectileAtDistance(2));
        ClassicWorldMonsterCatalog.MonsterTemplate melee = template(60530);
        assertEquals(1, melee.attackRange());
        assertEquals(0, melee.bowActionId());
        assertFalse(melee.projectileAtDistance(2));
        assertTrue(template(60560).aggressive());
        assertEquals(4, template(60560).weakAttribute());
        assertFalse(template(60501).aggressive());

        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                population.size());
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION
                        + ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION
                        + ClassicWorldGuardianCatalog.POPULATION
                        + ClassicWorldGuardCatalog.POPULATION,
                population.runtimeActorCount());
        for (ClassicWorldNpcCatalog.StaticNpc source
                : ClassicWorldGuardCatalog.all()) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    population.actor(source.mapId(), source.objectId());
            assertNotNull(actor);
            assertTrue(ClassicWorldGuardCatalog.isGuard(actor));
            assertFalse(ClassicWorldNpcCatalog.occupies(
                    source.mapId(), source.x(), source.y()));
        }
    }

    @Test
    void retainedCastleTalkMatrixIncludesEveryGateAndGuardBranch() {
        assertTalk(70549, 1, "gateokeeper", "gatekeeperop",
                ClassicWorldGuardCatalog.TalkArguments.PLAYER_NAME);
        assertTalk(70600, 2, "orckeeper", "orckeeperop",
                ClassicWorldGuardCatalog.TalkArguments.NONE);
        assertTalk(70778, 3, "gatekeeper", "gatekeeperop",
                ClassicWorldGuardCatalog.TalkArguments.PLAYER_NAME);
        assertTalk(70800, 4, "gateokeeper", "gatekeeperop",
                ClassicWorldGuardCatalog.TalkArguments.PLAYER_NAME);
        assertTalk(70863, 5, "gatekeeper", "gatekeeperop",
                ClassicWorldGuardCatalog.TalkArguments.PLAYER_NAME);
        assertTalk(70995, 6, "gatekeeper", "gatekeeperop",
                ClassicWorldGuardCatalog.TalkArguments.PLAYER_NAME);
        assertTalk(70996, 7, "gatekeeper", "gatekeeperop",
                ClassicWorldGuardCatalog.TalkArguments.PLAYER_NAME);
        assertTalk(60514, 1, "ktguard6", "ktguard6",
                ClassicWorldGuardCatalog.TalkArguments.CASTLE_IDENTITY);
        assertTalk(60560, 2, "orcguard6", "orcguard6",
                ClassicWorldGuardCatalog.TalkArguments.CASTLE_IDENTITY);
        assertTalk(60552, 3, "wdguard6", "wdguard6",
                ClassicWorldGuardCatalog.TalkArguments.CASTLE_IDENTITY);
        assertTalk(60524, 4, "grguard6", "grguard6",
                ClassicWorldGuardCatalog.TalkArguments.CASTLE_IDENTITY);
        assertTalk(70857, 5, "heguard6", "heguard6",
                ClassicWorldGuardCatalog.TalkArguments.CASTLE_IDENTITY);
        assertTalk(60530, 6, "dcguard6", "dcguard6",
                ClassicWorldGuardCatalog.TalkArguments.CASTLE_IDENTITY);
        assertTalk(60533, 7, "adguard6", "adguard6",
                ClassicWorldGuardCatalog.TalkArguments.CASTLE_IDENTITY);
        assertNull(ClassicWorldGuardCatalog.talkProfile(60501));
    }

    @Test
    void noTargetWalksHomeAndPathFailureUsesTwelveTileSafeTeleport() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        MoveChoice choice = movableGuard(population);
        long targetId = 0x7000_0001L;
        long now = 10_000_000_000L;
        assertTrue(population.provoke(
                choice.actor().mapId(), choice.actor().objectId(), targetId));
        int targetX = choice.actor().x() + DX[choice.heading()] * 6;
        int targetY = choice.actor().y() + DY[choice.heading()] * 6;
        ClassicWorldMonsterPopulation.Movement pursuit = population.moveToward(
                choice.actor().mapId(), choice.actor().objectId(),
                targetId, targetX, targetY, Set.of(), now);
        assertNotNull(pursuit);
        population.clearTarget(
                choice.actor().mapId(), choice.actor().objectId(), targetId);

        ClassicWorldMonsterPopulation.GuardHomecoming walked =
                population.returnGuardHome(
                        choice.actor().mapId(), choice.actor().objectId(),
                        Set.of(), now + 10_000_000_000L);
        assertNotNull(walked);
        assertFalse(walked.teleported());
        assertEquals(choice.actor().definition().locX(), walked.after().x());
        assertEquals(choice.actor().definition().locY(), walked.after().y());
        assertTrue(population.guardsNeedingHome(choice.actor().mapId()).stream()
                .noneMatch(actor -> actor.objectId() == choice.actor().objectId()));

        long later = now + 20_000_000_000L;
        assertTrue(population.provoke(
                choice.actor().mapId(), choice.actor().objectId(), targetId));
        pursuit = population.moveToward(
                choice.actor().mapId(), choice.actor().objectId(),
                targetId, targetX, targetY, Set.of(), later);
        assertNotNull(pursuit);
        population.clearTarget(
                choice.actor().mapId(), choice.actor().objectId(), targetId);
        Set<ClassicWorldMonsterPopulation.GridPoint> blocked =
                new LinkedHashSet<>();
        for (int heading = 0; heading < 8; heading++) {
            blocked.add(new ClassicWorldMonsterPopulation.GridPoint(
                    pursuit.after().x() + DX[heading],
                    pursuit.after().y() + DY[heading]));
        }
        ClassicWorldMonsterPopulation.GuardHomecoming teleported =
                population.returnGuardHome(
                        choice.actor().mapId(), choice.actor().objectId(),
                        blocked, later + 10_000_000_000L);
        assertNotNull(teleported);
        assertTrue(teleported.teleported());
        assertEquals(1, teleported.after().heading());
        assertTrue(Math.max(
                Math.abs(teleported.after().x()
                        - choice.actor().definition().locX()),
                Math.abs(teleported.after().y()
                        - choice.actor().definition().locY())) <= 12);
    }

    @Test
    void deathCorpseRemovalAndSourceZeroDelayRespawnRestoreGuardHome() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(22L);
        ClassicWorldNpcCatalog.StaticNpc source =
                ClassicWorldGuardCatalog.all().stream()
                        .filter(row -> row.npcId() == 60530)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterPopulation.Snapshot before =
                population.actor(source.mapId(), source.objectId());
        ClassicWorldMonsterPopulation.Hit death = population.damage(
                source.mapId(), source.objectId(), 0x7000_0002L,
                before.currentHp());
        assertNotNull(death);
        assertTrue(death.diedNow());
        assertTrue(death.after().present());
        assertFalse(death.after().alive());

        ClassicWorldMonsterPopulation.Snapshot removed =
                population.removeCorpse(source.mapId(), source.objectId());
        assertNotNull(removed);
        assertFalse(removed.present());
        assertEquals(0, population.nextRespawnDelaySeconds(
                source.mapId(), source.objectId()));
        ClassicWorldMonsterPopulation.Respawn respawn = population.respawn(
                source.mapId(), source.objectId());
        assertNotNull(respawn);
        assertTrue(respawn.after().alive());
        assertTrue(respawn.after().present());
        assertEquals(source.hp(), respawn.after().currentHp());
        assertEquals(source.x(), respawn.after().x());
        assertEquals(source.y(), respawn.after().y());
        assertEquals(source.heading(), respawn.after().heading());
        assertEquals(0, population.targetObjectId(
                source.mapId(), source.objectId()));
    }

    private static ClassicWorldMonsterCatalog.MonsterTemplate template(int npcId) {
        return ClassicWorldGuardCatalog.definitions().stream()
                .map(ClassicWorldMonsterCatalog.SpawnDefinition::template)
                .filter(template -> template.npcId() == npcId)
                .findFirst().orElseThrow();
    }

    private static void assertTalk(
            int npcId, int castleId, String member, String outsider,
            ClassicWorldGuardCatalog.TalkArguments arguments) {
        ClassicWorldGuardCatalog.TalkProfile profile =
                ClassicWorldGuardCatalog.talkProfile(npcId);
        assertNotNull(profile);
        assertEquals(castleId, profile.castleId());
        assertEquals(member, profile.memberHtml());
        assertEquals(outsider, profile.outsiderHtml());
        assertEquals(arguments, profile.arguments());
    }

    private static MoveChoice movableGuard(
            ClassicWorldMonsterPopulation population) {
        for (ClassicWorldNpcCatalog.StaticNpc source
                : ClassicWorldGuardCatalog.all()) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    population.actor(source.mapId(), source.objectId());
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(source.mapId());
            for (int heading = 0; heading < 8; heading++) {
                int x = actor.x() + DX[heading];
                int y = actor.y() + DY[heading];
                int targetX = actor.x() + DX[heading] * 6;
                int targetY = actor.y() + DY[heading] * 6;
                ClassicWorldNpcPathfinder.Result path =
                        ClassicWorldNpcPathfinder.nextStep(
                                map, actor.x(), actor.y(), targetX, targetY,
                                actor.template().attackRange(),
                                (candidateX, candidateY) ->
                                        population.occupies(
                                                source.mapId(),
                                                candidateX, candidateY)
                                                || ClassicWorldNpcCatalog.occupies(
                                                        source.mapId(),
                                                        candidateX, candidateY));
                if (map.canStep(actor.x(), actor.y(), x, y)
                        && !population.occupies(source.mapId(), x, y)
                        && !ClassicWorldNpcCatalog.occupies(
                                source.mapId(), x, y)
                        && path.moved()) {
                    return new MoveChoice(actor, heading);
                }
            }
        }
        throw new AssertionError("no movable selected L1Guard");
    }

    private record MoveChoice(
            ClassicWorldMonsterPopulation.Snapshot actor, int heading) {}
}
