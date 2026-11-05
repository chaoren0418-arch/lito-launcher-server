package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Conservative tree rewrites. Never writes application class bytes. */
final class Normalizations implements Opcodes {
    private Normalizations() {}

    static void prepare(ClassNode owner) {
        for (MethodNode m : owner.methods) {
            for (AbstractInsnNode n : m.instructions) {
                if (n instanceof InvokeDynamicInsnNode indy) {
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        if (indy.bsmArgs[i] instanceof Handle h && h.getOwner().equals(owner.name)
                                && (h.getTag() == H_INVOKEVIRTUAL || h.getTag() == H_INVOKESPECIAL)
                                && !h.getName().startsWith("<") && owner.methods.stream().anyMatch(t ->
                                    t.name.equals(h.getName()) && t.desc.equals(h.getDesc())
                                    && (t.access & (ACC_PRIVATE | ACC_STATIC)) == ACC_PRIVATE)) {
                            indy.bsmArgs[i] = new Handle(H_INVOKESPECIAL, h.getOwner(), h.getName(), h.getDesc(), h.isInterface());
                        }
                    }
                }
            }
            if ("java/lang/Object".equals(owner.superName) && "<init>".equals(m.name)) objectSuper(owner, m);
            trimTry(m);
            cleanLabels(m);
        }
    }

    static boolean harmless(AbstractInsnNode n, boolean monitor) {
        int op = n.getOpcode();
        if (op < 0 || op == NOP || op == GOTO) return true;
        if (op >= ILOAD && op <= ALOAD || op >= ISTORE && op <= ASTORE) return true;
        if (op >= ACONST_NULL && op <= SIPUSH) return true;
        if (FourthNormalizations.enabled("coverage")) {
            if (op >= POP && op <= SWAP || op == IINC) return true;
            if (op >= IADD && op <= DCMPG && op != IDIV && op != LDIV && op != IREM && op != LREM) return true;
            if (op >= IFEQ && op <= GOTO || op == IFNULL || op == IFNONNULL
                    || op == TABLESWITCH || op == LOOKUPSWITCH) return true;
        }
        if (op >= IRETURN && op <= RETURN) return !monitor;
        // Class/MethodHandle/MethodType/condy resolution can throw; do not trim these.
        return n instanceof LdcInsnNode ldc && (ldc.cst instanceof Number || ldc.cst instanceof String);
    }

    static void trimTry(MethodNode m) {
        boolean monitor = Arrays.stream(m.instructions.toArray()).anyMatch(n -> n.getOpcode() == MONITORENTER);
        for (Iterator<TryCatchBlockNode> it = m.tryCatchBlocks.iterator(); it.hasNext();) {
            TryCatchBlockNode t = it.next();
            AbstractInsnNode first = t.start, last = t.end.getPrevious();
            while (first != t.end && harmless(first, monitor)) first = first.getNext();
            if (first == t.end) { it.remove(); continue; }
            while (last != first && harmless(last, monitor)) last = last.getPrevious();
            LabelNode start = new LabelNode(), end = new LabelNode();
            m.instructions.insertBefore(first, start);
            m.instructions.insert(last, end);
            t.start = start;
            t.end = end;
        }
    }

    static void cleanLabels(MethodNode m) {
        Set<LabelNode> used = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractInsnNode n : m.instructions) {
            if (n instanceof JumpInsnNode j) used.add(j.label);
            if (n instanceof TableSwitchInsnNode s) { used.add(s.dflt); used.addAll(s.labels); }
            if (n instanceof LookupSwitchInsnNode s) { used.add(s.dflt); used.addAll(s.labels); }
        }
        for (TryCatchBlockNode t : m.tryCatchBlocks) { used.add(t.start); used.add(t.end); used.add(t.handler); }
        for (List<LocalVariableAnnotationNode> anns : Arrays.asList(m.visibleLocalVariableAnnotations, m.invisibleLocalVariableAnnotations)) {
            if (anns != null) for (LocalVariableAnnotationNode a : anns) { used.addAll(a.start); used.addAll(a.end); }
        }
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n instanceof LabelNode l && !used.contains(l)) m.instructions.remove(n);
        }
    }

    private static AbstractInsnNode previous(AbstractInsnNode n) {
        n = n.getPrevious();
        while (n != null && n.getOpcode() < 0) n = n.getPrevious();
        return n;
    }

    static MethodNode ctorOrder(ClassNode owner, MethodNode source) {
        MethodNode m = new MethodNode(source.access, source.name, source.desc, source.signature,
                source.exceptions.toArray(String[]::new));
        source.accept(m);
        MethodInsnNode superCall = null;
        for (AbstractInsnNode n : m.instructions) if (n instanceof MethodInsnNode c && c.getOpcode() == INVOKESPECIAL
                && c.owner.equals(owner.superName) && c.name.equals("<init>")) { superCall = c; break; }
        if (superCall == null) return m;
        // Plan against the original instruction stream before removing any assignment.
        Map<FieldInsnNode, List<AbstractInsnNode>> plan = new LinkedHashMap<>();
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n == superCall) break;
            if (!(n instanceof FieldInsnNode f) || f.getOpcode() != PUTFIELD || !f.owner.equals(owner.name)
                    || owner.fields.stream().noneMatch(x -> x.name.equals(f.name) && x.desc.equals(f.desc))) continue;
            AbstractInsnNode value = previous(n), receiver = value == null ? null : previous(value);
            // Only a self assignment of an already-computed local/constant is movable.
            // Calls (notably NativeDeInstanceNames.name) remain at their original position.
            boolean simple = value instanceof VarInsnNode v && v.getOpcode() >= ILOAD && v.getOpcode() <= ALOAD
                    && !(v.getOpcode() == ALOAD && v.var == 0)
                    || value != null && value.getOpcode() >= ACONST_NULL && value.getOpcode() <= SIPUSH;
            if (!simple || !(receiver instanceof VarInsnNode r) || r.getOpcode() != ALOAD || r.var != 0) continue;
            if (receiver.getNext() != value || value.getNext() != n) continue;
            if (!stableAssignmentInputs(value, n, superCall)) continue;
            plan.put(f, List.of(receiver, value, n));
        }
        // Moving an earlier write past a retained write of the same field reverses
        // the final value. Walk backwards so rejecting one move also blocks every
        // earlier dependent move; writes moved together retain their original order.
        Set<String> retainedWrites = new HashSet<>();
        for (AbstractInsnNode n = superCall.getPrevious(); n != null; n = n.getPrevious()) {
            if (!(n instanceof FieldInsnNode f) || f.getOpcode() != PUTFIELD) continue;
            String field = f.owner + "#" + f.name + ":" + f.desc;
            if (retainedWrites.contains(field)) plan.remove(f);
            if (!plan.containsKey(f)) retainedWrites.add(field);
        }
        AbstractInsnNode cursor = superCall;
        for (List<AbstractInsnNode> assignment : plan.values()) {
            for (AbstractInsnNode n : assignment) {
                m.instructions.remove(n);
                m.instructions.insert(cursor, n);
                cursor = n;
            }
        }
        return m;
    }

    /** Moving a load must preserve its value, including both slots of long/double locals. */
    private static boolean stableAssignmentInputs(AbstractInsnNode value, AbstractInsnNode assignment,
            MethodInsnNode superCall) {
        int readSlot = value instanceof VarInsnNode local ? local.var : -1;
        int readWidth = value.getOpcode() == LLOAD || value.getOpcode() == DLOAD ? 2 : 1;
        for (AbstractInsnNode n = assignment.getNext(); n != superCall; n = n.getNext()) {
            // Only prove straight-line movement. Labels can be branch or handler boundaries.
            if (n == null || n instanceof LabelNode || n instanceof JumpInsnNode
                    || n instanceof TableSwitchInsnNode || n instanceof LookupSwitchInsnNode) return false;
            int op = n.getOpcode();
            if (op == RET || op == ATHROW || op >= IRETURN && op <= RETURN) return false;
            int writeSlot;
            int writeWidth;
            if (n instanceof VarInsnNode local && op >= ISTORE && op <= ASTORE) {
                writeSlot = local.var;
                writeWidth = op == LSTORE || op == DSTORE ? 2 : 1;
            } else if (n instanceof IincInsnNode increment) {
                writeSlot = increment.var;
                writeWidth = 1;
            } else {
                continue;
            }
            // ALOAD 0 is also moved, even when the assigned value is a constant.
            if (writeSlot == 0 || readSlot >= 0 && writeSlot < readSlot + readWidth
                    && readSlot < writeSlot + writeWidth) return false;
        }
        return true;
    }

    private static void objectSuper(ClassNode owner, MethodNode m) {
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode c) || c.getOpcode() != INVOKESPECIAL
                    || !c.owner.equals("java/lang/Object") || !c.name.equals("<init>") || !c.desc.equals("()V")) continue;
            AbstractInsnNode receiver = previous(n);
            if (!(receiver instanceof VarInsnNode v) || v.getOpcode() != ALOAD || v.var != 0) continue;
            validatePrefix(owner, m, receiver);
            m.instructions.remove(receiver);
            m.instructions.remove(n);
            m.instructions.insert(n);
            m.instructions.insert(receiver);
            return;
        }
    }

    /** Simulate a straight-line prefix; this may only be the receiver of an own PUTFIELD. */
    private static void validatePrefix(ClassNode owner, MethodNode m, AbstractInsnNode end) {
        List<Boolean> stack = new ArrayList<>();
        for (AbstractInsnNode n = m.instructions.getFirst(); n != end; n = n.getNext()) {
            if (n == null) throw new IllegalArgumentException("invalid constructor prefix: " + owner.name + m.desc);
            int op = n.getOpcode();
            if (op < 0 || op == NOP) continue;
            if (n instanceof VarInsnNode v && op >= ILOAD && op <= ALOAD) {
                push(stack, op == LLOAD || op == DLOAD ? 2 : 1, op == ALOAD && v.var == 0); continue;
            }
            if (n instanceof FieldInsnNode f) {
                int size = Type.getType(f.desc).getSize();
                if (op == PUTFIELD && f.owner.equals(owner.name)
                        && owner.fields.stream().anyMatch(x -> x.name.equals(f.name) && x.desc.equals(f.desc))) {
                    pop(stack, size, false);
                    if (stack.isEmpty() || !stack.remove(stack.size() - 1)) fail(owner, m);
                } else if (op == GETSTATIC) push(stack, size, false);
                else if (op == GETFIELD) { pop(stack, 1, false); push(stack, size, false); }
                else fail(owner, m);
                continue;
            }
            if (n instanceof MethodInsnNode c) {
                int count = c.getOpcode() == INVOKESTATIC ? 0 : 1;
                for (Type t : Type.getArgumentTypes(c.desc)) count += t.getSize();
                pop(stack, count, false); push(stack, Type.getReturnType(c.desc).getSize(), false); continue;
            }
            if (n instanceof InvokeDynamicInsnNode c) {
                int count = 0; for (Type t : Type.getArgumentTypes(c.desc)) count += t.getSize();
                pop(stack, count, false); push(stack, Type.getReturnType(c.desc).getSize(), false); continue;
            }
            if (op >= ACONST_NULL && op <= LDC) {
                int size = op == LCONST_0 || op == LCONST_1 || op == DCONST_0 || op == DCONST_1
                        || n instanceof LdcInsnNode l && (l.cst instanceof Long || l.cst instanceof Double) ? 2 : 1;
                push(stack, size, false); continue;
            }
            if (op == NEW) { push(stack, 1, false); continue; }
            if (op == DUP) { if (stack.isEmpty() || stack.get(stack.size()-1)) fail(owner, m); push(stack, 1, false); continue; }
            if (op == CHECKCAST) { pop(stack, 1, false); push(stack, 1, false); continue; }
            if (op == POP || op == POP2) { pop(stack, op == POP ? 1 : 2, false); continue; }
            if (op >= IADD && op <= DREM) {
                int width = (op - IADD) % 4 == 1 || (op - IADD) % 4 == 3 ? 2 : 1;
                pop(stack, 2 * width, false); push(stack, width, false); continue;
            }
            if (op >= INEG && op <= DNEG) {
                int width = op == LNEG || op == DNEG ? 2 : 1;
                pop(stack, width, false); push(stack, width, false); continue;
            }
            if (op >= ISHL && op <= LXOR) {
                int width = (op - ISHL) % 2 == 1 ? 2 : 1;
                pop(stack, width + (op <= LUSHR ? 1 : width), false); push(stack, width, false); continue;
            }
            if (op == IINC && n instanceof IincInsnNode i && i.var != 0) continue;
            if (op == INSTANCEOF || op == ARRAYLENGTH) { pop(stack, 1, false); push(stack, 1, false); continue; }
            if (op == NEWARRAY || op == ANEWARRAY) { pop(stack, 1, false); push(stack, 1, false); continue; }
            if (n instanceof MultiANewArrayInsnNode a) { pop(stack, a.dims, false); push(stack, 1, false); continue; }
            if (op >= IALOAD && op <= SALOAD) { pop(stack, 2, false); push(stack, op == LALOAD || op == DALOAD ? 2 : 1, false); continue; }
            if (op >= I2L && op <= I2S) {
                int input = op >= L2I && op <= L2D || op >= D2I && op <= D2F ? 2 : 1;
                int output = op == I2L || op == I2D || op == L2D || op == F2L || op == F2D || op == D2L ? 2 : 1;
                pop(stack, input, false); push(stack, output, false); continue;
            }
            if (op >= LCMP && op <= DCMPG) { pop(stack, op == FCMPL || op == FCMPG ? 2 : 4, false); push(stack, 1, false); continue; }
            if (op >= ISTORE && op <= ASTORE && n instanceof VarInsnNode v && v.var != 0) {
                pop(stack, op == LSTORE || op == DSTORE ? 2 : 1, false); continue;
            }
            // Refuse unknown stack/control effects rather than accidentally exposing uninitialized this.
            fail(owner, m);
        }
        // Other values may legitimately remain on the operand stack across the void Object call.
        if (stack.contains(true)) fail(owner, m);
    }

    private static void push(List<Boolean> stack, int count, boolean self) { for (int i=0;i<count;i++) stack.add(self); }
    private static void pop(List<Boolean> stack, int count, boolean self) {
        for (int i=0;i<count;i++) if (stack.isEmpty() || stack.remove(stack.size()-1) != self)
            throw new IllegalArgumentException("unsafe use of this / invalid stack before Object super()");
    }
    private static void fail(ClassNode c, MethodNode m) {
        throw new IllegalArgumentException("unsupported/unsafe prefix before Object super(): " + c.name + "#" + m.name + m.desc);
    }
}
