package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Complete immutable debit/effect preparation. This does not enable skill29
 * in any battle, dispatcher or wire. The owning engine still MUST authorize
 * identity/target, source probability and immunity, geometry, life/equipment,
 * cooldowns and cancellation, recheck after RNG/I/O, and publish this whole
 * result and the target under its single monitor. Never commit its pieces
 * separately or interpret this arithmetic proof as admission. */
final class ModernSlowTransaction {
    record State(ModernEquipmentHistory.State history, ModernSpellRuntime.State runtime,
            ModernSpellMaterials.State materials) {
        State {
            Objects.requireNonNull(history);Objects.requireNonNull(runtime);Objects.requireNonNull(materials);
            runtime.validate(history);
            var id=history.identity();var spell=history.spells().get(ModernUseHistory.Lane.SLOW);
            if(spell==null||!id.world().equals(materials.world())||!id.source().equals(materials.scope())
                    ||!id.actor().equals(materials.caster())||materials.revision()>history.worldRevision()
                    ||materials.uses()!=spell.current().casts())throw bad();
            if(spell.last()!=null) {
                var use=spell.last();
                if(materials.revision()!=use.revision()||materials.lastSequence()!=use.sequence()
                        ||materials.lastLife()!=use.before().life()
                        ||materials.lastEquipmentRevision()!=use.before().equipmentRevision())throw bad();
            }
        }
        @Override public String toString(){return "SlowTransactionState[redacted]";}
    }
    static final class Prepared {
        private final State before,after;
        private final ModernInventoryWeight.Snapshot weight;
        private final ModernSlowReceipt effect;
        private Prepared(State before,State after,ModernInventoryWeight.Snapshot weight,ModernSlowReceipt effect) {
            this.before=before;this.after=after;this.weight=weight;this.effect=effect;
        }
        State before(){return before;}
        State after(){return after;}
        ModernInventoryWeight.Snapshot weight(){return weight;}
        ModernSlowReceipt effect(){return effect;}
        /** Only the caster-side comparison; the engine must also pin the exact
         * live target and world phase. No mutation or external callback here. */
        boolean matchesCaster(State current){return before.equals(current);}
        @Override public String toString(){return "PreparedSlowTransaction[redacted]";}
    }
    static Prepared prepare(State before,ModernSlow.Profile profile,ModernSlowCatalog.Snapshot catalog,
            long nextRevision,long sequence,long now,ModernWorldMap.Cell origin,ModernSlowReceipt effect,
            ModernInventoryWeight.Basis basis,List<ModernItemProtocol.Stock> items,List<ModernAmmo.Stock> ammo,
            List<ModernInventoryWeight.Unit> units) {
        Objects.requireNonNull(before);Objects.requireNonNull(profile);Objects.requireNonNull(catalog);
        Objects.requireNonNull(effect);Objects.requireNonNull(basis);
        var h=before.history();var id=h.identity();var lane=ModernUseHistory.Lane.SLOW;
        if(!catalog.accepts(profile)||catalog.accessLevel().isEmpty()
                ||profile.key().sourceId()!=id.source().sourceId()||profile.key().objectId()!=id.source().objectId()
                ||profile.equipmentRevision()!=h.resources().equipmentRevision()
                ||profile.mpCost()!=h.spells().get(lane).current().cost()
                ||nextRevision!=Math.incrementExact(h.worldRevision())
                ||basis.equipmentRevision()!=profile.equipmentRevision())throw bad();
        // Validate the PRE-cast weight too. A material debit must never make an
        // overweight cast appear legal retrospectively.
        if(!before.materials().reweight(basis,items,ammo,units).view().permitsAttackCast())throw bad();
        var history=h.cast(lane,nextRevision,sequence,origin,List.of());
        var materials=before.materials().slow(nextRevision,sequence,Math.incrementExact(h.resources().life()),profile,catalog);
        var runtime=before.runtime().cast(h,history,lane,now,effect);
        var after=new State(history,runtime,materials);
        var weight=materials.reweight(basis,items,ammo,units);
        return new Prepared(before,after,weight,effect);
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid complete slow transaction");}
    private ModernSlowTransaction(){}
}
