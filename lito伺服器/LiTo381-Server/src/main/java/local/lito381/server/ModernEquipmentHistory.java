package local.lito381.server;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A complete participant equipment/history candidate. Arithmetic and binding
 * checks are necessary, not sufficient authority to change a live world. The
 * old <=34 wire and advanced SET guards remain separate and strict. */
final class ModernEquipmentHistory {
    record Identity(ModernTargetHistory.World world,ModernRooms.Scope source,ModernTargetHistory.Participant actor) {
        Identity {
            Objects.requireNonNull(world);Objects.requireNonNull(source);Objects.requireNonNull(actor);
            if(!actor.identity().isHuman()||actor.identity().sourceId()!=source.sourceId()||actor.identity().id()!=source.objectId())throw bad();
        }
    }
    record Profile(long equipmentRevision,long maximumHp,long maximumMp,Map<ModernUseHistory.Lane,Integer> costs,Long fixedRecovery) {
        Profile {
            costs=Map.copyOf(costs);
            if(equipmentRevision<0||maximumHp<1||maximumMp<0||fixedRecovery!=null&&fixedRecovery<0
                    ||costs.values().stream().anyMatch(c->c<0))throw bad();
        }
    }
    record State(Identity identity,long worldRevision,ModernVitalBalance.State resources,
            Map<ModernUseHistory.Lane,ModernUseHistory.Spell> spells,ModernUseHistory.Items items,
            Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets,ModernPlayerHealingHistory.Entry healing) {
        State(Identity identity,long worldRevision,ModernVitalBalance.State resources,
                Map<ModernUseHistory.Lane,ModernUseHistory.Spell> spells,ModernUseHistory.Items items,
                Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets) {
            this(identity,worldRevision,resources,spells,items,targets,null);
        }
        State {
            Objects.requireNonNull(identity);Objects.requireNonNull(resources);spells=Map.copyOf(spells);targets=Map.copyOf(targets);
            if(worldRevision<resources.revision()||!spells.keySet().containsAll(targets.keySet()))throw bad();
            long spent=0,profileRevision=0;var revisions=new HashSet<Long>();var sequences=new HashSet<Long>();
            for(var entry:spells.entrySet()) {
                var lane=entry.getKey();var spell=entry.getValue();var ledger=spell.current();
                if(ledger.equipmentRevision()!=resources.equipmentRevision()||ledger.profileRevision()>resources.revision()
                        ||profileRevision!=0&&profileRevision!=ledger.profileRevision())throw bad();
                profileRevision=ledger.profileRevision();spent=Math.addExact(spent,ledger.spent());
                var target=targets.get(lane);
                if(spell.last()==null){if(target!=null)throw bad();continue;}
                var use=spell.last();checkAnchor(resources,use.before(),use.revision());
                if(!revisions.add(use.revision())||!sequences.add(use.sequence())||target==null||!target.last().use().equals(use))throw bad();
                var batch=target.last();
                if(!batch.world().equals(identity.world())||!batch.source().equals(identity.source())
                        ||!batch.caster().equals(identity.actor())||batch.lane()!=lane)throw bad();
            }
            long restored=0;
            if(healing!=null) {
                healing.check(identity,resources);restored=healing.incoming().restored();
                var outgoing=healing.outgoing();
                if(outgoing!=null) {
                    var ledger=outgoing.spending().current();
                    if(ledger.equipmentRevision()!=resources.equipmentRevision()||ledger.profileRevision()>resources.revision()
                            ||profileRevision!=0&&profileRevision!=ledger.profileRevision())throw bad();
                    profileRevision=ledger.profileRevision();spent=Math.addExact(spent,ledger.spent());
                    if(outgoing.last()!=null) {
                        var use=outgoing.last().use();checkAnchor(resources,use.before(),use.revision());
                        if(!revisions.add(use.revision())||!sequences.add(use.sequence()))throw bad();
                    }
                }
                var incoming=healing.incoming().last();
                if(incoming!=null) {
                    var sent=outgoing==null?null:outgoing.last();
                    boolean same=sent!=null&&sent.impact().equals(incoming);
                    if(!same&&!revisions.add(incoming.use().revision()))throw bad();
                    // Foreign caster sequences are actor-scoped, not ours.
                    if(incoming.self()&&!same&&!sequences.add(incoming.use().sequence()))throw bad();
                }
            }
            if(spent!=resources.flows().spent())throw bad();
            if(items!=null) {
                if(items.equipmentRevision()!=resources.equipmentRevision()||items.profileRevision()>resources.revision()
                        ||profileRevision!=0&&items.profileRevision()!=profileRevision)throw bad();
                restored=Math.addExact(restored,items.restored());
                if(items.last()!=null){checkAnchor(resources,items.last().before(),items.last().revision());
                    if(!revisions.add(items.last().revision()))throw bad();}
            }
            if(restored!=resources.flows().restored())throw bad();
        }
        Profile profile() {
            var costs=new EnumMap<ModernUseHistory.Lane,Integer>(ModernUseHistory.Lane.class);
            spells.forEach((lane,history)->costs.put(lane,history.current().cost()));
            return new Profile(resources.equipmentRevision(),resources.vitals().maximumHp(),resources.vitals().maximumMp(),costs,items==null?null:items.fixedRecovery());
        }
        State reprofile(long next,Profile profile) {
            return reprofile(next,profile,null);
        }
        /** Healing cost MUST come from the newly qualified equipment profile. */
        State reprofile(long next,Profile profile,Integer healingCost) {
            Objects.requireNonNull(profile);
            if(next<=worldRevision||!spells.keySet().equals(profile.costs().keySet())||(items==null)!=(profile.fixedRecovery()==null))throw bad();
            if(healing==null&&healingCost!=null)throw bad();
            var balance=resources.equip(next,profile.equipmentRevision(),profile.maximumHp(),profile.maximumMp());
            var histories=new EnumMap<ModernUseHistory.Lane,ModernUseHistory.Spell>(ModernUseHistory.Lane.class);
            spells.forEach((lane,history)->histories.put(lane,history.reprice(profile.equipmentRevision(),next,profile.costs().get(lane))));
            var supplies=items==null?null:items.reprofile(profile.equipmentRevision(),next,profile.fixedRecovery());
            // A price change cannot erase, rewrite or re-cost any earlier use
            // or its last structure-bearing target receipt.
            return new State(identity,next,balance,histories,supplies,targets,
                    healing==null?null:healing.reprice(profile.equipmentRevision(),next,healingCost));
        }
        /** One already-authorized cast. Target effects come from the server's
         * qualified combat calculation, never from a client damage claim. */
        State cast(ModernUseHistory.Lane lane,long next,long sequence,ModernWorldMap.Cell origin,
                List<ModernTargetHistory.Effect> effects) {
            Objects.requireNonNull(origin);var previous=spells.get(lane);
            if(previous==null||next<=worldRevision
                    ||healing!=null&&healing.outgoing()!=null&&sequence<=healing.outgoing().spending().current().lastCastSequence()
                    ||spells.values().stream().anyMatch(s->sequence<=s.current().lastCastSequence()))throw bad();
            var anchor=new ModernUseHistory.Anchor(resources.equipmentRevision(),Math.incrementExact(resources.life()),
                    resources.vitals(),origin.x(),origin.y());
            var spell=previous.cast(next,sequence,anchor);
            var balance=resources.spend(next,spell.last().cost());
            var batch=new ModernTargetHistory.Batch(identity.world(),identity.source(),identity.actor(),lane,spell.last(),effects);
            var histories=new EnumMap<ModernUseHistory.Lane,ModernUseHistory.Spell>(ModernUseHistory.Lane.class);
            histories.putAll(spells);histories.put(lane,spell);
            var receipts=new EnumMap<ModernUseHistory.Lane,ModernTargetHistory.History>(ModernUseHistory.Lane.class);
            receipts.putAll(targets);var previousTargets=targets.get(lane);
            receipts.put(lane,previousTargets==null?ModernTargetHistory.History.first(batch):previousTargets.commit(batch));
            return new State(identity,next,balance,histories,items,receipts,healing);
        }
    }
    static void checkAnchor(ModernVitalBalance.State resources,ModernUseHistory.Anchor anchor,long revision) {
        if(revision>resources.revision()||anchor.life()>Math.incrementExact(resources.life())
                ||anchor.equipmentRevision()>resources.equipmentRevision()
                ||anchor.equipmentRevision()==resources.equipmentRevision()
                    &&(anchor.vitals().maximumHp()!=resources.vitals().maximumHp()||anchor.vitals().maximumMp()!=resources.vitals().maximumMp()))throw bad();
    }
    static boolean followsReprofile(State before,State after) {
        if(before==null||after==null)return false;
        try{return before.reprofile(after.worldRevision(),after.profile(),after.healing()!=null&&after.healing().outgoing()!=null
                ?after.healing().outgoing().spending().current().cost():null).equals(after);}
        catch(ArithmeticException|IllegalArgumentException rejected){return false;}
    }
    static boolean followsCast(State before,State after,ModernUseHistory.Lane lane) {
        if(before==null||after==null||lane==null)return false;
        var receipt=after.targets().get(lane);if(receipt==null)return false;
        var use=receipt.last().use();
        try{return before.cast(lane,after.worldRevision(),use.sequence(),
                new ModernWorldMap.Cell(use.before().x(),use.before().y()),receipt.last().targets()).equals(after);}
        catch(ArithmeticException|IllegalArgumentException rejected){return false;}
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid complete equipment history");}
    private ModernEquipmentHistory(){ }
}
