package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Production scheduler proof for retained passive-red and hidden hate semantics. */
final class ClientSessionClassicWorldMonsterAggressionIntegrationTest {
    private static final int MAP_ID = 4;
    private static final long PLAYER_OBJECT_ID = 0x4200_0404L;

    @TempDir Path temporary;

    @Test
    void completelyPassiveMonsterIgnoresNeutralButAcquiresChaoticPlayer()
            throws Exception {
        try (Fixture neutral = fixture(45009, 0, false)) {
            assertTrue(ClassicWorldMonsterAggressionCatalog
                    .forNpc(45009).completelyPassive());
            neutral.tick();
            assertEquals(0, neutral.population.targetObjectId(
                    MAP_ID, neutral.candidate.actor().objectId()));
        }
        try (Fixture chaotic = fixture(45009, -1001, false)) {
            chaotic.tick();
            assertEquals(PLAYER_OBJECT_ID, chaotic.population.targetObjectId(
                    MAP_ID, chaotic.candidate.actor().objectId()));
        }
    }

    @Test
    void provokedMonsterRetainsInvisiblePlayerAlreadyInHateList()
            throws Exception {
        try (Fixture fixture = fixture(45278, 0, true)) {
            assertTrue(fixture.population.provoke(MAP_ID,
                    fixture.candidate.actor().objectId(), PLAYER_OBJECT_ID));
            fixture.tick();
            assertEquals(PLAYER_OBJECT_ID, fixture.population.targetObjectId(
                    MAP_ID, fixture.candidate.actor().objectId()));
        }
    }

    @Test
    void schedulerPromotesNextHighestHateWhenCurrentTargetIsInvalid()
            throws Exception {
        try (Fixture fixture = fixture(45278, 0, false)) {
            long actorObjectId = fixture.candidate.actor().objectId();
            int firstAttackBonus = fixture.candidate.actor().maximumHp() / 10;
            assertNotNull(fixture.population.damage(
                    MAP_ID, actorObjectId, PLAYER_OBJECT_ID, 1));
            long invalidObjectId = 0x4200_FFFEL;
            assertNotNull(fixture.population.damage(
                    MAP_ID, actorObjectId, invalidObjectId,
                    firstAttackBonus + 2));
            assertEquals(invalidObjectId, fixture.population.targetObjectId(
                    MAP_ID, actorObjectId));

            fixture.tick();

            assertEquals(PLAYER_OBJECT_ID, fixture.population.targetObjectId(
                    MAP_ID, actorObjectId));
        }
    }

    @Test
    void chaoticPlayerMakesPassiveRabbitUseNativeEscapeMovementBranch()
            throws Exception {
        assertTrue(ClassicWorldMonsterAggressionCatalog
                .forNpc(45010).completelyPassive());
        try (Fixture fixture = fixture(45010, -1001, false, true)) {
            ClassicWorldMonsterPopulation.Snapshot before =
                    fixture.candidate.actor();
            fixture.tick();
            ClassicWorldMonsterPopulation.Snapshot after =
                    fixture.population.actor(MAP_ID, before.objectId());
            assertTrue(before.x() != after.x() || before.y() != after.y());
            assertTrue(chebyshev(after.x(), after.y(),
                    fixture.candidate.target().x(), fixture.candidate.target().y())
                    > chebyshev(before.x(), before.y(),
                        fixture.candidate.target().x(), fixture.candidate.target().y()));
            assertEquals(PLAYER_OBJECT_ID, fixture.population.targetObjectId(
                    MAP_ID, before.objectId()));
        }
    }

    @Test
    void neutralPlayerDrivesNativeNoTargetSchedulerAndMovePacket()
            throws Exception {
        try (Fixture fixture = fixture(45009, 0, false)) {
            ClassicWorldMonsterPopulation idle = idlePopulation(
                    fixture.candidate, fixture.candidate.target());
            ClassicWorldMonsterPopulation.Snapshot before = idle.actor(
                    MAP_ID, 0x6810_0001L);
            fixture.tick(idle, 1L);
            ClassicWorldMonsterPopulation.Snapshot after = idle.actor(
                    MAP_ID, before.objectId());
            assertTrue(before.x() != after.x() || before.y() != after.y());
            String frames = Files.readString(fixture.capture.path());
            assertTrue(frames.contains("\"opcode\":\"0x12\""));
            assertTrue(frames.contains(
                    "S_MOVE_OBJECT classic-world-monster=" + before.objectId()));
            assertEquals(0, idle.targetObjectId(MAP_ID, before.objectId()));
        }
    }

    @Test
    void movementDistanceSendsMoveEffectRemoveThenHomeRebuild()
            throws Exception {
        try (Fixture fixture = fixture(45009, 0, false)) {
            MovementFixture movementFixture = movementDistancePopulation(
                    fixture.candidate);
            ClassicWorldMonsterPopulation population =
                    movementFixture.population();
            long objectId = 0x6830_0001L;
            assertTrue(population.provoke(MAP_ID, objectId, PLAYER_OBJECT_ID));
            assertNotNull(population.moveToward(
                    MAP_ID, objectId, PLAYER_OBJECT_ID,
                    movementFixture.target().x(), movementFixture.target().y(),
                    Set.of(), 1L));
            ClassicWorldMonsterPopulation.Movement leash = population.moveToward(
                    MAP_ID, objectId, PLAYER_OBJECT_ID,
                    movementFixture.target().x(), movementFixture.target().y(),
                    Set.of(), 10_000_000_000L);
            assertNotNull(leash);
            assertTrue(leash.teleportedHome());
            fixture.session.serverClassicWorldMonsterMoved(leash);

            String frames = Files.readString(fixture.capture.path());
            int move = frames.indexOf(
                    "S_MOVE_OBJECT classic-world-monster=" + objectId);
            int effect = frames.indexOf(
                    "S_EFFECT classic-world-monster-home=" + objectId, move);
            int remove = frames.indexOf(
                    "S_REMOVE_OBJECT classic-world-monster-home=" + objectId,
                    effect);
            int put = frames.indexOf("monster-home-teleport-movement-distance",
                    remove);
            assertTrue(move >= 0 && move < effect && effect < remove
                    && remove < put);
        }
    }

    @Test
    void productionWorldCompanionClearsProvenUnreachableTargetOnTwentiethRetry()
            throws Exception {
        try (Fixture fixture = fixture(45278, 0, false)) {
            ClassicWorldMonsterPopulation.Snapshot target =
                    fixture.candidate.actor();
            ArenaMapCatalog.Point spawn = farPassableCompanionPoint(
                    fixture.population, target);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0776L, 45207, 2158, "$1791", 26,
                    137, 200, 20, -20, 17, 17, spawn.x(), spawn.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(45207));
            assertTrue(companion.selectTarget(target.objectId()));
            tamedCompanions(fixture.session).add(companion);

            long nowNanos = 1_000_000_000L;
            for (int attempt = 1;
                 attempt < BattleCompanion.PATH_FAIL_GIVE_UP;
                 attempt++) {
                fixture.session.serverClassicWorldCompanionTick(
                        MAP_ID, nowNanos);
                assertEquals(attempt, companion.pathFailureCount());
                assertEquals(target.objectId(),
                        companion.snapshot().targetObjectId());
                nowNanos += BattleCompanion.CROWD_RETRY_MILLIS * 1_000_000L;
            }

            fixture.session.serverClassicWorldCompanionTick(MAP_ID, nowNanos);
            assertEquals(0, companion.pathFailureCount());
            assertEquals(0, companion.snapshot().targetObjectId());
        }
    }

    @Test
    void worldCompanionPoisonsNpcBeforeEncryptedAttackAndTickKeepsPetOwner()
            throws Exception {
        try (Fixture fixture = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-npc-poison-selftest.properties"),
                45278, 0, false, false)) {
            ClassicWorldMonsterPopulation.Snapshot target =
                    fixture.candidate.actor();
            ArenaMapCatalog.Point point = fixture.candidate.target();
            fixture.packets();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0801L, 81217, 4205, "$3378", 28,
                    240, 240, 80, 0, 16, 14, point.x(), point.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81217));
            assertTrue(companion.selectTarget(target.objectId()));
            tamedCompanions(fixture.session).add(companion);

            long schedulerNow = System.nanoTime();
            companion.defer(schedulerNow, 60_000);
            long nowNanos = schedulerNow + 60_000_000_000L;
            long poisonStartedAt = 0;
            for (int attempt = 0; attempt < 100 && poisonStartedAt == 0;
                 attempt++, nowNanos += 2_000_000_000L) {
                fixture.session.serverClassicWorldCompanionTick(
                        MAP_ID, nowNanos);
                if (fixture.population.poisonActive(
                        MAP_ID, target.objectId(), nowNanos)) {
                    poisonStartedAt = nowNanos;
                }
            }

            assertTrue(poisonStartedAt > 0);
            List<byte[]> strikePackets = fixture.packets();
            int poisonPacket = indexOf(strikePackets,
                    Protocol.poison(target.objectId(), 1));
            int physicalAttack = firstPhysicalAttackIndex(
                    strikePackets, companion.snapshot().objectId(),
                    target.objectId(), poisonPacket);
            assertTrue(poisonPacket >= 0,
                    () -> strikePackets.stream().map(Protocol::hex)
                            .toList().toString());
            assertTrue(physicalAttack > poisonPacket,
                    () -> strikePackets.stream().map(Protocol::hex)
                            .toList().toString());

            int beforeTick = fixture.population.actor(
                    MAP_ID, target.objectId()).currentHp();
            fixture.session.serverExpireTimedStatuses(
                    poisonStartedAt + 3_000_000_000L);
            assertEquals(beforeTick - 5, fixture.population.actor(
                    MAP_ID, target.objectId()).currentHp());
            assertTrue(fixture.population.poisonActive(
                    MAP_ID, target.objectId(),
                    poisonStartedAt + 3_000_000_000L));
            assertTrue(indexOf(fixture.packets(), Protocol.hitRatio(
                    target.objectId(), beforeTick - 5,
                    target.maximumHp())) >= 0);
        }
    }

    @Test
    void monsterOrdinaryAttackPoisonsTamedMonsterBeforeEncryptedAttackAndTicks()
            throws Exception {
        try (Fixture fixture = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-npc-poison-selftest.properties"),
                45278, 0, false, false)) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    poisonActor(fixture.candidate, 1);
            ArenaMapCatalog.Point point = fixture.candidate.target();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0810L, 81217, 4205, "$3378", 28,
                    5_000, 5_000, 80, 0, 16, 14, point.x(), point.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81217));
            tamedCompanions(fixture.session).add(companion);
            fixture.packets();

            long before = System.nanoTime();
            fixture.session.serverClassicWorldMonsterCompanionAttack(
                    new ClassicWorldMonsterPopulation.Attack(
                            actor, companion.snapshot().objectId(),
                            point.x(), point.y(), false),
                    companion.snapshot().objectId());
            long after = System.nanoTime();

            List<byte[]> strikePackets = fixture.packets();
            int poison = indexOf(strikePackets,
                    Protocol.poison(companion.snapshot().objectId(), 1));
            int physical = firstPhysicalAttackIndex(
                    strikePackets, actor.objectId(),
                    companion.snapshot().objectId(), poison);
            assertTrue(poison >= 0 && physical > poison,
                    () -> strikePackets.stream().map(Protocol::hex)
                            .toList().toString());
            int afterStrikeHp = companion.snapshot().hp();
            fixture.session.serverExpireTimedStatuses(
                    after + 3_000_000_001L);
            assertEquals(afterStrikeHp - 5, companion.snapshot().hp());
            assertTrue(after >= before);
            assertTrue(indexOf(fixture.packets(), Protocol.hitRatio(
                    companion.snapshot().objectId(), afterStrikeHp - 5,
                    companion.snapshot().maxHp())) >= 0);
        }
    }

    @Test
    void monsterPhysicalSkillUsesSamePoisonCalcDamageOrderingForCompanion()
            throws Exception {
        try (Fixture fixture = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-npc-poison-selftest.properties"),
                45278, 0, false, false)) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    poisonActor(fixture.candidate, 1);
            ArenaMapCatalog.Point point = fixture.candidate.target();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0811L, 81217, 4205, "$3378", 28,
                    5_000, 5_000, 80, 0, 16, 14, point.x(), point.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81217));
            tamedCompanions(fixture.session).add(companion);
            ClassicWorldMonsterPhysicalSkillCatalog.Definition skill =
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            actor.npcId(), 99, 100, -2, 0,
                            2, 0, 0, 20, 0, 30);
            fixture.packets();

            fixture.session.serverClassicWorldMonsterPhysicalSkillAttack(
                    new ClassicWorldMonsterPopulation.PhysicalSkillAttack(
                            actor, companion.snapshot().objectId(),
                            point.x(), point.y(), skill, 0, false));

            List<byte[]> packets = fixture.packets();
            int poison = indexOf(packets,
                    Protocol.poison(companion.snapshot().objectId(), 1));
            int physical = firstPhysicalAttackIndex(
                    packets, actor.objectId(), companion.snapshot().objectId(),
                    poison);
            assertTrue(poison >= 0 && physical > poison,
                    () -> packets.stream().map(Protocol::hex)
                            .toList().toString());
        }
    }

    @Test
    void monsterParalysisPoisonPublishesNpcPalettesWithoutPlayerOnlyPackets()
            throws Exception {
        try (Fixture fixture = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-npc-poison-selftest.properties"),
                45278, 0, false, false)) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    poisonActor(fixture.candidate, 4);
            ArenaMapCatalog.Point point = fixture.candidate.target();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0812L, 81217, 4205, "$3378", 28,
                    5_000, 5_000, 80, 0, 16, 14, point.x(), point.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81217));
            assertTrue(companion.selectTarget(0x7300_1812L));
            tamedCompanions(fixture.session).add(companion);
            fixture.packets();

            fixture.session.serverClassicWorldMonsterCompanionAttack(
                    new ClassicWorldMonsterPopulation.Attack(
                            actor, companion.snapshot().objectId(),
                            point.x(), point.y(), false),
                    companion.snapshot().objectId());
            long after = System.nanoTime();
            List<byte[]> warning = fixture.packets();
            assertTrue(indexOf(warning,
                    Protocol.poison(companion.snapshot().objectId(), 1)) >= 0);
            assertEquals(-1, indexOf(warning, Protocol.messageCode(212)));

            fixture.session.serverExpireTimedStatuses(
                    after + 20_000_000_001L);
            assertTrue(indexOf(fixture.packets(),
                    Protocol.poison(companion.snapshot().objectId(), 2)) >= 0);

            fixture.session.serverExpireTimedStatuses(
                    after + 65_000_000_001L);
            assertTrue(indexOf(fixture.packets(),
                    Protocol.poison(companion.snapshot().objectId(), 0)) >= 0);
        }
    }

    @Test
    void monsterCurseParalysisImmediatelyLocksTamedAndPublishesNpcPackets()
            throws Exception {
        try (Fixture fixture = fixture(45278, 0, false)) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    fixture.candidate.actor();
            ArenaMapCatalog.Point point = fixture.candidate.target();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0813L, 81217, 4205, "$3378", 28,
                    5_000, 5_000, 80, 0, 16, 14, point.x(), point.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81217));
            assertTrue(companion.selectTarget(0x7300_1813L));
            tamedCompanions(fixture.session).add(companion);
            ClassicWorldMonsterStatusSkillCatalog.Definition retained =
                    ClassicWorldMonsterStatusSkillCatalog.forNpc(87005).get(0);
            ClassicWorldMonsterStatusSkillCatalog.Definition skill =
                    new ClassicWorldMonsterStatusSkillCatalog.Definition(
                            actor.npcId(), retained.actNo(), retained.triggerChance(),
                            retained.triggerRange(), retained.leverage(),
                            retained.skillId(), retained.mpCost(),
                            retained.reuseDelayMillis(), retained.maximumRange(),
                            retained.actionId(), retained.effectGfx(),
                            retained.actionDelayMillis(), retained.warningDelayMillis(),
                            retained.paralysisDurationMillis(),
                            retained.failureMessage(), retained.probabilityHigh(),
                            retained.probabilityEqual(), retained.probabilityLow(),
                            retained.intelligenceMultiplier(),
                            retained.magicResistanceMultiplier(),
                            retained.includeMagicLevel(), retained.includeMagicHit());
            fixture.packets();

            long before = System.nanoTime();
            fixture.session.serverClassicWorldMonsterStatusSkillAttack(
                    new ClassicWorldMonsterPopulation.StatusSkillAttack(
                            actor, companion.snapshot().objectId(),
                            point.x(), point.y(), skill, 0, false), 1);
            long after = System.nanoTime();

            List<byte[]> packets = fixture.packets();
            int palette = indexOf(packets,
                    Protocol.poison(companion.snapshot().objectId(), 2));
            int action = indexOf(packets,
                    Protocol.objectAction(actor.objectId(), skill.actionId()));
            int effect = indexOf(packets, Protocol.effectObject(
                    companion.snapshot().objectId(), skill.effectGfx()));
            assertTrue(palette >= 0 && palette < action && action < effect,
                    () -> packets.stream().map(Protocol::hex).toList().toString());
            assertEquals(-1, indexOf(packets, Protocol.messageCode(212)));
            assertTrue(packets.stream().noneMatch(packet -> packet.length > 0
                    && Byte.toUnsignedInt(packet[0]) == Protocol.S_PARALYSE));
            assertTrue(companion.npcCurseParalysisActive(after));
            assertFalse(companion.ready(after));
            assertTrue(after >= before);

            fixture.session.serverExpireTimedStatuses(
                    after + 10_000_000_001L);
            assertTrue(indexOf(fixture.packets(),
                    Protocol.poison(companion.snapshot().objectId(), 0)) >= 0);
            assertFalse(companion.npcCurseParalysisActive(
                    after + 10_000_000_001L));
        }
    }

    @Test
    void worldCompanionSilencePoisonIsPersistentNpcLeaseWithoutPlayerMessage()
            throws Exception {
        try (Fixture fixture = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-npc-poison-selftest.properties"),
                45278, 0, false, false)) {
            ClassicWorldMonsterPopulation.Snapshot target =
                    fixture.candidate.actor();
            ArenaMapCatalog.Point point = fixture.candidate.target();
            fixture.packets();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0802L, 81236, 2064, "$3106", 30,
                    300, 300, 100, -10, 15, 20, point.x(), point.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81236));
            assertTrue(companion.selectTarget(target.objectId()));
            tamedCompanions(fixture.session).add(companion);

            long schedulerNow = System.nanoTime();
            companion.defer(schedulerNow, 60_000);
            long nowNanos = schedulerNow + 60_000_000_000L;
            long poisonStartedAt = 0;
            for (int attempt = 0; attempt < 100 && poisonStartedAt == 0;
                 attempt++, nowNanos += 2_000_000_000L) {
                fixture.session.serverClassicWorldCompanionTick(
                        MAP_ID, nowNanos);
                if (fixture.population.poisonActive(
                        MAP_ID, target.objectId(), nowNanos)) {
                    poisonStartedAt = nowNanos;
                }
            }

            assertTrue(poisonStartedAt > 0);
            List<byte[]> packets = fixture.packets();
            int poisonPacket = indexOf(packets,
                    Protocol.poison(target.objectId(), 1));
            assertTrue(poisonPacket >= 0);
            assertTrue(firstPhysicalAttackIndex(
                    packets, companion.snapshot().objectId(),
                    target.objectId(), poisonPacket) > poisonPacket);
            assertEquals(-1, indexOf(packets, Protocol.messageCode(310)));

            int hp = fixture.population.actor(
                    MAP_ID, target.objectId()).currentHp();
            fixture.session.serverExpireTimedStatuses(
                    poisonStartedAt + 60_000_000_000L);
            assertEquals(hp, fixture.population.actor(
                    MAP_ID, target.objectId()).currentHp());
            assertTrue(fixture.population.poisonActive(
                    MAP_ID, target.objectId(),
                    poisonStartedAt + 60_000_000_000L));
            assertTrue(fixture.hub.cureClassicWorldMonsterPoison(
                    MAP_ID, -1, target.objectId()));
            assertEquals(0, fixture.population.poisonPalette(
                    MAP_ID, target.objectId(),
                    poisonStartedAt + 60_000_000_001L));
        }
    }

    @Test
    void worldCompanionParalysisPoisonTransitionsEncryptedPaletteAndNpcLock()
            throws Exception {
        try (Fixture fixture = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-npc-poison-selftest.properties"),
                45278, 0, false, false)) {
            ClassicWorldMonsterPopulation.Snapshot target =
                    fixture.candidate.actor();
            ArenaMapCatalog.Point point = fixture.candidate.target();
            fixture.packets();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0803L, 45157, 144, "$317", 30,
                    240, 240, 80, -4, 16, 16, point.x(), point.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(45157));
            assertTrue(companion.selectTarget(target.objectId()));
            tamedCompanions(fixture.session).add(companion);

            long schedulerNow = System.nanoTime();
            companion.defer(schedulerNow, 60_000);
            long nowNanos = schedulerNow + 60_000_000_000L;
            long poisonStartedAt = 0;
            for (int attempt = 0; attempt < 100 && poisonStartedAt == 0;
                 attempt++, nowNanos += 2_000_000_000L) {
                fixture.session.serverClassicWorldCompanionTick(
                        MAP_ID, nowNanos);
                if (fixture.population.poisonActive(
                        MAP_ID, target.objectId(), nowNanos)) {
                    poisonStartedAt = nowNanos;
                }
            }

            assertTrue(poisonStartedAt > 0);
            List<byte[]> warningPackets = fixture.packets();
            int warning = indexOf(warningPackets,
                    Protocol.poison(target.objectId(), 1));
            assertTrue(warning >= 0);
            assertTrue(firstPhysicalAttackIndex(
                    warningPackets, companion.snapshot().objectId(),
                    target.objectId(), warning) > warning);
            assertTrue(fixture.population.actionReady(
                    MAP_ID, target.objectId(),
                    poisonStartedAt + 19_999_999_999L));

            fixture.session.serverExpireTimedStatuses(
                    poisonStartedAt + 20_000_000_000L);
            assertTrue(indexOf(fixture.packets(),
                    Protocol.poison(target.objectId(), 2)) >= 0);
            assertFalse(fixture.population.actionReady(
                    MAP_ID, target.objectId(),
                    poisonStartedAt + 20_000_000_000L));

            fixture.session.serverExpireTimedStatuses(
                    poisonStartedAt + 65_000_000_000L);
            assertTrue(indexOf(fixture.packets(),
                    Protocol.poison(target.objectId(), 0)) >= 0);
            assertFalse(fixture.population.poisonActive(
                    MAP_ID, target.objectId(),
                    poisonStartedAt + 65_000_000_000L));
        }
    }

    @Test
    void encryptedWorldWeaponBreakTargetsCompanionAndAddsNativeNpcStack()
            throws Exception {
        try (Fixture fixture = fixture(
                Path.of("src/test/resources/server-config/config/server.weapon-break-native-monster-selftest.properties"),
                45278, 0, false, false)) {
            ClassicWorldMonsterPopulation.Snapshot nearby =
                    fixture.candidate.actor();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7300_0804L, 81217, 4205, "$3378", 28,
                    240, 240, 80, 0, 16, 14,
                    nearby.x(), nearby.y());
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81217));
            tamedCompanions(fixture.session).add(companion);
            setField(fixture.session, "learnedSkills",
                    new HashSet<>(Set.of(BattleSpellCatalog.WEAPON_BREAK)));
            setField(fixture.session, "inventory",
                    RuntimeInventory.fromCatalogCounts(
                            Map.of(InventoryCatalog.MAGIC_GEM, 1),
                            () -> 0x7400_0804L));
            fixture.packets();

            BattleSpellCatalog.WeaponBreakSpell spell =
                    BattleSpellCatalog.weaponBreakSpell(
                            BattleSpellCatalog.WEAPON_BREAK);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(spell.row()).c(spell.column())
                    .d(companion.snapshot().objectId())
                    .h(nearby.x()).h(nearby.y()).bytes();
            Method handler = ClientSession.class.getDeclaredMethod(
                    "useClassicWorldWeaponBreakSpell", byte[].class,
                    BattleSpellCatalog.WeaponBreakSpell.class);
            handler.setAccessible(true);
            handler.invoke(fixture.session, request, spell);

            assertEquals(1, companion.weaponBreakStacks());
            List<byte[]> packets = fixture.packets();
            assertTrue(indexOf(packets, Protocol.effectObject(
                    companion.snapshot().objectId(), spell.effectGfx())) >= 0,
                    () -> packets.stream().map(Protocol::hex).toList().toString());
            assertTrue(indexOf(packets, Protocol.manaPoint(
                    100 - spell.effectiveMpCost(8, 0), 100)) >= 0);
        }
    }

    @Test
    void productionSchedulerMonsterPickupEmitsEncryptedGroundRemoval()
            throws Exception {
        try (Fixture fixture = fixture(45009, 0, false)) {
            ClassicWorldMonsterPopulation pickupPopulation =
                    pickupPopulation(fixture.candidate);
            long monsterId = 0x6841_0001L;
            ClassicWorldMonsterPopulation.Snapshot actor =
                    pickupPopulation.actor(MAP_ID, monsterId);
            ArenaMapCatalog.Point groundPoint = adjacentGroundPoint(
                    actor, fixture.candidate.target());
            ClassicWorldGroundItems.Item item = fixture.hub.putClassicWorldGroundItem(
                    MAP_ID, -1, ClassicWorldDropCatalog.item(
                            MAP_ID, (int) InventoryCatalog.ADENA),
                    23, groundPoint.x(), groundPoint.y(), actor.npcId());
            assertNotNull(item);

            fixture.tick(pickupPopulation, 1L);

            assertEquals(null, fixture.hub.classicWorldGroundItem(
                    MAP_ID, -1, item.objectId()));
            List<RuntimeInventory.Item> carried = pickupPopulation.carriedItems(
                    MAP_ID, monsterId);
            assertEquals(1, carried.size());
            assertEquals(23, carried.get(0).quantity());
            String frames = Files.readString(fixture.capture.path());
            assertTrue(frames.contains("\"opcode\":\"0x15\""));
            assertTrue(frames.contains(
                    "S_REMOVE_OBJECT ground-item=" + item.objectId()
                            + " reason=monster-pickup monster=" + monsterId));
            assertTrue(frames.contains("refinements=0 digest=0"));
            int pickupFrame = frames.indexOf(
                    "reason=monster-pickup monster=" + monsterId);
            assertFalse(frames.substring(pickupFrame).contains(
                    "S_CHANGE_LIGHT object=" + monsterId));
        }
    }

    @Test
    void productionSchedulerDamagedFlyLandsForPotionBeforePickup()
            throws Exception {
        try (Fixture fixture = fixture(45009, 0, false)) {
            ClassicWorldMonsterPopulation.Snapshot sourceFlying =
                    sourceFlyingActor(fixture.population);
            ClassicWorldMonsterCatalog.SpawnDefinition definition =
                    new ClassicWorldMonsterCatalog.SpawnDefinition(
                            MAP_ID, 993_774, "air-item-landing-wire-fixture",
                            1, 0,
                            fixture.candidate.actor().x(),
                            fixture.candidate.actor().y(),
                            0, 0, 0, 0, 0, 0,
                            sourceFlying.heading(), 0, 0, false, 0,
                            false, 0,
                            passiveFlyingTemplate(sourceFlying.template()));
            ClassicWorldMonsterPopulation flying =
                    ClassicWorldMonsterPopulation.retainedSourceSlice(
                            774L, 0x6842_0000L, 0x6842_1000L,
                            List.of(definition));
            long monsterId = 0x6842_0001L;
            ClassicWorldMonsterPopulation.Snapshot actor =
                    flying.actor(MAP_ID, monsterId);
            assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_FLY,
                    actor.hiddenStatus());
            setMonsterCurrentHp(flying, monsterId, actor.maximumHp() - 1);
            actor = flying.actor(MAP_ID, monsterId);
            ArenaMapCatalog.Point point = adjacentGroundPoint(
                    actor, fixture.candidate.target());
            ClassicWorldGroundItems.Item potion =
                    fixture.hub.putClassicWorldGroundItem(
                            MAP_ID, -1,
                            InventoryCatalog.builtIn(
                                    InventoryCatalog.NATIVE_HEALING_POTION),
                            1, point.x(), point.y(), actor.npcId());
            assertNotNull(potion);

            fixture.tick(flying, 1L);

            ClassicWorldMonsterPopulation.Snapshot landed =
                    flying.actor(MAP_ID, monsterId);
            assertEquals(ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE,
                    landed.hiddenStatus());
            assertEquals(0, flying.targetObjectId(MAP_ID, monsterId));
            assertEquals(null, fixture.hub.classicWorldGroundItem(
                    MAP_ID, -1, potion.objectId()));
            assertEquals(InventoryCatalog.NATIVE_HEALING_POTION,
                    flying.carriedItems(MAP_ID, monsterId).get(0)
                            .definition().catalogId());
            String frames = Files.readString(fixture.capture.path());
            int movedown = frames.indexOf(
                    "air-item-landing=" + monsterId + " action="
                            + BattleActionCodes.MOVE_DOWN);
            int rebuild = frames.indexOf(
                    "native-fly-item-landing-actor-rebuild", movedown);
            int remove = frames.indexOf(
                    "reason=monster-pickup monster=" + monsterId, rebuild);
            assertTrue(movedown >= 0 && movedown < rebuild
                    && rebuild < remove);
        }
    }

    @Test
    void monsterItemUseEmitsHasteSpeedBeforeEffectsOnEncryptedStream()
            throws Exception {
        try (Fixture fixture = fixture(45009, 0, false)) {
            ClassicWorldMonsterPopulation itemPopulation =
                    pickupPopulation(fixture.candidate);
            long monsterId = 0x6841_0001L;
            ClassicWorldMonsterPopulation.Snapshot actor =
                    itemPopulation.actor(MAP_ID, monsterId);
            ArenaMapCatalog.Point groundPoint = adjacentGroundPoint(
                    actor, fixture.candidate.target());
            long now = carryMonsterItem(
                    fixture.hub, itemPopulation, actor, groundPoint,
                    InventoryCatalog.SELF_HASTE_POTION, 1L);
            now = carryMonsterItem(
                    fixture.hub, itemPopulation, actor, groundPoint,
                    InventoryCatalog.NATIVE_HEALING_POTION, now);
            ClassicWorldMonsterPopulation.Snapshot full =
                    itemPopulation.actor(MAP_ID, monsterId);
            assertNotNull(itemPopulation.damage(
                    MAP_ID, monsterId, PLAYER_OBJECT_ID,
                    full.currentHp() - 1));
            List<ClassicWorldMonsterPopulation.ItemUse> uses =
                    itemPopulation.itemUse(
                            MAP_ID, monsterId, now, 0, 0);
            assertEquals(2, uses.size());
            for (ClassicWorldMonsterPopulation.ItemUse use : uses) {
                fixture.session.serverClassicWorldMonsterUsedItem(use);
            }

            String frames = Files.readString(fixture.capture.path());
            int speed = frames.indexOf(
                    "reason=monster-item catalog=40013");
            int hasteEffect = frames.indexOf(
                    "reason=monster-item kind=HASTE catalog=40013", speed);
            int healEffect = frames.indexOf(
                    "reason=monster-item kind=HEAL catalog=40010",
                    hasteEffect);
            assertTrue(speed >= 0 && speed < hasteEffect
                    && hasteEffect < healEffect);
        }
    }

    @Test
    void productionSchedulerRebuildsDoppelWithSelectedCharacterIdentity()
            throws Exception {
        try (Fixture fixture = fixture(45189, -777, false)) {
            setField(fixture.session, "classicTitle", "Mirror Champion");
            long objectId = fixture.candidate.actor().objectId();
            assertEquals(31, fixture.candidate.actor().gfxId());

            fixture.tick();

            ClassicWorldMonsterPopulation.Snapshot transformed =
                    fixture.population.actor(MAP_ID, objectId);
            assertEquals(1, transformed.gfxId());
            assertEquals("AggressionTarget", transformed.nameId());
            assertEquals("Mirror Champion", transformed.title());
            assertEquals(-777, transformed.lawful());
            assertEquals(640, transformed.passispeed());
            assertEquals(900, transformed.atkspeed());
            String frames = Files.readString(fixture.capture.path());
            int remove = frames.indexOf(
                    "reason=doppel-transform player=" + PLAYER_OBJECT_ID);
            int put = frames.indexOf("reason=doppel-transform", remove + 1);
            assertTrue(remove >= 0 && put > remove);
        }
    }

    private static long carryMonsterItem(
            GameHub hub, ClassicWorldMonsterPopulation population,
            ClassicWorldMonsterPopulation.Snapshot actor,
            ArenaMapCatalog.Point point, long catalogId, long nowNanos) {
        InventoryCatalog.Definition definition =
                InventoryCatalog.builtIn(catalogId);
        ClassicWorldGroundItems.Item item = hub.putClassicWorldGroundItem(
                MAP_ID, -1, definition, 1,
                point.x(), point.y(), actor.npcId());
        assertNotNull(item);
        ClassicWorldMonsterPopulation.ItemAction action = population.itemAction(
                MAP_ID, actor.objectId(),
                hub.classicWorldGroundItemsInScreen(
                        MAP_ID, -1, actor.x(), actor.y()),
                Set.of(), nowNanos, 0);
        assertNotNull(action);
        assertEquals(ClassicWorldMonsterPopulation.ItemActionKind.PICKUP_READY,
                action.kind());
        ClassicWorldMonsterPopulation.ItemPickup[] pickup =
                new ClassicWorldMonsterPopulation.ItemPickup[1];
        ClassicWorldGroundItems.Take take = groundItems(hub).takeWholeIf(
                MAP_ID, -1, item.objectId(), ground -> {
                    pickup[0] = population.acceptGroundItem(
                            MAP_ID, actor.objectId(), ground, nowNanos);
                    return pickup[0] != null;
                });
        assertNotNull(take);
        assertNotNull(pickup[0]);
        return nowNanos + 1_000_000_000L;
    }

    private static ClassicWorldGroundItems groundItems(GameHub hub) {
        try {
            Field field = GameHub.class.getDeclaredField(
                    "classicWorldGroundItems");
            field.setAccessible(true);
            return (ClassicWorldGroundItems) field.get(hub);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private Fixture fixture(int npcId, int lawful, boolean invisible)
            throws Exception {
        return fixture(npcId, lawful, invisible, false);
    }

    private Fixture fixture(
            int npcId, int lawful, boolean invisible, boolean escape)
            throws Exception {
        return fixture(Path.of("src/test/resources/server-config/config/server.selftest.properties"),
                npcId, lawful, invisible, escape);
    }

    private Fixture fixture(
            Path configFile, int npcId, int lawful,
            boolean invisible, boolean escape) throws Exception {
        ServerConfig config = ServerConfig.load(configFile);
        AccountRepository accounts = AccountRepository.open(config);
        GameHub hub = new GameHub(config);
        ClassicWorldMonsterPopulation population = population(hub);
        Candidate candidate = escape
                ? escapeCandidate(population, npcId)
                : candidate(population, npcId);
        GameStateStore state = new GameStateStore(
                temporary.resolve("monster-aggression-" + npcId + "-" + lawful
                        + "-" + invisible + ".properties"), false);
        CaptureLog capture = new CaptureLog(temporary.resolve(
                "capture-monster-aggression-" + npcId + "-" + lawful
                        + "-" + invisible));
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(500);

        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, accounts, state, capture, hub);
        Protocol.CipherState receiver = new Protocol.CipherState(0x4040_0404);
        setField(session, "outgoing", new Protocol.CipherState(0x4040_0404));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "monster-aggression");
        setField(session, "selectedCharacter", "AggressionTarget");
        setField(session, "characters", List.of(character(candidate.target(), lawful)));
        setField(session, "playerLawful", lawful);
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "sourcePhysicalGrowthStats", SourcePhysicalGrowthStats.classic381());
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7400_0404L));
        setField(session, "playerHp", 5_000);
        setField(session, "playerMaxHp", 5_000);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        setField(session, "classicWorldMapId", MAP_ID);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", candidate.target().x());
        setField(session, "positionY", candidate.target().y());
        setField(session, "playerHeading", 0);
        setField(session, "invisible", invisible);
        hub.connect(session);
        hub.enterClassicWorld(session, MAP_ID,
                candidate.target().x(), candidate.target().y(), 0);
        return new Fixture(accounts, hub, population, capture, client, server,
                session, receiver, candidate);
    }

    private static Candidate candidate(
            ClassicWorldMonsterPopulation population, int npcId) {
        Set<ClassicWorldMonsterPopulation.GridPoint> occupied = new HashSet<>();
        for (long ordinal = 1; ordinal <= population.size(); ordinal++) {
            ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                    MAP_ID, ClassicWorldMonsterPopulation.OBJECT_BASE + ordinal);
            if (actor != null && actor.present() && actor.alive()) {
                occupied.add(new ClassicWorldMonsterPopulation.GridPoint(
                        actor.x(), actor.y()));
            }
        }
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(MAP_ID);
        for (long ordinal = 1; ordinal <= population.size(); ordinal++) {
            ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                    MAP_ID, ClassicWorldMonsterPopulation.OBJECT_BASE + ordinal);
            if (actor == null || actor.npcId() != npcId || !actor.present()
                    || !actor.alive() || actor.hiddenStatus()
                        != ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE
                    || actor.template().atkspeed() <= 0) continue;
            for (int x = actor.x() - 1; x <= actor.x() + 1; x++) {
                for (int y = actor.y() - 1; y <= actor.y() + 1; y++) {
                    if (x == actor.x() && y == actor.y()) continue;
                    var point = new ClassicWorldMonsterPopulation.GridPoint(x, y);
                    if (occupied.contains(point) || ClassicWorldNpcCatalog.occupies(
                            MAP_ID, x, y) || !map.standingPassable(x, y)) continue;
                    return new Candidate(actor, new ArenaMapCatalog.Point(x, y));
                }
            }
        }
        throw new AssertionError("no open source actor for NPC " + npcId);
    }

    private static Candidate escapeCandidate(
            ClassicWorldMonsterPopulation population, int npcId) {
        Set<ClassicWorldMonsterPopulation.GridPoint> occupied = new HashSet<>();
        for (long ordinal = 1; ordinal <= population.size(); ordinal++) {
            ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                    MAP_ID, ClassicWorldMonsterPopulation.OBJECT_BASE + ordinal);
            if (actor != null && actor.present() && actor.alive()) {
                occupied.add(new ClassicWorldMonsterPopulation.GridPoint(
                        actor.x(), actor.y()));
            }
        }
        int[] headingX = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] headingY = {-1, -1, 0, 1, 1, 1, 0, -1};
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (long ordinal = 1; ordinal <= population.size(); ordinal++) {
            ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                    MAP_ID, ClassicWorldMonsterPopulation.OBJECT_BASE + ordinal);
            if (actor == null || actor.npcId() != npcId || !actor.present()
                    || !actor.alive() || actor.hiddenStatus()
                        != ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE
                    || actor.template().atkspeed() != 0
                    || actor.template().passispeed() <= 0) continue;
            for (int toward = 0; toward < 8; toward++) {
                int targetX = actor.x() + headingX[toward];
                int targetY = actor.y() + headingY[toward];
                int reverse = (toward + 4) & 7;
                int escapeX = actor.x() + headingX[reverse];
                int escapeY = actor.y() + headingY[reverse];
                if (!map.canStep(actor.x(), actor.y(), escapeX, escapeY)
                        || occupied.contains(
                            new ClassicWorldMonsterPopulation.GridPoint(
                                    targetX, targetY))
                        || occupied.contains(
                            new ClassicWorldMonsterPopulation.GridPoint(
                                    escapeX, escapeY))
                        || ClassicWorldNpcCatalog.occupies(
                            MAP_ID, targetX, targetY)
                        || ClassicWorldNpcCatalog.occupies(
                            MAP_ID, escapeX, escapeY)) continue;
                return new Candidate(actor,
                        new ArenaMapCatalog.Point(targetX, targetY));
            }
        }
        throw new AssertionError("no open escape actor for NPC " + npcId);
    }

    private static int chebyshev(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    private static int indexOf(List<byte[]> packets, byte[] expected) {
        for (int index = 0; index < packets.size(); index++) {
            if (Arrays.equals(packets.get(index), expected)) return index;
        }
        return -1;
    }

    private static int firstPhysicalAttackIndex(
            List<byte[]> packets, long attackerObjectId, long targetObjectId,
            int exclusiveIndex) {
        for (int index = exclusiveIndex + 1; index < packets.size(); index++) {
            byte[] packet = packets.get(index);
            if (packet.length < 10 || Byte.toUnsignedInt(packet[0])
                    != Protocol.S_ATTACK) continue;
            Protocol.Reader in = new Protocol.Reader(packet);
            in.c();
            in.c();
            if (in.d() == attackerObjectId && in.d() == targetObjectId) {
                return index;
            }
        }
        return -1;
    }

    private static ClassicWorldMonsterPopulation idlePopulation(
            Candidate candidate, ArenaMapCatalog.Point player) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        int selectedHeading = -1;
        for (int heading = 0; heading < 8; heading++) {
            int x = candidate.actor().x() + dx[heading];
            int y = candidate.actor().y() + dy[heading];
            if ((x != player.x() || y != player.y())
                    && map.canStep(candidate.actor().x(), candidate.actor().y(), x, y)
                    && !ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)) {
                selectedHeading = heading;
                break;
            }
        }
        if (selectedHeading < 0) {
            throw new AssertionError("no idle scheduler direction");
        }
        ClassicWorldMonsterCatalog.SpawnDefinition source =
                candidate.actor().definition();
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 991_769, "scheduler-noTarget-fixture", 1, 0,
                        candidate.actor().x(), candidate.actor().y(),
                        0, 0, 0, 0, 0, 0, selectedHeading,
                        0, 0, false, 0, false, 0, source.template());
        Set<ClassicWorldMonsterPopulation.GridPoint> occupied = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(
                        player.x(), player.y()));
        for (long seed = 1; seed <= 10_000; seed++) {
            ClassicWorldMonsterPopulation probe =
                    ClassicWorldMonsterPopulation.retainedSourceSlice(
                            seed, 0x6810_0000L, 0x6810_1000L,
                            List.of(definition));
            if (probe.idleMove(MAP_ID, 0x6810_0001L,
                    occupied, 1L) != null) {
                return ClassicWorldMonsterPopulation.retainedSourceSlice(
                        seed, 0x6810_0000L, 0x6810_1000L,
                        List.of(definition));
            }
        }
        throw new AssertionError("no deterministic idle scheduler seed");
    }

    private static MovementFixture movementDistancePopulation(
            Candidate candidate) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        ArenaMapCatalog.Point player = candidate.target();
        for (int heading = 0; heading < 8; heading++) {
            int oneX = candidate.actor().x() + dx[heading];
            int oneY = candidate.actor().y() + dy[heading];
            int twoX = oneX + dx[heading];
            int twoY = oneY + dy[heading];
            if ((oneX == player.x() && oneY == player.y())
                    || (twoX == player.x() && twoY == player.y())
                    || !map.canStep(candidate.actor().x(), candidate.actor().y(),
                            oneX, oneY)
                    || !map.canStep(oneX, oneY, twoX, twoY)
                    || ClassicWorldNpcCatalog.occupies(MAP_ID, oneX, oneY)
                    || ClassicWorldNpcCatalog.occupies(MAP_ID, twoX, twoY)) {
                continue;
            }
            ClassicWorldMonsterCatalog.SpawnDefinition definition =
                    new ClassicWorldMonsterCatalog.SpawnDefinition(
                            MAP_ID, 992_770, "movement-distance-wire-fixture",
                            1, 0, candidate.actor().x(), candidate.actor().y(),
                            0, 0, 0, 0, 0, 0, heading,
                            0, 0, false, 1, false, 0,
                            candidate.actor().template());
            ClassicWorldMonsterPopulation population =
                    ClassicWorldMonsterPopulation.retainedSourceSlice(
                            770L, 0x6830_0000L, 0x6830_1000L,
                            List.of(definition));
            return new MovementFixture(population,
                    new ArenaMapCatalog.Point(
                            candidate.actor().x() + dx[heading] * 10,
                            candidate.actor().y() + dy[heading] * 10));
        }
        throw new AssertionError("no movement-distance wire corridor");
    }

    private static ClassicWorldMonsterPopulation pickupPopulation(
            Candidate candidate) {
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 993_771, "monster-pickup-wire-fixture", 1, 0,
                        candidate.actor().x(), candidate.actor().y(),
                        0, 0, 0, 0, 0, 0, candidate.actor().heading(),
                        0, 0, false, 0, false, 0,
                        candidate.actor().template());
        return ClassicWorldMonsterPopulation.retainedSourceSlice(
                771L, 0x6841_0000L, 0x6841_1000L,
                List.of(definition));
    }

    private static ClassicWorldMonsterPopulation.Snapshot sourceFlyingActor(
            ClassicWorldMonsterPopulation population) throws Exception {
        Field actorsField = ClassicWorldMonsterPopulation.class
                .getDeclaredField("actors");
        actorsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Long> objectIds = Set.copyOf(
                ((Map<Long, Object>) actorsField.get(population)).keySet());
        for (long objectId : objectIds) {
            for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(mapId, objectId);
                if (actor != null && actor.hiddenStatus()
                        == ClassicWorldMonsterPopulation.HIDDEN_STATUS_FLY) return actor;
            }
        }
        throw new AssertionError("missing source flying actor");
    }

    private static ClassicWorldMonsterPopulation.Snapshot poisonActor(
            Candidate candidate, int poisonAttack) {
        ClassicWorldMonsterPopulation.Snapshot source = candidate.actor();
        ClassicWorldMonsterCatalog.MonsterTemplate template = source.template();
        ClassicWorldMonsterCatalog.MonsterTemplate poisoned =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        template.npcId(), template.nameId(), template.implementation(),
                        template.gfxId(), template.level(), template.hp(), template.mp(),
                        template.armorClass(), template.strength(),
                        template.constitution(), template.dexterity(), template.wisdom(),
                        template.intelligence(), template.magicResistance(),
                        template.experience(), template.lawful(), template.size(),
                        template.weakAttribute(), template.ranged(), template.passispeed(),
                        template.atkspeed(), template.bowActionId(), template.undead(),
                        poisonAttack, template.paralysisAttack(), template.aggressive(),
                        template.damageReduction(), template.hard(), template.doppel(),
                        template.boss(), template.lightSize(), template.changeHead());
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        source.mapId(), source.spawnId(), source.definition().location(),
                        1, 0, source.x(), source.y(), 0, 0, 0, 0, 0, 0,
                        source.heading(), 0, 0, false, 0, false, 0, poisoned);
        return new ClassicWorldMonsterPopulation.Snapshot(
                source.mapId(), source.spawnId(), source.spawnNumber(),
                source.objectId(), source.npcId(), source.x(), source.y(),
                source.heading(), source.currentHp(), source.maximumHp(),
                source.currentMp(), source.maximumMp(), true, true, definition);
    }

    private static ClassicWorldMonsterCatalog.MonsterTemplate
            passiveFlyingTemplate(
                    ClassicWorldMonsterCatalog.MonsterTemplate source) {
        return new ClassicWorldMonsterCatalog.MonsterTemplate(
                45067, source.nameId(), source.implementation(),
                source.gfxId(), source.level(), source.hp(), source.mp(),
                source.armorClass(), source.strength(), source.constitution(),
                source.dexterity(), source.wisdom(), source.intelligence(),
                source.magicResistance(), source.experience(), source.lawful(),
                source.size(), source.weakAttribute(), source.ranged(),
                source.passispeed(), source.atkspeed(), source.bowActionId(),
                source.undead(), source.poisonAttack(), source.paralysisAttack(),
                false, source.damageReduction(), source.hard(), source.doppel(),
                source.boss(), source.lightSize(), source.changeHead());
    }

    @SuppressWarnings("unchecked")
    private static void setMonsterCurrentHp(
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

    private static ArenaMapCatalog.Point adjacentGroundPoint(
            ClassicWorldMonsterPopulation.Snapshot actor,
            ArenaMapCatalog.Point player) {
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int heading = 0; heading < 8; heading++) {
            int x = actor.x() + dx[heading];
            int y = actor.y() + dy[heading];
            if ((x == player.x() && y == player.y())
                    || !map.canStep(actor.x(), actor.y(), x, y)
                    || ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)) continue;
            return new ArenaMapCatalog.Point(x, y);
        }
        throw new AssertionError("no adjacent ground-item fixture cell");
    }

    private static ArenaMapCatalog.Point farPassableCompanionPoint(
            ClassicWorldMonsterPopulation population,
            ClassicWorldMonsterPopulation.Snapshot target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int y = map.startY(); y <= map.endY(); y++) {
            for (int x = map.startX(); x <= map.endX(); x++) {
                if (chebyshev(x, y, target.x(), target.y()) <= 30
                        || !map.standingPassable(x, y)
                        || population.occupies(MAP_ID, x, y)
                        || ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)) {
                    continue;
                }
                return new ArenaMapCatalog.Point(x, y);
            }
        }
        throw new AssertionError("no distant WORLD companion fixture cell");
    }

    @SuppressWarnings("unchecked")
    private static List<BattleCompanion> tamedCompanions(ClientSession session)
            throws Exception {
        Field field = ClientSession.class.getDeclaredField("tamedCompanions");
        field.setAccessible(true);
        return (List<BattleCompanion>) field.get(session);
    }


    private static CharacterSummary character(
            ArenaMapCatalog.Point target, int lawful) {
        return new CharacterSummary(
                "AggressionTarget", "", 1, 0, 50, 5_000, 100,
                0, 12, 12, 12, 8, 8, 8, 8, 8,
                PLAYER_OBJECT_ID, 0, lawful, 0, 1,
                target.x(), target.y(), MAP_ID,
                12, 40, 1, 12, 12, 0);
    }

    private static ClassicWorldMonsterPopulation population(GameHub hub)
            throws Exception {
        Field field = GameHub.class.getDeclaredField("classicWorldMonsters");
        field.setAccessible(true);
        return (ClassicWorldMonsterPopulation) field.get(hub);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf(
                (Class<? extends Enum>) field.getType(), value));
    }

    private record Candidate(ClassicWorldMonsterPopulation.Snapshot actor,
                             ArenaMapCatalog.Point target) {}

    private record MovementFixture(
            ClassicWorldMonsterPopulation population,
            ArenaMapCatalog.Point target) {}

    private record Fixture(
            AccountRepository accounts, GameHub hub,
            ClassicWorldMonsterPopulation population,
            CaptureLog capture, Socket client, Socket server,
            ClientSession session, Protocol.CipherState receiver,
            Candidate candidate) implements AutoCloseable {
        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(
                            client.getInputStream(), 65_535);
                    if (encrypted == null) return result;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
        }

        void tick() throws Exception {
            Method method = GameHub.class.getDeclaredMethod(
                    "classicWorldMonsterTickSafely");
            method.setAccessible(true);
            method.invoke(hub);
        }

        void tick(ClassicWorldMonsterPopulation selectedPopulation,
                  long nowNanos) throws Exception {
            Method method = GameHub.class.getDeclaredMethod(
                    "classicWorldMonsterTick", int.class, int.class,
                    ClassicWorldMonsterPopulation.class, long.class);
            method.setAccessible(true);
            method.invoke(hub, MAP_ID, -1, selectedPopulation, nowNanos);
        }

        @Override
        public void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
            accounts.close();
        }
    }
}
