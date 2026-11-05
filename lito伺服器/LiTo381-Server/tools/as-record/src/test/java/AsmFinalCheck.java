import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import local.lito381.asrecord.AsRecord;

/** 驗證一般／抽象類別補 final、保留父類與冪等性；同時供 CLI 與 Maven fixture 使用。 */
public final class AsmFinalCheck {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        for (String name : new String[]{"PlainFinal", "AbstractFinal"}) {
            byte[] bytes = Files.readAllBytes(root.resolve(name + ".class"));
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            if ((node.access & Opcodes.ACC_RECORD) != 0 || !node.superName.equals(name.equals("PlainFinal") ? "Parent" : "java/lang/Object"))
                throw new AssertionError("changed class kind/superclass");
            for (var field : node.fields) if (!Modifier.isFinal(field.access)) throw new AssertionError("not final: " + field.name);
            if (node.invisibleAnnotations != null && node.invisibleAnnotations.stream().anyMatch(a -> a.desc.endsWith("/AsmFinal;")))
                throw new AssertionError("marker retained");
            if (AsRecord.transform(bytes) != bytes) throw new AssertionError("not idempotent");
        }
        Class<?> plain = Class.forName("PlainFinal");
        Object zero = plain.getDeclaredConstructor().newInstance();
        Object value = plain.getDeclaredConstructor(Object.class).newInstance("ok");
        if (plain.getDeclaredField("value").get(zero) != null || !"ok".equals(plain.getDeclaredField("value").get(value)))
            throw new AssertionError("constructor value changed");
        System.out.println("ASM FINAL CHECK PASS (plain, inherited, abstract, static, marker removal, idempotence, JVM verification)");
    }
}
