import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Add explicit output routing; original target selection/mutation methods remain intact. */
public final class CloseoutAreaOutputMethods {
    static final String P = "local/lito381/server/", H = P + "NativeCloseoutAreaOutput";
    static void patch(ClassNode c) {
        int count = 0;
        if (c.name.equals(P + "NativeAreaMaterialAction$Context")) {
            if (c.fields.stream().anyMatch(f -> f.name.equals("nativeCloseoutAreaOutput"))) throw new AssertionError("fresh output scope");
            c.fields.add(new FieldNode(ACC_TRANSIENT, "nativeCloseoutAreaOutput", "Ljava/lang/Object;", null, null));
            int returns = 0;
            for (var m : c.methods) if (m.name.equals("npc") && m.desc.equals("(J)Z")) {
                for (var n : m.instructions.toArray()) if (n.getOpcode() == IRETURN) {
                    var code = new InsnList(); code.add(new VarInsnNode(ALOAD, 0)); code.add(new VarInsnNode(LLOAD, 1));
                    code.add(new MethodInsnNode(INVOKESTATIC, H, "npc", "(ZL" + c.name + ";J)Z", false));
                    m.instructions.insertBefore(n, code); returns++;
                }
            }
            if (returns != 1) throw new AssertionError("one original Context NPC predicate result");
            return;
        }
        if (c.name.equals(P + "ClientSession")) {
            var counts = new HashMap<String,Integer>();
            for (var m : c.methods) if (Set.of("nativeAreaBattleSlowBody", "nativeMaterialAreaWindBody").contains(m.name)) {
                for (var n : m.instructions.toArray()) if (n instanceof MethodInsnNode call && call.owner.equals(P + "NativeSourceAreaClassSelection") && call.name.equals("iterator")) {
                    m.instructions.insert(call, new MethodInsnNode(INVOKESTATIC, H, "iterator", "(Ljava/util/Iterator;)Ljava/util/Iterator;", false));
                    counts.merge(m.name, 1, Integer::sum);
                }
            }
            if (!counts.equals(Map.of("nativeAreaBattleSlowBody", 3, "nativeMaterialAreaWindBody", 4))) throw new AssertionError("seven original calculation iterators " + counts);
            return;
        }
        if (c.name.equals(P + "NativeAreaMaterialAction")) {
            for (var m : c.methods) if (m.name.equals("captureOutput")) {
                var code = new InsnList(); var rest = new LabelNode();
                for (int i = 0; i < 3; i++) code.add(new VarInsnNode(ALOAD, i));
                code.add(new MethodInsnNode(INVOKESTATIC, H, "capture", m.desc, false));
                code.add(new JumpInsnNode(IFEQ, rest)); code.add(new InsnNode(ICONST_1)); code.add(new InsnNode(IRETURN)); code.add(rest);
                m.instructions.insert(code); count++;
            }
            if (count != 1) throw new AssertionError("one existing area output entry");
            return;
        }
        if (c.name.endsWith("$Aim")) {
            int installed = 0, closed = 0;
            for (var m : c.methods) if (m.name.equals("<init>")) {
                for (var n : m.instructions.toArray()) if (n instanceof MethodInsnNode call
                        && call.owner.equals(P + "NativeSourceAreaPairFirst") && call.name.equals("capture")) {
                    var code = new InsnList(); code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new MethodInsnNode(INVOKESTATIC, H, "install", "(L" + c.name + ";)V", false));
                    m.instructions.insert(call, code); installed++;
                }
            } else if (m.name.equals("close")) {
                var code = new InsnList(); code.add(new VarInsnNode(ALOAD, 0));
                code.add(new MethodInsnNode(INVOKESTATIC, H, "close", "(L" + c.name + ";)V", false));
                m.instructions.insert(code); closed++;
            }
            if (installed != 1 || closed != 1) throw new AssertionError("one first install and complete original close");
            return;
        }
        if (!c.name.equals(P + "NativeSourceWeaponFinalFrame")) {
            var original = c.methods.stream().filter(m -> m.name.equals("lambda$run$2")).findFirst().orElseThrow();
            var sibling = new MethodNode(original.access & ~(ACC_PRIVATE | ACC_SYNTHETIC), "nativeCloseoutAreaOriginalProducer", original.desc,
                    original.signature, original.exceptions.toArray(String[]::new));
            original.accept(sibling); c.methods.add(sibling);
            int produced = 0, registered = 0, fallbacks = 0;
            for (var m : c.methods) if (m.name.equals("run")) {
                int originals = 0;
                for (var n : m.instructions.toArray()) if (n instanceof MethodInsnNode call && call.owner.equals(c.name) && call.name.equals("original")) {
                    if (++originals != 2) continue;
                    var loads = new InsnList(); loads.add(new VarInsnNode(ALOAD, 5)); loads.add(new VarInsnNode(ALOAD, 6));
                    m.instructions.insertBefore(call, loads); call.owner = P + "NativeCloseoutAreaNatural";
                    call.name = c.name.endsWith("MassSlowTargets") ? "slow" : "wind";
                    call.desc = call.desc.replace(")V", "L" + P + "NativeAreaMaterialAction$Context;L" + P + "NativeAreaMaterialAction$Npc;)V");
                    fallbacks++;
                }
                if (originals != 2) throw new AssertionError("two original dispatcher fallback branches");
                for (var n : m.instructions.toArray()) if (n instanceof InvokeDynamicInsnNode indy) {
                    for (int i = 0; i < indy.bsmArgs.length; i++) if (indy.bsmArgs[i] instanceof Handle h
                            && h.getOwner().equals(c.name) && h.getName().equals(original.name)) {
                        if (h.getTag() != H_INVOKESTATIC || !h.getDesc().equals(original.desc)) throw new AssertionError("producer descriptor");
                        indy.bsmArgs[i] = new Handle(H_INVOKESTATIC, H, "produce", h.getDesc(), false); produced++;
                    }
                }
            } else if (m.name.equals("register")) {
                var code = new InsnList(); var rest = new LabelNode(); code.add(new VarInsnNode(ALOAD, 0));
                code.add(new MethodInsnNode(INVOKESTATIC, H, "register", m.desc.replace(")V", ")Z"), false));
                code.add(new JumpInsnNode(IFEQ, rest)); code.add(new InsnNode(RETURN)); code.add(rest);
                m.instructions.insert(code); registered++;
            }
            if (produced != 1 || registered != 1 || fallbacks != 1) throw new AssertionError("one producer/registration/natural fallback seam");
            for (var m : c.methods) if (m.name.equals("captureOutput")) {
                var x = new InsnList(); var old = new LabelNode();
                for (int i = 0; i < 3; i++) x.add(new VarInsnNode(ALOAD, i));
                x.add(new MethodInsnNode(INVOKESTATIC, H, "capture", m.desc, false));
                x.add(new JumpInsnNode(IFEQ, old)); x.add(new InsnNode(ICONST_1));
                x.add(new InsnNode(IRETURN)); x.add(old); m.instructions.insert(x); count++;
            }
            if (count != 1) throw new AssertionError("one existing capture seam");
            return;
        }
        for (var m : c.methods) if (m.name.equals("frame")) {
            var x = new InsnList(); var old = new LabelNode();
            for (int i = 0; i < 4; i++) x.add(new VarInsnNode(ALOAD, i));
            x.add(new MethodInsnNode(INVOKESTATIC, H, "frame", m.desc.replace(")V", ")Z"), false));
            x.add(new JumpInsnNode(IFEQ, old)); x.add(new InsnNode(RETURN)); x.add(old);
            m.instructions.insert(x); count++;
        } else if (m.name.equals("allow")) {
            var x = new InsnList(); var old = new LabelNode();
            x.add(new VarInsnNode(ALOAD, 2));
            x.add(new MethodInsnNode(INVOKESTATIC, H, "kind", "(Ljava/lang/Object;)Z", false));
            x.add(new JumpInsnNode(IFEQ, old));
            for (int i = 0; i < 3; i++) x.add(new VarInsnNode(ALOAD, i));
            x.add(new MethodInsnNode(INVOKESTATIC, H, "allow", m.desc, false));
            x.add(new InsnNode(IRETURN)); x.add(old); m.instructions.insert(x); count++;
        }
        if (count != 2) throw new AssertionError("two complete original final sender dispatch seams");
    }
    public static void main(String[] args) throws Exception {
        var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(args[0]).toUri().toURL());
        try (var files = Files.list(Path.of(args[2]))) {
            for (var p : files.filter(x -> x.toString().endsWith(".jar")).toList()) urls.add(p.toUri().toURL());
        }
        try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), ClassLoader.getPlatformClassLoader());
             var jar = new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy = loader;
            for (var name : List.of("NativeSourceMassSlowTargets", "NativeSourceWindTargets", "NativeSourceWeaponFinalFrame",
                    "ClientSession", "NativeAreaMaterialAction", "NativeAreaMaterialAction$Context", "NativeSourceMassSlowTargets$Aim", "NativeSourceWindTargets$Aim")) {
                var c = FreezeAcceptedMethods.read(jar, P + name); patch(c);
                var out = Path.of(args[1]).resolve(P + name + ".class");
                Files.createDirectories(out.getParent()); Files.write(out, FreezeAcceptedMethods.bytes(c, true));
            }
        }
    }
}
