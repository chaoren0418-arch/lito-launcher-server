import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes{
 static final String P="local/lito381/server/",H=P+"LiveBattleStats";
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 public static void main(String[] a)throws Exception{
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)));
  if(!hash.equals("4a3bb9be8185e6c5efa3b76d5dfe88e38062b3aa0d3985315be09af6a28e7836"))throw new IllegalStateException("Unexpected base");
  int hooks=0;try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))){
   for(var es=z.entries();es.hasMoreElements();){var e=es.nextElement();byte[] b=z.getInputStream(e).readAllBytes();
    if(e.getName().equals(P+"ClientSession.class")||e.getName().equals(P+"GameStateStore.class")){
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);
     for(var m:c.methods){
      String fn=m.name.equals("loadSqlTournamentProfiles")?"enable":m.name.equals("sendRoomList")?"lobby":null;
      if(c.name.equals(P+"ClientSession")&&fn!=null){var h=new InsnList();h.add(new VarInsnNode(ALOAD,0));h.add(call(fn,"(L"+P+"ClientSession;)V"));m.instructions.insert(h);hooks++;}
      if(c.name.equals(P+"GameStateStore")&&m.name.equals("ensureRankingSnapshot")){var h=new InsnList();h.add(new VarInsnNode(ALOAD,0));h.add(call("beforeSnapshot","(L"+P+"GameStateStore;)V"));m.instructions.insert(h);hooks++;}
      for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode mi){
       if(m.name.equals("sendRanking")&&mi.name.equals("persistSqlTournamentRoster")){m.instructions.set(mi,call("persistRankingRoster","(L"+P+"ClientSession;)V"));hooks++;}
       if(m.name.equals("persistSqlTournamentResult")&&mi.name.equals("equals")){
        m.instructions.insertBefore(mi,new VarInsnNode(ALOAD,0));m.instructions.set(mi,call("baselineEquals","(Ljava/lang/Object;Ljava/lang/Object;L"+P+"ClientSession;)Z"));hooks++;
       }
      }
     }
     ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(b);o.closeEntry();
   }
   o.putNextEntry(new ZipEntry(H+".class"));o.write(Files.readAllBytes(classes.resolve(H+".class")));o.closeEntry();
  }
  if(hooks!=5)throw new IllegalStateException("Expected 5 hooks got "+hooks);System.out.println("PASS patched five qualified live-stat/ranking hooks");
 }
}
