import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Add an owned-receipt recovery clone; original arithmetic, FIFO completion and family fallbacks remain. */
public final class CompanionManaTransferMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",COM=P+"NativePlayerCommit",E=P+"NativeEquipmentDefense",A=P+"NativeCombatAttacker",H=P+"NativeCompanionManaTransfer",V=P+"NativePlayerVitalCompletions",R=V+"$Receipt";
 static ClassNode read(JarFile j,String name)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(name+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static void patch(ClassNode c){if(c.name.equals(E)){int count=0;for(var m:c.methods)if(m.name.equals("apply"))for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(A)&&v.name.equals("debit")){v.owner=H;v.name="drain";count++;}if(count!=1)throw new AssertionError("one equipment NPC MP consumer");return;}
  if(!c.name.equals(COM))throw new AssertionError(c.name);int count=0;for(var m:new ArrayList<>(c.methods))if(m.name.equals("recover")){
   var q=new MethodNode(m.access,"nativeManaTransferRecover",m.desc,null,m.exceptions.toArray(String[]::new));m.accept(q);q.desc="(L"+R+";"+m.desc.substring(1);for(var n:q.instructions){if(n instanceof VarInsnNode v)v.var++;if(n instanceof IincInsnNode v)v.var++;if(n instanceof FrameNode f){if(f.type!=F_NEW)throw new AssertionError("expanded frames");f.local.add(0,R);}}q.maxLocals++;int queues=0,reserves=0;
   for(var n:q.instructions.toArray())if(n instanceof MethodInsnNode v){if(v.owner.equals(COM)&&v.name.equals("queue")){v.owner=H;queues++;}if(v.owner.equals(V)&&v.name.equals("reserve")){var i=new InsnList();i.add(new InsnNode(POP));i.add(new VarInsnNode(ALOAD,0));q.instructions.insertBefore(v,i);q.instructions.remove(v);reserves++;}}
   if(queues!=1||reserves!=1)throw new AssertionError("original recovery dependencies "+queues+" "+reserves);c.methods.add(q);count++;
  }if(count!=1)throw new AssertionError("one original recovery body");
 }
 public static void main(String[]a)throws Exception{try(var j=new JarFile(a[1])){for(var name:List.of(COM,E)){var c=read(j,name);patch(c);var b=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,b);}else try(var out=new JarFile(a[2])){if(!Arrays.equals(b,bytes(read(out,name))))throw new AssertionError(name);}}}System.out.println("PASS exact one MP consumer and original arithmetic/completion recovery clone with owned receipt/Frame queue");}
}
