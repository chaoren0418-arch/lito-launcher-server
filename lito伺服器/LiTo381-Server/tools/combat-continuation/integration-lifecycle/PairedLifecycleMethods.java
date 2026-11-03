import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Selective C0/C1 method transplantation preserves all later ASM dispatch additions. */
public final class PairedLifecycleMethods {
 static final String P="local/lito381/server/";
 static final Map<String,Set<String>> ALLOWED=Map.of("NativeLightAction",Set.of("commitPair","lambda$commitPair$9","deferSelfSend","lambda$deferSelfSend$7","validTargets"),"NativeLightAction$Batch",Set.of("run","emit"),"NativeCastMaterial",Set.of("active","commit","consumeLocked"));
 static ClassNode read(byte[] b){var c=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static ClassNode jar(JarFile j,String n)throws Exception{return read(j.getInputStream(j.getJarEntry(P+n+".class")).readAllBytes());}
 static ClassNode dir(Path p,String n)throws Exception{return read(Files.readAllBytes(p.resolve(P+n+".class")));}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static byte[] norm(MethodNode m){var c=new ClassNode();c.version=Opcodes.V17;c.access=Opcodes.ACC_PUBLIC;c.name="MethodComparison";c.superName="java/lang/Object";c.methods.add(m);var clean=new ClassNode();new ClassReader(bytes(c)).accept(clean,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return bytes(clean);}
 static String key(MethodNode m){return m.name+m.desc;}
 static void same(MethodNode a,MethodNode b,String why){if(!Arrays.equals(norm(a),norm(b)))throw new AssertionError(why);}
 static MethodNode named(ClassNode c,String k){return c.methods.stream().filter(m->key(m).equals(k)).findFirst().orElseThrow();}
 static void metadata(ClassNode parent,ClassNode next){for(var inner:next.innerClasses)if(parent.innerClasses.stream().noneMatch(i->i.name.equals(inner.name)))parent.innerClasses.add(inner);if(next.nestMembers!=null){if(parent.nestMembers==null)parent.nestMembers=new ArrayList<>();for(var n:next.nestMembers)if(!parent.nestMembers.contains(n))parent.nestMembers.add(n);}}
 static ClassNode patch(JarFile base,String n,Path c0,Path c1)throws Exception{
  var before=jar(base,n);var original=dir(c0,n);var next=dir(c1,n);var allowed=ALLOWED.get(n);
  for(var m:original.methods)if(allowed.contains(m.name))same(named(before,key(m)),m,"C0 selected method differs: "+n+key(m));
  for(var m:next.methods)if(!allowed.contains(m.name))same(named(original,key(m)),m,"unreviewed compiled method change: "+n+key(m));
  for(var m:before.methods)if(allowed.contains(m.name)&&next.methods.stream().noneMatch(x->x.name.equals(m.name)))throw new AssertionError("C1 omitted selected method "+n+m.name);
  var replacement=new ArrayList<MethodNode>();for(var m:before.methods){if(!allowed.contains(m.name)){replacement.add(m);continue;}var match=next.methods.stream().filter(x->key(x).equals(key(m))).findFirst();if(match.isPresent())replacement.add(match.get());}
  for(var m:next.methods)if(allowed.contains(m.name)&&replacement.stream().noneMatch(x->key(x).equals(key(m))))replacement.add(m);before.methods=replacement;
  if(n.endsWith("$Batch")){var f=next.fields.stream().filter(x->x.name.equals("targetReceipt")).findFirst().orElseThrow();if(before.fields.stream().anyMatch(x->x.name.equals(f.name)))throw new AssertionError("existing target receipt field");before.fields.add(f);}
  metadata(before,next);return before;
 }
 public static void main(String[]a)throws Exception{try(var base=new JarFile(a[1])){var c0=Path.of(a[3]);var c1=Path.of(a[4]);for(var n:new TreeSet<>(ALLOWED.keySet())){
   var expected=patch(base,n,c0,c1);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(P+n+".class");Files.createDirectories(p.getParent());
    // Reuse frames from C1 on changed methods; the input helpers are compiled with current JDK17.
    var framed=new ClassNode();new ClassReader(Files.readAllBytes(c1.resolve(P+n+".class"))).accept(framed,ClassReader.SKIP_DEBUG);
    for(var m:new ArrayList<>(expected.methods))if(ALLOWED.get(n).contains(m.name))expected.methods.set(expected.methods.indexOf(m),named(framed,key(m)));
    Files.write(p,bytes(expected));continue;}
   try(var candidate=new JarFile(a[2])){var actual=jar(candidate,n);if(!Arrays.equals(bytes(expected),bytes(actual)))throw new AssertionError("exact selective regeneration differs "+n);
    var before=jar(base,n);actual.methods.removeIf(m->ALLOWED.get(n).contains(m.name));for(var m:before.methods)if(ALLOWED.get(n).contains(m.name))actual.methods.add(m);
    // Compare declarations and every old method by key; removed/added methods are explicit above.
    if(actual.methods.size()!=before.methods.size())throw new AssertionError("method count inverse "+n);for(var m:before.methods)same(m,named(actual,key(m)),"whole original method inverse "+n+key(m));
    actual.fields.removeIf(f->n.endsWith("$Batch")&&f.name.equals("targetReceipt"));if(actual.fields.size()!=before.fields.size())throw new AssertionError("unapproved field "+n);for(int i=0;i<before.fields.size();i++){var x=before.fields.get(i);var y=actual.fields.get(i);if(!Objects.equals(x.name,y.name)||!Objects.equals(x.desc,y.desc)||x.access!=y.access||!Objects.equals(x.value,y.value))throw new AssertionError("field inverse "+n);}
   }
  }System.out.println("PASS selective lifecycle C0/C1 regeneration and complete old method/field inverse; late dispatch, NPC/area/companion hooks and completion cleanup retained");}}
}
