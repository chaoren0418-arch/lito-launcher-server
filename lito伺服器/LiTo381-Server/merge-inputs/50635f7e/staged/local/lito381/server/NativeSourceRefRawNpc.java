package local.lito381.server;
import java.io.*;
/** The already validated natural Ref feedback port joins the same raw NPC FIFO as direct producers. */
final class NativeSourceRefRawNpc {
 static int kind(String method,String descriptor,Object[] args,NativeCombatAttacker.Ref ref){
  if(ref==null||!"(JILjava/lang/String;)I".equals(descriptor)||args==null||args.length!=3||!(args[0]instanceof Long id)||id!=ref.id()||!(args[1]instanceof Integer damage)||damage<=0||args[2]!=null&&!(args[2]instanceof String))return 0;
  return ref.kind()==3&&"applyQuestMonsterDamage".equals(method)?3:ref.kind()==4&&"applyTimeAttackActorDamage".equals(method)?4:0;
 }
 static Object call(Object receiver,String method,String descriptor,Object[] args,NativeCombatAttacker.Ref ref,Object validated){
  if(!(receiver instanceof ClientSession pc)||!NativeSourceDollCommit.enabled(pc)||ref==null||ref.kind()!=3&&ref.kind()!=4)return CombatAccess.call(receiver,method,descriptor,args);
  var action=NativeSourceRawDamage.current();if(action==null||action.c.pc!=pc)return CombatAccess.call(receiver,method,descriptor,args);
  int kind=kind(method,descriptor,args,ref);var frame=NativePlayerCommit.frame();if(kind==0||frame==null||frame.pc!=pc||!frame.accepted||frame.event.world()||!NativePlayerCommit.validFrame(pc))return 0;
  var npc=action.npcs.get(ref.id());if(npc==null||npc.npc.kind()!=(kind==3?1:2)||npc.life!=ref.owner()||ref.room()!=action.c.room||validated==null||CombatAccess.field(validated,"actor")!=npc.npc.actor())return 0;
  try{return kind==3?NativeSourceRawDamage.quest(pc,ref.id(),(int)args[1],(String)args[2]):NativeSourceRawDamage.time(pc,ref.id(),(int)args[1],(String)args[2]);}catch(IOException failure){throw new UncheckedIOException(failure);}
 }
 private NativeSourceRefRawNpc(){}
}
