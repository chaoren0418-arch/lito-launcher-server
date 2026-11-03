import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Guard only the actual resolved NPC, preserving original lookup and mutation bodies. */
public final class CloseoutAreaCommitMethods {
    static final String P = "local/lito381/server/";
    static void patch(ClassNode c) {
        int methods = 0;
        for (var m : c.methods) if (Set.of("slow", "windShackle", "consumeEraseMagic", "cancelSleep").contains(m.name)) {
            if ((m.access & ACC_SYNCHRONIZED) == 0 || !m.desc.startsWith("(J") || !m.desc.endsWith(")Z"))
                throw new AssertionError("original synchronized boolean encounter method");
            int sites = 0;
            for (var n : m.instructions.toArray()) if (n instanceof VarInsnNode store && n.getOpcode() == ASTORE) {
                var previous = n.getPrevious();
                while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
                boolean actor = previous instanceof TypeInsnNode cast && cast.getOpcode() == CHECKCAST && cast.desc.equals(c.name + "$Monster")
                        || previous instanceof MethodInsnNode call && call.owner.equals(c.name) && call.name.equals("monster") && call.desc.equals("(J)L" + c.name + "$Monster;");
                if (!actor) continue;
                var code = new InsnList(); var original = new LabelNode();
                code.add(new VarInsnNode(ALOAD, 0)); code.add(new VarInsnNode(LLOAD, 1)); code.add(new VarInsnNode(ALOAD, store.var));
                code.add(new MethodInsnNode(INVOKESTATIC, P + "NativeCloseoutAreaCommit", "allow", "(Ljava/lang/Object;JLjava/lang/Object;)Z", false));
                code.add(new JumpInsnNode(IFNE, original)); code.add(new InsnNode(ICONST_0)); code.add(new InsnNode(IRETURN)); code.add(original);
                m.instructions.insert(n, code); sites++;
            }
            if (sites != 1) throw new AssertionError("one original actor resolution: " + c.name + "." + m.name + m.desc);
            methods++;
        }
        if (methods != 5) throw new AssertionError("five status mutation entries: " + c.name);
    }
    public static void main(String[] args) throws Exception {
        var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(args[0]).toUri().toURL());
        try (var files = Files.list(Path.of(args[2]))) {
            for (var p : files.filter(x -> x.toString().endsWith(".jar")).toList()) urls.add(p.toUri().toURL());
        }
        try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), ClassLoader.getPlatformClassLoader()); var jar = new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy = loader;
            for (var name : List.of("BattleQuestEncounter", "BattleTimeAttackEncounter")) {
                var c = FreezeAcceptedMethods.read(jar, P + name); patch(c);
                var out = Path.of(args[1]).resolve(P + name + ".class");
                Files.createDirectories(out.getParent()); Files.write(out, FreezeAcceptedMethods.bytes(c, true));
            }
        }
    }
}
