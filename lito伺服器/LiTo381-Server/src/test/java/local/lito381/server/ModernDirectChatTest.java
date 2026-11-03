package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ModernDirectChatTest {
    static final class Fixture implements AutoCloseable {
        final ModernChatTest.Fixture base;
        Fixture(){base=new ModernChatTest.Fixture();}
        Fixture(ModernSpeechAuthority speech){base=new ModernChatTest.Fixture(speech);}
        ModernChatTest.Player player(int source, long object, int channel) {
            var p = base.player(source, object, channel);
            base.rooms.enableDirectChat(p.owner(), p.scope()); return p;
        }
        ModernDirectChat.Address address(ModernChatTest.Player p) {
            return new ModernDirectChat.Address(p.selection().key().sourceId(), p.selection().characterName());
        }
        ModernDirectChat.Result execute(ModernChatTest.Player p, ModernDirectChat.Request r) {
            return base.rooms.directChat(p.owner(), p.scope(), base.room(p), base.read(p).membership(), r);
        }
        ModernDirectChat.Snapshot read(ModernChatTest.Player p) {
            return execute(p, new ModernDirectChat.Request(ModernDirectChat.Action.READ, 0, 0, null, null, "")).snapshot();
        }
        ModernDirectChat.Result action(ModernChatTest.Player p, ModernDirectChat.Action action,
                ModernDirectChat.Address target, ModernDirectChat.Preferences prefs, String text) {
            var before = read(p);
            return execute(p, new ModernDirectChat.Request(action, before.requestSequence() + 1, before.cursor(), target, prefs, text));
        }
        ModernDirectChat.Result send(ModernChatTest.Player from, ModernChatTest.Player to, String text) {
            return action(from, ModernDirectChat.Action.SEND, address(to), null, text);
        }
        public void close() { base.close(); assertEquals(0, base.rooms.directChatOwnerCount()); }
    }

    @Test void qualifiedPrivateDeliveryCrossesChannelsAndRoomsButNeverReachesNamesakesOrPublicHistory() {
        try (var f = new Fixture()) {
            var sender = f.player(7, 1, 0); var sun = f.player(7, 3333, 0);
            var moon = f.player(42, 3333, 1); var stranger = f.player(42, 9, 0);
            f.base.create(sender, "private");
            var before = f.base.roomView(sender);
            assertEquals(ModernDirectChat.Status.OK, f.send(sender, moon, "跨來源私訊🙂").status());
            var outgoing = f.read(sender).messages().get(0); var incoming = f.read(moon).messages().get(0);
            assertEquals(sender.selection().key(), incoming.sender().key());
            assertEquals(moon.selection().key(), incoming.recipient().key());
            assertEquals("月亮神", incoming.recipient().sourceName());
            assertEquals(outgoing.senderSequence(), incoming.senderSequence());
            assertEquals("跨來源私訊🙂", incoming.text());
            assertTrue(f.read(sun).messages().isEmpty()); assertTrue(f.read(stranger).messages().isEmpty());
            for (var p : List.of(sender, sun, moon, stranger)) assertTrue(f.base.read(p).messages().isEmpty());
            assertEquals(before, f.base.roomView(sender));
            assertFalse(outgoing.toString().contains("跨來源")); assertFalse(f.read(moon).toString().contains("3333"));
            assertThrows(UnsupportedOperationException.class, () -> f.read(sender).messages().clear());
        }
    }
    @Test void legacyRecipientIsNotEnrolledAndRepeatedEnrollmentCannotEraseConfirmationOrPreferences() {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.base.player(42, 2, 0);
            assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(a, b, "尚未協商").status());
            assertEquals(1, f.read(a).requestSequence()); assertEquals(1, f.base.rooms.directChatOwnerCount());
            assertThrows(SecurityException.class, () -> f.read(b));
            f.base.rooms.enableDirectChat(b.owner(), b.scope());
            assertEquals(ModernDirectChat.Status.OK, f.send(a, b, "可收").status());
            f.action(b, ModernDirectChat.Action.PREFERENCES, null, new ModernDirectChat.Preferences(false, false, true), "");
            var before = f.read(b); f.base.rooms.enableDirectChat(b.owner(), b.scope()); assertEquals(before, f.read(b));
            assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(a, b, "關閉私訊").status());
            assertEquals(1, f.read(b).messages().size());
            f.action(b, ModernDirectChat.Action.PREFERENCES, null, ModernDirectChat.Preferences.DEFAULT, "");
            assertEquals(ModernDirectChat.Status.OK, f.send(a, b, ".de 20 只是私訊正文").status());
            assertEquals(0, f.base.rooms.roomCount()); assertTrue(f.base.read(a).messages().isEmpty());
        }
    }
    @Test void blockListIsSourceQualifiedBoundedAndCanBlockOfflineNamesWithoutQueryingPresence() {
        try (var f = new Fixture()) {
            var sun = f.player(7, 3333, 0); var moon = f.player(42, 3333, 0); var receiver = f.player(7, 9, 0);
            assertEquals(ModernDirectChat.Status.OK, f.action(receiver, ModernDirectChat.Action.BLOCK, f.address(sun), null, "").status());
            assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(sun, receiver, "被拒收").status());
            assertEquals(ModernDirectChat.Status.OK, f.send(moon, receiver, "不同來源仍可收").status());
            for (int i = 0; i < 15; i++) assertEquals(ModernDirectChat.Status.OK,
                    f.action(receiver, ModernDirectChat.Action.BLOCK, new ModernDirectChat.Address(42, "OFFLINE" + i), null, "").status());
            assertEquals(16, f.read(receiver).blocked().size());
            assertEquals(ModernDirectChat.Status.OK, f.action(receiver, ModernDirectChat.Action.BLOCK,
                    new ModernDirectChat.Address(42, " offline0 "), null, "").status());
            assertEquals(ModernDirectChat.Status.BLOCK_LIST_FULL, f.action(receiver, ModernDirectChat.Action.BLOCK,
                    new ModernDirectChat.Address(42, "overflow"), null, "").status());
            assertEquals(16, f.read(receiver).blocked().size());
            f.action(receiver, ModernDirectChat.Action.UNBLOCK, f.address(sun), null, "");
            assertEquals(ModernDirectChat.Status.OK, f.send(sun, receiver, "解除後可收").status());
            f.action(receiver, ModernDirectChat.Action.CLEAR_BLOCKS, null, null, "");
            assertTrue(f.read(receiver).blocked().isEmpty());
        }
    }
    @Test void privateAndPublicSendsShareRateBudgetAndMalformedOrReplayedCommandsDoNotAppend() {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0);
            for (int i = 0; i < 4; i++) assertEquals(ModernDirectChat.Status.OK, f.send(a, b, "burst" + i).status());
            assertEquals(ModernChat.Status.RATE_LIMITED, f.base.say(a, "一般聊天也限速").status());
            var before = f.read(a);
            assertThrows(SecurityException.class, () -> f.execute(a, new ModernDirectChat.Request(ModernDirectChat.Action.SEND,
                    before.requestSequence(), before.cursor(), f.address(b), null, "duplicate")));
            assertThrows(IllegalArgumentException.class, () -> f.execute(a, new ModernDirectChat.Request(ModernDirectChat.Action.SEND,
                    before.requestSequence() + 1, before.cursor() + 1, f.address(b), null, "future cursor")));
            assertEquals(before, f.read(a)); assertEquals(4, f.read(b).messages().size());
            f.base.now.addAndGet(ModernChat.REFILL_NANOS);
            assertEquals(ModernChat.Status.OK, f.base.say(a, "額度只有一份").status());
            assertEquals(ModernDirectChat.Status.RATE_LIMITED, f.send(a, b, "不能切私訊洗速率").status());
            assertEquals(5, f.read(a).requestSequence()); assertEquals(4, f.read(b).messages().size());
        }
    }
    @Test void inboxIsBoundedWithExplicitGapAndSelfWhisperCreatesOnlyOneRecord() {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0);
            for (int i = 0; i < 70; i++) { f.base.now.addAndGet(ModernChat.REFILL_NANOS); f.send(a, b, "訊息" + i); }
            var view = f.read(b); assertEquals(70, view.cursor()); assertEquals(64, view.messages().size());
            assertEquals(7, view.messages().get(0).id()); assertTrue(view.truncated());
            assertTrue(f.execute(b, new ModernDirectChat.Request(ModernDirectChat.Action.READ, 0, 70, null, null, "")).snapshot().messages().isEmpty());
            assertEquals(ModernDirectChat.Status.OK, f.send(b, b, "自己備忘").status());
            assertEquals(71, f.read(b).cursor()); assertEquals(70, f.read(a).cursor());
            assertEquals(1, f.read(b).messages().stream().filter(m -> m.text().equals("自己備忘")).count());
        }
    }
    @Test void roomReentryKeepsPrivateHistoryButRejectsOldGeneralMembershipAndForeignOwnerScope() {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0); f.base.create(a, ""); f.base.join(b, a, "");
            f.send(a, b, "私訊不等於房間公聊"); var before = f.read(b); var token = f.base.read(b); long room = f.base.room(b);
            f.base.leave(b); f.base.join(b, a, ""); assertEquals(before, f.read(b));
            var request = new ModernDirectChat.Request(ModernDirectChat.Action.SEND, 1, before.cursor(), f.address(a), null, "stale");
            assertThrows(SecurityException.class, () -> f.base.rooms.directChat(b.owner(), b.scope(), room, token.membership(), request));
            assertThrows(SecurityException.class, () -> f.base.rooms.directChat(a.owner(), b.scope(), room, f.base.read(a).membership(), request));
            assertEquals(before, f.read(b)); assertEquals(ModernDirectChat.Status.OK, f.send(b, a, "新資格可送").status());
        }
    }
    @Test void leavingSelectionDiscardsInboxAndDifferentSourceDoesNotInheritPreferencesOrBlockList() {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0); f.send(a, b, "離開前");
            f.action(b, ModernDirectChat.Action.BLOCK, f.address(a), null, "");
            f.action(b, ModernDirectChat.Action.PREFERENCES, null, new ModernDirectChat.Preferences(false, false, false), "");
            long seq = f.read(b).requestSequence();
            f.base.rooms.leaveLobby(b.owner()); assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(a, b, "離線").status());
            var next = new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7, 2), "太陽神", "角色2", 50, 6, 1);
            var scope = new ModernRooms.Scope(7, 2, 2, 1); f.base.rooms.enterLobby(b.owner(), next, 2, 1);
            f.base.rooms.enableDirectChat(b.owner(), scope);
            var token = f.base.rooms.chatRead(b.owner(), scope, 0, 0, 0);
            var view = f.base.rooms.directChat(b.owner(), scope, 0, token.membership(),
                    new ModernDirectChat.Request(ModernDirectChat.Action.READ, 0, 0, null, null, "")).snapshot();
            assertTrue(view.messages().isEmpty()); assertTrue(view.blocked().isEmpty());
            assertEquals(ModernDirectChat.Preferences.DEFAULT, view.preferences()); assertEquals(seq, view.requestSequence());
            assertEquals(next.key(), view.owner().key());
            assertThrows(SecurityException.class, () -> f.read(b));
            f.base.rooms.leaveLobby(b.owner()); f.base.rooms.disconnect(b.owner());
            assertEquals(1, f.base.rooms.directChatOwnerCount());
        }
    }
    @Test void concurrentBlockAndSendAreAtomicAndDoNotMutateRoomReadiness() throws Exception {
        for (int iteration = 0; iteration < 20; iteration++) try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0); f.base.create(a, ""); f.base.join(b, a, "");
            var before = f.base.roomView(a); var gate = new CountDownLatch(1); var workers = Executors.newFixedThreadPool(2);
            try {
                var send = workers.submit(() -> { gate.await(); return f.send(a, b, "race").status(); });
                var block = workers.submit(() -> { gate.await(); return f.action(b, ModernDirectChat.Action.BLOCK, f.address(a), null, "").status(); });
                gate.countDown(); var status = send.get(2, TimeUnit.SECONDS); assertEquals(ModernDirectChat.Status.OK, block.get(2, TimeUnit.SECONDS));
                assertTrue(status == ModernDirectChat.Status.OK || status == ModernDirectChat.Status.UNREACHABLE);
                assertEquals(status == ModernDirectChat.Status.OK ? 1 : 0, f.read(b).messages().size());
                assertEquals(f.read(a).messages().size(), f.read(b).messages().size());
                assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(a, b, "block committed").status());
                assertEquals(before, f.base.roomView(a));
            } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS)); }
        }
    }
    @Test void requestValidationAndSnapshotOwnershipNeverAcceptMalformedTextOrUnrelatedPrivateRecords() {
        var address = new ModernDirectChat.Address(42, " PLAYER "); assertEquals("player", address.name());
        for (String name : List.of("", "\u202Espoof", "a\nb", "\uD800", "中".repeat(86)))
            assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Address(7, name));
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Address(0, "name"));
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Request(ModernDirectChat.Action.READ, 1, 0, null, null, ""));
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Request(ModernDirectChat.Action.BLOCK, 1, 0, address, null, "hidden payload"));
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Request(ModernDirectChat.Action.SEND, 1, 0, address, null, "\uD800"));
        var a = new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(7, 1), "a", "Sun");
        var b = new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(42, 1), "b", "Moon");
        var other = new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(7, 2), "c", "Sun");
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Snapshot(other, 0, 1, true, false,
                ModernDirectChat.Preferences.DEFAULT, List.of(), List.of(new ModernDirectChat.Message(1, 1, a, b, "private"))));
        var spoof = new ModernDirectChat.Peer(a.key(), "wrong name", "wrong source label");
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Snapshot(a, 1, 1, true, false,
                ModernDirectChat.Preferences.DEFAULT, List.of(), List.of(new ModernDirectChat.Message(1, 1, spoof, b, "private"))));
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Snapshot(a, 0, 1, true, false,
                ModernDirectChat.Preferences.DEFAULT, List.of(), List.of(new ModernDirectChat.Message(1, 1, a, b, "future own confirmation"))));
        assertThrows(IllegalArgumentException.class, () -> new ModernDirectChat.Snapshot(b, 0, 2, true, false,
                ModernDirectChat.Preferences.DEFAULT, List.of(), List.of(new ModernDirectChat.Message(1, 1, a, b, "missing tail"))));
        assertFalse(address.toString().contains("player"));
    }
    @Test void channelReentryRetainsPreferencesAndBothChatBudgetsButNeverRetainsTheOldInbox() {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0);
            for (int i = 0; i < 4; i++) f.send(a, b, "before reentry");
            f.action(a, ModernDirectChat.Action.BLOCK, f.address(b), null, "");
            var prefs = new ModernDirectChat.Preferences(false, false, true);
            f.action(a, ModernDirectChat.Action.PREFERENCES, null, prefs, "");
            var prior = f.read(a); var oldMember = f.base.read(a).membership();
            f.base.rooms.leaveLobby(a.owner());
            f.base.rooms.enterLobby(a.owner(), a.selection(), a.scope().epoch(), 0);
            f.base.rooms.enableDirectChat(a.owner(), a.scope());
            var current = f.read(a);
            assertTrue(current.messages().isEmpty()); assertEquals(0, current.cursor());
            assertEquals(prior.requestSequence(), current.requestSequence());
            assertEquals(prior.blocked(), current.blocked()); assertEquals(prefs, current.preferences());
            assertNotEquals(oldMember, f.base.read(a).membership());
            assertEquals(ModernDirectChat.Status.RATE_LIMITED, f.send(a, b, "no budget reset").status());
            assertEquals(ModernChat.Status.RATE_LIMITED, f.base.say(a, "no public budget reset").status());
        }
    }
    @Test void directMessagesAndPreferencesDoNotUnreadyOrCancelTheFrozenBattleRoster() {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0); f.base.create(a, ""); f.base.join(b, a, "");
            for (var p : List.of(a, b)) {
                var s = f.base.roomView(p).joined().summary();
                assertEquals(ModernRooms.Status.OK, f.base.rooms.execute(p.owner(), new ModernRooms.Command(p.scope(),
                        ModernRooms.Action.READY, s.id(), s.revision(), 1, null, "")).status());
            }
            var s = f.base.roomView(a).joined().summary(); assertTrue(s.readySatisfied());
            var prepared = f.base.rooms.prepareWorld(a.owner(), a.scope(), s.id(), s.revision());
            assertEquals(ModernRooms.Status.OK, prepared.result().status()); var frozen = f.base.roomView(a);
            assertEquals(ModernDirectChat.Status.OK, f.send(a, b, "準備後仍可私訊").status());
            f.action(b, ModernDirectChat.Action.PREFERENCES, null, new ModernDirectChat.Preferences(true, false, false), "");
            assertEquals(frozen, f.base.roomView(a)); assertTrue(prepared.ticket().isLive());
        }
    }
    @Test void recipientDepartureRacingWithSendCannotRedirectToAnotherRoleOrLeaveOwnership() throws Exception {
        for (int i = 0; i < 20; i++) try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0); var namesake = f.player(7, 2, 0);
            var gate = new CountDownLatch(1); var workers = Executors.newFixedThreadPool(2);
            try {
                var send = workers.submit(() -> { gate.await(); return f.send(a, b, "departure race").status(); });
                var leave = workers.submit(() -> { gate.await(); f.base.rooms.leaveLobby(b.owner()); return true; });
                gate.countDown(); var status = send.get(2, TimeUnit.SECONDS); assertTrue(leave.get(2, TimeUnit.SECONDS));
                assertTrue(status == ModernDirectChat.Status.OK || status == ModernDirectChat.Status.UNREACHABLE);
                assertEquals(status == ModernDirectChat.Status.OK ? 1 : 0, f.read(a).messages().size());
                assertTrue(f.read(namesake).messages().isEmpty());
                assertThrows(SecurityException.class, () -> f.read(b));
                assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(a, b, "already departed").status());
                f.base.rooms.disconnect(b.owner()); assertEquals(2, f.base.rooms.directChatOwnerCount());
            } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS)); }
        }
    }
    @Test void conflictingTrustedNamesFailClosedInsteadOfSelectingHashMapOrder() {
        try (var f = new Fixture()) {
            var sender = f.player(42, 9, 0); var a = f.player(7, 1, 0); var b = f.player(7, 2, 0);
            // A malformed trusted adapter could supply two rows with one source/name;
            // the social authority must still not choose a recipient by map order.
            f.base.rooms.leaveLobby(b.owner());
            var conflicting = new TournamentPresence.Selection(b.selection().key(), "太陽神", a.selection().characterName(), 80, 6, 1);
            f.base.rooms.enterLobby(b.owner(), conflicting, b.scope().epoch(), 0);
            f.base.rooms.enableDirectChat(b.owner(), b.scope());
            assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(sender, a, "ambiguous").status());
            assertTrue(f.read(a).messages().isEmpty()); assertTrue(f.read(b).messages().isEmpty());
            assertTrue(f.read(sender).messages().isEmpty());
        }
    }
    private static Object ledger(Fixture f, ModernChatTest.Player p) throws Exception {
        var field = ModernRooms.class.getDeclaredField("directChat"); field.setAccessible(true); var service = field.get(f.base.rooms);
        var entries = ModernDirectChat.class.getDeclaredField("ledgers"); entries.setAccessible(true);
        return ((Map<?, ?>) entries.get(service)).get(p.owner());
    }
    private static void setLong(Object target, String fieldName, long value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName); field.setAccessible(true); field.setLong(target, value);
    }
    @Test void counterExhaustionDoesNotWrapPartiallyDeliverOrPreventNormalRoomExit() throws Exception {
        try (var f = new Fixture()) {
            var a = f.player(7, 1, 0); var b = f.player(42, 2, 0);
            setLong(ledger(f, b), "cursor", Long.MAX_VALUE);
            assertEquals(ModernDirectChat.Status.UNREACHABLE, f.send(a, b, "receiver full").status());
            assertTrue(f.read(a).messages().isEmpty()); assertTrue(f.read(b).messages().isEmpty());
            setLong(ledger(f, a), "sequence", Long.MAX_VALUE - 1);
            var finalCommand = f.action(a, ModernDirectChat.Action.PREFERENCES, null, ModernDirectChat.Preferences.DEFAULT, "");
            assertEquals(ModernDirectChat.Status.OK, finalCommand.status()); assertFalse(finalCommand.snapshot().available());
            assertThrows(SecurityException.class, () -> f.execute(a, new ModernDirectChat.Request(ModernDirectChat.Action.SEND, 1, 0, f.address(b), null, "wrap")));
            f.base.create(a, ""); f.base.leave(a); assertEquals(0, f.base.rooms.roomCount());
        }
    }
}
