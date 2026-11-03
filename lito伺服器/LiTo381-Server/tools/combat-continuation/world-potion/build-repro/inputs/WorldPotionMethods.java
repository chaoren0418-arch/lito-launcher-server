import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Replace only the proven HEAL phase gate; retain the original item/status/effect body. */
public final class WorldPotionMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",NAME="sourcePoisonBody$useInventoryItemBeforeTravel";
 static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n;}
 static MethodNode body(ClassNode c){return c.methods.stream().filter(m->m.name.equals(NAME)).findFirst().orElseThrow();}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var m=body(c);FieldInsnNode heal=null;
  for(var n:m.instructions)if(n instanceof FieldInsnNode f&&f.owner.equals(P+"InventoryCatalog$Effect")&&f.name.equals("HEAL")){if(heal!=null)throw new AssertionError("multiple HEAL branches");heal=f;}
  if(heal==null)throw new AssertionError("missing HEAL branch");var effectJump=next(heal);var self=next(effectJump);var phase=next(self);var battle=next(phase);var gate=next(battle);
  if(effectJump.getOpcode()!=IF_ACMPNE||!(self instanceof VarInsnNode v)||v.var!=0||v.getOpcode()!=ALOAD||!(phase instanceof FieldInsnNode f)||!f.owner.equals(PC)||!f.name.equals("phase")||!(battle instanceof FieldInsnNode b)||!b.owner.equals(PC+"$Phase")||!b.name.equals("BATTLE")||gate.getOpcode()!=IF_ACMPNE)throw new AssertionError("HEAL phase contract changed");
  m.instructions.set(phase,new MethodInsnNode(INVOKESTATIC,P+"NativeWorldPotionPhase","allowed","(L"+PC+";)Z",false));m.instructions.remove(battle);((JumpInsnNode)gate).setOpcode(IFEQ);
  var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patched=transform(old,loader);if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(patched,false));return;}
  try(var z=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(z.getInputStream(z.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("transform mismatch");var m=body(actual);int restored=0;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeWorldPotionPhase")&&call.name.equals("allowed")){var gate=next(call);if(gate.getOpcode()!=IFEQ)throw new AssertionError();m.instructions.insertBefore(gate,new FieldInsnNode(GETSTATIC,PC+"$Phase","BATTLE","L"+PC+"$Phase;"));m.instructions.set(call,new FieldInsnNode(GETFIELD,PC,"phase","L"+PC+"$Phase;"));((JumpInsnNode)gate).setOpcode(IF_ACMPNE);restored++;}
   var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());if(restored!=1||!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated item/PC change");System.out.println("PASS exact HEAL gate inverse; every other PC instruction unchanged");
  }
 }}
}
