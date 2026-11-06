package local.lito381.server;
import java.io.*;import java.util.*;import java.util.function.IntUnaryOperator;
/** Explicit native null-attacker damage, separate from unknown or retired NPC provenance. */
final class NativeSourceFatalityDamage {
 static final class Root {
  final NativeAreaMaterialAction.Context context;final NativeSourceRawDamage.Member actor;final NativePlayerLifetime lifetime;final Map<ClientSession,NativeSourceRawDamage.Member> targets=new IdentityHashMap<>();
  Root(NativeAreaMaterialAction.Context c,NativeSourceRawDamage.Member original){context=c;lifetime=NativePlayerLifetime.of(c.hub);actor=original;for(var pc:c.players){var binding=c.bindings.get(pc);if(binding!=null)targets.put(pc,pc==c.pc?actor:NativeSourcePlayerFactories.retained(pc,binding,NativeSourceControlledDisplay.rawLife(pc),c));}targets.put(c.pc,actor);}
  boolean before(){return lifetime.mode==NativePlayerLifetime.Mode.OPEN&&!(boolean)CombatAccess.field(context.hub,"nativeStaticMotionClosed")&&context.caster.equals(actor.binding)&&actor.current(true)&&(actor.dead?NativeSourceDamageDisplay.n(actor.pc,"playerHp")==0:NativeSourceDamageDisplay.n(actor.pc,"playerHp")>0);}
  boolean before(ClientSession pc){var member=targets.get(pc);return before()&&member!=null&&member.current(true)&&member.binding.phase().equals("BATTLE")&&member.binding.room()==actor.binding.room()&&NativeSourceDamageDisplay.n(pc,"playerHp")>0;}
 }
 record Proof(NativeSourceRawDamage.Member target,NativePlayerLifetime lifetime){}
 record Output(NativeSourceRawDamage.Member root,NativeSourceRawDamage.Member subject,NativePlayerLifetime lifetime,GameHub hub,long room,ClientSession pc,NativePlayerCommit.Binding binding,byte[] packet,String note){}
 static Object marker(){var c=NativeAreaMaterialAction.current.get();return c==null?null:c.nativeSourceCubeStatusProof;}
 static Root root(){return marker()instanceof Root root?root:null;}
 static boolean enabled(ClientSession pc){return NativeSourceDollCommit.enabled(pc);}

 static void enter(ClientSession pc,BattleDeathmatchFatalityCatalog.Effect effect,int radius)throws IOException{
  if(!enabled(pc)){if(radius<0)pc.nativeSourceFatalityOriginalSingle(effect);else pc.nativeSourceFatalityOriginalArea(effect,radius);return;}
  NativeSourceRawDamage.Member ingress;var binding=(ingress=NativeSourceAcceptedPlayers.ingress(pc)).binding;var life=NativeSourceControlledDisplay.rawLife(pc);var actor=NativeSourceAcceptedPlayers.entryMember(pc,binding,life,ingress);var c=new NativeAreaMaterialAction.Context(pc);var root=new Root(c,actor);if(!root.before())return;c.nativeSourceCubeStatusProof=root;
  try{NativeAreaMaterialAction.owned(c,()->{if(root.before()){if(radius<0)pc.nativeSourceFatalityOriginalSingle(effect);else pc.nativeSourceFatalityOriginalArea(effect,radius);}});}finally{c.nativeSourceCubeStatusProof=null;}
 }
 static List<ClientSession> members(GameHub hub,long room){var root=root();return root==null?hub.roomCombatants(room):root.context.hub==hub&&root.actor.binding.room()==room&&root.before()?root.context.players:List.of();}
 static int roll(BattleDeathmatchFatalityCatalog.Effect effect,IntUnaryOperator random,ClientSession owner,ClientSession target)throws IOException{
  var root=root();if(root==null)return BattleDeathmatchFatalityCatalog.rollRawDamage(effect,random);if(root.context.pc!=owner)return 0;
  try{NativeSourcePlayerActionLock.runPairOrdered(owner,target,()->{synchronized(root.lifetime.admission){if(!root.before(target))return;try{NativeLightAction.commitSelf(target,()->{if(!root.before(target))return;int damage=BattleDeathmatchFatalityCatalog.rollRawDamage(effect,random);var member=root.targets.get(target);var actor=new NativeSourceDamageDisplay.Actor(target,member.binding,member.life,member.x,member.y);var plan=new NativeSourceDamageDisplay.Plan(actor,actor,List.of());plan.nativeRawGuard=new NativeSourceRawDamage.Guard(member,null,false,root.lifetime);plan.nativeSourceFatalityProof=new Proof(member,root.lifetime);NativeSourceDamageDisplay.damage(target,damage,null,false,false,0,plan);});}catch(IOException error){throw new UncheckedIOException(error);}}});}catch(UncheckedIOException error){throw error.getCause();}return 0;
 }
 /** Scoped HP was submitted once under its original pair; the old post-roll sink remains for native fallback. */
 static void completed(ClientSession target,int value,ClientSession caster,ClientSession owner)throws IOException{var root=root();if(root==null){target.nativeSourceFatalityOriginalDamage(value,caster);return;}if(root.context.pc!=owner||caster!=null||value!=0)throw new IllegalStateException("invalid scoped Fatality sink");}
 static boolean noAttacker(ClientSession pc,NativePlayerCommit.CalcTicket ticket){
  if(ticket==null||ticket.caster!=null||ticket.attacker!=null||ticket.sourceDisplay==null||!(ticket.sourceDisplay.nativeSourceFatalityProof instanceof Proof proof))return false;var guard=ticket.sourceDisplay.nativeRawGuard;
  return proof.target.pc==pc&&ticket.target!=null&&ticket.target.equals(proof.target.binding)&&guard!=null&&guard.target()==proof.target&&guard.source()==null&&guard.lifetime()==proof.lifetime&&proof.target.binding.phase().equals("BATTLE");
 }
 static boolean skipAttackerCalc(ClientSession pc){return CombatRules.loaded()&&noAttacker(pc,NativePlayerCommit.nativeSourceFatalityCalculation());}
 static boolean current(Output out){return out.lifetime.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(out.hub,"nativeStaticMotionClosed")&&out.root.current(true)&&out.subject.current(true)&&NativeAcceptedFreezeMutation.roleFrame(out.binding,out.pc)&&!(boolean)CombatAccess.field(out.pc,"sourceItemDisconnected")&&out.binding.phase().equals("BATTLE")&&out.binding.room()==out.room&&CombatAccess.field(out.pc,"hub")==out.hub;}
 static void send(ClientSession receiver,byte[] packet,String note,ClientSession subject,ClientSession owner)throws IOException{
  var root=root();if(root==null){receiver.nativeSourceFatalityOriginalSend(packet,note);return;}var member=root.targets.get(subject);var binding=root.context.bindings.get(receiver);if(root.context.pc!=owner||member==null||binding==null)return;var out=new Output(root.actor,member,root.lifetime,root.context.hub,root.actor.binding.room(),receiver,binding,packet.clone(),note);if(!current(out))return;
  NativeLightAction.deferScoped(()->{if(!current(out))return;var previous=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.remove();try{var c=new NativeAreaMaterialAction.Context(receiver);c.nativeSourceCubeStatusProof=out;try{NativeAreaMaterialAction.owned(c,()->{synchronized(receiver){if(current(out))receiver.nativeSelfOriginalSend(out.packet,out.note);}});}finally{c.nativeSourceCubeStatusProof=null;}}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(out.lifetime);}finally{if(previous==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(previous);}});
 }
 static boolean packet(byte[] original,byte[] rewritten){return original!=null&&rewritten!=null&&original.length==7&&rewritten.length==7&&Arrays.equals(Arrays.copyOf(original,5),Arrays.copyOf(rewritten,5));}
 static boolean allowFrame(ClientSession pc,byte[]packet){return !(marker()instanceof Output out)||out.pc==pc&&current(out)&&packet(out.packet,packet);}
 private NativeSourceFatalityDamage(){}
}
