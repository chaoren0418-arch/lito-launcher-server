import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class InspectInvisibility {
 public static void main(String[]args)throws Exception {
  try(var jar=new JarFile(args[0])){for(var entry:jar.stream().filter(e->e.getName().endsWith(".class")).toList()) {
   var c=new ClassNode();new ClassReader(jar.getInputStream(entry)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals("local/lito381/server/ClientSession")&&Set.of("revealInvisibility","cancelInvisibilityState","activateInvisibilityState","clearInvisibilityStateSilently").contains(call.name)) {
    var context=new ArrayList<String>();var p=n;for(int i=0;i<5&&(p=p.getPrevious())!=null;i++)if(p instanceof LdcInsnNode v)context.add(String.valueOf(v.cst));else if(p instanceof InvokeDynamicInsnNode v)context.add(Arrays.toString(v.bsmArgs));
    System.out.println(c.name+"\t"+m.name+"\t"+call.name+call.desc+"\t"+context);
   }
  }}
 }
}
