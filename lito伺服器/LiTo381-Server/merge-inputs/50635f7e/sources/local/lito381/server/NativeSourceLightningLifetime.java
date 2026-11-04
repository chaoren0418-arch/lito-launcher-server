package local.lito381.server;
import java.io.*;import java.util.*;
/** Per-invocation primary capture before the existing sorted area action boundary. */
final class NativeSourceLightningLifetime {
 record Output(ClientSession receiver,NativePlayerCommit.Binding binding,byte[] packet,String note,Object life){}
 static final class Aim {
  final NativeAreaMaterialAction.Context c;final ClientSession player,owner;final NativeAreaMaterialAction.Npc target;final BattleCompanion pet;final Object life;final NativePlayerLifetime lifetime;final int cx,cy,x,y;boolean registered;volatile boolean closed;volatile Output outbound;
  Aim(NativeAreaMaterialAction.Context c,ClientSession player,NativeAreaMaterialAction.Npc target){this.c=c;this.player=player;this.target=target;pet=target==null?null:(BattleCompanion)target.actor();owner=player!=null?player:(ClientSession)target.owner();lifetime=NativePlayerLifetime.of(c.hub);cx=number(c.pc,"positionX");cy=number(c.pc,"positionY");x=player!=null?number(player,"positionX"):pet.snapshot().x();y=player!=null?number(player,"positionY"):pet.snapshot().y();life=token();}
  Object actor(){return player!=null?player:pet;}
  Object token(){return NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(actor(),"timedStatus"),true);}
  boolean valid(){return !closed&&lifetime.mode==NativePlayerLifetime.Mode.OPEN&&!(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")&&c.scene()&&!(boolean)CombatAccess.field(owner,"sourceItemDisconnected")&&(player!=null?c.player(player):c.npc(target.id()))&&number(c.pc,"positionX")==cx&&number(c.pc,"positionY")==cy&&number(actor(),player!=null?"positionX":"x")==x&&number(actor(),player!=null?"positionY":"y")==y&&token()==life;}
  void close(){closed=true;outbound=null;if(c.nativeSourceLightningAim==this)c.nativeSourceLightningAim=null;}
 }
 static int number(Object p,String n){return(int)CombatAccess.field(p,n);}
 static Aim aim(){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceLightningAim instanceof Aim a?a:null;}
 static void battle(ClientSession pc,Protocol.AttackSpellRequest request,BattleSpellCatalog.DirectionalLineSpell spell)throws IOException{
  if(spell.skillId()!=17||NativeSkillTargetMetadata.size()==0){pc.nativeSourceLightningOriginalBody(request,spell);return;}var c=new NativeAreaMaterialAction.Context(pc);if(!c.phase.equals("BATTLE"))return;ClientSession target=null;for(var p:c.players)if(c.bindings.get(p).object()==request.targetObjectId()){target=p;break;}var npc=c.npcs.get(request.targetObjectId());if(target==null&&(npc==null||npc.kind()!=3)){pc.nativeSourceLightningOriginalBody(request,spell);return;}if(target!=null)npc=null;var a=new Aim(c,target,npc);
  try(var scope=new NativeCompanionTargets.Scope(c)){if(!scope.valid)return;CombatAccess.set(c,"nativeCompanionTargetScope",scope);c.nativeSourceLightningAim=a;var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(c);
   try{NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(pc,17,()->{synchronized(a.lifetime.admission){if(!a.valid())return;NativeLightAction.finishScoped((published,failure)->a.close());a.registered=true;pc.nativeSourceLightningOriginalBody(request,spell);}},c);}
   finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);if(!a.registered)a.close();CombatAccess.set(c,"nativeCompanionTargetScope",null);}
  }
 }
 static boolean admit(ClientSession pc,BattleSpellCatalog.DirectionalLineSpell spell){var a=aim();if(a==null)return true;if(a.c.pc!=pc||spell.skillId()!=17||!a.valid())return false;return NativeLightAction.admitPair(pc,pc,17,spell.actionId(),spell.reuseDelayMillis());}
 static boolean active(){return aim()!=null;}
 static boolean captureOutput(ClientSession receiver,byte[] packet,String note){var a=aim();if(a==null||a.outbound!=null)return false;var binding=a.c.bindings.get(receiver);if(binding==null)return true;byte[] frozen=packet.clone();var out=new Output(receiver,binding,frozen,note,a.token());NativeLightAction.deferScoped(()->{if(a.closed||a.lifetime.mode==NativePlayerLifetime.Mode.STOPPED||!a.c.caster.matches(a.c.pc)||!binding.matches(receiver))return;a.outbound=out;try{NativeAreaMaterialAction.owned(a.c,()->receiver.nativeSelfOriginalSend(out.packet,out.note));}finally{a.outbound=null;}});return true;}
 /** Existing final wire monitor: role/life/pose reads only; never acquire an action/Hub/status lock. */
 static boolean allowFrame(ClientSession receiver,byte[] packet){var a=aim();var out=a==null?null:a.outbound;if(out==null)return true;if(a.closed||out.receiver!=receiver||a.lifetime.mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(a.c.hub,"nativeStaticMotionClosed")||!NativeAcceptedFreezeMutation.roleFrame(a.c.caster,a.c.pc)||!NativeAcceptedFreezeMutation.roleFrame(out.binding,receiver))return false;int op=packet.length==0?-1:packet[0]&255;if(receiver==a.c.pc&&(op==Protocol.S_MANA_POINT||op==Protocol.S_HIT_POINT||op==Protocol.S_CHANGE_DIRECTION))return true;var binding=a.c.bindings.get(a.owner);if(binding==null||!NativeAcceptedFreezeMutation.roleFrame(binding,a.owner)||(boolean)CombatAccess.field(a.owner,"sourceItemDisconnected")||number(a.c.pc,"positionX")!=a.cx||number(a.c.pc,"positionY")!=a.cy||number(a.actor(),a.player!=null?"positionX":"x")!=a.x||number(a.actor(),a.player!=null?"positionY":"y")!=a.y)return false;var state=CombatAccess.field(CombatAccess.field(a.actor(),"timedStatus"),"sourcePoisonLifecycle");return state!=null&&CombatAccess.field(state,"sourceNpcOwner")==out.life;}
 private NativeSourceLightningLifetime(){}
}
