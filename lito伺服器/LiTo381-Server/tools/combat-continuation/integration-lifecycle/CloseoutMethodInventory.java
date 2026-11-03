import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.util.*;

/** Bytecode-only inventory. No application classes are loaded or initialized. */
public final class CloseoutMethodInventory {
    static String text(MethodNode m) {
        var p = new Textifier(); m.accept(new TraceMethodVisitor(p));
        var s = new StringWriter(); p.print(new PrintWriter(s)); return s.toString();
    }
    public static void main(String[] args) throws Exception {
        var lines = new ArrayList<String>();
        lines.add("owner\tmethod\tdescriptor\tsha256");
        try (var jar = new JarFile(args[0])) {
            for (String owner : List.of("ClientSession", "Protocol", "GmTravel", "InventoryCatalog$NativeArmorStatus")) {
                var c = new ClassNode(Opcodes.ASM8);
                new ClassReader(jar.getInputStream(jar.getJarEntry("local/lito381/server/" + owner + ".class")))
                    .accept(c, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (var m : c.methods) {
                    var digest = java.security.MessageDigest.getInstance("SHA-256").digest(text(m).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    lines.add(owner + "\t" + m.name + "\t" + m.desc + "\t" + HexFormat.of().formatHex(digest));
                }
            }
        }
        Files.write(Path.of(args[1]), lines);
    }
}
