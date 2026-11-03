package local.lito381.server;

import java.util.Objects;

/** Immutable candidate, not a live cast. The engine MUST still authorize
 * phase/membership/force visibility, loaded/alive state, LOS, weight, cooldown,
 * global sequence and cancellation, and compare the captured state again
 * after RNG/clock callbacks. Publish BOTH actors, spell spending and incoming
 * healing receipts under one monitor. */
final class ModernPlayerHealingTransaction {
    record Actor(ModernTargetHistory.World world, ModernTargetHistory.Participant participant,
            ModernVitalBalance.State resources, ModernWorldMap.Cell cell, long effectRevision,
            ModernPlayerHealing.Effects effects) {
        Actor {
            Objects.requireNonNull(world); Objects.requireNonNull(participant); Objects.requireNonNull(resources);
            Objects.requireNonNull(cell); Objects.requireNonNull(effects);
            if (effectRevision<0) throw bad();
        }
        @Override public String toString() { return "PlayerHealingActor[redacted]"; }
    }
    record State(long worldRevision, ModernRooms.Scope scope, Actor caster, Actor target,
            ModernPlayerHealing.Profile profile, ModernSkills.Snapshot skills,
            ModernSkillSpending.Ledger spending, long sequence, long lawful) {
        State {
            Objects.requireNonNull(scope); Objects.requireNonNull(caster); Objects.requireNonNull(target);
            Objects.requireNonNull(profile); Objects.requireNonNull(skills); Objects.requireNonNull(spending);
            var id=caster.participant().identity(); var resources=caster.resources();
            if (!id.isHuman() || id.sourceId()!=scope.sourceId() || id.id()!=scope.objectId()
                    || !id.humanKey().equals(profile.key()) || !profile.accepts(skills)
                    || !caster.world().equals(target.world()) || worldRevision<resources.revision()
                    || worldRevision<target.resources().revision() || worldRevision<caster.effectRevision()
                    || worldRevision<target.effectRevision() || resources.vitals().hp()==0 || target.resources().vitals().hp()==0
                    || resources.equipmentRevision()!=profile.equipmentRevision()
                    || resources.vitals().maximumHp()!=profile.maximumHp() || resources.vitals().maximumMp()!=profile.maximumMp()
                    || spending.equipmentRevision()!=profile.equipmentRevision() || spending.cost()!=profile.mpCost()
                    || spending.profileRevision()>resources.revision() || spending.lastCastRevision()>resources.revision()
                    || spending.spent()>resources.flows().spent()
                    || sequence<spending.lastCastSequence() || sequence<0 || sequence>=worldRevision
                    || lawful< -32768 || lawful>32767) throw bad();
            boolean sameEntity=caster.participant().entityId()==target.participant().entityId();
            boolean sameIdentity=id.equals(target.participant().identity());
            if (sameEntity!=sameIdentity || sameEntity && !caster.equals(target)) throw bad();
        }
        boolean self() { return caster.participant().equals(target.participant()); }
        @Override public String toString() { return "PlayerHealingTransactionState[redacted]"; }
    }
    record Receipt(ModernTargetHistory.World world, ModernRooms.Scope source,
            ModernTargetHistory.Participant caster, ModernUseHistory.Cast use,
            long targetEffectRevision, ModernPlayerHealing.Effects effects,
            long baseAmount, long adjustedAmount, ModernTargetHistory.Effect target) {
        Receipt {
            Objects.requireNonNull(world); Objects.requireNonNull(source); Objects.requireNonNull(caster);
            var id=caster.identity();
            if (!id.isHuman() || id.sourceId()!=source.sourceId() || id.id()!=source.objectId()) throw bad();
            new ModernPlayerHealingHistory.Impact(world,caster,use,targetEffectRevision,effects,baseAmount,adjustedAmount,target);
        }
        ModernPlayerHealingHistory.Impact impact() {
            return new ModernPlayerHealingHistory.Impact(world,caster,use,targetEffectRevision,effects,baseAmount,adjustedAmount,target);
        }
        @Override public String toString() { return "PlayerHealingReceipt[redacted]"; }
    }
    static final class Prepared {
        private final State before;
        private final ModernVitalBalance.State caster, target;
        private final ModernSkillSpending.Ledger spending;
        private final Receipt receipt;
        private final ModernPlayerHealing.Effects targetEffects;
        private final ModernPlayerHealing.Outcome outcome;
        private Prepared(State before, ModernVitalBalance.State caster, ModernVitalBalance.State target,
                ModernSkillSpending.Ledger spending, Receipt receipt, ModernPlayerHealing.Outcome outcome) {
            this.before=before;this.caster=caster;this.target=target;this.spending=spending;this.receipt=receipt;this.targetEffects=outcome.after();this.outcome=outcome;
        }
        boolean matches(State current) { return before.equals(current); }
        State before() { return before; }
        ModernVitalBalance.State caster() { return caster; }
        ModernVitalBalance.State target() { return target; }
        ModernSkillSpending.Ledger spending() { return spending; }
        Receipt receipt() { return receipt; }
        ModernPlayerHealing.Effects targetEffects() { return targetEffects; }
        @Override public String toString() { return "PreparedPlayerHealing[redacted]"; }
    }
    static Prepared prepare(State before, long nextRevision, long sequence, ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(before); Objects.requireNonNull(random);
        var caster=before.caster(); var target=before.target();
        if (nextRevision!=Math.incrementExact(before.worldRevision()) || sequence!=Math.incrementExact(before.sequence())
                || !ModernHonor.inScreen(caster.cell().x(),caster.cell().y(),target.cell().x(),target.cell().y())
                || caster.resources().vitals().mp()<before.profile().mpCost()) throw bad();
        // Fail cheap resource/sequence/counter checks before invoking RNG.
        var ledger=before.spending().cast(nextRevision,sequence);
        caster.resources().spend(nextRevision,ledger.cost());
        var v=target.resources().vitals();
        var outcome=ModernPlayerHealing.resolve(before.profile(),before.lawful(),v.hp(),v.maximumHp(),target.effects(),random);
        return resolved(before,nextRevision,sequence,outcome);
    }
    /** Only the publication revision may change. The engine must separately
     * revalidate BOTH actors and complete histories. Reuse the one resolved
     * roll; never reroll or merge changed HP/MP into an old transaction. */
    static Prepared atWorldRevision(Prepared prepared,long revision) {
        Objects.requireNonNull(prepared);var b=prepared.before();
        if(revision<b.worldRevision())throw bad();
        if(revision==b.worldRevision())return prepared;
        var current=new State(revision,b.scope(),b.caster(),b.target(),b.profile(),b.skills(),b.spending(),b.sequence(),b.lawful());
        return resolved(current,Math.incrementExact(revision),prepared.receipt().use().sequence(),prepared.outcome);
    }
    private static Prepared resolved(State before,long nextRevision,long sequence,ModernPlayerHealing.Outcome outcome) {
        var caster=before.caster();var target=before.target();
        var ledger=before.spending().cast(nextRevision,sequence);
        var spent=caster.resources().spend(nextRevision,ledger.cost());
        var use=new ModernUseHistory.Cast(nextRevision,sequence,
                new ModernUseHistory.Anchor(caster.resources().equipmentRevision(),Math.incrementExact(caster.resources().life()),
                        caster.resources().vitals(),caster.cell().x(),caster.cell().y()),ledger.cost(),spent.vitals().mp());
        var v=target.resources().vitals();
        var health=new ModernTargetHistory.Health(target.participant(),Math.incrementExact(target.resources().life()),
                target.resources().equipmentRevision(),target.cell(),v.hp(),v.maximumHp());
        var effect=new ModernTargetHistory.Effect(ModernTargetHistory.Kind.HEAL,health,
                Math.max(0,outcome.adjustedAmount()),outcome.restored(),outcome.remainingHp());
        var receipt=new Receipt(caster.world(),before.scope(),caster.participant(),use,target.effectRevision(),target.effects(),
                outcome.baseAmount(),outcome.adjustedAmount(),effect);
        var restored=before.self()?caster.resources().restoreAndSpend(nextRevision,outcome.restored(),ledger.cost())
                :target.resources().restore(nextRevision,outcome.restored());
        return new Prepared(before,before.self()?restored:spent,restored,ledger,receipt,outcome);
    }
    /** A complete human/human candidate, never a live mutation. Keep both
     * histories together until the engine rechecks authorization/captured state. */
    record Complete(ModernEquipmentHistory.State caster,ModernEquipmentHistory.State target) { }
    static Complete complete(Prepared prepared,ModernEquipmentHistory.State caster,ModernEquipmentHistory.State target) {
        Objects.requireNonNull(prepared);Objects.requireNonNull(caster);Objects.requireNonNull(target);
        var before=prepared.before();var receipt=prepared.receipt();long next=receipt.use().revision();
        checkHistory(before.worldRevision(),before.caster(),caster);
        checkHistory(before.worldRevision(),before.target(),target);
        if(!caster.identity().source().equals(before.scope())||caster.healing()==null||caster.healing().outgoing()==null
                ||!caster.healing().outgoing().spending().current().equals(before.spending())
                ||caster.spells().values().stream().anyMatch(s->s.current().lastCastSequence()>=receipt.use().sequence())
                ||target.healing()==null||before.self()&&!caster.equals(target))throw bad();
        var sent=caster.healing().outgoing().cast(receipt);
        if(!sent.spending().current().equals(prepared.spending()))throw bad();
        var received=target.healing().incoming().receive(receipt.impact());
        var casterEntry=new ModernPlayerHealingHistory.Entry(before.self()?received:caster.healing().incoming(),sent);
        var casterAfter=new ModernEquipmentHistory.State(caster.identity(),next,prepared.caster(),caster.spells(),caster.items(),caster.targets(),casterEntry);
        var targetAfter=before.self()?casterAfter:new ModernEquipmentHistory.State(target.identity(),next,prepared.target(),target.spells(),target.items(),target.targets(),
                new ModernPlayerHealingHistory.Entry(received,target.healing().outgoing()));
        return new Complete(casterAfter,targetAfter);
    }
    private static void checkHistory(long revision,Actor actor,ModernEquipmentHistory.State history) {
        if(history.worldRevision()!=revision||!history.identity().world().equals(actor.world())
                ||!history.identity().actor().equals(actor.participant())||!history.resources().equals(actor.resources()))throw bad();
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid complete player healing transaction"); }
    private ModernPlayerHealingTransaction() { }
}
