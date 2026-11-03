package local.lito381.server;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Immutable event-time target health. Never reconstructs a past life from
 * today's capacity. Internal dual recording only, not a negotiated packet. */
final class ModernTargetHistory {
    record World(long roomId,long matchId,long generation) {
        World {if(roomId<1||matchId<1||generation<1)throw bad();}
        boolean contains(ModernNpcRoster.Scope scope) {
            return scope!=null&&roomId==scope.roomId()&&matchId==scope.matchId()&&generation==scope.generation();
        }
    }
    sealed interface Target permits Participant,Npc,Honor,Quest,TimeTower { }
    record Participant(long entityId,ModernParticipantId identity) implements Target {
        Participant {Objects.requireNonNull(identity);if(entityId<1||entityId>100)throw bad();}
    }
    record Npc(ModernNpcRoster.Id id) implements Target {Npc {Objects.requireNonNull(id);}}
    record Honor(long entityId,int team) implements Target {Honor {if(entityId<3||entityId>102||team<0||team>1)throw bad();}}
    record Quest(ModernQuestObjectives.Id id,long bornRevision) implements Target {
        Quest {Objects.requireNonNull(id);if(bornRevision<1)throw bad();}
    }
    record TimeTower(ModernTimeAttack.TowerId id) implements Target {TimeTower {Objects.requireNonNull(id);}}
    record Health(Target target,long life,long equipmentRevision,ModernWorldMap.Cell cell,long hp,long maximumHp) {
        Health {
            Objects.requireNonNull(target);Objects.requireNonNull(cell);
            if(life<1||equipmentRevision<0||maximumHp<1||hp<1||hp>maximumHp
                    ||!(target instanceof Participant)&&equipmentRevision!=0
                    ||!(target instanceof Participant)&&!(target instanceof Npc)&&life!=1)throw bad();
        }
    }
    enum Kind { DAMAGE, HEAL }
    record Effect(Kind kind,Health before,long raw,long applied,long hpAfter) {
        Effect {
            Objects.requireNonNull(kind);Objects.requireNonNull(before);
            if(raw<0||applied<0)throw bad();
            long expected=kind==Kind.DAMAGE?Math.min(raw,before.hp()):Math.min(raw,before.maximumHp()-before.hp());
            if(applied!=expected||hpAfter!=(kind==Kind.DAMAGE?before.hp()-expected:Math.addExact(before.hp(),expected)))throw bad();
        }
        boolean death(){return kind==Kind.DAMAGE&&hpAfter==0;}
    }
    record Batch(World world,ModernRooms.Scope source,Participant caster,ModernUseHistory.Lane lane,
            ModernUseHistory.Cast use,List<Effect> targets) {
        Batch {
            Objects.requireNonNull(world);Objects.requireNonNull(source);Objects.requireNonNull(caster);
            Objects.requireNonNull(lane);Objects.requireNonNull(use);targets=List.copyOf(targets);
            if(!caster.identity().isHuman()||caster.identity().sourceId()!=source.sourceId()||caster.identity().id()!=source.objectId())throw bad();
            int maximum=switch(lane){case TELEPORT,GREATER_HASTE,HASTE,SLOW->0;case FIRE_STORM->101;case NPC_FIRE_STORM->48;default->1;};
            if(targets.size()>maximum||maximum==1&&targets.size()!=1)throw bad();
            var unique=new HashSet<Target>();var entities=new HashSet<Long>();var participants=new HashSet<ModernParticipantId>();
            int structures=0;
            for(var effect:targets) {
                var t=effect.before().target();
                if(!unique.add(t)||effect.kind()!=(lane==ModernUseHistory.Lane.HEALING?Kind.HEAL:Kind.DAMAGE))throw bad();
                boolean admitted=switch(lane) {
                    case TELEPORT,GREATER_HASTE,HASTE,SLOW->false;
                    case LIGHT_ARROW,FIRE_STORM->t instanceof Participant||t instanceof Honor;
                    case HEALING->t instanceof Honor;
                    case DISINTEGRATE->t instanceof Participant;
                    case NPC_LIGHT_ARROW,NPC_DISINTEGRATE->t instanceof Npc;
                    case NPC_FIRE_STORM->t instanceof Npc||t instanceof Quest||t instanceof TimeTower;
                };
                if(!admitted)throw bad();
                if(t instanceof Participant p) {
                    if(p.entityId()==caster.entityId()||p.identity().equals(caster.identity())
                            ||!entities.add(p.entityId())||!participants.add(p.identity()))throw bad();
                } else if(t instanceof Honor h) {
                    if(h.entityId()==caster.entityId()||!entities.add(h.entityId()))throw bad();
                } else if(t instanceof Npc n) {if(!world.contains(n.id().scope()))throw bad();}
                else if(t instanceof Quest q) {if(!world.contains(q.id().scope())||q.bornRevision()>=use.revision())throw bad();structures++;}
                else if(t instanceof TimeTower t0) {if(!world.contains(t0.id().scope()))throw bad();structures++;}
            }
            if(structures>1)throw bad();
        }
        boolean hasStructure(){return targets.stream().anyMatch(t->t.before().target() instanceof Quest||t.before().target() instanceof TimeTower);}
    }
    /** Keep the last structure-bearing event across later NPC-only/empty AoE
     * casts, so a destroyed objective is not erased by a subsequent animation. */
    record History(Batch last,Batch structure) {
        History {
            Objects.requireNonNull(last);
            if(last.hasStructure()) {if(!last.equals(structure))throw bad();}
            else if(structure!=null&&(!structure.hasStructure()||!sameAuthority(last,structure)
                    ||structure.use().revision()>=last.use().revision()||structure.use().sequence()>=last.use().sequence()
                    ||structure.use().before().life()>last.use().before().life()
                    ||structure.use().before().equipmentRevision()>last.use().before().equipmentRevision()))throw bad();
        }
        static History first(Batch batch){return new History(batch,batch.hasStructure()?batch:null);}
        History commit(Batch next) {
            if(!sameAuthority(last,next)||next.use().revision()<=last.use().revision()||next.use().sequence()<=last.use().sequence()
                    ||next.use().before().life()<last.use().before().life()
                    ||next.use().before().equipmentRevision()<last.use().before().equipmentRevision())throw bad();
            for(var before:last.targets())for(var after:next.targets()) {
                var a=before.before();var b=after.before();
                if(a.target().equals(b.target())&&(b.life()<a.life()||b.equipmentRevision()<a.equipmentRevision()))throw bad();
                if(a.target() instanceof Participant p&&b.target() instanceof Participant q
                        &&(p.entityId()==q.entityId()&&!p.identity().equals(q.identity())
                            ||p.identity().equals(q.identity())&&p.entityId()!=q.entityId()))throw bad();
                if(entity(a.target())!=0&&entity(a.target())==entity(b.target())&&a.target().getClass()!=b.target().getClass())throw bad();
            }
            return new History(next,next.hasStructure()?next:structure);
        }
    }
    static boolean follows(History before,History after) {
        if(before==null||after==null)return false;
        if(before.equals(after))return true;
        try{return before.commit(after.last()).equals(after);}catch(IllegalArgumentException rejected){return false;}
    }
    private static boolean sameAuthority(Batch a,Batch b) {
        return a.world().equals(b.world())&&a.source().equals(b.source())&&a.caster().equals(b.caster())&&a.lane()==b.lane();
    }
    private static long entity(Target target){return target instanceof Participant p?p.entityId():target instanceof Honor h?h.entityId():0;}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid historical target health");}
    private ModernTargetHistory(){ }
}
