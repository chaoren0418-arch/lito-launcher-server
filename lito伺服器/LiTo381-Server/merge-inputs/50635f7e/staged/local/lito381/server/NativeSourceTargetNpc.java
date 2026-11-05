package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.ThreadLocalRandom;import java.util.concurrent.atomic.AtomicBoolean;
/** Calculated NPC target mutations join the existing actor HP FIFO; original chance rolls never repeat. */
final class NativeSourceTargetNpc {
 record Mutation(BattleEraseMagicStatus.Lease erase,int freezeSeconds,BattleSpellCatalog.ShockStunSpell shock,boolean shockRolled,AtomicBoolean claimed){
  Mutation(BattleEraseMagicStatus.Lease erase,int freeze,BattleSpellCatalog.ShockStunSpell shock,boolean rolled){this(erase,Math.max(0,freeze),shock,rolled,new AtomicBoolean());}
 }
 record Status(NativeSourcePhysicalNpc.Origin origin,Object life,Object lease,long expires,boolean freeze){}
 record Output(Status status,ClientSession receiver,NativePlayerCommit.Binding binding,byte[] packet,String note,boolean removal){}
 static boolean supported(String original,boolean physical){return !physical&&Set.of("nativeSourceControlledOriginal_nativeSourceLightningOriginalBody","nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle","nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle").contains(original);}
 static boolean enter(ClientSession pc,Object[] args,String original,String descriptor,boolean physical)throws IOException{
  if(!supported(original,physical)||!NativeSourceDollCommit.enabled(pc))return false;
  var old=NativeAreaMaterialAction.current.get();if(old!=null&&old.pc==pc&&old.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot){NativeSourceControlledDisplay.nativeSourceMagicOriginalEnter(pc,args,original,descriptor,false);return true;}
  var c=new NativeAreaMaterialAction.Context(pc);NativeAreaMaterialAction.owned(c,()->NativeSourceControlledDisplay.nativeSourceMagicOriginalEnter(pc,args,original,descriptor,false));return true;
 }
 static void shockEnter(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc)){NativeShockPlayerAction.nativeSourceTargetOriginalBattle(pc,packet,spell);return;}
  var c=new NativeAreaMaterialAction.Context(pc);NativeAreaMaterialAction.owned(c,()->NativeShockPlayerAction.nativeSourceTargetOriginalBattle(pc,packet,spell));
 }
 static boolean before(ClientSession pc,Protocol.AttackSpellRequest request){return NativeSourceSingleNpc.before(pc,request);}
 static boolean beforePacket(ClientSession pc,byte[] packet){return !NativeSourceDollCommit.enabled(pc)||before(pc,Protocol.attackSpellRequest(packet));}
 static Object payload(ClientSession pc){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.pc==pc&&c.nativeSourceMagicAdmission instanceof Mutation m?m:null;}
 static NativeSourceMagicNpc.Snapshot admission(){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceMagicAdmission instanceof Mutation&&c.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot s?s:null;}
 static int scalar(Map<?,?> map,Object key,int fallback){var value=map==null?null:map.get(key);return value instanceof Integer i?i:fallback;}
 static boolean freezeAllowed(boolean present,boolean boss,int chance,int roll){return present&&!boss&&chance>=roll;}
 static boolean line(ClientSession pc,Map.Entry<?,?> entry,BattleSpellCatalog.DirectionalLineSpell spell,BattleSpellCatalog.FreezeAttackSpell freeze,Map<?,?> chances,Map<?,?> rolls,Map<?,?> bosses,Map<?,?> erases,boolean quest)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc))return false;if(entry==null||!(entry.getKey()instanceof Long id)||!(entry.getValue()instanceof Integer damage))return true;
  boolean blocked=bosses!=null&&Boolean.TRUE.equals(bosses.get(id));int seconds=freezeAllowed(freeze!=null,blocked,scalar(chances,id,0),scalar(rolls,id,100))?freeze.durationSeconds():0;
  return submit(pc,id,damage,new Mutation(lease(erases,id),seconds,null,false),"line-magic-"+spell.skillId(),quest);
 }
 static BattleEraseMagicStatus.Lease lease(Map<?,?> map,Object key){var value=map==null?null:map.get(key);return value instanceof BattleEraseMagicStatus.Lease l?l:null;}
 static long id(Object snapshot){return snapshot instanceof BattleQuestEncounter.Snapshot q?q.objectId():snapshot instanceof BattleTimeAttackEncounter.Snapshot t?t.objectId():0;}
 static boolean area(ClientSession pc,Object snapshot,BattleSpellCatalog.FreezeAttackSpell spell,int damage,Map<?,?> chances,Map<?,?> rolls,Map<?,?> erases,boolean quest)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc))return false;long id=id(snapshot);if(id==0)return true;boolean boss=snapshot instanceof BattleTimeAttackEncounter.Snapshot t&&CombatProbability.bossGate(t.finalBoss(),spell);
  int seconds=freezeAllowed(spell!=null,boss,scalar(chances,id,0),scalar(rolls,id,100))?spell.durationSeconds():0;
  return submit(pc,id,damage,new Mutation(lease(erases,id),seconds,null,false),"area-freeze-"+spell.skillId(),quest);
 }
 static boolean single(ClientSession pc,Object snapshot,BattleSpellCatalog.FreezeAttackSpell spell,int damage,BattleEraseMagicStatus.Lease erase,int chance,int roll,SourceNpcPhysicalStats stats,boolean quest)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc))return false;long id=id(snapshot);if(id==0)return true;boolean boss=(snapshot instanceof BattleTimeAttackEncounter.Snapshot t&&CombatProbability.bossGate(t.finalBoss(),spell))||(stats!=null&&CombatProbability.bossGate(stats.boss(),spell));
  int seconds=freezeAllowed(spell!=null,boss,chance,roll)?spell.durationSeconds():0;
  return submit(pc,id,damage,new Mutation(erase,seconds,null,false),"single-freeze-"+spell.skillId(),quest);
 }
 static boolean shock(ClientSession pc,Object snapshot,BattleSpellCatalog.ShockStunSpell spell,int damage,boolean rolled,boolean quest)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc))return false;long id=id(snapshot);if(id==0)return true;
  // Source SHOCK_STUN draws duration in its accepted target effect, unlike freeze probability.
  return submit(pc,id,damage,new Mutation(null,0,spell,rolled),spell.key(),quest);
 }
 static boolean submit(ClientSession pc,long id,int damage,Mutation mutation,String source,boolean quest)throws IOException{
  var aim=NativeSourceMagicNpc.aim(pc,id,quest);if(aim==null)return true;var c=aim.c();
  synchronized(c.hub){synchronized(aim.npc().owner()){if(!NativeSourceSingleNpc.current(aim)||(int)CombatAccess.field(aim.npc().actor(),"hp")<=0)return true;}}
  if(damage<=0){withoutHp(aim,mutation);return true;}
  var prior=c.nativeSourceMagicAdmission;c.nativeSourceMagicAdmission=mutation;try{NativeSourcePhysicalNpc.submit(aim,damage,2,false,source,null,null,null);}finally{c.nativeSourceMagicAdmission=prior;}return true;
 }
 static boolean claim(Mutation mutation,boolean accepted){return mutation!=null&&accepted&&mutation.claimed().compareAndSet(false,true);}
 static void early(NativeSourcePhysicalNpc.Commit p){if(p==null||!(p.nativeSourceMagicAfter instanceof Mutation m)||!claim(m,p.accepted))return;var pc=p.origin.source().pc();try{NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{synchronized(p.lifetime.admission){if(!p.current()||!NativeSourcePhysicalNpc.sourceCurrent(p.origin))return;try{NativeLightAction.commitSelf(pc,()->{
   try{NativeSourceDamageDisplay.publish(p.visual);}catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}
   release(p.origin,p.acceptedLife,m,p.recipients,p.viewers);
  });}catch(IOException failure){throw new UncheckedIOException(failure);}}});}catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}}
 static void withoutHp(NativeSourceMonsterHp.Aim aim,Mutation mutation){var c=aim.c();var life=NativePlayerLifetime.of(c.hub);NativeSourcePlayerActionLock.runPairOrdered(c.pc,c.pc,()->{synchronized(life.admission){if(!life.admits())return;synchronized(c.hub){synchronized(aim.npc().owner()){
  if(!NativeSourceSingleNpc.current(aim)||(int)CombatAccess.field(aim.npc().actor(),"hp")<=0)return;var snapshot=(NativeSourceMagicNpc.Snapshot)c.nativeSourceMagicSnapshot;var n=aim.npc();var room=((Map<?,?>)CombatAccess.field(c.hub,"rooms")).get(c.room);if(room==null)return;
  int force=n.kind()==1?(int)CombatAccess.call(c.hub,"forceOf","(Llocal/lito381/server/GameHub$Room;Llocal/lito381/server/ClientSession;)I",room,c.pc):-1;
  var target=new NativeSourceCubeOrigin.Target(n.id(),n.kind(),null,null,n.actor(),n.owner(),n.epoch(),aim.generation());var origin=new NativeSourcePhysicalNpc.Origin(new NativeSourceCubeOrigin.Scene(c.hub,room,c.room),target,aim.caster(),null,force,aim.x(),aim.y());
  var members=n.kind()==1?snapshot.recipients().getOrDefault(force,List.of()):snapshot.members();release(origin,n.epoch(),mutation,members,snapshot.bindings());
 }}}});}
 /** All status writes and native placement keep their original Hub/encounter monitors; output is only deferred here. */
 static void release(NativeSourcePhysicalNpc.Origin origin,Object life,Mutation mutation,List<ClientSession> members,Map<ClientSession,NativePlayerCommit.Binding> bindings){var s=origin.scene();var t=origin.target();synchronized(s.hub()){synchronized(t.owner()){
  if(!targetCurrent(origin,life))return;if(mutation.erase()!=null)((BattleEraseMagicStatus)CombatAccess.field(t.actor(),"eraseMagicStatus")).consume(mutation.erase());
  if((int)CombatAccess.field(t.actor(),"hp")<=0||NativeSourceArenaPoison.actor(t.owner(),t.id(),t.kind()==1)!=t.actor())return;
  boolean freeze=mutation.freezeSeconds()>0;int seconds=freeze?mutation.freezeSeconds():mutation.shockRolled()&&mutation.shock()!=null?mutation.shock().rollStunSeconds(bound->ThreadLocalRandom.current().nextInt(1,bound+1)):0;if(seconds<=0)return;long now=System.nanoTime();var pc=origin.source().pc();boolean applied=freeze?(t.kind()==1?s.hub().freezeQuestMonster(s.id(),pc,t.id(),now,seconds):s.hub().freezeTimeAttackActor(s.id(),pc,t.id(),now,seconds)):(t.kind()==1?s.hub().shockStunQuestMonster(s.id(),pc,t.id(),now,seconds):s.hub().shockStunTimeAttackActor(s.id(),pc,t.id(),now,seconds));if(!applied)return;
  String field=freeze?"freezeExpiresAtNanos":"shockStunExpiresAtNanos";long expiry=(long)CombatAccess.field(t.actor(),field);Object lease=CombatAccess.field(t.actor(),freeze?"nativeSourceTargetFreezeLease":"nativeSourceTargetShockLease");var status=new Status(origin,life,lease,expiry,freeze);
  if(freeze){var placed=s.hub().placeFreezeEffect(s.id(),t.id(),origin.x(),origin.y(),now,seconds);if(placed!=null){if(placed.previous()!=null)emit(status,members,bindings,Protocol.removeObject(placed.previous().objectId()),"Source NPC replaced freeze effect",true);emit(status,members,bindings,Protocol.freezeEffect(placed.placed()),"Source NPC accepted freeze effect",false);}emit(status,members,bindings,Protocol.poison(t.id(),2),"Source NPC accepted freeze palette",false);}
  else{var placed=s.hub().placeShockStunEffect(s.id(),t.id(),origin.x(),origin.y(),now,seconds);if(placed!=null){if(placed.previous()!=null)emit(status,members,bindings,Protocol.removeObject(placed.previous().objectId()),"Source NPC replaced shock effect",true);emit(status,members,bindings,Protocol.shockStunEffect(placed.placed()),"Source NPC accepted shock effect",false);}}
 }}}
 static boolean targetCurrent(NativeSourcePhysicalNpc.Origin origin,Object life){var t=origin.target();return NativeSourcePhysicalNpc.sceneCurrent(origin,NativePlayerLifetime.of(origin.scene().hub()))&&NativeSourcePhysicalNpc.sourceCurrent(origin)&&CombatAccess.field(t.owner(),"sourceProcEncounterGeneration")==t.generation()&&NativeSourceArenaPoison.rawLife(t.actor())==life&&(int)CombatAccess.field(t.actor(),"x")==origin.x()&&(int)CombatAccess.field(t.actor(),"y")==origin.y();}
 static boolean leaseCurrent(Object expected,Object actual,long expires,long actualExpires,long now){return expected!=null&&expected==actual&&expires==actualExpires&&now>=0&&now<expires;}
 // Called at every original actor status-expiry write, under that original method's monitor.
 static void writeFreeze(Object actor,long expires){CombatAccess.set(actor,"nativeSourceTargetFreezeLease",new Object());CombatAccess.set(actor,"freezeExpiresAtNanos",expires);}
 static void writeShock(Object actor,long expires){CombatAccess.set(actor,"nativeSourceTargetShockLease",new Object());CombatAccess.set(actor,"shockStunExpiresAtNanos",expires);}
 /** Existing final Frame monitor reads only original identity and volatile status/life fields. */
 static boolean current(Output out){var status=out.status();var o=status.origin();var pc=out.receiver();var b=out.binding();var life=NativePlayerLifetime.of(o.scene().hub());if(b==null||!NativeSourcePhysicalNpc.sceneCurrent(o,life)||!NativeAcceptedFreezeMutation.roleFrame(b,pc)||(boolean)CombatAccess.field(pc,"sourceItemDisconnected")||CombatAccess.field(pc,"hub")!=o.scene().hub()||!String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")||(long)CombatAccess.field(pc,"currentRoomId")!=o.scene().id())return false;
  if(out.removal())return true;if(!targetCurrent(o,status.life())||(int)CombatAccess.field(o.target().actor(),"hp")<=0)return false;String prefix=status.freeze()?"Freeze":"Shock";return leaseCurrent(status.lease(),CombatAccess.field(o.target().actor(),"nativeSourceTarget"+prefix+"Lease"),status.expires(),(long)CombatAccess.field(o.target().actor(),status.freeze()?"freezeExpiresAtNanos":"shockStunExpiresAtNanos"),System.nanoTime());
 }
 static void emit(Status status,List<ClientSession> members,Map<ClientSession,NativePlayerCommit.Binding> bindings,byte[] packet,String note,boolean removal){for(var pc:members){var out=new Output(status,pc,bindings.get(pc),packet.clone(),note,removal);NativeLightAction.deferScoped(()->{try{var c=new NativeAreaMaterialAction.Context(pc);var previous=c.nativeSourceSingleNpcOutput;c.nativeSourceSingleNpcOutput=out;try{NativeAreaMaterialAction.owned(c,()->{synchronized(pc){if(current(out))pc.nativeSelfOriginalSend(out.packet(),out.note());}});}finally{c.nativeSourceSingleNpcOutput=previous;}}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(NativePlayerLifetime.of(status.origin().scene().hub()));}});}}
 static boolean allowFrame(ClientSession pc,byte[] packet){var c=NativeAreaMaterialAction.current.get();return c==null||!(c.nativeSourceSingleNpcOutput instanceof Output out)||out.receiver()==pc&&current(out);}
 private NativeSourceTargetNpc(){}
}
