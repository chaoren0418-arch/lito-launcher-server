package local.lito381.server;
/** Reviewed skill14 metadata uses the existing atomic Source material admission. */
final class NativeWeightMaterialAction {
 static boolean admit(ClientSession pc,BattleSpellCatalog.WeightSpell spell){
  if(spell.skillId()!=14||spell.actionId()!=19)throw new IllegalArgumentException("unreviewed weight skill contract");
  return NativeLightAction.admitSelf(pc,spell.skillId(),spell.actionId(),spell.reuseDelayMillis());
 }
 private NativeWeightMaterialAction(){}
}
