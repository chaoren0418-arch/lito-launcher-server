import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/",H=P+"NativeRoomRules";
 static MethodInsnNode call(String name,String d){return new MethodInsnNode(INVOKESTATIC,H,name,d,false);}
 static void self(InsnList i){i.add(new VarInsnNode(ALOAD,0));}
 static void hub(InsnList i){self(i);i.add(new FieldInsnNode(GETFIELD,P+"ClientSession","hub","L"+P+"GameHub;"));}
 static void clear(MethodNode m){m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();}
 public static void main(String[] a)throws Exception{
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("bbc59308c43df89ee5029d3b3c802be8ad55fee65c41faca99a76912dd5dce43"))throw new IllegalStateException("0.6.60 input mismatch");
  List<Path> generated;try(var files=Files.walk(classes.resolve(P))){generated=files.filter(p->p.toString().endsWith(".class")).toList();}
  Set<String> replaced=new HashSet<>();for(var p:generated)replaced.add(classes.relativize(p).toString().replace('\\','/'));
  Map<String,Integer> counts=new TreeMap<>();
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))){
   for(var it=z.entries();it.hasMoreElements();){var e=it.nextElement();if(replaced.contains(e.getName()))continue;byte[] bytes=z.getInputStream(e).readAllBytes();
    if(Set.of(P+"GameHub.class",P+"ClientSession.class").contains(e.getName())){
     var c=new ClassNode();new ClassReader(bytes).accept(c,0);
     for(var m:c.methods){
      if(c.name.equals(P+"GameHub")){
       if(m.name.equals("<init>")&&m.desc.endsWith("AccountRepository;)V")){
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.name.equals("ensureDefaultRoom")){
         var i=new InsnList();self(i);i.add(new VarInsnNode(ALOAD,1));i.add(new VarInsnNode(ALOAD,3));i.add(call("install","(L"+P+"GameHub;L"+P+"ServerConfig;L"+P+"AccountRepository;)V"));m.instructions.insertBefore(n,i);counts.merge("install",1,Integer::sum);
        }
       }
       if(m.name.equals("ensureDefaultRoom")){
        var i=new InsnList();var next=new LabelNode();self(i);i.add(call("enabled","(L"+P+"GameHub;)Z"));i.add(new JumpInsnNode(IFNE,next));i.add(new InsnNode(ACONST_NULL));i.add(new InsnNode(ARETURN));i.add(next);m.instructions.insert(i);counts.merge("default",1,Integer::sum);
       }
       if(m.name.equals("nativeRosterReady")){
        clear(m);self(m.instructions);m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new VarInsnNode(ALOAD,2));m.instructions.add(call("rosterReady","(L"+P+"GameHub;Ljava/lang/Object;Ljava/util/List;)Z"));m.instructions.add(new InsnNode(IRETURN));counts.merge("admission",1,Integer::sum);
       }
       for(var n:m.instructions.toArray()){
        if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.name.equals("botHp")&&f.owner.equals(P+"GameHub$Room")&&n.getPrevious().getOpcode()==ICONST_0){
         // room,value -> room,room,hub -> room,newValue. Damage/death stores are untouched.
         m.instructions.remove(n.getPrevious());var i=new InsnList();i.add(new InsnNode(DUP));self(i);i.add(call("botHp","(Ljava/lang/Object;L"+P+"GameHub;)I"));m.instructions.insertBefore(n,i);counts.merge("hp",1,Integer::sum);
        }
        if(Set.of("winningForce","forceWinner").contains(m.name)&&n instanceof MethodInsnNode x&&x.owner.equals("java/util/List")&&x.name.equals("size")&&n.getNext().getOpcode()==ICONST_M1){
         var i=new InsnList();self(i);i.add(new VarInsnNode(LLOAD,1));i.add(call("botSingle","(L"+P+"GameHub;J)I"));m.instructions.insertBefore(n.getNext(),i);m.instructions.remove(n.getNext().getNext().getNext().getNext());counts.merge("winner",1,Integer::sum);
        }
       }
      } else {
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals(P+"GameHub")&&Set.of("leaveRoom","disconnect").contains(x.name)){
        var i=new InsnList();self(i);i.add(call("restoreBot","(L"+P+"ClientSession;)V"));m.instructions.insert(n,i);counts.merge("restore",1,Integer::sum);
       }
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals(P+"BattleRules$Mode")&&x.name.equals("usesArenaBot")){
        var i=new InsnList();String d;
        if(Set.of("findUser","sendRoomUser","joinRoom","sendRoomWho").contains(m.name)){
         hub(i);
         if(m.name.equals("findUser")){i.add(new VarInsnNode(ALOAD,5));i.add(new MethodInsnNode(INVOKEVIRTUAL,P+"GameHub$RoomSnapshot","id","()J",false));}
         else i.add(new VarInsnNode(LLOAD,m.name.equals("sendRoomUser")?3:m.name.equals("joinRoom")?5:4));
         d="(L"+P+"BattleRules$Mode;L"+P+"GameHub;J)Z";
        }else{self(i);d="(L"+P+"BattleRules$Mode;L"+P+"ClientSession;)Z";}
        m.instructions.insertBefore(n,i);m.instructions.set(n,call("bot",d));counts.merge("bot-calls",1,Integer::sum);
       }
       if(m.name.equals("startBattle"))for(var n:m.instructions.toArray())if(n instanceof VarInsnNode v&&v.getOpcode()==ISTORE&&v.var==7){
        var i=new InsnList();i.add(new InsnNode(POP));self(i);i.add(call("activeBot","(L"+P+"ClientSession;)Z"));m.instructions.insertBefore(n,i);counts.merge("bootstrap",1,Integer::sum);
       }
       if(m.name.equals("recordBattleResult")){
        var i=new InsnList();var next=new LabelNode();self(i);i.add(new MethodInsnNode(INVOKESTATIC,P+"DurableBattleSettlement","unranked","(L"+P+"ClientSession;)Z",false));i.add(new JumpInsnNode(IFEQ,next));i.add(new InsnNode(RETURN));i.add(next);m.instructions.insert(i);counts.merge("unranked",1,Integer::sum);
       }
       if(m.name.equals("setReady"))for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN){var i=new InsnList();self(i);i.add(call("feedback","(L"+P+"ClientSession;)V"));m.instructions.insertBefore(n,i);counts.merge("feedback",1,Integer::sum);}
       if(m.name.equals("joinRoom")&&m.desc.equals("([BLjava/lang/String;Z)V"))for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN){var i=new InsnList();self(i);i.add(call("joined","(L"+P+"ClientSession;)V"));m.instructions.insertBefore(n,i);counts.merge("joined",1,Integer::sum);}
      }
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);bytes=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(bytes);o.closeEntry();
   }
   for(var p:generated){o.putNextEntry(new ZipEntry(classes.relativize(p).toString().replace('\\','/')));o.write(Files.readAllBytes(p));o.closeEntry();}
  }
  System.out.println(counts);
  for(var e:Map.of("install",1,"default",1,"admission",1,"hp",4,"winner",2,"bootstrap",1,"unranked",1,"bot-calls",13).entrySet())if(!e.getValue().equals(counts.get(e.getKey())))throw new IllegalStateException("hook mismatch "+e);
 }
}
