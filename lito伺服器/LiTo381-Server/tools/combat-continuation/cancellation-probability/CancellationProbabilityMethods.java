import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Exact two player effect entries; no NPC or lifecycle cancellation call is redirected. */
public final class CancellationProbabilityMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession";
 static final List<String>NAMES=List.of("nativeMaterialCancellationBody","nativeMaterialWorldCancellationBody");
 static final List<String>SLEEP=List.of("usePhantasmSpell","useClassicWorldSleepSpell","useSleepSpell");
 static boolean hasSleep(MethodNode m){for(var n:m.instructions)if(n instanceof MethodInsnNode c&&c.owner.equals(P+"BattleTimedStatus")&&c.name.equals("activateSleep"))return true;return false;}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
  for(int k=0;k<2;k++){
   var name=NAMES.get(k);var m=c.methods.stream().filter(v->v.name.equals(name)).findFirst().orElseThrow();
   int target=k==0?5:2,spell=k==0?2:3,now=k==0?19:5;var points=new ArrayList<MethodInsnNode>();
   for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleTimedStatus")&&call.name.equals("hasteSkillId"))points.add(call);
   if(points.size()!=1)throw new AssertionError("effect entry changed");
   var call=points.get(0);var time=NativeSkillConsumptionMethods.prev(call);var status=NativeSkillConsumptionMethods.prev(time);var load=NativeSkillConsumptionMethods.prev(status);
   if(!(time instanceof VarInsnNode t&&t.var==now&&t.getOpcode()==LLOAD)||!(load instanceof VarInsnNode v&&v.var==target&&v.getOpcode()==ALOAD))throw new AssertionError("effect operands changed");
   var gate=new InsnList();var ok=new LabelNode();gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,target));gate.add(new VarInsnNode(ALOAD,spell));gate.add(new VarInsnNode(LLOAD,now));gate.add(new VarInsnNode(ILOAD,k==0?18:14));gate.add(new MethodInsnNode(INVOKESTATIC,P+"NativeCancellationProbability","gate","(L"+PC+";L"+PC+";L"+P+"BattleSpellCatalog$CancellationSpell;JI)Z",false));gate.add(new JumpInsnNode(IFNE,ok));gate.add(new InsnNode(RETURN));gate.add(ok);m.instructions.insertBefore(load,gate);changed.add(m);
  }
  for(var name:SLEEP){var m=c.methods.stream().filter(v->v.name.equals(name)&&hasSleep(v)).findFirst().orElseThrow();int count=0;for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v&&v.owner.equals(P+"BattleTimedStatus")&&v.name.equals("activateSleep")){m.instructions.insertBefore(v,new IntInsnNode(SIPUSH,name.equals("usePhantasmSpell")?212:66));m.instructions.set(v,new MethodInsnNode(INVOKESTATIC,P+"NativeCancellationSleep","activate","(L"+P+"BattleTimedStatus;JII)Z",false));count++;}if(count!=1)throw new AssertionError("sleep producer changed "+name);changed.add(m);}
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]a)throws Exception{try(var old=new JarFile(a[1]);var loader=new URLClassLoader(new URL[]{Path.of(a[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var expected=transform(old,loader);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(PC+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(expected,false));return;}
  try(var next=new JarFile(a[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("transform mismatch");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());var names=new ArrayList<>(NAMES);names.addAll(SLEEP);for(var name:names){var m=parent.methods.stream().filter(v->v.name.equals(name)&&(!SLEEP.contains(name)||hasSleep(v))).findFirst().orElseThrow();var n=NativeSkillConsumptionMethods.method(actual,m.name,m.desc);actual.methods.set(actual.methods.indexOf(n),m);}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated method changed");System.out.println("PASS two player probability entries and three sleep identities; inverse preserves all other methods");}
 }}
}
