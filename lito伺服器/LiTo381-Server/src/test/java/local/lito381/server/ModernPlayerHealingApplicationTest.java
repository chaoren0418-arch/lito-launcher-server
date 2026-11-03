package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernPlayerHealingApplicationTest {
    @TempDir Path root;
    static ModernSessionBoundary.Negotiated peer(int extensions){return new ModernSessionBoundary.Negotiated(1,47,1,-1,extensions);}
    @Test void selectedCommandIsBoundedAndOldOrReadOnlyContextsStayClosed()throws Exception {
        try(var f=new ModernPlayerHealingServiceTest.Fixture()) {
            f.active();var q=f.intent(0,1,1);var message=new ModernApplicationProtocol.PlayerHealingRequest(q);
            var bytes=ModernApplicationProtocol.encode(message,peer(3));assertEquals(160,bytes.length);
            assertArrayEquals(ModernPlayerHealingProtocol.encodeCandidateRequest(q),bytes);
            assertEquals(message,ModernApplicationProtocol.decode(bytes,peer(3)));
            var input=new ByteArrayInputStream(ByteBuffer.allocate(320).put(bytes).put(bytes).array());
            assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(input,peer(3)));assertEquals(160,input.available());
            for(int version=1;version<=47;version++){final int old=version;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,old));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,old));}
            for(int extension:new int[]{0,1,4,5}) {
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,peer(extension)));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,peer(extension)));
                var denied=new ByteArrayInputStream(bytes);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(denied,peer(extension)));
                assertEquals(144,denied.available(),"reject at header, before body allocation/read");
            }
            for(int size:new int[]{0,16,159,161,Integer.MAX_VALUE}) {
                var changed=bytes.clone();ByteBuffer.wrap(changed).putInt(4,size);var bad=new ByteArrayInputStream(changed);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(bad,peer(3)));assertEquals(144,bad.available());
            }
            for(int n=0;n<160;n++){var prefix=Arrays.copyOf(bytes,n);assertThrows(Exception.class,()->ModernApplicationProtocol.decode(prefix,peer(3)));}
            for(int offset:new int[]{0,8,15,120}) {
                var bad=bytes.clone();if(offset==8)ByteBuffer.wrap(bad).putInt(8,0);else bad[offset]^=0x7f;
                assertThrows(Exception.class,()->ModernApplicationProtocol.decode(bad,peer(3)));
            }
            for(var invalid:new ModernSessionBoundary.Negotiated[]{new ModernSessionBoundary.Negotiated(1,46,1,-1,3),
                    new ModernSessionBoundary.Negotiated(1,47,2,-1,3),peer(2),peer(6),peer(8),
                    new ModernSessionBoundary.Negotiated(1,47,1,-1&~ModernSessionProtocol.CAP_WEIGHT,3)})
                assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(message,invalid));
        }
    }
    @Test void fullSceneReadAndWorldResultsRetainEveryHealingTraceWithoutDowngrading()throws Exception {
        var count=new AtomicInteger();
        ModernHealingSceneTraceTest.trace(root,(label,capture,catalog)->{
            var publication=ModernBattleHealingWireTest.publication(capture,catalog);
            for(var owner:capture.ownerViews(capture.world()).keySet()) {
            var view=publication.audience(owner);var bytes=ModernBattleWire.encodePlayerHealing(view);var world=view.world().identity();
            var read=new ModernApplicationProtocol.BattleReadResult(2,new ModernBattleReadProtocol.Request(owner,world),new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,view));
            var request=new ModernBattleWorldProtocol.Request(owner,ModernBattleWorldProtocol.Action.READ,world.roomId(),0,world.matchId(),world.generation(),ModernWorldProtocol.ZERO_DIGEST);
            var result=new ModernApplicationProtocol.BattleWorldResult(3,request,new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,world,ModernBattleLifecycle.from(publication),view));
            for(var p:new ModernSessionBoundary.Negotiated[]{peer(1),peer(3)}) {
                payload(bytes,ModernApplicationProtocol.encode(read,p),ModernBattleReadProtocol.MIN_RESULT_BYTES);
                payload(bytes,ModernApplicationProtocol.encode(result,p),ModernBattleWorldProtocol.MIN_RESULT_BYTES);
            }
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(read,47));
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(read,peer(0)));
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(result,peer(0)));
            if(label.equals("npc-loading")) {
                var actor=view.world().actors().get(0);
                var proof=new ModernBattleCommandLife.Proof(owner,world,actor.pose().entityId(),actor.resources().life()+1,view.world().revision());
                var echo=new ModernBattleRevivalProtocol.Request(proof,proof.revision());
                var revival=new ModernApplicationProtocol.BattleRevivalResult(4,echo,new ModernBattleRevivalProtocol.Reply(ModernBattleRevivalProtocol.Reason.NOT_ACTIVE,view,ModernBattleLifecycle.from(publication)));
                payload(bytes,ModernApplicationProtocol.encode(revival,peer(3)),ModernBattleRevivalProtocol.MIN_RESULT_BYTES);
                assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(revival,peer(0)));
            }
            count.incrementAndGet();
            }
        });
        assertEquals(36,count.get()); // Eighteen captures, both source-scoped audiences.
    }
    @Test void actualServiceHealingAndOrdinaryOperationUseTheSelectedFormat()throws Exception {
        try(var f=new ModernPlayerHealingServiceTest.Fixture()) {
            f.active();var q=f.intent(0,1,1);
            var inbound=(ModernApplicationProtocol.PlayerHealingRequest)ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(new ModernApplicationProtocol.PlayerHealingRequest(q),peer(3)),peer(3));
            var reply=f.service.playerHeal(f.player(0).owner(),inbound.request());var result=new ModernApplicationProtocol.PlayerHealingResult(reply);
            var bytes=ModernApplicationProtocol.encode(result,peer(3));assertArrayEquals(ModernPlayerHealingProtocol.encodeCandidateReply(reply),bytes);
            payload(ModernBattleWire.encodePlayerHealing(reply.view()),bytes,ModernPlayerHealingProtocol.RESULT_PREFIX_BYTES);
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(result,peer(1)));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,peer(3)),"server result is never an inbound client command");
            var operation=f.move(0,1);var wire=ModernApplicationProtocol.encode(operation,peer(3));
            int prefix=ModernBattleOperationProtocol.encodeRequest(new ModernApplicationProtocol.BattleOperationRequest(operation.requestId(),operation.echo()),47).length+ModernBattleOperationProtocol.TRAILER_BYTES;
            payload(ModernBattleWire.encodePlayerHealing(operation.receipt().view()),wire,prefix);
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(operation,peer(0)));
        }
    }
    @Test void oldRepliesRemainByteIdenticalWithAnExplicitZeroExtensionContext()throws Exception {
        try(var f=new ModernHasteServiceTest.Fixture()) {
            f.active();var old=new ModernSessionBoundary.Negotiated(1,44,1,ModernHasteServiceTest.CAPS);
            var reply=f.read(0);var result=new ModernApplicationProtocol.BattleWorldResult(2,f.worldRequest(0,ModernBattleWorldProtocol.Action.READ),reply);
            assertArrayEquals(ModernApplicationProtocol.encode(result,44),ModernApplicationProtocol.encode(result,old));
            var haste=new ModernApplicationProtocol.HasteResult(f.service.haste(f.player(0).owner(),f.intent(0,1)));
            assertArrayEquals(ModernApplicationProtocol.encode(haste,44),ModernApplicationProtocol.encode(haste,old));
        }
    }
    @Test void hasteAndSlowRepliesPreserveHealingAndTheirOwnReceipts()throws Exception {
        try(var f=ModernBattleHealingWireTest.statusFixture(2)) {
            var before=ModernBattlePlayerHealingTest.active(f);
            var q=ModernHasteProtocolTest.intent(before,0,1,ModernBattleHasteTest.target(before,1));
            var cast=f.engine.canonicalHaste(f.players.get(0).owner(),q);assertEquals(ModernHasteCast.Reason.NONE,cast.reason());
            var publication=ModernBattleHealingWireTest.publication(cast.observation(),null);
            var haste=new ModernApplicationProtocol.HasteResult(ModernHasteProtocolTest.reply(q,cast,publication));
            payload(ModernBattleWire.encodePlayerHealing(haste.reply().view()),ModernApplicationProtocol.encode(haste,peer(3)),ModernHasteProtocol.RESULT_PREFIX_BYTES);
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(haste,peer(0)));
            f.elapsed(2_000_000_000L);var current=ModernBattleHealingWireTest.publication(f.engine.serviceObservation(),null);
            var slowQ=ModernSlowProtocolTest.intent(current.audience(f.players.get(0).scope()),1,2);
            var slowCast=f.engine.canonicalSlow(f.players.get(0).owner(),slowQ);assertEquals(ModernSlowCast.Reason.NONE,slowCast.reason());
            var next=ModernBattleHealingWireTest.publication(slowCast.observation(),null);
            var slow=new ModernApplicationProtocol.SlowResult(new ModernSlowProtocol.Reply(slowQ,slowCast.reason(),next.audience(slowQ.proof().player()),ModernBattleLifecycle.from(next)));
            payload(ModernBattleWire.encodePlayerHealing(slow.reply().view()),ModernApplicationProtocol.encode(slow,peer(3)),ModernSlowProtocol.RESULT_PREFIX_BYTES);
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(slow,peer(0)));
        }
    }
    @Test void greaterHasteReplyPreservesItsLeaseAlongsideHealing()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,300,1_000_000_000L,1,true,true,
                p->ModernBattleGreaterHasteTest.enable(p,ModernGreaterHasteCatalog.Status.QUALIFIED).withPlayerHealing())) {
            var before=ModernBattlePlayerHealingTest.active(f);var q=ModernGreaterHasteProtocolTest.intent(before,0,1);
            var cast=f.engine.canonicalGreaterHaste(f.players.get(0).owner(),q);assertEquals(ModernGreaterHasteCast.Reason.NONE,cast.reason());
            var p=ModernBattleHealingWireTest.publication(cast.observation(),null);
            var result=new ModernApplicationProtocol.GreaterHasteResult(new ModernGreaterHasteProtocol.Reply(q,cast.reason(),p.audience(q.proof().player()),ModernBattleLifecycle.from(p)));
            payload(ModernBattleWire.encodePlayerHealing(result.reply().view()),ModernApplicationProtocol.encode(result,peer(3)),ModernGreaterHasteProtocol.RESULT_PREFIX_BYTES);
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(result,peer(0)));
        }
    }
    static void payload(byte[] expected,byte[] frame,int offset) {
        assertEquals(frame.length,ByteBuffer.wrap(frame).getInt(4));assertEquals(expected.length,ByteBuffer.wrap(frame).getInt(offset-4));
        assertArrayEquals(expected,Arrays.copyOfRange(frame,offset,frame.length));assertEquals(7,ByteBuffer.wrap(frame).getShort(offset+4));
    }
}
