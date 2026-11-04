package local.lito381.server;
import java.io.*;import java.util.*;
/** Source PC cube HP consumer. Ref kind5 is the actual effect NPC, never its player creator. */
final class NativeSourceCubePlayer {
 record Effect(NativeSourceCubeOrigin.Entity entity,NativePlayerLifetime lifetime){public String toString(){return "CubeEffectNpc[id="+entity.objectId()+"]";}}
 static final class Proof {
  final NativeSourceCubeOrigin.Lease lease;final NativeSourceCubeOrigin.Target target;final NativePlayerLifetime lifetime;final Effect actor;final Map<ClientSession,NativePlayerCommit.Binding> receivers;
  volatile Object acceptedLife;volatile boolean dead;
  Proof(NativeSourceCubeOrigin.Lease lease,NativePlayerLifetime lifetime,Map<ClientSession,NativePlayerCommit.Binding> receivers){this.lease=lease;target=lease.target();this.lifetime=lifetime;actor=new Effect(lease.effect(),lifetime);acceptedLife=target.life();this.receivers=Collections.unmodifiableMap(new IdentityHashMap<>(receivers));}
  boolean current(){return effectCurrent(lease,lifetime)&&targetCurrent(target,acceptedLife,lease.effect().scene())&&(dead?number(target.pc(),"playerHp")==0:number(target.pc(),"playerHp")>0);}
 }
 record Reaction(NativeSourceCubeOrigin.Lease lease,NativePlayerLifetime lifetime,ClientSession receiver,NativePlayerCommit.Binding binding,byte[] packet){}
 static int number(ClientSession pc,String field){return NativeSourceDamageDisplay.n(pc,field);}
 static boolean effect(NativeCombatAttacker.Ref ref){return ref!=null&&ref.kind()==5;}
 static boolean flags(boolean world,boolean earth,boolean suppress,boolean playerCaster,boolean emptyPlan){return !world&&!earth&&suppress&&!playerCaster&&emptyPlan;}
 static boolean sameTarget(NativeSourceCubeOrigin.Target a,NativeSourceCubeOrigin.Target b){return a!=null&&b!=null&&a.id()==b.id()&&a.kind()==b.kind()&&a.pc()==b.pc()&&a.actor()==b.actor()&&a.owner()==b.owner()&&Objects.equals(a.binding(),b.binding())&&a.life()==b.life()&&a.generation()==b.generation();}
 /** Original synchronized advance owns the map; stale PC leases must not block a new role with the same id. */
 static void retire(Object engine){
  NativeSourceCubeNpcLease.retire(engine);
  if(!CombatRules.loaded()||!(NativeSourceCubeOrigin.scene(engine)instanceof NativeSourceCubeOrigin.Scene scene)||!Thread.holdsLock(scene.hub())||!(CombatAccess.field(engine,"nativeSourceCubeScan")instanceof NativeSourceCubeOrigin.Scan scan)||scan.epoch()!=NativeSourceCubeOrigin.epoch(engine))return;
  var iterator=((Map<?,?>)CombatAccess.field(engine,"statuses")).values().iterator();while(iterator.hasNext()){var value=iterator.next();if(!String.valueOf(CombatAccess.field(value,"targetKind")).equals("PLAYER"))continue;var origin=CombatAccess.field(value,"nativeSourceCubeOrigin");if(!(origin instanceof NativeSourceCubeOrigin.Lease lease)||lease.target()==null||!sameTarget(lease.target(),scan.targets().get(lease.target().id()))||!targetCurrent(lease.target(),lease.target().life(),scene))iterator.remove();}
 }
 static boolean effectCurrent(NativeSourceCubeOrigin.Lease lease,NativePlayerLifetime lifetime){return entityCurrent(lease.effect(),lifetime);}
 static boolean entityCurrent(NativeSourceCubeOrigin.Entity e,NativePlayerLifetime lifetime){var s=e.scene();return s!=null&&lifetime.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(s.hub(),"nativeStaticMotionClosed")&&NativeSourceCubeOrigin.epoch(e.engine())==e.epoch()&&NativeSourceCubeOrigin.scene(e.engine())==s;}
 /** No pose check: the original eight-second status follows its actor out of the cube radius. */
 static boolean targetCurrent(NativeSourceCubeOrigin.Target t,Object life,NativeSourceCubeOrigin.Scene s){var pc=t.pc();return t.kind()==0&&pc!=null&&t.actor()==pc&&t.owner()==s.room()&&CombatAccess.field(pc,"hub")==s.hub()&&NativeAcceptedFreezeMutation.roleFrame(t.binding(),pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&!(boolean)CombatAccess.field(pc,"observer")&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&(long)CombatAccess.field(pc,"currentRoomId")==s.id()&&NativeSourceControlledDisplay.rawLife(pc)==life;}
 /** Called only while the original Hub/engine monitors are held, before HP submission. */
 static boolean installed(NativeSourceCubeOrigin.Lease lease,long now){
  if(now>=lease.expires())return false;var e=lease.effect();if(NativeSourceCubeOrigin.epoch(e.engine())!=e.epoch())return false;
  for(var value:((Map<?,?>)CombatAccess.field(e.engine(),"statuses")).values())if(value==lease.actual())return CombatAccess.field(value,"nativeSourceCubeOrigin")==lease;
  return false;
 }
 static boolean matches(NativeCombatAttacker.Ref ref,Proof proof){var e=proof.lease.effect();var s=e.scene();return effect(ref)&&ref.owner()==proof.actor&&ref.id()==e.objectId()&&ref.master()==null&&ref.epoch()==0&&s!=null&&ref.room()==s.id()&&ref.map()==-1&&ref.instance()==-1;}
 static NativeCombatAttacker.Ref reference(Proof proof){var e=proof.lease.effect();if(e.scene()==null)throw new IllegalArgumentException("unbound cube effect scene");return new NativeCombatAttacker.Ref(5,e.objectId(),proof.actor,0,null,e.scene().id(),-1,-1);}
 static Proof proof(NativePlayerCommit.Event event){
  var ref=event.attacker();var plan=event.sourceDisplay();if(!effect(ref)||plan==null||!(plan.nativeSourceCubePlayerProof instanceof Proof p)||!matches(ref,p))return null;
  return event.target()!=null&&event.target().equals(p.target.binding())&&flags(event.world(),event.earth(),event.suppress(),event.caster()!=null,plan.effects.isEmpty())?p:null;
 }
 static boolean beforeEvent(ClientSession pc,NativePlayerCommit.Event event){if(!effect(event.attacker()))return true;var p=proof(event);return p!=null&&p.target.pc()==pc&&p.current();}
 static boolean validFrame(ClientSession pc){var frame=NativePlayerCommit.frame();if(frame==null||!effect(frame.event.attacker()))return true;var p=proof(frame.event);return p!=null&&frame.pc==pc&&p.target.pc()==pc&&p.current();}
 static void accepted(NativePlayerCommit.Frame frame){if(!frame.accepted||!frame.fatal)return;var p=proof(frame.event);if(p!=null&&p.target.pc()==frame.pc){p.acceptedLife=NativeSourceControlledDisplay.rawLife(frame.pc);p.dead=true;}}
 static boolean alive(ClientSession pc,NativeCombatAttacker.Ref ref){if(pc==null||!effect(ref)||!(ref.owner()instanceof Effect actor)||!entityCurrent(actor.entity,actor.lifetime))return false;var e=actor.entity;return ref.id()==e.objectId()&&ref.master()==null&&ref.epoch()==0&&ref.room()==e.scene().id()&&ref.map()==-1&&ref.instance()==-1&&CombatAccess.field(pc,"hub")==e.scene().hub()&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&(long)CombatAccess.field(pc,"currentRoomId")==e.scene().id();}
 static boolean damage(ClientSession pc,long room,int map,int instance,ClientSession ignoredCreator,BattleCubeIgnitionEffects.Event event)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc))return false;
  if(event!=null&&event.kind()==BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE&&event.targetKind()==BattleCubeIgnitionEffects.ActorKind.PLAYER&&event.nativeSourceCubeOrigin instanceof NativeSourceCubeOrigin.Event original&&event.cube()==original.lease().effect().cube()&&original.lease().target()!=null&&event.targetObjectId()==original.lease().target().id())damage(pc,room,map,instance,original.lease(),true);
  return true;
 }
 static boolean damage(ClientSession pc,long room,int map,int instance,ClientSession ignoredCreator,BattleCubeHarmonyEffects.Event event)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc))return false;
  if(event!=null&&event.kind()==BattleCubeHarmonyEffects.EventKind.HP_DAMAGE&&event.targetKind()==BattleCubeHarmonyEffects.ActorKind.PLAYER&&event.nativeSourceCubeOrigin instanceof NativeSourceCubeOrigin.Event original&&event.cube()==original.lease().effect().cube()&&original.lease().target()!=null&&event.targetObjectId()==original.lease().target().id())damage(pc,room,map,instance,original.lease(),false);
  return true;
 }
 static void damage(ClientSession pc,long room,int map,int instance,NativeSourceCubeOrigin.Lease lease,boolean ignition)throws IOException{
  var e=lease.effect();var s=e.scene();var t=lease.target();if(s==null||t==null||t.pc()!=pc||s.id()!=room||map!=-1||instance!=-1||e.npcId()!=(ignition?80149:80152)||!targetCurrent(t,t.life(),s)||NativeCombatAttacker.blocked())return;
  var owner=NativePlayerLifetime.of(s.hub());var c=new NativeAreaMaterialAction.Context(pc);if(!c.caster.equals(t.binding()))return;var p=new Proof(lease,owner,c.bindings);if(!p.current())return;
  try{NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{synchronized(owner.admission){if(owner.mode!=NativePlayerLifetime.Mode.OPEN||!p.current())return;try{NativeLightAction.commitSelf(pc,()->NativeAreaMaterialAction.owned(c,()->{
   synchronized(s.hub()){if(!p.current()||((Map<?,?>)CombatAccess.field(s.hub(),"rooms")).get(s.id())!=s.room())return;synchronized(e.engine()){if(!installed(lease,System.nanoTime()))return;}}
   if(!p.current()||ignition&&CombatPlayerMagic.protectedZero(pc,System.nanoTime()))return;
   if(ignition)reaction(c,p);
   var actor=new NativeSourceDamageDisplay.Actor(pc,t.binding(),t.life(),number(pc,"positionX"),number(pc,"positionY"));var plan=new NativeSourceDamageDisplay.Plan(actor,actor,List.of());
   plan.nativeRawGuard=new NativeSourceRawDamage.Guard(new NativeSourceRawDamage.Member(pc,t.binding(),t.life()),null,false,owner);plan.nativeSourceCubePlayerProof=p;
   var ingress=NativeCombatAttacker.enterKnown(reference(p));try{NativeSourceDamageDisplay.damage(pc,ignition?10:25,null,false,true,0,plan);}finally{NativeCombatAttacker.leave(ingress);}
  }));}catch(IOException error){throw new UncheckedIOException(error);}}});}catch(UncheckedIOException error){throw error.getCause();}
 }
 static void reaction(NativeAreaMaterialAction.Context c,Proof proof){
  var pc=proof.target.pc();byte[] packet=Protocol.objectAction(proof.target.id(),2);
  for(var receiver:c.players){var binding=c.bindings.get(receiver);if(binding==null)continue;var output=new Reaction(proof.lease,proof.lifetime,receiver,binding,packet.clone());
   if(!reactionCurrent(output))continue;
   NativeLightAction.deferScoped(()->{Object previous=c.nativeSourceCubePlayerOutput;try{c.nativeSourceCubePlayerOutput=output;synchronized(receiver){if(reactionCurrent(output))NativeAreaMaterialAction.owned(c,()->receiver.nativeSelfOriginalSend(output.packet,"S_ACTION Source cube ignition raw damage"));}}catch(IOException|RuntimeException failure){NativeSourceDollSkills.failed(pc);}finally{c.nativeSourceCubePlayerOutput=previous;}});
  }
 }
 /** Initial pre-HP reaction life stays distinct from the mutable accepted-death proof. */
 static boolean reactionCurrent(Reaction r){var t=r.lease.target();var pc=t.pc();if(!effectCurrent(r.lease,r.lifetime)||!targetCurrent(t,t.life(),r.lease.effect().scene())||!NativeAcceptedFreezeMutation.roleFrame(r.binding,r.receiver)||(boolean)CombatAccess.field(r.receiver,"sourceItemDisconnected"))return false;if(r.receiver!=pc&&(boolean)CombatAccess.field(pc,"invisible"))return false;return BattleSpellCatalog.isInNativeScreen(number(r.receiver,"positionX"),number(r.receiver,"positionY"),number(pc,"positionX"),number(pc,"positionY"));}
 static void inherit(NativeAreaMaterialAction.Context child,NativeAreaMaterialAction.Context parent){if(parent!=null)child.nativeSourceCubePlayerOutput=parent.nativeSourceCubePlayerOutput;}
 static void inherit(NativeAreaMaterialAction.Context child){inherit(child,NativeAreaMaterialAction.current.get());}
 /** Actual native Frame gate: identity/volatile reads only; no actor/Hub/status/inventory locks. */
 static boolean allowFrame(ClientSession receiver,byte[] packet){
  var c=NativeAreaMaterialAction.current.get();if(c!=null&&c.nativeSourceCubePlayerOutput instanceof Reaction r&&(!reactionCurrent(r)||r.receiver!=receiver))return false;
  var frame=NativePlayerCommit.frame();if(frame==null||!effect(frame.event.attacker()))return true;var p=proof(frame.event);var binding=p==null?null:p.receivers.get(receiver);return p!=null&&validFrame(frame.pc)&&binding!=null&&NativeAcceptedFreezeMutation.roleFrame(binding,receiver)&&!(boolean)CombatAccess.field(receiver,"sourceItemDisconnected");
 }
 private NativeSourceCubePlayer(){}
}
