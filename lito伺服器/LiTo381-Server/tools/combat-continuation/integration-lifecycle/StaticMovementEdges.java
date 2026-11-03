import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Read-only census of every static NPC location/catalog consumer in the actual candidate. */
public final class StaticMovementEdges {
 public static void main(String[]args)throws Exception{var rows=new ArrayList<String>();rows.add("class\tmethod\tdescriptor\tstatic\tcallee\tcallee_descriptor");String p="local/lito381/server/";
  try(var jar=new JarFile(args[0])){for(var e:Collections.list(jar.entries()))if(e.getName().startsWith(p)&&e.getName().endsWith(".class")){
   var c=new ClassNode();new ClassReader(jar.getInputStream(e)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   for(var m:c.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&((call.owner.equals(p+"ClassicWorldNpcCatalog$StaticNpc")&&Set.of("x","y","heading","passispeed","movementDistance").contains(call.name))
     ||call.owner.equals(p+"ClassicWorldNpcCatalog")&&Set.of("inScreen","fixedMerchantsInScreen","occupies").contains(call.name)))rows.add(c.name+"\t"+m.name+"\t"+m.desc+"\t"+((m.access&Opcodes.ACC_STATIC)!=0)+"\t"+call.owner+"."+call.name+"\t"+call.desc);
  }}Collections.sort(rows.subList(1,rows.size()));Files.write(Path.of(args[1]),rows);System.out.println("static NPC movement/location consumers="+(rows.size()-1));
 }
}
