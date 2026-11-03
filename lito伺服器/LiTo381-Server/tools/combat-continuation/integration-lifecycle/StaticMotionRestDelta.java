import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Exact proof that the later rest-phase adjustment preserves all previously replayed skill code. */
public final class StaticMotionRestDelta {
 static ClassNode read(byte[] b){var c=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(0);c.accept(w);return w.toByteArray();}
 static String key(MethodNode m){return m.name+m.desc;}
 static byte[] method(MethodNode m){var c=new ClassNode();c.version=Opcodes.V17;c.access=Opcodes.ACC_PUBLIC;c.name="ExactMethod";c.superName="java/lang/Object";c.methods.add(m);return bytes(c);}
 public static void main(String[]a)throws Exception{
  try(var old=new JarFile(a[0]);var next=new JarFile(a[1])){var changed=new TreeSet<String>();var names=new TreeSet<String>();old.stream().forEach(e->names.add(e.getName()));var current=new TreeSet<String>();next.stream().forEach(e->current.add(e.getName()));if(!current.equals(names))throw new AssertionError("rest adjustment added/deleted archive entries");
   for(var n:names)if(!Arrays.equals(old.getInputStream(old.getJarEntry(n)).readAllBytes(),next.getInputStream(next.getJarEntry(n)).readAllBytes()))changed.add(n);
   String p="local/lito381/server/";if(!changed.equals(new TreeSet<>(Set.of(p+"GameHub.class",p+"NativeStaticNpcMotion.class"))))throw new AssertionError("unexpected rest delta "+changed);
   var rows=new ArrayList<String>();for(var n:changed){var before=read(old.getInputStream(old.getJarEntry(n)).readAllBytes());var after=read(next.getInputStream(next.getJarEntry(n)).readAllBytes());var allowed=n.endsWith("GameHub.class")?Set.<String>of():Set.of("state","tick","restPhase");var oldFields=before.fields.stream().map(f->f.access+" "+f.name+" "+f.desc+" "+f.value).toList();var extra=n.endsWith("GameHub.class")?"nativeStaticMotionRestTick":"REST";if(after.fields.stream().filter(f->f.name.equals(extra)).count()!=1)throw new AssertionError("phase scalar declaration");after.fields.removeIf(f->f.name.equals(extra));if(!oldFields.equals(after.fields.stream().map(f->f.access+" "+f.name+" "+f.desc+" "+f.value).toList()))throw new AssertionError("unrelated field delta");
    for(var m:before.methods)if(!allowed.contains(m.name)){var actual=after.methods.stream().filter(x->key(x).equals(key(m))).findFirst().orElseThrow();if(!Arrays.equals(method(m),method(actual)))throw new AssertionError("unrelated method changed "+n+key(m));rows.add("unchanged\t"+n+"\t"+key(m));}
    var added=after.methods.stream().filter(m->before.methods.stream().noneMatch(o->key(o).equals(key(m)))).map(m->m.name).toList();if(!added.equals(n.endsWith("GameHub.class")?List.of():List.of("restPhase")))throw new AssertionError("method declarations");
   }rows.add("changed_classes\t"+String.join(",",changed));rows.add("scope\tone hub long scalar, one helper constant, state/tick/restPhase only; PC, NPC, materials, pair/Ctrl and all other classes byte-identical");Files.write(Path.of(a[2]),rows);System.out.println("PASS exact rest-phase delta; unchanged methods="+(rows.size()-2));
  }
 }
}
