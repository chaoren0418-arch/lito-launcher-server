package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Unique reaching definitions after LocalWebs has separated reused physical slots. */
final class TemporaryValues implements Opcodes {
    private TemporaryValues() {}

    static Set<Integer> eligible(MethodNode method) throws AnalyzerException {
        Frame<SourceValue>[] frames = new Analyzer<>(new LocalWebs.Definitions()).analyze("Temporaries",method);
        Map<Integer,List<VarInsnNode>> uses = new HashMap<>();
        Set<Integer> blocked = new HashSet<>();
        for (AbstractInsnNode n : method.instructions) {
            if (n instanceof VarInsnNode v) uses.computeIfAbsent(v.var,k -> new ArrayList<>()).add(v);
            if (n instanceof IincInsnNode i) blocked.add(i.var);
        }
        Set<AbstractInsnNode> handlerCode = handlerCode(method);
        Set<Integer> result = new HashSet<>();
        for (var entry : uses.entrySet()) {
            int slot = entry.getKey();
            if (slot < LocalWebs.parameters(method) || blocked.contains(slot)) continue;
            List<VarInsnNode> stores = entry.getValue().stream().filter(v -> LocalWebs.store(v.getOpcode())).toList();
            if (stores.size() != 1) continue;
            VarInsnNode store = stores.get(0);
            int width = LocalWebs.width(store.getOpcode());
            boolean safe = !FourthNormalizations.annotated(store) && !handlerCode.contains(store);
            if (width == 2 && (uses.containsKey(slot+1) || blocked.contains(slot+1))) safe = false;
            if (uses.getOrDefault(slot-1,List.of()).stream().anyMatch(v -> LocalWebs.width(v.getOpcode()) == 2)) safe = false;
            for (VarInsnNode use : entry.getValue()) if (use != store) {
                Frame<SourceValue> frame = frames[method.instructions.indexOf(use)];
                if (use.getOpcode() != store.getOpcode()-(ISTORE-ILOAD) || FourthNormalizations.annotated(use)
                        || handlerCode.contains(use) || frame == null
                        || !frame.getLocal(slot).insns.equals(Set.of(store))) safe = false;
            }
            if (safe && entry.getValue().size() > 1) result.add(slot);
        }
        return result;
    }

    private static Set<AbstractInsnNode> handlerCode(MethodNode method) {
        Set<AbstractInsnNode> reached = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<AbstractInsnNode> pending = new ArrayDeque<>();
        for (TryCatchBlockNode t : method.tryCatchBlocks) pending.add(t.handler);
        while (!pending.isEmpty()) {
            AbstractInsnNode n = pending.removeFirst();
            if (!reached.add(n)) continue;
            int op = n.getOpcode();
            if (n instanceof JumpInsnNode j) pending.add(j.label);
            if (n instanceof TableSwitchInsnNode s) { pending.add(s.dflt); pending.addAll(s.labels); continue; }
            if (n instanceof LookupSwitchInsnNode s) { pending.add(s.dflt); pending.addAll(s.labels); continue; }
            if (op == GOTO || op == ATHROW || op >= IRETURN && op <= RETURN) continue;
            if (n.getNext() != null) pending.add(n.getNext());
        }
        return reached;
    }
}
