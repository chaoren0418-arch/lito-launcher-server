package local.lito381.server;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Live spell authority, independent of the <=34 fixed-price serializers.
 * The engine plans this immutable state BEFORE publishing victims/resources.
 * Timestamps are server monotonic clock tokens, not wall time or a wire ABI.
 * Target permission, geometry, random calculation and cooldown admission are
 * still performed by the engine. This is not permission to enable live SET. */
final class ModernSpellRuntime {
    sealed interface Detail permits Ordinary, Teleport, Disintegrate, Arrow, FireStorm, NpcFireStorm, GreaterHaste, GreaterHasteNeutralized, ModernHasteReceipt, ModernSlowReceipt {}
    record GreaterHaste(long activeElapsedNanos,long hasteUntilNanos) implements Detail {
        GreaterHaste {
            if(activeElapsedNanos<0||activeElapsedNanos>ModernTimedEffects.MAX_OBSERVED_NANOS
                    ||hasteUntilNanos!=Math.addExact(activeElapsedNanos,
                        java.util.concurrent.TimeUnit.SECONDS.toNanos(ModernGreaterHaste.DURATION_SECONDS)))throw bad();
        }
    }
    record GreaterHasteNeutralized(long activeElapsedNanos,ModernTimedEffects.State before,ModernTimedEffects.State after) implements Detail {
        GreaterHasteNeutralized {
            Objects.requireNonNull(before);Objects.requireNonNull(after);
            if(before.slowUntilNanos()<=activeElapsedNanos||after.hasteUntilNanos()!=0
                    ||!ModernTimedEffects.applyGreaterHaste(before,activeElapsedNanos,after.revision()).equals(after))throw bad();
        }
        void validate(ModernTargetHistory.Batch batch) {
            if(before.entityId()!=batch.caster().entityId()||before.revision()>=batch.use().revision()
                    ||after.revision()!=batch.use().revision())throw bad();
        }
    }
    enum Ordinary implements Detail { INSTANCE }
    record Arrow(boolean critical) implements Detail {}
    record FireStorm(List<Long> damageLossAfter) implements Detail {
        FireStorm {damageLossAfter=List.copyOf(damageLossAfter);if(damageLossAfter.stream().anyMatch(n->n<0))throw bad();}
    }
    record NpcFireStorm(ModernQuestObjectives.Kind questKind) implements Detail {}
    record Teleport(ModernWorldMap.Cell destination) implements Detail {
        Teleport { Objects.requireNonNull(destination); }
    }
    record Disintegrate(boolean critical,long lawfulBefore,long lawfulAfter,long damageLossAfter) implements Detail {
        Disintegrate(boolean critical,long before,long after){this(critical,before,after,0);}
        Disintegrate {
            if(!ModernDisintegrate.lawfulAllows(lawfulBefore)
                    ||lawfulAfter!=ModernDisintegrate.lawfulAfterCast(lawfulBefore)||damageLossAfter<0)throw bad();
        }
    }
    record Receipt(ModernTargetHistory.Batch batch,long casts,long spent,long atNanos,Detail detail) {
        Receipt {
            Objects.requireNonNull(batch);Objects.requireNonNull(detail);
            new ModernSkillSpending.Totals(casts,spent);
            if(casts<1||casts>batch.use().sequence()||spent<batch.use().cost()
                    ||casts==1&&spent!=batch.use().cost())throw bad();
            var lane=batch.lane();
            if(lane==ModernUseHistory.Lane.TELEPORT) {
                if(!(detail instanceof Teleport t)
                        ||t.destination().equals(new ModernWorldMap.Cell(batch.use().before().x(),batch.use().before().y())))throw bad();
            } else if(lane==ModernUseHistory.Lane.GREATER_HASTE) {
                if(!(detail instanceof GreaterHaste)&&!(detail instanceof GreaterHasteNeutralized)||!batch.targets().isEmpty()
                        ||batch.use().cost()<25||batch.use().cost()>ModernGreaterHaste.BASE_MP)throw bad();
                if(detail instanceof GreaterHasteNeutralized n)n.validate(batch);
            } else if(lane==ModernUseHistory.Lane.HASTE) {
                if(!(detail instanceof ModernHasteReceipt h))throw bad();
                h.validate(batch);
            } else if(lane==ModernUseHistory.Lane.SLOW) {
                if(!(detail instanceof ModernSlowReceipt s))throw bad();
                s.validate(batch);
            } else if(disintegrate(lane)) {
                if(!(detail instanceof Disintegrate d)
                        ||(lane==ModernUseHistory.Lane.DISINTEGRATE?d.damageLossAfter()<batch.targets().get(0).applied():d.damageLossAfter()!=0))throw bad();
            } else if(lane==ModernUseHistory.Lane.LIGHT_ARROW||lane==ModernUseHistory.Lane.NPC_LIGHT_ARROW) {
                if(!(detail instanceof Arrow))throw bad();
            } else if(lane==ModernUseHistory.Lane.FIRE_STORM) {
                if(!(detail instanceof FireStorm f)||f.damageLossAfter().size()!=batch.targets().size())throw bad();
                for(int i=0;i<batch.targets().size();i++)if(f.damageLossAfter().get(i)<batch.targets().get(i).applied())throw bad();
            } else if(lane==ModernUseHistory.Lane.NPC_FIRE_STORM) {
                if(!(detail instanceof NpcFireStorm n))throw bad();checkStructureKind(batch,n.questKind());
            } else if(lane!=ModernUseHistory.Lane.HEALING||detail!=Ordinary.INSTANCE)throw bad();
        }
    }
    /** The last structure event retains its own lane count and Quest kind;
     * neither can be reconstructed from a later NPC-only cast or current map. */
    record Structure(long casts,ModernTargetHistory.Batch batch,ModernQuestObjectives.Kind questKind) {
        Structure {
            Objects.requireNonNull(batch);
            if(casts<1||casts>batch.use().sequence()||batch.lane()!=ModernUseHistory.Lane.NPC_FIRE_STORM||!hasStructure(batch))throw bad();
            checkStructureKind(batch,questKind);
        }
    }
    /** Separate from the old Honor HEALING lane; private caster scope stays here. */
    record PlayerHealingReceipt(ModernPlayerHealingTransaction.Receipt receipt,long casts,long spent,long atNanos) {
        PlayerHealingReceipt {
            Objects.requireNonNull(receipt);new ModernSkillSpending.Totals(casts,spent);
            if(casts<1||casts>receipt.use().sequence()||spent<receipt.use().cost()
                    ||casts==1&&spent!=receipt.use().cost()||spent/4>casts||spent/4==casts&&spent%4!=0)throw bad();
        }
    }
    private record Stamp(ModernUseHistory.Cast use,long atNanos) { }
    record State(ModernEquipmentHistory.Identity identity,Set<ModernUseHistory.Lane> lanes,long birthRevision,
            long sequence,long requestRevision,long initialLawful,long lawful,Map<ModernUseHistory.Lane,Receipt> receipts,Structure structure,
            boolean playerHealingEnabled,PlayerHealingReceipt playerHealing) {
        State(ModernEquipmentHistory.Identity identity,Set<ModernUseHistory.Lane> lanes,long birthRevision,
                long sequence,long requestRevision,long initialLawful,long lawful,Map<ModernUseHistory.Lane,Receipt> receipts,Structure structure) {
            this(identity,lanes,birthRevision,sequence,requestRevision,initialLawful,lawful,receipts,structure,false,null);
        }
        State(ModernEquipmentHistory.Identity identity,Set<ModernUseHistory.Lane> lanes,long birthRevision,
                long sequence,long requestRevision,long initialLawful,long lawful,Map<ModernUseHistory.Lane,Receipt> receipts) {
            this(identity,lanes,birthRevision,sequence,requestRevision,initialLawful,lawful,receipts,null);
        }
        State {
            Objects.requireNonNull(identity);lanes=Set.copyOf(lanes);receipts=Map.copyOf(receipts);
            if(birthRevision<1||sequence<0||requestRevision<0||(sequence==0)!=(requestRevision==0)
                    ||sequence>0&&(requestRevision<=birthRevision||sequence>requestRevision-birthRevision)
                    ||!lanes.containsAll(receipts.keySet())||!playerHealingEnabled&&playerHealing!=null)throw bad();
            long count=0,lawfulCount=0;Receipt latestDisintegrate=null;
            var revisions=new HashSet<Long>();var sequences=new HashSet<Long>();
            for(var entry:receipts.entrySet()) {
                var lane=entry.getKey();var receipt=entry.getValue();var batch=receipt.batch();var use=batch.use();
                if(batch.lane()!=lane||!batch.world().equals(identity.world())||!batch.source().equals(identity.source())
                        ||!batch.caster().equals(identity.actor())||use.sequence()>sequence||use.revision()>requestRevision
                        ||use.revision()<=birthRevision||!revisions.add(use.revision())||!sequences.add(use.sequence()))throw bad();
                count=Math.addExact(count,receipt.casts());
                if(disintegrate(lane)) {
                    lawfulCount=Math.addExact(lawfulCount,receipt.casts());
                    var d=(Disintegrate)receipt.detail();if(d.lawfulBefore()<initialLawful||d.lawfulAfter()>lawful)throw bad();
                    if(latestDisintegrate==null||use.sequence()>latestDisintegrate.batch().use().sequence())latestDisintegrate=receipt;
                }
            }
            if(playerHealing!=null) {
                var r=playerHealing.receipt();var u=r.use();
                if(!r.world().equals(identity.world())||!r.source().equals(identity.source())||!r.caster().equals(identity.actor())
                        ||u.sequence()>sequence||u.revision()>requestRevision||u.revision()<=birthRevision
                        ||!revisions.add(u.revision())||!sequences.add(u.sequence()))throw bad();
                count=Math.addExact(count,playerHealing.casts());
            }
            if(count>sequence||lawful!=Math.addExact(initialLawful,lawfulCount)
                    ||latestDisintegrate!=null&&((Disintegrate)latestDisintegrate.detail()).lawfulAfter()!=lawful)throw bad();
            var stamps=new java.util.ArrayList<Stamp>();receipts.values().forEach(r->stamps.add(new Stamp(r.batch().use(),r.atNanos())));
            if(playerHealing!=null)stamps.add(new Stamp(playerHealing.receipt().use(),playerHealing.atNanos()));
            var ordered=stamps.stream().sorted(java.util.Comparator.comparingLong(r->r.use().sequence())).toList();
            for(int i=1;i<ordered.size();i++)if(ordered.get(i).use().revision()<=ordered.get(i-1).use().revision()
                    ||ordered.get(i).atNanos()-ordered.get(i-1).atNanos()<0)throw bad();
            var fire=receipts.get(ModernUseHistory.Lane.NPC_FIRE_STORM);
            if(fire!=null&&hasStructure(fire.batch())) {
                if(structure==null||structure.casts()!=fire.casts()||!structure.batch().equals(fire.batch())
                        ||structure.questKind()!=((NpcFireStorm)fire.detail()).questKind())throw bad();
            }
            if(structure!=null) {
                var batch=structure.batch();var use=batch.use();
                if(fire==null||!batch.world().equals(identity.world())||!batch.source().equals(identity.source())||!batch.caster().equals(identity.actor())
                        ||structure.casts()>fire.casts()||use.revision()<=birthRevision||use.revision()>fire.batch().use().revision()
                        ||use.sequence()>fire.batch().use().sequence()||use.before().life()>fire.batch().use().before().life()
                        ||structure.casts()==fire.casts()&&!batch.equals(fire.batch())
                        ||structure.casts()<fire.casts()&&(use.revision()>=fire.batch().use().revision()||use.sequence()>=fire.batch().use().sequence()
                            ||fire.casts()-structure.casts()>fire.batch().use().revision()-use.revision()
                            ||fire.casts()-structure.casts()>fire.batch().use().sequence()-use.sequence()))throw bad();
            }
        }
        static State initial(ModernEquipmentHistory.Identity identity,Set<ModernUseHistory.Lane> lanes,long revision,long lawful) {
            return new State(identity,lanes,revision,0,0,lawful,lawful,Map.of());
        }
        static State initial(ModernEquipmentHistory.Identity identity,Set<ModernUseHistory.Lane> lanes,long revision,long lawful,boolean playerHealing) {
            return new State(identity,lanes,revision,0,0,lawful,lawful,Map.of(),null,playerHealing,null);
        }
        private void next(long revision,long nextSequence) {
            if(nextSequence!=Math.incrementExact(sequence)||revision<=Math.max(birthRevision,requestRevision))throw bad();
        }
        State denied(long revision,long nextSequence) {
            next(revision,nextSequence);
            return new State(identity,lanes,birthRevision,nextSequence,revision,initialLawful,lawful,receipts,structure,playerHealingEnabled,playerHealing);
        }
        /** before/after are captured under the same engine monitor. A success
         * must be precisely one cast, not a profile/regen/item/damage rewrite. */
        State cast(ModernEquipmentHistory.State before,ModernEquipmentHistory.State after,ModernUseHistory.Lane lane,
                long now,Detail detail) {
            validate(before);
            if(!ModernEquipmentHistory.followsCast(before,after,lane))throw bad();
            var batch=after.targets().get(lane).last();var use=batch.use();next(use.revision(),use.sequence());
            var latest=latestStamp();if(latest!=null&&now-latest.atNanos()<0)throw bad();
            long nextLawful=lawful;
            if(disintegrate(lane)) {
                if(!(detail instanceof Disintegrate d)||d.lawfulBefore()!=lawful)throw bad();nextLawful=d.lawfulAfter();
            }
            var ledger=after.spells().get(lane).current();var next=new EnumMap<ModernUseHistory.Lane,Receipt>(ModernUseHistory.Lane.class);
            next.putAll(receipts);next.put(lane,new Receipt(batch,ledger.casts(),ledger.spent(),now,detail));
            var retained=lane==ModernUseHistory.Lane.NPC_FIRE_STORM&&hasStructure(batch)
                    ?new Structure(ledger.casts(),batch,((NpcFireStorm)detail).questKind()):structure;
            var result=new State(identity,lanes,birthRevision,use.sequence(),use.revision(),initialLawful,nextLawful,next,retained,playerHealingEnabled,playerHealing);
            result.validate(after);return result;
        }
        /** Exact caster-side success. The engine must ALSO commit the target's
         * prepared resources/incoming event and recheck both captured actors. */
        State heal(ModernEquipmentHistory.State before,ModernEquipmentHistory.State after,
                ModernPlayerHealingTransaction.Receipt receipt,long now) {
            validate(before);
            if(!playerHealingEnabled||!receipt.world().equals(identity.world())||!receipt.source().equals(identity.source())
                    ||!receipt.caster().equals(identity.actor())||before.healing().outgoing()==null)throw bad();
            var use=receipt.use();next(use.revision(),use.sequence());
            var latest=latestStamp();if(latest!=null&&now-latest.atNanos()<0)throw bad();
            var resources=before.resources();var anchor=use.before();
            if(use.revision()!=Math.incrementExact(before.worldRevision())||anchor.life()!=Math.incrementExact(resources.life())
                    ||anchor.equipmentRevision()!=resources.equipmentRevision()||!anchor.vitals().equals(resources.vitals()))throw bad();
            var outgoing=before.healing().outgoing().cast(receipt);var incoming=before.healing().incoming();
            boolean self=receipt.impact().self();if(self)incoming=incoming.receive(receipt.impact());
            var balance=self?resources.restoreAndSpend(use.revision(),receipt.target().applied(),use.cost()):resources.spend(use.revision(),use.cost());
            var expected=new ModernEquipmentHistory.State(identity,use.revision(),balance,before.spells(),before.items(),before.targets(),
                    new ModernPlayerHealingHistory.Entry(incoming,outgoing));
            if(!expected.equals(after))throw bad();
            var ledger=outgoing.spending().current();
            var result=new State(identity,lanes,birthRevision,use.sequence(),use.revision(),initialLawful,lawful,receipts,structure,true,
                    new PlayerHealingReceipt(receipt,ledger.casts(),ledger.spent(),now));
            result.validate(after);return result;
        }
        void validate(ModernEquipmentHistory.State history) {
            if(!identity.equals(history.identity())||playerHealingEnabled!=(history.healing()!=null))throw bad();
            validateOrdinary(history.worldRevision(),history.spells(),history.targets());
            if(playerHealingEnabled) {
                var outgoing=history.healing().outgoing();
                if(outgoing==null||(outgoing.last()==null)!=(playerHealing==null))throw bad();
                if(playerHealing!=null&&(!playerHealing.receipt().equals(outgoing.last())
                        ||playerHealing.casts()!=outgoing.spending().current().casts()||playerHealing.spent()!=outgoing.spending().current().spent()))throw bad();
            }
        }
        /** Live legacy snapshots validate spell authority without constructing
         * an unrelated private item/resource envelope. Complete owner export
         * still requires the stricter EquipmentHistory constructor above. */
        void validate(long worldRevision,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> spells,
                Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets) {
            if(playerHealingEnabled)throw bad();
            validateOrdinary(worldRevision,spells,targets);
        }
        private void validateOrdinary(long worldRevision,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> spells,
                Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets) {
            if(!lanes.equals(spells.keySet())||!targets.keySet().equals(receipts.keySet())
                    ||worldRevision<Math.max(birthRevision,requestRevision))throw bad();
            var fire=targets.get(ModernUseHistory.Lane.NPC_FIRE_STORM);
            if(!Objects.equals(fire==null?null:fire.structure(),structure==null?null:structure.batch()))throw bad();
            for(var lane:lanes) {
                var spell=spells.get(lane);var receipt=receipts.get(lane);
                if(spell.last()==null) {if(receipt!=null)throw bad();}
                else if(receipt==null||!receipt.batch().use().equals(spell.last())||!receipt.batch().equals(targets.get(lane).last())
                        ||receipt.casts()!=spell.current().casts()||receipt.spent()!=spell.current().spent())throw bad();
            }
        }
        /** Old receipt-shaped observation cannot discard a player-healing cast. */
        Receipt latest() {
            if(playerHealingEnabled)throw bad();
            return receipts.values().stream().max(java.util.Comparator.comparingLong(r->r.batch().use().sequence())).orElse(null);
        }
        private Stamp latestStamp() {
            var ordinary=receipts.values().stream().max(java.util.Comparator.comparingLong(r->r.batch().use().sequence())).orElse(null);
            if(playerHealing!=null&&(ordinary==null||playerHealing.receipt().use().sequence()>ordinary.batch().use().sequence()))
                return new Stamp(playerHealing.receipt().use(),playerHealing.atNanos());
            return ordinary==null?null:new Stamp(ordinary.batch().use(),ordinary.atNanos());
        }
        boolean cooling(long now,long interval) {
            if(interval<0)throw bad();var last=latestStamp();return last!=null&&now-last.atNanos()<interval;
        }
        boolean healingCooling(long now,long interval) {
            if(interval<0||!playerHealingEnabled)throw bad();return playerHealing!=null&&now-playerHealing.atNanos()<interval;
        }
        boolean cooling(ModernUseHistory.Lane lane,long now,long interval) {
            if(interval<0)throw bad();var last=receipts.get(lane);return last!=null&&now-last.atNanos()<interval;
        }
    }
    /** Atomic owner-only projection. Does not expose another player's private
     * selection epoch and does not pretend to be the complete public world. */
    record Snapshot(ModernEquipmentHistory.State history,State spells) {
        Snapshot {Objects.requireNonNull(history);Objects.requireNonNull(spells);spells.validate(history);}
    }
    private static boolean disintegrate(ModernUseHistory.Lane lane) {
        return lane==ModernUseHistory.Lane.DISINTEGRATE||lane==ModernUseHistory.Lane.NPC_DISINTEGRATE;
    }
    private static boolean hasStructure(ModernTargetHistory.Batch batch) {
        return batch.targets().stream().anyMatch(t->t.before().target() instanceof ModernTargetHistory.Quest||t.before().target() instanceof ModernTargetHistory.TimeTower);
    }
    private static void checkStructureKind(ModernTargetHistory.Batch batch,ModernQuestObjectives.Kind kind) {
        var quest=batch.targets().stream().map(t->t.before().target()).filter(t->t instanceof ModernTargetHistory.Quest).map(t->(ModernTargetHistory.Quest)t).findFirst();
        if(quest.isPresent()?(kind==null||(kind!=ModernQuestObjectives.Kind.ROOM_SYMBOL)!=(quest.get().id().stage()==4)):kind!=null)throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid live spell runtime");}
    private ModernSpellRuntime(){}
}
