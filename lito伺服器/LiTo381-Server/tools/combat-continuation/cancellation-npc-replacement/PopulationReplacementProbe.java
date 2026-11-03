package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real population, actor constructor, collision accounting and inherited retirement. */
public final class PopulationReplacementProbe {
 static int cases,checks;
 static void need(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static int occupied(ClassicWorldMonsterPopulation p)throws Exception{return ((Map<?,Integer>)get(p,"occupied")).values().stream().mapToInt(Integer::intValue).sum();}
 static <T>T copy(T record,Map<String,Object>changes)throws Exception{
  var components=record.getClass().getRecordComponents();Class<?>[]types=new Class<?>[components.length];Object[]values=new Object[components.length];
  for(int i=0;i<components.length;i++){var c=components[i];types[i]=c.getType();var a=c.getAccessor();a.setAccessible(true);values[i]=changes.getOrDefault(c.getName(),a.invoke(record));}
  var ctor=record.getClass().getDeclaredConstructor(types);ctor.setAccessible(true);return(T)ctor.newInstance(values);
 }
 static ClassicWorldMonsterCatalog.MonsterTemplate template(NativeDeNpcTemplate n){
  return new ClassicWorldMonsterCatalog.MonsterTemplate(n.get_npcId(),n.get_nameid(),n.getImpl(),n.get_gfxid(),n.get_level(),n.get_hp(),n.get_mp(),n.get_ac(),n.get_str(),n.get_con(),n.get_dex(),n.get_wis(),n.get_int(),n.get_mr(),n.get_exp(),n.get_lawful(),n.get_size(),n.get_weakAttr(),n.get_ranged(),n.get_passispeed(),n.get_atkspeed(),n.getBowActId(),n.get_undead(),n.get_poisonatk(),n.get_paralysisatk(),n.is_agro(),n.get_damagereduction(),n.is_hard(),n.is_doppel(),n.isBoss(),n.getLightSize(),n.getChangeHead());
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[2]).toRealPath();need(Path.of(ClassicWorldMonsterPopulation.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate population");
  ResourceDatabase.load(ServerConfig.load(Path.of(args[0]),"standard"));
  var rule=NativeCancellationNpcRules.lookup(91123);need(rule!=null&&rule.type()==2&&rule.targetId()==45641&&rule.durationSeconds()==300,"actual readonly SQL replacement rule");
  var templates=NativeDeNpcTemplateResources.snapshot().npcs();var originalTemplate=template(templates.get(91123));var target=template(templates.get(rule.targetId()));
  var fixture=copy(ClassicWorldMonsterCatalog.definitions(4).get(0),Map.of("template",originalTemplate,"count",1,"groupId",0));
  for(boolean oneShot:new boolean[]{false,true})for(int mode=0;mode<12;mode++){
   var p=oneShot?ClassicWorldMonsterPopulation.questInstance(42,0x72000000L,0x72001000L,List.of(fixture)):ClassicWorldMonsterPopulation.retainedSourceSlice(42,0x72000000L,0x72001000L,List.of(fixture));
   var actors=(Map<Long,Object>)get(p,"actors");need(actors.size()==1,"one original actor");long id=actors.keySet().iterator().next();var actor=actors.get(id);int map=fixture.mapId();
   var old=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(actor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");int cells=occupied(p);long allocation=(long)get(p,"nextQuestDynamicObjectId");
   if(mode==4)set(p,"nextQuestDynamicObjectId",(long)get(p,"questDynamicObjectEnd")+1);
   if(mode==5)set(p,"nextQuestDynamicObjectId",id);
   if(mode==6)set(actor,"alive",false);if(mode==7)set(actor,"present",false);
   if(mode==10){
    var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
    try{List<Future<Boolean>> jobs=new ArrayList<>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{ready.countDown();start.await();synchronized(p){var added=p.nativeCancellationSpawn(map,id,target);if(added==null)return false;return p.nativeCancellationDelete(map,id)!=null;}}));need(ready.await(3,TimeUnit.SECONDS),"two concurrent casters ready");start.countDown();int success=0;for(var job:jobs)if(job.get(5,TimeUnit.SECONDS))success++;need(success==1&&occupied(p)==cells,"one conversion, no lost or duplicate tile ownership");}finally{pool.shutdownNow();need(pool.awaitTermination(3,TimeUnit.SECONDS),"concurrent tasks drained");}
    cases++;continue;
   }
   var created=p.nativeCancellationSpawn(mode==2?map+1:map,mode==3?id+999:id,mode==1?null:target);
   if(mode>=1&&mode<=7){need(created==null&&actors.get(id)==actor&&actors.size()==1&&occupied(p)==cells,"failed creation keeps old actor and tile mode="+mode);if(mode<=3||mode>=6)need((long)get(p,"nextQuestDynamicObjectId")==allocation,"failed construction does not consume ID");cases++;continue;}
   need(created!=null&&created.objectId()!=id&&created.npcId()==45641&&created.alive()&&created.present(),"new canonical live NPC");
   need(created.x()==old.x()&&created.y()==old.y()&&created.heading()==old.heading()&&created.mapId()==old.mapId(),"Source exact coordinates and direction");
   need((boolean)get(actor,"alive")&&(boolean)get(actor,"present")&&occupied(p)==cells+1,"old actor retained until creation succeeds and shared tile counted");
   if(mode==11){var second=p.nativeCancellationSpawn(map,id,target);need(second!=null&&occupied(p)==cells+2,"independent actor can share tile");}
   ((Map<?,?>)get(actor,"digestSeconds")).clear();((Map<Long,Integer>)get(actor,"digestSeconds")).put(123L,10);((Set<Long>)get(p,"digestActors")).add(id);
   var retired=p.nativeCancellationDelete(map,id);need(retired!=null&&!retired.present()&&!retired.alive(),"old actor retired without death combat");
   need(occupied(p)==cells+(mode==11?1:0),"deletion releases only original tile reference");
   need(((Map<?,?>)get(actor,"digestSeconds")).isEmpty()&&!((Set<?>)get(p,"digestActors")).contains(id),"digest ownership released");
   need(actors.containsKey(id)!=oneShot,"ordinary respawn slot retained; one-shot removed");
   if(mode==8)need(p.nativeCancellationDelete(map,id)==null&&occupied(p)==cells,"duplicate deletion cannot release replacement tile");
   if(mode==9){var removed=p.nativeCancellationDelete(map,created.objectId());need(removed!=null&&!actors.containsKey(created.objectId())&&occupied(p)==0,"replacement expiry primitive removes one-shot and releases tile");}
   cases++;
  }
  Files.writeString(Path.of(args[1]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"cast_dispatch_connected\":false,\"deadline_scheduler_connected\":false,\"source_database_mutated\":false}\n");
 }
}
