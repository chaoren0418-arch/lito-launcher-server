package local.lito381.server;
/** Natural-NPC wand reaction only; the original raw Action owns identity and output lifetime. */
final class NativeSourceWandReaction {
 static int kind(NativeAreaMaterialAction.Npc npc,long id){
  if(npc==null||npc.kind()!=1&&npc.kind()!=2)return 0;
  return id>0&&npc.id()==id?npc.kind():-1;
 }
 static Integer gfx(NativeAreaMaterialAction.Npc npc){
  if(npc==null||npc.actor()==null||npc.kind()!=1&&npc.kind()!=2)return null;
  String type=npc.kind()==1?"BattleQuestEncounter":"BattleTimeAttackEncounter";
  if(!npc.actor().getClass().getName().equals("local.lito381.server."+type+"$Monster"))return null;
  var snapshot=CombatAccess.call(npc.actor(),"snapshot","()Llocal/lito381/server/"+type+"$Snapshot;");
  return NativeSourceNpcReaction.gfx(npc.kind()+2,snapshot);
 }
 static byte[] hurt(long id,int action,Object selected){
  if(!CombatRules.loaded()||action!=2)return NativeSourceRawDamage.nativeSourceWandReactionOriginalHurt(id,action,selected);
  var a=NativeSourceRawDamage.current();
  if(a==null||!"BATTLE".equals(String.valueOf(CombatAccess.field(a.c.pc,"phase"))))return NativeSourceRawDamage.nativeSourceWandReactionOriginalHurt(id,action,selected);
  var n=a.npcs.get(id);int kind=kind(n==null?null:n.npc,id);
  if(kind==0)return NativeSourceRawDamage.nativeSourceWandReactionOriginalHurt(id,action,selected);
  if(kind<0||!a.frame())return new byte[0];
  var gfx=gfx(n.npc);if(gfx==null||!BattleHitReactionSuppressCatalog.shouldSendDamageAction(gfx))return new byte[0];
  return NativeSourceRawDamage.nativeSourceWandReactionOriginalHurt(id,action,selected);
 }
 private NativeSourceWandReaction(){}
}
