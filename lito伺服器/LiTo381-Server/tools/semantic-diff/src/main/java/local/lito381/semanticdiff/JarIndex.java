package local.lito381.semanticdiff;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Loads {@code .class} bytes from a JAR and builds per-class synthetic name maps. */
final class JarIndex {
    final Path path;
    final Map<String, byte[]> classes = new TreeMap<>();
    /** owner internal name → original synthetic method name → canonical name */
    final Map<String, Map<String, String>> syntheticMethods = new TreeMap<>();

    private JarIndex(Path path) {
        this.path = path;
    }

    static JarIndex load(Path jar) throws IOException {
        JarIndex index = new JarIndex(jar);
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class")) {
                    continue;
                }
                byte[] bytes;
                try (InputStream in = zip.getInputStream(entry)) {
                    bytes = readAll(in);
                }
                String internal = name.substring(0, name.length() - 6);
                index.classes.put(internal, bytes);
            }
        }
        for (Map.Entry<String, byte[]> e : index.classes.entrySet()) {
            index.syntheticMethods.put(e.getKey(), syntheticMap(e.getValue()));
        }
        return index;
    }

    String remapMethod(String owner, String name) {
        Map<String, String> map = syntheticMethods.get(owner);
        if (map == null) {
            return name;
        }
        return map.getOrDefault(name, name);
    }

    ClassNode readTree(String internalName) {
        byte[] bytes = classes.get(internalName);
        if (bytes == null) {
            return null;
        }
        ClassNode node = new ClassNode();
        // SKIP_DEBUG drops LineNumberTable / LocalVariable(Type)Table / SourceFile /
        // SourceDebugExtension / MethodParameters. SKIP_FRAMES drops StackMap (not compared).
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static Map<String, String> syntheticMap(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(
                node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Map<String, Integer> counters = new LinkedHashMap<>();
        Map<String, String> map = new LinkedHashMap<>();
        if (node.methods == null) {
            return Collections.emptyMap();
        }
        for (MethodNode method : node.methods) {
            String prefix = syntheticPrefix(method.name);
            if (prefix == null) {
                continue;
            }
            int n = counters.getOrDefault(prefix, 0);
            counters.put(prefix, n + 1);
            map.put(method.name, prefix + "#" + n);
        }
        return map.isEmpty() ? Collections.emptyMap() : map;
    }

    /**
     * {@code lambda$xxx$N} → prefix {@code lambda$xxx$}; {@code access$NNN} → prefix {@code access$}.
     * Numbering is assigned later by appearance order.
     */
    static String syntheticPrefix(String name) {
        if (name == null) {
            return null;
        }
        if (name.startsWith("lambda$")) {
            int last = name.lastIndexOf('$');
            if (last > "lambda$".length() && last < name.length() - 1) {
                String number = name.substring(last + 1);
                if (isDigits(number)) {
                    return name.substring(0, last + 1);
                }
            }
        }
        if (name.startsWith("access$")) {
            String number = name.substring("access$".length());
            if (!number.isEmpty() && isDigits(number)) {
                return "access$";
            }
        }
        return null;
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
