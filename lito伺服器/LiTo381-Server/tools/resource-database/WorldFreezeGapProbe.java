package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

/** Real packets observe known freeze gaps; a green observation is never a runtime parity pass. */
public class WorldFreezeGapProbe extends WorldMagicCommitRegression {
 static String tracedCast(ClientSession pc,ClassicWorldMonsterPopulation.Snapshot a,int skill)throws Exception{
  var saved=System.out;var text=new ByteArrayOutputStream();var tee=new PrintStream(new OutputStream(){public void write(int b){saved.write(b);text.write(b);}public void write(byte[] b,int off,int len){saved.write(b,off,len);text.write(b,off,len);}},true,StandardCharsets.UTF_8);
  try{System.setOut(tee);cast(pc,a,skill);}finally{tee.flush();System.setOut(saved);}return text.toString(StandardCharsets.UTF_8);
 }
 static void observe(ClientSession pc,GameHub hub,ClassicWorldMonsterPopulation pop,Path output)throws Exception{
  int differences=0,n=10000;var rows=new ArrayList<String>();
  for(int skill:new int[]{50,80,194}){
   mr=600;ready(pc,37);var a=wounded(pop,n++);cast(pc,a,skill);drain(hub);int signed=hp(pop,a.objectId());if(signed!=40001)differences++;discard(pop,a.objectId());
   mr=80;ready(pc,37);a=wounded(pop,n++);long id=a.objectId();var t=timed(pop,id);produce(pc,companionDamage(pc,BattleCompanion.Kind.SUMMON),a);int held;boolean frozenWhileHeld;
   try(var gate=new Gate();var scope=CombatPoisonLifecycle.open()){long now=next(t);dispatch(pc,CombatMonsterCurseLifecycle.pollDamageWorld(hub,4,-1,now,3000,5,pc,scope),now,scope);cast(pc,a,skill);held=hp(pop,id);frozenWhileHeld=pop.freezeActive(4,id,System.nanoTime());if(held!=40000)differences++;}
   drain(hub);check(hp(pop,id)==39972,"eventual frozen magic23 plus accepted poison5 skill="+skill);boolean positiveFreezeObserved=pop.freezeActive(4,id,System.nanoTime());if(skill==194)check(positiveFreezeObserved,"existing breath fixture percentile1 is eligible and succeeds on this same source NPC template");discard(pop,id);
   boolean selected=false,zeroFrozen=false;int selectedChance=0,selectedRoll=0;mr=0;ready(pc,0);a=wounded(pop,n++);id=a.objectId();
   for(int attempt=0;attempt<80&&!selected;attempt++){ready(pc,0);String trace=tracedCast(pc,a,skill);if(!CombatMonsterCurseLifecycle.awaitDamage(hub,5000))throw new AssertionError("zero freeze observation lane did not drain");int chance=1,roll=1;
    if(skill!=194){String pattern=skill==50?"C_USE_SPELL classic-world Ice Lance skill=50[^\\n]*probability=(\\d+) roll=(\\d+)":"classic-world centered-freeze monster="+id+"[^\\n]*probability=(\\d+) roll=(\\d+)";var match=Pattern.compile(pattern).matcher(trace);if(!match.find())throw new AssertionError("native freeze probability diagnostic missing skill="+skill+" trace="+trace);chance=Integer.parseInt(match.group(1));roll=Integer.parseInt(match.group(2));}
    selected=chance>=roll;if(selected){selectedChance=chance;selectedRoll=roll;zeroFrozen=pop.freezeActive(4,id,System.nanoTime());check(hp(pop,id)==40000,"zero freeze success changes no HP");if(!zeroFrozen)differences++;}
   }
   check(selected,"a real successful source-bound freeze percentile was observed without replacing native RNG skill="+skill);discard(pop,id);
   rows.add("{\"skill\":"+skill+",\"signed_HP\":"+signed+",\"held_HP\":"+held+",\"freeze_while_consumers_held\":"+frozenWhileHeld+",\"zero_success_probability_lower_bound\":"+selectedChance+",\"probability_from_native_diagnostic\":"+(skill!=194)+",\"zero_success_roll\":"+selectedRoll+",\"zero_freeze_active\":"+zeroFrozen+"}");
  }
  Files.writeString(output,"{\"known_differences\":"+differences+",\"families\":["+String.join(",",rows)+"],\"native_full_freeze_parity_verified\":false}\n");System.out.println("PASS WORLD freeze gap observations checks="+checks+"; known-differences="+differences+"; source signed HP/target lane/zero-damage independent freeze across three real spell families; no runtime-port claim");
 }
 public static void main(String[] args)throws Exception{
  var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("freezingBreathFixtureD100",1,"classicWorldMonsterPoisonFixtureRoll",1,"cursePoisonFixtureDieRoll",1,"cursePoisonFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false));ResourceDatabase.load(config);var df=CombatRegression.current(CombatDatabase.class);var data=(CombatDatabase.Snapshot)df.get(null);var skills=new HashMap<>(data.skills());for(int id:new int[]{50,80,194}){var old=skills.get(id);skills.put(id,new CombatDatabase.Skill(17,0,0,7,0,1,0,0,0,old.probabilityDice(),old.probabilityValue()));}df.set(null,data.withSkills(skills));
  var root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);AccountRepository memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);AccountRepository accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(WorldFreezeGapProbe.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{if(m.getName().equals("repositoryForClassicSource"))return p;if(m.getName().equals("npcPhysicalStats"))return copy(SourceNpcPhysicalStats.memoryFixture((int)a[0]),Map.of("magicResistance",mr,"armorClass",10,"damageReduction",0,"hard",false,"boss",false,"undead",0));try{return m.invoke(memory,a);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}});
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,TimeUnit.SECONDS),"native clock stopped for controlled freeze observations");var pc=f.session;NativeDirectoryRegression.init(f,"WorldFreezeGap",0,config.classicServerNames().get(0));var c=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(c,Map.of("objectId",0x38f30001L,"type",3,"level",99,"intelligence",9,"originalIntelligence",9))));set(pc,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());set(pc,"sourceBaseMagicResistance",new SourceBaseMagicResistance(0,0,0,0,0));set(pc,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());set(pc,"learnedSkills",Set.of(50,80,194));set(pc,"phase",Enum.valueOf((Class)get(pc,"phase").getClass(),"WORLD"));set(pc,"classicWorldMapId",4);set(pc,"classicWorldInstanceId",-1);set(pc,"positionX",33089);set(pc,"positionY",32717);hub.enterClassicWorld(pc,4,33089,32717,0);observe(pc,hub,(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters"),Path.of(args[2]));
  }finally{df.set(null,data);}
 }
}
