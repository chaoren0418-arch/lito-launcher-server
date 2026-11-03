package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Exercise independent skill timers and real deferred HP commits, not a replacement receiver. */
public final class InvisibilityLeaseProbe extends NativeMagicalWeaponProbe {
 static CombatRules.Snapshot original;
 static void flag(boolean enabled)throws Exception{var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);var v=new HashMap<>(original.values());v.put("INVIS_CAST_BREAK",enabled?1.0:0.0);f.set(null,copy(original,Map.of("values",v)));}
 static BattleInvisibilityStatus state(ClientSession pc)throws Exception{return (BattleInvisibilityStatus)get(pc,"invisibilityStatus");}
 static void activate(ClientSession pc,int skill,long now)throws Exception{call(pc,"activateInvisibilityState",new Class[]{int.class,long.class,int.class},skill,now,skill==60?0:60);}
 static Object token(BattleInvisibilityStatus s)throws Exception{return get(s,"sourceInvisibilityLease");}
 static void unit()throws Exception {
  for(int first:new int[]{60,97})for(int refresh:new int[]{0,60,97}){
   var s=new BattleInvisibilityStatus();s.activate(first,100,first==60?0:60);s.activate(first==60?97:60,100,first==60?60:0);Object old=token(s);
   if(refresh!=0)s.activate(refresh,100,refresh==60?0:60);
   need(s.cancelCaptured(old),"captured original leases removed");need(s.active()==(refresh!=0)&&s.hasSkill(60)==(refresh==60)&&s.hasSkill(97)==(refresh==97),"same-deadline fresh timer identity retained");need(!s.cancelCaptured(old),"captured removal idempotent");s.cancel();need(!s.active(),"final clear releases both");cases++;
  }
  for(int first:new int[]{60,97}){
   var s=new BattleInvisibilityStatus();s.activate(first,100,first==60?0:2);s.activate(first==60?97:60,100,first==60?2:0);need(s.expire(2_000_000_099L)==0&&s.hasSkill(97),"deadline not reached");need(s.expire(2_000_000_100L)==0&&s.hasSkill(60)&&!s.hasSkill(97),"timed hiding expires without revealing indefinite invisibility");need(s.expire(Long.MAX_VALUE)==0&&s.active(),"indefinite invisibility survives");s.cancel();s.activate(97,100,2);need(s.expire(2_000_000_100L)==97&&!s.active(),"last timed lease emits one expiry");need(s.expire(Long.MAX_VALUE)==0,"expiry once");cases++;
  }
  var s=new BattleInvisibilityStatus();s.activate(60,100,0);Object old=token(s);try{s.activate(60,Long.MAX_VALUE,1);throw new AssertionError("overflow accepted");}catch(ArithmeticException expected){}need(s.hasSkill(60)&&token(s)==old,"overflow cannot erase previous timer");try{s.activate(97,100,-1);throw new AssertionError("negative accepted");}catch(IllegalArgumentException expected){}need(token(s)==old,"invalid cast cannot mutate");cases++;
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int first,int refresh)throws Exception {
  reset(pc,peer,hub,world);call(pc,"clearInvisibilityStateSilently",new Class[]{});long now=System.nanoTime();activate(pc,first,now);activate(pc,first==60?97:60,now);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   var release=hold();try{receive(pc,peer,world,20,false);need((int)get(pc,"playerHp")==4000,"accepted HP waits for original workers");if(refresh!=0)activate(pc,refresh,now);}finally{release.countDown();}idle();
   need((int)get(pc,"playerHp")==3980,"HP committed exactly once");need(state(pc).hasSkill(60)==(refresh==60)&&state(pc).hasSkill(97)==(refresh==97),"only captured skill timers removed");need((boolean)get(pc,"invisible")== (refresh!=0),"PC visibility follows remaining timer");var self=collect(wire);collect(other);String reveal=HexFormat.of().formatHex(Protocol.invisible(pc.playerObjectId(),0));need(self.contains(reveal)==(refresh==0),"partial removal must not emit stale reveal");need(wire.locked+other.locked==0,"no output under state/vital locks");need(NativeAcceptedState.idle()&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"accepted state and completion receipts drained");
  }cases++;
 }
 static void castOverlap(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int skill,boolean toggle)throws Exception {
  reset(pc,peer,hub,world);flag(toggle);call(pc,"clearInvisibilityStateSilently",new Class[]{});int prior=skill==60?97:60;long now=System.nanoTime();if(toggle)activate(pc,skill,now);activate(pc,prior,now);var chars=(List<CharacterSummary>)get(pc,"characters");set(pc,"characters",List.of(copy(chars.get(0),Map.of("type",skill==97?4:2,"originalIntelligence",10))));set(pc,"playerMp",1000);set(pc,"learnedSkills",new HashSet<>(Set.of(skill)));set(pc,"observer",false);
  int reagent=CombatDatabase.value(0,skill,"LeaseProbe","itemConsumeId"),count=CombatDatabase.value(0,skill,"LeaseProbe","itemConsumeCount");if(reagent>0)NativeDeItemPublication.grant(pc,reagent,count+2);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   var p=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7);if(BattleSpellCatalog.invisibilitySpell(skill).requestShape()==BattleSpellCatalog.RequestShape.SELF_TICK)p.d(17);call(pc,"handle",new Class[]{byte[].class},p.bytes());need((int)get(pc,"playerMp")<1000&&((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill),"real packet accepted with resource and cooldown");
   if(toggle)need(!state(pc).active()&&!(boolean)get(pc,"invisible"),"flag true toggles earlier matching skill and clears both timers like Source delInvis");
   else{need(state(pc).hasSkill(60)&&state(pc).hasSkill(97),"actual cast retains both Source skill timers");need((int)call(pc,"expireInvisibilityState",new Class[]{long.class},Long.MAX_VALUE)==0&&(boolean)get(pc,"invisible")&&state(pc).hasSkill(60)&&!state(pc).hasSkill(97),"PC expiry does not reveal surviving indefinite timer");}
   var self=collect(wire);collect(other);need(self.contains(HexFormat.of().formatHex(Protocol.invisible(pc.playerObjectId(),0)))==toggle,"reveal only for enabled toggle, never partial expiry");need(wire.locked+other.locked==0,"cast output unlocked");
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var c:List.of(ClientSession.class,BattleInvisibilityStatus.class,NativeAcceptedState.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"exact runtime class");unit();var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);original=(CombatRules.Snapshot)f.get(null);var root=Path.of(args[1]);Files.createDirectories(root);
  try{for(boolean world:new boolean[]{false,true})for(int first:new int[]{60,97})for(int refresh:new int[]{0,60,97,-1,-2}){
   var path=root.resolve(world+"-"+first+"-"+refresh);Files.createDirectories(path);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",false,"stateFile",path.resolve("must-not-write")));var store=new GameStateStore(path.resolve("must-not-write"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(path.resolve("capture"));var hub=new GameHub(cfg,store,accounts);var a=new SourceLoginRegression.Fixture(cfg,accounts,store,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,store,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,store,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,store,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(a,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,a.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(refresh<0)castOverlap(p.session,m.session,hub,world,first,refresh==-2);else exercise(p.session,m.session,hub,world,first,refresh);
   }need(!Files.exists(path.resolve("must-not-write")),"memory-only fixture");
  }}finally{f.set(null,original);NativePlayerCommit.closeFixture();}
  need(NativePlayerLifetime.allIdle(),"all fixture lifetimes drained");var result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"formal_changed\":false}";Files.writeString(Path.of(args[2]),result);System.out.println(result);
 }
}
