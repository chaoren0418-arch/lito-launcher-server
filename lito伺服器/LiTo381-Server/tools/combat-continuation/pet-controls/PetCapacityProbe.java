package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual private native capacity consumer, live companion list and immutable configured values. */
public final class PetCapacityProbe extends ElementalMaterialProbe {
 static int decisions;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int type,int setting)throws Exception{
  reset(pc,peer,hub,world);var original=((List<CharacterSummary>)get(pc,"characters")).get(0);
  var field=CombatRules.class.getDeclaredField("current");field.setAccessible(true);var previous=(CombatRules.Snapshot)field.get(null);var values=new HashMap<>(previous.values());var properties=new Properties();
  for(int i=0;i<7;i++)if(setting==1)properties.setProperty(NativePetControls.KEYS.get(i),""+(i*3-5));
  SourcePetReference.configure(properties);var bonuses=NativePetControls.parse(properties);for(int i=0;i<7;i++)values.put(NativePetControls.KEYS.get(i),bonuses.get(i).doubleValue());field.set(null,copy(previous,Map.of("values",values)));
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   var owned=(List<BattleCompanion>)get(pc,"tamedCompanions");
   for(int charisma:new int[]{1,9,254,500})for(int count:new int[]{0,7,8}){
    owned.clear();var character=copy(original,Map.of("type",type,"charisma",charisma));set(pc,"characters",List.of(character));
    for(int n=0;n<count;n++){var state=new GameStateStore.PetState("capacity-pet",0x70001652L+n,0x90001652L+n,(int)InventoryCatalog.PET_COLLAR,45034,"CapacityPet",12,78,17,21375,0);owned.add(BattleCompanion.pet(state,SourcePetTemplate.memoryFixture(45034),(int)get(pc,"positionX")+2+n,(int)get(pc,"positionY")+2));}
    var live=pc.controlledCompanionSnapshots().stream().filter(BattleCompanion.Snapshot::active).toList();need(live.size()==count,"real native list includes each controlled pet");int used=live.stream().mapToInt(BattleCompanion.Snapshot::petCost).sum();int effective=Math.max(1,Math.min(254,(int)call(pc,"effectiveCharisma",new Class[]{CharacterSummary.class},character)));var source=new SourcePetReference(type,effective,count,used);
    for(int requested:new int[]{0,1,6,12,1000}){boolean actual=(boolean)call(pc,"canTakeControlledCompanion",new Class[]{int.class},requested);need(actual==source.canTakePet(requested),"actual native capacity vs unchanged Source methods type="+type+" setting="+setting+" charisma="+charisma+" count="+count+" cost="+requested);decisions++;}
    if(count==8){owned.get(0).deactivate();var remaining=pc.controlledCompanionSnapshots().stream().filter(BattleCompanion.Snapshot::active).toList();need(remaining.size()==7,"real retirement releases one slot");int cost=remaining.stream().mapToInt(BattleCompanion.Snapshot::petCost).sum();need((boolean)call(pc,"canTakeControlledCompanion",new Class[]{int.class},6)==new SourcePetReference(type,effective,7,cost).canTakePet(6),"no stale cached pet cost after retirement");decisions++;}
   }
   owned.clear();need(wire.raw.size()==0&&other.raw.size()==0,"capacity query emits no packets");need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"capacity query retains no action receipts");
  }finally{((List<BattleCompanion>)get(pc,"tamedCompanions")).clear();field.set(null,previous);}cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var cls:List.of(ClientSession.class,NativePetControls.class,CombatRules.class,BattleCompanion.class))need(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate classes, no PC/helper overlay");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int type=0;type<7;type++)for(int setting=0;setting<2;setting++){
   var r=root.resolve(world+"-"+app+"-"+type+"-"+setting);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,type,setting);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only fixture");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all actor lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"decisions\":"+decisions+",\"checks\":"+ownChecks+",\"collar_recall_not_yet_tested\":true,\"full_goal_achieved\":false}\n");
 }
}
