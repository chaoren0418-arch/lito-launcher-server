package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernGmCommandProtocol.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernGmCommandProtocolTest {
    @Test void realNestedTypedBotRosterPreservesLegacyBytesAndAddsOnlyV19CapacityFields()throws Exception {
        var f=new Fixture();var spawned=f.rooms.spawnBots(f.owner,f.scope,f.room.id(),f.room.revision(),2,f.grant());
        var request=new Request(f.scope,f.room.id(),f.room.revision(),2);var m=new ModernApplicationProtocol.GmResult(ID,request,Reply.result(spawned));
        var baseline=encode(m);
        for(int v=7;v<=ModernSessionProtocol.MAX_VERSION;v++){
            var bytes=ModernApplicationProtocol.encode(m,v);
            if(v<=18)assertArrayEquals(baseline,bytes,"version "+v);
            else {
                assertEquals(baseline.length+80,bytes.length);
                assertEquals(ByteBuffer.wrap(baseline).getInt(68)+80,ByteBuffer.wrap(bytes).getInt(68));
                assertArrayEquals(Arrays.copyOfRange(baseline,8,68),Arrays.copyOfRange(bytes,8,68));
                assertArrayEquals(Arrays.copyOfRange(baseline,72,142),Arrays.copyOfRange(bytes,72,142));
                assertEquals(100,Short.toUnsignedInt(ByteBuffer.wrap(bytes).getShort(142)));
                var bad=bytes.clone();ByteBuffer.wrap(bad).putShort(142,(short)101);
                int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,version));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,18));
            }
            assertEquals(m,ModernApplicationProtocol.decode(bytes,v));
            assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),v));
        }
        for(int v:new int[]{6,ModernSessionProtocol.MAX_VERSION+1}){
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(baseline,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernRoomProtocol.putBotResult(ByteBuffer.allocate(32768),f.scope,spawned.result(),v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernRoomProtocol.getBotResult(ByteBuffer.allocate(32768),f.scope,v));
        }
    }
    static final int ID = 0xfedcba98;
    static final Request REQUEST = new Request(new ModernRooms.Scope(7,3333,-1,2),0x100000002L,0x200000003L,20);
    static final String BODY_HEX = "000000070000000000000d05ffffffffffffffff020000000000000100000002000000020000000300140000";
    static final String REQUEST_HEX = "4c544d310000003cfedcba98001e0000"+BODY_HEX;
    static final String BUSY_HEX = "4c544d3100000048fedcba98001f0000"+BODY_HEX+"010000000000000000000000";
    static final String UNAVAILABLE_HEX = "4c544d3100000048fedcba98001f0000"+BODY_HEX+"020000000000000000000000";
    private static byte[] hex(String text) { return HexFormat.of().parseHex(text); }
    private static byte[] encode(ModernApplicationProtocol.Message message) throws Exception { return ModernApplicationProtocol.encode(message,7); }
    private static void reject(byte[] frame) {
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(frame,7));
    }

    @Test void literalBigEndianRequestAndNoSnapshotRepliesRoundTrip() throws Exception {
        var messages=List.<ModernApplicationProtocol.Message>of(new ModernApplicationProtocol.GmRequest(ID,REQUEST),
                new ModernApplicationProtocol.GmResult(ID,REQUEST,Reply.busy()),
                new ModernApplicationProtocol.GmResult(ID,REQUEST,Reply.unavailable()));
        var literals=List.of(REQUEST_HEX,BUSY_HEX,UNAVAILABLE_HEX);
        for(int i=0;i<messages.size();i++) {
            assertArrayEquals(hex(literals.get(i)),encode(messages.get(i)));
            assertEquals(messages.get(i),ModernApplicationProtocol.decode(hex(literals.get(i)),7));
        }
        assertEquals(44,hex(BODY_HEX).length);assertEquals(60,hex(REQUEST_HEX).length);assertEquals(72,hex(BUSY_HEX).length);
    }
    @Test void fullI64RoomObjectRevisionAndU64EpochNeverUseNativeNarrowing() throws Exception {
        for(long epoch:new long[]{1,Long.MAX_VALUE,Long.MIN_VALUE,-1})for(int count:new int[]{0,1,100}) {
            var request=new Request(new ModernRooms.Scope(Integer.MAX_VALUE,Long.MAX_VALUE,epoch,3),Long.MAX_VALUE,Long.MAX_VALUE,count);
            var message=new ModernApplicationProtocol.GmRequest(ID,request);
            assertEquals(message,ModernApplicationProtocol.decode(encode(message),7));
        }
        for(int count:new int[]{-1,101,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->new Request(REQUEST.scope(),1,1,count));
        for(long value:new long[]{0,-1,Long.MIN_VALUE}) {
            assertThrows(IllegalArgumentException.class,()->new Request(REQUEST.scope(),value,1,0));
            assertThrows(IllegalArgumentException.class,()->new Request(REQUEST.scope(),1,value,0));
        }
    }
    @Test void everyTruncationTrailingByteReservedUnknownAndOversizedCountIsRejected() {
        for(String literal:List.of(REQUEST_HEX,BUSY_HEX,UNAVAILABLE_HEX)) {
            byte[] good=hex(literal);
            for(int length=0;length<good.length;length++) {
                byte[] bad=Arrays.copyOf(good,length);if(length>=8)ByteBuffer.wrap(bad).putInt(4,length);reject(bad);
            }
            byte[] tail=Arrays.copyOf(good,good.length+1);ByteBuffer.wrap(tail).putInt(4,tail.length);reject(tail);
            for(int offset:new int[]{14,15,37,38,39,58,59}){byte[] bad=good.clone();bad[offset]=1;reject(bad);}
        }
        for(int offset:new int[]{61,62,63}){byte[] bad=hex(BUSY_HEX);bad[offset]=1;reject(bad);}
        byte[] bad=hex(REQUEST_HEX);ByteBuffer.wrap(bad).putShort(56,(short)101);reject(bad);
        bad=hex(BUSY_HEX);bad[60]=3;reject(bad);
        bad=hex(REQUEST_HEX);ByteBuffer.wrap(bad).putShort(12,(short)32);reject(bad);
        bad=hex(REQUEST_HEX);ByteBuffer.wrap(bad).putInt(8,0);reject(bad);
    }
    @Test void requestScopeAndKindVersionRemainStrict() throws Exception {
        byte[] good=hex(REQUEST_HEX);
        for(int offset:new int[]{16,20,28,40,48}) {
            byte[] bad=good.clone();if(offset==16)ByteBuffer.wrap(bad).putInt(offset,0);else ByteBuffer.wrap(bad).putLong(offset,0);reject(bad);
        }
        byte[] bad=good.clone();bad[36]=4;reject(bad);
        for(int version=1;version<=6;version++) {
            int old=version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(good,old));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.GmRequest(ID,REQUEST),old));
        }
    }
    @Test void backendRepliesCannotSmuggleCountsRoomPayloadOrSuccessfulShape() {
        for(String literal:List.of(BUSY_HEX,UNAVAILABLE_HEX))for(int offset:new int[]{64,66,68}) {
            byte[] bad=hex(literal);if(offset==68)ByteBuffer.wrap(bad).putInt(offset,1);else ByteBuffer.wrap(bad).putShort(offset,(short)1);reject(bad);
        }
        byte[] bad=hex(BUSY_HEX);bad[60]=0;reject(bad);
        assertThrows(IllegalArgumentException.class,()->new Reply(Delivery.RESULT,null));
    }
    @Test void actualRoomSpawnResultContainsConfirmedTypedRosterAndExactNestedReadScope() throws Exception {
        var fixture=new Fixture();var spawned=fixture.rooms.spawnBots(fixture.owner,fixture.scope,fixture.room.id(),fixture.room.revision(),20,fixture.grant());
        assertEquals(2,spawned.added());assertEquals(3,spawned.result().view().joined().members().size());
        var request=new Request(fixture.scope,fixture.room.id(),fixture.room.revision(),20);
        var message=new ModernApplicationProtocol.GmResult(ID,request,Reply.result(spawned));
        byte[] bytes=encode(message);assertEquals(message,ModernApplicationProtocol.decode(bytes,7));
        assertEquals(bytes.length-72,ByteBuffer.wrap(bytes).getInt(68));
        // The embedded room grammar is a payload, not a recursive LTM frame.
        assertEquals(fixture.scope.sourceId(),ByteBuffer.wrap(bytes).getInt(72));
        byte[] wrong=bytes.clone();ByteBuffer.wrap(wrong).putInt(72,42);reject(wrong);
        wrong=bytes.clone();wrong[93]=1;reject(wrong); // nested READ action changed to CREATE
        wrong=bytes.clone();ByteBuffer.wrap(wrong).putInt(68,ByteBuffer.wrap(bytes).getInt(68)-1);reject(wrong);
        wrong=bytes.clone();ByteBuffer.wrap(wrong).putShort(64,(short)21);reject(wrong);
        var current=spawned.result().view().joined().summary();
        var clear=fixture.rooms.spawnBots(fixture.owner,fixture.scope,current.id(),current.revision(),0,fixture.grant());
        var cleared=new ModernApplicationProtocol.GmResult(ID,new Request(fixture.scope,current.id(),current.revision(),0),Reply.result(clear));
        assertEquals(2,clear.removed());assertEquals(cleared,ModernApplicationProtocol.decode(encode(cleared),7));
    }
    @Test void freshGrantDenialOnlyUsesGmReasonAndNeverReportsMutation() throws Exception {
        var fixture=new Fixture();var denied=fixture.rooms.spawnBots(fixture.owner,fixture.scope,fixture.room.id(),fixture.room.revision(),1,null);
        assertEquals(ModernRooms.Reason.GM_NOT_AUTHORIZED,denied.result().reason());
        var message=new ModernApplicationProtocol.GmResult(ID,new Request(fixture.scope,fixture.room.id(),fixture.room.revision(),1),Reply.result(denied));
        assertEquals(message,ModernApplicationProtocol.decode(encode(message),7));
        byte[] bad=encode(message);ByteBuffer.wrap(bad).putShort(64,(short)1);reject(bad);
        var ordinary=new ModernApplicationProtocol.RoomResult(ID,new ModernRoomProtocol.Echo(fixture.scope,ModernRooms.Action.READ,0,0,0),denied.result());
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->encode(ordinary));
    }
    @Test void outerAndNestedFrameBoundsRejectBeforeUnboundedAllocation() {
        for(int kind:new int[]{30,31}) {
            int size=kind==30?61:MAX_FRAME_BYTES+1;
            byte[] header=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(size).putInt(ID).putShort((short)kind).putShort((short)0).array();
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header)));
        }
        byte[] bad=hex(BUSY_HEX);ByteBuffer.wrap(bad).putInt(68,Integer.MAX_VALUE);reject(bad);
    }
    private static final class Fixture {
        final TournamentPresence presence=new TournamentPresence();final TournamentPresence.Owner owner=presence.connect();
        final ModernWorldBinding.Key map=new ModernWorldBinding.Key(1,514);
        final ModernRooms rooms=new ModernRooms(Set.of(map),Set.of(map));
        final ModernRooms.Scope scope=new ModernRooms.Scope(7,3333,1,0);
        final ModernApplicationProtocol.CharacterRow character=new ModernApplicationProtocol.CharacterRow(3333,"GM",1,0,80);
        final ModernRooms.Summary room;
        Fixture() {
            var selected=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,3333),"Sun",character.name(),80,1,0);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selected));rooms.enterLobby(owner,selected,1,0,true);
            room=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,
                    new ModernRooms.Draft("training","",3,1,514,0),"")).view().joined().summary();
        }
        ModernGmCommands.Snapshot grant() { return new ModernGmCommands.Snapshot(7,"sun-owner",character,200,false,false,
                new ModernGmCommands.Definition("de",200,"L1SpawnDe",false)); }
    }
}
