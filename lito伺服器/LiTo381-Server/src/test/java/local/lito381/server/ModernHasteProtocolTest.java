package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ModernHasteProtocolTest {
    static ModernHasteProtocol.Request intent(ModernBattleObservation.Capture c,int index,long sequence,ModernHasteCast.Target target) {
        var a=c.actors().get(index);
        return new ModernHasteProtocol.Request(-1,new ModernBattleCommandLife.Proof(a.scope(),c.identity(),a.pose().entityId(),a.resources().life()+1,c.revision()),
                sequence,a.resources().equipmentRevision(),a.pose().position().x(),a.pose().position().y(),a.buffs().revision(),target);
    }
    static ModernHasteProtocol.Reply reply(ModernHasteProtocol.Request q,ModernBattleEngine.HasteResult outcome,ModernBattlePublication publication) {
        return new ModernHasteProtocol.Reply(q,outcome.reason(),publication.audience(q.proof().player()),ModernBattleLifecycle.from(publication));
    }
    @Test void playerBotAndWideNpcSerialHaveExactBoundedFraming()throws Exception {
        try(var f=ModernBattleHasteTest.fixture()) {
            var c=ModernBattleHasteTest.active(f);var p=ModernBattleHasteTest.target(c,1);
            var bot=new ModernHasteCast.Player(new ModernTargetHistory.Participant(3,ModernParticipantId.bot(42,Long.MAX_VALUE)),1,0,p.cell(),0);
            var npc=new ModernHasteCast.Npc(new ModernNpcRoster.Id(new ModernNpcRoster.Scope(c.identity().roomId(),c.identity().matchId(),c.identity().generation()),Long.MAX_VALUE),1,1,p.cell());
            for(var target:java.util.List.of(p,bot,npc)) {
                var q=intent(c,0,1,target);var bytes=ModernHasteProtocol.encodeRequest(q,44);
                assertEquals(184,bytes.length);assertEquals(74,ByteBuffer.wrap(bytes).getShort(12));
                assertEquals(q,ModernHasteProtocol.decodeRequest(bytes,44));
                assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),44));
                assertEquals(q,((ModernApplicationProtocol.HasteRequest)ModernApplicationProtocol.decode(bytes,44)).request());
                assertArrayEquals(bytes,ModernHasteProtocol.encodeRequest(q,45));assertEquals(q,ModernHasteProtocol.decodeRequest(bytes,45));
                for(int version:new int[]{1,42,43,ModernSessionProtocol.MAX_VERSION+1})assertThrows(IllegalArgumentException.class,()->ModernHasteProtocol.decodeRequest(bytes,version));
                for(int n=0;n<bytes.length;n++){var truncated=Arrays.copyOf(bytes,n);assertThrows(IllegalArgumentException.class,()->ModernHasteProtocol.decodeRequest(truncated,44));}
                assertThrows(IllegalArgumentException.class,()->ModernHasteProtocol.decodeRequest(Arrays.copyOf(bytes,185),44));
                for(int offset:new int[]{0,4,13,15,120,121,127}){var bad=bytes.clone();bad[offset]^=127;assertThrows(IllegalArgumentException.class,()->ModernHasteProtocol.decodeRequest(bad,44));}
                for(int length:new int[]{16,120,183,185,Integer.MAX_VALUE}) {
                    var header=Arrays.copyOf(bytes,16);ByteBuffer.wrap(header).putInt(4,length);
                    assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),44));
                }
            }
        }
    }
    @Test void targetedSuccessCannotBeBorrowedByAnotherTargetOwnerSequenceOrLife()throws Exception {
        try(var f=ModernBattleHasteTest.fixture()) {
            var c=ModernBattleHasteTest.active(f);var q=intent(c,0,1,ModernBattleHasteTest.target(c,1));
            var result=f.engine.canonicalHaste(f.players.get(0).owner(),q);var success=reply(q,result,ModernHasteEffectsTest.publish(result.observation()));
            assertEquals(ModernHasteCast.Reason.NONE,success.reason());
            var bytes=ModernHasteProtocol.encodeReply(success,44);assertEquals(75,ByteBuffer.wrap(bytes).getShort(12));
            assertEquals(bytes.length-216,ByteBuffer.wrap(bytes).getInt(212));assertEquals(5,ByteBuffer.wrap(bytes).getShort(220));
            assertThrows(IllegalArgumentException.class,()->ModernHasteProtocol.encodeReply(success,43));
            var self=intent(c,0,1,ModernBattleHasteTest.target(c,0));
            var next=intent(c,0,2,q.target());var other=intent(c,1,1,q.target());
            var target=(ModernHasteCast.Player)q.target();
            var stale=intent(c,0,1,new ModernHasteCast.Player(target.participant(),target.life()+1,target.equipmentRevision(),target.cell(),target.buffRevision()));
            for(var bad:java.util.List.of(self,next,other,stale))assertThrows(IllegalArgumentException.class,()->new ModernHasteProtocol.Reply(bad,success.reason(),success.view(),success.lifecycle()));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteProtocol.Reply(q,ModernHasteCast.Reason.TOO_FAST,success.view(),success.lifecycle()));
            var current=result.observation();var deniedQ=intent(current,0,2,ModernBattleHasteTest.target(current,1));
            var denial=f.engine.canonicalHaste(f.players.get(0).owner(),deniedQ);
            var denied=reply(deniedQ,denial,ModernHasteEffectsTest.publish(denial.observation()));
            assertEquals(ModernHasteCast.Reason.TOO_FAST,denied.reason());assertEquals(5,ModernHasteProtocol.encodeReply(denied,44)[184]);
            assertEquals(success.view().personal().history().resources(),denied.view().personal().history().resources());
        }
    }
    @Test void npcCastAndStaleRosterReplyRetainCompleteStatusInsteadOfDamageOrPlayerProjection()throws Exception {
        try(var f=ModernBattleNpcHasteTest.fixture(false,2,0,300)) {
            ModernBattleNpcHasteTest.active(f);var npc=ModernBattleNpcHasteTest.spawn(f,0,11,10);var c=f.engine.serviceObservation();
            var q=intent(c,0,1,ModernBattleNpcHasteTest.target(npc));
            var result=f.engine.canonicalHaste(f.players.get(0).owner(),q);
            var success=reply(q,result,ModernHasteEffectsTest.publish(f));assertEquals(ModernHasteCast.Reason.NONE,success.reason());
            assertNotNull(success.view().world().hasteEffects().npcs());assertEquals(5,ByteBuffer.wrap(ModernHasteProtocol.encodeReply(success,44)).getShort(220));
            var stale=intent(result.observation(),0,2,q.target());var denied=f.engine.canonicalHaste(f.players.get(0).owner(),stale);
            assertEquals(ModernHasteCast.Reason.STALE_TARGET,reply(stale,denied,ModernHasteEffectsTest.publish(f)).reason());
        }
    }
}
