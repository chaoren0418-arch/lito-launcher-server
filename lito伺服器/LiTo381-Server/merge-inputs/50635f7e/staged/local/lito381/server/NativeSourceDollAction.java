package local.lito381.server;
import java.io.*;import java.util.concurrent.atomic.AtomicReference;
/** Physical ingress captures before action-lock waits; delayed proc proofs contain no ingress Context. */
final class NativeSourceDollAction {
 static final String FIELD="nativeSourceDollAction";
 record Stamp(ClientSession caster,NativeSourceDamageDisplay.Actor source,NativeMagicDoll doll,RuntimeInventory inventory,AtomicReference<RuntimeInventory.Item> item){
  boolean current(){
   if(!source.valid(source.life())||CombatAccess.field(caster,"inventory")!=inventory)return false;
   var now=(NativeMagicDoll)CombatAccess.field(caster,"activeMagicDoll");
   return sameSummon(doll,now,System.nanoTime());
  }
  boolean before(){if(!current())return false;if(doll==null)return true;var actual=inventory.item(doll.backingItemObjectId());if(actual==null)return false;item.compareAndSet(null,actual);return item.get()==actual;}
 }
 // Inventory membership is first sealed under the original actor action lock, before proc RNG; no unlocked map read.
 static Stamp capture(ClientSession pc){var source=NativeSourceDamageDisplay.Actor.capture(pc);var inventory=(RuntimeInventory)CombatAccess.field(pc,"inventory");var doll=(NativeMagicDoll)CombatAccess.field(pc,"activeMagicDoll");return new Stamp(pc,source,doll,inventory,new AtomicReference<>());}
 static final class Action {
  final NativeAreaMaterialAction.Context c;final Stamp stamp;final NativePlayerLifetime lifetime;final long id;final ClientSession peer;final NativeSourceRawDamage.Member player;final NativeAreaMaterialAction.Npc npc;final NativeSourceNpcDamageDisplay.Proof natural;final NativeSourceControlledDisplay.Target controlled;NativeMagicDollPowers powers;boolean registered;volatile boolean closed;
  Action(NativeAreaMaterialAction.Context c,Stamp stamp,long id){
   this.c=c;this.stamp=stamp;this.id=id;lifetime=NativePlayerLifetime.of(c.hub);ClientSession target=null;for(var p:c.players)if(c.bindings.get(p).object()==id){target=p;break;}
   player=target==null?null:NativeSourcePlayerFactories.retained(target,c.bindings.get(target),c);npc=c.npcs.get(id);peer=target!=null?target:npc!=null&&npc.kind()==3?(ClientSession)npc.owner():c.pc;
   if(npc!=null&&(npc.kind()==1||npc.kind()==2))natural=new NativeSourceNpcDamageDisplay.Proof(npc.actor(),npc.owner(),NativeProcHealingEncounter.generation(npc.owner()),npc.epoch(),(int)CombatAccess.field(npc.actor(),"x"),(int)CombatAccess.field(npc.actor(),"y"),true);else natural=null;
   controlled=npc!=null&&npc.kind()==3?new NativeSourceControlledDisplay.Action(c,true,id).targets.get(id):null;
  }
  boolean before(){return !closed&&lifetime.mode==NativePlayerLifetime.Mode.OPEN&&!(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")&&c.scene()&&stamp.source.valid(stamp.source.life())&&CombatAccess.field(c.pc,"inventory")==stamp.inventory&&NativeSourceDamageDisplay.n(c.pc,"playerHp")>0&&!(boolean)CombatAccess.field(c.pc,"observer")&&(player==null||player.current(true)&&c.player(player.pc))&&(npc==null||c.npc(id)&&(natural==null||natural.current())&&(controlled==null||controlled.initial.current()));}
  void close(){closed=true;if(c.nativeSourceDollAction==this)c.nativeSourceDollAction=null;if(controlled!=null)controlled.action.close();}
 }
 static boolean sameSummon(NativeMagicDoll expected,NativeMagicDoll actual,long now){return expected==null?actual==null:actual!=null&&actual.objectId()==expected.objectId()&&actual.backingItemObjectId()==expected.backingItemObjectId()&&actual.type()==expected.type()&&actual.expiresAtNanos()==expected.expiresAtNanos()&&now<expected.expiresAtNanos();}
 static Action current(ClientSession pc){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.pc==pc&&c.nativeSourceDollAction instanceof Action a&&!a.closed?a:null;}

    static Object stamp(ClientSession clientSession) {
        return NativeSourcePhysicalNpc.stamp(clientSession);
    }
 /** A retired doll cannot cancel ordinary physical HP or grant a replacement doll's powers to the old action. */

    static NativeMagicDollPowers powers(ClientSession clientSession) {
        return NativeSourcePhysicalNpc.powers(clientSession);
    }
 static boolean valid(Object value){return !(value instanceof Stamp stamp)||stamp.before();}
 static boolean before(ClientSession pc){var a=current(pc);return a==null||a.before();}
 static void inherit(NativeAreaMaterialAction.Context child,NativeAreaMaterialAction.Context parent){if(parent!=null&&parent.pc==child.pc&&parent.nativeSourceDollAction instanceof Action a&&!a.closed)child.nativeSourceDollAction=a;}
 static void inherit(NativeAreaMaterialAction.Context child){inherit(child,NativeAreaMaterialAction.current.get());}
 static void enter(ClientSession pc,Object[] args,String original,String descriptor,boolean physical)throws IOException{
  if(!physical||!NativeSourceDollCommit.enabled(pc)||current(pc)!=null){NativeSourceControlledDisplay.enter(pc,args,original,descriptor,physical);return;}
  var stamp=capture(pc);var c=new NativeAreaMaterialAction.Context(pc);var a=new Action(c,stamp,((Protocol.AttackRequest)args[0]).targetObjectId());c.nativeSourceDollAction=a;
  NativeSelfCastAction.Task body=()->NativeAreaMaterialAction.owned(c,()->NativeSourceControlledDisplay.enter(pc,args,original,descriptor,true));
  try{
   // The original controlled path reserves all required owners and takes its own existing lock set.
   if(a.controlled!=null){body.run();return;}
   try{NativeSourceOuterPair.run(pc,a.peer,()->{synchronized(a.lifetime.admission){if(!a.before())return;try{NativeLightAction.commitPair(pc,a.peer,()->{if(!a.before())return;NativeLightAction.finishScoped((published,failure)->a.close());a.registered=true;body.run();});}catch(IOException error){throw new UncheckedIOException(error);}}});}catch(UncheckedIOException error){throw error.getCause();}
  }finally{if(!a.registered)a.close();}
 }
 private NativeSourceDollAction(){}

    static NativeMagicDollPowers nativeSourcePhysicalOriginalPowers(ClientSession clientSession) {
        Action action = NativeSourceDollAction.current(clientSession);
        if (action == null) {
            return NativeSourceDollPassive.powers(clientSession);
        }
        if (action.powers == null) {
            action.powers = action.stamp.doll != null && action.stamp.before() ? NativeMagicDollPowers.from(action.stamp.doll.type()) : NativeMagicDollPowers.empty();
        }
        return action.powers;
    }

    static Object nativeSourcePhysicalOriginalStamp(ClientSession clientSession) {
        Action action = NativeSourceDollAction.current(clientSession);
        return action == null ? null : action.stamp;
    }
}
