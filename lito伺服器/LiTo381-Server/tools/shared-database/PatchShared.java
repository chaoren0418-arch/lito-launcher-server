import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class PatchShared implements Opcodes {
 static final String P="local/lito381/server/", H=P+"SharedDatabase";
 static MethodInsnNode call(String name,String desc){return new MethodInsnNode(INVOKESTATIC,H,name,desc,false);}
 static String obj(String name){return "L"+P+name+";";}
 public static void main(String[] args)throws Exception {
  Path base=Path.of(args[0]),classes=Path.of(args[1]),out=Path.of(args[2]);int hooks=0;
  String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)));
  if(!hash.equals("3f31ef32c8ca1ca8f9507897f03b54ca37593326ad986b5c10fcff68f695ed5c"))throw new IllegalArgumentException("Unexpected base JAR");
  try(var input=new ZipFile(base.toFile());var output=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var entries=input.entries();entries.hasMoreElements();) {
    var e=entries.nextElement();byte[] bytes=input.getInputStream(e).readAllBytes();
    if(Set.of(P+"AccountRepository.class",P+"LegacyMySqlRepository.class",P+"ServerConfig.class").contains(e.getName())) {
     ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);
     for(var m:c.methods) {
      InsnList h=new InsnList();LabelNode next=new LabelNode();
      if(c.name.equals(P+"AccountRepository")&&m.name.equals("openLegacy")) {
       h.add(new VarInsnNode(ALOAD,0));h.add(call("enabled","("+obj("ServerConfig")+")Z"));h.add(new JumpInsnNode(IFEQ,next));
       h.add(new VarInsnNode(ALOAD,0));h.add(call("open","("+obj("ServerConfig")+")"+obj("AccountRepository")));h.add(new InsnNode(ARETURN));h.add(next);
      } else if(c.name.equals(P+"ServerConfig")&&m.name.equals("applyExternalSourceServers")) {
       h.add(new VarInsnNode(ALOAD,0));h.add(new VarInsnNode(ALOAD,1));h.add(call("applySources","(Ljava/util/Properties;Ljava/nio/file/Path;)Z"));h.add(new JumpInsnNode(IFEQ,next));h.add(new InsnNode(RETURN));h.add(next);
      } else if(c.name.equals(P+"LegacyMySqlRepository")) {
       if(m.name.equals("resourceTable")) {
        m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
        h.add(new VarInsnNode(ALOAD,0));h.add(new VarInsnNode(ALOAD,1));h.add(new VarInsnNode(ALOAD,0));h.add(new FieldInsnNode(GETFIELD,c.name,"resourceDatabase","Ljava/lang/String;"));
        h.add(call("resourceTable","("+obj("LegacyMySqlRepository")+"Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));h.add(new InsnNode(ARETURN));
       } else if(m.name.equals("healthCheck")) {
        h.add(new VarInsnNode(ALOAD,0));h.add(call("check","("+obj("LegacyMySqlRepository")+")V"));
       } else if(Set.of("migrateLitoExtensions","tournamentProfiles","persistTournamentProfile").contains(m.name)) {
        h.add(new VarInsnNode(ALOAD,0));h.add(call("registered","("+obj("LegacyMySqlRepository")+")Z"));h.add(new JumpInsnNode(IFEQ,next));
        if(m.name.equals("migrateLitoExtensions"))h.add(new InsnNode(RETURN));
        else {
         h.add(new VarInsnNode(ALOAD,0));
         if(m.name.equals("tournamentProfiles"))h.add(call("profiles","("+obj("LegacyMySqlRepository")+")"+obj("RepositoryTournamentRoster")));
         else {
          h.add(new VarInsnNode(LLOAD,1));h.add(new VarInsnNode(ALOAD,3));h.add(new VarInsnNode(ALOAD,4));h.add(new VarInsnNode(ALOAD,5));
          h.add(call("persist","("+obj("LegacyMySqlRepository")+"JLjava/lang/String;"+obj("RepositoryTournamentProfile")+obj("GameStateStore$TournamentProfileState")+")"+obj("RepositoryTournamentMutation")));
         }
         h.add(new InsnNode(ARETURN));
        }
        h.add(next);
       }
      }
      if(h.size()>0){m.instructions.insert(h);hooks++;System.out.println(c.name+"."+m.name);}
     }
     ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(writer);bytes=writer.toByteArray();
    }
    output.putNextEntry(new ZipEntry(e.getName()));output.write(bytes);output.closeEntry();
   }
   for(String name:List.of("SharedDatabase","SharedDatabase$Route")) {
    String entry=P+name+".class";output.putNextEntry(new ZipEntry(entry));output.write(Files.readAllBytes(classes.resolve(entry)));output.closeEntry();
   }
  }
  if(hooks!=7)throw new IllegalStateException("Expected 7 hooks, got "+hooks);
 }
}
