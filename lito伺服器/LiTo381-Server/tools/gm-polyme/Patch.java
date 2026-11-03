import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class Patch {
 public static void main(String[] a)throws Exception {
  var node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(a[0]))).accept(node,0);
  if (node.name.endsWith("/ClientSession")) {
   if(node.methods.stream().anyMatch(m->m.name.equals("exitRoomBeforePolyMe")))throw new IllegalStateException("Already patched");
   var old=node.methods.stream().filter(m->m.name.equals("exitRoom")&&m.desc.equals("(Z)V")).findFirst().orElseThrow();
   old.name="exitRoomBeforePolyMe";
   var wrapper=new MethodNode(old.access,"exitRoom",old.desc,old.signature,old.exceptions.toArray(String[]::new));
   wrapper.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));wrapper.instructions.add(new VarInsnNode(Opcodes.ILOAD,1));
   wrapper.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"local/lito381/server/GmPolyMe","exitRoom","(Llocal/lito381/server/ClientSession;Z)V",false));wrapper.instructions.add(new InsnNode(Opcodes.RETURN));wrapper.maxStack=2;wrapper.maxLocals=2;node.methods.add(wrapper);
   var writer=new ClassWriter(0);node.accept(writer);Files.write(Path.of(a[1]),writer.toByteArray());return;
  }
  var method=node.methods.stream().filter(m->m.name.equals("command")&&m.desc.equals("(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Z")).findFirst().orElseThrow();
  for(var insn:method.instructions)if(insn instanceof MethodInsnNode call&&call.owner.endsWith("/GmPolyMe"))throw new IllegalStateException("Already patched");
  var code=new InsnList();var rest=new LabelNode();
  code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ALOAD,1));
  code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"local/lito381/server/GmPolyMe","command",method.desc,false));
  code.add(new JumpInsnNode(Opcodes.IFEQ,rest));code.add(new InsnNode(Opcodes.ICONST_1));code.add(new InsnNode(Opcodes.IRETURN));code.add(rest);
  method.instructions.insert(code);
  var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);node.accept(writer);
  Files.write(Path.of(a[1]),writer.toByteArray());
 }
}
