import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Overlay only the two BUFF read/validation methods on the pinned formal core. */
public class PatchBuffRead implements Opcodes {
    public static void main(String[] args) throws Exception {
        String owner = "local/lito381/server/LegacyMySqlRepository";
        String helper = "local/lito381/server/SourceBuffRead";
        try (ZipFile base = new ZipFile(args[0]); ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(Path.of(args[2])))) {
            for (var entry : Collections.list(base.entries())) {
                byte[] bytes = base.getInputStream(entry).readAllBytes();
                if (entry.getName().equals(owner + ".class")) {
                    ClassNode c = new ClassNode(); new ClassReader(bytes).accept(c, 0);
                    MethodNode health = c.methods.stream().filter(m -> m.name.equals("healthCheck") && m.desc.equals("()V")).findFirst().orElseThrow();
                    int removed = 0, projected = 0;
                    for (var n : health.instructions.toArray()) {
                        if (n instanceof LdcInsnNode ldc && "character_buff_snapshot".equals(ldc.cst)) {
                            AbstractInsnNode first = n;
                            for (int i = 0; i < 7; i++) first = first.getPrevious();
                            int[] expected = {ALOAD, ALOAD, GETFIELD, ALOAD, GETFIELD, ALOAD, GETFIELD, LDC, INVOKEVIRTUAL};
                            AbstractInsnNode current = first;
                            List<AbstractInsnNode> removal = new ArrayList<>();
                            for (int op : expected) {
                                if (current == null || current.getOpcode() != op) throw new IllegalStateException("Unexpected health-check layout");
                                removal.add(current); current = current.getNext();
                            }
                            MethodInsnNode call = (MethodInsnNode)removal.get(8);
                            if (!call.owner.equals(owner) || !call.name.equals("checkTable")) throw new IllegalStateException("Unexpected table call");
                            removal.forEach(health.instructions::remove); removed++;
                        }
                        if (n instanceof LdcInsnNode ldc && ldc.cst instanceof String sql
                                && sql.startsWith("SELECT b.char_obj_id,b.skill_id,b.remaining_time,b.poly_id,")) {
                            var next = n.getNext();
                            if (!(next instanceof MethodInsnNode call) || !call.owner.equals("java/sql/Connection") || !call.name.equals("prepareStatement")) throw new IllegalStateException("Unexpected BUFF projection");
                            health.instructions.set(next, new MethodInsnNode(INVOKESTATIC, helper, "healthStatement", "(Ljava/sql/Connection;Ljava/lang/String;)Ljava/sql/PreparedStatement;", false));
                            projected++;
                        }
                    }
                    if (removed != 1 || projected != 1) throw new IllegalStateException("BUFF hook counts: " + removed + "/" + projected);
                    MethodNode load = c.methods.stream().filter(m -> m.name.equals("loadBuffSnapshot") && m.desc.equals("(Ljava/sql/Connection;J)Llocal/lito381/server/RepositoryBuffSnapshot;")).findFirst().orElseThrow();
                    load.instructions.clear(); load.tryCatchBlocks.clear();
                    if (load.localVariables != null) load.localVariables.clear();
                    load.visibleLocalVariableAnnotations = null; load.invisibleLocalVariableAnnotations = null;
                    load.instructions.add(new VarInsnNode(ALOAD, 0)); load.instructions.add(new VarInsnNode(LLOAD, 1));
                    load.instructions.add(new MethodInsnNode(INVOKESTATIC, helper, "load", load.desc, false));
                    load.instructions.add(new InsnNode(ARETURN));
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); c.accept(writer); bytes = writer.toByteArray();
                }
                ZipEntry copied = new ZipEntry(entry.getName()); copied.setTime(entry.getTime());
                out.putNextEntry(copied); out.write(bytes); out.closeEntry();
            }
            ZipEntry added = new ZipEntry(helper + ".class"); added.setTime(0); out.putNextEntry(added);
            out.write(Files.readAllBytes(Path.of(args[1]).resolve(helper + ".class"))); out.closeEntry();
        }
    }
}
