import java.nio.file.*;import java.util.*;import java.util.jar.*;import java.net.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class LeaseMethods implements Opcodes {
 static final String P="local/lito381/server/",PC=P+"ClientSession";
 static ClassNode accepted(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(P+"NativeAcceptedState.class")).readAllBytes());var m=NativeSkillConsumptionMethods.method(c,"reveal","(L"+PC+";Ljava/lang/String;)V");MethodInsnNode cancel=null;JumpInsnNode publication=null;MethodInsnNode boxed=null;int count=0;
  for(var n:m.instructions.toArray()){
   if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleInvisibilityStatus")&&call.name.equals("cancel")){cancel=call;count++;}
   if(n instanceof JumpInsnNode jump&&jump.getOpcode()==IFNULL&&jump.getPrevious() instanceof VarInsnNode v&&v.var==4)publication=jump;
   if(n instanceof MethodInsnNode call&&call.owner.equals("java/lang/Boolean")&&call.name.equals("valueOf"))boxed=call;
  }
  if(count!=1||publication==null||boxed==null)throw new AssertionError("accepted reveal shape changed");
  AbstractInsnNode receiver=cancel.getPrevious(),comparison=receiver.getPrevious();if(!(comparison instanceof JumpInsnNode j&&j.getOpcode()==IF_ACMPNE))throw new AssertionError("missing original generation comparison");
  AbstractInsnNode first=comparison;for(int i=0;i<5;i++)first=first.getPrevious();
  if(!(first instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==6))throw new AssertionError("status receiver moved");
  var replacement=new InsnList();replacement.add(new VarInsnNode(ALOAD,6));replacement.add(new VarInsnNode(ALOAD,3));replacement.add(new FieldInsnNode(GETFIELD,P+"NativeAcceptedState$Incoming","invisible","Ljava/lang/Object;"));replacement.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleInvisibilityStatus","cancelCaptured","(Ljava/lang/Object;)Z",false));m.instructions.insertBefore(first,replacement);
  for(AbstractInsnNode n=first;n!=null;){var next=n.getNext();m.instructions.remove(n);if(n==cancel)break;n=next;}
  var constant=boxed.getPrevious();if(constant.getOpcode()!=ICONST_0)throw new AssertionError("old visible flag changed");var active=new InsnList();active.add(new VarInsnNode(ALOAD,6));active.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleInvisibilityStatus","active","()Z",false));m.instructions.insertBefore(constant,active);m.instructions.remove(constant);
  // Keep the existing completion receipt and lock ordering; a partial removal never reveals a new lease.
  var skip=new InsnList();skip.add(new VarInsnNode(ALOAD,6));skip.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleInvisibilityStatus","active","()Z",false));skip.add(new JumpInsnNode(IFNE,publication.label));AbstractInsnNode loadReceipt=publication.getNext();while(!(loadReceipt instanceof VarInsnNode receipt&&receipt.getOpcode()==ALOAD&&receipt.var==7))loadReceipt=loadReceipt.getNext();m.instructions.insertBefore(loadReceipt,skip);
  var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var m=c.methods.stream().filter(x->x.name.equals("nativeMaterialInvisibilityBody")).findFirst().orElseThrow();int count=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatInvisibilityRules")&&call.name.equals("toggle")){
   var pc=n.getPrevious();var same=pc.getPrevious();if(!(pc instanceof VarInsnNode p&&p.getOpcode()==ALOAD&&p.var==0&&same instanceof VarInsnNode s&&s.getOpcode()==ILOAD&&s.var==14))throw new AssertionError("toggle shape changed");
   var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new FieldInsnNode(GETFIELD,PC,"invisibilityStatus","L"+P+"BattleInvisibilityStatus;"));x.add(new VarInsnNode(ALOAD,2));x.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$InvisibilitySpell","skillId","()I",false));x.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleInvisibilityStatus","hasSkill","(I)Z",false));m.instructions.insertBefore(same,x);m.instructions.remove(same);count++;
  }
  if(count!=1)throw new AssertionError("toggle count "+count);var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   if(args[0].equals("c0")){
    for(String name:List.of("BattleInvisibilityStatus")){
     var actual=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(P+name+".class")).readAllBytes());var source=NativeSkillConsumptionMethods.read(Files.readAllBytes(Path.of(args[2]).resolve(P+name+".class")));
     if(name.equals("BattleInvisibilityStatus")){if(!actual.fields.removeIf(f->f.name.equals("sourceInvisibilityLease")))throw new AssertionError("missing accepted-state token");int hooks=0;for(var m:actual.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeAcceptedState")&&call.name.equals("invisibleChanged")){var prior=n.getPrevious();if(!(prior instanceof VarInsnNode v&&v.var==0&&v.getOpcode()==ALOAD))throw new AssertionError();m.instructions.remove(prior);m.instructions.remove(n);hooks++;}if(hooks!=3)throw new AssertionError("unexpected lease mutation hooks");}
     if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(source,true)))throw new AssertionError("C0 differs "+name);
    }System.out.println("PASS C0: existing accepted-state bridge retained; three original mutation hooks accounted for");return;
   }
   var transformed=transform(old,loader);
   if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(transformed,false));Files.write(Path.of(args[2]).resolve(P+"NativeAcceptedState.class"),NativeSkillConsumptionMethods.bytes(accepted(old,loader),false));return;}
   try(var next=new JarFile(args[2])){
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(transformed,true)))throw new AssertionError("transformation differs");
    var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());var before=original.methods.stream().filter(m->m.name.equals("nativeMaterialInvisibilityBody")).findFirst().orElseThrow();var after=NativeSkillConsumptionMethods.method(actual,before.name,before.desc);actual.methods.set(actual.methods.indexOf(after),before);
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(original,true),NativeSkillConsumptionMethods.bytes(actual,true)))throw new AssertionError("unrelated PC mutation");System.out.println("PASS single-toggle inverse preserves all other PC behavior");
    var name=P+"NativeAcceptedState.class";var oldState=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(name)).readAllBytes());var newState=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(name)).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(newState,true),NativeSkillConsumptionMethods.bytes(accepted(old,loader),true)))throw new AssertionError("accepted-state transformation differs");var oldReveal=oldState.methods.stream().filter(m->m.name.equals("reveal")).findFirst().orElseThrow();var newReveal=NativeSkillConsumptionMethods.method(newState,oldReveal.name,oldReveal.desc);newState.methods.set(newState.methods.indexOf(newReveal),oldReveal);if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(oldState,true),NativeSkillConsumptionMethods.bytes(newState,true)))throw new AssertionError("unrelated accepted-state change");System.out.println("PASS accepted-state inverse: reveal only, publication and lifetime fencing unchanged");
   }
  }
 }
}
