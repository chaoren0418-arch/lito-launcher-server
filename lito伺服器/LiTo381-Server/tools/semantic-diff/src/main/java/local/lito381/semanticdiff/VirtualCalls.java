package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Pair-local public dispatch proof; never changes cached classes or loads application code. */
final class VirtualCalls implements Opcodes {
    private VirtualCalls() {}

    static MethodNode[] normalize(MethodNode left, MethodNode right, JarIndex lj, JarIndex rj) {
        MethodNode[] unchanged = {left, right};
        if (!FourthNormalizations.enabled("virtual-owner")) return unchanged;
        MethodNode l = copy(lj.beforeWebs.getOrDefault(left, left));
        MethodNode r = copy(rj.beforeWebs.getOrDefault(right, right));
        List<MethodInsnNode> lc = calls(l), rc = calls(r);
        if (lc.size() != rc.size()) return unchanged;
        boolean candidate = false;
        for (int i = 0; i < lc.size(); i++) if (eligible(lc.get(i), rc.get(i))) candidate = true;
        if (!candidate) return unchanged;
        String lo = lj.methodOwners.get(left), ro = rj.methodOwners.get(right);
        if (lo == null || ro == null) return unchanged;
        try {
            Frame<BasicValue>[] lf = new Analyzer<>(new Types(lj)).analyze(lo, l);
            Frame<BasicValue>[] rf = new Analyzer<>(new Types(rj)).analyze(ro, r);
            boolean changed = false;
            for (int i = 0; i < lc.size(); i++) {
                MethodInsnNode a = lc.get(i), b = rc.get(i);
                if (!eligible(a, b) || !proof(a, b, lo, lj) || !proof(a, b, ro, rj)) continue;
                String lt = receiver(lf[l.instructions.indexOf(a)], a), rt = receiver(rf[r.instructions.indexOf(b)], b);
                if (lt == null || rt == null || !subtype(lt, a.owner, lj) || !subtype(lt, b.owner, lj)
                        || !subtype(rt, a.owner, rj) || !subtype(rt, b.owner, rj)) continue;
                // A common symbolic spelling is used only on these proven call sites.
                b.owner = a.owner; b.setOpcode(a.getOpcode()); b.itf = a.itf;
                changed = true;
            }
            if (!changed) return unchanged;
            register(l, lo, lj); register(r, ro, rj);
            return new MethodNode[]{l, r};
        } catch (AnalyzerException | IllegalArgumentException ex) {
            return unchanged;
        }
    }

    private static boolean virtual(MethodInsnNode c) { return c.getOpcode() == INVOKEVIRTUAL || c.getOpcode() == INVOKEINTERFACE; }
    private static boolean eligible(MethodInsnNode a, MethodInsnNode b) {
        return virtual(a) && virtual(b) && !a.owner.equals(b.owner) && a.name.equals(b.name) && a.desc.equals(b.desc);
    }
    private static List<MethodInsnNode> calls(MethodNode m) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode n : m.instructions) if (n instanceof MethodInsnNode c) result.add(c);
        return result;
    }
    private static String receiver(Frame<BasicValue> f, MethodInsnNode c) {
        if (f == null) return null;
        int index = f.getStackSize() - Type.getArgumentTypes(c.desc).length - 1;
        if (index < 0) return null;
        Type t = f.getStack(index).getType();
        return t != null && t.getSort() == Type.OBJECT && !t.getInternalName().equals("null") ? t.getInternalName() : null;
    }
    private static boolean proof(MethodInsnNode a, MethodInsnNode b, String caller, JarIndex jar) {
        ClassNode ac = jar.header(a.owner), bc = jar.header(b.owner);
        if (ac == null || bc == null
                || !accessible(ac, caller) || !accessible(bc, caller)
                || a.itf != ((ac.access & ACC_INTERFACE) != 0) || b.itf != ((bc.access & ACC_INTERFACE) != 0)
                || (a.getOpcode() == INVOKEINTERFACE) != a.itf || (b.getOpcode() == INVOKEINTERFACE) != b.itf) return false;
        MethodNode am = resolve(a.owner, a.name, a.desc, jar), bm = resolve(b.owner, b.name, b.desc, jar);
        return am != null && bm != null && (am.access & (ACC_PUBLIC | ACC_STATIC)) == ACC_PUBLIC
                && (bm.access & (ACC_PUBLIC | ACC_STATIC)) == ACC_PUBLIC
                && (subtype(a.owner, b.owner, jar) || subtype(b.owner, a.owner, jar));
    }
    private static boolean accessible(ClassNode c, String caller) {
        return (c.access & ACC_PUBLIC) != 0 || packageName(c.name).equals(packageName(caller));
    }
    private static String packageName(String name) { return name.substring(0, name.lastIndexOf('/') + 1); }

    static boolean subtype(String type, String parent, JarIndex jar) {
        Set<String> seen = new HashSet<>(); ArrayDeque<String> queue = new ArrayDeque<>(); queue.add(type);
        while (!queue.isEmpty()) {
            String name = queue.remove(); if (!seen.add(name)) continue;
            if (name.equals(parent)) return true;
            ClassNode c = jar.header(name); if (c == null) continue;
            if (c.superName != null) queue.add(c.superName); queue.addAll(c.interfaces);
        }
        return false;
    }

    private static MethodNode declared(ClassNode c, String name, String desc) {
        return c.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElse(null);
    }
    private static MethodNode resolve(String owner, String name, String desc, JarIndex jar) {
        Set<String> seen = new HashSet<>(), interfaces = new LinkedHashSet<>();
        String current = owner;
        while (current != null) {
            if (!seen.add(current)) return null;
            ClassNode c = jar.header(current); if (c == null) return null;
            MethodNode m = declared(c, name, desc); if (m != null) return m;
            interfaces.addAll(c.interfaces); current = c.superName;
        }
        Map<String, MethodNode> candidates = new LinkedHashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>(interfaces); seen.clear();
        while (!queue.isEmpty()) {
            current = queue.remove(); if (!seen.add(current)) continue;
            ClassNode c = jar.header(current); if (c == null) return null;
            MethodNode m = declared(c, name, desc);
            if (m != null && (m.access & (ACC_PRIVATE | ACC_STATIC)) == 0) candidates.put(current, m);
            queue.addAll(c.interfaces);
        }
        List<String> maximal = candidates.keySet().stream().filter(x -> candidates.keySet().stream()
                .noneMatch(y -> !x.equals(y) && subtype(y, x, jar))).toList();
        return maximal.size() == 1 ? candidates.get(maximal.get(0)) : null;
    }

    private static final class Types extends BasicInterpreter {
        private final JarIndex jar;
        Types(JarIndex jar) { super(ASM9); this.jar = jar; }
        @Override public BasicValue newValue(Type t) {
            if (t != null && (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY)) return new BasicValue(t);
            return super.newValue(t);
        }
        @Override public BasicValue merge(BasicValue a, BasicValue b) {
            if (a.equals(b)) return a;
            Type x = a.getType(), y = b.getType();
            if (x != null && y != null && x.getSort() == Type.OBJECT && y.getSort() == Type.OBJECT) {
                if (x.getInternalName().equals("null")) return b;
                if (y.getInternalName().equals("null")) return a;
                if (subtype(x.getInternalName(), y.getInternalName(), jar)) return b;
                if (subtype(y.getInternalName(), x.getInternalName(), jar)) return a;
            }
            // Unknown joins retain no guessed receiver type.
            return BasicValue.UNINITIALIZED_VALUE;
        }
    }
    private static MethodNode copy(MethodNode m) {
        MethodNode c = new MethodNode(m.access, m.name, m.desc, m.signature, m.exceptions.toArray(String[]::new)); m.accept(c); return c;
    }
    private static void register(MethodNode m, String owner, JarIndex jar) {
        jar.methodOwners.put(m, owner); jar.beforeWebs.put(m, copy(m));
        if (FourthNormalizations.enabled("webs") && !jar.physicalConstructor(owner, m)) LocalWebs.normalize(owner, m);
        Normalizations.cleanLabels(m);
    }
}
