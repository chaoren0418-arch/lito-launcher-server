package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Reaching-definition webs, including loop joins and ordered exception edges. */
final class LocalWebs implements Opcodes {
    private LocalWebs() {}
    static int parameters(MethodNode m) {
        int n = (m.access & ACC_STATIC) == 0 ? 1 : 0;
        for (Type t : Type.getArgumentTypes(m.desc)) n += t.getSize();
        return n;
    }
    static int width(int op) { return op == LLOAD || op == DLOAD || op == LSTORE || op == DSTORE ? 2 : 1; }
    static boolean load(int op) { return op >= ILOAD && op <= ALOAD; }
    static boolean store(int op) { return op >= ISTORE && op <= ASTORE; }
    static final class Definitions extends SourceInterpreter {
        final Map<AbstractInsnNode,Integer> params = new IdentityHashMap<>();
        Definitions() { super(ASM9); }
        @Override public SourceValue newParameterValue(boolean instance, int local, Type type) {
            AbstractInsnNode marker = new InsnNode(NOP); params.put(marker, local);
            return new SourceValue(type.getSize(), marker);
        }
        @Override public SourceValue copyOperation(AbstractInsnNode n, SourceValue v) {
            return load(n.getOpcode()) ? v : super.copyOperation(n, v);
        }
    }
    static AbstractInsnNode root(Map<AbstractInsnNode,AbstractInsnNode> parents, AbstractInsnNode n) {
        AbstractInsnNode p = parents.get(n);
        if (p == null) { parents.put(n,n); return n; }
        if (p == n) return n;
        p = root(parents,p); parents.put(n,p); return p;
    }
    static void normalize(String owner, MethodNode m) {
        if (m.instructions.size() == 0 || m.visibleLocalVariableAnnotations != null || m.invisibleLocalVariableAnnotations != null) return;
        for (AbstractInsnNode n : m.instructions) if (n.getOpcode() == RET || n.getOpcode() == JSR) return;
        try {
            Definitions interpreter = new Definitions();
            Frame<SourceValue>[] frames = new Analyzer<>(interpreter).analyze(owner,m);
            Map<AbstractInsnNode,AbstractInsnNode> parents = new IdentityHashMap<>();
            Map<AbstractInsnNode,AbstractInsnNode> variable = new IdentityHashMap<>();
            for (AbstractInsnNode n : m.instructions) {
                int op = n.getOpcode();
                if (!(n instanceof VarInsnNode) && !(n instanceof IincInsnNode)) continue;
                Frame<SourceValue> f = frames[m.instructions.indexOf(n)];
                if (f == null) return; // Do not invent reaching definitions for unreachable code.
                int slot = n instanceof VarInsnNode v ? v.var : ((IincInsnNode)n).var;
                if (store(op)) { variable.put(n,n); root(parents,n); }
                else {
                    SourceValue v = f.getLocal(slot);
                    if (v.insns.isEmpty() || v.getSize() != width(op)) return;
                    AbstractInsnNode first = v.insns.iterator().next();
                    for (AbstractInsnNode def : v.insns) parents.put(root(parents,def),root(parents,first));
                    if (n instanceof IincInsnNode) parents.put(root(parents,n),root(parents,first));
                    variable.put(n,first);
                }
            }
            Map<AbstractInsnNode,Integer> ids = new IdentityHashMap<>();
            for (var p : interpreter.params.entrySet()) {
                AbstractInsnNode r = root(parents,p.getKey());
                Integer old = ids.put(r,p.getValue());
                if (old != null && !old.equals(p.getValue())) return;
            }
            Map<AbstractInsnNode,Integer> replacements = new IdentityHashMap<>();
            int next = parameters(m);
            for (AbstractInsnNode n : m.instructions) if (variable.containsKey(n)) {
                AbstractInsnNode r = root(parents,variable.get(n));
                if (!ids.containsKey(r)) { ids.put(r,next); next += width(n.getOpcode()); }
                replacements.put(n,ids.get(r));
            }
            for (var e : replacements.entrySet()) {
                if (e.getKey() instanceof VarInsnNode v) v.var = e.getValue();
                else ((IincInsnNode)e.getKey()).var = e.getValue();
            }
            m.maxLocals = Math.max(m.maxLocals,next);
        } catch (AnalyzerException | RuntimeException unsupported) {
            // A failed proof leaves the original method unchanged.
        }
    }
}
