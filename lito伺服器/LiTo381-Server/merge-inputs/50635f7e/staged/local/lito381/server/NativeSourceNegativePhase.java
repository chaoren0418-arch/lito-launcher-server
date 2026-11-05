package local.lito381.server;
/** Source normal casts resolve material/action before probability, and HP/MP after calculation. */
final class NativeSourceNegativePhase {
 static boolean deferred(int skill){var c=NativeAreaMaterialAction.current.get();return (skill==29||skill==152||skill==153)&&NativeSourcePlayerFactories.scoped(c)&&NativeSourceNpcTargetFailure.probability(skill);}
 static NativeSourceNpcFactoryType.Stamp known(Object quest,Object timed){return NativeSourceNpcTargetFailure.known(quest!=null?quest:timed);}
 static boolean eligible(SourceNpcPhysicalStats stats,BattleCompanion actor,int skill,Object quest,Object timed){return deferred(skill)&&known(quest,timed)!=null||NativeSourceNegativeFailure.eligible(stats,actor,skill);}
 static boolean failure(BattleCompanion actor,int skill,Object quest,Object timed){if(actor!=null)return NativeSourceNegativeFailure.failure(actor,skill);var stamp=deferred(skill)?known(quest,timed):null;return stamp!=null?NativeSourceNpcTargetFailure.failed(stamp,skill):NativeSourceNegativeFailure.failure(actor,skill);}
 private NativeSourceNegativePhase(){}
}
