package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TournamentPresenceTest {
    @Test void sourceObjectIdentityIsLosslessAndNamesCannotBypassAReservation() {
        var core = new TournamentPresence(); var sun = core.connect(); var moon = core.connect(); var rival = core.connect();
        long objectId = Long.MAX_VALUE;
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(sun, selection(7, objectId, "Sun", "one", Long.MAX_VALUE)));
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(moon, selection(42, objectId, "Moon", "one", 80)));
        assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(rival, selection(7, objectId, "Sun", "renamed", 80)));
        assertEquals(Long.MAX_VALUE, core.selected(sun).key().objectId());
        assertEquals(0, TournamentPresence.recommendedChannel(Long.MAX_VALUE).id());
        core.disconnect(sun);
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(rival, selection(7, objectId, "Sun", "renamed", 80)));
        assertNotNull(core.selected(moon));
    }

    @Test void typedSelectionsAndLegacyDeletionUseTheSameAliasReservation() {
        var core = new TournamentPresence(); var nativeOwner = core.connect(); var modern = core.connect();
        var selected = selection(7, 3333, "Sun", "Knight", 80);
        assertTrue(core.reserveLegacyAlias(nativeOwner, selected.legacyAlias()));
        assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(modern, selected));
        assertNull(core.selected(nativeOwner), "legacy alias never fabricates an object ID");
        core.clearSelection(nativeOwner);
        assertTrue(core.beginDeletion(selected.legacyAlias(), nativeOwner));
        assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(modern, selected));
        core.endDeletion(selected.legacyAlias());
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(modern, selected));
        assertFalse(core.beginDeletion(selected.legacyAlias(), nativeOwner));
        core.disconnect(modern);
        assertTrue(core.beginDeletion(selected.legacyAlias(), nativeOwner));
    }

    @Test void selectionTicketsRejectBothCompletedDeletionAndQueriesStartedDuringDeletion() {
        var core = new TournamentPresence(); var nativeOwner = core.connect(); var modern = core.connect();
        var selected = selection(7, 3333, "Sun", "Knight", 80);
        var before = core.selectionTicket(modern);
        assertTrue(core.beginDeletion(selected.legacyAlias(), nativeOwner));
        var during = core.selectionTicket(modern);
        core.endDeletion(selected.legacyAlias());
        assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(modern, selected, before));
        assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(modern, selected, during));
        assertNull(core.selected(modern)); assertEquals(0, core.userCount());
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED,
                core.select(modern, selected, core.selectionTicket(modern)),
                "a genuinely fresh post-deletion query may qualify an existing/recreated row");
        core.disconnect(modern); core.disconnect(nativeOwner);
        assertEquals(0, core.ownerCount()); assertEquals(0, core.userCount());
    }

    @Test void selectionTicketsAreOwnerAndCoreBoundAndCannotResurrectDisconnectedOwners() {
        var core = new TournamentPresence(); var first = core.connect(); var other = core.connect();
        var selected = selection(7, 3333, "Sun", "Knight", 80);
        var ticket = core.selectionTicket(first);
        assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(other, selected, ticket));
        var separate = new TournamentPresence(); var separateOwner = separate.connect();
        assertEquals(TournamentPresence.SelectStatus.BUSY, separate.select(separateOwner, selected, ticket));
        core.disconnect(first);
        assertNull(core.selectionTicket(first));
        assertEquals(TournamentPresence.SelectStatus.CLOSED, core.select(first, selected, ticket));
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED,
                core.select(other, selected, core.selectionTicket(other)));
        core.disconnect(other); separate.disconnect(separateOwner);
        assertEquals(0, core.ownerCount()); assertEquals(0, separate.ownerCount());
    }

    @Test void rejectedOrUnmatchedDeletionDoesNotInvalidateUnrelatedFreshSelectionTickets() {
        var core = new TournamentPresence(); var nativeOwner = core.connect(); var modern = core.connect();
        var locked = selection(7, 3333, "Sun", "Knight", 80);
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(nativeOwner, locked));
        var ticket = core.selectionTicket(modern);
        assertFalse(core.beginDeletion(locked.legacyAlias(), modern));
        core.endDeletion("not an active deletion");
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED,
                core.select(modern, selection(42, 3333, "Moon", "Mage", 80), ticket));
        core.disconnect(nativeOwner); core.disconnect(modern);
        assertEquals(0, core.ownerCount());
    }

    @Test void retentionRequiresTheOpaqueOwnerAndExactAlreadySelectedKey() {
        var core = new TournamentPresence(); var owner = core.connect(); var rival = core.connect();
        var selected = selection(7, Long.MAX_VALUE, "Sun", "Knight", 80);
        assertNull(core.retainSelection(owner, selected.key()));
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(owner, selected));
        assertNull(core.retainSelection(rival, selected.key()));
        assertNull(core.retainSelection(owner, new TournamentPresence.CharacterKey(42, Long.MAX_VALUE)));
        assertNull(core.retainSelection(owner, new TournamentPresence.CharacterKey(7, 3333)));
        assertNull(core.retainSelection(owner, null));
        var unrelated = new TournamentPresence();
        assertNull(unrelated.retainSelection(owner, selected.key()));
        var retained = core.retainSelection(owner, selected.key()); assertNotNull(retained);
        assertEquals(selected.key(), retained.key());
        assertTrue(retained.matches(owner, new TournamentPresence.CharacterKey(7, Long.MAX_VALUE)));
        assertFalse(retained.matches(rival, selected.key())); assertFalse(retained.matches(owner, null));
        assertNull(core.retainSelection(owner, selected.key()), "one pin, not an unbounded reference counter");
        assertEquals("TournamentSelectionRetention[redacted]", retained.toString());
        retained.close(); assertFalse(retained.matches(owner, selected.key()));
        assertEquals(selected, core.selected(owner), "closing the pin alone does not request selection removal");
        core.disconnect(owner); core.disconnect(rival); assertEquals(0, core.ownerCount());
    }

    @Test void pinnedSelectionsCannotBeReplacedClearedReturnedOrDeletedBeforeRelease() {
        for (boolean returning : new boolean[] {false, true}) {
            var core = new TournamentPresence(); var owner = core.connect(); var rival = core.connect();
            var selected = selection(7, 3333, "Sun", "Knight", 80);
            var replacement = selection(7, 4444, "Sun", "Second", 80);
            var ticket = core.selectionTicket(owner);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(owner, selected));
            var retained = core.retainSelection(owner, selected.key()); assertNotNull(retained);
            assertNull(core.selectionTicket(owner));
            assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(owner, selected));
            assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(owner, replacement, ticket));
            assertFalse(core.reserveLegacyAlias(owner, replacement.legacyAlias()));
            assertFalse(core.beginDeletion(selected.legacyAlias(), owner));
            assertFalse(core.beginDeletion(selected.legacyAlias(), rival));
            assertNotNull(core.enterChannel(owner, 0));
            if (returning) core.returnToCharacters(owner); else core.clearSelection(owner);
            assertEquals(selected, core.selected(owner));
            assertFalse(retained.matches(owner, selected.key()));
            assertNull(core.enterChannel(owner, 1), "a pending release cannot re-enter a channel");
            assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(rival, selected));
            assertEquals(returning ? 0 : 1, core.userCount());
            retained.close(); retained.close();
            assertNull(core.selected(owner)); assertEquals(2, core.ownerCount());
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(rival, selected));
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(owner, replacement));
            var newer = core.retainSelection(owner, replacement.key()); assertNotNull(newer);
            retained.close();
            assertTrue(newer.matches(owner, replacement.key())); assertEquals(replacement, core.selected(owner));
            core.disconnect(owner); core.disconnect(rival); newer.close();
            assertEquals(0, core.ownerCount()); assertEquals(0, core.userCount());
        }
    }

    @Test void disconnectImmediatelyFreesChannelButRetainsBothRoleAndAliasUntilExactPinCloses() {
        var core = new TournamentPresence(); var owner = core.connect(); var rival = core.connect();
        var moon = core.connect(); var selected = selection(7, Long.MAX_VALUE, "Sun", "Knight", 80);
        var oldTicket = core.selectionTicket(owner);
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(owner, selected));
        var retained = core.retainSelection(owner, selected.key()); assertNotNull(retained);
        assertNotNull(core.enterChannel(owner, 0));
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED,
                core.select(moon, selection(42, Long.MAX_VALUE, "Moon", "Knight", 80)));
        core.disconnect(owner); core.disconnect(owner);
        assertFalse(retained.matches(owner, selected.key()), "retained ownership is not live selection authority");
        assertEquals(0, core.userCount()); assertEquals(0, core.snapshot(0).users());
        assertEquals(3, core.ownerCount(), "the admitted worker still has a real authority owner");
        assertNull(core.enterChannel(owner, 0)); assertNull(core.selectionTicket(owner));
        assertNull(core.retainSelection(owner, selected.key()));
        assertEquals(TournamentPresence.SelectStatus.CLOSED, core.select(owner, selected, oldTicket));
        assertFalse(core.reserveLegacyAlias(owner, "unrelated alias"));
        core.clearSelection(owner); core.returnToCharacters(owner);
        assertEquals(selected, core.selected(owner));
        assertEquals(TournamentPresence.SelectStatus.BUSY,
                core.select(rival, selection(7, Long.MAX_VALUE, "Sun", "Renamed", 80)));
        assertEquals(TournamentPresence.SelectStatus.BUSY,
                core.select(rival, selection(7, 7777, "Sun", "Knight", 80)));
        assertFalse(core.beginDeletion(selected.legacyAlias(), owner));
        assertFalse(core.beginDeletion(selected.legacyAlias(), rival));
        retained.close(); retained.close();
        assertEquals(2, core.ownerCount()); assertNull(core.selected(owner));
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(rival, selected));
        assertNotNull(core.selected(moon));
        core.disconnect(rival); core.disconnect(moon); assertEquals(0, core.ownerCount());
    }

    @Test void deletionAndRetentionAdmissionAreSerializedWithoutChangingNativeSameOwnerDeletion() {
        var core = new TournamentPresence(); var owner = core.connect(); var other = core.connect();
        var selected = selection(7, 3333, "Sun", "Knight", 80);
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(owner, selected));
        assertTrue(core.beginDeletion(selected.legacyAlias(), owner), "unretained native rule is unchanged");
        assertNull(core.retainSelection(owner, selected.key()), "no pin may begin inside active deletion");
        core.endDeletion(selected.legacyAlias());
        var retained = core.retainSelection(owner, selected.key()); assertNotNull(retained);
        var freshTicket = core.selectionTicket(other);
        assertFalse(core.beginDeletion(selected.legacyAlias(), owner));
        core.endDeletion("not an active deletion");
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED,
                core.select(other, selection(42, 3333, "Moon", "Mage", 80), freshTicket));
        retained.close();
        assertTrue(core.beginDeletion(selected.legacyAlias(), owner));
        core.endDeletion(selected.legacyAlias());
        var next = core.retainSelection(owner, selected.key()); assertNotNull(next);
        core.returnToCharacters(owner); retained.close();
        assertEquals(selected, core.selected(owner), "old close cannot release a same-key replacement pin");
        next.close(); assertNull(core.selected(owner));
        core.disconnect(owner); core.disconnect(other); assertEquals(0, core.ownerCount());
    }

    @Test void cancelledCallerCannotReleasePinnedRoleUntilActualBackendWorkFinally() throws Exception {
        var core = new TournamentPresence(); var owner = core.connect(); var rival = core.connect();
        var selected = selection(7, 3333, "Sun", "Knight", 80);
        assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(owner, selected));
        var retained = core.retainSelection(owner, selected.key()); assertNotNull(retained);
        var lease = new ModernEquipmentLease.Issuer().open(owner, selected.key(), 9, retained::close);
        assertNotNull(core.enterChannel(owner, 0));
        var executor = new ModernBackendExecutor(1, 0); var gate = executor.openSession();
        var callers = Executors.newFixedThreadPool(2);
        var committed = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var holder = new AtomicReference<ModernEquipmentLease.Operation>();
        try {
            var call = callers.submit(() -> assertThrows(ModernBackendExecutor.BackendException.class,
                    () -> gate.call(5_000, new ModernBackendExecutor.Work<Integer>() {
                        @Override public Integer run(ModernBackendScope scope) throws Exception {
                            var operation = lease.operation(scope); holder.set(operation);
                            assertTrue(operation.tryCommit()); committed.countDown();
                            if (!finish.await(4, TimeUnit.SECONDS)) throw new AssertionError("retention test barrier timeout");
                            return 1;
                        }
                        @Override public void close() {
                            var operation = holder.get(); if (operation != null) operation.close();
                        }
                    })));
            assertTrue(committed.await(2, TimeUnit.SECONDS));
            callers.submit(() -> { core.disconnect(owner); lease.invalidate(); gate.close(); }).get(1, TimeUnit.SECONDS);
            assertEquals(ModernBackendExecutor.Failure.CANCELLED, call.get(1, TimeUnit.SECONDS).failure());
            assertTrue(gate.isBusy()); assertEquals(0, core.userCount()); assertEquals(2, core.ownerCount());
            assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(rival, selected));
            assertFalse(core.beginDeletion(selected.legacyAlias(), owner));
            assertFalse(core.beginDeletion(selected.legacyAlias(), rival));
            finish.countDown(); executor.close(); assertTrue(executor.awaitTermination(3_000));
            assertTrue(lease.isRetired()); assertEquals(1, core.ownerCount());
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(rival, selected));
        } finally {
            finish.countDown(); core.disconnect(owner); lease.invalidate(); gate.close(); executor.close();
            callers.shutdown(); assertTrue(executor.awaitTermination(3_000));
            assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS));
            core.disconnect(rival); assertEquals(0, core.ownerCount());
        }
    }

    @Test void capacityAndAtomicMovesPreserveThePreviousChannelWhenTargetIsFull() {
        var core = new TournamentPresence(); var owners = new ArrayList<TournamentPresence.Owner>();
        for (int i = 0; i < 100; i++) {
            var owner = core.connect(); owners.add(owner); assertNotNull(core.enterChannel(owner, 0));
        }
        var moving = core.connect(); assertNotNull(core.enterChannel(moving, 1));
        assertNull(core.enterChannel(moving, 0)); assertEquals(1, core.channelOf(moving));
        assertEquals(100, core.snapshot(0).users()); assertEquals(1, core.snapshot(1).users());
        assertNotNull(core.enterChannel(owners.get(0), 0)); assertEquals(101, core.userCount());
        core.disconnect(owners.get(0)); assertNotNull(core.enterChannel(moving, 0));
        assertEquals(0, core.snapshot(1).users()); assertEquals(100, core.userCount());
        core.returnToCharacters(moving); core.returnToCharacters(moving);
        assertEquals(99, core.userCount()); assertNull(core.channelOf(moving));
    }

    @Test void finalSlotIsSerializedAcrossIndependentAdapters() throws Exception {
        var core = new TournamentPresence();
        for (int i = 0; i < 99; i++) core.enterChannel(core.connect(), 0);
        var first = core.connect(); var second = core.connect(); var start = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var left = workers.submit(() -> { start.await(); return core.enterChannel(first, 0); });
            var right = workers.submit(() -> { start.await(); return core.enterChannel(second, 0); });
            start.countDown();
            int admitted = (left.get(2, TimeUnit.SECONDS) == null ? 0 : 1) + (right.get(2, TimeUnit.SECONDS) == null ? 0 : 1);
            assertEquals(1, admitted); assertEquals(100, core.snapshot(0).users());
        } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS)); }
    }

    @Test void nativeGameHubDerivesCapacityAndReservationsFromTheInjectedCore() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        var core = new TournamentPresence();
        try (var hub = new GameHub(config, null, null, core)) {
            var nativeSession = new ClientSession(null, ClientSession.Endpoint.GAME, config, null, null, LocalResources.discardCapture(), hub);
            hub.connect(nativeSession);
            var row = character("Owned", 1234567890123L);
            int sourceId = config.classicServerIds().get(0); String source = config.classicServerNames().get(0);
            assertTrue(hub.selectCharacter(nativeSession, sourceId, source, row));
            var modern = core.connect();
            assertEquals(TournamentPresence.SelectStatus.BUSY, core.select(modern, selection(sourceId, row.objectId(), source, row.name(), row.level())));
            for (int i = 0; i < 100; i++) assertNotNull(core.enterChannel(core.connect(), 0));
            assertNull(hub.enterChannel(0, nativeSession));
            assertEquals(100, hub.channelSnapshot(0).users());
            assertTrue(hub.channelMembers(0).isEmpty(), "modern owners are not fake native packet recipients");
            assertNotNull(hub.enterChannel(1, nativeSession));
            assertEquals(1, core.snapshot(1).users()); assertEquals(101, hub.tournamentUserCount());
            hub.disconnect(nativeSession);
            assertEquals(0, core.snapshot(1).users());
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, core.select(modern, selection(sourceId, row.objectId(), source, row.name(), row.level())));
        }
    }

    @Test void sourceMappingsRejectAmbiguousRenumberingAndClosedOwnerCannotReturn() {
        var core = new TournamentPresence(); core.registerSource(7, "Sun");
        assertThrows(IllegalArgumentException.class, () -> core.registerSource(7, "Moon"));
        assertThrows(IllegalArgumentException.class, () -> core.registerSource(42, "Sun"));
        var owner = core.connect(); core.disconnect(owner);
        assertNull(core.enterChannel(owner, 0));
        assertEquals(TournamentPresence.SelectStatus.CLOSED, core.select(owner, selection(7, 1, "Sun", "one", 80)));
        assertEquals(0, core.ownerCount());
        assertEquals(List.of(0, 1, 2, 3), core.snapshots().stream().map(TournamentPresence.ChannelSnapshot::id).toList());
    }

    static TournamentPresence.Selection selection(int source, long object, String sourceName, String name, long level) {
        return new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source, object), sourceName, name, level, 1, 0);
    }
    static CharacterSummary character(String name, long objectId) {
        return new CharacterSummary(name, "", 1, 0, 80, 500, 300, -20, 18, 18, 18, 12, 18, 18, 18, 18,
                objectId, 0L, 0L, 0, 9999L, 32768L, 32768L, 4L);
    }
}
