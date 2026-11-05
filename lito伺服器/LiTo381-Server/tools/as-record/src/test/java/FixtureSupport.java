import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** 獨立 ASM 對照樣本，不呼叫轉換器。 */
public final class FixtureSupport implements Opcodes {
    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[0]);
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "memo", "Ljava/lang/String;", null, null));
        MethodNode canonical = node.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        for (AbstractInsnNode insn : canonical.instructions.toArray()) {
            if (insn.getOpcode() != RETURN) continue;
            InsnList extra = new InsnList();
            extra.add(new VarInsnNode(ALOAD, 0));
            extra.add(new LdcInsnNode("memo"));
            extra.add(new FieldInsnNode(PUTFIELD, node.name, "memo", "Ljava/lang/String;"));
            canonical.instructions.insertBefore(insn, extra);
        }
        MethodNode alternate = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        alternate.visitCode();
        alternate.visitVarInsn(ALOAD, 0);
        alternate.visitMethodInsn(INVOKESPECIAL, "java/lang/Record", "<init>", "()V", false);
        alternate.visitVarInsn(ALOAD, 0);
        alternate.visitInsn(ICONST_0);
        alternate.visitFieldInsn(PUTFIELD, node.name, "value", "I");
        alternate.visitInsn(RETURN);
        alternate.visitMaxs(2, 1);
        alternate.visitEnd();
        node.methods.add(alternate);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        System.out.println("independent ASM finalFields reference generated");
    }
}
