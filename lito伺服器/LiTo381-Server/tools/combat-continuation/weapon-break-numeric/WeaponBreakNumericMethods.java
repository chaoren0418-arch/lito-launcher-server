import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;
/** Replace one numeric method; C0 and inverse retain all other class contracts. */
public final class WeaponBreakNumericMethods {
 static final String KEY="local/lito381/server/BattleWeaponBreak.class";
 static ClassNode read(byte[]bytes){return NativeSkillConsumptionMethods.read(bytes);}
 static byte[] norm(ClassNode node){return NativeSkillConsumptionMethods.bytes(node,true);}
 static MethodNode target(ClassNode node){return NativeSkillConsumptionMethods.method(node,"damageAfterStacks","(II)I");}
 public static void main(String[]args)throws Exception{
  try(var jar=new JarFile(args[1])){
   byte[]priorBytes=jar.getInputStream(jar.getJarEntry(KEY)).readAllBytes();var old=read(priorBytes);var c0=read(Files.readAllBytes(Path.of(args[3]).resolve(KEY)));
   if(!Arrays.equals(norm(read(priorBytes)),norm(c0)))throw new AssertionError("C0 semantic class mismatch");
   var replacement=read(Files.readAllBytes(Path.of(args[4]).resolve(KEY)));var originalMethod=target(old);old.methods.set(old.methods.indexOf(originalMethod),target(replacement));
   if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(KEY);Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(old,false));return;}
   try(var next=new JarFile(args[2])){
    var actual=read(next.getInputStream(next.getJarEntry(KEY)).readAllBytes());if(!Arrays.equals(norm(actual),norm(old)))throw new AssertionError("unexpected projected numeric class");
    actual.methods.set(actual.methods.indexOf(target(actual)),target(read(priorBytes)));if(!Arrays.equals(norm(actual),norm(read(priorBytes))))throw new AssertionError("unrelated method changed");
    for(var entry:jar.stream().toList())if(!entry.getName().equals(KEY)&&!Arrays.equals(jar.getInputStream(entry).readAllBytes(),next.getInputStream(next.getJarEntry(entry.getName())).readAllBytes()))throw new AssertionError("unrelated entry changed "+entry.getName());
    if(jar.size()!=next.size())throw new AssertionError("extra entries");
   }
   System.out.println("PASS semantic C0, exact single-method inverse and every other parent entry byte-identical");
  }
 }
}
