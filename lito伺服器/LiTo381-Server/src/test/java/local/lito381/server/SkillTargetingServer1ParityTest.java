package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Issue #305（T106）：技能目標與範圍判定對照 1-伺服器。
 *
 * <p>輸入 {@code server1/skills-targeting-181.tsv}：1-伺服器 {@code db/380_db.sql}
 * {@code skills} 表 181 個有入口技能的 target／target_to／type／ranged／area／through，
 * 加上 {@code tools/skill-targeting/golden.py} 依 {@code L1SkillUse} 另行產生的預期欄位。
 * 規格見 {@code docs/design/skill-targeting.md}。</p>
 */
final class SkillTargetingServer1ParityTest {
    record Fixture(int skillId, NativeSkillTargetMetadata.Row row, String shape, boolean losBlocks,
                   Map<SourceSkillTargeting.Kind, String> effects) {
        @Override public String toString() { return "skill" + skillId; }
    }

    private static final List<SourceSkillTargeting.Kind> PRIMARIES = List.of(
            SourceSkillTargeting.Kind.SELF, SourceSkillTargeting.Kind.ENEMY,
            SourceSkillTargeting.Kind.PARTY, SourceSkillTargeting.Kind.MONSTER,
            SourceSkillTargeting.Kind.PET);

    /** LiTo 原生螢幕距離的表示法：Source ranged=−1 可對應 −1（呼叫端再看螢幕）或 19（isInNativeScreen 外框）。 */
    private static final Set<Integer> SCREEN_ENCODINGS = Set.of(-1, 19);

    /** 沒有 BattleSpellCatalog 紀錄的入口技能：5 指定傳送走獨立處理器。 */
    private static final Set<Integer> NO_CATALOG_RECORD = Set.of(5);

    /**
     * 目錄欄位原值與 Source 不同，但執行期由 NativeSkillTargetMetadata 投影成 Source 值的項目
     * （key = 技能:欄位）。每一項都寫明投影位置；新出現的不一致會讓測試失敗。
     */
    private static final Map<String, String> RUNTIME_PROJECTED = Map.of(
            "103:maximumRange", "NativeBlindSceneAction.selected 以 row.ranged()=0 判定命中（目錄 3 只用於寵物分支准入）",
            "133:maximumRange", "NativeSourceRemainingMaterials.valid/maximumRange 以 row.ranged()=-1 改看螢幕",
            "148:maximumRange", "NativeSourceWeaponBuffTargets.Aim.valid 要求 ranged=-1 並以 isInNativeScreen 判定（BATTLE）",
            "149:maximumRange", "NativeSourceWeaponBuffTargets.Aim.valid 要求 ranged=-1 並以 isInNativeScreen 判定（BATTLE）");

    private static final List<String> RANGE_FIELDS = List.of("maximumRange");
    private static final List<String> AREA_FIELDS = List.of("area", "areaRadius", "sourceAreaValue");

    private static List<Fixture> fixtures;
    private static Map<Integer, NativeSkillTargetMetadata.Row> prior;

    @BeforeAll static void installSourceRows() {
        fixtures = load();
        Map<Integer, NativeSkillTargetMetadata.Row> rows = new LinkedHashMap<>();
        for (Fixture f : fixtures) rows.put(f.skillId(), f.row());
        prior = NativeSkillTargetMetadata.replaceForTesting(rows);
    }

    @AfterAll static void restoreRows() {
        NativeSkillTargetMetadata.replaceForTesting(prior == null ? Map.of() : prior);
    }

    static Stream<Fixture> rows() {
        if (fixtures == null) fixtures = load();
        return fixtures.stream();
    }

    private static List<Fixture> load() {
        InputStream in = SkillTargetingServer1ParityTest.class.getResourceAsStream("/server1/skills-targeting-181.tsv");
        assertNotNull(in, "missing server1/skills-targeting-181.tsv");
        List<Fixture> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("skill_id")) continue;
                String[] f = line.split("\t", -1);
                assertEquals(9 + PRIMARIES.size() + 2, f.length, line);
                int id = Integer.parseInt(f[0]);
                var row = new NativeSkillTargetMetadata.Row(f[1], Integer.parseInt(f[2]), Integer.parseInt(f[4]),
                        Integer.parseInt(f[5]), f[6].equals("1"), Integer.parseInt(f[3]),
                        Integer.parseInt(f[9 + PRIMARIES.size()]), Integer.parseInt(f[10 + PRIMARIES.size()]));
                Map<SourceSkillTargeting.Kind, String> effects = new LinkedHashMap<>();
                for (int i = 0; i < PRIMARIES.size(); i++) effects.put(PRIMARIES.get(i), f[9 + i]);
                out.add(new Fixture(id, row, f[7], f[8].equals("1"), effects));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(out);
    }

    private static String encode(EnumSet<SourceSkillTargeting.Kind> set) {
        if (set == null) return "PLACE";
        if (set.isEmpty()) return "-";
        return set.stream().map(Enum::name).collect(Collectors.joining("+"));
    }

    @Test void fixtureCoversExactly181EntrySkills() {
        List<Fixture> all = rows().toList();
        assertEquals(181, all.size());
        assertEquals(181, all.stream().map(Fixture::skillId).distinct().count());
        // 收尾矩陣 player-selector-skill：1..220 去掉 38 個保留槽與 122。
        Set<Integer> reserved = new TreeSet<>(List.of(24, 81, 82, 83, 84, 85, 86, 92, 93, 94, 95, 96,
                119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 135, 136, 139, 140, 141, 142, 143, 144,
                177, 178, 179, 180, 196, 197, 198, 199, 200));
        for (int id = 1; id <= 220; id++) {
            final int skill = id;
            assertEquals(!reserved.contains(id), all.stream().anyMatch(f -> f.skillId() == skill), "skill" + id);
        }
    }

    /** 規格模型（SourceSkillTargeting）與獨立產生的預期表逐列一致：形狀、視線、五種主目標下的效果對象。 */
    @ParameterizedTest @MethodSource("rows")
    void sourceModelMatchesIndependentExpectation(Fixture f) {
        assertEquals(f.shape(), SourceSkillTargeting.shape(f.skillId(), f.row()).name(), "shape");
        assertEquals(f.losBlocks(), SourceSkillTargeting.losBlocks(f.row()), "losBlocks");
        for (var primary : PRIMARIES) {
            assertEquals(f.effects().get(primary),
                    encode(SourceSkillTargeting.effects(f.skillId(), f.row(), primary)), "primary " + primary);
        }
    }

    /** ranged 邊界：N 格內可、N+1 格不可；−1 只看螢幕。 */
    @ParameterizedTest @MethodSource("rows")
    void rangedBoundary(Fixture f) {
        var row = f.row();
        if (row.ranged() == SourceSkillTargeting.RANGED_SCREEN) {
            assertTrue(SourceSkillTargeting.inRange(row, 100, true));
            assertFalse(SourceSkillTargeting.inRange(row, 0, false));
            assertTrue(BattleSpellCatalog.isInNativeScreen(0, 0, 18, 0));
            assertFalse(BattleSpellCatalog.isInNativeScreen(0, 0, 20, 0));
        } else {
            assertTrue(SourceSkillTargeting.inRange(row, row.ranged(), true));
            assertFalse(SourceSkillTargeting.inRange(row, row.ranged() + 1, true));
            assertTrue(SourceSkillTargeting.inRange(row, row.ranged(), false));
        }
    }

    private static List<Object> catalogRecords(int skillId) {
        List<Object> out = new ArrayList<>();
        for (Method m : BattleSpellCatalog.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1
                    || m.getParameterTypes()[0] != int.class || !m.getName().endsWith("Spell")
                    || !m.getReturnType().isRecord()) continue;
            try {
                m.setAccessible(true);
                Object value = m.invoke(null, skillId);
                if (value != null && out.stream().noneMatch(v -> v == value)) out.add(value);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(m.getName() + "(" + skillId + ")", e);
            }
        }
        return out;
    }

    private static Integer component(Object record, String name) {
        for (RecordComponent c : record.getClass().getRecordComponents()) {
            if (!c.getName().equals(name) || c.getType() != int.class) continue;
            try {
                Method accessor = c.getAccessor();
                accessor.setAccessible(true);
                return (Integer) accessor.invoke(record);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(record.getClass().getSimpleName() + "." + name, e);
            }
        }
        return null;
    }

    /** 在 Source 列已載入的情況下，LiTo 目錄記錄的「有效」射程／範圍／target_to／type 與 380_db.skills 相同。 */
    @ParameterizedTest @MethodSource("rows")
    void catalogEffectiveTargetingMatchesSource(Fixture f) {
        List<Object> records = catalogRecords(f.skillId());
        if (NO_CATALOG_RECORD.contains(f.skillId())) {
            assertTrue(records.isEmpty(), "skill" + f.skillId() + " unexpectedly has catalog records");
            return;
        }
        assertFalse(records.isEmpty(), "no BattleSpellCatalog record for skill" + f.skillId());
        var row = f.row();
        for (Object record : records) {
            String where = record.getClass().getSimpleName();
            for (String field : RANGE_FIELDS) {
                Integer value = component(record, field);
                if (value == null || RUNTIME_PROJECTED.containsKey(f.skillId() + ":" + field)) continue;
                if (row.ranged() == SourceSkillTargeting.RANGED_SCREEN) {
                    assertTrue(SCREEN_ENCODINGS.contains(value), where + "." + field + "=" + value + " (Source -1)");
                } else {
                    assertEquals(row.ranged(), value, where + "." + field);
                }
            }
            for (String field : AREA_FIELDS) {
                Integer value = component(record, field);
                if (value != null) assertEquals(row.area(), value, where + "." + field);
            }
            Integer targetTo = component(record, "targetTo");
            if (targetTo != null) assertEquals(row.targetTo(), targetTo, where + ".targetTo");
            Integer type = component(record, "type");
            if (type != null) assertEquals(row.type(), type, where + ".type");
        }
    }

    /** RUNTIME_PROJECTED 清單必須正好是目前仍不一致的欄位（修好就要移除，避免清單過期）。 */
    @Test void runtimeProjectedListIsExact() {
        Map<String, Integer> stillDifferent = new TreeMap<>();
        for (Fixture f : rows().toList()) {
            for (Object record : catalogRecords(f.skillId())) {
                Integer value = component(record, "maximumRange");
                if (value == null) continue;
                boolean same = f.row().ranged() == SourceSkillTargeting.RANGED_SCREEN
                        ? SCREEN_ENCODINGS.contains(value) : f.row().ranged() == value;
                if (!same) stillDifferent.put(f.skillId() + ":maximumRange", value);
            }
        }
        assertEquals(new TreeSet<>(RUNTIME_PROJECTED.keySet()), stillDifferent.keySet(), stillDifferent.toString());
    }

    private static NativeSkillTargetMetadata.Row row(int skill) {
        return NativeSkillTargetMetadata.row(skill);
    }

    // ---- customer-gaps G03–G08 ----

    /** G03：167 風之枷鎖是單體（area0／through0），射程看螢幕；LiTo 目錄值由 DB 投影。 */
    @Test void g03WindShackleIsSingleTarget() {
        var r = row(167);
        assertEquals(SourceSkillTargeting.Shape.SINGLE, SourceSkillTargeting.shape(167, r));
        assertEquals(EnumSet.of(SourceSkillTargeting.Kind.ENEMY),
                SourceSkillTargeting.effects(167, r, SourceSkillTargeting.Kind.ENEMY));
        var spell = BattleSpellCatalog.windShackleSpell(167);
        assertEquals(0, spell.area());
        assertEquals(3, spell.targetTo());
        assertEquals(-1, spell.maximumRange());
        assertTrue(SourceSkillTargeting.losBlocks(r));
    }

    /** G04：194 暴龍之眼 area0、ranged10；Source 的直線列舉分支用 calcType 比對技能 ID，永遠不會進入，所以是單體。 */
    @Test void g04FreezingBreathIsSingleTargetRangeTen() {
        var r = row(194);
        assertEquals(SourceSkillTargeting.Shape.SINGLE, SourceSkillTargeting.shape(194, r));
        var spell = BattleSpellCatalog.freezeAttackSpell(194);
        assertFalse(NativeSourceFreezeTarget.line(spell), "Source194 must not enumerate a line");
        assertEquals(10, NativeSourceFreezeTarget.range(spell));
        assertEquals(10, spell.maximumRange());
        assertEquals(EnumSet.of(SourceSkillTargeting.Kind.ENEMY),
                SourceSkillTargeting.effects(194, r, SourceSkillTargeting.Kind.ENEMY));
    }

    /** G05：76 集體緩速術對自己或自己的寵物為主目標時 Source 扣 MP、播動作但不對任何人生效；其餘範圍內不含自己與自己的寵物。 */
    @Test void g05MassSlowSelfPrimaryAndExclusions() {
        var r = row(76);
        assertEquals(EnumSet.noneOf(SourceSkillTargeting.Kind.class),
                SourceSkillTargeting.effects(76, r, SourceSkillTargeting.Kind.SELF));
        assertEquals(EnumSet.noneOf(SourceSkillTargeting.Kind.class),
                SourceSkillTargeting.effects(76, r, SourceSkillTargeting.Kind.PET));
        var onEnemy = SourceSkillTargeting.effects(76, r, SourceSkillTargeting.Kind.ENEMY);
        assertFalse(onEnemy.contains(SourceSkillTargeting.Kind.SELF));
        assertFalse(onEnemy.contains(SourceSkillTargeting.Kind.PET));
        assertTrue(onEnemy.contains(SourceSkillTargeting.Kind.ENEMY));
        var spell = BattleSpellCatalog.massSlowSpell(76);
        assertEquals(3, spell.areaRadius());
        assertEquals(19, spell.maximumRange(), "Source ranged=-1 projects to the native screen bound");
        assertEquals(Integer.MAX_VALUE, NativeSourceVisibilityRange.signedDistance76(30, 0));
        assertEquals(5, NativeSourceVisibilityRange.signedDistance76(5, -3));
    }

    /** G06：29／152／153 單體機率技能，target=buff 且 target_to 含 PC，所以 Source 允許對自己施放。 */
    @Test void g06NegativeSingleTargetsAllowSelf() {
        for (int skill : new int[] {29, 152, 153}) {
            var r = row(skill);
            // NativeSourceNegativeSelf.legal 的 metadata 前提。
            assertEquals(1, r.type());
            assertEquals(0, r.area());
            assertTrue((r.targetTo() & 1) != 0);
            assertEquals("buff", r.target());
            assertEquals(EnumSet.of(SourceSkillTargeting.Kind.SELF),
                    SourceSkillTargeting.effects(skill, r, SourceSkillTargeting.Kind.SELF), "skill" + skill);
        }
        assertEquals(19, BattleSpellCatalog.entangleSpell(29).maximumRange());
        assertEquals(10, BattleSpellCatalog.entangleSpell(152).maximumRange());
        assertEquals(-1, BattleSpellCatalog.eraseMagicSpell(153).maximumRange());
        assertEquals(Integer.MAX_VALUE, NativeSourceVisibilityRange.signedDistance29(0, 25));
    }

    /** G07：17 極光雷電在 Source 是以目標為中心 area3（直線分支不可達）；148／149 是只對 PC 的單體 buff、射程看螢幕。 */
    @Test void g07LightningAreaAndWeaponBuffTargets() {
        var lightning = BattleSpellCatalog.lineAttackSpell(17);
        List<ArenaMapCatalog.Point> points = NativeSourceLightningArea.points(10, 10, 14, 10, lightning);
        assertEquals(49, points.size());
        assertTrue(points.contains(new ArenaMapCatalog.Point(14, 10)));
        assertTrue(points.contains(new ArenaMapCatalog.Point(17, 13)));
        assertFalse(points.contains(new ArenaMapCatalog.Point(10, 10)), "caster tile is outside area3 of the target");
        assertEquals(SourceSkillTargeting.Shape.AREA, SourceSkillTargeting.shape(17, row(17)));
        for (int skill : new int[] {148, 149}) {
            var r = row(skill);
            // NativeSourceWeaponBuffTargets.Aim.valid 的 metadata 前提。
            assertEquals(2, r.type());
            assertEquals(1, r.targetTo());
            assertEquals(0, r.area());
            assertEquals(-1, r.ranged());
            assertFalse(r.through());
            assertEquals(EnumSet.noneOf(SourceSkillTargeting.Kind.class),
                    SourceSkillTargeting.effects(skill, r, SourceSkillTargeting.Kind.MONSTER));
            assertEquals(EnumSet.of(SourceSkillTargeting.Kind.PARTY),
                    SourceSkillTargeting.effects(skill, r, SourceSkillTargeting.Kind.PARTY));
        }
    }

    /** G08：40 黑闇之影 ranged3、103 暗黑盲咒 ranged0；兩者都是單體。 */
    @Test void g08DarknessAndDarkBlindRanges() {
        assertEquals(3, row(40).ranged());
        assertEquals(0, row(103).ranged());
        assertTrue(SourceSkillTargeting.inRange(row(40), 3, true));
        assertFalse(SourceSkillTargeting.inRange(row(40), 4, true));
        assertTrue(SourceSkillTargeting.inRange(row(103), 0, true));
        assertFalse(SourceSkillTargeting.inRange(row(103), 1, true));
        assertEquals(SourceSkillTargeting.Shape.SINGLE, SourceSkillTargeting.shape(40, row(40)));
        assertEquals(SourceSkillTargeting.Shape.SINGLE, SourceSkillTargeting.shape(103, row(103)));
        assertEquals(3, BattleSpellCatalog.blindSpell(40).maximumRange());
    }

    /** 183／188／193 目錄的 target_to／type 已改回 380_db.skills（原本錯放 probability_dice 與 target_to）。 */
    @Test void guardBrakeFamilyTargetColumnsFollowSource() {
        for (int skill : new int[] {183, 193}) {
            var spell = BattleSpellCatalog.guardBrakeSpell(skill);
            assertEquals(row(skill).targetTo(), spell.targetTo());
            assertEquals(row(skill).type(), spell.type());
        }
        var fear = BattleSpellCatalog.resistFearSpell(188);
        assertEquals(row(188).targetTo(), fear.targetTo());
        assertEquals(row(188).type(), fear.type());
    }

    /** 反制魔法硬編碼例外表（L1SkillUse.java:334-344）抽查。 */
    @Test void counterMagicExceptionSpotChecks() {
        assertFalse(SourceSkillTargeting.counterMagicApplies(1));
        assertFalse(SourceSkillTargeting.counterMagicApplies(87));
        assertFalse(SourceSkillTargeting.counterMagicApplies(148));
        assertTrue(SourceSkillTargeting.counterMagicApplies(4));
        assertTrue(SourceSkillTargeting.counterMagicApplies(167));
        assertTrue(SourceSkillTargeting.counterMagicApplies(194));
        assertTrue(SourceSkillTargeting.counterMagicApplies(103));
    }
}
