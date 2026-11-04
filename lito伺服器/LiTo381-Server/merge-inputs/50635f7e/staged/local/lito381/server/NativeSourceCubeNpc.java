package local.lito381.server;
import java.io.*;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Source effect-NPC HP ingress for the original arena encounter actor, independent of creator availability. */
final class NativeSourceCubeNpc {
 static final String P="local/lito381/server/";
 record Effect(NativeSourceCubeOrigin.Entity entity,NativePlayerLifetime lifetime,NativeSourceCubeOrigin.Target target){public String toString(){return "CubeNpcSource[id="+entity.objectId()+"]";}}
 record Spawn(Object actor,Object life,Object generation){}
 static final class Commit {
  final NativeSourceCubeOrigin.Lease lease;final NativeSourceCubeOrigin.Target target;final NativePlayerLifetime lifetime;final Effect effect;final int damage,force,palette,maxHp;
  final List<ClientSession> members,recipients;final Map<ClientSession,NativePlayerCommit.Binding> viewers;final NativeSourceMonsterHp.Cache cache;
  final AtomicBoolean released=new AtomicBoolean();final Map<Long,Spawn> spawned=new HashMap<>();
  volatile boolean admitted,accepted,dead;volatile Object acceptedLife,outcome;
  volatile boolean nativeSourceCubeQueued;volatile boolean nativeSourceCubeQueueClaimed;
  Commit(NativeSourceCubeOrigin.Lease lease,NativePlayerLifetime lifetime,boolean ignition){
   this.lease=lease;target=lease.target();this.lifetime=lifetime;effect=new Effect(lease.effect(),lifetime,target);damage=rawDamage(ignition);force=target.kind()==1?(int)CombatAccess.field(target.actor(),"force"):-1;
   var scene=lease.effect().scene();members=NativeSourceElementAudience.copy(NativeSourceElementAudience.members(scene.hub(),scene.id()));recipients=target.kind()==1?NativeSourceElementAudience.copy(NativeSourceElementAudience.recipients(scene.hub(),scene.id(),force,members)):members;
   var bindings=new IdentityHashMap<ClientSession,NativePlayerCommit.Binding>();for(var pc:members)bindings.put(pc,NativePlayerCommit.Binding.capture(pc));for(var pc:recipients)bindings.putIfAbsent(pc,NativePlayerCommit.Binding.capture(pc));viewers=Collections.unmodifiableMap(bindings);
   var snapshot=CombatAccess.call(target.actor(),"snapshot","()L"+P+(target.kind()==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Snapshot;");maxHp=(int)CombatAccess.call(snapshot,"maxHp","()I");
   palette=target.kind()==1?scene.hub().questMonsterPoisonPalette(scene.id(),target.id(),System.nanoTime()):scene.hub().timeAttackActorPoisonPalette(scene.id(),target.id(),System.nanoTime());cache=(NativeSourceMonsterHp.Cache)CombatAccess.field(target.actor(),NativeSourceMonsterHp.CACHE);acceptedLife=target.life();
  }
  void release(){if(admitted&&released.compareAndSet(false,true))lifetime.pending.decrementAndGet();}
  boolean current(){return accepted&&entityCurrent(effect)&&CombatAccess.field(target.owner(),"sourceProcEncounterGeneration")==target.generation()&&NativeSourceArenaPoison.rawLife(target.actor())==acceptedLife;}
  void seal(Object outcome){this.outcome=outcome;acceptedLife=NativeSourceMonsterHp.life(target.actor());dead=(int)CombatAccess.field(target.actor(),"hp")<=0;List<?> created=outcome instanceof BattleQuestEncounter.Hit q?q.spawned():((GameHub.TimeAttackActorHit)outcome).hit().spawned();for(var snapshot:created){long id=(long)CombatAccess.call(snapshot,"objectId","()J");var actor=NativeSourceArenaPoison.actor(target.owner(),id,target.kind()==1);if(actor!=null)spawned.put(id,new Spawn(actor,NativeSourceMonsterHp.life(actor),target.generation()));}accepted=true;}
 }
 record Output(Commit commit,ClientSession receiver,NativePlayerCommit.Binding binding,byte[] packet,boolean reaction){}
 static Object captureFaction(Object value){if(!(value instanceof NativeSourceCubeOrigin.Entity e)||e.scene()==null||e.creator()==null||!Thread.holdsLock(e.scene().hub())||!NativeAcceptedFreezeMutation.roleFrame(e.creator().binding(),e.creator().pc()))return null;return CombatAccess.call(e.scene().hub(),"forceOf","(L"+P+"GameHub$Room;L"+P+"ClientSession;)I",e.scene().room(),e.creator().pc());}
 static boolean factionAllowed(int kind,Object faction,int force){return kind==2||kind==1&&faction instanceof Integer captured&&(captured==0||captured==1)&&captured==force;}
 static boolean factionAllowed(NativeSourceCubeOrigin.Lease lease){return factionAllowed(lease.target().kind(),CombatAccess.field(lease.effect().cube(),"nativeSourceCubeFaction"),lease.target().kind()==1?(int)CombatAccess.field(lease.target().actor(),"force"):-1);}
 static int ratio(int hp,int max){return max<=0?100:(int)Math.max(0,Math.min(100,100L*hp/max));}
 static int rawDamage(boolean ignition){return ignition?10:25;}
 static boolean accepted(Object outcome){if(outcome instanceof BattleQuestEncounter.Hit q)return q.damage()!=null&&q.damage().before().hp()>q.damage().after().hp();if(outcome instanceof GameHub.TimeAttackActorHit t)return t.hit()!=null&&t.hit().damage()!=null&&t.hit().damage().before().hp()>t.hit().damage().after().hp();return false;}
 static boolean entityCurrent(Effect e){return e!=null&&NativeSourceCubePlayer.entityCurrent(e.entity(),e.lifetime());}
 static boolean matches(Object event,NativeSourceCubeOrigin.Lease lease,long id,boolean ignition){
  Object provenance=event instanceof BattleCubeIgnitionEffects.Event i?i.nativeSourceCubeOrigin:event instanceof BattleCubeHarmonyEffects.Event h?h.nativeSourceCubeOrigin:null;if(!(provenance instanceof NativeSourceCubeOrigin.Event bound)||bound.lease()!=lease)return false;
  if(lease==null||lease.effect()==null||!NativeSourceCubeNpcLease.natural(lease.target())||lease.target().id()!=id||lease.effect().npcId()!=(ignition?80149:80152))return false;
  return event instanceof BattleCubeIgnitionEffects.Event i?ignition&&i.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE&&i.targetKind()==BattleCubeIgnitionEffects.ActorKind.MONSTER&&i.targetObjectId()==id&&i.cube()==lease.effect().cube():event instanceof BattleCubeHarmonyEffects.Event h&&!ignition&&h.kind()==BattleCubeHarmonyEffects.EventKind.HP_DAMAGE&&h.targetKind()==BattleCubeHarmonyEffects.ActorKind.MONSTER&&h.targetObjectId()==id&&h.cube()==lease.effect().cube();
 }
 static boolean hp(Object event){return event instanceof BattleCubeIgnitionEffects.Event i&&i.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE&&i.targetKind()==BattleCubeIgnitionEffects.ActorKind.MONSTER||event instanceof BattleCubeHarmonyEffects.Event h&&h.kind()==BattleCubeHarmonyEffects.EventKind.HP_DAMAGE&&h.targetKind()==BattleCubeHarmonyEffects.ActorKind.MONSTER;}
 /** The original clock reaches this before its caster-null branch; no lookup of tick.caster. */
 static boolean dispatch(GameHub hub,Object tick){
  if(!CombatRules.loaded()||(int)CombatAccess.field(tick,"mapId")>=0)return false;var event=CombatAccess.field(tick,"event");if(!hp(event))return false;
  deliver(hub,(long)CombatAccess.field(tick,"roomId"),(int)CombatAccess.field(tick,"mapId"),(int)CombatAccess.field(tick,"instanceId"),event);return true;
 }
 static boolean legacy(ClientSession transport,long room,int map,int instance,BattleCubeIgnitionEffects.Event event){return legacy(transport,room,map,instance,(Object)event);}
 static boolean legacy(ClientSession transport,long room,int map,int instance,BattleCubeHarmonyEffects.Event event){return legacy(transport,room,map,instance,(Object)event);}
 static boolean legacy(ClientSession transport,long room,int map,int instance,Object event){if(!CombatRules.loaded()||map>=0||!hp(event))return false;deliver((GameHub)CombatAccess.field(transport,"hub"),room,map,instance,event);return true;}
 static void deliver(GameHub hub,long room,int map,int instance,Object event){
  Commit p=null;try{
   Object origin=event instanceof BattleCubeIgnitionEffects.Event i?i.nativeSourceCubeOrigin:((BattleCubeHarmonyEffects.Event)event).nativeSourceCubeOrigin;
   if(!(origin instanceof NativeSourceCubeOrigin.Event original))return;var lease=original.lease();boolean ignition=event instanceof BattleCubeIgnitionEffects.Event;long id=event instanceof BattleCubeIgnitionEffects.Event i?i.targetObjectId():((BattleCubeHarmonyEffects.Event)event).targetObjectId();
   if(!matches(event,lease,id,ignition)||map!=-1||instance!=-1||lease.effect().scene()==null||lease.effect().scene().hub()!=hub||lease.effect().scene().id()!=room||NativeCombatAttacker.blocked())return;
   var owner=NativePlayerLifetime.of(hub);
   synchronized(owner.admission){if(owner.mode!=NativePlayerLifetime.Mode.OPEN)return;synchronized(hub){var scene=lease.effect().scene();if(((Map<?,?>)CombatAccess.field(hub,"rooms")).get(room)!=scene.room()||!(boolean)CombatAccess.field(scene.room(),"battleStarted"))return;synchronized(lease.effect().engine()){if(!NativeSourceCubePlayer.installed(lease,System.nanoTime()))return;}
    synchronized(lease.target().owner()){if(!originalCurrent(lease.target(),scene)||!factionAllowed(lease)||ignition&&protectedZero(lease.target(),System.nanoTime()))return;p=new Commit(lease,owner,ignition);p.admitted=true;}
   }}
   // Source invokes the reaction before receiveDamage. No HP/Hub/engine/admission lock is held over IO.
   if(ignition)reaction(p);
   var admitted=p;
   NativeSourceCubeNpcQueue.enqueue(admitted);
  }catch(RuntimeException failure){if(p!=null)failed(p.lifetime);}
  finally{if(p!=null)NativeSourceCubeNpcQueue.producerFinished(p);}
 }
 /** Original actor membership, read only while the original Hub and encounter monitors are held. */
 static boolean member(NativeSourceCubeOrigin.Target t){
  if(t.kind()==1){for(var lane:(Object[])CombatAccess.field(t.owner(),"lanes"))for(var actor:(List<?>)CombatAccess.field(lane,"monsters"))if(actor==t.actor())return true;}
  else if(t.kind()==2)for(var actor:(List<?>)CombatAccess.field(t.owner(),"actors"))if(actor==t.actor())return true;
  return false;
 }
 static boolean originalCurrent(NativeSourceCubeOrigin.Target t,NativeSourceCubeOrigin.Scene s){return NativeSourceCubeNpcLease.natural(t)&&CombatAccess.field(s.room(),t.kind()==1?"questEncounter":"timeAttackEncounter")==t.owner()&&CombatAccess.field(t.owner(),"sourceProcEncounterGeneration")==t.generation()&&NativeSourceArenaPoison.rawLife(t.actor())==t.life()&&(long)CombatAccess.field(t.actor(),"objectId")==t.id()&&(int)CombatAccess.field(t.actor(),"hp")>0&&member(t);}
 static boolean protectedZero(NativeSourceCubeOrigin.Target t,long now){return(boolean)CombatAccess.call(t.actor(),"freezeActive","(J)Z",now)||(boolean)CombatAccess.call(t.actor(),"earthBindActive","(J)Z",now);}
 static boolean nativeSourceCubeQueueOriginalBefore(GameHub hub,long room,Commit p,long id,int damage){
  if(p==null||!p.admitted||p.accepted||p.released.get()||p.damage!=damage||p.target.id()!=id||p.lease.effect().scene().id()!=room||p.lease.effect().scene().hub()!=hub||!Thread.holdsLock(hub)||!Thread.holdsLock(p.target.owner())||!entityCurrent(p.effect))return false;
  var scene=p.lease.effect().scene();return((Map<?,?>)CombatAccess.field(hub,"rooms")).get(room)==scene.room()&&(boolean)CombatAccess.field(scene.room(),"battleStarted")&&factionAllowed(p.lease)&&originalCurrent(p.target,scene);
 }
 /** Original roster predicate now validates the admitted effect event; it grants no player authority. */
 static boolean authority(Set<?> ignored,Commit p){return p!=null&&before(p.lease.effect().scene().hub(),p.lease.effect().scene().id(),p,p.target.id(),p.damage);}
 static int force(Object room,Commit p){if(p==null||room!=p.lease.effect().scene().room()||p.target.kind()!=1)throw new IllegalStateException("original cube target lane required");return p.force;}
 static NativeCombatAttacker.Ref reference(Commit p){var creator=p.lease.effect().creator();return new NativeCombatAttacker.Ref(5,p.lease.effect().objectId(),p.effect,0,creator==null?null:creator.binding(),p.lease.effect().scene().id(),-1,-1);}
 static boolean rawEffectTarget(Object actor){var ref=NativeCombatAttacker.nativeSourceCubeCurrent();return ref!=null&&ref.kind()==5&&ref.owner()instanceof Effect e&&e.target().actor()==actor&&ref.id()==e.entity().objectId()&&entityCurrent(e)&&NativeSourceArenaPoison.rawLife(actor)==e.target().life();}
 static void complete(Commit p){try{commit(p);}finally{if(p.accepted){try{hpbar(p);}catch(RuntimeException failure){failed(p.lifetime);}publish(p);}}}
 static void commit(Commit p){
  var scene=p.lease.effect().scene();synchronized(p.lifetime.admission){if(p.lifetime.mode==NativePlayerLifetime.Mode.STOPPED)return;synchronized(scene.hub()){synchronized(p.target.owner()){
   if(!before(scene.hub(),scene.id(),p,p.target.id(),p.damage))return;var ingress=NativeCombatAttacker.enterKnown(reference(p));try{
    Object outcome=p.target.kind()==1?scene.hub().nativeSourceCubeQuestDamage(scene.id(),p,p.target.id(),p.damage):scene.hub().nativeSourceCubeTimeDamage(scene.id(),p,p.target.id(),p.damage);if(accepted(outcome))p.seal(outcome);
   }finally{NativeCombatAttacker.leave(ingress);}
  }}}
 }
 static ClientSession creator(Commit p){var c=p.lease.effect().creator();if(c==null||!entityCurrent(p.effect))return null;var pc=c.pc();var s=p.lease.effect().scene();return NativeAcceptedFreezeMutation.roleFrame(c.binding(),pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&CombatAccess.field(pc,"hub")==s.hub()&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&(long)CombatAccess.field(pc,"currentRoomId")==s.id()?pc:null;}
 static boolean receiver(Commit p,ClientSession pc,NativePlayerCommit.Binding binding){var scene=p.lease.effect().scene();return binding!=null&&entityCurrent(p.effect)&&NativeAcceptedFreezeMutation.roleFrame(binding,pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&CombatAccess.field(pc,"hub")==scene.hub()&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&(long)CombatAccess.field(pc,"currentRoomId")==scene.id();}
 static long object(byte[] packet){return packet.length<5?-1:(packet[1]&255L)|((packet[2]&255L)<<8)|((packet[3]&255L)<<16)|((packet[4]&255L)<<24);}
 /** Final native Frame predicate: detached identities and volatile epoch/life reads; no new locks. */
 static boolean current(Output out){var p=out.commit();if(!receiver(p,out.receiver(),out.binding()))return false;if(out.reaction())return CombatAccess.field(p.target.owner(),"sourceProcEncounterGeneration")==p.target.generation()&&NativeSourceArenaPoison.rawLife(p.target.actor())==p.target.life()&&(int)CombatAccess.field(p.target.actor(),"hp")>0&&Math.abs((long)NativeSourceDamageDisplay.n(out.receiver(),"positionX")-(int)CombatAccess.field(p.target.actor(),"x"))<=10&&Math.abs((long)NativeSourceDamageDisplay.n(out.receiver(),"positionY")-(int)CombatAccess.field(p.target.actor(),"y"))<=10;
  if(!p.current())return false;if(out.packet().length>0&&(out.packet()[0]&255)==Protocol.S_HIT_RATIO){if(p.dead)return(out.packet()[out.packet().length-1]&255)==255;if((out.packet()[out.packet().length-1]&255)!=ratio((int)CombatAccess.field(p.target.actor(),"hp"),p.maxHp)||creator(p)!=out.receiver()||!NativeSourceOtherControls.settings().hpbar()||(int)CombatAccess.field(p.target.actor(),"hp")<=0||!BattleSpellCatalog.isInNativeScreen(NativeSourceDamageDisplay.n(out.receiver(),"positionX"),NativeSourceDamageDisplay.n(out.receiver(),"positionY"),(int)CombatAccess.field(p.target.actor(),"x"),(int)CombatAccess.field(p.target.actor(),"y")))return false;}
  var spawn=p.spawned.get(object(out.packet()));return spawn==null||CombatAccess.field(p.target.owner(),"sourceProcEncounterGeneration")==spawn.generation()&&NativeSourceArenaPoison.rawLife(spawn.actor())==spawn.life();
 }
 static boolean allowFrame(ClientSession pc,byte[] packet){var c=NativeAreaMaterialAction.current.get();return c==null||!(c.nativeSourceCubeNpcOutput instanceof Output out)||out.receiver()==pc&&current(out);}
 static void inherit(NativeAreaMaterialAction.Context child,NativeAreaMaterialAction.Context parent){if(parent!=null)child.nativeSourceCubeNpcOutput=parent.nativeSourceCubeNpcOutput;}
 static void inherit(NativeAreaMaterialAction.Context child){inherit(child,NativeAreaMaterialAction.current.get());}
 static void output(Commit p,ClientSession pc,NativePlayerCommit.Binding binding,byte[] packet,String note,boolean reaction){
  var out=new Output(p,pc,binding,packet.clone(),reaction);if(!current(out))return;
  try{var c=new NativeAreaMaterialAction.Context(pc);var previous=c.nativeSourceCubeNpcOutput;c.nativeSourceCubeNpcOutput=out;try{NativeAreaMaterialAction.owned(c,()->{synchronized(pc){if(current(out))pc.nativeSelfOriginalSend(out.packet(),note);}});}finally{c.nativeSourceCubeNpcOutput=previous;}}
  catch(IOException|RuntimeException failure){failed(p.lifetime);}
 }
 static void failed(NativePlayerLifetime lifetime){synchronized(lifetime.admission){lifetime.nativeSourceDisplayJobs.failed++;}}
 static void reaction(Commit p){String type=p.target.kind()==1?"BattleQuestEncounter":"BattleTimeAttackEncounter";var snapshot=CombatAccess.call(p.target.actor(),"snapshot","()L"+P+type+"$Snapshot;");int gfx=(int)CombatAccess.call(snapshot,"gfx","()I");if(!BattleHitReactionSuppressCatalog.shouldSendDamageAction(gfx))return;var packet=Protocol.objectAction(p.target.id(),2);for(var pc:p.members)output(p,pc,p.viewers.get(pc),packet,"S_ACTION Source cube effect NPC raw hit",true);}
 static void publish(Commit p){try{if(p.target.kind()==1)ClientSession.nativeSourceCubeQuestCommitted(null,(BattleQuestEncounter.Hit)p.outcome,p.palette,"source-cube-effect-"+p.lease.effect().npcId(),p.damage,p);else ClientSession.nativeSourceCubeTimeCommitted(null,(GameHub.TimeAttackActorHit)p.outcome,p.palette,"source-cube-effect-"+p.lease.effect().npcId(),p.damage,p);}catch(IOException|RuntimeException failure){failed(p.lifetime);}finally{if(p.outcome instanceof BattleQuestEncounter.Hit q&&!q.spawned().isEmpty()||p.outcome instanceof GameHub.TimeAttackActorHit t&&t.hit().spawned().stream().anyMatch(BattleTimeAttackEncounter.Snapshot::activeAi))arm(p.lease.effect().scene().hub(),p.lease.effect().scene().id(),p);if(p.outcome instanceof GameHub.TimeAttackActorHit t&&t.resolution().ended())finish(p.lease.effect().scene().hub(),p.lease.effect().scene().id(),p);}}
 // Bounded copies of the original encounter publication body call these typed ports. A null PC carrier is never a caster.
 static GameHub hub(ClientSession unused,Commit p){return p.lease.effect().scene().hub();}
 static long room(ClientSession unused,Commit p){return p.lease.effect().scene().id();}
 static String name(ClientSession unused,Commit p){return "effect-npc:"+p.lease.effect().objectId();}
 static List<ClientSession> members(GameHub unused,long room,Commit p){return p.current()?p.members.stream().filter(pc->receiver(p,pc,p.viewers.get(pc))).toList():List.of();}
 static List<ClientSession> recipients(GameHub unused,long room,int force,Commit p){return p.current()&&force==p.force?p.recipients.stream().filter(pc->receiver(p,pc,p.viewers.get(pc))).toList():List.of();}
 static void send(ClientSession pc,byte[] packet,String note,Commit p){if(packet.length>0&&(packet[0]&255)==Protocol.S_HIT_RATIO)return;output(p,pc,p.viewers.get(pc),packet,note,false);}
 static List<BattleGuardianTower.Snapshot> visible(GameHub hub,long room,ClientSession pc,Commit p){synchronized(hub){return p.current()&&receiver(p,pc,p.viewers.get(pc))?hub.visibleGuardianTowers(room,pc):List.of();}}
 static List<BattleQuestBoundary.Attribute> opened(GameHub hub,long room,int force,int cleared,Commit p){synchronized(hub){return p.current()?hub.openedQuestBoundary(room,force,cleared):List.of();}}
 static void arm(GameHub hub,long room,Commit p){synchronized(hub){if(p.current())hub.armPveAi(room);}}
 static boolean cancel(GameHub hub,long room,long id,Commit p){synchronized(hub){return p.current()&&id==p.target.id()&&hub.cancelArmorBreak(room,id);}}
 static void finish(GameHub hub,long room,Commit p){synchronized(hub){if(p.current()&&((Map<?,?>)CombatAccess.field(hub,"rooms")).get(room)==p.lease.effect().scene().room())hub.endBattle(room);}}
 static void end(ClientSession pc,BattleRules.Resolution resolution,Commit p){try{NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{if(!p.current()||!receiver(p,pc,p.viewers.get(pc)))return;try{CombatAccess.call(pc,"serverEndBattle","(L"+P+"BattleRules$Resolution;)V",resolution);}catch(RuntimeException failure){failed(p.lifetime);}});}catch(RuntimeException failure){failed(p.lifetime);}}
 static void nativeSourceCubeQueueOriginalLawful(ClientSession unused,int npc,String note,Commit p){var pc=creator(p);if(pc==null||!p.current())return;NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{if(p.current()&&creator(p)==pc)CombatAccess.call(pc,"applyNpcLawfulReward","(ILjava/lang/String;)V",npc,note);});}
 static void hpbar(Commit p){
  if(!p.current())return;
  if(p.dead){List<NativeSourceMonsterHp.Viewer> prior;synchronized(p.cache){if(p.cache.life!=p.target.life())return;prior=List.copyOf(p.cache.viewers.values());p.cache.viewers.clear();p.cache.life=null;}var packet=new Protocol.Writer().c(Protocol.S_HIT_RATIO).d(p.target.id()).c(255).bytes();for(var entry:prior){var pc=entry.pc().get();if(pc!=null)output(p,pc,entry.binding(),packet,"S_HIT_RATIO Source cube monster death hide",false);}return;}
  var pc=creator(p);if(pc==null||!NativeSourceOtherControls.settings().hpbar())return;int ratio=ratio((int)CombatAccess.field(p.target.actor(),"hp"),p.maxHp);var creator=p.lease.effect().creator();if(!BattleSpellCatalog.isInNativeScreen(NativeSourceDamageDisplay.n(pc,"positionX"),NativeSourceDamageDisplay.n(pc,"positionY"),(int)CombatAccess.field(p.target.actor(),"x"),(int)CombatAccess.field(p.target.actor(),"y")))return;
  synchronized(p.cache){if(!p.current()||creator(p)!=pc)return;if(p.cache.life!=p.target.life()){p.cache.viewers.clear();p.cache.life=p.target.life();}long id=creator.binding().object();var old=p.cache.viewers.get(id);if(old!=null&&old.pc().get()==pc&&old.binding().equals(creator.binding())&&old.ratio()==ratio)return;if(p.cache.viewers.size()>=1024&&!p.cache.viewers.containsKey(id))p.cache.viewers.clear();p.cache.viewers.put(id,new NativeSourceMonsterHp.Viewer(new WeakReference<>(pc),creator.binding(),ratio));}
  output(p,pc,creator.binding(),new Protocol.Writer().c(Protocol.S_HIT_RATIO).d(p.target.id()).c(ratio).bytes(),"S_HIT_RATIO Source cube actual master",false);
 }
 static boolean before(GameHub hub,long room,Commit p,long id,int damage){return NativeSourceCubeNpcQueue.before(hub,room,p,id,damage);}
static void lawful(ClientSession unused,int npc,String note,Commit p){NativeSourceCubeNpcQueue.lawful(unused,npc,note,p);}
private NativeSourceCubeNpc(){}
}
