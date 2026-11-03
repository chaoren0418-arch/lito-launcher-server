package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernItemProtocol.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernItemProtocolTest {
    static final int ID=0xfedcba98;
    static final Request REQUEST=new Request(new ModernRooms.Scope(7,3333,-1,2),Action.USE,0x100000002L,0x200000003L,4,40010);
    static final String BODY="000000070000000000000d05ffffffffffffffff02010000"+
            "0000000100000002000000020000000300000000000000040000000000009c4a";
    static final String REQUEST_HEX="4c544d3100000048fedcba9800220000"+BODY;
    static final String UNAVAILABLE_HEX="4c544d310000004cfedcba9800230000"+BODY+"03010000";
    private static byte[] hex(String value){return HexFormat.of().parseHex(value);}
    private static void reject(byte[] value){assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(value,8));}

    @Test void independentBigEndianRequestAndUnavailableGoldenHaveExactWidths() throws Exception {
        assertEquals(56,hex(BODY).length);assertEquals(72,hex(REQUEST_HEX).length);
        var messages=List.<ModernApplicationProtocol.Message>of(new ModernApplicationProtocol.ItemRequest(ID,REQUEST),
                new ModernApplicationProtocol.ItemResult(ID,REQUEST,new Reply(Status.UNAVAILABLE,Reason.NOT_ACTIVE,null)));
        for(int i=0;i<messages.size();i++) {
            byte[] bytes=hex(i==0?REQUEST_HEX:UNAVAILABLE_HEX);
            assertArrayEquals(bytes,ModernApplicationProtocol.encode(messages.get(i),8));
            assertEquals(messages.get(i),ModernApplicationProtocol.decode(bytes,8));
        }
        for(int v=1;v<=7;v++){final int old=v;for(var message:messages)assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,old));}
    }
    @Test void malformedScopesReservedEnumsEveryTruncationAndTailAreRejected() {
        for(String value:List.of(REQUEST_HEX,UNAVAILABLE_HEX)) {
            var valid=hex(value);
            for(int length=0;length<valid.length;length++) {
                byte[] bad=Arrays.copyOf(valid,length);if(length>=8)ByteBuffer.wrap(bad).putInt(4,length);reject(bad);
            }
            var tail=Arrays.copyOf(valid,valid.length+1);ByteBuffer.wrap(tail).putInt(4,tail.length);reject(tail);
            for(int offset:new int[]{14,15,38,39}){var bad=valid.clone();bad[offset]=1;reject(bad);}
        }
        for(int offset:new int[]{20,28,40,48,56,64}){var bad=hex(REQUEST_HEX);ByteBuffer.wrap(bad).putLong(offset,0);reject(bad);}
        var bad=hex(REQUEST_HEX);bad[36]=4;reject(bad);bad=hex(REQUEST_HEX);bad[37]=2;reject(bad);
        bad=hex(UNAVAILABLE_HEX);bad[72]=0;reject(bad);bad=hex(UNAVAILABLE_HEX);bad[75]=1;reject(bad);
    }
    @Test void stockConservationSourceNamespaceAndInitialHistoryAreStrict() {
        for(long id:new long[]{1,3333,40009,40015,Long.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->new Stock(id,1,1));
        for(long initial:new long[]{0,-1,1_000_001})assertThrows(IllegalArgumentException.class,()->new Stock(40010,initial,0));
        assertThrows(IllegalArgumentException.class,()->new Stock(40010,1,2));
        var initial=State.initial(1,3_000_000_000L,List.of(new Stock(40010,2,2)));assertEquals(0,initial.useCount());
        assertThrows(IllegalArgumentException.class,()->State.initial(1,0,List.of(new Stock(40010,2,1))));
        assertThrows(IllegalArgumentException.class,()->State.initial(1,0,List.of(new Stock(40011,1,1),new Stock(40010,1,1))));
        assertThrows(IllegalArgumentException.class,()->State.initial(1,-1,List.of()));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,1,1,40010,20,0,20,189,500,10,10,0,List.of(new Stock(40010,2,1))));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,1,1,40010,21,10,31,189,500,10,10,0,List.of(new Stock(40010,2,1))));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,1,1,40010,20,10,30,194,500,10,10,0,List.of(new Stock(40010,2,1))));
    }
    @Test void fullCombatFrameCarriesHealingAndInventoryTogetherAndCannotBeSentToOldPeer() throws Exception {
        try(var f=new ModernBattleEngineItemTest.Fixture(0,false)) {
            f.active();f.hurt(1);var source=f.use(1,40010).snapshot();var frame=frame(source);
            var p=f.room.players.get(0);var request=new Request(p.scope(),Action.USE,source.matchId(),source.generation(),1,40010);
            var message=new ModernApplicationProtocol.ItemResult(77,request,new Reply(Status.OK,Reason.NONE,frame));
            byte[] encoded=ModernApplicationProtocol.encode(message,8);assertEquals(message,ModernApplicationProtocol.decode(encoded,8));
            assertEquals(source.revision()+1,frame.items().get(0).useRevision());assertEquals(source.items().get(0).stocks(),frame.items().get(0).stocks());
            var combat=new ModernApplicationProtocol.CombatResult(78,new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.READ,
                    source.matchId(),source.generation(),0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame));
            assertEquals(combat,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(combat,8),8));
            for(int v=1;v<=7;v++){final int old=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(combat,old));}
            var wrongRequest=new Request(p.scope(),Action.USE,source.matchId(),source.generation(),1,40011);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                    new ModernApplicationProtocol.ItemResult(79,wrongRequest,new Reply(Status.OK,Reason.NONE,frame)),8));
            for(int length=0;length<encoded.length;length++) {
                byte[] bad=Arrays.copyOf(encoded,length);if(length>=8)ByteBuffer.wrap(bad).putInt(4,length);reject(bad);
            }
        }
    }
    @Test void compactItemAppendixHasIndependentLiteralAndRejectsCountTailInsideBody() throws Exception {
        var state=new State(1,2,1,7,40010,10,50,60,189,500,32724,32784,3_000_000_000L,List.of(new Stock(40010,2,1)));
        var buffer=ByteBuffer.allocate(4+STATE_PREFIX_BYTES+STOCK_BYTES);putStates(buffer,List.of(state));
        assertEquals(120,buffer.position());buffer.flip();assertEquals(List.of(state),getStates(buffer,1));assertFalse(buffer.hasRemaining());
        byte[] encoded=buffer.array();assertEquals(3_000_000_000L,ByteBuffer.wrap(encoded).getLong(84));
        assertEquals(1,ByteBuffer.wrap(encoded).getShort(92));assertEquals(40010,ByteBuffer.wrap(encoded).getLong(96));
        for(int offset:new int[]{2,3,94,95}){var bad=encoded.clone();bad[offset]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->getStates(ByteBuffer.wrap(bad),1));}
        var bad=encoded.clone();ByteBuffer.wrap(bad).putShort(92,(short)4);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->getStates(ByteBuffer.wrap(bad),1));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->getStates(ByteBuffer.wrap(encoded),2));
    }
    @Test void confirmedUseCannotInventSubminimumHealingOrADifferentCurrentOrigin() throws Exception {
        try(var f=new ModernBattleEngineItemTest.Fixture(0,false)) {
            f.active();var frame=frame(f.use(1,40010).snapshot());var original=frame.items().get(0);
            // Real full-HP consumption remains valid with zero restored HP.
            assertEquals(0,original.restored());assertEquals(100,original.hpAfter());
            var first=frame.vitals().get(0);
            for(long restored:new long[]{0,9}) {
                long after=10+restored;
                var state=new State(original.entityId(),original.requestSequence(),original.useCount(),original.useRevision(),
                        original.itemId(),restored,10,after,original.effect(),original.delayMillis(),original.x(),original.y(),0,original.stocks());
                var vital=new ModernCombatProtocol.Vital(first.entityId(),after,first.maxHp(),first.mp(),first.maxMp(),first.ac(),first.weaponType(),
                        first.attackSequence(),first.attackRevision(),first.hurtRevision(),first.deathRevision(),first.lastTargetEntityId(),first.attackIntervalNanos());
                assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(frame.world(),
                        List.of(vital,frame.vitals().get(1)),frame.damages(),List.of(state,frame.items().get(1))));
            }
            var other=frame.world().actors().get(1).position();
            var moved=new State(original.entityId(),original.requestSequence(),original.useCount(),original.useRevision(),original.itemId(),
                    original.restored(),original.hpBefore(),original.hpAfter(),original.effect(),original.delayMillis(),other.x(),other.y(),0,original.stocks());
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(frame.world(),frame.vitals(),frame.damages(),
                    List.of(moved,frame.items().get(1))));
        }
    }
    @Test void laterWorldRevisionCannotInventHealingAfterLastConfirmedUse() throws Exception {
        try(var f=new ModernBattleEngineItemTest.Fixture(0,false)) {
            f.active();var base=frame(f.use(1,40010).snapshot());var w=base.world();var old=base.items().get(0);var vital=base.vitals().get(0);
            var later=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+1,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts());
            var used=new State(old.entityId(),old.requestSequence(),old.useCount(),old.useRevision(),40010,10,30,40,189,500,old.x(),old.y(),0,old.stocks());
            for(long hp:new long[]{40,50,35}) {
                var current=new ModernCombatProtocol.Vital(vital.entityId(),hp,vital.maxHp(),vital.mp(),vital.maxMp(),vital.ac(),vital.weaponType(),
                        vital.attackSequence(),vital.attackRevision(),vital.hurtRevision(),vital.deathRevision(),vital.lastTargetEntityId(),vital.attackIntervalNanos());
                org.junit.jupiter.api.function.Executable build=()->new ModernCombatProtocol.Frame(later,List.of(current,base.vitals().get(1)),base.damages(),List.of(used,base.items().get(1)));
                if(hp==40)assertDoesNotThrow(build);else assertThrows(IllegalArgumentException.class,build);
            }
        }
    }
    static ModernCombatProtocol.Frame frame(ModernBattleEngine.Snapshot source) {
        var world=new ModernWorldProtocol.Frame(source.matchId(),source.generation(),source.revision()+1,
                ModernWorldProtocol.Phase.valueOf(source.phase().name()),ModernWorldProtocol.AbortReason.valueOf(source.abortReason().name()),
                source.room(),source.manifest(),source.actors(),source.result()==null?null:new ModernWorldProtocol.Completion(source.result(),ModernWorldProtocol.SettlementState.PENDING),
                source.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList());
        var vitals=source.vitals().stream().map(v->new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp(),v.maxMp(),v.ac(),v.weaponType(),
                v.attackSequence(),revision(v.attackRevision()),revision(v.hurtRevision()),revision(v.deathRevision()),v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
        return new ModernCombatProtocol.Frame(world,vitals,source.damages().stream().map(ModernDamageSpellProtocol.State::wireRevision).toList(),
                source.items().stream().map(State::wireRevision).toList());
    }
    private static long revision(long value){return value==0?0:value+1;}
}
