import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/",H=P+"DragonCombat",T=P+"ClientSession$FoeSlayerTarget";
 public static void main(String[]a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("e35cb91c43a18a690cc304df8e5a9b2ba15014822261651616ae9420fa8cc5a2"))throw new IllegalStateException("baseline mismatch");
  int wield=0,resolve=0,recheck=0,bind=0,effect=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
    if(e.getName().equals(P+"ClientSession.class") || e.getName().equals(P+"BattleSpellCatalog.class") || e.getName().equals(P+"BattleSpellCatalog$FoeSlayerSpell.class")) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     for(var m:c.methods) {
      if(m.name.equals("resolveFoeSlayerTarget")) {
       var i=new InsnList();var normal=new LabelNode();i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,H,"foeCompanion","(L"+P+"ClientSession;)Ljava/lang/Object;",false));i.add(new TypeInsnNode(CHECKCAST,T));i.add(new InsnNode(DUP));i.add(new JumpInsnNode(IFNULL,normal));i.add(new InsnNode(ARETURN));i.add(normal);i.add(new InsnNode(POP));m.instructions.insert(i);resolve++;
      }
      if(m.name.equals("foeSlayerTargetStillEligible")) {
       var i=new InsnList();var normal=new LabelNode();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(new MethodInsnNode(INVOKEVIRTUAL,T,"objectId","()J",false));i.add(new VarInsnNode(ALOAD,2));i.add(new VarInsnNode(LLOAD,3));for(int v=5;v<=9;v++)i.add(new VarInsnNode(ILOAD,v));
       i.add(new MethodInsnNode(INVOKESTATIC,H,"foeCompanionStillEligible","(L"+P+"ClientSession;JLjava/lang/Object;JIIIII)Z",false));i.add(new JumpInsnNode(IFEQ,normal));i.add(new InsnNode(ICONST_1));i.add(new InsnNode(IRETURN));i.add(normal);m.instructions.insert(i);recheck++;
      }
      for(var n:m.instructions.toArray()) {
       if(m.name.equals("<init>") && n instanceof FieldInsnNode f && f.getOpcode()==PUTFIELD && f.name.equals("dragonSkinStatus")) {
        m.instructions.insertBefore(n,new InsnNode(DUP2));var i=new InsnList();i.add(new InsnNode(SWAP));i.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleDragonSkinStatus","bind","(L"+P+"ClientSession;)V",false));m.instructions.insert(n,i);bind++;
       }
       if(m.name.equals("broadcastWieldState") && n instanceof MethodInsnNode call && call.owner.equals(P+"BattleAnimationCatalog") && call.name.equals("requireWalkAction")){call.name="resolveEquippedWalkAction";wield++;}
       if(c.name.startsWith(P+"BattleSpellCatalog") && n instanceof IntInsnNode v && v.operand==12119){v.operand=6509;effect++;}
       if(n instanceof InvokeDynamicInsnNode v) {
        for(int b=0;b<v.bsmArgs.length;b++) if(v.bsmArgs[b] instanceof String s && s.contains("Dragon Skin")) v.bsmArgs[b]=s.replace("icon=omitted-no-selected-LiTo-packetbox-receiver","icon=native-dragon-skin");
       }
       if(n instanceof LdcInsnNode v && v.cst instanceof String s && s.contains("Dragon Skin") && s.contains("icon=omitted-no-selected-LiTo-packetbox-receiver"))v.cst=s.replace("icon=omitted-no-selected-LiTo-packetbox-receiver","icon=native-dragon-skin");
      }
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    if(e.getName().startsWith(P+"BattleDragonSkinStatus") && e.getName().endsWith(".class"))data=Files.readAllBytes(classes.resolve(e.getName()));
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
   String helper=P+"DragonCombat.class";o.putNextEntry(new ZipEntry(helper));o.write(Files.readAllBytes(classes.resolve(helper)));o.closeEntry();
  }
  if(!List.of(wield,resolve,recheck,bind,effect).equals(List.of(1,1,1,1,2)))throw new IllegalStateException("hook counts "+List.of(wield,resolve,recheck,bind,effect));
  System.out.println("PASS: wield fallback, explicit companion, revalidation, status owner and shared effect hooks exactly once");
 }
}
