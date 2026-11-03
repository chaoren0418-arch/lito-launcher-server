package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import static local.lito381.server.ModernBattleRevivalProtocol.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ModernBattleRevivalProtocolTest {
    static Request request(ModernBattleEngineNpcTest.Fixture f,int player) {
        var capture=f.engine.serviceObservation();var life=capture.modes().quest().lives().get(player);
        return new Request(new ModernBattleCommandLife.Proof(f.players.get(player).scope(),capture.identity(),
                life.entityId(),life.incarnation(),capture.revision()),life.deathRevision());
    }
    static BattleRevivalResult result(ModernBattleEngineNpcTest.Fixture f,Request q,ModernQuestLife.Result r) {
        var publication=new ModernBattlePublication(r.observation(),ModernQuestLifeProtocolTest.descriptor(),
                ModernNpcProtocol.Resources.from(f.config.catalog()),ModernNpcNames.Table.from(f.config.catalog()));
        return new BattleRevivalResult(9,q,new Reply(Reason.valueOf(r.reason().name()),publication.audience(q.proof().player()),ModernBattleLifecycle.from(publication)));
    }
    static byte[] encoded(BattleRevivalResult r)throws Exception {
        ModernNpcMotionWireTest.assertPayload43(ModernApplicationProtocol.encode(r,43),r.reply().view());
        var b=ModernApplicationProtocol.encode(r,40);var p=ByteBuffer.wrap(b);
        assertEquals(69,p.getShort(12));assertEquals(b.length,p.getInt(4));assertEquals(b.length-128,p.getInt(124));
        assertArrayEquals(ModernBattleWire.encode(r.reply().view(),2),Arrays.copyOfRange(b,128,b.length));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(b,40));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.encode(r,39));
        assertSame(r,ModernMatchDescriptor.forPeer(r,new ModernSessionBoundary.Negotiated(1,40,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(r,new ModernSessionBoundary.Negotiated(1,39,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        return b;
    }
    static void golden(String name,BattleRevivalResult r)throws Exception {
        var b=encoded(r);
        if(Boolean.getBoolean("modern.revival.goldens")) {
            var root=Path.of("target","modern-revival-test-fixtures");Files.createDirectories(root);
            Files.write(root.resolve(name+".request"),ModernApplicationProtocol.encode(new BattleRevivalRequest(9,r.echo()),40));
            Files.write(root.resolve(name+".reply"),b);
        }
    }
    @Test void exactWidthsAndOldVersionsRejectBeforeAnyUnboundedRead()throws Exception {
        var q=new BattleRevivalRequest(9,new Request(new ModernBattleCommandLife.Proof(new ModernRooms.Scope(7,3333,Long.MIN_VALUE,3),
                new ModernTargetHistory.World(11,12,13),1,2,7),6));
        var b=ModernApplicationProtocol.encode(q,40);assertEquals(96,b.length);assertEquals(q,ModernApplicationProtocol.decode(b,40));
        assertEquals(Long.MIN_VALUE,ByteBuffer.wrap(b).getLong(28));assertEquals(6,ByteBuffer.wrap(b).getLong(88));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.encode(q,39));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(b,39));
        for(int n=0;n<96;n++){var cut=Arrays.copyOf(b,n);assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(cut,40));}
        for(int at:new int[]{14,36,40,48,56,64,72,80,88}) {
            var bad=b.clone();bad[at]=(byte)255;assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bad,40));
        }
        for(int[] spec:new int[][]{{68,95},{68,97},{69,128},{68,Integer.MAX_VALUE}}) {
            var h=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(spec[1]).putInt(9).putShort((short)spec[0]).putShort((short)0).array();
            var in=new java.io.InputStream(){int at;public int read(){if(at==16)fail("invalid request read its body");return h[at++]&255;}};
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.readFrame(in,40));
        }
    }
    @Test void actualDeathRevivesOnceAndKeepsExactCanonicalLifeAndDeathLedger()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();f.move(0,1,10,10,0);ModernQuestLifeTest.die(f,100);var q=request(f,0);var p=q.proof();
            var before=f.engine.serviceObservation();
            if(Boolean.getBoolean("modern.revival.goldens")) {
                var publication=new ModernBattlePublication(before,ModernQuestLifeProtocolTest.descriptor(),
                        ModernNpcProtocol.Resources.from(f.config.catalog()),ModernNpcNames.Table.from(f.config.catalog()));
                var read=ModernBattleWorldProtocolTest.read(p.player(),p.world());
                var world=new BattleWorldResult(8,read,new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.OK,
                        ModernWorldProtocol.Reason.NONE,p.world(),ModernBattleLifecycle.from(publication),publication.audience(p.player())));
                var root=Path.of("target","modern-revival-test-fixtures");Files.createDirectories(root);
                Files.write(root.resolve("dead.request"),ModernApplicationProtocol.encode(new BattleWorldRequest(8,read),40));
                Files.write(root.resolve("dead.reply"),ModernApplicationProtocol.encode(world,40));
            }
            assertThrows(SecurityException.class,()->f.engine.confirmCanonicalQuestDeath(f.players.get(1).owner(),p,q.deathRevision()));
            assertEquals(before.world(),f.engine.serviceObservation().world());
            var revived=f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),p,q.deathRevision());
            assertEquals(ModernQuestLife.Reason.REVIVED,revived.reason());var reply=result(f,q,revived);golden("revived",reply);
            var after=revived.observation();assertEquals(before.revision()+1,after.revision());
            assertEquals(before.actors().get(0).pose().lastMoveSequence()+1,after.actors().get(0).pose().lastMoveSequence());
            assertEquals(2,after.modes().quest().lives().get(0).incarnation());
            var stale=f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),p,q.deathRevision());
            assertEquals(ModernQuestLife.Reason.STALE_LIFE,stale.reason());golden("stale",result(f,q,stale));
            assertEquals(after.world(),stale.observation().world());
            var alive=request(f,0);var denied=f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),alive.proof(),alive.deathRevision());
            assertEquals(ModernQuestLife.Reason.NOT_DEAD,denied.reason());golden("not-dead",result(f,alive,denied));
            assertThrows(IllegalArgumentException.class,()->encoded(new BattleRevivalResult(9,q,new Reply(Reason.REVIVED,reply.reply().view(),
                    new ModernBattleLifecycle(reply.reply().lifecycle().revision()-1,ModernWorldProtocol.Phase.ACTIVE,ModernWorldProtocol.AbortReason.NONE,null,null,ModernPetRaceProtocol.Snapshot.disabled(),false,false)))));
            f.rooms.releasePreparation(f.ticket);var retired=f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),p,q.deathRevision());
            assertEquals(ModernQuestLife.Reason.NOT_ACTIVE,retired.reason());golden("aborted",result(f,q,retired));
        }
    }
    @Test void wrongOwnerWorldEntityFutureRevisionAndFutureDeathCannotMutate()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();ModernQuestLifeTest.die(f,100);var q=request(f,0);var p=q.proof();var before=f.engine.serviceObservation();
            for(int changed=0;changed<4;changed++) {
                var world=p.world();var other=new ModernBattleCommandLife.Proof(p.player(),
                        new ModernTargetHistory.World(world.roomId()+(changed==0?1:0),world.matchId(),world.generation()+(changed==1?1:0)),
                        p.entity()+(changed==2?1:0),p.life(),p.revision()+(changed==3?1:0));
                assertThrows(SecurityException.class,()->f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),other,q.deathRevision()));
            }
            assertThrows(SecurityException.class,()->f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),p,p.revision()+1));
            assertEquals(before.world(),f.engine.serviceObservation().world());
        }
    }
    @Test void retainedRevivalReceiptMayBeAbortedButCannotClaimANewLifeTwice()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();ModernQuestLifeTest.die(f,100);var q=request(f,0);
            var reply=result(f,q,f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),q.proof(),q.deathRevision()));
            var l=reply.reply().lifecycle();var aborted=new ModernBattleLifecycle(l.revision()+1,ModernWorldProtocol.Phase.ABORTED,
                    ModernWorldProtocol.AbortReason.DISCONNECTED,null,null,ModernPetRaceProtocol.Snapshot.disabled(),false,false);
            golden("revived-retired",new BattleRevivalResult(9,q,new Reply(Reason.REVIVED,reply.reply().view(),aborted)));
            var wrong=new Request(new ModernBattleCommandLife.Proof(q.proof().player(),q.proof().world(),q.proof().entity(),q.proof().life()+1,q.proof().revision()),q.deathRevision());
            assertThrows(IllegalArgumentException.class,()->encoded(new BattleRevivalResult(9,wrong,reply.reply())));
        }
    }
    @Test void canonicalBlockedSpawnDenialIsAtomicAndFreshConfirmationWorksAfterAllyLeaves()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(3)) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,4).status());
            ModernQuestLifeTest.die(f,100);int x=30;long seq=0,elapsed=1000;
            f.elapsed(elapsed);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(2,++seq,x,10,0).status());
            while(x>10){f.elapsed(elapsed+=200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(2,++seq,x--,9,6).status());}
            f.elapsed(elapsed+=200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(2,++seq,10,9,4).status());
            var q=request(f,0);var before=f.engine.serviceObservation();
            var denied=f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),q.proof(),q.deathRevision());
            assertEquals(ModernQuestLife.Reason.SPAWN_BLOCKED,denied.reason());encoded(result(f,q,denied));
            assertEquals(before.world(),denied.observation().world());
            f.elapsed(elapsed+200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(2,++seq,10,10,0).status());
            var fresh=request(f,0);var revived=f.engine.confirmCanonicalQuestDeath(f.players.get(0).owner(),fresh.proof(),fresh.deathRevision());
            assertEquals(ModernQuestLife.Reason.REVIVED,revived.reason());encoded(result(f,fresh,revived));
            assertNull(revived.observation().result());
        }
    }
}
