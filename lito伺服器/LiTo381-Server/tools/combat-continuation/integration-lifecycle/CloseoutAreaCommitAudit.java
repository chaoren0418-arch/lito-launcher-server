import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/** Invert the ten explicit guards and compare every original method without execution. */
public final class CloseoutAreaCommitAudit {
    static final String P = "local/lito381/server/", H = P + "NativeCloseoutAreaCommit";
    static void need(boolean value, String why) { CloseoutAreaOutputAudit.need(value, why); }
    static void inverse(ClassNode c) {
        int count = 0;
        for (var m : c.methods) for (var n : m.instructions.toArray())
            if (n instanceof MethodInsnNode call && call.owner.equals(H)) {
                need(call.name.equals("allow") && call.desc.equals("(Ljava/lang/Object;JLjava/lang/Object;)Z") && call.getOpcode() == INVOKESTATIC, "guard signature");
                need((m.access & ACC_SYNCHRONIZED) != 0 && Set.of("slow", "windShackle", "consumeEraseMagic", "cancelSleep").contains(m.name), "original mutation monitor");
                var actor = n.getPrevious(); var id = actor.getPrevious(); var owner = id.getPrevious(); var stored = owner.getPrevious();
                need(actor instanceof VarInsnNode a && a.getOpcode() == ALOAD && stored instanceof VarInsnNode s && s.getOpcode() == ASTORE && s.var == a.var, "original resolved actor");
                need(id instanceof VarInsnNode i && i.getOpcode() == LLOAD && i.var == 1 && owner instanceof VarInsnNode o && o.getOpcode() == ALOAD && o.var == 0, "original encounter and id");
                var jump = n.getNext(); var zero = jump.getNext(); var result = zero.getNext(); var rest = result.getNext();
                need(jump instanceof JumpInsnNode j && j.getOpcode() == IFNE && j.label == rest && zero.getOpcode() == ICONST_0 && result.getOpcode() == IRETURN, "bounded false-return guard");
                for (var item : List.of(owner, id, actor, n, jump, zero, result, rest)) m.instructions.remove(item);
                count++;
            }
        need(count == 5, "five guards per encounter");
    }
    public static void main(String[] args) throws Exception {
        var owners = List.of("BattleQuestEncounter", "BattleTimeAttackEncounter"); int methods = 0;
        try (var base = new JarFile(args[0]); var candidate = new JarFile(args[1])) {
            for (var name : owners) {
                var a = CloseoutAreaOutputAudit.read(base, name); var b = CloseoutAreaOutputAudit.read(candidate, name); inverse(b);
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
                if (owners.stream().anyMatch(n -> entry.getName().equals(P + n + ".class"))) continue;
                need(Arrays.equals(base.getInputStream(entry).readAllBytes(), candidate.getInputStream(candidate.getJarEntry(entry.getName())).readAllBytes()), "retained entry " + entry.getName());
            }
        }
        Files.writeString(Path.of(args[2]), "Static inversion: ten resolved-actor guards in two encounter classes; " + methods + " original methods retained; unrelated archive entries identical. No application execution or runtime tests.\n");
    }
}
