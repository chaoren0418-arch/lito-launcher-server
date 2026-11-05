package local.lito381.semanticdiff;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Erase only adjacent, non-throwing producers whose entire result is discarded. */
final class DiscardedLoads implements Opcodes {
    private DiscardedLoads() {}

    static void normalize(MethodNode m) {
        if (!FourthNormalizations.enabled("discarded-loads")) return;
        boolean changed;
        do {
            changed = false;
            for (AbstractInsnNode n : m.instructions.toArray()) {
                AbstractInsnNode pop = n.getNext();
                // Literal adjacency also excludes every jump label and try boundary.
                if (pop == null || FourthNormalizations.annotated(n) || FourthNormalizations.annotated(pop)) continue;
                int width = width(n);
                if (width == 0 || pop.getOpcode() != (width == 2 ? POP2 : POP)) continue;
                m.instructions.remove(n);
                m.instructions.remove(pop);
                changed = true;
            }
        } while (changed);
    }

    private static int width(AbstractInsnNode n) {
        int op = n.getOpcode();
        if (n instanceof VarInsnNode && op >= ILOAD && op <= ALOAD) return LocalWebs.width(op);
        if (op == DUP) return 1;
        if (op >= ACONST_NULL && op <= SIPUSH)
            return op == LCONST_0 || op == LCONST_1 || op == DCONST_0 || op == DCONST_1 ? 2 : 1;
        // Class, MethodType, MethodHandle and ConstantDynamic can resolve/execute/throw.
        // Do not erase those even when their result is immediately discarded.
        if (n instanceof LdcInsnNode l) {
            if (l.cst instanceof Long || l.cst instanceof Double) return 2;
            if (l.cst instanceof Integer || l.cst instanceof Float || l.cst instanceof String) return 1;
        }
        return 0;
    }
}
