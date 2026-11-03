package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real skill67 ingress, actual ring inventory and original random-form factory. */
public class PolymorphMaterialProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={67};static int selectedSkill=67,selectedMode;static long targetId;static int gaps;
 static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return BattleSpellCatalog.polymorphSpell(skill);}
 static void probability(int skill){} // Shape Change has no percentile roll in its original root.
 static void cast(ClientSession pc,int skill,Object row)throws Exception {
  byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).bytes();
  call(pc,"handle",new Class[]{byte[].class},packet);
 }
 static boolean active(ClientSession pc,int skill)throws Exception {
  return get(pc,"pendingShapeChange")!=null||((BattlePolymorphStatus)get(pc,"polymorphStatus")).active(System.nanoTime())!=null;
 }
 static void ring(ClientSession pc,boolean equip)throws Exception {
  NativeDeItemPublication.grant(pc,20281,1);var inv=pc.nativeDeInventory(0);
  var item=inv.items().stream().filter(i->i.definition().catalogId()==20281).findFirst().orElseThrow();
  if(equip)need(!(item.definition().sourceArmor()?inv.toggleNativeArmor(item.objectId()):inv.toggleRing(item.objectId())).isEmpty(),"real control ring equipped");
  need(inv.controlAbilityEnabled(InventoryCatalog.require(20281).controlAbilityType())==equip,"real control ring predicate");
 }
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception {
  reset(pc,peer,hub,world);var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
  set(pc,"characters",List.of(copy(c,Map.of("type",3,"originalIntelligence",10))));
  for(var p:List.of(pc,peer)){
   set(p,"playerHp",4000);set(p,"playerMaxHp",5000);set(p,"playerMp",1000);set(p,"playerMaxMp",1000);set(p,"observer",false);
   call(p,"clearInvisibilityStateSilently",new Class[]{});((BattleTimedStatus)get(p,"timedStatus")).clear();
   ((BattlePolymorphStatus)get(p,"polymorphStatus")).clear();set(p,"pendingShapeChange",null);
  }
  set(pc,"learnedSkills",new HashSet<>(Set.of(2,67)));targetId=peer.playerObjectId();
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  set(peer,"positionX",(int)get(pc,"positionX")+1);set(peer,"positionY",(int)get(pc,"positionY"));
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,world);int skill=67;var row=BattleSpellCatalog.polymorphSpell(skill);
  if(mode==3)set(pc,"playerMp",0);if(mode==5)targetId=0x7feeeeeeL;if(mode==6)set(peer,"playerHp",0);
  if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);if(mode==8)set(peer,"invisible",true);
  if(mode==9)targetId=pc.playerObjectId();if(mode==15)set(peer,"observer",true);
  var target=mode==9?pc:peer;boolean controlled=mode!=1&&mode!=12&&mode!=13&&mode!=14;
  if(controlled)ring(target,true);if(mode==13)ring(pc,true);if(mode==14)ring(target,false);
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode==9||mode>=10&&mode<=14||mode==4&&before;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,skill,row):0)&&(int)get(pc,"playerHp")==hp-(accepted?row.hpCost():0),"Source resources mode="+mode+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material once after valid target/resources");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"reuse follows material");
   need(active(target,skill)==accepted,"target shape state only after acceptance");
   need((get(target,"pendingShapeChange")!=null)==(accepted&&controlled),"only target equipped ring enables selection");
   if(accepted&&!controlled){
    var status=(BattlePolymorphStatus)get(target,"polymorphStatus");var form=status.active(System.nanoTime());
    need(form!=null&&form.gfx()>0,"native random form selected");long end=(long)get(status,"expiresAtNanos");need(end>System.nanoTime(),"native timed duration stored");
    if(mode==12){need(status.expire(end-1)==null,"no early form expiration");need(status.expire(end)==form&&status.active(end)==null&&status.expire(end)==null,"form expires exactly once");}
   }
   if(mode==4){need((wire.raw.size()>bytes)==before,"Source shared action window");if(before)gaps++;}
   if(mode==11){int out=wire.raw.size();cast(pc,skill,row);need(wire.raw.size()==out&&(int)get(pc,"playerMp")==mp-cost(pc,skill,row)&&pc.nativeDeInventory(0).countCatalog(item)==3,"repeat packet cannot consume twice");}
   var self=collect(wire);var peers=collect(other);
   if(mode==2)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material299");
   if(accepted&&controlled)need((target==pc?self:peers).contains(HexFormat.of().formatHex(Protocol.hypertext(target.playerObjectId(),"monlist","",List.of()))),"actual monlist reaches target");
   if(mode==0||mode==9)goldens.add("world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"output unlocked and receipts drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class,BattlePolymorphStatus.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native producer/material/status");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<16;mode++){selectedMode=mode;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
