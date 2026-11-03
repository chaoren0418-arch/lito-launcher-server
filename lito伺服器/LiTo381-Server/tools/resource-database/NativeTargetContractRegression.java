package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Formal DLL cccdhh target shape; no previous victim may substitute for a missing explicit target. */
public class NativeTargetContractRegression extends WorldFoeGapProbe {
 static final Set<Integer> IDS=new TreeSet<>(Set.of(183,184,187,188,192,193,194,202,203,204,207,208,209,211,212,213,214,216,217,218,219));
 static void reset(ClientSession pc,int type)throws Exception{
  set(pc,"learnedSkills",IDS);set(pc,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());set(pc,"sourceBaseMagicResistance",new SourceBaseMagicResistance(0,0,0,0,0));set(pc,"sourceEquipmentMagicStats",new SourceEquipmentMagicStats(copy(SourceEquipmentPhysicalStats.none(),Map.of("weapon",new SourceWeaponPhysicalStats(272,18,1,1,2,0,0,0,0),"itemMeleeDamage",3)),37,0));
  if(type==6){KiringkuRegression.equip(pc,copy(RangedDamageRegression.carrier(BattleActionCodes.CLAW_WALK),Map.of("catalogId",270L)),0);set(pc,"sourceEquipmentMagicStats",copy((SourceEquipmentMagicStats)get(pc,"sourceEquipmentMagicStats"),Map.of("itemMagicDamage",37)));}
  set(pc,"playerHp",1000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);((Map<?,?>)get(pc,"spellReadyNanos")).clear();
 }
 public static void main(String[] args)throws Exception{
  var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("uncannyDodgeHitFixtureD20",20,"uncannyDodgeHitFixtureD100",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false));ResourceDatabase.load(config);var root=Path.of(args[1]);Files.createDirectories(root);var rows=new ArrayList<String>();int cases=0,differences=0;
  var field=DragonSkillRequests.class.getDeclaredField("TARGETED");field.setAccessible(true);var actual=new TreeSet<>((Set<Integer>)field.get(null));actual.addAll(IllusionSkillRequests.ATTACK);actual.addAll(IllusionSkillRequests.OTHER);check(actual.equals(IDS),"actual native server gateways have the exact21 formal DLL target skills");
  try(var capture=new CaptureLog(root.resolve("capture"))){for(boolean world:new boolean[]{false,true})for(int skill:IDS)for(boolean explicit:new boolean[]{false,true}){
   int serial=cases++;var state=new GameStateStore(root.resolve("case"+serial+"-unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var hub=new GameHub(config,state,accounts);var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(2,TimeUnit.SECONDS),"native clock stopped for isolated explicit-target contract");var fixtures=List.of(f,p,m,o);
    for(int i=0;i<4;i++){var pc=fixtures.get(i).session;NativeDirectoryRegression.init(fixtures.get(i),"Target"+serial+"_"+i,0,config.classicServerNames().get(0));var c=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(c,Map.of("objectId",0x3a000001L+serial*8+i,"type",i==0?(skill<200?5:6):0,"level",99,"intelligence",9,"originalIntelligence",9))));set(pc,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());set(pc,"sourceBaseMagicResistance",new SourceBaseMagicResistance(0,0,0,0,0));}
    if(!world){FlowRegression.setup(hub,f.session,p.session,m.session,o.session);for(int i=0;i<4;i++){set(fixtures.get(i).session,"positionX",32740+i);set(fixtures.get(i).session,"positionY",32798);}}else for(int i=0;i<2;i++){var pc=fixtures.get(i).session;set(pc,"phase",Enum.valueOf((Class)get(pc,"phase").getClass(),"WORLD"));set(pc,"classicWorldMapId",4);set(pc,"classicWorldInstanceId",-1);set(pc,"positionX",33089+i);set(pc,"positionY",32717);hub.enterClassicWorld(pc,4,33089+i,32717,0);}
    var pc=f.session;var target=p.session;reset(pc,skill<200?5:6);set(target,"playerHp",5000);set(target,"playerMaxHp",5000);set(target,"playerMp",1000);set(target,"playerMaxMp",1000);long id=target.playerObjectId();int x=(int)get(target,"positionX"),y=(int)get(target,"positionY");
    call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(0x17).d(id).h(x).h(y).bytes());drain(hub);check((long)get(pc,"lastCombatTargetObjectId")==id,"real prior native C_ATTACK establishes previous ID in mode="+(world?"WORLD":"BATTLE"));reset(pc,skill<200?5:6);set(target,"playerHp",5000);
    var packet=new Protocol.Writer().c(0x14).c((skill-1)/8).c((skill-1)%8);if(explicit)packet.d(id).h(x).h(y);call(pc,"handle",new Class[]{byte[].class},packet.bytes());drain(hub);int hp=(int)get(pc,"playerHp"),mp=(int)get(pc,"playerMp");boolean used=hp!=1000||mp!=1000;
    if(explicit)check(used,"formal explicit11 target request reaches actual native skill handler skill="+skill+" WORLD="+world+" hp="+hp+" mp="+mp);else{if(used)differences++;check(!used||world&&(skill==187||skill==194),"only the identified historical fallback accepts missing target");}
    check(DragonSkillRequests.current()==null&&IllusionSkillRequests.current()==null,"original request scopes always retire after exact dispatch");rows.add("{\"skill\":"+skill+",\"world\":"+world+",\"explicit11\":"+explicit+",\"caster_HP\":"+hp+",\"caster_MP\":"+mp+",\"resource_used\":"+used+"}");
   }
  }}
  Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"unexpected_selector_acceptances\":"+differences+",\"formal_targeted_skill_count\":21,\"BATTLE_and_WORLD_real_prior_C_ATTACK\":true,\"full_skill_effect_or_eligibility_parity_verified\":false,\"rows\":["+String.join(",",rows)+"]}\n");System.out.println("PASS formal native target request checks="+checks+"; cases="+cases+"; unsupported selector acceptances="+differences+"; explicit21 skills both phases preserve native handlers");
 }
}
