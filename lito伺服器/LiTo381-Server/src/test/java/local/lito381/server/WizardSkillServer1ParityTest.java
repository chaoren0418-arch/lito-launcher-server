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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Issue #307（T107）：共通魔法／法師 79 個技能（1..80 去掉保留槽 24）的行為對照 1-伺服器。
 *
 * <p>輸入 {@code server1/skills-wizard-79.tsv}：1-伺服器 {@code db/380_db.sql} {@code skills} 表的
 * 消耗、冷卻、期限、傷害骰、機率骰、屬性、正義值欄位（{@code tools/skill-targeting/extract_wizard.py}
 * 唯讀擷取）。期望值一律由這張表或本檔依 1-伺服器 原始碼另寫的 {@code ref*} 函式產生，
 * 不讀 LiTo 自己的常數。逐技能對照見 {@code docs/design/skills-wizard.md}。</p>
 */
final class WizardSkillServer1ParityTest {
    record Row(int skillId, int skillLevel, int mp, int hp, int itemId, int itemCount, int reuseMillis,
               int buffSeconds, int damageValue, int damageDice, int damageDiceCount, int probabilityValue,
               int probabilityDice, int attr, int type, int lawful) {
        @Override public String toString() { return "skill" + skillId; }
    }

    /** 沒有 BattleSpellCatalog 紀錄的技能：5 指定傳送走獨立處理器（ClientSession 直接讀 skills 第 5 列）。 */
    private static final Set<Integer> NO_CATALOG_RECORD = Set.of(5);

    /**
     * S1 {@code L1SkillUse.useConsume}（:2047）每次施放都 {@code addLawful(skills.lawful)}；
     * 下列技能 SQL lawful≠0，但 LiTo 對應的 catalog 紀錄沒有正義值欄位、處理器也不呼叫
     * {@code applySpellLawfulDelta}——已知不一致，未在本 PR 修改（見 skills-wizard.md §4）。
     * 清單必須正好等於目前的差距，修好就要移除。
     */
    private static final Set<Integer> LAWFUL_NOT_APPLIED = Set.of(10, 20, 27, 28, 40, 41, 66, 77);

    private static List<Row> rows;

    static Stream<Row> rows() {
        if (rows == null) rows = load();
        return rows.stream();
    }

    private static List<Row> load() {
        InputStream in = WizardSkillServer1ParityTest.class.getResourceAsStream("/server1/skills-wizard-79.tsv");
        assertNotNull(in, "missing server1/skills-wizard-79.tsv");
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

    @Test void fixtureCoversExactly79WizardSkills() {
        List<Row> all = rows().toList();
        assertEquals(79, all.size());
        for (int id = 1; id <= 80; id++) {
            final int skill = id;
            assertEquals(id != 24, all.stream().anyMatch(r -> r.skillId() == skill), "skill" + id);
        }
        // skill_level 是 ((id-1)>>3)+1（每 8 個一級）
        for (Row r : all) assertEquals(((r.skillId() - 1) >> 3) + 1, r.skillLevel(), r.toString());
    }

    // ---- 反射工具（與 SkillTargetingServer1ParityTest 相同做法） ----

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
        if (NO_CATALOG_RECORD.contains(r.skillId())) {
            assertTrue(records.isEmpty(), r + " unexpectedly has catalog records");
            return;
        }
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

    /** 效果數值：傷害骰／治癒骰、屬性、機率骰、正義值。 */
    @ParameterizedTest @MethodSource("rows")
    void damageProbabilityAttributeAndLawfulMatchSource(Row r) {
        for (Object record : catalogRecords(r.skillId())) {
            String where = r + " " + record.getClass().getSimpleName();
            boolean damage = component(record, "baseDamage") != null;
            boolean heal = component(record, "baseDiceCount") != null;
            if (damage) {
                // L1MagicPc.calcMagicDiceDamage: Σ(nextInt(damage_dice)+1)×damage_dice_count + damage_value
                same(record, "baseDamage", r.damageValue(), r);
                same(record, "diceCount", r.damageDiceCount(), r);
                same(record, "dieSides", r.damageDice(), r);
            }
            if (heal) {
                // L1MagicPc.calcHealing: (damage_value + MB) d damage_dice
                same(record, "baseDiceCount", r.damageValue(), r);
                same(record, "dieSides", r.damageDice(), r);
            }
            if (damage || heal) same(record, "attribute", r.attr(), r);
            same(record, "probabilityDice", r.probabilityDice(), r);
            same(record, "probabilityValue", r.probabilityValue(), r);
            Long lawful = component(record, "lawfulDelta");
            if (lawful != null) assertEquals(r.lawful(), lawful.longValue(), where + ".lawfulDelta");
        }
    }

    /** 正義值：SQL lawful≠0 而 LiTo 沒有套用的技能，必須正好是 LAWFUL_NOT_APPLIED（防止清單過期或新增缺口）。 */
    @Test void lawfulGapListIsExact() {
        Set<Integer> missing = new TreeSet<>();
        for (Row r : rows().toList()) {
            if (r.lawful() == 0 || NO_CATALOG_RECORD.contains(r.skillId())) continue;
            boolean applied = catalogRecords(r.skillId()).stream()
                    .anyMatch(record -> component(record, "lawfulDelta") != null);
            if (!applied) missing.add(r.skillId());
        }
        assertEquals(new TreeSet<>(LAWFUL_NOT_APPLIED), missing);
    }

    /** 77 究極光裂術：S1 checkUseSkill（L1SkillUse.java:565）正義值 < 500 不能施放。 */
    @Test void disintegrateRequiresLawful500() {
        assertEquals(500, BattleSpellCatalog.attackSpell(77).minimumLawful());
    }

    /** 期限特例：S1 addMagicList 跳過 33／11，由 L1CurseParalysis／L1DamagePoison 自己計時。 */
    @Test void paralysisAndPoisonTimingMatchSourceExecutors() {
        // skillmode/CURSE_PARALYZE.java:38-41：ConfigSkill CURSE_READY_TIME=5、CURSE_TIME=10（秒）
        var paralysis = BattleSpellCatalog.paralysisSpell(33);
        assertEquals(5_000, paralysis.warningDelayMillis());
        assertEquals(10_000, paralysis.paralysisDurationMillis());
        // L1SkillUse.java:3105 doInfection(user, cha, 3000, 5)；L1DamagePoison.java:147 STATUS_POISON 30000ms
        var poison = BattleSpellCatalog.poisonSpell(11);
        assertEquals(3_000, poison.tickIntervalMillis());
        assertEquals(5, poison.tickDamage());
        assertEquals(30, poison.durationSeconds());
    }

    // ---- MP 消耗公式 ----

    /** 依 S1 L1SkillUse.isHPMPConsume（:1869-1971）另寫：INT 減免、頭盔減半、最低 1、原始 INT 減免。 */
    private static int refMp(int skillId, int sqlMp, int intelligence, int originalReduction, boolean helm) {
        int mp = sqlMp;
        int[][] tiers = {{12, 8}, {13, 16}, {14, 23}, {15, 32}, {16, 40}, {17, 48}, {18, 56}};
        for (int[] t : tiers) if (intelligence > t[0] && skillId > t[1] && skillId <= 80) mp--;
        if (intelligence > 12 && skillId >= 87 && skillId <= 91) mp -= intelligence - 12;
        // :1919-1962 頭盔：26／43／1／19／12／13／42／54
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

    /** S1 L1PcOriginal.resetOriginalMagicConsumeReduction（:1012-1060），職業 0..6。 */
    private static int refOriginalReduction(int type, int originalInt) {
        return switch (type) {
            case 0 -> originalInt == 11 || originalInt == 12 ? 1 : originalInt >= 13 ? 2 : 0;
            case 1 -> originalInt == 9 || originalInt == 10 ? 1 : originalInt >= 11 ? 2 : 0;
            case 4 -> originalInt == 13 || originalInt == 14 ? 1 : originalInt >= 15 ? 2 : 0;
            case 6 -> originalInt == 14 ? 1 : originalInt >= 15 ? 2 : 0;
            default -> 0; // 妖精 2、法師 3、龍騎士 5
        };
    }

    @Test void originalIntelligenceReductionMatchesL1PcOriginal() {
        for (int type = 0; type <= 6; type++) {
            for (int originalInt = 0; originalInt <= 30; originalInt++) {
                assertEquals(refOriginalReduction(type, originalInt),
                        NativeSourceSkillConsumptionMath.originalReduction(type, originalInt),
                        "type=" + type + " originalInt=" + originalInt);
            }
        }
    }

    /** S1 :1964-1971：最低 1 在原始 INT 減免之前，所以 1 MP 的技能被減 2 會變 −1（不夾到 0）。 */
    @Test void minimumOneIsAppliedBeforeOriginalReduction() {
        assertEquals(-1, NativeSourceSkillConsumptionMath.mpCost(39, 1, 18, 2, false));
        assertEquals(refMp(39, 1, 18, 2, false), NativeSourceSkillConsumptionMath.mpCost(39, 1, 18, 2, false));
    }

    // ---- BUFF 互斥 ----

    /**
     * 速度：S1 skillmode/HASTE.java:31-61 與 L1SkillUse.java:3753-3777（43／54 在緩速中只解除緩速）、
     * :3050-3091（29／76 在加速中只解除加速），以及 ALWAYS_REPEATED_SKILLS {43,54,STATUS_HASTE}（:2512-2517）。
     */
    @Test void hasteAndSlowCancelEachOtherLikeSource() {
        long now = TimeUnit.SECONDS.toNanos(100);
        var status = new BattleTimedStatus();
        var haste = status.activateHaste(now, 1200, 43);
        assertEquals(BattleTimedStatus.HASTE, haste.type());
        assertEquals(43, status.hasteSkillId(now));

        // 54 覆蓋 43（同一 ALWAYS 群組）
        status.activateHaste(now, 2400, 54);
        assertEquals(54, status.hasteSkillId(now));
        assertEquals(2400, status.speed(now).remainingSeconds());

        // 加速中被緩速：兩者都解除，速度回到正常
        var slowOnHaste = status.activateSlow(now, 64, 29);
        assertEquals(BattleTimedStatus.NORMAL, slowOnHaste.type());
        assertEquals(54, slowOnHaste.stoppedSkillId());
        assertEquals(0, status.hasteSkillId(now));
        assertEquals(0, status.slowSkillId(now));
        assertEquals(BattleTimedStatus.NORMAL, status.speed(now).type());

        // 正常時緩速成立
        var slow = status.activateSlow(now, 64, 76);
        assertEquals(BattleTimedStatus.SLOW, slow.type());
        assertEquals(76, status.slowSkillId(now));

        // 緩速中加速：只解除緩速，不加速
        var hasteOnSlow = status.activateHaste(now, 1200, 43);
        assertEquals(BattleTimedStatus.NORMAL, hasteOnSlow.type());
        assertEquals(76, hasteOnSlow.stoppedSkillId());
        assertEquals(0, status.hasteSkillId(now));
        assertEquals(0, status.slowSkillId(now));
    }

    /** 期限：S1 setSkillEffect(id, buffDuration×1000)；到期那一刻（≥）即解除。 */
    @Test void hasteExpiresExactlyAtSqlDuration() {
        long now = TimeUnit.SECONDS.toNanos(10);
        var status = new BattleTimedStatus();
        status.activateHaste(now, row(43).buffSeconds(), 43);
        long end = now + TimeUnit.SECONDS.toNanos(row(43).buffSeconds());
        assertEquals(43, status.hasteSkillId(end - 1));
        assertFalse(status.expireHaste(end - 1));
        assertEquals(0, status.hasteSkillId(end));
        assertTrue(status.expireHaste(end));
    }

    /** 52 神聖疾走：S1 L1SkillUse.java:3779-3786 setBraveSpeed(4)，並與勇水同屬 ALWAYS 群組（:2513-2514）。 */
    @Test void holyWalkReplacesBravePotionWithBraveMode4() {
        var holyWalk = BattleSpellCatalog.movementBuffSpell(52);
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, holyWalk.braveMode());
        assertEquals(4, BattleTimedStatus.BRAVE_HOLY_WALK);
        long now = TimeUnit.SECONDS.toNanos(5);
        var status = new BattleTimedStatus();
        status.activateBrave(now, 300, BattleTimedStatus.BRAVE_FIRST);
        status.activateBrave(now, holyWalk.durationSeconds(), holyWalk.braveMode());
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, status.brave(now).mode());
        assertEquals(row(52).buffSeconds(), status.brave(now).remainingSeconds());
    }

    /**
     * ORIGINAL_REPEATED_SKILLS（L1SkillUse.java:2520-2527，SkillCoexistSet=0 時才生效；
     * 1-伺服器 db server_event 116 預設 1）：LiTo CombatBuffLeases.GROUPS 中，同組 ⇔ S1 同組。
     * 159 大地的祝福、168 鋼鐵防護（妖精）在 LiTo 是獨立狀態類別，不在 GROUPS，交給 T108。
     */
    @Test void buffReplacementGroupsMatchOriginalRepeatedSkills() throws Exception {
        List<Set<Integer>> source = List.of(Set.of(8, 48, 12), Set.of(3, 99, 151, 159, 168),
                Set.of(26, 110), Set.of(42, 109), Set.of(114, 115));
        Field field = CombatBuffLeases.class.getDeclaredField("GROUPS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, String> groups = (Map<Integer, String>) field.get(null);
        for (int a : groups.keySet()) {
            for (int b : groups.keySet()) {
                if (a == b) continue;
                boolean sameSource = source.stream().anyMatch(g -> g.contains(a) && g.contains(b));
                assertEquals(sameSource, groups.get(a).equals(groups.get(b)), a + " vs " + b);
            }
        }
        for (int wizard : List.of(3, 8, 12, 21, 26, 42, 48)) assertTrue(groups.containsKey(wizard), "skill" + wizard);
        assertFalse(groups.containsKey(159));
        assertFalse(groups.containsKey(168));
    }

    /** BUFF 能力值後備（未載入 DB 時）與 1-伺服器 db skills_buff_ability 相同。 */
    @Test void buffAbilityFallbacksMatchSkillsBuffAbility() {
        assertEquals(-2, BattleSpellCatalog.shieldSpell(3).armorClassModifier());          // (3,'AC',-2)
        var holy = BattleSpellCatalog.weaponBuffSpell(8);                                  // (8,DMG1,HIT1)
        assertEquals(1, holy.meleeHit());
        assertEquals(1, holy.meleeDamage());
        assertEquals(0, holy.rangedHit());
        assertEquals(0, holy.rangedDamage());
        var bless = BattleSpellCatalog.weaponBuffSpell(48);                                // (48,BOW_HIT2,DMG2,HIT2)
        assertEquals(2, bless.meleeHit());
        assertEquals(2, bless.meleeDamage());
        assertEquals(2, bless.rangedHit());
        assertEquals(0, bless.rangedDamage());
        assertEquals(2, BattleSpellCatalog.itemWeaponEnchantSpell(12).meleeDamage());      // (12,DMG2)
        assertEquals(-3, BattleSpellCatalog.itemArmorEnchantSpell(21).armorClass());       // (21,AC,-3)
        assertEquals(5, BattleSpellCatalog.abilityBuffSpell(26).modifier());               // (26,DEX,5)
        assertEquals(5, BattleSpellCatalog.abilityBuffSpell(42).modifier());               // (42,STR,5)
        var weakness = BattleSpellCatalog.weaknessSpell(47);                               // (47,DMG-5,HIT-1)
        assertEquals(-5, weakness.meleeDamageModifier());
        assertEquals(-1, weakness.meleeHitModifier());
        assertEquals(0, weakness.armorClassModifier());
        var berserk = BattleSpellCatalog.combatBuffSpell(55);                              // (55,AC10,DMG5,HIT2)
        assertEquals(10, berserk.armorClass());
        assertEquals(5, berserk.meleeDamage());
        assertEquals(2, berserk.meleeHit());
        var disease = BattleSpellCatalog.weaknessSpell(56);                                // (56,AC12,DMG-6)
        assertEquals(12, disease.armorClassModifier());
        assertEquals(-6, disease.meleeDamageModifier());
        assertEquals(0, disease.meleeHitModifier());
    }
}
