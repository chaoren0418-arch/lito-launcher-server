import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** One NPC ownership guard and six fixed-membership calls; exact inverse of both classes. */
public final class CancellationBattleMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",M=P+"NativeCancellationMaterialAction",A=P+"NativeCancellationBattleAction";
 static final Map<String,String> METHODS=Map.of(PC,"nativeMaterialCancellationBody",M,"battle");
 static ClassNode transform(JarFile jar,ClassLoader loader,String owner)throws Exception {
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(owner+".class")).readAllBytes());
  var m=c.methods.stream().filter(v->v.name.equals(METHODS.get(owner))).findFirst().orElseThrow();
  if(owner.equals(M)){
   var guard=new InsnList();var legacy=new LabelNode();for(int i=0;i<4;i++)guard.add(new VarInsnNode(ALOAD,i));
   guard.add(new MethodInsnNode(INVOKESTATIC,A,"cast",m.desc.substring(0,m.desc.length()-1)+"Z",false));guard.add(new JumpInsnNode(IFEQ,legacy));guard.add(new InsnNode(RETURN));guard.add(legacy);m.instructions.insert(guard);
  }else{
   int count=0;var names=Set.of("roomCombatants","roomMembers","battleRecipients","visibleQuestMonsters","visibleTimeAttackActors");
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"GameHub")&&names.contains(call.name)){
    m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,A,call.name,"(L"+call.owner+";"+call.desc.substring(1),false));count++;
   }
   if(count!=6)throw new AssertionError("NPC membership call shape "+count);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   for(var owner:METHODS.keySet()){
    var expected=transform(old,loader,owner);
    if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(owner+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(expected,false));continue;}
    try(var next=new JarFile(args[2])){
     var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(owner+".class")).readAllBytes());
     if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("unexpected transform "+owner);
     var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(owner+".class")).readAllBytes());var original=parent.methods.stream().filter(m->m.name.equals(METHODS.get(owner))).findFirst().orElseThrow();
     actual.methods.set(actual.methods.indexOf(NativeSkillConsumptionMethods.method(actual,original.name,original.desc)),original);
     if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated method changed "+owner);
    }
   }
  }
  System.out.println("PASS exact battle NPC boundary inverse; player probability and WORLD roots preserved");
 }
}
