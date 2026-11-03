package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernTimedCombatProtocolTest {
    static long revision(long r){return r==0?0:Math.addExact(r,1);}
    static ModernCombatProtocol.Frame wire(ModernBattleEngine.Snapshot s) {
        var world=new ModernWorldProtocol.Frame(s.matchId(),s.generation(),s.revision()+1,ModernWorldProtocol.Phase.valueOf(s.phase().name()),
                ModernWorldProtocol.AbortReason.valueOf(s.abortReason().name()),s.room(),s.manifest(),s.actors(),
                s.result()==null?null:new ModernWorldProtocol.Completion(s.result(),ModernWorldProtocol.SettlementState.PENDING),
                s.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList());
        var vitals=s.vitals().stream().map(v->new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp(),v.maxMp(),v.ac(),v.weaponType(),
                v.attackSequence(),revision(v.attackRevision()),revision(v.hurtRevision()),revision(v.deathRevision()),v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
        return new ModernCombatProtocol.Frame(world,vitals,s.damages().stream().map(ModernDamageSpellProtocol.State::wireRevision).toList(),
                s.items().stream().map(ModernItemProtocol.State::wireRevision).toList(),s.weights(),s.buffs().stream().map(ModernTimedEffects.State::wireRevision).toList());
    }
    @Test void completeTimedItemReceiptRoundTripsOnlyInVersionTen()throws Exception {
        try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            f.active();var after=f.use(0,40013).snapshot();var p=f.players.get(0);
            var request=new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40013);
            var message=new ModernApplicationProtocol.ItemResult(7,request,new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,wire(after)));
            byte[] bytes=ModernApplicationProtocol.encode(message,10);assertEquals(message,ModernApplicationProtocol.decode(bytes,10));
            assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),10));
            for(int version:new int[]{8,9})assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,version));
            for(int offset=bytes.length-55;offset<bytes.length;offset++) {
                byte[] cut=Arrays.copyOf(bytes,offset);ByteBuffer.wrap(cut).putInt(4,cut.length);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,10));
            }
        }
    }
    @Test void disabledTimedAppendixIsEightZerosAfterTheByteExactV9Body()throws Exception {
        try(var f=new ModernBattleEngineWeightTest.Fixture(0,true)) {
            f.active();var p=f.prepared.get(0);var frame=wire(f.engine.tick());
            var message=new ModernApplicationProtocol.CombatResult(3,new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.READ,f.ticket.matchId(),f.ticket.generation(),0,0),
                    new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame));
            byte[] old=ModernApplicationProtocol.encode(message,9),next=ModernApplicationProtocol.encode(message,10);
            assertEquals(old.length+8,next.length);assertArrayEquals(Arrays.copyOfRange(old,8,old.length),Arrays.copyOfRange(next,8,old.length));
            assertArrayEquals(new byte[8],Arrays.copyOfRange(next,old.length,next.length));assertEquals(message,ModernApplicationProtocol.decode(next,10));
        }
    }
    @Test void timedStockHpMutationAndHealingTupleCannotBeLaunderedThroughNewStateShape() {
        var stock=List.of(new ModernItemProtocol.Stock(40013,1,0));
        assertThrows(IllegalArgumentException.class,()->new ModernItemProtocol.State(1,1,1,1,40013,1,99,100,191,0,1,1,0,stock));
        assertThrows(IllegalArgumentException.class,()->new ModernItemProtocol.State(1,1,1,1,40013,0,100,100,191,500,1,1,0,stock));
        assertThrows(IllegalArgumentException.class,()->new ModernItemProtocol.State(1,1,1,1,40013,0,0,0,191,0,1,1,0,stock));
        assertThrows(IllegalArgumentException.class,()->new ModernItemProtocol.State(1,1,1,1,40010,0,100,100,191,0,1,1,0,List.of(new ModernItemProtocol.Stock(40010,1,0))));
    }
    @Test void classRestrictedIsOnlyAConfirmedLivingIneligibleTimedUseWithOwnedStock()throws Exception {
        try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            f.active();var after=f.use(0,40068).snapshot();var p=f.players.get(0);var frame=wire(after);
            var valid=new ModernApplicationProtocol.ItemResult(1,new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40068),
                    new ModernItemProtocol.Reply(ModernItemProtocol.Status.DENIED,ModernItemProtocol.Reason.CLASS_RESTRICTED,frame));
            assertEquals(valid,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(valid,10),10));
            for(var request:List.of(new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.READ,f.ticket.matchId(),f.ticket.generation(),0,0),
                    new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40013),
                    new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40010)))
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.ItemResult(1,request,valid.reply()),10));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(valid,9));
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(frame.world(),frame.vitals(),frame.damages(),frame.items(),List.of(),frame.buffs()));
        }
    }
    @Test void lateEffectDenialRetainsExactActionSequenceAndCannotBeSentToOldPeers()throws Exception {
        for(int action=0;action<3;action++)try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            f.active();f.use(0,40013);f.elapsed(300*ModernBattleEngineTimedTest.SECOND-1);f.jumpOnRandom=1;
            var p=f.players.get(0);ModernApplicationProtocol.Message message;
            if(action==0){var result=f.attack(0,1);message=new ModernApplicationProtocol.CombatResult(1,
                    new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.ATTACK,f.ticket.matchId(),f.ticket.generation(),1,2),
                    new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.DENIED,ModernCombatProtocol.Reason.STALE_EFFECT,wire(result.snapshot())));}
            else {int skill=action==1?5:4;var result=f.cast(0,1,skill);message=new ModernApplicationProtocol.CastResult(1,
                    new ModernSpellProtocol.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),1,skill,skill==5?0:2),
                    new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.STALE_EFFECT,wire(result.snapshot())));}
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,10),10));
            final var checked=message;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(checked,9));
        }
    }
    @Test void headerBoundUsesNegotiatedVersionBeforeAllocatingOrReadingAnyBody()throws Exception {
        for(int kind:new int[]{23,27,35}) {
            byte[] header=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(131073).putInt(1).putShort((short)kind).putShort((short)0).array();
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),9));
            assertThrows(EOFException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),10));
            ByteBuffer.wrap(header).putInt(4,262145);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),10));
        }
        byte[] ordinary=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(65537).putInt(1).putShort((short)7).putShort((short)0).array();
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(ordinary),10));
    }
}
