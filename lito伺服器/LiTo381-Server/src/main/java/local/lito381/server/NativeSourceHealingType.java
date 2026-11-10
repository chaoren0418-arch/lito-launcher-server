package local.lito381.server;
/** Source tests the actual target class, not the undead flag of a pet/summon template. */
final class NativeSourceHealingType {
 static int undead(SourceNpcPhysicalStats stats,Object selected){
  int original=stats.undead();
  if(NativeSourceHealingCast.current()==null||selected==null)return original;
  var actor=CombatCompanionDamage.controlled(selected);
  if(actor==null)return original;
  // The native kind is final. Reading it needs no snapshot, actor monitor or new lookup.
  var kind=(BattleCompanion.Kind)CombatAccess.field(actor,"kind");
  return kind==BattleCompanion.Kind.PET||kind==BattleCompanion.Kind.SUMMON||kind==BattleCompanion.Kind.TAMED?0:original;
 }
 private NativeSourceHealingType(){}
}
