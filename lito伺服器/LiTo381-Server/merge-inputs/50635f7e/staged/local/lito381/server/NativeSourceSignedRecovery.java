package local.lito381.server;
import java.io.*;import java.util.*;
/** BATTLE signed recovery joins the existing player HP lane with target-only queued authority. */
final class NativeSourceSignedRecovery {
 record Intent(NativeSourceRawDamage.Member target,NativeSourceRawDamage.Member source,GameHub hub,NativePlayerLifetime lifetime){
  boolean before(){return target!=null&&source!=null&&lifetime.mode==NativePlayerLifetime.Mode.OPEN&&!(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")&&target.current(false)&&source.current(false)&&target.binding.phase().equals("BATTLE")&&target.binding.room()==source.binding.room()&&CombatAccess.field(target.pc,"hub")==hub&&CombatAccess.field(source.pc,"hub")==hub&&NativeSourceDamageDisplay.n(target.pc,"playerHp")>0;}
  NativeSourceRawDamage.Guard queued(){return new NativeSourceRawDamage.Guard(target,null,false,lifetime);}
 }
 static Intent capture(ClientSession pc,ClientSession caster){if(caster==null)return null;var c=NativeAreaMaterialAction.current.get();var targetBinding=c==null?NativePlayerCommit.Binding.capture(pc):c.bindings.get(pc);var sourceBinding=c==null?NativePlayerCommit.Binding.capture(caster):c.bindings.get(caster);if(targetBinding==null||sourceBinding==null)return null;var hub=(GameHub)CombatAccess.field(pc,"hub");var target=member(pc,targetBinding,c);var source=pc==caster?target:member(caster,sourceBinding,c);return target==null||source==null?null:new Intent(target,source,hub,NativePlayerLifetime.of(hub));}
 static NativeSourceRawDamage.Member member(ClientSession pc,NativePlayerCommit.Binding binding,NativeAreaMaterialAction.Context c){
  if(NativeSourceHealingCast.scoped(c))return NativeSourceHealingCast.member(pc,binding,c);
  if(c!=null){if(c.nativeSourceRawAction instanceof NativeSourceRawDamage.Action a){var old=a.members.get(pc);if(old!=null)return old.binding.equals(binding)?old:null;}
   NativeSourceDamageDisplay.Actor original=null;
   if(c.nativeSourceDollAction instanceof NativeSourceDollAction.Action a&&a.stamp.caster()==pc)original=a.stamp.source();
   else if(c.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot snapshot&&snapshot.source().pc()==pc)original=snapshot.source();
   if(original!=null)return original.binding().equals(binding)?NativeSourceAcceptedPlayers.fromActor(pc,binding,original,original.life()):null;
  }return NativeSourceFirstPlayers.member(pc,binding,c);
 }
 static void receive(ClientSession pc,int amount,ClientSession caster,boolean magic,boolean world){
  if(amount>=0||world||!CombatRules.loaded()){CombatPlayerRecovery.nativeSourceSignedOriginalReceive(pc,amount,caster,magic,world);return;}
  var intent=capture(pc,caster);if(intent==null||!intent.before())return;
  NativeSourcePlayerActionLock.runPairOrdered(pc,caster,()->{synchronized(intent.lifetime().admission){if(!intent.before())return;try{NativeLightAction.commitPair(caster,pc,()->{if(!intent.before())return;var context=new NativeAreaMaterialAction.Context(pc);if(!context.caster.equals(intent.target().binding)||!intent.before())return;var target=intent.target();var actor=new NativeSourceDamageDisplay.Actor(pc,target.binding,target.life,target.x,target.y);var plan=new NativeSourceDamageDisplay.Plan(actor,actor,List.of());plan.nativeRawGuard=intent.queued();NativeAreaMaterialAction.owned(context,()->NativeSourceDamageDisplay.damage(pc,amount,caster,magic,false,0,plan));});}catch(IOException failure){throw new UncheckedIOException(failure);}}});
 }
 private NativeSourceSignedRecovery(){}
}
