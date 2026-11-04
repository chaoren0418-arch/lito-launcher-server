package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicLong;
/** Existing BATTLE tame/liberation entrances; role-scoped memory only. */
final class NativeBattleTamedLifecycle {
 static final String P="local/lito381/server/",PREP="nativeBattleTamedCompanion";
 static boolean battle(ClientSession pc){return String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE");}
 static void run(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body,int skill)throws IOException{
  if(!battle(pc)){NativePetWorldLifetime.run(pc,packet,body);return;}
  long id=skill==36?Protocol.attackSpellRequest(packet).targetObjectId():Protocol.objectSpellRequest(packet).targetObjectId();
  var captured=new NativeAreaMaterialAction.Context(pc);var npc=captured.npcs.get(id);var corpse=skill==41?new NativeCallNatureMaterialAction.Target(pc,id):null;
  NativeAreaMaterialAction.run(pc,skill,()->{
   if(!captured.scene())return;
   if(skill==36&&npc!=null&&(npc.kind()==1||npc.kind()==2)&&!captured.npc(id))return;
   if(corpse!=null&&(corpse.kind==4||corpse.kind==5)&&!corpse.valid())return;
   Object owner=skill==36&&npc!=null&&(npc.kind()==1||npc.kind()==2)?npc.owner():corpse!=null&&(corpse.kind==4||corpse.kind==5)?corpse.owner:null;
   if(owner==null)body.run();else synchronized(owner){if(skill==36&&!captured.npc(id)||skill==41&&!corpse.valid())return;body.run();}
  });
 }
 static BattleCompanion vitals(BattleCompanion pet,Object snapshot){
  CombatAccess.set(pet,"hp",CombatAccess.integerCall(snapshot,"hp","()I"));CombatAccess.set(pet,"mp",CombatAccess.integerCall(snapshot,"currentMp","()I"));return pet;
 }
 static int initialHp(Object snapshot){return Math.min(CombatAccess.integerCall(snapshot,"hp","()I"),CombatAccess.integerCall(snapshot,"maxHp","()I"));}
 static int visualHealthRatio(int hp,int maximum){return Protocol.healthRatio(Math.min(hp,maximum),maximum);}
 static byte[] visualHitRatio(long id,int hp,int maximum){return Protocol.hitRatio(id,Math.min(hp,maximum),maximum);}
 private static final AtomicLong SEQUENCE=sequence();
 private static AtomicLong sequence(){try{var f=ClientSession.class.getDeclaredField("COMPANION_SEQUENCE");f.setAccessible(true);return (AtomicLong)f.get(null);}catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}}
 static long nextId(){return SEQUENCE.getAndIncrement();}
 @SuppressWarnings("unchecked") static List<BattleCompanion> pets(ClientSession pc){return (List<BattleCompanion>)CombatAccess.field(pc,"tamedCompanions");}
 static Object encounter(GameHub hub,long room,int kind){var r=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(room);return r==null?null:CombatAccess.field(r,kind==1?"questEncounter":"timeAttackEncounter");}
 static Object actor(Object encounter,int kind,long id){return encounter==null?null:CombatAccess.call(encounter,"monster","(J)L"+P+(kind==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Monster;",id);}
 static BattleQuestEncounter.Capture captureQuest(GameHub hub,long room,ClientSession pc,long id,SourceNpcPhysicalStats stats){return (BattleQuestEncounter.Capture)capture(hub,room,pc,id,stats,1);}
 static BattleTimeAttackEncounter.Capture captureTime(GameHub hub,long room,ClientSession pc,long id,SourceNpcPhysicalStats stats){return (BattleTimeAttackEncounter.Capture)capture(hub,room,pc,id,stats,2);}
 static Object capture(GameHub hub,long room,ClientSession pc,long id,SourceNpcPhysicalStats stats,int kind){
  synchronized(hub){var e=encounter(hub,room,kind);if(e==null)return null;synchronized(e){
   var a=actor(e,kind,id);if(a==null)return kind==1?hub.captureQuestMonster(room,pc,id):hub.captureTimeAttackActor(room,pc,id);
   if(!pc.nativePlayerActionLock().isHeldByCurrentThread()||!battle(pc)||(long)CombatAccess.field(pc,"currentRoomId")!=room)return null;
   var context=NativeAreaMaterialAction.current.get();if(context!=null&&context.npcs.containsKey(id)&&!context.npc(id))return null;
   var binding=NativePlayerCommit.Binding.capture(pc);Object snapshot=CombatAccess.call(a,"snapshot","()L"+P+(kind==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Snapshot;");
   if(CombatAccess.integerCall(snapshot,"hp","()I")<=0)return null;
   BattleCompanion pet=kind==1?BattleCompanion.tamedFromQuest(nextId(),(BattleQuestEncounter.Snapshot)snapshot,stats):BattleCompanion.tamedFromTimeAttack(nextId(),(BattleTimeAttackEncounter.Snapshot)snapshot,stats);
   var bag=(RuntimeInventory)CombatAccess.field(a,"inventory");var empty=new RuntimeInventory();var list=pets(pc);boolean committed=false;var rollback=new CaptureRollback(hub,room,e,snapshot,kind);var originalNotice=context==null?null:context.npcs.get(id);
   try{
    synchronized(list){list.add(pet);}
    if(!binding.matches(pc))return null;
    Object result=kind==1?hub.captureQuestMonster(room,pc,id):hub.captureTimeAttackActor(room,pc,id);if(result==null)return null;
    var captured=CombatAccess.field(result,"captured");CombatAccess.set(pet,kind==1?"questTamedOrigin":"timeAttackTamedOrigin",captured);
    CombatAccess.set(pet,"carriedItems",bag);CombatAccess.set(a,"inventory",empty);CombatAccess.set(captured,PREP,pet);
    if(context!=null){context.npcs.remove(id);long petId=pet.snapshot().objectId();context.npcs.put(petId,new NativeAreaMaterialAction.Npc(3,petId,pc,pet,NativeCombatAttacker.companion(pc,pet)));}
    committed=true;return result;
   }finally{if(!committed){rollback.restore();CombatAccess.set(a,"inventory",bag);CombatAccess.set(pet,"carriedItems",empty);if(context!=null){context.npcs.remove(pet.snapshot().objectId());if(originalNotice!=null)context.npcs.put(id,originalNotice);}synchronized(list){list.remove(pet);}pet.deactivate();}}
  }}
 }
 /** Local transaction state only; never attached to a player, task or persistent registry. */
 static final class CaptureRollback {
  final Object room,encounter,lane,match,tower;final int kind,index;final boolean awaiting;final Object phase;final long publishAfter;final List<Object> actors;final List<Object> before;final Map<Object,Object> scores;final Map<String,Object> towerFields=new LinkedHashMap<>();
  @SuppressWarnings("unchecked") CaptureRollback(GameHub hub,long roomId,Object e,Object captured,int k){
   kind=k;room=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(roomId);encounter=e;match=CombatAccess.field(room,"match");publishAfter=(long)CombatAccess.field(room,"pveAiPublishAfterNanos");scores=new LinkedHashMap<>((Map<Object,Object>)CombatAccess.field(match,"teamScores"));
   if(k==1){int force=CombatAccess.integerCall(captured,"force","()I");lane=((Object[])CombatAccess.field(e,"lanes"))[force];index=(int)CombatAccess.field(lane,"roomIndex");awaiting=(boolean)CombatAccess.field(lane,"awaitingObjective");phase=null;actors=(List<Object>)CombatAccess.field(lane,"monsters");var guardians=CombatAccess.field(room,"guardianTowers");synchronized(guardians){tower=((Object[])CombatAccess.field(guardians,"questFinalTowers"))[force];for(String f:List.of("force","x","y","gfx","name","placementSource","objectiveKind","maximumHp","hp","action","active"))towerFields.put(f,CombatAccess.field(tower,f));}}
   else{lane=null;tower=null;index=(int)CombatAccess.field(e,"ordinaryFragmentIndex");awaiting=false;phase=CombatAccess.field(e,"phase");actors=(List<Object>)CombatAccess.field(e,"actors");}
   before=new ArrayList<>(actors);
  }
  @SuppressWarnings("unchecked") void restore(){
   actors.clear();actors.addAll(before);if(kind==1){CombatAccess.set(lane,"roomIndex",index);CombatAccess.set(lane,"awaitingObjective",awaiting);var guardians=CombatAccess.field(room,"guardianTowers");synchronized(guardians){for(var f:towerFields.entrySet())CombatAccess.set(tower,f.getKey(),f.getValue());}}
   else{CombatAccess.set(encounter,"ordinaryFragmentIndex",index);CombatAccess.set(encounter,"phase",phase);}
   var current=(Map<Object,Object>)CombatAccess.field(match,"teamScores");current.clear();current.putAll(scores);CombatAccess.set(room,"pveAiPublishAfterNanos",publishAfter);
  }
 }
 static BattleCompanion complete(ClientSession pc,Object captured,SourceNpcPhysicalStats stats,int kind){
  var prepared=(BattleCompanion)CombatAccess.field(captured,PREP);
  if(prepared!=null){var list=pets(pc);synchronized(list){if(!list.contains(prepared))throw new IllegalStateException("prepared BATTLE tame owner retired");}CombatAccess.set(captured,PREP,null);return prepared;}
  // Existing direct constructors used by supported native fixtures remain valid.
  var pet=kind==1?BattleCompanion.tamedFromQuest(nextId(),(BattleQuestEncounter.Snapshot)captured,stats):BattleCompanion.tamedFromTimeAttack(nextId(),(BattleTimeAttackEncounter.Snapshot)captured,stats);
  var list=pets(pc);synchronized(list){list.add(pet);}return pet;
 }
 static BattleQuestEncounter.Snapshot releaseQuest(GameHub hub,long room,ClientSession pc,BattleQuestEncounter.Snapshot captured,BattleCompanion.Snapshot expected,List<RuntimeInventory.Item> ignored,BattleCompanion pet){return (BattleQuestEncounter.Snapshot)release(hub,room,pc,captured,expected,pet,1);}
 static BattleTimeAttackEncounter.Snapshot releaseTime(GameHub hub,long room,ClientSession pc,BattleTimeAttackEncounter.Snapshot captured,BattleCompanion.Snapshot expected,List<RuntimeInventory.Item> ignored,BattleCompanion pet){return (BattleTimeAttackEncounter.Snapshot)release(hub,room,pc,captured,expected,pet,2);}
 static Object release(GameHub hub,long room,ClientSession pc,Object captured,BattleCompanion.Snapshot expected,BattleCompanion pet,int kind){
  var caster=NativeReturnNatureMaterialAction.caster(pc,pet);if(caster==null||!NativeLightAction.retainedMaterial(caster)||!pc.nativePlayerActionLock().isHeldByCurrentThread())return null;
  synchronized(hub){var e=encounter(hub,room,kind);if(e==null||!battle(pc)||(long)CombatAccess.field(pc,"currentRoomId")!=room)return null;
   synchronized(e){synchronized(pet){
    if(!pet.snapshot().active()||pet.snapshot().hp()<=0||pet.snapshot().objectId()!=expected.objectId()||!NativeLightAction.validTargets(caster))return null;
    var bag=(RuntimeInventory)CombatAccess.field(pet,"carriedItems");var empty=new RuntimeInventory();
    var registered=registered(e,kind,captured);var before=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());before.addAll(registered);Object released=null;boolean committed=false;
    try{
     Object result=kind==1?hub.releaseQuestTamedCompanion(room,pc,(BattleQuestEncounter.Snapshot)captured,expected,List.of()):hub.releaseTimeAttackTamedCompanion(room,pc,(BattleTimeAttackEncounter.Snapshot)captured,expected,List.of());if(result==null)return null;
     long id=((Number)CombatAccess.field(result,"objectId")).longValue();released=findRegistered(e,kind,id);if(released==null)throw new IllegalStateException("native liberated BATTLE actor missing");
     CombatAccess.set(released,"hp",expected.hp());CombatAccess.set(released,"mp",expected.mp());CombatAccess.set(released,"inventory",bag);CombatAccess.set(pet,"carriedItems",empty);
     Object resultSnapshot=CombatAccess.call(released,"snapshot","()L"+P+(kind==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Snapshot;");committed=true;return resultSnapshot;
    }finally{if(!committed){if(released!=null&&CombatAccess.field(released,"inventory")==bag)CombatAccess.set(released,"inventory",empty);CombatAccess.set(pet,"carriedItems",bag);registered.removeIf(a->!before.contains(a));}}
   }}
  }
 }
 @SuppressWarnings("unchecked") static List<Object> registered(Object e,int kind,Object captured){
  if(kind==2)return (List<Object>)CombatAccess.field(e,"actors");
  int force=CombatAccess.integerCall(captured,"force","()I");return (List<Object>)CombatAccess.field(((Object[])CombatAccess.field(e,"lanes"))[force],"monsters");
 }
 static Object findRegistered(Object e,int kind,long id){
  if(kind==2){for(var a:(List<?>)CombatAccess.field(e,"actors"))if(((Number)CombatAccess.field(a,"objectId")).longValue()==id)return a;}
  else for(var lane:(Object[])CombatAccess.field(e,"lanes"))if(lane!=null)for(var a:(List<?>)CombatAccess.field(lane,"monsters"))if(((Number)CombatAccess.field(a,"objectId")).longValue()==id)return a;
  return null;
 }
 private NativeBattleTamedLifecycle(){}
}
