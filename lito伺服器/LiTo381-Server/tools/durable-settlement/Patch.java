import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes{
 static final String P="local/lito381/server/",D=P+"DurableBattleSettlement",T=P+"TeamDisconnectSettlement";
 static MethodInsnNode call(String owner,String n,String desc){return new MethodInsnNode(INVOKESTATIC,owner,n,desc,false);}
 public static void main(String[] args)throws Exception{
  Path base=Path.of(args[0]),classes=Path.of(args[1]),out=Path.of(args[2]);if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("ebf5ee66f051700ac4b8f524847d7372af8065c4dbb5d91b2f74e03dadff4e7b"))throw new IllegalStateException("baseline");int hooks=0;
  List<Path> generated;try(var files=Files.walk(classes.resolve(P))){generated=files.filter(p->p.toString().endsWith(".class")).toList();}Set<String> replaced=new HashSet<>();for(var p:generated)replaced.add(classes.relativize(p).toString().replace('\\','/'));
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))){
   for(var it=z.entries();it.hasMoreElements();){var e=it.nextElement();if(replaced.contains(e.getName()))continue;byte[] bytes=z.getInputStream(e).readAllBytes();
    if(Set.of(P+"ClientSession.class",P+"GameHub.class").contains(e.getName())){
     var c=new ClassNode();new ClassReader(bytes).accept(c,0);
     for(var m:c.methods){InsnList ins=new InsnList();
      if(c.name.equals(P+"ClientSession")&&m.name.equals("serverPrepareBattle")){ins.add(new VarInsnNode(ALOAD,0));ins.add(call(D,"begin","(L"+P+"ClientSession;)V"));}
      if(c.name.equals(P+"ClientSession")&&m.name.equals("serverEndBattle")){ins.add(new VarInsnNode(ALOAD,0));ins.add(new VarInsnNode(ALOAD,1));ins.add(call(D,"prepare","(L"+P+"ClientSession;L"+P+"BattleRules$Resolution;)L"+P+"BattleRules$Resolution;"));ins.add(new VarInsnNode(ASTORE,1));}
      if(c.name.equals(P+"ClientSession")&&m.name.equals("recordBattleResult")){ins.add(new VarInsnNode(ALOAD,0));ins.add(new VarInsnNode(ILOAD,1));ins.add(call(D,"won","(L"+P+"ClientSession;Z)Z"));ins.add(new VarInsnNode(ISTORE,1));ins.add(new VarInsnNode(ALOAD,0));ins.add(new VarInsnNode(ILOAD,2));ins.add(call(D,"disconnected","(L"+P+"ClientSession;Z)Z"));ins.add(new VarInsnNode(ISTORE,2));}
      if(c.name.equals(P+"GameHub")&&m.name.equals("endBattle")){ins.add(new VarInsnNode(ALOAD,0));ins.add(new VarInsnNode(LLOAD,1));ins.add(call(D,"requireDurable","(L"+P+"GameHub;J)V"));}
      if(c.name.equals(P+"GameHub")&&m.name.equals("resetOrRemove")){LabelNode next=new LabelNode();ins.add(new VarInsnNode(ALOAD,0));ins.add(new VarInsnNode(ALOAD,1));ins.add(new FieldInsnNode(GETFIELD,P+"GameHub$Room","id","J"));ins.add(call(D,"cleanupAllowed","(L"+P+"GameHub;J)Z"));ins.add(new JumpInsnNode(IFNE,next));ins.add(new InsnNode(RETURN));ins.add(next);}
      if(ins.size()>0){m.instructions.insert(ins);hooks++;}
      if(c.name.equals(P+"GameHub")&&m.name.equals("disconnect")){
       for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.name.equals("disconnectResolutionPending")&&f.getOpcode()==GETFIELD&&n.getPrevious() instanceof VarInsnNode roomVar&&n.getNext() instanceof JumpInsnNode jump&&jump.getOpcode()==IFNE){
        var guard=new InsnList();guard.add(new VarInsnNode(ALOAD,0));guard.add(new VarInsnNode(ALOAD,roomVar.var));guard.add(new FieldInsnNode(GETFIELD,P+"GameHub$Room","id","J"));guard.add(call(D,"cleanupAllowed","(L"+P+"GameHub;J)Z"));guard.add(new JumpInsnNode(IFEQ,jump.label));m.instructions.insert(jump,guard);hooks++;
       }
      }
      if(c.name.equals(P+"GameHub")&&m.name.equals("resolveBattleDisconnectSafely")){
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode mc && mc.name.equals("battleDisconnectWork")){
        AbstractInsnNode store=n.getNext();if(!(store instanceof VarInsnNode v)||v.getOpcode()!=ASTORE)throw new IllegalStateException("work local");
        var add=new InsnList();add.add(new VarInsnNode(ALOAD,v.var));add.add(new VarInsnNode(ALOAD,0));add.add(call(T,"freezeWork","(Ljava/lang/Object;L"+P+"GameHub;)Ljava/lang/Object;"));add.add(new InsnNode(POP));m.instructions.insert(store,add);hooks++;
       }
       Set<LabelNode> handlers=new HashSet<>();
       for(var tc:m.tryCatchBlocks)if("java/lang/Throwable".equals(tc.type)&&handlers.add(tc.handler)){
        AbstractInsnNode store=tc.handler;while(!(store instanceof VarInsnNode))store=store.getNext();
        var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(LLOAD,1));add.add(new VarInsnNode(LLOAD,3));add.add(call(T,"retryDisconnect","(L"+P+"GameHub;JJ)V"));m.instructions.insert(store,add);hooks++;
       }
      }
      if(c.name.equals(P+"GameHub")&&m.name.equals("battleDisconnectWork")){
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode mc&&mc.name.equals("resolveBattleDisconnect")){
        var before=new InsnList();before.add(new VarInsnNode(ALOAD,0));before.add(new VarInsnNode(LLOAD,1));m.instructions.insertBefore(n,before);mc.owner=T;mc.name="resolve";mc.desc="(Ljava/util/Set;Ljava/util/Set;L"+P+"GameHub;J)L"+P+"BattleRules$Resolution;";
        var store=n.getNext();if(!(store instanceof VarInsnNode v)||v.var!=9)throw new IllegalStateException("resolution local");
        var add=new InsnList();LabelNode resume=new LabelNode();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(LLOAD,1));add.add(new VarInsnNode(ALOAD,6));add.add(new VarInsnNode(ALOAD,9));add.add(new VarInsnNode(ALOAD,8));add.add(call(T,"continuingWork","(L"+P+"GameHub;JLjava/util/List;L"+P+"BattleRules$Resolution;Ljava/util/Set;)Ljava/lang/Object;"));add.add(new InsnNode(DUP));add.add(new JumpInsnNode(IFNULL,resume));add.add(new TypeInsnNode(CHECKCAST,P+"GameHub$BattleDisconnectWork"));add.add(new InsnNode(ARETURN));add.add(resume);add.add(new InsnNode(POP));m.instructions.insert(store,add);hooks++;
       }
      }
      if(c.name.equals(P+"GameHub")&&m.name.equals("resolveBattleDisconnect"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode mc&&mc.name.equals("size")&&n.getNext().getOpcode()==ICONST_1){m.instructions.set(n.getNext(),new InsnNode(ICONST_M1));hooks++;}
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);bytes=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(bytes);o.closeEntry();
   }
   for(var p:generated){String name=classes.relativize(p).toString().replace('\\','/');o.putNextEntry(new ZipEntry(name));o.write(Files.readAllBytes(p));o.closeEntry();}
  }
  if(hooks!=10)throw new IllegalStateException("hook count "+hooks);System.out.println("Durable settlement hooks="+hooks);
 }
}
