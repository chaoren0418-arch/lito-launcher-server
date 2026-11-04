// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/elemental-material/NativeElementalCastAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
/** Preserve both native elemental outcomes; acquire material/action admission at the original debit. */
final class NativeElementalCastAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.ElementalSummonSpell spell)throws IOException {
  NativeLightAction.commitSelf(pc,()->pc.nativeMaterialElementalSummonBody(packet,spell));
 }
 static boolean admit(ClientSession pc,BattleSpellCatalog.ElementalSummonSpell spell){
  if(spell.skillId()!=154&&spell.skillId()!=162)throw new IllegalStateException("unreviewed elemental skill");
  return NativeLightAction.admitSelf(pc,spell.skillId(),spell.actionId(),spell.reuseDelayMillis());
 }
 private NativeElementalCastAction(){}
}
