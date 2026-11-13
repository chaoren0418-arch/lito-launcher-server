package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Issue #308（T108）：妖精 40 個技能（129..176 去掉保留槽 135、136、139..144）的行為對照 1-伺服器。
 *
 * <p>輸入 {@code server1/skills-elf-40.tsv}：1-伺服器 {@code db/380_db.sql} {@code skills} 表的 16 欄
 * （{@code tools/skill-targeting/extract_elf.py} 唯讀擷取，欄位與 T107 相同）。期望值一律由這張表、
 * {@code skills_buff_ability} 或本檔依 1-伺服器 原始碼另寫的 {@code ref*} 函式產生，不讀 LiTo 自己的常數。
 * 逐技能對照見 {@code docs/design/skills-elf.md}。</p>
 */
final class ElfSkillServer1ParityTest {
    record Row(int skillId, int skillLevel, int mp, int hp, int itemId, int itemCount, int reuseMillis,
               int buffSeconds, int damageValue, int damageDice, int damageDiceCount, int probabilityValue,
               int probabilityDice, int attr, int type, int lawful) {
        @Override public String toString() { return "skill" + skillId; }
    }

    /** 1-伺服器 skills 中 name=none 的妖精保留槽。 */
    private static final Set<Integer> RESERVED = Set.of(135, 136, 139, 140, 141, 142, 143, 144);

    private static List<Row> rows;

    static Stream<Row> rows() {
        if (rows == null) rows = load();
        return rows.stream();
    }

    private static List<Row> load() {
        InputStream in = ElfSkillServer1ParityTest.class.getResourceAsStream("/server1/skills-elf-40.tsv");
        assertNotNull(in, "missing server1/skills-elf-40.tsv");
        List<Row> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("skill_id")) continue;
                String[] f = line.split("\t", -1);
                assertEquals(16, f.length, line);
                int[] v = new int[16];
                for (int i = 0; i < 16; i++) v[i] = Integer.parseInt(f[i]);
                out.add(new Row(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
                        v[12], v[13], v[14], v[15]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(out);
    }

    private static Row row(int skillId) {
        return rows().filter(r -> r.skillId() == skillId).findFirst().orElseThrow();
    }

    @Test void fixtureCoversExactly40ElfSkills() {
        List<Row> all = rows().toList();
        assertEquals(40, all.size());
        for (int id = 129; id <= 176; id++) {
            final int skill = id;
            assertEquals(!RESERVED.contains(id), all.stream().anyMatch(r -> r.skillId() == skill), "skill" + id);
        }
        for (Row r : all) {
            assertEquals(((r.skillId() - 1) >> 3) + 1, r.skillLevel(), r.toString());
            assertEquals(r.skillLevel(), SourceElfAttributeGate.skillLevel(r.skillId()), r.toString());
            assertTrue(r.skillLevel() >= 17 && r.skillLevel() <= 22, r.toString());
        }
    }

    // ---- 反射工具（與 WizardSkillServer1ParityTest 相同做法） ----

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

    private static Long component(Object record, String name) {
        for (RecordComponent c : record.getClass().getRecordComponents()) {
            if (!c.getName().equals(name) || (c.getType() != int.class && c.getType() != long.class)) continue;
            try {
                Method accessor = c.getAccessor();
                accessor.setAccessible(true);
                return ((Number) accessor.invoke(record)).longValue();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(record.getClass().getSimpleName() + "." + name, e);
            }
        }
        return null;
    }

    private static void same(Object record, String field, long expected, Row r) {
        Long value = component(record, field);
        if (value != null) {
            assertEquals(expected, value.longValue(),
                    r + " " + record.getClass().getSimpleName() + "." + field);
        }
    }

    /** 消耗（MP／HP／材料）、冷卻、期限：catalog 有效值（未載入 DB 時的後備值）與 380_db.skills 相同。 */
    @ParameterizedTest @MethodSource("rows")
    void consumptionCooldownAndDurationMatchSource(Row r) {
        List<Object> records = catalogRecords(r.skillId());
        assertFalse(records.isEmpty(), "no BattleSpellCatalog record for " + r);
        for (Object record : records) {
            String where = r + " " + record.getClass().getSimpleName();
            Long id = component(record, "skillId");
            assertNotNull(id, where);
            assertEquals(r.skillId(), id.intValue(), where);
            Long mp = component(record, "mpCost");
            assertNotNull(mp, where + " has no mpCost");
            assertEquals(r.mp(), mp.longValue(), where + ".mpCost");
            Long hp = component(record, "hpCost");
            if (hp == null) assertEquals(0, r.hp(), where + " has no hpCost but SQL hp≠0");
            else assertEquals(r.hp(), hp.longValue(), where + ".hpCost");
            same(record, "reuseDelayMillis", r.reuseMillis(), r);
            Long duration = component(record, "durationSeconds");
            if (duration != null) assertEquals(r.buffSeconds(), duration.longValue(), where + ".durationSeconds");
            Long item = component(record, "itemConsumeId");
            if (item == null) {
                assertEquals(0, r.itemId(), where + " has no itemConsumeId but SQL itemConsumeId≠0");
            } else {
                assertEquals(r.itemId(), item.longValue(), where + ".itemConsumeId");
                Long count = component(record, "itemConsumeCount");
                assertNotNull(count, where + " has no itemConsumeCount");
                assertEquals(r.itemCount(), count.longValue(), where + ".itemConsumeCount");
            }
        }
    }

    /** 治癒骰（164）、機率骰、正義值（妖精 40 個 SQL lawful 全為 0）。 */
    @ParameterizedTest @MethodSource("rows")
    void healingProbabilityAndLawfulMatchSource(Row r) {
        assertEquals(0, r.lawful(), r + " SQL lawful");
        for (Object record : catalogRecords(r.skillId())) {
            String where = r + " " + record.getClass().getSimpleName();
            if (component(record, "baseDiceCount") != null) {
                // L1MagicPc.calcHealing: (damage_value + MB) d damage_dice
                same(record, "baseDiceCount", r.damageValue(), r);
                same(record, "dieSides", r.damageDice(), r);
            }
            same(record, "probabilityDice", r.probabilityDice(), r);
            same(record, "probabilityValue", r.probabilityValue(), r);
            Long lawful = component(record, "lawfulDelta");
            if (lawful != null) assertEquals(0, lawful.longValue(), where + ".lawfulDelta");
        }
    }

    /**
     * 施放屬性：catalog 的 {@code attribute}／{@code requiredElfAttribute} 等於 SQL {@code attr}。
     * BindSpell（157）沒有欄位，改由 {@link BattleSpellCatalog#EARTH_BIND_ATTRIBUTE} 提供。
     */
    @ParameterizedTest @MethodSource("rows")
    void castAttributeMatchesSource(Row r) {
        boolean found = false;
        for (Object record : catalogRecords(r.skillId())) {
            Long attribute = component(record, "attribute");
            if (attribute == null) attribute = component(record, "requiredElfAttribute");
            if (attribute == null) continue;
            found = true;
            assertEquals(r.attr(), attribute.longValue(), r + " " + record.getClass().getSimpleName() + ".attribute");
        }
        if (!found) {
            if (r.skillId() == BattleSpellCatalog.EARTH_BIND) {
                assertEquals(r.attr(), BattleSpellCatalog.EARTH_BIND_ATTRIBUTE, r.toString());
            } else {
                assertEquals(0, r.attr(), r + " has no attribute component but SQL attr≠0");
            }
        }
    }

    // ---- 屬性閘門：S1 L1SkillUse.isAttrAgrees（:1850-1863） ----

    private static boolean refIsAttrAgrees(int skillLevel, int magicAttr, int elfAttr, boolean gm) {
        if (skillLevel >= 17 && skillLevel <= 22 && magicAttr != 0 && (elfAttr & magicAttr) == 0 && !gm) {
            return false;
        }
        return true;
    }

    @ParameterizedTest @MethodSource("rows")
    void elfAttributeGateMatchesIsAttrAgrees(Row r) {
        for (int elfAttr = 0; elfAttr <= 15; elfAttr++) {
            for (boolean gm : new boolean[] {false, true}) {
                assertEquals(refIsAttrAgrees(r.skillLevel(), r.attr(), elfAttr, gm),
                        SourceElfAttributeGate.agrees(r.skillId(), r.attr(), elfAttr, gm),
                        r + " elfAttr=" + elfAttr + " gm=" + gm);
            }
        }
    }

    @Test void elfAttributeGateIgnoresNonElfSkillLevels() {
        // 法師 43、騎士 87、黑妖 97、龍騎士 181（等級 23）都不受屬性限制，即使 attr 欄位非 0。
        for (int skill : new int[] {43, 87, 97, 128, 177, 181}) {
            assertTrue(SourceElfAttributeGate.agrees(skill, 8, 0, false), "skill" + skill);
        }
    }

    /**
     * SQL attr≠0 的 13 個技能，每個都要在實際施放路徑上檢查 ElfAttr（宣告後 1500 字內出現 elfAttribute）。
     * 157／159／160／170 是本 PR 新增（elfAttributeRejects），其餘是既有檢查。
     */
    private static final Map<Integer, List<String>> ATTRIBUTE_GATES = new TreeMap<>(Map.ofEntries(
            Map.entry(157, List.of("private void useBindSpell(", "private void useClassicWorldBindSpell(")),
            Map.entry(159, List.of("private void useEarthBlessSpell(")),
            Map.entry(160, List.of("private void useAquaProtectSpell(")),
            Map.entry(163, List.of("void nativeSourceFireWeaponOriginalBody(")),
            Map.entry(164, List.of("private void nativeSourceHealingOriginalNature(")),
            Map.entry(165, List.of("void nativeMaterialCallNatureBody(")),
            Map.entry(167, List.of("void nativeMaterialAreaWindBody(")),
            Map.entry(170, List.of("void nativeMaterialWaterLifeBody(")),
            Map.entry(171, List.of("void nativeMaterialPhysicalProcBuffBody(")),
            Map.entry(172, List.of("private void useOtherNoEffectSpell(")),
            Map.entry(173, List.of("void nativeMaterialTargetNegativeBody(")),
            Map.entry(174, List.of("void nativeMaterialTargetNegativeBody(")),
            Map.entry(175, List.of("void nativeMaterialPhysicalProcBuffBody("))));

    @Test void everyAttributeSkillHasACastTimeGate() throws IOException {
        Set<Integer> attributeSkills = rows().filter(r -> r.attr() != 0).map(Row::skillId)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(attributeSkills, ATTRIBUTE_GATES.keySet());
        String session = Files.readString(Path.of("src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        for (var entry : ATTRIBUTE_GATES.entrySet()) {
            for (String declaration : entry.getValue()) {
                int at = session.indexOf(declaration);
                assertTrue(at >= 0, "missing " + declaration);
                assertEquals(at, session.lastIndexOf(declaration), "duplicate " + declaration);
                String window = session.substring(at, Math.min(session.length(), at + 1500));
                assertTrue(window.contains("elfAttribute"), "skill" + entry.getKey() + " " + declaration);
            }
        }
        for (String added : List.of("private void useBindSpell(", "private void useClassicWorldBindSpell(",
                "private void useEarthBlessSpell(", "private void useAquaProtectSpell(",
                "void nativeMaterialWaterLifeBody(")) {
            int at = session.indexOf(added);
            assertTrue(session.substring(at, at + 1500).contains("elfAttributeRejects("), added);
        }
    }

    // ---- MP 消耗公式（妖精技能 > 80，不吃 INT 減免；頭盔表沒有妖精技能） ----

    private static int refMp(int skillId, int sqlMp, int intelligence, int originalReduction, boolean helm) {
        int mp = sqlMp;
        int[][] tiers = {{12, 8}, {13, 16}, {14, 23}, {15, 32}, {16, 40}, {17, 48}, {18, 56}};
        for (int[] t : tiers) if (intelligence > t[0] && skillId > t[1] && skillId <= 80) mp--;
        if (intelligence > 12 && skillId >= 87 && skillId <= 91) mp -= intelligence - 12;
        if (helm && Set.of(26, 43, 1, 19, 12, 13, 42, 54).contains(skillId)) mp >>= 1;
        if (sqlMp > 0) mp = Math.max(mp, 1);
        if (originalReduction > 0) mp -= originalReduction;
        return mp;
    }

    @ParameterizedTest @MethodSource("rows")
    void runtimeMpCostMatchesIsHpMpConsume(Row r) {
        for (int intelligence = 1; intelligence <= 40; intelligence++) {
            for (int reduction = 0; reduction <= 2; reduction++) {
                for (boolean helm : new boolean[] {false, true}) {
                    assertEquals(refMp(r.skillId(), r.mp(), intelligence, reduction, helm),
                            NativeSourceSkillConsumptionMath.mpCost(r.skillId(), r.mp(), intelligence, reduction, helm),
                            r + " int=" + intelligence + " reduction=" + reduction + " helm=" + helm);
                }
            }
        }
    }

    /** L1PcOriginal.resetOriginalMagicConsumeReduction（:1012-1060）：妖精（type 2）沒有原始 INT 減免。 */
    @Test void elfHasNoOriginalIntelligenceReduction() {
        for (int originalInt = 0; originalInt <= 30; originalInt++) {
            assertEquals(0, NativeSourceSkillConsumptionMath.originalReduction(2, originalInt), "int=" + originalInt);
        }
    }

    // ---- 效果數值（skills_buff_ability 與 L1SkillUse 分支） ----

    /** 1-伺服器 db skills_buff_ability 的妖精列，以及 L1SkillUse.java:3552-3603 的直接加值。 */
    @Test void buffAbilityFallbacksMatchSource() {
        assertEquals(10, BattleSpellCatalog.magicResistanceSpell(129).magicResistance());       // :3558 addMr(10)
        assertEquals(3, BattleSpellCatalog.clearMindSpell(137).wisdomModifier());              // (137,'WIS',3)
        assertEquals(10, BattleSpellCatalog.resistElementalSpell(138).resistanceModifier());   // :3578-3581 +10×4
        assertEquals(50, BattleSpellCatalog.elementalProtectionSpell(147).resistanceModifier());// :3592-3603 +50
        var fire = BattleSpellCatalog.fireWeaponSpell(148);                                    // (148,'DMG',4)
        assertEquals(4, fire.meleeDamageModifier());
        assertEquals(0, fire.meleeHitModifier());
        assertEquals(6, BattleSpellCatalog.windShotSpell(149).rangedHitModifier());            // (149,'BOW_HIT',6)
        assertEquals(-6, BattleSpellCatalog.shieldSpell(151).armorClassModifier());            // (151,'AC',-6)
        assertEquals(4, BattleSpellCatalog.fireBlessSpell(155).meleeDamageModifier());         // (155,'DMG',4)
        var stormEye = BattleSpellCatalog.stormEyeSpell(156);                                  // (156,BOW_HIT2,BOW_DMG3)
        assertEquals(2, stormEye.rangedHitModifier());
        assertEquals(3, stormEye.rangedDamageModifier());
        assertEquals(-7, BattleSpellCatalog.earthBlessSpell(159).armorClassModifier());        // (159,'AC',-7)
        var burning = BattleSpellCatalog.fireWeaponSpell(163);                                 // (163,DMG6,HIT3)
        assertEquals(6, burning.meleeDamageModifier());
        assertEquals(3, burning.meleeHitModifier());
        var stormShot = BattleSpellCatalog.stormShotSpell(166);                                // (166,BOW_DMG5,BOW_HIT-1)
        assertEquals(-1, stormShot.rangedHitModifier());
        assertEquals(5, stormShot.rangedDamageModifier());
        assertEquals(-10, BattleSpellCatalog.ironSkinSpell(168).armorClassModifier());         // (168,'AC',-10)
    }

    /** skillmode/BODY_TO_MIND.java:22 adjustCurrentMp(2)；BLOODY_SOUL.java:23 用 config BLOODY_SOULADDMP=20。 */
    @Test void mindConversionManaGainMatchesSkillModes() {
        assertEquals(2, BattleSpellCatalog.bodyToMindSpell(130).manaGain());
        assertEquals(8, BattleSpellCatalog.bodyToMindSpell(130).hpCost());
        // config/其他控制端/4-職業_技能設定表.properties:60 BLOODY_SOULADDMP = 20（程式預設 12）
        assertEquals(20, BattleSpellCatalog.bodyToMindSpell(146).manaGain());
        assertEquals(40, BattleSpellCatalog.bodyToMindSpell(146).hpCost());
    }

    /** skillmode/TRIPLE_ARROW.java:46-47 onAction 三次；ELEMENTAL_FALL_DOWN.java:34 屬性 −50。 */
    @Test void tripleArrowAndElementalFallDownMatchSkillModes() {
        assertEquals(3, BattleSpellCatalog.tripleArrowSpell(132).hitCount());
        assertEquals(50, BattleSpellCatalog.elementalFallDownSpell(133).resistanceDebuff());
    }

    /** 150 風之疾走：L1SkillUse.java:3779-3786 setBraveSpeed(4)，與神聖疾走同在 ALWAYS 群組（:2513-2514）。 */
    @Test void windWalkUsesBraveMode4LikeHolyWalk() {
        var windWalk = BattleSpellCatalog.movementBuffSpell(150);
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, windWalk.braveMode());
        assertEquals(BattleSpellCatalog.movementBuffSpell(52).braveMode(), windWalk.braveMode());
        long now = java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        var status = new BattleTimedStatus();
        status.activateBrave(now, 300, BattleTimedStatus.BRAVE_FIRST);
        status.activateBrave(now, windWalk.durationSeconds(), windWalk.braveMode());
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, status.brave(now).mode());
        assertEquals(row(150).buffSeconds(), status.brave(now).remainingSeconds());
    }

    /**
     * SkillCoexistSet=1（1-伺服器 server_event 116 預設）時 ORIGINAL_REPEATED_SKILLS（L1SkillUse.java:2520-2527）
     * 不生效：{148,149,155,156,163,166} 與 {159,168} 各自獨立計時。LiTo 每個都有自己的狀態欄位。
     */
    @Test void elfWeaponAndArmorBuffsHaveIndependentLeases() {
        List<Class<?>> types = List.of(BattleFireWeaponStatus.class, BattleWindShotStatus.class,
                BattleFireBlessStatus.class, BattleStormEyeStatus.class, BattleBurningWeaponStatus.class,
                BattleStormShotStatus.class, BattleEarthBlessStatus.class, BattleIronSkinStatus.class);
        Set<String> fields = new HashSet<>();
        for (Class<?> type : types) {
            List<Field> matches = new ArrayList<>();
            for (Field f : ClientSession.class.getDeclaredFields()) {
                if (f.getType() == type && !Modifier.isStatic(f.getModifiers())) matches.add(f);
            }
            assertEquals(1, matches.size(), type.getSimpleName());
            assertTrue(fields.add(matches.get(0).getName()), type.getSimpleName());
        }
        assertEquals(types.size(), fields.size());
    }

    /** 159／160 修正後的屬性與 SQL 相同（地 1、水 4），且不再是「未指定」。 */
    @Test void earthBlessAndAquaProtectCarrySourceAttribute() {
        assertEquals(row(159).attr(), BattleSpellCatalog.earthBlessSpell(159).attribute());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_EARTH, BattleSpellCatalog.earthBlessSpell(159).attribute());
        assertEquals(row(160).attr(), BattleSpellCatalog.aquaProtectSpell(160).attribute());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, BattleSpellCatalog.aquaProtectSpell(160).attribute());
        assertNotEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, BattleSpellCatalog.waterLifeSpell(170).attribute());
    }
}
