import java.nio.file.*;import java.security.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Instructions/declarations only. Does not load or execute either native candidate. */
public final class CompareCandidateMethods {
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(n))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c;}
 static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static String method(String owner,MethodNode m)throws Exception{var c=new ClassNode();c.version=Opcodes.V17;c.access=Opcodes.ACC_PUBLIC;c.name=owner;c.superName="java/lang/Object";c.methods.add(m);var w=new ClassWriter(0);c.accept(w);return hash(w.toByteArray());}
 static Map<String,String> methods(ClassNode c)throws Exception{var out=new TreeMap<String,String>();for(var m:c.methods)out.put(m.name+m.desc,method(c.name,m));return out;}
 public static void main(String[]a)throws Exception{try(var left=new JarFile(a[0]);var right=new JarFile(a[1])){
  var names=new TreeSet<>(left.stream().map(e->e.getName()).filter(n->n.endsWith(".class")&&right.getJarEntry(n)!=null).toList());
  for(var n:names){byte[] l=left.getInputStream(left.getJarEntry(n)).readAllBytes(),r=right.getInputStream(right.getJarEntry(n)).readAllBytes();if(Arrays.equals(l,r))continue;var lc=read(left,n);var rc=read(right,n);var lm=methods(lc);var rm=methods(rc);var keys=new TreeSet<>(lm.keySet());keys.addAll(rm.keySet());for(var key:keys)if(!Objects.equals(lm.get(key),rm.get(key)))System.out.println(String.join("\t",List.of(n,key,lm.getOrDefault(key,"absent"),rm.getOrDefault(key,"absent"))));
  }
 }}
}
