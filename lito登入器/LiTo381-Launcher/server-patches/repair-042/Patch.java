import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 public static void main(String[] a)throws Exception {
  String P="local/lito381/server/";Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("eeb0ba0b4c62e49cc73770b69ab67f877d3607a91ecce795a7a7b968e9c86409"))throw new IllegalStateException("Unexpected base");
  int hooks=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var es=z.entries();es.hasMoreElements();) {var e=es.nextElement();byte[] b=z.getInputStream(e).readAllBytes();
    if(e.getName().startsWith(P+"SharedDatabase")&&e.getName().endsWith(".class"))b=Files.readAllBytes(classes.resolve(e.getName()));
    if(e.getName().equals(P+"LegacyMySqlRepository.class")||e.getName().equals(P+"ServerConfig.class")) {
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);
     for(var m:c.methods)if(m.name.equals("<init>")||m.name.equals("load"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode mi&&mi.owner.equals("java/util/Properties")&&mi.name.equals("load")&&mi.desc.equals("(Ljava/io/Reader;)V")) {
      // Preserve a Properties reference across load; no local-slot assumptions.
      m.instructions.insertBefore(n,new InsnNode(DUP2));var h=new InsnList();h.add(new InsnNode(POP));
      if(c.name.equals(P+"ServerConfig")){h.add(new VarInsnNode(ALOAD,0));h.add(new MethodInsnNode(INVOKESTATIC,P+"UnifiedConfiguration","loaded","(Ljava/util/Properties;Ljava/nio/file/Path;)V",false));}
      else h.add(new MethodInsnNode(INVOKESTATIC,P+"UnifiedConfiguration","aliases","(Ljava/util/Properties;)V",false));
      m.instructions.insert(n,h);hooks++;
     }
     ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(b);o.closeEntry();
   }
   try(var files=Files.list(classes.resolve(P))) {for(Path f:files.filter(x->x.getFileName().toString().startsWith("UnifiedConfiguration")&&x.toString().endsWith(".class")).toList()){o.putNextEntry(new ZipEntry(P+f.getFileName()));o.write(Files.readAllBytes(f));o.closeEntry();}}
  }
  if(hooks!=2)throw new IllegalStateException("Expected config and credentials adapters: "+hooks);System.out.println("PASS unified configuration adapters patched");
 }
}
