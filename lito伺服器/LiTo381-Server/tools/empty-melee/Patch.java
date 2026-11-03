import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/",H=P+"EmptyMelee",S=P+"ClientSession",R=P+"Protocol$AttackRequest";
 public static void main(String[]a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("9c97f86821738cd4e039f20bf69cae286bb1f095bdda77e3305799a77f5c1a23"))throw new IllegalStateException("baseline mismatch");
  int dispatch=0,parser=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
    if(e.getName().equals(S+".class") || e.getName().equals(P+"Protocol.class")) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     if(c.name.equals(S))c.fields.add(new FieldNode(ACC_PRIVATE,"emptyMeleeReadyNanos","J",null,null));
     for(var m:c.methods) {
      if(m.name.equals("attack") && m.desc.equals("(L"+R+";L"+S+"$AttackEnvelope;)V")) {
       var i=new InsnList();var normal=new LabelNode();
       i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(new VarInsnNode(ALOAD,2));
       i.add(new MethodInsnNode(INVOKEVIRTUAL,S+"$AttackEnvelope","name","()Ljava/lang/String;",false));
       i.add(new MethodInsnNode(INVOKESTATIC,H,"handle","(L"+S+";L"+R+";Ljava/lang/String;)Z",false));
       i.add(new JumpInsnNode(IFEQ,normal));i.add(new InsnNode(RETURN));i.add(normal);m.instructions.insert(i);dispatch++;
      }
      if(c.name.equals(P+"Protocol") && m.name.equals("attackRequest")) {
       m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
       m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ILOAD,1));
       m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"parse","([BI)L"+R+";",false));m.instructions.add(new InsnNode(ARETURN));parser++;
      }
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
   o.putNextEntry(new ZipEntry(H+".class"));o.write(Files.readAllBytes(classes.resolve(H+".class")));o.closeEntry();
  }
  if(dispatch!=1 || parser!=1)throw new IllegalStateException("hook count mismatch");
  System.out.println("PASS: one target-zero dispatch, one parser; existing nonzero combat branch retained");
 }
}
