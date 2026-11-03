import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Two named consumers only: one read in the existing SQL transaction and random selection. */
public final class PolymorphSourceMethods {
 static final String P="local/lito381/server/",DB=P+"CombatDatabase",CAT=P+"BattlePolymorphCatalog",H=P+"NativePolymorphSource",SN=H+"$Snapshot",FORM=CAT+"$Form";
 static Map<String,ClassNode> transform(JarFile jar,ClassLoader loader)throws Exception{
  var db=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(DB+".class")).readAllBytes());var load=db.methods.stream().filter(m->m.name.equals("load")).findFirst().orElseThrow();int slot=load.maxLocals++;int reads=0,publishes=0;
  var init=new InsnList();init.add(new InsnNode(ACONST_NULL));init.add(new VarInsnNode(ASTORE,slot));load.instructions.insert(init);
  for(var n:load.instructions.toArray()){
   if(n instanceof MethodInsnNode call&&call.owner.equals("java/sql/Connection")&&call.name.equals("rollback")){
    var receiver=NativeSkillConsumptionMethods.prev(call);if(!(receiver instanceof VarInsnNode local)||local.getOpcode()!=ALOAD)throw new AssertionError("connection rollback receiver");
    var code=new InsnList();code.add(new VarInsnNode(ALOAD,local.var));code.add(new MethodInsnNode(INVOKESTATIC,H,"read","(Ljava/sql/Connection;)L"+SN+";",false));code.add(new VarInsnNode(ASTORE,slot));load.instructions.insertBefore(receiver,code);reads++;
   }
   if(n instanceof FieldInsnNode field&&field.getOpcode()==PUTSTATIC&&field.owner.equals(DB)&&field.name.equals("current")){
    var code=new InsnList();code.add(new VarInsnNode(ALOAD,slot));code.add(new MethodInsnNode(INVOKESTATIC,H,"publish","(L"+SN+";)V",false));load.instructions.insert(n,code);publishes++;
   }
  }if(reads!=1||publishes!=1)throw new AssertionError("SQL snapshot hook counts "+reads+"/"+publishes);
  var reframed=NativeSkillConsumptionMethods.frames(db,List.of(load),loader);db.methods.set(db.methods.indexOf(load),NativeSkillConsumptionMethods.method(reframed,load.name,load.desc));
  var catalog=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(CAT+".class")).readAllBytes());var original=catalog.methods.stream().filter(m->m.name.equals("randomForm")).findFirst().orElseThrow();original.name="nativePackagedRandomForm";
  var wrapper=new MethodNode(ASM9,original.access,"randomForm",original.desc,original.signature,null);var fallback=new LabelNode();wrapper.instructions.add(new VarInsnNode(ILOAD,0));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"selectOrNull","(I)L"+FORM+";",false));wrapper.instructions.add(new InsnNode(DUP));wrapper.instructions.add(new JumpInsnNode(IFNULL,fallback));wrapper.instructions.add(new InsnNode(ARETURN));wrapper.instructions.add(fallback);wrapper.instructions.add(new InsnNode(POP));wrapper.instructions.add(new VarInsnNode(ILOAD,0));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,CAT,original.name,original.desc,false));wrapper.instructions.add(new InsnNode(ARETURN));catalog.methods.add(wrapper);
  var framed=NativeSkillConsumptionMethods.frames(catalog,List.of(wrapper),loader);catalog.methods.set(catalog.methods.indexOf(wrapper),NativeSkillConsumptionMethods.method(framed,wrapper.name,wrapper.desc));return Map.of(DB,db,CAT,catalog);
 }
 public static void main(String[]args)throws Exception{
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL(),Path.of(args[3]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   var expected=transform(old,loader);
   if(args[0].equals("splice")){for(var e:expected.entrySet()){var path=Path.of(args[2]).resolve(e.getKey()+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(e.getValue(),false));}return;}
   try(var next=new JarFile(args[2])){for(var e:expected.entrySet()){
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(e.getKey()+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(e.getValue(),true)))throw new AssertionError("unexpected transformed class "+e.getKey());
    var prior=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(e.getKey()+".class")).readAllBytes());
    if(e.getKey().equals(DB)){var method=prior.methods.stream().filter(m->m.name.equals("load")).findFirst().orElseThrow();var patched=actual.methods.stream().filter(m->m.name.equals("load")).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(patched),method);}
    else{actual.methods.removeIf(m->m.name.equals("randomForm"));actual.methods.stream().filter(m->m.name.equals("nativePackagedRandomForm")).findFirst().orElseThrow().name="randomForm";}
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(prior,true)))throw new AssertionError("unrelated methods changed "+e.getKey());
   }System.out.println("PASS two named consumer inverses; unrelated DB/catalog methods preserved");}
  }
 }
}
