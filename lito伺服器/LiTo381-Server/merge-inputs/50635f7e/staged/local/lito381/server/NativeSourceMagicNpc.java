package local.lito381.server;
import java.io.*;import java.util.*;
/** Original cast snapshot and immutable accepted metadata for natural NPC magic on the shared target FIFO. */
final class NativeSourceMagicNpc {
 record Npc(NativeAreaMaterialAction.Npc actor,Object generation,int x,int y){}
 record Snapshot(NativeSourceDamageDisplay.Actor source,Map<Long,Npc> targets,List<ClientSession> members,Map<ClientSession,NativePlayerCommit.Binding> bindings,Map<Integer,List<ClientSession>> recipients){}
 record After(BattleEraseMagicStatus.Lease erase){}
 static boolean supported(String original,boolean physical){return !physical&&Set.of("nativeSourceControlledOriginal_useAreaAttackSpell","nativeSourceControlledOriginal_useCenteredAreaAttackSpell").contains(original);}
 static Object snapshot(NativeAreaMaterialAction.Context c){
  var previous=NativeAreaMaterialAction.current.get();if(previous!=null&&previous.pc==c.pc&&previous.nativeSourceMagicSnapshot instanceof Snapshot s)return s;
  if(!NativeSourceDollCommit.enabled(c.pc)||!c.phase.equals("BATTLE"))return null;
  synchronized(c.hub){var source=new NativeSourceDamageDisplay.Actor(c.pc,c.caster,NativeSourceDamageDisplay.life(c.pc),NativeSourceDamageDisplay.n(c.pc,"positionX"),NativeSourceDamageDisplay.n(c.pc,"positionY"));var targets=new HashMap<Long,Npc>();
   for(var n:c.npcs.values())if(n.kind()==1||n.kind()==2)synchronized(n.owner()){targets.put(n.id(),new Npc(n,NativeProcHealingEncounter.generation(n.owner()),(int)CombatAccess.field(n.actor(),"x"),(int)CombatAccess.field(n.actor(),"y")));}
   var bindings=new IdentityHashMap<ClientSession,NativePlayerCommit.Binding>();bindings.putAll(c.bindings);var recipients=new HashMap<Integer,List<ClientSession>>();for(int force=0;force<2;force++)recipients.put(force,List.copyOf(c.hub.battleRecipients(c.room,force)));
   return new Snapshot(source,Map.copyOf(targets),List.copyOf(c.players),Collections.unmodifiableMap(bindings),Map.copyOf(recipients));
  }
 }
 static boolean matches(NativeAreaMaterialAction.Npc actual,Npc frozen){return actual!=null&&frozen!=null&&actual.id()==frozen.actor().id()&&actual.kind()==frozen.actor().kind()&&actual.owner()==frozen.actor().owner()&&actual.actor()==frozen.actor().actor()&&actual.epoch()==frozen.actor().epoch();}
 static NativeSourceMonsterHp.Aim aim(ClientSession pc,long id,boolean quest){var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||!(c.nativeSourceMagicSnapshot instanceof Snapshot s)||s.source().pc()!=pc||!s.source().binding().equals(c.caster))return null;var frozen=s.targets().get(id);var n=c.npcs.get(id);if(!matches(n,frozen)||n.kind()!=(quest?1:2))return null;return new NativeSourceMonsterHp.Aim(c,n,(NativeSourceMonsterHp.Cache)CombatAccess.field(n.actor(),NativeSourceMonsterHp.CACHE),s.source(),frozen.generation(),frozen.x(),frozen.y());}
 static void enter(ClientSession pc,Object[] args,String original,String descriptor,boolean physical)throws IOException{
  if(!supported(original,physical)||!NativeSourceDollCommit.enabled(pc)){NativeSourceControlledDisplay.nativeSourceMagicOriginalEnter(pc,args,original,descriptor,physical);return;}
  var old=NativeAreaMaterialAction.current.get();if(old!=null&&old.pc==pc&&old.nativeSourceMagicSnapshot instanceof Snapshot){NativeSourceControlledDisplay.nativeSourceMagicOriginalEnter(pc,args,original,descriptor,false);return;}
  var c=new NativeAreaMaterialAction.Context(pc);NativeAreaMaterialAction.owned(c,()->NativeSourceControlledDisplay.nativeSourceMagicOriginalEnter(pc,args,original,descriptor,false));
 }
 static int magicQuest(ClientSession pc,long id,int damage,String source,BattleEraseMagicStatus.Lease erase)throws IOException{return magic(pc,id,damage,source,erase,true);}
 static int magicTime(ClientSession pc,long id,int damage,String source,BattleEraseMagicStatus.Lease erase)throws IOException{return magic(pc,id,damage,source,erase,false);}
 static int magic(ClientSession pc,long id,int damage,String source,BattleEraseMagicStatus.Lease erase,boolean quest)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc))return quest?NativeSourceNpcDamageDisplay.magicQuest(pc,id,damage,source):NativeSourceNpcDamageDisplay.magicTime(pc,id,damage,source);
  var aim=aim(pc,id,quest);if(aim==null)return 0;if(damage<=0){eraseWithoutHp(aim,erase);return 0;}var c=aim.c();Object previous=c.nativeSourceMagicAdmission;c.nativeSourceMagicAdmission=new After(erase);
  try{NativeSourcePhysicalNpc.submit(aim,damage,2,false,source,null,null,null);return 0;}finally{c.nativeSourceMagicAdmission=previous;}
 }
 /** Fixed Source applies nonpositive-damage target mutations without an accepted HP event. */
 static void eraseWithoutHp(NativeSourceMonsterHp.Aim aim,BattleEraseMagicStatus.Lease lease){if(lease==null)return;var life=NativePlayerLifetime.of(aim.c().hub);NativeSourcePlayerActionLock.runPairOrdered(aim.caster().pc(),aim.caster().pc(),()->{synchronized(life.admission){if(!life.admits()||!aim.caster().valid(aim.caster().life())||NativeSourceDamageDisplay.n(aim.caster().pc(),"playerHp")<=0||(boolean)CombatAccess.field(aim.caster().pc(),"observer")||(boolean)CombatAccess.field(aim.c().hub,"nativeStaticMotionClosed"))return;synchronized(aim.c().hub){synchronized(aim.npc().owner()){if(!aim.c().scene()||!aim.c().npc(aim.npc().id())||NativeSourceArenaPoison.rawLife(aim.npc().actor())!=aim.npc().epoch()||NativeProcHealingEncounter.generation(aim.npc().owner())!=aim.generation()||(int)CombatAccess.field(aim.npc().actor(),"x")!=aim.x()||(int)CombatAccess.field(aim.npc().actor(),"y")!=aim.y())return;if(NativeSourceArenaPoison.actor(aim.npc().owner(),aim.npc().id(),aim.npc().kind()==1)!=aim.npc().actor())return;if(aim.npc().kind()==1)aim.c().hub.consumeQuestMonsterEraseMagic(aim.c().room,aim.npc().id(),lease);else aim.c().hub.consumeTimeAttackActorEraseMagic(aim.c().room,aim.npc().id(),lease);}}}});}
 static boolean consumeQuest(GameHub hub,long room,long id,BattleEraseMagicStatus.Lease lease,ClientSession pc){return NativeSourceDollCommit.enabled(pc)?false:hub.consumeQuestMonsterEraseMagic(room,id,lease);}
 static boolean consumeTime(GameHub hub,long room,long id,BattleEraseMagicStatus.Lease lease,ClientSession pc){return NativeSourceDollCommit.enabled(pc)?false:hub.consumeTimeAttackActorEraseMagic(room,id,lease);}
 static Object payload(ClientSession pc){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.pc==pc&&c.nativeSourceMagicAdmission instanceof After a?a:null;}
 static Snapshot admission(){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceMagicAdmission instanceof After&&c.nativeSourceMagicSnapshot instanceof Snapshot s?s:null;}
 static List<ClientSession> members(GameHub hub,long room){var s=admission();return s!=null&&s.source().binding().room()==room?s.members():hub.roomMembers(room);}
 static List<ClientSession> recipients(GameHub hub,long room,int force){var s=admission();return s!=null&&s.source().binding().room()==room?s.recipients().getOrDefault(force,List.of()):hub.battleRecipients(room,force);}
 static NativePlayerCommit.Binding binding(ClientSession pc){var s=admission();return s==null?NativePlayerCommit.Binding.capture(pc):s.bindings().get(pc);}
 /** Invoked by the original accepted callback under its source actor/admission lock and owned output scope. */
 static void accepted(NativeSourcePhysicalNpc.Commit p){
  if(p==null||!p.accepted||!(p.nativeSourceMagicAfter instanceof After a))return;try{NativeSourceDamageDisplay.publish(p.visual);}catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}if(a.erase()==null||!p.current()||!NativeSourcePhysicalNpc.sourceCurrent(p.origin))return;var s=p.origin.scene();
  synchronized(s.hub()){synchronized(p.target.owner()){if(!p.current()||!NativeSourcePhysicalNpc.sourceCurrent(p.origin)||NativeSourceArenaPoison.actor(p.target.owner(),p.target.id(),p.target.kind()==1)!=p.target.actor())return;
   if(p.target.kind()==1)s.hub().consumeQuestMonsterEraseMagic(s.id(),p.target.id(),a.erase());else s.hub().consumeTimeAttackActorEraseMagic(s.id(),p.target.id(),a.erase());
  }}
 }
 private NativeSourceMagicNpc(){}
}
