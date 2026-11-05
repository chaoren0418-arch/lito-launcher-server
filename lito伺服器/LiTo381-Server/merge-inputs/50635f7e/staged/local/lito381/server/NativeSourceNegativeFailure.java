package local.lito381.server;
/** Source PET/SUMMON are NPC, never Monster. No per-actor state or alternate effect/cost implementation. */
final class NativeSourceNegativeFailure {
 static boolean failure(BattleCompanion actor,int skill){
  if(actor==null||NativeSkillTargetMetadata.size()==0)return false;
  if(skill!=29&&skill!=152&&skill!=153)throw new IllegalStateException("unqualified Source negative failure skill");
  var row=NativeSkillTargetMetadata.row(skill);if(row.type()!=1||row.area()!=0||(row.targetTo()&2)==0)throw new IllegalStateException("unqualified Source negative actor metadata");
  return true;
 }
 static boolean eligible(SourceNpcPhysicalStats stats,BattleCompanion actor,int skill){return failure(actor,skill)||stats.eraseEligible();}
 private NativeSourceNegativeFailure(){}
}
