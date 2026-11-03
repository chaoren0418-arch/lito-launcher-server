package local.lito381.server;
/** Actual native Ref dispatch ABI cases; no PC/Hub/HP/socket/SQL fixture. */
public final class SourceRefRawNpcPlan {
 static int cases;static void check(boolean value,String why){if(!value)throw new AssertionError(why);cases++;}
 public static void main(String[]a){var owner=new Object();var q=new NativeCombatAttacker.Ref(3,7,owner,0,null,5,0,0);var t=new NativeCombatAttacker.Ref(4,7,owner,0,null,5,0,0);String descriptor="(JILjava/lang/String;)I";Object[]args={7L,10,"mortal-body"};
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,args,q)==3,"actual Quest Ref maps only to original Quest ABI");check(NativeSourceRefRawNpc.kind("applyTimeAttackActorDamage",descriptor,args,t)==4,"actual Time Ref maps only to original Time ABI");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,args,t)==0&&NativeSourceRefRawNpc.kind("applyTimeAttackActorDamage",descriptor,args,q)==0,"equal id cannot cross original encounter kind");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{8L,10,"mortal-body"},q)==0,"different id cannot borrow frozen Ref");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage","(JI)I",args,q)==0,"different reflective overload cannot authorize raw HP");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,null,q)==0&&NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{7L,10},q)==0,"missing/truncated original operands are rejected");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{7,10,"mortal-body"},q)==0,"original long id operand type is retained");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{7L,10L,"mortal-body"},q)==0,"original int damage operand type is retained");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{7L,0,"joy-of-pain"},q)==0&&NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{7L,-1,"joy-of-pain"},q)==0,"nonpositive Ref feedback cannot invent HP");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{7L,10,new Object()},q)==0,"arbitrary reflective note object cannot change the original ABI");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,new Object[]{7L,10,null},q)==3,"nullable original String note preserves the native ABI");
  check(NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,args,new NativeCombatAttacker.Ref(2,7,owner,0,null,5,0,0))==0&&NativeSourceRefRawNpc.kind("applyQuestMonsterDamage",descriptor,args,null)==0,"controlled/missing Ref cannot enter natural HP dispatch");
  if(cases!=12)throw new AssertionError(cases);System.out.println("PASS12 actual native Ref Quest-Time reflective ABI/kind/id/operand contracts; no bound accepted-feedback/dead-defender/Main/game/SQL qualification");
 }
}
