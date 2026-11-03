package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real item packet, imported in-memory collar and configured native recall consumer. */
public final class PetCollarProbe extends ElementalMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,int type,int mode)throws Exception{
  reset(pc,peer,hub,true);set(pc,"observer",false);set(pc,"playerHp",1000);set(pc,"playerMaxHp",1000);
  ((BattleTimedStatus)get(pc,"timedStatus")).clear();
  var original=((List<CharacterSummary>)get(pc,"characters")).get(0);var character=copy(original,Map.of("type",type,"charisma",1));set(pc,"characters",List.of(character));
  var field=CombatRules.class.getDeclaredField("current");field.setAccessible(true);var previous=(CombatRules.Snapshot)field.get(null);var values=new HashMap<>(previous.values());var properties=new Properties();
  for(int i=0;i<7;i++)if(i==type&&(mode==1||mode==2))properties.setProperty(NativePetControls.KEYS.get(i),mode==1?"-100":"100");
  SourcePetReference.configure(properties);var bonuses=NativePetControls.parse(properties);for(int i=0;i<7;i++)values.put(NativePetControls.KEYS.get(i),bonuses.get(i).doubleValue());field.set(null,copy(previous,Map.of("values",values)));
  var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");pets.clear();
  try{
   String key=(String)call(pc,"stateCharacter",new Class[]{});
   var oldInventory=pc.nativeDeInventory(0);state.saveEconomyState(key,(long)get(pc,"shopPoints"),oldInventory.catalogCounts(),oldInventory.catalogItemStates());
   var durable=state.importSourcePets(key,List.of(new SourcePet(10231,10233,(int)InventoryCatalog.PET_COLLAR,45034,"RecallPet",12,78,17,21375,0))).get(0);
   var economy=state.economy(key);var imported=RuntimeInventory.fromDurableState(economy.inventory(),economy.itemStates());NativeDeItemApplication.replace(pc,imported,0);
   var collar=pc.nativeDeInventory(0).item(durable.collarObjectId());need(collar!=null,"real imported collar is in canonical inventory");
   int initialFlutes=Math.toIntExact(pc.nativeDeInventory(0).countCatalog(InventoryCatalog.PET_CALLING_FLUTE));
   if(initialFlutes>0){var clean=pc.nativeDeInventory(0).copy();clean.removeCatalog(InventoryCatalog.PET_CALLING_FLUTE,initialFlutes);NativeDeItemApplication.replace(pc,clean,0);}
   if(mode!=3)NativeDeItemPublication.grant(pc,(int)InventoryCatalog.PET_CALLING_FLUTE,2);
   var template=SourcePetTemplate.memoryFixture(durable.npcId());
   if(mode==4)((BattleTimedStatus)get(pc,"timedStatus")).activateEarthBind(System.nanoTime(),60);
   if(mode==5)pets.add(BattleCompanion.pet(durable,template,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2));
   if(mode==6)for(int n=0;n<8;n++)pets.add(BattleCompanion.pet(new GameStateStore.PetState("other-pet",0x70001652L+n,0x90001652L+n,(int)InventoryCatalog.PET_COLLAR,45034,"OtherPet",12,78,17,21375,0),template,(int)get(pc,"positionX")+2+n,(int)get(pc,"positionY")+2));
   var live=pc.controlledCompanionSnapshots().stream().filter(BattleCompanion.Snapshot::active).toList();int used=live.stream().mapToInt(BattleCompanion.Snapshot::petCost).sum();
   int effective=Math.max(1,Math.min(254,(int)call(pc,"effectiveCharisma",new Class[]{CharacterSummary.class},character)));
   boolean capacity=new SourcePetReference(type,effective,live.size(),used).canTakePet(template.charismaCost());
   need(ClassicWorldMapCatalog.forMap((int)get(pc,"classicWorldMapId")).allowsPetRecall(),"fixture uses a real recall-enabled map");
   boolean accepted=mode<3&&capacity;int countBefore=pets.size();
   byte[] packet=new Protocol.Writer().c(Protocol.C_USE_ITEM).d(collar.objectId()).s("").bytes();
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    call(pc,"handle",new Class[]{byte[].class},packet);
    need(pets.size()==countBefore+(accepted?1:0),"real collar packet obeys configured Source capacity and native gates type="+type+" mode="+mode);
    need(pc.nativeDeInventory(0).countCatalog(InventoryCatalog.PET_CALLING_FLUTE)==(mode==3?0:2)-(accepted?1:0),"flute consumed once only after accepted recall");
    need(pc.nativeDeInventory(0).item(collar.objectId())!=null&&state.petForCollar(collar.objectId()).equals(durable),"recall retains original collar and durable pet identity");
    if(accepted){var recalled=pets.get(pets.size()-1).snapshot();need(recalled.active()&&recalled.objectId()==durable.objectId()&&recalled.collarObjectId()==collar.objectId()&&recalled.npcId()==45034,"native pet object matches imported data");}
    var self=collect(wire);var remote=collect(other);
    String effect=HexFormat.of().formatHex(Protocol.effectObject(durable.objectId(),755));
    need(self.contains(effect)==accepted,"native recall effect only after successful recall");
    if(mode==1)need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(489))),"configured insufficient charisma is visible");
    if(mode==3)need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(79))),"missing flute is visible");
    need(wire.locked+other.locked==0,"collar output is outside action locks");
    need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"recall leaves no receipt or output queue");
   }
  }finally{pets.clear();field.set(null,previous);}cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var cls:List.of(ClientSession.class,NativePetControls.class,CombatRules.class,GameStateStore.class))need(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate classes without consumer overlays");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean app:new boolean[]{false,true})for(int type=0;type<7;type++)for(int mode=0;mode<7;mode++){
   var r=root.resolve(app+"-"+type+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,type,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"all imported pet state remains memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all actor lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
