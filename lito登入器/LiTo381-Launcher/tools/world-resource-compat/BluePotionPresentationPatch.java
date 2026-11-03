import java.nio.file.*;
import java.io.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.util.*;

/** Add the previously omitted icon after the existing authoritative activation.
 * No potion consumption, duration, regeneration, persistence or other path changes.
 */
public final class BluePotionPresentationPatch {
    private static String text(MethodNode method) {
        Textifier printer=new Textifier();method.accept(new TraceMethodVisitor(printer));
        StringWriter out=new StringWriter();printer.print(new PrintWriter(out));return out.toString();
    }
    public static void main(String[] args) throws Exception {
        ClassReader reader=new ClassReader(Files.readAllBytes(Path.of(args[0])));
        ClassNode original=new ClassNode(Opcodes.ASM8),edited=new ClassNode(Opcodes.ASM8);
        reader.accept(original,0);reader.accept(edited,0);
        if(!edited.name.equals("local/lito381/server/ClientSession"))throw new AssertionError("wrong class");
        String changed=null;int sites=0;
        for(MethodNode method:edited.methods) {
            for(AbstractInsnNode node=method.instructions.getFirst();node!=null;node=node.getNext()) {
                if(!(node instanceof MethodInsnNode call) ||
                   !call.owner.equals("local/lito381/server/BattleTimedStatus") ||
                   !call.name.equals("activateBluePotion") || !call.desc.equals("(JI)V"))continue;
                if(!(node.getPrevious() instanceof VarInsnNode local) || local.getOpcode()!=Opcodes.ILOAD)
                    throw new AssertionError("duration local seam changed");
                if(!method.name.equals("sourcePoisonBody$useInventoryItemBeforeTravel") ||
                   !method.desc.equals("([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V"))
                    throw new AssertionError("unqualified potion caller "+method.name);
                InsnList added=new InsnList();
                added.add(new VarInsnNode(Opcodes.ALOAD,0));
                added.add(new VarInsnNode(Opcodes.ILOAD,local.var));
                added.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "local/lito381/server/SharedPresentationPackets","bluePotion","(I)[B",false));
                added.add(new LdcInsnNode("S_PACKET_BOX shared-blue-potion-status"));
                added.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,edited.name,"sendEncrypted",
                    "([BLjava/lang/String;)V",false));
                method.instructions.insert(node,added);changed=method.name;sites++;
            }
        }
        if(sites!=1)throw new AssertionError("expected one qualified activation, got "+sites);
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);edited.accept(writer);
        byte[] output=writer.toByteArray();ClassNode checked=new ClassNode(Opcodes.ASM8);
        new ClassReader(output).accept(checked,0);
        if(original.methods.size()!=checked.methods.size())throw new AssertionError("method count");
        int unchanged=0;
        for(int i=0;i<original.methods.size();i++){
            MethodNode a=original.methods.get(i),b=checked.methods.get(i);
            if(!a.name.equals(b.name)||!a.desc.equals(b.desc))throw new AssertionError("method identity");
            if(a.name.equals(changed))continue;
            if(!text(a).equals(text(b)))throw new AssertionError("unrelated method changed "+a.name);
            unchanged++;
        }
        Files.write(Path.of(args[1]),output);
        System.out.println("PASS one blue-potion icon send; "+unchanged+" unrelated ClientSession methods unchanged");
    }
}
