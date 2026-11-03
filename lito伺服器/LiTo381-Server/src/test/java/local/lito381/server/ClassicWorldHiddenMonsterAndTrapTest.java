package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClassicWorldHiddenMonsterAndTrapTest {
    private static final Set<Integer> SINK_NPCS = Set.of(
            45061, 45161, 45181, 45455,
            45045, 45126, 45134, 45281, 46107, 46108);
    private static final Set<Integer> FLY_NPCS = Set.of(
            45067, 45264, 45452, 45090, 45321, 45445, 45681);
    private static final Set<Integer> ICE_NPCS = Set.of(
            46125, 46126, 46127, 46128);

    @Test
    void selectedSinkActorsStartHiddenAndRevealAtomically() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot hidden = null;
        for (long id = ClassicWorldMonsterPopulation.OBJECT_BASE + 1;
             id < ClassicWorldMonsterPopulation.OBJECT_BASE
                     + population.runtimeActorCount() + 1L; id++) {
            for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(mapId, id);
                if (actor != null && SINK_NPCS.contains(actor.npcId())
                        && actor.hiddenStatus()
                            == ClassicWorldMonsterPopulation.HIDDEN_STATUS_SINK) {
                    hidden = actor;
                    break;
                }
            }
            if (hidden != null) break;
        }
        assertNotNull(hidden);
        assertEquals(0, hidden.spawnNumber() % 3);
        assertTrue(hidden.actorStatus() == 4 || hidden.actorStatus() == 13);
        assertNull(population.damage(
                hidden.mapId(), hidden.objectId(), 777L, 1));
        assertFalse(population.actionReady(
                hidden.mapId(), hidden.objectId(), Long.MAX_VALUE));

        List<ClassicWorldMonsterPopulation.Reveal> reveals =
                population.revealSinkInScreen(
                        hidden.mapId(), hidden.x(), hidden.y(), 777L);
        long hiddenObjectId = hidden.objectId();
        ClassicWorldMonsterPopulation.Reveal reveal = reveals.stream()
                .filter(row -> row.after().objectId() == hiddenObjectId)
                .findFirst().orElseThrow();
        assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE,
                reveal.after().hiddenStatus());
        assertEquals(0, reveal.after().actorStatus());
        assertEquals(777L, population.targetObjectId(
                hidden.mapId(), hidden.objectId()));
        assertNotNull(population.damage(
                hidden.mapId(), hidden.objectId(), 777L, 1));
    }

    @Test
    void npcPackCarriesRetainedSinkActorStatus() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot hidden = null;
        outer: for (long id = ClassicWorldMonsterPopulation.OBJECT_BASE + 1;
             id < ClassicWorldMonsterPopulation.OBJECT_BASE
                     + population.runtimeActorCount() + 1L; id++) {
            for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(mapId, id);
                if (actor != null && actor.hiddenStatus()
                        == ClassicWorldMonsterPopulation.HIDDEN_STATUS_SINK) {
                    hidden = actor;
                    break outer;
                }
            }
        }
        assertNotNull(hidden);
        byte[] packet = Protocol.classicWorldMonster(hidden);
        assertEquals(hidden.actorStatus(), packet[11] & 0xFF);
    }

    @Test
    void selectedFlyingActorsStartAirborneAndLandOnlyAtRadiusOne() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        java.util.ArrayList<ClassicWorldMonsterPopulation.Snapshot> flying =
                new java.util.ArrayList<>();
        for (long id = ClassicWorldMonsterPopulation.OBJECT_BASE + 1;
             id < ClassicWorldMonsterPopulation.OBJECT_BASE
                     + population.runtimeActorCount() + 1L; id++) {
            for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(mapId, id);
                if (actor != null && FLY_NPCS.contains(actor.npcId())) {
                    flying.add(actor);
                }
            }
        }
        assertEquals(72, flying.size());
        assertTrue(flying.stream().allMatch(actor ->
                actor.hiddenStatus()
                    == ClassicWorldMonsterPopulation.HIDDEN_STATUS_FLY
                        && actor.actorStatus() == 4));

        ClassicWorldMonsterPopulation.Snapshot hidden = flying.get(0);
        assertEquals(4, Protocol.classicWorldMonster(hidden)[11] & 0xFF);
        assertNull(population.damage(
                hidden.mapId(), hidden.objectId(), 888L, 1));
        assertFalse(population.actionReady(
                hidden.mapId(), hidden.objectId(), Long.MAX_VALUE));
        assertTrue(population.revealFlyNear(
                hidden.mapId(), hidden.x() + 2, hidden.y(), 888L).isEmpty());

        ClassicWorldMonsterPopulation.Reveal reveal =
                population.revealFlyNear(
                        hidden.mapId(), hidden.x() + 1, hidden.y(), 888L)
                        .stream()
                        .filter(row -> row.after().objectId() == hidden.objectId())
                        .findFirst().orElseThrow();
        assertEquals(BattleActionCodes.MOVE_DOWN, reveal.actionId());
        assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE,
                reveal.after().hiddenStatus());
        assertEquals(0, reveal.after().actorStatus());
        assertEquals(888L, population.targetObjectId(
                hidden.mapId(), hidden.objectId()));
        assertNotNull(population.damage(
                hidden.mapId(), hidden.objectId(), 888L, 1));
    }

    @Test
    void damagedFlyingActorLandsForPotionWithoutProvokingObserver()
            throws Exception {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot flying = null;
        outer: for (long id = ClassicWorldMonsterPopulation.OBJECT_BASE + 1;
             id < ClassicWorldMonsterPopulation.OBJECT_BASE
                     + population.runtimeActorCount() + 1L; id++) {
            for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(mapId, id);
                if (actor != null && actor.hiddenStatus()
                        == ClassicWorldMonsterPopulation.HIDDEN_STATUS_FLY) {
                    flying = actor;
                    break outer;
                }
            }
        }
        assertNotNull(flying);
        setCurrentHp(population, flying.objectId(), flying.maximumHp() - 1);
        ClassicWorldMonsterPopulation.Snapshot damaged = population.actor(
                flying.mapId(), flying.objectId());
        assertTrue(population.revealFlyNear(
                damaged.mapId(), damaged.x() + 1, damaged.y(), 991L).isEmpty());
        assertEquals(0, population.targetObjectId(
                damaged.mapId(), damaged.objectId()));

        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item adena = ground.put(
                damaged.mapId(), -1,
                InventoryCatalog.builtIn(InventoryCatalog.ADENA), 1,
                damaged.x() + 1, damaged.y(), damaged.npcId());
        ClassicWorldGroundItems.Item potion = ground.put(
                damaged.mapId(), -1,
                InventoryCatalog.builtIn(
                        InventoryCatalog.NATIVE_HEALING_POTION), 1,
                damaged.x() + 1, damaged.y(), damaged.npcId());
        assertNotNull(adena);
        assertNotNull(potion);

        ClassicWorldMonsterPopulation.AirItemLanding landing =
                population.searchItemFromAir(
                        damaged.mapId(), damaged.objectId(),
                        ground.inScreen(
                                damaged.mapId(), -1,
                                damaged.x(), damaged.y()), 0);
        assertNotNull(landing);
        assertEquals(BattleActionCodes.MOVE_DOWN, landing.actionId());
        assertEquals(potion.objectId(), landing.targetItemObjectId());
        assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE,
                landing.after().hiddenStatus());
        assertEquals(0, population.targetObjectId(
                damaged.mapId(), damaged.objectId()));
        ClassicWorldMonsterPopulation.ItemAction action = population.itemAction(
                damaged.mapId(), damaged.objectId(),
                ground.inScreen(
                        damaged.mapId(), -1, damaged.x(), damaged.y()),
                Set.of(), 1L, 0);
        assertNotNull(action);
        assertEquals(ClassicWorldMonsterPopulation.ItemActionKind.PICKUP_READY,
                action.kind());
        assertEquals(potion.objectId(), action.targetItemObjectId());
    }

    @SuppressWarnings("unchecked")
    private static void setCurrentHp(
            ClassicWorldMonsterPopulation population, long objectId, int hp)
            throws Exception {
        Field actorsField = ClassicWorldMonsterPopulation.class
                .getDeclaredField("actors");
        actorsField.setAccessible(true);
        Object actor = ((Map<Long, Object>) actorsField.get(population))
                .get(objectId);
        Field hpField = actor.getClass().getDeclaredField("currentHp");
        hpField.setAccessible(true);
        hpField.setInt(actor, hp);
    }

    @Test
    void retainedIceActorIsTargetableButRevealsOnlyAfterDamage() {
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                isolatedIceDefinition();
        ClassicWorldMonsterPopulation population =
                ClassicWorldMonsterPopulation.retainedSourceSlice(
                        21L, 0x6600_0000L, 0x6600_1000L,
                        List.of(definition));
        ClassicWorldMonsterPopulation.Snapshot frozen = population.actor(
                definition.mapId(), 0x6600_0001L);
        assertNotNull(frozen);
        assertTrue(ICE_NPCS.contains(frozen.npcId()));
        assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_ICE,
                frozen.hiddenStatus());
        assertEquals(4, frozen.actorStatus());
        assertEquals(4, Protocol.classicWorldMonster(frozen)[11] & 0xFF);
        assertFalse(population.actionReady(
                frozen.mapId(), frozen.objectId(), Long.MAX_VALUE));
        assertTrue(population.revealDamagedIceInScreen(
                frozen.mapId(), frozen.x(), frozen.y(), 777L).isEmpty());

        ClassicWorldMonsterPopulation.Hit hit = population.damage(
                frozen.mapId(), frozen.objectId(), 888L, 1);
        assertNotNull(hit);
        assertEquals(frozen.maximumHp() - 1, hit.after().currentHp());
        assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_ICE,
                hit.after().hiddenStatus());
        assertTrue(population.provoke(
                frozen.mapId(), frozen.objectId(), 888L));

        ClassicWorldMonsterPopulation.Reveal reveal =
                population.revealDamagedIceInScreen(
                        frozen.mapId(), frozen.x(), frozen.y(), 999L)
                        .stream().findFirst().orElseThrow();
        assertEquals(BattleActionCodes.APPEAR, reveal.actionId());
        assertEquals(888L, reveal.provokedObjectId());
        assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE,
                reveal.after().hiddenStatus());
        assertEquals(0, reveal.after().actorStatus());
        assertEquals(888L, population.targetObjectId(
                frozen.mapId(), frozen.objectId()));
        assertTrue(population.actionReady(
                frozen.mapId(), frozen.objectId(), Long.MAX_VALUE));
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition
            isolatedIceDefinition() {
        ClassicWorldMonsterCatalog.SpawnDefinition source =
                ClassicWorldMonsterCatalog.definitions(4).get(0);
        ClassicWorldMonsterCatalog.MonsterTemplate template = source.template();
        ClassicWorldMonsterCatalog.MonsterTemplate ice =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        46125, "$1789", template.implementation(),
                        template.gfxId(), template.level(),
                        Math.max(10, template.hp()),
                        template.mp(), template.armorClass(), template.strength(),
                        template.constitution(), template.dexterity(),
                        template.wisdom(), template.intelligence(),
                        template.magicResistance(), template.experience(),
                        template.lawful(), template.size(), template.weakAttribute(),
                        template.ranged(), template.passispeed(),
                        template.atkspeed(), template.bowActionId(),
                        template.undead(), template.poisonAttack(),
                        template.paralysisAttack(), template.aggressive(),
                        template.damageReduction(), template.hard(),
                        template.doppel(), template.boss(), template.lightSize(),
                        template.changeHead());
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                source.mapId(), 990_001, "isolated retained ICE probe", 1,
                source.groupId(), source.locX(), source.locY(),
                source.randomX(), source.randomY(), source.locX1(),
                source.locY1(), source.locX2(), source.locY2(), source.heading(),
                source.minRespawnSeconds(), source.maxRespawnSeconds(),
                source.respawnScreen(), source.movementDistance(), source.rest(),
                source.nearSpawn(), ice);
    }

    @Test
    void selectedTrapResourceAndDetectionLifecycleAreComplete() {
        assertEquals(ClassicWorldTrapPopulation.DEFINITION_COUNT,
                ClassicWorldTrapPopulation.definitions().size());
        assertEquals(ClassicWorldTrapPopulation.POPULATION,
                ClassicWorldTrapPopulation.definitions().stream()
                        .mapToInt(ClassicWorldTrapPopulation.Definition::count).sum());
        assertEquals(ClassicWorldTrapPopulation.DETECTIONABLE_POPULATION,
                ClassicWorldTrapPopulation.definitions().stream()
                        .filter(ClassicWorldTrapPopulation.Definition::detectionable)
                        .mapToInt(ClassicWorldTrapPopulation.Definition::count).sum());

        ClassicWorldTrapPopulation population =
                new ClassicWorldTrapPopulation(381L);
        ClassicWorldTrapPopulation.Snapshot trap = population.snapshots(1L).stream()
                .filter(row -> row.mapId() == 104 && row.detectionable())
                .findFirst().orElseThrow();
        ClassicWorldTrapPopulation.Detection detected = population.detect(
                trap.mapId(), trap.x(), trap.y(), 2L);
        assertTrue(detected.activated().stream().anyMatch(row ->
                row.spawnId() == trap.spawnId()
                        && row.spawnNumber() == trap.spawnNumber()));
        assertTrue(detected.effects().stream().anyMatch(row ->
                row.spawnId() == trap.spawnId()
                        && row.gfxId() == trap.gfxId()));
        assertFalse(population.snapshots(3L).stream()
                .filter(row -> row.spawnId() == trap.spawnId()
                        && row.spawnNumber() == trap.spawnNumber())
                .findFirst().orElseThrow().enabled());
    }

    @Test
    void nondetectionableIvoryTrapActivatesWithoutEffect() {
        ClassicWorldTrapPopulation population =
                new ClassicWorldTrapPopulation(381L);
        ClassicWorldTrapPopulation.Snapshot trap = population.snapshots(1L).stream()
                .filter(row -> row.mapId() == 78 && !row.detectionable())
                .findFirst().orElseThrow();
        ClassicWorldTrapPopulation.Detection detected = population.detect(
                trap.mapId(), trap.x(), trap.y(), 2L);
        assertFalse(detected.activated().isEmpty());
        assertTrue(detected.effects().isEmpty());
    }

    @Test
    void map104EntryHasDeterministicSourceBackedDetectionContext() {
        ClassicWorldTrapPopulation population = new ClassicWorldTrapPopulation();
        ClassicWorldTrapPopulation.Detection detected = population.detect(
                104, 32671, 32860, 1L);
        assertEquals(5, detected.activated().size());
        assertEquals(4, detected.effects().size());
        assertEquals(Set.of(1066, 1085), detected.effects().stream()
                .map(ClassicWorldTrapPopulation.Snapshot::gfxId)
                .collect(java.util.stream.Collectors.toSet()));
    }
}
