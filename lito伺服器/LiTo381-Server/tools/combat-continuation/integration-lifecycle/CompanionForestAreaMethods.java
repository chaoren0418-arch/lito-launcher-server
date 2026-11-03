import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** 僅補161的空目標及已接受交易接點；原人物／普通NPC方法與機率本體保留。 */
public final class CompanionForestAreaMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",A=P+"NativeAreaMaterialAction",C=A+"$Context",H=P+"NativeCompanionForestArea",SPELL=P+"BattleSpellCatalog$AreaSilenceSpell";
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(n+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static void patch(ClassNode c){int count=0;
  if(c.name.equals(P+"ForcedCombat")){for(var m:new ArrayList<>(c.methods))if(m.name.equals("unwrap")){m.name="nativeForestOriginalUnwrap";var q=new MethodNode(m.access,"unwrap",m.desc,null,null);q.instructions.add(new VarInsnNode(ALOAD,0));q.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"NativeSelectorOnlyCtrl","unwrap",m.desc,false));q.instructions.add(new InsnNode(ARETURN));c.methods.add(q);count++;}if(count!=1)throw new AssertionError("one unwrap root");return;}
  if(c.name.equals(A)){for(var m:c.methods)if(m.name.equals("run"))for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(P+"NativeCompanionArea")&&v.name.equals("run")){v.owner=H;count++;}if(count!=1)throw new AssertionError("one run port");return;}
  int empty=0,admit=0,alias=0;for(var m:new ArrayList<>(c.methods)){
   if(m.name.equals("combatProbability$10")){var q=new MethodNode(ACC_STATIC,"nativeForestSilenceChance",m.desc,null,m.exceptions.toArray(String[]::new));m.accept(q);c.methods.add(q);alias++;}
   if(m.name.equals("nativeMaterialAreaSilenceBody"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v){
    if(v.owner.equals("java/util/List")&&v.name.equals("isEmpty")&&prev(v) instanceof VarInsnNode load&&load.var==18){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ALOAD,2));m.instructions.insertBefore(v,add);m.instructions.set(v,new MethodInsnNode(INVOKESTATIC,H,"empty","(Ljava/util/List;L"+PC+";L"+SPELL+";)Z",false));empty++;}
    if(v.owner.equals("java/util/List")&&v.name.equals("iterator")&&prev(v) instanceof VarInsnNode load&&load.var==16){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ALOAD,2));for(int local:new int[]{22,23,26,3})add.add(new VarInsnNode(ILOAD,local));add.add(new VarInsnNode(LLOAD,29));add.add(new MethodInsnNode(INVOKESTATIC,H,"apply","(L"+PC+";L"+SPELL+";IIIIJ)V",false));m.instructions.insertBefore(load,add);admit++;}
   }
  }if(empty!=1||admit!=1||alias!=1)throw new AssertionError("161 ports "+empty+" "+admit+" "+alias);

 }
 public static void main(String[]a)throws Exception{try(var j=new JarFile(a[1])){for(var n:List.of(PC,A,P+"ForcedCombat")){var c=read(j,n);patch(c);var b=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(n+".class");Files.createDirectories(p.getParent());Files.write(p,b);}else try(var o=new JarFile(a[2])){if(!Arrays.equals(b,bytes(read(o,n))))throw new AssertionError(n);}}}System.out.println("PASS exact isolated area161 original empty/admission ports, original probability alias/output bridge, existing unchanged NPC dispatch and selector-only Ctrl wrapper; other consumers delegate unchanged");}
}
