import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Small verified splices; no Ref schema or player/pair binding change. */
public final class CompanionRefSceneMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",A=P+"NativeCombatAttacker",REF=A+"$Ref",LIVE=A+"$Live",COM=P+"NativePlayerCommit",B=COM+"$Binding",H=P+"NativeCompanionRefScene",HUB=P+"GameHub";
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(n+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static MethodNode copy(MethodNode m,String name){var q=new MethodNode(m.access,name,m.desc,null,m.exceptions.toArray(String[]::new));m.accept(q);return q;}
 static MethodInsnNode call(String owner,String name,String desc){return new MethodInsnNode(INVOKESTATIC,owner,name,desc,false);}
 static void patch(ClassNode c){
  if(c.name.equals(COM)){int count=0;for(var m:new ArrayList<>(c.methods))if(m.name.equals("caster")&&m.desc.equals("(L"+PC+";L"+B+";)L"+PC+";")){
   var q=copy(m,"nativeRefCompanionCaster");int matches=0;for(var n:q.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(B)&&v.name.equals("matches")){v.setOpcode(INVOKESTATIC);v.owner=H;v.name="binding";v.desc="(L"+B+";L"+PC+";)Z";matches++;}if(matches!=4)throw new AssertionError("owner roster ports "+matches);c.methods.add(q);count++;
  }if(count!=1)throw new AssertionError("caster overload");return;}
  if(!c.name.equals(A))throw new AssertionError(c.name);int captures=0,lives=0;
  for(var m:new ArrayList<>(c.methods)){
   if(m.name.equals("companion")){m.name="nativeRefOriginalCompanion";var q=new MethodNode(m.access,"companion",m.desc,null,null);q.instructions.add(new VarInsnNode(ALOAD,0));q.instructions.add(new VarInsnNode(ALOAD,1));q.instructions.add(call(H,"capture",m.desc));q.instructions.add(new InsnNode(ARETURN));c.methods.add(q);captures++;}
   if(m.name.equals("live")){
    m.name="nativeRefOriginalLive";int caster=0,same=0;
    for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v){
     if(v.owner.equals(COM)&&v.name.equals("caster")){v.name="nativeRefCompanionCaster";caster++;}
     if(v.owner.equals(A)&&v.name.equals("same")&&same++==0){m.instructions.insertBefore(v,new VarInsnNode(ALOAD,3));v.owner=H;v.name="liveSame";v.desc="(L"+REF+";Ljava/lang/Object;L"+P+"BattleTimedStatus;L"+PC+";)Z";}
    }if(caster!=1||same!=3)throw new AssertionError("live companion ports "+caster+" "+same);
    var q=new MethodNode(m.access,"live",m.desc,null,null);var i=q.instructions;var direct=new LabelNode();var start=new LabelNode();var end=new LabelNode();var fail=new LabelNode();
    i.add(new VarInsnNode(ALOAD,1));i.add(call(H,"worldRef","(L"+REF+";)Z"));i.add(new JumpInsnNode(IFEQ,direct));
    i.add(new VarInsnNode(ALOAD,0));i.add(call(H,"hub","(L"+PC+";)L"+HUB+";"));i.add(new InsnNode(DUP));i.add(new VarInsnNode(ASTORE,2));i.add(new InsnNode(MONITORENTER));i.add(start);
    i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(call(A,m.name,m.desc));i.add(new VarInsnNode(ASTORE,3));i.add(new VarInsnNode(ALOAD,2));i.add(new InsnNode(MONITOREXIT));i.add(end);i.add(new VarInsnNode(ALOAD,3));i.add(new InsnNode(ARETURN));
    i.add(fail);i.add(new FrameNode(F_NEW,3,new Object[]{PC,REF,HUB},1,new Object[]{"java/lang/Throwable"}));i.add(new VarInsnNode(ASTORE,4));i.add(new VarInsnNode(ALOAD,2));i.add(new InsnNode(MONITOREXIT));i.add(new VarInsnNode(ALOAD,4));i.add(new InsnNode(ATHROW));
    i.add(direct);i.add(new FrameNode(F_NEW,2,new Object[]{PC,REF},0,new Object[]{}));i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(call(A,m.name,m.desc));i.add(new InsnNode(ARETURN));q.tryCatchBlocks.add(new TryCatchBlockNode(start,end,fail,null));c.methods.add(q);lives++;
   }
  }if(captures!=1||lives!=1)throw new AssertionError("Ref roots");
 }
 public static void main(String[] args)throws Exception{try(var j=new JarFile(args[1])){for(var n:List.of(A,COM)){var c=read(j,n);patch(c);var b=bytes(c);if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(n+".class");Files.createDirectories(p.getParent());Files.write(p,b);}else try(var out=new JarFile(args[2])){if(!Arrays.equals(b,bytes(read(out,n))))throw new AssertionError(n);}}}System.out.println("PASS exact WORLD companion Ref capture/live/roster splices; unchanged Ref and player/pair bindings");}
}
