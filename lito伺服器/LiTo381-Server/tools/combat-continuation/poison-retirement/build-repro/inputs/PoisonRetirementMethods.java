import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** An old curse removal cannot release the newly selected role. Preserve old lease cleanup. */
public final class PoisonRetirementMethods {
 static final String P="local/lito381/server/",R=P+"CombatCurseLifecycle$Removal",C=P+"CombatCurseLifecycle$Context",L=P+"CombatCurseLifecycle$Curse",T=P+"CombatCurseLifecycle$Target";
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(R+".class")).readAllBytes());
  var m=c.methods.stream().filter(v->v.name.equals("drain")).findFirst().orElseThrow();int count=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatCurseLifecycle$Release")&&call.name.equals("send")){
   var i=new InsnList();var skip=new LabelNode();var done=new LabelNode();
   i.add(new VarInsnNode(ALOAD,0));i.add(new FieldInsnNode(GETFIELD,R,"context","L"+C+";"));i.add(new MethodInsnNode(INVOKEVIRTUAL,C,"valid","()Z",false));i.add(new JumpInsnNode(IFEQ,skip));
   i.add(new VarInsnNode(ALOAD,0));i.add(new FieldInsnNode(GETFIELD,R,"lease","L"+L+";"));i.add(new FieldInsnNode(GETFIELD,L,"target","L"+T+";"));
   i.add(new VarInsnNode(ALOAD,0));i.add(new FieldInsnNode(GETFIELD,R,"context","L"+C+";"));i.add(new FieldInsnNode(GETFIELD,C,"pc","L"+P+"ClientSession;"));i.add(new MethodInsnNode(INVOKEVIRTUAL,T,"valid","(L"+P+"ClientSession;)Z",false));i.add(new JumpInsnNode(IFEQ,skip));
   i.add(new MethodInsnNode(call.getOpcode(),call.owner,call.name,call.desc,call.itf));i.add(new JumpInsnNode(GOTO,done));i.add(skip);i.add(new InsnNode(POP));i.add(done);
   m.instructions.insertBefore(n,i);m.instructions.remove(n);count++;
  }if(count!=1)throw new AssertionError("expected one release delivery: "+count);
  var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var expected=transform(old,loader);if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(R+".class");Files.createDirectories(out.getParent());Files.write(out,NativeSkillConsumptionMethods.bytes(expected,false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(R+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("unexpected transformed removal");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(R+".class")).readAllBytes());var a=actual.methods.stream().filter(m->m.name.equals("drain")).findFirst().orElseThrow();var b=parent.methods.stream().filter(m->m.name.equals("drain")).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(a),b);if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated removal method changed");System.out.println("PASS one release delivery guarded by original context and curse target; inverse equals parent");}
 }}
}
