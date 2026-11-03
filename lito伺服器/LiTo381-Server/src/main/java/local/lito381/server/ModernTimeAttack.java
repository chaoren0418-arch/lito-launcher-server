package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Cooperative mode 4 authority. No LT geometry, guessed pattern weights,
 * client damage, SQL character aliases or second participant HP owner.
 * The chosen ordered pattern and shared placements are explicit policy.
 * Boss-specific combat/resources still require their own qualification. */
final class ModernTimeAttack {
    enum Kind { ORDINARY, FINAL_BOSSES, DRAGON }
    enum Phase { WAITING_FOR_TOWER, SPAWN_PENDING, FIGHTING, FINISHED, ABORTED }
    enum End { NONE, CLEARED, TIMEOUT, LEADER_DEFEATED, PARTY_DEFEATED }
    record Tower(ModernWorldMap.Cell cell,int graphic,long maximumHp) {
        Tower {Objects.requireNonNull(cell);if(graphic!=2665&&graphic!=2667&&graphic!=2669&&graphic!=2671||maximumHp<1)throw bad();}
    }
    record TowerId(ModernNpcRoster.Scope scope) {TowerId {Objects.requireNonNull(scope);}}
    /** NPC stage tags are roles, not a recycled serial or the ordered wave index. */
    record Wave(Kind kind,List<ModernNpcRoster.Spawn> spawns) {
        Wave {
            Objects.requireNonNull(kind);spawns=List.copyOf(spawns);
            if(spawns.isEmpty()||spawns.size()>ModernNpcRoster.MAX_RESIDENT)throw bad();
            var cells=new HashSet<ModernWorldMap.Cell>();
            for(var s:spawns)if(s.lane()!=0||s.stage()!=kind.ordinal()+1||!cells.add(s.cell()))throw bad();
        }
    }
    record Plan(ModernWorldBinding.Key key,Tower tower,List<Wave> waves,int durationMillis) {
        Plan {
            Objects.requireNonNull(key);Objects.requireNonNull(tower);waves=List.copyOf(waves);
            if(key.mode()!=4||key.logicalMapId()!=540||durationMillis<50||durationMillis>1_800_000||waves.size()<2||waves.size()>32)throw bad();
            int finals=0,dragons=0;
            for(int i=0;i<waves.size();i++) {
                var kind=waves.get(i).kind();
                if(i==0&&kind!=Kind.ORDINARY||kind==Kind.ORDINARY&&(finals!=0||dragons!=0)
                        ||kind==Kind.FINAL_BOSSES&&(++finals!=1||dragons!=0)
                        ||kind==Kind.DRAGON&&(finals!=1||++dragons!=1||i!=waves.size()-1))throw bad();
            }
            if(finals!=1)throw bad();
        }
        void qualify(ModernWorldBinding binding,ModernNpcCatalog.Catalog catalog,List<ModernWorldMap.Cell> camps,int capacity) {
            if(!key.equals(binding.key())||camps.size()!=1||capacity<1||capacity>128)throw bad();
            var camp=camps.get(0);if(camp.equals(tower.cell()))throw bad();
            try {
                binding.map().validateSpawns(List.of(camp,tower.cell()));
                for(var wave:waves) {
                    if(wave.spawns().size()>capacity)throw bad();
                    var cells=new ArrayList<ModernWorldMap.Cell>();cells.add(camp);
                    for(var spawn:wave.spawns()) {
                        var t=spawn.template();
                        if(!catalog.require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId())).rosterTemplate().equals(t)
                                ||camp.equals(spawn.cell())||tower.cell().equals(spawn.cell()))throw bad();
                        cells.add(spawn.cell());
                    }
                    for(int from=1;from<cells.size();from+=ModernWorldMap.MAX_SPAWNS-1) {
                        var part=new ArrayList<ModernWorldMap.Cell>();part.add(camp);
                        part.addAll(cells.subList(from,Math.min(cells.size(),from+ModernWorldMap.MAX_SPAWNS-1)));
                        binding.map().validateSpawns(part);
                    }
                }
            }catch(ModernWorldMap.MapException failure){throw bad();}
        }
        Set<ModernWorldMap.Cell> reservedCells(){var cells=new HashSet<ModernWorldMap.Cell>();cells.add(tower.cell());for(var w:waves)for(var s:w.spawns())cells.add(s.cell());return Set.copyOf(cells);}
        @Override public String toString(){return "TimeAttackPlan[explicit-shared-policy]";}
    }
    record State(ModernNpcRoster.Scope scope,Plan plan,long revision,Phase phase,int wave,
                 long startedMillis,long observedMillis,long leader,Set<Long> departed,Set<ModernNpcRoster.Id> living,
                 long towerHp,long towerHurtRevision,long towerDeathRevision,End end) {
        State {
            Objects.requireNonNull(scope);Objects.requireNonNull(plan);Objects.requireNonNull(phase);Objects.requireNonNull(end);
            departed=Set.copyOf(departed);living=Set.copyOf(living);
            if(revision<1||wave<0||wave>=plan.waves().size()||startedMillis< -1||observedMillis<0||observedMillis>ModernNpcRoster.MAX_ELAPSED_MILLIS
                    ||startedMillis>observedMillis||startedMillis>=60_000||leader<1||leader>60||departed.size()>60||departed.stream().anyMatch(id->id<1||id>60)
                    ||living.size()>128||living.stream().anyMatch(id->!id.scope().equals(scope))
                    ||towerHp<0||towerHp>plan.tower().maximumHp()||towerHurtRevision<0||towerHurtRevision>revision||towerDeathRevision<0||towerDeathRevision>revision
                    ||(towerHp==0)!=(towerDeathRevision>0)||(towerHp<plan.tower().maximumHp())!=(towerHurtRevision>0)
                    ||(startedMillis>=0)!=(towerHp==0)
                    ||towerDeathRevision>0&&towerDeathRevision!=towerHurtRevision
                    ||(phase==Phase.FINISHED)!=(end!=End.NONE)
                    ||phase==Phase.WAITING_FOR_TOWER&&(startedMillis!=-1||towerHp==0||wave!=0||!living.isEmpty())
                    ||(phase==Phase.SPAWN_PENDING||phase==Phase.FIGHTING)&&(startedMillis<0||towerHp!=0)
                    ||phase==Phase.FIGHTING&&living.isEmpty()||phase==Phase.SPAWN_PENDING&&!living.isEmpty()
                    ||end==End.CLEARED&&(wave!=plan.waves().size()-1||!living.isEmpty()||startedMillis<0)
                    ||end==End.TIMEOUT&&(startedMillis<0||observedMillis-startedMillis<plan.durationMillis()))throw bad();
        }
        boolean terminal(){return phase==Phase.FINISHED||phase==Phase.ABORTED;}
        boolean won(){return end==End.CLEARED;}
        State changed(long rev,Phase next,int stage,long start,long elapsed,long leaderId,Set<Long> left,Set<ModernNpcRoster.Id> alive,long hp,long hurt,long death,End reason) {
            if(rev<revision||elapsed<observedMillis)throw bad();
            return new State(scope,plan,rev,next,stage,start,elapsed,leaderId,left,alive,hp,hurt,death,reason);
        }
    }
    record Exchange(long entityId,TowerId target,long sequence,long revision,ModernPhysicalCombat.Outcome outcome) {
        Exchange {Objects.requireNonNull(target);Objects.requireNonNull(outcome);if(entityId<1||entityId>60||sequence<1||revision<1)throw bad();}
    }
    record Snapshot(State state,List<Exchange> exchanges) {
        Snapshot {exchanges=List.copyOf(exchanges);if(state==null&&!exchanges.isEmpty()||exchanges.size()>60)throw bad();}
        static Snapshot disabled(){return new Snapshot(null,List.of());}
        boolean enabled(){return state!=null;}
        boolean finished(){return enabled()&&state.phase()==Phase.FINISHED;}
        void validate(ModernBattleEngine.Snapshot world) {
            if(!enabled())return;
            if(world.room().summary().config().mode()!=4||world.room().summary().config().mapId()!=540
                    ||world.actors().size()>60||world.vitals().size()!=world.actors().size()||world.isolatedForces()
                    ||world.result()!=null||world.race().enabled()||world.quest().enabled()||world.honor().enabled()
                    ||state.scope().matchId()!=world.matchId()||state.scope().generation()!=world.generation()||state.scope().roomId()!=world.room().summary().id()
                    ||state.revision()>world.revision()||(world.phase()==ModernBattleEngine.Phase.FINISHED)!=finished()
                    ||(world.phase()==ModernBattleEngine.Phase.ABORTED)!=(state.phase()==Phase.ABORTED)
                    ||world.actors().stream().anyMatch(a->!a.member().identity().isHuman()||a.member().team()!=0)
                    ||state.leader()>world.actors().size()||state.departed().stream().anyMatch(id->id>world.actors().size()))throw bad();
            var seen=new HashSet<Long>();long previous=0;
            for(var e:exchanges) {
                if(e.entityId()>world.actors().size()||!e.target().scope().equals(state.scope())||!seen.add(e.entityId())||e.revision()<=previous||e.revision()>world.revision()
                    ||e.sequence()>world.vitals().get((int)e.entityId()-1).attackSequence()||e.outcome().remainingHp()<state.towerHp()
                    ||e.outcome().remainingHp()>state.plan().tower().maximumHp()||e.outcome().appliedDamage()>state.plan().tower().maximumHp())throw bad();
                previous=e.revision();
            }
            if(state.towerDeathRevision()>0&&exchanges.stream().noneMatch(e->e.revision()==state.towerDeathRevision()&&e.outcome().death())
                    &&!ModernNpcFireStormStructures.towerDestroyed(world.npcFireStorms(),state))throw bad();
            if(state.end()==End.LEADER_DEFEATED&&world.vitals().get((int)state.leader()-1).hp()!=0)throw bad();
        }
        List<ModernMatchSettlement.Participant> outcomes(ModernBattleEngine.Snapshot world) {
            validate(world);if(!finished())return List.of();
            return world.actors().stream().map(a->new ModernMatchSettlement.Participant(a.entityId(),a.member().identity(),0,
                    state.won()&&!state.departed().contains(a.entityId())&&world.vitals().get((int)a.entityId()-1).hp()>0
                            ?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
        }
    }
    static State initial(ModernNpcRoster.Scope scope,Plan plan,long leader) {
        return new State(scope,plan,1,Phase.WAITING_FOR_TOWER,0,-1,0,leader,Set.of(),Set.of(),plan.tower().maximumHp(),0,0,End.NONE);
    }
    static State towerHit(State s,ModernPhysicalCombat.Outcome hit,long revision) {
        if(s.phase()!=Phase.WAITING_FOR_TOWER||revision<=s.revision()||hit.appliedDamage()>s.towerHp()||hit.remainingHp()!=s.towerHp()-hit.appliedDamage()||hit.death()!=(hit.remainingHp()==0))throw bad();
        return s.changed(revision,hit.death()?Phase.SPAWN_PENDING:s.phase(),0,hit.death()?s.observedMillis():-1,s.observedMillis(),s.leader(),s.departed(),Set.of(),
                hit.remainingHp(),hit.appliedDamage()>0?revision:s.towerHurtRevision(),hit.death()?revision:0,End.NONE);
    }
    static State towerHit(State s,ModernFireStorm.Outcome hit,long revision) {
        if(s.phase()!=Phase.WAITING_FOR_TOWER||revision<=s.revision()||hit.appliedDamage()>s.towerHp()||hit.remainingHp()!=s.towerHp()-hit.appliedDamage()||hit.death()!=(hit.remainingHp()==0))throw bad();
        return s.changed(revision,hit.death()?Phase.SPAWN_PENDING:s.phase(),0,hit.death()?s.observedMillis():-1,s.observedMillis(),s.leader(),s.departed(),Set.of(),
                hit.remainingHp(),hit.appliedDamage()>0?revision:s.towerHurtRevision(),hit.death()?revision:0,End.NONE);
    }
    record SpawnStep(State state,List<ModernNpcRoster.Spawn> batch) {SpawnStep {batch=List.copyOf(batch);}}
    static SpawnStep spawn(State s,ModernNpcRoster.Snapshot roster,long revision) {
        if(s.phase()!=Phase.SPAWN_PENDING||revision<=s.revision()||!s.scope().equals(roster.scope())||roster.closed())throw bad();
        var batch=s.plan().waves().get(s.wave()).spawns();var living=new HashSet<ModernNpcRoster.Id>();
        long last=Math.addExact(roster.spawned(),batch.size());for(long id=roster.spawned();id<last;)living.add(new ModernNpcRoster.Id(s.scope(),++id));
        return new SpawnStep(s.changed(revision,Phase.FIGHTING,s.wave(),s.startedMillis(),s.observedMillis(),s.leader(),s.departed(),living,s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),End.NONE),batch);
    }
    static State defeated(State s,ModernNpcRoster.Actor before,long revision) {
        return defeated(s,List.of(before),revision);
    }
    static State defeated(State s,List<ModernNpcRoster.Actor> defeated,long revision) {
        defeated=List.copyOf(defeated);
        if(s.phase()!=Phase.FIGHTING||revision<=s.revision()||defeated.isEmpty()||defeated.size()>ModernNpcRoster.MAX_RESIDENT)throw bad();
        var living=new HashSet<>(s.living());
        for(var before:defeated)if(!before.alive()||before.life()!=1||!living.remove(before.id())
                ||before.lane()!=0||before.stage()!=s.plan().waves().get(s.wave()).kind().ordinal()+1)throw bad();
        boolean last=s.wave()==s.plan().waves().size()-1;
        return s.changed(revision,living.isEmpty()?(last?Phase.FINISHED:Phase.SPAWN_PENDING):Phase.FIGHTING,
                living.isEmpty()&&!last?s.wave()+1:s.wave(),s.startedMillis(),s.observedMillis(),s.leader(),s.departed(),living,s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),living.isEmpty()&&last?End.CLEARED:End.NONE);
    }
    static State advance(State s,long elapsed,long revision) {
        if(s.terminal())return s;if(elapsed<s.observedMillis())throw bad();
        boolean timeout=s.startedMillis()>=0&&elapsed-s.startedMillis()>=s.plan().durationMillis();
        // A delayed poll observes the authoritative deadline, not an unbounded
        // post-match clock. It cannot overflow the bounded NPC/world timeline.
        long observed=timeout?s.startedMillis()+s.plan().durationMillis():elapsed;
        return s.changed(timeout?revision:s.revision(),timeout?Phase.FINISHED:s.phase(),s.wave(),s.startedMillis(),observed,s.leader(),s.departed(),s.living(),s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),timeout?End.TIMEOUT:End.NONE);
    }
    static State leaderDefeated(State s,long entity,long revision) {
        if(s.terminal()||entity!=s.leader())return s;
        return s.changed(revision,Phase.FINISHED,s.wave(),s.startedMillis(),s.observedMillis(),s.leader(),s.departed(),s.living(),s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),End.LEADER_DEFEATED);
    }
    /** A departure is not a combat death. An eligible living member becomes
     * leader; no remaining living member is an explicit party defeat. */
    static State depart(State s,long entity,Set<Long> eligible,long revision) {
        if(s.terminal())return s;if(entity<1||entity>60||s.departed().contains(entity)||revision<=s.revision()
                ||eligible.size()>60||eligible.stream().anyMatch(id->id<1||id>60))throw bad();
        var left=new HashSet<>(s.departed());left.add(entity);
        var remaining=eligible.stream().filter(id->!left.contains(id)).sorted().toList();
        long leader=remaining.isEmpty()?s.leader():s.leader()==entity?remaining.get(0):s.leader();
        return s.changed(revision,remaining.isEmpty()?Phase.FINISHED:s.phase(),s.wave(),s.startedMillis(),s.observedMillis(),leader,left,s.living(),s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),remaining.isEmpty()?End.PARTY_DEFEATED:End.NONE);
    }
    static State abort(State s,long revision) {
        if(s.terminal())return s;
        return s.changed(revision,Phase.ABORTED,s.wave(),s.startedMillis(),s.observedMillis(),s.leader(),s.departed(),s.living(),s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),End.NONE);
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid cooperative Time Attack authority");}
    private ModernTimeAttack(){}
}
