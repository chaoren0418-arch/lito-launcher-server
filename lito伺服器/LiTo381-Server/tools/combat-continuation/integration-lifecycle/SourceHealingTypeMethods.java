import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceHealingTypeMethods {
 static final String P="local/lito381/server/",N=P+"NativeSourceHealingType";static final List<String> OWNERS=List.of(P+"ClientSession",P+"CombatHealing");
 static void patch(ClassNode c){
  boolean single=c.name.equals(P+"ClientSession");String name=single?"nativeSourceControlledOriginal_useHealingSpell":"owned";int count=0;
  for(var m:c.methods)if(m.name.equals(name))for(var node:m.instructions.toArray())if(node instanceof MethodInsnNode call&&call.owner.equals(P+"SourceNpcPhysicalStats")&&call.name.equals("undead")&&call.desc.equals("()I")){
   m.instructions.insertBefore(call,new VarInsnNode(ALOAD,single?16:0));call.setOpcode(INVOKESTATIC);call.owner=N;call.name="undead";call.desc="(L"+P+"SourceNpcPhysicalStats;Ljava/lang/Object;)I";call.itf=false;count++;
  }
  if(count!=(single?1:2))throw new AssertionError("exact existing single/group/Nature undead reads "+count);
 }
 public static void main(String[]a)throws Exception{var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var f=Files.list(Path.of(a[3]))){for(var p:f.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}try(var hierarchy=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(a[1])){FreezeAcceptedMethods.hierarchy=hierarchy;for(var owner:OWNERS){var c=FreezeAcceptedMethods.read(base,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(owner+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var jar=new JarFile(a[2])){if(!Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(jar,owner),false)))throw new AssertionError(owner);}}}System.out.println("PASS three existing single/group/Nature template-undead reads use actual controlled type only in original Source healing cast; original cost/RNG/HP/lifecycle bodies retained");}
}
