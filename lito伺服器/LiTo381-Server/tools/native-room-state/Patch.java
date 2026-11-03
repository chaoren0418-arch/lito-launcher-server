import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/";
 public static void main(String[] a)throws Exception {
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("e5f2e01b6294279dc350c96d38b27bd4dc53e3a1b92535b9be7abeb7283a9fdf"))throw new IllegalStateException("baseline");
  int count=0, movementMethods=0, visibilityHooks=0, entryHooks=0;
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
   for(var it=z.entries();it.hasMoreElements();) {
    var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
    if(Set.of(P+"ClientSession.class",P+"GameHub.class",P+"Protocol.class").contains(e.getName())) {
     var c=new ClassNode();new ClassReader(data).accept(c,0);
     for(var m:c.methods) {
      if(c.name.equals(P+"ClientSession") && Set.of("startBattle","sendPortalWorldRebuild").contains(m.name)) {
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call && call.owner.equals(P+"GameHub") && call.name.equals("visibleBattleCombatants")) {
        var ins=new InsnList();ins.add(new VarInsnNode(ALOAD,0));ins.add(new MethodInsnNode(INVOKESTATIC,P+"NativeBattleVisibility","localFirst","(Ljava/util/List;L"+P+"ClientSession;)Ljava/util/List;",false));m.instructions.insert(n,ins);entryHooks++;
       }
      }
      if(c.name.equals(P+"ClientSession") && Set.of("authoritativeMove","respawnAtBattleSpawn","move","moveRentalTown","publishRentalTownMovement","moveClassicWorld","completePortal").contains(m.name)) {
       int sites=0;
       for(var n:m.instructions.toArray()) if(n instanceof MethodInsnNode call && call.owner.equals(P+"Protocol$Writer") && call.name.equals("bytes")) {
        m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,P+"NativeMovement","legacyDestinationPacket","([B)[B",false));sites++;
       }
       if(sites==0)throw new IllegalStateException("missing movement writer "+m.name);movementMethods++;
       if(m.name.equals("move"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call && call.name.equals("handlePetRaceMove")) {
        var ins=new InsnList();ins.add(new VarInsnNode(ALOAD,0));ins.add(new VarInsnNode(ALOAD,1));ins.add(new MethodInsnNode(INVOKESTATIC,P+"NativeBattleVisibility","afterMove","(L"+P+"ClientSession;[B)V",false));m.instructions.insertBefore(n,ins);visibilityHooks++;
       }
      }
      if(c.name.equals(P+"ClientSession") && m.name.equals("sendRoomListRow")) {
       m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
       m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new VarInsnNode(ALOAD,2));
       m.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"NativeRoomDirectoryRow","send","(L"+P+"ClientSession;L"+P+"GameHub$RoomSnapshot;Ljava/lang/String;)V",false));m.instructions.add(new InsnNode(RETURN));count++;
      }
      if(c.name.equals(P+"GameHub") && m.name.equals("createRoom") && m.desc.equals("(IL"+P+"GameHub$RoomSettings;)J")) {
       int matches=0;
       for(var n:m.instructions.toArray()) if(n.getOpcode()==IFNULL && n.getPrevious() instanceof VarInsnNode v && v.getOpcode()==ALOAD && v.var==4) {
        m.instructions.set(v,new InsnNode(ACONST_NULL));matches++;
       }
       if(matches!=1)throw new IllegalStateException("default room reuse branch "+matches);count++;
      }
      if(c.name.equals(P+"Protocol") && m.name.equals("battleActorWithHeading")) {
       Type[] types=Type.getArgumentTypes(m.desc);int slot=0;
       // status is the argument following name and title (index 10).
       for(int i=0;i<10;i++)slot+=types[i].getSize();
       int returns=0;
       for(var n:m.instructions.toArray())if(n.getOpcode()==ARETURN) {
        var ins=new InsnList();ins.add(new VarInsnNode(ILOAD,slot));ins.add(new MethodInsnNode(INVOKESTATIC,P+"NativeActorTail","complete","([BI)[B",false));m.instructions.insertBefore(n,ins);returns++;
       }
       if(returns!=1)throw new IllegalStateException("actor return "+returns);count++;
      }
     }
     var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
    }
    o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
   }
   for(String helper:List.of("NativeActorTail","NativeRoomDirectoryRow","NativeMovement","NativeBattleVisibility")) {
    String name=P+helper+".class";o.putNextEntry(new ZipEntry(name));o.write(Files.readAllBytes(classes.resolve(name)));o.closeEntry();
   }
  }
  if(count!=3 || movementMethods!=7 || visibilityHooks!=1 || entryHooks!=2)throw new IllegalStateException("hook count "+count+"/"+movementMethods+"/"+visibilityHooks+"/"+entryHooks);System.out.println("Patched native actor tail, unique room IDs/upserts, 7 destination movement paths and battle visibility/entry order");
 }
}
