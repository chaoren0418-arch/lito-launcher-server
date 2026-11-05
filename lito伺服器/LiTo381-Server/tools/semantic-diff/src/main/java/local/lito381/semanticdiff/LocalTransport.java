package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Local stack-preserving rewrites, independent of producer unions elsewhere in a method.
 * The resulting method is still compared in full; no merged operand is elided or summarized.
 */
final class LocalTransport implements Opcodes {
    private LocalTransport() {}

    static MethodNode normalize(MethodNode source) throws AnalyzerException {
        MethodNode m = new MethodNode(source.access, source.name, source.desc, source.signature,
                source.exceptions.toArray(String[]::new));
        source.accept(m);
        if (m.instructions.size() == 0 || m.visibleLocalVariableAnnotations != null
                || m.invisibleLocalVariableAnnotations != null) return m;
        for (AbstractInsnNode n : m.instructions) if (n.getOpcode() == JSR || n.getOpcode() == RET) return m;
        boolean changed;
        do {
            changed = false;
            Frame<SourceValue>[] frames = new Analyzer<>(new LocalWebs.Definitions()).analyze("LocalTransport", m);
            for (AbstractInsnNode n : m.instructions.toArray()) {
                if (!(n instanceof VarInsnNode store) || !LocalWebs.store(n.getOpcode())
                        || store.var < LocalWebs.parameters(m) || FourthNormalizations.annotated(store)) continue;
                List<VarInsnNode> loads = uses(m, store);
                if (loads == null || loads.isEmpty()) continue;
                boolean defined = true;
                for (VarInsnNode load : loads) {
                    Frame<SourceValue> f = frames[m.instructions.indexOf(load)];
                    if (f == null || !f.getLocal(store.var).insns.equals(Set.of(store))) defined = false;
                }
                if (!defined) continue;
                if (loads.size() == 1 && corridor(m, store, loads.get(0), frames)) {
                    m.instructions.remove(store);
                    m.instructions.remove(loads.get(0));
                    m.maxStack += LocalWebs.width(store.getOpcode());
                    changed = true;
                } else if (nullReturn(m, store, loads)) changed = true;
                if (changed) break; // Recompute frames after every independently proved rewrite.
            }
        } while (changed);
        Normalizations.trimTry(m);
        if (FourthNormalizations.enabled("try")) FourthNormalizations.mergeTry(m);
        Normalizations.cleanLabels(m);
        return m;
    }

    /** Exactly one write, matching reads, no overlapping wide accesses or annotations. */
    private static List<VarInsnNode> uses(MethodNode m, VarInsnNode store) {
        List<VarInsnNode> loads = new ArrayList<>();
        int width = LocalWebs.width(store.getOpcode());
        for (AbstractInsnNode n : m.instructions) {
            if (n == store) continue;
            if (n instanceof IincInsnNode i && i.var >= store.var && i.var < store.var + width) return null;
            if (!(n instanceof VarInsnNode v)) continue;
            if (v.var >= store.var + width || v.var + LocalWebs.width(v.getOpcode()) <= store.var) continue;
            if (v.var != store.var || v.getOpcode() != store.getOpcode() - (ISTORE - ILOAD)
                    || FourthNormalizations.annotated(v)) return null;
            loads.add(v);
        }
        return loads;
    }

    private static Set<AbstractInsnNode> entries(MethodNode m) {
        Set<AbstractInsnNode> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractInsnNode n : m.instructions) {
            if (n instanceof JumpInsnNode j) result.add(FourthNormalizations.next(j.label));
            if (n instanceof TableSwitchInsnNode s) {
                result.add(FourthNormalizations.next(s.dflt));
                for (LabelNode l : s.labels) result.add(FourthNormalizations.next(l));
            }
            if (n instanceof LookupSwitchInsnNode s) {
                result.add(FourthNormalizations.next(s.dflt));
                for (LabelNode l : s.labels) result.add(FourthNormalizations.next(l));
            }
        }
        for (TryCatchBlockNode t : m.tryCatchBlocks) result.add(FourthNormalizations.next(t.handler));
        return result;
    }

    /** STORE ... LOAD: the corridor neither enters elsewhere nor consumes the saved prefix.
     * Executing with an empty stack proves every intervening operand is produced inside it.
     * Calls/fields/monitors remain in place; exceptions discard the stack on both forms, and
     * the one-read rule proves no handler can observe the removed local assignment.
     */
    private static boolean corridor(MethodNode m, VarInsnNode store, VarInsnNode load,
            Frame<SourceValue>[] frames) {
        Frame<SourceValue> initial = frames[m.instructions.indexOf(store)];
        if (initial == null) return false;
        Frame<SourceValue> frame = new Frame<>(initial);
        frame.clearStack();
        Set<AbstractInsnNode> entries = entries(m);
        SourceInterpreter interpreter = new SourceInterpreter();
        try {
            for (AbstractInsnNode n = store.getNext(); n != null; n = n.getNext()) {
                if (n.getOpcode() < 0) continue;
                if (entries.contains(n)) return false;
                if (n == load) return frame.getStackSize() == 0;
                int op = n.getOpcode();
                if (n instanceof JumpInsnNode || n instanceof TableSwitchInsnNode || n instanceof LookupSwitchInsnNode
                        || op == ATHROW || op >= IRETURN && op <= RETURN) return false;
                frame.execute(n, interpreter);
            }
        } catch (AnalyzerException | RuntimeException unsupported) { return false; }
        return false;
    }

    /** ASTORE n; ALOAD n; IFNULL target; ALOAD n; ARETURN; target: ...
     * becomes DUP; IFNULL target; ARETURN; target: POP; ... . The target has exactly this
     * predecessor and n has no other uses, so both paths preserve their exact selected value.
     */
    private static boolean nullReturn(MethodNode m, VarInsnNode store, List<VarInsnNode> loads) {
        if (store.getOpcode() != ASTORE || loads.size() != 2) return false;
        AbstractInsnNode first = FourthNormalizations.next(store.getNext());
        AbstractInsnNode branch = first == null ? null : FourthNormalizations.next(first.getNext());
        AbstractInsnNode second = branch == null ? null : FourthNormalizations.next(branch.getNext());
        AbstractInsnNode ret = second == null ? null : FourthNormalizations.next(second.getNext());
        if (first != loads.get(0) || second != loads.get(1) || !(branch instanceof JumpInsnNode jump)
                || jump.getOpcode() != IFNULL || FourthNormalizations.annotated(jump)
                || ret == null || ret.getOpcode() != ARETURN) return false;
        AbstractInsnNode target = FourthNormalizations.next(ret.getNext());
        if (target == null || FourthNormalizations.next(jump.label) != target) return false;
        Set<AbstractInsnNode> entries = entries(m);
        if (entries.contains(first) || entries.contains(branch) || entries.contains(second) || entries.contains(ret)) return false;
        // Exclude aliases of the same target as well as explicit uses of jump.label.
        for (AbstractInsnNode n : m.instructions) {
            if (n instanceof JumpInsnNode j && n != jump && FourthNormalizations.next(j.label) == target) return false;
            if (n instanceof TableSwitchInsnNode s && (FourthNormalizations.next(s.dflt) == target
                    || s.labels.stream().anyMatch(l -> FourthNormalizations.next(l) == target))) return false;
            if (n instanceof LookupSwitchInsnNode s && (FourthNormalizations.next(s.dflt) == target
                    || s.labels.stream().anyMatch(l -> FourthNormalizations.next(l) == target))) return false;
        }
        for (TryCatchBlockNode t : m.tryCatchBlocks) if (FourthNormalizations.next(t.handler) == target) return false;
        m.instructions.set(store, new InsnNode(DUP));
        m.instructions.remove(first);
        m.instructions.remove(second);
        m.instructions.insertBefore(target, new InsnNode(POP));
        m.maxStack += 1;
        return true;
    }
}
