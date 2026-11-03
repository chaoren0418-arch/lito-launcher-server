import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Bind every question store to its source/target roles; preserve the original answer protocol. */
public final class MassTeleportAnswerLifeMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",A=P+"NativeMassTeleportAnswerLife",FIELD="nativeMassTeleportAnswerLease",BODY="nativeMassTeleportAnswerBody",CLEAR="nativeRejectMassTeleportQuestion";
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static String key(MethodNode m){return m.name+m.desc;}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());
  if(c.fields.stream().anyMatch(f->f.name.equals(FIELD)))throw new AssertionError("lease field already exists");
  c.fields.add(new FieldNode(ASM9,ACC_VOLATILE,"nativeMassTeleportRoleToken","Ljava/lang/Object;",null,null));
  c.fields.add(new FieldNode(ASM9,ACC_VOLATILE,FIELD,"L"+A+"$Lease;",null,null));
  var old=c.methods.stream().filter(m->m.name.equals("answerMassTeleport")).findFirst().orElseThrow();var body=clone(old);body.name=BODY;body.access&=~ACC_PRIVATE;int monitors=0;
  for(var n:body.instructions.toArray())if(n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT){if(!(NativeSkillConsumptionMethods.prev(n)instanceof VarInsnNode v)||!Set.of(3,10,11).contains(v.var))throw new AssertionError("unexpected answer monitor");body.instructions.set(n,new InsnNode(POP));monitors++;}
  if(monitors!=9)throw new AssertionError("answer monitor count "+monitors);
  int scenes=0;
  for(var n:body.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(PC)&&(f.name.equals("classicWorldMapId")||f.name.equals("classicWorldInstanceId"))&&NativeSkillConsumptionMethods.prev(n) instanceof VarInsnNode v&&v.var==3){
   var first=NativeSkillConsumptionMethods.prev(n);var a=n.getNext();var method=a.getNext();var branch=method.getNext();
   if(!(a instanceof VarInsnNode load)||load.var!=2||!(method instanceof MethodInsnNode call)||!call.owner.equals(PC+"$PendingMassTeleport")||!call.name.equals(f.name.equals("classicWorldMapId")?"sourceMapId":"sourceInstanceId")||!(branch instanceof JumpInsnNode jump)||jump.getOpcode()!=IF_ICMPNE)throw new AssertionError("caster scene slice");
   for(var x:new AbstractInsnNode[]{first,n,a,method,branch})body.instructions.remove(x);scenes++;
  }
  if(scenes!=2)throw new AssertionError("caster scene gates "+scenes);
  int sends=0;
  for(var n:body.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(PC)&&call.name.equals("sendEncrypted")){body.instructions.set(n,new MethodInsnNode(INVOKESTATIC,A,"publish","(L"+PC+";[BLjava/lang/String;)V",false));sends++;}
  if(sends!=1)throw new AssertionError("original answer broadcast");
  var wrapper=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));wrapper.instructions.add(new VarInsnNode(ALOAD,0));wrapper.instructions.add(new VarInsnNode(ALOAD,1));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,A,"answer","(L"+PC+";L"+P+"Protocol$AskAnswer;)V",false));wrapper.instructions.add(new InsnNode(RETURN));
  c.methods.set(c.methods.indexOf(old),wrapper);c.methods.add(body);var changed=new LinkedHashSet<MethodNode>();changed.add(wrapper);changed.add(body);
  var clear=new MethodNode(ASM9,0,CLEAR,"()V",null,null);
  for(String field:List.of("pendingMassTeleport")){var f=c.fields.stream().filter(x->x.name.equals(field)).findFirst().orElseThrow();clear.instructions.add(new VarInsnNode(ALOAD,0));clear.instructions.add(new InsnNode(ACONST_NULL));clear.instructions.add(new FieldInsnNode(PUTFIELD,PC,field,f.desc));}clear.instructions.add(new InsnNode(RETURN));c.methods.add(clear);changed.add(clear);
  int stores=0;
  for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(PC)&&f.name.equals("pendingMassTeleport")){
   var hook=new InsnList();hook.add(new InsnNode(DUP2));hook.add(new MethodInsnNode(INVOKESTATIC,A,"capture","(L"+PC+";Ljava/lang/Object;)V",false));m.instructions.insertBefore(n,hook);stores++;changed.add(m);
  }
  if(stores<4)throw new AssertionError("missing original question stores");
  var list=new ArrayList<>(changed);var framed=NativeSkillConsumptionMethods.frames(c,list,loader);for(var method:list)c.methods.set(c.methods.indexOf(method),NativeSkillConsumptionMethods.method(framed,method.name,method.desc));
  return c;
 }
 static final String COMMIT=P+"NativePlayerCommit";
 static ClassNode commit(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(COMMIT+".class")).readAllBytes());var m=c.methods.stream().filter(x->x.name.equals("selected")).findFirst().orElseThrow();int hooks=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeDeItemApplication")&&call.name.equals("retireClient")){
   var hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(new MethodInsnNode(INVOKESTATIC,A,"retireRole","(L"+PC+";)V",false));m.instructions.insert(n,hook);hooks++;
  }
  if(hooks!=1)throw new AssertionError("selected role authority");var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patched=transform(old,loader);if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(patched,false));Files.write(Path.of(args[2]).resolve(COMMIT+".class"),NativeSkillConsumptionMethods.bytes(commit(old,loader),false));return;}
  try(var next=new JarFile(args[2])){
   var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("transform mismatch");
   var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());int hooks=0;
   for(var m:actual.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(A)&&call.name.equals("capture")){var dup=NativeSkillConsumptionMethods.prev(n);if(dup.getOpcode()!=DUP2)throw new AssertionError("capture stack");m.instructions.remove(dup);m.instructions.remove(n);hooks++;}
   if(!actual.fields.removeIf(f->f.name.equals(FIELD)||f.name.equals("nativeMassTeleportRoleToken"))||!actual.methods.removeIf(m->m.name.equals(BODY))||!actual.methods.removeIf(m->m.name.equals(CLEAR)))throw new AssertionError("new member shape");
   var original=parent.methods.stream().filter(m->m.name.equals("answerMassTeleport")).findFirst().orElseThrow();var wrapper=NativeSkillConsumptionMethods.method(actual,original.name,original.desc);actual.methods.set(actual.methods.indexOf(wrapper),original);
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated original PC instructions changed");
   var cm=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(COMMIT+".class")).readAllBytes());
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(cm,true),NativeSkillConsumptionMethods.bytes(commit(old,loader),true)))throw new AssertionError("role transform mismatch");
   for(var m:cm.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(A)&&call.name.equals("retireRole")){m.instructions.remove(NativeSkillConsumptionMethods.prev(n));m.instructions.remove(n);}
   var originalCommit=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(COMMIT+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(cm,true),NativeSkillConsumptionMethods.bytes(originalCommit,true)))throw new AssertionError("unrelated commit instructions");
   System.out.println("PASS question store hooks="+hooks+"; original PC inverse exact; one pending lease, no global collection");
  }
 }}
}
