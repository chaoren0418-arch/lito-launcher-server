import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** One existing WORLD clock port; preserve the complete original method and all BATTLE paths. */
public final class CompanionExpiryMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession";
 static ClassNode read(JarFile j)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(PC+".class"))).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static void patch(ClassNode c){int count=0;for(var m:c.methods)if(m.name.equals("serverClassicWorldCompanionTick")&&m.desc.equals("(IJ)V")){var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(LLOAD,2));i.add(new MethodInsnNode(INVOKESTATIC,P+"NativeCompanionExpiry","tick","(L"+PC+";J)V",false));m.instructions.insert(i);count++;}if(count!=1)throw new AssertionError("one WORLD timer port");}
 public static void main(String[]args)throws Exception{try(var j=new JarFile(args[1])){var c=read(j);patch(c);var expected=bytes(c);if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(out.getParent());Files.write(out,expected);}else try(var candidate=new JarFile(args[2])){if(!Arrays.equals(expected,bytes(read(candidate))))throw new AssertionError("exact single WORLD clock projection");}}System.out.println("PASS one WORLD timer prefix; every existing body/field/BATTLE path preserved");}
}
