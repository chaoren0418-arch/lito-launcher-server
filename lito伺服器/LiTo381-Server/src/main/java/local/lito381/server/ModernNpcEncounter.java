package local.lito381.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Trusted immutable encounter admission, not client spawn input or a Quest
 * completion policy. Catalog and blessing conventions are operator-selected;
 * the client only receives the distinct shared graphics required beforehand. */
record ModernNpcEncounter(ModernWorldBinding.Key key, ModernNpcCatalog.Catalog catalog,
                          Map<Integer,ModernNpcMelee.BlessingBinding> sources,
                          List<ModernWorldMap.Cell> lanes, int capacity, long corpseMillis,
                          List<ModernNpcRoster.Spawn> initial, int pathBudget, BooleanSupplier night, int playerCapacity,ModernQuestProgress.Plan questPlan,boolean lightArrow,ModernTimeAttack.Plan timeAttack,boolean disintegrate,boolean fireStorm) {
    ModernNpcEncounter(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<Integer,ModernNpcMelee.BlessingBinding> sources,
            List<ModernWorldMap.Cell> lanes,int capacity,long corpseMillis,List<ModernNpcRoster.Spawn> initial,int pathBudget,BooleanSupplier night,int playerCapacity,ModernQuestProgress.Plan questPlan,boolean lightArrow,ModernTimeAttack.Plan timeAttack,boolean disintegrate) {
        this(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,questPlan,lightArrow,timeAttack,disintegrate,false);
    }
    ModernNpcEncounter withFireStorm(boolean enabled){return new ModernNpcEncounter(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,questPlan,lightArrow,timeAttack,disintegrate,enabled);}
    ModernNpcEncounter(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<Integer,ModernNpcMelee.BlessingBinding> sources,
            List<ModernWorldMap.Cell> lanes,int capacity,long corpseMillis,List<ModernNpcRoster.Spawn> initial,int pathBudget,BooleanSupplier night,int playerCapacity,ModernQuestProgress.Plan questPlan,boolean lightArrow,ModernTimeAttack.Plan timeAttack) {
        this(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,questPlan,lightArrow,timeAttack,false);
    }
    ModernNpcEncounter withDisintegrate(boolean enabled){return new ModernNpcEncounter(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,questPlan,lightArrow,timeAttack,enabled,fireStorm);}
    ModernNpcEncounter(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<Integer,ModernNpcMelee.BlessingBinding> sources,
            List<ModernWorldMap.Cell> lanes,int capacity,long corpseMillis,List<ModernNpcRoster.Spawn> initial,int pathBudget,BooleanSupplier night,int playerCapacity,ModernQuestProgress.Plan questPlan,boolean lightArrow) {
        this(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,questPlan,lightArrow,null);
    }
    ModernNpcEncounter(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<Integer,ModernNpcMelee.BlessingBinding> sources,
            List<ModernWorldMap.Cell> lanes,int capacity,long corpseMillis,List<ModernNpcRoster.Spawn> initial,int pathBudget,BooleanSupplier night,int playerCapacity,ModernQuestProgress.Plan questPlan) {
        this(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,questPlan,false);
    }
    ModernNpcEncounter withLightArrow(boolean enabled){return new ModernNpcEncounter(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,questPlan,enabled,timeAttack,disintegrate,fireStorm);}
    ModernNpcEncounter(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<Integer,ModernNpcMelee.BlessingBinding> sources,
            List<ModernWorldMap.Cell> lanes,int capacity,long corpseMillis,List<ModernNpcRoster.Spawn> initial,int pathBudget,BooleanSupplier night,int playerCapacity) {
        this(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,playerCapacity,null);
    }
    ModernNpcEncounter(ModernWorldBinding.Key key, ModernNpcCatalog.Catalog catalog,
            Map<Integer,ModernNpcMelee.BlessingBinding> sources,List<ModernWorldMap.Cell> lanes,int capacity,long corpseMillis,
            List<ModernNpcRoster.Spawn> initial,int pathBudget,BooleanSupplier night) {
        this(key,catalog,sources,lanes,capacity,corpseMillis,initial,pathBudget,night,100);
    }
    ModernNpcEncounter {
        Objects.requireNonNull(key); Objects.requireNonNull(catalog); Objects.requireNonNull(night);
        sources=Map.copyOf(sources); lanes=List.copyOf(lanes); initial=List.copyOf(initial);
        if((timeAttack==null?key.mode()!=2:key.mode()!=4||!timeAttack.key().equals(key)||questPlan!=null||!initial.isEmpty())
                ||sources.isEmpty()||sources.size()>100||lanes.size()!=(timeAttack==null?2:1)
                ||timeAttack==null&&questPlan==null&&lanes.get(0).equals(lanes.get(1))
                ||capacity<1||capacity>128||corpseMillis<1||corpseMillis>60_000||timeAttack==null&&initial.isEmpty()||initial.size()>capacity
                ||pathBudget<1||pathBudget>64||playerCapacity<TournamentRoomPolicy.rules(key.mode(),key.logicalMapId()).minimumPlayers()||playerCapacity>(timeAttack==null?100:60))throw bad();
        sources.forEach((id,b)->{if(id!=b.sourceId())throw bad();});
        ModernNpcProtocol.Resources.from(catalog);
        for(var n:catalog.npcs().values())if(!n.pendingCleanMelee().isEmpty()||n.get(ModernNpcCatalog.Field.BOSS)!=0)throw bad();
        var cells=new HashSet<ModernQuestSpace.Cell>();
        for(var spawn:initial) {
            var t=spawn.template();
            if(!catalog.require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId())).rosterTemplate().equals(t)
                    ||!cells.add(ModernQuestSpace.cell(questPlan!=null,spawn.lane(),spawn.cell())))throw bad();
        }
        if(questPlan!=null&&(!questPlan.key().equals(key)||!questPlan.firstWaves().equals(initial)))throw bad();
    }
    void validate(ModernWorldBinding binding) {
        if(!key.equals(binding.key()))throw bad();
        try { if(questPlan==null)binding.map().validateSpawns(lanes);else for(var lane:lanes)binding.map().validateSpawns(List.of(lane)); }
        catch(ModernWorldMap.MapException failure){throw bad();}
        if(initial.stream().anyMatch(s->!binding.map().standing(s.cell())))throw bad();
        if(questPlan==null&&lanes.stream().anyMatch(reservedCells()::contains))throw bad();
        if(questPlan!=null)questPlan.qualify(binding,catalog,lanes,capacity);
        if(timeAttack!=null)timeAttack.qualify(binding,catalog,lanes,capacity);
    }
    Set<ModernWorldMap.Cell> reservedCells() { return timeAttack!=null?timeAttack.reservedCells():Set.copyOf(initial.stream().map(ModernNpcRoster.Spawn::cell).toList()); }
    ModernNpcBattle.Config capture(List<ModernBattleEngine.PreparedMember> members) {
        var players=new HashMap<ModernParticipantId,ModernNpcMelee.BlessingBinding>();
        for(var p:members) {
            var scope=p.scope();var binding=sources.get(scope.sourceId());if(binding==null)throw bad();
            var id=ModernParticipantId.human(scope.sourceId(),scope.objectId());
            if(players.put(id,binding)!=null)throw bad();
        }
        return new ModernNpcBattle.Config(key,catalog,players,lanes,capacity,corpseMillis,questPlan,lightArrow,timeAttack).withDisintegrate(disintegrate).withFireStorm(fireStorm);
    }
    @Override public String toString(){return "ModernNpcEncounter[redacted]";}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid trusted NPC encounter");}
}
