import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;
/** Only two existing scalar operands change; activation/cancellation/deadlines remain exact. */
final class VerifyPatience{
 static final String P="local/lito381/server/",H=P+"CombatSourceReductions";
 static MethodNode normalize(MethodNode original,String owner){var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);int count=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){var prior=VerifyCombatPreservation.prior(call);boolean status=owner.equals(P+"BattlePatienceStatus")&&m.name.equals("physicalReduction")&&m.desc.equals("(JJ)I");boolean catalog=owner.equals(P+"BattleSpellCatalog$PatienceSpell")&&m.name.equals("physicalDamageReduction")&&m.desc.equals("()I");if(call.getOpcode()!=INVOKESTATIC||!call.name.equals("patience")||!call.desc.equals("(I)I")||!(status&&prior.getOpcode()==ICONST_2||catalog&&prior instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(owner)&&f.name.equals("physicalDamageReduction")&&f.desc.equals("I")))throw new AssertionError("unqualified source Patience scalar");m.instructions.remove(call);count++;}
  int expected=owner.equals(P+"BattlePatienceStatus")&&m.name.equals("physicalReduction")||owner.equals(P+"BattleSpellCatalog$PatienceSpell")&&m.name.equals("physicalDamageReduction")?1:0;if(count!=expected)throw new AssertionError("source Patience exact getter count="+owner+"."+m.name);return m;
 }
}
