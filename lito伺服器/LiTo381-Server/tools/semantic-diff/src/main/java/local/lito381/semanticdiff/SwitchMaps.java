package local.lito381.semanticdiff;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Recognize the complete javac enum-map initializer before assigning content identities.
 * No partial initializer, arbitrary call, extra handler or duplicate assignment is accepted.
 */
final class SwitchMaps implements Opcodes {
    private SwitchMaps() {}
    record Segment(String field, String enumName, Map<String,Integer> cases, int start, int end) {
        String identity() {
            try {
                String content = enumName + "\n" + cases;
                return "$SwitchMap$content$" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(content.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        }
    }
    record Model(Map<String,String> names, List<Segment> segments) {}

    static Model inspect(ClassNode owner) {
        if (!FourthNormalizations.enabled("switch-content") || !owner.name.contains("$")) return null;
        MethodNode init = owner.methods.stream().filter(m -> m.name.equals("<clinit>") && m.desc.equals("()V"))
                .findFirst().orElse(null);
        if (init == null || FourthNormalizations.metadata(init)) return null;
        List<AbstractInsnNode> code = new ArrayList<>();
        Map<AbstractInsnNode,Integer> positions = new IdentityHashMap<>();
        for (AbstractInsnNode n : init.instructions) {
            if (FourthNormalizations.annotated(n)) return null;
            positions.put(n,code.size());
            if (n.getOpcode() >= 0) code.add(n);
        }
        Map<Integer,TryCatchBlockNode> handlers = new HashMap<>();
        for (TryCatchBlockNode t : init.tryCatchBlocks) {
            if (!"java/lang/NoSuchFieldError".equals(t.type) || t.visibleTypeAnnotations != null
                    || t.invisibleTypeAnnotations != null || handlers.put(positions.get(t.start),t) != null) return null;
        }
        List<Segment> segments = new ArrayList<>();
        Set<String> fields = new HashSet<>();
        int index = 0, assignments = 0;
        while (index + 4 < code.size()) {
            int start = index;
            if (!(code.get(index) instanceof MethodInsnNode values) || values.getOpcode() != INVOKESTATIC
                    || values.itf || !values.name.equals("values") || !values.desc.equals("()[L"+values.owner+";")) return null;
            String enumName = values.owner;
            if (code.get(index+1).getOpcode() != ARRAYLENGTH || !(code.get(index+2) instanceof IntInsnNode array)
                    || array.getOpcode() != NEWARRAY || array.operand != T_INT
                    || !(code.get(index+3) instanceof FieldInsnNode put) || put.getOpcode() != PUTSTATIC
                    || !put.owner.equals(owner.name) || !put.name.startsWith("$SwitchMap$") || !put.desc.equals("[I")
                    || !fields.add(put.name)) return null;
            if (owner.fields.stream().noneMatch(f -> f.name.equals(put.name) && f.desc.equals("[I")
                    && (f.access & ACC_STATIC) != 0)) return null;
            index += 4;
            Map<String,Integer> cases = new TreeMap<>();
            while (index < code.size() && code.get(index) instanceof FieldInsnNode get && get.getOpcode() == GETSTATIC) {
                if (index+7 >= code.size() || !get.owner.equals(owner.name) || !get.name.equals(put.name)
                        || !get.desc.equals("[I") || !(code.get(index+1) instanceof FieldInsnNode constant)
                        || constant.getOpcode() != GETSTATIC || !constant.owner.equals(enumName)
                        || !constant.desc.equals("L"+enumName+";")
                        || !(code.get(index+2) instanceof MethodInsnNode ordinal) || ordinal.getOpcode() != INVOKEVIRTUAL
                        || ordinal.itf || !ordinal.owner.equals(enumName) || !ordinal.name.equals("ordinal")
                        || !ordinal.desc.equals("()I")) return null;
                Integer value = integer(code.get(index+3));
                TryCatchBlockNode handler = handlers.get(index);
                AbstractInsnNode catchStore = code.get(index+6);
                if (value == null || cases.put(constant.name,value) != null || code.get(index+4).getOpcode() != IASTORE
                        || !(code.get(index+5) instanceof JumpInsnNode jump) || jump.getOpcode() != GOTO
                        || positions.get(jump.label) != index+7 || handler == null
                        || positions.get(handler.end) != index+5 || positions.get(handler.handler) != index+6
                        || !(catchStore.getOpcode() == POP || catchStore instanceof VarInsnNode v && v.getOpcode() == ASTORE)) return null;
                // The handler local must be dead. Otherwise grouping enum segments could change its value.
                if (catchStore instanceof VarInsnNode store) for (AbstractInsnNode n : code)
                    if (n instanceof VarInsnNode v && v.var == store.var && v.getOpcode() != ASTORE
                            || n instanceof IincInsnNode inc && inc.var == store.var) return null;
                assignments++;
                index += 7;
            }
            if (cases.isEmpty()) return null;
            segments.add(new Segment(put.name,enumName,cases,start,index));
        }
        long declared = owner.fields.stream().filter(f -> f.name.startsWith("$SwitchMap$") && f.desc.equals("[I")).count();
        if (index != code.size()-1 || code.get(index).getOpcode() != RETURN || handlers.size() != assignments
                || declared != fields.size()) return null;
        Map<String,String> names = new HashMap<>();
        Set<String> identities = new HashSet<>();
        for (Segment segment : segments) {
            String identity = segment.identity();
            if (!identities.add(identity)) return null;
            names.put(segment.field,identity);
        }
        return new Model(names,List.copyOf(segments));
    }

    private static Integer integer(AbstractInsnNode n) {
        int op = n.getOpcode();
        if (op >= ICONST_M1 && op <= ICONST_5) return op-ICONST_0;
        if (n instanceof IntInsnNode i && (op == BIPUSH || op == SIPUSH)) return i.operand;
        return n instanceof LdcInsnNode l && l.cst instanceof Integer i ? i : null;
    }

    static void normalize(ClassNode owner, Model model) {
        if (model == null) return;
        MethodNode init = owner.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElseThrow();
        // Each segment has its own NoSuchFieldError edges. Clone all labels first so
        // shared end/start labels survive reordering without duplicate label identities.
        List<AbstractInsnNode> code = new ArrayList<>();
        Map<AbstractInsnNode,Integer> positions = new IdentityHashMap<>();
        for (AbstractInsnNode n : init.instructions) {
            positions.put(n,code.size());
            if (n.getOpcode() >= 0) code.add(n);
        }
        InsnList reordered = new InsnList();
        List<TryCatchBlockNode> catches = new ArrayList<>();
        for (Segment segment : model.segments.stream().sorted(Comparator.comparing(Segment::identity)).toList()) {
            Map<LabelNode,LabelNode> labels = new IdentityHashMap<>();
            Map<Integer,LabelNode> boundaries = new HashMap<>();
            for (AbstractInsnNode n : init.instructions) if (n instanceof LabelNode l
                    && positions.get(n) >= segment.start && positions.get(n) <= segment.end)
                labels.put(l,boundaries.computeIfAbsent(positions.get(n), k -> new LabelNode()));
            for (int i=segment.start;i<segment.end;i++) {
                if (boundaries.containsKey(i)) reordered.add(boundaries.get(i));
                reordered.add(code.get(i).clone(labels));
            }
            if (boundaries.containsKey(segment.end)) reordered.add(boundaries.get(segment.end));
            for (TryCatchBlockNode t : init.tryCatchBlocks) if (positions.get(t.start) >= segment.start
                    && positions.get(t.start) < segment.end)
                catches.add(new TryCatchBlockNode(labels.get(t.start),labels.get(t.end),labels.get(t.handler),t.type));
        }
        reordered.add(new InsnNode(RETURN));
        init.instructions = reordered;
        init.tryCatchBlocks = catches;
        init.localVariables = null;
    }
}
