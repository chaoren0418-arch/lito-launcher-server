package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.io.*;import java.net.Socket;
/** Queued native packets against actual replacement, death/respawn and STOPPED transitions. */
public final class PetWorldPublicationProbe extends ElementalMaterialProbe {
 static int gaps;
 static final AtomicInteger npcLockedWrites=new AtomicInteger();
 static void observeNpcMonitors(ClientSession pc,GameHub hub,ClassicWorldMonsterPopulation population)throws Exception{
  var old=(Socket)get(pc,"socket");set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(old.getOutputStream()){
   public void write(byte[]bytes,int offset,int count)throws IOException{if(Thread.holdsLock(hub)||Thread.holdsLock(population))npcLockedWrites.incrementAndGet();out.write(bytes,offset,count);}
   public void write(int value)throws IOException{write(new byte[]{(byte)value},0,1);}
  };}public void close(){}});
 }
 static boolean clean()throws Exception{
  if(!before)for(String name:List.of("ingress","current")){var f=Class.forName("local.lito381.server.NativePetWorldLifetime").getDeclaredField(name);f.setAccessible(true);if(((ThreadLocal<?>)f.get(null)).get()!=null)return false;}
  return NativeLightStoppedProbe.idleContext();
 }
 static ClassicWorldMonsterPopulation population(ClassicWorldMonsterCatalog.SpawnDefinition definition,int skill){
  var p=ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e0000ffL,List.of(definition));var target=p.actor(definition.mapId(),0x5e000001L);if(skill==41)p.damage(definition.mapId(),target.objectId(),Integer.MAX_VALUE);return p;
 }
 static Object actor(ClassicWorldMonsterPopulation p,long id)throws Exception{return ((Map<?,?>)get(p,"actors")).get(id);}
 static Object epoch(Object actor)throws Exception{return NativeCombatAttacker.token((BattleTimedStatus)get(actor,"damagePoisonStatus"),false);}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,int skill,int mode)throws Exception{
  reset(pc,peer,hub,true);set(pc,"observer",false);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"learnedSkills",new HashSet<>(Set.of(skill)));((BattleTimedStatus)get(pc,"timedStatus")).clear();
  var character=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(character,Map.of("type",3,"charisma",25,"originalIntelligence",10))));var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");pets.clear();
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var prior=(CombatDatabase.Snapshot)field.get(null);var probabilities=new HashMap<>(prior.controls().probabilities());probabilities.put(36,new SourceSkillControls.Probability(36,50,50,50,0,0,0,0,false,false,0,0,0));field.set(null,copy(prior,Map.of("controls",copy(prior.controls(),Map.of("probabilities",probabilities)))));
  try{
   int map=(int)get(pc,"classicWorldMapId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
   var template=ClassicWorldMonsterCatalog.definitions(map).stream().map(ClassicWorldMonsterCatalog.SpawnDefinition::template).filter(t->!t.boss()&&SourceNpcPhysicalStats.memoryFixture(t.npcId()).tamable()).findFirst().orElseThrow();
   var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"pet-lifetime-fixture",1,0,x,y,0,0,0,0,0,0,0,0,0,false,30,false,0,template);definition=copy(definition,Map.of("minRespawnSeconds",60,"maxRespawnSeconds",60));
   var population=population(definition,skill);set(hub,"classicWorldMonsters",population);long id=0x5e000001L;need(population.actor(map,id)!=null,"actual deterministic native actor");
   Object spell=skill==36?BattleSpellCatalog.tamingSpell(skill):BattleSpellCatalog.createZombieSpell(skill);int material=value(spell,"itemConsumeId"),count=value(spell,"itemConsumeCount"),mpCost=cost(pc,skill,spell);NativeDeItemPublication.grant(pc,material,count+3);
   var writer=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(id);if(skill==36)writer.h(x).h(y);byte[]packet=writer.bytes();
   var failure=new AtomicReference<Throwable>();var cleaned=new AtomicBoolean();var life=NativePlayerLifetime.of(pc);ClassicWorldMonsterPopulation replacement=null;
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    observeNpcMonitors(pc,hub,population);observeNpcMonitors(peer,hub,population);
    if(mode<7){
     var lock=pc.nativePlayerActionLock();Thread worker=new Thread(()->{try{call(pc,"handle",new Class[]{byte[].class},packet);cleaned.set(clean());}catch(Throwable error){failure.set(error);}},"pet-world-queued");
     lock.lock();try{
      worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
      need(lock.hasQueuedThread(worker)&&NativePlayerVitalCompletions.pending(life)==1,"actual native ingress reserved before lock wait");need(wire.raw.size()==0&&(int)get(pc,"playerMp")==1000,"no premature commit");
      if(mode==1){replacement=population(definition,skill);set(hub,"classicWorldMonsters",replacement);}
      if(mode==2){replacement=population(definition,skill);synchronized(population){((Map<Long,Object>)get(population,"actors")).put(id,actor(replacement,id));}}
      if(mode==3){var object=actor(population,id);Object generation=epoch(object);if(skill==36)population.damage(map,id,Integer.MAX_VALUE);need(population.removeCorpse(map,id)!=null,"actual corpse retired");need(population.respawn(map,id)!=null,"actual same-ID actor respawned");if(skill==41)population.damage(map,id,Integer.MAX_VALUE);if(!before)need(epoch(object)!=generation,"actual lifecycle advances NPC generation without pre-existing curse");}
      if(mode==4){if(skill==36)population.damage(map,id,Integer.MAX_VALUE);else need(population.resurrect(map,id,template.hp())!=null,"actual corpse resurrected");}
      if(mode==5)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
      if(mode==6)NativePlayerLifetime.shutdown(hub,false,0);
     }finally{lock.unlock();worker.join(3000);}
     if(failure.get()!=null)throw new AssertionError("queued native pet cast",failure.get());need(!worker.isAlive()&&cleaned.get(),"worker and scopes drained");
     boolean accepted=mode==0||mode==6||before&&(mode>=1&&mode<=3);if(before&&mode>=1&&mode<=3)gaps++;
     need(pets.size()==(accepted?1:0)&&pc.nativeDeInventory(0).countCatalog(material)==count+3-(accepted?count:0)&&(int)get(pc,"playerMp")==1000-(accepted?mpCost:0),"only originally bound live NPC/corpse may consume and convert mode="+mode+" before="+before);
     if(!accepted)need(wire.raw.size()==0&&other.raw.size()==0,"stale ingress publishes no packets");
     if(mode==6){int size=wire.raw.size();call(pc,"handle",new Class[]{byte[].class},packet);need(wire.raw.size()==size&&pets.size()==1,"new STOPPED producer denied");}
    }else{
     var replacementPopulation=population(definition,skill);
     NativeSourcePlayerActionLock.run(pc,()->{try{call(pc,"handle",new Class[]{byte[].class},packet);need(pets.size()==1&&wire.raw.size()==0&&other.raw.size()==0,"accepted conversion defers outputs");if(skill==36)need(population.releaseTamed(map,id,pets.get(0).snapshot())!=null,"actual captured NPC released into same actor slot");else need(population.respawn(map,id)!=null,"actual consumed corpse respawned");}catch(Throwable error){failure.set(error);}});
     if(failure.get()!=null)throw new AssertionError(failure.get());var self=collect(wire);var peers=collect(other);String removed=HexFormat.of().formatHex(Protocol.removeObject(id));boolean leaked=self.contains(removed)||peers.contains(removed);need(leaked==(before&&skill==36),"deferred old removal cannot erase same-slot released NPC");if(leaked)gaps++;
     need(self.contains(HexFormat.of().formatHex(Protocol.manaPoint(1000-mpCost,1000))),"accepted caster mana publication retained");
    }
    collect(wire);collect(other);need(wire.locked+other.locked==0&&npcLockedWrites.get()==0&&NativePlayerVitalCompletions.pending(life)==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&clean(),"unlocked player/NPC/hub network and no scope/receipt leaks");
   }
  }finally{pets.clear();field.set(null,prior);}cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var cls:List.of(ClientSession.class,ClassicWorldMonsterPopulation.class,NativePetMaterialAction.class))need(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and NPC factory");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean app:new boolean[]{false,true})for(int skill:new int[]{36,41})for(int mode=7;mode<8;mode++){
   var r=root.resolve(app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("tamingMonsterFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    var petClock=new ScheduledThreadPoolExecutor(1,task->{var t=new Thread(task,"pet-lifetime-fixture");t.setDaemon(true);return t;});petClock.setRemoveOnCancelPolicy(true);set(hub,"battleClock",petClock);try{exercise(p.session,m.session,hub,skill,mode);}finally{petClock.shutdownNow();need(petClock.awaitTermination(2,TimeUnit.SECONDS),"corpse scheduler closed");}
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only runtime");
  }}catch(Throwable error){primary=error;throw error;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable error){if(primary!=null)primary.addSuppressed(error);else throw error;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"stale_target_or_publication_gaps\":"+gaps+",\"locked_writes\":"+lockedWrites+",\"frames\":"+frames+",\"battle_encounters_qualified\":false,\"full_goal_achieved\":false}\n");
 }
}
