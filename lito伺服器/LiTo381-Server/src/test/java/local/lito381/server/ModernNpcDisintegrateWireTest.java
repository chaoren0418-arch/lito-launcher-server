package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcDisintegrateState.Reason.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Cross-language payload vectors from real engine transactions. Not a TLS,
 * application-frame or client-rendering test; the NPC77 capability stays reserved. */
class ModernNpcDisintegrateWireTest {
    static final int V=ModernNpcDisintegrateWire.VERSION;
    static final ModernNpcRoster.Scope S=new ModernNpcRoster.Scope(11,12,13);
    private static final Map<String,String> GOLDENS=Map.ofEntries(
            Map.entry("huge","00010000000000000000000000000001000000000000000100000000000000090000000000000005000000000000004600000000000001f400000000000001f5000000460000138801000000000000000000000000000002000000000000000100000000000000030000800a0000800a0000800b0000800a002000000000000100000000000000d800000000000000d8001fffffffffff29"),
            Map.entry("critical-death","00010000000000000000000000000001000000000000000100000000000000090000000000000005000000000000004600000000000001f400000000000001f5000000460000138801000300000000000000000000000002000000000000000100000000000000030000800a0000800a0000800b0000800a000000000000000100000000000001b000000000000000010000000000000000"),
            Map.entry("zero","00010000000000000000000000000001000000000000000100000000000000090000000000000005000000000000004600000000000001f400000000000001f5000000460000138801000000000000000000000000000002000000000000000100000000000000030000800a0000800a0000800b0000800a7fffffffffffffff000000000000000000000000000000007fffffffffffffff"),
            Map.entry("quest-sparse","00010000000000000000000000000002000000000000000100000000000000060000000000000001000000000000004600000000000001f400000000000001f500000046000013880101030000000000000000000000000100000000000000020000000000000001000080140000800a000080150000800a000000000000000100000000000001b000000000000000010000000000000000"),
            Map.entry("time-attack-finished","00020000000000000000000000000001000000000000000400000000000000110000000000000004000000000000011800000000000001f400000000000001f80000004600001388010003000000000000000000000000010000000000000004000000000000000100007fca0000800a00007fcb0000800a000000000000000100000000000002a6000000000000000100000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("engine-normal-before","00020000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("engine-normal-hit","00020000000000000000000000000001000000000000000100000000000000050000000000000001000000000000004600000000000001f400000000000001f5000000460000138801000000000000000000000000000001000000000000000100000000000000010000800a0000800a0000800b0000800a00000000000027100000000000000153000000000000015300000000000025bd0000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("engine-normal-second","000200000000000000000000000000010000000000000002000000000000000a0000000000000004000000000000008c00000000000001f400000000000001f6000000460000138801000000000000000000000000000001000000000000000100000000000000010000800a0000800a0000800b0000800a00000000000025ac0000000000000153000000000000015300000000000024590000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("engine-critical-before","00020000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("engine-critical-hit","00020000000000000000000000000001000000000000000100000000000000050000000000000001000000000000004600000000000001f400000000000001f5000000460000138801000200000000000000000000000001000000000000000100000000000000010000800a0000800a0000800b0000800a000000000000271000000000000001b000000000000001b000000000000025600000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("engine-critical-second","000200000000000000000000000000010000000000000002000000000000000a0000000000000004000000000000008c00000000000001f400000000000001f6000000460000138801000200000000000000000000000001000000000000000100000000000000010000800a0000800a0000800b0000800a000000000000255600000000000001b000000000000001b000000000000023a60000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000001f400000000000001f4000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("empty","0000000000000000"),
            Map.entry("initial","000200000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000800000000000000080000000000000000000003b0000138800ff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            Map.entry("sparse","0001000000000000000000000000006400000000000000000000000000000000000000000000000000000000000000007fffffffffffffff7fffffffffffffff000000460000138800ff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
    );
    static byte[] encode(ModernNpcRoster.Scope scope,List<ModernNpcDisintegrateState.State> states) {
        var b=ByteBuffer.allocate(8+states.size()*ModernNpcDisintegrateWire.STATE_BYTES);
        ModernNpcDisintegrateWire.putStates(b,scope,states,Math.max(1,states.size()),V);assertEquals(b.capacity(),b.position());return b.array();
    }
    static byte[] vector(String name,ModernNpcRoster.Scope scope,List<ModernNpcDisintegrateState.State> states) {
        var bytes=encode(scope,states);var b=ByteBuffer.wrap(bytes);
        assertEquals(GOLDENS.get(name),HexFormat.of().formatHex(bytes),name);
        assertEquals(states,ModernNpcDisintegrateWire.getStates(b,scope,Math.max(1,states.size()),V));assertFalse(b.hasRemaining());
        System.out.println("npc77-section:"+name+":"+scope.roomId()+":"+scope.matchId()+":"+scope.generation()+":"+states.size()+":"+HexFormat.of().formatHex(bytes));return bytes;
    }
    static ModernNpcDisintegrateState.State state(long hp,boolean critical) {
        long raw=critical?432:216,applied=Math.min(raw,hp);
        var hit=new ModernNpcDisintegrateState.Hit(new ModernNpcRoster.Id(S,1),3,0,2,new ModernWorldMap.Cell(32778,32778),new ModernWorldMap.Cell(32779,32778),hp,
                new ModernDisintegrate.Outcome(raw,applied,hp-applied,hp==applied,critical));
        return ModernNpcDisintegrateState.State.initial(1,70,500).cast(9,5,hit);
    }
    static boolean follows(ModernNpcRoster.Scope scope,List<ModernNpcDisintegrateState.State> a,long ar,List<ModernNpcDisintegrateState.State> b,long br) {
        return ModernNpcDisintegrateWire.follows(scope,a,ar,scope,b,br);
    }
    @Test void requestUsesExactIndependentNpcAndCasterLivesWithUnsignedEpoch() {
        var request=new ModernNpcDisintegrateState.Request(new ModernRooms.Scope(7,9007199254740993L,-1,3),new ModernNpcRoster.Id(S,23),17,19,29);
        var b=ByteBuffer.allocate(80);ModernNpcDisintegrateWire.putRequest(b,request,V);assertEquals(80,b.position());
        assertEquals("000000070020000000000001ffffffffffffffff03000000000000000000000b000000000000000c000000000000000d000000000000001100000000000000130000000000000017000000000000001d",HexFormat.of().formatHex(b.array()));
        assertEquals(request,ModernNpcDisintegrateWire.getRequest(ByteBuffer.wrap(b.array()),V));
        System.out.println("npc77-request:"+HexFormat.of().formatHex(b.array()));
        assertEquals(9007199254740993L,ByteBuffer.wrap(b.array()).getLong(4));
        for(int i=0;i<80;i++){var bytes=Arrays.copyOf(b.array(),i);assertThrows(RuntimeException.class,()->ModernNpcDisintegrateWire.getRequest(ByteBuffer.wrap(bytes),V));}
        for(int offset:List.of(21,22,23)){var bytes=b.array().clone();bytes[offset]=1;assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.getRequest(ByteBuffer.wrap(bytes),V));}
        for(int offset:List.of(4,12,24,32,40,48,56,64,72)){var bytes=b.array().clone();ByteBuffer.wrap(bytes).putLong(offset,0);assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.getRequest(ByteBuffer.wrap(bytes),V));}
        var joined=ByteBuffer.allocate(81).put(b.array()).put((byte)99);joined.flip();assertEquals(request,ModernNpcDisintegrateWire.getRequest(joined,V));assertEquals(99,joined.get());
    }
    @Test void versionsOrdersCountsAndEncoderScopeAreFailClosed() {
        var states=List.of(state(1000,false));
        for(int version:List.of(0,1,24,27,28,ModernWorldProtocol.CODEC_MAX_VERSION+1,65535)) {
            assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.putStates(ByteBuffer.allocate(200),S,states,1,version));
            assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(encode(S,states)),S,1,version));
        }
        assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.putStates(ByteBuffer.allocate(200).order(ByteOrder.LITTLE_ENDIAN),S,states,1,V));
        assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(encode(S,states)).order(ByteOrder.LITTLE_ENDIAN),S,1,V));
        for(int count:List.of(0,2,101))assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.putStates(ByteBuffer.allocate(200),S,states,count,V));
        var unchanged=ByteBuffer.allocate(200);assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.putStates(unchanged,new ModernNpcRoster.Scope(11,12,14),states,1,V));assertEquals(0,unchanged.position());
        assertThrows(IllegalArgumentException.class,()->encode(S,List.of(states.get(0),states.get(0))));
        assertThrows(IllegalArgumentException.class,()->encode(S,List.of(ModernNpcDisintegrateState.State.initial(101,70,500))));
        assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.putStates(ByteBuffer.allocate(151),S,states,1,V));
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_NPC_DISINTEGRATE,"request and renderer admission are still pending");
    }
    @Test void canonicalInitialBotSparseAndMaximumSectionsHaveFixedSize() {
        vector("empty",S,List.of());
        vector("initial",S,List.of(ModernNpcDisintegrateState.State.initial(1,59,Long.MIN_VALUE),ModernNpcDisintegrateState.State.initial(2,0,0)));
        vector("sparse",S,List.of(ModernNpcDisintegrateState.State.initial(100,70,Long.MAX_VALUE)));
        var states=IntStream.rangeClosed(1,100).mapToObj(i->ModernNpcDisintegrateState.State.initial(i,0,0)).toList();
        var bytes=encode(S,states);assertEquals(14408,bytes.length);assertEquals(states,ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(bytes),S,100,V));
        assertThrows(UnsupportedOperationException.class,()->ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(bytes),S,100,V).clear());
    }
    @Test void allTruncationsReservedBytesAbsentPayloadAndInvalidFieldsAreRejected() {
        var initial=encode(S,List.of(ModernNpcDisintegrateState.State.initial(1,70,500)));var hit=encode(S,List.of(state(1000,false)));
        for(var bytes:List.of(initial,hit))for(int n=0;n<bytes.length;n++){var shortBytes=Arrays.copyOf(bytes,n);assertThrows(RuntimeException.class,()->ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(shortBytes),S,1,V));}
        for(int offset:List.of(2,3,4,5,6,7,75,76,77,78,79)){var bad=hit.clone();bad[offset]=1;assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(bad),S,1,V));}
        for(int offset:List.of(72,74,80,87,88,95,96,103,104,119,120,151)){var bad=initial.clone();bad[offset]=1;assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(bad),S,1,V));}
        for(int offset:List.of(8,16,24,32,40,48,56,64,68,72,73,74,80,88,96,120,128,136,144)) {
            var bad=hit.clone();Arrays.fill(bad,offset,Math.min(bad.length,offset+(offset>=64&&offset<=74?1:8)),(byte)255);
            assertThrows(RuntimeException.class,()->ModernNpcDisintegrateWire.getStates(ByteBuffer.wrap(bad),S,1,V),"offset "+offset);
        }
        var appended=ByteBuffer.allocate(hit.length+4).put(hit).putInt(123);appended.flip();
        assertEquals(List.of(state(1000,false)),ModernNpcDisintegrateWire.getStates(appended,S,1,V));assertEquals(123,appended.getInt());
    }
    @Test void exactI64CriticalDeathAndZeroDamageAreDistinctCanonicalReceipts() {
        vector("huge",S,List.of(state(9007199254740993L,false)));
        vector("critical-death",S,List.of(state(1,true)));
        var old=state(Long.MAX_VALUE,false);var h=old.hit();
        var zero=new ModernNpcDisintegrateState.Hit(h.target(),h.targetLife(),h.lane(),h.casterLife(),h.from(),h.to(),Long.MAX_VALUE,new ModernDisintegrate.Outcome(0,0,Long.MAX_VALUE,false,false));
        vector("zero",S,List.of(ModernNpcDisintegrateState.State.initial(1,70,500).cast(9,5,zero)));
    }
    @Test void actualEngineHitDenialMixedSpellAndRecastProduceMonotonicSections()throws Exception {
        for(boolean critical:List.of(false,true))try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();String prefix=critical?"engine-critical":"engine-normal";
            vector(prefix+"-before",f.scope,before.world().npcDisintegrations());f.randomValue=critical?0:99;
            var first=ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,npc);assertEquals(NONE,first.reason());var after=first.frame();
            vector(prefix+"-hit",f.scope,after.world().npcDisintegrations());
            assertTrue(follows(f.scope,before.world().npcDisintegrations(),before.world().revision(),after.world().npcDisintegrations(),after.world().revision()));
            assertEquals(TOO_FAST,ModernBattleEngineNpcDisintegrateTest.cast(f,0,2,npc).reason());var denied=f.view();
            assertEquals(after.world().npcDisintegrations(),denied.world().npcDisintegrations());
            assertTrue(follows(f.scope,after.world().npcDisintegrations(),after.world().revision(),denied.world().npcDisintegrations(),denied.world().revision()));
            f.elapsed(1200);assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,3,npc).reason());
            f.elapsed(5000);assertEquals(NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,4,npc).reason());var next=f.view();
            vector(prefix+"-second",f.scope,next.world().npcDisintegrations());
            assertTrue(follows(f.scope,after.world().npcDisintegrations(),after.world().revision(),next.world().npcDisintegrations(),next.world().revision()));
            assertEquals(140,next.world().npcDisintegrations().get(0).mpSpent());assertEquals(3,next.world().npcSpells().get(0).mpSpent());
        }
    }
    @Test void monotonicLedgerRejectsRetargetedHistoryCountRollbackSkippedSequenceAndScopeChanges() {
        var initial=List.of(ModernNpcDisintegrateState.State.initial(1,70,500));var a=state(1000,false);var b=a.cast(12,8,a.hit());
        assertTrue(follows(S,initial,1,List.of(a),9));assertTrue(follows(S,List.of(a),9,List.of(b),12));
        assertFalse(follows(S,List.of(b),12,List.of(a),13));assertFalse(follows(S,List.of(a),9,List.of(state(1000,true)),10));
        assertFalse(follows(S,List.of(a),9,List.of(b),11));assertFalse(follows(S,initial,9,List.of(a),9));
        assertFalse(ModernNpcDisintegrateWire.follows(S,List.of(a),9,new ModernNpcRoster.Scope(11,12,14),List.of(b),12));
        assertFalse(follows(S,initial,1,List.of(ModernNpcDisintegrateState.State.initial(1,69,500)),2));
        assertFalse(follows(S,initial,1,List.of(ModernNpcDisintegrateState.State.initial(1,70,501)),2));
        assertTrue(follows(S,List.of(a),9,List.of(a.cast(12,6,a.hit()).cast(13,7,a.hit())),13),"two legitimate skipped casts are allowed");
        var shortBudget=new ModernNpcDisintegrateState.State(1,3,13,6,210,70,500,503,a.hit());
        assertFalse(follows(S,List.of(a),9,List.of(shortBudget),13));
        assertFalse(follows(S,List.of(a),9,List.of(),10));
    }
    @Test void actualQuestProjectionAndTimeAttackTerminalDoNotInventPlayerTargets()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture(true,100,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500))) {
            f.active();var npc=ModernQuestEngineTest.living(f,1);assertEquals(NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,1,1,npc).reason());
            var w=f.view().world();var sparse=w.npcDisintegrations().stream().filter(s->s.entityId()==2).toList();
            assertEquals(1,sparse.get(0).hit().lane());vector("quest-sparse",f.scope,sparse);
            assertEquals(0,w.npcDisintegrations().get(0).castCount());
        }
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,true,1,true)) {
            f.active();f.tower(0,1);
            for(int i=0;i<f.plan.waves().size();i++){f.elapsed(1000+5000L*i);assertEquals(NONE,ModernNpcDisintegrateTimeAttackTest.cast(f,0,i+1,f.living()).reason());}
            var end=f.view();assertEquals(ModernBattleEngine.Phase.FINISHED,end.world().phase());
            vector("time-attack-finished",end.npcs().scope(),end.world().npcDisintegrations());
        }
    }
}
