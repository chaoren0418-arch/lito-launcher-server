package local.lito381.server;
import java.io.IOException;
/** Preserve both native elemental outcomes; acquire material/action admission at the original debit. */
final class NativeSummonCastAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.SummonSpell spell)throws IOException {
  NativeLightAction.commitSelf(pc,()->pc.nativeMaterialSummonBody(packet,spell));
 }
 static boolean admit(ClientSession pc,BattleSpellCatalog.SummonSpell spell){
  if(spell.skillId()!=51)throw new IllegalStateException("unreviewed elemental skill");
  return NativeLightAction.admitSelf(pc,spell.skillId(),spell.actionId(),spell.reuseDelayMillis());
 }
 private NativeSummonCastAction(){}
}
