package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import java.util.Objects;

/** Service revision and persistence state, independent of legacy combat DTOs.
 * The engine result is immutable; a saved receipt never changes its outcome. */
record ModernBattleLifecycle(long revision,Phase phase,AbortReason abortReason,
        ModernMatchSettlement.Event result,SettlementState settlement,
        ModernPetRaceProtocol.Snapshot race,boolean timeAttackFinished,boolean siegeFinished) {
    ModernBattleLifecycle {
        Objects.requireNonNull(phase);Objects.requireNonNull(abortReason);Objects.requireNonNull(race);
        if(revision<1||(phase==Phase.ABORTED)!=(abortReason!=AbortReason.NONE)
                ||(result==null)!=(settlement==null)
                ||(phase==Phase.FINISHED)!=(result!=null||race.complete()||timeAttackFinished||siegeFinished)
                ||result!=null&&result.engineRevision()>=revision)throw new IllegalArgumentException("invalid battle service lifecycle");
    }
    static ModernBattleLifecycle preparing(Frame frame) {
        if(frame.phase()!=Phase.PREPARING)throw new IllegalArgumentException("expected preparing frame");
        return new ModernBattleLifecycle(frame.revision(),frame.phase(),frame.abortReason(),null,null,frame.race(),false,false);
    }
    static ModernBattleLifecycle from(ModernBattlePublication publication) {
        var c=publication.capture();var m=c.modes();
        var result=m.siegeResult()!=null?m.siegeResult():m.timeAttackResult()!=null?m.timeAttackResult():c.result();
        var race=m.race()==null?ModernPetRaceProtocol.Snapshot.disabled():ModernPetRaceProtocol.fromState(m.race(),m.raceResultId());
        return new ModernBattleLifecycle(Math.incrementExact(c.revision()),Phase.valueOf(c.phase().name()),
                AbortReason.valueOf(c.abortReason().name()),result,result==null?null:SettlementState.PENDING,race,
                m.timeAttack().finished(),m.siege().finished());
    }
    /** Explicit full-VIP publication; never a conversion through legacy DTOs. */
    static ModernBattleLifecycle fromVip(ModernBattleVipWire.Payload payload) {
        var w=Objects.requireNonNull(payload).view().world();
        return new ModernBattleLifecycle(Math.incrementExact(w.revision()),Phase.valueOf(w.phase().name()),
                AbortReason.valueOf(w.abortReason().name()),w.result(),w.result()==null?null:SettlementState.PENDING,
                ModernPetRaceProtocol.Snapshot.disabled(),false,false);
    }
    ModernBattleLifecycle terminal(ModernBattlePublication authority,long previousRevision,AbortReason reason) {
        long next=Math.max(Math.incrementExact(previousRevision),revision);
        if(authority!=null&&(result!=null||race.complete()||authority.capture().modes().timeAttack().enabled()
                ||authority.capture().modes().siege().enabled()))
            return new ModernBattleLifecycle(next,phase,abortReason,result,settlement,race,timeAttackFinished,siegeFinished);
        return new ModernBattleLifecycle(next,Phase.ABORTED,reason,null,null,
                ModernPetRaceProtocol.aborted(race,next),false,false);
    }
    ModernBattleLifecycle settled(SettlementState state) {
        Objects.requireNonNull(state);
        if(state==SettlementState.PENDING||result==null||settlement!=SettlementState.PENDING)return this;
        return new ModernBattleLifecycle(Math.incrementExact(revision),phase,abortReason,result,state,race,timeAttackFinished,siegeFinished);
    }
    ModernBattleLifecycle raceSettled(ModernPetRaceProtocol.Settlement state) {
        if(state!=ModernPetRaceProtocol.Settlement.SAVED&&state!=ModernPetRaceProtocol.Settlement.FAILED
                ||!race.complete()||race.settlement()!=ModernPetRaceProtocol.Settlement.PENDING)return this;
        return new ModernBattleLifecycle(Math.incrementExact(race.terminalRevision()),phase,abortReason,result,settlement,
                race.withSettlement(state),timeAttackFinished,siegeFinished);
    }
    boolean resultOutstanding(){return result!=null||race.complete()||timeAttackFinished||siegeFinished;}
    @Override public String toString(){return "BattleServiceLifecycle[redacted]";}
}
