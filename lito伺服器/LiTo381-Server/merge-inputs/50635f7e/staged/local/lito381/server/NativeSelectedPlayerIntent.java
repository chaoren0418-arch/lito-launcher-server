package local.lito381.server;
/** Selected decoded PC only. Existing skill kind, faction, life, range and commit gates remain authoritative. */
final class NativeSelectedPlayerIntent {
 static boolean object(ClientSession pc,Protocol.ObjectSpellRequest request){return request==null||allowed(pc,request.skillId(),request.targetObjectId());}
 static boolean attack(ClientSession pc,Protocol.AttackSpellRequest request){return request==null||allowed(pc,request.skillId(),request.targetObjectId());}
 static boolean allowed(ClientSession pc,int skill,long id){
  if(NativeSkillTargetMetadata.size()==0||!CombatAccess.field(pc,"phase").toString().equals("BATTLE"))return true;
  var row=NativeSkillTargetMetadata.row(skill);if(row==null||((row.targetTo()&13)==0&&skill!=68))return true;
  var hub=(GameHub)CombatAccess.field(pc,"hub");long room=((Number)CombatAccess.field(pc,"currentRoomId")).longValue();
  for(var target:hub.roomCombatants(room))if(target.playerObjectId()==id){
   if(target==pc||ForcedCombat.nativeSelectedObjectIntent(pc,id))return true;
   // This call has no selected Ctrl authority. Current room mode/team rules identify opponents.
   // Friendly requests retain every original skill-specific clan/party and negative-skill restriction.
   return !hub.canAttack(room,pc,target);
  }
  return true; // NPC/ArenaBot/pet/summon and unresolved IDs keep their original kind/admission path.
 }
 private NativeSelectedPlayerIntent(){}
}
