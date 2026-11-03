import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceHitReactionMethods {
 static final String P="local/lito381/server/",M=P+"NativeSkillTargetMetadata",C=P+"BattleHitReactionSuppressCatalog",N=P+"NativeSourceHitReaction";
 static final List<String>TARGETS=List.of(M,C);
 static void patch(ClassNode c){
  if(c.name.equals(M)){int count=0;for(var m:c.methods)if(m.name.equals("read"))for(var node:m.instructions.toArray())if(node instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceDollSkillData")&&call.name.equals("read")){
   var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,N,"read","(Ljava/sql/Connection;)V",false));m.instructions.insert(call,x);count++;
  }if(count!=1)throw new AssertionError("same original readonly startup port");return;}
  for(String name:List.of("shouldSendDamageAction","suppressedGfx")){
   var old=c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();old.name=name.equals("shouldSendDamageAction")?"nativeSourceHitReactionOriginalShouldSend":"nativeSourceHitReactionOriginalSuppressedGfx";
   var m=new MethodNode(old.access,name,old.desc,null,old.exceptions.toArray(String[]::new));if(name.equals("shouldSendDamageAction"))m.instructions.add(new VarInsnNode(ILOAD,0));m.instructions.add(new MethodInsnNode(INVOKESTATIC,N,name,m.desc,false));m.instructions.add(new InsnNode(name.equals("shouldSendDamageAction")?IRETURN:ARETURN));c.methods.add(m);
  }
 }
 public static void main(String[]a)throws Exception{var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var f=Files.list(Path.of(a[3]))){for(var p:f.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}try(var hierarchy=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(a[1])){FreezeAcceptedMethods.hierarchy=hierarchy;for(String name:TARGETS){var c=FreezeAcceptedMethods.read(base,name);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var next=new JarFile(a[2])){if(!Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(next,name),false)))throw new AssertionError(name);}}}System.out.println("PASS Source hit reaction readonly startup and catalog ports; original NPC consumers unchanged");}
}
