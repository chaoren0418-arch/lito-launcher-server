import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** One explicit call replacement; inverse validates the entire original ClientSession. */
public final class ReturnNatureTransferMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"NativeReturnNatureTransfer";
 static ClassNode transform(JarFile j)throws Exception{
  var c=NativeSkillConsumptionMethods.read(j.getInputStream(j.getJarEntry(PC+".class")).readAllBytes());int changed=0;
  for(var m:c.methods)if(m.name.equals("nativeMaterialReturnNatureBody"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(PC)&&call.name.equals("releaseCompanionToNature")){
   if(call.getOpcode()!=INVOKEVIRTUAL||!call.desc.equals("(L"+P+"BattleCompanion;L"+P+"BattleCompanion$Snapshot;I)Z"))throw new AssertionError("release signature changed");
   m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"release","(L"+PC+";"+call.desc.substring(1),false));changed++;
  }if(changed!=1)throw new AssertionError("release calls="+changed);return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1])){
  var patched=transform(old);if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(patched,false));return;}
  try(var next=new JarFile(args[2])){
   var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("unapproved ClientSession change");
   var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());
   var prior=original.methods.stream().filter(m->m.name.equals("nativeMaterialReturnNatureBody")).findFirst().orElseThrow();
   var target=actual.methods.stream().filter(m->m.name.equals(prior.name)).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(target),prior);
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(original,true)))throw new AssertionError("inverse mismatch");
   System.out.println("PASS one exact Return-to-Nature release call; full unrelated ClientSession inverse");
  }
 }}
}
