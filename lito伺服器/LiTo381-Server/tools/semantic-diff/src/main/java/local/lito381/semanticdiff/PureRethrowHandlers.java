package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Symmetric, local proof: erase only an identity rethrow with identical outer dispatch. */
final class PureRethrowHandlers implements Opcodes {
    private PureRethrowHandlers() {}

    static void normalize(MethodNode m) {
        if (!FourthNormalizations.enabled("pure-rethrow")
                || m.visibleLocalVariableAnnotations != null || m.invisibleLocalVariableAnnotations != null) return;
        for (AbstractInsnNode n : m.instructions) if (n.getOpcode() == JSR || n.getOpcode() == RET) return;
        boolean changed, erased = false;
        Set<Integer> discardedLocals = new HashSet<>();
        do {
            changed = false;
            for (TryCatchBlockNode t : new ArrayList<>(m.tryCatchBlocks)) {
                AbstractInsnNode entry = next(t.handler);
                List<TryCatchBlockNode> group = m.tryCatchBlocks.stream().filter(x -> next(x.handler) == entry).toList();
                if (group.stream().anyMatch(x -> !Objects.equals(x.type, t.type)
                        || x.visibleTypeAnnotations != null || x.invisibleTypeAnnotations != null)) continue;
                List<AbstractInsnNode> body = body(m, entry, group);
                if (body == null) continue;
                AbstractInsnNode thrown = body.get(body.size() - 1);
                List<TryCatchBlockNode> outgoing = coverage(m, thrown);
                if (outgoing.stream().anyMatch(group::contains)) continue;
                boolean safe = true;
                for (AbstractInsnNode n : m.instructions) {
                    if (n.getOpcode() < 0 || Normalizations.harmless(n, true)) continue;
                    List<TryCatchBlockNode> active = coverage(m, n);
                    int first = -1;
                    for (int i = 0; i < active.size(); i++) if (group.contains(active.get(i))) { first = i; break; }
                    if (first < 0) continue;
                    List<TryCatchBlockNode> tail = active.subList(first + 1, active.size()).stream()
                            .filter(x -> !group.contains(x)).toList();
                    if (!sameDispatch(tail, outgoing)) { safe = false; break; }
                }
                if (!safe) continue;
                // Writes performed by the catch must not become visible to any outer handler.
                for (AbstractInsnNode n : body) if (n instanceof VarInsnNode v && v.getOpcode() >= ISTORE && v.getOpcode() <= ASTORE) {
                    int width = v.getOpcode() == LSTORE || v.getOpcode() == DSTORE ? 2 : 1;
                    for (TryCatchBlockNode outer : outgoing)
                        for (int slot = v.var; slot < v.var + width; slot++)
                            if (live(m, next(outer.handler), slot, Collections.newSetFromMap(new IdentityHashMap<>()))) safe = false;
                }
                if (!safe) continue;
                for (AbstractInsnNode n : body)
                    if (n instanceof VarInsnNode v && v.getOpcode() >= ILOAD && v.getOpcode() <= ALOAD
                            && next(n) != null && next(n).getOpcode() == (LocalWebs.width(v.getOpcode()) == 2 ? POP2 : POP))
                        discardedLocals.add(v.var);
                m.tryCatchBlocks.removeAll(group);
                body.forEach(m.instructions::remove);
                changed = true;
                erased = true;
                break;
            }
        } while (changed);
        if (!erased) return;
        Normalizations.cleanLabels(m);
        cleanupTransport(m, discardedLocals);
        for (AbstractInsnNode n : m.instructions.toArray())
            if (n instanceof JumpInsnNode j && j.getOpcode() == GOTO && next(n) == next(j.label)
                    && !FourthNormalizations.annotated(n)) m.instructions.remove(n);
        Normalizations.cleanLabels(m);
    }

    /** The erased monitor catch can leave its lock temporary and LOAD/POP on normal exits.
     * Keep this cleanup local to a method where the catch proof succeeded. Producers with
     * effects stay in place; a dead store becomes POP, never deletion of its producer.
     */
    private static void cleanupTransport(MethodNode m, Set<Integer> slots) {
        boolean changed;
        do {
            changed = false;
            for (AbstractInsnNode n : m.instructions.toArray()) {
                if (FourthNormalizations.annotated(n)) continue;
                if (n instanceof VarInsnNode v && v.getOpcode() >= ISTORE && v.getOpcode() <= ASTORE
                        && slots.contains(v.var) && v.var >= LocalWebs.parameters(m) && unread(m, v)) {
                    m.instructions.set(n, new InsnNode(LocalWebs.width(v.getOpcode()) == 2 ? POP2 : POP));
                    changed = true; continue;
                }
                AbstractInsnNode after = n.getNext();
                // Do not cross any label: it could be a separate entry or handler boundary.
                if (after == null || FourthNormalizations.annotated(after)) continue;
                int op = n.getOpcode(), width = n instanceof VarInsnNode ? LocalWebs.width(op) : op == DUP2 ? 2 : 1;
                if ((op >= ILOAD && op <= ALOAD || op == DUP || op == DUP2)
                        && after.getOpcode() == (width == 2 ? POP2 : POP)) {
                    m.instructions.remove(n); m.instructions.remove(after); changed = true;
                }
            }
        } while (changed);
    }
    private static boolean unread(MethodNode m, VarInsnNode store) {
        for (int slot = store.var; slot < store.var + LocalWebs.width(store.getOpcode()); slot++)
            if (live(m, next(store), slot, Collections.newSetFromMap(new IdentityHashMap<>()))) return false;
        return true;
    }

    private record Value(boolean exception, int size) {}
    private static List<AbstractInsnNode> body(MethodNode m, AbstractInsnNode entry, List<TryCatchBlockNode> group) {
        if (!(entry instanceof VarInsnNode first) || first.getOpcode() != ASTORE) return null;
        AbstractInsnNode prev = entry.getPrevious();
        while (prev != null && prev.getOpcode() < 0) prev = prev.getPrevious();
        if (prev == null || !terminal(prev)) return null;
        List<AbstractInsnNode> result = new ArrayList<>();
        List<Value> stack = new ArrayList<>(List.of(new Value(true, 1)));
        Map<Integer, Value> locals = new HashMap<>();
        for (AbstractInsnNode n = entry; n != null; n = n.getNext()) {
            if (FourthNormalizations.annotated(n)) return null;
            int op = n.getOpcode();
            if (op < 0) continue;
            result.add(n);
            if (op == ATHROW) {
                if (stack.size() != 1 || !stack.get(0).exception) return null;
                Set<AbstractInsnNode> body = new HashSet<>(result);
                for (AbstractInsnNode x : m.instructions) {
                    if (x instanceof JumpInsnNode j && body.contains(next(j.label))) return null;
                    if (x instanceof TableSwitchInsnNode s && (body.contains(next(s.dflt)) || s.labels.stream().anyMatch(l -> body.contains(next(l))))) return null;
                    if (x instanceof LookupSwitchInsnNode s && (body.contains(next(s.dflt)) || s.labels.stream().anyMatch(l -> body.contains(next(l))))) return null;
                }
                for (TryCatchBlockNode x : m.tryCatchBlocks)
                    if (!group.contains(x) && body.contains(next(x.handler))) return null;
                return result;
            }
            if (op == NOP) continue;
            if (n instanceof VarInsnNode v && op >= ILOAD && op <= ALOAD) {
                int width = op == LLOAD || op == DLOAD ? 2 : 1;
                Value value = locals.getOrDefault(v.var, new Value(false, width));
                if (value.size != width || value.exception && op != ALOAD) return null;
                stack.add(value); continue;
            }
            if (n instanceof VarInsnNode v && op >= ISTORE && op <= ASTORE) {
                int width = op == LSTORE || op == DSTORE ? 2 : 1;
                if (stack.isEmpty()) return null;
                Value value = stack.remove(stack.size() - 1);
                if (value.size != width || value.exception && op != ASTORE) return null;
                locals.put(v.var, value);
                if (width == 2) locals.put(v.var + 1, new Value(false, 0));
                continue;
            }
            if (op == POP || op == POP2) {
                if (stack.isEmpty()) return null;
                Value value = stack.remove(stack.size() - 1);
                if (op == POP && value.size != 1) return null;
                if (op == POP2 && value.size == 1 && (stack.isEmpty() || stack.remove(stack.size() - 1).size != 1)) return null;
                continue;
            }
            if (op >= ACONST_NULL && op <= SIPUSH) {
                stack.add(new Value(false, op == LCONST_0 || op == LCONST_1 || op == DCONST_0 || op == DCONST_1 ? 2 : 1)); continue;
            }
            if (n instanceof LdcInsnNode l && (l.cst instanceof Number || l.cst instanceof String)) {
                stack.add(new Value(false, l.cst instanceof Long || l.cst instanceof Double ? 2 : 1)); continue;
            }
            // In particular: calls, fields, monitor, allocation, casts and arithmetic are never erased.
            return null;
        }
        return null;
    }

    private static boolean live(MethodNode m, AbstractInsnNode start, int slot, Set<AbstractInsnNode> seen) {
        if (start == null) return false;
        ArrayDeque<AbstractInsnNode> queue = new ArrayDeque<>(); queue.add(start);
        while (!queue.isEmpty()) {
            AbstractInsnNode n = queue.remove();
            if (!seen.add(n)) continue;
            if (n instanceof IincInsnNode i && i.var == slot) return true;
            if (n instanceof VarInsnNode v) {
                int op = v.getOpcode(), width = LocalWebs.width(op);
                if (slot >= v.var && slot < v.var + width) {
                    if (op < ISTORE) return true;
                    continue; // This path overwrites the value before observing it.
                }
            }
            if (!Normalizations.harmless(n, true))
                for (TryCatchBlockNode t : coverage(m, n)) enqueue(queue, next(t.handler));
            if (n instanceof JumpInsnNode j) enqueue(queue, next(j.label));
            if (n instanceof TableSwitchInsnNode sw) {
                enqueue(queue, next(sw.dflt)); for (LabelNode label : sw.labels) enqueue(queue, next(label));
            }
            if (n instanceof LookupSwitchInsnNode sw) {
                enqueue(queue, next(sw.dflt)); for (LabelNode label : sw.labels) enqueue(queue, next(label));
            }
            if (!terminal(n)) enqueue(queue, next(n));
        }
        return false;
    }
    private static void enqueue(ArrayDeque<AbstractInsnNode> queue, AbstractInsnNode n) { if (n != null) queue.add(n); }

    private static boolean sameDispatch(List<TryCatchBlockNode> a, List<TryCatchBlockNode> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (!Objects.equals(a.get(i).type, b.get(i).type) || next(a.get(i).handler) != next(b.get(i).handler)) return false;
        return true;
    }
    private static List<TryCatchBlockNode> coverage(MethodNode m, AbstractInsnNode n) {
        List<TryCatchBlockNode> result = new ArrayList<>(); int pos = m.instructions.indexOf(n);
        for (TryCatchBlockNode t : m.tryCatchBlocks)
            if (pos >= m.instructions.indexOf(t.start) && pos < m.instructions.indexOf(t.end)) {
                result.add(t);
            }
        return result;
    }
    private static AbstractInsnNode next(AbstractInsnNode n) {
        do { n = n.getNext(); } while (n != null && n.getOpcode() < 0); return n;
    }
    private static boolean terminal(AbstractInsnNode n) {
        int op = n.getOpcode(); return op == GOTO || op == ATHROW || op >= IRETURN && op <= RETURN || op == TABLESWITCH || op == LOOKUPSWITCH;
    }
}
