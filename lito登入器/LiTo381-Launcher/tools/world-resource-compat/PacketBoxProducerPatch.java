import java.nio.file.Files;
import java.nio.file.Path;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

/** Change only the two proved doll writers in an existing qualified class.
 * Retain all unrelated candidate methods and already-applied resource hooks.
 */
public final class PacketBoxProducerPatch {
    private static String text(MethodNode method) {
        Textifier printer=new Textifier();
        method.accept(new TraceMethodVisitor(printer));
        StringWriter out=new StringWriter();
        printer.print(new PrintWriter(out));
        return out.toString();
    }
    public static void main(String[] args) throws Exception {
        byte[] before = Files.readAllBytes(Path.of(args[0]));
        ClassReader reader = new ClassReader(before);
        ClassNode clazz = new ClassNode(Opcodes.ASM8);
        reader.accept(clazz, 0);
        ClassNode original = new ClassNode(Opcodes.ASM8);
        reader.accept(original, 0);
        if (!clazz.name.equals("local/lito381/server/Protocol"))
            throw new IllegalArgumentException("wrong class");
        int methods = 0;
        for (MethodNode method : clazz.methods) {
            boolean generic = method.name.equals("magicDollIcon") && method.desc.equals("(I)[B");
            boolean custom = method.name.equals("magicDollCustomIcon") && method.desc.equals("(IIZ)[B");
            if (!generic && !custom) continue;
            int changed = 0, end = 0;
            for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
                if (node instanceof IntInsnNode value && value.getOpcode() == Opcodes.SIPUSH
                        && value.operand == 250) {
                    AbstractInsnNode next = value.getNext();
                    if (!(next instanceof MethodInsnNode call) || !call.name.equals("c")
                            || !call.owner.equals("local/lito381/server/Protocol$Writer"))
                        throw new IllegalArgumentException("opcode writer seam drift");
                    value.operand = 123;
                    ++changed;
                }
                if (node instanceof MethodInsnNode call && call.name.equals("bytes")
                        && call.owner.equals("local/lito381/server/Protocol$Writer")) {
                    ++end;
                    if (generic) {
                        InsnList suffix = new InsnList();
                        suffix.add(new InsnNode(Opcodes.ICONST_0));
                        suffix.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,call.owner,"h",
                                "(I)Llocal/lito381/server/Protocol$Writer;",false));
                        method.instructions.insertBefore(node,suffix);
                    }
                }
            }
            if (changed != 1 || end != 1) throw new IllegalArgumentException("writer seam drift " + method.name);
            ++methods;
        }
        if (methods != 2) throw new IllegalArgumentException("missing writers");
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        clazz.accept(writer);
        byte[] output=writer.toByteArray();
        ClassNode checked=new ClassNode(Opcodes.ASM8);
        new ClassReader(output).accept(checked,0);
        if (checked.methods.size()!=original.methods.size())throw new AssertionError("method count drift");
        int unchanged=0;
        for(int i=0;i<original.methods.size();++i) {
            MethodNode first=original.methods.get(i),last=checked.methods.get(i);
            if(!first.name.equals(last.name) || !first.desc.equals(last.desc))
                throw new AssertionError("method identity drift");
            if(first.name.equals("magicDollIcon") || first.name.equals("magicDollCustomIcon"))continue;
            if(!text(first).equals(text(last)))throw new AssertionError("unrelated method changed "+first.name);
            ++unchanged;
        }
        Files.write(Path.of(args[1]),output);
        System.out.println("PASS "+unchanged+" unrelated method bodies/metadata unchanged");
    }
}
