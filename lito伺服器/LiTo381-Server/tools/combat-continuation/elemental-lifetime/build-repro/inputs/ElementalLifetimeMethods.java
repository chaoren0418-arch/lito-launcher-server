import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Start the NPC lease at construction, after the source NPC and landing lookups. */
public final class ElementalLifetimeMethods {
    static final String P = "local/lito381/server/", PC = P + "ClientSession.class";
    static byte[] transform(byte[] bytes, boolean reverse) {
        var c = NativeSkillConsumptionMethods.read(bytes);
        var m = c.methods.stream().filter(x -> x.name.equals("nativeMaterialElementalSummonBody")).findFirst().orElseThrow();
        int changes = 0;
        for (var n : m.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode call) || !call.owner.equals(P + "BattleCompanion")
                    || !Set.of("lesserElemental", "greaterElemental").contains(call.name)) continue;
            var time = NativeSkillConsumptionMethods.prev(n);
            if (reverse) {
                if (!(time instanceof MethodInsnNode clock) || clock.getOpcode() != INVOKESTATIC
                        || !clock.owner.equals("java/lang/System") || !clock.name.equals("nanoTime")
                        || !clock.desc.equals("()J")) throw new AssertionError("missing construction clock");
                m.instructions.set(time, new VarInsnNode(LLOAD, 11));
            } else {
                if (!(time instanceof VarInsnNode v) || v.getOpcode() != LLOAD || v.var != 11)
                    throw new AssertionError("unreviewed factory timestamp");
                m.instructions.set(time, new MethodInsnNode(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false));
            }
            changes++;
        }
        if (changes != 2) throw new AssertionError("expected exactly two elemental factory timestamps");
        return NativeSkillConsumptionMethods.bytes(c, false);
    }
    public static void main(String[] args) throws Exception {
        try (var old = new JarFile(args[1])) {
            byte[] bytes = old.getInputStream(old.getJarEntry(PC)).readAllBytes();
            if (args[0].equals("splice")) {
                var path = Path.of(args[2]).resolve(PC); Files.createDirectories(path.getParent());
                Files.write(path, transform(bytes, false));
            } else try (var next = new JarFile(args[2])) {
                byte[] actual = next.getInputStream(next.getJarEntry(PC)).readAllBytes();
                if (!Arrays.equals(actual, transform(bytes, false))) throw new AssertionError("candidate transform mismatch");
                var inverse = NativeSkillConsumptionMethods.read(transform(actual, true));
                if (!Arrays.equals(NativeSkillConsumptionMethods.bytes(inverse, true),
                        NativeSkillConsumptionMethods.bytes(NativeSkillConsumptionMethods.read(bytes), true)))
                    throw new AssertionError("unrelated PC code changed");
                System.out.println("PASS exact two-clock inverse; cost, reuse, effect, NPC identity, expiry mechanics unchanged");
            }
        }
    }
}
