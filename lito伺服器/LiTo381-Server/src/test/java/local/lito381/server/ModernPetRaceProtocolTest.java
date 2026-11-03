package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModernPetRaceProtocolTest {
    static ModernApplicationProtocol.WorldResult message(ModernWorldProtocol.Frame frame,ModernRooms.Scope scope){
        return new ModernApplicationProtocol.WorldResult(7,new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.READ,0,0,0,frame.matchId(),frame.generation(),0,0,0,ModernWorldProtocol.ZERO_DIGEST),
                new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,frame));
    }
    static void roundtrip(ModernWorldProtocol.Frame frame,ModernRooms.Scope scope)throws Exception {
        var message=message(frame,scope);var bytes=ModernApplicationProtocol.encode(message,16);
        assertEquals(message,ModernApplicationProtocol.decode(bytes,16));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),16));
        for(int version=1;version<=15;version++){int v=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,v));}
    }
    @Test void loadingActiveWindowCompleteAndSavedHaveAnIndependentWorldReceipt()throws Exception{
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()){roundtrip(f.frame(),f.a.scope());f.active();roundtrip(f.frame(),f.a.scope());var first=f.finish(0);roundtrip(f.frame(first),f.a.scope());
            f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var pending=f.frame();roundtrip(pending,f.a.scope());assertNull(pending.completion());
            var saved=new ModernWorldProtocol.Frame(pending.matchId(),pending.generation(),pending.revision()+1,pending.phase(),pending.abortReason(),pending.room(),pending.manifest(),pending.actors(),null,List.of(),pending.descriptor(),pending.race().withSettlement(ModernPetRaceProtocol.Settlement.SAVED));
            roundtrip(saved,f.a.scope());assertEquals(pending.race().contentSha256(),saved.race().contentSha256());
            assertThrows(IllegalArgumentException.class,()->new ModernWorldProtocol.Frame(pending.matchId(),pending.generation(),pending.revision()+2,pending.phase(),pending.abortReason(),pending.room(),pending.manifest(),pending.actors(),null,List.of(),pending.descriptor(),saved.race()));
        }
    }
    @Test void everyRaceTruncationReservedFieldAndUnknownEnumFailsClosed()throws Exception{
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()){
            var r=f.frame().race();var b=ByteBuffer.allocate(112+r.gates().size()*16+r.runners().size()*72);ModernPetRaceProtocol.put(b,r);byte[] good=b.array();
            for(int length=0;length<good.length;length++){var cut=Arrays.copyOf(good,length);assertThrows(RuntimeException.class,()->ModernPetRaceProtocol.get(ByteBuffer.wrap(cut)));}
            for(int offset:new int[]{0,1,2,3,4,5,6,7}){var bad=good.clone();bad[offset]=(byte)255;assertThrows(RuntimeException.class,()->ModernPetRaceProtocol.get(ByteBuffer.wrap(bad)),"offset="+offset);}
            var mixed=good.clone();ByteBuffer.wrap(mixed).putLong(good.length-8,938);assertThrows(IllegalArgumentException.class,()->ModernPetRaceProtocol.get(ByteBuffer.wrap(mixed)));
            var disabled=ByteBuffer.allocate(8);ModernPetRaceProtocol.put(disabled,ModernPetRaceProtocol.Snapshot.disabled());assertArrayEquals(new byte[8],disabled.array());
        }
    }
    @Test void capRequiresWorldAndDescriptorAndCannotLeakToVersionFifteenOrNoRacePolicy()throws Exception{
        int cap=ModernSessionProtocol.CAP_PET_RACE;assertEquals(131072,cap);
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(cap|3,0));
        var policy=ModernSessionBoundary.Policy.world(false,false,false,false,false,false,false,false,false,true);assertEquals(196975,policy.supportedCapabilities());
        for(int version=1;version<=16;version++){var boundary=new ModernSessionBoundary(policy.withMaxVersion(version));
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,16,262143,3,1))));
            assertEquals(version,ack.version());assertEquals(version==16?cap:0,ack.capabilities()&cap);}
        assertEquals(0,ModernSessionBoundary.Policy.world(false).supportedCapabilities()&cap);
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()){
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(message(f.frame(),f.a.scope()),new ModernSessionBoundary.Negotiated(1,15,1,131071)));
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(f.frame(),List.of()));
        }
    }
    @Test void sceneVersionOneThroughFifteenKeepsItsBytesAndSixteenOnlyAddsDisabledEightBytes()throws Exception{
        var old=ModernWorldProtocolTest.frame();var message=message(old,ModernWorldProtocolTest.read().scope());
        byte[] v15=ModernApplicationProtocol.encode(message,15),v16=ModernApplicationProtocol.encode(message,16);
        assertEquals(v15.length+8,v16.length);assertArrayEquals(new byte[8],Arrays.copyOfRange(v16,v16.length-8,v16.length));
        assertEquals(message,ModernApplicationProtocol.decode(v16,16));
        for(int v=1;v<=15;v++)assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,v),v));
    }
    @Test void independentBigEndianCourseAndResultCanonicalAgreeWithTheRustLiteralFixture(){
        var key=new ModernWorldBinding.Key(0,565);var manifest=new ModernWorldMap.Manifest(514,List.of(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(32767,32768),137784,"01".repeat(32))),"02".repeat(32));
        var gates=List.of(new ModernPetRace.Gate(32726,32727,32784,32784),new ModernPetRace.Gate(32728,32729,32784,32784));
        var literal=ByteBuffer.allocate(4096);utf(literal,"NEW_MATCH_PET_RACE_V1");literal.putInt(0).putInt(565).putInt(514).put(HexFormat.of().parseHex("02".repeat(32))).putInt(1)
                .putInt(32767).putInt(32768).putInt(137784).put(HexFormat.of().parseHex("01".repeat(32))).putLong(2145).putLong(200_000_000).putInt(4).putLong(10_000_000_000L).putInt(2)
                .putInt(32726).putInt(32727).putInt(32784).putInt(32784).putInt(32728).putInt(32729).putInt(32784).putInt(32784);
        String course=ModernPetRaceProtocol.digest(Arrays.copyOf(literal.array(),literal.position()));assertEquals(course,ModernPetRace.courseHash(key,manifest,gates,2145,200_000_000));
        var rows=List.of(new ModernPetRaceProtocol.ResultRow(new TournamentPresence.CharacterKey(7,3333),new ModernPetRace.Runner(1,11,24,4,0,1,1_000_000_000,ModernPetRace.Dnf.NONE,2145)),
                new ModernPetRaceProtocol.ResultRow(new TournamentPresence.CharacterKey(42,3333),new ModernPetRace.Runner(2,20,0,0,0,0,0,ModernPetRace.Dnf.WINDOW_CLOSED,2145)));
        UUID id=UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");literal.clear();utf(literal,"LITO_MODERN_PET_RACE_RESULT_V1");literal.put(HexFormat.of().parseHex("00112233445566778899aabbccddeeff"))
                .putLong(9).putLong(3).putLong(20).putInt(0).putInt(565).putInt(514).put(HexFormat.of().parseHex("02".repeat(32))).put(HexFormat.of().parseHex(course)).putLong(1).putLong(11_000_000_000L).putInt(2)
                .putLong(1).putInt(7).putLong(3333).putLong(11).putLong(24).putLong(4).putLong(0).putLong(1).putLong(1_000_000_000).putLong(0).putLong(2145)
                .putLong(2).putInt(42).putLong(3333).putLong(20).putLong(0).putLong(0).putLong(0).putLong(0).putLong(0).putLong(3).putLong(2145);
        byte[] bytes=Arrays.copyOf(literal.array(),literal.position());assertArrayEquals(bytes,ModernPetRaceProtocol.resultBody(id,9,3,20,key,manifest,course,1,11_000_000_000L,rows));
        var event=new ModernPetRaceSettlement.Event(id,9,3,20,565,manifest,200_000_000,gates,course,1,11_000_000_000L,rows,ModernPetRaceProtocol.digest(bytes),1000);
        assertTrue(event.sameAuthority(new ModernPetRaceSettlement.Event(id,9,3,20,565,manifest,200_000_000,gates,course,1,11_000_000_000L,rows,event.contentSha256(),9999)));
    }
    @Test void rehashedLateFinisherCannotEnterWorldDecodeOrDurableEvent()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()){
            f.active();f.finish(0);var window=f.finish(1);f.elapsed(window.race().finishWindowDeadlineElapsedNanos());var world=f.frame();var event=ModernPetRaceSettlement.event(world,1000);
            var rows=new java.util.ArrayList<>(event.rows());var old=rows.get(1);var r=old.runner();long late=event.finishWindowDeadlineElapsedNanos()+1;
            rows.set(1,new ModernPetRaceProtocol.ResultRow(old.source(),new ModernPetRace.Runner(r.entityId(),r.revision(),r.progressMoveSequence(),r.completedLaps(),r.nextCheckpoint(),r.placement(),late,r.dnf(),r.baseGraphicsId())));
            String hash=ModernPetRaceProtocol.resultHash(event.resultId(),event.matchId(),event.generation(),event.terminalRevision(),ModernPetRaceTest.KEY,event.manifest(),event.courseSha256(),event.firstFinisherEntity(),event.finishWindowDeadlineElapsedNanos(),rows);
            assertThrows(IllegalArgumentException.class,()->new ModernPetRaceSettlement.Event(event.resultId(),event.matchId(),event.generation(),event.terminalRevision(),event.logicalMapId(),event.manifest(),event.stepIntervalNanos(),event.gates(),event.courseSha256(),event.firstFinisherEntity(),event.finishWindowDeadlineElapsedNanos(),rows,hash,1000));
            byte[] bytes=ModernApplicationProtocol.encode(message(world,f.a.scope()),16);int start=bytes.length-(112+world.race().gates().size()*16+world.race().runners().size()*72);
            ByteBuffer.wrap(bytes).putLong(start+112+world.race().gates().size()*16+72+48,late);System.arraycopy(HexFormat.of().parseHex(hash),0,bytes,start+80,32);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,16));
        }
    }
    @Test void rehashedGatesOutsideManifestCannotEnterFrameOrEventButBoundaryIntersectionIsValid()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()){
            var outside=List.of(new ModernPetRace.Gate(0,0,0,0),new ModernPetRace.Gate(1,1,1,1));var world=f.frame();var race=world.race();
            String course=ModernPetRace.courseHash(ModernPetRaceTest.KEY,world.manifest(),outside,2145,ModernBattleEnginePetRaceTest.STEP);
            var forged=new ModernPetRaceProtocol.Snapshot(true,race.phase(),race.settlement(),course,0,0,0,race.resultId(),race.contentSha256(),outside,race.runners());
            assertThrows(IllegalArgumentException.class,()->new ModernWorldProtocol.Frame(world.matchId(),world.generation(),world.revision(),world.phase(),world.abortReason(),world.room(),world.manifest(),world.actors(),null,List.of(),world.descriptor(),forged));
            f.active();var first=f.finish(0);f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var event=ModernPetRaceSettlement.event(f.frame(),1000);
            String hash=ModernPetRaceProtocol.resultHash(event.resultId(),event.matchId(),event.generation(),event.terminalRevision(),ModernPetRaceTest.KEY,event.manifest(),course,event.firstFinisherEntity(),event.finishWindowDeadlineElapsedNanos(),event.rows());
            assertThrows(IllegalArgumentException.class,()->new ModernPetRaceSettlement.Event(event.resultId(),event.matchId(),event.generation(),event.terminalRevision(),event.logicalMapId(),event.manifest(),event.stepIntervalNanos(),outside,course,event.firstFinisherEntity(),event.finishWindowDeadlineElapsedNanos(),event.rows(),hash,1000));
            var origin=world.manifest().sectors().get(0).key().origin();assertDoesNotThrow(()->ModernPetRaceProtocol.validateGatesManifest(world.manifest(),List.of(new ModernPetRace.Gate(origin.x()-1,origin.x(),origin.y(),origin.y()))));
            assertThrows(IllegalArgumentException.class,()->ModernPetRaceProtocol.validateGatesManifest(world.manifest(),List.of(new ModernPetRace.Gate(origin.x()-1,origin.x()-1,origin.y(),origin.y()))));
        }
    }
    @Test void maximumTenHumanSixtyFourGateWorldRoundtripsAndAdjacentCountsReject()throws Exception {
        var fixture=new ModernRoomsTest.Fixture();
        var players=new java.util.ArrayList<ModernRoomsTest.Player>();
        ModernRooms.FrozenPreparation ticket=null;
        try {
            for(int i=0;i<10;i++)players.add(fixture.player(i<5?7:42,3333+i%5,80,0));
            var owner=players.get(0);
            ModernRoomsTest.accepted(fixture.create(owner,ModernRoomsTest.draft(10,0,565,0)));
            for(var player:players.subList(1,10))ModernRoomsTest.accepted(fixture.join(player,fixture.view(owner).joined().summary(),""));
            for(var player:players)ModernRoomsTest.accepted(fixture.act(player,ModernRooms.Action.READY,1));
            var summary=fixture.view(owner).joined().summary();
            var capture=fixture.rooms.prepareWorld(owner.owner(),owner.scope(),summary.id(),summary.revision());
            ModernRoomsTest.accepted(capture.result());ticket=capture.ticket();
            var map=ModernPetRaceTest.map();var gates=new java.util.ArrayList<ModernPetRace.Gate>();
            for(int i=0;i<64;i++)gates.add(ModernPetRaceTest.gate(ModernPetRaceTest.cell(i,12)));
            var actors=new java.util.ArrayList<ModernBattleEngine.Actor>();
            var runners=new java.util.ArrayList<ModernPetRace.Runner>();
            for(int i=0;i<10;i++) {
                actors.add(new ModernBattleEngine.Actor(i+1,ticket.snapshot().members().get(i),ModernPetRaceTest.cell(10+i,10),0,false,0,1_000_000));
                runners.add(new ModernPetRace.Runner(i+1,0,0,0,0,0,0,ModernPetRace.Dnf.NONE,2145));
            }
            String course=ModernPetRace.courseHash(ModernPetRaceTest.KEY,map.manifest(),gates,2145,1_000_000);
            var race=new ModernPetRaceProtocol.Snapshot(true,ModernPetRace.Phase.WAITING,ModernPetRaceProtocol.Settlement.NONE,course,
                    0,0,0,ModernPetRaceProtocol.ZERO_ID,ModernPetRaceProtocol.ZERO_HASH,gates,runners);
            var descriptor=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PET_RACE,1024,2000,2000,0,30_000);
            var frame=new ModernWorldProtocol.Frame(ticket.matchId(),ticket.generation(),1,ModernWorldProtocol.Phase.LOADING,
                    ModernWorldProtocol.AbortReason.NONE,ticket.snapshot(),map.manifest(),actors,null,List.of(),descriptor,race);
            roundtrip(frame,owner.scope());
            var bytes=ModernApplicationProtocol.encode(message(frame,owner.scope()),16);
            int start=bytes.length-(112+64*16+10*72);assertTrue(start>0);assertTrue(bytes.length<ModernWorldProtocol.MAX_FRAME_BYTES);
            var tooManyGates=bytes.clone();ByteBuffer.wrap(tooManyGates).putShort(start+4,(short)65);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(tooManyGates,16));
            var tooManyRunners=bytes.clone();tooManyRunners[start+6]=11;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(tooManyRunners,16));
        }finally{
            if(ticket!=null)fixture.rooms.releasePreparation(ticket);
            for(var player:players){fixture.rooms.disconnect(player.owner());fixture.presence.disconnect(player.owner());}
        }
    }
    private static void utf(ByteBuffer b,String text){byte[] raw=text.getBytes(StandardCharsets.US_ASCII);b.putShort((short)raw.length).put(raw);}
}
