package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernRooms.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ModernWorldPreparationTest {
    static final class ReadyRoom {
        final ModernRoomsTest.Fixture fixture=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player owner=fixture.player(7,Long.MAX_VALUE,80,0);
        final ModernRoomsTest.Player member=fixture.player(42,Long.MAX_VALUE,80,0);
        ReadyRoom() {
            var created=fixture.create(owner);ModernRoomsTest.accepted(created);
            ModernRoomsTest.accepted(fixture.join(member,created.view().joined().summary(),""));ready();
        }
        void ready(){ModernRoomsTest.accepted(fixture.act(owner,Action.READY,1));ModernRoomsTest.accepted(fixture.act(member,Action.READY,1));}
        PreparationResult prepare(){var room=fixture.view(owner).joined().summary();return fixture.rooms.prepareWorld(owner.owner(),owner.scope(),room.id(),room.revision());}
    }

    @Test void preparationMintsOpaqueSortedRosterOnlyFromAnActuallyReadyRoom() {
        var ready=new ReadyRoom();var before=ready.fixture.view(ready.owner);var result=ready.prepare();
        assertEquals(Status.OK,result.result().status());var ticket=result.ticket();assertNotNull(ticket);
        assertTrue(ticket.isLive());assertEquals(1,ticket.matchId());
        assertEquals(before.joined().summary().revision()+1,ticket.generation());
        assertEquals(before.listRevision()+1,result.result().view().listRevision());
        assertEquals(result.result().view().joined(),ticket.snapshot());
        assertEquals(List.of(7,42),ticket.participants().stream().map(row->row.member().key().sourceId()).toList());
        for(var participant:ticket.participants()) {
            assertEquals(Long.MAX_VALUE,participant.scope().objectId());assertTrue(participant.member().ready());
            assertEquals(participant.scope().sourceId(),participant.member().key().sourceId());
        }
        assertThrows(UnsupportedOperationException.class,()->ticket.participants().clear());
        assertThrows(UnsupportedOperationException.class,()->ticket.snapshot().members().clear());
        try(var guard=ticket.guard()){assertTrue(ticket.isLive());}
        assertTrue(ready.fixture.rooms.releasePreparation(ticket));assertFalse(ticket.isLive());
        assertThrows(IllegalStateException.class,ticket::guard);
    }

    @Test void incompleteReadyOutsiderWrongOwnerWrongScopeAndStaleRevisionCannotCapture() {
        var ready=new ReadyRoom();var f=ready.fixture;var owner=ready.owner;var summary=f.view(owner).joined().summary();
        assertThrows(SecurityException.class,()->f.rooms.prepareWorld(f.presence.connect(),owner.scope(),summary.id(),summary.revision()));
        assertThrows(SecurityException.class,()->f.rooms.prepareWorld(owner.owner(),new Scope(7,Long.MAX_VALUE,2,0),summary.id(),summary.revision()));
        var denied=f.rooms.prepareWorld(ready.member.owner(),ready.member.scope(),summary.id(),summary.revision());
        assertEquals(Reason.NOT_OWNER,denied.result().reason());assertNull(denied.ticket());
        denied=f.rooms.prepareWorld(owner.owner(),owner.scope(),summary.id(),summary.revision()-1);
        assertEquals(Status.CONFLICT,denied.result().status());assertNull(denied.ticket());
        f.act(ready.member,Action.READY,0);var before=f.view(owner);denied=ready.prepare();
        assertEquals(Reason.INVALID_STATE,denied.result().reason());assertNull(denied.ticket());assertEquals(before,denied.result().view());
    }

    @Test void frozenRosterRejectsEveryMutationIncludingEquipmentButAllowsConfirmedRead() {
        var ready=new ReadyRoom();var f=ready.fixture;var captured=ready.prepare();var ticket=captured.ticket();
        var before=f.view(ready.owner);assertEquals(before,captured.result().view());
        for(var action:List.of(Action.LEAVE,Action.READY,Action.TEAM,Action.CONFIGURE)) {
            var denied=f.act(ready.owner,action,0,action==Action.CONFIGURE?ModernRoomsTest.draft(100,1,514,0):null);
            assertEquals(Reason.INVALID_STATE,denied.reason());assertEquals(before,denied.view());
        }
        var outside=f.player();var denied=f.join(outside,before.joined().summary(),"");
        assertEquals(Reason.INVALID_STATE,denied.reason());assertNull(denied.view().joined());
        assertFalse(f.rooms.unreadyForActivity(ready.owner.owner()));assertEquals(before,f.view(ready.owner));
        assertTrue(f.rooms.isPreparing(ready.member.owner()));
        assertEquals(Reason.INVALID_STATE,ready.prepare().result().reason());
        assertTrue(f.rooms.releasePreparation(ticket));assertEquals(0,f.view(ready.owner).joined().summary().ready());
        assertTrue(f.rooms.unreadyForActivity(ready.owner.owner()));
        ModernRoomsTest.accepted(f.act(ready.owner,Action.LEAVE,0));
    }

    @Test void foreignOrOldTicketsCannotCancelAReplacementPreparation() {
        var ready=new ReadyRoom();var first=ready.prepare().ticket();
        assertThrows(SecurityException.class,()->new ModernRooms().releasePreparation(first));assertTrue(first.isLive());
        assertTrue(ready.fixture.rooms.releasePreparation(first));assertFalse(ready.fixture.rooms.releasePreparation(first));
        ready.ready();var second=ready.prepare().ticket();assertEquals(2,second.matchId());assertTrue(second.generation()>first.generation());
        assertFalse(ready.fixture.rooms.releasePreparation(first));assertTrue(second.isLive());
        assertTrue(ready.fixture.rooms.releasePreparation(second));
    }

    @Test void disconnectInvalidatesWholeCaptureUnreadsSurvivorsAndTransfersRoomOwner() {
        var ready=new ReadyRoom();var f=ready.fixture;var ticket=ready.prepare().ticket();var before=f.view(ready.member);
        f.rooms.disconnect(ready.owner.owner());assertFalse(ticket.isLive());
        var after=f.view(ready.member);assertEquals(1,after.joined().summary().users());assertEquals(0,after.joined().summary().ready());
        assertEquals(new TournamentPresence.CharacterKey(42,Long.MAX_VALUE),after.joined().owner());
        assertEquals(before.listRevision()+1,after.listRevision());assertFalse(f.rooms.isPreparing(ready.member.owner()));
        assertFalse(f.rooms.releasePreparation(ticket));f.rooms.disconnect(ready.member.owner());assertEquals(0,f.rooms.roomCount());
    }

    @Test void cancellationWaitsForBoundedGuardWorkThenPreventsEveryLaterGuard() throws Exception {
        var ready=new ReadyRoom();var ticket=ready.prepare().ticket();var executor=Executors.newFixedThreadPool(2);
        var holding=new CountDownLatch(1);var release=new CountDownLatch(1);var cancelling=new CountDownLatch(1);
        try {
            var work=executor.submit(()->{try(var guard=ticket.guard()){holding.countDown();assertTrue(release.await(2,TimeUnit.SECONDS));return ticket.isLive();}});
            assertTrue(holding.await(2,TimeUnit.SECONDS));
            var cancellation=executor.submit(()->{cancelling.countDown();return ready.fixture.rooms.releasePreparation(ticket);});
            assertTrue(cancelling.await(2,TimeUnit.SECONDS));assertFalse(cancellation.isDone());
            release.countDown();assertTrue(work.get(2,TimeUnit.SECONDS));assertTrue(cancellation.get(2,TimeUnit.SECONDS));
            assertFalse(ticket.isLive());assertThrows(IllegalStateException.class,ticket::guard);
        } finally {release.countDown();executor.shutdownNow();assertTrue(executor.awaitTermination(2,TimeUnit.SECONDS));}
    }

    @Test void matchIdentityExhaustionRejectsBeforeAnyFreezeOrRevisionMutation() throws Exception {
        var ready=new ReadyRoom();var before=ready.fixture.view(ready.owner);
        var counter=ModernRooms.class.getDeclaredField("nextMatchId");counter.setAccessible(true);counter.setLong(ready.fixture.rooms,Long.MAX_VALUE);
        var result=ready.prepare();assertEquals(Status.UNAVAILABLE,result.result().status());assertNull(result.ticket());
        assertEquals(before,result.result().view());assertFalse(ready.fixture.rooms.isPreparing(ready.owner.owner()));
    }

    @Test void releaseAndDisconnectStillInvalidateCaptureWhenChannelRevisionCannotAdvance() throws Exception {
        for(boolean disconnect:List.of(false,true)) {
            var ready=new ReadyRoom();var ticket=ready.prepare().ticket();
            var field=ModernRooms.class.getDeclaredField("listRevisions");field.setAccessible(true);
            ((long[])field.get(ready.fixture.rooms))[0]=Long.MAX_VALUE;
            if(disconnect)ready.fixture.rooms.disconnect(ready.owner.owner());
            else assertTrue(ready.fixture.rooms.releasePreparation(ticket));
            assertFalse(ticket.isLive());assertThrows(IllegalStateException.class,ticket::guard);
            assertFalse(ready.fixture.rooms.releasePreparation(ticket));
            assertThrows(IllegalStateException.class,()->ready.fixture.view(ready.member));
            ready.fixture.rooms.disconnect(ready.owner.owner());ready.fixture.rooms.disconnect(ready.member.owner());
            assertEquals(0,ready.fixture.rooms.roomCount());assertEquals(0,ready.fixture.rooms.memberCount());
        }
    }

    @Test void engineClaimRequiresLiveGuardAndCannotBeReusedAfterGuardReturns() {
        var ready=new ReadyRoom();var ticket=ready.prepare().ticket();
        assertThrows(IllegalStateException.class,ticket::claimEngine);
        try(var guard=ticket.guard()){ticket.claimEngine();assertThrows(IllegalStateException.class,ticket::claimEngine);}
        try(var guard=ticket.guard()){assertThrows(IllegalStateException.class,ticket::claimEngine);}
        assertTrue(ready.fixture.rooms.releasePreparation(ticket));assertThrows(IllegalStateException.class,ticket::claimEngine);
    }
}
