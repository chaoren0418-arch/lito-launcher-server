package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class BuddyGroupTest {
    @Test
    void createsRenamesMovesAndRemovesNativeBuddyGroups() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            GameHub.BuddyData initial = hub.buddyData("TestKnight", -1);
            assertEquals(List.of(new GameHub.BuddyGroup(1, "Friends")), initial.groups());
            assertEquals("ArenaGuide", initial.buddies().get(0).name());

            int groupId = hub.createBuddyGroup("TestKnight", "Raid");
            assertEquals(2, groupId);
            assertTrue(hub.renameBuddyGroup("TestKnight", groupId, "Raiders"));
            assertTrue(hub.changeBuddyGroup("TestKnight", "ArenaGuide", groupId));
            assertEquals(groupId, hub.buddyData("TestKnight", -1).buddies().get(0).groupId());

            List<String> moved = hub.removeBuddyGroup("TestKnight", groupId);
            assertNotNull(moved);
            assertEquals(List.of("ArenaGuide"), moved);
            GameHub.BuddyData removed = hub.buddyData("TestKnight", -1);
            assertEquals(1, removed.buddies().get(0).groupId());
            assertFalse(removed.groups().stream().anyMatch(group -> group.id() == groupId));
        }
    }

    @Test
    void rejectsDefaultGroupRemovalAndDuplicateNames() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            hub.buddyData("TestKnight", -1);
            assertEquals(-1, hub.createBuddyGroup("TestKnight", "friends"));
            assertEquals(null, hub.removeBuddyGroup("TestKnight", 1));
        }
    }

    @Test
    void addsRemovesAndEnforcesNativeOneByteBuddyCapacity() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            hub.buddyData("TestKnight", -1); // Adds the deterministic ArenaGuide row.
            assertEquals(GameHub.BuddyAddResult.ADDED,
                    hub.addBuddy("TestKnight", "ManualBuddy", "Local", false));
            assertEquals(GameHub.BuddyAddResult.DUPLICATE,
                    hub.addBuddy("TestKnight", "manualbuddy", "Local", false));
            assertTrue(hub.removeBuddy("TestKnight", "MANUALBUDDY"));
            assertFalse(hub.removeBuddy("TestKnight", "MissingBuddy"));

            for (int index = 0; index < GameHub.MAX_BUDDIES - 1; index++) {
                assertEquals(GameHub.BuddyAddResult.ADDED,
                        hub.addBuddy("TestKnight", "Buddy" + index, "Local", false));
            }
            assertEquals(GameHub.BuddyAddResult.FULL,
                    hub.addBuddy("TestKnight", "OverflowBuddy", "Local", false));
        }
    }

    @Test
    void sameDisplayOwnerHasIndependentBuddyProfilePerOriginalSource() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String sun = GameStateStore.sourceCharacter("Sun", "SameHero");
            String moon = GameStateStore.sourceCharacter("Moon", "SameHero");
            assertEquals(GameHub.BuddyAddResult.ADDED,
                    hub.addBuddy(sun, "SunFriend", "Sun", false));
            assertEquals(GameHub.BuddyAddResult.ADDED,
                    hub.addBuddy(moon, "MoonFriend", "Moon", false));

            int sunGroup = hub.createBuddyGroup(sun, "SunRaid");
            int moonGroup = hub.createBuddyGroup(moon, "MoonRaid");
            assertEquals(2, sunGroup);
            assertEquals(2, moonGroup);
            assertTrue(hub.changeBuddyGroup(sun, "SunFriend", sunGroup));
            assertTrue(hub.changeBuddyGroup(moon, "MoonFriend", moonGroup));
            assertFalse(hub.changeBuddyGroup(sun, "MoonFriend", sunGroup));
            assertTrue(hub.setBuddyPresence(sun, "SunFriend", true));
            assertFalse(hub.setBuddyPresence(sun, "MoonFriend", true));

            assertTrue(hub.buddyData(sun, -1).buddies().stream()
                    .anyMatch(buddy -> buddy.name().equals("SunFriend")
                            && buddy.groupId() == sunGroup
                            && buddy.online()
                            && buddy.activity() == Protocol.LitoBuddyActivity.WAITING));
            assertFalse(hub.buddyData(sun, -1).buddies().stream()
                    .anyMatch(buddy -> buddy.name().equals("MoonFriend")));
            assertTrue(hub.buddyData(moon, -1).buddies().stream()
                    .anyMatch(buddy -> buddy.name().equals("MoonFriend")
                            && buddy.groupId() == moonGroup));
        }
    }

    @Test
    void persistenceHookReceivesOnlyExplicitDurableBuddies() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String owner = GameStateStore.sourceCharacter("380_ac_1", "TestKnight");
            hub.replacePersistentBuddyData(owner, new GameHub.BuddyPersistentData(
                    List.of(new GameHub.BuddyGroup(1, "Friends")), List.of()));
            AtomicReference<GameHub.BuddyPersistentData> written = new AtomicReference<>();
            hub.registerBuddyPersistence(owner, replacement -> {
                written.set(replacement);
                return true;
            });

            hub.buddyData(owner, -1);
            assertEquals(GameHub.BuddyAddResult.ADDED,
                    hub.addBuddy(owner, "SqlFriend", "Local", "380_ac_1", false));
            assertEquals(List.of("SqlFriend"), written.get().buddies().stream()
                    .map(GameHub.BuddyPersistentEntry::name).toList());
            assertFalse(written.get().buddies().stream()
                    .anyMatch(buddy -> buddy.name().equals("ArenaGuide")));
        }
    }

    @Test
    void rejectedPersistenceNeverCommitsStagedBuddyOrGroupMutation() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String owner = GameStateStore.sourceCharacter("380_ac_1", "ReadOnlyHero");
            hub.replacePersistentBuddyData(owner, new GameHub.BuddyPersistentData(
                    List.of(new GameHub.BuddyGroup(1, "Friends")),
                    List.of(new GameHub.BuddyPersistentEntry(
                            "Existing", 123, "380_ac_1", 1, 1, 1, "Local"))));
            hub.registerBuddyPersistence(owner, ignored -> false);

            assertEquals(GameHub.BuddyAddResult.PERSISTENCE_REJECTED,
                    hub.addBuddy(owner, "Rejected", "Local", "380_ac_1", false));
            assertEquals(-1, hub.createBuddyGroup(owner, "RejectedGroup"));
            assertFalse(hub.removeBuddy(owner, "Existing"));
            GameHub.BuddyData unchanged = hub.buddyData(owner, -1);
            assertTrue(unchanged.buddies().stream()
                    .anyMatch(buddy -> buddy.name().equals("Existing")));
            assertFalse(unchanged.buddies().stream()
                    .anyMatch(buddy -> buddy.name().equals("Rejected")));
            assertEquals(List.of(new GameHub.BuddyGroup(1, "Friends")),
                    unchanged.groups());
        }
    }
}
