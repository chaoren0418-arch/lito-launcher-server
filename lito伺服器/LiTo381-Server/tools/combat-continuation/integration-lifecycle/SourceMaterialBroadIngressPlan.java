package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Inventory actual 58 native material ingress outcomes on fresh strict memory fixtures. */
public final class SourceMaterialBroadIngressPlan {
 record Row(int id,String getter,int reagent,int count,int mp,String parser,Object model){}
 static final List<String> output=new ArrayList<>();static int attempts,closed;
 static int number(Object o,String key,int fallback)throws Exception{try{var m=o.getClass().getDeclaredMethod(key);m.setAccessible(true);return((Number)m.invoke(o)).intValue();}catch(NoSuchMethodException missing){return fallback;}}
 static String clean(Object o){return String.valueOf(o).replace('\t',' ').replace('\r',' ').replace('\n',' ');}
 static Throwable root(Throwable e){while(e instanceof java.lang.reflect.InvocationTargetException&&e.getCause()!=null)e=e.getCause();return e;}
 static void run(Row row,String mode)throws Throwable{
  var f=SourceWeaponPlayerFullPathPlan.fixture("normal");var b=f.base().first().base().base();Throwable failure=null,cleanupFailure=null;int mp=-1;long gems=-1;int bytes=-1;
  try{
   var pc=b.caster();int type=row.id()<=80?3:row.id()<129?4:row.id()<177?2:row.id()<200?5:6;
   var character=(CharacterSummary)CombatAccess.call(pc,"selectedCharacterSummary","()Llocal/lito381/server/CharacterSummary;");SourceWeaponFullPathPlan.put(pc,"characters",List.of(SourceWeaponOverlayPlan.copy(character,Map.of("type",type))));
   SourceWeaponFullPathPlan.put(pc,"learnedSkills",Set.of(row.id()));SourceWeaponFullPathPlan.put(pc,"elfAttribute",number(row.model(),"attribute",15));
   SourceWeaponFullPathPlan.put(pc,"playerMp",2000);SourceWeaponFullPathPlan.put(pc,"playerMaxMp",2000);
   SourceWeaponFullPathPlan.put(pc,"playerHp",1000);SourceWeaponFullPathPlan.put(pc,"playerMaxHp",1000);
   var inventory=RuntimeInventory.fromDurableState(Map.of(24L,1,(long)row.reagent(),row.count()*3),Map.of(24L,List.of(new RuntimeInventory.PersistentItem(99993L,1,0,0,true,1,false,true))));
   SourceWeaponFullPathPlan.put(pc,"inventory",inventory);var state=(GameStateStore)CombatAccess.field(pc,"state");String key=(String)CombatAccess.call(pc,"stateCharacter","()Ljava/lang/String;");state.saveEconomyState(key,0L,inventory.catalogCounts(),inventory.catalogItemStates());
   if(mode.equals("Main"))SourceWeaponFullPathPlan.put(b.hub(),"nativeStaticMotionClosed",true);if(mode.equals("STOPPED"))SourceWeaponFullPathPlan.put(b.life(),"mode",NativePlayerLifetime.Mode.STOPPED);
   boolean item=Set.of(73,107).contains(row.id());long target=item?99993L:mode.equals("self")?pc.playerObjectId():b.target().playerObjectId();int x=(int)CombatAccess.field(pc,"positionX"),y=(int)CombatAccess.field(pc,"positionY");
   var writer=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((row.id()-1)/8).c((row.id()-1)%8);String parser=row.parser();
   if(row.id()==60){var shape=row.model().getClass().getDeclaredMethod("requestShape");shape.setAccessible(true);parser=shape.invoke(row.model()).toString().equals("SELF_TICK")?"selfSpellRequest":"selectorOnlySpellRequest";}
   switch(parser){case "attackSpellRequest"->writer.d(target).h(x).h(y);case "objectSpellRequest"->writer.d(target);case "groundSpellRequest"->writer.h(x).h(y);case "summonSpellRequest"->writer.d(1);case "massTeleportSpellRequest"->writer.h(x).h(y).h(554);case "selfSpellRequest"->writer.d(42);case "selectorOnlySpellRequest"->{}default->throw new AssertionError("unreviewed packet parser "+parser);}
   byte[] packet=writer.bytes();
   try{NativeLightAction.dispatch(pc,packet);}catch(Throwable e){failure=root(e);}
   mp=(int)CombatAccess.field(pc,"playerMp");gems=((RuntimeInventory)CombatAccess.field(pc,"inventory")).countCatalog(row.reagent());bytes=f.sockets().values().stream().mapToInt(s->s.bytes.size()).sum();
   if(Set.of("Main","STOPPED").contains(mode)&&(failure!=null||mp!=2000||gems!=row.count()*3||bytes!=0))throw new AssertionError("closed native ingress changed state or threw: "+mode+" "+row.id()+" mp="+mp+" gems="+gems+" bytes="+bytes,failure);
  }catch(Throwable e){failure=root(e);}finally{try{SourceWeaponFullPathPlan.cleanup(f);closed++;}catch(Throwable e){cleanupFailure=root(e);}}
  String status=cleanupFailure!=null?"cleanup-failed":failure!=null?"ingress-error":mp<2000||gems<row.count()*3?"cost-observed":bytes>0?"response-without-cost":"rejected-or-no-output";
  String stack=failure==null?"":Arrays.stream(failure.getStackTrace()).limit(8).map(StackTraceElement::toString).reduce((a,z)->a+" | "+z).orElse("");
  output.add(String.join("\t",Integer.toString(row.id()),mode,status,Integer.toString(mp),Long.toString(gems),Integer.toString(bytes),failure==null?"":clean(failure.getClass().getName()),failure==null?"":clean(failure.getMessage()),clean(stack),cleanupFailure==null?"":clean(cleanupFailure)));attempts++;
 }
 public static void main(String[]args)throws Throwable{
  var database=CombatDatabase.class.getDeclaredField("current");database.setAccessible(true);Object priorDatabase=database.get(null);var rows=new ArrayList<Row>();var skills=new HashMap<Integer,CombatDatabase.Skill>();
  try{database.set(null,null);for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("id\t"))continue;String[] s=line.split("\t");int id=Integer.parseInt(s[0]);var method=BattleSpellCatalog.class.getDeclaredMethod(s[1],int.class);method.setAccessible(true);Object model=method.invoke(null,id);if(model==null)throw new AssertionError("missing native model "+id);var r=new Row(id,s[1],Integer.parseInt(s[2]),Integer.parseInt(s[3]),Integer.parseInt(s[4]),s[5],model);rows.add(r);skills.put(id,new CombatDatabase.Skill(r.mp(),number(model,"hpCost",0),number(model,"reuseDelayMillis",0),number(model,"durationSeconds",0),number(model,"baseDamage",0),number(model,"dieSides",0),number(model,"diceCount",0),r.reagent(),r.count()));}}finally{database.set(null,priorDatabase);}
  output.add("skill_id\tmode\tstatus\tremaining_mp\tremaining_material\tencrypted_memory_bytes\terror_type\terror_message\tstack\tcleanup_error");
  SourceWeaponOverlayPlan.environment(()->{Object old=database.get(null);try{database.set(null,new CombatDatabase.Snapshot(skills,new SourceSkillControls.Snapshot(Map.of(),Map.of(),Map.of(),Map.of())));for(var row:rows){if(args.length>2&&row.id()!=Integer.parseInt(args[2]))continue;for(String mode:List.of("peer","self","Main","STOPPED"))run(row,mode);}}finally{database.set(null,old);}});
  Files.write(Path.of(args[1]),output);System.out.println("INVENTORY actual material native ingress: rows="+rows.size()+", attempts="+attempts+", cleanups="+closed+"; each outcome remains individually scoped; full semantic/Source/RNG/target/Ctrl/cursor/lifecycle qualification not inferred");
 }
}
