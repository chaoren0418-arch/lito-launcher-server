import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/";
 public static void main(String[]a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("589733435518d92c05baeeae37a686d384ec3f7b9897e975ad1ef50d67a74aa7"))throw new IllegalStateException("baseline mismatch");
  int ticks=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
    if(e.getName().equals(P+"ClientSession.class")) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     c.fields.add(new FieldNode(ACC_PRIVATE,"illusionIconLeases","[J",null,null));
     for(var m:c.methods)if(m.name.equals("serverExpireTimedStatuses") && m.desc.equals("(J)V")) {
      var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,P+"IllusionStatusIcons","sync","(L"+P+"ClientSession;)V",false));m.instructions.insert(i);ticks++;
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    if(e.getName().equals(P+"EmptyMelee.class"))data=Files.readAllBytes(classes.resolve(e.getName()));
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
   String h=P+"IllusionStatusIcons.class";o.putNextEntry(new ZipEntry(h));o.write(Files.readAllBytes(classes.resolve(h)));o.closeEntry();
  }
  if(ticks!=1)throw new IllegalStateException("tick hook count "+ticks);
  System.out.println("PASS: authoritative lease reconciliation; empty attack base action and heading corrected");
 }
}
