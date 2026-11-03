import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Extend the existing read-only startup transaction; preserve all other methods. */
public final class CancellationNpcMethods {
 static final String P="local/lito381/server/",DB=P+"CombatDatabase",H=P+"NativeCancellationNpcRules",SN=H+"$Snapshot";
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception {
  var db=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(DB+".class")).readAllBytes());
  var load=db.methods.stream().filter(m->m.name.equals("load")).findFirst().orElseThrow();int slot=load.maxLocals++,reads=0,publishes=0;
  var init=new InsnList();init.add(new InsnNode(ACONST_NULL));init.add(new VarInsnNode(ASTORE,slot));load.instructions.insert(init);
  for(var n:load.instructions.toArray()) {
   if(n instanceof MethodInsnNode call&&call.owner.equals("java/sql/Connection")&&call.name.equals("rollback")) {
    var receiver=NativeSkillConsumptionMethods.prev(call);if(!(receiver instanceof VarInsnNode local)||local.getOpcode()!=ALOAD)throw new AssertionError("connection rollback receiver");
    var code=new InsnList();code.add(new VarInsnNode(ALOAD,local.var));code.add(new VarInsnNode(ALOAD,0));code.add(new MethodInsnNode(INVOKESTATIC,H,"read","(Ljava/sql/Connection;L"+P+"ServerConfig;)L"+SN+";",false));code.add(new VarInsnNode(ASTORE,slot));load.instructions.insertBefore(receiver,code);reads++;
   }
   if(n instanceof FieldInsnNode field&&field.getOpcode()==PUTSTATIC&&field.owner.equals(DB)&&field.name.equals("current")) {
    var code=new InsnList();code.add(new VarInsnNode(ALOAD,slot));code.add(new MethodInsnNode(INVOKESTATIC,H,"publish","(L"+SN+";)V",false));load.instructions.insert(n,code);publishes++;
   }
  }
  if(reads!=1||publishes!=1)throw new AssertionError("startup hook counts "+reads+"/"+publishes);
  var framed=NativeSkillConsumptionMethods.frames(db,List.of(load),loader);db.methods.set(db.methods.indexOf(load),NativeSkillConsumptionMethods.method(framed,load.name,load.desc));return db;
 }
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
   var expected=transform(old,loader);
   if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(DB+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(expected,false));return;}
   try(var next=new JarFile(args[2])) {
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(DB+".class")).readAllBytes());
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("unexpected transformed class");
    var prior=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(DB+".class")).readAllBytes());
    var m=prior.methods.stream().filter(x->x.name.equals("load")).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(actual.methods.stream().filter(x->x.name.equals("load")).findFirst().orElseThrow()),m);
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(prior,true)))throw new AssertionError("unrelated database method changed");
    System.out.println("PASS startup load inverse; all other database methods preserved");
   }
  }
 }
}
