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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Issue #309（T109）：騎士 5、黑暗妖精 16、王族 6、龍騎士 12、幻術師 23，共 62 個技能的行為對照 1-伺服器。
 *
 * <p>輸入 {@code server1/skills-others-62.tsv}：1-伺服器 {@code db/380_db.sql} {@code skills} 表的 16 欄
 * （{@code tools/skill-targeting/extract_others.py} 唯讀擷取，欄位與 T107／T108 相同）。期望值一律由這張表、
 * {@code skills_buff_ability}、{@code skillmode/*.java} 或 {@code ConfigSkill} 的數值另寫，不讀 LiTo 自己的常數。
 * 逐技能對照見 {@code docs/design/skills-others.md}。</p>
 */
final class OtherClassSkillServer1ParityTest {
    record Row(int skillId, int skillLevel, int mp, int hp, int itemId, int itemCount, int reuseMillis,
               int buffSeconds, int damageValue, int damageDice, int damageDiceCount, int probabilityValue,
               int probabilityDice, int attr, int type, int lawful) {
        @Override public String toString() { return "skill" + skillId; }
    }

    /** combat-coverage-current.md 的 SQL 分組（name≠none）。 */
    private static final Set<Integer> EXPECTED_IDS = Set.copyOf(Stream.of(
            IntStream.rangeClosed(87, 91), IntStream.rangeClosed(97, 118),
            IntStream.rangeClosed(181, 195), IntStream.rangeClosed(201, 220))
            .flatMapToInt(s -> s).boxed().toList());

    /**
     * S1 {@code L1SkillUse.useConsume}（:2047）每次施放都 {@code addLawful(skills.lawful)}；SQL lawful≠0 但 LiTo
     * 沒有套用的技能。#309 時是 {103}（BlindSpell 缺口），#325 補上 BlindSpell.lawfulDelta 後必須為空。
     */
    private static final Set<Integer> LAWFUL_NOT_APPLIED = Set.of();

    private static List<Row> rows;

    static Stream<Row> rows() {
        if (rows == null) rows = load();
        return rows.stream();
    }

    private static List<Row> load() {
        InputStream in = OtherClassSkillServer1ParityTest.class.getResourceAsStream("/server1/skills-others-62.tsv");
        assertNotNull(in, "missing server1/skills-others-62.tsv");
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

    @Test void fixtureCoversExactly62Skills() {
        List<Row> all = rows().toList();
        assertEquals(62, all.size());
        assertEquals(new TreeSet<>(EXPECTED_IDS), new TreeSet<>(all.stream().map(Row::skillId).toList()));
        for (Row r : all) assertEquals(((r.skillId() - 1) >> 3) + 1, r.skillLevel(), r.toString());
    }

    // ---- 反射工具（與 T107／T108 相同；另外支援包住 AttackSpell 的 192／202／203／207／208／213） ----

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
            return invokeNumber(record, c.getAccessor());
        }
        return null;
    }

    /** 不是 record 元件、但宣告為無參數 int 方法的值（例如包裝 record 的 skillId()、AttackSpell.hpCost()）。 */
    private static Long method(Object record, String name) {
        try {
            Method m = record.getClass().getDeclaredMethod(name);
            if (m.getReturnType() != int.class && m.getReturnType() != long.class) return null;
            return invokeNumber(record, m);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Long invokeNumber(Object record, Method accessor) {
        try {
            accessor.setAccessible(true);
            return ((Number) accessor.invoke(record)).longValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(record.getClass().getSimpleName() + "." + accessor.getName(), e);
        }
    }

    private static String where(Row r, Object record, String field) {
        return r + " " + record.getClass().getSimpleName() + "." + field;
    }

    /** S1 L1MagicMode.calcAttrResistance（:188-230）只處理 1/2/4/8；0 與 16 都是無屬性。 */
    private static long neutral(long attribute) {
        return attribute == BattleSpellCatalog.ATTRIBUTE_NONE ? BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED : attribute;
    }

    /**
     * 期限例外：S1 skillmode 自己決定時間、不用 buffDuration 的技能。
     * 208 BONE_BREAK.java:35 {@code time = 2}（SQL buffDuration 3；正式環境 CombatDatabase.value 也固定回 2）。
     */
    private static final Map<Integer, Integer> MODE_DURATION = Map.of(208, 2);

    /** 消耗（MP／HP／材料）、冷卻、期限：catalog 有效值（未載入 DB 時的後備值）與 380_db.skills 相同。 */
    @ParameterizedTest @MethodSource("rows")
    void consumptionCooldownAndDurationMatchSource(Row r) {
        List<Object> records = catalogRecords(r.skillId());
        assertFalse(records.isEmpty(), "no BattleSpellCatalog record for " + r);
        boolean mpSeen = false;
        boolean hpComponent = records.stream().anyMatch(x -> component(x, "hpCost") != null);
        for (Object record : records) {
            Long id = component(record, "skillId");
            if (id == null) id = method(record, "skillId");
            assertNotNull(id, where(r, record, "skillId"));
            assertEquals(r.skillId(), id.intValue(), where(r, record, "skillId"));
            Long mp = component(record, "mpCost");
            if (mp != null) {
                mpSeen = true;
                assertEquals(r.mp(), mp.longValue(), where(r, record, "mpCost"));
            }
            Long hp = hpComponent ? component(record, "hpCost") : method(record, "hpCost");
            if (hp != null) assertEquals(r.hp(), hp.longValue(), where(r, record, "hpCost"));
            Long reuse = component(record, "reuseDelayMillis");
            if (reuse != null) assertEquals(r.reuseMillis(), reuse.longValue(), where(r, record, "reuseDelayMillis"));
            Long duration = component(record, "durationSeconds");
            if (duration != null) {
                assertEquals(MODE_DURATION.getOrDefault(r.skillId(), r.buffSeconds()).longValue(),
                        duration.longValue(), where(r, record, "durationSeconds"));
            }
            Long item = component(record, "itemConsumeId");
            if (item != null) {
                assertEquals(r.itemId(), item.longValue(), where(r, record, "itemConsumeId"));
                Long count = component(record, "itemConsumeCount");
                assertNotNull(count, where(r, record, "itemConsumeCount"));
                assertEquals(r.itemCount(), count.longValue(), where(r, record, "itemConsumeCount"));
            }
        }
        assertTrue(mpSeen, r + " has no mpCost component");
        boolean hpSeen = records.stream().anyMatch(x -> (hpComponent ? component(x, "hpCost") : method(x, "hpCost")) != null);
        if (!hpSeen) assertEquals(0, r.hp(), r + " has no hpCost but SQL hp≠0");
        if (records.stream().noneMatch(x -> component(x, "itemConsumeId") != null)) {
            assertEquals(0, r.itemId(), r + " has no itemConsumeId but SQL itemConsumeId≠0");
        }
    }

    /** 傷害骰、屬性、type、正義值。202 混亂是 TYPE_CURSE（type 4），S1 不計算 damage_value 11。 */
    @ParameterizedTest @MethodSource("rows")
    void damageAttributeTypeAndLawfulMatchSource(Row r) {
        for (Object record : catalogRecords(r.skillId())) {
            Long base = component(record, "baseDamage");
            if (base != null && r.skillId() != BattleSpellCatalog.CONFUSION) {
                // L1MagicPc.calcMagicDiceDamage: Σ(nextInt(damage_dice)+1)×damage_dice_count + damage_value
                assertEquals(r.damageValue(), base.longValue(), where(r, record, "baseDamage"));
                assertEquals(r.damageDiceCount(), component(record, "diceCount").longValue(), where(r, record, "diceCount"));
                assertEquals(r.damageDice(), component(record, "dieSides").longValue(), where(r, record, "dieSides"));
            }
            Long damageValue = component(record, "damageValue");
            if (damageValue != null) assertEquals(r.damageValue(), damageValue.longValue(), where(r, record, "damageValue"));
            Long attribute = component(record, "attribute");
            if (attribute != null) assertEquals(r.attr(), neutral(attribute), where(r, record, "attribute"));
            Long type = component(record, "type");
            if (type != null) assertEquals(r.type(), type.longValue(), where(r, record, "type"));
            Long lawful = component(record, "lawfulDelta");
            if (lawful != null) assertEquals(r.lawful(), lawful.longValue(), where(r, record, "lawfulDelta"));
        }
    }

    /** SQL lawful≠0 而 LiTo 沒有套用的技能必須正好是 LAWFUL_NOT_APPLIED。 */
    @Test void lawfulGapListIsExact() {
        Set<Integer> missing = new TreeSet<>();
        for (Row r : rows().toList()) {
            if (r.lawful() == 0) continue;
            boolean applied = catalogRecords(r.skillId()).stream()
                    .anyMatch(record -> component(record, "lawfulDelta") != null);
            if (!applied) missing.add(r.skillId());
        }
        assertEquals(new TreeSet<>(LAWFUL_NOT_APPLIED), missing);
    }

    /** 208 骷髏毀壞：SQL lawful=0（45 是 probability_value），施放不改正義值；期限由 mode 固定 2 秒。 */
    @Test void boneBreakDoesNotChangeLawfulAndKeepsModeDuration() {
        var spell = BattleSpellCatalog.boneBreakSpell(BattleSpellCatalog.BONE_BREAK);
        assertEquals(row(208).lawful(), spell.lawfulDelta());
        assertEquals(45, row(208).probabilityValue());
        assertEquals(2, spell.durationSeconds());          // skillmode/BONE_BREAK.java:35
        assertEquals(800, spell.failureThreshold());       // :32-34 nextInt(1000)+1 > 800
        assertEquals(1_000, spell.maximumRoll());
    }

    /** 108 會心一擊：U:2036-2038 施放後 HP=1、MP=0；U:1974 HP 必須 ≥ hpConsume+1。 */
    @Test void finalBurnCommitsHpOneAndMpZero() {
        var spell = BattleSpellCatalog.attackSpell(108);
        assertTrue(spell.finalBurn());
        assertEquals(row(108).hp(), spell.hpCost());
        assertEquals(row(108).mp(), spell.mpCost());
    }

    /** 202 混亂：TYPE_CURSE 不算傷害，但 catalog 保留 SQL damage_value 供對照。 */
    @Test void confusionKeepsDormantDatabaseDamageValue() {
        var spell = BattleSpellCatalog.confusionSpell(BattleSpellCatalog.CONFUSION);
        assertEquals(row(202).type(), 4);
        assertEquals(row(202).damageValue(), spell.dormantDatabaseDamageValue());
        assertEquals(0, spell.attackSpell().baseDamage());
    }

    // ---- MP 消耗公式（L1SkillUse.isHPMPConsume :1869-1971；騎士 87..91 吃 INT−12） ----

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

    /** L1PcOriginal.resetOriginalMagicConsumeReduction（:1013-1062）：王族 0、騎士 1、黑妖 4、龍騎 5、幻術 6。 */
    private static int refOriginalReduction(int type, int originalInt) {
        return switch (type) {
            case 0 -> originalInt == 11 || originalInt == 12 ? 1 : originalInt >= 13 ? 2 : 0;
            case 1 -> originalInt == 9 || originalInt == 10 ? 1 : originalInt >= 11 ? 2 : 0;
            case 4 -> originalInt == 13 || originalInt == 14 ? 1 : originalInt >= 15 ? 2 : 0;
            case 6 -> originalInt == 14 ? 1 : originalInt >= 15 ? 2 : 0;
            default -> 0;
        };
    }

    @Test void originalIntelligenceReductionMatchesL1PcOriginal() {
        for (int type : new int[] {0, 1, 4, 5, 6}) {
            for (int originalInt = 0; originalInt <= 30; originalInt++) {
                assertEquals(refOriginalReduction(type, originalInt),
                        NativeSourceSkillConsumptionMath.originalReduction(type, originalInt),
                        "type=" + type + " int=" + originalInt);
            }
        }
    }

    // ---- 效果數值：skills_buff_ability 與 skillmode ----

    /** 1-伺服器 db skills_buff_ability 的 15 個技能列（DMG 只算近戰，BOW_DMG 另計：L1PcBuffAbilityCalc.java:294-295）。 */
    @Test void buffAbilityFallbacksMatchSkillsBuffAbility() {
        var bounce = BattleSpellCatalog.bounceAttackSpell(89);                  // (89,AC,-2)(89,HIT,6)
        assertEquals(-2, bounce.armorClassModifier());
        assertEquals(6, bounce.meleeHitModifier());
        assertEquals(-3, BattleSpellCatalog.shieldSpell(99).armorClassModifier());   // (99,AC,-3)
        assertEquals(5, BattleSpellCatalog.itemWeaponEnchantSpell(107).meleeDamage()); // (107,DMG,5)
        var mighty = BattleSpellCatalog.abilityBuffSpell(109);                  // (109,STR,2)
        assertEquals(BattleSpellCatalog.Ability.STRENGTH, mighty.ability());
        assertEquals(2, mighty.modifier());
        var dexterity = BattleSpellCatalog.abilityBuffSpell(110);               // (110,DEX,2)
        assertEquals(BattleSpellCatalog.Ability.DEXTERITY, dexterity.ability());
        assertEquals(2, dexterity.modifier());
        var glowing = BattleSpellCatalog.royalAuraSpell(114);                   // (114,HIT,5)(114,BOW_HIT,5)；U:3384 addMr(20)
        assertEquals(5, glowing.meleeHit());
        assertEquals(5, glowing.rangedHit());
        assertEquals(20, glowing.magicResistance());
        assertEquals(-8, BattleSpellCatalog.royalAuraSpell(115).armorClass());   // (115,AC,-8)
        assertEquals(5, BattleSpellCatalog.royalAuraSpell(117).meleeDamage());   // (117,DMG,5)
        assertEquals(15, BattleSpellCatalog.guardBrakeSpell(183).armorClassModifier()); // (183,AC,15)
        assertEquals(0, BattleSpellCatalog.guardBrakeSpell(193).armorClassModifier());  // 193 沒有能力值列
        var antharas = BattleSpellCatalog.awakenSpell(185);                     // (185,AC,-12)(185,HP,127)
        assertEquals(-12, antharas.armorClassModifier());
        assertEquals(127, antharas.maximumHpBonus());
        assertEquals(5, BattleSpellCatalog.awakenSpell(195).allAbilityScoreBonus()); // (195,STR/DEX/CON/WIS/INT/CHA,5)
        var ogre = BattleSpellCatalog.illusionOgreSpell(204);                   // (204,DMG,4)(204,HIT,4)
        assertEquals(4, ogre.meleeDamage());
        assertEquals(4, ogre.meleeHit());
        assertEquals(-2, BattleSpellCatalog.armBreakerSpell(213).meleeDamageModifier()); // (213,DMG,-2)
        assertEquals(-20, BattleSpellCatalog.diamondGolemSpell(214).armorClass());       // (214,AC,-20)
        var avatar = BattleSpellCatalog.illusionAvatarSpell(219);               // (219,DMG,10)(219,BOW_DMG,10)
        assertEquals(10, avatar.meleeDamage());
        assertEquals(10, avatar.rangedDamage());
    }

    /** skillmode 與 L1PcInstance／L1Attack* 內的固定數值。 */
    @Test void skillModeConstantsMatchSource() {
        var fafurion = BattleSpellCatalog.awakenSpell(190);           // AWAKEN_FAFURION.java:42-47
        assertEquals(30, fafurion.magicResistanceModifier());
        assertEquals(30, fafurion.allElementalResistance());
        assertEquals(1, BattleSpellCatalog.insightSpell(216).abilityScoreModifier());  // INSIGHT.java:29-33
        assertEquals(-1, BattleSpellCatalog.panicSpell(217).abilityScoreModifier());   // PANIC.java:29-33
        assertEquals(5, BattleSpellCatalog.dodgeBuffSpell(106).dodgeModifier());       // UNCANNY_DODGE.java:26
        assertEquals(5, BattleSpellCatalog.dodgeBuffSpell(201).dodgeModifier());       // MIRROR_IMAGE.java:26
        assertEquals(5, BattleSpellCatalog.resistFearSpell(188).dodgeDownModifier());  // RESIST_FEAR.java:26
        assertEquals(2, BattleSpellCatalog.illusionLichSpell(209).spellPower());       // ILLUSION_LICH.java:29
        assertEquals(8, BattleSpellCatalog.illusionAvatarSpell(219).spellPower());     // ILLUSION_AVATAR.java:31
        assertEquals(15, BattleSpellCatalog.solidCarriageSpell(90).evasionRateModifier());      // L1PcInstance.java:4720-4721
        assertEquals(12, BattleSpellCatalog.rangedEvasionBuffSpell(111).evasionRateModifier()); // L1PcInstance.java:4716-4717
        var reduction = BattleSpellCatalog.reductionArmorSpell(88);   // L1AttackPc.java:1021-1023、L1AttackNpc.java:338-340
        assertEquals(50, reduction.levelThreshold());
        assertEquals(5, reduction.levelDivisor());
        assertEquals(10, reduction.playerPhysicalBaseReduction());
        assertEquals(1, reduction.otherBaseReduction());
        var venom = BattleSpellCatalog.enchantVenomSpell(98);         // L1AttackPc.java:1525-1528：10%、每 3 秒 HP-5
        assertEquals(10, venom.procPercent());
        assertEquals(3_000, venom.tickIntervalMillis());
        assertEquals(5, venom.tickDamage());
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, BattleSpellCatalog.movementBuffSpell(101).braveMode()); // U:3779-3782 setBraveSpeed(4)
    }

    /**
     * ConfigSkill 預設值（1-伺服器 ConfigSkill.java:93-133 {@code _loadFromProperties} 的缺鍵預設）。
     * 正式環境 LiTo 經 {@code config/combat.properties:5} 直接讀同一份 {@code 4-職業_技能設定表.properties}，
     * 缺鍵時用 {@link NativeSourceSkillDefaults}，必須等於 S1 的缺鍵預設。
     */
    @Test void missingConfigSkillKeysUseSourceDefaults() {
        Map<String, String> source = Map.ofEntries(
                Map.entry("ILLUSION_AVATAR_DAMAGE_RATE", "1.5"), Map.entry("JOY_OF_PAIN_COUNTDMG", "3"),
                Map.entry("JOY_OF_PAIN_DMG", "1500"), Map.entry("SHOCK_STUN_TIMER", "2,2"),
                Map.entry("COUNTER_BARRIER_DMG", "1.5"), Map.entry("SOLID_CARRIAGE_MODE", "1"),
                Map.entry("BURNING_CHANCE", "33"), Map.entry("BURNING_DMG", "1.5"),
                Map.entry("DOUBLE_BREAK_CHANCE", "15"), Map.entry("DOUBLE_BREAK_DMG", "1.5"),
                Map.entry("FOE_SLAYER_DMG", "10"), Map.entry("VULNERABILITY_ROM", "90"),
                Map.entry("VULNERABILITY_1", "10"), Map.entry("VULNERABILITY_2", "20"),
                Map.entry("VULNERABILITY_3", "30"), Map.entry("SLAY_BREAK_TIMER", "4,0"),
                Map.entry("MORTAL_BODY_ROM", "23"), Map.entry("MORTAL_BODY_DMG", "30"));
        for (var e : source.entrySet()) {
            assertEquals(e.getValue(), NativeSourceSkillDefaults.value(e.getKey()), e.getKey());
            assertTrue(CombatRules.SKILL_KEYS.contains(e.getKey()), e.getKey());
        }
    }

    /** 102／105 觸發倍率的離線後備值等於 S1 設定檔（BURNING 33／1.5、DOUBLE_BREAK 15／1.5）。 */
    @Test void physicalProcFallbacksMatchConfigSkill() {
        assertEquals(33, BattlePhysicalProcBuff.BURNING_SPIRIT_CHANCE);
        assertEquals(1.5D, BattlePhysicalProcBuff.BURNING_SPIRIT_MULTIPLIER);
        assertEquals(15, BattlePhysicalProcBuff.DOUBLE_BRAKE_CHANCE);
        assertEquals(1.5D, BattlePhysicalProcBuff.DOUBLE_BRAKE_MULTIPLIER);
        assertEquals(1.0D, BattleSpellCatalog.counterBarrierSpell(91).damageMultiplier()); // 設定檔 COUNTER_BARRIER_DMG = 1.0
        assertEquals(20, BattleSpellCatalog.mortalBodySpell(191).baseReflectionDamage()); // 設定檔 MORTAL_BODY_DMG = 20
    }

    /** 192 奪命之雷：設定檔 SLAY_BREAK_TIMER = 2,2 → 2..4 秒（L1SkillUse.java:3833-3838）。 */
    @Test void thunderGrabBindMatchesConfiguredSlayBreakTimer() {
        var spell = BattleSpellCatalog.thunderGrabSpell(BattleSpellCatalog.THUNDER_GRAB);
        assertEquals(2, spell.minimumBindSeconds());
        assertEquals(4, spell.maximumBindSeconds());
    }
}
