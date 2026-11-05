package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleWorldProtocol.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernBattleWorldProtocolTest {
    @TempDir Path root;
    static Request read(ModernRooms.Scope scope,ModernTargetHistory.World world) {
        return new Request(scope,Action.READ,world==null?0:world.roomId(),0,world==null?0:world.matchId(),world==null?0:world.generation(),ModernWorldProtocol.ZERO_DIGEST);
    }
    static Request action(ModernRooms.Scope scope,Action action,Reply reply,String digest) {
        var w=reply.world();return new Request(scope,action,w.roomId(),0,w.matchId(),w.generation(),digest);
    }
    static byte[] encoded(Request q,Reply r)throws Exception {
        var m=new BattleWorldResult(9,q,r);ModernBattleWorldProtocol.validate(m);
        if(r.view()!=null)ModernNpcMotionWireTest.assertPayload43(ModernApplicationProtocol.encode(m,43),r.view());
        var bytes=ModernApplicationProtocol.encode(m,39);var b=ByteBuffer.wrap(bytes);
        assertEquals(RESULT_KIND,Short.toUnsignedInt(b.getShort(12)));assertEquals(bytes.length,b.getInt(4));
        assertEquals(bytes.length-MIN_RESULT_BYTES,b.getInt(REQUEST_BYTES+52));
        if(r.view()!=null)assertArrayEquals(ModernBattleWire.encode(r.view(),2),java.util.Arrays.copyOfRange(bytes,MIN_RESULT_BYTES,bytes.length));
        return bytes;
    }
    static final class Scene implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room=new ModernBattleEngineBotTest.RoomFixture(2,0);
        final ModernWorldService service;
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        Scene(boolean fail)throws Exception {
            service=new ModernWorldService(room.rooms,Map.of(room.binding.key(),new ModernWorldService.Mapping(room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000)),
                    new ModernWorldService.Limits(2,1,0,4000,10,new ModernBattleEngine.Limits(4000,0,30000)));
            for(var p:room.players)service.register(p.owner(),p.scope(),d->{
                if(p==room.players.get(0)){entered.countDown();if(!release.await(3,TimeUnit.SECONDS)||fail)throw new java.io.IOException("bounded fixture preparation");}return p.row();
            },null,false,null,false,null,false,false,false,false,false,false,new ModernSessionBoundary.Negotiated(1,39,1,ModernBattleReadProtocolTest.CAPS|ModernSessionProtocol.CAP_ACTION_LIFE));
        }
        Reply send(int index,Request q)throws Exception {var r=service.battleWorld(room.players.get(index).owner(),q);encoded(q,r);return r;}
        ModernRooms.Scope scope(int index){return room.players.get(index).scope();}
        Request prepare(){var s=room.view().summary();return new Request(scope(0),Action.PREPARE,s.id(),s.revision(),0,0,ModernWorldProtocol.ZERO_DIGEST);}
        public void close()throws Exception{release.countDown();service.close();assertTrue(service.awaitTermination(4000));assertTrue(service.isQuiescent());room.close();}
    }
    @Test void boundedLifecycleRequestsRoundTripAndRejectOldVersionNestedMoveAndServerReply()throws Exception {
        try(var f=new Scene(false)) {
            var q=new BattleWorldRequest(9,f.prepare());var bytes=ModernApplicationProtocol.encode(q,39);
            assertEquals(REQUEST_BYTES,bytes.length);assertEquals(q,ModernApplicationProtocol.decode(bytes,39));
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.encode(q,38));
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,38));
            for(int at:new int[]{14,41,47}){var bad=bytes.clone();bad[at]=1;assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bad,39));}
            var bad=bytes.clone();bad[40]=4;assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bad,39));
            for(int n=0;n<bytes.length;n++){var truncated=java.util.Arrays.copyOf(bytes,n);assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,39));}
            for(int kind:new int[]{REQUEST_KIND,RESULT_KIND}) {
                var header=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(REQUEST_BYTES+1).putInt(9).putShort((short)kind).putShort((short)0).array();
                var in=new java.io.InputStream(){int at;public int read(){if(at==16)fail("body read for invalid header");return header[at++]&255;}};
                assertThrows(ProtocolException.class,()->ModernApplicationProtocol.readFrame(in,39));
            }
        }
    }
    @Test void prepareLoadingBothAcksReadAndLeaveUseOnlyCanonicalLifecycle()throws Exception {
        try(var f=new Scene(false)) {
            assertEquals(State.DETACHED,f.send(0,read(f.scope(0),null)).state());
            var start=f.send(0,f.prepare());assertEquals(State.BEFORE_ENGINE,start.state());assertNull(start.view());assertEquals(1,start.lifecycle().revision());
            assertTrue(f.entered.await(2,TimeUnit.SECONDS));assertEquals(ModernWorldProtocol.Phase.PREPARING,start.lifecycle().phase());
            var early=f.send(0,action(f.scope(0),Action.LOADED,start,f.room.map.digest()));assertEquals(ModernWorldProtocol.Reason.NOT_ACTIVE,early.reason());
            assertEquals(State.BEFORE_ENGINE,f.send(1,read(f.scope(1),start.world())).state());
            f.release.countDown();ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.service.battleWorld(f.room.players.get(0).owner(),read(f.scope(0),start.world())).state()==State.DATA);
            var loading=f.send(0,read(f.scope(0),start.world()));assertEquals(ModernWorldProtocol.Phase.LOADING,loading.lifecycle().phase());
            var one=f.send(0,action(f.scope(0),Action.LOADED,loading,f.room.map.digest()));assertEquals(ModernWorldProtocol.Phase.LOADING,one.lifecycle().phase());
            var both=f.send(1,action(f.scope(1),Action.LOADED,loading,f.room.map.digest()));assertEquals(ModernWorldProtocol.Phase.ACTIVE,both.lifecycle().phase());
            assertTrue(both.view().world().actors().stream().allMatch(a->a.pose().loaded()));
            assertEquals(State.DETACHED,f.send(0,action(f.scope(0),Action.LEAVE,both,ModernWorldProtocol.ZERO_DIGEST)).state());
            var aborted=f.send(1,read(f.scope(1),start.world()));assertEquals(ModernWorldProtocol.Phase.ABORTED,aborted.lifecycle().phase());
            f.send(1,action(f.scope(1),Action.LEAVE,aborted,ModernWorldProtocol.ZERO_DIGEST));ModernWorldServiceTest.awaitNoMatches(f.service);
            assertEquals(State.DETACHED,f.send(0,read(f.scope(0),null)).state());assertNull(start.view());
        }
    }
    @Test void leaveWaitsForActualRetirementBeforeAssertingNoMatches() throws Exception {
        try (var f = new Scene(true)) {
            var start = f.send(0, f.prepare());
            assertTrue(f.entered.await(2, TimeUnit.SECONDS));
            // Pause the actual preparation worker immediately AFTER terminal publication.
            // Both completion latches are zero, but finishWorker has not run yet.
            var published = new CountDownLatch(1);
            var retire = new CountDownLatch(1);
            var gateField = ModernWorldService.class.getDeclaredField("gate");
            gateField.setAccessible(true);
            var entriesField = ModernWorldService.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            CountDownLatch releaseDone;
            var terminalDone = new CountDownLatch(1) {
                @Override public void countDown() {
                    super.countDown();
                    published.countDown();
                    boolean interrupted = false;
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    try {
                        while (retire.getCount() != 0) {
                            long left = deadline - System.nanoTime();
                            if (left <= 0) throw new AssertionError("retirement barrier timed out");
                            try { retire.await(left, TimeUnit.NANOSECONDS); }
                            catch (InterruptedException ignored) { interrupted = true; }
                        }
                    } finally { if (interrupted) Thread.currentThread().interrupt(); }
                }
            };
            synchronized (gateField.get(f.service)) {
                var entries = (java.util.Collection<?>) entriesField.get(f.service);
                assertEquals(1, entries.size());
                var entry = entries.iterator().next();
                var terminalField = entry.getClass().getDeclaredField("terminationDone");
                terminalField.setAccessible(true);
                terminalField.set(entry, terminalDone);
                var releaseField = entry.getClass().getDeclaredField("releaseDone");
                releaseField.setAccessible(true);
                releaseDone = (CountDownLatch) releaseField.get(entry);
            }
            var checked = new java.util.concurrent.FutureTask<Void>(() -> {
                ModernWorldServiceTest.awaitNoMatches(f.service);
                return null;
            });
            var checker = new Thread(checked, "leave-retirement-assertion");
            try {
                f.release.countDown();
                assertTrue(published.await(2, TimeUnit.SECONDS));
                var aborted = f.send(0, read(f.scope(0), start.world()));
                assertEquals(ModernWorldProtocol.Phase.ABORTED, aborted.lifecycle().phase());
                for (int i = 0; i < 2; i++)
                    assertEquals(State.DETACHED, f.send(i, action(f.scope(i), Action.LEAVE,
                            aborted, ModernWorldProtocol.ZERO_DIGEST)).state());
                assertEquals(0, releaseDone.getCount());
                assertEquals(0, terminalDone.getCount());
                assertEquals(1, f.service.active());
                assertEquals(1, f.service.matchCount());
                System.out.println("controlled retirement: releaseDone=0 terminationDone=0 active=1 matchCount=1");
                checker.start();
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> checked.get(100, TimeUnit.MILLISECONDS));
                retire.countDown();
                checked.get(3, TimeUnit.SECONDS);
                assertEquals(0, f.service.active());
                assertEquals(0, f.service.matchCount());
            } finally {
                retire.countDown();
                checker.join(4000);
                assertFalse(checker.isAlive());
            }
        }
    }
    @Test void failedPreparationIsAttachedAbortedWithoutInventedBattleData()throws Exception {
        try(var f=new Scene(true)) {
            var start=f.send(0,f.prepare());f.release.countDown();
            ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.service.battleWorld(f.room.players.get(0).owner(),read(f.scope(0),start.world())).lifecycle().phase()==ModernWorldProtocol.Phase.ABORTED);
            var failed=f.send(0,read(f.scope(0),start.world()));assertEquals(State.BEFORE_ENGINE,failed.state());assertNull(failed.view());assertEquals(ModernWorldProtocol.AbortReason.PREPARATION_FAILED,failed.lifecycle().abortReason());
            assertEquals(start.world(),failed.world());assertTrue(failed.lifecycle().revision()>start.lifecycle().revision());
            f.send(0,action(f.scope(0),Action.LEAVE,failed,ModernWorldProtocol.ZERO_DIGEST));f.send(1,action(f.scope(1),Action.LEAVE,failed,ModernWorldProtocol.ZERO_DIGEST));
        }
    }
    @Test void mixedEquipmentHistoryCanReadAcknowledgeLoadAndLeaveWithoutLegacyProjection()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,39)) {
            f.reprice();var q=read(f.player(0).scope(),f.read(0).view().world().identity());var r=f.service.battleWorld(f.player(0).owner(),q);encoded(q,r);
            assertEquals(1,r.view().personal().equipment().revision());assertThrows(IllegalArgumentException.class,f::legacyRead);
            var loaded=action(q.scope(),Action.LOADED,r,f.world.manifest().digest());encoded(loaded,f.service.battleWorld(f.player(0).owner(),loaded));
            var leave=action(q.scope(),Action.LEAVE,r,ModernWorldProtocol.ZERO_DIGEST);assertEquals(State.DETACHED,f.service.battleWorld(f.player(0).owner(),leave).state());
            var other=read(f.player(1).scope(),r.world());var aborted=f.service.battleWorld(f.player(1).owner(),other);encoded(other,aborted);assertEquals(ModernWorldProtocol.Phase.ABORTED,aborted.lifecycle().phase());
            f.service.battleWorld(f.player(1).owner(),action(other.scope(),Action.LEAVE,aborted,ModernWorldProtocol.ZERO_DIGEST));ModernWorldServiceTest.awaitNoMatches(f.service);
        }
    }
    @Test void passiveReadRetainsSavedStateAndExactResultAfterMixedHistory()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,39)) {
            f.reprice();f.win();ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
            var q=read(f.player(0).scope(),f.read(0).view().world().identity());var r=f.service.battleWorld(f.player(0).owner(),q);var bytes=encoded(q,r);
            assertEquals(ModernWorldProtocol.SettlementState.SAVED,r.lifecycle().settlement());assertEquals(2,bytes[REQUEST_BYTES+38]);assertEquals(f.event.get(),r.view().world().result());
            f.service.battleWorld(f.player(0).owner(),action(q.scope(),Action.LEAVE,r,ModernWorldProtocol.ZERO_DIGEST));
            var other=read(f.player(1).scope(),r.world());var retained=f.service.battleWorld(f.player(1).owner(),other);encoded(other,retained);assertEquals(r.view().world().result(),retained.view().world().result());
            f.service.battleWorld(f.player(1).owner(),action(other.scope(),Action.LEAVE,retained,ModernWorldProtocol.ZERO_DIGEST));assertEquals(1,f.persisted.get());
        }
    }
    @Test void exactRoomOwnerAndNegotiatedVersionRemainMandatory()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,39)) {
            var w=f.read(0).view().world().identity();var q=read(f.player(0).scope(),w);
            assertThrows(SecurityException.class,()->f.service.battleWorld(f.player(1).owner(),q));
            var wrong=read(q.scope(),new ModernTargetHistory.World(w.roomId()+1,w.matchId(),w.generation()));assertThrows(SecurityException.class,()->f.service.battleWorld(f.player(0).owner(),wrong));
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.read(0).view().world().phase());
        }
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(java.nio.file.Files.createDirectory(root.resolve("old")),null,38)) {
            assertThrows(SecurityException.class,()->f.service.battleWorld(f.player(0).owner(),read(f.player(0).scope(),null)));
        }
    }
}
