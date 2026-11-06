package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Encrypted S_ACTION/S_EXP proof for retained ordinary-WORLD death loss. */
final class ClientSessionClassicWorldDeathPenaltyIntegrationTest {
    private static final long SOURCE_EXP = 244_276;
    private static final long EXPECTED_EXP = SOURCE_EXP - 4_558;
    @TempDir Path temporary;

    @Test void fatalNpcDamageEmitsDeathBeforeExpAndPersistsOverlay()
            throws Exception {
        Path stateFile = temporary.resolve("death-exp.properties");
        try (Fixture fixture = fixture(stateFile, 4, 33313, 32339)) {
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            byte[] death = Protocol.objectAction(
                    fixture.session.playerObjectId(), BattleActionCodes.DIE);
            byte[] experience = Protocol.experience(22, EXPECTED_EXP);
            int deathIndex = indexOf(packets, death);
            int experienceIndex = indexOf(packets, experience);
            assertTrue(deathIndex >= 0 && experienceIndex > deathIndex,
                    () -> packets.stream().map(Protocol::hex).toList().toString());
            assertEquals(0, intField(fixture.session, "playerHp"));
            assertEquals(EXPECTED_EXP,
                    longField(fixture.session, "playerClassicExperience"));
            assertEquals(22, intField(fixture.session, "playerClassicLevel"));
            GameStateStore reloaded = new GameStateStore(stateFile, true);
            assertEquals(EXPECTED_EXP, reloaded.reconcileClassicExperience(
                    fixture.session.sourceCharacterIdentity(), SOURCE_EXP, 22)
                    .effectiveExperience());
        }
    }

    @Test void safetyZoneSuppressesNpcDeathExperienceLoss() throws Exception {
        assertNpcDeathPenaltySuppressed(
                "death-exp-safety.properties", 33706, 32129);
    }

    @Test void combatZoneSuppressesNpcDeathExperienceLoss() throws Exception {
        assertNpcDeathPenaltySuppressed(
                "death-exp-combat.properties", 34053, 32230);
    }

    @Test void consumableExperienceDeathSafeIsDebitedAndPreventsLoss()
            throws Exception {
        Path stateFile = temporary.resolve("death-exp-consumable-safe.properties");
        try (Fixture fixture = fixture(stateFile, 4, 33313, 32339,
                Map.of(InventoryCatalog.DEATH_SAFE_EXPERIENCE, 1))) {
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            assertEquals(-1, indexOf(packets, Protocol.experience(22, EXPECTED_EXP)));
            assertTrue(indexOf(packets, Protocol.messageCode(
                    166, "已消耗一個死亡保護道具")) >= 0);
            RuntimeInventory after = (RuntimeInventory) field(fixture.session, "inventory");
            assertEquals(0, after.countCatalog(
                    InventoryCatalog.DEATH_SAFE_EXPERIENCE));
            assertEquals(SOURCE_EXP,
                    longField(fixture.session, "playerClassicExperience"));
            assertEquals(0, new GameStateStore(stateFile, true).economy(
                    fixture.session.sourceCharacterIdentity()).inventory()
                    .getOrDefault(InventoryCatalog.DEATH_SAFE_EXPERIENCE, 0));
        }
    }

    @Test void nonConsumableAllDeathSafePreventsLossWithoutDebit()
            throws Exception {
        try (Fixture fixture = fixture(
                temporary.resolve("death-exp-nonconsume-safe.properties"),
                4, 33313, 32339,
                Map.of(InventoryCatalog.DEATH_SAFE_ALL, 1))) {
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            assertEquals(-1, indexOf(packets, Protocol.experience(22, EXPECTED_EXP)));
            assertTrue(indexOf(packets, Protocol.messageCode(
                    166, "死亡保護道具發揮了作用")) >= 0);
            RuntimeInventory after = (RuntimeInventory) field(fixture.session, "inventory");
            assertEquals(1, after.countCatalog(InventoryCatalog.DEATH_SAFE_ALL));
            assertEquals(SOURCE_EXP,
                    longField(fixture.session, "playerClassicExperience"));
            assertEquals(1, countOf(packets, Protocol.messageCode(
                    166, "死亡保護道具發揮了作用")));
        }
    }

    @Test void negativeLawfulDeathRemovesSkillBeforeExperienceAndPersistsMask()
            throws Exception {
        Path stateFile = temporary.resolve("death-skill.properties");
        try (Fixture fixture = fixture(stateFile, 4, 33313, 32339)) {
            setInt(fixture.session, "playerLawful", -32_768);
            setField(fixture.session, "learnedSkills", Set.of(1));
            setInt(fixture.session, "classicDeathSkillChanceRollOverride", 1);
            setInt(fixture.session, "classicDeathSkillCountRollOverride", 1);
            setInt(fixture.session, "classicDeathSkillSelectorOverride", 0);
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            int removed = indexOf(packets, Protocol.removeSpells(Set.of(1)));
            int experience = indexOf(packets, Protocol.experience(22, EXPECTED_EXP));
            assertTrue(removed >= 0 && experience > removed,
                    () -> packets.stream().map(Protocol::hex).toList().toString());
            assertEquals(Set.of(), field(fixture.session, "learnedSkills"));
            assertEquals(Set.of(1), new GameStateStore(stateFile, true)
                    .deathLostSkills(fixture.session.sourceCharacterIdentity()));
        }
    }

    @Test void negativeLawfulDeathDropsWholeItemBeforeExperienceAndGroundIsPickable()
            throws Exception {
        Path stateFile = temporary.resolve("death-item.properties");
        try (Fixture fixture = fixture(stateFile, 4, 33313, 32339,
                Map.of(InventoryCatalog.LIQUOR, 2))) {
            setInt(fixture.session, "playerLawful", -32_768);
            setInt(fixture.session, "classicDeathItemChanceRollOverride", 1);
            setInt(fixture.session, "classicDeathItemCountRollOverride", 1);
            setInt(fixture.session, "classicDeathItemSelectorOverride", 0);
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            List<ClassicWorldGroundItems.Item> grounds =
                    fixture.hub.classicWorldGroundItemsInScreen(
                            4, -1, 33313, 32339);
            assertEquals(1, grounds.size());
            ClassicWorldGroundItems.Item ground = grounds.get(0);
            assertEquals(InventoryCatalog.LIQUOR,
                    ground.definition().catalogId());
            assertEquals(2, ground.quantity());
            assertTrue(!ground.state().sourceBound());
            int death = indexOf(packets, Protocol.objectAction(
                    fixture.session.playerObjectId(), BattleActionCodes.DIE));
            int put = indexOf(packets, Protocol.groundItem(
                    ground.objectId(), ground.x(), ground.y(),
                    ground.definition().groundGfx(), ground.quantity(),
                    ground.displayName()));
            int experience = indexOf(packets, Protocol.experience(22, EXPECTED_EXP));
            assertTrue(death >= 0 && put > death && experience > put,
                    () -> packets.stream().map(Protocol::hex).toList().toString());
            RuntimeInventory after = (RuntimeInventory) field(fixture.session, "inventory");
            assertEquals(0, after.countCatalog(InventoryCatalog.LIQUOR));
            assertEquals(InventoryCatalog.LIQUOR,
                    fixture.hub.takeClassicWorldGroundItem(
                            4, -1, ground.objectId(), 2).taken()
                            .definition().catalogId());
        }
    }

    @Test void itemDeathSafeConsumesOneSlotThenExperienceStillApplies()
            throws Exception {
        try (Fixture fixture = fixture(
                temporary.resolve("death-item-safe.properties"),
                4, 33313, 32339,
                Map.of(InventoryCatalog.DEATH_SAFE_ITEM, 1,
                        InventoryCatalog.LIQUOR, 2))) {
            setInt(fixture.session, "playerLawful", -32_768);
            setInt(fixture.session, "classicDeathItemChanceRollOverride", 1);
            setInt(fixture.session, "classicDeathItemCountRollOverride", 1);
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            assertTrue(fixture.hub.classicWorldGroundItemsInScreen(
                    4, -1, 33313, 32339).isEmpty());
            assertTrue(indexOf(packets, Protocol.experience(22, EXPECTED_EXP)) >= 0);
            RuntimeInventory after = (RuntimeInventory) field(fixture.session, "inventory");
            assertEquals(0, after.countCatalog(InventoryCatalog.DEATH_SAFE_ITEM));
            assertEquals(2, after.countCatalog(InventoryCatalog.LIQUOR));
        }
    }

    @Test void selectedIneligibleRandomItemConsumesSlotWithoutReroll()
            throws Exception {
        try (Fixture fixture = fixture(
                temporary.resolve("death-item-ineligible.properties"),
                4, 33313, 32339, Map.of(InventoryCatalog.ADENA, 100))) {
            setInt(fixture.session, "playerLawful", -32_768);
            setInt(fixture.session, "classicDeathItemChanceRollOverride", 1);
            setInt(fixture.session, "classicDeathItemCountRollOverride", 1);
            setInt(fixture.session, "classicDeathItemSelectorOverride", 0);
            fixture.damage(50, 0x6500_1234L);
            fixture.packets();
            assertTrue(fixture.hub.classicWorldGroundItemsInScreen(
                    4, -1, 33313, 32339).isEmpty());
            RuntimeInventory after = (RuntimeInventory) field(fixture.session, "inventory");
            assertEquals(100, after.countCatalog(InventoryCatalog.ADENA));
        }
    }

    @Test void selectedSettingFourItemVanishesInsteadOfPublishingGround()
            throws Exception {
        try (Fixture fixture = fixture(
                temporary.resolve("death-item-vanish.properties"),
                4, 33313, 32339)) {
            InventoryCatalog.Definition star = new InventoryCatalog.Definition(
                    300087, 0xFF, 8101, 45, 0, true,
                    "一星珠", "一星珠", 0, 0, false, true,
                    BattleActionCodes.WALK, 0, InventoryCatalog.Effect.NONE,
                    1342, 3900);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(star, 1, () -> 0x7792_1001L));
            setField(fixture.session, "inventory", inventory);
            setInt(fixture.session, "playerLawful", -32_768);
            setInt(fixture.session, "classicDeathItemChanceRollOverride", 1);
            setInt(fixture.session, "classicDeathItemCountRollOverride", 1);
            setInt(fixture.session, "classicDeathItemSelectorOverride", 0);
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            assertTrue(fixture.hub.classicWorldGroundItemsInScreen(
                    4, -1, 33313, 32339).isEmpty());
            assertEquals(0, ((RuntimeInventory) field(fixture.session, "inventory"))
                    .countCatalog(300087));
            assertTrue(indexOf(packets, Protocol.messageCode(638, "一星珠")) >= 0);
        }
    }

    @Test void selectedCantDeleteItemConsumesRandomSlotWithoutLoss()
            throws Exception {
        try (Fixture fixture = fixture(
                temporary.resolve("death-item-cant-delete.properties"),
                4, 33313, 32339)) {
            InventoryCatalog.Definition protectedItem = new InventoryCatalog.Definition(
                    65010, 0xFF, 100, 101, 0, true,
                    "不可刪除物品", "", 0, 0, false, true,
                    BattleActionCodes.WALK, 0, InventoryCatalog.Effect.NONE,
                    0, 1000, 0, null, null, true);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(protectedItem, 1, () -> 0x7792_1002L));
            setField(fixture.session, "inventory", inventory);
            setInt(fixture.session, "playerLawful", -32_768);
            setInt(fixture.session, "classicDeathItemChanceRollOverride", 1);
            setInt(fixture.session, "classicDeathItemCountRollOverride", 1);
            setInt(fixture.session, "classicDeathItemSelectorOverride", 0);
            fixture.damage(50, 0x6500_1234L);
            fixture.packets();
            assertEquals(1, ((RuntimeInventory) field(fixture.session, "inventory"))
                    .countCatalog(65010));
        }
    }

    @Test void skillDeathSafeConsumesBeforeSkillSlotButDoesNotProtectExperience()
            throws Exception {
        try (Fixture fixture = fixture(
                temporary.resolve("death-skill-safe.properties"),
                4, 33313, 32339,
                Map.of(InventoryCatalog.DEATH_SAFE_SKILL, 1))) {
            setInt(fixture.session, "playerLawful", -32_768);
            setField(fixture.session, "learnedSkills", Set.of(1));
            setInt(fixture.session, "classicDeathSkillChanceRollOverride", 1);
            setInt(fixture.session, "classicDeathSkillCountRollOverride", 1);
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            assertEquals(-1, indexOf(packets, Protocol.removeSpells(Set.of(1))));
            assertTrue(indexOf(packets, Protocol.experience(22, EXPECTED_EXP)) >= 0);
            assertEquals(Set.of(1), field(fixture.session, "learnedSkills"));
            RuntimeInventory after = (RuntimeInventory) field(fixture.session, "inventory");
            assertEquals(0, after.countCatalog(InventoryCatalog.DEATH_SAFE_SKILL));
        }
    }

    @Test void selectedMap303DisablesDeathExperienceLoss() throws Exception {
        try (Fixture fixture = fixture(
                temporary.resolve("death-exp-disabled.properties"),
                303, 32635, 32817)) {
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            assertTrue(indexOf(packets, Protocol.objectAction(
                    fixture.session.playerObjectId(), BattleActionCodes.DIE)) >= 0);
            assertEquals(-1, indexOf(packets, Protocol.experience(22, EXPECTED_EXP)));
            assertEquals(SOURCE_EXP,
                    longField(fixture.session, "playerClassicExperience"));
        }
    }

    @Test void encounterGuardianReturnsAfterDeathActionBeforeEveryPenalty()
            throws Exception {
        long sourceExperience = ClassicExperienceCatalog.minimumExperienceForLevel(20)
                + 1_000;
        try (Fixture victim = fixture(
                temporary.resolve("death-encounter-guardian.properties"),
                4, 33313, 32339,
                Map.of(InventoryCatalog.LIQUOR, 2),
                "Guarded", 20, 0x7792_0200L, sourceExperience);
             Peer attacker = peer(victim, "HighLevel", 30,
                     0x7792_0300L, 33314, 32339)) {
            setInt(victim.session, "playerLawful", -32_768);
            setField(victim.session, "learnedSkills", Set.of(1));
            setInt(victim.session, "classicDeathItemChanceRollOverride", 1);
            setInt(victim.session, "classicDeathItemCountRollOverride", 1);
            setInt(victim.session, "classicDeathItemSelectorOverride", 0);
            setInt(victim.session, "classicDeathSkillChanceRollOverride", 1);
            setInt(victim.session, "classicDeathSkillCountRollOverride", 1);
            setInt(victim.session, "classicDeathSkillSelectorOverride", 0);

            victim.playerDamage(50, attacker.session);
            List<byte[]> packets = victim.packets();

            assertTrue(indexOf(packets, Protocol.objectAction(
                    victim.session.playerObjectId(), BattleActionCodes.DIE)) >= 0);
            assertEquals(sourceExperience,
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(2, ((RuntimeInventory) field(victim.session, "inventory"))
                    .countCatalog(InventoryCatalog.LIQUOR));
            assertEquals(Set.of(1), field(victim.session, "learnedSkills"));
            assertTrue(victim.hub.classicWorldGroundItemsInScreen(
                    4, -1, 33313, 32339).isEmpty());
            assertEquals(-1, indexOf(packets, Protocol.systemMessage(9,
                    "\\fX[HighLevel] \\fT擊殺 \\fR[Guarded]")));
            assertEquals(0, intField(attacker.session, "classicKillCount"));
            assertEquals(0, intField(victim.session, "classicDeathCount"));
            assertFalse(((ClassicWorldPinkNameStatus) field(
                    attacker.session, "classicWorldPinkName")).active(),
                    "a chaotic victim is not pink-name eligible");
        }
    }

    @Test void encounterGuardianSkipsGeneralPkButKeepsAcceptedHitPinkName()
            throws Exception {
        long sourceExperience = ClassicExperienceCatalog.minimumExperienceForLevel(20)
                + 1_000;
        try (Fixture victim = fixture(
                temporary.resolve("death-encounter-pk.properties"),
                4, 33313, 32339, Map.of(),
                "LawfulGuarded", 20, 0x7792_0202L, sourceExperience);
             Peer attacker = peer(victim, "Level30", 30,
                     0x7792_0302L, 33314, 32339)) {
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);

            victim.playerDamage(50, attacker.session);
            victim.packets();

            assertEquals(sourceExperience,
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(0, intField(attacker.session, "generalPkCount"));
            assertEquals(0, intField(attacker.session, "playerLawful"));
            assertTrue(((ClassicWorldPinkNameStatus) field(
                    attacker.session, "classicWorldPinkName")).active(),
                    "pink-name is committed by the accepted hit before Death returns");
        }
    }

    @Test void encounterGapNineStillAppliesExperienceAndGeneralPk()
            throws Exception {
        long sourceExperience = ClassicExperienceCatalog.minimumExperienceForLevel(20)
                + 1_000;
        try (Fixture victim = fixture(
                temporary.resolve("death-encounter-gap-nine.properties"),
                4, 33313, 32339, Map.of(),
                "Boundary", 20, 0x7792_0201L, sourceExperience);
             Peer attacker = peer(victim, "Level29", 29,
                     0x7792_0301L, 33314, 32339)) {
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);

            victim.playerDamage(50, attacker.session);
            victim.packets();

            assertEquals(sourceExperience - ClassicExperienceCatalog.deathPenalty(20),
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(1, intField(attacker.session, "generalPkCount"));
            assertTrue(intField(attacker.session, "playerLawful") < 0);
        }
    }

    @Test void activeCastleWarKeepsDeathPenaltyButSuppressesPkAndPink()
            throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-active-castle-war.properties"),
                4, 33089, 32717, Map.of(),
                "CastleVictim", 22, 0x7792_0203L, SOURCE_EXP);
             Peer attacker = peer(victim, "CastleAttacker", 22,
                     0x7792_0303L, 33090, 32717)) {
            ServerConfig config = (ServerConfig) field(victim.session, "config");
            long start = System.currentTimeMillis() - 60_000L;
            victim.hub.registerClassicCastleWarSchedules(
                    config.classicServerNames().get(0),
                    List.of(new CastleWarSchedule(
                            1, "肯特城", start, 60, 10_080)));
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);

            victim.playerDamage(50, attacker.session);
            victim.packets();

            assertEquals(EXPECTED_EXP,
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(0, intField(attacker.session, "generalPkCount"));
            assertEquals(0, intField(attacker.session, "playerLawful"));
            assertFalse(((ClassicWorldPinkNameStatus) field(
                    attacker.session, "classicWorldPinkName")).active());
            assertEquals(1, intField(attacker.session, "classicKillCount"));
            assertEquals(1, intField(victim.session, "classicDeathCount"));
        }
    }

    @Test void inactiveCastleAreaKeepsDeathPenaltyAndPinkButSuppressesPk()
            throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-inactive-castle-area.properties"),
                4, 33089, 32717, Map.of(),
                "CastleAreaVictim", 22, 0x7792_0204L, SOURCE_EXP);
             Peer attacker = peer(victim, "CastleAreaAttacker", 22,
                     0x7792_0304L, 33090, 32717)) {
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);

            victim.playerDamage(50, attacker.session);
            victim.packets();

            assertEquals(EXPECTED_EXP,
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(0, intField(attacker.session, "generalPkCount"));
            assertEquals(0, intField(attacker.session, "playerLawful"));
            assertTrue(((ClassicWorldPinkNameStatus) field(
                    attacker.session, "classicWorldPinkName")).active());
        }
    }

    @Test void clanWarKeepsDeathPenaltyAndPinkButSuppressesGeneralPk()
            throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-clan-war.properties"),
                4, 33313, 32339, Map.of(),
                "ClanVictim", 22, 0x7792_0205L, SOURCE_EXP);
             Peer attacker = peer(victim, "ClanAttacker", 22,
                     0x7792_0305L, 33314, 32339)) {
            ServerConfig config = (ServerConfig) field(victim.session, "config");
            String source = config.classicServerNames().get(0);
            String victimOwner = GameStateStore.sourceCharacter(
                    source, "VictimCrown");
            String attackerOwner = GameStateStore.sourceCharacter(
                    source, "AttackerCrown");
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(victimOwner, "VictimClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(attackerOwner, "AttackerClan"));
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    victim.hub.joinClassicClan(victimOwner,
                            victim.session.sourceCharacterIdentity(), 8).status());
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    victim.hub.joinClassicClan(attackerOwner,
                            attacker.session.sourceCharacterIdentity(), 8).status());
            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    victim.hub.resolveClassicClanWar(
                            victimOwner, attackerOwner,
                            "VictimClan", "AttackerClan",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);

            victim.playerDamage(50, attacker.session);
            victim.packets();

            assertEquals(EXPECTED_EXP,
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(0, intField(attacker.session, "generalPkCount"));
            assertEquals(0, intField(attacker.session, "playerLawful"));
            assertTrue(((ClassicWorldPinkNameStatus) field(
                    attacker.session, "classicWorldPinkName")).active());
            assertTrue(victim.hub.classicClanWarActiveBetween(
                    victim.session.sourceCharacterIdentity(),
                    attacker.session.sourceCharacterIdentity()));
        }
    }

    @Test void clanLeaderDeathEndsWarBeforePenaltyAndRetainsFatalWarExemption()
            throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-clan-leader.properties"),
                4, 33313, 32339, Map.of(),
                "VictimCrown", 22, 0x7792_0206L, SOURCE_EXP);
             Peer attacker = peer(victim, "AttackerCrown", 22,
                     0x7792_0306L, 33314, 32339)) {
            String victimOwner = victim.session.sourceCharacterIdentity();
            String attackerOwner = attacker.session.sourceCharacterIdentity();
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(victimOwner, "VictimClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(attackerOwner, "AttackerClan"));
            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    victim.hub.resolveClassicClanWar(
                            victimOwner, attackerOwner,
                            "VictimClan", "AttackerClan",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);
            victim.packets();
            attacker.packets();

            victim.playerDamage(50, attacker.session);
            List<byte[]> victimPackets = victim.packets();
            List<byte[]> attackerPackets = attacker.packets();
            byte[] death = Protocol.objectAction(
                    victim.session.playerObjectId(), BattleActionCodes.DIE);
            byte[] ended = Protocol.clanWar(
                    Protocol.ClanWarNoticeType.ENDED,
                    "VictimClan", "AttackerClan");
            byte[] experience = Protocol.experience(22, EXPECTED_EXP);
            byte[] announcement = Protocol.systemMessage(9,
                    "\\fX[AttackerCrown] \\fT擊殺 \\fR[VictimCrown]");

            int deathIndex = indexOf(victimPackets, death);
            int endedIndex = indexOf(victimPackets, ended);
            int announcementIndex = indexOf(victimPackets, announcement);
            int experienceIndex = indexOf(victimPackets, experience);
            assertTrue(deathIndex >= 0 && deathIndex < endedIndex
                            && endedIndex < announcementIndex
                            && announcementIndex < experienceIndex,
                    () -> victimPackets.stream().map(Protocol::hex).toList().toString());
            assertTrue(indexOf(attackerPackets, ended) >= 0);
            assertTrue(indexOf(attackerPackets, announcement) >= 0);
            assertEquals(EXPECTED_EXP,
                    longField(victim.session, "playerClassicExperience"));
            assertFalse(victim.hub.classicClanWarActiveBetween(
                    victimOwner, attackerOwner));
            assertEquals(0, intField(attacker.session, "generalPkCount"));
            assertEquals(0, intField(attacker.session, "playerLawful"));
            assertEquals(1, intField(attacker.session, "classicKillCount"));
            assertEquals(1, intField(victim.session, "classicDeathCount"));
            assertTrue(((ClassicWorldPinkNameStatus) field(
                    attacker.session, "classicWorldPinkName")).active());
        }
    }

    @Test void outsiderKillingClanLeaderEndsWarButStillRecordsGeneralPk()
            throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-clan-leader-outsider.properties"),
                4, 33313, 32339, Map.of(),
                "VictimCrown", 22, 0x7792_0207L, SOURCE_EXP);
             Peer enemy = peer(victim, "EnemyCrown", 22,
                     0x7792_0307L, 33314, 32339);
             Peer outsider = peer(victim, "Outsider", 22,
                     0x7792_0407L, 33315, 32339)) {
            String victimOwner = victim.session.sourceCharacterIdentity();
            String enemyOwner = enemy.session.sourceCharacterIdentity();
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(victimOwner, "VictimClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(enemyOwner, "EnemyClan"));
            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    victim.hub.resolveClassicClanWar(
                            victimOwner, enemyOwner, "VictimClan", "EnemyClan",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            setInt(victim.session, "playerLawful", 0);
            setInt(outsider.session, "playerLawful", 0);
            victim.packets();
            enemy.packets();
            outsider.packets();

            victim.playerDamage(50, outsider.session);
            byte[] ended = Protocol.clanWar(
                    Protocol.ClanWarNoticeType.ENDED,
                    "VictimClan", "EnemyClan");
            assertTrue(indexOf(victim.packets(), ended) >= 0);
            assertTrue(indexOf(enemy.packets(), ended) >= 0);
            assertEquals(-1, indexOf(outsider.packets(), ended),
                    "retained simulated-war end is sent only to both clans");
            assertFalse(victim.hub.classicClanWarActiveBetween(
                    victimOwner, enemyOwner));
            assertEquals(EXPECTED_EXP,
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(1, intField(outsider.session, "generalPkCount"));
            assertTrue(intField(outsider.session, "playerLawful") < 0);
        }
    }

    @Test void encounterReturnPrecedesClanLeaderDeathWarTermination()
            throws Exception {
        long sourceExperience = ClassicExperienceCatalog.minimumExperienceForLevel(20)
                + 1_000;
        try (Fixture victim = fixture(
                temporary.resolve("death-clan-leader-encounter.properties"),
                4, 33313, 32339, Map.of(),
                "YoungCrown", 20, 0x7792_0208L, sourceExperience);
             Peer attacker = peer(victim, "VeteranCrown", 30,
                     0x7792_0308L, 33314, 32339)) {
            String victimOwner = victim.session.sourceCharacterIdentity();
            String attackerOwner = attacker.session.sourceCharacterIdentity();
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(victimOwner, "YoungClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(attackerOwner, "VeteranClan"));
            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    victim.hub.resolveClassicClanWar(
                            victimOwner, attackerOwner, "YoungClan", "VeteranClan",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);
            victim.packets();
            attacker.packets();

            victim.playerDamage(50, attacker.session);
            List<byte[]> packets = victim.packets();
            byte[] ended = Protocol.clanWar(
                    Protocol.ClanWarNoticeType.ENDED,
                    "YoungClan", "VeteranClan");
            assertEquals(-1, indexOf(packets, ended));
            assertEquals(sourceExperience,
                    longField(victim.session, "playerClassicExperience"));
            assertTrue(victim.hub.classicClanWarActiveBetween(
                    victimOwner, attackerOwner));
            assertEquals(0, intField(attacker.session, "generalPkCount"));
            assertEquals(0, intField(attacker.session, "classicKillCount"));
            assertEquals(0, intField(victim.session, "classicDeathCount"));
        }
    }

    @Test void attackingCastleLeaderDeathCeasesSiegeBeforePenaltyOutsideField()
            throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-castle-attacking-leader.properties"),
                4, 33313, 32339, Map.of(),
                "SiegeCrown", 22, 0x7792_0209L, SOURCE_EXP);
             Peer attacker = peer(victim, "OutsideKiller", 22,
                     0x7792_0309L, 33314, 32339)) {
            ServerConfig config = (ServerConfig) field(victim.session, "config");
            String source = config.classicServerNames().get(0);
            long start = System.currentTimeMillis() - 60_000L;
            victim.hub.registerClassicCastleWarSchedules(source,
                    List.of(new CastleWarSchedule(
                            1, "肯特城", start, 60, 10_080)));
            String victimOwner = victim.session.sourceCharacterIdentity();
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(victimOwner, "SiegeClan"));
            assertEquals(GameHub.ClassicCastleSiegeMutation.DECLARED,
                    victim.hub.mutateClassicCastleSiege(
                            source, 1, "SiegeClan", "LegacyKent",
                            Protocol.ClanWarRequestType.DECLARE).mutation());
            setInt(victim.session, "playerLawful", 0);
            setInt(attacker.session, "playerLawful", 0);
            victim.packets();
            attacker.packets();

            victim.playerDamage(50, attacker.session);
            List<byte[]> packets = victim.packets();
            byte[] death = Protocol.objectAction(
                    victim.session.playerObjectId(), BattleActionCodes.DIE);
            byte[] ended = Protocol.clanWar(
                    Protocol.ClanWarNoticeType.ENDED,
                    "SiegeClan", "LegacyKent");
            byte[] experience = Protocol.experience(22, EXPECTED_EXP);
            assertTrue(indexOf(packets, death) < indexOf(packets, ended));
            assertTrue(indexOf(packets, ended) < indexOf(packets, experience));
            assertFalse(victim.hub.classicCastleSiegeParticipant(
                    source, 1, "SiegeClan"));
            assertEquals(EXPECTED_EXP,
                    longField(victim.session, "playerClassicExperience"));
            assertEquals(1, intField(attacker.session, "generalPkCount"),
                    "outside the castle field the leader-death cease is not a PK exemption");
            assertTrue(intField(attacker.session, "playerLawful") < 0);
        }
    }

    @Test void ordinaryPlayerKillAnnouncesBeforePenaltyAndPersistsBothCounters()
            throws Exception {
        Path stateFile = temporary.resolve("death-kill-feed.properties");
        try (Fixture victim = fixture(
                stateFile, 4, 33313, 32339, Map.of(),
                "FeedVictim", 22, 0x7792_0210L, SOURCE_EXP);
             Peer attacker = peer(victim, "FeedWinner", 22,
                     0x7792_0310L, 33314, 32339)) {
            victim.packets();
            attacker.packets();
            byte[] announcement = Protocol.systemMessage(9,
                    "\\fX[FeedWinner] \\fT擊殺 \\fR[FeedVictim]");

            victim.playerDamage(50, attacker.session);
            List<byte[]> victimPackets = victim.packets();
            List<byte[]> attackerPackets = attacker.packets();
            int death = indexOf(victimPackets, Protocol.objectAction(
                    victim.session.playerObjectId(), BattleActionCodes.DIE));
            int feed = indexOf(victimPackets, announcement);
            int experience = indexOf(victimPackets,
                    Protocol.experience(22, EXPECTED_EXP));
            assertTrue(death >= 0 && feed > death && experience > feed,
                    () -> victimPackets.stream().map(Protocol::hex).toList().toString());
            assertTrue(indexOf(attackerPackets, announcement) >= 0);
            assertEquals(1, intField(attacker.session, "classicKillCount"));
            assertEquals(1, intField(victim.session, "classicDeathCount"));

            GameStateStore reloaded = new GameStateStore(stateFile, true);
            assertEquals(new GameStateStore.ClassicKillDeathState(1, 0),
                    reloaded.reconcileClassicKillDeath(
                            attacker.session.sourceCharacterIdentity(),
                            SourceKillDeathCounts.none()).effective());
            assertEquals(new GameStateStore.ClassicKillDeathState(0, 1),
                    reloaded.reconcileClassicKillDeath(
                            victim.session.sourceCharacterIdentity(),
                            SourceKillDeathCounts.none()).effective());
        }
    }

    @Test void nativeSlaveTargetLetsWorldCompanionKillPlayerForItsOwner()
            throws Exception {
        Path stateFile = temporary.resolve("death-companion-player.properties");
        try (Fixture victim = fixture(
                stateFile, 4, 33313, 32339, Map.of(),
                "CompanionVictim", 22, 0x7792_0241L, SOURCE_EXP);
             Peer owner = peer(victim, "CompanionOwner", 22,
                     0x7792_0341L, 33314, 32339)) {
            setInt(victim.session, "playerLawful", 0);
            setInt(owner.session, "playerLawful", 0);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0341L, 45207, 2158, "$1791", 50,
                    200, 200, 20, -20, 30, 20, 33314, 32339);
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(45207));
            tamedCompanions(owner.session).add(companion);

            victim.packets();
            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, victim.session.playerObjectId());
            List<byte[]> prompt = owner.packets();
            assertTrue(indexOf(prompt,
                    Protocol.slaveControl(companion.snapshot().objectId(), 0)) >= 0);
            assertEquals(victim.session.playerObjectId(),
                    companion.snapshot().targetObjectId());

            long nowNanos = System.nanoTime();
            for (int attempt = 0;
                 attempt < 200 && intField(victim.session, "playerHp") > 0;
                 attempt++, nowNanos += 2_000_000_000L) {
                owner.session.serverClassicWorldCompanionTick(4, nowNanos);
            }

            List<byte[]> victimPackets = victim.packets();
            List<byte[]> ownerPackets = owner.packets();
            assertEquals(0, intField(victim.session, "playerHp"));
            assertTrue(hasPhysicalAttack(
                    victimPackets, companion.snapshot().objectId(),
                    victim.session.playerObjectId()));
            byte[] announcement = Protocol.systemMessage(9,
                    "\\fX[CompanionOwner] \\fT擊殺 \\fR[CompanionVictim]");
            assertTrue(indexOf(victimPackets, announcement) >= 0);
            assertTrue(indexOf(ownerPackets, announcement) >= 0);
            assertEquals(1, intField(owner.session, "classicKillCount"));
            assertEquals(1, intField(victim.session, "classicDeathCount"));
            assertEquals(1, intField(owner.session, "generalPkCount"));
            assertTrue(intField(owner.session, "playerLawful") < 0);
            assertEquals(0, companion.snapshot().targetObjectId());

            GameStateStore reloaded = new GameStateStore(stateFile, true);
            assertEquals(new GameStateStore.ClassicKillDeathState(1, 0),
                    reloaded.reconcileClassicKillDeath(
                            owner.session.sourceCharacterIdentity(),
                            SourceKillDeathCounts.none()).effective());
            assertEquals(new GameStateStore.ClassicKillDeathState(0, 1),
                    reloaded.reconcileClassicKillDeath(
                            victim.session.sourceCharacterIdentity(),
                            SourceKillDeathCounts.none()).effective());
        }
    }

    @Test void nativeSlaveTargetRejectsWorldPlayerInSafetyZone()
            throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-companion-safety.properties"),
                4, 33706, 32129, Map.of(),
                "SafeCompanionVictim", 22, 0x7792_0242L, SOURCE_EXP);
             Peer owner = peer(victim, "SafeCompanionOwner", 22,
                     0x7792_0342L, 33707, 32129)) {
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0342L, 45207, 2158, "$1791", 50,
                    200, 200, 20, -20, 30, 20, 33707, 32129);
            tamedCompanions(owner.session).add(companion);

            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, victim.session.playerObjectId());

            assertTrue(indexOf(owner.packets(),
                    Protocol.slaveControl(companion.snapshot().objectId(), 0)) >= 0);
            assertEquals(0, companion.snapshot().targetObjectId());
            assertEquals(50, intField(victim.session, "playerHp"));
        }
    }

    @Test void successfulWorldPlayerHitCommandsOnlyAggressiveCompanion()
            throws Exception {
        try (Fixture target = fixture(
                temporary.resolve("death-companion-master-hit.properties"),
                4, 33313, 32339, Map.of(),
                "MasterHitTarget", 22, 0x7792_0243L, SOURCE_EXP);
             Peer owner = peer(target, "MasterHitOwner", 22,
                     0x7792_0343L, 33314, 32339)) {
            setInt(target.session, "playerHp", 5_000);
            setInt(target.session, "playerMaxHp", 5_000);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0343L, 45207, 2158, "$1791", 50,
                    200, 200, 20, -20, 30, 20, 33314, 32339);
            assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED,
                    companion.apply("aggressive"));
            BattleCompanion staying = BattleCompanion.tamed(
                    0x7392_0443L, 45207, 2158, "$1791", 50,
                    200, 200, 20, -20, 30, 20, 33314, 32340);
            tamedCompanions(owner.session).add(companion);
            tamedCompanions(owner.session).add(staying);

            for (int attempt = 0;
                 attempt < 100 && companion.snapshot().targetObjectId() == 0;
                 attempt++) {
                invokeClassicWorldPlayerAttack(owner.session, target.session);
            }

            assertEquals(target.session.playerObjectId(),
                    companion.snapshot().targetObjectId());
            assertEquals(0, staying.snapshot().targetObjectId());
        }
    }

    @Test void worldCompanionCounterBarrierSuppressesStrikeAndRemovesFatalTame()
            throws Exception {
        Path stateFile = temporary.resolve("companion-counter-barrier.properties");
        try (Fixture defender = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-counter-barrier-selftest.properties"),
                stateFile, 4, 33313, 32339, Map.of(),
                "CompanionBarrier", 22, 0x7792_0244L, SOURCE_EXP);
             Peer owner = peer(defender, "BarrierCompanionOwner", 22,
                     0x7792_0344L, 33314, 32339)) {
            equipAndActivateCounterBarrier(defender.session);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0344L, 45207, 2158, "$1791", 50,
                    30, 30, 20, -20, 30, 20, 33314, 32339);
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(45207));
            tamedCompanions(owner.session).add(companion);
            defender.packets();
            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, defender.session.playerObjectId());
            owner.packets();
            assertTrue(owner.hub.classicWorldMembers(4, -1)
                    .containsAll(List.of(defender.session, owner.session)));
            BattleCompanion.Snapshot before = companion.snapshot();
            assertTrue(BattleSpellCatalog.isInNativeScreen(
                    intField(owner.session, "positionX"),
                    intField(owner.session, "positionY"),
                    before.x(), before.y()),
                    () -> "owner=" + intFieldUnchecked(owner.session, "positionX")
                            + "," + intFieldUnchecked(owner.session, "positionY")
                            + " companion=" + before.x() + "," + before.y());

            long nowNanos = System.nanoTime();
            for (int attempt = 0;
                 attempt < 100 && companion.snapshot().active();
                 attempt++, nowNanos += 2_000_000_000L) {
                owner.session.serverClassicWorldCompanionTick(4, nowNanos);
            }

            List<byte[]> ownerPackets = owner.packets();
            List<byte[]> defenderPackets = defender.packets();
            assertTrue(owner.hub.classicWorldMembers(4, -1)
                    .containsAll(List.of(defender.session, owner.session)));
            assertEquals(50, intField(defender.session, "playerHp"),
                    "Counter Barrier suppresses the original companion strike");
            assertEquals(0, companion.snapshot().hp());
            assertFalse(companion.snapshot().active());
            assertTrue(tamedCompanions(owner.session).isEmpty());
            int barrierIndex = indexOf(defenderPackets, Protocol.effectObject(
                    defender.session.playerObjectId(), 10710));
            int reflectedDamageIndex = indexOf(defenderPackets,
                    Protocol.objectAction(companion.snapshot().objectId(),
                            BattleActionCodes.DAMAGE));
            assertTrue(barrierIndex >= 0);
            assertTrue(reflectedDamageIndex > barrierIndex);
            assertFalse(hasPhysicalAttackAfter(defenderPackets,
                    companion.snapshot().objectId(),
                    defender.session.playerObjectId(), barrierIndex));
            assertTrue(indexOf(defenderPackets,
                    Protocol.removeObject(companion.snapshot().objectId())) >= 0,
                    () -> defenderPackets.stream().map(Protocol::hex)
                            .toList().toString());
            byte[] remove = Protocol.removeObject(companion.snapshot().objectId());
            String ownerCapture = readUnchecked(owner.capture.path());
            assertTrue(indexOf(ownerPackets, remove) >= 0
                            || ownerCapture.contains("\"plain\":\""
                                    + Protocol.hex(remove) + "\"")
                            && ownerCapture.contains(
                                    "S_REMOVE_OBJECT classic-world-companion="
                                            + companion.snapshot().objectId()),
                    () -> "packets=" + ownerPackets.stream().map(Protocol::hex)
                            .toList() + " capture=" + ownerCapture);
            assertEquals(0, intField(defender.session, "generalPkCount"));
        }
    }

    @Test void worldCompanionCounterBarrierRequiresTwoHandSword()
            throws Exception {
        Path stateFile = temporary.resolve("companion-counter-barrier-weapon.properties");
        try (Fixture defender = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-counter-barrier-selftest.properties"),
                stateFile, 4, 33313, 32339, Map.of(),
                "CompanionBarrierNoSword", 22, 0x7792_0245L, SOURCE_EXP);
             Peer owner = peer(defender, "BarrierNoSwordOwner", 22,
                     0x7792_0345L, 33314, 32339)) {
            ((BattleAbilityStatus) field(defender.session, "abilityStatus"))
                    .activateCounterBarrier(System.nanoTime(), 64);
            setInt(defender.session, "playerHp", 5_000);
            setInt(defender.session, "playerMaxHp", 5_000);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0345L, 45207, 2158, "$1791", 50,
                    200, 200, 20, -20, 30, 20, 33314, 32339);
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(45207));
            tamedCompanions(owner.session).add(companion);
            defender.packets();
            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, defender.session.playerObjectId());
            owner.packets();

            long nowNanos = System.nanoTime();
            for (int attempt = 0;
                 attempt < 100 && intField(defender.session, "playerHp") == 5_000;
                 attempt++, nowNanos += 2_000_000_000L) {
                owner.session.serverClassicWorldCompanionTick(4, nowNanos);
            }

            List<byte[]> packets = defender.packets();
            assertTrue(intField(defender.session, "playerHp") < 5_000);
            assertEquals(200, companion.snapshot().hp());
            assertTrue(hasPhysicalAttack(packets, companion.snapshot().objectId(),
                    defender.session.playerObjectId()));
            assertEquals(-1, indexOf(packets, Protocol.effectObject(
                    defender.session.playerObjectId(), 10710)));
            assertEquals(0, ((BattleAbilityStatus) field(
                    defender.session, "abilityStatus"))
                    .counterBarrier(System.nanoTime()).remainingSeconds());
        }
    }

    @Test void worldRangedCompanionUsesNativeArrowWithoutMovingOrCounterBarrier()
            throws Exception {
        Path stateFile = temporary.resolve("companion-ranged-arrow.properties");
        try (Fixture defender = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-counter-barrier-selftest.properties"),
                stateFile, 4, 33313, 32339, Map.of(),
                "CompanionRangedTarget", 22, 0x7792_0248L, SOURCE_EXP);
             Peer owner = peer(defender, "RangedCompanionOwner", 22,
                     0x7792_0348L, 33314, 32339)) {
            equipAndActivateCounterBarrier(defender.session);
            setInt(defender.session, "playerHp", 5_000);
            setInt(defender.session, "playerMaxHp", 5_000);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0348L, 81227, 1832, "$1572", 40,
                    220, 220, 110, -5, 13, 16, 33311, 32339);
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81227));
            tamedCompanions(owner.session).add(companion);
            BattleCompanion.Snapshot before = companion.snapshot();
            defender.packets();
            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, defender.session.playerObjectId());
            owner.packets();

            long nowNanos = System.nanoTime();
            for (int attempt = 0;
                 attempt < 100 && intField(defender.session, "playerHp") == 5_000;
                 attempt++, nowNanos += 3_000_000_000L) {
                owner.session.serverClassicWorldCompanionTick(4, nowNanos);
            }

            List<byte[]> packets = defender.packets();
            assertTrue(intField(defender.session, "playerHp") < 5_000,
                    () -> "companion=" + companion.snapshot()
                            + " packets=" + packets.stream().map(Protocol::hex)
                                    .toList()
                            + " capture=" + readUnchecked(owner.capture.path()));
            assertEquals(before.x(), companion.snapshot().x());
            assertEquals(before.y(), companion.snapshot().y());
            assertEquals(220, companion.snapshot().hp());
            assertTrue(hasRangedPhysicalAttack(packets,
                    companion.snapshot().objectId(),
                    defender.session.playerObjectId(), 66,
                    before.x(), before.y(), 33313, 32339),
                    () -> packets.stream().map(Protocol::hex).toList().toString());
            assertEquals(-1, indexOf(packets, Protocol.effectObject(
                    defender.session.playerObjectId(), 10710)));
            assertTrue(((BattleAbilityStatus) field(
                    defender.session, "abilityStatus"))
                    .counterBarrier(System.nanoTime()).remainingSeconds() > 0);
        }
    }

    @Test void worldCompanionRetainsNpcDamagePoisonAndNativeTickLifecycle()
            throws Exception {
        Path stateFile = temporary.resolve("companion-damage-poison.properties");
        try (Fixture defender = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-poison-selftest.properties"),
                stateFile, 4, 33313, 32339, Map.of(),
                "CompanionPoisonTarget", 22, 0x7792_0249L, SOURCE_EXP);
             Peer owner = peer(defender, "PoisonCompanionOwner", 22,
                     0x7792_0349L, 33314, 32339)) {
            setInt(defender.session, "playerHp", 5_000);
            setInt(defender.session, "playerMaxHp", 5_000);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0349L, 81217, 4205, "$3378", 28,
                    240, 240, 80, 0, 16, 14, 33314, 32339);
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(81217));
            tamedCompanions(owner.session).add(companion);
            defender.packets();
            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, defender.session.playerObjectId());
            owner.packets();

            BattleTimedStatus targetStatus = (BattleTimedStatus) field(
                    defender.session, "timedStatus");
            long nowNanos = System.nanoTime();
            long poisonStartedAt = 0;
            for (int attempt = 0; attempt < 100 && poisonStartedAt == 0;
                 attempt++, nowNanos += 2_000_000_000L) {
                owner.session.serverClassicWorldCompanionTick(4, nowNanos);
                if (targetStatus.poisonActive(nowNanos)) {
                    poisonStartedAt = nowNanos;
                }
            }

            assertTrue(poisonStartedAt > 0);
            assertEquals(1, companion.poisonAttack());
            assertEquals(0, companion.paralysisAttack());
            List<byte[]> attackPackets = defender.packets();
            int poisonPacket = indexOf(attackPackets, Protocol.poison(
                    defender.session.playerObjectId(), 1));
            assertTrue(poisonPacket >= 0);
            assertTrue(hasPhysicalAttackAfter(
                            attackPackets, companion.snapshot().objectId(),
                            defender.session.playerObjectId(), poisonPacket),
                    () -> attackPackets.stream().map(Protocol::hex).toList().toString());

            int beforeTick = intField(defender.session, "playerHp");
            defender.session.serverExpireTimedStatuses(
                    poisonStartedAt + 3_000_000_000L);
            assertEquals(beforeTick - 5, intField(defender.session, "playerHp"));
            assertTrue(targetStatus.poisonActive(
                    poisonStartedAt + 3_000_000_000L));
        }
    }

    @Test void worldMortalBodyFatalFeedbackUsesTransientCompanionRemovalLifecycle()
            throws Exception {
        Path stateFile = temporary.resolve("companion-mortal-body.properties");
        try (Fixture defender = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-counter-barrier-selftest.properties"),
                stateFile, 4, 33313, 32339, Map.of(),
                "CompanionMortalBody", 22, 0x7792_0246L, SOURCE_EXP);
             Peer owner = peer(defender, "MortalBodyCompanionOwner", 22,
                     0x7792_0346L, 33314, 32339)) {
            ((BattleAbilityStatus) field(defender.session, "abilityStatus"))
                    .activateMortalBody(System.nanoTime(), 300);
            setInt(defender.session, "playerHp", 5_000);
            setInt(defender.session, "playerMaxHp", 5_000);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0346L, 45207, 2158, "$1791", 50,
                    20, 20, 20, -20, 30, 20, 33314, 32339);
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(45207));
            tamedCompanions(owner.session).add(companion);
            defender.packets();
            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, defender.session.playerObjectId());
            owner.packets();

            long nowNanos = System.nanoTime();
            for (int attempt = 0;
                 attempt < 100 && companion.snapshot().active();
                 attempt++, nowNanos += 2_000_000_000L) {
                owner.session.serverClassicWorldCompanionTick(4, nowNanos);
            }

            List<byte[]> ownerPackets = owner.packets();
            List<byte[]> defenderPackets = defender.packets();
            assertTrue(intField(defender.session, "playerHp") < 5_000);
            assertTrue(intField(defender.session, "playerHp") > 0);
            assertEquals(0, companion.snapshot().hp());
            assertFalse(companion.snapshot().active());
            assertTrue(tamedCompanions(owner.session).isEmpty());
            assertTrue(hasPhysicalAttack(defenderPackets,
                    companion.snapshot().objectId(),
                    defender.session.playerObjectId()));
            assertTrue(indexOf(defenderPackets, Protocol.objectAction(
                    companion.snapshot().objectId(), BattleActionCodes.DAMAGE)) >= 0);
            assertCompanionRemovalDelivered(
                    owner, ownerPackets, defenderPackets, companion);
        }
    }

    @Test void worldJoyOfPainFatalFeedbackUsesTransientCompanionRemovalLifecycle()
            throws Exception {
        Path stateFile = temporary.resolve("companion-joy-of-pain.properties");
        try (Fixture defender = fixture(
                Path.of("src/test/resources/server-config/config/server.world-companion-counter-barrier-selftest.properties"),
                stateFile, 4, 33313, 32339, Map.of(),
                "CompanionJoyOfPain", 22, 0x7792_0247L, SOURCE_EXP);
             Peer owner = peer(defender, "JoyOfPainCompanionOwner", 22,
                     0x7792_0347L, 33314, 32339)) {
            setInt(defender.session, "playerHp", 4_000);
            setInt(defender.session, "playerMaxHp", 5_000);
            BattleCompanion companion = BattleCompanion.tamed(
                    0x7392_0347L, 45207, 2158, "$1791", 50,
                    100, 100, 20, -20, 30, 20, 33314, 32339);
            companion.qualifyNpcDefense(
                    SourceNpcPhysicalStats.memoryFixture(45207));
            companion.activateJoyOfPain(System.nanoTime(), 600);
            tamedCompanions(owner.session).add(companion);
            defender.packets();
            owner.packets();
            requestCompanionPlayerTarget(
                    owner.session, companion, defender.session.playerObjectId());
            owner.packets();

            long nowNanos = System.nanoTime();
            for (int attempt = 0;
                 attempt < 100 && companion.snapshot().active();
                 attempt++, nowNanos += 2_000_000_000L) {
                owner.session.serverClassicWorldCompanionTick(4, nowNanos);
            }

            List<byte[]> ownerPackets = owner.packets();
            List<byte[]> defenderPackets = defender.packets();
            assertTrue(intField(defender.session, "playerHp") < 4_000);
            assertTrue(intField(defender.session, "playerHp") > 0);
            assertEquals(0, companion.snapshot().hp());
            assertFalse(companion.snapshot().active());
            assertTrue(tamedCompanions(owner.session).isEmpty());
            assertTrue(hasPhysicalAttack(defenderPackets,
                    companion.snapshot().objectId(),
                    defender.session.playerObjectId()));
            assertTrue(indexOf(defenderPackets, Protocol.objectAction(
                    companion.snapshot().objectId(), BattleActionCodes.DAMAGE)) >= 0);
            assertCompanionRemovalDelivered(
                    owner, ownerPackets, defenderPackets, companion);
        }
    }

    @Test void ordinaryCombatZoneKillSuppressesFeedAndCounters() throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-kill-feed-combat.properties"),
                4, 34053, 32230, Map.of(),
                "CombatVictim", 22, 0x7792_0211L, SOURCE_EXP);
             Peer attacker = peer(victim, "CombatWinner", 22,
                     0x7792_0311L, 34054, 32230)) {
            victim.packets();
            attacker.packets();
            byte[] announcement = Protocol.systemMessage(9,
                    "\\fX[CombatWinner] \\fT擊殺 \\fR[CombatVictim]");

            victim.playerDamage(50, attacker.session);
            assertEquals(-1, indexOf(victim.packets(), announcement));
            assertEquals(-1, indexOf(attacker.packets(), announcement));
            assertEquals(0, intField(attacker.session, "classicKillCount"));
            assertEquals(0, intField(victim.session, "classicDeathCount"));
        }
    }

    @Test void clanWarOverridesCombatZoneFeedSuppression() throws Exception {
        try (Fixture victim = fixture(
                temporary.resolve("death-kill-feed-war-combat.properties"),
                4, 34053, 32230, Map.of(),
                "WarFeedVictim", 22, 0x7792_0212L, SOURCE_EXP);
             Peer attacker = peer(victim, "WarFeedWinner", 22,
                     0x7792_0312L, 34054, 32230)) {
            ServerConfig config = (ServerConfig) field(victim.session, "config");
            String source = config.classicServerNames().get(0);
            String victimOwner = GameStateStore.sourceCharacter(
                    source, "WarFeedVictimOwner");
            String attackerOwner = GameStateStore.sourceCharacter(
                    source, "WarFeedWinnerOwner");
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(victimOwner, "WarFeedVictimClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    victim.hub.createClassicClan(attackerOwner, "WarFeedWinnerClan"));
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    victim.hub.joinClassicClan(victimOwner,
                            victim.session.sourceCharacterIdentity(), 8).status());
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    victim.hub.joinClassicClan(attackerOwner,
                            attacker.session.sourceCharacterIdentity(), 8).status());
            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    victim.hub.resolveClassicClanWar(
                            victimOwner, attackerOwner,
                            "WarFeedVictimClan", "WarFeedWinnerClan",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            victim.packets();
            attacker.packets();
            byte[] announcement = Protocol.systemMessage(9,
                    "\\fX[WarFeedWinner] \\fT擊殺 \\fR[WarFeedVictim]");

            victim.playerDamage(50, attacker.session);
            assertTrue(indexOf(victim.packets(), announcement) >= 0);
            assertTrue(indexOf(attacker.packets(), announcement) >= 0);
            assertEquals(1, intField(attacker.session, "classicKillCount"));
            assertEquals(1, intField(victim.session, "classicDeathCount"));
        }
    }

    @Test void lowLevelVictimAndGmAttackerSuppressFeedAndCounters()
            throws Exception {
        long lowExperience = ClassicExperienceCatalog.minimumExperienceForLevel(9)
                + 100;
        try (Fixture lowVictim = fixture(
                temporary.resolve("death-kill-feed-low.properties"),
                4, 33313, 32339, Map.of(),
                "LowVictim", 9, 0x7792_0213L, lowExperience);
             Peer ordinary = peer(lowVictim, "OrdinaryWinner", 9,
                     0x7792_0313L, 33314, 32339)) {
            lowVictim.packets();
            ordinary.packets();
            lowVictim.playerDamage(50, ordinary.session);
            assertEquals(0, intField(ordinary.session, "classicKillCount"));
            assertEquals(0, intField(lowVictim.session, "classicDeathCount"));
        }

        try (Fixture victim = fixture(
                temporary.resolve("death-kill-feed-gm.properties"),
                4, 33313, 32339, Map.of(),
                "GmVictim", 22, 0x7792_0214L, SOURCE_EXP);
             Peer administrator = peer(victim, "GmWinner", 22,
                     0x7792_0314L, 33314, 32339)) {
            MemoryAccountRepository repository =
                    (MemoryAccountRepository) field(administrator.session, "accounts");
            setField(administrator.session, "accounts", AccountAccessFixture.grant(repository, "test", 200));
            SourceAdministration.clear(administrator.session);
            setField(administrator.session, "account", "test");
            victim.packets();
            administrator.packets();
            victim.playerDamage(50, administrator.session);
            assertEquals(0, intField(administrator.session, "classicKillCount"));
            assertEquals(0, intField(victim.session, "classicDeathCount"));
        }
    }

    private void assertNpcDeathPenaltySuppressed(String file, int x, int y)
            throws Exception {
        try (Fixture fixture = fixture(temporary.resolve(file), 4, x, y)) {
            fixture.damage(50, 0x6500_1234L);
            List<byte[]> packets = fixture.packets();
            assertTrue(indexOf(packets, Protocol.objectAction(
                    fixture.session.playerObjectId(), BattleActionCodes.DIE)) >= 0);
            assertEquals(-1, indexOf(packets, Protocol.experience(22, EXPECTED_EXP)));
            assertEquals(SOURCE_EXP,
                    longField(fixture.session, "playerClassicExperience"));
        }
    }

    private Fixture fixture(Path stateFile, int mapId, int x, int y)
            throws Exception {
        return fixture(stateFile, mapId, x, y, Map.of());
    }

    private Fixture fixture(Path stateFile, int mapId, int x, int y,
                            Map<Long, Integer> inventoryCounts)
            throws Exception {
        return fixture(stateFile, mapId, x, y, inventoryCounts,
                "DeathExp", 22, 0x7792_0117L, SOURCE_EXP);
    }

    private Fixture fixture(Path stateFile, int mapId, int x, int y,
                            Map<Long, Integer> inventoryCounts,
                            String name, int level, long objectId,
                            long sourceExperience)
            throws Exception {
        return fixture(Path.of("src/test/resources/server-config/config/server.selftest.properties"),
                stateFile, mapId, x, y, inventoryCounts, name, level,
                objectId, sourceExperience);
    }

    private Fixture fixture(Path configFile, Path stateFile,
                            int mapId, int x, int y,
                            Map<Long, Integer> inventoryCounts,
                            String name, int level, long objectId,
                            long sourceExperience)
            throws Exception {
        ServerConfig config = ServerConfig.load(configFile);
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(stateFile, true);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(500);
        CaptureLog capture = new CaptureLog(temporary.resolve(
                "capture-" + mapId + "-" + objectId));
        ClientSession session = new ClientSession(server,
                ClientSession.Endpoint.GAME, config,
                new MemoryAccountRepository("test", "test"),
                state, capture, hub);
        int seed = 0x7920_0000 | mapId;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "death-exp-account-" + objectId);
        setField(session, "selectedCharacter", name);
        setInt(session, "currentChannelId", 0);
        CharacterSummary selected = character(
                name, level, objectId, sourceExperience, mapId, x, y);
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        AtomicLong objectIds = new AtomicLong(0x7792_0000L);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                inventoryCounts, objectIds::incrementAndGet));
        setInt(session, "classicDeathItemChanceRollOverride", 100);
        setInt(session, "playerHp", 50);
        setInt(session, "playerMaxHp", 50);
        setInt(session, "playerMp", 30);
        setInt(session, "playerMaxMp", 30);
        setLong(session, "playerClassicExperience", sourceExperience);
        setInt(session, "playerClassicLevel", level);
        setInt(session, "classicWorldMapId", mapId);
        setInt(session, "classicWorldInstanceId", -1);
        setInt(session, "positionX", x);
        setInt(session, "positionY", y);
        state.reconcileClassicExperience(
                session.sourceCharacterIdentity(), sourceExperience, level);
        hub.connect(session);
        hub.enterClassicWorld(session, mapId, x, y, 0);
        return new Fixture(hub, capture, client, server, session, receiver);
    }

    private static CharacterSummary character(int mapId, int x, int y) {
        return character("DeathExp", 22, 0x7792_0117L, SOURCE_EXP,
                mapId, x, y);
    }

    private static CharacterSummary character(
            String name, int level, long objectId, long experience,
            int mapId, int x, int y) {
        return new CharacterSummary(name, "", 1, 0, level,
                50, 30, 0, 14, 14, 12, 12, 12, 12, 14, 14,
                objectId, experience, 0,
                0, 0, 0, 0, 2, x, y, mapId,
                14, 40, 1, 14, 12, 0);
    }

    private Peer peer(Fixture owner, String name, int level, long objectId,
                      int x, int y) throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(500);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-peer-" + objectId));
        ServerConfig config = (ServerConfig) field(owner.session, "config");
        GameStateStore state = (GameStateStore) field(owner.session, "state");
        ClientSession session = new ClientSession(server,
                ClientSession.Endpoint.GAME, config,
                new MemoryAccountRepository("test", "test"),
                state, capture, owner.hub);
        int seed = (int) (0x7930_0000L | (objectId & 0xFFFF));
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "death-peer-account-" + objectId);
        setField(session, "selectedCharacter", name);
        setInt(session, "currentChannelId", 0);
        long experience = ClassicExperienceCatalog.minimumExperienceForLevel(level)
                + 1_000;
        CharacterSummary selected = character(
                name, level, objectId, experience, 4, x, y);
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "inventory", new RuntimeInventory());
        setInt(session, "playerHp", 50);
        setInt(session, "playerMaxHp", 50);
        setInt(session, "playerMp", 30);
        setInt(session, "playerMaxMp", 30);
        setLong(session, "playerClassicExperience", experience);
        setInt(session, "playerClassicLevel", level);
        setInt(session, "classicWorldMapId", 4);
        setInt(session, "classicWorldInstanceId", -1);
        setInt(session, "positionX", x);
        setInt(session, "positionY", y);
        state.reconcileClassicExperience(
                session.sourceCharacterIdentity(), experience, level);
        owner.hub.connect(session);
        owner.hub.enterClassicWorld(session, 4, x, y, 0);
        return new Peer(owner.hub, capture, client, server, session, receiver);
    }

    private static int indexOf(List<byte[]> packets, byte[] expected) {
        for (int index = 0; index < packets.size(); index++) {
            if (Arrays.equals(packets.get(index), expected)) return index;
        }
        return -1;
    }

    private static int intFieldUnchecked(Object target, String name) {
        try {
            return intField(target, name);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static void assertCompanionRemovalDelivered(
            Peer owner, List<byte[]> ownerPackets, List<byte[]> observerPackets,
            BattleCompanion companion) {
        byte[] remove = Protocol.removeObject(companion.snapshot().objectId());
        assertTrue(indexOf(observerPackets, remove) >= 0,
                () -> observerPackets.stream().map(Protocol::hex)
                        .toList().toString());
        String ownerCapture = readUnchecked(owner.capture.path());
        assertTrue(indexOf(ownerPackets, remove) >= 0
                        || ownerCapture.contains("\"plain\":\""
                                + Protocol.hex(remove) + "\"")
                        && ownerCapture.contains(
                                "S_REMOVE_OBJECT classic-world-companion="
                                        + companion.snapshot().objectId()),
                () -> "packets=" + ownerPackets.stream().map(Protocol::hex)
                        .toList() + " capture=" + ownerCapture);
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception error) {
            return error.toString();
        }
    }

    private static boolean hasPhysicalAttack(
            List<byte[]> packets, long attackerObjectId, long targetObjectId) {
        return hasPhysicalAttackAfter(packets, attackerObjectId, targetObjectId, -1);
    }

    private static int firstPhysicalAttackIndex(
            List<byte[]> packets, long attackerObjectId, long targetObjectId) {
        for (int index = 0; index < packets.size(); index++) {
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

    private static boolean hasPhysicalAttackAfter(
            List<byte[]> packets, long attackerObjectId, long targetObjectId,
            int exclusiveIndex) {
        for (int index = exclusiveIndex + 1; index < packets.size(); index++) {
            byte[] packet = packets.get(index);
            if (packet.length < 10 || Byte.toUnsignedInt(packet[0])
                    != Protocol.S_ATTACK) continue;
            Protocol.Reader in = new Protocol.Reader(packet);
            in.c();
            in.c();
            if (in.d() == attackerObjectId && in.d() == targetObjectId) return true;
        }
        return false;
    }

    private static boolean hasRangedPhysicalAttack(
            List<byte[]> packets, long attackerObjectId, long targetObjectId,
            int bowActionId, int attackerX, int attackerY,
            int targetX, int targetY) {
        for (byte[] packet : packets) {
            if (packet.length < 28 || Byte.toUnsignedInt(packet[0])
                    != Protocol.S_ATTACK) continue;
            Protocol.Reader in = new Protocol.Reader(packet);
            in.c();
            int action = in.c();
            long attacker = in.d();
            long target = in.d();
            int damage = in.c();
            int heading = in.c();
            long sequence = in.d();
            int projectile = in.h();
            int flightType = in.c();
            int sourceX = in.h();
            int sourceY = in.h();
            int destinationX = in.h();
            int destinationY = in.h();
            if (action == BattleActionCodes.ATTACK
                    && attacker == attackerObjectId && target == targetObjectId
                    && damage > 0 && heading >= 0 && heading <= 7 && sequence > 0
                    && projectile == bowActionId && flightType == 0x7f
                    && sourceX == attackerX && sourceY == attackerY
                    && destinationX == targetX && destinationY == targetY) {
                return true;
            }
        }
        return false;
    }

    private static void requestCompanionPlayerTarget(
            ClientSession owner, BattleCompanion companion, long targetObjectId)
            throws Exception {
        Method action = ClientSession.class.getDeclaredMethod(
                "handleClassicWorldCompanionAction",
                Protocol.HypertextActionRequest.class);
        action.setAccessible(true);
        action.invoke(owner, new Protocol.HypertextActionRequest(
                companion.snapshot().objectId(), "attackchr", "",
                Protocol.HypertextActionKind.ATTACK_TARGET));
        Method selected = ClientSession.class.getDeclaredMethod(
                "slaveControl", byte[].class);
        selected.setAccessible(true);
        selected.invoke(owner, (Object) new Protocol.Writer()
                .c(Protocol.C_SLAVE_CONTROL)
                .d(companion.snapshot().objectId()).c(0).d(targetObjectId)
                .bytes());
    }

    private static void equipAndActivateCounterBarrier(ClientSession defender)
            throws Exception {
        ServerConfig config = (ServerConfig) field(defender, "config");
        try (AccountRepository repository = AccountRepository.open(config)) {
            setField(defender, "sourceEquipmentMagicStats",
                    repository.equipmentMagicStats(20));
        }
        ((BattleAbilityStatus) field(defender, "abilityStatus"))
                .activateCounterBarrier(System.nanoTime(), 64);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeClassicWorldPlayerAttack(
            ClientSession attacker, ClientSession target) throws Exception {
        Class<? extends Enum> envelopeType = (Class<? extends Enum>) Class.forName(
                "local.lito381.server.ClientSession$AttackEnvelope");
        Object direct = Enum.valueOf(envelopeType, "DIRECT");
        Method method = ClientSession.class.getDeclaredMethod(
                "attackClassicWorldPlayer", Protocol.AttackRequest.class,
                envelopeType, ClientSession.class);
        method.setAccessible(true);
        method.invoke(attacker, new Protocol.AttackRequest(
                target.playerObjectId(), intField(target, "positionX"),
                intField(target, "positionY")), direct, target);
    }

    @SuppressWarnings("unchecked")
    private static List<BattleCompanion> tamedCompanions(ClientSession session)
            throws Exception {
        return (List<BattleCompanion>) field(session, "tamedCompanions");
    }

    private static int countOf(List<byte[]> packets, byte[] expected) {
        int count = 0;
        for (byte[] packet : packets) {
            if (Arrays.equals(packet, expected)) count++;
        }
        return count;
    }

    private static int intField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static long longField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getLong(target);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setInt(Object target, String name, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static void setLong(Object target, String name, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setLong(target, value);
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
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(GameHub hub, CaptureLog capture, Socket client,
                           Socket server, ClientSession session,
                           Protocol.CipherState receiver) implements AutoCloseable {
        void damage(double damage, long attackerObjectId) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "takeClassicWorldDirectDamage", double.class, long.class);
            method.setAccessible(true);
            method.invoke(session, damage, attackerObjectId);
        }

        void playerDamage(int damage, ClientSession attacker) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "takeClassicWorldPlayerDamage", int.class, ClientSession.class);
            method.setAccessible(true);
            method.invoke(session, damage, attacker);
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(client.getInputStream(), 65_535);
                    if (encrypted == null) return result;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
        }

        @Override public void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
        }
    }

    private record Peer(GameHub hub, CaptureLog capture, Socket client,
                        Socket server, ClientSession session,
                        Protocol.CipherState receiver) implements AutoCloseable {
        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(client.getInputStream(), 65_535);
                    if (encrypted == null) return result;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
        }

        @Override public void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
        }
    }
}
