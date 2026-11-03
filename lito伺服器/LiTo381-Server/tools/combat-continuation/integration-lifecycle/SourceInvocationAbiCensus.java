import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Static internal-owner interface ABI inventory, without loading or initializing runtime classes. */
public final class SourceInvocationAbiCensus {
 public static void main(String[]a)throws Exception{try(var jar=new JarFile(a[0])){
  var types=new TreeMap<String,ClassNode>();for(var entry:Collections.list(jar.entries()))if(entry.getName().endsWith(".class")){var c=new ClassNode();new ClassReader(jar.getInputStream(entry)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);types.put(c.name,c);}
  var rows=new ArrayList<String>();rows.add("owner\tmethod\tdescriptor\topcode\tcallee_owner\tcallee_method\tcallee_descriptor\towner_is_interface\tconstant_pool_is_interface");int calls=0;
  for(var c:types.values())for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call&&types.containsKey(call.owner)){
   calls++;boolean itf=(types.get(call.owner).access&ACC_INTERFACE)!=0;
   if(call.itf!=itf||call.getOpcode()==INVOKEVIRTUAL&&itf||call.getOpcode()==INVOKEINTERFACE&&!itf)rows.add(String.join("\t",c.name,m.name,m.desc,Integer.toString(call.getOpcode()),call.owner,call.name,call.desc,Boolean.toString(itf),Boolean.toString(call.itf)));
  }
  Files.write(Path.of(a[1]),rows);System.out.println("INVENTORY internal-owner invocation ABI: classes="+types.size()+", calls="+calls+", mismatched-call-sites="+(rows.size()-1)+"; static evidence only, no runtime semantic qualification");
 }}
}
