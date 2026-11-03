import java.nio.file.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Fixture-only mutable startup catalog for placing actual Source quest templates on an instanced map. */
public final class ObserveStaticCatalog {
 public static void main(String[]args)throws Exception{
  String name="local/lito381/server/ClassicWorldNpcCatalog";
  try(var jar=new JarFile(args[0])){
   var c=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(name+".class"))).accept(c,0);int count=0;
   for(var m:c.methods)if(m.name.equals("<clinit>"))for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTSTATIC&&f.name.equals("BY_MAP")){
    var code=new InsnList();code.add(new TypeInsnNode(NEW,"java/util/HashMap"));code.add(new InsnNode(DUP_X1));code.add(new InsnNode(SWAP));code.add(new MethodInsnNode(INVOKESPECIAL,"java/util/HashMap","<init>","(Ljava/util/Map;)V",false));m.instructions.insertBefore(n,code);count++;
   }
   if(count!=1)throw new AssertionError("catalog startup contract");
   var out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);var path=Path.of(args[1]).resolve(name+".class");Files.createDirectories(path.getParent());Files.write(path,out.toByteArray());
  }
 }
}
