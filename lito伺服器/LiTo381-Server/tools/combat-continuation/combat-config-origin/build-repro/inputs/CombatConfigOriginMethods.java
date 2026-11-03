import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class CombatConfigOriginMethods {
 static final String P="local/lito381/server/",C=P+"CombatRules";
 static ClassNode patch(byte[]data){var c=NativeSkillConsumptionMethods.read(data);var m=c.methods.stream().filter(x->x.name.equals("load")).findFirst().orElseThrow();int changed=0;
  for(var instruction:m.instructions.toArray())if(instruction instanceof LdcInsnNode ldc&&"combat.properties".equals(ldc.cst)){
   var source=NativeSkillConsumptionMethods.prev(ldc);if(!(source instanceof MethodInsnNode p)||!p.owner.equals("java/nio/file/Path")||!p.name.equals("getParent"))throw new AssertionError("parent chain");
   source=NativeSkillConsumptionMethods.prev(source);if(!(source instanceof MethodInsnNode abs)||!abs.name.equals("toAbsolutePath"))throw new AssertionError("absolute chain");
   source=NativeSkillConsumptionMethods.prev(source);if(!(source instanceof MethodInsnNode get)||!get.owner.equals(P+"ServerConfig")||!get.name.equals("source"))throw new AssertionError("source chain");
   var start=source;var last=ldc.getNext();if(!(last instanceof MethodInsnNode resolve)||!resolve.name.equals("resolve")||!resolve.desc.equals("(Ljava/lang/String;)Ljava/nio/file/Path;"))throw new AssertionError("resolve chain");
   var end=last.getNext();for(var x=start;x!=end;){var next=x.getNext();m.instructions.remove(x);x=next;}
   m.instructions.insertBefore(end,new MethodInsnNode(INVOKESTATIC,P+"NativeCombatConfigOrigin","entry","(L"+P+"ServerConfig;)Ljava/nio/file/Path;",false));changed++;
  }if(changed!=1)throw new AssertionError("exact origin path");return c;
 }
 public static void main(String[]args)throws Exception{try(var jar=new JarFile(args[1])){byte[]data=jar.getInputStream(jar.getJarEntry(C+".class")).readAllBytes();var expected=patch(data);
  if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(C+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(expected,false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(C+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(expected,true),NativeSkillConsumptionMethods.bytes(actual,true)))throw new AssertionError("unexpected transform");var parent=NativeSkillConsumptionMethods.read(data);var old=parent.methods.stream().filter(x->x.name.equals("load")).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(NativeSkillConsumptionMethods.method(actual,old.name,old.desc)),old);if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("other combat rule method changed");System.out.println("PASS one combat origin path; exact method inverse");}
 }}
}
