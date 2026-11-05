package local.lito381.semanticdiff;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
    private Map<String, Set<String>> exactMethods = Map.of();
    private final Map<String, ClassNode> trees = new TreeMap<>();
    private final Map<String, String> lambdaIds = new TreeMap<>();
    final Map<String,String> callsiteIds = new TreeMap<>();
    void clearLambdaIds() { lambdaIds.clear(); }
    boolean exact(String owner,MethodNode m) { return exactMethods.getOrDefault(owner,Set.of()).contains(m.name+m.desc); }
    private final java.util.Set<String> resolving = new java.util.HashSet<>();
    private final Map<String, Map<String, String>> switchFields = new TreeMap<>();
    final Map<MethodNode, String> methodOwners = new java.util.IdentityHashMap<>();
    final Map<MethodNode, MethodNode> beforeWebs = new java.util.IdentityHashMap<>();
    private final Map<String, byte[]> dependencies = new TreeMap<>();
    private final Map<String, ClassNode> headers = new TreeMap<>();
    private Set<String> otherReferences;
    Map<String, Set<String>> accessWiden = Map.of(), layouts = Map.of(), deviations = Map.of(), ctorOrders = Map.of();
    Map<String, Set<String>> exceptionsExempt = Map.of();
    Map<String, Set<String>> extraMethods = Map.of();
    Map<String, Set<String>> methodDiffs = Map.of();
    Set<String> innerAccessWiden = Set.of();
    record MethodResult(String status, String detail) {}
    final Map<String, Map<String, MethodResult>> methodResults = new TreeMap<>();
    void result(MethodNode m, String status, String detail) {
        methodResults.computeIfAbsent(methodOwners.get(m), k -> new TreeMap<>())
                .put(m.name + m.desc, new MethodResult(status, detail));
    }
    private Set<String> allMethodReferences, ldcStrings;
    private final Set<String> unresolvedMethodReferences = new java.util.HashSet<>();
    private final Map<String, Set<String>> orphanReferenceOwners = new TreeMap<>();
    private final Set<String> incompleteReferenceOwners = new java.util.HashSet<>();

    boolean orphan(MethodNode m) {
        int required = org.objectweb.asm.Opcodes.ACC_PRIVATE | org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
        if ((m.access & required) != required || m.name.startsWith("<")) return false;
        if (allMethodReferences == null) {
            allMethodReferences = new java.util.HashSet<>(); ldcStrings = new java.util.HashSet<>();
            for (byte[] bytes : classes.values()) {
                ClassNode c = new ClassNode();
                new ClassReader(bytes).accept(c, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (MethodNode method : c.methods) for (var n : method.instructions) {
                    if (n instanceof org.objectweb.asm.tree.MethodInsnNode call)
                        orphanMethodReference(call.owner, call.name, call.desc);
                    if (n instanceof org.objectweb.asm.tree.LdcInsnNode ldc) orphanReference(ldc.cst);
                    if (n instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode indy) {
                        orphanReference(indy.bsm);
                        for (Object arg : indy.bsmArgs) orphanReference(arg);
                    }
                }
            }
        }
        return !allMethodReferences.contains(methodOwners.get(m) + "#" + m.name + m.desc)
                && !unresolvedMethodReferences.contains(m.name + m.desc)
                && !ldcStrings.contains(m.name);
    }

    /**
     * Over-approximate method resolution, using original class headers rather than
     * normalized methods. A symbolic owner can name a subclass of the declaration,
     * even for a private static method called from its declaring class.
     * Retain every ancestor/interface candidate (including shadowed declarations):
     * false negatives for orphan removal are safer than a missed JVM reference.
     * If any hierarchy metadata is missing, no matching signature is proven orphan.
     */
    private void orphanMethodReference(String owner, String name, String desc) {
        Set<String> owners = orphanReferenceOwners.get(owner);
        if (owners == null) {
            owners = new java.util.HashSet<>();
            var pending = new java.util.ArrayDeque<String>();
            pending.add(owner);
            while (!pending.isEmpty()) {
                String current = pending.removeFirst();
                if (!owners.add(current)) continue;
                ClassNode c = header(current);
                if (c == null) {
                    incompleteReferenceOwners.add(owner);
                    continue;
                }
                if (c.superName != null) pending.add(c.superName);
                pending.addAll(c.interfaces);
            }
            orphanReferenceOwners.put(owner, owners);
        }
        for (String candidate : owners) allMethodReferences.add(candidate + "#" + name + desc);
        if (incompleteReferenceOwners.contains(owner)) unresolvedMethodReferences.add(name + desc);
    }

    private void orphanReference(Object v) {
        if (v instanceof String s) ldcStrings.add(s);
        if (v instanceof org.objectweb.asm.Handle h && h.getTag() >= org.objectweb.asm.Opcodes.H_INVOKEVIRTUAL)
            orphanMethodReference(h.getOwner(), h.getName(), h.getDesc());
        if (v instanceof org.objectweb.asm.ConstantDynamic c) {
            orphanReference(c.getBootstrapMethod());
            for(int i=0;i<c.getBootstrapMethodArgumentCount();i++)orphanReference(c.getBootstrapMethodArgument(i));
        }
    }

    Integer fieldAccess(org.objectweb.asm.tree.FieldInsnNode f) {
        String owner = resolveField(f.owner,f.name,f.desc,f.getOpcode(),new java.util.HashSet<>());
        ClassNode c = owner == null ? null : header(owner);
        if(c != null)for(var field:c.fields)if(field.name.equals(f.name)&&field.desc.equals(f.desc))return field.access;
        return null;
    }

    boolean approved(Map<String, Set<String>> list, MethodNode method) {
        return list.getOrDefault(methodOwners.get(method), Set.of()).contains(method.name + method.desc);
    }

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
            index.syntheticMethods.put(e.getKey(), syntheticMap(e.getValue(), Set.of()));
            ClassNode node = new ClassNode();
            new ClassReader(e.getValue()).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            Map<String, String> fields = new LinkedHashMap<>();
            for (var f : node.fields) if (f.name.startsWith("$SwitchMap$")
                    && f.desc.equals("[I"))
                fields.put(f.name, "$SwitchMap$#" + fields.size());
            index.switchFields.put(node.name, fields);
        }
        return index;
    }

    /** Approval identities must not consume or match a synthetic ordinal on any side. */
    void preserveExactMethods(Map<String, Set<String>> methods) {
        exactMethods = methods;
        lambdaIds.clear();
        for (var entry : methods.entrySet()) {
            byte[] bytes = classes.get(entry.getKey());
            if (bytes != null) syntheticMethods.put(entry.getKey(), syntheticMap(bytes, entry.getValue()));
        }
    }

    String remapMethod(String owner, String name, String desc) {
        if (exactMethods.getOrDefault(owner, Set.of()).contains(name + desc)) return name;
        String paired=callsiteIds.get(owner+"#"+name+desc);
        if(paired!=null)return paired;
        if (name.startsWith("lambda$") && syntheticPrefix(name) != null && classes.containsKey(owner)) {
            String key = owner + "#" + name + desc;
            if (lambdaIds.containsKey(key)) return lambdaIds.get(key);
            ClassNode node = readTree(owner);
            // ASM and decompiled Java may omit ACC_SYNTHETIC; that flag is already ignored for methods.
            MethodNode method = node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc))
                    .findFirst().orElse(null);
            if (method != null) {
                // Recursive synthetic graphs are not emitted by javac. Fail closed for such input.
                if (!resolving.add(key)) throw new IllegalArgumentException("recursive lambda body: " + key);
                try {
                    String body = ClassComparer.methodIdentity(method, this);
                    String id = "lambda$content$" + java.util.HexFormat.of().formatHex(
                            java.security.MessageDigest.getInstance("SHA-256").digest(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    lambdaIds.put(key, id);
                    return id;
                } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
                finally { resolving.remove(key); }
            }
        }
        Map<String, String> map = syntheticMethods.get(owner);
        if (map == null) {
            return name;
        }
        return name.startsWith("lambda$") ? name : map.getOrDefault(name, name);
    }

    String remapField(String owner, String name, String desc) {
        return desc.equals("[I") ? switchFields.getOrDefault(owner, Map.of()).getOrDefault(name, name) : name;
    }

    ClassNode readTree(String internalName) {
        if (trees.containsKey(internalName)) return trees.get(internalName);
        byte[] bytes = classes.get(internalName);
        if (bytes == null) {
            return null;
        }
        ClassNode node = new ClassNode();
        // SKIP_DEBUG drops LineNumberTable / LocalVariable(Type)Table / SourceFile /
        // SourceDebugExtension / MethodParameters. SKIP_FRAMES drops StackMap (not compared).
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Normalizations.prepare(node);
        for (MethodNode m : node.methods) NonNullChecks.normalize(node.name, m);
        FourthNormalizations.prepare(node, this);
        for (MethodNode m : node.methods) methodOwners.put(m, node.name);
        trees.put(internalName, node);
        return node;
    }

    void addClasspath(Path path) throws IOException {
        if (java.nio.file.Files.isDirectory(path)) {
            try (var files = java.nio.file.Files.walk(path)) {
                for (Path p : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(p);
                    dependencies.putIfAbsent(new ClassReader(bytes).getClassName(), bytes);
                }
            }
        } else JarIndex.load(path).classes.forEach(dependencies::putIfAbsent);
    }

    boolean physicalConstructor(String owner, MethodNode m) {
        return m.name.equals("<init>") && exactMethods.getOrDefault(owner,Set.of()).contains(m.name+m.desc);
    }

    ClassNode header(String name) {
        if (headers.containsKey(name)) return headers.get(name);
        byte[] bytes = classes.getOrDefault(name, dependencies.get(name));
        if (bytes == null && name.startsWith("java/")) {
            // Read platform class metadata without loading/initializing application classes.
            try (InputStream in = ClassLoader.getPlatformClassLoader().getResourceAsStream(name + ".class")) {
                if (in != null) bytes = readAll(in);
            } catch (IOException ex) { return null; }
        }
        if (bytes == null) return null;
        ClassNode c = new ClassNode();
        new ClassReader(bytes).accept(c, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        headers.put(name,c); return c;
    }

    String fieldOwner(String owner, String name, String desc, int opcode) {
        if (!FourthNormalizations.enabled("fields")) return owner;
        String result = resolveField(owner,name,desc,opcode,new java.util.HashSet<>());
        // Unresolved symbolic owners must not accidentally equal the declaration
        // obtained by a successful lookup on the other side.
        return result == null || result.isEmpty() ? "unresolved(" + owner + ")" : result;
    }

    // JVMS field lookup: declaration, direct superinterfaces, then superclass.
    // null means unresolved; empty means the complete hierarchy has no such field.
    private String resolveField(String owner, String name, String desc, int opcode, Set<String> seen) {
        if (owner == null || owner.equals("java/lang/Object")) return "";
        if (!seen.add(owner)) return null;
        ClassNode c = header(owner);
        if (c == null) return null;
        for (var f : c.fields) if (f.name.equals(name) && f.desc.equals(desc)) {
            boolean stat = (f.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0;
            return stat == (opcode == org.objectweb.asm.Opcodes.GETSTATIC || opcode == org.objectweb.asm.Opcodes.PUTSTATIC) ? c.name : null;
        }
        for (String itf : c.interfaces) {
            String r = resolveField(itf,name,desc,opcode,new java.util.HashSet<>(seen));
            if (r == null || !r.isEmpty()) return r;
        }
        return resolveField(c.superName,name,desc,opcode,seen);
    }

    boolean otherReference(String owner, MethodNode m) {
        if (exactMethods.getOrDefault(owner, Set.of()).contains(m.name + m.desc)) return true;
        if (otherReferences == null) {
            otherReferences = new java.util.HashSet<>();
            Map<String,byte[]> all = new TreeMap<>(dependencies); all.putAll(classes);
            for (byte[] bytes : all.values()) {
                ClassNode c = new ClassNode();
                new ClassReader(bytes).accept(c,ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (MethodNode method : c.methods) for (var n : method.instructions) {
                    if (n instanceof org.objectweb.asm.tree.MethodInsnNode call)
                        otherReferences.add(call.owner + "#" + call.name + call.desc);
                    if (n instanceof org.objectweb.asm.tree.LdcInsnNode ldc) reference(ldc.cst);
                    if (n instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode indy) {
                        reference(indy.bsm);
                        for (int i=0; i<indy.bsmArgs.length; i++) {
                            Object arg = indy.bsmArgs[i];
                            if (i == 1 && FourthNormalizations.lambda(indy) && arg instanceof org.objectweb.asm.Handle h
                                    && h.getTag() == org.objectweb.asm.Opcodes.H_INVOKESTATIC && h.getOwner().equals(c.name)) continue;
                            reference(arg);
                        }
                    }
                }
            }
        }
        return otherReferences.contains(owner + "#" + m.name + m.desc);
    }
    private void reference(Object value) {
        if (value instanceof org.objectweb.asm.Handle h) otherReferences.add(h.getOwner()+"#"+h.getName()+h.getDesc());
        if (value instanceof org.objectweb.asm.ConstantDynamic c) {
            reference(c.getBootstrapMethod());
            for (int i=0;i<c.getBootstrapMethodArgumentCount();i++) reference(c.getBootstrapMethodArgument(i));
        }
    }

    private static Map<String, String> syntheticMap(byte[] bytes, Set<String> exactMethods) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(
                node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Map<String, Integer> counters = new LinkedHashMap<>();
        Map<String, String> map = new LinkedHashMap<>();
        if (node.methods == null) {
            return Collections.emptyMap();
        }
        for (MethodNode method : node.methods) {
            if (exactMethods.contains(method.name + method.desc)) continue;
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
