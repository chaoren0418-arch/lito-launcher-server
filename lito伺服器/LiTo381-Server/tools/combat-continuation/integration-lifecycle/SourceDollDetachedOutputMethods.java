import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceDollDetachedOutputMethods {
 static final String P="local/lito381/server/",N=P+"NativeSourceDollDetachedOutput",PC=P+"ClientSession",OUT=P+"NativeSourceDollOutput",FINAL="nativeSourceDollDetachedFinalFrame";
 static final List<String> OWNERS=List.of(PC,OUT);static void need(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void patch(ClassNode c){
  int count=0;
  if(c.name.equals(OUT)){
   for(var m:c.methods)if(m.name.equals("send"))for(var n:m.instructions.toArray())if(n instanceof InvokeDynamicInsnNode indy&&indy.name.equals("run")&&indy.desc.equals("(L"+P+"NativeAreaMaterialAction$Context;L"+OUT+"$Output;L"+PC+";L"+PC+";)L"+P+"NativeSelfCastAction$Task;")){
    m.instructions.set(indy,new MethodInsnNode(INVOKESTATIC,N,"task",indy.desc,false));count++;
   }
   need(count==1,"one original doll deferred Task factory; original producer and Pending summon proof unchanged");return;
  }
  var old=c.methods.stream().filter(m->m.name.equals("nativeStaticOriginalFrame")).findFirst().orElseThrow();
  need((old.access&ACC_SYNCHRONIZED)!=0,"original receiver monitor");var sibling=SourceNpcFirstViewMethods.clone(old,FINAL,3);
  for(var n:sibling.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceDollSlow")&&call.name.equals("allowFrame")){
   sibling.instructions.insertBefore(call,new VarInsnNode(ALOAD,3));call.owner=N;call.name="allow";call.desc="(L"+PC+";[BLjava/lang/Object;)Z";count++;
  }
  need(count==1,"one explicit doll proof guard at exact original position before cipher/socket");c.methods.add(sibling);
 }
 public static void main(String[]a)throws Exception{
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var f=Files.list(Path.of(a[3]))){for(var p:f.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var old=new JarFile(a[1])){
   FreezeAcceptedMethods.hierarchy=loader;
   for(var owner:OWNERS){var c=FreezeAcceptedMethods.read(old,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);
    if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(owner+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var jar=new JarFile(a[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(jar,owner),false)),"same frozen detached doll output splice");}
   }
   if(a[0].equals("splice"))Files.write(Path.of(a[2]).getParent().resolve("doll-detached-output-ports.tsv"),List.of("owner\tmethod\tphase",OUT+"\tsend\toriginal deferred factory operands retained; producer Context ignored by fieldless helper",PC+"\t"+FINAL+"\tcomplete original synchronized receiver frame with explicit doll proof at original doll guard"));
  }System.out.println("PASS detached doll output original factory and complete original synchronized frame");
 }
}
