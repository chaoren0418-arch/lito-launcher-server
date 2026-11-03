package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual cccHH ground packets, placement gates and canonical material ownership. */
public class GroundMaterialProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={58,63};static int selectedSkill,selectedMode,targetX,targetY,gaps;
 static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return BattleSpellCatalog.groundSpell(skill);}
 static void cast(ClientSession pc,int skill,Object row)throws Exception{call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).h(targetX).h(targetY).bytes());}
 static Object room(ClientSession pc,GameHub hub)throws Exception{return ((Map<?,?>)get(hub,"rooms")).get((long)get(pc,"currentRoomId"));}
 static int active(ClientSession pc,GameHub hub,boolean world)throws Exception{
  if(!world)return ((BattleGroundEffects)get(room(pc,hub),"groundEffects")).activeCount();
  int count=0;for(var effects:((Map<?,ClassicWorldGroundEffects>)get(hub,"classicWorldGroundEffects")).values())count+=effects.activeCount(System.nanoTime());return count;
 }
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception{
  reset(pc,peer,hub,world);var c=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(c,Map.of("type",3,"originalIntelligence",10))));
  set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"observer",false);set(pc,"learnedSkills",new HashSet<>(Set.of(2,58,63)));call(pc,"clearInvisibilityStateSilently",new Class[]{});
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  int sx=(int)get(pc,"positionX"),sy=(int)get(pc,"positionY");targetX=sx+1;targetY=sy;
  var row=BattleSpellCatalog.groundSpell(selectedSkill);boolean found=false;
  for(int[]direction:List.of(new int[]{1,0},new int[]{0,1},new int[]{-1,0},new int[]{0,-1})){
   int x=sx+direction[0],y=sy+direction[1];boolean valid;
   if(world){var map=ClassicWorldMapCatalog.forMap((int)get(pc,"classicWorldMapId"));valid=map.standingPassable(x,y)&&new ClassicWorldGroundEffects().cast(row,pc.playerObjectId(),sx,sy,x,y,map,System.nanoTime()).accepted();}
   else{var map=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));valid=map.nativeStandingFloor(x,y)&&new BattleGroundEffects().cast(row,pc.playerObjectId(),sx,sy,x,y,map,System.nanoTime()).accepted();}
   if(valid){targetX=x;targetY=y;found=true;break;}
  }
  need(found,"real native geometry offers valid ground position");set(peer,"positionX",sx+2);set(peer,"positionY",sy);
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;var row=BattleSpellCatalog.groundSpell(skill);
  if(mode==2)set(pc,"playerMp",0);if(mode==4)targetX=(int)get(pc,"positionX")+40;if(mode==5)set(pc,"playerHp",0);if(mode==6)set(pc,"observer",true);
  if(mode==7){if(world)((Map<?,?>)get(hub,"classicWorldMembership")).remove(pc);else set(room(pc,hub),"battleStarted",false);}
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(item!=0&&mode!=1)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==3){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light action window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==8||mode==1&&item==0||mode==3&&before;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0)))&&(int)get(pc,"playerHp")==hp,"ground Source resources mode="+mode+" world="+world+" skill="+skill);
   if(item!=0)need(pc.nativeDeInventory(0).countCatalog(item)==(mode==1?0:count+3-(accepted?count:0)),"ground material exactly once");
   need((active(pc,hub,world)>0)==accepted,"live ground lease only after accepted placement and material");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"rejected ground placement has no reuse");
   if(mode==3){need((wire.raw.size()>bytes)==before,"ground shared Source action window");if(before)gaps++;}
   if(mode==8){int installed=active(pc,hub,world),spent=(int)get(pc,"playerMp"),sent=wire.raw.size();cast(pc,skill,row);need(active(pc,hub,world)==installed&&(int)get(pc,"playerMp")==spent&&wire.raw.size()==sent,"immediate replay cannot install or charge twice");}
   var self=collect(wire);var peers=collect(other);
   if(mode==1&&item!=0)need(self.equals(before?List.of():List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material legacy silence becomes visible299");
   if(mode==7)need(self.isEmpty()&&peers.isEmpty(),"actual placement rejection has no cast packets");
   if(mode==0)goldens.add("skill="+skill+" world="+world+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"ground output unlocked and receipts drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<9;mode++){selectedSkill=skill;selectedMode=mode;
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
