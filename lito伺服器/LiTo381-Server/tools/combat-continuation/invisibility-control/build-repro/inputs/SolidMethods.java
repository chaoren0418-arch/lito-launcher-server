import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SolidMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",ST=P+"SourceEquipmentPhysicalStats",H=P+"SolidCarriageRules";
 static ClassNode transform(JarFile jar,String name,ClassLoader loader)throws Exception {
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());
  String target=name.equals(PC)?"hasSolidCarriageEquipment":"supportsSolidCarriage";
  var old=NativeSkillConsumptionMethods.method(c,target,"()Z");var m=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));var x=m.instructions;
  if(name.equals(PC)) {
   x.add(new VarInsnNode(ALOAD,0));x.add(new FieldInsnNode(GETFIELD,PC,"inventory","L"+P+"RuntimeInventory;"));x.add(new MethodInsnNode(INVOKESTATIC,H,"runtime","(L"+P+"RuntimeInventory;)Z",false));var yes=new LabelNode();x.add(new JumpInsnNode(IFNE,yes));
   x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKEVIRTUAL,PC,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;",false));x.add(new MethodInsnNode(INVOKEVIRTUAL,P+"SourceEquipmentMagicStats","physical","()L"+ST+";",false));x.add(new MethodInsnNode(INVOKEVIRTUAL,ST,"supportsSolidCarriage","()Z",false));x.add(new InsnNode(IRETURN));x.add(yes);x.add(new InsnNode(ICONST_1));x.add(new InsnNode(IRETURN));
  }else {
   x.add(new VarInsnNode(ALOAD,0));x.add(new FieldInsnNode(GETFIELD,ST,"shieldEquipped","Z"));x.add(new VarInsnNode(ALOAD,0));x.add(new FieldInsnNode(GETFIELD,ST,"guarderEquipped","Z"));x.add(new MethodInsnNode(INVOKESTATIC,H,"equipment","(ZZ)Z",false));x.add(new InsnNode(IRETURN));
  }
  c.methods.set(c.methods.indexOf(old),m);var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[] args)throws Exception {
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
   if(args[0].equals("nested")) {
    for(String name:List.of("CombatRules$1","CombatRules$Snapshot")) {
     var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(P+name+".class")).readAllBytes());
     var compiled=NativeSkillConsumptionMethods.read(Files.readAllBytes(Path.of(args[2]).resolve(P+name+".class")));
     if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(original,true),NativeSkillConsumptionMethods.bytes(compiled,true)))throw new AssertionError("nested behavior changed "+name);
    }
    System.out.println("PASS nested classes differ only in debug metadata; retain original bytes");return;
   }
   for(String name:List.of(PC,ST)) {
    var transformed=transform(old,name,loader);
    if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(transformed,false));}
    else try(var jar=new JarFile(args[2])) {
     var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(name+".class")).readAllBytes());var actual=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());
     if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(transformed,true)))throw new AssertionError("transform differs "+name);
     String method=name.equals(PC)?"hasSolidCarriageEquipment":"supportsSolidCarriage";var changed=NativeSkillConsumptionMethods.method(actual,method,"()Z");actual.methods.set(actual.methods.indexOf(changed),NativeSkillConsumptionMethods.method(original,method,"()Z"));
     if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(original,true)))throw new AssertionError("unrelated changes "+name);
    }
   }
   if(args[0].equals("verify"))try(var jar=new JarFile(args[2])) {
    var name=P+"CombatRules.class";var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(name)).readAllBytes());var actual=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(name)).readAllBytes());
    for(String method:List.of("<clinit>","snapshot")){var before=original.methods.stream().filter(m->m.name.equals(method)).findFirst().orElseThrow();var after=NativeSkillConsumptionMethods.method(actual,before.name,before.desc);actual.methods.set(actual.methods.indexOf(after),before);}
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(original,true),NativeSkillConsumptionMethods.bytes(actual,true)))throw new AssertionError("unrelated CombatRules changes");
    System.out.println("PASS CombatRules inverse: only key declaration and snapshot parser changed");
   }
  }
  System.out.println("PASS two-method transformation; inverse preserves all other fields and methods");
 }
}
