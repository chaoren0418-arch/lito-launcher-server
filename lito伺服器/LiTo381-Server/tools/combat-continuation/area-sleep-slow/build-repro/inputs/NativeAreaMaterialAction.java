package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.locks.*;

/** Freeze membership before waiting; all player locks precede the hub/population. */
final class NativeAreaMaterialAction {
 static final String P="local/lito381/server/";
 static final ThreadLocal<Context> current=new ThreadLocal<>();
 record Npc(int kind,long id,Object owner,Object actor,Object epoch){}
 static final class Context {
  final ClientSession pc;final GameHub hub;final NativePlayerCommit.Binding caster;
  final String phase;final int map,instance;final long room;
  final List<ClientSession> players;
  final IdentityHashMap<ClientSession,NativePlayerCommit.Binding> bindings=new IdentityHashMap<>();
  final Map<Long,Npc> npcs=new HashMap<>();
  final ClassicWorldMonsterPopulation population;
  Context(ClientSession pc){
   this.pc=pc;hub=(GameHub)CombatAccess.field(pc,"hub");caster=NativePlayerCommit.Binding.capture(pc);
   phase=String.valueOf(CombatAccess.field(pc,"phase"));map=(int)CombatAccess.field(pc,"classicWorldMapId");instance=(int)CombatAccess.field(pc,"classicWorldInstanceId");room=(long)CombatAccess.field(pc,"currentRoomId");
   synchronized(hub){
    var members=new ArrayList<>(phase.equals("WORLD")?hub.classicWorldMembers(map,instance):hub.roomMembers(room));if(!members.contains(pc))members.add(pc);
    players=members.stream().distinct().sorted(Comparator.comparingInt(NativeSourcePlayerActionLock::signedId).thenComparingInt(System::identityHashCode)).toList();
    for(var member:players){bindings.put(member,NativePlayerCommit.Binding.capture(member));
     for(var snap:phase.equals("WORLD")?member.classicWorldCompanionSnapshots():member.controlledCompanionSnapshots()){
      var companion=(BattleCompanion)CombatAccess.call(member,"ownedCompanion","(J)L"+P+"BattleCompanion;",snap.objectId());
      if(companion!=null&&snap.active()&&snap.hp()>0)npcs.put(snap.objectId(),new Npc(3,snap.objectId(),member,companion,NativeCombatAttacker.companion(member,companion)));
     }
    }
    population=phase.equals("WORLD")?NativeCancellationNpcMaterialAction.population(hub,map,instance):null;
    if(population!=null)synchronized(population){for(var n:population.inScreen(map,(int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"))){Object actor=NativeCancellationNpcMaterialAction.actor(population,n.objectId());npcs.put(n.objectId(),new Npc(0,n.objectId(),population,actor,NativeCancellationNpcMaterialAction.epoch(actor,true)));}}
    if(phase.equals("BATTLE")){
     for(var n:hub.visibleQuestMonsters(room,pc))captureBattle(n.objectId(),1);
     for(var n:hub.visibleTimeAttackActors(room,pc))captureBattle(n.objectId(),2);
    }
   }
  }
  Object encounter(int kind){var roomObject=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(room);return roomObject==null?null:CombatAccess.field(roomObject,kind==1?"questEncounter":"timeAttackEncounter");}
  Object monster(Object owner,int kind,long id){return owner==null?null:CombatAccess.call(owner,"monster","(J)L"+P+(kind==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Monster;",id);}
  void captureBattle(long id,int kind){Object owner=encounter(kind),actor=monster(owner,kind,id);if(actor!=null)npcs.put(id,new Npc(kind,id,owner,actor,NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(actor,"damagePoison"),true)));}
  boolean player(ClientSession target){var binding=bindings.get(target);return binding!=null&&binding.matches(target)&&String.valueOf(CombatAccess.field(target,"phase")).equals(phase)
   &&(phase.equals("WORLD")?(int)CombatAccess.field(target,"classicWorldMapId")==map&&(int)CombatAccess.field(target,"classicWorldInstanceId")==instance:(long)CombatAccess.field(target,"currentRoomId")==room);}
  boolean npc(long id){var n=npcs.get(id);if(n==null)return false;
   if(n.kind==3){var owner=(ClientSession)n.owner;return player(owner)&&CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",id)==n.actor&&NativeCombatAttacker.alive(owner,(NativeCombatAttacker.Ref)n.epoch);}
   if(n.kind==0){return NativeCancellationNpcMaterialAction.population(hub,map,instance)==population&&NativeCancellationNpcMaterialAction.actor(population,id)==n.actor&&NativeCancellationNpcMaterialAction.epoch(n.actor,false)==n.epoch&&(boolean)CombatAccess.field(n.actor,"alive")&&(boolean)CombatAccess.field(n.actor,"present");}
   return encounter(n.kind)==n.owner&&monster(n.owner,n.kind,id)==n.actor&&NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(n.actor,"damagePoison"),false)==n.epoch&&(int)CombatAccess.field(n.actor,"hp")>0;
  }
  boolean scene(){return caster.matches(pc)&&player(pc)&&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED);}
 }
 // Source L1SkillUse MASS_SLOW excludes only caster and caster-owned companions, not other pets.
 static boolean companionSlowEligible(SourceNpcPhysicalStats stats){return stats!=null;}
 static void worldSleep(ClientSession pc,byte[] packet,BattleSpellCatalog.SleepSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaWorldSleepBody(packet,spell));}
 static void battleSleep(ClientSession pc,byte[] packet,BattleSpellCatalog.SleepSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaBattleSleepBody(packet,spell));}
 static void worldSlow(ClientSession pc,byte[] packet,BattleSpellCatalog.MassSlowSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaWorldSlowBody(packet,spell));}
 static void battleSlow(ClientSession pc,byte[] packet,BattleSpellCatalog.MassSlowSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaBattleSlowBody(packet,spell));}
 static List<ClientSession> roomCombatants(GameHub hub,long room){return filter(hub.roomCombatants(room));}
 static ClassicWorldMonsterPopulation.Snapshot worldNpc(GameHub hub,int map,int instance,long id){var c=current.get();return c.npc(id)?hub.classicWorldMonster(map,instance,id):null;}
 static BattleQuestEncounter.Snapshot questNpc(GameHub hub,long room,long id){var c=current.get();return c.npc(id)?hub.questMonster(room,id):null;}
 static BattleTimeAttackEncounter.Snapshot timeNpc(GameHub hub,long room,long id){var c=current.get();return c.npc(id)?hub.timeAttackActor(room,id):null;}
 static List<BattleCompanion.Snapshot> companions(ClientSession owner){var c=current.get();return owner.classicWorldCompanionSnapshots().stream().filter(s->c.npc(s.objectId())).toList();}
 static BattleCompanion companion(ClientSession owner,long id){var c=current.get();var n=c.npcs.get(id);return n!=null&&n.kind==3&&n.owner==owner&&c.npc(id)?(BattleCompanion)n.actor:null;}
 @SuppressWarnings("unchecked") static Map<Long,Object> phantasms(ClientSession pc,Object phase,long room,int map,int instance,int x,int y,int range,long now){
  var values=(Map<Long,Object>)CombatAccess.call(pc,"phantasmTargets","(L"+P+"ClientSession$Phase;JIIIIIJ)Ljava/util/Map;",phase,room,map,instance,x,y,range,now);
  if(values==null)return null;var c=current.get();var result=new LinkedHashMap<Long,Object>();
  for(var entry:values.entrySet()){
   Object value=entry.getValue();var player=(ClientSession)CombatAccess.call(value,"player","()L"+P+"ClientSession;");
   String kind=String.valueOf(CombatAccess.call(value,"kind","()L"+P+"BattleTrueTarget$Kind;"));
   if(kind.equals("PLAYER")?player!=null&&c.player(player):c.npc(entry.getKey()))result.put(entry.getKey(),value);
  }return result;
 }
 static void silence(ClientSession pc,byte[] packet,BattleSpellCatalog.AreaSilenceSpell spell,int fixture)throws IOException{run(pc,spell.skillId(),()->pc.nativeMaterialAreaSilenceBody(packet,spell,fixture));}
 static void wind(ClientSession pc,byte[] packet,BattleSpellCatalog.WindShackleSpell spell,int fixture)throws IOException{run(pc,spell.skillId(),()->pc.nativeMaterialAreaWindBody(packet,spell,fixture));}
 static void run(ClientSession pc,int skill,NativeSelfCastAction.Task body)throws IOException{
  var context=new Context(pc);
  if(!context.phase.equals("WORLD")&&!context.phase.equals("BATTLE"))return;
  // Nonblocking acquisition prevents inversion with an existing two-player action or a changing character ID.
  var locks=context.players.stream().map(ClientSession::nativePlayerActionLock).distinct().toList();int acquired=0;
  for(int attempt=0;attempt<32;attempt++){
   for(;acquired<locks.size();acquired++)if(!locks.get(acquired).tryLock())break;
   if(acquired==locks.size())break;
   while(acquired>0)locks.get(--acquired).unlock();
   if(Thread.currentThread().isInterrupted()||!context.caster.matches(pc))return;
   LockSupport.parkNanos(1_000_000L);
  }
  if(acquired!=locks.size())return;
  var scope=NativePlayerActionScope.enter(context.players.toArray(ClientSession[]::new));
  try{
   if(!context.scene())return;
   NativeLightAction.commitPair(pc,pc,()->NativeWorldMaterialPolicy.run(pc,context.phase.equals("WORLD"),skill,()->{
    synchronized(context.hub){
     if(!context.scene())return;
     if(context.population!=null)synchronized(context.population){owned(context,body);}else owned(context,body);
    }
   }));
  }finally{
   while(acquired>0)locks.get(--acquired).unlock();
   for(var member:context.players)NativePlayerActionScope.unlocked(member);
   scope.close();
  }
 }
 static void owned(Context context,NativeSelfCastAction.Task body)throws IOException{var prior=current.get();current.set(context);try{body.run();}finally{if(prior==null)current.remove();else current.set(prior);}}
 static List<ClientSession> filter(List<ClientSession> candidates){var c=current.get();if(c==null)throw new IllegalStateException("area membership outside cast");return candidates.stream().filter(c::player).toList();}
 static List<ClientSession> worldMembers(ClientSession pc,int map){var c=current.get();return filter(c.hub.classicWorldMembers(map,c.instance));}
 static List<ClientSession> battleMembers(GameHub hub,long room){return filter(hub.roomMembers(room));}
 static List<ClientSession> battleCombatants(GameHub hub,long room,ClientSession pc){return filter(hub.visibleBattleCombatants(room,pc));}
 static List<ClassicWorldMonsterPopulation.Snapshot> worldNpcs(GameHub hub,int map,int instance,int x,int y){var c=current.get();return hub.classicWorldMonstersInScreen(map,instance,x,y).stream().filter(n->c.npc(n.objectId())).toList();}
 static List<BattleQuestEncounter.Snapshot> questNpcs(GameHub hub,long room,ClientSession pc){var c=current.get();return hub.visibleQuestMonsters(room,pc).stream().filter(n->c.npc(n.objectId())).toList();}
 static List<BattleTimeAttackEncounter.Snapshot> timeNpcs(GameHub hub,long room,ClientSession pc){var c=current.get();return hub.visibleTimeAttackActors(room,pc).stream().filter(n->c.npc(n.objectId())).toList();}
 static boolean admit(ClientSession pc,int skill,int action,int reuse){if(skill!=161&&skill!=167&&skill!=66&&skill!=76)throw new IllegalStateException("unreviewed area skill");return NativeLightAction.admitPair(pc,pc,skill,action,reuse);}
 static boolean captureOutput(ClientSession receiver,byte[] plain,String note){
  var context=current.get();if(context==null){current.remove();return false;}
  var binding=context.bindings.get(receiver);if(binding==null)return true;
  byte[] frozen=plain.clone();long id=frozen.length<5?-1:((long)(frozen[1]&255)|((long)(frozen[2]&255)<<8)|((long)(frozen[3]&255)<<16)|((long)(frozen[4]&255)<<24));
  var targets=context.players.stream().filter(pc->pc.playerObjectId()==id).toList();boolean npc=context.npcs.containsKey(id);
  Runnable output=NativeRolePublication.completion(receiver,()->{
   boolean valid;
   synchronized(context.hub){if(context.population!=null)synchronized(context.population){valid=binding.matches(receiver)&&context.player(receiver)&&targets.stream().allMatch(context::player)&&(!npc||context.npc(id));}
    else valid=binding.matches(receiver)&&context.player(receiver)&&targets.stream().allMatch(context::player)&&(!npc||context.npc(id));}
   if(valid)try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException e){throw new UncheckedIOException(e);}
  });
  NativeLightAction.finishScoped((published,failure)->output.run());return true;
 }
 private NativeAreaMaterialAction(){}
}
