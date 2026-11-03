package local.lito381.server;

import static local.lito381.server.ModernRooms.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernRoomProtocolTest {
    static final Scope SCOPE = new Scope(7, Long.MAX_VALUE, -1, 0);
    static Command read() { return new Command(SCOPE, Action.READ, 0, 0, 0, null, ""); }
    static View empty() {
        var presence = new TournamentPresence(); var owner = presence.connect(); var rooms = new ModernRooms();
        try {
            rooms.enterLobby(owner, new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,Long.MAX_VALUE),
                    "太陽神", "測試", Long.MAX_VALUE, 1, 0), -1, 0);
            return rooms.execute(owner, read()).view();
        } finally { rooms.disconnect(owner); presence.disconnect(owner); }
    }
    static ModernApplicationProtocol.RoomResult result(View view) {
        return new ModernApplicationProtocol.RoomResult(0xfedcba98, read(), new Result(Status.OK, Reason.NONE, view));
    }
    @Test void v19CapacityIsStrictAndVersionsOneThroughEighteenKeepOriginalLayout()throws Exception {
        var old=empty();var options=new ArrayList<>(old.options());var first=options.get(0);
        options.set(0,new Option(first.mode(),first.mapId(),first.minimumPlayers(),first.timeLimitSeconds(),first.teamBased(),first.modeName(),first.worldAvailable(),48));
        var bounded=result(new View(old.listRevision(),old.rooms(),old.joined(),options));
        var wire=ModernApplicationProtocol.encode(bounded,19);var legacy=ModernApplicationProtocol.encode(bounded,18);
        assertEquals(legacy.length+80,wire.length);assertEquals(48,Short.toUnsignedInt(ByteBuffer.wrap(wire).getShort(86)));
        assertEquals(bounded,ModernApplicationProtocol.decode(wire,19));
        for(int version=1;version<=18;version++) {
            var bytes=ModernApplicationProtocol.encode(bounded,version);
            assertArrayEquals(ModernApplicationProtocol.encode(result(old),version),bytes);
            assertEquals(result(old),ModernApplicationProtocol.decode(bytes,version));
        }
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(wire,18));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(legacy,19));
        for(int value:new int[]{0,1,101,65535}) {
            var bad=wire.clone();ByteBuffer.wrap(bad).putShort(86,(short)value);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,19));
        }
    }

    @Test void exactRequestLayoutPreservesI64AndPasswordsAreNotEchoedOrLogged() throws Exception {
        var draft = new Draft("測試房間", " secret 密碼 ", 100, 1, 564, 48);
        var request = new ModernApplicationProtocol.RoomRequest(0xfedcba98,
                new Command(SCOPE,Action.CREATE,0,0,0,draft,""));
        byte[] frame = ModernApplicationProtocol.encode(request);
        var in = ByteBuffer.wrap(frame);
        assertEquals(18, Short.toUnsignedInt(in.getShort(12))); assertEquals(7,in.getInt(16));
        assertEquals(Long.MAX_VALUE,in.getLong(20)); assertEquals(-1,in.getLong(28));
        assertEquals(1,frame[37]); assertEquals(0,in.getLong(40)); assertEquals(0,in.getLong(48));
        assertEquals(100,in.getShort(56)); assertEquals(564,in.getInt(60));
        assertEquals(request,ModernApplicationProtocol.decode(frame));
        assertFalse(request.toString().contains(draft.password())); assertFalse(draft.toString().contains(draft.password()));
        byte[] reply = ModernApplicationProtocol.encode(result(empty()));
        assertFalse(new String(reply,StandardCharsets.UTF_8).contains(draft.password()));
        assertEquals(19,Short.toUnsignedInt(ByteBuffer.wrap(reply).getShort(12)));
        assertEquals(result(empty()),ModernApplicationProtocol.decode(reply));
    }

    @Test void allActionsHaveStrictUnusedFieldsAndRoundTrip() throws Exception {
        for (var action : Action.values()) {
            boolean zero = action == Action.READ || action == Action.CREATE;
            var draft = action == Action.CREATE || action == Action.CONFIGURE ? new Draft("r", "  ", 8, 1, 564, 16) : null;
            var command = new Command(SCOPE,action,zero?0:9,zero?0:Long.MAX_VALUE,
                    action==Action.TEAM || action==Action.READY?1:0,draft,action==Action.JOIN?"secret":"");
            var request = new ModernApplicationProtocol.RoomRequest(1,command);
            assertEquals(request,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(request)));
        }
        assertThrows(IllegalArgumentException.class,()->new Command(SCOPE,Action.READ,1,1,0,null,""));
        assertThrows(IllegalArgumentException.class,()->new Command(SCOPE,Action.READ,0,0,1,null,""));
        assertThrows(IllegalArgumentException.class,()->new Command(SCOPE,Action.READ,0,0,0,null,"unused"));
        assertThrows(IllegalArgumentException.class,()->new Command(SCOPE,Action.JOIN,1,0,0,null,""));
        assertThrows(IllegalArgumentException.class,()->new Draft("\u00a0", "", 8,1,564,0));
        assertThrows(IllegalArgumentException.class,()->new Draft("r", "\ud800", 8,1,564,0));
    }

    @Test void requestRejectsEveryTruncationTrailingBytesUnknownActionAndReservedFields() throws Exception {
        byte[] frame=ModernApplicationProtocol.encode(new ModernApplicationProtocol.RoomRequest(1,read()));
        for (int size=0;size<frame.length;size++) {
            byte[] shortFrame=Arrays.copyOf(frame,size);
            if(size>=8)ByteBuffer.wrap(shortFrame).putInt(4,size);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shortFrame));
        }
        for(int[] edit:new int[][]{{36,4},{37,7},{38,1},{39,1},{15,1}}) {
            byte[] bad=frame.clone();bad[edit[0]]=(byte)edit[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad));
        }
        byte[] trailing=Arrays.copyOf(frame,frame.length+1);ByteBuffer.wrap(trailing).putInt(4,trailing.length);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(trailing));
    }

    @Test void resultRejectsMalformedCountsBooleansRulesWorldFlagsAndUtf8() throws Exception {
        byte[] frame=ModernApplicationProtocol.encode(result(empty()));
        // Header16 + common40 + result metadata20 = first option at76.
        for(int[] edit:new int[][]{{56,5},{57,7},{58,1},{68,1},{70,1},{71,39},{72,2},{73,1},
                {76,6},{77,1},{78,2},{79,1},{90,1},{91,0}}) {
            byte[] bad=frame.clone();bad[edit[0]]=(byte)edit[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad),
                    "offset "+edit[0]);
        }
        byte[] available=frame.clone();available[78]=1;
        var availableResult=(ModernApplicationProtocol.RoomResult)ModernApplicationProtocol.decode(available);
        assertTrue(availableResult.result().view().options().get(0).worldAvailable());
        byte[] badUtf8=frame.clone();badUtf8[92]=(byte)0xff;
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(badUtf8));
        for(int size=0;size<frame.length;size++) {
            byte[] shorter=Arrays.copyOf(frame,size);if(size>=8)ByteBuffer.wrap(shorter).putInt(4,size);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shorter));
        }
    }

    @Test void maximumLegalRoomSnapshotRoundTripsAndKindSpecificLimitsRejectBeforeBodyRead() throws Exception {
        var members=new ArrayList<Member>();
        for(long id=1;id<100;id++)members.add(new Member(new TournamentPresence.CharacterKey(7,id),"n".repeat(256),"s".repeat(256),Long.MAX_VALUE,1,0,0,false));
        var self=new TournamentPresence.CharacterKey(7,Long.MAX_VALUE);
        members.add(new Member(self,"n".repeat(256),"s".repeat(256),Long.MAX_VALUE,1,0,1,false));
        var config=new Config("r".repeat(64),100,1,564,16);
        var summaries=new ArrayList<Summary>();
        for(long id=1;id<100;id++)summaries.add(new Summary(id,Long.MAX_VALUE,config,100,0,false));
        var joinedSummary=new Summary(Long.MAX_VALUE,Long.MAX_VALUE,config,100,0,false);summaries.add(joinedSummary);
        var message=result(new View(Long.MAX_VALUE,summaries,new RoomSnapshot(joinedSummary,self,members),empty().options()));
        byte[] frame=ModernApplicationProtocol.encode(message);
        assertTrue(frame.length>60_000); assertTrue(frame.length<ModernRoomProtocol.MAX_FRAME_BYTES);
        assertEquals(message,ModernApplicationProtocol.decode(frame));
        var stream=new ByteArrayInputStream(frame);assertArrayEquals(frame,ModernApplicationProtocol.readFrame(stream));assertEquals(0,stream.available());
        for(int kind:List.of(18,19)) {
            byte[] oversized=Arrays.copyOf(frame,16);var header=ByteBuffer.wrap(oversized);
            header.putShort(12,(short)kind).putInt(4,(kind==19?ModernRoomProtocol.MAX_FRAME_BYTES:ModernApplicationProtocol.MAX_FRAME_BYTES)+1);
            var input=new ByteArrayInputStream(oversized);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(input));
            assertEquals(0,input.available());
        }
    }

    @Test void joinedResultRequiresAuthenticatedEchoMemberAndSuccessfulActionTruth() throws Exception {
        var presence=new TournamentPresence();var owner=presence.connect();var rooms=new ModernRooms();
        try {
            rooms.enterLobby(owner,new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,Long.MAX_VALUE),"太陽神","測試",Long.MAX_VALUE,1,0),-1,0);
            var command=new Command(SCOPE,Action.CREATE,0,0,0,new Draft("r","",8,1,564,16),"");
            var created=rooms.execute(owner,command);var valid=new ModernApplicationProtocol.RoomResult(2,command,created);
            assertEquals(valid,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(valid)));
            var original=created.view().joined();
            var incremented=new Summary(original.summary().id(),2,original.summary().config(),1,0,false);
            var impossibleCreate=new Result(Status.OK,Reason.NONE,new View(3,List.of(incremented),
                    new RoomSnapshot(incremented,original.owner(),original.members()),created.view().options()));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                    new ModernApplicationProtocol.RoomResult(2,command,impossibleCreate)));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                    new ModernApplicationProtocol.RoomResult(2,new Command(SCOPE,Action.JOIN,original.summary().id(),1,0,null,""),created)));
            var foreignScope=new Scope(42,Long.MAX_VALUE,-1,0);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                    new ModernApplicationProtocol.RoomResult(2,new Command(foreignScope,Action.READ,0,0,0,null,""),created)));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                    new ModernApplicationProtocol.RoomResult(2,new Command(SCOPE,Action.LEAVE,created.view().joined().summary().id(),1,0,null,""),created)));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                    new ModernApplicationProtocol.RoomResult(2,new Command(SCOPE,Action.READY,created.view().joined().summary().id(),1,1,null,""),created)));
        } finally {rooms.disconnect(owner);presence.disconnect(owner);}
    }
}
