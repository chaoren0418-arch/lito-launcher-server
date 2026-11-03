import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;
/** Move only the six original initial-status publication calls into the owned output batch. */
public final class PoisonScopeMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",A=P+"NativePoisonMaterialAction";
 static final Set<String> METHODS=Set.of("nativeMaterialPoisonScopeBody","nativeMaterialWorldPoisonScopeBody","nativeMaterialParalysisScopeBody","nativeMaterialWorldParalysisScopeBody");
 static ClassNode change(ClassNode c,boolean reverse){int count=0;
  for(var m:c.methods)if(METHODS.contains(m.name))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
   if(!reverse){if(call.owner.equals(P+"CombatPoisonLifecycle")&&call.name.equals("palette")){call.owner=A;count++;}
    else if(call.owner.equals(P+"CombatMonsterCurseLifecycle")&&Set.of("send","paletteFinished").contains(call.name)){call.owner=A;call.name=call.name.equals("send")?"monsterSend":"monsterPaletteFinished";count++;}}
   else if(call.owner.equals(A)&&Set.of("palette","monsterSend","monsterPaletteFinished").contains(call.name)){call.owner=P+(call.name.equals("palette")?"CombatPoisonLifecycle":"CombatMonsterCurseLifecycle");if(call.name.equals("monsterSend"))call.name="send";else if(call.name.equals("monsterPaletteFinished"))call.name="paletteFinished";count++;}
  }if(count!=6)throw new AssertionError("scope output call count "+count);return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1])){
  var source=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());
  if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(out.getParent());Files.write(out,NativeSkillConsumptionMethods.bytes(change(source,false),false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(change(actual,true),true),NativeSkillConsumptionMethods.bytes(source,true)))throw new AssertionError("unrelated PC method changed");
   System.out.println("PASS exact six scope publication calls inverse; all other PC instructions retained");}
 }}
}
