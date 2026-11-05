package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Must analysis: a successful receiver use proves the same value non-null on normal edges only. */
final class NonNullChecks implements Opcodes {
    private NonNullChecks() {}

    static final class Aliases extends SourceInterpreter {
        Aliases() { super(ASM9); }
        @Override public SourceValue newParameterValue(boolean instance, int local, Type type) {
            return new SourceValue(type.getSize(), new InsnNode(NOP));
        }
        @Override public SourceValue newExceptionValue(TryCatchBlockNode t, Frame<SourceValue> f, Type type) {
            return new SourceValue(1, t.handler);
        }
        @Override public SourceValue copyOperation(AbstractInsnNode n, SourceValue v) { return v; }
    }
    static final class Flow extends Analyzer<SourceValue> {
        final Map<Integer,Set<Integer>> normal = new HashMap<>(), exceptional = new HashMap<>();
        Flow() { super(new Aliases()); }
        @Override protected void newControlFlowEdge(int from, int to) {
            normal.computeIfAbsent(from,k -> new HashSet<>()).add(to);
        }
        @Override protected boolean newControlFlowExceptionEdge(int from, int to) {
            exceptional.computeIfAbsent(from,k -> new HashSet<>()).add(to); return true;
        }
    }
    static AbstractInsnNode singleton(SourceValue v) {
        return v != null && v.insns.size() == 1 ? v.insns.iterator().next() : null;
    }
    static SourceValue receiver(AbstractInsnNode n, Frame<SourceValue> f) {
        int depth;
        int op = n.getOpcode();
        if (op == GETFIELD || op == ARRAYLENGTH || op == MONITORENTER || op == MONITOREXIT) depth = 1;
        else if (op == PUTFIELD) depth = 2;
        else if (op >= IALOAD && op <= SALOAD) depth = 2;
        else if (op >= IASTORE && op <= SASTORE) depth = 3;
        else if (n instanceof MethodInsnNode call && (op == INVOKEVIRTUAL || op == INVOKEINTERFACE
                || op == INVOKESPECIAL && !call.name.equals("<init>")))
            depth = Type.getArgumentTypes(call.desc).length + 1;
        else return null;
        return f.getStack(f.getStackSize() - depth);
    }
    static void normalize(String owner, MethodNode m) {
        if (!FourthNormalizations.enabled("nonnull") || m.instructions.size() == 0 || m.name.equals("<init>")
                || m.visibleLocalVariableAnnotations != null || m.invisibleLocalVariableAnnotations != null) return;
        boolean candidate = false;
        for (AbstractInsnNode n : m.instructions) if (n instanceof MethodInsnNode call
                && call.getOpcode() == INVOKESTATIC && !call.itf && call.owner.equals("java/util/Objects")
                && call.name.equals("requireNonNull") && call.desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;")) candidate = true;
        if (!candidate) return;
        for (AbstractInsnNode n : m.instructions) if (n.getOpcode() == JSR || n.getOpcode() == RET) return;
        try {
            Flow flow = new Flow();
            Frame<SourceValue>[] frames = flow.analyze(owner, m);
            AbstractInsnNode[] code = m.instructions.toArray();
            Map<Integer,Set<AbstractInsnNode>> facts = new HashMap<>();
            facts.put(0, Set.of());
            Deque<Integer> work = new ArrayDeque<>(); work.add(0);
            while (!work.isEmpty()) {
                int i = work.remove();
                if (frames[i] == null) continue;
                Set<AbstractInsnNode> before = facts.get(i), after = new HashSet<>(before);
                AbstractInsnNode receiver = singleton(receiver(code[i], frames[i]));
                if (receiver != null) after.add(receiver);
                // A producer in a loop represents a new runtime value at every evaluation.
                // Never carry a previous iteration's proof into its new output.
                after.remove(code[i]);
                for (int next : flow.normal.getOrDefault(i,Set.of())) merge(facts,work,next,after);
                for (int next : flow.exceptional.getOrDefault(i,Set.of())) {
                    Set<AbstractInsnNode> caught = new HashSet<>(before);
                    caught.remove(code[next]);
                    merge(facts,work,next,caught);
                }
            }
            Set<LabelNode> entries = new HashSet<>();
            for (AbstractInsnNode n : code) {
                if (n instanceof JumpInsnNode j) entries.add(j.label);
                if (n instanceof TableSwitchInsnNode s) { entries.add(s.dflt); entries.addAll(s.labels); }
                if (n instanceof LookupSwitchInsnNode s) { entries.add(s.dflt); entries.addAll(s.labels); }
            }
            for (TryCatchBlockNode t : m.tryCatchBlocks) entries.add(t.handler);
            Set<AbstractInsnNode> remove = new HashSet<>();
            for (int i = 0; i < code.length; i++) {
                if (!(code[i] instanceof MethodInsnNode call) || call.getOpcode() != INVOKESTATIC || call.itf
                        || !call.owner.equals("java/util/Objects") || !call.name.equals("requireNonNull")
                        || !call.desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;") || frames[i] == null) continue;
                AbstractInsnNode prev = call.getPrevious(), next = call.getNext();
                while (prev != null && prev.getOpcode() < 0 && !entries.contains(prev)) prev = prev.getPrevious();
                while (next != null && next.getOpcode() < 0 && !entries.contains(next)) next = next.getNext();
                if (prev == null || next == null || next.getOpcode() != POP
                        || !(prev.getOpcode() == DUP || prev instanceof VarInsnNode v && v.getOpcode() == ALOAD)
                        || FourthNormalizations.annotated(prev) || FourthNormalizations.annotated(call)
                        || FourthNormalizations.annotated(next)) continue;
                AbstractInsnNode value = singleton(frames[i].getStack(frames[i].getStackSize()-1));
                if (value != null && facts.getOrDefault(i,Set.of()).contains(value))
                    remove.addAll(List.of(prev,call,next));
            }
            if (remove.isEmpty()) return;
            for (AbstractInsnNode n : remove) m.instructions.remove(n);
            Normalizations.trimTry(m); Normalizations.cleanLabels(m);
        } catch (AnalyzerException | RuntimeException unsupported) {
            // No mutation occurs until the complete proof and removal plan have succeeded.
        }
    }
    private static void merge(Map<Integer,Set<AbstractInsnNode>> facts, Deque<Integer> work,
            int target, Set<AbstractInsnNode> incoming) {
        Set<AbstractInsnNode> value = new HashSet<>(incoming), old = facts.get(target);
        if (old != null) value.retainAll(old);
        if (!value.equals(old)) { facts.put(target,value); work.add(target); }
    }
}
