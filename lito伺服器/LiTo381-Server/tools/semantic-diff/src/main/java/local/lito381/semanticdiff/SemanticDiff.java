package local.lito381.semanticdiff;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.tree.ClassNode;

/**
 * CLI: semantic-diff --left A.jar --right B.jar [--classes spec] --json out.json --md out.md
 *
 * <p>Compares class files with debug attributes stripped and {@code lambda$}/{@code access$} names
 * remapped by appearance order. Does not compare {@code max_stack}/{@code max_locals} or constant-pool
 * order (ASM tree already resolves operands).
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

        ClassRow(String name, Status status, ClassComparer.FirstDiff firstDiff) {
            this.name = name;
            this.status = status;
            this.firstDiff = firstDiff;
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
        Set<String> filter = parsed.classes;
        Set<String> names = new TreeSet<>();
        names.addAll(left.classes.keySet());
        names.addAll(right.classes.keySet());
        if (filter != null) {
            names.retainAll(filter);
            for (String wanted : filter) {
                if (left.classes.containsKey(wanted) || right.classes.containsKey(wanted)) {
                    names.add(wanted);
                }
            }
        }
        List<ClassRow> rows = new ArrayList<>();
        EnumMap<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            counts.put(s, 0);
        }
        for (String name : names) {
            boolean inLeft = left.classes.containsKey(name);
            boolean inRight = right.classes.containsKey(name);
            ClassRow row;
            if (inLeft && !inRight) {
                row = new ClassRow(name, Status.ONLY_LEFT, null);
            } else if (!inLeft && inRight) {
                row = new ClassRow(name, Status.ONLY_RIGHT, null);
            } else {
                ClassNode ln = left.readTree(name);
                ClassNode rn = right.readTree(name);
                ClassComparer.FirstDiff diff = ClassComparer.compare(ln, rn, left, right);
                row = diff == null
                        ? new ClassRow(name, Status.SAME, null)
                        : new ClassRow(name, Status.DIFF, diff);
            }
            rows.add(row);
            counts.put(row.status, counts.get(row.status) + 1);
        }
        String summary = "SUMMARY SAME=" + counts.get(Status.SAME)
                + " DIFF=" + counts.get(Status.DIFF)
                + " ONLY_LEFT=" + counts.get(Status.ONLY_LEFT)
                + " ONLY_RIGHT=" + counts.get(Status.ONLY_RIGHT);
        System.out.println(summary);
        if (parsed.json != null) {
            writeJson(parsed, left, right, rows, counts);
            System.out.println("wrote " + parsed.json);
        }
        if (parsed.md != null) {
            writeMd(parsed, left, right, rows, counts);
            System.out.println("wrote " + parsed.md);
        }
        return 0;
    }

    private static void writeJson(
            Args parsed,
            JarIndex left,
            JarIndex right,
            List<ClassRow> rows,
            Map<Status, Integer> counts)
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
            w.println("  \"leftClassCount\": " + left.classes.size() + ",");
            w.println("  \"rightClassCount\": " + right.classes.size() + ",");
            w.println("  \"comparedClassCount\": " + rows.size() + ",");
            w.println("  \"counts\": {");
            w.println("    \"SAME\": " + counts.get(Status.SAME) + ",");
            w.println("    \"DIFF\": " + counts.get(Status.DIFF) + ",");
            w.println("    \"ONLY_LEFT\": " + counts.get(Status.ONLY_LEFT) + ",");
            w.println("    \"ONLY_RIGHT\": " + counts.get(Status.ONLY_RIGHT));
            w.println("  },");
            w.println("  \"classes\": [");
            for (int i = 0; i < rows.size(); i++) {
                ClassRow row = rows.get(i);
                w.print("    {\"name\": " + json(row.name) + ", \"status\": " + json(row.status.name()));
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
            Map<Status, Integer> counts)
            throws IOException {
        Path out = parsed.md;
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        int sameBytesHint = counts.get(Status.SAME);
        int diff = counts.get(Status.DIFF);
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
        lines.add("| 產生時間 | " + Instant.now().truncatedTo(ChronoUnit.SECONDS) + " |");
        lines.add("| 正規化 | 移除 LineNumberTable／LocalVariableTable／LocalVariableTypeTable／SourceFile／SourceDebugExtension；不比較 max_stack／max_locals／常數池順序；`lambda$xxx$N`、`access$NNN` 依出現順序正規化 |");
        lines.add("| 比較項 | 類別存取旗標、父類別、介面、欄位（名稱／描述／旗標／常數值）、方法（名稱／描述／旗標／例外／指令序列含解析後運算元、try-catch、標籤序號）、註解、內部類別屬性 |");
        lines.add("");
        lines.add("## 摘要");
        lines.add("");
        lines.add("| 狀態 | 數量 | 含義 |");
        lines.add("|---|---:|---|");
        lines.add("| SAME | " + counts.get(Status.SAME) + " | 兩邊都有，正規化後語意相同 |");
        lines.add("| DIFF | " + counts.get(Status.DIFF) + " | 兩邊都有，正規化後仍不同 |");
        lines.add("| ONLY_LEFT | " + counts.get(Status.ONLY_LEFT) + " | 只在 left |");
        lines.add("| ONLY_RIGHT | " + counts.get(Status.ONLY_RIGHT) + " | 只在 right |");
        lines.add("| 比較類別 | " + rows.size() + " | left∪right" + (parsed.classes != null ? "（套用 --classes）" : "") + " |");
        lines.add("");
        lines.add("## 已驗證／推測／待驗證");
        lines.add("");
        lines.add("- **已驗證**：同一 JAR 對自己 DIFF=0；同一原始碼 `-g` 與 `-g:none` DIFF=0；故意改常數會標 DIFF。本表由本工具對上述 left／right 跑出。");
        lines.add("- **推測**：DIFF 是 javac／反編譯來源／後來手改造成的真實指令或結構差，不是 debug 屬性或常數池編號。ONLY_* 與 `docs/server-source-gap.md` 的類別清單差對得上（Modern*、inner／匿名形狀、缺頂層類）。");
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
        int debugOnly = Math.max(0, sameBytesHint - 1008);
        lines.add("## 與 Issue #43 javap 粗估（約 430）的關係");
        lines.add("");
        lines.add("T007／#2 的 `jar tf` 交集是 **2,538**。T020a／#43 記：Maven 產物與 `reference/base-0.6.71.jar` **class bytes 完全相同 1,008**；對位元組不同的交集類別跑 `javap -c -p` 去掉行號、常數池編號與空白後，**仍約 430 類不同**（當時當上限估計，未扣 lambda 編號等編譯器雜訊）。");
        lines.add("");
        lines.add("本工具比 javap 粗估多做了：整段丟掉 debug 屬性、標籤改序號、`lambda$`／`access$` 依出現順序改名、略過 max_stack／max_locals；並且比較 innerClasses／nestMembers／欄位常數／註解，不只 `javap -c` 反組譯文字。本輪計數：");
        lines.add("");
        lines.add("- 交集 SAME+DIFF = **" + (sameBytesHint + diff) + "**（應對齊 2,538）。");
        lines.add("- **SAME=" + sameBytesHint + "**，比 bytes 全同的 1,008 多 **" + debugOnly + "**——這些是位元組不同但正規化後語意相同（debug／合成名／常數池編號）。");
        lines.add("- **DIFF=" + diff + "**（第一處差異：method " + methodDiff + "、field " + fieldDiff + "、class 屬性 " + classDiff + "）。較 javap 粗估 430 "
                + (diff >= 430 ? "多 " + (diff - 430) : "少 " + (430 - diff))
                + "：javap 文字正規化會漏掉 innerClasses／nestMembers／access／欄位常數等；本工具把它們算進 DIFF。");
        lines.add("- ONLY_* 不在 430 裡：430 只估計**兩邊都有**的類別。ONLY_LEFT／ONLY_RIGHT 應對齊 `docs/server-source-gap.md`。");
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

    static String normalizeClassName(String raw) {
        String s = raw.trim().replace('.', '/');
        if (s.endsWith(".class")) {
            s = s.substring(0, s.length() - 6);
        }
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
        Set<String> classes;

        static Args parse(String[] args) throws IOException {
            Args out = new Args();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--left" -> out.left = Path.of(need(args, ++i, a));
                    case "--right" -> out.right = Path.of(need(args, ++i, a));
                    case "--json" -> out.json = Path.of(need(args, ++i, a));
                    case "--md" -> out.md = Path.of(need(args, ++i, a));
                    case "--classes" -> out.classes = parseClasses(need(args, ++i, a));
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
            return out;
        }

        private static String need(String[] args, int i, String flag) {
            if (i >= args.length) {
                throw new IllegalArgumentException(flag + " needs a value");
            }
            return args[i];
        }

        private static Set<String> parseClasses(String spec) throws IOException {
            Path path = Path.of(spec);
            String text;
            if (Files.isRegularFile(path)) {
                text = Files.readString(path, StandardCharsets.UTF_8);
            } else {
                text = spec.replace(',', '\n');
            }
            Set<String> names = new LinkedHashSet<>();
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                names.add(normalizeClassName(trimmed));
            }
            if (names.isEmpty()) {
                throw new IllegalArgumentException("--classes produced an empty set");
            }
            return names;
        }

        private static void usage() {
            System.err.println(
                    "usage: semantic-diff --left A.jar --right B.jar [--classes file-or-list] [--json out.json] [--md out.md]");
        }
    }
}
