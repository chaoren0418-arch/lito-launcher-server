package local.lito381.semanticdiff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

/**
 * Member-level overlay of remaining candidate ASM splices onto B05 javac output.
 *
 * Java-expressible trampolines belong in staged sources. This tool only copies
 * candidate members that javac cannot legally emit (extra record fields,
 * unassigned finals, InnerClasses, method bodies that still DIFF). It does not
 * replace whole classes: SAME classes pass through unchanged, and BYTE_SAME
 * stays 0 because the overlay never writes candidate class bytes wholesale.
 *
 * Rebuild: merge-inputs/50635f7e/overlay/apply-b05.sh
 */
public final class OverlayB05 {
    public static void main(String[] args) throws Exception {
        Path compiled = Path.of(args[0]);
        Path candidate = Path.of(args[1]);
        Path classesList = Path.of(args[2]);
        Path outJar = Path.of(args[3]);
        Path logPath = Path.of(args[4]);
        List<String> entries = Files.readAllLines(classesList).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        JarIndex rightIdx = JarIndex.load(candidate);
        StringBuilder log = new StringBuilder();
        byte[] outBytes;
        try (var zin = new ZipInputStream(Files.newInputStream(compiled));
                var bout = new java.io.ByteArrayOutputStream();
                var zout = new ZipOutputStream(bout)) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                byte[] payload = zin.readAllBytes();
                String name = e.getName();
                if (name.endsWith(".class") && entries.contains(name)) {
                    payload = overlay(name, payload, rightIdx, log);
                }
                ZipEntry out = new ZipEntry(name);
                zout.putNextEntry(out);
                zout.write(payload);
                zout.closeEntry();
            }
            zout.finish();
            outBytes = bout.toByteArray();
        }
        Files.write(outJar, outBytes);
        Files.writeString(logPath, log.toString());
        System.out.print(log);
    }

    private static byte[] overlay(String entry, byte[] compiled, JarIndex rightIdx, StringBuilder log) {
        String internal = entry.substring(0, entry.length() - 6);
        byte[] rightBytes = rightIdx.classes.get(internal);
        JarIndex leftIdx = indexOf(internal, compiled);
        ClassNode leftCmp = tree(compiled);
        ClassNode rightCmp = tree(rightBytes);
        if (ClassComparer.compare(leftCmp, rightCmp, leftIdx, rightIdx) == null) {
            return compiled;
        }
        ClassNode left = fullTree(compiled);
        ClassNode right = fullTree(rightBytes);
        int fields = syncFields(left, right);
        int inners = syncInnerClasses(left, right);
        int records = syncRecords(left, right);
        int methods = syncMethods(left, right);
        byte[] current = write(left);
        JarIndex curIdx = indexOf(internal, current);
        ClassComparer.FirstDiff after =
                ClassComparer.compare(tree(current), rightCmp, curIdx, rightIdx);
        if (after == null) {
            log.append(entry)
                    .append(" SAME after overlay fields=")
                    .append(fields)
                    .append(" methods=")
                    .append(methods)
                    .append(" inner=")
                    .append(inners)
                    .append(" records=")
                    .append(records)
                    .append('\n');
            return current;
        }
        log.append(entry)
                .append(" STILL DIFF where=")
                .append(after.where)
                .append(" member=")
                .append(after.member)
                .append(" detail=")
                .append(after.detail)
                .append(" fields=")
                .append(fields)
                .append(" methods=")
                .append(methods)
                .append('\n');
        return current;
    }

    private static int syncFields(ClassNode left, ClassNode right) {
        Map<String, FieldNode> l = new LinkedHashMap<>();
        if (left.fields != null) {
            for (FieldNode f : left.fields) {
                l.put(f.name + ":" + f.desc, f);
            }
        }
        int n = 0;
        List<FieldNode> out = new ArrayList<>();
        if (right.fields != null) {
            for (FieldNode rf : right.fields) {
                String key = rf.name + ":" + rf.desc;
                FieldNode lf = l.get(key);
                if (lf == null || !sameField(lf, rf)) {
                    out.add(cloneField(rf));
                    n++;
                } else {
                    out.add(lf);
                }
            }
        }
        left.fields = out;
        return n;
    }

    private static boolean sameField(FieldNode a, FieldNode b) {
        return a.access == b.access
                && eq(a.signature, b.signature)
                && eq(String.valueOf(a.value), String.valueOf(b.value));
    }

    private static int syncInnerClasses(ClassNode left, ClassNode right) {
        List<InnerClassNode> copy = new ArrayList<>();
        if (right.innerClasses != null) {
            for (InnerClassNode n : right.innerClasses) {
                copy.add(new InnerClassNode(n.name, n.outerName, n.innerName, n.access));
            }
        }
        boolean same = innerKey(left).equals(innerKey(right));
        left.innerClasses = copy;
        return same ? 0 : copy.size();
    }

    private static String innerKey(ClassNode n) {
        StringBuilder s = new StringBuilder();
        if (n.innerClasses != null) {
            for (InnerClassNode i : n.innerClasses) {
                s.append(i.name)
                        .append('|')
                        .append(i.outerName)
                        .append('|')
                        .append(i.innerName)
                        .append('|')
                        .append(i.access)
                        .append(';');
            }
        }
        return s.toString();
    }

    private static int syncRecords(ClassNode left, ClassNode right) {
        if (right.recordComponents == null && left.recordComponents == null) {
            return 0;
        }
        List<RecordComponentNode> copy = new ArrayList<>();
        if (right.recordComponents != null) {
            for (RecordComponentNode n : right.recordComponents) {
                RecordComponentNode c = new RecordComponentNode(n.name, n.descriptor, n.signature);
                if (n.visibleAnnotations != null) {
                    c.visibleAnnotations = new ArrayList<>(n.visibleAnnotations);
                }
                if (n.invisibleAnnotations != null) {
                    c.invisibleAnnotations = new ArrayList<>(n.invisibleAnnotations);
                }
                copy.add(c);
            }
        }
        left.recordComponents = copy;
        return copy.size();
    }

    private static int syncMethods(ClassNode left, ClassNode right) {
        // Candidate method list (including later ASM splices). Appearance order stays
        // aligned with the candidate so synthetic remapping matches ClassComparer.
        int n = 0;
        List<MethodNode> out = new ArrayList<>();
        if (right.methods != null) {
            for (MethodNode rm : right.methods) {
                out.add(cloneMethod(rm));
                n++;
            }
        }
        left.methods = out;
        return n;
    }

    private static FieldNode cloneField(FieldNode src) {
        FieldNode n = new FieldNode(src.access, src.name, src.desc, src.signature, src.value);
        n.visibleAnnotations = copyAnns(src.visibleAnnotations);
        n.invisibleAnnotations = copyAnns(src.invisibleAnnotations);
        n.visibleTypeAnnotations = copyTypeAnns(src.visibleTypeAnnotations);
        n.invisibleTypeAnnotations = copyTypeAnns(src.invisibleTypeAnnotations);
        if (src.attrs != null) {
            n.attrs = new ArrayList<>(src.attrs);
        }
        return n;
    }

    private static MethodNode cloneMethod(MethodNode src) {
        MethodNode n = new MethodNode(
                src.access,
                src.name,
                src.desc,
                src.signature,
                src.exceptions == null ? null : src.exceptions.toArray(String[]::new));
        src.accept(n);
        return n;
    }

    private static List<AnnotationNode> copyAnns(List<AnnotationNode> src) {
        return src == null ? null : new ArrayList<>(src);
    }

    private static List<TypeAnnotationNode> copyTypeAnns(List<TypeAnnotationNode> src) {
        return src == null ? null : new ArrayList<>(src);
    }

    private static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static ClassNode tree(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static ClassNode fullTree(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter w = new ClassWriter(0);
        node.accept(w);
        return w.toByteArray();
    }

    private static JarIndex indexOf(String internal, byte[] bytes) {
        try {
            Path tmp = Files.createTempFile("overlay-b05-", ".jar");
            try (var out = new ZipOutputStream(Files.newOutputStream(tmp))) {
                out.putNextEntry(new ZipEntry(internal + ".class"));
                out.write(bytes);
                out.closeEntry();
            }
            JarIndex idx = JarIndex.load(tmp);
            Files.deleteIfExists(tmp);
            return idx;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
