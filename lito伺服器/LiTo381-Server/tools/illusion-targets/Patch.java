import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/",H=P+"IllusionSkillRequests";
 public static void main(String[] a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("db2ef1343a85a7dc830cbc42311a43ab66e2521bf623f115894f90fd36344511"))throw new IllegalStateException("baseline");
  Set<String> pc=Set.of("IllusionOgre","IllusionLich","DiamondGolem","Insight","IllusionAvatar"),actors=Set.of("Patience","Phantasm","Panic","JoyOfPain");int hooked=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
    if(e.getName().equals(P+"ClientSession.class")) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     for(var m:c.methods) {
      if(!m.name.startsWith("resolveSelectorOnly")||!m.name.endsWith("Target"))continue;
      String name=m.name.substring(19,m.name.length()-6);boolean isPc=pc.contains(name);if(!isPc&&!actors.contains(name))continue;
      int slot=-1;AbstractInsnNode insert=null;
      for(var n:m.instructions.toArray()) {
       if(n instanceof MethodInsnNode call && ((isPc&&call.owner.equals("java/util/ArrayList")&&call.name.equals("<init>"))||(!isPc&&Set.of("patienceTargets","phantasmTargets").contains(call.name)))) {
        var next=n.getNext();while(next!=null&&next.getOpcode()<0)next=next.getNext();
        if(next instanceof VarInsnNode v&&v.getOpcode()==ASTORE)slot=v.var;
       }
       if(n instanceof FieldInsnNode f&&f.name.equals("lastCombatTargetObjectId")&&!isPc) {
        // Map.get: ALOAD external, ALOAD this, GETFIELD id, box, invoke get.
        var previous=n.getPrevious();while(previous.getOpcode()<0)previous=previous.getPrevious();
        var before=previous.getPrevious();while(before.getOpcode()<0)before=before.getPrevious();
        if(before instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot){insert=before;break;}
       }
       if(isPc&&n instanceof JumpInsnNode j&&j.getOpcode()==IFNULL) {
        var next=n.getNext();while(next!=null&&next.getOpcode()<0)next=next.getNext();
        if(next instanceof VarInsnNode v&&v.getOpcode()==ALOAD) {
         var after=next.getNext();while(after!=null&&after.getOpcode()<0)after=after.getNext();
         if(after!=null&&after.getOpcode()==ARETURN) {insert=n.getPrevious();while(insert.getOpcode()<0)insert=insert.getPrevious();break;}
        }
       }
      }
      if(slot<0||insert==null)throw new IllegalStateException("resolver pattern "+m.name+" slot="+slot);
      var i=new InsnList();var legacy=new LabelNode();i.add(new MethodInsnNode(INVOKESTATIC,H,"current","()L"+P+"Protocol$AttackSpellRequest;",false));i.add(new JumpInsnNode(IFNULL,legacy));
      i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,slot));
      i.add(new MethodInsnNode(INVOKESTATIC,H,isPc?"selectPc":"selectActor","(L"+P+"ClientSession;Ljava/util/"+(isPc?"List;)L"+P+"ClientSession;":"Map;)Ljava/lang/Object;"),false));
      if(!isPc)i.add(new TypeInsnNode(CHECKCAST,P+"ClientSession$PatienceTarget"));i.add(new InsnNode(ARETURN));i.add(legacy);
      m.instructions.insertBefore(insert,i);hooked++;System.out.println("Patched "+m.name);
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    if(e.getName().equals(P+"DragonSkillRequests.class"))data=Files.readAllBytes(classes.resolve(e.getName()));
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
   String helper=H+".class";o.putNextEntry(new ZipEntry(helper));o.write(Files.readAllBytes(classes.resolve(helper)));o.closeEntry();
  }
  if(hooked!=9)throw new IllegalStateException("resolver count "+hooked);
 }
}
