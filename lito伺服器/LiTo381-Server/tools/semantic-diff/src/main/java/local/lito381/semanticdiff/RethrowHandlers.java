package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Directional, pair-dependent removal; never mutates cached trees or class bytes. */
final class RethrowHandlers implements Opcodes {
    private RethrowHandlers() {}

    static MethodNode normalize(MethodNode left, MethodNode right, JarIndex lj, JarIndex rj) {
        String owner = lj.methodOwners.get(left);
        if (!FourthNormalizations.enabled("rethrow") || owner == null
                || lj.extraMethods.getOrDefault(owner, Set.of()).isEmpty()) return left;
        MethodNode source = lj.beforeWebs.getOrDefault(left, left);
        MethodNode m = copy(source);
        Set<AbstractInsnNode> protectedInsns = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean changed = false;
        for (TryCatchBlockNode t : new ArrayList<>(m.tryCatchBlocks)) {
            List<AbstractInsnNode> body = body(m, t, owner, lj);
            if (body == null) continue;
            for (AbstractInsnNode n = t.start; n != t.end; n = n.getNext())
                if (n.getOpcode() >= 0) protectedInsns.add(n);
            m.tryCatchBlocks.remove(t);
            body.forEach(m.instructions::remove);
            changed = true;
        }
        if (!changed) return left;
        // javac jumps over the catch to its continuation. After removing the catch,
        // a jump to the immediately following instruction has no remaining effect.
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof JumpInsnNode j
                && j.getOpcode() == GOTO && next(n) == next(j.label) && !FourthNormalizations.annotated(n))
            m.instructions.remove(n);
        MethodNode r = rj.beforeWebs.getOrDefault(right, right);
        List<AbstractInsnNode> le = events(m), re = events(r);
        // Align potentially throwing operations, not physical local/label positions.
        // This is only a coverage proof; the full method comparer still proves equality.
        if (le.size() != re.size()) return left;
        for (int i = 0; i < le.size(); i++) {
            if (!ClassComparer.graphInsn(le.get(i), lj).equals(ClassComparer.graphInsn(re.get(i), rj))) return left;
            if (protectedInsns.contains(le.get(i)) && !coverage(r, re.get(i)).isEmpty()) return left;
        }
        Normalizations.cleanLabels(m);
        lj.beforeWebs.put(m, copy(m));
        if (FourthNormalizations.enabled("webs") && !lj.physicalConstructor(owner, m)) LocalWebs.normalize(owner, m);
        lj.methodOwners.put(m, owner);
        return m;
    }

    private static List<AbstractInsnNode> body(MethodNode m, TryCatchBlockNode t, String owner, JarIndex jar) {
        if (t.visibleTypeAnnotations != null || t.invisibleTypeAnnotations != null) return null;
        AbstractInsnNode store = next(t.handler), load = store == null ? null : next(store);
        AbstractInsnNode invoke = load == null ? null : next(load);
        if (!(store instanceof VarInsnNode s) || s.getOpcode() != ASTORE
                || !(load instanceof VarInsnNode l) || l.getOpcode() != ALOAD || l.var != s.var
                || !(invoke instanceof MethodInsnNode call) || call.getOpcode() != INVOKESTATIC
                || !helper(call, owner, t.type == null ? "java/lang/Throwable" : t.type, jar)) return null;
        List<AbstractInsnNode> result = new ArrayList<>(List.of(store, load, invoke));
        AbstractInsnNode after = next(invoke);
        if (after != null && (after.getOpcode() == ATHROW || after.getOpcode() == RETURN || after.getOpcode() == GOTO)) {
            // A shared continuation belongs to normal control flow, not the handler.
            if (!entryBetween(m, invoke, after, t)) result.add(after);
        } else if (after != null && !entryBetween(m, invoke, after, t)) return null;
        AbstractInsnNode last = result.get(result.size() - 1);
        for (AbstractInsnNode n = t.handler; n != last.getNext(); n = n.getNext()) {
            if (FourthNormalizations.annotated(n) || !coverage(m, n).isEmpty()) return null;
            if (n instanceof LabelNode label && referenced(m, label, t)) return null;
        }
        // No ordinary path may enter the handler by falling through.
        AbstractInsnNode prev = t.handler.getPrevious();
        while (prev != null && prev.getOpcode() < 0) {
            // Distinct labels at the same offset are the same entry point.
            if (prev instanceof LabelNode label && referenced(m, label, t)) return null;
            prev = prev.getPrevious();
        }
        if (prev == null || !terminal(prev)) return null;
        return result;
    }

    private static boolean helper(MethodInsnNode call, String owner, String caught, JarIndex jar) {
        // Same owner avoids introducing class initialization/linkage side effects.
        if (!call.owner.equals(owner) || call.itf
                || !jar.extraMethods.getOrDefault(owner, Set.of()).contains(call.name + call.desc)) return false;
        ClassNode c = jar.readTree(owner);
        if ((c.access & ACC_INTERFACE) != 0) return false;
        MethodNode h = c.methods.stream().filter(x -> x.name.equals(call.name) && x.desc.equals(call.desc)).findFirst().orElse(null);
        if (h == null || (h.access & (ACC_PRIVATE | ACC_STATIC | ACC_SYNCHRONIZED | ACC_NATIVE | ACC_ABSTRACT))
                != (ACC_PRIVATE | ACC_STATIC) || !h.tryCatchBlocks.isEmpty()) return false;
        Type[] args = Type.getArgumentTypes(h.desc);
        if (args.length != 1 || args[0].getSort() != Type.OBJECT || !subtype(caught, args[0].getInternalName(), jar)) return false;
        List<AbstractInsnNode> code = Arrays.stream(jar.beforeWebs.getOrDefault(h, h).instructions.toArray())
                .filter(n -> n.getOpcode() >= 0).toList();
        if (code.size() < 2 || !(code.get(0) instanceof VarInsnNode v) || v.getOpcode() != ALOAD || v.var != 0
                || code.get(code.size() - 1).getOpcode() != ATHROW || code.stream().anyMatch(FourthNormalizations::annotated)) return false;
        for (int i = 1; i < code.size() - 1; i++)
            if (!(code.get(i) instanceof TypeInsnNode cast) || cast.getOpcode() != CHECKCAST || !subtype(caught, cast.desc, jar)) return false;
        return true;
    }

    private static boolean subtype(String type, String target, JarIndex jar) {
        Set<String> seen = new HashSet<>();
        while (type != null && seen.add(type)) {
            if (type.equals(target)) return true;
            ClassNode c = jar.header(type);
            if (c == null) return false;
            type = c.superName;
        }
        return false;
    }

    private static boolean entryBetween(MethodNode m, AbstractInsnNode from, AbstractInsnNode to, TryCatchBlockNode own) {
        for (AbstractInsnNode n = from.getNext(); n != to; n = n.getNext())
            if (n instanceof LabelNode l && referenced(m, l, own)) return true;
        return false;
    }

    private static boolean referenced(MethodNode m, LabelNode label, TryCatchBlockNode own) {
        for (AbstractInsnNode n : m.instructions) {
            if (n instanceof JumpInsnNode j && j.label == label) return true;
            if (n instanceof TableSwitchInsnNode s && (s.dflt == label || s.labels.contains(label))) return true;
            if (n instanceof LookupSwitchInsnNode s && (s.dflt == label || s.labels.contains(label))) return true;
        }
        for (TryCatchBlockNode t : m.tryCatchBlocks)
            if (t != own && (t.handler == label || t.start == label || t.end == label)) return true;
        return false;
    }

    private static boolean terminal(AbstractInsnNode n) {
        int op = n.getOpcode();
        return op == GOTO || op == ATHROW || op >= IRETURN && op <= RETURN || op == TABLESWITCH || op == LOOKUPSWITCH;
    }

    /** Same harmless-operation policy as #174's per-instruction handler list. */
    private static List<AbstractInsnNode> events(MethodNode m) {
        boolean monitor = Arrays.stream(m.instructions.toArray()).anyMatch(n -> n.getOpcode() == MONITORENTER);
        return Arrays.stream(m.instructions.toArray()).filter(n -> n.getOpcode() >= 0 && !Normalizations.harmless(n, monitor)).toList();
    }

    private static List<TryCatchBlockNode> coverage(MethodNode m, AbstractInsnNode n) {
        int pos = m.instructions.indexOf(n);
        return m.tryCatchBlocks.stream().filter(t -> pos >= m.instructions.indexOf(t.start) && pos < m.instructions.indexOf(t.end)).toList();
    }

    private static AbstractInsnNode next(AbstractInsnNode n) {
        n = n.getNext();
        while (n != null && n.getOpcode() < 0) n = n.getNext();
        return n;
    }

    private static MethodNode copy(MethodNode m) {
        MethodNode result = new MethodNode(m.access, m.name, m.desc, m.signature, m.exceptions.toArray(String[]::new));
        m.accept(result);
        return result;
    }
}
