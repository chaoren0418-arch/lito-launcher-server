package local.lito381.asrecord;

import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** 唯讀 ASM 掃描；stdout 為 JSON，供 scan-record-fields.py 加上批次與雜湊。 */
public final class ScanRecordFields {
    public static void main(String[] args) throws Exception {
        List<Object> rows = new ArrayList<>();
        for (String arg : args) scan(Path.of(arg), rows);
        System.out.println(json(rows));
    }

    private static void scan(Path path, List<Object> rows) throws Exception {
        Map<String, ClassNode> classes = new TreeMap<>();
        try (JarFile jar = new JarFile(path.toFile())) {
            for (var entry : jar.stream().filter(e -> e.getName().endsWith(".class"))
                    .sorted(Comparator.comparing(e -> e.getName())).toList()) {
                ClassNode node = new ClassNode(Opcodes.ASM9);
                try (var input = jar.getInputStream(entry)) {
                    new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                classes.put(entry.getName(), node);
            }
        }
        // 只採實際 GETFIELD/PUTFIELD 指令，未使用的 constant-pool 引用不算直接存取。
        Map<List<String>, List<Object>> accesses = new HashMap<>();
        for (var entry : classes.entrySet()) {
            ClassNode c = entry.getValue();
            for (MethodNode m : c.methods) {
                Set<List<String>> seen = new HashSet<>();
                for (var insn : m.instructions) {
                    if (!(insn instanceof FieldInsnNode f)
                            || (f.getOpcode() != Opcodes.GETFIELD && f.getOpcode() != Opcodes.PUTFIELD)) continue;
                    String op = f.getOpcode() == Opcodes.GETFIELD ? "GETFIELD" : "PUTFIELD";
                    if (!seen.add(List.of(f.owner, f.name, f.desc, op))) continue;
                    accesses.computeIfAbsent(List.of(f.owner, f.name, f.desc), k -> new ArrayList<>())
                            .add(row("class", c.name, "entry", entry.getKey(),
                                    "method", m.name, "descriptor", m.desc, "opcode", op));
                }
            }
        }
        for (var entry : classes.entrySet()) {
            ClassNode c = entry.getValue();
            if (!"java/lang/Record".equals(c.superName)) continue;
            var components = c.recordComponents == null ? List.<RecordComponentNode>of() : c.recordComponents;
            var fields = c.fields.stream().filter(f -> (f.access & Opcodes.ACC_STATIC) == 0).toList();
            if (fields.size() <= components.size()) continue;
            Set<List<String>> keys = new HashSet<>();
            List<Object> componentRows = new ArrayList<>();
            for (var component : components) {
                keys.add(List.of(component.name, component.descriptor));
                componentRows.add(row("name", component.name, "descriptor", component.descriptor,
                        "signature", component.signature));
            }
            List<Object> extras = new ArrayList<>();
            Set<String> direct = new TreeSet<>();
            for (FieldNode f : fields) {
                if (keys.contains(List.of(f.name, f.desc))) continue;
                List<Object> uses = accesses.getOrDefault(List.of(c.name, f.name, f.desc), List.of());
                for (Object use : uses) direct.add((String) ((Map<?, ?>) use).get("class"));
                extras.add(row("name", f.name, "descriptor", f.desc, "access", f.access,
                        "flags", flags(f.access), "direct_accesses", uses));
            }
            rows.add(row("jar", path.getFileName().toString(), "entry", entry.getKey(),
                    "class", c.name, "instance_field_count", fields.size(),
                    "components", componentRows, "extra_fields", extras,
                    "direct_access_classes", new ArrayList<>(direct)));
        }
    }

    private static List<String> flags(int access) {
        int[] masks = {1, 2, 4, 8, 16, 64, 128, 4096, 16384};
        String[] names = {"ACC_PUBLIC", "ACC_PRIVATE", "ACC_PROTECTED", "ACC_STATIC", "ACC_FINAL",
                "ACC_VOLATILE", "ACC_TRANSIENT", "ACC_SYNTHETIC", "ACC_ENUM"};
        List<String> result = new ArrayList<>();
        for (int i = 0; i < masks.length; i++) if ((access & masks[i]) != 0) result.add(names[i]);
        return result;
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            var parts = new ArrayList<String>();
            map.forEach((k, v) -> parts.add(json(k) + ":" + json(v)));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof List<?> list) return "[" + String.join(",", list.stream().map(ScanRecordFields::json).toList()) + "]";
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32 || c > 126) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
