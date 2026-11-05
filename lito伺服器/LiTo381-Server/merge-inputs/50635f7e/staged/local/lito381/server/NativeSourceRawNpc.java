package local.lito381.server;
import java.io.*;import java.util.*;
/** Raw wand/accepted-feedback NPC leaves share the original FIFO without inventing ordinary magic figures. */
final class NativeSourceRawNpc {
 record Source(NativeSourceDamageDisplay.Actor actor,boolean position,boolean dead){}
 record Admission(Source source,Object actor,Object owner,Object life,Object generation){}
 static int monster(ClientSession pc,long id,int damage,String reason,boolean quest)throws IOException{
  var action=NativeSourceRawDamage.current();if(!NativeSourceDollCommit.enabled(pc)||action==null)return NativeSourceRawDamage.nativeSourceRawNpcOriginalMonster(pc,id,damage,reason,quest);
  var n=action.npcs.get(id);if(action.c.pc!=pc||n==null||n.npc.kind()!=(quest?1:2)||!action.frame()||!action.c.scene()||!n.before()||damage<=0)return 0;
  // Source raw Member.current permits an already accepted dead defender; freeze its actual accepted life.
  var member=action.root;var actor=new NativeSourceDamageDisplay.Actor(pc,member.binding,member.acceptedLife,member.x,member.y);var aim=new NativeSourceMonsterHp.Aim(action.c,n.npc,(NativeSourceMonsterHp.Cache)CombatAccess.field(n.npc.actor(),NativeSourceMonsterHp.CACHE),actor,n.generation,n.x,n.y);
  var source=new Source(actor,action.position,member.dead);var admission=new Admission(source,n.npc.actor(),n.npc.owner(),n.npc.epoch(),n.generation);var c=action.c;var prior=c.nativeSourceMagicAdmission;c.nativeSourceMagicAdmission=admission;
  try{NativeSourcePhysicalNpc.submit(aim,damage,0,false,reason,null,null,null);}finally{c.nativeSourceMagicAdmission=prior;}return 0;
 }
 static Object capture(NativeSourcePhysicalNpc.Origin origin){var c=NativeAreaMaterialAction.current.get();if(c==null||!(c.nativeSourceMagicAdmission instanceof Admission a)||origin==null)return null;var t=origin.target();var source=a.source().actor();return t.actor()==a.actor()&&t.owner()==a.owner()&&t.life()==a.life()&&t.generation()==a.generation()&&origin.source()==source?a.source():null;}
 static NativeSourceMagicNpc.Snapshot admission(){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceMagicAdmission instanceof Admission&&c.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot s?s:null;}
 /** Keep the old live-attacker HPBAR policy separate from accepted raw HP; corpse-meter cleanup still runs. */
 static boolean allowHpbar(NativeSourcePhysicalNpc.Commit commit){return commit==null||!(commit.origin.nativeSourceRawNpcSource instanceof Source)||commit.dead||NativeSourceDamageDisplay.n(commit.origin.source().pc(),"playerHp")>0;}
 static boolean allowOutput(NativeSourcePhysicalNpc.Output out){return out==null||out.packet().length==0||(out.packet()[0]&255)!=Protocol.S_HIT_RATIO||(out.packet()[out.packet().length-1]&255)==255||allowHpbar(out.commit());}
 /** -1 keeps the exact old ordinary-attack policy; raw only reproduces its original Member.current proof. */
 static int sourceState(NativeSourcePhysicalNpc.Origin origin){if(origin==null||!(origin.nativeSourceRawNpcSource instanceof Source proof))return -1;var source=proof.actor();var pc=source.pc();boolean valid=origin.source()==source&&NativeAcceptedFreezeMutation.roleFrame(source.binding(),pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&!(boolean)CombatAccess.field(pc,"observer")&&NativeSourceControlledDisplay.rawLife(pc)==source.life()&&(!proof.position()||NativeSourceDamageDisplay.n(pc,"positionX")==source.x()&&NativeSourceDamageDisplay.n(pc,"positionY")==source.y())&&(!proof.dead()||NativeSourceDamageDisplay.n(pc,"playerHp")==0)&&CombatAccess.field(pc,"hub")==origin.scene().hub()&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&(long)CombatAccess.field(pc,"currentRoomId")==origin.scene().id();return valid?1:0;}
 private NativeSourceRawNpc(){}
}
