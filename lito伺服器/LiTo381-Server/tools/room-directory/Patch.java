import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/";
 public static void main(String[] a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("c3c0fecd67510397e1436ffdaf9ad5bb5d314ff518887db13e1031683453b9f0"))throw new IllegalStateException("baseline");
  Map<String,String> targets=Map.of("joinRoom([BLjava/lang/String;Z)V","room-joined","exitRoom(Z)V","room-exited","editRoom([B)V","room-edited"); int count=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
    if(e.getName().equals(P+"ClientSession.class")) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     for(var m:c.methods) {
      if(Set.of("broadcastDefaultRoomReset", "broadcastRoomDeleted").contains(m.name) && m.desc.equals("(J)V")) {
       AbstractInsnNode last=null;for(var n:m.instructions.toArray())if(n.getOpcode()==ATHROW)last=n;
       if(last==null)throw new IllegalStateException("missing collected peer error "+m.name);
       m.instructions.set(last,new MethodInsnNode(INVOKESTATIC,P+"LobbyRoomDirectory","peerFailure","(Ljava/io/IOException;)V",false));count++;continue;
      }
      String reason=targets.get(m.name+m.desc);if(reason==null)continue;
      AbstractInsnNode tail=null;for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN)tail=n;
      if(tail==null)throw new IllegalStateException("missing final return "+m.name);
      var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new LdcInsnNode(reason));i.add(new MethodInsnNode(INVOKESTATIC,P+"LobbyRoomDirectory","refresh","(L"+P+"ClientSession;Ljava/lang/String;)V",false));m.instructions.insertBefore(tail,i);count++;
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
   String name=P+"LobbyRoomDirectory.class";o.putNextEntry(new ZipEntry(name));o.write(Files.readAllBytes(classes.resolve(name)));o.closeEntry();
  }
  if(count!=5)throw new IllegalStateException("hook count "+count);System.out.println("Patched 3 room lifecycle tails and 2 peer-error boundaries");
 }
}
