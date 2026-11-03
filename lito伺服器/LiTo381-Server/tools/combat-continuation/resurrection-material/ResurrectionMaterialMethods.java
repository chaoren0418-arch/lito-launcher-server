import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Preserve both resurrection protocols; replace only their resource/lock boundary. */
public final class ResurrectionMaterialMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"NativeCastMaterial",A=P+"NativeResurrectionMaterialAction",SPELL=P+"BattleSpellCatalog$ResurrectionSpell",RESULT=PC+"$ResurrectionCastCommit";
 record Spec(String root,String body,String helper,String commit,int monitor,int previous,int now){}
 static final List<Spec>SPECS=List.of(new Spec("useResurrectionSpell","nativeMaterialResurrectionBody","battle","commitResurrectionCast",27,9,4),new Spec("useClassicWorldResurrectionSpell","nativeMaterialWorldResurrectionBody","world","commitClassicWorldResurrectionCast",31,13,6));
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static void remove(MethodNode m,AbstractInsnNode first,AbstractInsnNode end){for(var n=first;n!=end;){if(n==null)throw new AssertionError("unbounded slice");var next=n.getNext();m.instructions.remove(n);n=next;}}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
  for(var s:SPECS){
   var old=c.methods.stream().filter(m->m.name.equals(s.root)).findFirst().orElseThrow();var m=clone(old);m.name=s.body;m.access&=~ACC_PRIVATE;
   MethodInsnNode save=null,delta=null;int monitors=0,commits=0;
   for(var n:m.instructions.toArray()){
    if(n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT){if(!(prev(n)instanceof VarInsnNode v)||(v.var!=s.monitor&&v.var!=s.monitor+1))throw new AssertionError("unknown resurrection monitor");m.instructions.set(n,new InsnNode(POP));monitors++;}
    if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"GameStateStore")&&call.name.equals("saveEconomyState"))save=call;if(call.owner.equals(PC)&&call.name.equals("sendInventoryDelta"))delta=call;if(call.owner.equals(PC)&&call.name.equals(s.commit))commits++;}
   }
   if(monitors!=6||commits!=2||save==null||delta==null)throw new AssertionError(s.root+" unexpected root shape "+monitors+"/"+commits);
   var inventory=prev(delta);if(!(inventory instanceof FieldInsnNode deltaField)||!deltaField.owner.equals(PC)||!deltaField.name.equals("inventory"))throw new AssertionError("delta input");m.instructions.set(inventory,new MethodInsnNode(INVOKESTATIC,H,"next","(L"+PC+";)L"+P+"RuntimeInventory;",false));
   AbstractInsnNode start=save;while((start=prev(start))!=null)if(start instanceof FieldInsnNode f&&f.owner.equals(PC)&&f.name.equals("state")){start=prev(start);break;}
   if(!(start instanceof VarInsnNode saveReceiver)||saveReceiver.var!=0)throw new AssertionError("save receiver");remove(m,start,save.getNext());
   var wrapper=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));for(int i=0;i<3;i++)wrapper.instructions.add(new VarInsnNode(ALOAD,i));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,A,s.helper,"(L"+PC+";"+old.desc.substring(1),false));wrapper.instructions.add(new InsnNode(RETURN));c.methods.set(c.methods.indexOf(old),wrapper);c.methods.add(m);changed.add(wrapper);changed.add(m);

   var priorCommit=c.methods.stream().filter(x->x.name.equals(s.commit)).findFirst().orElseThrow();var commit=clone(priorCommit);MethodInsnNode copy=null,replace=null;FieldInsnNode mpWrite=null;int removals=0;
   for(var n:commit.instructions.toArray()){
    if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"RuntimeInventory")&&call.name.equals("copy"))copy=call;if(call.owner.equals(P+"RuntimeInventory")&&call.name.equals("removeCatalog"))removals++;if(call.owner.equals(P+"NativeDeItemApplication")&&call.name.equals("replace"))replace=call;}
    if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(PC)&&f.name.equals("playerMp")){if(mpWrite!=null)throw new AssertionError("multiple MP writes");mpWrite=f;}
   }
   if(copy==null||replace==null||mpWrite==null||removals!=1)throw new AssertionError("commit material shape");
   start=prev(prev(copy));if(!(start instanceof VarInsnNode copyReceiver)||copyReceiver.var!=0)throw new AssertionError("copy receiver");var end=replace.getNext();remove(commit,start,end);
   var gate=new InsnList();var accepted=new LabelNode();gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,2));
   for(String accessor:List.of("skillId","actionId","reuseDelayMillis")){gate.add(new VarInsnNode(ALOAD,1));gate.add(new MethodInsnNode(INVOKEVIRTUAL,SPELL,accessor,"()I",false));}
   gate.add(new MethodInsnNode(INVOKESTATIC,A,"admit","(L"+PC+";L"+PC+";III)Z",false));gate.add(new JumpInsnNode(IFNE,accepted));
   gate.add(new TypeInsnNode(NEW,RESULT));gate.add(new InsnNode(DUP));gate.add(new InsnNode(ICONST_0));gate.add(new InsnNode(ICONST_0));gate.add(new InsnNode(ACONST_NULL));gate.add(new VarInsnNode(ALOAD,0));gate.add(new FieldInsnNode(GETFIELD,PC,"playerMp","I"));gate.add(new MethodInsnNode(INVOKESPECIAL,RESULT,"<init>","(ZZL"+P+"RuntimeInventory;I)V",false));gate.add(new InsnNode(ARETURN));
   gate.add(accepted);gate.add(new MethodInsnNode(INVOKESTATIC,"java/lang/System","nanoTime","()J",false));gate.add(new VarInsnNode(LSTORE,s.now));gate.add(new VarInsnNode(ALOAD,0));gate.add(new MethodInsnNode(INVOKESTATIC,H,"previous","(L"+PC+";)L"+P+"RuntimeInventory;",false));gate.add(new VarInsnNode(ASTORE,s.previous));commit.instructions.insertBefore(end,gate);
   var bound=new InsnList();bound.add(new InsnNode(DUP2));bound.add(new MethodInsnNode(INVOKESTATIC,P+"NativeSkillConsumption","boundMp","(L"+PC+";I)I",false));bound.add(new InsnNode(SWAP));bound.add(new InsnNode(POP));commit.instructions.insertBefore(mpWrite,bound);
   c.methods.set(c.methods.indexOf(priorCommit),commit);changed.add(commit);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var method:changed)c.methods.set(c.methods.indexOf(method),NativeSkillConsumptionMethods.method(framed,method.name,method.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patched=transform(old,loader);if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(patched,false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("transform mismatch");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());for(var s:SPECS){for(String name:List.of(s.root,s.commit)){var original=parent.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();var wrapper=NativeSkillConsumptionMethods.method(actual,original.name,original.desc);actual.methods.set(actual.methods.indexOf(wrapper),original);}if(!actual.methods.removeIf(m->m.name.equals(s.body)))throw new AssertionError();}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated PC changed");System.out.println("PASS resurrection roots/commits inverse; all other PC methods unchanged");}
 }}
}
