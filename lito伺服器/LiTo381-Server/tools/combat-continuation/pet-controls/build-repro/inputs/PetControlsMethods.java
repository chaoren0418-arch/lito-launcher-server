import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Exact native collar/taming capacity and shared character-settings load; no factory or SQL edits. */
public final class PetControlsMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",RULES=P+"CombatRules",HELP=P+"NativePetControls";
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
 static void remove(MethodNode m,AbstractInsnNode start,AbstractInsnNode end){for(var n=start;n!=end;){if(n==null)throw new AssertionError("unbounded slice");var next=n.getNext();m.instructions.remove(n);n=next;}}
 static void charisma(MethodNode m,int character,int result){
  VarInsnNode store=null;for(var n:m.instructions.toArray())if(n instanceof VarInsnNode v&&v.getOpcode()==ISTORE&&v.var==result){if(store!=null)throw new AssertionError("ambiguous charisma slot");store=v;}
  if(store==null)throw new AssertionError("missing charisma store");
  var start=prev(store);if(!(start instanceof MethodInsnNode call&&call.owner.equals("java/lang/Math")&&call.name.equals("max")))throw new AssertionError("original charisma max");
  int seen=0;for(var n=start;n!=null;n=prev(n))if(n instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==character){seen++;if(seen==2){start=n;break;}}
  if(seen!=2)throw new AssertionError("original nullable charisma slice");
  remove(m,start,store);var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,character));code.add(new MethodInsnNode(INVOKESTATIC,HELP,"effectiveCharisma","(L"+PC+";L"+P+"CharacterSummary;)I",false));m.instructions.insertBefore(store,code);
 }
 static ClassNode transform(JarFile jar,String name,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
  if(name.equals(PC)){
   var take=method(c,"canTakeControlledCompanion");VarInsnNode bonus=null;TableSwitchInsnNode branch=null;
   for(var n:take.instructions.toArray()){if(n instanceof TableSwitchInsnNode t){if(branch!=null)throw new AssertionError();branch=t;}if(n instanceof VarInsnNode v&&v.getOpcode()==ISTORE&&v.var==4)bonus=v;}
   if(branch==null||bonus==null||!(prev(branch) instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==3))throw new AssertionError("class bonus switch shape");
   remove(take,prev(branch),bonus);var code=new InsnList();code.add(new VarInsnNode(ILOAD,3));code.add(new MethodInsnNode(INVOKESTATIC,HELP,"configuredBonus","(I)I",false));take.instructions.insertBefore(bonus,code);charisma(take,2,5);changed.add(take);
   var collar=method(c,"usePetCollar");int count=0;
   for(var n:collar.instructions.toArray())if(n instanceof IntInsnNode k&&k.getOpcode()==BIPUSH&&k.operand==6&&prev(k) instanceof VarInsnNode collarValue&&collarValue.getOpcode()==ILOAD&&collarValue.var==8&&k.getNext().getOpcode()==IADD){
    var replacement=new InsnList();replacement.add(new VarInsnNode(ALOAD,7));replacement.add(new MethodInsnNode(INVOKESTATIC,HELP,"configuredBonus","(L"+P+"CharacterSummary;)I",false));collar.instructions.insertBefore(k,replacement);collar.instructions.remove(k);count++;
   }
   if(count!=1)throw new AssertionError("collar bonus shape "+count);charisma(collar,7,8);changed.add(collar);
  }else{
   var load=method(c,"load");int count=0;
   for(var n:load.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(RULES)&&call.name.equals("read")&&prev(n) instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==5){
    var code=new InsnList();code.add(new InsnNode(DUP));code.add(new VarInsnNode(ALOAD,8));code.add(new VarInsnNode(ALOAD,9));code.add(new VarInsnNode(ALOAD,5));code.add(new MethodInsnNode(INVOKESTATIC,HELP,"loadFromProperties","(Ljava/util/Properties;Ljava/util/Map;Ljava/util/Set;Ljava/nio/file/Path;)V",false));load.instructions.insert(n,code);count++;
   }
   if(count!=1)throw new AssertionError("single character properties snapshot "+count);changed.add(load);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var base=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  for(String name:List.of(PC,RULES)){
   var expected=transform(base,name,loader);
   if(args[0].equals("splice")){var file=Path.of(args[2]).resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,NativeSkillConsumptionMethods.bytes(expected,false));continue;}
   try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(name+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("transform differs");var original=NativeSkillConsumptionMethods.read(base.getInputStream(base.getJarEntry(name+".class")).readAllBytes());for(String methodName:name.equals(PC)?List.of("usePetCollar","canTakeControlledCompanion"):List.of("load")){var old=method(original,methodName);actual.methods.set(actual.methods.indexOf(method(actual,methodName)),old);}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(original,true),NativeSkillConsumptionMethods.bytes(actual,true)))throw new AssertionError("unrelated native methods changed");}
  }
  if(args[0].equals("verify"))System.out.println("PASS exact three-method inverse; all remaining native methods unchanged");
 }}
}
