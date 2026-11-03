package local.lito381.server;
import java.nio.file.*;
import java.util.*;

/** Native C_DIALOG, source NPC placements and source map, without character SQL. */
public class BlessingNpcRegression extends WeaponBuffRegression {
    static SourceBlessingControls.Snapshot loadRows(List<Map<String,Object>> stories,List<Map<String,Object>> npcs)throws Exception {
        var connection=(java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(BlessingNpcRegression.class.getClassLoader(),new Class[]{java.sql.Connection.class},(p,m,a)->{
            if(!m.getName().equals("createStatement"))throw new AssertionError("unexpected NPC loader connection operation");
            return java.lang.reflect.Proxy.newProxyInstance(BlessingNpcRegression.class.getClassLoader(),new Class[]{java.sql.Statement.class},(p2,m2,a2)->{
                if(m2.getName().equals("close"))return null;if(!m2.getName().equals("executeQuery")||!((String)a2[0]).startsWith("SELECT "))throw new AssertionError("non-SELECT NPC loader operation");
                var rows=((String)a2[0]).contains("系統_劇情任務")?stories:npcs;var cursor=new int[]{-1};
                return java.lang.reflect.Proxy.newProxyInstance(BlessingNpcRegression.class.getClassLoader(),new Class[]{java.sql.ResultSet.class},(p3,m3,a3)->{
                    if(m3.getName().equals("close"))return null;if(m3.getName().equals("next"))return ++cursor[0]<rows.size();var value=rows.get(cursor[0]).get((String)a3[0]);
                    return switch(m3.getName()){case "getInt"->value==null?0:((Number)value).intValue();case "getLong"->value==null?0L:((Number)value).longValue();case "getString"->value==null?null:value.toString();case "getBoolean"->value!=null&&((Number)value).intValue()!=0;default->throw new AssertionError("unexpected NPC loader result read");};
                });
            });
        });return SourceBlessingControls.read(connection);
    }
    static void loaderVectors(CombatDatabase.Snapshot data)throws Exception {
        var row=new HashMap<String,Object>();row.putAll(Map.of("npc_templateid",70670,"id",1294,"mapid",4,"count",1,"locx",33090,"locy",32717,"heading",6,"classname","quest2.Npc_Cray","nameid","$1015","gfxid",7618));
        check(loadRows(List.of(),List.of(row)).npcs().get(4).get(0).npcId()==70670,"source NPC loader accepts deterministic current executor profile");
        for(var invalid:Map.<String,Object>of("count",2,"randomx",1,"randomy",1,"movement_distance",1,"classname","other.Npc","gfxid",0,"locx",1).entrySet()) {
            var changed=new HashMap<>(row);changed.put(invalid.getKey(),invalid.getValue());boolean rejected=false;try{loadRows(List.of(),List.of(changed));}catch(java.sql.SQLException expected){rejected=true;}check(rejected,"unsupported NPC source profile rejected="+invalid.getKey());
        }
        boolean rejected=false;try{loadRows(List.of(Map.of("id",7,"event_type","NPC_TALK","action_name","")),List.of(row));}catch(java.sql.SQLException expected){rejected=true;}check(rejected,"source story override rejects partial NPC executor application");
        check(loadRows(List.of(),List.of()).npcs().isEmpty()&&CombatDatabase.controls()==data.controls(),"disabled source spawn profile stays empty and loader never publishes a partial snapshot");
    }
    static void talk(ClientSession actor,long object)throws Exception{call(actor,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_DIALOG).d(object).bytes());}
    static Map<Long,String> actorIdentities(ClassicWorldMonsterPopulation population)throws Exception {
        var result=new TreeMap<Long,String>();for(var actor:((Map<?,?>)get(population,"actors")).values()) {
            var row=(ClassicWorldMonsterPopulation.Snapshot)call(actor,"snapshot",new Class[]{});result.put(row.objectId(),row.mapId()+":"+row.npcId()+":"+row.spawnId()+":"+row.spawnNumber()+":"+row.x()+":"+row.y());
        }return result;
    }
    static void world(GameHub hub,ClientSession actor,ClassicWorldNpcCatalog.StaticNpc npc)throws Exception {
        Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));set(actor,"classicWorldMapId",npc.mapId());set(actor,"classicWorldInstanceId",-1);
        set(actor,"positionX",npc.x()+1);set(actor,"positionY",npc.y());set(actor,"playerHp",1000);set(actor,"playerMaxHp",1000);set(actor,"observer",false);
        hub.enterClassicWorld(actor,npc.mapId(),npc.x()+1,npc.y(),0);
    }
    public static void main(String[] args)throws Exception {
        var config=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(config);var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);var source=data.controls().blessings();
        loaderVectors(data);
        var cray=ClassicWorldNpcCatalog.all(1005).stream().filter(n->n.npcId()==70670).findFirst().orElseThrow();
        var saell=ClassicWorldNpcCatalog.all(1011).stream().filter(n->n.npcId()==70689).findFirst().orElseThrow();
        check(cray.spawnId()==1294&&cray.x()==32807&&cray.y()==32925&&cray.heading()==6&&cray.gfxId()==7618,"Cray identity and placement come from current shared rows");
        check(saell.spawnId()==1296&&saell.x()==32859&&saell.y()==32664,"Saell shared placement overlays old native row once");
        check(ClassicWorldNpcCatalog.all(1011).stream().filter(n->n.npcId()==70689).count()==1&&ClassicWorldNpcCatalog.npc(1005,cray.objectId()).equals(cray),"native listing and target lookup share source placement");
        check(ClassicWorldMapCatalog.supports(1005)&&ClassicWorldMapCatalog.supportedMapIds().contains(1005),"source Cray map is available through native map APIs");
        var originalIds=ClassicWorldMapCatalog.class.getDeclaredField("SUPPORTED_MAP_IDS");originalIds.setAccessible(true);var shipped=(List<Integer>)originalIds.get(null);
        check(ClassicWorldMapCatalog.supportedMapIds().subList(0,shipped.size()).equals(shipped),"source map IDs append without reordering the shipped actor allocation sequence");
        Map<Long,String> baseline;try{df.set(null,null);baseline=actorIdentities(new ClassicWorldMonsterPopulation());}finally{df.set(null,data);}
        check(actorIdentities(new ClassicWorldMonsterPopulation()).equals(baseline),"adding source blessing map preserves every shipped monster object/NPC/spawn/position identity");
        var map=ClassicWorldMapCatalog.forMap(1005);check(map.startX()==32512&&map.endX()==33023&&map.startY()==32576&&map.endY()==33023&&map.contains(cray.x(),cray.y()),"source map collision bounds match mapids and NPC coordinates");
        check(!map.allowsRandomTeleport()&&map.allowsEscape()&&map.allowsDeathPenalty(),"source map flags remain independent of LiTo no-death-penalty rule");
        boolean immutable=false;try{source.npcs().get(1005).clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"NPC source metadata list immutable");
        immutable=false;try{source.maps().clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"source map registry immutable");
        var lines=new ArrayList<String>();for(var entry:source.mapHashes().entrySet()) {
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(entry.getKey())));
            check(hash.equalsIgnoreCase(entry.getValue()),"loaded collision bytes match read-only source input hash");lines.add(entry.getKey()+"\t"+hash);
        }Files.write(Path.of(args[2]),lines);
        Path root=Path.of(args[1]);Files.createDirectories(root);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");
            var fixtures=List.of(f,peer,mate,other);for(int i=0;i<fixtures.size();i++) {
                var fixture=fixtures.get(i);NativeDirectoryRegression.init(fixture,"BlessingNpc"+i,0,config.classicServerNames().get(0));
                var summary=((List<CharacterSummary>)get(fixture.session,"characters")).get(0);set(fixture.session,"characters",List.of(copy(summary,Map.of("objectId",0x38170001L+i))));
            }
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);var actor=f.session;var story=CombatSpecialBuffs.story(actor);
            var original=((List<CharacterSummary>)get(actor,"characters")).get(0);var cipher=new Protocol.CipherState(0x37110001);f.client.setSoTimeout(30);NativeDirectoryRegression.drain(f,cipher);
            for(int type=0;type<7;type++) {
                set(actor,"characters",List.of(copy(original,Map.of("type",type))));set(actor,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(40318L,7,40308L,500)));
                set(actor,"learnedSkills",Set.of());story.clear();world(hub,actor,cray);SpecialBuffRegression.rebuild(actor);
                int baseHp=(int)get(actor,"playerMaxHp"),baseMp=(int)get(actor,"playerMaxMp");var inventory=((RuntimeInventory)get(actor,"inventory")).catalogCounts();
                set(actor,"playerMp",baseMp);NativeDirectoryRegression.drain(f,cipher);long before=System.nanoTime();talk(actor,cray.objectId());long after=System.nanoTime();var frames=NativeDirectoryRegression.drain(f,cipher);
                check(story.containsKey(4009)&&story.get(4009)>=before+2_400_000_000_000L&&story.get(4009)<=after+2_400_000_000_000L,"native C_DIALOG Cray source duration class="+type);
                check((int)get(actor,"playerMaxHp")==baseHp+100&&(int)get(actor,"playerMaxMp")==baseMp+50,"native Cray dialog updates vitals class="+type);
                check((int)get(actor,"playerMp")==baseMp&&((RuntimeInventory)get(actor,"inventory")).catalogCounts().equals(inventory),"NPC blessing has no player MP/material debit class="+type);
                check(frames.stream().anyMatch(p->Arrays.equals(p,Protocol.effectObject(actor.playerObjectId(),7681))),"encrypted native reply contains Cray gfx7681 class="+type);
                check(frames.stream().anyMatch(p->Arrays.equals(p,Protocol.manaPoint(baseMp,baseMp+50))),"encrypted native reply contains updated maximum MP class="+type);
                long lease=story.get(4009);talk(actor,cray.objectId());check(story.get(4009)==lease,"native repeated Cray dialog does not refresh class="+type);
                check(NativeDirectoryRegression.drain(f,cipher).stream().noneMatch(p->(p[0]&255)==Protocol.S_EFFECT),"native repeat dialog emits no duplicate blessing effect class="+type);
                CombatSpecialBuffs.startBlessing(actor,4016,30);talk(actor,cray.objectId());
                check(!story.containsKey(4016)&&story.get(4009)==lease&&(int)get(actor,"playerMaxHp")==baseHp+100,"Cray removes strengthened blessing before active-ID early return class="+type);
                world(hub,actor,saell);talk(actor,saell.objectId());
                check(!story.containsKey(4009)&&story.containsKey(4010)&&(int)get(actor,"playerMaxHp")==baseHp+80&&(int)get(actor,"playerMaxMp")==baseMp+10,"native Saell dialog replaces Cray class="+type);
                long water=story.get(4010);CombatSpecialBuffs.startBlessing(actor,4016,30);talk(actor,saell.objectId());
                check(!story.containsKey(4016)&&story.get(4010)==water,"Saell removes strengthened blessing before no-refresh class="+type);
                world(hub,actor,cray);talk(actor,cray.objectId());check(!story.containsKey(4010)&&story.containsKey(4009),"native Cray dialog replaces Saell class="+type);
            }
            for(String rejection:List.of("distance","outside-screen","dead","observer","unknown","wrong-map")) {
                story.clear();world(hub,actor,cray);long object=cray.objectId();
                switch(rejection){case "distance"->set(actor,"positionX",cray.x()+6);case "outside-screen"->set(actor,"positionX",cray.x()+30);case "dead"->set(actor,"playerHp",0);case "observer"->set(actor,"observer",true);case "unknown"->object=0x7ffffffeL;case "wrong-map"->set(actor,"classicWorldMapId",4);}
                talk(actor,object);check(story.isEmpty(),"native C_DIALOG rejects "+rejection);
            }
            world(hub,actor,cray);story.clear();Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"BATTLE"));talk(actor,cray.objectId());check(story.isEmpty(),"arena dialog does not invoke source world service");
            var moved=copy(cray,Map.of("mapId",4,"x",33090,"y",32717));var alternate=new SourceBlessingControls.Snapshot(Map.of(4,List.of(moved)),source.maps(),source.mapHashes());var c=data.controls();
            df.set(null,new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(c.probabilities(),c.bossModifiers(),c.abilities(),c.npcAttributes(),c.skillCoexist(),c.cancellationImmune(),c.eyes(),alternate)));
            check(ClassicWorldNpcCatalog.npc(1005,cray.objectId())==null&&ClassicWorldNpcCatalog.npc(1011,saell.objectId())==null&&ClassicWorldNpcCatalog.npc(4,cray.objectId()).equals(moved),"altered source placements remove stale native NPC fallback");
            world(hub,actor,moved);talk(actor,moved.objectId());check(story.containsKey(4009),"native dialog follows changed immutable source NPC placement");
        } finally {df.set(null,data);}
        System.out.println("PASS source blessing NPC checks="+checks+"; seven-class native C_DIALOG Cray/Saell, source NPC/map identities, 2400-second nonrefresh, exclusion before no-refresh, class-free/no-cost activation, rejection paths and immutable placement changes; 4016 source producer remains unproven; no SQL writes/deployment");
    }
}
