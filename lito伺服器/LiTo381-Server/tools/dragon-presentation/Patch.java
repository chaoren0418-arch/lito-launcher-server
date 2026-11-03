import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 public static void main(String[]a)throws Exception {
  Path base=Path.of(a[0]),out=Path.of(a[1]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("ae063385dcdf3898aeb003137f807e6c3365073aece62b8531fb7e86886dcd06"))throw new IllegalStateException("baseline mismatch");
  int removed=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
    if(e.getName().equals("local/lito381/server/ClientSession.class")) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     for(var m:c.methods) if(m.name.equals("useFoeSlayerSpell")) {
      boolean actionNote=false;
      for(var n:m.instructions.toArray()) {
       if(n instanceof InvokeDynamicInsnNode d) actionNote=Arrays.stream(d.bsmArgs).anyMatch(x->x instanceof String s && s.startsWith("S_ACTION Foe Slayer caster="));
       if(actionNote && n instanceof MethodInsnNode call && call.name.equals("sendEncrypted") && call.desc.equals("([BLjava/lang/String;)V")) {
        var i=new InsnList();i.add(new InsnNode(POP));i.add(new InsnNode(POP));i.add(new InsnNode(POP));m.instructions.insertBefore(n,i);m.instructions.remove(n);removed++;actionNote=false;
       }
      }
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
  }
  if(removed!=1)throw new IllegalStateException("Expected one Foe Slayer casting action, got "+removed);
  System.out.println("PASS: removed one extra S_ACTION; all physical strikes and effects retained");
 }
}
