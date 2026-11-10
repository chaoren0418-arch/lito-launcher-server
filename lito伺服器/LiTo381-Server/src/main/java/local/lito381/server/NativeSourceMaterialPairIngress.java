package local.lito381.server;
/** Carry the existing bounded Task through original binding, ingress and Receipt reservations. */
final class NativeSourceMaterialPairIngress {
 static NativeSelfCastAction.Task prepare(ClientSession pc,ClientSession target,NativeSelfCastAction.Task original){
  if(target!=null&&NativeSourcePairedScope.enabled(pc)&&(CombatAccess.field(pc,"timedStatus")==null||CombatAccess.field(target,"timedStatus")==null))return new NativeSourceMaterialPairGuard.Guarded(null,null,null,original);
  return NativeSourceLightPairLife.task(pc,target,original);
 }
 static boolean current(NativeSourceMaterialPairGuard.Guarded g){
  var p=g.proof();return CombatRules.loaded()&&p!=null&&p.caster()!=null&&p.target()!=null&&p.lifetime()!=null&&g.sourceTimed()!=null&&g.targetTimed()!=null
   &&CombatAccess.field(p.caster().pc(),"timedStatus")==g.sourceTimed()&&CombatAccess.field(p.target().pc(),"timedStatus")==g.targetTimed()
   &&CombatAccess.field(p.hub(),"sourcePlayerCommitLifetime")==p.lifetime()&&p.current();
 }
 static boolean enter(NativeSelfCastAction.Task task){return !(task instanceof NativeSourceMaterialPairGuard.Guarded g)||current(g);}
 static NativeSourceLightPairLife.Actor actor(ClientSession pc,NativeSourceMaterialPairGuard.Guarded g){var p=g.proof();return p==null?null:p.caster()!=null&&p.caster().pc()==pc?p.caster():p.target()!=null&&p.target().pc()==pc?p.target():null;}
 static boolean initial(ClientSession pc,NativeSelfCastAction.Task task){return task instanceof NativeSourceMaterialPairGuard.Guarded g&&current(g)&&actor(pc,g)!=null;}
 static NativePlayerCommit.Binding binding(ClientSession pc,NativeSelfCastAction.Task task){if(!(task instanceof NativeSourceMaterialPairGuard.Guarded g))return NativeWeaponBreakTargetLifecycle.targetBinding(pc);return initial(pc,task)?actor(pc,g).binding():null;}
 static NativePlayerLifetime lifetime(ClientSession pc,NativeSelfCastAction.Task task){return ((NativeSourceMaterialPairGuard.Guarded)task).proof().lifetime();}
 static boolean application(ClientSession pc,NativeSelfCastAction.Task task,NativeServerLifetime app){return initial(pc,task)&&lifetime(pc,task).application==app;}
 static boolean processes(boolean original,ClientSession pc,NativeSelfCastAction.Task task,NativeServerLifetime app){return original&&application(pc,task,app);}
 static NativePlayerVitalCompletions.Receipt reserve(ClientSession pc,NativeSelfCastAction.Task task){return initial(pc,task)?NativePlayerVitalCompletions.nativeSourceMaterialPairReserve(pc,task):null;}
 static NativePlayerVitalCompletions.Receipt targetReserve(ClientSession pc,NativeAreaMaterialAction.Context c,NativeSelfCastAction.Task task){if(!(task instanceof NativeSourceMaterialPairGuard.Guarded))return NativeSourceWeaponTargetLife.reserve(pc,c);return reserve(pc,task);}
 static NativePlayerVitalCompletions.Receipt reuse(NativePlayerVitalCompletions.Receipt r,ClientSession pc,NativeAreaMaterialAction.Context c,NativeSelfCastAction.Task task){
  if(task instanceof NativeSourceMaterialPairGuard.Guarded&&(!initial(pc,task)||r.pc!=pc||r.life!=lifetime(pc,task)||r.app!=r.life.application)){r.close();return null;}
  return NativeSourceWeaponTargetLife.reuse(r,pc,c);
 }
 static NativePlayerVitalCompletions.Receipt target(ClientSession pc,NativeSelfCastAction.Task task){if(!(task instanceof NativeSourceMaterialPairGuard.Guarded))return NativeWeaponBreakTargetLifecycle.reserveTarget(pc);return initial(pc,task)?NativeWeaponBreakTargetLifecycle.nativeSourceMaterialPairTarget(pc,task):null;}
 static Object ingress(ClientSession pc,NativeSelfCastAction.Task task){if(!(task instanceof NativeSourceMaterialPairGuard.Guarded))return NativeLightAction.nativeSourceMaterialPairOriginalReserve(pc);return initial(pc,task)?NativeLightAction.nativeSourceMaterialPairIngress(pc,task):null;}
 private NativeSourceMaterialPairIngress(){}
}
