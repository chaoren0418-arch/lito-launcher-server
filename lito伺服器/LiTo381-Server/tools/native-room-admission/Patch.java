import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/";
 public static void main(String[] a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("595245c8c3047124b1163e7c9dc2c6c9f2bd24494f41fa8a5aa5c0d78eae8838"))throw new IllegalStateException("baseline");
  int mode=0,hp=0,bootstrap=0,start=0,winner=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();if(e.getName().equals(P+"NativeRoomDirectoryRow.class"))continue;
    byte[] data=z.getInputStream(e).readAllBytes();
    if(Set.of(P+"ClientSession.class",P+"GameHub.class",P+"GameHub$Room.class",P+"BattleRules$Mode.class").contains(e.getName())) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     for(var m:c.methods) {
      if(c.name.equals(P+"BattleRules$Mode") && m.name.equals("usesArenaBot")) {
       m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();m.instructions.add(new InsnNode(ICONST_0));m.instructions.add(new InsnNode(IRETURN));mode++;
      }
      if(c.name.startsWith(P+"GameHub"))for(var n:m.instructions.toArray()) {
       if(n instanceof FieldInsnNode f && f.getOpcode()==PUTFIELD && f.owner.equals(P+"GameHub$Room") && f.name.equals("botHp") && n.getPrevious() instanceof IntInsnNode value && value.operand==100) {
        m.instructions.set(value,new InsnNode(ICONST_0));hp++;
       }
      }
      if(c.name.equals(P+"GameHub") && Set.of("winningForce","forceWinner").contains(m.name)) {
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call && call.owner.equals("java/util/List") && call.name.equals("size") && n.getNext().getOpcode()==ICONST_1 && n.getNext().getNext().getOpcode()==IF_ICMPNE){
        // Automatic single-player bot winner fallback is disabled with the roster.
        m.instructions.set(n.getNext(),new InsnNode(ICONST_M1));winner++;
       }
      }
      if(c.name.equals(P+"ClientSession") && m.name.equals("startBattle")) {
       for(var n:m.instructions.toArray())if(n instanceof VarInsnNode v && v.getOpcode()==ISTORE && v.var==7) {
        var ins=new InsnList();ins.add(new InsnNode(POP));ins.add(new InsnNode(ICONST_0));m.instructions.insertBefore(n,ins);bootstrap++;
       }
      }
      if(c.name.equals(P+"ClientSession") && m.name.equals("serverStartBattle")) {
       var ins=new InsnList();ins.add(new VarInsnNode(ALOAD,0));ins.add(new MethodInsnNode(INVOKESTATIC,P+"NativeRoomAdmission","hideStartedRoom","(L"+P+"ClientSession;)V",false));m.instructions.insert(ins);start++;
      }
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
   for(String helper:List.of("NativeRoomAdmission","NativeRoomDirectoryRow")) {
    String name=P+helper+".class";o.putNextEntry(new ZipEntry(name));o.write(Files.readAllBytes(classes.resolve(name)));o.closeEntry();
   }
  }
  if(mode!=1 || hp!=5 || bootstrap!=1 || start!=1 || winner!=2)throw new IllegalStateException("hook counts "+mode+"/"+hp+"/"+bootstrap+"/"+start+"/"+winner);
  System.out.println("Patched automatic bot roster/HP/bootstrap/winner fallback and battle-start directory hide");
 }
}
