package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Issue #338 (T118): Dragon Eye +1 SP, the {@code L1AttackList.SKD3} reduction
 * list and the {@code SKM0} "damage is 0" list versus 1-伺服器 (read-only).
 * Line references are in {@code docs/design/formulas-magic.md} §1 M3/M11/M17.
 */
final class MagicReductionServer1ParityTest {

    // 1-伺服器 L1SkillId constants used by L1AttackList.load.
    private static final int ICE_LANCE = 50;
    private static final int IMMUNE_TO_HARM = 68;
    private static final int ABSOLUTE_BARRIER = 78;
    private static final int FREEZING_BLIZZARD = 80;
    private static final int EARTH_BIND = 157;
    private static final int DRAGON_SKIN = 181;
    private static final int FREEZING_BREATH = 194;
    private static final int PATIENCE = 211;
    private static final int STATUS_FREEZE = 4000;

    /** L1AttackList.java:256-286 SKD3 rows, verbatim (value == key means "halve"). */
    private static Map<Integer, Integer> refSkd3() {
        Map<Integer, Integer> skd3 = new HashMap<>();
        // COOKING_1_0_S..COOKING_1_6_S, COOKING_2_0_S..2_6_S, COOKING_3_0_S..3_6_S, then the three *_7_S desserts.
        for (int base : new int[] {3008, 3024, 3040}) {
            for (int i = 0; i <= 7; i++) skd3.put(base + i, -5);
        }
        for (int id = 3048; id <= 3051; id++) skd3.put(id, -2); // COOKING_4_0_N..COOKING_4_3_N
        skd3.put(DRAGON_SKIN, -10);
        skd3.put(PATIENCE, -15);
        skd3.put(IMMUNE_TO_HARM, IMMUNE_TO_HARM);
        return skd3;
    }

    /** L1AttackList.java:289-294 SKM0 keys (= L1Character.isSkillEffectPhysicalDamageZero:1802-1807). */
    private static final Set<Integer> REF_SKM0 =
            Set.of(STATUS_FREEZE, ABSOLUTE_BARRIER, ICE_LANCE, FREEZING_BLIZZARD, FREEZING_BREATH, EARTH_BIND);

    /** L1MagicPc.calcPcMagicDamage:879-907 SKD3 loop (sum, then {@code >> 1} if any halving key). */
    private static int refSkd3Apply(int dmg, Set<Integer> effects) {
        boolean halve = false;
        for (int key : effects) {
            Integer value = refSkd3().get(key);
            if (value == null) continue;
            if (value.equals(key)) halve = true;
            else dmg += value;
        }
        return halve ? dmg >> 1 : dmg;
    }

    private static Map<Integer, BattleCookingCatalog.Recipe> recipesByStatus() {
        Map<Integer, BattleCookingCatalog.Recipe> byStatus = new TreeMap<>();
        long[][] ranges = {{41277, 41292}, {49049, 49064}, {49244, 49259}, {49825, 49828}};
        for (long[] range : ranges) {
            for (long id = range[0]; id <= range[1]; id++) {
                var recipe = BattleCookingCatalog.find(id);
                assertNotNull(recipe, "cooking item " + id);
                assertEquals(null, byStatus.put(recipe.statusId(), recipe), "duplicate status " + recipe.statusId());
            }
        }
        return byStatus;
    }

    // ---------------------------------------------------------------- SKD3

    @Test
    void cookingReductionMatchesEverySkd3CookingRowAndNoOtherStatus() {
        var byStatus = recipesByStatus();
        assertEquals(52, BattleCookingCatalog.size());
        assertEquals(52, byStatus.size());
        var skd3 = refSkd3();
        for (int status = 3000; status <= 3051; status++) {
            var recipe = byStatus.get(status);
            assertNotNull(recipe, "status " + status);
            int expected = -skd3.getOrDefault(status, 0);
            assertEquals(expected, recipe.damageReduction(), "cooking status " + status);
        }
    }

    @Test
    void mainCourseAndDessertReductionsAddLikeTheSkd3Loop() {
        var byStatus = recipesByStatus();
        for (var main : byStatus.values()) {
            if (main.dessert()) continue;
            for (var dessert : byStatus.values()) {
                if (!dessert.dessert()) continue;
                var status = new BattleCookingStatus();
                status.activate(0L, main, main.durationSeconds());
                status.activate(0L, dessert, dessert.durationSeconds());
                int reduction = status.active(1L).damageReduction();
                int lito = 100 - reduction;
                assertEquals(refSkd3Apply(100, Set.of(main.statusId(), dessert.statusId())), lito,
                        "main=" + main.statusId() + " dessert=" + dessert.statusId());
            }
        }
    }

    @Test
    void dragonSkinPatienceAndImmuneToHarmMatchSkd3Literals() {
        var skd3 = refSkd3();
        assertEquals(-skd3.get(DRAGON_SKIN), CombatSourceReductions.SOURCE_DRAGON_SKIN);
        assertEquals(-skd3.get(PATIENCE), CombatSourceReductions.SOURCE_PATIENCE);
        // Without loaded 1-伺服器 rules the legacy literals stay in force.
        assertEquals(CombatRules.loaded() ? 15 : 2, CombatSourceReductions.patience(2));
        assertEquals(10, CombatSourceReductions.dragonSkin(10));
        // IMMUNE_TO_HARM is the only halving key; LiTo CombatPlayerMagic.reductions uses result >>= 1 last.
        for (int dmg = -40; dmg <= 200; dmg++) {
            int lito = dmg - 5 - CombatSourceReductions.SOURCE_DRAGON_SKIN - CombatSourceReductions.SOURCE_PATIENCE;
            lito >>= 1;
            assertEquals(refSkd3Apply(dmg, new TreeSet<>(Set.of(3008, DRAGON_SKIN, PATIENCE, IMMUNE_TO_HARM))), lito,
                    "dmg=" + dmg);
        }
    }

    @Test
    void skd3HasExactlyTheRowsCoveredByLiTo() {
        // 28 cooking + DRAGON_SKIN + PATIENCE + IMMUNE_TO_HARM; COOKING_*_N (3000..3007, 3016..3023, 3032..3039) are absent.
        var skd3 = refSkd3();
        assertEquals(31, skd3.size());
        for (int id = 3000; id <= 3007; id++) assertEquals(null, skd3.get(id));
        for (int id = 3016; id <= 3023; id++) assertEquals(null, skd3.get(id));
        for (int id = 3032; id <= 3039; id++) assertEquals(null, skd3.get(id));
    }

    // ---------------------------------------------------------------- SKM0

    @Test
    void damageZeroStatusesMapOntoLiToFreezeEarthBindAndAbsoluteBarrier() {
        // LiTo CombatPlayerMagic.protectedZero = absoluteBarrier || earthBind || freeze.
        assertEquals(ABSOLUTE_BARRIER, BattleSpellCatalog.ABSOLUTE_BARRIER);
        assertEquals(EARTH_BIND, BattleSpellCatalog.EARTH_BIND);
        // The three freeze spells all drive BattleTimedStatus.activateFreeze (the single freeze lease).
        for (int id : new int[] {ICE_LANCE, FREEZING_BLIZZARD, FREEZING_BREATH}) {
            assertNotNull(BattleSpellCatalog.freezeAttackSpell(id), "freeze spell " + id);
        }
        assertEquals(6, REF_SKM0.size());
        var timed = new BattleTimedStatus();
        long now = 1_000_000_000L;
        timed.activateFreeze(now, 3);
        timed.activateEarthBind(now, 3);
        timed.activateAbsoluteBarrier(now, 3);
        assertEquals(true, timed.freezeActive(now + 1));
        assertEquals(true, timed.earthBindActive(now + 1));
        assertEquals(true, timed.absoluteBarrierActive(now + 1));
    }

    // ---------------------------------------------------------------- Dragon Eye SP

    @Test
    void dragonEyeMagicSpMatchesGetTargetSp() {
        // L1MagicMode.getTargetSp:103-116: 6686 wind, 6687 life, 6689 image add +1; 6683/6684/6685/6688 do not.
        assertEquals(Set.of(6686, 6687, 6689), CombatSpecialBuffs.EYE_MAGIC_SP_BUFFS);
        assertEquals(6686, BattleDragonEyeCatalog.WIND_DRAGON_EYE_BUFF);
        assertEquals(6687, BattleDragonEyeCatalog.LIFE_DRAGON_EYE_BUFF);
        assertEquals(6689, BattleDragonEyeCatalog.IMAGE_DRAGON_EYE_BUFF);
        Map<Integer, Integer> expected = Map.of(6683, 0, 6684, 0, 6685, 0, 6686, 1, 6687, 1, 6688, 0, 6689, 1);
        for (var eye : BattleDragonEyeCatalog.all()) {
            int buff = eye.buffId();
            assertEquals(expected.get(buff), CombatSpecialBuffs.eyeMagicSpellPower(buff), "eye buff " + buff);
        }
        assertEquals(7, BattleDragonEyeCatalog.all().size());
    }

    @Test
    void dragonEyeSpRaisesTheIntelligenceTermByOne() {
        // spByItem + 1 feeds max(INT + spByItem - 12, 1) exactly like L1MagicPc.calcMagicDiceDamage:1035-1036.
        for (int intPlusSp = 0; intPlusSp <= 60; intPlusSp++) {
            for (int dice = 1; dice <= 60; dice += 7) {
                int withEye = BattleElementalResistance.magicDamage(dice, intPlusSp + CombatSpecialBuffs.eyeMagicSpellPower(6686), 0);
                int reference = (int) (dice * Math.max(1.0 + Math.max(intPlusSp + 1 - 12, 1) * 3.0 / 32.0, 0.0));
                assertEquals(reference, withEye, "int+sp=" + intPlusSp + " dice=" + dice);
            }
        }
    }
}
