import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class PatchDetails {
 public static void main(String[] a)throws Exception {
  try(var in=new ZipFile(a[0]);var out=new ZipOutputStream(Files.newOutputStream(Path.of(a[2])))) {
   boolean found=false;
   for(var es=in.entries();es.hasMoreElements();){var e=es.nextElement();byte[] b=in.getInputStream(e).readAllBytes();
    if(e.getName().equals("local/lito381/server/GameHub.class")){
     ClassNode c=new ClassNode(),d=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_FRAMES);
     new ClassReader(Files.readAllBytes(Path.of(a[1]).resolveSibling("GameHub.class"))).accept(d,ClassReader.SKIP_FRAMES);
     MethodNode donor=d.methods.stream().filter(m->m.name.equals("botTicks")).findFirst().orElseThrow();
     c.methods.removeIf(m->m.name.equals(donor.name)&&m.desc.equals(donor.desc));c.methods.add(donor);
     ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(cw);b=cw.toByteArray();
    }
    if(e.getName().equals("local/lito381/server/ClientSession.class")){
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_FRAMES);
     for(var m:c.methods)if(m.name.equals("sendEncryptedFrame") && m.desc.equals("([BLjava/lang/String;)V")){
      if(found)throw new AssertionError();found=true;
      for(var i:m.instructions.toArray())if(i.getOpcode()==Opcodes.RETURN){InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,0));hook.add(new VarInsnNode(Opcodes.ALOAD,1));hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"local/lito381/server/LiToItemDetails","publish","(Llocal/lito381/server/ClientSession;[B)V",false));m.instructions.insertBefore(i,hook);}
     }
     ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(cw);b=cw.toByteArray();
    }
    out.putNextEntry(new ZipEntry(e.getName()));out.write(b);out.closeEntry();
   }
   if(!found)throw new AssertionError("sendEncryptedFrame hook absent");
   try(var helpers=Files.newDirectoryStream(Path.of(a[1]).getParent(),"LiToBotEffects*.class")){
    for(var file:helpers){out.putNextEntry(new ZipEntry("local/lito381/server/"+file.getFileName()));out.write(Files.readAllBytes(file));out.closeEntry();}
   }
   out.putNextEntry(new ZipEntry("local/lito381/server/LiToItemDetails.class"));out.write(Files.readAllBytes(Path.of(a[1])));out.closeEntry();
  }
 }
}
