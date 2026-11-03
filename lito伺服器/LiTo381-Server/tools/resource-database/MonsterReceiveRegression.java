package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
/** Real native producers and HP boundary, independently backed by the source kernel oracle. */
public class MonsterReceiveRegression extends MonsterDamageCallbackRegression {
 static void receiveBound(ClientSession pc,ClientSession peer,GameHub hub,ClassicWorldMonsterPopulation pop,CaptureLog capture)throws Exception{
  int n=3700;
  for(int type=0;type<7;type++)for(int producer=0;producer<5;producer++){
   CounterDetectionRegression.type(pc,type);var a=spawn(pop,n++);long id=a.objectId();var t=timed(pop,id);var other=pop.spawnQuestMonsterAt(4,960000+n++,33091,32717,0,copy(ClassicWorldMonsterCatalog.definitions(4).get(0).template(),Map.of("hp",50000)));check(other!=null,"independent nearby NPC spawned");
   if(producer<3)produce(pc,companionDamage(pc,List.of(BattleCompanion.Kind.SUMMON,BattleCompanion.Kind.PET,BattleCompanion.Kind.TAMED).get(producer)),a);else if(producer==3)spell(pc,a);else venom(pc,a,true);
   check(damage(t)!=null,"actual NPC poison source admitted before EarthBind");check(pop.earthBind(4,id,System.nanoTime(),60)&&pop.earthBind(4,other.objectId(),System.nanoTime(),60),"two NPC actors protected by real native EarthBind status");
   check(pop.damage(4,id,pc.playerObjectId(),7)==null&&hp(pop,id)==50000,"ordinary positive damage still observes original EarthBind guard");long offset=Files.size(capture.path());tick(pc,next(t));
   check(hp(pop,id)==49995,"source admitted poison HP input survives EarthBind producer="+producer+" class="+type);check(pop.earthBindActive(4,id,System.nanoTime()),"poison commit never clears authoritative EarthBind state");check(occurrences(frameTail(capture,offset),"source=Curse-Poison-tick")==2,"actual native HP publication per eligible recipient");
   check(pop.damage(4,id,7)==null&&hp(pop,id)==49995,"poison binding is removed before the next raw damage call");check(pop.damage(4,other.objectId(),7)==null&&hp(pop,other.objectId())==50000,"unrelated actor guard remains active");
   castMonster(peer,a,9);cleanDamage(pop,id,t);check(pop.cancelEarthBind(4,id)&&pop.cancelEarthBind(4,other.objectId()),"fixture ends independent protection");pop.damage(4,id,50000);pop.damage(4,other.objectId(),50000);
  }
 }
 public static void main(String[] args)throws Exception{
  var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("curseParalysisFixturePercentileRoll",1,"classicWorldMonsterPoisonFixtureRoll",1,"cursePoisonFixtureDieRoll",1,"cursePoisonFixturePercentileRoll",1,"enchantVenomProcFixtureRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false));ResourceDatabase.load(config);var root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);AccountRepository memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);AccountRepository accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(MonsterReceiveRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{if(m.getName().equals("repositoryForClassicSource"))return p;if(m.getName().equals("npcPhysicalStats"))return copy(SourceNpcPhysicalStats.memoryFixture((int)a[0]),Map.of("magicResistance",0,"boss",false));try{return m.invoke(memory,a);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}});
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,TimeUnit.SECONDS),"native clock stopped for controlled damage callbacks");var fixtures=List.of(f,p);for(int i=0;i<2;i++){var pc=fixtures.get(i).session;NativeDirectoryRegression.init(fixtures.get(i),"MonsterReceive"+i,0,config.classicServerNames().get(0));var c=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(c,Map.of("objectId",0x38d00001L+i,"intelligence",35,"originalIntelligence",35))));set(pc,"sourceBaseMagicResistance",new SourceBaseMagicResistance(0,0,0,0,0));set(pc,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());set(pc,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());set(pc,"phase",Enum.valueOf((Class)get(pc,"phase").getClass(),"WORLD"));set(pc,"classicWorldMapId",4);set(pc,"classicWorldInstanceId",-1);set(pc,"positionX",33089+i);set(pc,"positionY",32717);hub.enterClassicWorld(pc,4,33089+i,32717,0);}
   var pop=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");receiveBound(f.session,p.session,hub,pop,capture);
  }System.out.println("PASS WORLD NPC poison receive checks="+checks+"; admitted poison bypasses EarthBind only at HP call; normal positive damage and other actors keep their guard; scope cleanup and no deployment");
 }
}
