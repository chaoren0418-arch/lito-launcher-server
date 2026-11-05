package local.lito381.server;
/** Only Source-legal selected self29/152/153. No new Ctrl authority or general faction relaxation. */
final class NativeSourceNegativeSelf {
 static boolean legal(ClientSession pc,int skill){
  if(skill!=29&&skill!=152&&skill!=153||NativeSkillTargetMetadata.size()==0||!CombatAccess.field(pc,"phase").toString().equals("BATTLE"))return false;
  var row=NativeSkillTargetMetadata.row(skill);if(row.type()!=1||row.area()!=0||(row.targetTo()&1)==0||!row.target().equals("buff"))return false;
  var hub=(GameHub)CombatAccess.field(pc,"hub");long room=((Number)CombatAccess.field(pc,"currentRoomId")).longValue();return room>0&&hub.roomRules(room).mode()!=BattleRules.Mode.PET_RACE&&hub.roomCombatants(room).contains(pc);
 }
 static ClientSession selected(ClientSession pc,Protocol.AttackSpellRequest request,ClientSession original,int skill){return original==null&&request.skillId()==skill&&request.targetObjectId()==pc.playerObjectId()&&legal(pc,skill)?pc:original;}
 static boolean target(GameHub hub,long room,ClientSession pc,ClientSession selected,int skill){return selected==pc&&legal(pc,skill)||hub.canAttack(room,pc,selected);}
 private NativeSourceNegativeSelf(){}
}
