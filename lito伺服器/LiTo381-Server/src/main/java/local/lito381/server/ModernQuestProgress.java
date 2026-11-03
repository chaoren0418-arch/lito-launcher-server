package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Retained Quest flow, not a native coordinate/resource lookup. Each force
 * clears four waves independently. Maps 513/527 additionally require each
 * room's symbol; the other logical Quest maps require a final guardian only.
 * Wave clear, player defeat, and elapsed safety time are NEVER victory.
 * Objective destruction is a trusted domain event, not a client request or a
 * claim that an objective combat/renderer/settlement provider already exists.
 * See BattleQuestEncounter.hit/destroyObjective and BattleRules.waveCleared.
 */
final class ModernQuestProgress {
    enum Flow { FINAL_GUARDIAN, ROOM_SYMBOLS }
    enum Phase { SPAWN_PENDING, FIGHTING, OBJECTIVE }
    enum Status { RUNNING, FINISHED, ABORTED }
    record Wave(int lane,int stage,List<ModernNpcRoster.Spawn> spawns) {
        Wave {
            spawns=List.copyOf(spawns);
            if(lane<0||lane>1||stage<1||stage>4||spawns.isEmpty()||spawns.size()>128)throw bad();
            var cells=new HashSet<ModernWorldMap.Cell>();
            for(var spawn:spawns)if(spawn.lane()!=lane||spawn.stage()!=stage||!cells.add(spawn.cell()))throw bad();
        }
    }
    /** Exactly eight ordered waves. Reusing a cell in a later wave is legal;
     * dynamic occupancy and corpse residency can delay, never partially spawn,
     * that wave. Counts/positions are explicit qualified policy, not inferred
     * from the old photographs or imported from LTmap.
     */
    record Plan(ModernWorldBinding.Key key,List<Wave> waves,ModernQuestObjectives.Config objectives,boolean participantLives) {
        Plan(ModernWorldBinding.Key key,List<Wave> waves,ModernQuestObjectives.Config objectives){this(key,waves,objectives,false);}
        Plan(ModernWorldBinding.Key key,List<Wave> waves){this(key,waves,null);}
        Plan {
            Objects.requireNonNull(key);waves=List.copyOf(waves);
            if(key.mode()!=2||!BattleRules.Mode.QUEST_BATTLE.maps().contains(key.logicalMapId())||waves.size()!=8||participantLives&&objectives==null)throw bad();
            for(int i=0;i<8;i++)if(waves.get(i).lane()!=i/4||waves.get(i).stage()!=i%4+1)throw bad();
            if(objectives!=null) {
                boolean symbols=key.logicalMapId()==513||key.logicalMapId()==527;
                if(objectives.targets().size()!=(symbols?8:2))throw bad();
                for(int i=0;i<objectives.targets().size();i++)if(objectives.targets().get(i).slot()!=(symbols?i+1:(i+1)*4))throw bad();
            }
        }
        Flow flow(){return key.logicalMapId()==513||key.logicalMapId()==527?Flow.ROOM_SYMBOLS:Flow.FINAL_GUARDIAN;}
        Plan withParticipantLives(){return new Plan(key,waves,objectives,true);}
        Wave wave(int lane,int stage){if(lane<0||lane>1||stage<1||stage>4)throw bad();return waves.get(lane*4+stage-1);}
        List<ModernNpcRoster.Spawn> firstWaves(){var result=new ArrayList<ModernNpcRoster.Spawn>();result.addAll(wave(0,1).spawns());result.addAll(wave(1,1).spawns());return List.copyOf(result);}
        void qualify(ModernWorldBinding binding,ModernNpcCatalog.Catalog catalog,List<ModernWorldMap.Cell> anchors,int capacity) {
            if(!key.equals(binding.key())||anchors.size()!=2||capacity<1||capacity>128)throw bad();
            int maximum=0;
            for(int lane=0;lane<2;lane++) {
                int largest=0;
                for(int stage=1;stage<=4;stage++) {
                    var wave=wave(lane,stage);largest=Math.max(largest,wave.spawns().size());
                    var cells=new ArrayList<ModernWorldMap.Cell>();cells.add(anchors.get(lane));
                    for(var spawn:wave.spawns()) {
                        var t=spawn.template();
                        if(!catalog.require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId())).rosterTemplate().equals(t)
                                ||!binding.map().standing(spawn.cell())||anchors.get(lane).equals(spawn.cell()))throw bad();
                        cells.add(spawn.cell());
                    }
                    for(int from=1;from<cells.size();from+=ModernWorldMap.MAX_SPAWNS-1) {
                        var part=new ArrayList<ModernWorldMap.Cell>();part.add(cells.get(0));
                        part.addAll(cells.subList(from,Math.min(cells.size(),from+ModernWorldMap.MAX_SPAWNS-1)));
                        try{binding.map().validateSpawns(part);}catch(ModernWorldMap.MapException failure){throw bad();}
                    }
                }
                maximum=Math.addExact(maximum,largest);
            }
            if(maximum>capacity)throw bad();
            if(objectives!=null)objectives.qualify(this,binding,anchors);
            // The two forces may use the same geometry; only same-force cells
            // collide. Wave's constructor already enforces that uniqueness.
        }
        @Override public String toString(){return "ModernQuestPlan[redacted]";}
    }
    record Lane(int stage,Phase phase,Set<ModernNpcRoster.Id> living) {
        Lane {
            Objects.requireNonNull(phase);living=Set.copyOf(living);
            if(stage<1||stage>4||living.size()>128||(phase==Phase.FIGHTING)!=!living.isEmpty())throw bad();
        }
    }
    record State(ModernNpcRoster.Scope scope,Plan plan,long revision,Status status,List<Lane> lanes,int winningLane) {
        State {
            Objects.requireNonNull(scope);Objects.requireNonNull(plan);Objects.requireNonNull(status);lanes=List.copyOf(lanes);
            if(revision<1||lanes.size()!=2||winningLane< -1||winningLane>1||(status==Status.FINISHED)!=(winningLane>=0))throw bad();
            var identities=new HashSet<ModernNpcRoster.Id>();
            for(var lane:lanes)for(var id:lane.living())if(!id.scope().equals(scope)||!identities.add(id))throw bad();
            if(identities.size()>128)throw bad();
            if(status==Status.FINISHED&&(lanes.get(winningLane).stage()!=4||lanes.get(winningLane).phase()!=Phase.OBJECTIVE))throw bad();
        }
    }
    record SpawnStep(State before,State after,List<ModernNpcRoster.Spawn> batch) {
        SpawnStep {batch=List.copyOf(batch);}
    }
    static State initial(ModernNpcRoster.Scope scope,Plan plan) {
        return new State(scope,plan,1,Status.RUNNING,List.of(new Lane(1,Phase.SPAWN_PENDING,Set.of()),new Lane(1,Phase.SPAWN_PENDING,Set.of())),-1);
    }
    /** Build every identity/state value before the roster commit. The owning
     * Engine lock ensures the supplied roster cannot change in between. */
    static SpawnStep spawn(State state,int lane,ModernNpcRoster.Snapshot roster) {
        require(state,lane);if(!state.scope().equals(roster.scope())||roster.closed())throw bad();
        var current=state.lanes().get(lane);if(state.status()!=Status.RUNNING||current.phase()!=Phase.SPAWN_PENDING)throw bad();
        var wave=state.plan().wave(lane,current.stage());var ids=new HashSet<ModernNpcRoster.Id>();
        long last=Math.addExact(roster.spawned(),wave.spawns().size());
        for(long serial=roster.spawned();serial<last;)ids.add(new ModernNpcRoster.Id(state.scope(),++serial));
        var after=replace(state,lane,new Lane(current.stage(),Phase.FIGHTING,ids),Status.RUNNING,-1);
        return new SpawnStep(state,after,wave.spawns());
    }
    /** Called only for a death that the shared combat authority will commit.
     * A stale life, another lane/generation, or untracked summon cannot clear
     * a wave. Resurrection needs a separate explicit policy before enabling it.
     */
    static State defeated(State state,ModernNpcRoster.Actor before) {
        require(state,before.lane());var lane=state.lanes().get(before.lane());
        if(state.status()!=Status.RUNNING||!before.alive()||before.life()!=1||before.stage()!=lane.stage()
                ||lane.phase()!=Phase.FIGHTING||!lane.living().contains(before.id()))throw bad();
        var living=new HashSet<>(lane.living());living.remove(before.id());
        var next=!living.isEmpty()?new Lane(lane.stage(),Phase.FIGHTING,living)
                :state.plan().flow()==Flow.ROOM_SYMBOLS||lane.stage()==4?new Lane(lane.stage(),Phase.OBJECTIVE,Set.of())
                :new Lane(lane.stage()+1,Phase.SPAWN_PENDING,Set.of());
        return replace(state,before.lane(),next,Status.RUNNING,-1);
    }
    /** Only the qualified objective authority invokes this after actual damage.
     * Not exported as a client event or invoked on an ordinary NPC kill. */
    static State objectiveDestroyed(State state,ModernNpcRoster.Scope scope,int lane,int stage) {
        require(state,lane);if(!state.scope().equals(scope)||stage<1||stage>4)throw bad();
        if(state.status()!=Status.RUNNING)return state;
        var current=state.lanes().get(lane);
        if(current.stage()!=stage||current.phase()!=Phase.OBJECTIVE)throw bad();
        return stage==4?replace(state,lane,current,Status.FINISHED,lane)
                :replace(state,lane,new Lane(stage+1,Phase.SPAWN_PENDING,Set.of()),Status.RUNNING,-1);
    }
    static State abort(State state) {
        if(state.status()!=Status.RUNNING)return state;
        return new State(state.scope(),state.plan(),Math.incrementExact(state.revision()),Status.ABORTED,state.lanes(),-1);
    }
    private static State replace(State state,int lane,Lane changed,Status status,int winner) {
        var lanes=new ArrayList<>(state.lanes());lanes.set(lane,changed);
        return new State(state.scope(),state.plan(),Math.incrementExact(state.revision()),status,lanes,winner);
    }
    private static void require(State state,int lane){Objects.requireNonNull(state);if(lane<0||lane>1)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid qualified Quest progression");}
    private ModernQuestProgress(){}
}
