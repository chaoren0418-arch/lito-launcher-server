package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual selected roots with existing deterministic roll arguments; one native packet case per skill/phase/configuration. */
public class TargetNegativeMaterialProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={173,174};static int selectedSkill,selectedMode;static long targetId;static int gaps;
 static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return skill==173?BattleSpellCatalog.polluteWaterSpell(skill):BattleSpellCatalog.strikerGaleSpell(skill);}
 static void cast(ClientSession pc,int skill,Object row)throws Exception {
  byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).h((int)get(pc,"positionX")+1).h((int)get(pc,"positionY")).bytes();
  dispatch(pc,packet,row,skill,selectedMode);
 }
 static void dispatch(ClientSession pc,byte[] packet,Object row,int skill,int mode)throws Exception{
  if(mode==12){call(pc,"handle",new Class[]{byte[].class},packet);return;}
  var kind=Class.forName("local.lito381.server.ClientSession$TargetNegativeStatusKind");Object value=Enum.valueOf((Class)kind,skill==173?"POLLUTE_WATER":"STRIKER_GALE");
  call(pc,"useTargetNegativeStatusSpell",new Class[]{byte[].class,BattleSpellCatalog.TargetNegativeStatusSpell.class,int.class,kind},packet,row,mode==1?100:1,value);
 }
 static void probability(int skill)throws Exception{
  var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var data=(CombatDatabase.Snapshot)f.get(null);
  var rows=new HashMap<>(data.controls().probabilities());rows.put(skill,new SourceSkillControls.Probability(skill,50,50,50,0,0,0,0,false,false,0,0,0));
  f.set(null,copy(data,Map.of("controls",copy(data.controls(),Map.of("probabilities",rows)))));
 }
 static boolean active(ClientSession pc,int skill)throws Exception{
  return skill==173?((BattleTimedStatus)get(pc,"timedStatus")).polluteWaterActive(System.nanoTime()):((BattleTimedStatus)get(pc,"timedStatus")).strikerGaleActive(System.nanoTime());
 }
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception {
  reset(pc,peer,hub,world);for(var target:List.of(pc,peer))set(target,"elfAttribute",(int)call(spell(selectedSkill),"attribute",new Class[]{}));
  var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
  set(pc,"characters",List.of(copy(c,Map.of("type",2,"originalIntelligence",10))));
  for(var p:List.of(pc,peer)){
   set(p,"playerHp",4000);set(p,"playerMaxHp",5000);set(p,"playerMp",1000);set(p,"playerMaxMp",1000);set(p,"observer",false);
   call(p,"clearInvisibilityStateSilently",new Class[]{});((BattleTimedStatus)get(p,"timedStatus")).clear();((BattleEraseMagicStatus)get(p,"eraseMagicStatus")).clear();
  }
  set(pc,"learnedSkills",new HashSet<>(Set.of(2,173,174)));targetId=peer.playerObjectId();
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  set(peer,"positionX",(int)get(pc,"positionX")+1);set(peer,"positionY",(int)get(pc,"positionY"));

 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);
  if(mode==3)set(pc,"playerMp",0);if(mode==5)targetId=0x7feeeeeeL;if(mode==6)set(peer,"playerHp",0);
  if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);if(mode==8)set(peer,"invisible",true);
  if(mode==9)targetId=pc.playerObjectId();if(mode==10)((BattleTimedStatus)get(peer,"timedStatus")).activateCounterMagic(System.nanoTime(),60);
  if(mode==11)((BattleTimedStatus)get(peer,"timedStatus")).activateHaste(System.nanoTime(),60,43);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode>=10||mode==4&&before;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0)))&&(int)get(pc,"playerHp")==hp,"Source resources mode="+mode+" skill="+skill+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material once after valid target and resources");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"reuse follows accepted material");
   if(mode!=12){boolean applied=accepted&&mode!=1&&mode!=10;need(active(peer,skill)==applied&&!active(pc,skill),"actual target status follows Source roll/counter/haste rules mode="+mode+" skill="+skill);}
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared Source time window");if(before)gaps++;}
   if(mode==13){
    var timed=(BattleTimedStatus)get(peer,"timedStatus");
    if(skill==173){
     NativeDeItemPublication.grant(peer,40010,3);var potion=peer.nativeDeInventory(0).items().stream().filter(i->i.definition().catalogId()==40010).findFirst().orElseThrow();int initial=(int)get(peer,"playerHp");long quantity=peer.nativeDeInventory(0).countCatalog(40010);
     var use=new Protocol.Writer().c(Protocol.C_USE_ITEM).d(potion.objectId());if(potion.definition().useType()==10)use.d(12345);else use.s("");call(peer,"handle",new Class[]{byte[].class},use.bytes());
     int restored=(int)get(peer,"playerHp")-initial;int[] range=InventoryCatalog.healingRange(40010);need(restored>=range[0]/2&&restored<=range[1]/2&&peer.nativeDeInventory(0).countCatalog(40010)==quantity-1,"real native healing potion follows active Pollute Water half-recovery and consumes once");
     need(timed.expirePolluteWater((long)get(timed,"polluteWaterExpiresAtNanos"))&&!active(peer,skill),"real Pollute Water deadline expires");
    }else{
     var field=CombatRules.class.getDeclaredField("current");field.setAccessible(true);var original=(CombatRules.Snapshot)field.get(null);var values=new HashMap<>(original.values());values.put("STRIKER_DMG2",2.75);
     try{field.set(null,copy(original,Map.of("values",values)));for(int type:new int[]{4,13,10,1,17})need(BattleStrikerGale.damageAfterStatus(101,active(peer,skill),BattleStrikerGale.playerWeaponEligible(type))==(type==1||type==17?101:277),"actual status feeds Source weapon categories and configured ranged multiplier; melee unchanged");
      need(timed.expireStrikerGale((long)get(timed,"strikerGaleExpiresAtNanos"))&&!active(peer,skill)&&BattleStrikerGale.damageAfterStatus(101,active(peer,skill),true)==101,"expired status stops configured ranged multiplier");
     }finally{field.set(null,original);}
    }
   }
   var self=collect(wire);var peers=collect(other);
   if(mode==2)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material299");
   if(mode==0||mode==1||mode==10||mode==11)goldens.add("skill="+skill+" world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<14;mode++){selectedSkill=skill;selectedMode=mode;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
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
