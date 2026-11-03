package local.lito381.server;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** Immutable applied-VIP ledger and compare-and-accept candidates. This is not
 * equipment authority, a SQL commit, a render/shape-resource grant or a saved-AC
 * reconstruction. The engine must atomically publish this result WITH its
 * authoritative equipment/vitals and durable result, under its owner guards.
 * Pin already-applied templates across catalog reload; only a newly equipped
 * item/shape adopts fresh templates. No classic executor is called. */
final class ModernVipLifecycle {
    enum Operation { EQUIP, UNEQUIP, EQUIPMENT_REVISION, EXPIRE, CLEAR_SHAPE, TIME_REFRESH }
    record Actor(UUID context,TournamentPresence.CharacterKey owner,long entity,long life) {
        Actor {Objects.requireNonNull(context);Objects.requireNonNull(owner);if(entity<1||life<0)throw invalid();}
        @Override public String toString(){return "VipLifecycleActor[redacted]";}
    }
    static final class State {
        private final Actor actor;
        private final ModernEquipmentReconciliation.ExpectedBinding binding;
        private final String database;
        private final long equipmentRevision,sequence,observedMillis;
        private final Map<Long,ModernVipLoadout.Applied> items;
        private final ModernVipEquipment.Polymorph shape;
        private final ModernVipEquipment.Abilities total;
        private final Set<Long> icons;
        private State(Actor actor,ModernEquipmentReconciliation.ExpectedBinding binding,String database,
                long equipmentRevision,long sequence,long now,Map<Long,ModernVipLoadout.Applied> items,ModernVipEquipment.Polymorph shape) {
            this.actor=Objects.requireNonNull(actor);this.binding=Objects.requireNonNull(binding);this.database=database;
            if(!actor.owner().equals(binding.key())||equipmentRevision<0||sequence<0||now<0||items.size()>ModernCharacterData.MAX_ITEMS)throw invalid();
            ModernPhysicalCombat.identifier(database);this.equipmentRevision=equipmentRevision;this.sequence=sequence;observedMillis=now;
            var types=new HashSet<Long>();var icons=new HashSet<Long>();var total=ModernVipEquipment.Abilities.zero();
            boolean shapeOwned=shape==null;
            for(var e:items.entrySet()) {
                var item=Objects.requireNonNull(e.getValue());
                if(e.getKey()==null||e.getKey()!=item.objectId()||expired(item,now)||!types.add(item.template().exclusiveType()))throw invalid();
                total=total.plus(item.template().abilities());
                if(item.template().iconId()!=0)icons.add(item.template().iconId());
                if(shape!=null&&item.template().polymorphId()==shape.graphicsId())shapeOwned=true;
            }
            if(!shapeOwned)throw invalid();
            this.items=Map.copyOf(new TreeMap<>(items));this.shape=shape;this.icons=Set.copyOf(icons);
            this.total=shape==null?total:total.plus(shape.abilities());
        }
        Actor actor(){return actor;}
        ModernEquipmentReconciliation.ExpectedBinding binding(){return binding;}
        String database(){return database;}
        long equipmentRevision(){return equipmentRevision;}
        long sequence(){return sequence;}
        long observedMillis(){return observedMillis;}
        List<ModernVipLoadout.Applied> items(){return items.values().stream().sorted(java.util.Comparator.comparingLong(ModernVipLoadout.Applied::objectId)).toList();}
        ModernVipEquipment.Abilities total(){return total;}
        Set<Long> icons(){return icons;}
        Optional<ModernVipEquipment.Polymorph> shape(){return Optional.ofNullable(shape);}
        @Override public String toString(){return "AppliedVipState[redacted]";}
    }
    static final class Candidate {
        private final State before,after;
        private final Operation operation;
        private final long itemObjectId;
        private final ModernVipEquipment.Abilities delta;
        private final Set<Long> addedIcons,removedIcons;
        private Candidate(State before,State after,Operation operation,long itemObjectId) {
            this.before=before;this.after=after;this.operation=operation;this.itemObjectId=itemObjectId;
            delta=after.total.minus(before.total); // Check EVERY i64 delta before any publication.
            var add=new HashSet<>(after.icons);add.removeAll(before.icons);addedIcons=Set.copyOf(add);
            var remove=new HashSet<>(before.icons);remove.removeAll(after.icons);removedIcons=Set.copyOf(remove);
        }
        State before(){return before;} State after(){return after;}
        Operation operation(){return operation;} long itemObjectId(){return itemObjectId;}
        ModernVipEquipment.Abilities delta(){return delta;}
        Set<Long> addedIcons(){return addedIcons;} Set<Long> removedIcons(){return removedIcons;}
        /** Caller supplies authoritative LIVE maxima that already include the
         * before ledger, never persisted SQL maxima or FULL_NEW_MATCH values. */
        ModernEquipmentVitals.Vitals retainVitals(ModernEquipmentVitals.Vitals live) {
            return ModernEquipmentVitals.retain(live,Math.addExact(live.maximumHp(),delta.get(ModernVipEquipment.Field.MAX_HP)),
                    Math.addExact(live.maximumMp(),delta.get(ModernVipEquipment.Field.MAX_MP)));
        }
        @Override public String toString(){return "VipLifecycleCandidate[redacted]";}
    }
    static State start(Actor actor,ModernVipLoadout loadout,long now) {
        Objects.requireNonNull(loadout);fresh(loadout,now);
        return new State(actor,loadout.binding(),loadout.database(),loadout.equipmentRevision(),0,now,index(loadout),loadout.shape().orElse(null));
    }
    /** Engine-owned incarnation binding. Retains pinned templates, sequence,
     * deadlines and saved version; never replays an equip or reloads a catalog. */
    static State rebind(State before,Actor actor) {
        if(!before.actor.owner().equals(actor.owner()))throw invalid();
        return new State(actor,before.binding,before.database,before.equipmentRevision,before.sequence,before.observedMillis,before.items,before.shape);
    }
    static Candidate prepareEquip(State before,ModernVipLoadout next,long objectId,long now) {
        return equipment(before,next,objectId,Operation.EQUIP,now);
    }
    static Candidate prepareUnequip(State before,ModernVipLoadout next,long objectId,long now) {
        return equipment(before,next,objectId,Operation.UNEQUIP,now);
    }
    /** Removal of a known applied item must remain calculable even when its
     * catalog row was deleted/reloaded. This does not grant a durable SET: the
     * caller must still validate ownership/selection and commit that operation.
     * Nothing is learned from a new template, and no sibling state is changed. */
    static Candidate prepareRetainedUnequip(State before,long objectId,long now) {
        current(before,now);var applied=before.items.get(objectId);if(applied==null)throw invalid();
        var items=new TreeMap<>(before.items);items.remove(objectId);
        return candidate(before,items,applied.template().polymorphId()==0?before.shape:null,
                Math.incrementExact(before.equipmentRevision),now,Operation.UNEQUIP,objectId);
    }
    /** Non-VIP equipment changes still advance the shared equipment revision,
     * but cannot refresh unchanged VIP templates or their current shape. */
    static Candidate prepareEquipmentRevision(State before,ModernVipLoadout next,long now) {
        return equipment(before,next,0,Operation.EQUIPMENT_REVISION,now);
    }
    private static Candidate equipment(State before,ModernVipLoadout next,long objectId,Operation operation,long now) {
        current(before,now);Objects.requireNonNull(next);fresh(next,now);
        if(!before.binding.equals(next.binding())||!before.database.equals(next.database())
                ||next.equipmentRevision()!=Math.incrementExact(before.equipmentRevision))throw invalid();
        var fresh=index(next);var expected=new HashSet<>(before.items.keySet());
        if(operation==Operation.EQUIP) {if(objectId<1||!expected.add(objectId))throw invalid();}
        else if(operation==Operation.UNEQUIP) {if(objectId<1||!expected.remove(objectId))throw invalid();}
        if(!expected.equals(fresh.keySet()))throw invalid(); // No hidden sibling equip/unequip/expiry.
        var items=new TreeMap<Long,ModernVipLoadout.Applied>();
        for(long id:expected) {
            var observed=fresh.get(id);var applied=before.items.get(id);
            if(applied==null)items.put(id,observed);
            else {
                if(!applied.sourceItem().equals(observed.sourceItem())||applied.template().catalogId()!=observed.template().catalogId())throw invalid();
                // Fresh DB clock rounding must NEVER extend an applied lease.
                var oldLife=applied.lifetime();var newLife=observed.lifetime();
                if(oldLife.state()!=newLife.state())throw invalid();
                long end=oldLife.state()==ModernItemLifetime.State.ACTIVE?Math.min(oldLife.expiresAtMillis(),newLife.expiresAtMillis()):-1;
                items.put(id,new ModernVipLoadout.Applied(id,applied.template(),new ModernItemLifetime.View(id,oldLife.state(),end),applied.sourceItem()));
            }
        }
        var shape=before.shape;
        if(operation==Operation.EQUIP&&fresh.get(objectId).template().polymorphId()!=0)shape=next.shape().orElseThrow(ModernVipLifecycle::invalid);
        if(operation==Operation.UNEQUIP&&before.items.get(objectId).template().polymorphId()!=0)shape=null;
        long expectedShape=shape==null?0:shape.graphicsId();
        if(next.shape().map(ModernVipEquipment.Polymorph::graphicsId).orElse(0L)!=expectedShape)throw invalid();
        if(operation==Operation.EQUIP&&fresh.get(objectId).template().polymorphId()!=0
                &&fresh.get(objectId).template().polymorphId()!=expectedShape)throw invalid();
        return candidate(before,items,shape,next.equipmentRevision(),now,operation,objectId);
    }
    static Optional<Candidate> prepareExpiry(State before,long now) {
        time(before,now);var items=new TreeMap<>(before.items);boolean removed=false,clear=false;
        for(var item:before.items.values())if(expired(item,now)) {
            items.remove(item.objectId());removed=true;clear|=item.template().polymorphId()!=0;
        }
        return removed?Optional.of(candidate(before,items,clear?null:before.shape,before.equipmentRevision,now,Operation.EXPIRE,0)):Optional.empty();
    }
    /** Explicit already-authorized shape removal only. The cancellation flag
     * alone is not proof of the spell/map/weapon admission policy; no automatic
     * cancellation decision is invented here. VIP item abilities remain. */
    static Optional<Candidate> prepareClearShape(State before,long now) {
        current(before,now);
        return before.shape==null?Optional.empty():Optional.of(candidate(before,before.items,null,before.equipmentRevision,now,Operation.CLEAR_SHAPE,0));
    }
    /** Refresh another timed equipment layer without changing the VIP ledger.
     * The combined character state admits this only at its qualified deadline. */
    static Candidate prepareTimeRefresh(State before,long now) {
        current(before,now);
        return candidate(before,before.items,before.shape,before.equipmentRevision,now,Operation.TIME_REFRESH,0);
    }
    /** No mutation: caller owns commit, HP races, selected source, room/match,
     * life and durable equipment admission. Exact before identity prevents ABA
     * and replay; advancing time cannot renew a lease or accept newly expired gear. */
    static State acceptAt(Candidate candidate,Actor actor,State current,long currentEquipmentRevision,long now) {
        Objects.requireNonNull(candidate);
        if(candidate.before!=current||!current.actor.equals(actor)||current.equipmentRevision!=currentEquipmentRevision
                ||now<candidate.after.observedMillis)throw stale();
        for(var item:current.items.values())if(expired(item,candidate.after.observedMillis)!=expired(item,now))throw stale();
        for(var item:candidate.after.items.values())if(expired(item,now))throw stale();
        var after=candidate.after;
        return new State(after.actor,after.binding,after.database,after.equipmentRevision,after.sequence,now,after.items,after.shape);
    }
    private static Candidate candidate(State before,Map<Long,ModernVipLoadout.Applied> items,ModernVipEquipment.Polymorph shape,
            long revision,long now,Operation operation,long id) {
        return new Candidate(before,new State(before.actor,before.binding,before.database,revision,Math.incrementExact(before.sequence),now,items,shape),operation,id);
    }
    private static Map<Long,ModernVipLoadout.Applied> index(ModernVipLoadout loadout) {
        var result=new TreeMap<Long,ModernVipLoadout.Applied>();
        for(var item:loadout.items())if(result.put(item.objectId(),item)!=null)throw invalid();
        return result;
    }
    private static void fresh(ModernVipLoadout loadout,long now) {
        if(now<loadout.evaluatedAtMillis()||loadout.validUntilMillis()>=0&&now>=loadout.validUntilMillis())throw invalid();
    }
    private static void time(State state,long now){Objects.requireNonNull(state);if(now<state.observedMillis)throw invalid();}
    private static void current(State state,long now){time(state,now);for(var item:state.items.values())if(expired(item,now))throw invalid();}
    private static boolean expired(ModernVipLoadout.Applied item,long now){return item.lifetime().state()==ModernItemLifetime.State.ACTIVE&&now>=item.lifetime().expiresAtMillis();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid VIP lifecycle candidate");}
    private static IllegalArgumentException stale(){return new IllegalArgumentException("stale VIP lifecycle candidate");}
    private ModernVipLifecycle(){}
}
