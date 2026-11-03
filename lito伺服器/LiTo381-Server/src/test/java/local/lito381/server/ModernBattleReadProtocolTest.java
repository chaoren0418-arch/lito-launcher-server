package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ModernBattleReadProtocolTest {
    static final int CAPS=ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_DISINTEGRATE
            |ModernSessionProtocol.CAP_NPC_DISINTEGRATE|ModernSessionProtocol.CAP_NPC_FIRE_STORM);
    static ModernBattleReadProtocol.Request request(ModernRooms.Scope scope,ModernWorldProtocol.Frame frame) {
        return new ModernBattleReadProtocol.Request(scope,new ModernTargetHistory.World(frame.room().summary().id(),frame.matchId(),frame.generation()));
    }
    static void enable(ModernNpcServiceTest.Fixture f) {
        for(var p:List.of(f.a,f.b))f.register(p,35,CAPS,null);
    }
    @Test void requestHasExactWidthsOpaqueEpochAndNoVersionFallback()throws Exception {
        var r=new ModernBattleReadProtocol.Request(new ModernRooms.Scope(7,3333,Long.MIN_VALUE,3),new ModernTargetHistory.World(101,102,103));
        var m=new ModernApplicationProtocol.BattleReadRequest(9,r);var b=ModernApplicationProtocol.encode(m,35);
        assertEquals(64,b.length);assertEquals(60,ByteBuffer.wrap(b).getShort(12));assertEquals(Long.MIN_VALUE,ByteBuffer.wrap(b).getLong(28));
        assertEquals(m,ModernApplicationProtocol.decode(b,35));
        for(int v=1;v<35;v++){int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,version));}
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(b,34));
        for(int n=0;n<b.length;n++){var truncated=java.util.Arrays.copyOf(b,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,35));}
        b[39]=4;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(b,35));
    }
    @Test void v35CapabilityIsExplicitAndOlderNegotiationDropsIt()throws Exception {
        var policy=ModernSessionBoundary.Policy.world(false).withBattleData();
        int caps=policy.supportedCapabilities();
        assertEquals(0,ModernSessionBoundary.Policy.world(false).supportedCapabilities()&ModernSessionProtocol.CAP_BATTLE_DATA);
        var hello=ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,35,caps,7,1));
        var current=new ModernSessionBoundary(policy);current.acceptHello(hello);assertTrue(ModernBattleReadProtocol.negotiated(current.negotiated().orElseThrow()));
        var old=new ModernSessionBoundary(policy.withMaxVersion(34));old.acceptHello(hello);
        assertEquals(0,old.negotiated().orElseThrow().capabilities()&ModernSessionProtocol.CAP_BATTLE_DATA);
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,34,1,caps)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,34,caps,7,1)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(ModernSessionProtocol.CAP_BATTLE_DATA,0));
    }
    @Test void inboundResultAndOversizeRequestsRejectBeforeBodyRead()throws Exception {
        for(int[] c:new int[][]{{61,ModernBattleReadProtocol.MAX_FRAME_BYTES},{60,65},{60,63},{60,64}}) {
            var b=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(c[1]).putInt(1).putShort((short)c[0]).putShort((short)0).array();
            var input=new InputStream(){int at;@Override public int read(){if(at>=b.length)throw new AssertionError("must not read attacker body");return Byte.toUnsignedInt(b[at++]);}};
            int version=c[1]==64?34:35;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(input,version));
        }
    }
    @Test void serviceReadSelectsActualSparseAudienceAndRejectsWrongOwnerEpochChannelOrWorld()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,true,true,new ModernQuestServiceTest.Journal(),513)) {
            enable(f);var legacy=f.active().combat().world();ModernBattlePublicationTest.stopScheduler(f.service);
            var a=f.service.readBattle(f.a.owner(),request(f.a.scope(),legacy));
            assertSame(ModernBattleAudienceTest.published(f.service,f.a.owner()).audience(f.a.scope()),a.view());
            var b=f.service.readBattle(f.b.owner(),request(f.b.scope(),legacy));
            assertSame(ModernBattleAudienceTest.published(f.service,f.b.owner()).audience(f.b.scope()),b.view());
            assertEquals(ModernBattleReadProtocol.State.DATA,a.state());assertEquals(ModernBattleReadProtocol.State.DATA,b.state());
            assertEquals(1,a.view().world().actors().size());assertEquals(2,b.view().personal().entityId());
            assertEquals(0,a.view().world().lane());assertEquals(1,b.view().world().lane());
            assertEquals(f.a.scope(),a.view().personal().scope());assertEquals(f.b.scope(),b.view().personal().scope());
            var cache=ModernBattlePublicationTest.cache(f.service,ModernBattlePublicationTest.entry(f.service,f.b.owner()));
            ModernBattlePublicationTest.consistent(cache);
            assertEquals(cache.revision(),b.view().world().revision(),"return the current engine revision without legacy offset");
            var r=request(f.a.scope(),legacy);
            assertThrows(SecurityException.class,()->f.service.readBattle(f.b.owner(),r));
            for(var scope:List.of(new ModernRooms.Scope(7,3333,2,0),new ModernRooms.Scope(7,3333,1,1),new ModernRooms.Scope(42,3333,1,0)))
                assertThrows(SecurityException.class,()->f.service.readBattle(f.a.owner(),new ModernBattleReadProtocol.Request(scope,r.world())));
            var w=r.world();
            for(var world:List.of(new ModernTargetHistory.World(w.roomId()+1,w.matchId(),w.generation()),new ModernTargetHistory.World(w.roomId(),w.matchId()+1,w.generation()),new ModernTargetHistory.World(w.roomId(),w.matchId(),w.generation()+1)))
                assertThrows(SecurityException.class,()->f.service.readBattle(f.a.owner(),new ModernBattleReadProtocol.Request(r.scope(),world)));
            var frame=ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadResult(5,r,a),35);
            var payload=ModernBattleWire.encode(a.view());assertEquals(72+payload.length,frame.length);
            assertArrayEquals(payload,java.util.Arrays.copyOfRange(frame,72,frame.length));
            assertEquals(1,frame[64]);assertEquals(payload.length,ByteBuffer.wrap(frame).getInt(68));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadResult(5,r,b),35));
            f.service.detach(f.a.owner());assertThrows(SecurityException.class,()->f.service.readBattle(f.a.owner(),r));
            assertEquals(ModernBattleEngine.Phase.ABORTED,f.service.readBattle(f.b.owner(),request(f.b.scope(),legacy)).view().world().phase());
        }
    }
    @Test void legacyRegistrationCannotUseCanonicalReadEvenWithValidMatchIdentity()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture()) {
            var w=f.active().combat().world();assertThrows(SecurityException.class,()->f.service.readBattle(f.a.owner(),request(f.a.scope(),w)));
        }
    }
    @Test void clockOnlyPublicationKeepsLegacyCachesAndCannotRollbackOrResurrect()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture()) {
            enable(f);f.prepare();ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.LOADING);
            ModernBattlePublicationTest.stopScheduler(f.service);
            var entry=ModernBattlePublicationTest.entry(f.service,f.a.owner());
            var engine=(ModernBattleEngine)ModernBattlePublicationTest.field(entry,"engine");
            var first=engine.serviceObservation();ModernBattlePublicationTest.publish(f.service,entry,engine,first);
            var before=ModernBattlePublicationTest.cache(f.service,entry);
            Thread.sleep(2);
            var later=engine.serviceObservation();assertEquals(first.revision(),later.revision());
            assertTrue(later.clock().loadingElapsedNanos()>first.clock().loadingElapsedNanos());
            ModernBattlePublicationTest.publish(f.service,entry,engine,later);
            var after=ModernBattlePublicationTest.cache(f.service,entry);ModernBattlePublicationTest.consistent(after);
            assertEquals(before.revision(),after.revision());assertSame(before.world(),after.world());
            assertSame(before.combat(),after.combat());assertSame(before.npc(),after.npc());
            assertNotSame(before.authority(),after.authority());
            assertEquals(later.clock(),after.authority().world().clock());
            assertEquals(first.clock(),before.authority().world().clock(),"retained publication stays immutable");
            ModernBattlePublicationTest.publish(f.service,entry,engine,first);
            ModernBattlePublicationTest.unchanged(after,ModernBattlePublicationTest.cache(f.service,entry));
            ModernBattlePublicationTest.publish(f.service,entry,engine,later);
            ModernBattlePublicationTest.unchanged(after,ModernBattlePublicationTest.cache(f.service,entry));
            var read=f.service.readBattle(f.a.owner(),request(f.a.scope(),after.world()));
            assertEquals(after.revision(),read.view().world().revision());
            assertTrue(read.view().world().clock().loadingElapsedNanos()>=later.clock().loadingElapsedNanos());
            f.service.detach(f.a.owner());var terminal=ModernBattlePublicationTest.cache(f.service,entry);
            ModernBattlePublicationTest.publish(f.service,entry,engine,later);
            ModernBattlePublicationTest.unchanged(terminal,ModernBattlePublicationTest.cache(f.service,entry));
        }
    }
    @Test void olderClientsDoNotAllocateNewCanonicalPublicationsForClockOnlyTicks()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture()) {
            f.prepare();ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.LOADING);
            ModernBattlePublicationTest.stopScheduler(f.service);
            var entry=ModernBattlePublicationTest.entry(f.service,f.a.owner());
            var engine=(ModernBattleEngine)ModernBattlePublicationTest.field(entry,"engine");
            var before=ModernBattlePublicationTest.cache(f.service,entry);
            Thread.sleep(2);
            var next=engine.serviceObservation();assertEquals(before.revision(),next.revision());
            assertTrue(next.clock().loadingElapsedNanos()>before.authority().world().clock().loadingElapsedNanos());
            ModernBattlePublicationTest.publish(f.service,entry,engine,next);
            ModernBattlePublicationTest.unchanged(before,ModernBattlePublicationTest.cache(f.service,entry));
        }
    }
    @Test void preparationAndFailureAreServiceStatesWithoutInventedEnginePayload()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var f=new ModernNpcServiceTest.Fixture()) {
            f.register(f.a,35,CAPS,deadline->{entered.countDown();assertTrue(release.await(1500,TimeUnit.MILLISECONDS));return f.fighter(f.a);});
            f.register(f.b,35,CAPS,null);var pending=f.prepare().frame();assertTrue(entered.await(1,TimeUnit.SECONDS));
            var r=request(f.a.scope(),pending);var read=f.service.readBattle(f.a.owner(),r);
            assertEquals(ModernBattleReadProtocol.State.PREPARING,read.state());assertNull(read.view());
            assertEquals(72,ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadResult(6,r,read),35).length);
            release.countDown();ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.LOADING);
            assertNotNull(f.service.readBattle(f.a.owner(),r).view());
        } finally {release.countDown();}
        try(var f=new ModernNpcServiceTest.Fixture()) {
            f.register(f.a,35,CAPS,deadline->{throw new java.io.IOException("owned preparation failure");});f.register(f.b,35,CAPS,null);
            var pending=f.prepare().frame();ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.ABORTED);
            var read=f.service.readBattle(f.a.owner(),request(f.a.scope(),pending));
            assertEquals(ModernBattleReadProtocol.State.UNAVAILABLE,read.state());assertNull(read.view());
        }
    }
}
