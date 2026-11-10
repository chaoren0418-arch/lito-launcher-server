package local.lito381.server;
import java.io.*;import java.util.*;
/** Preserve original defense Calc/RNG and accepted publication; add detached original-summon provenance. */
final class NativeSourceDollDefense {
 @AsRecord(components = {"pc", "binding"}, finalFields = {"nativeSourceDollDefenseAudienceProof"})
    static final class Receiver {
        private final ClientSession pc;
        private final NativePlayerCommit.Binding binding;
        transient Object nativeSourceDollDefenseAudienceProof;

        Receiver(ClientSession pc, NativePlayerCommit.Binding binding) {
            this.pc = pc;
            this.binding = binding;
        }

        Receiver(ClientSession pc, NativePlayerCommit.Binding binding, Object proof) {
            this(pc, binding);
            this.nativeSourceDollDefenseAudienceProof = proof;
        }

        public ClientSession pc() {
            return this.pc;
        }

        public NativePlayerCommit.Binding binding() {
            return this.binding;
        }
    }
 /** Owned by the existing synchronous Context only; never put into a queued HP event. */
 @AsRecord(components = {"dolls", "receivers"})
    static final class Snapshot {
        private final Map<ClientSession,NativeSourceDollAction.Stamp> dolls;
        private final List<Receiver> receivers;
        final Object nativeSourceDollPassivePowers;

        Snapshot(Map<ClientSession,NativeSourceDollAction.Stamp> dolls, List<Receiver> receivers) {
            this.dolls = dolls;
            this.receivers = receivers;
            this.nativeSourceDollPassivePowers = NativeSourceDollPassive.selections(this);
        }

        public Map<ClientSession,NativeSourceDollAction.Stamp> dolls() {
            return this.dolls;
        }

        public List<Receiver> receivers() {
            return this.receivers;
        }
    }
 static final class Proof {
  final NativeSourceDollAction.Stamp stamp;final NativePlayerCommit.Binding binding;final List<Receiver> receivers;final NativePlayerLifetime lifetime;final GameHub hub;
  volatile Object acceptedLife;
  Proof(NativeSourceDollAction.Stamp stamp,List<Receiver> receivers){this.stamp=stamp;binding=stamp.source().binding();acceptedLife=stamp.source().life();this.receivers=List.copyOf(receivers);hub=(GameHub)CombatAccess.field(stamp.caster(),"hub");lifetime=NativePlayerLifetime.of(hub);}
  boolean current(){var pc=stamp.caster();return lifetime.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")&&NativeAcceptedFreezeMutation.roleFrame(binding,pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&NativeSourceControlledDisplay.rawLife(pc)==acceptedLife&&CombatAccess.field(pc,"inventory")==stamp.inventory()&&NativeSourceDamageDisplay.n(pc,"positionX")==stamp.source().x()&&NativeSourceDamageDisplay.n(pc,"positionY")==stamp.source().y()&&NativeSourceDollAction.sameSummon(stamp.doll(),(NativeMagicDoll)CombatAccess.field(pc,"activeMagicDoll"),System.nanoTime());}
 }
 record Output(Proof proof,Receiver receiver,byte[] packet,int gfx){}
 static Object snapshot(NativeAreaMaterialAction.Context context){
  if(!NativeSourceDollCommit.enabled(context.pc))return null;
  var parent=NativeAreaMaterialAction.current.get();if(parent!=null&&parent.pc==context.pc&&parent.nativeSourceDollDefenseSnapshot instanceof Snapshot previous)return previous;
  var dolls=new IdentityHashMap<ClientSession,NativeSourceDollAction.Stamp>();var receivers=new ArrayList<Receiver>();
  for(var pc:context.players){var binding=context.bindings.get(pc);if(binding==null)continue;var stamp=NativeSourceDollAction.capture(pc);if(binding.equals(stamp.source().binding()))dolls.put(pc,stamp);receivers.add(NativeSourceDollDefenseAudience.receiver(pc,binding,context));}
  return new Snapshot(Collections.unmodifiableMap(dolls),List.copyOf(receivers));
 }
 static Snapshot current(){var context=NativeAreaMaterialAction.current.get();return context!=null&&context.nativeSourceDollDefenseSnapshot instanceof Snapshot value?value:null;}
 /** The original drawDefense monitor/loop remains. Inventory sealing is allowed only in its actor action lock. */
 static Object dollField(Object actor,String field){
  if(!(actor instanceof ClientSession pc)||!field.equals("activeMagicDoll")||!NativeSourceDollCommit.enabled(pc)||!pc.nativePlayerActionLock().isHeldByCurrentThread())return CombatAccess.field(actor,field);
  var snapshot=current();if(snapshot==null)return CombatAccess.field(actor,field);
  var stamp=snapshot.dolls.get(pc);return stamp!=null&&stamp.before()?stamp.doll():null;
 }
 static Object proof(NativePlayerCommit.DefenseVisual visual,ClientSession pc){
  if(!NativeSourceDollCommit.enabled(pc)||!pc.nativePlayerActionLock().isHeldByCurrentThread())return null;
  var snapshot=current();if(snapshot==null)return null;var stamp=snapshot.dolls.get(pc);
  if(stamp==null||!stamp.before()||!NativeSourceDollAction.sameSummon(stamp.doll(),visual.doll(),System.nanoTime()))return Boolean.FALSE;
  return new Proof(stamp,snapshot.receivers);
 }
 static Object recipients(Object actor,String method,String descriptor,Object[] args,NativePlayerCommit.DefenseVisual visual){
  if(visual.nativeSourceDollDefenseProof==Boolean.FALSE)return List.of();
  if(!(visual.nativeSourceDollDefenseProof instanceof Proof proof))return CombatAccess.call(actor,method,descriptor,args);
  return proof.receivers.stream().map(Receiver::pc).toList();
 }
 /** At the existing actual-HP accepted-state boundary, never after a completion wait. */
 static void accepted(NativePlayerCommit.Frame frame){
  if(!frame.accepted||!frame.fatal||frame.event.world())return;
  for(var visual:frame.event.defense())if(visual.nativeSourceDollDefenseProof instanceof Proof proof&&proof.stamp.caster()==frame.pc&&proof.binding.equals(frame.event.target()))proof.acceptedLife=NativeSourceControlledDisplay.rawLife(frame.pc);
 }
 static boolean current(Output output){if(!NativeSourceDollDefenseAudience.current(output))return false;
  var proof=output.proof;var receiver=output.receiver.pc;
  if(!proof.current()||!NativeAcceptedFreezeMutation.roleFrame(output.receiver.binding,receiver)||(boolean)CombatAccess.field(receiver,"sourceItemDisconnected"))return false;
  int x=proof.stamp.source().x(),y=proof.stamp.source().y();
  if(output.gfx<=0){var doll=(NativeMagicDoll)CombatAccess.field(proof.stamp.caster(),"activeMagicDoll");if(doll==null)return false;x=doll.x();y=doll.y();}
  int rx=NativeSourceDamageDisplay.n(receiver,"positionX"),ry=NativeSourceDamageDisplay.n(receiver,"positionY");
  return visible(output.gfx,receiver.nativeSourceShowProcSpr,rx,ry,x,y);
 }
 static boolean visible(int gfx,boolean preference,int rx,int ry,int x,int y){return preference&&(gfx>0?NativeSourceDollSkills.procVisible(true,rx,ry,x,y):BattleSpellCatalog.isInNativeScreen(rx,ry,x,y));}
 static void send(ClientSession receiver,byte[] packet,String note,NativePlayerCommit.DefenseVisual visual,ClientSession victim){
  if(visual.nativeSourceDollDefenseProof==Boolean.FALSE)return;
  if(!(visual.nativeSourceDollDefenseProof instanceof Proof proof)){NativePlayerCommit.safeSend(receiver,packet,note);return;}
  var frame=NativePlayerCommit.frame();if(frame==null||!frame.accepted||frame.pc!=victim||frame.event.world()||!NativePlayerCommit.validFrame(victim))return;
  Receiver selected=null;for(var candidate:proof.receivers)if(candidate.pc==receiver){selected=candidate;break;}if(selected==null)return;
  var output=new Output(proof,selected,packet.clone(),visual.gfx());Object previous=frame.nativeSourceDollDefenseOutput;
  try{frame.nativeSourceDollDefenseOutput=output;synchronized(receiver){if(current(output))receiver.nativeSelfOriginalSend(output.packet,note);}}
  catch(IOException|RuntimeException failure){NativeSourceDollSkills.failed(victim);}
  finally{frame.nativeSourceDollDefenseOutput=previous;}
 }
 /** Final frame read-only check; does not acquire actor/Hub/status/inventory locks. */

 static boolean allowFrame(ClientSession receiver,byte[] packet){if(!NativeSourceDollDefenseAudience.allowFrame(receiver,packet))return false;var frame=NativePlayerCommit.frame();if(frame==null||!(frame.nativeSourceDollDefenseOutput instanceof Output output))return true;return frame.accepted&&frame.pc==output.proof.stamp.caster()&&NativePlayerCommit.validFrame(frame.pc)&&output.receiver.pc==receiver&&current(output);}
 private NativeSourceDollDefense(){}
}
