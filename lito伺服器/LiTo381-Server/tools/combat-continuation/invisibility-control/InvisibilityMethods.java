import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class InvisibilityMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"CombatInvisibilityRules";
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static MethodNode gated(String name,String target,String desc) {
  var m=new MethodNode(ASM9,ACC_PRIVATE,name,desc,null,new String[]{"java/io/IOException"});var x=m.instructions;var run=new LabelNode();x.add(new MethodInsnNode(INVOKESTATIC,H,"breakOnCast","()Z",false));x.add(new JumpInsnNode(IFNE,run));if(desc.equals("()Z"))x.add(new InsnNode(ICONST_0));x.add(new InsnNode(desc.equals("()Z")?IRETURN:RETURN));x.add(run);x.add(new VarInsnNode(ALOAD,0));if(!desc.equals("()Z"))x.add(new VarInsnNode(ALOAD,1));x.add(new MethodInsnNode(INVOKEVIRTUAL,PC,target,desc,false));x.add(new InsnNode(desc.equals("()Z")?IRETURN:RETURN));return m;
 }
 static boolean castReason(MethodInsnNode call) {
  var p=prev(call);
  if(p instanceof LdcInsnNode v&&v.cst.equals("spell-5")){var receiver=prev(p);if(!(receiver instanceof VarInsnNode r&&r.getOpcode()==ALOAD&&r.var==0))throw new AssertionError("nonself constant cast");return true;}
  if(p instanceof InvokeDynamicInsnNode v&&v.bsmArgs.length==1&&(v.bsmArgs[0].equals("spell-\u0001")||v.bsmArgs[0].equals("classic-world-spell-\u0001"))) {
   var argument=prev(p);AbstractInsnNode receiver;
   if(argument instanceof MethodInsnNode getter&&getter.name.equals("skillId")&&getter.desc.equals("()I")){if(!(prev(argument) instanceof VarInsnNode))throw new AssertionError("skill receiver shape");receiver=prev(prev(argument));}
   else if(argument instanceof VarInsnNode r&&r.getOpcode()==ILOAD)receiver=prev(argument);
   else throw new AssertionError("unreviewed spell argument "+argument);
   if(!(receiver instanceof VarInsnNode r&&r.getOpcode()==ALOAD&&r.var==0))throw new AssertionError("nonself cast reveal");return true;
  }
  return false;
 }
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception {
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new LinkedHashSet<MethodNode>();int casts=0;
  for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(PC)&&call.name.equals("revealInvisibility")&&castReason(call)){call.name="nativeSpellRevealControlled";changed.add(m);casts++;}
  if(casts!=105)throw new AssertionError("unexpected cast census "+casts);
  var body=c.methods.stream().filter(m->m.name.equals("nativeMaterialInvisibilityBody")).findFirst().orElseThrow();int stores=0,clears=0,publishes=0;
  for(var n:body.instructions.toArray()) {
   if(n instanceof VarInsnNode v&&v.getOpcode()==ISTORE&&v.var==14){var x=new InsnList();x.add(new VarInsnNode(ILOAD,14));x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"toggle","(ZL"+PC+";)Z",false));x.add(new VarInsnNode(ISTORE,14));body.instructions.insert(n,x);stores++;}
   if(n instanceof MethodInsnNode call&&call.owner.equals(PC)){
    if(call.name.equals("cancelInvisibilityState")){call.name="nativeClearInvisibilityOnCast";clears++;}
    if(call.name.equals("publishInvisibilityReveal")){call.name="nativePublishInvisibilityOnCast";publishes++;}
   }
  }
  if(stores!=1||clears!=1||publishes!=1)throw new AssertionError("native invisibility body changed");changed.add(body);
  for(var m:List.of(gated("nativeSpellRevealControlled","revealInvisibility","(Ljava/lang/String;)V"),gated("nativeClearInvisibilityOnCast","cancelInvisibilityState","()Z"),gated("nativePublishInvisibilityOnCast","publishInvisibilityReveal","(Ljava/lang/String;)V"))){c.methods.add(m);changed.add(m);}
  var framed=NativeSkillConsumptionMethods.frames(c,new ArrayList<>(changed),loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));System.out.println("cast_reveal_calls="+casts+" changed_methods="+changed.size());return c;
 }
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   var transformed=transform(old,loader);
   if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(transformed,false));return;}
   try(var next=new JarFile(args[2])) {
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(transformed,true)))throw new AssertionError("transformation differs");
    var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());
    for(var m:actual.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(PC)&&call.name.equals("nativeSpellRevealControlled"))call.name="revealInvisibility";
    var oldBody=original.methods.stream().filter(m->m.name.equals("nativeMaterialInvisibilityBody")).findFirst().orElseThrow();var body=NativeSkillConsumptionMethods.method(actual,oldBody.name,oldBody.desc);actual.methods.set(actual.methods.indexOf(body),oldBody);actual.methods.removeIf(m->Set.of("nativeSpellRevealControlled","nativeClearInvisibilityOnCast","nativePublishInvisibilityOnCast").contains(m.name));
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(original,true)))throw new AssertionError("unrelated PC mutation");
    System.out.println("PASS inverse preserves all non-cast reveal/clear paths, other bodies and fields");
    var rules="local/lito381/server/CombatRules.class";var beforeRules=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(rules)).readAllBytes());var afterRules=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(rules)).readAllBytes());
    for(String name:List.of("<clinit>","snapshot")){var before=beforeRules.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();var after=NativeSkillConsumptionMethods.method(afterRules,before.name,before.desc);afterRules.methods.set(afterRules.methods.indexOf(after),before);}
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(beforeRules,true),NativeSkillConsumptionMethods.bytes(afterRules,true)))throw new AssertionError("unrelated CombatRules mutation");
    System.out.println("PASS CombatRules inverse preserves every other method and field");
   }
  }
 }
}
