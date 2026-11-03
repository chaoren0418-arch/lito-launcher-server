package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import static local.lito381.server.ModernBattleOperationProtocol.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernBattleOperationProtocolTest {
    @TempDir Path root;
    static BattleOperationRequest move(ModernBattleLifecycleServiceTest.Fixture f,long sequence) {
        var q=ModernBattleCanonicalActionServiceTest.move(f,sequence,4);
        return new BattleOperationRequest(q.requestId(),new ModernBattleActionProtocol.Request(ModernBattleCommandLifeTest.proof(f.engine,0),ModernBattleActionProtocol.NO_TARGET,q));
    }
    static int metadata(byte[] bytes){return 116+ByteBuffer.wrap(bytes).getInt(112);}
    static byte[] encoded(BattleOperationResult result)throws Exception {
        ModernNpcMotionWireTest.assertPayload43(ModernApplicationProtocol.encode(result,43),result.receipt().view());
        var b=ModernApplicationProtocol.encode(result,38);var meta=metadata(b);var p=ByteBuffer.wrap(b);
        assertEquals(RESULT_KIND,Short.toUnsignedInt(p.getShort(12)));assertEquals(b.length,p.getInt(4));
        assertEquals(result.receipt().lifecycle().revision(),p.getLong(meta+4));
        assertEquals(b.length-meta-TRAILER_BYTES,p.getInt(meta+28));
        assertArrayEquals(ModernBattleWire.encode(result.receipt().view(),ModernBattleWire.PRESENTATION_SCHEMA),Arrays.copyOfRange(b,meta+TRAILER_BYTES,b.length));
        return b;
    }
    @Test void distinctSmallRequestIsBoundAndCannotDowngradeOrNest()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,38)) {
            var q=move(f,1);var b=ModernApplicationProtocol.encode(q,38);
            assertEquals(236,b.length);assertEquals(q,ModernApplicationProtocol.decode(b,38));
            assertArrayEquals(b,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(b),38));
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.encode(q,37));
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(b,37));
            var nested=b.clone();ByteBuffer.wrap(nested).putShort(128,(short)REQUEST_KIND);
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(nested,38));
            assertFalse(negotiated(new ModernSessionBoundary.Negotiated(1,37,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
            assertFalse(negotiated(new ModernSessionBoundary.Negotiated(1,38,1,ModernBattleReadProtocolTest.CAPS)));
        }
    }
    @Test void inboundHeaderRejectsServerReplyAndOversizeBeforeReadingBody()throws Exception {
        for(int kind:new int[]{REQUEST_KIND,RESULT_KIND}) {
            var b=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(kind==REQUEST_KIND?MAX_REQUEST_BYTES+1:MIN_RESULT_BYTES).putInt(1).putShort((short)kind).putShort((short)0).array();
            var in=new InputStream(){int at;public int read(){if(at==16)fail("body read before header rejection");return b[at++]&255;}};
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.readFrame(in,38));
        }
    }
    @Test void realMoveCarriesExactAudienceNotLegacyAckAndRequiresPeerCapabilities()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,38)) {
            var q=move(f,1);var result=f.service.operation(f.player(0).owner(),q);var bytes=encoded(result);int at=metadata(bytes);
            assertEquals(Disposition.APPLIED,outcome(q.request(),result.receipt()).disposition());
            assertEquals(1,result.receipt().view().world().actors().get(0).pose().lastMoveSequence());
            assertEquals(0,bytes[at]);assertEquals(1,bytes[at+1]);assertEquals(0,bytes[at+2]);
            assertSame(result,ModernMatchDescriptor.forPeer(result,new ModernSessionBoundary.Negotiated(1,38,1,ModernBattleReadProtocolTest.CAPS|ModernSessionProtocol.CAP_ACTION_LIFE)));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(result,new ModernSessionBoundary.Negotiated(1,37,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.encode(result,37));
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,38));
        }
    }
    @Test void mixedPriceHistorySurvivesActualNewWireAndDenial()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,38)) {
            f.reprice();var q=move(f,++f.moves);var moved=f.service.operation(f.player(0).owner(),q);encoded(moved);
            assertEquals(1,moved.receipt().view().personal().equipment().revision());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(moved.receipt().outcome().observation()));
            Thread.sleep(2);var spell=new CastRequest(10,new ModernSpellProtocol.Request(f.player(0).scope(),f.world.matchId(),f.world.generation(),3,70,0));
            var cast=new BattleOperationRequest(10,new ModernBattleActionProtocol.Request(ModernBattleCommandLifeTest.proof(f.engine,0),ModernBattleActionProtocol.NO_TARGET,spell));
            var denied=f.service.operation(f.player(0).owner(),cast);encoded(denied);
            assertEquals(new Outcome(Disposition.DENIED,Reason.NO_MP),outcome(cast.request(),denied.receipt()));
        }
    }
    @Test void staleLifeHasNoTypedAckAndCannotConsumeSequenceOrCrossOwner()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,38)) {
            var q=move(f,1);var p=q.request().proof();var stale=new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life()+1,p.revision());
            var old=new BattleOperationRequest(9,new ModernBattleActionProtocol.Request(stale,q.request().target(),q.request().command()));
            assertThrows(SecurityException.class,()->f.service.operation(f.player(1).owner(),old));
            var result=f.service.operation(f.player(0).owner(),old);var bytes=encoded(result);var at=metadata(bytes);
            assertEquals(2,bytes[at]);assertEquals(3,bytes[at+1]);assertNull(result.receipt().outcome());
            assertEquals(0,result.receipt().view().world().actors().get(0).pose().lastMoveSequence());
            var applied=f.service.operation(f.player(0).owner(),q);encoded(applied);
            assertEquals(Disposition.APPLIED,outcome(q.request(),applied.receipt()).disposition());
        }
    }
    @Test void terminalReplyCarriesSavedStateAndOriginalResult()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,38)) {
            f.win();ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
            var result=f.service.operation(f.player(0).owner(),move(f,1));var bytes=encoded(result);var at=metadata(bytes);
            assertEquals(1,bytes[at]);assertEquals(3,bytes[at+1]);assertEquals(5,bytes[at+12]);assertEquals(2,bytes[at+14]);
            assertEquals(f.event.get(),result.receipt().view().world().result());
        }
    }
    @Test void oldRegistrationCannotUseNewOperationOrConsumeACommand()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null)) {
            var q=move(f,1);assertThrows(SecurityException.class,()->f.service.operation(f.player(0).owner(),q));
            assertEquals(0,f.engine.serviceObservation().actors().get(0).pose().lastMoveSequence());
        }
    }
}
