package local.lito381.server;
import java.io.IOException;
/** Preserve selector-only targeting and Source PC-only eligibility before ordered material admission. */
final class NativeArmorMaterialAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.ArmorBreakSpell spell)throws IOException {
  Protocol.selectorOnlySpellRequest(packet);
  Object phase=CombatAccess.field(pc,"phase");String name=String.valueOf(phase);ClientSession player=null;
  if(name.equals("BATTLE")||name.equals("WORLD")){
   var hub=(GameHub)CombatAccess.field(pc,"hub");long room=((Number)CombatAccess.field(pc,"currentRoomId")).longValue();
   int map=name.equals("WORLD")?(int)CombatAccess.field(pc,"classicWorldMapId"):hub.battleMapId(room,pc);
   Object target=CombatAccess.call(pc,"resolveArmorBreakTarget","(Llocal/lito381/server/ClientSession$Phase;JIIII)Llocal/lito381/server/ClientSession$ArmorBreakTarget;",phase,room,map,(int)CombatAccess.field(pc,"classicWorldInstanceId"),(int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"));
   target=CombatNpcDebuffs.armorTarget(target,pc);if(target!=null)player=(ClientSession)CombatAccess.get(target,"player");
  }
  NativeLightAction.commitPair(pc,player==null?pc:player,()->pc.nativeMaterialArmorBreakBody(packet,spell));
 }
 static boolean admit(ClientSession pc,Object target,BattleSpellCatalog.ArmorBreakSpell spell){
  if(spell.skillId()!=112)throw new IllegalStateException("unreviewed Armor Break material skill");
  var player=(ClientSession)CombatAccess.get(target,"player");
  return NativeLightAction.admitPair(pc,player==null?pc:player,spell.skillId(),spell.actionId(),spell.reuseDelayMillis());
 }
 static RuntimeInventory previous(ClientSession pc){
  return NativeLightAction.currentMaterial(pc)==null?(RuntimeInventory)CombatAccess.field(pc,"inventory"):NativeCastMaterial.previous(pc);
 }
 private NativeArmorMaterialAction(){}
}
