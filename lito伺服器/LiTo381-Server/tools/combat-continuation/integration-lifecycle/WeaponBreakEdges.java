import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Actual helper callers and the next damage/commit call layer; never loads game classes. */
public final class WeaponBreakEdges {
 public static void main(String[]args)throws Exception{var classes=new ArrayList<ClassNode>();var direct=new HashSet<String>();var rows=new TreeSet<String>();
  try(var jar=new JarFile(args[0])){for(var e:Collections.list(jar.entries()))if(e.getName().startsWith("local/lito381/server/")&&e.getName().endsWith(".class")){var c=new ClassNode();new ClassReader(jar.getInputStream(e)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);classes.add(c);}}
  for(var c:classes)for(var m:c.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("local/lito381/server/BattleWeaponBreak")&&call.name.equals("damageAfterStacks")){direct.add(c.name+"."+m.name+m.desc);rows.add("direct\t"+c.name+"\t"+m.name+"\t"+m.desc+"\t"+call.owner+"."+call.name+call.desc);}
  for(var c:classes)for(var m:c.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&direct.contains(call.owner+"."+call.name+call.desc))rows.add("consumer\t"+c.name+"\t"+m.name+"\t"+m.desc+"\t"+call.owner+"."+call.name+call.desc);
  var output=new ArrayList<String>();output.add("layer\tclass\tmethod\tdescriptor\tcallee");output.addAll(rows);Files.write(Path.of(args[1]),output);System.out.println("direct damage methods="+direct.size()+" downstream caller methods="+rows.stream().filter(r->r.startsWith("consumer")).count());
 }
}
