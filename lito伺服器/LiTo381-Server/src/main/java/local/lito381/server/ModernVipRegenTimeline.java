package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Private VIP deathmatch timeline. Source query-start clock pairs locate lease
 * boundaries on the monotonic battle clock; they are never re-anchored at a
 * delayed poll. A forward wall observation may shorten, never extend a lease.
 * Builds a complete immutable candidate, without publishing or saving gear. */
final class ModernVipRegenTimeline {
    record State(ModernBattleLoadout loadout,ModernEquipmentHistory.State history,
            ModernRegenProtocol.State regen,ModernNaturalRegen.Clock clock,
            ModernInventoryWeight.Snapshot weight,List<ModernItemProtocol.Stock> stocks,
            List<ModernAmmo.Stock> ammo,List<ModernSpellMaterials.Stock> materials) {
        State {
            Objects.requireNonNull(loadout);Objects.requireNonNull(history);Objects.requireNonNull(regen);
            Objects.requireNonNull(clock);Objects.requireNonNull(weight);stocks=List.copyOf(stocks);
            ammo=List.copyOf(ammo);materials=List.copyOf(materials);
            if(loadout.fighter()==null||loadout.fighter().vip()==null||loadout.regen()==null
                    ||!loadout.historyProfile(history.spells().keySet(),history.items()!=null).equals(history.profile())
                    ||regen.entityId()!=history.identity().actor().entityId()
                    ||!weight.equals(loadout.reweight(stocks,ammo,weight.units(),materials)))throw invalid();
            if(history.healing()!=null&&(history.healing().outgoing()==null
                    ||history.healing().outgoing().spending().current().cost()!=ModernPlayerHealing.qualify(loadout.fighter()).mpCost()))throw invalid();
        }
    }
    static State advance(State before,long activeStart,long nowNanos,long wallMillis,boolean active,
            boolean suppressMp,ModernPhysicalCombat.BoundedRandom random) {
        long elapsed=active?nowNanos-activeStart:-1;
        if(wallMillis<0||active&&(elapsed<0||elapsed>ModernNaturalRegen.MAX_ELAPSED_NANOS))throw invalid();
        var state=before;long effective=effectiveEpoch(before.loadout(),nowNanos,wallMillis);
        if(effective<before.loadout().combatant().evaluatedAtMillis())throw invalid();
        int changes=0;
        while(true) {
            long deadline=deadline(state.loadout());
            if(deadline<0||deadline>effective)break;
            if(++changes>ModernCharacterData.MAX_ITEMS+1)throw invalid();
            long boundary=active?Math.max(0,Math.min(elapsed,boundary(state.loadout(),deadline,activeStart))):0;
            if(active&&boundary>0)state=regenerate(state,boundary-1,suppressMp,random);
            var previous=state.loadout().fighter();var vip=previous.vip().state();
            var change=ModernVipCharacterState.prepareExpiry(vip,deadline).orElseThrow(ModernVipRegenTimeline::invalid);
            var vitals=state.history().resources().vitals();
            var next=ModernVipCharacterState.acceptAt(change,vip,vip.ledger().actor(),vip.ledger().equipmentRevision(),deadline,vitals.hp(),vitals.mp());
            var fighter=ModernPhysicalCombat.qualifyVipRuntime(next,deadline,Math.incrementExact(previous.profile().equipmentRevision()),previous.armorProtection());
            var loadout=state.loadout().replacement(fighter);
            var history=state.history().reprofile(Math.incrementExact(state.history().worldRevision()),
                    loadout.historyProfile(state.history().spells().keySet(),state.history().items()!=null),
                    state.history().healing()==null?null:ModernPlayerHealing.qualify(loadout.fighter()).mpCost());
            // Remaining match stocks, never the captured shop's original
            // quantities. Expiry changes carrying capacity/gear, not purchases
            // or already-paid arrows and spell materials.
            var weight=loadout.reweight(state.stocks(),state.ammo(),state.weight().units(),state.materials());
            state=new State(loadout,history,state.regen(),state.clock(),weight,state.stocks(),state.ammo(),state.materials());
        }
        return active?regenerate(state,elapsed,suppressMp,random):state;
    }
    /** A lease is bounded by BOTH clocks. Wall rollback is checked by the
     * authority; a stalled wall clock cannot keep an elapsed lease alive. */
    static long effectiveEpoch(ModernBattleLoadout loadout,long nanos,long wallMillis) {
        var observation=loadout.fighter().vip().source().input().equipment().lifetimes();
        long elapsed=nanos-observation.queryStartedNanos();
        if(elapsed<0)throw invalid(); // subtraction deliberately supports nanoTime wrap
        return Math.max(wallMillis,Math.addExact(observation.queryStartedEpochMillis(),elapsed/1_000_000));
    }
    static long deadline(ModernBattleLoadout loadout) {
        long a=loadout.combatant().validUntilMillis();
        long b=loadout.weightBasis()==null?-1:loadout.weightBasis().validUntilMillis();
        return a<0?b:b<0?a:Math.min(a,b);
    }
    private static long boundary(ModernBattleLoadout loadout,long deadline,long activeStart) {
        var observation=loadout.fighter().vip().source().input().equipment().lifetimes();
        long duration=Math.multiplyExact(Math.subtractExact(deadline,observation.queryStartedEpochMillis()),1_000_000);
        return Math.addExact(observation.queryStartedNanos()-activeStart,duration);
    }
    private static State regenerate(State state,long elapsed,boolean suppressMp,ModernPhysicalCombat.BoundedRandom random) {
        var resources=state.history().resources();var v=resources.vitals();
        if(v.hp()==0||elapsed<state.clock().nextSecondElapsedNanos())return state;
        var values=state.loadout().regen().values();
        boolean eligible=values.fed()&&state.weight().view().permitsNaturalRegen();
        var due=ModernNaturalRegen.poll(values,state.clock(),elapsed,eligible&&v.hp()<v.maximumHp(),eligible&&v.mp()<v.maximumMp());
        if(suppressMp)due=new ModernNaturalRegen.Due(due.clock(),due.hpEvents(),0);
        var out=ModernNaturalRegen.resolve(values,due,v.hp(),v.mp(),random);
        var history=state.history();var ledger=state.regen();
        if(out.changed()) {
            long revision=Math.incrementExact(history.worldRevision());
            resources=resources.regenerateBatch(revision,out.hpGain(),out.hpLoss(),out.mpGain(),out.mpLoss(),out.hp(),out.mp());
            ledger=ledger.regenerated(out,revision);
            history=new ModernEquipmentHistory.State(history.identity(),revision,resources,history.spells(),history.items(),history.targets(),history.healing());
        }
        return new State(state.loadout(),history,ledger,out.clock(),state.weight(),state.stocks(),state.ammo(),state.materials());
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid VIP regeneration timeline");}
    private ModernVipRegenTimeline(){}
}
