package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
public final class InvisibilityControlProbe extends NativeMagicalWeaponProbe {
 static CombatRules.Snapshot original;static final List<String> packets=new ArrayList<>();
 static void install(CombatRules.Snapshot s)throws Exception{var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);f.set(null,s);}
 static void flag(boolean enabled)throws Exception{var values=new HashMap<>(original.values());values.put("INVIS_CAST_BREAK",enabled?1.0:0.0);install(copy(original,Map.of("values",values)));}
 static void invoke(ClientSession pc,int skill)throws Exception{var packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7);var spell=BattleSpellCatalog.invisibilitySpell(skill);if(spell!=null&&spell.requestShape()==BattleSpellCatalog.RequestShape.SELF_TICK)packet.d(17);call(pc,"handle",new Class[]{byte[].class},packet.bytes());}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,boolean enabled,int skill,int initial,boolean noMp)throws Exception {
  reset(pc,peer,hub,world);flag(enabled);var chars=(List<CharacterSummary>)get(pc,"characters");set(pc,"characters",List.of(copy(chars.get(0),Map.of("type",skill==97?4:2,"originalIntelligence",10))));set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",noMp?0:1000);set(pc,"playerMaxMp",1000);set(pc,"learnedSkills",new HashSet<>(Set.of(skill)));set(pc,"observer",false);call(pc,"clearInvisibilityStateSilently",new Class[]{});
  int prior=initial==0?0:initial==1?60:97;if(prior!=0)call(pc,"activateInvisibilityState",new Class[]{int.class,long.class,int.class},prior,System.nanoTime(),120);
  int reagent=CombatDatabase.value(0,skill,"InvisibilityProbe","itemConsumeId"),count=CombatDatabase.value(0,skill,"InvisibilityProbe","itemConsumeCount");if(reagent>0)NativeDeItemPublication.grant(pc,reagent,count+2);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)) {
   invoke(pc,skill);boolean accepted=!noMp;boolean expected=noMp?prior!=0:skill==2?!enabled&&prior!=0:!(enabled&&prior==skill);
   need((boolean)get(pc,"invisible")==expected,"actual visibility world="+world+" flag="+enabled+" skill="+skill+" initial="+prior+" noMp="+noMp);need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"only accepted casts commit cooldown");need(((int)get(pc,"playerMp")<(noMp?0:1000))==accepted,"cost follows actual admission");if(reagent>0)need(pc.nativeDeInventory(0).countCatalog(reagent)==count+2-(accepted?count:0),"material once or none");
   var self=collect(wire);var peers=collect(other);String reveal=HexFormat.of().formatHex(Protocol.invisible(pc.playerObjectId(),0));if(!enabled)need(!self.contains(reveal),"flag false does not publish own reveal");need(wire.locked+other.locked==0,"all output outside action lock");if(enabled)packets.add(world+"/"+skill+"/"+prior+"/"+noMp+" self="+self+" peer="+peers);
   // An explicit damage/attack reveal remains effective regardless of cast flag.
   if(expected){call(pc,"revealInvisibility",new Class[]{String.class},"accepted-damage");need(!(boolean)get(pc,"invisible"),"damage reveal is never controlled by cast flag");}
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var c:List.of(ClientSession.class,CombatRules.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate class");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);original=(CombatRules.Snapshot)f.get(null);var root=Path.of(args[1]);Files.createDirectories(root);
  try {
   for(boolean enabled:before?new boolean[]{true}:new boolean[]{true,false})for(boolean world:new boolean[]{false,true})for(int skill:new int[]{2,60,97})for(int initial=0;initial<3;initial++)for(boolean noMp:new boolean[]{false,true}) {
    var path=root.resolve(enabled+"-"+world+"-"+skill+"-"+initial+"-"+noMp);Files.createDirectories(path);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",false,"stateFile",path.resolve("must-not-write")));var state=new GameStateStore(path.resolve("must-not-write"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
    try(var capture=new CaptureLog(path.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var a=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
     var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(a,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,a.session,p.session,m.session,o.session);if(world)world(fixtures,hub);exercise(p.session,m.session,hub,world,enabled,skill,initial,noMp);
    }need(!Files.exists(path.resolve("must-not-write")),"memory-only state");
   }
  }finally{install(original);NativePlayerCommit.closeFixture();}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("enabled-goldens.txt"),String.join("\n",packets));var result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"formal_changed\":false,\"full_goal_achieved\":false}";Files.writeString(output,result);System.out.println(result);
 }
}
