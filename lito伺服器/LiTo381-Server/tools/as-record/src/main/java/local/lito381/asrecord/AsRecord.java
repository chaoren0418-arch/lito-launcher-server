package local.lito381.asrecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.*;

/** 將 javac 可接受的欄位宣告還原為保有額外實例欄位的 JVM record。 */
public final class AsRecord implements Opcodes {
    private static final String MARKER = "Llocal/lito381/server/AsRecord;";
    private static final Handle BOOTSTRAP = new Handle(H_INVOKESTATIC,
            "java/lang/runtime/ObjectMethods", "bootstrap",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;"
                    + "Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
            false);

    private AsRecord() {}

    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("usage: as-record <classes directory>");
            Path root = Path.of(args[0]);
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("not a directory (or is a symlink): " + root);
            }
            // 先完成整批驗證；無效標註不會造成先前 class 已被部分轉換。
            Map<Path, byte[]> changes = new LinkedHashMap<>();
            int scanned = 0;
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted().toList()) {
                    if (!path.toString().endsWith(".class")) continue;
                    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("not a regular class file: " + path);
                    }
                    byte[] original = Files.readAllBytes(path);
                    byte[] converted;
                    try {
                        converted = transform(original);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException(path + ": " + ex.getMessage(), ex);
                    }
                    scanned++;
                    if (converted != original) changes.put(path, converted);
                }
            }
            for (var entry : changes.entrySet()) Files.write(entry.getKey(), entry.getValue());
            System.out.println("as-record: scanned=" + scanned + " converted=" + changes.size());
        } catch (Exception ex) {
            System.err.println("as-record: " + ex.getMessage());
            System.exit(1);
        }
    }

    /** 未標註時直接回傳輸入，不重新序列化。 */
    public static byte[] transform(byte[] input) {
        ClassReader reader = new ClassReader(input);
        ClassNode node = new ClassNode(ASM9);
        reader.accept(node, 0);
        List<AnnotationNode> markers = new ArrayList<>();
        collect(node.visibleAnnotations, markers);
        collect(node.invisibleAnnotations, markers);
        if (markers.isEmpty()) return input;
        require(markers.size() == 1, "duplicate @AsRecord");
        require(node.version >= V16, "record requires class version >= 60");
        require("java/lang/Object".equals(node.superName), "superclass must be java/lang/Object");
        require((node.access & (ACC_INTERFACE | ACC_ANNOTATION | ACC_ENUM | ACC_ABSTRACT | ACC_RECORD)) == 0,
                "must be a concrete non-record class");
        require(node.recordComponents == null || node.recordComponents.isEmpty(), "existing Record attribute");

        AnnotationNode marker = markers.get(0);
        List<String> components = names(marker, "components", true);
        List<String> finals = names(marker, "finalFields", false);
        Map<String, FieldNode> fields = new LinkedHashMap<>();
        for (FieldNode field : node.fields) {
            require(fields.put(field.name, field) == null, "ambiguous field: " + field.name);
        }
        for (String name : finals) {
            FieldNode field = fields.get(name);
            require(field != null, "finalFields field not found: " + name);
            require((field.access & (ACC_STATIC | ACC_VOLATILE)) == 0,
                    "finalFields must be non-static and non-volatile: " + name);
            field.access |= ACC_FINAL;
        }
        node.recordComponents = new ArrayList<>();
        Object[] bootstrapArgs = new Object[components.size() + 2];
        bootstrapArgs[0] = Type.getObjectType(node.name);
        bootstrapArgs[1] = String.join(";", components);
        for (int i = 0; i < components.size(); i++) {
            String name = components.get(i);
            FieldNode field = fields.get(name);
            require(field != null, "component field not found: " + name);
            require((field.access & (ACC_PRIVATE | ACC_FINAL | ACC_STATIC)) == (ACC_PRIVATE | ACC_FINAL),
                    "component must be private final instance field: " + name);
            require(node.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals("()" + field.desc)
                    && (m.access & (ACC_PUBLIC | ACC_STATIC)) == ACC_PUBLIC), "missing public accessor: " + name);
            node.recordComponents.add(new RecordComponentNode(name, field.desc, field.signature));
            bootstrapArgs[i + 2] = new Handle(H_GETFIELD, node.name, name, field.desc, false);
        }
        require(node.methods.stream().anyMatch(m -> m.name.equals("<init>")), "missing constructor");
        for (MethodNode method : node.methods) {
            require(!isObjectMethod(method), "do not declare equals/hashCode/toString: " + method.name + method.desc);
            if (!method.name.equals("<init>")) continue;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.getOpcode() == INVOKESPECIAL
                        && call.owner.equals("java/lang/Object") && call.name.equals("<init>")
                        && call.desc.equals("()V")) {
                    AbstractInsnNode previous = call.getPrevious();
                    while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
                    // javac 的 Object super() receiver 是 ALOAD 0；保留 new Object() 的建構呼叫。
                    if (previous instanceof VarInsnNode var && var.getOpcode() == ALOAD && var.var == 0) {
                        call.owner = "java/lang/Record";
                    }
                }
            }
        }
        node.access |= ACC_FINAL | ACC_RECORD;
        node.superName = "java/lang/Record";
        if (node.signature != null) node.signature = recordSignature(node.signature);
        remove(node.visibleAnnotations);
        remove(node.invisibleAnnotations);
        // 與 javac 生成的 bootstrap descriptor 對應；ASM 不會自動補 InnerClasses。
        if (node.innerClasses.stream().noneMatch(c -> c.name.equals("java/lang/invoke/MethodHandles$Lookup"))) {
            node.innerClasses.add(new InnerClassNode("java/lang/invoke/MethodHandles$Lookup",
                    "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC | ACC_STATIC | ACC_FINAL));
        }
        addObjectMethod(node, "toString", "()Ljava/lang/String;", ARETURN, bootstrapArgs);
        addObjectMethod(node, "hashCode", "()I", IRETURN, bootstrapArgs);
        addObjectMethod(node, "equals", "(Ljava/lang/Object;)Z", IRETURN, bootstrapArgs);
        // 沿用既有 frames/maxs；新增方法為直線指令，不載入任何應用類別。
        ClassWriter writer = new ClassWriter(reader, 0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static String recordSignature(String signature) {
        SignatureWriter writer = new SignatureWriter() {
            private boolean superclass;
            @Override public SignatureVisitor visitSuperclass() {
                superclass = true;
                return super.visitSuperclass();
            }
            @Override public void visitClassType(String name) {
                if (superclass) {
                    require(name.equals("java/lang/Object"), "unexpected superclass signature: " + name);
                    name = "java/lang/Record";
                    superclass = false;
                }
                super.visitClassType(name);
            }
        };
        new SignatureReader(signature).accept(writer);
        return writer.toString();
    }

    private static void addObjectMethod(ClassNode node, String name, String desc, int returnOpcode, Object[] args) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_FINAL, name, desc, null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        boolean equals = name.equals("equals");
        if (equals) method.visitVarInsn(ALOAD, 1);
        method.visitInvokeDynamicInsn(name, "(L" + node.name + ";" + desc.substring(1), BOOTSTRAP, args);
        method.visitInsn(returnOpcode);
        method.visitMaxs(equals ? 2 : 1, equals ? 2 : 1);
        method.visitEnd();
        node.methods.add(method);
    }

    private static boolean isObjectMethod(MethodNode method) {
        return (method.name.equals("equals") && method.desc.equals("(Ljava/lang/Object;)Z"))
                || (method.name.equals("hashCode") && method.desc.equals("()I"))
                || (method.name.equals("toString") && method.desc.equals("()Ljava/lang/String;"));
    }

    private static List<String> names(AnnotationNode marker, String key, boolean required) {
        if (marker.values != null) {
            for (int i = 0; i < marker.values.size(); i += 2) {
                if (!marker.values.get(i).equals(key)) continue;
                Object raw = marker.values.get(i + 1);
                require(raw instanceof List<?>, "invalid " + key);
                List<String> result = new ArrayList<>();
                for (Object value : (List<?>) raw) {
                    require(value instanceof String && !((String) value).isBlank(), "invalid field name in " + key);
                    result.add((String) value);
                }
                require(new HashSet<>(result).size() == result.size(), "duplicate field name in " + key);
                return result;
            }
        }
        require(!required, "missing " + key);
        return List.of();
    }

    private static void collect(List<AnnotationNode> annotations, List<AnnotationNode> markers) {
        if (annotations != null) for (AnnotationNode a : annotations) if (a.desc.equals(MARKER)) markers.add(a);
    }

    private static void remove(List<AnnotationNode> annotations) {
        if (annotations != null) annotations.removeIf(a -> a.desc.equals(MARKER));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
