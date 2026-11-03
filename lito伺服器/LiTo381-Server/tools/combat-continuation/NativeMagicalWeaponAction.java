package local.lito381.server;
import java.io.IOException;

/** Skill 73 uses the existing cast receipt and one staged inventory commit. */
final class NativeMagicalWeaponAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.MagicalWeaponSpell spell)throws IOException {
  NativeLightAction.commitSelf(pc,()->pc.nativeMaterialMagicalWeaponBody(packet,spell));
 }
 static boolean admit(ClientSession pc,BattleSpellCatalog.MagicalWeaponSpell spell,long target) {
  if(spell.skillId()!=73)throw new IllegalStateException("unreviewed magical weapon skill");
  var cast=NativeLightAction.material(pc,73);
  if(cast.effect!=null||cast.resolved)throw new IllegalStateException("duplicate item effect");
  cast.effect=inventory->inventory.createMagicalWeapon(target);
  return NativeLightAction.admitSelf(pc,73,spell.actionId(),spell.reuseDelayMillis());
 }
 static boolean enchanted(ClientSession pc) {
  var cast=NativeLightAction.currentMaterial(pc);
  if(cast==null||!cast.resolved||cast.effect==null||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("uncommitted item effect");
  return cast.effectResult;
 }
 private NativeMagicalWeaponAction(){}
}
