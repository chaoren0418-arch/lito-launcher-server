import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/** No application loading: inverse the six prefixes and retain every original method. */
public final class CloseoutAreaPlayerCommitAudit {
    static final String P = "local/lito381/server/", H = P + "NativeCloseoutAreaPlayerCommit";
    static void need(boolean value, String why) { CloseoutAreaOutputAudit.need(value, why); }
    static int inverse(ClassNode c) {
        int count = 0;
        for (var m : c.methods) {
            var first = m.instructions.getFirst(); if (first == null) continue;
            var call = first.getNext();
            if (!(call instanceof MethodInsnNode q) || !q.owner.equals(H)) continue;
            need(first instanceof VarInsnNode v && v.getOpcode() == ALOAD && v.var == 0 && q.name.equals("allow") && q.desc.equals("(Ljava/lang/Object;)Z"), "explicit original status argument");
            need((m.access & ACC_SYNCHRONIZED) != 0, "original status monitor");
            var jump = call.getNext(); var fallback = jump.getNext(); var result = fallback.getNext(); var rest = result.getNext();
            need(jump instanceof JumpInsnNode j && j.getOpcode() == IFNE && j.label == rest, "bounded original-body branch");
            if (m.name.equals("activateSlow")) need(fallback instanceof MethodInsnNode n && n.owner.equals(H) && n.name.equals("unchangedSlow") && result.getOpcode() == ARETURN, "neutral slow result");
            else need(fallback.getOpcode() == ICONST_0 && result.getOpcode() == IRETURN, "false rejected result");
            for (var n : List.of(first, call, jump, fallback, result, rest)) m.instructions.remove(n);
            count++;
        }
        return count;
    }
    public static void main(String[] args) throws Exception {
        var owners = Map.of("BattleTimedStatus", 3, "BattleEraseMagicStatus", 2, "BattleWindShackleStatus", 1); int methods = 0;
        try (var base = new JarFile(args[0]); var candidate = new JarFile(args[1])) {
            for (var name : owners.keySet()) {
                var a = CloseoutAreaOutputAudit.read(base, name); var b = CloseoutAreaOutputAudit.read(candidate, name);
                need(inverse(b) == owners.get(name), "expected prefix count " + name);
                need(a.fields.size() == b.fields.size() && a.methods.size() == b.methods.size(), "member counts");
                for (int i = 0; i < a.fields.size(); i++) {
                    var x = a.fields.get(i); var y = b.fields.get(i);
                    need(x.name.equals(y.name) && x.desc.equals(y.desc) && x.access == y.access && Objects.equals(x.value, y.value), "unchanged field");
                }
                for (int i = 0; i < a.methods.size(); i++) {
                    var x = a.methods.get(i); var y = b.methods.get(i);
                    need(x.name.equals(y.name) && x.desc.equals(y.desc) && x.access == y.access && CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)), "retained method " + name + "." + x.name); methods++;
                }
            }
            for (var entry : Collections.list(base.entries())) {
                if (owners.keySet().stream().anyMatch(n -> entry.getName().equals(P + n + ".class"))) continue;
                need(Arrays.equals(base.getInputStream(entry).readAllBytes(), candidate.getInputStream(candidate.getJarEntry(entry.getName())).readAllBytes()), "retained entry " + entry.getName());
            }
        }
        Files.writeString(Path.of(args[2]), "Static inversion: six status guards in three classes; " + methods + " original methods retained; unrelated entries identical. No application execution or runtime tests.\n");
    }
}
