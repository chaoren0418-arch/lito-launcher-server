package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual catalog loader, PC equipment predicate, dispatcher and removal tail. */
public final class SolidControlProbe extends NativeMagicalWeaponProbe {
 static final String KEY="SOLID_CARRIAGE_MODE";
 static CombatRules.Snapshot original;
 static void install(CombatRules.Snapshot s)throws Exception{var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);f.set(null,s);}
 static void mode(int n)throws Exception{var values=new HashMap<>(original.values());values.put(KEY,(double)n);install(copy(original,Map.of("values",values)));}
 static InventoryCatalog.Definition armor(int type)throws Exception {
  var cls=InventoryCatalog.NativeArmorStatus.class;var parts=cls.getRecordComponents();var types=new Class<?>[parts.length];var values=new Object[parts.length];
  for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=types[i]==boolean.class?false:0;if(parts[i].getName().equals("armorType"))values[i]=type;}
  var constructor=cls.getDeclaredConstructor(types);constructor.setAccessible(true);var status=constructor.newInstance(values);
  var changes=new HashMap<String,Object>();changes.put("sourceWeaponStatus",null);changes.put("sourceArmorStatus",status);changes.put("useType",25);changes.put("weaponWalkAction",0);
  return copy(InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),changes);
 }
 static RuntimeInventory inventory(int type,boolean equipped)throws Exception{var inv=new RuntimeInventory();if(type!=0)need(inv.add(new RuntimeInventory.Item(800001,armor(type),1,equipped),1),"construct typed armor fixture");return inv;}
 static void parser()throws Exception {
  var skill=new Properties();skill.putAll(CombatRules.read(original.skillFile()));var damage=CombatRules.read(original.damageFile());
  String[] values={null,""," ","bad","1.0","2147483648","-2147483649","0","1","2","-1"," 1 ","2147483647","-2147483648"};
  int[] expected={1,1,1,1,1,1,1,0,1,2,-1,1,Integer.MAX_VALUE,Integer.MIN_VALUE};
  for(int i=0;i<values.length;i++){if(values[i]==null)skill.remove(KEY);else skill.setProperty(KEY,values[i]);var snapshot=CombatRules.snapshot(skill,damage,original.skillFile(),original.damageFile());need(snapshot.values().get(KEY).intValue()==expected[i],"source _getInt default/parser "+values[i]);need(SourceSolidReference.mode(skill)==expected[i],"compiled original Source parser");need(snapshot.defaultedKeys().contains(KEY)==(i<7),"default provenance");}
 }
 public static void main(String[] args)throws Exception {
  var jar=Path.of(args[3]).toRealPath();for(var c:List.of(ClientSession.class,CombatRules.class,SourceEquipmentPhysicalStats.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"candidate classes");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var field=CombatRules.class.getDeclaredField("current");field.setAccessible(true);original=(CombatRules.Snapshot)field.get(null);parser();var root=Path.of(args[1]);Files.createDirectories(root);
  try {
   for(int mode:new int[]{0,1,2,-1})for(boolean shield:new boolean[]{false,true})for(boolean guarder:new boolean[]{false,true}){mode(mode);if(shield&&guarder){try{new SourceEquipmentPhysicalStats(SourceEquipmentPhysicalStats.none().weapon(),true,true);throw new AssertionError("mutually exclusive slot accepted");}catch(IllegalArgumentException expected){need(true,"original equipment exclusivity retained");}continue;}var stats=new SourceEquipmentPhysicalStats(SourceEquipmentPhysicalStats.none().weapon(),shield,guarder);need(stats.supportsSolidCarriage()==SourceSolidReference.eligible(shield,guarder,mode),"actual source stats predicate vs compiled Source expression");}
   var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",false,"stateFile",root.resolve("must-not-write")));var state=new GameStateStore(root.resolve("must-not-write"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);
    for(boolean world:new boolean[]{false,true}){if(world)world(fixtures,hub);for(int mode:new int[]{0,1})for(int type:new int[]{0,7,13})for(boolean equipped:new boolean[]{false,true}) {
     reset(p.session,m.session,hub,world);mode(mode);var pc=p.session;set(pc,"inventory",inventory(type,equipped));set(pc,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());set(pc,"learnedSkills",new HashSet<>(Set.of(90)));set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"observer",false);call(pc,"clearInvisibilityStateSilently",new Class[]{});
     long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!pc.nativeCastTimelineState().timeline.canReserveSkillAction()&&System.nanoTime()<limit)Thread.sleep(5);need(pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"previous Source action window naturally retired");((Map<?,?>)get(pc,"spellReadyNanos")).clear();
     boolean expected=equipped&&(type==7||type==13&&mode==1);need((boolean)call(pc,"hasSolidCarriageEquipment",new Class[]{})==expected,"actual PC equipment predicate mode="+mode+" type="+type);
     var liveInventory=get(pc,"inventory");var physical=new SourceEquipmentPhysicalStats(SourceEquipmentPhysicalStats.none().weapon(),equipped&&type==7,equipped&&type==13);set(pc,"inventory",new RuntimeInventory());set(pc,"sourceEquipmentMagicStats",copy(SourceEquipmentMagicStats.none(),Map.of("physical",physical)));need((boolean)call(pc,"hasSolidCarriageEquipment",new Class[]{})==expected,"actual PC Source snapshot fallback obeys same mode");set(pc,"inventory",liveInventory);set(pc,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());
     try(var wire=new SourceActionPairProbe.Wire(pc,m.session);var peer=new SourceActionPairProbe.Wire(m.session,pc)) {
      call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(11).c(1).bytes());
      boolean accepted=((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(90);need(accepted==expected,"actual skill90 dispatcher mode="+mode+" type="+type+" world="+world);need(((int)get(pc,"playerMp")<1000)==expected,"MP changes only for allowed equipment");
      var status=get(pc,"abilityStatus");if(expected){set(pc,"inventory",new RuntimeInventory());call(pc,"removeSolidCarriageUnlessEquipped",new Class[]{String.class},"fixture-unequip");need(!(boolean)call(status,"removeSolidCarriage",new Class[]{}),"actual unequip tail already removed status");}
      collect(wire);collect(peer);
     }
     cases++;
    }}
   }
   need(!Files.exists(root.resolve("must-not-write")),"memory only");
  } finally {install(original);NativePlayerCommit.closeFixture();}
  need(NativePlayerLifetime.allIdle(),"lifetimes idle");var result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"parser_cases\":14,\"stats_cases\":16,\"source_database_writes\":false,\"full_goal_achieved\":false}";Files.writeString(Path.of(args[2]),result);System.out.println(result);
 }
}
