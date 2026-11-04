package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
/** Firewall natural targets use the existing actor FIFO and detached lease proof. */
final class NativeSourceGroundNpc {
 static final String P="local/lito381/server/";
 record Audience(List<ClientSession> members,Map<ClientSession,NativePlayerCommit.Binding> bindings,Map<Integer,List<ClientSession>> recipients){}
 record Selected(NativeSourceCubeOrigin.Target target,int x,int y,Audience audience){}
 record Proof(NativeSourceGroundOrigin.Entity entity,NativeSourceGroundOrigin.Actor source,Selected selected){}
 static Object faction(NativeSourceGroundOrigin.Entity e){
  if(e==null||e.scene()==null||e.scene().hub()==null||e.creator()==null||!Thread.holdsLock(e.scene().hub())||!NativeAcceptedFreezeMutation.roleFrame(e.creator().binding(),e.creator().pc()))return null;
  return CombatAccess.call(e.scene().hub(),"forceOf","(L"+P+"GameHub$Room;L"+P+"ClientSession;)I",e.scene().room(),e.creator().pc());
 }
 static boolean allowed(int kind,Object faction,int force){return NativeSourceCubeNpc.factionAllowed(kind,faction,force);}
 static boolean allowed(Proof p){var t=p.selected().target();return allowed(t.kind(),p.entity().nativeSourceGroundFaction,t.kind()==1?(int)CombatAccess.field(t.actor(),"force"):-1);}
 static Audience audience(NativeSourceGroundOrigin.Scene s){
  var members=List.copyOf(s.hub().roomMembers(s.id()));var bindings=new IdentityHashMap<ClientSession,NativePlayerCommit.Binding>();var recipients=new HashMap<Integer,List<ClientSession>>();
  for(var pc:members)bindings.put(pc,NativePlayerCommit.Binding.capture(pc));for(int force=0;force<2;force++){var list=List.copyOf(s.hub().battleRecipients(s.id(),force));recipients.put(force,list);for(var pc:list)bindings.putIfAbsent(pc,NativePlayerCommit.Binding.capture(pc));}
  return new Audience(members,Collections.unmodifiableMap(bindings),Map.copyOf(recipients));
 }
 static List<BattleGroundEffects.Pulse> advance(BattleGroundEffects engine,long now,List<BattleGroundEffects.Actor> actors,GameHub hub){
  if(!CombatRules.loaded()||!(engine.nativeSourceGroundScene instanceof NativeSourceGroundOrigin.Scene s)||s.hub()!=hub||!Thread.holdsLock(hub))return NativeSourceGroundOrigin.advance(engine,now,actors,hub);
  boolean bound=false;for(var lease:(List<?>)CombatAccess.field(engine,"leases"))if(CombatAccess.field(lease,"nativeSourceGroundEntity") instanceof NativeSourceGroundOrigin.Entity e&&NativeSourceGroundQueued.sameEntity(e)){bound=true;break;}
  if(!bound)return NativeSourceGroundOrigin.advance(engine,now,actors,hub);
  var expanded=new ArrayList<BattleGroundEffects.Actor>();if(actors!=null)expanded.addAll(actors);var scan=new HashMap<Long,Object>();scan.putAll(NativeSourceGroundOrigin.scan(engine,actors,hub));var selected=new LinkedHashMap<Long,Selected>();var ambiguous=new HashSet<Long>();
  if(actors!=null)for(var actor:actors)if(actor!=null)ambiguous.add(actor.objectId());var audience=audience(s);
  for(int kind=1;kind<=2;kind++){var owner=CombatAccess.field(s.room(),kind==1?"questEncounter":"timeAttackEncounter");if(owner==null)continue;synchronized(owner){var all=new ArrayList<Object>();
   if(kind==1){for(var lane:(Object[])CombatAccess.field(owner,"lanes"))all.addAll((List<?>)CombatAccess.field(lane,"monsters"));}else all.addAll((List<?>)CombatAccess.field(owner,"actors"));
   for(var actor:all){long id=(long)CombatAccess.field(actor,"objectId");if(ambiguous.contains(id))continue;var t=new NativeSourceCubeOrigin.Target(id,kind,null,null,actor,owner,NativeSourceMonsterHp.life(actor),NativeProcHealingEncounter.generation(owner));
    if(!NativeSourceCubeNpc.originalCurrent(t,new NativeSourceCubeOrigin.Scene(hub,s.room(),s.id())))continue;
    var value=new Selected(t,(int)CombatAccess.field(actor,"x"),(int)CombatAccess.field(actor,"y"),audience);if(selected.putIfAbsent(id,value)!=null){selected.remove(id);ambiguous.add(id);}
   }
  }}
  for(var entry:selected.entrySet()){var n=entry.getValue();scan.put(entry.getKey(),n);expanded.add(new BattleGroundEffects.Actor(entry.getKey(),n.x(),n.y(),true,false));}
  var previous=engine.nativeSourceGroundScan;try{engine.nativeSourceGroundScan=Map.copyOf(scan);return engine.advance(now,expanded);}finally{engine.nativeSourceGroundScan=previous;}
 }
 static Object pulse(BattleGroundEffects.Pulse pulse,BattleGroundEffects engine,Object lease){
  if(!(engine.nativeSourceGroundScan instanceof Map<?,?> scan)||!(scan.get(pulse.targetObjectId()) instanceof Selected selected))return NativeSourceGroundOrigin.nativeSourceGroundNpcOriginalPulse(pulse,engine,lease);
  if(pulse.skillId()!=58||pulse.bot()||!(CombatAccess.field(lease,"nativeSourceGroundEntity") instanceof NativeSourceGroundOrigin.Entity e)||!NativeSourceGroundQueued.sameEntity(e)||e.creator()==null)return null;
  var source=new NativeSourceGroundOrigin.Actor(e.creator().pc(),e.creator().binding(),NativeSourceControlledDisplay.rawLife(e.creator().pc()));return new Proof(e,source,selected);
 }
 static boolean schedule(Object room,BattleGroundEffects.Pulse pulse,List<Object> ticks){
  if(!(pulse.nativeSourceGroundProof instanceof Proof p))return false;
  if(p.entity()!=null&&p.entity().scene()!=null&&p.entity().scene().room()==room&&pulse.skillId()==58&&p.source()!=null)ticks.add(GameHub.nativeSourceGroundNpcTick(p.entity().scene().id(),p.source().pc(),pulse));return true;
 }
 static boolean entity(Proof p){
  if(p==null||p.entity()==null||p.entity().scene()==null||p.entity().creator()==null||p.source()==null||p.selected()==null||p.selected().target()==null)return false;var e=p.entity();var s=e.scene();
  return s.hub()!=null&&NativeSourceGroundQueued.sameEntity(e)&&p.source().pc()==e.creator().pc()&&Objects.equals(p.source().binding(),e.creator().binding())&&NativeSourceGroundOrigin.role(p.source(),s)&&NativePlayerLifetime.of(s.hub()).mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(s.hub(),"nativeStaticMotionClosed");
 }
 static boolean target(Proof p){var n=p.selected();var s=p.entity().scene();var t=n.target();return NativeSourceCubeNpc.originalCurrent(t,new NativeSourceCubeOrigin.Scene(s.hub(),s.room(),s.id()))&&(int)CombatAccess.field(t.actor(),"x")==n.x()&&(int)CombatAccess.field(t.actor(),"y")==n.y()&&allowed(p);}
 static boolean installed(Proof p){if(!entity(p))return false;var e=p.entity();var s=e.scene();if(((Map<?,?>)CombatAccess.field(s.hub(),"rooms")).get(s.id())!=s.room()||!(boolean)CombatAccess.field(s.room(),"battleStarted"))return false;for(var lease:(List<?>)CombatAccess.field(e.engine(),"leases"))if(lease==e.actual())return true;return false;}
 static int fire(int weakAttribute){return weakAttribute==2?-50:weakAttribute==-2?50:0;}
 static int fire(SourceNpcPhysicalStats stats,NativeSourceCubeOrigin.Target target,long now){return fire(stats.weakAttribute())+((BattleElementalFallDownStatus)CombatAccess.field(target.actor(),"elementalFallDownStatus")).modifier(2,now);}
 static int bound(int intelligence){return Math.max(intelligence/2,1);}
 static int damage(int fire,int roll){double reduction=fire>0?(int)(0.32*Math.abs(fire))/32.0:0;return Math.max(0,(int)((1.0-reduction)*(19+roll)));}
 static boolean immune(NativeSourceCubeOrigin.Target t,long now){return NativeSourceCubeNpc.protectedZero(t,now);}
 static NativeSourceMagicNpc.Snapshot admission(){
  var c=NativeAreaMaterialAction.current.get();if(c==null||!(c.nativeSourceGroundProof instanceof Proof p)||!(c.nativeSourceMagicAdmission instanceof NativeSourceRawNpc.Admission raw))return null;
  var a=p.selected().audience();var t=p.selected().target();var n=new NativeAreaMaterialAction.Npc(t.kind(),t.id(),t.owner(),t.actor(),t.life());
  return new NativeSourceMagicNpc.Snapshot(raw.source().actor(),Map.of(t.id(),new NativeSourceMagicNpc.Npc(n,t.generation(),p.selected().x(),p.selected().y())),a.members(),a.bindings(),a.recipients());
 }
 static Object capture(NativeSourcePhysicalNpc.Origin origin){
  var c=NativeAreaMaterialAction.current.get();if(c==null||!(c.nativeSourceGroundProof instanceof Proof p)||origin==null)return null;var t=p.selected().target();var a=origin.source();
  return origin.target()==t&&a.pc()==p.source().pc()&&Objects.equals(a.binding(),p.source().binding())&&a.life()==p.source().life()&&origin.scene().hub()==p.entity().scene().hub()&&origin.scene().room()==p.entity().scene().room()&&origin.scene().id()==p.entity().scene().id()?p:null;
 }
 /** Post-admission expiry does not revoke HP. Epoch/role/life/Main fences remain effective through final output. */
 static boolean scene(NativeSourcePhysicalNpc.Origin origin,NativePlayerLifetime lifetime){return !(origin.nativeSourceGroundNpcProof instanceof Proof p)||lifetime==NativePlayerLifetime.of(p.entity().scene().hub())&&entity(p);}
 /** The original tile test precedes receiveDamage. Admitted raw HP follows that actor when it moves. */
 static int targetState(NativeSourcePhysicalNpc.Origin origin){if(!(origin.nativeSourceGroundNpcProof instanceof Proof p))return -1;var s=p.entity().scene();return origin.target()==p.selected().target()&&allowed(p)&&NativeSourceCubeNpc.originalCurrent(origin.target(),new NativeSourceCubeOrigin.Scene(s.hub(),s.room(),s.id()))?1:0;}
 static boolean dispatch(GameHub hub,Object tick){
  if(!(CombatAccess.field(tick,"nativeSourceGroundProof") instanceof Proof p))return false;
  try{if(CombatRules.loaded()&&p.entity()!=null&&p.entity().scene()!=null&&p.entity().scene().hub()==hub&&(long)CombatAccess.field(tick,"roomId")==p.entity().scene().id()&&(int)CombatAccess.field(tick,"skillId")==58)deliver(p);}catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(NativePlayerLifetime.of(hub));}return true;
 }
 static void deliver(Proof proof){
  if(!entity(proof))return;var pc=proof.source().pc();var s=proof.entity().scene();var life=NativePlayerLifetime.of(s.hub());var prepared=new NativeSourcePhysicalNpc.Commit[1];
  NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{synchronized(life.admission){if(!life.admits())return;synchronized(s.hub()){synchronized(proof.selected().target().owner()){
   if(!installed(proof)||!target(proof)||immune(proof.selected().target(),System.nanoTime()))return;
  }}}
   // Existing readonly repository/effective INT paths; no IO while holding Hub or encounter monitors.
   var t=proof.selected().target();String type=t.kind()==1?"BattleQuestEncounter":"BattleTimeAttackEncounter";var snapshot=CombatAccess.call(t.actor(),"snapshot","()L"+P+type+"$Snapshot;");int npc=(int)CombatAccess.call(snapshot,"npcId","()I");
   var stats=(SourceNpcPhysicalStats)CombatAccess.call(pc,"areaSilenceNpcStats","(I)L"+P+"SourceNpcPhysicalStats;",npc);if(stats==null)return;int intelligence=pc.nativeSkillConsumptionIntelligence();
   synchronized(life.admission){if(!life.admits())return;synchronized(s.hub()){synchronized(t.owner()){
    long now=System.nanoTime();if(!installed(proof)||!target(proof)||immune(t,now))return;int damage=damage(fire(stats,t,now),ThreadLocalRandom.current().nextInt(bound(intelligence)));if(damage<=0)return;
    var actor=new NativeSourceDamageDisplay.Actor(pc,proof.source().binding(),proof.source().life(),NativeSourceDamageDisplay.n(pc,"positionX"),NativeSourceDamageDisplay.n(pc,"positionY"));
    var c=new NativeAreaMaterialAction.Context(pc);c.nativeSourceGroundProof=proof;c.nativeSourceMagicAdmission=new NativeSourceRawNpc.Admission(new NativeSourceRawNpc.Source(actor,false,NativeSourceDamageDisplay.n(pc,"playerHp")==0),t.actor(),t.owner(),t.life(),t.generation());
    try{NativeAreaMaterialAction.owned(c,()->{var origin=new NativeSourcePhysicalNpc.Origin(new NativeSourceCubeOrigin.Scene(s.hub(),s.room(),s.id()),t,actor,null,t.kind()==1?(int)CombatAccess.field(t.actor(),"force"):-1,proof.selected().x(),proof.selected().y());if(origin.nativeSourceGroundNpcProof==proof&&origin.nativeSourceRawNpcSource!=null&&NativeSourcePhysicalNpc.sceneCurrent(origin,life)&&NativeSourcePhysicalNpc.sourceCurrent(origin)&&NativeSourcePhysicalNpc.targetCurrent(origin))prepared[0]=new NativeSourcePhysicalNpc.Commit(origin,damage,0,false,"source-firewall",null,stats,null,null);});}catch(IOException failure){throw new UncheckedIOException(failure);}finally{c.nativeSourceGroundProof=null;c.nativeSourceMagicAdmission=null;}
   }}}
  });
  var commit=prepared[0];if(commit==null)return;
  try{reaction(commit);NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{synchronized(life.admission){if(!life.admits())return;synchronized(s.hub()){synchronized(commit.target.owner()){if(NativeSourcePhysicalNpc.sceneCurrent(commit.origin,life)&&NativeSourcePhysicalNpc.sourceCurrent(commit.origin)&&NativeSourcePhysicalNpc.targetCurrent(commit.origin))NativeSourcePhysicalNpc.enqueue(commit);}}}});}finally{if(!commit.queued)NativeSourcePhysicalNpc.cancel(commit);}
 }
 static void reaction(NativeSourcePhysicalNpc.Commit p){
  var npc=new NativeAreaMaterialAction.Npc(p.target.kind(),p.target.id(),p.target.owner(),p.target.actor(),p.target.life());var gfx=NativeSourceWandReaction.gfx(npc);if(gfx==null||!BattleHitReactionSuppressCatalog.shouldSendDamageAction(gfx))return;
  var packet=Protocol.objectAction(p.target.id(),2);for(var receiver:p.members){var out=new NativeSourcePhysicalNpc.Output(p,receiver,p.viewers.get(receiver),packet,true);if(!NativeSourcePhysicalNpc.current(out))continue;
   try{var c=new NativeAreaMaterialAction.Context(receiver);c.nativeSourcePhysicalNpcOutput=out;NativeAreaMaterialAction.owned(c,()->{synchronized(receiver){if(NativeSourcePhysicalNpc.current(out))receiver.nativeSelfOriginalSend(packet,"S_ACTION Source firewall natural NPC raw hit");}});}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}
  }
 }
 private NativeSourceGroundNpc(){}
}
