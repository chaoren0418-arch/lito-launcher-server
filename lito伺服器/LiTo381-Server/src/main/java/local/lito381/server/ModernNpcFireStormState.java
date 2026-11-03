package local.lito381.server;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Typed NPC AoE ledger; v31 admission remains explicit and owner-scoped. */
final class ModernNpcFireStormState {
    enum Reason { NONE,NOT_ACTIVE,DEAD,UNLEARNED,UNSUPPORTED,TOO_FAST,NO_MP,TOO_HEAVY,NUMERIC,STALE_STATE,STALE_REGEN }
    record Request(ModernRooms.Scope player,ModernNpcRoster.Scope world,long sequence,long casterLife) {
        Request {Objects.requireNonNull(player);Objects.requireNonNull(world);if(sequence<1||casterLife<1)throw bad();}
    }
    record Target(ModernNpcRoster.Id id,long life,int lane,ModernWorldMap.Cell cell,long hpBefore,ModernFireStorm.Outcome outcome) {
        Target {
            Objects.requireNonNull(id);Objects.requireNonNull(cell);Objects.requireNonNull(outcome);
            if(life<1||lane<0||lane>1||hpBefore<1||outcome.appliedDamage()!=Math.min(hpBefore,outcome.rawDamage())
                    ||outcome.remainingHp()!=hpBefore-outcome.appliedDamage())throw bad();
        }
        static Target from(ModernNpcFireStorm.Hit hit){var a=hit.before();return new Target(a.id(),a.life(),a.lane(),a.cell(),a.hp(),hit.outcome());}
    }
    record State(long entityId,long castCount,long revision,long lastSequence,long mpSpent,int mpCost,
                 long casterLife,ModernNpcRoster.Scope scope,ModernWorldMap.Cell origin,int lane,List<Target> targets,ModernNpcFireStormStructures.Receipt structure) {
        State(long entityId,long castCount,long revision,long lastSequence,long mpSpent,int mpCost,
                long casterLife,ModernNpcRoster.Scope scope,ModernWorldMap.Cell origin,int lane,List<Target> targets) {
            this(entityId,castCount,revision,lastSequence,mpSpent,mpCost,casterLife,scope,origin,lane,targets,null);
        }
        State {
            targets=List.copyOf(targets);
            if(entityId<1||castCount<0||revision<castCount||lastSequence<castCount||mpSpent<0||mpCost!=0&&(mpCost<39||mpCost>48)
                    ||mpSpent!=Math.multiplyExact(castCount,mpCost)||targets.size()>ModernNpcFireStorm.MAX_TARGETS)throw bad();
            if(castCount==0) {
                if(revision!=0||lastSequence!=0||casterLife!=0||scope!=null||origin!=null||lane!=-1||!targets.isEmpty()||structure!=null)throw bad();
            } else {
                if(revision<1||mpCost==0||casterLife<1||scope==null||origin==null||lane<0||lane>1)throw bad();
                long previous=0;
                for(var t:targets) {
                    long distance=ModernNpcFireStorm.distance(origin,t.cell());
                    if(!t.id().scope().equals(scope)||t.id().serial()<=previous||t.lane()!=lane||distance<1||distance>ModernFireStorm.RADIUS)throw bad();
                    previous=t.id().serial();
                }
                if(structure!=null) {
                    var e=structure;var h=e.hit();boolean latest=e.castCount()==castCount;
                    if(e.castCount()>castCount||e.revision()>revision||e.sequence()>lastSequence||e.casterLife()>casterLife
                            ||!h.scope().equals(scope)||h.lane()!=lane
                            ||latest&&(e.revision()!=revision||e.sequence()!=lastSequence||e.casterLife()!=casterLife||!e.origin().equals(origin)
                                ||targets.size()+1>ModernNpcFireStorm.MAX_TARGETS||targets.stream().anyMatch(t->t.cell().equals(h.cell())))
                            ||!latest&&(e.revision()>=revision||e.sequence()>=lastSequence
                                ||castCount-e.castCount()>revision-e.revision()||castCount-e.castCount()>lastSequence-e.sequence()))throw bad();
                }
            }
        }
        static State initial(long entity,int cost){return new State(entity,0,0,0,0,cost,0,null,null,-1,List.of());}
        /** Engine -> world-publication revision space, exactly once. */
        State wireRevision() {
            return castCount==0?this:new State(entityId,castCount,Math.incrementExact(revision),lastSequence,mpSpent,mpCost,
                    casterLife,scope,origin,lane,targets,structure==null?null:structure.wireRevision());
        }
        State cast(long next,long sequence,long life,ModernNpcFireStorm.Incoming batch) {
            return cast(next,sequence,life,batch,null);
        }
        State cast(long next,long sequence,long life,ModernNpcFireStorm.Incoming batch,ModernNpcFireStormStructures.Hit hit) {
            if(next<=revision||sequence<=lastSequence||batch.mpCost()!=mpCost)throw bad();var spending=ModernSkillSpending.advance(castCount,mpSpent,mpCost);
            return new State(entityId,spending.casts(),next,sequence,spending.spent(),mpCost,life,batch.scope(),batch.origin(),batch.lane(),
                    batch.hits().stream().map(Target::from).toList(),hit==null?structure:new ModernNpcFireStormStructures.Receipt(spending.casts(),next,sequence,life,batch.origin(),hit));
        }
    }
    record Result(Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        Result {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
        ModernNpcBattle.Frame frame(){return ModernLegacyBattleProjection.npcs(observation);}
    }
    static void validate(long match,long generation,long revision,ModernBattleEngine.Phase phase,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest map,List<ModernBattleEngine.Actor> actors,
            List<ModernCombatProtocol.Vital> vitals,List<ModernSpellProtocol.CastState> casts,List<ModernItemProtocol.State> items,
            List<ModernRegenProtocol.State> regen,ModernQuestObjectives.Snapshot quest,ModernTimeAttack.Snapshot timeAttack,
            List<ModernNpcMagic.CastState> arrows,List<ModernNpcDisintegrateState.State> disintegrations,List<State> states) {
        if(states.isEmpty())return;int n=actors.size();
        if(states.size()!=n||vitals.size()!=n||casts.size()!=n||regen.size()!=n
                ||room.summary().config().mode()!=(timeAttack.enabled()?4:2)
                ||!arrows.isEmpty()&&arrows.size()!=n||!disintegrations.isEmpty()&&disintegrations.size()!=n)throw bad();
        var occupied=new HashSet<Long>();var used=new HashSet<Long>();
        for(var c:casts)occupied.add(c.castRevision());
        for(var a:arrows)occupied.add(a.revision());
        for(var d:disintegrations)occupied.add(d.revision());
        for(var i:items)occupied.add(i.useRevision());
        for(var v:vitals){occupied.add(v.attackRevision());occupied.add(v.hurtRevision());occupied.add(v.deathRevision());}
        for(var r:regen){occupied.add(r.hpRevision());occupied.add(r.mpRevision());}
        for(var l:quest.lives()){occupied.add(l.deathRevision());occupied.add(l.respawnRevision());}
        for(var e:quest.exchanges())occupied.add(e.revision());
        if(timeAttack.enabled())for(var e:timeAttack.exchanges())occupied.add(e.revision());
        for(int i=0;i<n;i++) {
            var s=states.get(i);var a=actors.get(i);var v=vitals.get(i);var c=casts.get(i);
            long count=Math.addExact(c.castCount(),s.castCount());
            count=Math.addExact(count,arrows.isEmpty()?0:arrows.get(i).castCount());
            count=Math.addExact(count,disintegrations.isEmpty()?0:disintegrations.get(i).castCount());
            if(s.entityId()!=a.entityId()||s.revision()>revision||s.lastSequence()>c.requestSequence()||count>c.requestSequence()
                    ||(a.member().identity().isHuman()?s.mpCost()==0:!s.equals(State.initial(s.entityId(),0)))
                    ||(phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN)&&s.castCount()>0)throw bad();
            if(s.castCount()==0)continue;
            var life=quest.lives().isEmpty()?null:quest.lives().get(i);long incarnation=life==null?1:life.incarnation();
            if(!used.add(s.revision())||occupied.contains(s.revision())
                    ||s.scope().roomId()!=room.summary().id()||s.scope().matchId()!=match||s.scope().generation()!=generation
                    ||s.lane()!=a.member().team()||!ModernWorldProtocol.contains(map,s.origin())
                    ||s.casterLife()>incarnation||life!=null&&(s.casterLife()==incarnation?s.revision()<=life.respawnRevision():s.revision()>life.respawnRevision())
                    ||s.casterLife()==incarnation&&v.hp()==0&&s.revision()>=v.deathRevision()
                    ||s.targets().stream().anyMatch(t->!ModernWorldProtocol.contains(map,t.cell()))
                    ||s.revision()==revision&&(v.hp()==0||!a.position().equals(s.origin())||c.requestSequence()!=s.lastSequence()
                        ||s.casterLife()!=incarnation||timeAttack.enabled()&&timeAttack.state().departed().contains(s.entityId())))throw bad();
        }
        ModernNpcFireStormStructures.validate(match,generation,revision,phase,room,map,actors,vitals,casts,quest,timeAttack,states,occupied);
    }
    static void validateNpcs(ModernBattleEngine.Snapshot world,ModernNpcRoster.Snapshot npcs) {
        for(var s:world.npcFireStorms())for(var h:s.targets()) {
            if(!h.id().scope().equals(npcs.scope())||h.id().serial()>npcs.spawned())throw bad();
            var target=npcs.actors().stream().filter(a->a.id().equals(h.id())).findFirst().orElse(null);
            if(target!=null&&(target.lane()!=h.lane()||target.life()<h.life()||h.hpBefore()>target.template().maximumHp()
                    ||target.life()==h.life()&&h.outcome().death()&&target.alive()))throw bad();
            if(s.revision()==world.revision()&&(target==null||target.life()!=h.life()||!target.cell().equals(h.cell())
                    ||target.hp()!=h.outcome().remainingHp()||h.outcome().appliedDamage()>0
                        &&target.action()!=(h.outcome().death()?ModernNpcRoster.Action.DEATH:ModernNpcRoster.Action.HURT)))throw bad();
        }
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC fire storm ledger");}
    private ModernNpcFireStormState(){}
}
