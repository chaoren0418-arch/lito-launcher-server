import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Bind every question store to its source/target roles; preserve the original answer protocol. */
public final class ResurrectionAnswerLifeMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",A=P+"NativeResurrectionAnswerLife",FIELD="nativeResurrectionAnswerLease",BODY="nativeResurrectionAnswerBody",CLEAR="nativeRejectResurrectionQuestion";
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static String key(MethodNode m){return m.name+m.desc;}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());
  if(c.fields.stream().anyMatch(f->f.name.equals(FIELD)))throw new AssertionError("lease field already exists");
  c.fields.add(new FieldNode(ASM9,ACC_VOLATILE,FIELD,"L"+A+"$Lease;",null,null));
  var old=c.methods.stream().filter(m->m.name.equals("answerResurrection")).findFirst().orElseThrow();var body=clone(old);body.name=BODY;body.access&=~ACC_PRIVATE;int monitors=0;
  for(var n:body.instructions.toArray())if(n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT){if(!(NativeSkillConsumptionMethods.prev(n)instanceof VarInsnNode v)||v.var!=11)throw new AssertionError("unexpected answer monitor");body.instructions.set(n,new InsnNode(POP));monitors++;}
  if(monitors!=4)throw new AssertionError("answer monitor count "+monitors);
  var wrapper=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));wrapper.instructions.add(new VarInsnNode(ALOAD,0));wrapper.instructions.add(new VarInsnNode(ALOAD,1));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,A,"answer","(L"+PC+";L"+P+"Protocol$AskAnswer;)V",false));wrapper.instructions.add(new InsnNode(RETURN));
  c.methods.set(c.methods.indexOf(old),wrapper);c.methods.add(body);var changed=new LinkedHashSet<MethodNode>();changed.add(wrapper);changed.add(body);
  var clear=new MethodNode(ASM9,0,CLEAR,"()V",null,null);
  for(String field:List.of("pendingResurrection","pendingMassTeleport")){var f=c.fields.stream().filter(x->x.name.equals(field)).findFirst().orElseThrow();clear.instructions.add(new VarInsnNode(ALOAD,0));clear.instructions.add(new InsnNode(ACONST_NULL));clear.instructions.add(new FieldInsnNode(PUTFIELD,PC,field,f.desc));}clear.instructions.add(new InsnNode(RETURN));c.methods.add(clear);changed.add(clear);
  int stores=0;
  for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(PC)&&f.name.equals("pendingResurrection")){
   var hook=new InsnList();hook.add(new InsnNode(DUP2));hook.add(new MethodInsnNode(INVOKESTATIC,A,"capture","(L"+PC+";Ljava/lang/Object;)V",false));m.instructions.insertBefore(n,hook);stores++;changed.add(m);
  }
  if(stores<4)throw new AssertionError("missing original question stores");
  var list=new ArrayList<>(changed);var framed=NativeSkillConsumptionMethods.frames(c,list,loader);for(var method:list)c.methods.set(c.methods.indexOf(method),NativeSkillConsumptionMethods.method(framed,method.name,method.desc));
  return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patched=transform(old,loader);if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(patched,false));return;}
  try(var next=new JarFile(args[2])){
   var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patched,true)))throw new AssertionError("transform mismatch");
   var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());int hooks=0;
   for(var m:actual.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(A)&&call.name.equals("capture")){var dup=NativeSkillConsumptionMethods.prev(n);if(dup.getOpcode()!=DUP2)throw new AssertionError("capture stack");m.instructions.remove(dup);m.instructions.remove(n);hooks++;}
   if(!actual.fields.removeIf(f->f.name.equals(FIELD))||!actual.methods.removeIf(m->m.name.equals(BODY))||!actual.methods.removeIf(m->m.name.equals(CLEAR)))throw new AssertionError("new member shape");
   var original=parent.methods.stream().filter(m->m.name.equals("answerResurrection")).findFirst().orElseThrow();var wrapper=NativeSkillConsumptionMethods.method(actual,original.name,original.desc);actual.methods.set(actual.methods.indexOf(wrapper),original);
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated original PC instructions changed");
   System.out.println("PASS question store hooks="+hooks+"; original PC inverse exact; one pending lease, no global collection");
  }
 }}
}
