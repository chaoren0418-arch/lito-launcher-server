import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Preserve the existing NPC body; move atomic material admission after its final target recheck. */
public final class CancellationNpcMaterialMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"NativeCastMaterial",A=P+"NativeCancellationNpcMaterialAction";
 record Spec(String name,String body,String helper,int args,int spell,int previous,int now,int target,int count,int monitorCount,Set<Integer> monitors,Set<Integer> copies){}
 static final List<Spec> SPECS=List.of(
 new Spec("sourcePoisonBody$useWorldNpcCancellation","nativeMaterialWorldNpcCancellationBody","worldNpc",8,6,20,25,2,1,10,Set.of(27),Set.of(30)));
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static void remove(MethodNode m,AbstractInsnNode first,AbstractInsnNode end){for(var n=first;n!=end;){if(n==null)throw new AssertionError("unbounded slice");var next=n.getNext();m.instructions.remove(n);n=next;}}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
  for(var s:SPECS){
   var old=c.methods.stream().filter(m->m.name.equals(s.name)).findFirst().orElseThrow();var m=clone(old);m.name=s.body;m.access&=~ACC_PRIVATE;
   var consumes=new ArrayList<MethodInsnNode>();var replaces=new ArrayList<MethodInsnNode>();var deltas=new ArrayList<MethodInsnNode>();var saves=new ArrayList<MethodInsnNode>();var previousInventories=new ArrayList<FieldInsnNode>();int monitors=0;
   for(var n:m.instructions.toArray()){
    if(n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT){if(!(prev(n) instanceof VarInsnNode v)||!s.monitors.contains(v.var))throw new AssertionError("unknown cancellation monitor");m.instructions.set(n,new InsnNode(POP));monitors++;}
    if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"CombatDatabase")&&call.name.equals("consumeMaterial"))consumes.add(call);if(call.owner.equals(P+"NativeDeItemApplication")&&call.name.equals("replace"))replaces.add(call);if(call.owner.equals(PC)&&call.name.equals("sendInventoryDelta"))deltas.add(call);if(call.owner.equals(P+"GameStateStore")&&call.name.equals("saveEconomyState"))saves.add(call);}
    if(n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==s.previous&&prev(n) instanceof FieldInsnNode f&&f.owner.equals(PC)&&f.name.equals("inventory"))previousInventories.add(f);
   }
   if(monitors!=s.monitorCount||consumes.size()!=s.count||replaces.size()!=s.count||deltas.size()!=s.count||saves.size()!=s.count||previousInventories.size()!=s.count)throw new AssertionError(s.name+" shape "+monitors+" "+consumes.size()+" "+replaces.size()+" "+deltas.size()+" "+saves.size()+" "+previousInventories.size());
   for(var consume:consumes){
    AbstractInsnNode start=consume;for(int i=0;i<6;i++)start=prev(start);if(!(start instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&s.copies.contains(v.var)))throw new AssertionError("material argument slice");var end=consume.getNext();remove(m,start,end);
    m.instructions.insertBefore(end,new InsnNode(ICONST_1));
   }
   int effectGates=0;
   for(var instruction:m.instructions.toArray())if(instruction instanceof MethodInsnNode effect&&
       (effect.owner.equals(P+"BattleCompanion")&&effect.name.equals("cancelMagicEffects")||effect.owner.equals(P+"CombatMonsterCurseLifecycle")&&effect.name.equals("magicWorld"))){
    var start=prev(effect);
    if(effect.name.equals("magicWorld")){for(int i=0;i<7;i++)start=prev(start);}
    if(!(start instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==(effect.name.equals("magicWorld")?0:3)))throw new AssertionError("NPC cancellation effect operands");
    var gate=new InsnList();var accepted=new LabelNode();gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,2));
    for(String accessor:List.of("skillId","actionId","reuseDelayMillis")){gate.add(new VarInsnNode(ALOAD,6));gate.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$CancellationSpell",accessor,"()I",false));}
    gate.add(new MethodInsnNode(INVOKESTATIC,A,"admit","(L"+PC+";L"+PC+";III)Z",false));gate.add(new JumpInsnNode(IFNE,accepted));gate.add(new InsnNode(RETURN));gate.add(accepted);gate.add(new MethodInsnNode(INVOKESTATIC,"java/lang/System","nanoTime","()J",false));gate.add(new VarInsnNode(LSTORE,25));m.instructions.insertBefore(start,gate);effectGates++;
   }
   if(effectGates!=2)throw new AssertionError("both NPC admission branches required");
   for(var f:previousInventories)m.instructions.set(f,new MethodInsnNode(INVOKESTATIC,H,"previous","(L"+PC+";)L"+P+"RuntimeInventory;",false));
   for(var replace:replaces){AbstractInsnNode start=replace;for(int i=0;i<3;i++)start=prev(start);if(!(start instanceof VarInsnNode v&&v.var==0&&v.getOpcode()==ALOAD))throw new AssertionError("replace receiver");remove(m,start,replace.getNext());}
   for(var delta:deltas){var inventory=prev(delta);if(!(inventory instanceof FieldInsnNode f&&f.name.equals("inventory")))throw new AssertionError("delta inventory");m.instructions.set(inventory,new MethodInsnNode(INVOKESTATIC,H,"next","(L"+PC+";)L"+P+"RuntimeInventory;",false));}
   for(var save:saves){AbstractInsnNode start=save;while((start=prev(start))!=null)if(start instanceof FieldInsnNode f&&f.owner.equals(PC)&&f.name.equals("state")){start=prev(start);break;}if(!(start instanceof VarInsnNode v&&v.var==0&&v.getOpcode()==ALOAD))throw new AssertionError("save receiver");remove(m,start,save.getNext());}
   m.tryCatchBlocks.removeIf(t->m.instructions.indexOf(t.start)<0||m.instructions.indexOf(t.end)<0||m.instructions.indexOf(t.handler)<0);
   var wrapper=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));for(int i=0;i<s.args;i++)wrapper.instructions.add(new VarInsnNode(ALOAD,i));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,A,s.helper,"(L"+PC+";"+old.desc.substring(1),false));wrapper.instructions.add(new InsnNode(RETURN));c.methods.set(c.methods.indexOf(old),wrapper);c.methods.add(m);changed.add(wrapper);changed.add(m);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var method:changed)c.methods.set(c.methods.indexOf(method),NativeSkillConsumptionMethods.method(framed,method.name,method.desc));return c;
 }
 static final String LIGHT=P+"NativeLightAction";
 static ClassNode light(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(LIGHT+".class")).readAllBytes());var m=c.methods.stream().filter(v->v.name.equals("deferSelfSend")).findFirst().orElseThrow();
  var code=new InsnList();var rest=new LabelNode();for(int i=0;i<3;i++)code.add(new VarInsnNode(ALOAD,i));code.add(new MethodInsnNode(INVOKESTATIC,A,"captureOutput","(L"+PC+";[BLjava/lang/String;)Z",false));code.add(new JumpInsnNode(IFEQ,rest));code.add(new InsnNode(ICONST_1));code.add(new InsnNode(IRETURN));code.add(rest);m.instructions.insert(code);
  var changes=new ArrayList<MethodNode>();changes.add(m);int dispatch=0;for(var method:c.methods)for(var node:method.instructions.toArray())if(node instanceof MethodInsnNode call&&call.owner.equals(P+"NativePetWorldLifetime")&&call.name.equals("dispatch")){call.owner=A;changes.add(method);dispatch++;}if(dispatch!=1)throw new AssertionError("unique early NPC lifetime ingress");
  var framed=NativeSkillConsumptionMethods.frames(c,changes,loader);for(var method:changes)c.methods.set(c.methods.indexOf(method),NativeSkillConsumptionMethods.method(framed,method.name,method.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patched=transform(old,loader);if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(patched,false));Files.write(Path.of(args[2]).resolve(LIGHT+".class"),NativeSkillConsumptionMethods.bytes(light(old,loader),false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("transform mismatch");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());for(var s:SPECS){var original=parent.methods.stream().filter(m->m.name.equals(s.name)).findFirst().orElseThrow();var wrapper=NativeSkillConsumptionMethods.method(actual,original.name,original.desc);actual.methods.set(actual.methods.indexOf(wrapper),original);if(!actual.methods.removeIf(m->m.name.equals(s.body)))throw new AssertionError();}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated PC changed");var expectedLight=light(old,loader);var actualLight=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(LIGHT+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actualLight,true),NativeSkillConsumptionMethods.bytes(expectedLight,true)))throw new AssertionError("Light output hook mismatch");
 var priorLight=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(LIGHT+".class")).readAllBytes());for(var originalLight:priorLight.methods){boolean ingressMethod=false;for(var node:originalLight.instructions)if(node instanceof MethodInsnNode call&&call.owner.equals(P+"NativePetWorldLifetime")&&call.name.equals("dispatch"))ingressMethod=true;if(originalLight.name.equals("deferSelfSend")||ingressMethod)actualLight.methods.set(actualLight.methods.indexOf(NativeSkillConsumptionMethods.method(actualLight,originalLight.name,originalLight.desc)),originalLight);}
 if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actualLight,true),NativeSkillConsumptionMethods.bytes(priorLight,true)))throw new AssertionError("unrelated Light method changed");System.out.println("PASS one NPC body and early ingress and scoped output hooks inverse");}
 }}
}
