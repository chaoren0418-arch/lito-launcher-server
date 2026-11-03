package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Source-only regression additions for the v7 typed participant boundary. */
class ModernBotProtocolRegressionTest {
    private static final ModernRooms.Scope SCOPE = new ModernRooms.Scope(7,3333,-1,0);
    private static final String LEGACY_JOINED_HEX =
            "0000000000000005000000000000000600020100000002020001520002000201000000"
            + "000000070000000000000d0500020000"
            + "000000070000000000000d050000000000000050000100000100000000014e000153"
            + "0000002a0000000000000d050000000000000050000100010100000000014e000153";

    @Test void humanJoinedBytesStayLiteralExactForVersionsOneThroughSix() throws Exception {
        var room = room(false,7);
        byte[] expected = HexFormat.of().parseHex(LEGACY_JOINED_HEX);
        for (int version=1;version<=6;version++) {
            byte[] bytes = joined(room,version);
            assertArrayEquals(expected,bytes,"version " + version);
            var input = ByteBuffer.wrap(bytes);
            assertEquals(room,ModernRoomProtocol.getJoined(input,version));
            assertFalse(input.hasRemaining());
        }
        byte[] tagged = joined(room,7);
        assertEquals(expected.length+8,tagged.length);
        // Summary35 + human owner12 + count/reserved4; each old member is34 bytes.
        assertArrayEquals(expected,withoutTags(tagged,51,89));
    }

    @Test void typedMemberTagsRejectUnknownKindsReservedAndHumanAliases() throws Exception {
        var room = room(true,7);
        byte[] bytes = joined(room,7);
        assertEquals(0,bytes[51]); assertEquals(1,bytes[89]);
        assertEquals(room,ModernRoomProtocol.getJoined(ByteBuffer.wrap(bytes),7));
        assertThrows(IllegalStateException.class,room.members().get(1)::key);
        for (int offset : new int[]{51,52,53,54,89,90,91,92}) {
            byte[] bad = bytes.clone(); bad[offset] = (byte)(offset==51 || offset==89 ? 2 : 1);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,
                    ()->ModernRoomProtocol.getJoined(ByteBuffer.wrap(bad),7),"offset " + offset);
        }
        for (int version=1;version<=6;version++) {
            final int v = version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->joined(room,v));
        }
        var onlyBot = room.members().get(1);
        assertThrows(IllegalArgumentException.class,()->new ModernRooms.RoomSnapshot(
                new ModernRooms.Summary(5,6,room.summary().config(),1,1,false),
                room.owner(),List.of(onlyBot)));
    }

    @Test void gmReasonElevenIsLegalOnlyInsideVersionSevenGmResult() throws Exception {
        var denied = new ModernRooms.Result(ModernRooms.Status.DENIED,
                ModernRooms.Reason.GM_NOT_AUTHORIZED,view(room(true,7)));
        var request = new ModernGmCommandProtocol.Request(SCOPE,5,6,2);
        var result = new ModernApplicationProtocol.GmResult(0xfedcba98,request,
                ModernGmCommandProtocol.Reply.result(new ModernRooms.BotResult(2,0,0,denied)));
        assertEquals(result,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(result,7),7));
        for (int version=1;version<=6;version++) {
            final int v = version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(result,v));
        }
        var ordinary = new ModernApplicationProtocol.RoomResult(1,echo(SCOPE),denied);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(ordinary,7));
        var okay = new ModernApplicationProtocol.RoomResult(1,echo(SCOPE),
                new ModernRooms.Result(ModernRooms.Status.OK,ModernRooms.Reason.NONE,denied.view()));
        byte[] forged = ModernApplicationProtocol.encode(okay,7);
        forged[56] = (byte)ModernRooms.Status.DENIED.ordinal();
        forged[57] = (byte)ModernRooms.Reason.GM_NOT_AUTHORIZED.ordinal();
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(forged,7));
    }

    @Test void botWithMatchingNumbersCannotSatisfyHumanRoomOrWorldScope() {
        var room = room(true,42); // HUMAN(42,3333), BOT(7,3333), no HUMAN(7,3333).
        var rooms = new ModernApplicationProtocol.RoomResult(1,echo(SCOPE),
                new ModernRooms.Result(ModernRooms.Status.OK,ModernRooms.Reason.NONE,view(room)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(rooms,7));
        var world = world(room,List.of(),null);
        var result = new ModernApplicationProtocol.WorldResult(1,read(SCOPE),new ModernWorldProtocol.Reply(
                ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,world));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(result,7));
    }

    @Test void sameNumericHumanAndBotCanBePhysicalTargetButSameEntityCannot() throws Exception {
        var world = world(room(true,7),List.of(),null);
        var frame = new ModernCombatProtocol.Frame(world,List.of(
                vital(1,100,100,20,20,1,10,0,0,2),vital(2,25,40,0,0,0,0,10,0,0)));
        var request = new ModernCombatProtocol.Request(SCOPE,ModernCombatProtocol.Action.ATTACK,9,6,1,2);
        var result = new ModernApplicationProtocol.CombatResult(1,request,new ModernCombatProtocol.Reply(
                ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame));
        assertEquals(result,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(result,7),7));
        var self = new ModernCombatProtocol.Request(SCOPE,ModernCombatProtocol.Action.ATTACK,9,6,1,1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                new ModernApplicationProtocol.CombatResult(1,self,result.reply()),7));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(result,6));
    }

    @Test void targetedMagicResolvesHumanSenderSeparatelyFromSameNumericBot() throws Exception {
        var casts = List.of(new ModernSpellProtocol.CastState(1,1,0,0,0,0,0,0,0,0,1000),
                ModernSpellProtocol.CastState.initial(2,1000));
        var world = world(room(true,7),casts,null);
        var damages = List.of(new ModernDamageSpellProtocol.State(1,1,10,3,4,2,32724,32784,32725,32784,3),
                ModernDamageSpellProtocol.State.initial(2,3));
        var frame = new ModernCombatProtocol.Frame(world,List.of(
                vital(1,100,100,17,20,0,0,0,0,0),vital(2,27,40,0,0,0,0,10,0,0)),damages);
        var request = new ModernSpellProtocol.Request(SCOPE,9,6,1,4,2);
        var result = new ModernApplicationProtocol.CastResult(1,request,new ModernSpellProtocol.Reply(
                ModernSpellProtocol.Status.OK,ModernSpellProtocol.Reason.NONE,frame));
        assertEquals(result,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(result,7),7));
        var wrongScope = new ModernSpellProtocol.Request(new ModernRooms.Scope(42,3333,-1,0),9,6,1,4,2);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                new ModernApplicationProtocol.CastResult(1,wrongScope,result.reply()),7));
    }

    @Test void completionRetainsBothTypedParticipantsAndRejectsUnknownOrReservedTags() throws Exception {
        var room = room(true,7);
        var event = new ModernMatchSettlement.Event(new UUID(1,2),9,6,9,1,514,1000,List.of(
                new ModernMatchSettlement.Participant(1,room.members().get(0).identity(),0,ModernMatchSettlement.Verdict.WIN),
                new ModernMatchSettlement.Participant(2,room.members().get(1).identity(),1,ModernMatchSettlement.Verdict.LOSS)));
        var completion = new ModernWorldProtocol.Completion(event,ModernWorldProtocol.SettlementState.PENDING);
        var frame = world(room,List.of(),completion);
        var out = ByteBuffer.allocate(4096); ModernWorldProtocol.putFrame(out,frame,7);
        byte[] bytes = Arrays.copyOf(out.array(),out.position());
        assertEquals(frame,ModernWorldProtocol.getFrame(ByteBuffer.wrap(bytes),7));
        // Frame prefix28, joined, manifest40+one sector40, actors4+2*36,
        // completion presence4 + event prefix60; participants each tag4+legacy24.
        int first = 28 + joined(room,7).length + 40 + 40 + 4 + 2*36 + 4 + 60;
        assertEquals(0,bytes[first]); assertEquals(1,bytes[first+28]);
        for (int relative : new int[]{0,1,2,3,28,29,30,31}) {
            byte[] bad = bytes.clone(); bad[first+relative] = (byte)(relative%28==0 ? 2 : 1);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,
                    ()->ModernWorldProtocol.getFrame(ByteBuffer.wrap(bad),7));
        }
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                ()->ModernWorldProtocol.putFrame(ByteBuffer.allocate(4096),frame,6));
    }

    private static ModernRooms.RoomSnapshot room(boolean bot,int humanSource) {
        var key = new TournamentPresence.CharacterKey(humanSource,3333);
        var members = List.of(new ModernRooms.Member(key,"N","S",80,1,0,0,true),
                new ModernRooms.Member(bot?ModernParticipantId.bot(7,3333):ModernParticipantId.human(42,3333),
                        "N","S",80,1,0,1,true));
        return new ModernRooms.RoomSnapshot(new ModernRooms.Summary(5,6,
                new ModernRooms.Config("R",2,1,514,0),2,2,true),key,members);
    }
    private static ModernRooms.View view(ModernRooms.RoomSnapshot room) {
        return new ModernRooms.View(10,List.of(room.summary()),room,ModernRoomProtocolTest.empty().options());
    }
    private static ModernRoomProtocol.Echo echo(ModernRooms.Scope scope) {
        return new ModernRoomProtocol.Echo(scope,ModernRooms.Action.READ,0,0,0);
    }
    private static ModernWorldProtocol.Request read(ModernRooms.Scope scope) {
        return new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.READ,0,0,0,9,6,0,0,0,
                ModernWorldProtocol.ZERO_DIGEST);
    }
    private static byte[] joined(ModernRooms.RoomSnapshot room,int version) throws Exception {
        var out = ByteBuffer.allocate(4096); ModernRoomProtocol.putJoined(out,room,version);
        return Arrays.copyOf(out.array(),out.position());
    }
    private static byte[] withoutTags(byte[] source,int first,int second) {
        byte[] result = new byte[source.length-8]; int to = 0;
        for (int i=0;i<source.length;i++) if (!(i>=first && i<first+4 || i>=second && i<second+4)) result[to++] = source[i];
        assertEquals(result.length,to); return result;
    }
    private static ModernWorldProtocol.Frame world(ModernRooms.RoomSnapshot room,
                                                   List<ModernSpellProtocol.CastState> casts,
                                                   ModernWorldProtocol.Completion completion) {
        var actors = List.of(new ModernBattleEngine.Actor(1,room.members().get(0),
                        new ModernWorldMap.Cell(32724,32784),2,true,0,50_000_000),
                new ModernBattleEngine.Actor(2,room.members().get(1),
                        new ModernWorldMap.Cell(32725,32784),6,true,0,50_000_000));
        return new ModernWorldProtocol.Frame(9,6,10,completion==null?ModernWorldProtocol.Phase.ACTIVE:ModernWorldProtocol.Phase.FINISHED,
                ModernWorldProtocol.AbortReason.NONE,room,ModernWorldProtocolTest.frame().manifest(),actors,completion,casts);
    }
    private static ModernCombatProtocol.Vital vital(long id,long hp,long maximumHp,long mp,long maximumMp,
                                                    long sequence,long attack,long hurt,long death,long target) {
        return new ModernCombatProtocol.Vital(id,hp,maximumHp,mp,maximumMp,10,0,sequence,attack,hurt,death,target,1_000_000_000);
    }
}
