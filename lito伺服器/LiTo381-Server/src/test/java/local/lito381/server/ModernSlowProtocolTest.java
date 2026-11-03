package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Actual server preparation and command capture, not a Rust/UI acceptance. */
class ModernSlowProtocolTest {
    static final int CAPS=ModernSlowServiceTest.CAPS;
    static ModernSlowServiceTest.Fixture active()throws Exception {
        var f=new ModernSlowServiceTest.Fixture();
        try{for(int i=0;i<2;i++)f.register(i,47,CAPS,f.reader(i));f.active();return f;}
        catch(Exception|AssertionError e){f.close();throw e;}
    }
    static ModernSlowProtocol.Request intent(ModernBattleAudience.View v,int target,long sequence) {
        var a=v.world().actors().stream().filter(p->p.pose().entityId()==v.personal().entityId()).findFirst().orElseThrow();
        var t=v.world().actors().get(target);
        return new ModernSlowProtocol.Request(1,new ModernBattleCommandLife.Proof(v.personal().scope(),v.world().identity(),
                a.pose().entityId(),a.resources().life()+1,v.world().revision()),sequence,a.resources().equipmentRevision(),
                a.pose().position().x(),a.pose().position().y(),a.buffs().revision(),
                new ModernSlowCast.Player(new ModernTargetHistory.Participant(t.pose().entityId(),t.pose().member().identity()),
                        t.resources().life()+1,t.resources().equipmentRevision(),t.pose().position(),t.buffs().revision()));
    }
    @Test void productionIntentConsumesOwnMpAndGemOnceAndPreservesOtherSource()throws Exception {
        try(var f=active()) {
            var before=f.read(0).view();var q=intent(before,1,1);
            var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.SlowRequest(q),47);
            var decoded=(ModernApplicationProtocol.SlowRequest)ModernApplicationProtocol.decode(bytes,47);
            var r=f.service.slow(f.player(0).owner(),decoded.request());
            assertEquals(ModernSlowCast.Reason.NONE,r.reason());
            assertEquals(280,r.view().world().actors().get(0).vital().values().mp());
            assertEquals(300,r.view().world().actors().get(1).vital().values().mp());
            assertEquals(3,r.view().personal().materials().stocks().get(0).remaining());
            assertEquals(1,r.view().personal().materials().uses());
            assertEquals(4,f.read(1).view().personal().materials().stocks().get(0).remaining());
            assertTrue(r.view().world().actors().get(1).buffs().slowUntilNanos()>0);
            var result=new ModernApplicationProtocol.SlowResult(r);
            assertSame(result,ModernMatchDescriptor.forPeer(result,new ModernSessionBoundary.Negotiated(1,47,1,CAPS)));
            var reply=ModernApplicationProtocol.encode(result,47);
            assertEquals(77,ByteBuffer.wrap(reply).getShort(12));assertEquals(6,ByteBuffer.wrap(reply).getShort(220));
            assertEquals(reply.length-216,ByteBuffer.wrap(reply).getInt(212));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(reply,47));
            assertThrows(SecurityException.class,()->f.service.slow(f.player(0).owner(),q));
            var denied=f.service.slow(f.player(0).owner(),intent(f.read(0).view(),1,2));
            assertEquals(ModernSlowCast.Reason.TOO_FAST,denied.reason());
            assertEquals(r.view().personal().history().resources(),denied.view().personal().history().resources());
            assertEquals(r.view().personal().materials(),denied.view().personal().materials());
            var unlearned=f.service.slow(f.player(1).owner(),intent(f.read(1).view(),0,1));
            assertEquals(ModernSlowCast.Reason.UNLEARNED,unlearned.reason());
            assertEquals(4,unlearned.view().personal().materials().stocks().get(0).remaining());
            assertEquals(2,f.reads.get(),"commands must not reacquire unrelated database authority");
            f.leave(0);
            var terminal=f.service.slow(f.player(1).owner(),intent(f.read(1).view(),1,2));
            assertEquals(ModernSlowCast.Reason.NOT_ACTIVE,terminal.reason());f.leave(1);assertEquals(0,f.service.matchCount());
        }
    }
    @Test void readonly46CannotCastAndFullOwnerRoomRevisionAndTargetIdentityAreRequired()throws Exception {
        try(var f=new ModernSlowServiceTest.Fixture()) {
            f.active();var q=intent(f.read(0).view(),0,1);
            assertThrows(SecurityException.class,()->f.service.slow(f.player(0).owner(),q));
            assertEquals(0,f.read(0).view().personal().materials().uses());
        }
        try(var f=active()) {
            var v=f.read(0).view();var q=intent(v,0,1);var p=q.proof();
            assertThrows(SecurityException.class,()->f.service.slow(f.player(1).owner(),q));
            for(var proof:List.of(new ModernBattleCommandLife.Proof(p.player(),p.world(),2,p.life(),p.revision()),
                    new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life(),Long.MAX_VALUE),
                    new ModernBattleCommandLife.Proof(p.player(),new ModernTargetHistory.World(p.world().roomId()+1,p.world().matchId(),p.world().generation()),p.entity(),p.life(),p.revision()))) {
                var bad=new ModernSlowProtocol.Request(1,proof,1,q.equipmentRevision(),q.x(),q.y(),q.buffRevision(),q.target());
                assertThrows(SecurityException.class,()->f.service.slow(f.player(0).owner(),bad));
            }
            var stale=new ModernSlowProtocol.Request(1,p,1,q.equipmentRevision()+1,q.x(),q.y(),q.buffRevision(),q.target());
            var denied=f.service.slow(f.player(0).owner(),stale);
            assertEquals(ModernSlowCast.Reason.STALE_STATE,denied.reason());
            assertEquals(v.personal().materials(),denied.view().personal().materials());
            assertEquals(ModernSlowCast.Reason.NONE,f.service.slow(f.player(0).owner(),intent(f.read(0).view(),0,2)).reason());
        }
    }
    @Test void replyCannotBeBorrowedByAnotherOwnerTargetSequenceOrMaterialHistory()throws Exception {
        try(var f=active()) {
            var v=f.read(0).view();var q=intent(v,1,1);
            var r=f.service.slow(f.player(0).owner(),q);
            for(var bad:List.of(intent(v,0,1),intent(v,1,2),intent(f.read(1).view(),1,1)))
                assertThrows(IllegalArgumentException.class,()->new ModernSlowProtocol.Reply(bad,r.reason(),r.view(),r.lifecycle()));
            assertThrows(IllegalArgumentException.class,()->new ModernSlowProtocol.Reply(q,ModernSlowCast.Reason.NO_MATERIAL,r.view(),r.lifecycle()));
            for(var peer:List.of(new ModernSessionBoundary.Negotiated(1,46,1,CAPS),new ModernSessionBoundary.Negotiated(1,47,1,CAPS&~ModernSessionProtocol.CAP_SLOW_DATA)))
                assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(new ModernApplicationProtocol.SlowResult(r),peer));
        }
    }
    @Test void requestsAreBoundedAndOldVersionsCannotTreatReadPermissionAsCastPermission()throws Exception {
        try(var f=active()) {
            var q=intent(f.read(0).view(),0,1);var t=(ModernSlowCast.Player)q.target();var w=q.proof().world();
            var bot=new ModernSlowCast.Player(new ModernTargetHistory.Participant(3,ModernParticipantId.bot(42,Long.MAX_VALUE)),1,0,t.cell(),0);
            var npc=new ModernSlowCast.Npc(new ModernNpcRoster.Id(new ModernNpcRoster.Scope(w.roomId(),w.matchId(),w.generation()),Long.MAX_VALUE),1,1,t.cell());
            for(var target:List.of(t,bot,npc)) {
                var n=new ModernSlowProtocol.Request(-1,q.proof(),1,q.equipmentRevision(),q.x(),q.y(),q.buffRevision(),target);
                var bytes=ModernSlowProtocol.encodeRequest(n,47);assertEquals(184,bytes.length);
                assertEquals(n,ModernSlowProtocol.decodeRequest(bytes,47));
                assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),47));
                for(int version=1;version<47;version++){final int old=version;
                    assertThrows(IllegalArgumentException.class,()->ModernSlowProtocol.encodeRequest(n,old));
                    assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,old));
                }
                for(int length=0;length<184;length++){var truncated=Arrays.copyOf(bytes,length);assertThrows(IllegalArgumentException.class,()->ModernSlowProtocol.decodeRequest(truncated,47));}
                for(int offset:new int[]{0,4,13,15,120,121,127}){var bad=bytes.clone();bad[offset]^=127;assertThrows(IllegalArgumentException.class,()->ModernSlowProtocol.decodeRequest(bad,47));}
                for(int length:new int[]{16,183,185,Integer.MAX_VALUE}){var header=Arrays.copyOf(bytes,16);ByteBuffer.wrap(header).putInt(4,length);
                    assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),47));}
            }
        }
    }
    @Test void npcCommandCapturesPrivateMaterialsAndActualNpcSpeedInOneReply()throws Exception {
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,11,10);
            var c=f.engine.serviceObservation();var a=c.actors().get(0);
            var q=new ModernSlowProtocol.Request(1,new ModernBattleCommandLife.Proof(a.scope(),c.identity(),a.pose().entityId(),a.resources().life()+1,c.revision()),
                    1,a.resources().equipmentRevision(),a.pose().position().x(),a.pose().position().y(),a.buffs().revision(),
                    new ModernSlowCast.Npc(n.id(),n.life(),n.changedRevision(),n.cell()));
            var outcome=f.engine.canonicalSlow(f.players.get(0).owner(),q);
            var p=ModernSpeedEffectsTest.publish(outcome.observation(),f.config.catalog());
            var reply=new ModernSlowProtocol.Reply(q,outcome.reason(),p.audience(a.scope()),ModernBattleLifecycle.from(p));
            assertEquals(ModernSlowCast.Reason.NONE,reply.reason());assertEquals(1,reply.view().personal().materials().uses());
            assertNotNull(reply.view().world().speedEffects().npcs());assertEquals(6,ByteBuffer.wrap(ModernSlowProtocol.encodeReply(reply,47)).getShort(220));
        }
    }
    @Test void commandPolicyDropsOptionalSlowFrom46ButKeepsReadOnly46Policy()throws Exception {
        var hello=ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,46,CAPS,7,1));
        var commands=new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true,true,true,true,true).withBattleActions().withSlowCommands());
        commands.acceptHello(hello);assertEquals(0,commands.negotiated().orElseThrow().capabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
        var data=new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true,true,true,true,true).withBattleActions().withSlowData());
        data.acceptHello(hello);assertTrue(ModernBattleSpeedWire.negotiated(data.negotiated().orElseThrow()));
        assertFalse(ModernSlowProtocol.negotiated(data.negotiated().orElseThrow()));
        var allRequired=new ModernSessionProtocol.Hello(1,1,46,CAPS,CAPS,1);
        var required=new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true,true,true,true,true).withBattleActions().withSlowCommands().withMaxVersion(46));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->required.acceptHello(ModernSessionProtocol.encode(allRequired)));
    }
}
