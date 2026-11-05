package local.lito381.semanticdiff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Semantic comparison of two ASM ClassNodes after debug stripping and synthetic-name remap. */
final class ClassComparer {
    private ClassComparer() {}

    static final String[] OPCODES = (
            "NOP,ACONST_NULL,ICONST_M1,ICONST_0,ICONST_1,ICONST_2,"
                    + "ICONST_3,ICONST_4,ICONST_5,LCONST_0,LCONST_1,FCONST_0,FCONST_1,FCONST_2,DCONST_0,"
                    + "DCONST_1,BIPUSH,SIPUSH,LDC,,,ILOAD,LLOAD,FLOAD,DLOAD,ALOAD,,,,,,,,,,,,,,,,,,,,,IALOAD,"
                    + "LALOAD,FALOAD,DALOAD,AALOAD,BALOAD,CALOAD,SALOAD,ISTORE,LSTORE,FSTORE,DSTORE,ASTORE,,,,,,,,,,,,,,,,,,,,,IASTORE,"
                    + "LASTORE,FASTORE,DASTORE,AASTORE,BASTORE,CASTORE,SASTORE,POP,POP2,DUP,DUP_X1,DUP_X2,DUP2,"
                    + "DUP2_X1,DUP2_X2,SWAP,IADD,LADD,FADD,DADD,ISUB,LSUB,FSUB,DSUB,IMUL,LMUL,FMUL,DMUL,IDIV,"
                    + "LDIV,FDIV,DDIV,IREM,LREM,FREM,DREM,INEG,LNEG,FNEG,DNEG,ISHL,LSHL,ISHR,LSHR,IUSHR,LUSHR,"
                    + "IAND,LAND,IOR,LOR,IXOR,LXOR,IINC,I2L,I2F,I2D,L2I,L2F,L2D,F2I,F2L,F2D,D2I,D2L,D2F,I2B,"
                    + "I2C,I2S,LCMP,FCMPL,FCMPG,DCMPL,DCMPG,IFEQ,IFNE,IFLT,IFGE,IFGT,IFLE,IF_ICMPEQ,IF_ICMPNE,"
                    + "IF_ICMPLT,IF_ICMPGE,IF_ICMPGT,IF_ICMPLE,IF_ACMPEQ,IF_ACMPNE,GOTO,JSR,RET,TABLESWITCH,"
                    + "LOOKUPSWITCH,IRETURN,LRETURN,FRETURN,DRETURN,ARETURN,RETURN,GETSTATIC,PUTSTATIC,GETFIELD,"
                    + "PUTFIELD,INVOKEVIRTUAL,INVOKESPECIAL,INVOKESTATIC,INVOKEINTERFACE,INVOKEDYNAMIC,NEW,"
                    + "NEWARRAY,ANEWARRAY,ARRAYLENGTH,ATHROW,CHECKCAST,INSTANCEOF,MONITORENTER,MONITOREXIT,,"
                    + "MULTIANEWARRAY,IFNULL,IFNONNULL"
            ).split(",", -1);

    static {
        if (!"LDC".equals(OPCODES[18])
                || !"ILOAD".equals(OPCODES[21])
                || !"IALOAD".equals(OPCODES[46])
                || !"ISTORE".equals(OPCODES[54])
                || !"IASTORE".equals(OPCODES[79])
                || !"IINC".equals(OPCODES[132])
                || !"IFEQ".equals(OPCODES[153])
                || !"IRETURN".equals(OPCODES[172])
                || !"INVOKEVIRTUAL".equals(OPCODES[182])
                || !"IFNONNULL".equals(OPCODES[199])) {
            throw new IllegalStateException("opcode table mismatch len=" + OPCODES.length);
        }
    }

    static FirstDiff compare(ClassNode left, ClassNode right, JarIndex leftJar, JarIndex rightJar) {
        return compare(left, right, null, Set.of(), leftJar, rightJar, null);
    }

    static FirstDiff compare(
            ClassNode left,
            ClassNode right,
            ClassNode allowRight,
            Set<String> allowedMethodKeys,
            JarIndex leftJar,
            JarIndex rightJar,
            JarIndex allowJar) {
        return compare(left, right, allowRight, allowedMethodKeys, leftJar, rightJar, allowJar, Set.of());
    }

    static FirstDiff compare(
            ClassNode left, ClassNode right, ClassNode allowRight, Set<String> allowedMethodKeys,
            JarIndex leftJar, JarIndex rightJar, JarIndex allowJar, Set<String> effectsMethods) {
        return compare(left, right, allowRight, allowedMethodKeys, leftJar, rightJar, allowJar,
                effectsMethods, Set.of(), false);
    }

    static FirstDiff compare(
            ClassNode left, ClassNode right, ClassNode allowRight, Set<String> allowedMethodKeys,
            JarIndex leftJar, JarIndex rightJar, JarIndex allowJar, Set<String> effectsMethods,
            Set<String> extraMethods, boolean metadataExempt) {
        FirstDiff d;
        int ignoredAccess = Opcodes.ACC_SUPER | (metadataExempt ? Opcodes.ACC_SYNTHETIC : 0);
        d = cmp("access", hex(left.access & ~ignoredAccess), hex(right.access & ~ignoredAccess));
        if (d != null) {
            return d;
        }
        d = cmp("super", str(left.superName), str(right.superName));
        if (d != null) {
            return d;
        }
        d = cmp("interfaces", joinSorted(left.interfaces), joinSorted(right.interfaces));
        if (d != null) {
            return d;
        }
        d = cmp("signature", str(left.signature), str(right.signature));
        if (d != null) {
            return d;
        }
        if (!metadataExempt) {
            d = cmp("outerClass", str(left.outerClass), str(right.outerClass));
            if (d != null) {
                return d;
            }
            d = cmp("outerMethod", str(left.outerMethod) + str(left.outerMethodDesc),
                    str(right.outerMethod) + str(right.outerMethodDesc));
            if (d != null) {
                return d;
            }
            d = cmp("nestHost", str(left.nestHostClass), str(right.nestHostClass));
            if (d != null) {
                return d;
            }
            d = cmp("nestMembers", joinSorted(left.nestMembers), joinSorted(right.nestMembers));
            if (d != null) {
                return d;
            }
        }
        d = cmp("permittedSubclasses", joinSorted(left.permittedSubclasses),
                joinSorted(right.permittedSubclasses));
        if (d != null) {
            return d;
        }
        d = cmp("annotations", annotations(left), annotations(right));
        if (d != null) {
            return d;
        }
        d = cmp("innerClasses", innerClasses(left, metadataExempt), innerClasses(right, metadataExempt));
        if (d != null) {
            return d;
        }
        d = cmp("recordComponents", records(left, leftJar), records(right, rightJar));
        if (d != null) {
            return d;
        }
        d = compareFields(left, right, leftJar, rightJar);
        if (d != null) {
            return d;
        }
        return compareMethods(left, right, allowRight, allowedMethodKeys, leftJar, rightJar, allowJar, effectsMethods, extraMethods);
    }

    private static FirstDiff compareFields(ClassNode left, ClassNode right, JarIndex leftJar, JarIndex rightJar) {
        Map<String, FieldNode> lmap = fieldMap(left, leftJar);
        Map<String, FieldNode> rmap = fieldMap(right, rightJar);
        for (String key : unionKeys(lmap, rmap)) {
            FieldNode lf = lmap.get(key);
            FieldNode rf = rmap.get(key);
            if (lf == null) {
                return FirstDiff.of("field", key, null, "only right");
            }
            if (rf == null) {
                return FirstDiff.of("field", key, null, "only left");
            }
            FirstDiff d = cmp("field " + key + " access", memberAccess(lf.access), memberAccess(rf.access));
            if (d != null) {
                return d.withMember("field", key);
            }
            d = cmp("field " + key + " signature", str(lf.signature), str(rf.signature));
            if (d != null) {
                return d.withMember("field", key);
            }
            d = cmp("field " + key + " value", value(lf.value), value(rf.value));
            if (d != null) {
                return d.withMember("field", key);
            }
            d = cmp("field " + key + " annotations", fieldAnns(lf), fieldAnns(rf));
            if (d != null) {
                return d.withMember("field", key);
            }
        }
        return null;
    }

    private static FirstDiff compareMethods(
            ClassNode left,
            ClassNode right,
            ClassNode allowRight,
            Set<String> allowedMethodKeys,
            JarIndex leftJar,
            JarIndex rightJar,
            JarIndex allowJar,
            Set<String> effectsMethods, Set<String> extraMethods) {
        Map<String, MethodNode> lmap = methodMap(left, leftJar);
        Map<String, MethodNode> rmap = methodMap(right, rightJar);
        Map<String, MethodNode> amap =
                allowRight == null || allowJar == null ? Map.of() : methodMap(allowRight, allowJar);
        Set<String> keys = new TreeSet<>(unionKeys(lmap, rmap));
        // CLI validates these canonical keys against allow-right before comparison.
        // Include methods absent from BOTH left and right: these are missing ports.
        keys.addAll(allowedMethodKeys);
        for (String key : keys) {
            MethodNode lm = lmap.get(key);
            MethodNode fromRight = rmap.get(key);
            boolean allow = allowedMethodKeys.contains(key);
            MethodNode rm;
            JarIndex rJar;
            String rightLabel;
            if (allow) {
                if (allowRight == null || allowJar == null) {
                    return FirstDiff.of("method", key, null, "allow-right class missing");
                }
                rm = amap.get(key);
                rJar = allowJar;
                rightLabel = "allow-right";
            } else {
                rm = fromRight;
                rJar = rightJar;
                rightLabel = "right";
            }
            if (lm == null) {
                return FirstDiff.of("method", key, null, "only " + rightLabel);
            }
            if (rm == null) {
                if (!allow && leftJar.approved(leftJar.deviations, lm)
                        && right.methods.stream().noneMatch(m -> m.name.equals(lm.name) && m.desc.equals(lm.desc))) continue;
                if (!allow && ((extraMethods.contains(lm.name + lm.desc)
                        && right.methods.stream().noneMatch(m -> m.name.equals(lm.name) && m.desc.equals(lm.desc)))
                        || implicitConstructor(left, right, lm))) continue;
                return FirstDiff.of("method", key, null, "only left");
            }
            if (effectsMethods.contains(rm.name + rm.desc) && !lm.name.equals(rm.name)) {
                return FirstDiff.of("method", key, null, "effects approval requires exact original method name on both sides");
            }
            FirstDiff d = compareMethod(key, lm, rm, leftJar, rJar,
                    effectsMethods.contains(lm.name + lm.desc) && effectsMethods.contains(rm.name + rm.desc));
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    private static boolean implicitConstructor(ClassNode left, ClassNode right, MethodNode method) {
        if (!"<init>".equals(method.name) || !"()V".equals(method.desc)
                || !"java/lang/Object".equals(left.superName)
                || right.methods.stream().anyMatch(m -> "<init>".equals(m.name))
                || left.methods.stream().filter(m -> "<init>".equals(m.name)).count() != 1
                || (method.access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED
                        | Opcodes.ACC_SYNTHETIC)) != 0
                || method.signature != null || !method.exceptions.isEmpty()
                || !method.tryCatchBlocks.isEmpty() || method.annotationDefault != null
                || !methodAnns(method, labelIds(method), null).equals(methodAnns(new MethodNode(), Map.of(), null))) {
            return false;
        }
        List<AbstractInsnNode> code = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() >= 0) {
                if (!typeAnns(insn.visibleTypeAnnotations, null).isEmpty()
                        || !typeAnns(insn.invisibleTypeAnnotations, null).isEmpty()) return false;
                code.add(insn);
            }
        }
        return code.size() == 3
                && code.get(0) instanceof VarInsnNode load && load.getOpcode() == Opcodes.ALOAD && load.var == 0
                && code.get(1) instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL
                && "java/lang/Object".equals(call.owner) && "<init>".equals(call.name)
                && "()V".equals(call.desc) && !call.itf && code.get(2).getOpcode() == Opcodes.RETURN;
    }

    static FirstDiff compareMethod(String key, MethodNode lm, MethodNode rm,
            JarIndex leftJar, JarIndex rJar, boolean effects) {
        int leftAccess = lm.access;
        if (leftJar.approved(leftJar.accessWiden, lm) && lm.name.equals(rm.name)
                && wider(lm.access, rm.access)) leftAccess = (leftAccess & ~7) | (rm.access & 7);
        FirstDiff d = cmp("method " + key + " access", memberAccess(leftAccess), memberAccess(rm.access));
        if (d != null) {
            return d.withMember("method", key);
        }
        d = cmp("method " + key + " signature", str(lm.signature), str(rm.signature));
        if (d != null) {
            return d.withMember("method", key);
        }
        d = cmp("method " + key + " exceptions", joinSorted(lm.exceptions), joinSorted(rm.exceptions));
        if (d != null) {
            return d.withMember("method", key);
        }
        d = cmp(
                "method " + key + " annotations",
                methodAnns(lm, labelIds(lm), leftJar),
                methodAnns(rm, labelIds(rm), rJar));
        if (d != null) {
            return d.withMember("method", key);
        }
        d = cmp(
                "method " + key + " annotationDefault",
                defaultValue(lm.annotationDefault, leftJar),
                defaultValue(rm.annotationDefault, rJar));
        if (d != null) {
            return d.withMember("method", key);
        }
        if (!leftJar.approved(leftJar.layouts,lm) && instructions(lm,leftJar).equals(instructions(rm,rJar))
                && tryCatch(lm,leftJar).equals(tryCatch(rm,rJar))) return null;
        if (TransportComparison.same(lm, rm, leftJar, rJar, leftJar.approved(leftJar.layouts, lm))) return null;
        if (effects) {
            // Keep the existing explicitly approved effects heuristic on physical slots.
            // The default data-flow proof above already used the normalized webs.
            lm = leftJar.beforeWebs.getOrDefault(lm,lm);
            rm = rJar.beforeWebs.getOrDefault(rm,rm);
            d = cmpList("effects sequence", effectsSequence(lm, leftJar, tempStores(lm, rm)), effectsSequence(rm, rJar, tempStores(rm, lm)));
            if (d == null) d = cmp("effects constants", constants(lm, leftJar).toString(), constants(rm, rJar).toString());
            if (d == null) d = cmpList("effects tryCatch", catchTypes(lm, leftJar), catchTypes(rm, rJar));
            if (d == null) d = cmp("effects branches", Integer.toString(branches(lm)), Integer.toString(branches(rm)));
            return d == null ? null : FirstDiff.of("method", key, d.insnIndex, d.member + ": " + d.detail);
        }
        if (leftJar.approved(leftJar.layouts, lm)) {
            String difference = LayoutComparison.difference(lm, rm, leftJar, rJar);
            return difference == null ? null : FirstDiff.of("method", key, null, difference);
        }
        if (leftJar.approved(leftJar.ctorOrders, lm)) {
            // Directional exception: left may move an assignment AFTER right's super call.
            if (!instructions(lm, leftJar).equals(instructions(rm, rJar)) || !tryCatch(lm, leftJar).equals(tryCatch(rm, rJar)))
                rm = Normalizations.ctorOrder(rJar.readTree(rJar.methodOwners.get(rm)), rm);
        }
        List<String> ltry = tryCatch(lm, leftJar);
        List<String> rtry = tryCatch(rm, rJar);
        d = cmpList("method " + key + " tryCatch", ltry, rtry);
        if (d != null) {
            return d.withMember("method", key);
        }
        List<String> lins = instructions(lm, leftJar);
        List<String> rins = instructions(rm, rJar);
        int n = Math.min(lins.size(), rins.size());
        for (int i = 0; i < n; i++) {
            if (!lins.get(i).equals(rins.get(i))) {
                return FirstDiff.of(
                        "method",
                        key,
                        i,
                        "left=" + clip(lins.get(i)) + " right=" + clip(rins.get(i)));
            }
        }
        if (lins.size() != rins.size()) {
            return FirstDiff.of(
                    "method",
                    key,
                    n,
                    "insn length left=" + lins.size() + " right=" + rins.size());
        }
        return null;
    }

    private static boolean wider(int left, int right) {
        int l = left & 7, r = right & 7;
        if (!Set.of(0,1,2,4).contains(l) || !Set.of(0,1,2,4).contains(r)) return false;
        return l == Opcodes.ACC_PUBLIC || l == r
                || r == Opcodes.ACC_PRIVATE && (l == 0 || l == Opcodes.ACC_PROTECTED)
                || r == 0 && l == Opcodes.ACC_PROTECTED;
    }

    private static Set<AbstractInsnNode> tempStores(MethodNode m, MethodNode other) {
        long dups = java.util.Arrays.stream(other.instructions.toArray()).filter(n -> n.getOpcode() == Opcodes.DUP).count();
        Set<AbstractInsnNode> result = Collections.newSetFromMap(new IdentityHashMap<>());
        if (dups == 0) return result;
        for (AbstractInsnNode n : m.instructions) if (n instanceof VarInsnNode v && v.getOpcode() == Opcodes.ASTORE) {
            if (n.visibleTypeAnnotations != null || n.invisibleTypeAnnotations != null) continue;
            int writes = 0, reads = 0; AbstractInsnNode read = null;
            for (AbstractInsnNode x : m.instructions) {
                if (x instanceof VarInsnNode t && t.var == v.var) {
                    if (t.getOpcode() == Opcodes.ASTORE) writes++;
                    else if (t.getOpcode() == Opcodes.ALOAD) { reads++; read = t; }
                    else { writes += 2; }
                }
                if (x instanceof IincInsnNode t && t.var == v.var) writes += 2;
            }
            if (writes != 1 || reads != 1) continue;
            AbstractInsnNode x = n.getNext();
            while (x != null && x != read && !(x instanceof LabelNode) && !(x instanceof JumpInsnNode)
                    && !(x instanceof TableSwitchInsnNode) && !(x instanceof LookupSwitchInsnNode)
                    && !(x instanceof IincInsnNode) && !(x.getOpcode() >= Opcodes.ISTORE && x.getOpcode() <= Opcodes.ASTORE)
                    && x.getOpcode() != Opcodes.PUTFIELD && x.getOpcode() != Opcodes.PUTSTATIC) x = x.getNext();
            if (x == read) result.add(n);
        }
        return result.size() <= dups ? result : Set.of();
    }

    static String methodIdentity(MethodNode m, JarIndex jar) {
        return m.desc + "\n" + memberAccess(m.access) + "\n" + str(m.signature) + "\n" + joinSorted(m.exceptions)
                + "\n" + methodAnns(m, labelIds(m), jar) + "\n" + defaultValue(m.annotationDefault, jar)
                + "\n" + tryCatch(m, jar) + "\n" + instructions(m, jar);
    }

    static String graphInsn(AbstractInsnNode n, JarIndex jar) {
        Map<LabelNode, Integer> labels = new IdentityHashMap<>();
        if (n instanceof JumpInsnNode j) labels.put(j.label, 0);
        if (n instanceof TableSwitchInsnNode s) { labels.put(s.dflt, 0); for (LabelNode l : s.labels) labels.put(l, 0); }
        if (n instanceof LookupSwitchInsnNode s) { labels.put(s.dflt, 0); for (LabelNode l : s.labels) labels.put(l, 0); }
        String body = n instanceof VarInsnNode ? opcodeName(n.getOpcode())
                : n instanceof IincInsnNode i ? "IINC " + i.incr : formatInsn(n, labels, jar);
        return withInsnTypeAnns(body, n, jar);
    }

    static String graphCatch(TryCatchBlockNode t, JarIndex jar) {
        return catchType(t.type) + " " + anns(t.visibleTypeAnnotations, jar) + " " + anns(t.invisibleTypeAnnotations, jar);
    }

    private static Map<String, FieldNode> fieldMap(ClassNode node, JarIndex jar) {
        Map<String, FieldNode> map = new TreeMap<>();
        if (node.fields == null) {
            return map;
        }
        for (FieldNode field : node.fields) {
            map.put(jar.remapField(node.name, field.name) + ":" + field.desc, field);
        }
        return map;
    }

    private static Map<String, MethodNode> methodMap(ClassNode node, JarIndex jar) {
        Map<String, MethodNode> map = new TreeMap<>();
        if (node.methods == null) {
            return map;
        }
        for (MethodNode method : node.methods) {
            String name = jar.remapMethod(node.name, method.name, method.desc);
            map.put(name + method.desc, method);
        }
        return map;
    }

    /** One bijection for physical slots; wide operands retain BOTH halves to preserve aliasing. */
    private static final class LocalSlots {
        private final Map<Integer, Integer> ids = new TreeMap<>();

        LocalSlots(MethodNode method) {
            int parameters = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
            if (method.desc != null) for (Type type : Type.getArgumentTypes(method.desc)) parameters += type.getSize();
            for (int i = 0; i < parameters; i++) ids.put(i, i);
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof VarInsnNode n) {
                    id(n.var);
                    if (wide(n.getOpcode())) id(n.var + 1);
                } else if (insn instanceof IincInsnNode n) id(n.var);
            }
        }

        int id(int slot) { return ids.computeIfAbsent(slot, k -> ids.size()); }

        String operand(VarInsnNode n) {
            return id(n.var) + (wide(n.getOpcode()) ? "/" + id(n.var + 1) : "");
        }

        private static boolean wide(int opcode) {
            return opcode == Opcodes.LLOAD || opcode == Opcodes.LSTORE
                    || opcode == Opcodes.DLOAD || opcode == Opcodes.DSTORE;
        }
    }

    static List<String> instructions(MethodNode method, JarIndex jar) {
        Map<LabelNode, Integer> labels = labelIds(method);
        LocalSlots slots = new LocalSlots(method);
        List<String> out = new ArrayList<>();
        if (method.instructions == null) {
            return out;
        }
        Set<Integer> emitted = new java.util.HashSet<>();
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof LabelNode label) {
                if (emitted.add(labels.get(label))) out.add("LABEL " + labelId(labels, label));
                continue;
            }
            if (insn.getOpcode() < 0) {
                continue;
            }
            out.add(withInsnTypeAnns(formatInsn(insn, labels, jar, slots), insn, jar));
        }
        return out;
    }

    static List<String> tryCatch(MethodNode method, JarIndex jar) {
        Map<LabelNode, Integer> labels = labelIds(method);
        List<String> out = new ArrayList<>();
        if (method.tryCatchBlocks == null) {
            return out;
        }
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            out.add("TRY "
                    + labelId(labels, block.start)
                    + " "
                    + labelId(labels, block.end)
                    + " "
                    + labelId(labels, block.handler)
                    + " "
                    + catchType(block.type)
                    + " "
                    + anns(block.visibleTypeAnnotations, jar)
                    + " "
                    + anns(block.invisibleTypeAnnotations, jar));
        }
        return out;
    }

    private static Map<LabelNode, Integer> labelIds(MethodNode method) {
        Map<LabelNode, Integer> ids = new IdentityHashMap<>();
        int next = 0;
        if (method.instructions != null) {
            boolean newPosition = true;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof LabelNode label && !ids.containsKey(label)) {
                    if (newPosition) next++;
                    ids.put(label, next - 1);
                    newPosition = false;
                }
                if (insn.getOpcode() >= 0) newPosition = true;
            }
        }
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                for (LabelNode label : new LabelNode[] {block.start, block.end, block.handler}) {
                    if (label != null && !ids.containsKey(label)) {
                        ids.put(label, next++);
                    }
                }
            }
        }
        return ids;
    }

    private static String formatInsn(AbstractInsnNode insn, Map<LabelNode, Integer> labels, JarIndex jar) {
        return formatInsn(insn, labels, jar, null);
    }

    private static String formatInsn(AbstractInsnNode insn, Map<LabelNode, Integer> labels, JarIndex jar, LocalSlots slots) {
        String constant = pushedConstant(insn, jar);
        if (constant != null) return "CONST " + constant;
        String op = opcodeName(insn.getOpcode());
        if (insn instanceof IntInsnNode n) {
            return op + " " + n.operand;
        }
        if (insn instanceof VarInsnNode n) {
            return op + " " + (slots == null ? n.var : slots.operand(n));
        }
        if (insn instanceof TypeInsnNode n) {
            return op + " " + n.desc;
        }
        if (insn instanceof FieldInsnNode n) {
            return op + " " + jar.fieldOwner(n.owner, n.name, n.desc, n.getOpcode()) + "." + jar.remapField(n.owner, n.name) + ":" + n.desc;
        }
        if (insn instanceof MethodInsnNode n) {
            return op + " " + n.owner + "." + jar.remapMethod(n.owner, n.name, n.desc) + n.desc
                    + (n.itf ? " itf" : "");
        }
        if (insn instanceof InvokeDynamicInsnNode n) {
            StringBuilder sb = new StringBuilder(op);
            sb.append(' ').append(n.name).append(n.desc);
            sb.append(" bsm=").append(handle(n.bsm, jar));
            sb.append(" args=[");
            if (n.bsmArgs != null) {
                for (int i = 0; i < n.bsmArgs.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(cst(n.bsmArgs[i], jar));
                }
            }
            sb.append(']');
            return sb.toString();
        }
        if (insn instanceof JumpInsnNode n) {
            return op + " " + labelId(labels, n.label);
        }
        if (insn instanceof LdcInsnNode n) {
            return op + " " + cst(n.cst, jar);
        }
        if (insn instanceof IincInsnNode n) {
            return op + " var=" + (slots == null ? n.var : slots.id(n.var)) + " incr=" + n.incr;
        }
        if (insn instanceof TableSwitchInsnNode n) {
            StringBuilder sb = new StringBuilder(op);
            sb.append(" min=").append(n.min).append(" max=").append(n.max);
            sb.append(" default=").append(labelId(labels, n.dflt));
            sb.append(" labels=");
            for (int i = 0; i < n.labels.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(labelId(labels, n.labels.get(i)));
            }
            return sb.toString();
        }
        if (insn instanceof LookupSwitchInsnNode n) {
            StringBuilder sb = new StringBuilder(op);
            sb.append(" default=").append(labelId(labels, n.dflt));
            sb.append(" keys=");
            for (int i = 0; i < n.keys.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(n.keys.get(i));
            }
            sb.append(" labels=");
            for (int i = 0; i < n.labels.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(labelId(labels, n.labels.get(i)));
            }
            return sb.toString();
        }
        if (insn instanceof MultiANewArrayInsnNode n) {
            return op + " " + n.desc + " dims=" + n.dims;
        }
        return op;
    }

    /** Typed constants: keep -0.0 distinct and preserve NaN payload bits. */
    private static String pushedConstant(AbstractInsnNode insn, JarIndex jar) {
        int op = insn.getOpcode();
        if (op == Opcodes.ACONST_NULL) return "null";
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) return cst(op - Opcodes.ICONST_0, jar);
        if (op >= Opcodes.LCONST_0 && op <= Opcodes.LCONST_1) return cst((long) (op - Opcodes.LCONST_0), jar);
        if (op >= Opcodes.FCONST_0 && op <= Opcodes.FCONST_2) return cst((float) (op - Opcodes.FCONST_0), jar);
        if (op >= Opcodes.DCONST_0 && op <= Opcodes.DCONST_1) return cst((double) (op - Opcodes.DCONST_0), jar);
        if (insn instanceof IntInsnNode n && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) return cst(n.operand, jar);
        if (insn instanceof LdcInsnNode n) return cst(n.cst, jar);
        return null;
    }

    private static String memberAccess(int access) {
        return hex(access & ~Opcodes.ACC_SYNTHETIC);
    }

    private static String catchType(String type) {
        return type == null ? "java/lang/Throwable" : type;
    }

    private static List<String> effectsSequence(MethodNode method, JarIndex jar) {
        return effectsSequence(method, jar, Set.of());
    }

    private static List<String> effectsSequence(MethodNode method, JarIndex jar, Set<AbstractInsnNode> skipped) {
        List<String> out = new ArrayList<>();
        Map<LabelNode, Integer> labels = labelIds(method);
        for (AbstractInsnNode insn : method.instructions) {
            if (skipped.contains(insn)) continue;
            int op = insn.getOpcode();
            String effect = null;
            if (insn instanceof FieldInsnNode n) {
                effect = opcodeName(op) + " " + n.owner + "." + jar.remapField(n.owner, n.name);
            } else if (insn instanceof MethodInsnNode || insn instanceof InvokeDynamicInsnNode
                    || op == Opcodes.NEW || op == Opcodes.CHECKCAST || op == Opcodes.INSTANCEOF) {
                // invokedynamic has no owner: retain name/descriptor, bootstrap handle and arguments.
                effect = formatInsn(insn, labels, jar);
            } else if ((op >= Opcodes.IASTORE && op <= Opcodes.SASTORE)
                    || op == Opcodes.ASTORE || op == Opcodes.ATHROW
                    || op == Opcodes.MONITORENTER || op == Opcodes.MONITOREXIT
                    || (op >= Opcodes.IRETURN && op <= Opcodes.RETURN)) {
                // ASTORE keeps its opcode, but not the compiler-assigned local slot.
                effect = opcodeName(op);
            }
            if (effect != null) out.add(withInsnTypeAnns(effect, insn, jar));
            else if (insn.visibleTypeAnnotations != null || insn.invisibleTypeAnnotations != null) {
                out.add(withInsnTypeAnns("ANNOTATION", insn, jar));
            }
        }
        return out;
    }

    private static Map<String, Integer> constants(MethodNode method, JarIndex jar) {
        Map<String, Integer> bag = new TreeMap<>();
        for (AbstractInsnNode insn : method.instructions) {
            String value = pushedConstant(insn, jar);
            if (value != null) bag.merge(value, 1, Integer::sum);
            if (insn instanceof IincInsnNode n) bag.merge("IINC " + cst(n.incr, jar), 1, Integer::sum);
            if (insn instanceof LookupSwitchInsnNode n) {
                for (int key : n.keys) bag.merge("SWITCH " + cst(key, jar), 1, Integer::sum);
            }
            if (insn instanceof TableSwitchInsnNode n) {
                for (int i = 0; i < n.labels.size(); i++) bag.merge("SWITCH " + cst(n.min + i, jar), 1, Integer::sum);
            }
        }
        return bag;
    }

    private static List<String> catchTypes(MethodNode method, JarIndex jar) {
        List<String> types = new ArrayList<>();
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            types.add(catchType(block.type) + " " + typeAnns(block.visibleTypeAnnotations, jar)
                    + " " + typeAnns(block.invisibleTypeAnnotations, jar));
        }
        return types;
    }

    private static int branches(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof JumpInsnNode || insn instanceof TableSwitchInsnNode
                    || insn instanceof LookupSwitchInsnNode || insn.getOpcode() == Opcodes.RET) count++;
        }
        return count;
    }

    private static String opcodeName(int op) {
        if (op >= 0 && op < OPCODES.length && !OPCODES[op].isEmpty()) {
            return OPCODES[op];
        }
        return "OP" + op;
    }

    private static String labelId(Map<LabelNode, Integer> labels, LabelNode label) {
        if (label == null) {
            return "L?";
        }
        Integer id = labels.get(label);
        return id == null ? "L?" : "L" + id;
    }

    private static String handle(Handle handle, JarIndex jar) {
        return "H" + handle.getTag()
                + " " + handle.getOwner()
                + "." + (handle.getTag() <= Opcodes.H_PUTSTATIC ? jar.remapField(handle.getOwner(), handle.getName())
                        : jar.remapMethod(handle.getOwner(), handle.getName(), handle.getDesc()))
                + handle.getDesc()
                + (handle.isInterface() ? " itf" : "");
    }

    private static String cst(Object value, JarIndex jar) {
        if (value instanceof Float f) return "Float:0x" + Integer.toHexString(Float.floatToRawIntBits(f));
        if (value instanceof Double d) return "Double:0x" + Long.toHexString(Double.doubleToRawLongBits(d));
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "str:" + quote(s);
        }
        if (value instanceof Type type) {
            return "type:" + type.getDescriptor();
        }
        if (value instanceof Handle handle) {
            return "handle:" + handle(handle, jar);
        }
        if (value instanceof ConstantDynamic dynamic) {
            StringBuilder sb = new StringBuilder("condy:");
            sb.append(dynamic.getName()).append(dynamic.getDescriptor());
            sb.append(" bsm=").append(handle(dynamic.getBootstrapMethod(), jar));
            sb.append(" args=[");
            int count = dynamic.getBootstrapMethodArgumentCount();
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(cst(dynamic.getBootstrapMethodArgument(i), jar));
            }
            sb.append(']');
            return sb.toString();
        }
        if (value instanceof AnnotationNode annotation) {
            return annotation(annotation, jar);
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(cst(list.get(i), jar));
            }
            sb.append(']');
            return sb.toString();
        }
        if (value instanceof String[] pair && pair.length == 2) {
            return "enum:" + pair[0] + "." + pair[1];
        }
        return value.getClass().getSimpleName() + ":" + value;
    }

    private static String annotations(ClassNode node) {
        return anns(node.visibleAnnotations, null)
                + "|" + anns(node.invisibleAnnotations, null)
                + "|" + typeAnns(node.visibleTypeAnnotations, null)
                + "|" + typeAnns(node.invisibleTypeAnnotations, null);
    }

    private static String fieldAnns(FieldNode node) {
        return anns(node.visibleAnnotations, null)
                + "|" + anns(node.invisibleAnnotations, null)
                + "|" + typeAnns(node.visibleTypeAnnotations, null)
                + "|" + typeAnns(node.invisibleTypeAnnotations, null);
    }

    private static String methodAnns(MethodNode node, Map<LabelNode, Integer> labels, JarIndex jar) {
        StringBuilder sb = new StringBuilder();
        sb.append(anns(node.visibleAnnotations, jar));
        sb.append('|').append(anns(node.invisibleAnnotations, jar));
        sb.append('|').append(typeAnns(node.visibleTypeAnnotations, jar));
        sb.append('|').append(typeAnns(node.invisibleTypeAnnotations, jar));
        sb.append("|pvis=").append(paramAnns(node.visibleParameterAnnotations, jar));
        sb.append("|pinv=").append(paramAnns(node.invisibleParameterAnnotations, jar));
        LocalSlots slots = new LocalSlots(node);
        sb.append("|lvtvis=").append(localTypeAnns(node.visibleLocalVariableAnnotations, labels, jar, slots));
        sb.append("|lvtinv=").append(localTypeAnns(node.invisibleLocalVariableAnnotations, labels, jar, slots));
        return sb.toString();
    }

    private static String withInsnTypeAnns(String body, AbstractInsnNode insn, JarIndex jar) {
        String vis = typeAnns(insn.visibleTypeAnnotations, jar);
        String inv = typeAnns(insn.invisibleTypeAnnotations, jar);
        if (vis.isEmpty() && inv.isEmpty()) {
            return body;
        }
        return body + " typeAnn={" + vis + "|" + inv + "}";
    }

    private static String localTypeAnns(
            List<LocalVariableAnnotationNode> list, Map<LabelNode, Integer> labels, JarIndex jar, LocalSlots slots) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            LocalVariableAnnotationNode node = list.get(i);
            sb.append("typeRef=").append(node.typeRef)
                    .append(" path=").append(node.typePath)
                    .append(" ranges=");
            int n = node.start == null ? 0 : node.start.size();
            for (int r = 0; r < n; r++) {
                if (r > 0) {
                    sb.append(',');
                }
                sb.append(labelId(labels, node.start.get(r)))
                        .append('-')
                        .append(labelId(labels, node.end.get(r)))
                        .append('@')
                        .append(slots.id(node.index.get(r)));
            }
            sb.append(' ').append(annotation(node, jar));
        }
        return sb.toString();
    }

    private static String defaultValue(Object value, JarIndex jar) {
        return value == null ? "" : cst(value, jar);
    }

    private static String anns(List<? extends AnnotationNode> list, JarIndex jar) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(annotation(list.get(i), jar));
        }
        return sb.toString();
    }

    private static String typeAnns(List<? extends TypeAnnotationNode> list, JarIndex jar) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            TypeAnnotationNode node = list.get(i);
            sb.append("typeRef=").append(node.typeRef)
                    .append(" path=").append(node.typePath)
                    .append(' ')
                    .append(annotation(node, jar));
        }
        return sb.toString();
    }

    private static String paramAnns(List<AnnotationNode>[] table, JarIndex jar) {
        if (table == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < table.length; i++) {
            if (i > 0) {
                sb.append(" ;; ");
            }
            sb.append(i).append(':').append(anns(table[i], jar));
        }
        return sb.toString();
    }

    private static String annotation(AnnotationNode node, JarIndex jar) {
        StringBuilder sb = new StringBuilder("@").append(node.desc);
        if (node.values != null) {
            for (int i = 0; i < node.values.size(); i += 2) {
                sb.append(' ').append(node.values.get(i)).append('=');
                sb.append(cst(node.values.get(i + 1), jar));
            }
        }
        return sb.toString();
    }

    private static String innerClasses(ClassNode node, boolean metadataExempt) {
        if (node.innerClasses == null || node.innerClasses.isEmpty()) {
            return "";
        }
        List<String> rows = new ArrayList<>();
        for (InnerClassNode inner : node.innerClasses) {
            if (!node.name.equals(inner.name) && !node.name.equals(inner.outerName)) continue;
            if (metadataExempt && node.name.equals(inner.name)) continue;
            rows.add(str(inner.name) + "|" + str(inner.outerName) + "|" + str(inner.innerName)
                    + "|" + hex(inner.access));
        }
        Collections.sort(rows);
        return String.join("; ", rows);
    }

    private static String records(ClassNode node, JarIndex jar) {
        if (node.recordComponents == null || node.recordComponents.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (RecordComponentNode rec : node.recordComponents) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(rec.name).append(':').append(rec.descriptor)
                    .append(" sig=").append(str(rec.signature))
                    .append(" anns=").append(anns(rec.visibleAnnotations, jar))
                    .append('|').append(anns(rec.invisibleAnnotations, jar))
                    .append(" typeAnns=").append(typeAnns(rec.visibleTypeAnnotations, jar))
                    .append('|').append(typeAnns(rec.invisibleTypeAnnotations, jar));
        }
        return sb.toString();
    }

    private static FirstDiff cmp(String where, String left, String right) {
        if (Objects.equals(left, right)) {
            return null;
        }
        return FirstDiff.of("class", where, null, "left=" + clip(left) + " right=" + clip(right));
    }

    private static FirstDiff cmpList(String where, List<String> left, List<String> right) {
        int n = Math.min(left.size(), right.size());
        for (int i = 0; i < n; i++) {
            if (!left.get(i).equals(right.get(i))) {
                return FirstDiff.of(
                        "class",
                        where + "[" + i + "]",
                        null,
                        "left=" + clip(left.get(i)) + " right=" + clip(right.get(i)));
            }
        }
        if (left.size() != right.size()) {
            return FirstDiff.of(
                    "class",
                    where,
                    null,
                    "length left=" + left.size() + " right=" + right.size());
        }
        return null;
    }

    private static List<String> unionKeys(Map<String, ?> left, Map<String, ?> right) {
        TreeMap<String, Boolean> keys = new TreeMap<>();
        for (String k : left.keySet()) {
            keys.put(k, Boolean.TRUE);
        }
        for (String k : right.keySet()) {
            keys.put(k, Boolean.TRUE);
        }
        return new ArrayList<>(keys.keySet());
    }

    private static String joinSorted(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        List<String> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return String.join(",", copy);
    }

    private static String hex(int access) {
        return "0x" + Integer.toHexString(access);
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }

    private static String value(Object v) {
        if (v == null) {
            return "";
        }
        return v.getClass().getSimpleName() + ":" + v;
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String clip(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 240 ? s : s.substring(0, 240) + "…";
    }

    static final class FirstDiff {
        final String where;
        final String member;
        final Integer insnIndex;
        final String detail;

        private FirstDiff(String where, String member, Integer insnIndex, String detail) {
            this.where = where;
            this.member = member;
            this.insnIndex = insnIndex;
            this.detail = detail;
        }

        static FirstDiff of(String where, String member, Integer insnIndex, String detail) {
            return new FirstDiff(where, member, insnIndex, detail);
        }

        FirstDiff withMember(String where, String member) {
            return new FirstDiff(where, member, insnIndex, detail);
        }

        String oneLine() {
            StringBuilder sb = new StringBuilder();
            sb.append(where);
            if (member != null && !member.isEmpty()) {
                sb.append(' ').append(member);
            }
            if (insnIndex != null) {
                sb.append(" insn[").append(insnIndex).append(']');
            }
            if (detail != null && !detail.isEmpty()) {
                sb.append(": ").append(detail);
            }
            return sb.toString();
        }
    }
}
