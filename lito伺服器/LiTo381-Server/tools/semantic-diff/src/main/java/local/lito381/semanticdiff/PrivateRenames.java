package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Directional, explicitly approved private shadow renames. Uses headers, never application loading. */
final class PrivateRenames implements Opcodes {
    private PrivateRenames() {}

    static void configure(Map<String, Map<String, String>> rules, Set<String> scope,
            JarIndex left, JarIndex right) {
        for (var entry : rules.entrySet()) {
            String owner = entry.getKey();
            if (!scope.contains(owner)) throw new IllegalArgumentException("private-rename class outside comparison scope: " + owner);
            ClassNode l = left.header(owner), r = right.header(owner);
            for (var rename : entry.getValue().entrySet()) {
                String key = rename.getKey(), target = rename.getValue();
                String desc = key.substring(key.indexOf('('));
                String targetKey = target + desc;
                right.privateRenameTargets.computeIfAbsent(owner, k -> new HashSet<>()).add(targetKey);
                MethodNode lm = declared(l, key), rm = declared(r, targetKey);
                if (!privateInstance(lm) || !privateInstance(rm)
                        || l.methods.stream().anyMatch(m -> m.name.equals(target))
                        || r.methods.stream().anyMatch(m -> (m.name + m.desc).equals(key))
                        || !ancestor(l, targetKey, left) || !ancestor(r, targetKey, right)) continue;
                right.privateRenames.computeIfAbsent(owner, k -> new TreeMap<>()).put(targetKey, key);
            }
        }
    }

    private static MethodNode declared(ClassNode c, String key) {
        return c == null ? null : c.methods.stream().filter(m -> (m.name + m.desc).equals(key)).findFirst().orElse(null);
    }

    private static boolean privateInstance(MethodNode m) {
        return m != null && !m.name.startsWith("<") && (m.access & (ACC_PRIVATE | ACC_STATIC)) == ACC_PRIVATE;
    }

    private static boolean ancestor(ClassNode c, String key, JarIndex jar) {
        Set<String> seen = new HashSet<>();
        for (String name = c.superName; name != null && seen.add(name);) {
            ClassNode parent = jar.header(name);
            if (parent == null) return false;
            MethodNode m = declared(parent, key);
            if (m != null && (m.access & (ACC_PRIVATE | ACC_STATIC)) == 0) return true;
            name = parent.superName;
        }
        return false;
    }

    /** Called after private opcode normalization, before local webs / lambda identities. */
    static void calls(ClassNode c, JarIndex jar) {
        Map<String, String> pairs = jar.privateRenames.getOrDefault(c.name, Map.of());
        if (pairs.isEmpty()) return;
        for (MethodNode m : c.methods) for (AbstractInsnNode n : m.instructions) {
            if (!(n instanceof MethodInsnNode call) || !call.owner.equals(c.name) || call.itf
                    || (call.getOpcode() != INVOKEVIRTUAL && call.getOpcode() != INVOKESPECIAL)) continue;
            String key = pairs.get(call.name + call.desc);
            if (key != null) call.name = key.substring(0, key.indexOf('('));
        }
    }
}
