import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/", H=P+"RuntimeLifecycle";
 public static void main(String[] a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("8b9f0319c410fcddae594619c081259c823bdcdbda8c5eac946c55eee0ccb019"))throw new IllegalStateException("Unexpected base");
  int resets=0,loads=0,writes=0;Set<String> replaced=new HashSet<>();
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var es=z.entries();es.hasMoreElements();) {
    var e=es.nextElement();byte[] b=z.getInputStream(e).readAllBytes();
    if(e.getName().startsWith(P+"SharedDatabase")&&e.getName().endsWith(".class")) {b=Files.readAllBytes(classes.resolve(e.getName()));replaced.add(e.getName());}
    if(e.getName().equals(P+"ClientSession.class")) {
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);
     for(var m:c.methods)for(var n:m.instructions.toArray()) {
      if(n.getOpcode()==RETURN&&(m.name.equals("resetNativeTournamentEconomy")||m.name.equals("loadSqlTournamentProfiles"))) {
       var h=new InsnList();h.add(new VarInsnNode(ALOAD,0));String name,desc;
       if(m.name.equals("resetNativeTournamentEconomy")){h.add(new VarInsnNode(ALOAD,1));name="reset";desc="(L"+P+"ClientSession;Ljava/lang/String;)V";resets++;}
       else {name="loaded";desc="(L"+P+"ClientSession;)V";loads++;}
       h.add(new MethodInsnNode(INVOKESTATIC,H,name,desc,false));m.instructions.insertBefore(n,h);
      }
      if(m.name.equals("persistSqlTournamentResult")&&n instanceof MethodInsnNode mi&&mi.name.equals("persistTournamentProfile")) {
       m.instructions.set(mi,new MethodInsnNode(INVOKESTATIC,P+"SharedDatabase","commitResult","(L"+P+"AccountRepository;"+mi.desc.substring(1),false));writes++;
      }
     }
     ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(b);o.closeEntry();
   }
   o.putNextEntry(new ZipEntry(H+".class"));o.write(Files.readAllBytes(classes.resolve(H+".class")));o.closeEntry();
  }
  if(resets!=2||loads!=2||writes!=1||replaced.size()!=2)throw new IllegalStateException("Wrong hook counts "+resets+"/"+loads+"/"+writes+"/"+replaced);
  System.out.println("PASS settlement-only write gateway; two cleanup and two roster paths patched");
 }
}
