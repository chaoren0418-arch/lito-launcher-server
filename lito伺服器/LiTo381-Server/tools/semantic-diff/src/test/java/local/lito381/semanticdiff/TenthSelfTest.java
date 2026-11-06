package local.lito381.semanticdiff;

import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** Issue 235: the same framed JARs are compared and executed, with no application loader. */
public final class TenthSelfTest implements Opcodes {
    private static int checks;
    private static final String RULE = "Fixture#f$own(I)I  private-rename  f  # approved: https://example.test/235\n";
    private static void ok(String label, boolean value) {
        if (!value) throw new AssertionError(label);
        checks++; System.out.println("PASS " + label);
    }
    public static void main(String[] args) throws Exception {
        work = Path.of(args[0]); bin = Path.of(args[1]); Files.createDirectories(work); Files.createDirectories(bin);
        Path rule = work.resolve("renames.txt"); Files.writeString(rule, "\uFEFF# approval\n\n" + RULE);
        for (int opcode : new int[]{INVOKEVIRTUAL, INVOKESPECIAL}) {
            for (String mode : List.of("same", "right-public", "left-retains", "body", "left-public", "overload",
                    "no-ancestor", "private-ancestor", "missing-left", "missing-right", "wrong-owner", "handle", "metadata")) {
                Path l = fixture(mode + opcode + "-l", true, mode, opcode);
                Path r = fixture(mode + opcode + "-r", false, mode, opcode);
                String json = compare(mode + opcode, l, r, rule, mode.equals("same"));
                if (mode.equals("same")) {
                    ok("pair audit", json.contains("private-rename f$own(I)I -> f(I)I"));
                    ok("markdown audit", Files.readString(work.resolve(mode + opcode + ".md")).contains("f$own(I)I -> f(I)I"));
                    compare("unapproved" + opcode, l, r, null, false);
                    compare("reverse" + opcode, r, l, rule, false);
                    ok("recursive private and ancestor JVM", execute(l) == 12 && execute(r) == 12);
                }
                if (List.of("right-public", "left-retains", "body").contains(mode)) {
                    int a = execute(l), b = execute(r);
                    ok("JVM " + mode + " " + a + "/" + b, a != b);
                    ok("JVM expected dispatch " + mode, mode.equals("right-public") ? a == 12 && b == 22
                            : mode.equals("left-retains") ? a == 32 && b == 12 : a == 13 && b == 12);
                }
            }
        }
        Path l = fixture("parse-l", true, "same", INVOKEVIRTUAL), r = fixture("parse-r", false, "same", INVOKEVIRTUAL);
        for (String invalid : List.of("Fixture#f$own(I)I private-rename f", RULE + RULE,
                "Fixture#f$own(I)I private-rename <init> # approved: https://example.test/235",
                "Fixture#f$own(I)I private-rename f extra # approved: https://example.test/235",
                RULE.replace("Fixture#", "Absent#"), RULE + "Fixture#f$own(I)I method-diff # approved: https://example.test/235\n",
                RULE + "Fixture#g(I)I private-rename f # approved: https://example.test/235\n")) {
            Files.writeString(rule, invalid); boolean rejected = false;
            try { compare("invalid", l, r, rule, true); }
            catch (IllegalArgumentException expected) { rejected = true; }
            ok("reject malformed / overlapping policy", rejected);
        }
        // A missing dependency must keep ONLY methods; providing it may establish the guard.
        Files.writeString(rule, RULE);
        Path parent = jar("classpath", List.of(parent("same")));
        Path childL = jar("child-l", List.of(child(true, "same", INVOKEVIRTUAL)));
        Path childR = jar("child-r", List.of(child(false, "same", INVOKESPECIAL)));
        compare("missing-ancestor", childL, childR, rule, false);
        compare("classpath-ancestor", childL, childR, rule, true,
                "--left-classpath", parent.toString(), "--right-classpath", parent.toString());
        System.out.println("TENTH SELFTEST ALL PASS checks=" + checks);
    }
    private static String compare(String name, Path l, Path r, Path rule, boolean same, String... extra) throws Exception {
        Path json = work.resolve(name + ".json");
        List<String> args = new ArrayList<>(List.of("--left", l.toString(), "--right", r.toString(), "--classes", "Fixture",
                "--json", json.toString(), "--md", work.resolve(name + ".md").toString()));
        if (rule != null) args.addAll(List.of("--deviations", rule.toString())); args.addAll(List.of(extra));
        ok(name + " CLI", SemanticDiff.run(args.toArray(String[]::new)) == 0);
        String text = Files.readString(json);
        ok(name + " status", text.contains(same ? "\"DEVIATION\": 1" : "\"DIFF\": 1"));
        if (!same && rule != null && !name.startsWith("reverse") && !name.startsWith("body") && !name.startsWith("wrong-owner")
                && !name.startsWith("handle") && !name.startsWith("metadata"))
            ok(name + " no paired deviation", !text.contains("\"status\": \"DEVIATION\""));
        return text;
    }
    private static Path fixture(String name, boolean left, String mode, int opcode) throws Exception {
        return jar(name, List.of(parent(mode), child(left, mode, opcode)));
    }
    private static void invoke(MethodNode m, int opcode, String owner, String name, String desc) {
        m.instructions.add(new MethodInsnNode(opcode, owner, name, desc, false));
    }
    private static void constructor(ClassNode c) {
        MethodNode m = method(c, ACC_PUBLIC, "<init>", "()V"); var(m, ALOAD, 0);
        invoke(m, INVOKESPECIAL, c.superName, "<init>", "()V"); ins(m, RETURN);
    }
    private static ClassNode parent(String mode) {
        ClassNode c = clazz(); c.name = "Parent"; constructor(c);
        if (!mode.equals("no-ancestor")) {
            MethodNode m = method(c, mode.equals("private-ancestor") ? ACC_PRIVATE : ACC_PUBLIC, "f", "(I)I"); ins(m, ICONST_1, IRETURN);
        }
        MethodNode dispatch = method(c, ACC_PUBLIC, "dispatch", "()I"); var(dispatch, ALOAD, 0); ins(dispatch, ICONST_0);
        invoke(dispatch, INVOKEVIRTUAL, "Parent", "f", "(I)I"); ins(dispatch, IRETURN);
        return framed(c);
    }
    private static ClassNode child(boolean left, String mode, int opcode) {
        ClassNode c = clazz(); c.superName = "Parent"; constructor(c);
        String own = left ? "f$own" : "f";
        int access = mode.equals(left ? "left-public" : "right-public") ? ACC_PUBLIC : ACC_PRIVATE;
        if (!(left && mode.equals("missing-left")) && !(!left && mode.equals("missing-right"))) {
            MethodNode f = method(c, access, own, "(I)I");
            if (left && mode.equals("metadata")) f.exceptions.add("java/lang/Exception");
            LabelNode recur = new LabelNode(); var(f, ILOAD, 1); f.instructions.add(new JumpInsnNode(IFGT, recur));
            ins(f, left && mode.equals("body") ? ICONST_3 : ICONST_2, IRETURN);
            f.instructions.add(recur); var(f, ALOAD, 0); var(f, ILOAD, 1); ins(f, ICONST_1, ISUB);
            invoke(f, opcode, "Fixture", own, "(I)I"); ins(f, IRETURN);
        }
        if (left && (mode.equals("left-retains") || mode.equals("overload"))) {
            MethodNode retained = method(c, ACC_PUBLIC, "f", mode.equals("overload") ? "()I" : "(I)I"); ins(retained, ICONST_3, IRETURN);
        }
        MethodNode run = method(c, ACC_PUBLIC, "run", "()I"); var(run, ALOAD, 0);
        invoke(run, INVOKEVIRTUAL, "Parent", "dispatch", "()I");
        run.instructions.add(new IntInsnNode(BIPUSH, 10)); ins(run, IMUL); var(run, ALOAD, 0); ins(run, ICONST_1);
        invoke(run, opcode, left && mode.equals("wrong-owner") ? "Parent" : "Fixture", own, "(I)I"); ins(run, IADD, IRETURN);
        if (mode.equals("handle")) {
            MethodNode handle = method(c, ACC_PUBLIC | ACC_STATIC, "handle", "()Ljava/lang/invoke/MethodHandle;");
            handle.instructions.add(new LdcInsnNode(new Handle(H_INVOKEVIRTUAL, "Fixture", own, "(I)I", false))); ins(handle, ARETURN);
        }
        return framed(c);
    }
    private static ClassNode framed(ClassNode c) {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        c.accept(w); ClassNode result = new ClassNode(); new ClassReader(w.toByteArray()).accept(result, 0); return result;
    }
    private static int execute(Path jar) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> c = loader.loadClass("Fixture"); return (Integer)c.getMethod("run").invoke(c.getConstructor().newInstance());
        }
    }
}
