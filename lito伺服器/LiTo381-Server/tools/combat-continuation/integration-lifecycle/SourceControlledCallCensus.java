import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;
/** Enumerate actual archive methods, including package-private aliases that javap heading searches can miss. */
public final class SourceControlledCallCensus {
 public static void main(String[]a)throws Exception{try(var jar=new JarFile(a[0])){var c=FreezeSourceTargetDelta.read(jar,"local/lito381/server/ClientSession");for(var m:c.methods)for(var node:m.instructions)if(node instanceof MethodInsnNode call&&call.owner.equals(c.name)&&Set.of("applyBattleSpellCompanionDamage","applyBattleCompanionFeedbackDamage").contains(call.name))System.out.println(m.name+m.desc+" -> "+call.name+call.desc);}}
}
