import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class PatchSourceLogin implements Opcodes {
 static final String P="local/lito381/server/", H=P+"SourceLogin";
 static String o(String n){return "L"+P+n+";";}
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 public static void main(String[] args)throws Exception {
  int startup=0,constructor=0,guard=0,ownership=0,handoffs=0,close=0,banRead=0,banWrite=0;
  Set<String> names=Set.of("clearClassicSourceHandoff","stageClassicSourceHandoff","pendingClassicSourceHandoff","consumeClassicSourceHandoff",
    "stageChannelPortHandoff","pendingChannelPortHandoff","consumeChannelPortHandoff",
    "stageRentalBattleHandoff","hasRentalBattleHandoff","consumeRentalBattleHandoff");
  try(var input=new ZipFile(args[0]);var output=new ZipOutputStream(Files.newOutputStream(Path.of(args[2])))) {
   for(var entries=input.entries();entries.hasMoreElements();) {
    var entry=entries.nextElement();byte[] bytes=input.getInputStream(entry).readAllBytes();
    if(Set.of(P+"Main.class",P+"ClientSession.class",P+"GameHub.class").contains(entry.getName())) {
     ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);
     for(var m:c.methods) {
      if(c.name.equals(P+"Main")&&m.name.equals("main")) {
       for(var n:m.instructions.toArray())if(n instanceof LdcInsnNode ldc && "[LiTo] ready".equals(ldc.cst)) {
        InsnList h=new InsnList();for(int slot:new int[]{3,8,7,10,11,5})h.add(new VarInsnNode(ALOAD,slot));
        h.add(call("start","("+o("ServerConfig")+o("AccountRepository")+o("GameStateStore")+o("CaptureLog")+o("GameHub")+"Ljava/util/concurrent/ExecutorService;)V"));
        m.instructions.insertBefore(n,h);startup++;
       }
      }
      if(c.name.equals(P+"ClientSession")) {
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x) {
        if(x.owner.equals(P+"GameStateStore")&&x.name.equals("isAccountBanned")) {
         InsnList h=new InsnList();h.add(new VarInsnNode(ALOAD,0));h.add(new InsnNode(SWAP));h.add(call("account","("+o("ClientSession")+"Ljava/lang/String;)Ljava/lang/String;"));m.instructions.insertBefore(n,h);banRead++;
        }
        if(x.owner.equals(P+"ClientSession")&&x.name.equals("accountName")) {
         AbstractInsnNode after=n.getNext();while(after!=null&&after.getOpcode()<0)after=after.getNext();
         if(after instanceof MethodInsnNode next&&next.owner.equals(P+"GameStateStore")&&next.name.equals("banAccount")) {
          m.instructions.insertBefore(n,new InsnNode(DUP));m.instructions.insert(n,call("account","("+o("ClientSession")+"Ljava/lang/String;)Ljava/lang/String;"));banWrite++;
         }
        }
       }
       if(m.name.equals("<init>")&&m.desc.startsWith("(Ljava/net/Socket;"+o("ClientSession$Endpoint")+"I")) {
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals("java/lang/Object")&&x.name.equals("<init>")) {
         InsnList h=new InsnList();h.add(new VarInsnNode(ALOAD,4));h.add(new VarInsnNode(ALOAD,1));h.add(call("configuration","("+o("ServerConfig")+"Ljava/net/Socket;)"+o("ServerConfig")));h.add(new VarInsnNode(ASTORE,4));
         h.add(new VarInsnNode(ALOAD,5));h.add(new VarInsnNode(ALOAD,1));h.add(call("repository","("+o("AccountRepository")+"Ljava/net/Socket;)"+o("AccountRepository")));h.add(new VarInsnNode(ASTORE,5));
         m.instructions.insert(n,h);constructor++;break;
        }
       }
       if(m.name.equals("run")&&m.desc.equals("()V")) {
        InsnList h=new InsnList();LabelNode next=new LabelNode();
        h.add(new VarInsnNode(ALOAD,0));h.add(new FieldInsnNode(GETFIELD,c.name,"socket","Ljava/net/Socket;"));
        h.add(new VarInsnNode(ALOAD,0));h.add(new FieldInsnNode(GETFIELD,c.name,"config",o("ServerConfig")));
        h.add(call("allowed","(Ljava/net/Socket;"+o("ServerConfig")+")Z"));h.add(new JumpInsnNode(IFNE,next));h.add(new InsnNode(RETURN));h.add(next);m.instructions.insert(h);guard++;
       }
       for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals(P+"GameHub")&&names.contains(x.name)) {
        Type[] types=Type.getArgumentTypes(x.desc);if(!types[0].equals(Type.getType(String.class)))throw new IllegalStateException("Bad account argument");
        int self=0;if((m.access&ACC_STATIC)!=0){self=-1;int pos=0;for(Type t:Type.getArgumentTypes(m.desc)){if(t.getDescriptor().equals(o("ClientSession")))self=pos;pos+=t.getSize();}if(self<0)throw new IllegalStateException("No session in "+m.name);}
        int[] slots=new int[types.length];for(int k=0;k<types.length;k++){slots[k]=m.maxLocals;m.maxLocals+=types[k].getSize();}
        int receiver=m.maxLocals++;InsnList h=new InsnList();
        for(int k=types.length-1;k>=0;k--)h.add(new VarInsnNode(types[k].getOpcode(ISTORE),slots[k]));
        h.add(new VarInsnNode(ASTORE,receiver));h.add(new VarInsnNode(ALOAD,receiver));
        h.add(new VarInsnNode(ALOAD,self));h.add(new VarInsnNode(ALOAD,slots[0]));h.add(call("account","("+o("ClientSession")+"Ljava/lang/String;)Ljava/lang/String;"));
        for(int k=1;k<types.length;k++)h.add(new VarInsnNode(types[k].getOpcode(ILOAD),slots[k]));
        m.instructions.insertBefore(n,h);handoffs++;
       }
      }
      if(c.name.equals(P+"GameHub")) {
       if(Set.of("acceptAuthenticatedAccount","releaseAuthenticatedAccountForTransfer").contains(m.name)) {
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.name.equals("key")&&x.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
         InsnList h=new InsnList();h.add(new VarInsnNode(ALOAD,1));h.add(new InsnNode(SWAP));h.add(call("account","("+o("ClientSession")+"Ljava/lang/String;)Ljava/lang/String;"));m.instructions.insertBefore(n,h);ownership++;break;
        }
       }
       if(m.name.equals("close")&&m.desc.equals("()V")) {InsnList h=new InsnList();h.add(new VarInsnNode(ALOAD,0));h.add(call("close","("+o("GameHub")+")V"));m.instructions.insert(h);close++;}
      }
     }
     ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(writer);bytes=writer.toByteArray();
    }
    output.putNextEntry(new ZipEntry(entry.getName()));output.write(bytes);output.closeEntry();
   }
   for(String name:List.of("SourceLogin","SourceLogin$Binding")){String n=P+name+".class";output.putNextEntry(new ZipEntry(n));output.write(Files.readAllBytes(Path.of(args[1]).resolve(n)));output.closeEntry();}
  }
  System.out.println("Source login hooks: startup="+startup+" constructor="+constructor+" guard="+guard+" ownership="+ownership+" handoffs="+handoffs+" close="+close+" ban="+banRead+"/"+banWrite);
  if(startup!=1||constructor!=1||guard!=1||ownership!=2||handoffs!=17||close!=1||banRead!=1||banWrite!=1)throw new IllegalStateException("Source login hooks incomplete");
 }
}
