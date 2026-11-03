import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class PatchSkills {
 static final String C="local/lito381/server/ClientSession";
 static ClassNode read(byte[] b){ClassNode c=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_FRAMES);return c;}
 public static void main(String[] a)throws Exception {
  Path base=Path.of(a[0]), donor=Path.of(a[1]), output=Path.of(a[2]);
  String pin=HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)));
  if(!pin.equals("D2DFA6ACF1B3913D3038286F0CD26A4B3A7880A2EB87F467BB01E0409D82B178"))throw new AssertionError("0.6.13 pin");
  ClassNode d=read(Files.readAllBytes(donor));
  Set<String> names=new HashSet<>(List.of("useAreaAttackSpell","useCenteredAreaAttackSpell","useBlindSpell","useWeaknessSpell","useManaDrainSpell"));
  List<MethodNode> selected=new ArrayList<>();
  for(var m:d.methods)if(names.contains(m.name))selected.add(m);
  if(selected.size()!=6)throw new AssertionError("3 exact methods "+selected.size());
  Map<String,String> rename=new LinkedHashMap<>();
  for(int i=0;i<selected.size();i++) for(var ins:selected.get(i).instructions.toArray()) {
   if(ins instanceof InvokeDynamicInsnNode indy)for(Object arg:indy.bsmArgs)if(arg instanceof Handle h&&h.getOwner().equals(C)&&h.getName().startsWith("lambda$")) {
    if(!rename.containsKey(h.getName())) { rename.put(h.getName(),"repair032$"+h.getName()); selected.add(d.methods.stream().filter(m->m.name.equals(h.getName())&&m.desc.equals(h.getDesc())).findFirst().orElseThrow()); }
   }
  }
  for(var m:selected){
   for(var ins:m.instructions.toArray()){
    if(ins instanceof InvokeDynamicInsnNode indy)for(int j=0;j<indy.bsmArgs.length;j++)if(indy.bsmArgs[j] instanceof Handle h&&h.getOwner().equals(C)&&rename.containsKey(h.getName()))indy.bsmArgs[j]=new Handle(h.getTag(),C,rename.get(h.getName()),h.getDesc(),h.isInterface());
    if(ins instanceof MethodInsnNode call&&call.owner.equals(C)&&rename.containsKey(call.name))call.name=rename.get(call.name);
   }
   if(rename.containsKey(m.name))m.name=rename.get(m.name);
  }
  try(var in=new ZipFile(base.toFile());var out=new ZipOutputStream(Files.newOutputStream(output))){
   for(var entries=in.entries();entries.hasMoreElements();){var entry=entries.nextElement();byte[] b=in.getInputStream(entry).readAllBytes();
    if(entry.getName().equals(C+".class")){
     ClassNode c=read(b);
     for(var m:selected){c.methods.removeIf(old->old.name.equals(m.name)&&old.desc.equals(m.desc));c.methods.add(m);System.out.println(m.name+m.desc);}
     ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();
    }
    var e=new ZipEntry(entry.getName());e.setTime(entry.getTime());out.putNextEntry(e);out.write(b);out.closeEntry();
   }
  }
 }
}
