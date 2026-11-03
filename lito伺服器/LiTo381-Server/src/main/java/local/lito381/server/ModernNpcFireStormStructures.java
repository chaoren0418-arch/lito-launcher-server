package local.lito381.server;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/** Internal skill70 structure proofs. Neither Quest slots nor the cooperative
 * start tower may become an NPC serial, participant ID or melee exchange. */
final class ModernNpcFireStormStructures {
    sealed interface Hit permits QuestHit,TowerHit {
        ModernNpcRoster.Scope scope();
        int lane();
        ModernWorldMap.Cell cell();
        long maximumHp();
        long hpBefore();
        ModernFireStorm.Outcome outcome();
    }
    record QuestHit(ModernQuestObjectives.Id id,ModernQuestObjectives.Kind kind,ModernWorldMap.Cell cell,
                    long maximumHp,long hpBefore,long bornRevision,ModernFireStorm.Outcome outcome) implements Hit {
        QuestHit {
            Objects.requireNonNull(id);Objects.requireNonNull(kind);Objects.requireNonNull(cell);
            check(maximumHp,hpBefore,outcome);
            if(bornRevision<1||(kind!=ModernQuestObjectives.Kind.ROOM_SYMBOL)!=(id.stage()==4))throw bad();
        }
        QuestHit(ModernQuestObjectives.Actor before,ModernFireStorm.Outcome outcome) {
            this(before.id(),before.kind(),before.cell(),before.target().maximumHp(),before.hp(),before.bornRevision(),outcome);
        }
        public ModernNpcRoster.Scope scope(){return id.scope();}
        public int lane(){return id.lane();}
    }
    record TowerHit(ModernTimeAttack.TowerId id,ModernWorldMap.Cell cell,long maximumHp,long hpBefore,
                    ModernFireStorm.Outcome outcome) implements Hit {
        TowerHit {Objects.requireNonNull(id);Objects.requireNonNull(cell);check(maximumHp,hpBefore,outcome);}
        TowerHit(ModernTimeAttack.State before,ModernFireStorm.Outcome outcome) {
            this(new ModernTimeAttack.TowerId(before.scope()),before.plan().tower().cell(),before.plan().tower().maximumHp(),before.towerHp(),outcome);
        }
        public ModernNpcRoster.Scope scope(){return id.scope();}
        public int lane(){return 0;}
    }
    /** Last STRUCTURE hit, retained across subsequent NPC-only casts so the
     * tower's real destruction remains provable through the final wave. */
    record Receipt(long castCount,long revision,long sequence,long casterLife,ModernWorldMap.Cell origin,Hit hit) {
        Receipt {
            Objects.requireNonNull(origin);Objects.requireNonNull(hit);
            long distance=ModernNpcFireStorm.distance(origin,hit.cell());
            if(castCount<1||revision<castCount||sequence<castCount||casterLife<1||distance<1||distance>ModernFireStorm.RADIUS
                    ||hit instanceof QuestHit q&&revision<=q.bornRevision())throw bad();
        }
        Receipt wireRevision() {
            Hit projected=hit instanceof QuestHit q?new QuestHit(q.id(),q.kind(),q.cell(),q.maximumHp(),q.hpBefore(),Math.incrementExact(q.bornRevision()),q.outcome()):hit;
            return new Receipt(castCount,Math.incrementExact(revision),sequence,casterLife,origin,projected);
        }
    }
    static void validate(long match,long generation,long revision,ModernBattleEngine.Phase phase,ModernRooms.RoomSnapshot room,
            ModernWorldMap.Manifest map,List<ModernBattleEngine.Actor> actors,List<ModernCombatProtocol.Vital> vitals,
            List<ModernSpellProtocol.CastState> casts,ModernQuestObjectives.Snapshot quest,ModernTimeAttack.Snapshot timeAttack,
            List<ModernNpcFireStormState.State> states,java.util.Set<Long> occupied) {
        var owners=new HashMap<Long,Long>();
        for(var s:states)if(s.castCount()>0)owners.put(s.revision(),s.entityId());
        for(int i=0;i<states.size();i++) {
            var s=states.get(i);var e=s.structure();if(e==null)continue;
            var a=actors.get(i);var v=vitals.get(i);var hit=e.hit();var scope=hit.scope();
            var other=owners.putIfAbsent(e.revision(),s.entityId());
            var life=quest.lives().isEmpty()?null:quest.lives().get(i);long incarnation=life==null?1:life.incarnation();
            if(other!=null&&other!=s.entityId()||occupied.contains(e.revision())||!a.member().identity().isHuman()
                    ||scope.roomId()!=room.summary().id()||scope.matchId()!=match||scope.generation()!=generation
                    ||hit.lane()!=a.member().team()||!ModernWorldProtocol.contains(map,e.origin())||!ModernWorldProtocol.contains(map,hit.cell())
                    ||e.casterLife()>incarnation||life!=null&&(e.casterLife()==incarnation?e.revision()<=life.respawnRevision():e.revision()>life.respawnRevision())
                    ||e.casterLife()==incarnation&&v.hp()==0&&e.revision()>=v.deathRevision()
                    ||e.revision()==revision&&(v.hp()==0||!e.origin().equals(a.position())||e.sequence()!=casts.get(i).requestSequence()
                        ||timeAttack.enabled()&&timeAttack.state().departed().contains(a.entityId()))
                    ||phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN)throw bad();
            if(hit instanceof QuestHit q)validateQuest(e,q,quest,timeAttack);
            else validateTower(e,(TowerHit)hit,quest,timeAttack);
        }
        // An objective HP event may share a Fire Storm revision only with its
        // own exact structure proof, not an unrelated NPC cast or other lane.
        for(var a:quest.actors())for(var s:states)if(s.castCount()>0
                &&(s.revision()==a.hurtRevision()||s.revision()==a.deathRevision())
                &&(s.structure()==null||s.structure().revision()!=s.revision()
                    ||!(s.structure().hit() instanceof QuestHit q)||!q.id().equals(a.id())))throw bad();
        if(timeAttack.enabled())for(var s:states)if(s.castCount()>0
                &&(s.revision()==timeAttack.state().towerHurtRevision()||s.revision()==timeAttack.state().towerDeathRevision())
                &&(s.structure()==null||s.structure().revision()!=s.revision()||!(s.structure().hit() instanceof TowerHit)))throw bad();
    }
    private static void validateQuest(Receipt e,QuestHit hit,ModernQuestObjectives.Snapshot quest,ModernTimeAttack.Snapshot timeAttack) {
        if(!quest.enabled()||timeAttack.enabled()||!quest.progress().scope().equals(hit.scope()))throw bad();
        var config=quest.progress().plan().objectives().target(hit.lane(),hit.id().stage());
        var kind=quest.progress().plan().flow()==ModernQuestProgress.Flow.FINAL_GUARDIAN?ModernQuestObjectives.Kind.FINAL_GUARDIAN:
                hit.id().stage()==4?ModernQuestObjectives.Kind.FINAL_SYMBOL:ModernQuestObjectives.Kind.ROOM_SYMBOL;
        var current=quest.actors().stream().filter(a->a.id().lane()==hit.lane()).findFirst().orElseThrow(ModernNpcFireStormStructures::bad);
        if(!hit.cell().equals(config.cell())||hit.maximumHp()!=config.maximumHp()||hit.kind()!=kind||current.id().stage()<hit.id().stage())throw bad();
        if(current.id().equals(hit.id())&&(current.bornRevision()!=hit.bornRevision()||e.revision()>current.revision()
                ||hit.outcome().remainingHp()<current.hp()||hit.outcome().death()&&current.deathRevision()!=e.revision()
                ||e.revision()==current.revision()&&(current.hp()!=hit.outcome().remainingHp()
                    ||hit.outcome().appliedDamage()>0&&current.hurtRevision()!=e.revision())))throw bad();
    }
    private static void validateTower(Receipt e,TowerHit hit,ModernQuestObjectives.Snapshot quest,ModernTimeAttack.Snapshot timeAttack) {
        if(quest.enabled()||!timeAttack.enabled())throw bad();var state=timeAttack.state();var tower=state.plan().tower();
        if(!hit.scope().equals(state.scope())||!hit.cell().equals(tower.cell())||hit.maximumHp()!=tower.maximumHp()
                ||e.revision()>state.revision()||hit.outcome().remainingHp()<state.towerHp()
                ||hit.outcome().death()&&state.towerDeathRevision()!=e.revision()
                ||e.revision()==state.towerHurtRevision()&&hit.outcome().remainingHp()!=state.towerHp()
                ||e.revision()==state.revision()&&(hit.outcome().remainingHp()!=state.towerHp()
                    ||hit.outcome().appliedDamage()>0&&state.towerHurtRevision()!=e.revision()))throw bad();
    }
    static boolean questDestroyed(List<ModernNpcFireStormState.State> states,ModernQuestObjectives.Id id,long revision) {
        return states.stream().map(ModernNpcFireStormState.State::structure).filter(Objects::nonNull)
                .anyMatch(e->e.revision()==revision&&e.hit() instanceof QuestHit q&&q.id().equals(id)&&q.outcome().death());
    }
    static boolean towerDestroyed(List<ModernNpcFireStormState.State> states,ModernTimeAttack.State tower) {
        return states.stream().map(ModernNpcFireStormState.State::structure).filter(Objects::nonNull)
                .anyMatch(e->e.revision()==tower.towerDeathRevision()&&e.hit() instanceof TowerHit t&&t.scope().equals(tower.scope())&&t.outcome().death());
    }
    private static void check(long maximum,long before,ModernFireStorm.Outcome outcome) {
        Objects.requireNonNull(outcome);
        if(maximum<1||before<1||before>maximum||outcome.appliedDamage()!=Math.min(before,outcome.rawDamage())
                ||outcome.remainingHp()!=before-outcome.appliedDamage())throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC fire storm structure proof");}
    private ModernNpcFireStormStructures(){}
}
