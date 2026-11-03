import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class WorldMaterialPolicyMethods {
 static final String P="local/lito381/server/",TYPE=P+"NativeCastMaterialPolicy";
 static ClassNode transform(JarFile jar)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(TYPE+".class")).readAllBytes());var old=c.methods.stream().filter(m->m.name.equals("gm")).findFirst().orElseThrow();old.name="nativeOriginalGm";
  var m=new MethodNode(ASM9,old.access,"gm",old.desc,old.signature,old.exceptions.toArray(String[]::new));m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"NativeWorldMaterialPolicy","gm",old.desc,false));m.instructions.add(new InsnNode(IRETURN));m.maxLocals=1;m.maxStack=1;c.methods.add(m);return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1])){
  var expected=transform(old);if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(TYPE+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(expected,false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(TYPE+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("policy wrapper mismatch");actual.methods.removeIf(m->m.name.equals("gm"));actual.methods.stream().filter(m->m.name.equals("nativeOriginalGm")).findFirst().orElseThrow().name="gm";var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(TYPE+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(original,true)))throw new AssertionError("unrelated policy logic changed");System.out.println("PASS original role policy method preserved; scoped preparation only");}
 }}
}
