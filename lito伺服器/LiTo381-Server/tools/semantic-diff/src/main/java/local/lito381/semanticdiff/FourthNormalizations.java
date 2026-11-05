package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Issue 166: exact structural rewrites, performed only on in-memory trees. */
final class FourthNormalizations implements Opcodes {
    private FourthNormalizations() {}

    static boolean lambda(InvokeDynamicInsnNode i) {
        return i.bsm.getTag() == H_INVOKESTATIC
                && i.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                && i.bsm.getName().equals("metafactory") && i.bsmArgs.length == 3
                && i.bsmArgs[0] instanceof Type && i.bsmArgs[1] instanceof Handle
                && i.bsmArgs[2] instanceof Type;
    }

    static void prepare(ClassNode c, JarIndex jar) {
        if (enabled("captures")) captures(c, jar);
        if (enabled("forward")) forward(c, jar);
        for (MethodNode m : c.methods) {
            if (enabled("try")) mergeTry(m);
            MethodNode original = new MethodNode(m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));
            m.accept(original); jar.beforeWebs.put(m,original);
            // Approved constructor movement still reasons about original physical overwrites.
            if (enabled("webs") && !jar.physicalConstructor(c.name,m)) LocalWebs.normalize(c.name, m);
            Normalizations.cleanLabels(m);
        }
    }

    // Diagnostic ablation switch; all rules are enabled in normal invocations.
    static boolean enabled(String rule) {
        return !Arrays.asList(System.getProperty("semanticdiff.disable", "").split(",")).contains(rule);
    }

    static void forward(ClassNode c, JarIndex jar) {
        for (MethodNode m : new ArrayList<>(c.methods)) {
            if (m.access != (ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC)
                    || !m.tryCatchBlocks.isEmpty() || metadata(m) || jar.otherReference(c.name, m)) continue;
            List<AbstractInsnNode> code = Arrays.stream(m.instructions.toArray()).filter(n -> n.getOpcode() >= 0).toList();
            Type[] args = Type.getArgumentTypes(m.desc);
            if (code.size() != args.length + 2) continue;
            int slot = 0; boolean good = true;
            for (int j = 0; j < args.length; j++) {
                AbstractInsnNode n = code.get(j);
                if (!(n instanceof VarInsnNode v) || v.var != slot || v.getOpcode() != args[j].getOpcode(ILOAD)) good = false;
                slot += args[j].getSize();
            }
            if (!good || !(code.get(args.length) instanceof MethodInsnNode call)) continue;
            int tag = switch (call.getOpcode()) { case INVOKESTATIC -> H_INVOKESTATIC;
                case INVOKEVIRTUAL -> H_INVOKEVIRTUAL; case INVOKEINTERFACE -> H_INVOKEINTERFACE; default -> -1; };
            if (tag < 0 || call.name.startsWith("<") || call.owner.equals(c.name) && call.name.equals(m.name)) continue;
            List<Type> target = new ArrayList<>();
            if (tag != H_INVOKESTATIC) target.add(Type.getObjectType(call.owner));
            target.addAll(Arrays.asList(Type.getArgumentTypes(call.desc)));
            Type ret = Type.getReturnType(m.desc);
            if (!target.equals(Arrays.asList(args)) || !ret.equals(Type.getReturnType(call.desc))
                    || code.get(code.size()-1).getOpcode() != ret.getOpcode(IRETURN)
                    || code.stream().anyMatch(FourthNormalizations::annotated)) continue;
            boolean replaced = false;
            for (MethodNode caller : c.methods) for (AbstractInsnNode n : caller.instructions)
                if (n instanceof InvokeDynamicInsnNode i && lambda(i) && i.bsmArgs[1] instanceof Handle h
                        && h.getTag() == H_INVOKESTATIC && h.getOwner().equals(c.name)
                        && h.getName().equals(m.name) && h.getDesc().equals(m.desc)) {
                    i.bsmArgs[1] = new Handle(tag, call.owner, call.name, call.desc, call.itf); replaced = true;
                }
            if (replaced) c.methods.remove(m);
        }
    }

    static boolean annotated(AbstractInsnNode n) {
        return n.visibleTypeAnnotations != null || n.invisibleTypeAnnotations != null;
    }
    static boolean metadata(MethodNode m) {
        return m.signature != null || m.annotationDefault != null || m.visibleAnnotations != null
                || m.invisibleAnnotations != null || m.visibleTypeAnnotations != null || m.invisibleTypeAnnotations != null
                || m.visibleParameterAnnotations != null || m.invisibleParameterAnnotations != null
                || m.visibleLocalVariableAnnotations != null || m.invisibleLocalVariableAnnotations != null;
    }

    static void mergeTry(MethodNode m) {
        boolean changed;
        do {
            changed = false;
            for (int a = 0; a < m.tryCatchBlocks.size() && !changed; a++) {
                TryCatchBlockNode x = m.tryCatchBlocks.get(a);
                for (int b = a + 1; b < m.tryCatchBlocks.size(); b++) {
                    TryCatchBlockNode y = m.tryCatchBlocks.get(b);
                    if (!Objects.equals(x.type == null ? "java/lang/Throwable" : x.type,
                            y.type == null ? "java/lang/Throwable" : y.type) || next(x.handler) != next(y.handler)
                            || x.visibleTypeAnnotations != null || x.invisibleTypeAnnotations != null
                            || y.visibleTypeAnnotations != null || y.invisibleTypeAnnotations != null) continue;
                    boolean forward = next(x.end) == next(y.start);
                    if (!forward && next(y.end) != next(x.start)) continue;
                    // Preserve exception-table priority, including interleaved overlapping handlers.
                    int start = m.instructions.indexOf(y.start), end = m.instructions.indexOf(y.end);
                    boolean safe = true;
                    for (int k = a + 1; k < b; k++) {
                        TryCatchBlockNode t = m.tryCatchBlocks.get(k);
                        if (m.instructions.indexOf(t.start) < end && m.instructions.indexOf(t.end) > start) safe = false;
                    }
                    if (safe) { if (forward) x.end = y.end; else x.start = y.start;
                        m.tryCatchBlocks.remove(b); changed = true; break; }
                }
            }
        } while (changed);
    }
    static AbstractInsnNode next(AbstractInsnNode n) {
        while (n != null && n.getOpcode() < 0) n = n.getNext();
        return n;
    }

    static void captures(ClassNode c, JarIndex jar) {
        // Nested lambdas must be canonicalized before their callers: their reordered
        // capture loads determine the caller's first-use order as well.
        List<MethodNode> ordered = new ArrayList<>();
        Set<MethodNode> done = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<MethodNode> active = Collections.newSetFromMap(new IdentityHashMap<>());
        for (MethodNode m : c.methods) if (!captureOrder(c,m,done,active,ordered)) return;
        for (MethodNode m : ordered) {
            if (!m.name.startsWith("lambda$") || (m.access & (ACC_PRIVATE | ACC_STATIC)) != (ACC_PRIVATE | ACC_STATIC)
                    || metadata(m) || jar.otherReference(c.name, m)) continue;
            List<InvokeDynamicInsnNode> sites = new ArrayList<>();
            for (MethodNode caller : c.methods) for (AbstractInsnNode n : caller.instructions)
                if (n instanceof InvokeDynamicInsnNode i && lambda(i) && i.bsmArgs[1] instanceof Handle h
                        && h.getTag() == H_INVOKESTATIC && h.getOwner().equals(c.name)
                        && h.getName().equals(m.name) && h.getDesc().equals(m.desc)) sites.add(i);
            if (sites.isEmpty()) continue;
            Type[] all = Type.getArgumentTypes(m.desc), cap = Type.getArgumentTypes(sites.get(0).desc);
            int count = cap.length;
            if (count < 2 || count > all.length || !Arrays.equals(cap, Arrays.copyOf(all, count))) continue;
            List<List<VarInsnNode>> loads = new ArrayList<>(); boolean safe = true;
            for (InvokeDynamicInsnNode site : sites) {
                if (!Arrays.equals(cap, Type.getArgumentTypes(site.desc))) { safe = false; break; }
                List<VarInsnNode> list = new ArrayList<>(); AbstractInsnNode n = site.getPrevious();
                for (int j = count - 1; j >= 0; j--) {
                    // A label can be a branch entry: never reorder through it.
                    if (!(n instanceof VarInsnNode v) || v.getOpcode() != cap[j].getOpcode(ILOAD) || annotated(n)) { safe = false; break; }
                    list.add(0, v); n = n.getPrevious();
                }
                loads.add(list);
            }
            if (!safe) continue;
            int[] oldSlots = new int[all.length]; int total = 0;
            for (int j = 0; j < all.length; j++) { oldSlots[j] = total; total += all[j].getSize(); }
            List<Integer> order = new ArrayList<>();
            for (AbstractInsnNode n : m.instructions) {
                int slot = n instanceof VarInsnNode v ? v.var : n instanceof IincInsnNode i ? i.var : -1;
                for (int j = 0; j < count; j++) if (slot == oldSlots[j] && !order.contains(j)) order.add(j);
            }
            // Unused captures cannot be disambiguated by body use; keep the original order.
            if (order.size() != count) continue;
            for (int j = count; j < all.length; j++) order.add(j);
            int[] mapping = new int[total]; Type[] reordered = new Type[all.length]; int dest = 0;
            for (int j = 0; j < order.size(); j++) {
                int old = order.get(j); reordered[j] = all[old];
                for (int k = 0; k < all[old].getSize(); k++) mapping[oldSlots[old]+k] = dest++;
            }
            String desc = Type.getMethodDescriptor(Type.getReturnType(m.desc), reordered);
            if (c.methods.stream().anyMatch(other -> other != m && other.name.equals(m.name) && other.desc.equals(desc))) continue;
            for (AbstractInsnNode n : m.instructions) {
                if (n instanceof VarInsnNode v && v.var < total) v.var = mapping[v.var];
                if (n instanceof IincInsnNode i && i.var < total) i.var = mapping[i.var];
            }
            m.desc = desc;
            for (int s = 0; s < sites.size(); s++) {
                InvokeDynamicInsnNode site = sites.get(s); Handle h = (Handle) site.bsmArgs[1];
                site.bsmArgs[1] = new Handle(h.getTag(), h.getOwner(), h.getName(), desc, h.isInterface());
                site.desc = Type.getMethodDescriptor(Type.getReturnType(site.desc), Arrays.copyOf(reordered, count));
                // Replace operands, keeping instruction identity and annotations (none) stable.
                List<VarInsnNode> list = loads.get(s); int[] vars = list.stream().mapToInt(v -> v.var).toArray();
                for (int j = 0; j < count; j++) {
                    VarInsnNode v = list.get(j); v.var = vars[order.get(j)]; v.setOpcode(reordered[j].getOpcode(ILOAD));
                }
            }
        }
    }

    private static boolean captureOrder(ClassNode c, MethodNode m, Set<MethodNode> done,
            Set<MethodNode> active, List<MethodNode> out) {
        if (done.contains(m)) return true;
        if (!active.add(m)) return false;
        for (AbstractInsnNode n : m.instructions) if (n instanceof InvokeDynamicInsnNode i && lambda(i)
                && i.bsmArgs[1] instanceof Handle h && h.getOwner().equals(c.name)) {
            MethodNode child = c.methods.stream().filter(t -> t.name.equals(h.getName()) && t.desc.equals(h.getDesc())).findFirst().orElse(null);
            if (child != null && !captureOrder(c,child,done,active,out)) return false;
        }
        active.remove(m); done.add(m); out.add(m); return true;
    }
}
