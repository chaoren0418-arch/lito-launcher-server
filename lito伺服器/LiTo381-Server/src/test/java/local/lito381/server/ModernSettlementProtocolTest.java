package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Literal v3 appendix arithmetic, while all existing v1/v2 goldens stay unchanged. */
class ModernSettlementProtocolTest {
    // presence/reserved + UUID + match/generation/engine/time + mode/state/reserved/map/count
    // + two exact entity/source/object/team/verdict/reserved rows; shared with Rust.
    static final String TAIL = "01000000" + "00112233445566778899aabbccddeeff"
            + "00000000000000090000000000000006000000000000000700000000000004d2"
            + "010000000000020200020000"
            + "0000000000000001000000070000000000000d0500000000"
            + "00000000000000020000002a0000000000000d0501010000";
    static ModernWorldProtocol.Frame finished() {
        var base=ModernWorldProtocolTest.frame();var first=base.room().members().get(0);
        var second=new ModernRooms.Member(new TournamentPresence.CharacterKey(42,3333),"M","Moon",80,3,1,1,true);
        var room=new ModernRooms.RoomSnapshot(new ModernRooms.Summary(5,6,new ModernRooms.Config("R",2,1,514,32),2,2,true),first.key(),List.of(first,second));
        var actors=List.of(new ModernBattleEngine.Actor(1,first,new ModernWorldMap.Cell(32724,32784),2,true,0,50_000_000),
                new ModernBattleEngine.Actor(2,second,new ModernWorldMap.Cell(32725,32784),6,true,0,50_000_000));
        var event=new ModernMatchSettlement.Event(UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),9,6,7,1,514,1234,
                List.of(new ModernMatchSettlement.Participant(1,first.key(),0,ModernMatchSettlement.Verdict.WIN),
                        new ModernMatchSettlement.Participant(2,second.key(),1,ModernMatchSettlement.Verdict.LOSS)));
        return new ModernWorldProtocol.Frame(9,6,8,ModernWorldProtocol.Phase.FINISHED,ModernWorldProtocol.AbortReason.NONE,
                room,base.manifest(),actors,new ModernWorldProtocol.Completion(event,ModernWorldProtocol.SettlementState.PENDING));
    }
    static ModernApplicationProtocol.WorldResult message(ModernWorldProtocol.Frame frame) {
        return new ModernApplicationProtocol.WorldResult(31,ModernWorldProtocolTest.read(),
                new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,frame));
    }
    @Test void literalAppendixV3OnlyWithExactOldBytePrefix() throws Exception {
        var frame=finished();var message=message(frame);byte[] encoded=ModernApplicationProtocol.encode(message,3);
        byte[] tail=HexFormat.of().parseHex(TAIL);assertEquals(112,tail.length);
        assertArrayEquals(tail,Arrays.copyOfRange(encoded,encoded.length-tail.length,encoded.length));
        assertEquals(message,ModernApplicationProtocol.decode(encoded,3));
        for(int version:List.of(1,2)) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,version));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(encoded,version));
        }
        var active=new ModernWorldProtocol.Frame(frame.matchId(),frame.generation(),frame.revision(),ModernWorldProtocol.Phase.ACTIVE,
                frame.abortReason(),frame.room(),frame.manifest(),frame.actors());
        byte[] old=ModernApplicationProtocol.encode(message(active),2);
        byte[] expected=Arrays.copyOf(old,old.length+tail.length);System.arraycopy(tail,0,expected,old.length,tail.length);
        ByteBuffer.wrap(expected).putInt(4,expected.length);expected[124+24]=5;
        assertArrayEquals(expected,encoded);
        byte[] ongoing=ModernApplicationProtocol.encode(message(active),3);
        assertEquals(old.length+4,ongoing.length);
        assertEquals(message(active),ModernApplicationProtocol.decode(ongoing,3));
    }
    @Test void missingExtraTruncatedUnknownReservedAndCrossIdentityNeverAccepted() throws Exception {
        byte[] encoded=ModernApplicationProtocol.encode(message(finished()),3);int start=encoded.length-112;
        for(int length=0;length<encoded.length;length++) {
            byte[] cut=Arrays.copyOf(encoded,length);if(length>=8)ByteBuffer.wrap(cut).putInt(4,length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,3));
        }
        for(int offset:new int[]{start,start+1,start+53,start+54,start+62,start+64+23,start+88+21}) {
            byte[] bad=encoded.clone();bad[offset]=(byte)255;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,3));
        }
        byte[] revisionBad=encoded.clone();ByteBuffer.wrap(revisionBad).putLong(start+36,8); // engine revision cannot equal world revision
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(revisionBad,3));
        byte[] zero=encoded.clone();Arrays.fill(zero,start+4,start+20,(byte)0);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(zero,3));
        byte[] cross=encoded.clone();ByteBuffer.wrap(cross).putInt(start+64+8,42);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cross,3));
        byte[] extra=Arrays.copyOf(encoded,encoded.length+1);ByteBuffer.wrap(extra).putInt(4,extra.length);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(extra,3));
    }
    @Test void lastAttackCanFinishAtomicallyButCannotClaimLaterReceiptRevision() throws Exception {
        var f=finished();var request=new ModernCombatProtocol.Request(ModernWorldProtocolTest.read().scope(),ModernCombatProtocol.Action.ATTACK,9,6,1,2);
        var vitals=List.of(new ModernCombatProtocol.Vital(1,50,50,0,0,-9,0,1,8,0,0,2,1_000_000_000),
                new ModernCombatProtocol.Vital(2,0,50,0,0,-10,0,0,0,8,8,0,1_000_000_000));
        var result=new ModernApplicationProtocol.CombatResult(31,request,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,
                ModernCombatProtocol.Reason.NONE,new ModernCombatProtocol.Frame(f,vitals)));
        assertEquals(result,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(result,3),3));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(result,2));
        var saved=new ModernWorldProtocol.Frame(9,6,9,f.phase(),f.abortReason(),f.room(),f.manifest(),f.actors(),
                new ModernWorldProtocol.Completion(f.completion().event(),ModernWorldProtocol.SettlementState.SAVED));
        var wrong=new ModernApplicationProtocol.CombatResult(31,request,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,
                ModernCombatProtocol.Reason.NONE,new ModernCombatProtocol.Frame(saved,vitals)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(wrong,3));
    }
    @Test void newHelloNegotiatesOldAndNewPolicyWithoutNewCapabilityBits() throws Exception {
        var hello=new ModernSessionProtocol.Hello(31,1,4,255,7,1);
        for(int max:List.of(1,2,3,4)) {
            var boundary=new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true,true).withMaxVersion(max));
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(hello)));
            assertEquals(max,ack.version());assertEquals(255,ack.capabilities());
        }
        assertDoesNotThrow(()->ModernSessionBoundary.Policy.current().withMaxVersion(5));
        assertDoesNotThrow(()->ModernSessionBoundary.Policy.current().withMaxVersion(6));
        assertDoesNotThrow(()->ModernSessionBoundary.Policy.current().withMaxVersion(7));
        assertDoesNotThrow(()->ModernSessionBoundary.Policy.current().withMaxVersion(8));
        assertDoesNotThrow(()->ModernSessionBoundary.Policy.current().withMaxVersion(9));
        assertThrows(IllegalArgumentException.class,()->ModernSessionBoundary.Policy.current().withMaxVersion(ModernSessionProtocol.MAX_VERSION+1));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(31,1,3,
                255 | ModernSessionProtocol.CAP_SLOW_DATA,7,1)));
    }
    @Test void finishedCombatRequiresDeadLosingTeamLivingWinnerAndExactLastKillRevision() {
        var f=finished();
        var winner=new ModernCombatProtocol.Vital(1,50,50,0,0,-9,0,1,8,0,0,2,1_000_000_000);
        var loser=new ModernCombatProtocol.Vital(2,0,50,0,0,-10,0,0,0,8,8,0,1_000_000_000);
        assertDoesNotThrow(()->new ModernCombatProtocol.Frame(f,List.of(winner,loser)));
        var livingLoser=new ModernCombatProtocol.Vital(2,1,50,0,0,-10,0,0,0,8,0,0,1_000_000_000);
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(f,List.of(winner,livingLoser)));
        var deadWinner=new ModernCombatProtocol.Vital(1,0,50,0,0,-9,0,1,7,7,7,2,1_000_000_000);
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(f,List.of(deadWinner,loser)));
        var earlierDeath=new ModernCombatProtocol.Vital(2,0,50,0,0,-10,0,0,0,7,7,0,1_000_000_000);
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(f,List.of(winner,earlierDeath)));
        var saved=new ModernWorldProtocol.Frame(9,6,9,f.phase(),f.abortReason(),f.room(),f.manifest(),f.actors(),
                new ModernWorldProtocol.Completion(f.completion().event(),ModernWorldProtocol.SettlementState.SAVED));
        var receiptAttack=new ModernCombatProtocol.Vital(1,50,50,0,0,-9,0,2,9,0,0,2,1_000_000_000);
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(saved,List.of(receiptAttack,loser)));
        assertDoesNotThrow(()->new ModernCombatProtocol.Frame(saved,List.of(winner,loser)));
    }
}
