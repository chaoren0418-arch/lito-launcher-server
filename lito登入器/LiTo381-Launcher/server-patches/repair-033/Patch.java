import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch {
 public static void main(String[] args)throws Exception{
  Path base=Path.of(args[0]),classes=Path.of(args[1]),out=Path.of(args[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("3554b171ab0f66f1d95a4668a02b56142c051c8653a327c24c26545fadfe7c02"))throw new AssertionError("0.6.32 pin");
  boolean patched=false;
  try(var input=new ZipFile(base.toFile());var output=new ZipOutputStream(Files.newOutputStream(out))){
   for(var es=input.entries();es.hasMoreElements();){var e=es.nextElement();if(e.getName().equals("local/lito381/server/LiToItemDetails.class"))continue;byte[] b=input.getInputStream(e).readAllBytes();
    if(e.getName().equals("local/lito381/server/ClientSession.class")){
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);
     for(var m:c.methods)if(m.name.equals("selectedSourceAccounts")&&m.desc.equals("()Llocal/lito381/server/AccountRepository;")){
      for(var i:m.instructions.toArray())if(i.getOpcode()==Opcodes.ARETURN){var hook=new InsnList();hook.add(new InsnNode(Opcodes.DUP));hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"local/lito381/server/LiToItemTemplates","preload","(Llocal/lito381/server/AccountRepository;)V",false));m.instructions.insertBefore(i,hook);patched=true;}
     }
     ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();
    }
    output.putNextEntry(new ZipEntry(e.getName()));output.write(b);output.closeEntry();
   }
   for(String name:List.of("LiToItemDetails","LiToItemTemplates")){output.putNextEntry(new ZipEntry("local/lito381/server/"+name+".class"));output.write(Files.readAllBytes(classes.resolve("local/lito381/server/"+name+".class")));output.closeEntry();}
  }
  if(!patched)throw new AssertionError("source selection hook missing");
 }
}
