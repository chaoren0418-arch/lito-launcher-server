package local.lito381.server;
/** Nonpositive Source target mutations do not require a fabricated accepted HP event. */
final class NativeSourceNpcMagicDirect {
 record Mutation(NativeSourceRawDamage.Member target,GameHub hub,NativePlayerLifetime lifetime,BattleEraseMagicStatus status,BattleEraseMagicStatus.Lease erase){}
 static boolean current(Mutation m){return m!=null&&m.lifetime().mode==NativePlayerLifetime.Mode.OPEN&&!(boolean)CombatAccess.field(m.hub(),"nativeStaticMotionClosed")&&m.target()!=null&&m.target().binding!=null&&m.target().binding.phase().equals("BATTLE")&&CombatAccess.field(m.target().pc,"hub")==m.hub()&&m.target().current(false)&&NativeSourceDamageDisplay.n(m.target().pc,"playerHp")>0&&CombatAccess.field(m.target().pc,"eraseMagicStatus")==m.status();}
 static boolean apply(Mutation m){if(m==null||m.erase()==null||!m.erase().present()||m.target()==null||!m.target().pc.nativePlayerActionLock().isHeldByCurrentThread())return false;synchronized(m.lifetime().admission){return current(m)&&m.status().consume(m.erase());}}
 static boolean handled(NativeSourceDamageDisplay.Plan plan,NativeSourceNpcPlayer.Scope scope,ClientSession pc,int amount,boolean magic){
  if(!magic||amount>0||scope==null||scope!=NativeCombatAttacker.nativeSourceNpcPlayerCurrent()||!pc.nativePlayerActionLock().isHeldByCurrentThread())return false;
  var value=CombatAccess.field(scope,"nativeSourceNpcMagicDefinition");if(!(value instanceof BattleTimeAttackMagicSkillCatalog.Definition definition))return false;
  NativeSkillTargetMetadata.Row row;try{row=NativeSkillTargetMetadata.row(definition.skillId());}catch(IllegalStateException absent){return false;}if(row==null||row.type()!=64)return false;
  var member=scope.origin().members().get(pc);if(member==null||plan.nativeRawGuard==null||plan.nativeRawGuard.target()!=member)return false;
  var status=(BattleEraseMagicStatus)CombatAccess.field(pc,"eraseMagicStatus");var mutation=new Mutation(member,scope.origin().hub(),scope.lifetime(),status,status.capture(System.nanoTime()));if(amount==0)apply(mutation);else plan.nativeSourceNpcMagicAfter=mutation;
  // Source _dmg==0 has no HP commit; negative damage still goes through the original signed HP path.
  return amount==0;
 }
 static void afterSigned(NativeSourceDamageDisplay.Plan plan,int amount){if(amount>=0||!(plan.nativeSourceNpcMagicAfter instanceof Mutation m))return;try{apply(m);}finally{if(plan.nativeSourceNpcMagicAfter==m)plan.nativeSourceNpcMagicAfter=null;}}
 private NativeSourceNpcMagicDirect(){}
}
