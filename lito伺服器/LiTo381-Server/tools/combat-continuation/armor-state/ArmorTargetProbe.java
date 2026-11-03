package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Selector-only fallback and explicit companion rejection, with actual native packet dispatch. */
public final class ArmorTargetProbe extends ArmorMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,ClientSession first,ClientSession other,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var row=BattleSpellCatalog.armorBreakSpell(112);
  set(first,"observer",false);set(other,"observer",false);
  ClientSession extra=world?first:hub.canAttack((long)get(pc,"currentRoomId"),pc,first)?first:other;
  need(world||hub.canAttack((long)get(pc,"currentRoomId"),pc,extra),"second enemy available for ambiguity fixture");
  set(first,"observer",true);set(other,"observer",true);
  set(pc,"lastCombatTargetObjectId",mode==1?0x7feeeeeeL:mode==7?pc.playerObjectId():0L);
  if(mode==2||mode==3){set(extra,"observer",false);set(extra,"positionX",get(peer,"positionX"));set(extra,"positionY",get(peer,"positionY"));if(mode==3)set(pc,"lastCombatTargetObjectId",peer.playerObjectId());}
  if(mode==4||mode==5){
   var pet=BattleCompanion.lesserElemental(0x5f000003L,SourceSummonTemplate.memoryFixture(45306),6,600,(int)get(peer,"positionX"),(int)get(peer,"positionY"),System.nanoTime());
   ((List<BattleCompanion>)get(mode==4?pc:peer,"tamedCompanions")).add(pet);set(pc,"lastCombatTargetObjectId",pet.snapshot().objectId());
  }
  if(mode==6){
   set(peer,"observer",true);
   var type=Class.forName("local.lito381.server.ClientSession$ArmorBreakTarget");var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
   var npc=constructor.newInstance(2L,(int)get(pc,"positionX")+1,(int)get(pc,"positionY"),1,null,null,null,null,true);
   need(CombatRules.loaded()&&CombatNpcDebuffs.armorTarget(npc,pc)==null,"Source NPC-only candidate refused by actual Armor Break filter");
  }
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var recipient=new SourceActionPairProbe.Wire(peer,pc)){
   boolean accepted=mode==0||mode==1||mode==3||mode==7;cast(pc,112,row);
   need((int)get(pc,"playerMp")==1000-(accepted?cost(pc,112,row):0)&&(int)get(pc,"playerHp")==4000-(accepted?row.hpCost():0),"selector target resource decision mode="+mode+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"only eligible selector consumes materials");
   need(active(peer,hub,world)==accepted,"unique/explicit PC receives effect; companion/NPC/ambiguity rejected");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(112)==accepted,"selector rejection creates no reuse");
   var self=collect(wire);var peers=collect(recipient);if(!accepted)need(self.isEmpty()&&peers.isEmpty(),"rejected selector emits no unrelated effect");
   goldens.add("world="+world+" mode="+mode+" self="+self+" peer="+peers);
   need(wire.locked+recipient.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"selector output unlocked; no receipt leak");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<8;mode++){
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("armorBreakFixtureD100",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    set(f.session,"observer",true);set(o.session,"observer",true);exercise(p.session,m.session,f.session,o.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
