package local.lito381.server;
/** Actual Source flag gate, reference-kind adapter and strict detached target identity; no BATTLE fixture. */
public final class SourceCubePlayerPlan {
 static int cases;static void check(boolean b,String why){if(!b)throw new AssertionError(why);cases++;}
 static NativeCombatAttacker.Ref ref(int kind){return new NativeCombatAttacker.Ref(kind,7001,new Object(),0,null,7,-1,-1);}
 static NativeSourceCubeOrigin.Target target(long id,int kind,Object actor,Object owner,Object life,Object generation){return new NativeSourceCubeOrigin.Target(id,kind,null,null,actor,owner,life,generation);}
 public static void main(String[]args){
  check(!NativeSourceCubePlayer.effect(null),"no source is not an effect NPC");
  for(int kind=1;kind<=4;kind++)check(!NativeSourceCubePlayer.effect(ref(kind)),"existing source kind preserved "+kind);
  var effect=ref(5);check(NativeSourceCubePlayer.effect(effect)&&effect.master()==null,"effect kind is separate and has no player master");
  check(NativeSourceCubePlayer.flags(false,false,true,false,true),"Source raw false/true no player caster and no magic plan");
  check(!NativeSourceCubePlayer.flags(true,false,true,false,true),"WORLD is original fallback");
  check(!NativeSourceCubePlayer.flags(false,true,true,false,true),"ordinary earth-blocking route rejected");
  check(!NativeSourceCubePlayer.flags(false,false,false,false,true),"reactive feedback route rejected");
  check(!NativeSourceCubePlayer.flags(false,false,true,true,true),"player creator cannot become HP caster");
  check(!NativeSourceCubePlayer.flags(false,false,true,false,false),"ordinary magic display plan rejected");
  Object actor=new Object(),owner=new Object(),life=new Object(),generation=new Object();var original=target(20,1,actor,owner,life,generation);
  check(NativeSourceCubePlayer.sameTarget(original,target(20,1,actor,owner,life,generation)),"same actual detached target identity");
  check(!NativeSourceCubePlayer.sameTarget(original,target(20,1,new Object(),owner,life,generation)),"same id replacement actor rejected");
  check(!NativeSourceCubePlayer.sameTarget(original,target(20,1,actor,owner,new Object(),generation)),"same actor new life rejected");
  check(!NativeSourceCubePlayer.sameTarget(original,target(20,1,actor,new Object(),life,generation)),"same actor new owner rejected");
  check(!NativeSourceCubePlayer.sameTarget(original,target(20,1,actor,owner,life,new Object())),"same actor new generation rejected");
  check(!NativeSourceCubePlayer.sameTarget(original,target(20,2,actor,owner,life,generation)),"kind cannot be substituted");
  check(!NativeSourceCubePlayer.sameTarget(original,null),"unbound target cannot inherit lease");
  check(!NativeSourceCubePlayer.sameTarget(original,target(21,1,actor,owner,life,generation)),"different object id rejected");
  check(NativeCombatAttacker.feedback(null,effect)==null,"actual effect adapter does not enter generic reactive NPC calculation");
  check(!NativeCombatAttacker.alive(null,effect),"unbound effect reference cannot create HP authority");
  NativeCombatAttacker.acceptedFeedback(null,effect,null);NativeCombatAttacker.reaction(null,effect,"pure-effect-no-reaction");
  if(cases!=22)throw new AssertionError(cases);System.out.println("PASS22 actual cube PC flags/reference adapter/strict detached identity contracts, no player/Hub/SQL/socket/BATTLE fixture");
 }
}
