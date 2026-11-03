package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernRegenProtocolTest {
    static long event(long revision){return revision==0?0:Math.incrementExact(revision);}
    static ModernCombatProtocol.Frame frame(ModernBattleEngine.Snapshot s) {
        var world=new ModernWorldProtocol.Frame(s.matchId(),s.generation(),Math.incrementExact(s.revision()),ModernWorldProtocol.Phase.valueOf(s.phase().name()),
                ModernWorldProtocol.AbortReason.valueOf(s.abortReason().name()),s.room(),s.manifest(),s.actors(),
                s.result()==null?null:new ModernWorldProtocol.Completion(s.result(),ModernWorldProtocol.SettlementState.PENDING),s.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList());
        var vitals=s.vitals().stream().map(v->new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp(),v.maxMp(),v.ac(),v.weaponType(),v.attackSequence(),
                event(v.attackRevision()),event(v.hurtRevision()),event(v.deathRevision()),v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
        return new ModernCombatProtocol.Frame(world,vitals,s.damages().stream().map(ModernDamageSpellProtocol.State::wireRevision).toList(),
                s.items().stream().map(ModernItemProtocol.State::wireRevision).toList(),s.weights(),s.buffs().stream().map(ModernTimedEffects.State::wireRevision).toList(),
                s.ammo().stream().map(ModernAmmo.State::wireRevision).toList(),s.regen().stream().map(ModernRegenProtocol.State::wireRevision).toList());
    }
    static ModernCombatProtocol.Frame with(ModernCombatProtocol.Frame f,List<ModernRegenProtocol.State> regen) {
        return new ModernCombatProtocol.Frame(f.world(),f.vitals(),f.damages(),f.items(),f.weights(),f.buffs(),f.ammo(),regen);
    }
    static ModernApplicationProtocol.CombatResult message(ModernBattleEngineRegenTest.Fixture f,ModernCombatProtocol.Frame frame) {
        return new ModernApplicationProtocol.CombatResult(1,new ModernCombatProtocol.Request(f.prepared.get(0).scope(),ModernCombatProtocol.Action.READ,f.ticket.matchId(),f.ticket.generation(),0,0),
                new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame));
    }
    @Test void v12RoundTripCarriesBothRegenAndUnchangedItemDamageAndCastHistory()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()){f.active();f.cast(0,1,4);f.elapsed(16_000_000_000L);var frame=frame(f.engine.tick());var m=message(f,frame);
            byte[] bytes=ModernApplicationProtocol.encode(m,12);assertEquals(m,ModernApplicationProtocol.decode(bytes,12));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),12));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,11));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,11));
            assertEquals(2,frame.regen().size());assertEquals(3,frame.regen().get(0).mpGain());}
    }
    @Test void prefixReservedTruncatedAndNegativeCounterBodiesAreRejected()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()){var m=message(f,frame(f.active()));byte[] bytes=ModernApplicationProtocol.encode(m,12);int at=bytes.length-8-2*104;
            for(int offset:new int[]{at+2,at+7,at+8+8}){var changed=bytes.clone();changed[offset]=(byte)0xff;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(changed,12));}
            var partial=java.util.Arrays.copyOf(bytes,bytes.length-1);ByteBuffer.wrap(partial).putInt(4,partial.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(partial,12));}
    }
    @Test void ledgerMustCoverEveryActorInOrderAndProveExactBigIntegerHpMpConservation()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()){var frame=frame(f.active());assertThrows(IllegalArgumentException.class,()->with(frame,List.of(frame.regen().get(0))));
            assertThrows(IllegalArgumentException.class,()->with(frame,List.of(frame.regen().get(1),frame.regen().get(0))));
            var rows=new ArrayList<>(frame.regen());rows.set(0,new ModernRegenProtocol.State(1,1,0,frame.world().revision(),999,1000,0,0,0,0,0,0,0));
            assertThrows(IllegalArgumentException.class,()->with(frame,rows));
            rows.set(0,new ModernRegenProtocol.State(1,0,0,0,0,0,0,0,0,0,0,1,1));assertThrows(IllegalArgumentException.class,()->with(frame,rows));}
    }
    @Test void sameActorCastAndRegenCannotShareARevisionEvenWithZeroNetBatchTotals()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()){f.active();var frame=frame(f.cast(0,1,4).snapshot());var rows=new ArrayList<>(frame.regen());
            rows.set(0,new ModernRegenProtocol.State(1,0,0,0,0,0,1,1,frame.damages().get(0).castRevision(),26,27,0,0));
            assertThrows(IllegalArgumentException.class,()->with(frame,rows));}
    }
    @Test void cumulativeSpellSpendAboveMaximumUsesLedgerRatherThanRelaxingOldFrames()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()){f.active();for(int n=1;n<=11;n++){f.elapsed((n-1)*16_000_000_000L);f.cast(0,n,4);}
            var frame=frame(f.engine.tick());var m=message(f,frame);assertEquals(m,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(m,12),12));
            assertThrows(IllegalArgumentException.class,()->with(frame,List.of()));}
    }
    @Test void staleRegenIsOnlyAConfirmedActiveActionDenialAndCannotMasqueradeAsRead()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()){f.active();f.attack(0,1);f.elapsed(1_999_999_999L);f.engine.tick();f.jump=1;var result=f.attack(0,2);var frame=frame(result.snapshot());
            var request=new ModernCombatProtocol.Request(f.prepared.get(0).scope(),ModernCombatProtocol.Action.ATTACK,f.ticket.matchId(),f.ticket.generation(),2,2);
            var m=new ModernApplicationProtocol.CombatResult(2,request,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.DENIED,ModernCombatProtocol.Reason.STALE_REGEN,frame));
            assertEquals(m,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(m,12),12));
            var wrong=new ModernApplicationProtocol.CombatResult(3,message(f,frame).echo(),m.reply());
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(wrong,12));
            var w=frame.world();long deathRevision=Math.incrementExact(w.revision());
            var deadWorld=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),deathRevision,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts());
            var vitals=new ArrayList<>(frame.vitals());var target=vitals.get(1);
            vitals.set(1,new ModernCombatProtocol.Vital(target.entityId(),0,target.maxHp(),target.mp(),target.maxMp(),target.ac(),target.weaponType(),
                    target.attackSequence(),target.attackRevision(),deathRevision,deathRevision,target.lastTargetEntityId(),target.attackIntervalNanos()));
            var ledger=new ArrayList<>(frame.regen());ledger.set(1,ledger.get(1).damaged(target.hp()));
            var deadFrame=new ModernCombatProtocol.Frame(deadWorld,vitals,frame.damages(),frame.items(),frame.weights(),frame.buffs(),frame.ammo(),ledger);
            var deadTarget=new ModernApplicationProtocol.CombatResult(4,request,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.DENIED,ModernCombatProtocol.Reason.STALE_REGEN,deadFrame));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(deadTarget,12));}
    }
    @Test void terminalAndBotRecordsKeepOnlyTheirProvenHistory()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture(0,4,225,100,0,1,true)){f.active();var frame=frame(f.cast(0,1,4).snapshot());var m=message(f,frame);
            assertEquals(m,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(m,12),12));assertEquals(1,frame.regen().get(1).damageLoss());
            var rows=new ArrayList<>(frame.regen());rows.set(1,new ModernRegenProtocol.State(2,0,0,0,0,0,1,1,frame.vitals().get(1).deathRevision(),29,30,0,1));
            assertThrows(IllegalArgumentException.class,()->with(frame,rows));}
    }
}
