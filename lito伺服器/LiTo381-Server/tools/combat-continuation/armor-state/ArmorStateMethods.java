import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Keep all three original state bodies; replace only their lock contract and wrapper. */
public final class ArmorStateMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession";
 static final Map<String,String> NAMES=Map.of("activateClassicWorldArmorBreak","Activate","classicWorldArmorBreakActive","Active","cancelClassicWorldArmorBreak","Cancel");
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
  for(var name:new TreeSet<>(NAMES.keySet())){
   var old=c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();if((old.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("changed baseline lock "+name);
   var body=clone(old);body.name="nativeArmorState"+NAMES.get(name);body.access&=~(ACC_PRIVATE|ACC_SYNCHRONIZED);
   var wrapper=new MethodNode(ASM9,old.access&~ACC_SYNCHRONIZED,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));wrapper.instructions.add(new VarInsnNode(ALOAD,0));int slot=1;
   for(var arg:Type.getArgumentTypes(old.desc)){wrapper.instructions.add(new VarInsnNode(arg.getOpcode(ILOAD),slot));slot+=arg.getSize();}
   String helper=NAMES.get(name).substring(0,1).toLowerCase(Locale.ROOT)+NAMES.get(name).substring(1);wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"NativeArmorState",helper,"(L"+PC+";"+old.desc.substring(1),false));wrapper.instructions.add(new InsnNode(IRETURN));
   c.methods.set(c.methods.indexOf(old),wrapper);c.methods.add(body);changed.add(wrapper);changed.add(body);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var method:changed)c.methods.set(c.methods.indexOf(method),NativeSkillConsumptionMethods.method(framed,method.name,method.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patched=transform(old,loader);if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(patched,false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("transformed state mismatch");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());
   for(var name:NAMES.keySet()){var original=parent.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(NativeSkillConsumptionMethods.method(actual,name,original.desc)),original);if(!actual.methods.removeIf(m->m.name.equals("nativeArmorState"+NAMES.get(name))))throw new AssertionError("body missing");}
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated PC changed");System.out.println("PASS three WORLD Armor Break state methods inverse; original values/expiry/return behavior retained");
  }
 }}
}
