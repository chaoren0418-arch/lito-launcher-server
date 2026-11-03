import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Read classfiles only. Edges are static overapproximations, not branch/packet acceptance proof. */
public final class TargetParserCallInventory {
 static final String P="local/lito381/server/";record Key(String owner,String name,String desc){String show(){return owner.substring(P.length())+"."+name+desc;}}
 static final Map<Key,List<Key>> graph=new HashMap<>();
 static boolean follow(Key k){return k.owner.startsWith(P)&&!Set.of(P+"GameHub",P+"ResourceDatabase",P+"CombatDatabase",P+"SourceLoginRegression",P+"AccountRepository").contains(k.owner)&&!k.name.equals("handle")&&!k.name.equals("handleBeforeForcedCombat")&&!k.name.equals("handleBeforeNativeBattleCastIngress");}
 public static void main(String[]a)throws Exception{
  var roots=new TreeSet<String>();for(String row:Files.readAllLines(Path.of(a[1]))){if(row.startsWith("skill_id\t"))continue;var cols=row.split("\t",-1);for(String s:cols[16].replace("[","").replace("]","").split(","))if(!s.isBlank())roots.add(s.trim());}
  try(var jar=new JarFile(a[0])){for(var entry:Collections.list(jar.entries())){if(!entry.getName().startsWith(P)||!entry.getName().endsWith(".class"))continue;var c=new ClassNode();new ClassReader(jar.getInputStream(entry)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);for(var m:c.methods){var edges=new ArrayList<Key>();for(var n:m.instructions){if(n instanceof MethodInsnNode call)edges.add(new Key(call.owner,call.name,call.desc));else if(n instanceof InvokeDynamicInsnNode indy)for(var value:indy.bsmArgs)if(value instanceof Handle h&&h.getOwner().startsWith(P))edges.add(new Key(h.getOwner(),h.getName(),h.getDesc()));}graph.put(new Key(c.name,m.name,m.desc),edges);}}}
  var out=new ArrayList<String>();out.add("root\troot_descriptor\tparser\tshortest_static_path\tvisited_methods\tbounded_or_reflective_gap");int found=0;
  for(String name:roots)for(Key start:graph.keySet().stream().filter(k->k.owner.equals(P+"ClientSession")&&k.name.equals(name)).sorted(Comparator.comparing(Key::desc)).toList()){
   var queue=new ArrayDeque<Key>();var seen=new HashMap<Key,List<Key>>();queue.add(start);seen.put(start,List.of(start));var parsers=new TreeMap<String,List<Key>>();
   while(!queue.isEmpty()){var k=queue.remove();var path=seen.get(k);for(Key edge:graph.getOrDefault(k,List.of())){if(edge.owner.equals(P+"Protocol")&&edge.name.endsWith("Request")){var next=new ArrayList<>(path);next.add(edge);parsers.putIfAbsent(edge.show(),next);continue;}if(!follow(edge)||path.size()>=24||seen.containsKey(edge))continue;var next=new ArrayList<>(path);next.add(edge);seen.put(edge,next);queue.add(edge);}}
   if(parsers.isEmpty())out.add(String.join("\t",name,start.desc,"<none>",start.show(),""+seen.size(),"static graph only; reflection/dynamic type/skill-id branches unproved"));
   for(var entry:parsers.entrySet()){found++;out.add(String.join("\t",name,start.desc,entry.getKey(),String.join(" -> ",entry.getValue().stream().map(Key::show).toList()),""+seen.size(),"static graph only; reflection/dynamic type/skill-id branches unproved"));}
  }
  Files.write(Path.of(a[2]),out);System.out.println("PASS classfile-only roots="+roots.size()+" parser_paths="+found+" rows="+(out.size()-1)+"; zero initialization/Hub/SQL");
 }
}
