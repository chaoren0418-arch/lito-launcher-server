package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Elides transport only after proving every consumer's ordered value provenance.
 * Producers, calls, fields, monitors, branches and exception edges remain observable.
 */
final class TransportComparison implements Opcodes {
    record View(MethodNode method, Map<AbstractInsnNode,List<SourceValue>> inputs, int copies,
            Map<Set<AbstractInsnNode>,Integer> copyCounts) {}
    private TransportComparison() {}

    static boolean same(MethodNode a, MethodNode b, JarIndex aj, JarIndex bj, boolean layout) {
        if (!FourthNormalizations.enabled("transport")) return false;
        try {
            // First prove local rewrites without interpreting merged origins as equal unions.
            // The complete rewritten instruction stream (or approved CFG) must still match.
            MethodNode al = LocalTransport.normalize(a), bl = LocalTransport.normalize(b);
            if (layout ? LayoutComparison.same(al,bl,aj,bj)
                    : ClassComparer.instructions(al,aj).equals(ClassComparer.instructions(bl,bj))
                        && ClassComparer.tryCatch(al,aj).equals(ClassComparer.tryCatch(bl,bj))) return true;
        } catch (AnalyzerException | RuntimeException unsupported) {
            // A failed local proof must not disable the independent singleton-provenance proof.
        }
        try {
            View av = view(a), bv = view(b);
            return av != null && bv != null && av.copies == bv.copies
                    && LayoutComparison.transportSame(av,bv,aj,bj,layout);
        } catch (AnalyzerException | RuntimeException unsupported) { return false; }
    }

    static View view(MethodNode source) throws AnalyzerException {
        if (source.instructions.size() == 0 || source.visibleLocalVariableAnnotations != null
                || source.invisibleLocalVariableAnnotations != null) return null;
        MethodNode m = new MethodNode(source.access, source.name, source.desc, source.signature, source.exceptions.toArray(String[]::new));
        source.accept(m);
        Map<Integer,List<VarInsnNode>> locals = new HashMap<>();
        Set<Integer> blocked = new HashSet<>();
        for (AbstractInsnNode n : m.instructions) {
            if (n.getOpcode() == RET || n.getOpcode() == JSR) return null;
            // SourceInterpreter does not distinguish the several outputs of these shuffles.
            if (n.getOpcode() == DUP_X1 || n.getOpcode() == DUP_X2 || n.getOpcode() == DUP2_X1
                    || n.getOpcode() == DUP2_X2 || n.getOpcode() == SWAP) return null;
            if (n instanceof VarInsnNode v) {
                locals.computeIfAbsent(v.var,k -> new ArrayList<>()).add(v);
                if (FourthNormalizations.annotated(n)) blocked.add(v.var);
            }
            if (n instanceof IincInsnNode i) blocked.add(i.var);
        }
        Set<Integer> temps = new HashSet<>(); int copies = 0;
        LocalWebs.Definitions defs = new LocalWebs.Definitions();
        Frame<SourceValue>[] reaching = new Analyzer<>(defs).analyze("Transport",m);
        for (var entry : locals.entrySet()) {
            if (entry.getKey() < LocalWebs.parameters(m) || blocked.contains(entry.getKey())) continue;
            List<VarInsnNode> stores = entry.getValue().stream().filter(v -> LocalWebs.store(v.getOpcode())).toList();
            List<VarInsnNode> loads = entry.getValue().stream().filter(v -> LocalWebs.load(v.getOpcode())).toList();
            if (stores.size() != 1 || loads.isEmpty() || stores.size()+loads.size() != entry.getValue().size()) continue;
            VarInsnNode store = stores.get(0); boolean good = true;
            for (VarInsnNode load : loads) {
                Frame<SourceValue> f = reaching[m.instructions.indexOf(load)];
                if (f == null || load.getOpcode() != store.getOpcode() - (ISTORE-ILOAD)
                        || !f.getLocal(load.var).insns.equals(Set.of(store))) good = false;
            }
            if (good) { temps.add(entry.getKey()); copies += loads.size()-1; }
        }
        Provenance interpreter = new Provenance(temps);
        Frame<SourceValue>[] frames = new Analyzer<>(interpreter).analyze("Transport",m);
        Set<AbstractInsnNode> remove = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Set<AbstractInsnNode>,Integer> copyCounts = new HashMap<>();
        for (AbstractInsnNode n : m.instructions) {
            Frame<SourceValue> f = frames[m.instructions.indexOf(n)];
            if (n.getOpcode() >= 0 && f == null) return null;
            int op = n.getOpcode();
            if (n instanceof VarInsnNode v && temps.contains(v.var)) {
                remove.add(n);
                if (LocalWebs.store(op)) {
                    int count = (int)locals.get(v.var).stream().filter(x -> LocalWebs.load(x.getOpcode())).count()-1;
                    if (count > 0) copyCounts.merge(f.getStack(f.getStackSize()-1).insns,count,Integer::sum);
                }
            } else if ((op == DUP || op == DUP2) && !FourthNormalizations.annotated(n)) {
                SourceValue top = f.getStack(f.getStackSize()-1);
                remove.add(n); copyCounts.merge(top.insns,1,Integer::sum); copies++;
                if (op == DUP2 && top.getSize() == 1) { copyCounts.merge(f.getStack(f.getStackSize()-2).insns,1,Integer::sum); copies++; }
            }
        }
        // A path may discard an unneeded stack value while the local form simply stops reading it.
        // Its producer is retained, and duplication counts are checked per value origin below.
        for (AbstractInsnNode n : m.instructions) if ((n.getOpcode() == POP || n.getOpcode() == POP2)
                && !FourthNormalizations.annotated(n)) remove.add(n);
        // Re-execute final frames to obtain operands after fixed-point convergence.
        interpreter.inputs.clear();
        for (AbstractInsnNode n : m.instructions) if (n.getOpcode() >= 0) {
            Frame<SourceValue> f = new Frame<>(frames[m.instructions.indexOf(n)]);
            if (n.getOpcode() == POP || n.getOpcode() == POP2)
                interpreter.inputs.put(n,List.of(f.getStack(f.getStackSize()-1)));
            f.execute(n,interpreter);
        }
        for (AbstractInsnNode n : remove) { m.instructions.remove(n); interpreter.inputs.remove(n); }
        // SourceInterpreter merges alternatives into an unordered union, losing the predecessor
        // edge that selected each operand. Equal unions (and copy counts) do NOT prove equality:
        // branches loading (a,b)/(b,a) versus (b,a)/(a,b) differ at a joined ISUB.
        // Until we track edge/value correlations, transport requires one known producer per use
        // and per duplicated value. This gate applies to both ordered and layout-approved CFGs.
        for (List<SourceValue> operands : interpreter.inputs.values())
            for (SourceValue value : operands) if (value.insns.size() != 1) return null;
        for (Set<AbstractInsnNode> origins : copyCounts.keySet()) if (origins.size() != 1) return null;
        Normalizations.trimTry(m);
        if (FourthNormalizations.enabled("try")) FourthNormalizations.mergeTry(m);
        Normalizations.cleanLabels(m);
        return new View(m,interpreter.inputs,copies,copyCounts);
    }

    static final class Provenance extends SourceInterpreter {
        final Set<Integer> temps;
        final Map<AbstractInsnNode,List<SourceValue>> inputs = new IdentityHashMap<>();
        Provenance(Set<Integer> temps) { super(ASM9); this.temps=temps; }
        @Override public SourceValue newExceptionValue(TryCatchBlockNode t, Frame<SourceValue> f, Type type) {
            return new SourceValue(1,t.handler);
        }
        @Override public SourceValue copyOperation(AbstractInsnNode n, SourceValue v) {
            if (n instanceof VarInsnNode x && temps.contains(x.var) || n.getOpcode() == DUP || n.getOpcode() == DUP2) return v;
            if (LocalWebs.store(n.getOpcode()) || n.getOpcode() >= DUP_X1 && n.getOpcode() <= SWAP) inputs.put(n,List.of(v));
            return super.copyOperation(n,v);
        }
        @Override public SourceValue unaryOperation(AbstractInsnNode n, SourceValue a) {
            inputs.put(n,List.of(a)); return super.unaryOperation(n,a);
        }
        @Override public SourceValue binaryOperation(AbstractInsnNode n, SourceValue a, SourceValue b) {
            inputs.put(n,List.of(a,b)); return super.binaryOperation(n,a,b);
        }
        @Override public SourceValue ternaryOperation(AbstractInsnNode n, SourceValue a, SourceValue b, SourceValue c) {
            inputs.put(n,List.of(a,b,c)); return super.ternaryOperation(n,a,b,c);
        }
        @Override public SourceValue naryOperation(AbstractInsnNode n, List<? extends SourceValue> args) {
            inputs.put(n,new ArrayList<>(args)); return super.naryOperation(n,args);
        }
        @Override public void returnOperation(AbstractInsnNode n, SourceValue value, SourceValue expected) {
            inputs.put(n,List.of(value));
        }
    }
}
