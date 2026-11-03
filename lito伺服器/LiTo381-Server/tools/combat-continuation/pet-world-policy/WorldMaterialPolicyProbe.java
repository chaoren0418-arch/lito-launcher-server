package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.lang.reflect.*;import java.sql.*;import com.zaxxer.hikari.HikariDataSource;
/** Actual native pet cast and Legacy JDBC policy; only the JDBC transport is observed. */
public final class WorldMaterialPolicyProbe extends ElementalMaterialProbe {
 static int lockedReads,queries,invalidEarlyReads,replacedDuringRead;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,int skill,int mode,String database)throws Exception{
  reset(pc,peer,hub,true);set(pc,"observer",false);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"learnedSkills",new HashSet<>(Set.of(skill)));((BattleTimedStatus)get(pc,"timedStatus")).clear();
  var character=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(character,Map.of("type",3,"charisma",25,"originalIntelligence",10))));var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");pets.clear();
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var prior=(CombatDatabase.Snapshot)field.get(null);var probabilities=new HashMap<>(prior.controls().probabilities());probabilities.put(36,new SourceSkillControls.Probability(36,50,50,50,0,0,0,0,false,false,0,0,0));field.set(null,copy(prior,Map.of("controls",copy(prior.controls(),Map.of("probabilities",probabilities)))));
  var accounts=(AccountRepository)get(pc,"accounts");
  try(var jdbc=new NativeCastMaterialPolicyProbe.Jdbc()){
   int map=(int)get(pc,"classicWorldMapId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
   var template=ClassicWorldMonsterCatalog.definitions(map).stream().map(ClassicWorldMonsterCatalog.SpawnDefinition::template).filter(t->!t.boss()&&SourceNpcPhysicalStats.memoryFixture(t.npcId()).tamable()).findFirst().orElseThrow();
   var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"policy-fixture",1,0,x,y,0,0,0,0,0,0,0,0,0,false,30,false,0,template);definition=copy(definition,Map.of("minRespawnSeconds",60,"maxRespawnSeconds",60));
   var population=ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e0000ffL,List.of(definition));set(hub,"classicWorldMonsters",population);long id=0x5e000001L;
   if((skill==41)!=(mode==5))population.damage(map,id,Integer.MAX_VALUE);
   Object spell=skill==36?BattleSpellCatalog.tamingSpell(skill):BattleSpellCatalog.createZombieSpell(skill);int item=value(spell,"itemConsumeId"),count=value(spell,"itemConsumeCount"),mpCost=cost(pc,skill,spell);if(mode!=6)NativeDeItemPublication.grant(pc,item,count+3);
   var sql=NativeCastMaterialPolicyProbe.repository(jdbc);set(sql,"resourceDatabase",database);
   var observed=new HikariDataSource(){public Connection getConnection(){queries++;if(Thread.holdsLock(hub)||Thread.holdsLock(population))lockedReads++;need(pc.nativePlayerActionLock().isHeldByCurrentThread(),"role read retains player authority");
    if(mode==7&&!before){var done=new java.util.concurrent.atomic.AtomicBoolean();var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();var worker=new Thread(()->{try{synchronized(hub){synchronized(population){if(skill==36)population.damage(map,id,Integer.MAX_VALUE);need(population.removeCorpse(map,id)!=null,"original corpse retired during SQL");need(population.respawn(map,id)!=null,"same slot respawned during SQL");if(skill==41)population.damage(map,id,Integer.MAX_VALUE);done.set(true);}}}catch(Throwable e){error.set(e);}},"policy-read-world-progress");worker.start();try{worker.join(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}need(done.get()&&!worker.isAlive()&&error.get()==null,"another world actor progresses while policy read is outstanding");replacedDuringRead++;}
    return jdbc.getConnection();}};
   set(sql,"dataSource",observed);
   var routed=(AccountRepository)Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{if(m.getName().equals("repositoryForClassicSource"))return sql;try{m.setAccessible(true);return m.invoke(accounts,a);}catch(InvocationTargetException e){throw e.getCause();}});set(pc,"accounts",routed);
   NativeSourcePlayerActionLock.run(pc,()->NativeReuseGate.actor(pc).materialPolicy=null);
   jdbc.level=mode==6?200:0;jdbc.mode=switch(mode){case 1,5->"query";case 2->"rollback";case 3->"close";case 4->"missing";default->"ok";};
   var writer=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(id);if(skill==36)writer.h(x).h(y);byte[]packet=writer.bytes();
   int reads=queries,locked=lockedReads;
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    call(pc,"handle",new Class[]{byte[].class},packet);boolean accepted=mode==0||mode==6||mode==7&&before;
    need(pets.size()==(accepted?1:0)&&(int)get(pc,"playerMp")==1000-(accepted?mpCost:0),"valid target and original policy failure gate effects/cost");
    need(pc.nativeDeInventory(0).countCatalog(item)==(mode==6?0:count+3-(accepted?count:0)),"failure and GM exemption preserve source material semantics");
    int expectedReads=mode==5&&before?0:1;need(queries-reads==expectedReads,"one cold role read; invalid target preparation is read-only");
    need(lockedReads-locked==(before&&mode!=5?1:0),"no JDBC while holding shared world monitors");
    var self=collect(wire);var peers=collect(other);
    if(mode==5){need(self.isEmpty()&&peers.isEmpty(),"deferred read failure never changes invalid-target rejection");if(!before)invalidEarlyReads++;}
    if(mode==7&&!before)need(self.isEmpty()&&peers.isEmpty(),"NPC replaced during policy read rejects the original cast before resources or packets");
    if(mode>=1&&mode<=4){need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"original policy failure remains exactly material299");need(jdbc.closed&&jdbc.statementClosed&&jdbc.trace.contains("rollback"),"read failure closes all resources");need(pc.nativeCastTimelineState().reuseActor.materialPolicy==null,"failed role lookup is not cached");jdbc.mode="ok";call(pc,"handle",new Class[]{byte[].class},packet);need(pets.size()==1&&(int)get(pc,"playerMp")==1000-mpCost&&pc.nativeDeInventory(0).countCatalog(item)==3,"immediate retry succeeds after policy recovery");}
    if(mode!=5){int cached=queries;NativeSourcePlayerActionLock.run(pc,()->{try{need(NativeCastMaterialPolicy.gm(pc)==(mode==6),"cached source role policy");}catch(Exception e){throw new RuntimeException(e);}});need(queries==cached,"completed role policy uses immutable cache");}
    collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativeLightStoppedProbe.idleContext(),"unlocked network and released native receipt");
    if(!before){var local=NativeWorldMaterialPolicy.class.getDeclaredField("current");local.setAccessible(true);need(((ThreadLocal<?>)local.get(null)).get()==null,"prepared policy scope released");}
   }finally{observed.close();}
  }finally{set(pc,"accounts",accounts);pets.clear();field.set(null,prior);}cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var c:List.of(ClientSession.class,NativeCastMaterialPolicy.class,NativePetWorldLifetime.class,LegacyMySqlRepository.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate methods");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);String database=UnifiedConfiguration.read(base.source()).getProperty("database.static.database");var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean app:new boolean[]{false,true})for(int skill:new int[]{36,41})for(int mode=0;mode<8;mode++){
   var r=root.resolve(app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("tamingMonsterFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    var petClock=new ScheduledThreadPoolExecutor(1,task->{var t=new Thread(task,"policy-fixture");t.setDaemon(true);return t;});set(hub,"battleClock",petClock);try{exercise(p.session,m.session,hub,skill,mode,database);}finally{petClock.shutdownNow();need(petClock.awaitTermination(2,TimeUnit.SECONDS),"corpse scheduler closed");}
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only role state");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"policy_queries\":"+queries+",\"locked_policy_queries\":"+lockedReads+",\"invalid_target_read_only_preparations\":"+invalidEarlyReads+",\"npc_replaced_during_policy_read\":"+replacedDuringRead+",\"locked_writes\":"+lockedWrites+",\"frames\":"+frames+",\"full_goal_achieved\":false}\n");
 }
}
