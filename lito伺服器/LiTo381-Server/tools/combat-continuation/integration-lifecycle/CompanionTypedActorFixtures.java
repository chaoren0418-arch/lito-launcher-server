package local.lito381.server;
/** 保留既有 AI／Calc／Frame 流程；SUMMON 明確使用召喚工廠並核對真正型別。 */
public class CompanionTypedActorFixtures extends NativeCombatAttackerRegression {
 static Actor fresh(ClientSession pc,ClientSession master,GameHub hub,String label)throws Exception{
  var a=NativeCombatAttackerRegression.fresh(pc,master,hub,label);var kind=BattleCompanion.Kind.valueOf(label.substring(label.lastIndexOf('-')+1));
  if(kind==BattleCompanion.Kind.SUMMON){var old=(BattleCompanion)a.raw();NativeBattleTamedLifecycle.pets(master).remove(old);var pet=BattleCompanion.lesserElemental(a.id(),SourceSummonTemplate.memoryFixture(45306),6,600,a.x(),a.y(),System.nanoTime());pet.qualifyNpcDefense(SourceNpcPhysicalStats.memoryFixture(45306));pet.selectTarget(pc.playerObjectId());NativeBattleTamedLifecycle.pets(master).add(pet);a=new Actor(a.label(),pet,pet,a.id(),a.world(),true,a.mode(),a.x(),a.y());}
  check(((BattleCompanion)a.raw()).snapshot().kind()==kind,"actual snapshot kind equals declared fixture kind="+label);return a;
 }
}
