import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/";
 static MethodInsnNode invoke(String c,String n,String d){return new MethodInsnNode(INVOKESTATIC,P+c,n,d,false);}
 static InsnList guard(String method,String result){var h=new InsnList();h.add(new VarInsnNode(ALOAD,0));h.add(invoke("LiToGmNpcs","active","(L"+P+"BattleTimeAttackEncounter;)Z"));var next=new LabelNode();h.add(new JumpInsnNode(IFEQ,next));h.add(new VarInsnNode(ALOAD,0));if(method.equals("applyDamage")){h.add(new VarInsnNode(ALOAD,1));h.add(new VarInsnNode(ILOAD,2));h.add(new VarInsnNode(ILOAD,3));h.add(invoke("LiToGmNpcs","damage","(L"+P+"BattleTimeAttackEncounter;Ljava/lang/Object;IZ)L"+P+"BattleTimeAttackEncounter$Hit;"));}else{h.add(new VarInsnNode(LLOAD,1));h.add(invoke("LiToGmNpcs","capture","(L"+P+"BattleTimeAttackEncounter;J)L"+P+"BattleTimeAttackEncounter$Capture;"));}h.add(new InsnNode(ARETURN));h.add(next);return h;}
 public static void main(String[] args)throws Exception{
  Path base=Path.of(args[0]),classes=Path.of(args[1]),out=Path.of(args[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("fa059cf8cd3364d86963ad93b6f7901a95a08b804e77b1143b4130991cace01d"))throw new AssertionError("0.6.33 pin");
  int hooks=0;
  try(var input=new ZipFile(base.toFile());var output=new ZipOutputStream(Files.newOutputStream(out))){
   for(var es=input.entries();es.hasMoreElements();){var e=es.nextElement();byte[] b=input.getInputStream(e).readAllBytes();
    if(Set.of(P+"ClientSession.class",P+"GameHub.class",P+"BattleTimeAttackEncounter.class").contains(e.getName())){
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);boolean changed=false;
     for(var m:c.methods){
      if(c.name.equals(P+"ClientSession")&&m.name.equals("handleGmChatCommand")){
       var h=new InsnList();h.add(new VarInsnNode(ALOAD,0));h.add(new VarInsnNode(ALOAD,1));h.add(invoke("LiToGmCommands","handle","(L"+P+"ClientSession;Ljava/lang/String;)Z"));var next=new LabelNode();h.add(new JumpInsnNode(IFEQ,next));h.add(new InsnNode(ICONST_1));h.add(new InsnNode(IRETURN));h.add(next);m.instructions.insert(h);changed=true;hooks++;
      }
      if(c.name.equals(P+"BattleTimeAttackEncounter")){
       if(m.name.equals("applyDamage")||m.name.equals("capture")){m.instructions.insert(guard(m.name,""));changed=true;hooks++;}
       if(m.name.equals("deactivate")){var h=new InsnList();h.add(new VarInsnNode(ALOAD,0));h.add(invoke("LiToGmNpcs","clear","(L"+P+"BattleTimeAttackEncounter;)V"));m.instructions.insert(h);changed=true;hooks++;}
      }
      // Only NPC access/status/AI gates change. Match rules, player targeting,
      // arena bot eligibility, settlement, scoring and mode packets stay original.
      if(c.name.equals(P+"GameHub")&&(m.name.toLowerCase(Locale.ROOT).contains("timeattackactor")||m.name.equals("monsterAiTicks"))){
       for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode mi&&mi.owner.equals(P+"BattleRules$RuleSet")&&mi.name.equals("mode")){
        m.instructions.set(mi,invoke("LiToGmNpcs","combatMode","(L"+P+"BattleRules$RuleSet;)L"+P+"BattleRules$Mode;"));changed=true;hooks++;System.out.println("NPC gate "+m.name);
       }
      }
     }
     if(changed){ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(cw);b=cw.toByteArray();}
    }
    output.putNextEntry(new ZipEntry(e.getName()));output.write(b);output.closeEntry();
   }
   for(String name:List.of("LiToGmCommands","LiToGmNpcs")){output.putNextEntry(new ZipEntry(P+name+".class"));output.write(Files.readAllBytes(classes.resolve(P+name+".class")));output.closeEntry();}
  }
  if(hooks<20)throw new AssertionError("missing GM hooks "+hooks);System.out.println("GM hooks="+hooks);
 }
}
