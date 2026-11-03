package local.lito381.server;
import java.util.*;
/** Unbound type contracts: no session, actor activation, HP mutation or game startup. */
public final class SourceHealingTypePlan {
 static int cases,comparisons;static void check(boolean value,String why){if(!value)throw new AssertionError(why);cases++;}
 static BattleCompanion actor(BattleCompanion.Kind kind)throws Exception{var actor=(BattleCompanion)SourceSignedRecoveryPlan.unsafe().allocateInstance(BattleCompanion.class);SourceSignedRecoveryPlan.set(actor,"kind",kind);return actor;}
 static Object selected(BattleCompanion actor,SourceNpcPhysicalStats stats)throws Exception{var c=Class.forName("local.lito381.server.ClientSession$MassSlowCompanionTarget").getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(null,actor,null,stats);}
 public static void main(String[]args)throws Exception{
  var previous=NativeAreaMaterialAction.current.get();var context=(NativeAreaMaterialAction.Context)SourceSignedRecoveryPlan.unsafe().allocateInstance(NativeAreaMaterialAction.Context.class);SourceSignedRecoveryPlan.set(context,"nativeSourceHealingCast",new NativeSourceHealingCast.Cast(null,null,null,Map.of()));var spell=BattleSpellCatalog.healingSpell(1);check(spell!=null,"actual original native single-healing model");
  try{
   for(var kind:BattleCompanion.Kind.values()){var actor=actor(kind);for(int undead=0;undead<=3;undead++){var stats=new SourceNpcPhysicalStats(123,0,false,0,false,undead,0,0);Object selected=selected(actor,stats);
    NativeAreaMaterialAction.current.remove();check(NativeSourceHealingType.undead(stats,actor)==undead&&NativeSourceHealingType.undead(stats,selected)==undead,"unmarked/WORLD original template semantics retained for "+kind+"/"+undead);
    NativeAreaMaterialAction.current.set(context);int expected=kind==BattleCompanion.Kind.ARENA?undead:0;check(NativeSourceHealingType.undead(stats,actor)==expected&&NativeSourceHealingType.undead(stats,selected)==expected,"actual owned actor and actual selected native record agree on type "+kind+"/"+undead);
    for(int healing:new int[]{Integer.MIN_VALUE,-100,-1,0,1,100,Integer.MAX_VALUE}){int actual=spell.npcHpDelta(NativeSourceHealingType.undead(stats,selected),healing);int wanted=SourceHealingTypeOracle.delta(kind!=BattleCompanion.Kind.ARENA,undead,healing);if(actual!=wanted)throw new AssertionError("exact Source type/undead signed arithmetic "+kind+"/"+undead+"/"+healing);comparisons++;}
   }}
   NativeAreaMaterialAction.current.set(context);for(int undead=0;undead<=3;undead++){var stats=new SourceNpcPhysicalStats(123,0,false,0,false,undead,0,0);check(NativeSourceHealingType.undead(stats,null)==undead,"no selected controlled actor does not reclassify natural NPC or unknown bot");}
   check(comparisons==112,"112 actual native model signed deltas against exact frozen Source target-class/undead statements");check(NativeSourceHealingType.class.getDeclaredFields().length==0,"helper retains no context, actor, lifecycle or snapshot");
  }finally{if(previous==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(previous);}
  check(NativeAreaMaterialAction.current.get()==previous,"original context restored");System.out.println("PASS"+cases+" actual controlled type/scoped native record/unmarked fallback contracts plus112 exact Source type/signed-delta comparisons; no real actor/HP/role/IO/Main/database/game qualification");
 }
}
