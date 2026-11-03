import java.nio.file.*;import java.util.*;import java.util.jar.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** TEST JAR ONLY: observe original gate results without replacing their decisions. */
public final class SourceMaterialGateTrace {
 static final String P="local/lito381/server/";
 static void message(InsnList out,String text){out.add(new FieldInsnNode(GETSTATIC,"java/lang/System","err","Ljava/io/PrintStream;"));out.add(new LdcInsnNode(text));out.add(new MethodInsnNode(INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false));}
 public static void main(String[]args)throws Exception {
  var specs=Map.of(P+"NativeSourceTargetNegativeLife",Set.of("capture","player","current"),P+"NativeSourceWeaponTargetLife",Set.of("dispatch","admit","current"));var entries=new TreeMap<String,byte[]>();int methods=0,ports=0;
  try(var jar=new JarFile(args[0])){for(var e:Collections.list(jar.entries())){byte[] bytes=jar.getInputStream(e).readAllBytes();String owner=e.getName().endsWith(".class")?e.getName().substring(0,e.getName().length()-6):"";
   if(specs.containsKey(owner)){var c=new ClassNode();new ClassReader(bytes).accept(c,0);for(var m:c.methods)if(specs.get(owner).contains(m.name)){methods++;if(m.name.equals("dispatch")){var x=new InsnList();message(x,"GATE ENTER "+owner+"."+m.name);m.instructions.insert(x);ports++;}int i=0;for(var n:m.instructions.toArray()){if(n.getOpcode()==IRETURN){var x=new InsnList();message(x,"GATE RETURN "+owner+"."+m.name+m.desc+" at="+i);x.add(new InsnNode(DUP));x.add(new FieldInsnNode(GETSTATIC,"java/lang/System","err","Ljava/io/PrintStream;"));x.add(new InsnNode(SWAP));x.add(new MethodInsnNode(INVOKEVIRTUAL,"java/io/PrintStream","println","(Z)V",false));m.instructions.insertBefore(n,x);ports++;}i++;}}var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(writer);bytes=writer.toByteArray();}
   entries.put(e.getName(),bytes);
  }}
  try(var out=new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))){for(var e:entries.entrySet()){var z=new ZipEntry(e.getKey());z.setTime(0);out.putNextEntry(z);out.write(e.getValue());out.closeEntry();}}
  System.out.println("TEST ONLY gate tracing: methods="+methods+", original-return observations="+ports+"; original gate decisions retained, no production qualification");
 }
}
