import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Exact original terminal, factory and retry ports; no whole ClientSession compilation. */
public final class SummonTerminalMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",PET=P+"BattleCompanion",H=P+"NativeSummonTerminalLifecycle";
 static ClassNode read(JarFile j,String name)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(name+".class"))).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static void patch(ClassNode c){
  if(c.name.equals(PET)){if(c.fields.stream().anyMatch(f->f.name.equals("nativeSummonTerminalStamp")))throw new AssertionError("duplicate terminal field");c.fields.add(new FieldNode(ACC_PRIVATE|ACC_VOLATILE,"nativeSummonTerminalStamp","L"+P+"NativeCompanionCallbacks$Stamp;",null,null));return;}
  int gates=0,factories=0,retries=0;
  for(var m:c.methods){
   if(c.name.equals(PC)&&m.name.equals("releaseCompanionToNature"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode i&&i.owner.equals(PET+"$Snapshot")&&i.name.equals("active")){
    var load=prev(n);if(!(load instanceof VarInsnNode v)||v.var!=4)throw new AssertionError("snapshot gate port");var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,1));m.instructions.insertBefore(load,args);i.setOpcode(INVOKESTATIC);i.owner=H;i.name="releasable";i.desc="(L"+PC+";L"+PET+";L"+PET+"$Snapshot;)Z";gates++;
   }
   if(c.name.equals(PC)&&Set.of("nativeMaterialSummonBody","nativeMaterialElementalSummonBody").contains(m.name))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode i&&i.owner.equals(PET)&&Set.of("summoned","greaterElemental","lesserElemental").contains(i.name)){
    var arm=new InsnList();arm.add(new VarInsnNode(ALOAD,0));arm.add(new MethodInsnNode(INVOKESTATIC,H,"arm","(L"+PET+";L"+PC+";)L"+PET+";",false));m.instructions.insert(n,arm);factories++;
   }
   if(c.name.equals(PC)&&(m.name.equals("serverCompanionTick")&&m.desc.equals("(JJ)V")||m.name.equals("serverClassicWorldCompanionTick")&&m.desc.equals("(IJ)V"))){var in=new InsnList();in.add(new VarInsnNode(ALOAD,0));in.add(new VarInsnNode(LLOAD,m.desc.equals("(JJ)V")?3:2));in.add(new MethodInsnNode(INVOKESTATIC,H,"retry","(L"+PC+";J)V",false));m.instructions.insert(in);retries++;}
   if(c.name.equals(P+"NativeReturnNatureTransfer")&&m.name.startsWith("lambda$release$"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode i&&i.owner.equals(PET+"$Snapshot")&&i.name.equals("active")){
    var snapshot=prev(n);var load=prev(snapshot);if(!(snapshot instanceof MethodInsnNode call)||!call.owner.equals(PET)||!call.name.equals("snapshot")||!(load instanceof VarInsnNode v)||v.var!=3)throw new AssertionError("transfer gate locals");
    m.instructions.insertBefore(load,new VarInsnNode(ALOAD,0));m.instructions.remove(snapshot);i.setOpcode(INVOKESTATIC);i.owner=H;i.name="transferable";i.desc="(L"+PC+";L"+PET+";)Z";gates++;
   }
  }
  if(c.name.equals(PC)?gates!=1||factories!=3||retries!=2:gates!=1||factories!=0||retries!=0)throw new AssertionError(c.name+" gates="+gates+" factories="+factories+" retries="+retries);
 }
 public static void main(String[]a)throws Exception{try(var old=new JarFile(a[1])){for(var name:List.of(PC,PET,P+"NativeReturnNatureTransfer")){var c=read(old,name);patch(c);var expected=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,expected);}else try(var candidate=new JarFile(a[2])){if(!Arrays.equals(expected,bytes(read(candidate,name))))throw new AssertionError("exact regeneration differs "+name);}}}System.out.println("PASS exact terminal active gates, three player factory edges, two existing clock retry edges and one per-companion volatile stamp");}
}
