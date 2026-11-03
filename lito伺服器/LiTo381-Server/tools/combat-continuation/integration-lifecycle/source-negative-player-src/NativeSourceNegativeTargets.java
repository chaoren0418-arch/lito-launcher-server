package local.lito381.server;
import java.io.*;import java.util.*;
/** Actual controlled primary, original area player/NPC arithmetic, Source failure by controlled actor class. */
final class NativeSourceNegativeTargets {
 record Output(ClientSession receiver,NativePlayerCommit.Binding binding,byte[] packet,String note){}
 static final class Aim {
  final int skill;final NativeAreaMaterialAction.Context c;final NativeAreaMaterialAction.Npc target;final ClientSession player,owner;final BattleCompanion pet;final NativePlayerLifetime lifetime;final int cx,cy,x,y;final Object life;final NativeCompanionPlacement.State placement;final Object pose,scene,master;boolean registered;volatile boolean closed;volatile Output outbound;
  Aim(NativeAreaMaterialAction.Context c,NativeAreaMaterialAction.Npc target,ClientSession player,int skill){this.skill=skill;
   this.c=c;this.target=target;this.player=player;owner=player!=null?player:(ClientSession)target.owner();pet=target==null?null:(BattleCompanion)target.actor();
   lifetime=NativePlayerLifetime.of(c.hub);cx=NativePlayerCommit.integer(c.pc,"positionX");cy=NativePlayerCommit.integer(c.pc,"positionY");
   var snapshot=pet==null?null:pet.snapshot();x=pet==null?NativePlayerCommit.integer(player,"positionX"):snapshot.x();y=pet==null?NativePlayerCommit.integer(player,"positionY"):snapshot.y();
   life=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet==null?player:pet,"timedStatus"),true);placement=pet==null?null:NativeCompanionPlacement.state(pet);pose=placement==null?null:placement.pose;scene=placement==null?null:placement.scene;master=placement==null?null:placement.master;
  }
  void close(){closed=true;outbound=null;if(c.nativeSourceNegativeAim==this)c.nativeSourceNegativeAim=null;}
  boolean valid(){
   if(lifetime.mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")||!c.scene()||!(pet==null?c.player(player):c.npc(target.id()))||NativePlayerCommit.integer(c.pc,"positionX")!=cx||NativePlayerCommit.integer(c.pc,"positionY")!=cy)return false;
   if(pet==null){if(NativePlayerCommit.integer(player,"positionX")!=x||NativePlayerCommit.integer(player,"positionY")!=y)return false;}else{var snapshot=pet.snapshot();if(snapshot.x()!=x||snapshot.y()!=y)return false;}
   var row=NativeSkillTargetMetadata.row(skill);if(row.area()!=0||row.targetTo()!=3||row.type()!=1||row.through())throw new IllegalStateException("unqualified Source167 target metadata");
   if(!BattleSpellCatalog.isInNativeScreen(cx,cy,x,y)||row.ranged()>=0&&Math.max(Math.abs(cx-x),Math.abs(cy-y))>row.ranged())return false;
   var scope=NativeCompanionTargets.scope(c);if(pet!=null&&(scope==null||owner!=c.pc&&!scope.owns(c,owner)))return false;
   long id=pet==null?c.bindings.get(player).object():target.id();return !c.phase.equals("BATTLE")||owner==c.pc||ForcedCombat.canAttackTarget(c.hub,c.room,c.pc,owner,id);
  }
 }
 static void entangle(ClientSession pc,byte[] packet,BattleSpellCatalog.EntangleSpell spell,int fixture)throws IOException{run(pc,packet,spell,fixture);}
 static void erase(ClientSession pc,byte[] packet,BattleSpellCatalog.EraseMagicSpell spell,int fixture)throws IOException{run(pc,packet,spell,fixture);}
 static void original(ClientSession pc,byte[] packet,Object spell,int fixture)throws IOException{if(spell instanceof BattleSpellCatalog.EntangleSpell e)NativeNegativeMaterialAction.entangle(pc,packet,e,fixture);else NativeNegativeMaterialAction.erase(pc,packet,(BattleSpellCatalog.EraseMagicSpell)spell,fixture);}
 static void run(ClientSession pc,byte[] packet,Object spell,int fixture)throws IOException{
  var request=Protocol.attackSpellRequest(packet);int skill=request.skillId();if(NativeSkillTargetMetadata.size()==0||skill!=29&&skill!=152&&skill!=153){original(pc,packet,spell,fixture);return;}var c=new NativeAreaMaterialAction.Context(pc);if(!c.phase.equals("WORLD")&&!c.phase.equals("BATTLE"))return;var n=c.npcs.get(request.targetObjectId());ClientSession player=null;for(var candidate:c.players)if(c.bindings.get(candidate).object()==request.targetObjectId()){player=candidate;break;}if(player==null&&(n==null||n.kind()!=3)){original(pc,packet,spell,fixture);return;}if(player!=null)n=null;var aim=new Aim(c,n,player,skill);
  try(var scope=new NativeCompanionTargets.Scope(c)){if(!scope.valid)return;CombatAccess.set(c,"nativeCompanionTargetScope",scope);c.nativeSourceNegativeAim=aim;CombatAccess.set(c,NativeCompanionNegative.STAGE,1);var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(c);
   try{NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(pc,skill,()->{synchronized(aim.lifetime.admission){register(aim);if(NativeAreaMaterialAction.current.get()!=c||!aim.valid())return;c.npcs.entrySet().removeIf(e->e.getValue().kind()==3&&scope.receipts.containsKey((ClientSession)e.getValue().owner())&&!scope.owns(c,(ClientSession)e.getValue().owner()));CombatAccess.set(c,NativeCompanionNegative.STAGE,0);try{if(spell instanceof BattleSpellCatalog.EntangleSpell e)pc.nativeMaterialEntangleBody(packet,e,fixture);else pc.nativeMaterialEraseBody(packet,(BattleSpellCatalog.EraseMagicSpell)spell,fixture);}finally{CombatAccess.set(c,NativeCompanionNegative.STAGE,1);}}},c);}
   finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);if(!aim.registered)aim.close();CombatAccess.set(c,"nativeCompanionTargetScope",null);}
  }
 }
 static Aim aim(){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceNegativeAim instanceof Aim a?a:null;}
 static void register(Aim a){if(!a.registered){NativeLightAction.finishScoped((published,failure)->a.close());a.registered=true;}}
 static boolean active(){return aim()!=null;}
 static boolean impact(Aim a,Output o){int op=o.packet.length==0?-1:o.packet[0]&255;return o.receiver==a.c.pc&&(op==Protocol.S_MANA_POINT||op==Protocol.S_HIT_POINT||op==Protocol.S_CHANGE_DIRECTION);}
 static boolean position(Aim a){if(a.closed||a.lifetime.mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(a.c.hub,"nativeStaticMotionClosed")||!a.c.scene()||!(a.pet==null?a.c.player(a.player):a.c.npc(a.target.id()))||NativePlayerCommit.integer(a.c.pc,"positionX")!=a.cx||NativePlayerCommit.integer(a.c.pc,"positionY")!=a.cy)return false;if(a.pet==null)return NativePlayerCommit.integer(a.player,"positionX")==a.x&&NativePlayerCommit.integer(a.player,"positionY")==a.y;var snapshot=a.pet.snapshot();return snapshot.x()==a.x&&snapshot.y()==a.y;}
 static boolean captureOutput(ClientSession receiver,byte[] packet,String note){var a=aim();if(a==null||a.outbound!=null)return false;register(a);var binding=a.c.bindings.get(receiver);if(binding==null)return true;var out=new Output(receiver,binding,packet.clone(),note);NativeLightAction.deferScoped(()->{if(a.closed||a.lifetime.mode==NativePlayerLifetime.Mode.STOPPED||!a.c.caster.matches(a.c.pc)||!out.binding.matches(receiver)||!impact(a,out)&&!position(a))return;a.outbound=out;try{NativeAreaMaterialAction.owned(a.c,()->receiver.nativeSelfOriginalSend(out.packet,out.note));}finally{a.outbound=null;}});return true;}
 /** Existing native output monitor only: direct role/life/pose reads, no action/Hub/status locks. */
 static boolean allowFrame(ClientSession receiver,byte[] packet){
  var a=aim();var out=a==null?null:a.outbound;if(out==null)return true;
  if(out.receiver!=receiver||a.closed||a.lifetime.mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(a.c.hub,"nativeStaticMotionClosed")||!NativeAcceptedFreezeMutation.roleFrame(a.c.caster,a.c.pc)||!NativeAcceptedFreezeMutation.roleFrame(out.binding,receiver))return false;
  if(impact(a,out))return true;
  var binding=a.c.bindings.get(a.owner);if(binding==null||!NativeAcceptedFreezeMutation.roleFrame(binding,a.owner)||(boolean)CombatAccess.field(a.owner,"sourceItemDisconnected")||NativePlayerCommit.integer(a.c.pc,"positionX")!=a.cx||NativePlayerCommit.integer(a.c.pc,"positionY")!=a.cy)return false;
  Object actor=a.pet==null?a.player:a.pet;
  if((int)CombatAccess.field(actor,a.pet==null?"positionX":"x")!=a.x||(int)CombatAccess.field(actor,a.pet==null?"positionY":"y")!=a.y)return false;
  var life=CombatAccess.field(CombatAccess.field(actor,"timedStatus"),"sourcePoisonLifecycle");if(life==null||CombatAccess.field(life,"sourceNpcOwner")!=a.life)return false;
  if(a.pet!=null){
   if(!((List<?>)CombatAccess.field(a.owner,"tamedCompanions")).contains(a.pet))return false;
   var placement=NativeCompanionPlacement.state(a.pet);if(placement!=a.placement||placement!=null&&(placement.owner.get()!=a.owner||placement.pose!=a.pose||placement.scene!=a.scene||placement.master!=a.master))return false;
  }
  if(packet.length>=5){int op=packet[0]&255;if(op==Protocol.S_EFFECT||op==Protocol.S_SPEED||op==Protocol.S_ACTION||op==Protocol.S_POISON){long id=NativeStaticNpcMotion.id(packet,1);for(var entry:a.c.bindings.entrySet())if(entry.getValue().object()==id&&!NativeAcceptedFreezeMutation.roleFrame(entry.getValue(),entry.getKey()))return false;}}
  return true;
 }
 static boolean admit(ClientSession pc,ClientSession player,ClientSession owner,int skill,int action,int reuse){var a=aim();if(a==null)return NativeNegativeMaterialAction.admit(pc,player,owner,skill,action,reuse);if(a.c.pc!=pc||a.skill!=skill||!a.valid()||(a.player!=null?(player!=a.player||owner!=null):(player!=null||owner!=a.owner)))return false;return NativeLightAction.admitPair(pc,pc,skill,action,reuse);}
 private NativeSourceNegativeTargets(){}
}
