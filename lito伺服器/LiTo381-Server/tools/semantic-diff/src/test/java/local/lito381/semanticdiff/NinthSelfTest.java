package local.lito381.semanticdiff;

import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** Issue 216: compare and execute the SAME verified fixture JARs, in isolated loaders. */
public final class NinthSelfTest implements Opcodes {
    private static int checks;
    private static void ok(String name, boolean value) {
        if (!value) throw new AssertionError(name); checks++; System.out.println("PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        work = Path.of(args[0]); bin = Path.of(args[1]); Files.createDirectories(work); Files.createDirectories(bin);
        metadata();
        for (int mode = 0; mode <= 8; mode++) handlers(mode);
        for (int mode = 0; mode <= 7; mode++) virtuals(mode);
        storedValues();
        System.out.println("NINTH SELFTEST ALL PASS checks=" + checks);
    }
    private static String cli(String name, Path l, Path r, String owner, boolean same, String... extra) throws Exception {
        Path json = work.resolve(name + ".json");
        List<String> args = new ArrayList<>(List.of("--left", l.toString(), "--right", r.toString(), "--classes", owner,
                "--json", json.toString(), "--md", work.resolve(name + ".md").toString()));
        args.addAll(List.of(extra)); ok(name + " CLI", SemanticDiff.run(args.toArray(String[]::new)) == 0);
        String text = Files.readString(json);
        ok(name + (same ? " accepted" : " DIFF"), same ? text.contains("\"DIFF\": 0") : text.contains("\"DIFF\": 1"));
        return text;
    }
    private static void metadata() throws Exception {
        Path narrow = work.resolve("narrow.txt"), broad = work.resolve("broad.txt");
        Files.writeString(narrow, "Fixture enclosing-method # approved: https://example.test/216\n");
        Files.writeString(broad, "Fixture # approved: https://example.test/216\n");
        for (String kind : List.of("method", "descriptor", "outer", "nest", "synthetic", "inner", "field", "code")) {
            ClassNode a = meta(), b = meta();
            switch (kind) {
                case "method" -> b.outerMethod = "renamed";
                case "descriptor" -> b.outerMethodDesc = "(I)V";
                case "outer" -> b.outerClass = "Different";
                case "nest" -> b.nestHostClass = "Different";
                case "synthetic" -> b.access |= ACC_SYNTHETIC;
                case "inner" -> b.innerClasses.add(new InnerClassNode("Fixture$Child", "Fixture", "Child", ACC_PUBLIC));
                case "field" -> b.fields.add(new FieldNode(ACC_PUBLIC, "extra", "I", null, null));
                case "code" -> b.methods.get(0).instructions.set(b.methods.get(0).instructions.getFirst(), new InsnNode(ICONST_2));
            }
            Path l = jar("meta-" + kind + "-l", List.of(a)), r = jar("meta-" + kind + "-r", List.of(b));
            boolean same = kind.equals("method") || kind.equals("descriptor");
            String text = cli("meta-" + kind, l, r, "Fixture", same, "--metadata-exempt", narrow.toString());
            ok("narrow audit " + kind, text.contains("\"enclosingMethodExempt\": [\"Fixture\"]") && text.contains("\"metadataExempt\": []"));
            cli("meta-" + kind + "-reverse", r, l, "Fixture", same, "--metadata-exempt", narrow.toString());
            if (same) cli("meta-" + kind + "-unapproved", l, r, "Fixture", false);
            if (Set.of("method", "descriptor", "outer", "nest", "synthetic").contains(kind))
                cli("meta-" + kind + "-legacy", l, r, "Fixture", true, "--metadata-exempt", broad.toString());
        }
        Path l = jar("meta-parser", List.of(meta()));
        for (String line : List.of("Fixture unknown", "Fixture enclosing-method extra", "Absent enclosing-method")) {
            Files.writeString(narrow, line + " # approved: https://example.test/216\n");
            boolean rejected = false;
            try { SemanticDiff.run(new String[]{"--left", l.toString(), "--right", l.toString(), "--metadata-exempt", narrow.toString()}); }
            catch (IllegalArgumentException expected) { rejected = true; }
            ok("reject metadata " + line, rejected);
        }
    }
    private static ClassNode meta() {
        ClassNode c = clazz(); c.outerClass = "Outer"; c.outerMethod = "original"; c.outerMethodDesc = "()V";
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "m", "()I"); ins(m, ICONST_1, IRETURN); return c;
    }
    private static void invoke(MethodNode m, int opcode, String owner, String name, String desc, boolean itf) {
        m.instructions.add(new MethodInsnNode(opcode, owner, name, desc, itf));
    }
    // 0 identity; 1 side-effect call before POP; 2 new exception; 3 real MONITOREXIT;
    // 4 different outer destination; 5 outer reads a catch write; 6 shared handler / dead stores.
    private static void handlers(int mode) throws Exception {
        String name = "pure-" + mode;
        Path l = jar(name + "-l", List.of(framed(handler(true, mode))));
        Path r = jar(name + "-r", List.of(framed(handler(false, mode))));
        boolean same = mode == 0 || mode == 6 || mode == 8;
        cli(name, l, r, "Fixture", same); cli(name + "-reverse", r, l, "Fixture", same);
        Object a = execute(l, "Fixture"), b = execute(r, "Fixture");
        ok("JVM " + name + " left=" + a + " right=" + b, a.equals(b) == same);
        ok("JVM " + name + " verified returns", a instanceof Integer && b instanceof Integer);
        if (same) {
            System.setProperty("semanticdiff.disable", "pure-rethrow");
            try { cli(name + "-ablation", l, r, "Fixture", false); }
            finally { System.clearProperty("semanticdiff.disable"); }
        }
    }
    private static ClassNode handler(boolean caught, int mode) {
        ClassNode c = clazz(); c.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "logs", "I", null, null));
        MethodNode log = method(c, ACC_STATIC, "log", "()I");
        ins(log, ICONST_1); log.instructions.add(new FieldInsnNode(PUTSTATIC, "Fixture", "logs", "I")); ins(log, ICONST_0, IRETURN);
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "m", "()I");
        // Initial value remains observable when the catch's ASTORE is removed in mode 5.
        ins(m, ACONST_NULL); var(m, ASTORE, 1);
        m.instructions.add(new TypeInsnNode(NEW, "java/lang/IllegalArgumentException")); ins(m, DUP);
        invoke(m, INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "()V", false); var(m, ASTORE, 0);
        if (caught && (mode == 6 || mode == 7)) {
            var(m, ALOAD, 0); ins(m, DUP); var(m, ASTORE, 4); ins(m, POP); var(m, ALOAD, 4); ins(m, POP);
            if (mode == 7) { invoke(m, INVOKESTATIC, "Fixture", "log", "()I", false); ins(m, POP); }
        }
        LabelNode start = new LabelNode(), mid = new LabelNode(), end = new LabelNode(), h = new LabelNode(), he = new LabelNode();
        LabelNode outer = new LabelNode(), alternate = new LabelNode();
        m.instructions.add(start);
        if (mode == 6) {
            // Two disjoint table entries share the same identity handler.
            ins(m, ICONST_0); LabelNode second = new LabelNode(); m.instructions.add(new JumpInsnNode(IFEQ, second));
            var(m, ALOAD, 0); ins(m, ATHROW); m.instructions.add(mid); m.instructions.add(second);
        }
        var(m, ALOAD, 0); ins(m, ATHROW); m.instructions.add(end);
        if (caught) {
            m.instructions.add(h); var(m, ASTORE, 1);
            if (mode == 1) { invoke(m, INVOKESTATIC, "Fixture", "log", "()I", false); ins(m, POP); }
            else if (mode == 2) {
                m.instructions.add(new TypeInsnNode(NEW, "java/lang/IllegalStateException")); ins(m, DUP);
                invoke(m, INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false); var(m, ASTORE, 1);
            } else { var(m, ALOAD, mode == 6 || mode == 7 ? 4 : 0); ins(m, mode == 3 ? MONITOREXIT : POP); }
            if (mode == 6) { ins(m, ICONST_5); var(m, ISTORE, 2); ins(m, LCONST_1, POP2); }
            var(m, ALOAD, 1); ins(m, ATHROW); m.instructions.add(he);
            m.tryCatchBlocks.add(new TryCatchBlockNode(start, mode == 6 ? mid : end, h, mode == 8 ? "java/lang/RuntimeException" : null));
            if (mode == 6) m.tryCatchBlocks.add(new TryCatchBlockNode(mid, end, h, null));
        }
        m.instructions.add(outer); var(m, ASTORE, 3);
        if (mode == 5) { var(m, ALOAD, 1); LabelNode no = new LabelNode(); m.instructions.add(new JumpInsnNode(IFNULL, no));
            ins(m, ICONST_2, IRETURN); m.instructions.add(no); ins(m, ICONST_1, IRETURN);
        } else {
            var(m, ALOAD, 3); m.instructions.add(new TypeInsnNode(INSTANCEOF, "java/lang/IllegalArgumentException"));
            m.instructions.add(new FieldInsnNode(GETSTATIC, "Fixture", "logs", "I")); ins(m, ICONST_2, IMUL, IADD, IRETURN);
        }
        if (mode == 4 && caught) { m.instructions.add(alternate); ins(m, POP, ICONST_5, IRETURN); }
        m.tryCatchBlocks.add(new TryCatchBlockNode(start, end, outer, null));
        if (caught) m.tryCatchBlocks.add(new TryCatchBlockNode(h, he, mode == 4 ? alternate : outer, null));
        return c;
    }

    private static Object execute(Path path, String owner) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{path.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            try { return loader.loadClass(owner.replace('/', '.')).getMethod("m").invoke(null); }
            catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof VerifyError) throw new AssertionError("invalid fixture", cause);
                return cause.getClass().getName();
            }
        }
    }
    private static ClassNode type(String name, String parent) { ClassNode c = clazz(); c.name = name; c.superName = parent; return c; }
    private static void ctor(ClassNode c) {
        MethodNode m = method(c, ACC_PUBLIC, "<init>", "()V"); var(m, ALOAD, 0);
        invoke(m, INVOKESPECIAL, c.superName, "<init>", "()V", false); ins(m, RETURN);
    }
    private static void value(ClassNode c, int access, int v) { MethodNode m = method(c, access, "f", "()I"); ins(m, ICONST_0 + v, IRETURN); }
    // 0 public override; 1 cross-package package-private; 2 interface invokespecial;
    // 3 unrelated interfaces (valid linkage failure); 4 public interface chain; 5 unresolved owner method.
    private static void virtuals(int mode) throws Exception {
        String name = "virtual-" + mode;
        Path l = jar(name + "-l", virtualFixture(true, mode)), r = jar(name + "-r", virtualFixture(false, mode));
        boolean same = mode == 0 || mode == 4;
        cli(name, l, r, "p/Fixture", same); cli(name + "-reverse", r, l, "p/Fixture", same);
        Object a = execute(l, "p/Fixture"), b = execute(r, "p/Fixture");
        ok("JVM " + name + " left=" + a + " right=" + b, a.equals(b) == same);
        if (mode == 0 || mode == 1 || mode == 2 || mode == 4) ok("JVM " + name + " actual integers", a instanceof Integer && b instanceof Integer);
        if (same) {
            System.setProperty("semanticdiff.disable", "virtual-owner");
            try { cli(name + "-ablation", l, r, "p/Fixture", false); }
            finally { System.clearProperty("semanticdiff.disable"); }
        }
    }
    private static List<ClassNode> virtualFixture(boolean left, int mode) {
        ClassNode a = type("p/Base", "java/lang/Object"), b = type("q/Child", mode <= 1 || mode >= 5 ? "p/Base" : "java/lang/Object");
        ClassNode c = type("p/Fixture", "java/lang/Object");
        if (mode <= 1 || mode >= 5) {
            ctor(a); ctor(b); value(a, mode == 1 ? 0 : ACC_PUBLIC, 1); value(b, ACC_PUBLIC, 2);
            if (mode == 5) a.methods.removeIf(m -> m.name.equals("f"));
            if (mode == 6) a.methods.stream().filter(m -> m.name.equals("f")).forEach(m -> m.access = ACC_PRIVATE);
            if (mode == 7) {
                a.methods.stream().filter(m -> m.name.equals("f")).forEach(m -> m.access |= ACC_STATIC);
                b.methods.stream().filter(m -> m.name.equals("f")).forEach(m -> m.access |= ACC_STATIC);
            }
            MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "m", "()I");
            m.instructions.add(new TypeInsnNode(NEW, b.name)); ins(m, DUP); invoke(m, INVOKESPECIAL, b.name, "<init>", "()V", false);
            var(m, ASTORE, 0); if (mode != 7) var(m, ALOAD, 0);
            invoke(m, mode == 7 ? INVOKESTATIC : INVOKEVIRTUAL, left ? a.name : b.name, "f", "()I", false); ins(m, IRETURN);
        } else {
            a.access = b.access = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;
            if (mode == 4) b.interfaces.add(a.name);
            value(a, ACC_PUBLIC, 1); value(b, ACC_PUBLIC, 2);
            c.interfaces.add(a.name); if (mode != 3) c.interfaces.add(b.name);
            ctor(c);
            if (mode == 2) {
                MethodNode own = method(c, ACC_PUBLIC, "f", "()I"); var(own, ALOAD, 0);
                invoke(own, INVOKESPECIAL, left ? a.name : b.name, "f", "()I", true); ins(own, IRETURN);
                // Compare the special caller as well as the static entry point.
            } else if (mode == 4) value(c, ACC_PUBLIC, 3);
            MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "m", "()I");
            m.instructions.add(new TypeInsnNode(NEW, c.name)); ins(m, DUP); invoke(m, INVOKESPECIAL, c.name, "<init>", "()V", false);
            invoke(m, mode == 2 ? INVOKEVIRTUAL : INVOKEINTERFACE, mode == 2 ? c.name : left ? a.name : b.name, "f", "()I", mode != 2); ins(m, IRETURN);
        }
        return List.of(framed(a), framed(b), framed(c));
    }

    private static void storedValues() throws Exception {
        for (boolean wide : new boolean[]{false, true}) for (boolean changed : new boolean[]{false, true}) {
            String name = "stored-" + wide + "-" + changed;
            Path l = jar(name + "-l", List.of(stored(true, false, wide))), r = jar(name + "-r", List.of(stored(false, changed, wide)));
            cli(name, l, r, "Fixture", !changed); cli(name + "-reverse", r, l, "Fixture", !changed);
            JarIndex lj = JarIndex.load(l), rj = JarIndex.load(r);
            MethodNode lm = lj.readTree("Fixture").methods.get(0), rm = rj.readTree("Fixture").methods.get(0);
            ok(name + " direct CFG", CfgComparison.compare(lm, rm, lj, rj).same() == !changed);
            Object a = execute(l, "Fixture"), b = execute(r, "Fixture");
            ok("JVM " + name + " left=" + a + " right=" + b, a.equals(b) == !changed);
            ok(name + " integers", a instanceof Integer && b instanceof Integer);
        }
    }
    private static ClassNode stored(boolean dup, boolean changed, boolean wide) {
        ClassNode c = clazz(); MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "m", "()I");
        ins(m, wide ? LCONST_1 : ICONST_1);
        if (dup) ins(m, wide ? DUP2 : DUP);
        var(m, wide ? LSTORE : ISTORE, 0);
        if (changed) { ins(m, wide ? LCONST_0 : ICONST_0); var(m, wide ? LSTORE : ISTORE, 0); }
        if (!dup) var(m, wide ? LLOAD : ILOAD, 0);
        // The slot is live across a block boundary, so ordinary temporary elimination cannot hide it.
        LabelNode join = new LabelNode(); m.instructions.add(new JumpInsnNode(GOTO, join)); m.instructions.add(join);
        var(m, wide ? LLOAD : ILOAD, 0); ins(m, wide ? LADD : IADD);
        if (wide) ins(m, L2I); ins(m, IRETURN); return framed(c);
    }
}
