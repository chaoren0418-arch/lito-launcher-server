import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** Archive facts only. Never loads or initializes a game class. */
public final class SourceCompletionCensus {
 public static void main(String[] args)throws Exception {
  var edges=new ArrayList<String>(); var methods=new ArrayList<String>();var literals=new ArrayList<String>();var keys=args.length>3?new HashSet<>(Files.readAllLines(Path.of(args[3]))):Set.<String>of();
  try(var jar=new JarFile(args[0])) {
   for(var e:Collections.list(jar.entries())) if(e.getName().startsWith("local/lito381/server/")&&e.getName().endsWith(".class")) {
    var c=new ClassNode();try(var in=jar.getInputStream(e)){new ClassReader(in).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}
    for(var m:c.methods){methods.add(c.name+"\t"+m.name+"\t"+m.desc);int ordinal=0;
     for(var n:m.instructions){if(n instanceof LdcInsnNode value&&value.cst instanceof String key&&keys.contains(key))literals.add(c.name+"\t"+m.name+"\t"+key);if(n instanceof MethodInsnNode call){
      if(call.owner.startsWith("local/lito381/server/"))edges.add(c.name+"\t"+m.name+"\t"+m.desc+"\t"+ordinal+"\t"+call.owner+"\t"+call.name+"\t"+call.desc);ordinal++;
     }}
    }
   }
  }
  Collections.sort(edges);Collections.sort(methods);
  Files.write(Path.of(args[1]),join("owner\tmethod\tdescriptor\tcall_ordinal\tcallee_owner\tcallee\tcallee_descriptor",edges));
  Files.write(Path.of(args[2]),join("owner\tmethod\tdescriptor",methods));
  if(args.length>4){Collections.sort(literals);Files.write(Path.of(args[4]),join("owner\tmethod\tSource_property_key",literals));}
  System.out.println("PASS archive-only census methods="+methods.size()+" local-call-sites="+edges.size()+"; initialized-game-classes=0");
 }
 static List<String> join(String header,List<String> rows){var out=new ArrayList<String>();out.add(header);out.addAll(rows);return out;}
}
