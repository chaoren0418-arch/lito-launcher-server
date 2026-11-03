import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** WORLD-only dispatch to an actor-scene gate; original BATTLE body stays intact. */
public final class CompanionProcSceneMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",C=P+"NativeProcHealingCompanion",PLAN=C+"$Plan",LIVE=C+"$Live",REF=P+"NativeCombatAttacker$Ref",H=P+"NativeCompanionProcScene",HUB=P+"GameHub",PET=P+"BattleCompanion",TIMED=P+"BattleTimedStatus",SNAP=PET+"$Snapshot",B=P+"NativePlayerCommit$Binding";
 static ClassNode read(JarFile j)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(C+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static MethodInsnNode call(String owner,String name,String desc){return new MethodInsnNode(INVOKESTATIC,owner,name,desc,false);}
 static void patch(ClassNode c){int gates=0,meters=0,sends=0;
  for(var m:new ArrayList<>(c.methods)){
   if(m.name.equals("withTarget")){
    m.name="nativeProcSceneOriginalWithTarget";var q=new MethodNode(m.access,"withTarget",m.desc,null,null);var i=q.instructions;var original=new LabelNode();
    i.add(new VarInsnNode(ALOAD,1));i.add(new MethodInsnNode(INVOKEVIRTUAL,PLAN,"target","()L"+REF+";",false));i.add(call(P+"NativeCompanionRefScene","worldRef","(L"+REF+";)Z"));i.add(new JumpInsnNode(IFEQ,original));
    i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(new VarInsnNode(ILOAD,2));i.add(new VarInsnNode(ALOAD,3));i.add(call(H,"withTarget",m.desc));i.add(new InsnNode(ARETURN));
    i.add(original);i.add(new FrameNode(F_NEW,4,new Object[]{HUB,PLAN,INTEGER,"java/util/function/Function"},0,new Object[]{}));i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(new VarInsnNode(ILOAD,2));i.add(new VarInsnNode(ALOAD,3));i.add(call(C,m.name,m.desc));i.add(new InsnNode(ARETURN));c.methods.add(q);gates++;
   }
   if(m.name.equals("publish"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v){
    if(v.owner.equals(B)&&v.name.equals("matches")){v.setOpcode(INVOKESTATIC);v.owner=H;v.name="meterBinding";v.desc="(L"+B+";L"+PC+";)Z";meters++;}
    if(v.owner.equals(P+"NativePlayerCommit")&&v.name.equals("safeSend")){var extra=new InsnList();extra.add(new VarInsnNode(ALOAD,0));extra.add(new VarInsnNode(ALOAD,1));extra.add(new MethodInsnNode(INVOKEVIRTUAL,C+"$Receipt","plan","()L"+PLAN+";",false));extra.add(new MethodInsnNode(INVOKEVIRTUAL,PLAN,"target","()L"+REF+";",false));m.instructions.insertBefore(v,extra);v.owner=H;v.name="sendMeter";v.desc="(L"+PC+";[BLjava/lang/String;L"+HUB+";L"+REF+";)V";sends++;}
   }
  }if(gates!=1||meters!=1||sends!=1)throw new AssertionError("Proc roots "+gates+" "+meters+" "+sends);
  String args="(L"+PC+";L"+PC+";L"+PET+";L"+TIMED+";L"+SNAP+";)";var bridge=new MethodNode(ACC_STATIC,"nativeProcSceneLive",args+"Ljava/lang/Object;",null,null);bridge.instructions.add(new TypeInsnNode(NEW,LIVE));bridge.instructions.add(new InsnNode(DUP));for(int k=0;k<5;k++)bridge.instructions.add(new VarInsnNode(ALOAD,k));bridge.instructions.add(new MethodInsnNode(INVOKESPECIAL,LIVE,"<init>",args+"V",false));bridge.instructions.add(new InsnNode(ARETURN));c.methods.add(bridge);
 }
 public static void main(String[]args)throws Exception{try(var j=new JarFile(args[1])){var c=read(j);patch(c);var b=bytes(c);if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(C+".class");Files.createDirectories(p.getParent());Files.write(p,b);}else try(var out=new JarFile(args[2])){if(!Arrays.equals(b,bytes(read(out))))throw new AssertionError("Proc splices");}}System.out.println("PASS exact WORLD Proc gate/owner meter splice and ephemeral Live bridge; original BATTLE gate retained");}
}
