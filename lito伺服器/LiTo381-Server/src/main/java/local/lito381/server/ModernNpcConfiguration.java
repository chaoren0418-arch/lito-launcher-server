package local.lito381.server;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.LongSupplier;

/** Explicit startup-only NPC policy. No database, file, environment or clock
 * is consulted during parse. NPC catalogs are independent of the active player. */
final class ModernNpcConfiguration {
    static final String CLOCK = "LINEAGE_SIX_TIMES_TAIPEI_V1";
    static final List<String> SUFFIXES = List.of("npcRules", "npcCatalogSource", "npcCatalogDatabase",
            "npcIds", "npcSpawns", "npcCapacity", "npcCorpseMillis", "npcPathBudget",
            "npcClock", "npcNightStartSecond", "npcNightEndSecond", "npcCamp0", "npcCamp1", "npcPlayerCapacity", "npcMagicRules", "npcDisintegrateRules", "npcFireStormRules");
    record Spawn(int id, int lane, int stage, ModernWorldMap.Cell cell, int heading) {}
    record Spec(ModernWorldBinding.Key key, int catalogSource, String catalogDatabase, List<Integer> ids,
                List<Spawn> spawns, List<ModernWorldMap.Cell> camps, int capacity, int corpseMillis,
                int pathBudget, int nightStart, int nightEnd, int playerCapacity,ModernQuestConfiguration.Spec quest,boolean lightArrow,ModernTimeAttackConfiguration.Spec timeAttack,boolean disintegrate,boolean fireStorm) {
        Spec(ModernWorldBinding.Key key,int catalogSource,String catalogDatabase,List<Integer> ids,List<Spawn> spawns,List<ModernWorldMap.Cell> camps,int capacity,int corpseMillis,int pathBudget,int nightStart,int nightEnd,int playerCapacity,ModernQuestConfiguration.Spec quest,boolean lightArrow,ModernTimeAttackConfiguration.Spec timeAttack,boolean disintegrate) {
            this(key,catalogSource,catalogDatabase,ids,spawns,camps,capacity,corpseMillis,pathBudget,nightStart,nightEnd,playerCapacity,quest,lightArrow,timeAttack,disintegrate,false);
        }
        Spec(ModernWorldBinding.Key key,int catalogSource,String catalogDatabase,List<Integer> ids,List<Spawn> spawns,List<ModernWorldMap.Cell> camps,int capacity,int corpseMillis,int pathBudget,int nightStart,int nightEnd,int playerCapacity,ModernQuestConfiguration.Spec quest,boolean lightArrow,ModernTimeAttackConfiguration.Spec timeAttack) {
            this(key,catalogSource,catalogDatabase,ids,spawns,camps,capacity,corpseMillis,pathBudget,nightStart,nightEnd,playerCapacity,quest,lightArrow,timeAttack,false);
        }
        Spec { ids=List.copyOf(ids);spawns=List.copyOf(spawns);camps=List.copyOf(camps); }
        ModernNpcEncounter qualify(ModernNpcCatalog.Catalog catalog,
                Map<Integer,ModernNpcMelee.BlessingBinding> sources, LongSupplier clock) {
            if(catalog.sourceId()!=catalogSource || !catalog.database().equals(catalogDatabase)
                    || !catalog.npcs().keySet().equals(new HashSet<>(ids)))throw invalid();
            var initial=spawns.stream().map(s->new ModernNpcRoster.Spawn(catalog.require(
                    new ModernNpcCatalog.Key(catalogSource,catalogDatabase,s.id())).rosterTemplate(),
                    s.lane(),s.stage(),s.cell(),s.heading())).toList();
            return new ModernNpcEncounter(key,catalog,sources,camps,capacity,corpseMillis,initial,pathBudget,
                    ()->nightAt(clock.getAsLong(),nightStart,nightEnd),playerCapacity,quest==null?null:quest.qualify(catalog),lightArrow,timeAttack==null?null:timeAttack.qualify(catalog),disintegrate,fireStorm);
        }
        @Override public String toString(){return "ModernNpcSpec[redacted]";}
    }
    static Spec parse(Properties p,String prefix,ModernWorldBinding.Key key) {
        boolean cooperative=ModernTimeAttackConfiguration.present(p,prefix);
        if(SUFFIXES.stream().noneMatch(s->p.containsKey(prefix+s))&&!ModernQuestConfiguration.present(p,prefix)&&!cooperative)return null;
        if((cooperative?key.mode()!=4:key.mode()!=2) || !ModernNpcMelee.RULESET.equals(required(p,prefix,"npcRules"))
                || !CLOCK.equals(required(p,prefix,"npcClock")))throw invalid();
        int source=number(required(p,prefix,"npcCatalogSource"),1,Integer.MAX_VALUE);
        String database=required(p,prefix,"npcCatalogDatabase");ModernPhysicalCombat.identifier(database);
        var ids=new TreeSet<Integer>();
        for(String id:required(p,prefix,"npcIds").split(",",-1))
            if(!ids.add(number(id,1,Integer.MAX_VALUE))||ids.size()>ModernNpcCatalog.MAX_NPCS)throw invalid();
        int capacity=number(required(p,prefix,"npcCapacity"),1,128);
        var camps=cooperative?List.of(cell(required(p,prefix,"npcCamp0"))):List.of(cell(required(p,prefix,"npcCamp0")),cell(required(p,prefix,"npcCamp1")));
        var quest=ModernQuestConfiguration.parse(p,prefix,key,ids,capacity,camps);
        var timeAttack=ModernTimeAttackConfiguration.parse(p,prefix,key,ids,capacity,camps);
        var spawns=new ArrayList<Spawn>();var occupied=new HashSet<ModernWorldMap.Cell>();
        if(quest!=null)spawns.addAll(quest.firstWaves());
        else if(!cooperative)for(String row:required(p,prefix,"npcSpawns").split(";",-1)) {
            String[] v=row.split(",",-1);if(v.length!=6||spawns.size()==capacity)throw invalid();
            int id=number(v[0],1,Integer.MAX_VALUE);var cell=new ModernWorldMap.Cell(signed(v[3]),signed(v[4]));
            if(!ids.contains(id)||!occupied.add(cell))throw invalid();
            spawns.add(new Spawn(id,number(v[1],0,1),number(v[2],1,4),cell,number(v[5],0,7)));
        }
        if(!cooperative&&quest==null&&(camps.get(0).equals(camps.get(1))||camps.stream().anyMatch(occupied::contains)))throw invalid();
        int start=number(required(p,prefix,"npcNightStartSecond"),0,86399);
        int end=number(required(p,prefix,"npcNightEndSecond"),0,86399);
        if(start==end)throw invalid();
        boolean fireStorm=p.containsKey(prefix+"npcFireStormRules");
        if(fireStorm&&!ModernNpcFireStorm.RULESET.equals(required(p,prefix,"npcFireStormRules")))throw invalid();
        boolean disintegrate=p.containsKey(prefix+"npcDisintegrateRules");
        if(disintegrate&&!ModernDisintegrate.RULESET.equals(required(p,prefix,"npcDisintegrateRules")))throw invalid();
        boolean lightArrow=p.containsKey(prefix+"npcMagicRules");
        if(lightArrow&&!ModernNpcMagic.RULESET.equals(required(p,prefix,"npcMagicRules")))throw invalid();
        return new Spec(key,source,database,List.copyOf(ids),spawns,camps,capacity,
                number(required(p,prefix,"npcCorpseMillis"),1,60_000),
                number(required(p,prefix,"npcPathBudget"),1,64),start,end,
                number(required(p,prefix,"npcPlayerCapacity"),Math.max(2,TournamentRoomPolicy.rules(key.mode(),key.logicalMapId()).minimumPlayers()),cooperative?60:100),quest,lightArrow,timeAttack,disintegrate,fireStorm);
    }
    /** Same retained six-times epoch/alignment/timezone, without native time_t
     * truncation. The administrator explicitly selects the night interval. */
    static int secondOfDay(long epochMillis) {
        long elapsed=Math.subtractExact(epochMillis,ClassicLineageClock.REAL_BASE_EPOCH_MILLIS);
        if(elapsed<0)throw invalid();
        long seconds=Math.addExact(Math.multiplyExact(elapsed/1000,ClassicLineageClock.SPEED),
                elapsed%1000*ClassicLineageClock.SPEED/1000);
        seconds-=Math.floorMod(seconds,ClassicLineageClock.ALIGNMENT_SECONDS);
        return Instant.ofEpochSecond(seconds).atZone(ClassicLineageClock.NATIVE_CLIENT_ZONE).toLocalTime().toSecondOfDay();
    }
    static boolean nightAt(long epochMillis,int start,int end) {
        if(start<0||start>86399||end<0||end>86399||start==end)throw invalid();
        int second=secondOfDay(epochMillis);
        return start<end ? second>=start&&second<end : second>=start||second<end;
    }
    /** All catalog reads share one bounded startup operation and an owned
     * executor. No listener/store is opened before it has actually retired. */
    static Map<ModernWorldBinding.Key,ModernNpcEncounter> load(ModernServerConfig config) throws Exception {
        return load(config,System.nanoTime()+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(config.backendDeadlineMillis()));
    }
    static Map<ModernWorldBinding.Key,ModernNpcEncounter> load(ModernServerConfig config,long deadline) throws Exception {
        var specs=config.worlds().npcSpecs();if(specs.isEmpty())return Map.of();
        var sources=new HashMap<Integer,ModernServerConfig.SourceConfig>();
        var blessings=new HashMap<Integer,ModernNpcMelee.BlessingBinding>();
        for(var source:config.sources()) {
            sources.put(source.id(),source);
            blessings.put(source.id(),new ModernNpcMelee.BlessingBinding(source.id(),source.catalogDatabase(),source.npcBlessing()));
        }
        var executor=new ModernBackendExecutor(1,0);
        try(var gate=executor.openSession()) {
            long remaining=java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());
            if(remaining<50)throw new java.io.IOException("NPC startup deadline exhausted");
            return gate.call((int)Math.min(config.backendDeadlineMillis(),remaining),scope->{
                var result=new HashMap<ModernWorldBinding.Key,ModernNpcEncounter>();
                for(var spec:specs.values()) {
                    scope.checkActive();var source=sources.get(spec.catalogSource());if(source==null)throw invalid();
                    var backend=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis());
                    var catalog=backend.npcCatalog(spec.catalogSource(),spec.catalogDatabase(),spec.ids());
                    var encounter=spec.qualify(catalog,Map.copyOf(blessings),System::currentTimeMillis);
                    encounter.night().getAsBoolean(); // Validate the real clock before admitting this binding.
                    result.put(spec.key(),encounter);
                }
                scope.checkActive();return Map.copyOf(result);
            });
        } finally {
            executor.close();boolean interrupted=false;
            try {for(;;)try{if(executor.awaitTermination(1000))break;}catch(InterruptedException waiting){interrupted=true;}}
            finally{if(interrupted)Thread.currentThread().interrupt();}
        }
    }
    private static String required(Properties p,String prefix,String key) {
        String value=p.getProperty(prefix+key);if(value==null||value.isEmpty())throw invalid();return value;
    }
    static int number(String text,int min,int max) {
        if(!text.matches("0|[1-9][0-9]{0,9}"))throw invalid();long n=Long.parseLong(text);
        if(n<min||n>max)throw invalid();return (int)n;
    }
    static int signed(String text) {
        if(!text.matches("0|-?[1-9][0-9]{0,9}"))throw invalid();return Integer.parseInt(text);
    }
    private static ModernWorldMap.Cell cell(String text) {
        String[] v=text.split(",",-1);if(v.length!=2)throw invalid();return new ModernWorldMap.Cell(signed(v[0]),signed(v[1]));
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid explicit NPC configuration");}
    private ModernNpcConfiguration() {}
}
