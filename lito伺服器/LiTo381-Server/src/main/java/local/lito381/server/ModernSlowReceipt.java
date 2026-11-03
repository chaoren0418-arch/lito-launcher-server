package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Internal event proof, not target admission or a public wire capability.
 * A valid probability miss and equipment immunity are paid attempts. No HP
 * target is invented to represent a speed change. Other native immunity and
 * probability qualification remain mandatory in the future engine adapter. */
record ModernSlowReceipt(long activeElapsedNanos, int probability, int roll,
        long equipmentHasteItems, ModernSlowReceipt.Target target, ModernSlowNativeRules.MissingEffect missingEffect) implements ModernSpellRuntime.Detail {
    ModernSlowReceipt(long activeElapsedNanos,int probability,int roll,long equipmentHasteItems,Target target) {
        this(activeElapsedNanos,probability,roll,equipmentHasteItems,target,null);
    }
    enum Outcome { MISS, EQUIPMENT_IMMUNE, APPLIED, NEUTRALIZED, NATIVE_IMMUNE }
    sealed interface Target permits Player, Npc { ModernWorldMap.Cell cell(); long life(); }
    record Player(ModernTargetHistory.Participant participant, long life, long equipmentRevision,
            ModernWorldMap.Cell cell, ModernTimedEffects.State before, ModernTimedEffects.State after) implements Target {
        Player {
            Objects.requireNonNull(participant);Objects.requireNonNull(cell);Objects.requireNonNull(before);Objects.requireNonNull(after);
            if(life<1||equipmentRevision<0||before.entityId()!=participant.entityId()
                    ||after.entityId()!=participant.entityId())throw bad();
        }
    }
    record Npc(ModernNpcRoster.Id id, long life, int lane, ModernWorldMap.Cell cell,
            long rosterRevision, ModernNpcSpeed.State before, ModernNpcSpeed.State after) implements Target {
        Npc {
            Objects.requireNonNull(id);Objects.requireNonNull(cell);Objects.requireNonNull(before);Objects.requireNonNull(after);
            if(life<1||lane<0||lane>1||rosterRevision<1||before.revision()>rosterRevision)throw bad();
        }
    }
    ModernSlowReceipt {
        Objects.requireNonNull(target);
        if(activeElapsedNanos<0||activeElapsedNanos>ModernTimedEffects.MAX_OBSERVED_NANOS
                ||equipmentHasteItems<0||target instanceof Npc&&equipmentHasteItems!=0
                ||(missingEffect==null?(probability<1||probability>90||roll<1||roll>100)
                    :(!(target instanceof Npc)||probability!=0||roll!=0||equipmentHasteItems!=0)))throw bad();
        boolean apply=missingEffect==null&&roll<=probability&&equipmentHasteItems==0;
        if(target instanceof Player p) {
            if(p.before().observedNanos()>activeElapsedNanos)throw bad();
            if(apply?!ModernTimedEffects.applySlow(p.before(),activeElapsedNanos,p.after().revision()).equals(p.after())
                    :!p.before().equals(p.after()))throw bad();
        } else if(target instanceof Npc n) {
            // NPC roster time is milliseconds; retain the exact event's
            // nanoseconds for the caster's monotonic receipt as well.
            n.before().at(activeElapsedNanos/1_000_000);
            if(apply?!ModernNpcSpeed.slow(n.before(),activeElapsedNanos/1_000_000,
                    Math.incrementExact(n.rosterRevision())).equals(n.after()):!n.before().equals(n.after()))throw bad();
        }
    }
    Outcome outcome() {
        if(missingEffect!=null)return Outcome.NATIVE_IMMUNE;
        if(roll>probability)return Outcome.MISS;
        if(equipmentHasteItems>0)return Outcome.EQUIPMENT_IMMUNE;
        boolean neutralized=target instanceof Player p?p.before().hasteUntilNanos()>activeElapsedNanos
                :((Npc)target).before().hasteUntilMillis()>activeElapsedNanos/1_000_000;
        return neutralized?Outcome.NEUTRALIZED:Outcome.APPLIED;
    }
    void validate(ModernTargetHistory.Batch batch) {
        validate(batch.world(),batch.caster(),batch.lane(),batch.use(),batch.targets());
    }
    void validate(ModernTargetHistory.World world,ModernTargetHistory.Participant caster,ModernUseHistory.Lane lane,
            ModernUseHistory.Cast use,List<ModernTargetHistory.Effect> effects) {
        if(lane!=ModernUseHistory.Lane.SLOW||!effects.isEmpty()||use.cost()<15||use.cost()>ModernSlow.BASE_MP
                ||!ModernSlow.inSourceRange(new ModernWorldMap.Cell(use.before().x(),use.before().y()),target.cell()))throw bad();
        boolean applied=outcome()==Outcome.APPLIED||outcome()==Outcome.NEUTRALIZED;
        if(target instanceof Player p) {
            if(p.before().revision()>=use.revision()||applied&&p.after().revision()!=use.revision())throw bad();
            if(p.participant().entityId()==caster.entityId()||p.participant().identity().equals(caster.identity())) {
                if(!p.participant().equals(caster)||p.life()!=use.before().life()
                        ||p.equipmentRevision()!=use.before().equipmentRevision()
                        ||p.cell().x()!=use.before().x()||p.cell().y()!=use.before().y())throw bad();
            }
        } else if(target instanceof Npc n) {
            if(!world.contains(n.id().scope())||n.rosterRevision()>=use.revision()
                    ||n.after().revision()>=use.revision())throw bad();
        }
    }
    /** Bind even a paid no-effect attempt to the exact match-owned NPC.
     * Removed corpses may retain historical receipts; IDs are never reused. */
    void validateNpc(ModernBattleObservation.Npcs npcs,long useRevision,long worldRevision,ModernBattleEngine.Phase phase) {
        if(!(target instanceof Npc n))return;
        if(npcs==null)throw bad();
        var roster=npcs.roster();
        if(!n.id().scope().equals(roster.scope())||n.id().serial()>roster.spawned()
                ||n.rosterRevision()>roster.revision()||n.after().revision()>roster.revision())throw bad();
        var resident=roster.actors().stream().filter(a->a.id().equals(n.id())).findFirst().orElse(null);
        if(resident!=null&&(n.life()>resident.life()||n.lane()!=resident.lane()
                ||missingEffect!=null&&missingEffect.npcTemplateId()!=resident.template().npcId()))throw bad();
        if(useRevision==worldRevision&&(phase!=ModernBattleEngine.Phase.ACTIVE||resident==null||!resident.alive()
                ||n.life()!=resident.life()||!n.cell().equals(resident.cell())||!n.after().equals(resident.speed())))throw bad();
    }
    @Override public String toString(){return "SlowReceipt[redacted]";}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern slow receipt");}
}
