package local.lito381.server;
import java.io.*;import java.util.*;
/** Synchronous controlled-actor healing uses original participant/life proofs and detached accepted meters. */
final class NativeSourceControlledHealing {
 static final String P="local/lito381/server/";
 record Frozen(NativeSourceRawDamage.Member owner,NativeSourceControlledDisplay.Proof target){boolean current(){return owner.current(false)&&target.current();}}
 record Scope(Map<BattleCompanion,Frozen> actors,List<NativeSourceRawDamage.Member> viewers){Scope{actors=Map.copyOf(actors);viewers=List.copyOf(viewers);}}
 record Output(Frozen target,NativeSourceRawDamage.Member viewer,GameHub hub,NativePlayerLifetime lifetime,long room,int hp,int max){}
 static boolean eligible(BattleCompanion actor){var kind=(BattleCompanion.Kind)CombatAccess.field(actor,"kind");return kind==BattleCompanion.Kind.PET||kind==BattleCompanion.Kind.SUMMON||kind==BattleCompanion.Kind.TAMED;}
 static Scope scope(NativeSourceHealingCast.Cast cast){return cast!=null&&cast.nativeSourceControlledHealing instanceof Scope s?s:null;}
 static void capture(NativeAreaMaterialAction.Context c,NativeSourceHealingCast.Cast cast){
  var actors=new IdentityHashMap<BattleCompanion,Frozen>();var viewers=new ArrayList<NativeSourceRawDamage.Member>();for(var pc:c.players){var m=cast.members.get(pc);if(m!=null)viewers.add(m);}
  synchronized(c.hub){for(var n:c.npcs.values())if(n.kind()==3&&n.actor() instanceof BattleCompanion actor&&eligible(actor)&&n.owner() instanceof ClientSession owner){var m=cast.members.get(owner);if(m==null||!Objects.equals(c.bindings.get(owner),m.binding)||!c.npc(n.id()))continue;var state=NativeCompanionPlacement.state(actor);var proof=new NativeSourceControlledDisplay.Proof(owner,m.binding,m.life,actor,NativeSourceNpcFirstLife.controlled(actor,n,c),CombatAccess.field(actor,"carriedItems"),state,state==null?null:state.pose,state==null?null:state.scene,state==null?null:state.master,(int)CombatAccess.field(actor,"x"),(int)CombatAccess.field(actor,"y"),false);if(c.npc(n.id()))actors.put(actor,new Frozen(m,proof));}}
  cast.nativeSourceControlledHealing=new Scope(actors,viewers);
 }
 static boolean owned(NativeSourceHealingCast.Cast cast,Frozen target){return cast!=null&&target!=null&&Thread.holdsLock(cast.lifetime.admission)&&cast.source.pc.nativePlayerActionLock().isHeldByCurrentThread()&&target.owner().pc.nativePlayerActionLock().isHeldByCurrentThread()&&cast.before()&&target.current();}
 static int sum(int before,int healing){return(int)Math.min(Integer.MAX_VALUE,(long)before+(long)healing);}
 static int heal(BattleCompanion actor,int healing){
  var cast=NativeSourceHealingCast.current();if(cast==null||!eligible(actor)||healing<0)return actor.heal(healing);var scope=scope(cast);var target=scope==null?null:scope.actors().get(actor);int before,after,max;
  if(!owned(cast,target))return(int)CombatAccess.field(actor,"hp");
  synchronized(cast.hub){synchronized(actor){if(!owned(cast,target))return(int)CombatAccess.field(actor,"hp");before=(int)CombatAccess.field(actor,"hp");if(healing==0)return before;after=(int)CombatAccess.call(actor,"nativeSourceControlledHealingHp","(I)I",healing);max=(int)CombatAccess.call(CombatAccess.field(actor,"profile"),"maxHp","()I");}}
  if(after!=before)for(var viewer:scope.viewers())if(viewer.binding.phase().equals("BATTLE")&&viewer.binding.room()==cast.source.binding.room()){var out=new Output(target,viewer,cast.hub,cast.lifetime,cast.source.binding.room(),after,max);NativeLightAction.deferScoped(()->send(out));}
  return after;
 }
 static boolean preview(byte[]packet){var s=scope(NativeSourceHealingCast.current());if(s==null||packet==null||packet.length!=6||(packet[0]&255)!=Protocol.S_HIT_RATIO)return false;long id=NativeSourcePhysicalNpc.object(packet);for(var actor:s.actors().keySet())if((long)CombatAccess.field(actor,"objectId")==id)return true;return false;}
 static boolean current(Output out){return out!=null&&out.lifetime().mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(out.hub(),"nativeStaticMotionClosed")&&NativeSourceOtherControls.settings().hpbar()&&out.target().current()&&out.viewer().current(false)&&out.viewer().binding.phase().equals("BATTLE")&&out.viewer().binding.room()==out.room()&&CombatAccess.field(out.viewer().pc,"hub")==out.hub()&&(int)CombatAccess.field(out.target().target().pet(),"hp")==out.hp()&&(int)CombatAccess.field(CombatAccess.field(out.target().target().pet(),"profile"),"maxHp")==out.max()&&BattleSpellCatalog.isInNativeScreen(NativeSourceDamageDisplay.n(out.viewer().pc,"positionX"),NativeSourceDamageDisplay.n(out.viewer().pc,"positionY"),out.target().target().x(),out.target().target().y());}
 static long object(Output out){return(long)CombatAccess.field(out.target().target().pet(),"objectId");}
 static boolean packet(Output out,byte[]packet){return packet!=null&&Arrays.equals(packet,Protocol.hitRatio(object(out),out.hp(),out.max()));}
 static void send(Output out){if(!current(out))return;var previous=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.remove();try{var c=new NativeAreaMaterialAction.Context(out.viewer().pc);c.nativeSourceCubeStatusProof=out;try{NativeAreaMaterialAction.owned(c,()->{synchronized(out.viewer().pc){if(current(out))out.viewer().pc.nativeSelfOriginalSend(Protocol.hitRatio(object(out),out.hp(),out.max()),"S_HIT_RATIO Source accepted controlled healing");}});}finally{c.nativeSourceCubeStatusProof=null;}}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(out.lifetime());}finally{if(previous==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(previous);}}
 static boolean allowFrame(ClientSession pc,byte[]packet){var c=NativeAreaMaterialAction.current.get();return c==null||!(c.nativeSourceCubeStatusProof instanceof Output out)||out.viewer().pc==pc&&current(out)&&packet(out,packet);}
 private NativeSourceControlledHealing(){}
}
