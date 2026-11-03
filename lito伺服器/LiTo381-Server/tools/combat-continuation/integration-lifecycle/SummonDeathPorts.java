import java.nio.file.*;import java.io.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Read-only inventory of real damage/deletion entrances; a port list is not runtime qualification. */
public final class SummonDeathPorts {
 public static void main(String[]a)throws Exception{try(var jar=new JarFile(a[0]);var out=new PrintWriter(Files.newBufferedWriter(Path.of(a[1])))){for(var entry:Collections.list(jar.entries())){
  if(!entry.getName().startsWith("local/lito381/server/")||!entry.getName().endsWith(".class"))continue;var c=new ClassNode();new ClassReader(jar.getInputStream(entry)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call&&(call.owner.equals("local/lito381/server/BattleCompanion")&&Set.of("applyDamage","deactivate").contains(call.name)||call.owner.equals("local/lito381/server/ClientSession")&&call.name.equals("removeClassicWorldCompanion")))out.println(c.name+"."+m.name+m.desc+" @"+m.instructions.indexOf(n)+" -> "+call.owner+"."+call.name+call.desc);
 }} }
}
