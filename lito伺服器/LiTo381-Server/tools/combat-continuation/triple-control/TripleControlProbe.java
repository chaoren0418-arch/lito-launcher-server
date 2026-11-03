package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
public final class TripleControlProbe extends NativeMagicalWeaponProbe {
 static CombatRules.Snapshot original;static int parserCases,multiplierCases,scopeCases;
 static void install(double value)throws Exception{var values=new HashMap<>(original.values());values.put("Triple_Arrow_Dmg",value);var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);f.set(null,copy(original,Map.of("values",values)));}
 static void parser()throws Exception{
  var props=new Properties();props.putAll(CombatRules.read(original.skillFile()));var damage=CombatRules.read(original.damageFile());
  for(String raw:new String[]{null,""," ","bad","1","1.5","-2","0"," 2.5 ","1e3","NaN","Infinity","-Infinity"}){
   if(raw==null)props.remove("Triple_Arrow_Dmg");else props.setProperty("Triple_Arrow_Dmg",raw);double expected=SourceTripleReference.configured(props);
   try{var parsed=CombatRules.snapshot(props,damage,original.skillFile(),original.damageFile());need(Double.isFinite(expected)&&parsed.values().get("Triple_Arrow_Dmg")==expected,"original source parser finite values");}catch(java.io.IOException error){need(!Double.isFinite(expected)&&error.getMessage().contains("Triple_Arrow_Dmg"),"non-finite control fails startup explicitly");}parserCases++;
  }
 }
 static void evaluate(ClientSession pc,ClientSession other)throws Exception{
  for(int type:new int[]{0,1,4,13,17})for(double configured:new double[]{-2,0,1,1.5,2,3})for(double prior:new double[]{1,1.8,2.5})for(int index:new int[]{-1,0,1,2,3}){
   install(configured);var weapon=type==0?SourceWeaponPhysicalStats.none():new SourceWeaponPhysicalStats(1,type,10,12,type==4||type==13?14:1,0,0,0,0);var base=BattleWeaponCritical.apply(weapon,100,0,prior);need(CombatTripleArrow.apply(weapon,100,0,prior,pc).equals(base),"normal attacks unchanged");
   var scope=CombatTripleArrow.enter(pc,index);try{
    need(CombatTripleArrow.apply(weapon,100,0,prior,other).equals(base),"scope cannot affect another caster");var reference=SourceTripleReference.multiplier(prior,index>=0&&index<3?3-index:0,type,configured);var expected=BattleWeaponCritical.apply(weapon,100,0,reference[0]);var actual=CombatTripleArrow.apply(weapon,100,0,prior,pc);need(actual.equals(expected),"source maximum at offensive subtotal");need(CombatTripleArrow.apply(weapon,100,0,prior,pc).equals(base),"one calculation claim per original attack");
   }finally{CombatTripleArrow.leave(scope);}need(CombatTripleArrow.idle(),"scope is removed after every attempt");multiplierCases++;
  }
  install(2);var weapon=new SourceWeaponPhysicalStats(1,4,10,12,14,0,0,0,0);
  var outer=CombatTripleArrow.enter(pc,0);try{var inner=CombatTripleArrow.enter(other,0);try{need(CombatTripleArrow.apply(weapon,100,0,1,pc).damage()==100,"nested other actor cannot inherit outer cast");}finally{CombatTripleArrow.leave(inner);}need(CombatTripleArrow.apply(weapon,100,0,1,pc).damage()==200,"outer restored after nested scope");}finally{CombatTripleArrow.leave(outer);}scopeCases++;
  var stale=CombatTripleArrow.enter(pc,0);int old=(int)get(pc,"classicWorldInstanceId");try{set(pc,"classicWorldInstanceId",old+1);need(CombatTripleArrow.apply(weapon,100,0,1,pc).damage()==100,"moved binding rejects captured multiplier");}finally{set(pc,"classicWorldInstanceId",old);CombatTripleArrow.leave(stale);}scopeCases++;
  var enumType=Class.forName("local.lito381.server.ClientSession$AttackEnvelope");Object envelope=Enum.valueOf((Class)enumType,"ALTERNATE");
  call(pc,"nativeTripleArrowAttempt",new Class[]{Protocol.AttackRequest.class,enumType,int.class},new Protocol.AttackRequest(0,0,0),envelope,0);need(CombatTripleArrow.idle(),"actual wrapper normal-return cleanup");scopeCases++;
  var weakness=get(pc,"foeSlayerSequence");set(pc,"foeSlayerSequence",null);try{Throwable thrown=null;try{call(pc,"nativeTripleArrowAttempt",new Class[]{Protocol.AttackRequest.class,enumType,int.class},new Protocol.AttackRequest(0,0,0),envelope,0);}catch(Throwable failure){thrown=failure;}need(thrown!=null,"injected native pre-calculation exception reached");need(CombatTripleArrow.idle(),"actual wrapper exception cleanup");}finally{set(pc,"foeSlayerSequence",weakness);}scopeCases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var c:List.of(ClientSession.class,CombatRules.class,CombatTripleArrow.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate classes");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);original=(CombatRules.Snapshot)f.get(null);parser();var root=Path.of(args[1]);Files.createDirectories(root);var cfg=copy(base,Map.of("statePersistenceEnabled",false,"stateFile",root.resolve("must-not-write")));var state=new GameStateStore(root.resolve("must-not-write"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var a=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"clock stopped");var fixtures=List.of(a,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,a.session,p.session,m.session,o.session);evaluate(p.session,m.session);
  }finally{f.set(null,original);NativePlayerCommit.closeFixture();}need(NativePlayerLifetime.allIdle()&&CombatTripleArrow.idle(),"no lifetime/scope retention");need(!Files.exists(root.resolve("must-not-write")),"memory-only");String result="{\"parser_cases\":"+parserCases+",\"multiplier_cases\":"+multiplierCases+",\"scope_cases\":"+scopeCases+",\"checks\":"+ownChecks+"}";Files.writeString(Path.of(args[2]),result);System.out.println(result);
 }
}
