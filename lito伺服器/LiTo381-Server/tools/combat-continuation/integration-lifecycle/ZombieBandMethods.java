import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Exact one-constant Source class ID correction; every other member preserved. */
public final class ZombieBandMethods {
 static final String OWNER="local/lito381/server/BattleSpellCatalog";
 static ClassNode read(JarFile jar)throws Exception{var c=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(OWNER+".class")).readAllBytes()).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode next(AbstractInsnNode i){do{i=i.getNext();}while(i!=null&&i.getOpcode()<0);return i;}
 static void patch(ClassNode c){int count=0;for(var m:c.methods)if(m.name.equals("zombieTemplate")&&m.desc.equals("(II)Llocal/lito381/server/BattleSpellCatalog$ZombieTemplate;"))for(var i:m.instructions.toArray())if(i instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==0){var value=next(i);var branch=next(value);if(value.getOpcode()==ICONST_1&&branch.getOpcode()==IF_ICMPNE){m.instructions.set(value,new InsnNode(ICONST_3));count++;}}if(count!=1)throw new AssertionError("exact old wizard-class predicate="+count);}
 public static void main(String[]a)throws Exception{try(var old=new JarFile(a[1])){var c=read(old);patch(c);var expected=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(OWNER+".class");Files.createDirectories(p.getParent());Files.write(p,expected);}else try(var candidate=new JarFile(a[2])){if(!Arrays.equals(expected,bytes(read(candidate))))throw new AssertionError("class band delta differs");}System.out.println("PASS only Source wizard-class constant 1 -> 3 in existing zombieTemplate; all other methods and fields retained");}}
}
