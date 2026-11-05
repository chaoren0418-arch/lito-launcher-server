package local.lito381.semanticdiff;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** ASM fixtures exercise bytecode shapes that javac cannot reliably emit. No server code runs. */
public final class NormalizationSelfTest implements Opcodes {
    private static Path reports;
    private static Path bin;
    private static int checks;
    private static final String APPROVED = "Fixture#m()V # approved: https://example.test/issues/132#issuecomment-1\n";

    public static void main(String[] args) throws Exception {
        reports = Path.of(args[0]);
        bin = Path.of(args[1]);
        Files.createDirectories(reports);
        Files.createDirectories(bin);
        for (int i = -1; i <= 5; i++) {
            same("iconst-" + i, constant(new InsnNode(ICONST_0 + i), "()I"),
                    constant(new LdcInsnNode(i), "()I"));
        }
        same("bipush-sipush", constant(new IntInsnNode(BIPUSH, 123), "()I"), constant(new IntInsnNode(SIPUSH, 123), "()I"));
        same("sipush-ldc", constant(new IntInsnNode(SIPUSH, 123), "()I"), constant(new LdcInsnNode(123), "()I"));
        diff("int-value", constant(new IntInsnNode(BIPUSH, 123), "()I"), constant(new IntInsnNode(BIPUSH, 124), "()I"));
        for (int i = 0; i <= 1; i++) {
            same("long-" + i, constant(new InsnNode(LCONST_0 + i), "()J"), constant(new LdcInsnNode((long) i), "()J"));
            same("double-" + i, constant(new InsnNode(DCONST_0 + i), "()D"), constant(new LdcInsnNode((double) i), "()D"));
        }
        for (int i = 0; i <= 2; i++) same("float-" + i, constant(new InsnNode(FCONST_0 + i), "()F"), constant(new LdcInsnNode((float) i), "()F"));
        diff("long-value", constant(new InsnNode(LCONST_0), "()J"), constant(new LdcInsnNode(1L), "()J"));
        diff("float-value", constant(new InsnNode(FCONST_0), "()F"), constant(new LdcInsnNode(1f), "()F"));
        diff("double-value", constant(new InsnNode(DCONST_0), "()D"), constant(new LdcInsnNode(1d), "()D"));
        diff("float-negative-zero", constant(new InsnNode(FCONST_0), "()F"), constant(new LdcInsnNode(-0f), "()F"));
        diff("double-negative-zero", constant(new InsnNode(DCONST_0), "()D"), constant(new LdcInsnNode(-0d), "()D"));
        diff("float-nan-payload", constant(new LdcInsnNode(Float.intBitsToFloat(0x7fc00001)), "()F"), constant(new LdcInsnNode(Float.intBitsToFloat(0x7fc00002)), "()F"));
        for (String kind : List.of("method", "field")) {
            same(kind + "-synthetic", flags(kind, ACC_SYNTHETIC), flags(kind, 0));
            diff(kind + "-final", flags(kind, ACC_FINAL), flags(kind, 0));
        }
        diff("class-synthetic-preserved", flags("class", ACC_SYNTHETIC), flags("class", 0));
        same("catch-any", caught(null, 1), caught("java/lang/Throwable", 1));
        diff("catch-exception", caught(null, 1), caught("java/lang/Exception", 1));
        diff("catch-count", caught(null, 1), caught(null, 2));

        ClassNode normal = effects(false, 1);
        ClassNode stack = effects(true, 500);
        diff("unapproved-stack", normal, stack);
        compare("approved-stack", normal, stack, APPROVED, "SAME_EFFECTS");
        compare("approved-identical", normal, normal, APPROVED, "SAME_EFFECTS");
        ClassNode dup2 = clazz(), swap = clazz();
        for (ClassNode c : List.of(dup2, swap)) {
            MethodNode m = method(c, "()V");
            m.instructions.add(new InsnNode(ICONST_1)); m.instructions.add(new InsnNode(ICONST_2));
            m.instructions.add(new InsnNode(c == dup2 ? DUP2 : SWAP));
            if (c == dup2) m.instructions.add(new InsnNode(POP2));
            m.instructions.add(new InsnNode(POP2)); m.instructions.add(new InsnNode(RETURN));
        }
        compare("unapproved-dup2-swap", dup2, swap, null, "SAME_PATHS");
        compare("approved-dup2-swap", dup2, swap, APPROVED, "SAME_EFFECTS");
        ClassNode unlistedLeft = effects(false, 1), unlistedRight = effects(true, 500);
        MethodNode extra = new MethodNode(ACC_PUBLIC | ACC_STATIC, "unlisted", "()V", null, null);
        extra.instructions.add(new InsnNode(RETURN));
        unlistedLeft.methods.add(extra);
        MethodNode extraRight = new MethodNode(ACC_PUBLIC | ACC_STATIC, "unlisted", "()V", null, null);
        extraRight.instructions.add(new InsnNode(NOP));
        extraRight.instructions.add(new InsnNode(RETURN));
        unlistedRight.methods.add(extraRight);
        String mixed = compare("unlisted-same-class", unlistedLeft, unlistedRight, APPROVED, "SAME_PATHS");
        require(mixed.contains("\"method\": \"m()V\", \"status\": \"SAME_EFFECTS\""), "independent method result");

        effectDiff("invoke-order", m -> {
            List<MethodInsnNode> calls = new ArrayList<>();
            for (AbstractInsnNode n : m.instructions) if (n instanceof MethodInsnNode call) calls.add(call);
            calls.get(0).name = "second";
            calls.get(1).name = "first";
        });
        effectDiff("invoke-owner", m -> first(m, MethodInsnNode.class).owner = "Other");
        effectDiff("invoke-desc", m -> first(m, MethodInsnNode.class).desc = "(J)V");
        effectDiff("invoke-opcode", m -> first(m, MethodInsnNode.class).setOpcode(INVOKEVIRTUAL));
        effectDiff("field-owner", m -> first(m, FieldInsnNode.class).owner = "Other");
        effectDiff("field-name", m -> first(m, FieldInsnNode.class).name = "other");
        effectDiff("field-opcode", m -> first(m, FieldInsnNode.class).setOpcode(GETFIELD));
        effectDiff("array-store", m -> m.instructions.insert(new InsnNode(IASTORE)));
        for (int op = IASTORE; op <= SASTORE; op++) {
            final int opcode = op;
            effectDiff("array-store-" + op, m -> m.instructions.insert(new InsnNode(opcode)));
        }
        for (int op : new int[]{GETFIELD, PUTFIELD, PUTSTATIC}) {
            final int opcode = op;
            effectDiff("field-operation-" + op, m -> first(m, FieldInsnNode.class).setOpcode(opcode));
        }
        effectDiff("astore-count", m -> m.instructions.insert(new VarInsnNode(ASTORE, 400)));
        effectDiff("new-type", m -> first(m, TypeInsnNode.class).desc = "java/lang/String");
        effectDiff("athrow", m -> m.instructions.insert(new InsnNode(ATHROW)));
        effectDiff("monitor-enter", m -> m.instructions.insert(new InsnNode(MONITORENTER)));
        effectDiff("monitor-exit", m -> m.instructions.insert(new InsnNode(MONITOREXIT)));
        effectDiff("return-type", m -> m.instructions.set(m.instructions.getLast(), new InsnNode(IRETURN)));
        effectDiff("checkcast", m -> m.instructions.insert(new TypeInsnNode(CHECKCAST, "java/lang/String")));
        effectDiff("instanceof", m -> m.instructions.insert(new TypeInsnNode(INSTANCEOF, "java/lang/String")));
        effectDiff("constant-count", m -> m.instructions.insert(new InsnNode(ICONST_1)));
        effectDiff("constant-value", m -> m.instructions.set(m.instructions.getFirst(), new InsnNode(ICONST_2)));
        effectDiff("constant-type", m -> m.instructions.set(m.instructions.getFirst(), new InsnNode(FCONST_1)));
        effectDiff("branch-count", m -> { LabelNode end = new LabelNode(); m.instructions.insert(new JumpInsnNode(GOTO, end)); m.instructions.add(end); });
        effectDiff("switch-count", m -> { LabelNode end = new LabelNode(); m.instructions.insert(new TableSwitchInsnNode(0, 0, end, end)); m.instructions.add(end); });
        compare("effects-catch-any", caught(null, 1), caught("java/lang/Throwable", 1), APPROVED, "SAME_EFFECTS");
        compare("effects-catch-type", caught(null, 1), caught("java/lang/Exception", 1), APPROVED, "DIFF");
        compare("effects-catch-count", caught(null, 1), caught(null, 2), APPROVED, "DIFF");
        ClassNode ordered = pair(1, 2), reordered = pair(2, 1);
        compare("constant-bag-order", ordered, reordered, APPROVED, "SAME_EFFECTS");
        compare("constant-bag-multiplicity", pair(1, 1), pair(1, 2), APPROVED, "SAME_PATHS");
        ClassNode incrementLeft = pair(1, 2), incrementRight = pair(1, 2);
        incrementLeft.methods.get(0).instructions.insert(new IincInsnNode(1, 1));
        incrementRight.methods.get(0).instructions.insert(new IincInsnNode(500, 1));
        compare("iinc-local-slot", incrementLeft, incrementRight, APPROVED, "SAME_EFFECTS");
        first(incrementRight.methods.get(0), IincInsnNode.class).incr = 2;
        compare("iinc-constant", incrementLeft, incrementRight, APPROVED, "DIFF");
        for (int op : new int[]{CHECKCAST, INSTANCEOF}) {
            ClassNode typeLeft = pair(1, 2), typeRight = pair(1, 2);
            typeLeft.methods.get(0).instructions.insert(new TypeInsnNode(op, "java/lang/Object"));
            typeRight.methods.get(0).instructions.insert(new TypeInsnNode(op, "java/lang/String"));
            compare("type-operand-" + op, typeLeft, typeRight, APPROVED, "DIFF");
        }
        ClassNode switchLeft = pair(1, 2), switchRight = pair(1, 2);
        LabelNode sl = new LabelNode(), sr = new LabelNode();
        switchLeft.methods.get(0).instructions.add(sl); switchRight.methods.get(0).instructions.add(sr);
        switchLeft.methods.get(0).instructions.insert(new LookupSwitchInsnNode(sl, new int[]{1}, new LabelNode[]{sl}));
        switchRight.methods.get(0).instructions.insert(new LookupSwitchInsnNode(sr, new int[]{2}, new LabelNode[]{sr}));
        compare("switch-case-value", switchLeft, switchRight, APPROVED, "DIFF");
        ClassNode dynamic = dynamic("bootstrap"), changedDynamic = dynamic("otherBootstrap");
        compare("dynamic-same", dynamic, dynamic, APPROVED, "SAME_EFFECTS");
        compare("dynamic-bootstrap", dynamic, changedDynamic, APPROVED, "DIFF");

        Path a = writeJar("cli-left", normal), b = writeJar("cli-right", stack), c = writeJar("cli-allow", pair(1, 2));
        Path approved = reports.resolve("approved.txt");
        Files.writeString(approved, "\uFEFF# comment\n" + APPROVED, StandardCharsets.UTF_8);
        Path allow = reports.resolve("allow.txt");
        Files.writeString(allow, "Fixture#m()V\n", StandardCharsets.UTF_8);
        Path json = reports.resolve("overlay.json");
        SemanticDiff.run(new String[]{"--left", a.toString(), "--right", c.toString(), "--allow-right", b.toString(), "--allow-methods", allow.toString(), "--effects-methods", approved.toString(), "--json", json.toString()});
        require(Files.readString(json).contains("\"status\": \"SAME_EFFECTS\""), "effects use allow-right");
        SemanticDiff.run(new String[]{"--left", a.toString(), "--right", a.toString(), "--allow-right", c.toString(), "--allow-methods", allow.toString(), "--effects-methods", approved.toString(), "--json", json.toString()});
        require(Files.readString(json).contains("\"status\": \"DIFF\""), "allow-right is not a waiver");
        invalid("typo", a, b, "Fixture#typo()V", new String[0]);
        invalid("descriptor", a, b, "Fixture#m(I)V", new String[0]);
        invalid("wildcard", a, b, "Fixture#*", new String[0]);
        invalid("excluded", a, b, APPROVED, new String[]{"--exclude", "Fixture"});
        invalid("class", a, b, "Missing#m()V", new String[0]);
        invalid("empty", a, b, "# comment", new String[0]);
        ClassNode missing = effects(false, 1);
        missing.methods.clear();
        compare("approved-missing-left", missing, normal, APPROVED, "DIFF");
        ClassNode metadata = effects(true, 500);
        metadata.methods.get(0).access |= ACC_FINAL;
        compare("effects-method-metadata", normal, metadata, APPROVED, "DIFF");
        ClassNode syntheticLeft = pair(1, 2), syntheticRight = pair(1, 2);
        syntheticLeft.methods.get(0).name = "lambda$work$1";
        syntheticRight.methods.get(0).name = "lambda$work$2";
        compare("effects-exact-original-name", syntheticLeft, syntheticRight,
                "Fixture#lambda$work$2()V", "DIFF");
        secondBatch();
        System.out.println("NORMALIZATION SELFTEST ALL PASS checks=" + checks);
    }


    private static void secondBatch() throws Exception {
        ClassNode reference = clazz();
        reference.innerClasses.add(new InnerClassNode("Other$Ticket", "Other", "Ticket", ACC_PUBLIC));
        same("inner-reference-only", reference, clazz());
        for (boolean self : new boolean[]{true, false}) {
            ClassNode own = clazz();
            own.innerClasses.add(new InnerClassNode(self ? "Fixture" : "Fixture$Child",
                    self ? "Other" : "Fixture", "Child", ACC_PRIVATE));
            diff("inner-owned-" + self, own, clazz());
            ClassNode changed = clazz();
            changed.innerClasses.add(new InnerClassNode(self ? "Fixture" : "Fixture$Child",
                    self ? "Other" : "Fixture", "Child", ACC_PUBLIC));
            diff("inner-access-" + self, own, changed);
        }
        same("class-super", flags("class", ACC_SUPER), flags("class", 0));
        diff("class-final-preserved", flags("class", ACC_FINAL), flags("class", 0));
        same("implicit-constructor", constructor(false), clazz());
        diff("constructor-extra-code", constructor(true), clazz());
        diff("constructor-right-only", clazz(), constructor(false));
        ClassNode two = constructor(false);
        MethodNode overload = new MethodNode(ACC_PUBLIC, "<init>", "(I)V", null, null);
        overload.instructions.add(new InsnNode(RETURN)); two.methods.add(overload);
        diff("constructor-overload", two, clazz());
        ClassNode withRight = constructor(false);
        withRight.methods.get(0).desc = "(I)V";
        diff("constructor-right-has-init", constructor(false), withRight);
        ClassNode annotation = constructor(false);
        annotation.methods.get(0).visibleAnnotations = new ArrayList<>(List.of(new AnnotationNode("LMarker;")));
        diff("constructor-annotation", annotation, clazz());
        ClassNode changedOwner = constructor(false);
        first(changedOwner.methods.get(0), MethodInsnNode.class).owner = "Other";
        diff("constructor-super-call", changedOwner, clazz());

        same("slots-permutation", slots("()V", true, ISTORE, 8, ISTORE, 2, ILOAD, 8, ILOAD, 2),
                slots("()V", true, ISTORE, 2, ISTORE, 8, ILOAD, 2, ILOAD, 8));
        diff("slots-use-mismatch", slots("()V", true, ISTORE, 8, ISTORE, 2, ILOAD, 8),
                slots("()V", true, ISTORE, 2, ISTORE, 8, ILOAD, 8));
        diff("slots-bijection", slots("()V", true, ISTORE, 8, ISTORE, 8),
                slots("()V", true, ISTORE, 2, ISTORE, 3));
        compare("slots-parameter", slots("(II)V", true, ILOAD, 0), slots("(II)V", true, ILOAD, 1), null, "SAME_PATHS");
        diff("slots-this", slots("()V", false, ALOAD, 0), slots("()V", false, ALOAD, 1));
        same("slots-wide-parameters", slots("(JDI)V", false, ILOAD, 5, ISTORE, 10, ILOAD, 10),
                slots("(JDI)V", false, ILOAD, 5, ISTORE, 20, ILOAD, 20));
        diff("slots-wide-parameter-tail", slots("(J)V", true, ILOAD, 1), slots("(J)V", true, ILOAD, 3));
        for (int op : new int[]{LSTORE, DSTORE}) {
            same("slots-wide-" + op, slots("()V", true, op, 5, op - 33, 5, ISTORE, 7),
                    slots("()V", true, op, 20, op - 33, 20, ISTORE, 22));
            diff("slots-wide-overlap-" + op, slots("()V", true, op, 5, ISTORE, 6),
                    slots("()V", true, op, 20, ISTORE, 22));
            same("slots-wide-tail-first-" + op, slots("()V", true, ISTORE, 6, op, 5),
                    slots("()V", true, ISTORE, 21, op, 20));
            diff("slots-wide-tail-first-mismatch-" + op, slots("()V", true, ISTORE, 6, op, 5),
                    slots("()V", true, ISTORE, 22, op, 20));
        }
        ClassNode incLeft = slots("()V", true, ISTORE, 2), incRight = slots("()V", true, ISTORE, 20);
        incLeft.methods.get(0).instructions.insert(new IincInsnNode(2, 1));
        incRight.methods.get(0).instructions.insert(new IincInsnNode(20, 1));
        same("slots-iinc", incLeft, incRight);
        first(incRight.methods.get(0), IincInsnNode.class).var = 21;
        diff("slots-iinc-mismatch", incLeft, incRight);
        same("slots-ret", slots("()V", true, ASTORE, 2, RET, 2), slots("()V", true, ASTORE, 20, RET, 20));
        same("slots-annotation", annotatedSlot(3, 3), annotatedSlot(30, 30));
        diff("slots-annotation-target", annotatedSlot(3, 3), annotatedSlot(30, 31));

        Path extra = approval("extra", "\uFEFF# comment\nFixture#helper()V # approved: https://example.test/135\n");
        ClassNode helper = helper(ACC_PRIVATE | ACC_STATIC);
        compare("extra-approved", helper, clazz(), null, "SAME", "--extra-methods", extra.toString());
        diff("extra-unlisted", helper, clazz());
        ClassNode extraUnlisted = helper(ACC_PRIVATE | ACC_STATIC);
        method(extraUnlisted, "()V").instructions.add(new InsnNode(RETURN));
        compare("extra-other-method", extraUnlisted, clazz(), null, "DIFF", "--extra-methods", extra.toString());
        ClassNode changedHelper = helper(ACC_PRIVATE | ACC_STATIC);
        changedHelper.methods.get(0).instructions.insert(new MethodInsnNode(INVOKESTATIC,"Sink","changed","()V",false));
        compare("extra-both-present", helper, changedHelper, null, "DIFF", "--extra-methods", extra.toString());
        compare("extra-right-only-unapproved", clazz(), helper, null, "DIFF");
        for (int access : new int[]{ACC_PUBLIC | ACC_STATIC, ACC_PRIVATE, ACC_STATIC, ACC_PROTECTED | ACC_STATIC}) {
            invalidFlag("extra-access-" + access, helper(access), clazz(), "--extra-methods", extra);
        }
        invalidFlag("extra-typo", helper, clazz(), "--extra-methods", approval("extra-typo", "Fixture#helper(I)V"));
        invalidFlag("extra-wildcard", helper, clazz(), "--extra-methods", approval("extra-wildcard", "Fixture#*"));
        invalidFlag("extra-empty", helper, clazz(), "--extra-methods", approval("extra-empty", "# comment"));
        invalidFlag("extra-missing-left", clazz(), helper, "--extra-methods", extra);
        invalidFlag("extra-excluded", helper, clazz(), "--extra-methods", extra, "--exclude", "Fixture");
        ClassNode remapped = helper(ACC_PRIVATE | ACC_STATIC); remapped.methods.get(0).name = "lambda$work$2";
        invalidFlag("extra-exact-synthetic", remapped, clazz(), "--extra-methods",
                approval("extra-synthetic", "Fixture#lambda$work$1()V"));
        syntheticExtraTests();

        Path exempt = approval("metadata", "\uFEFF# comment\nFixture # approved: https://example.test/135\n");
        for (String metadata : List.of("synthetic", "outerClass", "outerMethod", "nestHost", "nestMembers", "innerSelf")) {
            ClassNode changed = clazz();
            switch (metadata) {
                case "synthetic" -> changed.access |= ACC_SYNTHETIC;
                case "outerClass" -> changed.outerClass = "Other";
                case "outerMethod" -> { changed.outerClass = "Other"; changed.outerMethod = "m"; changed.outerMethodDesc = "()V"; }
                case "nestHost" -> changed.nestHostClass = "Other";
                case "nestMembers" -> changed.nestMembers = new ArrayList<>(List.of("Fixture$Child"));
                case "innerSelf" -> changed.innerClasses.add(new InnerClassNode("Fixture", null, null, ACC_SYNTHETIC));
            }
            diff("metadata-unlisted-" + metadata, changed, clazz());
            compare("metadata-approved-" + metadata, changed, clazz(), null, "SAME", "--metadata-exempt", exempt.toString());
        }
        ClassNode initLeft = clazz(), initRight = clazz();
        MethodNode il = method(initLeft, "()V"), ir = method(initRight, "()V");
        il.name = ir.name = "<clinit>";
        il.instructions.add(new InsnNode(RETURN)); ir.instructions.add(new MethodInsnNode(INVOKESTATIC,"Sink","changed","()V",false)); ir.instructions.add(new InsnNode(RETURN));
        compare("metadata-clinit-preserved", initLeft, initRight, null, "DIFF", "--metadata-exempt", exempt.toString());
        ClassNode field = clazz(); field.fields.add(new FieldNode(ACC_STATIC, "x", "I", null, null));
        compare("metadata-field-preserved", field, clazz(), null, "DIFF", "--metadata-exempt", exempt.toString());
        ClassNode child = clazz(); child.innerClasses.add(new InnerClassNode("Fixture$Child", "Fixture", "Child", ACC_PUBLIC));
        compare("metadata-child-preserved", child, clazz(), null, "DIFF", "--metadata-exempt", exempt.toString());
        compare("metadata-final-preserved", flags("class", ACC_FINAL), flags("class", 0), null, "DIFF", "--metadata-exempt", exempt.toString());
        invalidFlag("metadata-typo", clazz(), clazz(), "--metadata-exempt", approval("metadata-typo", "Other"));
        invalidFlag("metadata-wildcard", clazz(), clazz(), "--metadata-exempt", approval("metadata-wildcard", "Fixture*"));
        invalidFlag("metadata-empty", clazz(), clazz(), "--metadata-exempt", approval("metadata-empty", "# comment"));
        invalidFlag("metadata-excluded", clazz(), clazz(), "--metadata-exempt", exempt, "--exclude", "Fixture");

        // Overlay must still demand its method even when extra/constructor rules could waive B's absence.
        Path allowJar = writeJar("extra-overlay-target", changedHelper);
        Path allowList = approval("extra-overlay", "Fixture#helper()V");
        compare("extra-overlay-compare", helper, clazz(), null, "DIFF", "--extra-methods", extra.toString(),
                "--allow-methods", allowList.toString(), "--allow-right", allowJar.toString());
    }

    private static void syntheticExtraTests() throws Exception {
        for (String prefix : List.of("lambda$work$", "access$")) {
            String approved = prefix + "1";
            Path list = approval("extra-" + prefix, "Fixture#" + approved + "()I");
            ClassNode left = clazz(), right = clazz();
            syntheticInt(left, prefix + "0", 1);
            syntheticInt(left, approved, 2);
            syntheticInt(right, approved, 1);
            compare("extra-shifted-shared-" + prefix, left, right, null, "DIFF",
                    "--extra-methods", list.toString());

            right.methods.get(0).instructions.set(right.methods.get(0).instructions.getFirst(), new LdcInsnNode(2));
            compare("extra-unapproved-remains-" + prefix, left, right, null, "DIFF",
                    "--extra-methods", list.toString());

            left = clazz(); right = clazz();
            syntheticInt(left, approved, 2);
            syntheticInt(left, prefix + "2", 1);
            syntheticInt(right, prefix + "9", 1);
            syntheticCalls(left, prefix + "2"); syntheticCalls(right, prefix + "9");
            compare("extra-first-renumbered-" + prefix, left, right, null, "SAME",
                    "--extra-methods", list.toString());
            right.methods.get(0).instructions.set(right.methods.get(0).instructions.getFirst(), new LdcInsnNode(3));
            compare("extra-retained-body-" + prefix, left, right, null, "DIFF",
                    "--extra-methods", list.toString());

            left = clazz(); right = clazz();
            syntheticInt(left, approved, 2);
            syntheticInt(left, approved, 1); left.methods.get(1).desc = "(I)I";
            syntheticInt(right, prefix + "9", 1); right.methods.get(0).desc = "(I)I";
            compare("extra-exact-descriptor-" + prefix, left, right, null, "SAME",
                    "--extra-methods", list.toString());

            left = clazz(); right = clazz();
            syntheticInt(left, approved, 2); syntheticInt(left, prefix + "2", 2);
            syntheticInt(right, prefix + "9", 2);
            syntheticCalls(left, approved); syntheticCalls(right, prefix + "9");
            compare("extra-call-target-preserved-" + prefix, left, right, null, "DIFF",
                    "--extra-methods", list.toString());

            left = clazz(); right = clazz();
            syntheticInt(left, approved, 2); syntheticInt(right, approved, 2);
            compare("extra-shared-identical-" + prefix, left, right, null, "SAME",
                    "--extra-methods", list.toString());
            right.methods.get(0).instructions.set(right.methods.get(0).instructions.getFirst(), new LdcInsnNode(3));
            compare("extra-shared-changed-" + prefix, left, right, null, "DIFF",
                    "--extra-methods", list.toString());
            compare("extra-shared-effects-changed-" + prefix, left, right,
                    "Fixture#" + approved + "()I", "DIFF", "--extra-methods", list.toString());

            Path overlay = writeJar("extra-synthetic-overlay-" + prefix, right);
            compare("extra-synthetic-overlay-" + prefix, left, clazz(), null, "DIFF",
                    "--extra-methods", list.toString(), "--allow-methods", list.toString(),
                    "--allow-right", overlay.toString());
        }
    }

    private static void syntheticInt(ClassNode c, String name, int value) {
        MethodNode m = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, name, "()I", null, null);
        m.instructions.add(new LdcInsnNode(value)); m.instructions.add(new InsnNode(IRETURN));
        c.methods.add(m);
    }

    private static void syntheticCalls(ClassNode c, String target) {
        MethodNode m = method(c, "()V");
        m.instructions.add(new MethodInsnNode(INVOKESTATIC, c.name, target, "()I", false));
        m.instructions.add(new InsnNode(POP));
        m.instructions.add(new LdcInsnNode(new Handle(H_INVOKESTATIC, c.name, target, "()I", false)));
        m.instructions.add(new InsnNode(POP)); m.instructions.add(new InsnNode(RETURN));
    }

    private static ClassNode constructor(boolean extra) {
        ClassNode c = clazz();
        MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null); c.methods.add(m);
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        if (extra) m.instructions.add(new InsnNode(NOP));
        m.instructions.add(new InsnNode(RETURN)); return c;
    }

    private static ClassNode slots(String desc, boolean isStatic, int... operands) {
        ClassNode c = clazz(); MethodNode m = method(c, desc);
        if (!isStatic) m.access &= ~ACC_STATIC;
        for (int i = 0; i < operands.length; i += 2) m.instructions.add(new VarInsnNode(operands[i], operands[i + 1]));
        m.instructions.add(new InsnNode(RETURN)); return c;
    }

    private static ClassNode annotatedSlot(int slot, int annotated) {
        ClassNode c = slots("()V", true, ASTORE, slot, ALOAD, slot);
        MethodNode m = c.methods.get(0);
        LabelNode start = new LabelNode(), end = new LabelNode();
        m.instructions.insert(start); m.instructions.add(end);
        m.visibleLocalVariableAnnotations = new ArrayList<>(List.of(new LocalVariableAnnotationNode(
                org.objectweb.asm.TypeReference.LOCAL_VARIABLE << 24, null,
                new LabelNode[]{start}, new LabelNode[]{end}, new int[]{annotated}, "LMarker;")));
        return c;
    }

    private static ClassNode helper(int access) {
        ClassNode c = clazz(); MethodNode m = method(c, "()V"); m.access = access; m.name = "helper";
        m.instructions.add(new InsnNode(RETURN)); return c;
    }

    private static Path approval(String name, String text) throws Exception {
        Path file = reports.resolve(name + ".txt"); Files.writeString(file, text, StandardCharsets.UTF_8); return file;
    }

    private static void invalidFlag(String name, ClassNode left, ClassNode right,
            String flag, Path file, String... more) throws Exception {
        Path json = reports.resolve(name + ".json");
        List<String> args = new ArrayList<>(List.of("--left", writeJar(name + "-left", left).toString(),
                "--right", writeJar(name + "-right", right).toString(), flag, file.toString(), "--json", json.toString()));
        args.addAll(List.of(more));
        try { SemanticDiff.run(args.toArray(String[]::new)); throw new AssertionError("accepted " + name); }
        catch (IllegalArgumentException expected) { require(!Files.exists(json), name + " no successful report"); }
        System.out.println("PASS " + name);
    }

    private static ClassNode clazz() {
        ClassNode c = new ClassNode();
        c.version = V17; c.access = ACC_PUBLIC; c.name = "Fixture"; c.superName = "java/lang/Object";
        return c;
    }

    private static MethodNode method(ClassNode c, String desc) {
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "m", desc, null, null);
        c.methods.add(m); return m;
    }

    private static ClassNode constant(AbstractInsnNode n, String desc) {
        ClassNode c = clazz(); MethodNode m = method(c, desc); m.instructions.add(n);
        int ret = switch (desc) { case "()J" -> LRETURN; case "()F" -> FRETURN; case "()D" -> DRETURN; default -> IRETURN; };
        m.instructions.add(new InsnNode(ret)); return c;
    }

    private static ClassNode flags(String kind, int flag) {
        ClassNode c = pair(1, 2);
        c.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "field", "I", null, null));
        switch (kind) { case "method" -> c.methods.get(0).access |= flag; case "field" -> c.fields.get(0).access |= flag; default -> c.access |= flag; }
        return c;
    }

    private static ClassNode pair(int x, int y) {
        ClassNode c = clazz(); MethodNode m = method(c, "()V");
        m.instructions.add(new LdcInsnNode(x)); m.instructions.add(new InsnNode(POP));
        m.instructions.add(new LdcInsnNode(y)); m.instructions.add(new InsnNode(POP));
        m.instructions.add(new InsnNode(RETURN)); return c;
    }

    private static ClassNode caught(String type, int count) {
        ClassNode c = clazz(); MethodNode m = method(c, "()V");
        LabelNode start = new LabelNode(), end = new LabelNode(), handler = new LabelNode(), done = new LabelNode();
        m.instructions.add(start); m.instructions.add(new MethodInsnNode(INVOKESTATIC, "Sink", "run", "()V", false)); m.instructions.add(end);
        m.instructions.add(new JumpInsnNode(GOTO, done)); m.instructions.add(handler);
        m.instructions.add(new VarInsnNode(ASTORE, 0)); m.instructions.add(done); m.instructions.add(new InsnNode(RETURN));
        for (int i = 0; i < count; i++) m.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, type));
        return c;
    }

    private static ClassNode effects(boolean stack, int slot) {
        ClassNode c = clazz(); MethodNode m = method(c, "()V");
        m.instructions.add(new InsnNode(ICONST_1));
        if (stack) { m.instructions.add(new InsnNode(DUP)); m.instructions.add(new InsnNode(POP)); }
        m.instructions.add(new MethodInsnNode(INVOKESTATIC, "Sink", "first", "(I)V", false));
        m.instructions.add(new MethodInsnNode(INVOKESTATIC, "Sink", "second", "()V", false));
        m.instructions.add(new FieldInsnNode(GETSTATIC, "Sink", "field", "I")); m.instructions.add(new InsnNode(POP));
        m.instructions.add(new InsnNode(ACONST_NULL)); m.instructions.add(new VarInsnNode(ASTORE, slot));
        m.instructions.add(new TypeInsnNode(NEW, "java/lang/Object")); m.instructions.add(new InsnNode(DUP));
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        m.instructions.add(new InsnNode(POP)); m.instructions.add(new InsnNode(RETURN)); return c;
    }

    private static ClassNode dynamic(String bootstrap) {
        ClassNode c = clazz(); MethodNode m = method(c, "()V");
        m.instructions.add(new InvokeDynamicInsnNode("run", "()V", new Handle(H_INVOKESTATIC, "Bootstrap", bootstrap,
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false)));
        m.instructions.add(new InsnNode(RETURN)); return c;
    }

    private static <T> T first(MethodNode m, Class<T> kind) {
        for (AbstractInsnNode n : m.instructions) if (kind.isInstance(n)) return kind.cast(n);
        throw new AssertionError(kind);
    }

    private static void effectDiff(String name, Consumer<MethodNode> change) throws Exception {
        ClassNode changed = effects(true, 500); change.accept(changed.methods.get(0));
        compare(name, effects(false, 1), changed, APPROVED, "DIFF");
    }

    private static Path writeJar(String name, ClassNode c) throws Exception {
        Path p = bin.resolve(name + ".jar");
        // ClassWriter(0) intentionally retains malformed negative-test opcodes too: this is a structural comparator test.
        ClassWriter writer = new ClassWriter(0);
        for (MethodNode m : c.methods) { m.maxStack = 8; m.maxLocals = 501; }
        c.accept(writer);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(p))) {
            out.putNextEntry(new ZipEntry(c.name + ".class")); out.write(writer.toByteArray()); out.closeEntry();
        }
        return p;
    }

    private static void same(String name, ClassNode a, ClassNode b) throws Exception { compare(name, a, b, null, "SAME"); }
    private static void diff(String name, ClassNode a, ClassNode b) throws Exception { compare(name, a, b, null, "DIFF"); }

    private static String compare(String name, ClassNode a, ClassNode b, String list, String expected, String... flags) throws Exception {
        Path left = writeJar(name + "-left", a), right = writeJar(name + "-right", b);
        Path json = reports.resolve(name + ".json"), md = reports.resolve(name + ".md");
        List<String> args = new ArrayList<>(List.of("--left", left.toString(), "--right", right.toString(), "--json", json.toString(), "--md", md.toString()));
        if (list != null) { Path file = reports.resolve(name + ".txt"); Files.writeString(file, list, StandardCharsets.UTF_8); args.addAll(List.of("--effects-methods", file.toString())); }
        args.addAll(List.of(flags));
        require(SemanticDiff.run(args.toArray(String[]::new)) == 0, name + " exit");
        String result = Files.readString(json);
        require(result.contains("\"status\": \"" + expected + "\""), name + " expected " + expected);
        require(result.contains("\"" + expected + "\": 1"), name + " count");
        if (expected.equals("SAME_EFFECTS")) {
            require(result.contains("\"SAME\": 0"), name + " not counted SAME");
            require(Files.readString(md).contains("| SAME_EFFECTS | 1 |"), name + " markdown count");
        }
        System.out.println("PASS " + name + " " + expected); return result;
    }

    private static void invalid(String name, Path left, Path right, String list, String[] flags) throws Exception {
        Path file = reports.resolve("invalid-" + name + ".txt"); Files.writeString(file, list, StandardCharsets.UTF_8);
        Path json = reports.resolve("invalid-" + name + ".json");
        List<String> args = new ArrayList<>(List.of("--left", left.toString(), "--right", right.toString(), "--effects-methods", file.toString(), "--json", json.toString()));
        args.addAll(List.of(flags));
        try { SemanticDiff.run(args.toArray(String[]::new)); throw new AssertionError("accepted " + name); }
        catch (IllegalArgumentException expected) { require(!Files.exists(json), name + " no successful report"); }
        System.out.println("PASS invalid-" + name);
    }

    private static void require(boolean yes, String message) {
        checks++; if (!yes) throw new AssertionError(message);
    }
}
