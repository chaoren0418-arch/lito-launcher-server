package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ModernNpcFireStormWireTest {
    static final int V=ModernNpcFireStormWire.VERSION;
    static final ModernNpcRoster.Scope S=new ModernNpcRoster.Scope(11,12,13);
    static final ModernWorldMap.Cell FROM=new ModernWorldMap.Cell(32778,32778),TO=new ModernWorldMap.Cell(32779,32778);
    static ModernFireStorm.Outcome outcome(long before,long raw){long applied=Math.min(before,raw);return new ModernFireStorm.Outcome(raw,applied,before-applied,before==applied);}
    static ModernNpcFireStormState.Target target(long serial,long hp){return new ModernNpcFireStormState.Target(new ModernNpcRoster.Id(S,serial),1,0,TO,hp,outcome(hp,35));}
    static ModernNpcFireStormStructures.Receipt structure(int kind,long before) {
        var cell=new ModernWorldMap.Cell(32778,32779);ModernNpcFireStormStructures.Hit hit;
        if(kind==4)hit=new ModernNpcFireStormStructures.TowerHit(new ModernTimeAttack.TowerId(S),cell,before,before,outcome(before,35));
        else hit=new ModernNpcFireStormStructures.QuestHit(new ModernQuestObjectives.Id(S,kind==1?1:4),ModernQuestObjectives.Kind.values()[kind-1],cell,before,before,3,outcome(before,35));
        return new ModernNpcFireStormStructures.Receipt(1,9,5,1,FROM,hit);
    }
    static ModernNpcFireStormState.State state(List<ModernNpcFireStormState.Target> hits,ModernNpcFireStormStructures.Receipt receipt) {
        return new ModernNpcFireStormState.State(1,1,9,5,48,48,1,S,FROM,0,hits,receipt);
    }
    static ModernNpcFireStormState.State next(ModernNpcFireStormState.State a,List<ModernNpcFireStormState.Target> hits,ModernNpcFireStormStructures.Receipt receipt) {
        return new ModernNpcFireStormState.State(a.entityId(),2,12,8,96,48,1,S,FROM,0,hits,receipt);
    }
    static byte[] encode(ModernNpcRoster.Scope scope,List<ModernNpcFireStormState.State> states) {
        var b=ByteBuffer.allocate(ModernNpcFireStormWire.bytes(scope,states,Math.max(1,states.size())));
        ModernNpcFireStormWire.putStates(b,scope,states,Math.max(1,states.size()),V);assertEquals(b.capacity(),b.position());return b.array();
    }
    static void roundtrip(ModernNpcRoster.Scope scope,List<ModernNpcFireStormState.State> states) {
        var b=ByteBuffer.wrap(encode(scope,states));assertEquals(states,ModernNpcFireStormWire.getStates(b,scope,Math.max(1,states.size()),V));assertFalse(b.hasRemaining());
    }
    static boolean follows(ModernNpcFireStormState.State a,ModernNpcFireStormState.State b) {
        return ModernNpcFireStormWire.follows(S,List.of(a),9,S,List.of(b),12);
    }
    @Test void selectorOnlyRequestHasExact64BytesAndOpaqueEpoch() {
        var r=new ModernNpcFireStormState.Request(new ModernRooms.Scope(7,9007199254740993L,-1,3),S,17,19);
        var b=ByteBuffer.allocate(64);ModernNpcFireStormWire.putRequest(b,r,V);
        assertEquals("000000070020000000000001ffffffffffffffff03000000000000000000000b000000000000000c000000000000000d00000000000000110000000000000013",HexFormat.of().formatHex(b.array()));
        assertEquals(r,ModernNpcFireStormWire.getRequest(ByteBuffer.wrap(b.array()),V));
        for(int n=0;n<64;n++){var shortBytes=ByteBuffer.wrap(Arrays.copyOf(b.array(),n));assertThrows(RuntimeException.class,()->ModernNpcFireStormWire.getRequest(shortBytes,V));assertEquals(0,shortBytes.position());}
        for(int at:List.of(21,22,23)){var bytes=b.array().clone();bytes[at]=1;assertThrows(IllegalArgumentException.class,()->ModernNpcFireStormWire.getRequest(ByteBuffer.wrap(bytes),V));}
        for(int at:List.of(4,12,24,32,40,48,56)){var bytes=b.array().clone();ByteBuffer.wrap(bytes).putLong(at,0);assertThrows(IllegalArgumentException.class,()->ModernNpcFireStormWire.getRequest(ByteBuffer.wrap(bytes),V));}
        var joined=ByteBuffer.allocate(65).put(b.array()).put((byte)99);joined.flip();assertEquals(r,ModernNpcFireStormWire.getRequest(joined,V));assertEquals(99,joined.get());
    }
    @Test void supportedFireStormVersionsDoNotUnconditionallyEnableRuntime() {
        assertEquals(31,V);
        assertEquals(ModernSessionProtocol.MAX_VERSION,ModernWorldProtocol.CODEC_MAX_VERSION);
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_NPC_FIRE_STORM);
        var s=List.of(state(List.of(),null));var bytes=encode(S,s);
        for(int v=V;v<=ModernWorldProtocol.CODEC_MAX_VERSION;v++) {
            var b=ByteBuffer.allocate(bytes.length);ModernNpcFireStormWire.putStates(b,S,s,1,v);
            assertArrayEquals(bytes,b.array());
            var input=ByteBuffer.wrap(bytes);assertEquals(s,ModernNpcFireStormWire.getStates(input,S,1,v));assertFalse(input.hasRemaining());
        }
        for(int v:List.of(0,1,29,30,ModernSessionProtocol.MAX_VERSION+1,65535))assertThrows(IllegalArgumentException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(bytes),S,1,v));
        assertThrows(IllegalArgumentException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(encode(S,s)).order(ByteOrder.LITTLE_ENDIAN),S,1,V));
    }
    @Test void emptyInitialBotSparseAndHundredActorSectionsAreCanonical() {
        assertEquals("0000000000000000",HexFormat.of().formatHex(encode(S,List.of())));
        roundtrip(S,List.of());roundtrip(S,List.of(ModernNpcFireStormState.State.initial(1,39),ModernNpcFireStormState.State.initial(2,0)));
        roundtrip(S,List.of(ModernNpcFireStormState.State.initial(100,48)));
        var many=IntStream.rangeClosed(1,100).mapToObj(i->ModernNpcFireStormState.State.initial(i,0)).toList();assertEquals(7208,encode(S,many).length);roundtrip(S,many);
        assertThrows(UnsupportedOperationException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(encode(S,many)),S,100,V).clear());
    }
    @Test void npcEmptyZeroPartialDeathAndI64DamageRoundtrip() {
        roundtrip(S,List.of(state(List.of(),null)));
        for(long hp:List.of(1L,100L,9007199254740993L,Long.MAX_VALUE))roundtrip(S,List.of(state(List.of(target(1,hp)),null)));
        var zero=new ModernNpcFireStormState.Target(new ModernNpcRoster.Id(S,1),1,0,TO,Long.MAX_VALUE,outcome(Long.MAX_VALUE,0));roundtrip(S,List.of(state(List.of(zero),null)));
    }
    @Test void allStructureKindsAndHistoricalReceiptsRoundtrip() {
        for(int kind=1;kind<=4;kind++)for(long hp:List.of(1L,9007199254740993L)) {
            var a=state(List.of(target(1,100)),structure(kind,hp));assertEquals(248,encode(S,List.of(a)).length);roundtrip(S,List.of(a));
            var b=next(a,List.of(target(1,65)),a.structure());roundtrip(S,List.of(b));assertTrue(follows(a,b));
        }
    }
    @Test void maximumBoundedTargetsWithHistoricalStructureHaveExplicitSectionSize() {
        var hits=IntStream.rangeClosed(1,48).mapToObj(i->target(i,100)).toList();var a=state(List.of(),structure(4,1));var b=next(a,hits,a.structure());
        assertEquals(8+72+48*64+104,encode(S,List.of(b)).length);roundtrip(S,List.of(b));
        assertThrows(IllegalArgumentException.class,()->state(hits,a.structure()));
        var many=IntStream.rangeClosed(1,100).mapToObj(i->new ModernNpcFireStormState.State(i,b.castCount(),b.revision(),b.lastSequence(),b.mpSpent(),b.mpCost(),b.casterLife(),b.scope(),b.origin(),b.lane(),b.targets(),b.structure())).toList();
        assertEquals(324808,ModernNpcFireStormWire.MAX_SECTION_BYTES);assertEquals(ModernNpcFireStormWire.MAX_SECTION_BYTES,encode(S,many).length);roundtrip(S,many);
        assertTrue(ModernNpcFireStormWire.MAX_SECTION_BYTES>ModernNpcProtocol.MAX_FRAME_BYTES,"next frame version must handle this bound, not truncate");
    }
    @Test void rejectedWritesLeaveBufferUntouchedForScopeCountAndCapacity() {
        var states=List.of(state(List.of(target(1,100)),structure(4,1)));var b=ByteBuffer.allocate(1000);
        assertThrows(IllegalArgumentException.class,()->ModernNpcFireStormWire.putStates(b,new ModernNpcRoster.Scope(11,12,14),states,1,V));assertEquals(0,b.position());
        for(int count:List.of(0,2,101))assertThrows(IllegalArgumentException.class,()->ModernNpcFireStormWire.putStates(b,S,states,count,V));assertEquals(0,b.position());
        var small=ByteBuffer.allocate(247);assertThrows(IllegalArgumentException.class,()->ModernNpcFireStormWire.putStates(small,S,states,1,V));assertEquals(0,small.position());
        assertThrows(IllegalArgumentException.class,()->encode(S,List.of(states.get(0),states.get(0))));
        assertThrows(IllegalArgumentException.class,()->encode(S,List.of(ModernNpcFireStormState.State.initial(101,0))));
    }
    @Test void truncationsReservedBytesFlagsAndLengthsFailWithoutConsumingInput() {
        var good=encode(S,List.of(state(List.of(target(1,100)),structure(4,1))));
        for(int n=0;n<good.length;n++){var b=ByteBuffer.wrap(Arrays.copyOf(good,n));assertThrows(RuntimeException.class,()->ModernNpcFireStormWire.getStates(b,S,1,V));assertEquals(0,b.position());}
        for(int at:List.of(2,7,75,76,77,78,79,137,143,187,191)){var b=good.clone();b[at]=1;assertThrows(RuntimeException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(b),S,1,V),"reserved "+at);}
        for(int at:List.of(74,136,186)){var b=good.clone();b[at]=2;assertThrows(RuntimeException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(b),S,1,V));}
        for(int at:List.of(73,184,185)){var b=good.clone();b[at]=(byte)255;assertThrows(RuntimeException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(b),S,1,V));}
        var tail=ByteBuffer.allocate(good.length+8).put(good).putLong(123);tail.flip();ModernNpcFireStormWire.getStates(tail,S,1,V);assertEquals(123,tail.getLong());
    }
    @Test void initialNoncanonicalPayloadAndArithmeticOverflowRejected() {
        var bytes=encode(S,List.of(ModernNpcFireStormState.State.initial(1,48)));
        for(int at:List.of(24,32,40,56,64,68,72)){var b=bytes.clone();b[at]=1;assertThrows(RuntimeException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(b),S,1,V),"initial "+at);}
        var good=encode(S,List.of(state(List.of(target(1,100)),null)));ByteBuffer.wrap(good).putLong(16,Long.MAX_VALUE).putLong(32,Long.MAX_VALUE);
        assertThrows(RuntimeException.class,()->ModernNpcFireStormWire.getStates(ByteBuffer.wrap(good),S,1,V));
    }
    @Test void followsRejectsLostInventedRewrittenOrReplayedStructureProof() {
        var a=state(List.of(target(1,100)),structure(4,1));var b=next(a,List.of(target(1,65)),a.structure());assertTrue(follows(a,b));
        assertFalse(follows(a,next(a,List.of(),null)));
        assertFalse(follows(state(List.of(),null),b),"cannot introduce an old proof absent in prior snapshot");
        assertFalse(follows(a,next(a,List.of(),structure(3,1))),"old receipt cannot change type");
        assertFalse(follows(a,state(List.of(target(1,99)),a.structure())),"same count cannot rewrite victims");
        assertFalse(ModernNpcFireStormWire.follows(S,List.of(a),9,S,List.of(b),11));
        assertFalse(ModernNpcFireStormWire.follows(S,List.of(a),9,new ModernNpcRoster.Scope(11,12,14),List.of(b),12));
    }
    @Test void followsRejectsNpcResurrectionAndAcceptsNewLife() {
        var a=state(List.of(target(1,1)),null);assertFalse(follows(a,next(a,List.of(target(1,1)),null)));
        var h=target(1,100);var born=new ModernNpcFireStormState.Target(h.id(),2,0,TO,100,h.outcome());assertTrue(follows(a,next(a,List.of(born),null)));
    }
    @Test void followsAllowsLaterStructureHitButNotPostMortemRetargetOrHealing() {
        var a=state(List.of(),structure(4,100));var old=(ModernNpcFireStormStructures.TowerHit)a.structure().hit();
        var hit=new ModernNpcFireStormStructures.TowerHit(old.id(),old.cell(),100,65,outcome(65,35));var e=new ModernNpcFireStormStructures.Receipt(2,12,8,1,FROM,hit);
        assertTrue(follows(a,next(a,List.of(),e)));
        var healed=new ModernNpcFireStormStructures.TowerHit(old.id(),old.cell(),100,66,outcome(66,35));assertFalse(follows(a,next(a,List.of(),new ModernNpcFireStormStructures.Receipt(2,12,8,1,FROM,healed))));
        assertFalse(follows(state(List.of(),structure(4,1)),next(a,List.of(),e)));
    }
    @Test void actualQuestEngineStagesProduceTypedCanonicalSections()throws Exception {
        for(int map:List.of(513,554))try(var f=ModernNpcFireStormStructuresTest.quest(map,1)) {
            f.active();var previous=f.view().world();long seq=0,elapsed=0;roundtrip(f.scope,previous.npcFireStorms());
            while(f.view().world().phase()==ModernBattleEngine.Phase.ACTIVE) {
                f.elapsed(elapsed);elapsed+=2000;var result=ModernNpcFireStormStructuresTest.cast(f,0,++seq);assertEquals(ModernNpcFireStormState.Reason.NONE,result.reason());
                var w=result.frame().world();roundtrip(f.scope,w.npcFireStorms());assertTrue(ModernNpcFireStormWire.follows(f.scope,previous.npcFireStorms(),previous.revision(),f.scope,w.npcFireStorms(),w.revision()));previous=w;
                assertTrue(seq<=8);
            }
            assertEquals(ModernBattleEngine.Phase.FINISHED,previous.phase());assertTrue(previous.npcFireStorms().get(0).structure().hit().outcome().death());
        }
    }
    @Test void retainedStructureHasItsOwnSequenceAndRevisionBudgets() {
        var a=new ModernNpcFireStormState.State(1,1,9,100,48,48,1,S,FROM,0,List.of(),null);
        var hit=structure(4,100).hit();
        for(boolean sequence:List.of(false,true)) {
            var proof=new ModernNpcFireStormStructures.Receipt(3,sequence?11:10,sequence?101:102,1,FROM,hit);
            var b=new ModernNpcFireStormState.State(1,4,20,103,192,48,1,S,FROM,0,List.of(),proof);
            assertFalse(ModernNpcFireStormWire.follows(S,List.of(a),9,S,List.of(b),20));
        }
        assertThrows(IllegalArgumentException.class,()->new ModernNpcFireStormState.State(1,5,11,9,240,48,1,S,FROM,0,List.of(),structure(4,100)));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcFireStormState.State(1,5,12,6,240,48,1,S,FROM,0,List.of(),structure(4,100)));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcFireStormStructures.Receipt(10,9,10,1,FROM,hit));
    }
    @Test void publicationRevisionProjectsCurrentAndRetainedProofWithoutChangingCountsOrDamage() {
        var initial=ModernNpcFireStormState.State.initial(1,48);assertSame(initial,initial.wireRevision());
        for(int kind=1;kind<=4;kind++) {
            var a=state(List.of(target(1,100)),structure(kind,1));var b=next(a,List.of(target(1,65)),a.structure());
            for(var before:List.of(a,b)) {
                var after=before.wireRevision();assertEquals(before.revision()+1,after.revision());assertEquals(before.structure().revision()+1,after.structure().revision());
                assertEquals(before.castCount(),after.castCount());assertEquals(before.lastSequence(),after.lastSequence());assertEquals(before.mpSpent(),after.mpSpent());assertEquals(before.targets(),after.targets());
                assertEquals(before.structure().hit().outcome(),after.structure().hit().outcome());
                if(before.structure().hit() instanceof ModernNpcFireStormStructures.QuestHit q)assertEquals(q.bornRevision()+1,((ModernNpcFireStormStructures.QuestHit)after.structure().hit()).bornRevision());
                roundtrip(S,List.of(after));
            }
            assertTrue(ModernNpcFireStormWire.follows(S,List.of(a.wireRevision()),10,S,List.of(b.wireRevision()),13));
        }
        var huge=new ModernNpcFireStormState.State(1,1,Long.MAX_VALUE,5,48,48,1,S,FROM,0,List.of(),null);
        assertThrows(ArithmeticException.class,huge::wireRevision);
    }
    @Test void actualTimeAttackTowerProofSurvivesFourWavesAndTerminalEncoding()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
            f.active();var previous=f.view().world();var scope=f.view().npcs().scope();roundtrip(scope,previous.npcFireStorms());
            for(int i=0;i<5;i++) {
                f.elapsed(i*2000L);var result=ModernNpcFireStormStructuresTest.cast(f,0,i+1);assertEquals(ModernNpcFireStormState.Reason.NONE,result.reason());
                var w=result.frame().world();roundtrip(scope,w.npcFireStorms());assertTrue(ModernNpcFireStormWire.follows(scope,previous.npcFireStorms(),previous.revision(),scope,w.npcFireStorms(),w.revision()));previous=w;
                assertEquals(1,w.npcFireStorms().get(0).structure().castCount());
            }
            assertEquals(ModernBattleEngine.Phase.FINISHED,previous.phase());
        }
    }
}
