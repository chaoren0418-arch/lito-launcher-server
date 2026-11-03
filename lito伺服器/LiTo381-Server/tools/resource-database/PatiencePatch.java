import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
final class PatiencePatch implements Opcodes{
 static final String P="local/lito381/server/",H=P+"CombatSourceReductions";
 static boolean accepts(String n){return Set.of(P+"BattlePatienceStatus.class",P+"BattleSpellCatalog$PatienceSpell.class").contains(n);}
 static void apply(ClassNode c,Map<String,Integer> hooks){if(c.name.equals(P+"BattlePatienceStatus")){var m=CombatPatch.method(c,"physicalReduction","(JJ)I");int count=0;for(var n:m.instructions.toArray())if(n.getOpcode()==ICONST_2){m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,H,"patience","(I)I",false));count++;}CombatPatch.require(count,1,"source physical Patience active scalar");hooks.put("source-patience-physical",count);}
  if(c.name.equals(P+"BattleSpellCatalog$PatienceSpell")){var m=CombatPatch.method(c,"physicalDamageReduction","()I");int count=0;for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(c.name)&&f.name.equals("physicalDamageReduction")&&f.desc.equals("I")){m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,H,"patience","(I)I",false));count++;}CombatPatch.require(count,1,"source Patience catalog display scalar");hooks.put("source-patience-display",count);}
 }
}
