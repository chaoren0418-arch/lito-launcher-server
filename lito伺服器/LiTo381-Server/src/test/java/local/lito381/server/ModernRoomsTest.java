package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ModernRoomsTest {
    record Player(TournamentPresence.Owner owner, ModernRooms.Scope scope) {}
    static final class Fixture {
        final TournamentPresence presence = new TournamentPresence();
        final ModernRooms rooms = new ModernRooms();
        long next = 1;
        Player player() { return player(7, next++, 80, 0); }
        Player player(int source, long object, long level, int channel) {
            var owner = presence.connect();
            var selection = new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source, object),
                    "source-" + source, "player-" + object, level, 6, 1);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, presence.select(owner, selection));
            rooms.enterLobby(owner, selection, 1, channel);
            return new Player(owner, new ModernRooms.Scope(source, object, 1, channel));
        }
        ModernRooms.View view(Player player) {
            return rooms.execute(player.owner(), new ModernRooms.Command(player.scope(), ModernRooms.Action.READ,
                    0, 0, 0, null, "")).view();
        }
        ModernRooms.Result create(Player player, ModernRooms.Draft draft) {
            return rooms.execute(player.owner(), new ModernRooms.Command(player.scope(), ModernRooms.Action.CREATE,
                    0, 0, 0, draft, ""));
        }
        ModernRooms.Result create(Player player) { return create(player, draft(100, 1, 564, 0)); }
        ModernRooms.Result join(Player player, ModernRooms.Summary summary, String password) {
            return rooms.execute(player.owner(), new ModernRooms.Command(player.scope(), ModernRooms.Action.JOIN,
                    summary.id(), summary.revision(), 0, null, password));
        }
        ModernRooms.Result act(Player player, ModernRooms.Action action, int value, ModernRooms.Draft draft) {
            var summary = view(player).joined().summary();
            return rooms.execute(player.owner(), new ModernRooms.Command(player.scope(), action,
                    summary.id(), summary.revision(), value, draft, ""));
        }
        ModernRooms.Result act(Player player, ModernRooms.Action action, int value) { return act(player, action, value, null); }
    }
    static ModernRooms.Draft draft(int capacity, int mode, int map, int attributes) {
        return new ModernRooms.Draft("room", "", capacity, mode, map, attributes);
    }
    static void accepted(ModernRooms.Result result) {
        assertEquals(ModernRooms.Status.OK, result.status()); assertEquals(ModernRooms.Reason.NONE, result.reason());
    }
    @Test void configuredMapCapacityRejectsCreateAndMapChangeWithoutPublishingAnything() {
        var key=new ModernWorldBinding.Key(2,554);var available=java.util.Set.of(key);
        for(int value:new int[]{0,1,101})assertThrows(IllegalArgumentException.class,
                ()->new ModernRooms(available,java.util.Set.of(),java.util.Map.of(key,value)));
        assertThrows(IllegalArgumentException.class,()->new ModernRooms(java.util.Set.of(),java.util.Set.of(),java.util.Map.of(key,48)));
        var rooms=new ModernRooms(available,java.util.Set.of(),java.util.Map.of(key,48));
        var presence=new TournamentPresence();var owner=presence.connect();var scope=new ModernRooms.Scope(7,3333,1,0);
        rooms.enterLobby(owner,new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,3333),"source","player",80,1,0),1,0);
        java.util.function.Supplier<ModernRooms.View> view=()->rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view();
        try {
            var before=view.get();
            assertEquals(48,before.options().stream().filter(o->o.mode()==2&&o.mapId()==554).findFirst().orElseThrow().maximumPlayers());
            assertTrue(before.options().stream().filter(o->o.mode()!=2||o.mapId()!=554).allMatch(o->o.maximumPlayers()==100));
            var denied=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,draft(49,2,554,0),""));
            assertEquals(ModernRooms.Reason.INVALID_CONFIGURATION,denied.reason());assertEquals(before,view.get());
            accepted(rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,draft(100,1,564,0),"")));
            before=view.get();var summary=before.joined().summary();
            denied=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CONFIGURE,summary.id(),summary.revision(),0,draft(49,2,554,0),""));
            assertEquals(ModernRooms.Reason.INVALID_CONFIGURATION,denied.reason());assertEquals(before,view.get());
            accepted(rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CONFIGURE,summary.id(),summary.revision(),0,draft(48,2,554,0),"")));
            assertEquals(48,view.get().joined().summary().config().capacity());
        } finally {rooms.disconnect(owner);presence.disconnect(owner);}
    }

    @Test void createAtomicallyPublishesTheCreatorAndNeverAnEmptyDefaultRoom() {
        var f = new Fixture(); var player = f.player(); var before = f.view(player);
        assertTrue(before.rooms().isEmpty()); assertNull(before.joined()); assertEquals(1, before.listRevision());
        var result = f.create(player); accepted(result);
        var joined = result.view().joined(); assertNotNull(joined); assertTrue(f.rooms.hasRoom(player.owner()));
        assertEquals(player.scope().objectId(), joined.owner().objectId());
        assertEquals(1, joined.members().size()); assertEquals(1, joined.summary().users());
        assertEquals(1, joined.summary().revision()); assertEquals(2, result.view().listRevision());
        assertEquals(ModernRooms.Reason.INVALID_STATE, f.create(player).reason());
        assertEquals(joined, f.view(player).joined());
    }

    @Test void concurrentCreatorsOwnDistinctRoomsAndEachIsAlreadyAMember() throws Exception {
        var f = new Fixture(); var one = f.player(); var two = f.player();
        var gate = new CountDownLatch(1); var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> { gate.await(); return f.create(one); });
            var second = workers.submit(() -> { gate.await(); return f.create(two); }); gate.countDown();
            var a = first.get(2, TimeUnit.SECONDS); var b = second.get(2, TimeUnit.SECONDS);
            accepted(a); accepted(b); assertNotEquals(a.view().joined().summary().id(), b.view().joined().summary().id());
            assertEquals(one.scope().objectId(), a.view().joined().owner().objectId());
            assertEquals(two.scope().objectId(), b.view().joined().owner().objectId());
            assertEquals(2, f.view(one).rooms().size());
        } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS)); }
    }

    @Test void fullScopeAndOpaqueRegistrationAreRequiredIncludingEpochAfterReconnect() {
        var f = new Fixture(); var player = f.player(); var scope = player.scope();
        for (var wrong : List.of(new ModernRooms.Scope(42, scope.objectId(), 1, 0),
                new ModernRooms.Scope(7, scope.objectId() + 100, 1, 0),
                new ModernRooms.Scope(7, scope.objectId(), 2, 0),
                new ModernRooms.Scope(7, scope.objectId(), 1, 1))) {
            assertThrows(SecurityException.class, () -> f.rooms.execute(player.owner(),
                    new ModernRooms.Command(wrong, ModernRooms.Action.READ, 0, 0, 0, null, "")));
        }
        assertThrows(SecurityException.class, () -> f.rooms.execute(f.presence.connect(),
                new ModernRooms.Command(scope, ModernRooms.Action.READ, 0, 0, 0, null, "")));
        f.rooms.leaveLobby(player.owner());
        f.rooms.enterLobby(player.owner(), f.presence.selected(player.owner()), -1, 0);
        assertThrows(SecurityException.class, () -> f.view(player));
        accepted(f.rooms.execute(player.owner(), new ModernRooms.Command(new ModernRooms.Scope(7, scope.objectId(), -1, 0),
                ModernRooms.Action.READ, 0, 0, 0, null, "")));
    }

    @Test void sameObjectIdInDifferentSourcesRemainsDistinctAndSortedWithoutNarrowing() {
        var f = new Fixture(); var moon = f.player(42, Long.MAX_VALUE, Long.MAX_VALUE, 0);
        var sun = f.player(7, Long.MAX_VALUE, Long.MAX_VALUE, 0);
        var initial = f.create(moon); accepted(f.join(sun, initial.view().joined().summary(), ""));
        var joined = f.view(moon).joined(); assertEquals(2, joined.members().size());
        assertEquals(7, joined.members().get(0).key().sourceId()); assertEquals(42, joined.members().get(1).key().sourceId());
        assertEquals(Long.MAX_VALUE, joined.members().get(0).key().objectId());
        assertEquals(Long.MAX_VALUE, joined.members().get(0).level());
        assertEquals(42, joined.owner().sourceId()); assertEquals("source-7", joined.members().get(0).sourceName());
        assertThrows(SecurityException.class, () -> f.rooms.enterLobby(f.presence.connect(),
                f.presence.selected(sun.owner()), 1, 0));
    }

    @Test void channelsIsolateListsAndUnknownRoomResponseDoesNotRevealAnotherChannel() {
        var f = new Fixture(); var first = f.player(); var other = f.player(42, 1, 50, 1);
        var created = f.create(first); var summary = created.view().joined().summary();
        assertTrue(f.view(other).rooms().isEmpty());
        var denied = f.join(other, summary, ""); assertEquals(ModernRooms.Reason.ROOM_NOT_FOUND, denied.reason());
        assertTrue(denied.view().rooms().isEmpty()); assertEquals(1, denied.view().listRevision());
        accepted(f.create(other)); assertEquals(1, f.view(first).rooms().size());
    }

    @Test void passwordNeverAppearsInPublicViewsAndIsNotTrimmedOrNormalized() {
        var f = new Fixture(); var creator = f.player(); var joiner = f.player();
        var draft = new ModernRooms.Draft("private", " secret ", 2, 1, 564, 16);
        var created = f.create(creator, draft); var summary = created.view().joined().summary();
        assertEquals(20, summary.config().attributes()); assertFalse(created.toString().contains("secret"));
        assertFalse(draft.toString().contains("secret"));
        var denied = f.join(joiner, summary, "secret"); assertEquals(ModernRooms.Reason.BAD_PASSWORD, denied.reason());
        assertEquals(created.view(), f.view(creator)); accepted(f.join(joiner, summary, " secret "));
    }

    @Test void creatorCannotBypassTierGateUsingAllowUpperButJoinerMayUseIt() {
        var f = new Fixture(); var low = f.player(7, 1, 20, 3); var high = f.player(42, 1, Long.MAX_VALUE, 3);
        assertEquals(ModernRooms.Reason.LEVEL_RESTRICTED, f.create(high, draft(2, 1, 564, 16)).reason());
        var initial = f.create(low, draft(2, 1, 564, 0));
        assertEquals(ModernRooms.Reason.LEVEL_RESTRICTED, f.join(high, initial.view().joined().summary(), "").reason());
        var updated = f.act(low, ModernRooms.Action.CONFIGURE, 0, draft(2, 1, 564, 16));
        accepted(f.join(high, updated.view().joined().summary(), ""));
        var tooLow = f.player(9, 1, 14, 0);
        assertEquals(ModernRooms.Reason.LEVEL_RESTRICTED, f.create(tooLow).reason());
    }

    @Test void capacityAndRevisionAreCheckedInsideTheJoinMutation() {
        var f = new Fixture(); var creator = f.player(); var first = f.player(); var second = f.player();
        var created = f.create(creator, draft(2, 1, 564, 0)); var old = created.view().joined().summary();
        accepted(f.join(first, old, ""));
        var stale = f.join(second, old, ""); assertEquals(ModernRooms.Status.CONFLICT, stale.status());
        assertEquals(ModernRooms.Reason.REVISION_MISMATCH, stale.reason());
        var full = f.join(second, f.view(creator).joined().summary(), "");
        assertEquals(ModernRooms.Status.FULL, full.status()); assertEquals(ModernRooms.Reason.CAPACITY, full.reason());
        assertNull(full.view().joined()); assertEquals(2, full.view().rooms().get(0).users());
    }

    @Test void everyMutationRequiresActualMembershipAndConfigurationRequiresOwnership() {
        var f = new Fixture(); var creator = f.player(); var outsider = f.player();
        var summary = f.create(creator).view().joined().summary();
        for (var action : List.of(ModernRooms.Action.LEAVE, ModernRooms.Action.TEAM, ModernRooms.Action.READY, ModernRooms.Action.CONFIGURE)) {
            var result = f.rooms.execute(outsider.owner(), new ModernRooms.Command(outsider.scope(), action,
                    summary.id(), summary.revision(), 0, action == ModernRooms.Action.CONFIGURE ? draft(2, 1, 564, 0) : null, ""));
            assertEquals(ModernRooms.Reason.NOT_MEMBER, result.reason()); assertNull(result.view().joined());
        }
        accepted(f.join(outsider, summary, ""));
        assertEquals(ModernRooms.Reason.NOT_OWNER, f.act(outsider, ModernRooms.Action.CONFIGURE, 0, draft(2, 1, 564, 0)).reason());
    }

    @Test void explicitReadinessIsIdempotentAndNeverStartsAnUnimplementedWorld() {
        var f = new Fixture(); var one = f.player(); var two = f.player();
        var first = f.create(one); accepted(f.join(two, first.view().joined().summary(), ""));
        accepted(f.act(one, ModernRooms.Action.READY, 1));
        var ready = f.act(two, ModernRooms.Action.READY, 1); assertTrue(ready.view().joined().summary().readySatisfied());
        var repeated = f.act(two, ModernRooms.Action.READY, 1); assertEquals(ready.view(), repeated.view());
        for (var option : repeated.view().options()) assertFalse(option.worldAvailable());
        assertFalse(f.act(one, ModernRooms.Action.READY, 0).view().joined().summary().readySatisfied());
    }

    @Test void changingTeamUnreadsOnlyTheActorAndEqualSidesIsActuallyEnforced() {
        var f = new Fixture(); var one = f.player(); var two = f.player(); var three = f.player();
        f.create(one, draft(10, 1, 564, 32)); f.join(two, f.view(one).joined().summary(), "");
        f.join(three, f.view(one).joined().summary(), "");
        for (var player : List.of(one, two, three)) f.act(player, ModernRooms.Action.READY, 1);
        assertFalse(f.view(one).joined().summary().readySatisfied());
        var changed = f.act(one, ModernRooms.Action.TEAM, 1); accepted(changed);
        assertEquals(2, changed.view().joined().summary().ready());
        assertFalse(changed.view().joined().members().stream().filter(member -> member.key().objectId() == one.scope().objectId()).findFirst().orElseThrow().ready());
        assertEquals(changed.view(), f.act(one, ModernRooms.Action.TEAM, 1).view());
    }

    @Test void nonTeamModesAllowReadinessButRejectTeamChange() {
        var f = new Fixture(); var one = f.player();
        f.create(one, draft(2, 4, 540, 0));
        assertEquals(ModernRooms.Reason.INVALID_CONFIGURATION, f.act(one, ModernRooms.Action.TEAM, 1).reason());
        assertTrue(f.act(one, ModernRooms.Action.READY, 1).view().joined().summary().readySatisfied());
    }

    @Test void configurationIsAtomicClearsReadyAndRebalancesOnModeChanges() {
        var f = new Fixture(); var one = f.player(); var two = f.player();
        f.create(one); f.join(two, f.view(one).joined().summary(), "");
        f.act(one, ModernRooms.Action.READY, 1); f.act(two, ModernRooms.Action.READY, 1);
        var changed = f.act(one, ModernRooms.Action.CONFIGURE, 0, draft(2, 0, 565, 0)); accepted(changed);
        assertEquals(0, changed.view().joined().summary().ready());
        assertTrue(changed.view().joined().members().stream().allMatch(member -> member.team() == 0));
        assertEquals(changed.view(), f.act(one, ModernRooms.Action.CONFIGURE, 0, draft(2, 0, 565, 0)).view());
        var siege = f.act(one, ModernRooms.Action.CONFIGURE, 0, draft(100, 3, 573, 0));
        assertEquals(List.of(0, 1), siege.view().joined().members().stream().map(ModernRooms.Member::team).toList());
        assertFalse(siege.view().joined().summary().readySatisfied());
    }

    @Test void shrinkingCapacityBelowExistingRosterLeavesAllStateUnchanged() {
        var f = new Fixture(); var one = f.player(); f.create(one);
        for (int index = 0; index < 10; index++) { var player = f.player(); f.join(player, f.view(one).joined().summary(), ""); }
        f.act(one, ModernRooms.Action.READY, 1); var before = f.view(one);
        var result = f.act(one, ModernRooms.Action.CONFIGURE, 0, draft(100, 0, 565, 0));
        assertEquals(ModernRooms.Status.FULL, result.status()); assertEquals(before, result.view());
    }

    @Test void leaveTransfersOwnershipInJoinOrderAndLastLeaveDeletesWithoutReusingIds() {
        var f = new Fixture(); var first = f.player(42, 99, 80, 0); var second = f.player(7, 77, 80, 0);
        var created = f.create(first); long oldId = created.view().joined().summary().id();
        f.join(second, created.view().joined().summary(), "");
        var left = f.act(first, ModernRooms.Action.LEAVE, 0); accepted(left); assertNull(left.view().joined());
        assertEquals(7, f.view(second).joined().owner().sourceId());
        assertThrows(IllegalStateException.class, () -> f.rooms.leaveLobby(second.owner()));
        f.act(second, ModernRooms.Action.LEAVE, 0); assertTrue(f.view(first).rooms().isEmpty());
        assertTrue(f.create(first).view().joined().summary().id() > oldId);
        f.rooms.disconnect(first.owner()); assertTrue(f.view(second).rooms().isEmpty());
    }

    @Test void disconnectCleansMembershipAndBindingAndOrdinaryLobbyLeaveIsIdempotent() {
        var f = new Fixture(); var one = f.player(); var two = f.player();
        f.create(one); f.join(two, f.view(one).joined().summary(), "");
        f.rooms.disconnect(one.owner()); f.rooms.disconnect(one.owner());
        assertFalse(f.rooms.hasRoom(one.owner())); assertEquals(two.scope().objectId(), f.view(two).joined().owner().objectId());
        assertThrows(SecurityException.class, () -> f.view(one));
        f.rooms.disconnect(two.owner()); assertFalse(f.rooms.hasRoom(two.owner()));
        assertEquals(0, f.rooms.ownerCount()); assertEquals(0, f.rooms.roomCount()); assertEquals(0, f.rooms.memberCount());
        assertEquals(0, f.rooms.chatOwnerCount());
        f.rooms.leaveLobby(two.owner()); f.rooms.leaveLobby(f.presence.connect());
        var fresh = f.player(); assertTrue(f.view(fresh).rooms().isEmpty());
        f.rooms.leaveLobby(fresh.owner()); assertThrows(SecurityException.class, () -> f.view(fresh));
        assertEquals(1, f.rooms.chatOwnerCount(), "lobby return retains the live session flood budget");
        f.rooms.disconnect(fresh.owner()); assertEquals(0, f.rooms.chatOwnerCount());
    }

    @Test void gearActivityExplicitlyClearsReadyEvenWhenTheGearChangeWillBeANoop() {
        var f = new Fixture(); var one = f.player(); f.create(one);
        f.act(one, ModernRooms.Action.READY, 1); var ready = f.view(one);
        f.rooms.unreadyForActivity(one.owner()); var unready = f.view(one);
        assertEquals(0, unready.joined().summary().ready());
        assertEquals(ready.joined().summary().revision() + 1, unready.joined().summary().revision());
        f.rooms.unreadyForActivity(one.owner()); assertEquals(unready, f.view(one));
        f.rooms.unreadyForActivity(f.presence.connect());
    }

    @Test void allFortyModeMapPairsCanCreateConfigureReadyAndLeaveWaitingRooms() {
        var f = new Fixture(); var one = f.player(); var options = f.view(one).options(); assertEquals(40, options.size());
        int lastMode = -1, lastMap = -1;
        for (var option : options) {
            assertTrue(option.mode() > lastMode || option.mode() == lastMode && option.mapId() > lastMap);
            lastMode = option.mode(); lastMap = option.mapId();
            var created = f.create(one, draft(100, option.mode(), option.mapId(), 0)); accepted(created);
            assertEquals(option.mapId(), created.view().joined().summary().config().mapId());
            var ready = f.act(one, ModernRooms.Action.READY, 1); accepted(ready);
            assertEquals(option.minimumPlayers() == 1, ready.view().joined().summary().readySatisfied());
            assertFalse(option.worldAvailable()); accepted(f.act(one, ModernRooms.Action.LEAVE, 0));
        }
        assertTrue(f.view(one).rooms().isEmpty());
    }

    @Test void siegeBalancesJoinsAndRequiresActualThirtyReadyPlayers() {
        var f = new Fixture(); var owner = f.player(); f.create(owner, draft(100, 3, 573, 0));
        var players = new ArrayList<Player>(); players.add(owner);
        for (int index = 1; index < 30; index++) { var player = f.player(); players.add(player); f.join(player, f.view(owner).joined().summary(), ""); }
        for (int index = 0; index < 29; index++) f.act(players.get(index), ModernRooms.Action.READY, 1);
        assertFalse(f.view(owner).joined().summary().readySatisfied());
        var complete = f.act(players.get(29), ModernRooms.Action.READY, 1);
        assertTrue(complete.view().joined().summary().readySatisfied());
        assertEquals(15, complete.view().joined().members().stream().filter(member -> member.team() == 0).count());
        assertFalse(f.act(owner, ModernRooms.Action.TEAM, 1).view().joined().summary().readySatisfied());
    }

    @Test void siegeTeamLimitFiftyCannotBeExceededByManualTeamSelection() {
        var f = new Fixture(); var owner = f.player(); f.create(owner, draft(100, 3, 573, 0));
        var players = new ArrayList<Player>(); players.add(owner);
        for (int index = 1; index < 52; index++) { var player = f.player(); players.add(player); f.join(player, f.view(owner).joined().summary(), ""); }
        for (var player : players) {
            long first = f.view(owner).joined().members().stream().filter(member -> member.team() == 0).count();
            if (first == 50) break;
            f.act(player, ModernRooms.Action.TEAM, 0);
        }
        var member = f.view(owner).joined().members().stream().filter(row -> row.team() == 1).findFirst().orElseThrow();
        var player = players.stream().filter(row -> row.scope().objectId() == member.key().objectId()).findFirst().orElseThrow();
        var before = f.view(owner); var rejected = f.act(player, ModernRooms.Action.TEAM, 0);
        assertEquals(ModernRooms.Status.FULL, rejected.status()); assertEquals(before, f.view(owner));
    }

    @Test void commandAndTextBoundariesRejectUnusedFieldsUnknownBitsAndMalformedUnicode() {
        var scope = new ModernRooms.Scope(7, Long.MAX_VALUE, -1, 0);
        for (var badName : List.of("", "\u00a0", "\u0000", "\ud800", "界".repeat(22)))
            assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Draft(badName, "", 2, 1, 564, 0));
        assertDoesNotThrow(() -> new ModernRooms.Draft("x".repeat(64), " ".repeat(64), 2, 1, 564, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Draft("room", "x".repeat(65), 2, 1, 564, 0));
        for (int attributes : new int[]{4, 8, 64, -1})
            assertThrows(IllegalArgumentException.class, () -> draft(2, 1, 564, attributes));
        assertThrows(IllegalArgumentException.class, () -> draft(2, 6, 500, 0));
        assertThrows(IllegalArgumentException.class, () -> draft(2, 1, 540, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Scope(7, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Command(scope, ModernRooms.Action.READ, 1, 0, 0, null, ""));
        assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Command(scope, ModernRooms.Action.CREATE, 0, 0, 0, null, ""));
        assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Command(scope, ModernRooms.Action.JOIN, 1, 0, 0, null, ""));
        assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Command(scope, ModernRooms.Action.TEAM, 1, 1, 2, null, ""));
        assertThrows(IllegalArgumentException.class, () -> new ModernRooms.Command(scope, ModernRooms.Action.READY, 1, 1, 1, null, "unused"));
    }

    @Test void trustedSelectionTextIsBoundedBeforeRegistrationAndSnapshotsAreImmutable() {
        var f = new Fixture(); var player = f.player(); var selected = f.presence.selected(player.owner());
        assertThrows(IllegalArgumentException.class, () -> f.rooms.enterLobby(f.presence.connect(),
                new TournamentPresence.Selection(new TournamentPresence.CharacterKey(9, 1), "source-9",
                        "x".repeat(257), 80, 1, 0), 1, 0));
        var created = f.create(player).view();
        assertThrows(UnsupportedOperationException.class, () -> created.rooms().clear());
        assertThrows(UnsupportedOperationException.class, () -> created.options().clear());
        assertThrows(UnsupportedOperationException.class, () -> created.joined().members().clear());
        assertThrows(IllegalStateException.class, () -> f.rooms.enterLobby(player.owner(), selected, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> f.rooms.enterLobby(f.presence.connect(),
                new TournamentPresence.Selection(new TournamentPresence.CharacterKey(9, 1), "source-9", "unsupported-class",
                        80, 7, 0), 1, 0));
    }

    @Test void revisionExhaustionRejectsMutationWithoutWrapAndDisconnectStillRemovesOccupants() throws Exception {
        var f = new Fixture(); var owner = f.player(); f.create(owner); var witness = f.player();
        Field revisions = ModernRooms.class.getDeclaredField("listRevisions"); revisions.setAccessible(true);
        long[] values = (long[]) revisions.get(f.rooms); values[0] = Long.MAX_VALUE;
        var before = f.view(owner); var denied = f.act(owner, ModernRooms.Action.READY, 1);
        assertEquals(ModernRooms.Status.UNAVAILABLE, denied.status()); assertEquals(ModernRooms.Reason.REVISION_EXHAUSTED, denied.reason());
        assertEquals(before, denied.view()); f.rooms.disconnect(owner.owner());
        assertFalse(f.rooms.hasRoom(owner.owner()));
        assertThrows(IllegalStateException.class, () -> f.view(witness));
        assertThrows(SecurityException.class, () -> f.view(owner));
        var other = f.player(42, 1, 50, 1); accepted(f.create(other));
    }

    @Test void roomIdExhaustionDoesNotCreateAReusedOrPartialRoom() throws Exception {
        var f = new Fixture(); var player = f.player();
        Field next = ModernRooms.class.getDeclaredField("nextRoomId"); next.setAccessible(true); next.setLong(f.rooms, Long.MAX_VALUE);
        var before = f.view(player); var result = f.create(player);
        assertEquals(ModernRooms.Status.UNAVAILABLE, result.status()); assertEquals(before, result.view());
        assertFalse(f.rooms.hasRoom(player.owner()));
    }

    @Test void directoryCapacityOneHundredRejectsAnotherCreateWithoutPartialMembership() {
        var f = new Fixture();
        for (int index = 0; index < 100; index++) accepted(f.create(f.player()));
        var waiting = f.player(); var before = f.view(waiting);
        var result = f.create(waiting); assertEquals(ModernRooms.Status.FULL, result.status());
        assertEquals(before, result.view()); assertFalse(f.rooms.hasRoom(waiting.owner()));
        assertEquals(100, f.rooms.roomCount()); assertEquals(100, f.rooms.memberCount());
    }
}
