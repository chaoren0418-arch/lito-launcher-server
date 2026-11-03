import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Two exact original roots: retain the Source target/probability/effect logic and replace material ownership. */
public final class WeaknessMaterialMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"NativeCastMaterial",A=P+"NativeWeaknessMaterialAction";
 record Spec(String name,String body,String helper,String spell,int monitor,int copy,int previous,int now,int target,int monitorCount){}
 static final List<Spec> SPECS=List.of(new Spec("useWeaknessSpell","nativeMaterialWeaknessBody","battle","WeaknessSpell",48,56,42,44,14,12),new Spec("useClassicWorldWeaknessSpell","nativeMaterialWorldWeaknessBody","world","WeaknessSpell",47,55,41,43,12,10));
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static void remove(MethodNode m,AbstractInsnNode first,AbstractInsnNode end){for(var n=first;n!=end;){if(n==null)throw new AssertionError("unbounded slice");var next=n.getNext();m.instructions.remove(n);n=next;}}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
  for(var s:SPECS){
   var old=c.methods.stream().filter(m->m.name.equals(s.name)).findFirst().orElseThrow();var m=clone(old);m.name=s.body;m.access&=~ACC_PRIVATE;
   MethodInsnNode consume=null,replace=null,delta=null,save=null;FieldInsnNode previousInventory=null;int monitors=0;
   for(var n:m.instructions.toArray()){
    if(n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT){if(!(prev(n) instanceof VarInsnNode v)||(v.var!=s.monitor&&v.var!=s.monitor+1))throw new AssertionError("unknown monitor");m.instructions.set(n,new InsnNode(POP));monitors++;}
    if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"CombatDatabase")&&call.name.equals("consumeMaterial"))consume=call;if(call.owner.equals(P+"NativeDeItemApplication")&&call.name.equals("replace"))replace=call;if(call.owner.equals(PC)&&call.name.equals("sendInventoryDelta"))delta=call;if(call.owner.equals(P+"GameStateStore")&&call.name.equals("saveEconomyState"))save=call;}
    if(n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==s.previous&&prev(n) instanceof FieldInsnNode f&&f.owner.equals(PC)&&f.name.equals("inventory"))previousInventory=f;
   }
   if(monitors!=s.monitorCount||consume==null||replace==null||delta==null||save==null||previousInventory==null)throw new AssertionError(s.name+" handler shape "+monitors);
   AbstractInsnNode start=consume;for(int i=0;i<6;i++)start=prev(start);if(!(start instanceof VarInsnNode copyReceiver&&copyReceiver.getOpcode()==ALOAD&&copyReceiver.var==s.copy))throw new AssertionError("material argument slice");var end=consume.getNext();remove(m,start,end);
   var gate=new InsnList();var accepted=new LabelNode();gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,s.target));
   for(String accessor:List.of("skillId","actionId","reuseDelayMillis")){gate.add(new VarInsnNode(ALOAD,2));gate.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$"+s.spell,accessor,"()I",false));}
   gate.add(new MethodInsnNode(INVOKESTATIC,A,"admit","(L"+PC+";L"+PC+";III)Z",false));gate.add(new JumpInsnNode(IFNE,accepted));gate.add(new InsnNode(RETURN));gate.add(accepted);gate.add(new MethodInsnNode(INVOKESTATIC,"java/lang/System","nanoTime","()J",false));gate.add(new VarInsnNode(LSTORE,s.now));gate.add(new InsnNode(ICONST_1));m.instructions.insertBefore(end,gate);
   m.instructions.set(previousInventory,new MethodInsnNode(INVOKESTATIC,H,"previous","(L"+PC+";)L"+P+"RuntimeInventory;",false));
   start=replace;for(int i=0;i<3;i++)start=prev(start);if(!(start instanceof VarInsnNode replaceReceiver&&replaceReceiver.getOpcode()==ALOAD&&replaceReceiver.var==0))throw new AssertionError("replace receiver");remove(m,start,replace.getNext());
   var inventory=prev(delta);if(!(inventory instanceof FieldInsnNode deltaField&&deltaField.name.equals("inventory")))throw new AssertionError("delta inventory");m.instructions.set(inventory,new MethodInsnNode(INVOKESTATIC,H,"next","(L"+PC+";)L"+P+"RuntimeInventory;",false));
   start=save;while((start=prev(start))!=null)if(start instanceof FieldInsnNode f&&f.owner.equals(PC)&&f.name.equals("state")){start=prev(start);break;}if(!(start instanceof VarInsnNode stateReceiver&&stateReceiver.var==0&&stateReceiver.getOpcode()==ALOAD))throw new AssertionError("save receiver");remove(m,start,save.getNext());
   m.tryCatchBlocks.removeIf(t->m.instructions.indexOf(t.start)<0||m.instructions.indexOf(t.end)<0||m.instructions.indexOf(t.handler)<0);
   var wrapper=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));for(int i=0;i<3;i++)wrapper.instructions.add(new VarInsnNode(ALOAD,i));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,A,s.helper,"(L"+PC+";"+old.desc.substring(1),false));wrapper.instructions.add(new InsnNode(RETURN));c.methods.set(c.methods.indexOf(old),wrapper);c.methods.add(m);changed.add(wrapper);changed.add(m);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var method:changed)c.methods.set(c.methods.indexOf(method),NativeSkillConsumptionMethods.method(framed,method.name,method.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patched=transform(old,loader);if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(patched,false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("transform mismatch");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());for(var s:SPECS){var original=parent.methods.stream().filter(m->m.name.equals(s.name)).findFirst().orElseThrow();var wrapper=NativeSkillConsumptionMethods.method(actual,original.name,original.desc);actual.methods.set(actual.methods.indexOf(wrapper),original);if(!actual.methods.removeIf(m->m.name.equals(s.body)))throw new AssertionError();}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated PC changed");System.out.println("PASS two negative roots inverse; target, probability, effect and resource rejection tails preserved");}
 }}
}
