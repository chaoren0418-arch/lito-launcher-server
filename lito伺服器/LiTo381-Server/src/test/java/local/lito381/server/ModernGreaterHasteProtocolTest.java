package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleGreaterHasteTest.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ModernGreaterHasteProtocolTest {
    static ModernGreaterHasteProtocol.Request intent(ModernBattleObservation.Capture c,int index,long sequence) {
        var a=c.actors().get(index);var proof=new ModernBattleCommandLife.Proof(a.scope(),c.identity(),a.pose().entityId(),a.resources().life()+1,c.revision());
        return new ModernGreaterHasteProtocol.Request(-1,proof,sequence,a.resources().equipmentRevision(),a.pose().position().x(),a.pose().position().y(),a.buffs().revision());
    }
    static ModernGreaterHasteProtocol.Reply reply(ModernGreaterHasteProtocol.Request q,ModernBattleEngine.GreaterHasteResult result) {
        var p=ModernGreaterHasteWireTest.publication(result.observation());
        return new ModernGreaterHasteProtocol.Reply(q,result.reason(),p.audience(q.proof().player()),ModernBattleLifecycle.from(p));
    }
    @Test void requestIsExactBoundedBigEndianAndContainsNoClientPriceOrClock()throws Exception {
        try(var f=fixture()) {
            var q=intent(active(f),0,1);var bytes=ModernGreaterHasteProtocol.encodeRequest(q,42);
            assertEquals(120,bytes.length);var b=ByteBuffer.wrap(bytes);assertEquals(0x4c544d31,b.getInt());assertEquals(120,b.getInt());assertEquals(-1,b.getInt());
            assertEquals(72,b.getShort());assertEquals(0,b.getShort());assertEquals(7,b.getInt());
            assertEquals(q,ModernGreaterHasteProtocol.decodeRequest(bytes,42));assertEquals(q.sequence(),ByteBuffer.wrap(bytes).getLong(88));
            assertEquals(q.buffRevision(),ByteBuffer.wrap(bytes).getLong(112));
            assertArrayEquals(bytes,ModernGreaterHasteProtocol.encodeRequest(q,43));
            assertEquals(q,ModernGreaterHasteProtocol.decodeRequest(bytes,43));
            assertArrayEquals(bytes,ModernGreaterHasteProtocol.encodeRequest(q,44));
            assertEquals(q,ModernGreaterHasteProtocol.decodeRequest(bytes,44));
            for(int version:new int[]{1,41,ModernSessionProtocol.MAX_VERSION+1}) {
                assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.encodeRequest(q,version));
                assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.decodeRequest(bytes,version));
            }
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,41));
            assertEquals(47,ModernSessionProtocol.MAX_VERSION);
        }
    }
    @Test void everyTruncationTrailingUnknownKindPaddingAndInvalidScopeIsRejected()throws Exception {
        try(var f=fixture()) {
            var bytes=ModernGreaterHasteProtocol.encodeRequest(intent(active(f),0,1),42);
            for(int length=0;length<bytes.length;length++){var shortBytes=Arrays.copyOf(bytes,length);assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.decodeRequest(shortBytes,42));}
            assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.decodeRequest(Arrays.copyOf(bytes,121),42));
            for(int offset:new int[]{0,4,13,15,39}){var bad=bytes.clone();bad[offset]^=127;assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.decodeRequest(bad,42));}
            for(int offset:new int[]{8,16}){var bad=bytes.clone();ByteBuffer.wrap(bad).putInt(offset,0);assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.decodeRequest(bad,42));}
            for(int offset:new int[]{20,28,40,48,56,64,72,80,88}){var bad=bytes.clone();ByteBuffer.wrap(bad).putLong(offset,0);assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.decodeRequest(bad,42));}
        }
    }
    @Test void realSuccessAndDenialHaveDifferentReceiptsButEchoTheExactSubmittedIntent()throws Exception {
        try(var f=fixture()) {
            var q=intent(active(f),0,1);var success=reply(q,f.engine.greaterHaste(f.players.get(0).owner(),q.command()));
            assertEquals(ModernGreaterHasteCast.Reason.NONE,success.reason());var bytes=ModernGreaterHasteProtocol.encodeReply(success,42);
            ModernNpcMotionWireTest.assertPayload43(ModernGreaterHasteProtocol.encodeReply(success,43),success.view());
            assertEquals(bytes.length,ByteBuffer.wrap(bytes).getInt(4));assertEquals(73,ByteBuffer.wrap(bytes).getShort(12));
            assertEquals(0,bytes[120]);assertEquals(bytes.length-152,ByteBuffer.wrap(bytes).getInt(148));assertEquals(3,ByteBuffer.wrap(bytes).getShort(156));
            var next=intent(f.engine.serviceObservation(),0,2);var denied=reply(next,f.engine.greaterHaste(f.players.get(0).owner(),next.command()));
            assertEquals(ModernGreaterHasteCast.Reason.TOO_FAST,denied.reason());assertEquals(5,ModernGreaterHasteProtocol.encodeReply(denied,42)[120]);
            assertEquals(success.view().personal().history().resources(),denied.view().personal().history().resources());
            assertEquals(success.view().personal().history().spells(),denied.view().personal().history().spells());
        }
    }
    @Test void oldSuccessCannotAcknowledgeANewRequestAndSourcesCannotBorrowReceipts()throws Exception {
        try(var f=fixture()) {
            var q=intent(active(f),0,1);var success=reply(q,f.engine.greaterHaste(f.players.get(0).owner(),q.command()));
            var next=intent(f.engine.serviceObservation(),0,2);
            assertThrows(IllegalArgumentException.class,()->new ModernGreaterHasteProtocol.Reply(next,success.reason(),success.view(),success.lifecycle()));
            var foreign=intent(f.engine.serviceObservation(),1,1);
            assertThrows(IllegalArgumentException.class,()->new ModernGreaterHasteProtocol.Reply(foreign,success.reason(),success.view(),success.lifecycle()));
            assertThrows(IllegalArgumentException.class,()->new ModernGreaterHasteProtocol.Reply(q,ModernGreaterHasteCast.Reason.TOO_FAST,success.view(),success.lifecycle()));
            assertThrows(IllegalArgumentException.class,()->new ModernGreaterHasteProtocol.Reply(q,ModernGreaterHasteCast.Reason.NOT_ACTIVE,success.view(),success.lifecycle()));
        }
    }
}
