import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class CompanionForestCancellationMethods {
 static final String P="local/lito381/server/",B=P+"BattleCompanion";
 static ClassNode read(JarFile j)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(B+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 public static void main(String[]a)throws Exception{try(var j=new JarFile(a[1])){var c=read(j);int count=0;for(var m:c.methods)if(m.name.equals("cancelMagicEffects"))for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(P+"CombatNpcDebuffs")&&v.name.equals("cancel")){v.owner=P+"NativeCompanionForestCancellation";count++;}if(count!=1)throw new AssertionError("one explicit NPC cancellation status port");if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(B+".class");Files.createDirectories(p.getParent());Files.write(p,bytes(c));}else try(var other=new JarFile(a[2])){if(!Arrays.equals(bytes(c),bytes(read(other))))throw new AssertionError("original cancellation body mismatch");}}System.out.println("PASS exact single NPC cancellation call redirect; original body retained");}
}
