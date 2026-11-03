import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class InspectTimedLocks {
 public static void main(String[]args)throws Exception {
  try(var jar=new JarFile(args[0])) {for(var entry:jar.stream().filter(e->e.getName().endsWith(".class")).toList()) {
   var c=new ClassNode();new ClassReader(jar.getInputStream(entry)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals("local/lito381/server/BattleTimedStatus")&&Set.of("actionLocked","thunderGrabActive","activateThunderGrab","removeThunderGrab").contains(call.name))System.out.println(c.name+"\t"+m.name+"\t"+m.desc+"\t"+call.name+call.desc);
  }}
 }
}
