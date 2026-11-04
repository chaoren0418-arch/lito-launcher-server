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
        FirstDiff d;
        d = cmp("access", hex(left.access), hex(right.access));
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
        d = cmp("permittedSubclasses", joinSorted(left.permittedSubclasses),
                joinSorted(right.permittedSubclasses));
        if (d != null) {
            return d;
        }
        d = cmp("annotations", annotations(left), annotations(right));
        if (d != null) {
            return d;
        }
        d = cmp("innerClasses", innerClasses(left), innerClasses(right));
        if (d != null) {
            return d;
        }
        d = cmp("recordComponents", records(left, leftJar), records(right, rightJar));
        if (d != null) {
            return d;
        }
        d = compareFields(left, right);
        if (d != null) {
            return d;
        }
        return compareMethods(left, right, allowRight, allowedMethodKeys, leftJar, rightJar, allowJar);
    }

    private static FirstDiff compareFields(ClassNode left, ClassNode right) {
        Map<String, FieldNode> lmap = fieldMap(left);
        Map<String, FieldNode> rmap = fieldMap(right);
        for (String key : unionKeys(lmap, rmap)) {
            FieldNode lf = lmap.get(key);
            FieldNode rf = rmap.get(key);
            if (lf == null) {
                return FirstDiff.of("field", key, null, "only right");
            }
            if (rf == null) {
                return FirstDiff.of("field", key, null, "only left");
            }
            FirstDiff d = cmp("field " + key + " access", hex(lf.access), hex(rf.access));
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
            JarIndex allowJar) {
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
                return FirstDiff.of("method", key, null, "only left");
            }
            FirstDiff d = cmp("method " + key + " access", hex(lm.access), hex(rm.access));
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
        }
        return null;
    }

    private static Map<String, FieldNode> fieldMap(ClassNode node) {
        Map<String, FieldNode> map = new TreeMap<>();
        if (node.fields == null) {
            return map;
        }
        for (FieldNode field : node.fields) {
            map.put(field.name + ":" + field.desc, field);
        }
        return map;
    }

    private static Map<String, MethodNode> methodMap(ClassNode node, JarIndex jar) {
        Map<String, MethodNode> map = new TreeMap<>();
        if (node.methods == null) {
            return map;
        }
        for (MethodNode method : node.methods) {
            String name = jar.remapMethod(node.name, method.name);
            map.put(name + method.desc, method);
        }
        return map;
    }

    static List<String> instructions(MethodNode method, JarIndex jar) {
        Map<LabelNode, Integer> labels = labelIds(method);
        List<String> out = new ArrayList<>();
        if (method.instructions == null) {
            return out;
        }
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof LabelNode label) {
                out.add("LABEL " + labelId(labels, label));
                continue;
            }
            if (insn.getOpcode() < 0) {
                continue;
            }
            out.add(withInsnTypeAnns(formatInsn(insn, labels, jar), insn, jar));
        }
        return out;
    }

    private static List<String> tryCatch(MethodNode method, JarIndex jar) {
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
                    + str(block.type)
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
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof LabelNode label && !ids.containsKey(label)) {
                    ids.put(label, next++);
                }
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
        String op = opcodeName(insn.getOpcode());
        if (insn instanceof IntInsnNode n) {
            return op + " " + n.operand;
        }
        if (insn instanceof VarInsnNode n) {
            return op + " " + n.var;
        }
        if (insn instanceof TypeInsnNode n) {
            return op + " " + n.desc;
        }
        if (insn instanceof FieldInsnNode n) {
            return op + " " + n.owner + "." + n.name + ":" + n.desc;
        }
        if (insn instanceof MethodInsnNode n) {
            return op + " " + n.owner + "." + jar.remapMethod(n.owner, n.name) + n.desc
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
            return op + " var=" + n.var + " incr=" + n.incr;
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
                + "." + jar.remapMethod(handle.getOwner(), handle.getName())
                + handle.getDesc()
                + (handle.isInterface() ? " itf" : "");
    }

    private static String cst(Object value, JarIndex jar) {
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
        sb.append("|lvtvis=").append(localTypeAnns(node.visibleLocalVariableAnnotations, labels, jar));
        sb.append("|lvtinv=").append(localTypeAnns(node.invisibleLocalVariableAnnotations, labels, jar));
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
            List<LocalVariableAnnotationNode> list, Map<LabelNode, Integer> labels, JarIndex jar) {
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
                        .append(node.index.get(r));
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

    private static String innerClasses(ClassNode node) {
        if (node.innerClasses == null || node.innerClasses.isEmpty()) {
            return "";
        }
        List<String> rows = new ArrayList<>();
        for (InnerClassNode inner : node.innerClasses) {
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
