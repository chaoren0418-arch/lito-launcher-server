package local.lito381.semanticdiff;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.tree.ClassNode;

/**
 * CLI: semantic-diff --left A.jar --right B.jar
 * [--classes spec] [--exclude spec] [--allow-methods file --allow-right C.jar]
 * [--json out.json] [--md out.md]
 */
public final class SemanticDiff {
    enum Status {
        SAME,
        DIFF,
        ONLY_LEFT,
        ONLY_RIGHT
    }

    static final class ClassRow {
        final String name;
        final Status status;
        final ClassComparer.FirstDiff firstDiff;
        final boolean byteSame;
        final boolean allowMethodsApplied;

        ClassRow(
                String name,
                Status status,
                ClassComparer.FirstDiff firstDiff,
                boolean byteSame,
                boolean allowMethodsApplied) {
            this.name = name;
            this.status = status;
            this.firstDiff = firstDiff;
            this.byteSame = byteSame;
            this.allowMethodsApplied = allowMethodsApplied;
        }
    }

    public static void main(String[] args) {
        try {
            int code = run(args);
            System.exit(code);
        } catch (Exception e) {
            System.err.println("semantic-diff: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    static int run(String[] args) throws IOException {
        Args parsed = Args.parse(args);
        JarIndex left = JarIndex.load(parsed.left);
        JarIndex right = JarIndex.load(parsed.right);
        JarIndex allow = parsed.allowRight == null ? null : JarIndex.load(parsed.allowRight);

        Set<String> universe = new TreeSet<>();
        universe.addAll(left.classes.keySet());
        universe.addAll(right.classes.keySet());

        if (parsed.classes != null) {
            Set<String> missing = new TreeSet<>();
            for (String wanted : parsed.classes) {
                if (!universe.contains(wanted)) {
                    missing.add(wanted);
                }
            }
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException(
                        "missing --classes entries: " + String.join(",", missing));
            }
            universe.retainAll(parsed.classes);
        }

        Set<String> names = new TreeSet<>();
        for (String name : universe) {
            if (!excluded(name, parsed.exclude)) {
                names.add(name);
            }
        }
        Map<String, Set<String>> validatedAllow = validateAllowMethods(parsed, names, allow);

        List<ClassRow> rows = new ArrayList<>();
        EnumMap<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            counts.put(s, 0);
        }
        int byteSameCount = 0;
        int allowApplied = 0;
        for (String name : names) {
            boolean inLeft = left.classes.containsKey(name);
            boolean inRight = right.classes.containsKey(name);
            boolean bytesEqual = inLeft && inRight
                    && Arrays.equals(left.classes.get(name), right.classes.get(name));
            if (bytesEqual) {
                byteSameCount++;
            }
            Set<String> allowedKeys = validatedAllow.getOrDefault(name, Set.of());
            boolean allowThis = !allowedKeys.isEmpty() && allow != null;
            if (allowThis) {
                allowApplied++;
            }
            ClassRow row;
            if (inLeft && !inRight) {
                row = new ClassRow(name, Status.ONLY_LEFT, null, false, allowThis);
            } else if (!inLeft && inRight) {
                row = new ClassRow(name, Status.ONLY_RIGHT, null, false, allowThis);
            } else {
                ClassNode ln = left.readTree(name);
                ClassNode rn = right.readTree(name);
                ClassNode an = allowThis ? allow.readTree(name) : null;
                ClassComparer.FirstDiff diff = ClassComparer.compare(
                        ln, rn, an, allowedKeys, left, right, allowThis ? allow : null);
                row = diff == null
                        ? new ClassRow(name, Status.SAME, null, bytesEqual, allowThis)
                        : new ClassRow(name, Status.DIFF, diff, bytesEqual, allowThis);
            }
            rows.add(row);
            counts.put(row.status, counts.get(row.status) + 1);
        }
        String summary = "SUMMARY SAME=" + counts.get(Status.SAME)
                + " DIFF=" + counts.get(Status.DIFF)
                + " ONLY_LEFT=" + counts.get(Status.ONLY_LEFT)
                + " ONLY_RIGHT=" + counts.get(Status.ONLY_RIGHT)
                + " BYTE_SAME=" + byteSameCount
                + " ALLOW_METHODS=" + allowApplied;
        System.out.println(summary);
        if (parsed.json != null) {
            writeJson(parsed, left, right, rows, counts, byteSameCount);
            System.out.println("wrote " + parsed.json);
        }
        if (parsed.md != null) {
            writeMd(parsed, left, right, rows, counts, byteSameCount);
            System.out.println("wrote " + parsed.md);
        }
        return 0;
    }

    /** Resolve exact allow-right names before producing any successful report. */
    private static Map<String, Set<String>> validateAllowMethods(
            Args parsed, Set<String> names, JarIndex allow) {
        Map<String, Set<String>> resolved = new LinkedHashMap<>();
        if (parsed.allowMethods == null) {
            return resolved;
        }
        for (var entry : parsed.allowMethods.entrySet()) {
            String owner = entry.getKey();
            if (!names.contains(owner)) {
                throw new IllegalArgumentException(
                        "--allow-methods class outside comparison scope: " + owner);
            }
            ClassNode node = allow.readTree(owner);
            for (String wanted : entry.getValue()) {
                var method = node == null ? null : node.methods.stream()
                        .filter(m -> (m.name + m.desc).equals(wanted)).findFirst().orElse(null);
                if (method == null) {
                    throw new IllegalArgumentException(
                            "unmatched --allow-methods entry in --allow-right: " + owner + "#" + wanted);
                }
                resolved.computeIfAbsent(owner, k -> new LinkedHashSet<>())
                        .add(allow.remapMethod(owner, method.name) + method.desc);
            }
        }
        return resolved;
    }

    static boolean excluded(String className, Set<String> exclude) {
        if (exclude == null || exclude.isEmpty()) {
            return false;
        }
        if (exclude.contains(className)) {
            return true;
        }
        for (String outer : exclude) {
            if (className.startsWith(outer + "$")) {
                return true;
            }
        }
        return false;
    }

    private static void writeJson(
            Args parsed,
            JarIndex left,
            JarIndex right,
            List<ClassRow> rows,
            Map<Status, Integer> counts,
            int byteSameCount)
            throws IOException {
        Path out = parsed.json;
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            w.println("{");
            w.println("  \"tool\": \"semantic-diff\",");
            w.println("  \"generatedAt\": " + json(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()) + ",");
            w.println("  \"left\": " + json(parsed.left.toString()) + ",");
            w.println("  \"right\": " + json(parsed.right.toString()) + ",");
            w.println("  \"allowRight\": "
                    + (parsed.allowRight == null ? "null" : json(parsed.allowRight.toString())) + ",");
            w.println("  \"leftClassCount\": " + left.classes.size() + ",");
            w.println("  \"rightClassCount\": " + right.classes.size() + ",");
            w.println("  \"comparedClassCount\": " + rows.size() + ",");
            w.println("  \"byteSameCount\": " + byteSameCount + ",");
            w.println("  \"counts\": {");
            w.println("    \"SAME\": " + counts.get(Status.SAME) + ",");
            w.println("    \"DIFF\": " + counts.get(Status.DIFF) + ",");
            w.println("    \"ONLY_LEFT\": " + counts.get(Status.ONLY_LEFT) + ",");
            w.println("    \"ONLY_RIGHT\": " + counts.get(Status.ONLY_RIGHT) + ",");
            w.println("    \"BYTE_SAME\": " + byteSameCount);
            w.println("  },");
            w.println("  \"classes\": [");
            for (int i = 0; i < rows.size(); i++) {
                ClassRow row = rows.get(i);
                w.print("    {\"name\": " + json(row.name)
                        + ", \"status\": " + json(row.status.name())
                        + ", \"byteSame\": " + row.byteSame
                        + ", \"allowMethodsApplied\": " + row.allowMethodsApplied);
                if (row.firstDiff != null) {
                    w.print(", \"firstDiff\": {");
                    w.print("\"where\": " + json(row.firstDiff.where));
                    w.print(", \"member\": " + json(row.firstDiff.member));
                    w.print(", \"insnIndex\": "
                            + (row.firstDiff.insnIndex == null ? "null" : row.firstDiff.insnIndex.toString()));
                    w.print(", \"detail\": " + json(row.firstDiff.detail));
                    w.print("}");
                }
                w.print("}");
                if (i + 1 < rows.size()) {
                    w.print(',');
                }
                w.println();
            }
            w.println("  ]");
            w.println("}");
        }
    }

    private static void writeMd(
            Args parsed,
            JarIndex left,
            JarIndex right,
            List<ClassRow> rows,
            Map<Status, Integer> counts,
            int byteSameCount)
            throws IOException {
        Path out = parsed.md;
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        int same = counts.get(Status.SAME);
        int diff = counts.get(Status.DIFF);
        int intersection = same + diff;
        int bytesDiffSemanticSame = Math.max(0, same - byteSameCount);
        List<String> lines = new ArrayList<>();
        lines.add("# 伺服器語意缺口（T020d / Issue #44）");
        lines.add("");
        lines.add("以 ASM tree 正規化後比對兩個 JAR 的 `.class`。");
        lines.add("此檔由 `lito伺服器/LiTo381-Server/tools/semantic-diff` 產生。");
        lines.add("");
        lines.add("## 比對基準");
        lines.add("");
        lines.add("| 項目 | 值 |");
        lines.add("|---|---|");
        lines.add("| left | `" + mdEscape(parsed.left.toString()) + "`（" + left.classes.size() + " 類） |");
        lines.add("| right | `" + mdEscape(parsed.right.toString()) + "`（" + right.classes.size() + " 類） |");
        if (parsed.allowRight != null) {
            lines.add("| allow-right | `" + mdEscape(parsed.allowRight.toString()) + "` |");
        }
        lines.add("| 產生時間 | " + Instant.now().truncatedTo(ChronoUnit.SECONDS) + " |");
        lines.add("| 正規化 | 移除 LineNumberTable／LocalVariableTable／LocalVariableTypeTable／SourceFile／SourceDebugExtension；不比較 max_stack／max_locals／常數池順序；`lambda$xxx$N`、`access$NNN` 依出現順序正規化 |");
        lines.add("| 比較項 | 類別存取旗標、父類別、介面、欄位（名稱／描述／旗標／常數值）、方法（名稱／描述／旗標／例外／指令序列含解析後運算元、try-catch、標籤序號）、註解（含 annotationDefault 結構化值、指令／區域變數／record 型別註解）、內部類別屬性 |");
        lines.add("");
        lines.add("## 摘要");
        lines.add("");
        lines.add("| 狀態 | 數量 | 含義 |");
        lines.add("|---|---:|---|");
        lines.add("| SAME | " + same + " | 兩邊都有，正規化後語意相同 |");
        lines.add("| DIFF | " + diff + " | 兩邊都有，正規化後仍不同 |");
        lines.add("| ONLY_LEFT | " + counts.get(Status.ONLY_LEFT) + " | 只在 left |");
        lines.add("| ONLY_RIGHT | " + counts.get(Status.ONLY_RIGHT) + " | 只在 right |");
        lines.add("| 交集 SAME+DIFF | " + intersection + " | 兩邊都有的類別 |");
        lines.add("| BYTE_SAME | " + byteSameCount + " | **同一交集**裡 class bytes 完全相同（本輪實測） |");
        lines.add("| SAME − BYTE_SAME | " + bytesDiffSemanticSame + " | 同一交集：bytes 不同但語意相同 |");
        lines.add("| 比較類別 | " + rows.size() + " | left∪right"
                + (parsed.classes != null ? "（套用 --classes）" : "")
                + (parsed.exclude != null ? "（套用 --exclude）" : "") + " |");
        lines.add("");
        lines.add("## 已驗證／推測／待驗證");
        lines.add("");
        lines.add("- **已驗證**：自我測試見 `tools/semantic-diff/selftest.sh`（含同一 JAR、`-g` vs `-g:none`、改常數、`--exclude`、`--allow-methods`、`--classes` 副檔名／清單／不存在類別、註解 self-vs-self 與只改型別註解）。BYTE_SAME／SAME／DIFF 由本工具對上表 left／right **同一輪**量出。");
        lines.add("- **推測**：DIFF 多半是 javac／反編譯來源／後來手改造成的指令或結構差。ONLY_* 與 `docs/server-source-gap.md` 的類別清單差大致對得上。");
        lines.add("- **待驗證**：語意相同 ≠ 執行行為已對過回歸；未跑 `1.啟動伺服器.bat --check`／`--smoke`，未連正式 DB，未啟動遊戲。");
        lines.add("");
        int methodDiff = 0;
        int fieldDiff = 0;
        int classDiff = 0;
        for (ClassRow row : rows) {
            if (row.status != Status.DIFF || row.firstDiff == null) {
                continue;
            }
            switch (row.firstDiff.where) {
                case "method" -> methodDiff++;
                case "field" -> fieldDiff++;
                default -> classDiff++;
            }
        }
        lines.add("## 與 Issue #43 javap 粗估（約 430）的關係");
        lines.add("");
        lines.add("#43／T020a 的約 430 是**另一組測量**：JDK 21 編譯的 Maven 產物 vs 正式 JAR，用 `javap -c -p` 去掉行號、常數池編號與空白，**未**正規化 `lambda$`／`access$`。那次同時記 class bytes 全同 1,008。那些數字**不是**本表 left／right 的同一交集，不能從本輪 SAME 扣除 1,008，也不能把「本輪 DIFF − 430」逐類歸因到 innerClasses 等屬性。");
        lines.add("");
        lines.add("本表只報告**這一對 JAR、這一輪交集**的實測：");
        lines.add("");
        lines.add("- 交集 SAME+DIFF = **" + intersection + "**。");
        lines.add("- 其中 BYTE_SAME = **" + byteSameCount + "**（本輪 bytes 全同）。");
        lines.add("- SAME = **" + same + "**，故 bytes 不同但語意相同 = **" + bytesDiffSemanticSame + "**（本輪實測，含 debug／合成名／常數池編號等）。");
        lines.add("- DIFF = **" + diff + "**（第一處差異：method " + methodDiff + "、field " + fieldDiff
                + "、class 屬性 " + classDiff + "）。這是本工具先看到的位置，不是把與 430 的差拆開的因果。");
        lines.add("- 430 與本輪 DIFF 的數值差屬**推測／不可直接相減**：編譯器（JDK 21 vs 本輪 javac）、正規化深度、類別集合都不同。");
        lines.add("");
        lines.add("## DIFF 類別清單");
        lines.add("");
        List<ClassRow> diffs = new ArrayList<>();
        List<ClassRow> onlyLeft = new ArrayList<>();
        List<ClassRow> onlyRight = new ArrayList<>();
        for (ClassRow row : rows) {
            if (row.status == Status.DIFF) {
                diffs.add(row);
            } else if (row.status == Status.ONLY_LEFT) {
                onlyLeft.add(row);
            } else if (row.status == Status.ONLY_RIGHT) {
                onlyRight.add(row);
            }
        }
        if (diffs.isEmpty()) {
            lines.add("_（無）_");
            lines.add("");
        } else {
            lines.add("| 類別 | 第一處差異 |");
            lines.add("|---|---|");
            for (ClassRow row : diffs) {
                String detail = row.firstDiff == null ? "" : row.firstDiff.oneLine();
                lines.add("| `" + row.name + "` | " + mdCell(detail) + " |");
            }
            lines.add("");
        }
        lines.add("## ONLY_LEFT");
        lines.add("");
        lines.add("只在 left（" + onlyLeft.size() + "）。完整類別清單見 JSON artifact；與 `docs/server-source-gap.md` 的「只在 target」應對齊。");
        lines.add("");
        appendNames(lines, onlyLeft);
        lines.add("## ONLY_RIGHT");
        lines.add("");
        lines.add("只在 right（" + onlyRight.size() + "）。與 `docs/server-source-gap.md` 的「只在 reference」應對齊。");
        lines.add("");
        appendNames(lines, onlyRight);
        Files.write(out, lines, StandardCharsets.UTF_8);
    }

    private static void appendNames(List<String> lines, List<ClassRow> rows) {
        if (rows.isEmpty()) {
            lines.add("_（無）_");
            lines.add("");
            return;
        }
        lines.add("```");
        for (ClassRow row : rows) {
            lines.add(row.name);
        }
        lines.add("```");
        lines.add("");
    }

    private static String json(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String mdEscape(String s) {
        return s.replace("|", "\\|");
    }

    private static String mdCell(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("|", "\\|").replace("\n", " ");
    }

    /** Strip {@code .class} first, then dots → slashes. */
    static String normalizeClassName(String raw) {
        String s = raw.trim();
        if (s.length() >= 6 && s.regionMatches(true, s.length() - 6, ".class", 0, 6)) {
            s = s.substring(0, s.length() - 6);
        }
        s = s.replace('.', '/');
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

    static final class Args {
        Path left;
        Path right;
        Path json;
        Path md;
        Path allowRight;
        Set<String> classes;
        Set<String> exclude;
        Map<String, Set<String>> allowMethods;

        static Args parse(String[] args) throws IOException {
            Args out = new Args();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--left" -> out.left = Path.of(need(args, ++i, a));
                    case "--right" -> out.right = Path.of(need(args, ++i, a));
                    case "--json" -> out.json = Path.of(need(args, ++i, a));
                    case "--md" -> out.md = Path.of(need(args, ++i, a));
                    case "--classes" -> out.classes = parseNameList(need(args, ++i, a), "--classes");
                    case "--exclude" -> out.exclude = parseNameList(need(args, ++i, a), "--exclude");
                    case "--allow-methods" -> out.allowMethods = parseAllowMethods(Path.of(need(args, ++i, a)));
                    case "--allow-right" -> out.allowRight = Path.of(need(args, ++i, a));
                    case "--help", "-h" -> {
                        usage();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("unknown argument: " + a);
                }
            }
            if (out.left == null || out.right == null) {
                usage();
                throw new IllegalArgumentException("--left and --right are required");
            }
            if (!Files.isRegularFile(out.left)) {
                throw new IllegalArgumentException("left JAR not found: " + out.left);
            }
            if (!Files.isRegularFile(out.right)) {
                throw new IllegalArgumentException("right JAR not found: " + out.right);
            }
            if (out.allowMethods != null && out.allowRight == null) {
                throw new IllegalArgumentException("--allow-methods requires --allow-right");
            }
            if (out.allowRight != null && out.allowMethods == null) {
                throw new IllegalArgumentException("--allow-right requires --allow-methods");
            }
            if (out.allowRight != null && !Files.isRegularFile(out.allowRight)) {
                throw new IllegalArgumentException("allow-right JAR not found: " + out.allowRight);
            }
            return out;
        }

        private static String need(String[] args, int i, String flag) {
            if (i >= args.length) {
                throw new IllegalArgumentException(flag + " needs a value");
            }
            return args[i];
        }

        private static Set<String> parseNameList(String spec, String flag) throws IOException {
            Set<String> names = new LinkedHashSet<>();
            for (String line : specText(spec).split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                names.add(normalizeClassName(trimmed));
            }
            if (names.isEmpty()) {
                throw new IllegalArgumentException(flag + " produced an empty set");
            }
            return names;
        }

        private static Map<String, Set<String>> parseAllowMethods(Path file) throws IOException {
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("--allow-methods file not found: " + file);
            }
            Map<String, Set<String>> map = new LinkedHashMap<>();
            int n = 0;
            for (String line : Files.readString(file, StandardCharsets.UTF_8).split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int hash = trimmed.indexOf('#');
                if (hash <= 0 || hash == trimmed.length() - 1) {
                    throw new IllegalArgumentException(
                            "--allow-methods line must be class#nameDescriptor: " + trimmed);
                }
                String cls = normalizeClassName(trimmed.substring(0, hash));
                String method = trimmed.substring(hash + 1).trim();
                map.computeIfAbsent(cls, k -> new LinkedHashSet<>()).add(method);
                n++;
            }
            if (n == 0) {
                throw new IllegalArgumentException("--allow-methods produced an empty set");
            }
            return map;
        }

        private static String specText(String spec) throws IOException {
            Path path = Path.of(spec);
            if (Files.isRegularFile(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            return spec.replace(',', '\n');
        }

        private static void usage() {
            System.err.println(
                    "usage: semantic-diff --left A.jar --right B.jar"
                            + " [--classes file-or-list] [--exclude file-or-list]"
                            + " [--allow-methods file --allow-right C.jar]"
                            + " [--json out.json] [--md out.md]");
        }
    }
}
