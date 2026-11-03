import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutAreaPlayerCommitMethods {
    static final String P = "local/lito381/server/", H = P + "NativeCloseoutAreaPlayerCommit";
    static final Map<String, Set<String>> METHODS = Map.of(
        "BattleTimedStatus", Set.of("activateSlow(JII)L" + P + "BattleTimedStatus$SpeedChange;", "consumeCounterMagic(J)Z", "cancelSleep()Z"),
        "BattleEraseMagicStatus", Set.of("consume(J)Z", "consume(L" + P + "BattleEraseMagicStatus$Lease;)Z"),
        "BattleWindShackleStatus", Set.of("activate(JI)Z"));
    static void patch(ClassNode c, String name) {
        int count = 0;
        for (var m : c.methods) if (METHODS.get(name).contains(m.name + m.desc)) {
            if ((m.access & ACC_SYNCHRONIZED) == 0) throw new AssertionError("original status monitor required");
            var code = new InsnList(); var original = new LabelNode();
            code.add(new VarInsnNode(ALOAD, 0));
            code.add(new MethodInsnNode(INVOKESTATIC, H, "allow", "(Ljava/lang/Object;)Z", false));
            code.add(new JumpInsnNode(IFNE, original));
            if (m.name.equals("activateSlow")) {
                code.add(new MethodInsnNode(INVOKESTATIC, H, "unchangedSlow", "()L" + P + "BattleTimedStatus$SpeedChange;", false));
                code.add(new InsnNode(ARETURN));
            } else { code.add(new InsnNode(ICONST_0)); code.add(new InsnNode(IRETURN)); }
            code.add(original); m.instructions.insert(code); count++;
        }
        if (count != METHODS.get(name).size()) throw new AssertionError("exact status mutation entries");
    }
    public static void main(String[] args) throws Exception {
        var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(args[0]).toUri().toURL());
        try (var files = Files.list(Path.of(args[2]))) {
            for (var p : files.filter(x -> x.toString().endsWith(".jar")).toList()) urls.add(p.toUri().toURL());
        }
        try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), ClassLoader.getPlatformClassLoader()); var jar = new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy = loader;
            for (var name : METHODS.keySet()) {
                var c = FreezeAcceptedMethods.read(jar, P + name); patch(c, name);
                var out = Path.of(args[1]).resolve(P + name + ".class");
                Files.createDirectories(out.getParent()); Files.write(out, FreezeAcceptedMethods.bytes(c, true));
            }
        }
    }
}
