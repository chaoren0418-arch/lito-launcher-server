package local.lito381.server;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Force-owned symbols/guardians, never NPC serials, room members or client
 * damage claims. Configuration is explicit shared geometry and i64 HP; no LT
 * placement or nominal event-server tuning is inferred from the artwork. */
final class ModernQuestObjectives {
    enum Kind { ROOM_SYMBOL, FINAL_SYMBOL, FINAL_GUARDIAN }
    record Id(ModernNpcRoster.Scope scope,int slot) {
        Id {Objects.requireNonNull(scope);if(slot<1||slot>8)throw bad();}
        int lane(){return (slot-1)/4;}
        int stage(){return (slot-1)%4+1;}
    }
    record Target(int lane,int stage,ModernWorldMap.Cell cell,int graphic,long maximumHp) {
        Target {
            Objects.requireNonNull(cell);
            if(lane<0||lane>1||stage<1||stage>4||!List.of(2665,2667,2669,2671).contains(graphic)||maximumHp<1)throw bad();
        }
        int slot(){return lane*4+stage;}
    }
    record Config(List<Target> targets) {
        Config {
            targets=List.copyOf(targets);if(targets.size()!=2&&targets.size()!=8)throw bad();
            int previous=0;for(var t:targets){if(t.slot()<=previous)throw bad();previous=t.slot();}
        }
        Target target(int lane,int stage){return targets.stream().filter(t->t.lane()==lane&&t.stage()==stage).findFirst().orElseThrow(ModernQuestObjectives::bad);}
        void validate(ModernQuestProgress.Plan plan) {
            boolean symbols=plan.flow()==ModernQuestProgress.Flow.ROOM_SYMBOLS;
            if(targets.size()!=(symbols?8:2))throw bad();
            for(int i=0;i<targets.size();i++)if(targets.get(i).slot()!=(symbols?i+1:(i+1)*4))throw bad();
        }
        void qualify(ModernQuestProgress.Plan plan,ModernWorldBinding binding,List<ModernWorldMap.Cell> anchors) {
            validate(plan);if(!plan.key().equals(binding.key())||anchors.size()!=2)throw bad();
            for(var target:targets) {
                var anchor=anchors.get(target.lane());if(anchor.equals(target.cell()))throw bad();
                try{binding.map().validateSpawns(List.of(anchor,target.cell()));}
                catch(ModernWorldMap.MapException failure){throw bad();}
            }
        }
        List<Integer> graphics(){return targets.stream().map(Target::graphic).distinct().sorted().toList();}
    }
    record Actor(Id id,Target target,Kind kind,long hp,long bornRevision,long revision,long hurtRevision,long deathRevision) {
        Actor {
            Objects.requireNonNull(id);Objects.requireNonNull(target);Objects.requireNonNull(kind);
            if(id.slot()!=target.slot()||hp<0||hp>target.maximumHp()||bornRevision<1||revision<bornRevision
                    ||hurtRevision<0||hurtRevision>revision||hurtRevision>0&&hurtRevision<=bornRevision
                    ||deathRevision<0||deathRevision>revision||(hp==0)!=(deathRevision>0)
                    ||deathRevision>0&&deathRevision!=hurtRevision
                    ||revision==bornRevision&&(hp!=target.maximumHp()||hurtRevision!=0)
                    ||(hp<target.maximumHp())!=(hurtRevision>0)
                    ||(kind!=Kind.ROOM_SYMBOL)!=(target.stage()==4))throw bad();
        }
        int action(){return ModernHonor.action(hp,target.maximumHp());}
        boolean alive(){return hp>0;}
        ModernWorldMap.Cell cell(){return target.cell();}
        static Actor born(ModernQuestProgress.State progress,int lane,long revision) {
            var current=progress.lanes().get(lane);var plan=progress.plan();
            if(progress.status()!=ModernQuestProgress.Status.RUNNING||current.phase()!=ModernQuestProgress.Phase.OBJECTIVE||plan.objectives()==null)throw bad();
            var t=plan.objectives().target(lane,current.stage());
            var kind=plan.flow()==ModernQuestProgress.Flow.FINAL_GUARDIAN?Kind.FINAL_GUARDIAN:current.stage()==4?Kind.FINAL_SYMBOL:Kind.ROOM_SYMBOL;
            return new Actor(new Id(progress.scope(),t.slot()),t,kind,t.maximumHp(),revision,revision,0,0);
        }
        Actor damaged(ModernPhysicalCombat.Outcome hit,long next) {
            if(!alive()||next<=revision||hit.appliedDamage()>hp||hit.remainingHp()!=hp-hit.appliedDamage()||hit.death()!=(hit.remainingHp()==0))throw bad();
            return new Actor(id,target,kind,hit.remainingHp(),bornRevision,next,hit.appliedDamage()>0?next:hurtRevision,hit.death()?next:0);
        }
        Actor damaged(ModernFireStorm.Outcome hit,long next) {
            if(!alive()||next<=revision||hit.appliedDamage()>hp||hit.remainingHp()!=hp-hit.appliedDamage()||hit.death()!=(hit.remainingHp()==0))throw bad();
            return new Actor(id,target,kind,hit.remainingHp(),bornRevision,next,hit.appliedDamage()>0?next:hurtRevision,hit.death()?next:0);
        }
    }
    record Exchange(ModernParticipantId participant,Id target,long requestSequence,long revision,ModernPhysicalCombat.Outcome outcome) {
        Exchange {
            Objects.requireNonNull(participant);Objects.requireNonNull(target);Objects.requireNonNull(outcome);
            if(!participant.isHuman()||requestSequence<1||revision<1||(outcome.remainingHp()==0)!=outcome.death())throw bad();
        }
    }
    /** Latest objective per force and receipt per attacking human. Retained
     * ruins do not block later waves; a later objective replaces its own ruin. */
    record Snapshot(ModernQuestProgress.State progress,List<Actor> actors,List<Exchange> exchanges,List<ModernQuestLife.State> lives) {
        Snapshot(ModernQuestProgress.State progress,List<Actor> actors,List<Exchange> exchanges){this(progress,actors,exchanges,List.of());}
        Snapshot {
            actors=List.copyOf(actors);exchanges=List.copyOf(exchanges);lives=List.copyOf(lives);
            if(progress==null){if(!actors.isEmpty()||!exchanges.isEmpty()||!lives.isEmpty())throw bad();}
            else {
                if(progress.plan().participantLives()!=!lives.isEmpty()||lives.size()>100)throw bad();
                for(int i=0;i<lives.size();i++)if(lives.get(i).entityId()!=i+1)throw bad();
                var config=progress.plan().objectives();if(config==null||actors.size()>2||exchanges.size()>100)throw bad();
                var lanes=new HashSet<Integer>();int previous=0;
                for(var a:actors) {
                    var lane=progress.lanes().get(a.id().lane());
                    if(!a.id().scope().equals(progress.scope())||a.id().slot()<=previous||!lanes.add(a.id().lane())
                            ||!a.target().equals(config.target(a.id().lane(),a.id().stage()))||a.id().stage()>lane.stage()
                            ||a.alive()&&(a.id().stage()!=lane.stage()||lane.phase()!=ModernQuestProgress.Phase.OBJECTIVE)
                            ||!a.alive()&&(a.id().stage()==lane.stage()
                                ?progress.status()!=ModernQuestProgress.Status.FINISHED||progress.winningLane()!=a.id().lane()||a.id().stage()!=4
                                :a.id().stage()!=lane.stage()-1||a.kind()!=Kind.ROOM_SYMBOL)
                            ||a.kind()!=(progress.plan().flow()==ModernQuestProgress.Flow.FINAL_GUARDIAN?Kind.FINAL_GUARDIAN:a.id().stage()==4?Kind.FINAL_SYMBOL:Kind.ROOM_SYMBOL))throw bad();
                    previous=a.id().slot();
                }
                var players=new HashSet<ModernParticipantId>();long revision=0;
                for(var e:exchanges) {
                    if(!e.target().scope().equals(progress.scope())||!players.add(e.participant())||e.revision()<=revision)throw bad();
                    var target=config.target(e.target().lane(),e.target().stage());
                    long priorHp;
                    try{priorHp=Math.addExact(e.outcome().remainingHp(),e.outcome().appliedDamage());}
                    catch(ArithmeticException failure){throw bad();}
                    if(priorHp<1||priorHp>target.maximumHp())throw bad();
                    revision=e.revision();
                }
                if(progress.status()==ModernQuestProgress.Status.FINISHED&&actors.stream().noneMatch(a->a.id().lane()==progress.winningLane()&&a.id().stage()==4&&!a.alive()))throw bad();
            }
        }
        static Snapshot disabled(){return new Snapshot(null,List.of(),List.of());}
        boolean enabled(){return progress!=null;}
        Actor actor(Id id){return actors.stream().filter(a->a.id().equals(id)).findFirst().orElse(null);}
        void validate(long room,long match,long generation,long revision,ModernBattleEngine.Phase phase,
                List<ModernBattleEngine.Actor> players,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result) {
            validate(room,match,generation,revision,phase,players,vitals,result,List.of());
        }
        void validate(long room,long match,long generation,long revision,ModernBattleEngine.Phase phase,
                List<ModernBattleEngine.Actor> players,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernNpcFireStormState.State> fireStorms) {
            if(!enabled()){if(result!=null&&result.mode()==2)throw bad();return;}
            if(!lives.isEmpty()) {
                if(lives.size()!=players.size())throw bad();
                for(int i=0;i<lives.size();i++) {
                    var life=lives.get(i);var player=players.get(i);var vital=vitals.get(i);
                    if(life.dead()!=(vital.hp()==0)||life.deathRevision()!=vital.deathRevision()||life.deathRevision()>revision||life.respawnRevision()>revision
                            ||life.respawnRevision()==revision&&(vital.hp()!=vital.maxHp()||!player.position().equals(life.spawn()))
                            ||life.deathRevision()==revision&&!player.position().equals(life.deathCell())
                            ||(phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN)&&life.deaths()!=0)throw bad();
                }
            }
            if(!progress.scope().equals(new ModernNpcRoster.Scope(room,match,generation))
                    ||(phase==ModernBattleEngine.Phase.FINISHED)!=(progress.status()==ModernQuestProgress.Status.FINISHED)
                    ||(phase==ModernBattleEngine.Phase.ABORTED)!=(progress.status()==ModernQuestProgress.Status.ABORTED)
                    ||(phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN)&&(!actors.isEmpty()||!exchanges.isEmpty()))throw bad();
            for(var a:actors)if(a.revision()>revision||a.alive()&&players.stream().anyMatch(p->p.member().team()==a.id().lane()&&p.position().equals(a.cell())))throw bad();
            for(var e:exchanges) {
                var p=players.stream().filter(a->a.member().identity().equals(e.participant())).findFirst().orElseThrow(ModernQuestObjectives::bad);
                var v=vitals.stream().filter(a->a.entityId()==p.entityId()).findFirst().orElseThrow(ModernQuestObjectives::bad);
                var target=actor(e.target());
                if(e.target().lane()!=p.member().team()||e.revision()>revision||e.requestSequence()>v.attackSequence()
                        ||target!=null&&(e.revision()<=target.bornRevision()||e.revision()>target.revision()
                            ||e.outcome().remainingHp()<target.hp()
                            ||e.revision()==target.revision()&&(target.hp()!=e.outcome().remainingHp()
                                ||target.alive()==e.outcome().death()
                                ||e.outcome().appliedDamage()>0&&target.hurtRevision()!=e.revision()))
                        ||e.revision()==revision&&(target==null||target.hp()!=e.outcome().remainingHp()||v.hp()==0||v.attackSequence()!=e.requestSequence()))throw bad();
            }
            if(phase==ModernBattleEngine.Phase.FINISHED) {
                if(result==null||result.mode()!=2||result.engineRevision()!=revision)throw bad();
                var winner=actors.stream().filter(a->a.id().lane()==progress.winningLane()).findFirst().orElseThrow(ModernQuestObjectives::bad);
                if(winner.alive()||winner.id().stage()!=4||winner.deathRevision()!=revision)throw bad();
                if(exchanges.stream().noneMatch(e->e.target().equals(winner.id())&&e.revision()==revision&&e.outcome().death())
                        &&!ModernNpcFireStormStructures.questDestroyed(fireStorms,winner.id(),revision))throw bad();
                for(var p:result.participants())if((p.verdict()==ModernMatchSettlement.Verdict.WIN)!=(p.team()==progress.winningLane()))throw bad();
            }
        }
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid Quest objective authority");}
    private ModernQuestObjectives(){}
}
