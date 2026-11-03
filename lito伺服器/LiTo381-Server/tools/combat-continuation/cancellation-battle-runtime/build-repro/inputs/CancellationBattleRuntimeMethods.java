import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** One NPC effect call and hub-owned lifecycle; all previous admission logic is preserved. */
public final class CancellationBattleRuntimeMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",HUB=P+"GameHub",HELP=P+"NativeCancellationBattleRuntime";
 static ClassNode transform(JarFile jar,String name,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());
  var changed=new ArrayList<MethodNode>();
  if(name.equals(PC)){
   var m=c.methods.stream().filter(v->v.name.equals("nativeMaterialCancellationBody")).findFirst().orElseThrow();int count=0;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(HUB)&&Set.of("cancelQuestMonsterMagic","cancelTimeAttackActorMagic").contains(call.name)){m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,HELP,call.name,"(L"+HUB+";"+call.desc.substring(1),false));count++;}
   if(count!=2)throw new AssertionError("two committed battle NPC effects required");changed.add(m);
  }else{
   if(c.fields.stream().anyMatch(v->v.name.equals("nativeCancellationBattleRuntime")))throw new AssertionError("field exists");
   c.fields.add(new FieldNode(0,"nativeCancellationBattleRuntime","L"+HELP+"$State;",null,null));
   var m=c.methods.stream().filter(v->v.name.equals("close")&&v.desc.equals("()V")).findFirst().orElseThrow();
   var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new MethodInsnNode(INVOKESTATIC,HELP,"close","(L"+HUB+";)V",false));m.instructions.insert(code);changed.add(m);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  for(String name:List.of(PC,HUB)){
   var expected=transform(old,name,loader);
   if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(expected,false));continue;}
   try(var next=new JarFile(args[2])){
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(name+".class")).readAllBytes());
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("transform mismatch "+name);
    var prior=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(name+".class")).readAllBytes());String method=name.equals(PC)?"nativeMaterialCancellationBody":"close";
    var original=prior.methods.stream().filter(m->m.name.equals(method)).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(NativeSkillConsumptionMethods.method(actual,original.name,original.desc)),original);
    if(name.equals(HUB))actual.fields.removeIf(f->f.name.equals("nativeCancellationBattleRuntime"));
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(prior,true)))throw new AssertionError("unrelated changes "+name);
   }
  }
  System.out.println("PASS NPC effect call / hub close and one field exact inverse");
 }}
}
