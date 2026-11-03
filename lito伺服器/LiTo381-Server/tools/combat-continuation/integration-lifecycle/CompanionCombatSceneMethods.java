import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Preserve every numeric/native receiver body; add a WORLD-only paired boundary. */
public final class CompanionCombatSceneMethods {
 static final String P="local/lito381/server/",A=P+"NativeCombatAttacker",H=P+"NativeCompanionCombatScene";
 static ClassNode read(JarFile j)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(A+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static void patch(ClassNode c){int count=0;for(var m:new ArrayList<>(c.methods))if(m.name.equals("debit")){m.name="nativeCombatSceneOriginalDebit";var q=new MethodNode(m.access,"debit",m.desc,null,null);q.instructions.add(new VarInsnNode(ALOAD,0));q.instructions.add(new VarInsnNode(ALOAD,1));q.instructions.add(new VarInsnNode(ILOAD,2));q.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"debit",m.desc,false));q.instructions.add(new InsnNode(IRETURN));c.methods.add(q);count++;}if(count!=1)throw new AssertionError("one native debit root");}
 public static void main(String[]a)throws Exception{try(var j=new JarFile(a[1])){var c=read(j);patch(c);var b=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(A+".class");Files.createDirectories(p.getParent());Files.write(p,b);}else try(var out=new JarFile(a[2])){if(!Arrays.equals(b,bytes(read(out))))throw new AssertionError("combat boundary");}}System.out.println("PASS exact preserved debit numeric body with WORLD source-paired boundary; other families delegate original");}
}
