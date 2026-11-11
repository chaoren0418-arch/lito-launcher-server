package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Issue #336（T116）：法師技能效果數值與 1-伺服器 比對（設計文件 {@code docs/design/skills-wizard.md} §6）。
 *
 * <p>1-伺服器 參考（唯讀）：{@code src/com/lineage/server/model/skill/L1SkillUse.java}（{@code U}）、
 * {@code src/com/lineage/server/timecontroller/skill/EffectFirewallTimer.java}（{@code EFT}）、
 * {@code src/com/lineage/server/model/L1AttackList.java}（{@code AL}）、
 * {@code src/com/lineage/server/model/skill/skillmode/CANCELLATION.java}、{@code db/380_db.sql}。
 * 數值公式以 1-伺服器 的算式重寫成參照函式逐值比對；只能從原始碼確認的時序／條件以原始碼文字鎖住。</p>
 */
final class WizardSkillEffectServer1ParityTest {
    private static final Path MAIN = Path.of("src/main/java/local/lito381/server");

    private static String source(String name) throws Exception {
        return Files.readString(MAIN.resolve(name), StandardCharsets.UTF_8);
    }

    private static String method(String session, String signature) {
        int start = session.indexOf(signature);
        assertTrue(start >= 0, signature);
        int end = session.indexOf("\n   void ", start + signature.length());
        int alt = session.indexOf("\n    private ", start + signature.length());
        if (end < 0 || alt >= 0 && alt < end) end = alt;
        return session.substring(start, end < 0 ? session.length() : end);
    }

    // ---- 10／28 吸血 ------------------------------------------------------------------

    /** {@code U:3034-3035}：只有 CHILL_TOUCH(10)、VAMPIRIC_TOUCH(28) 把傷害轉為施法者回血。 */
    @Test void lifeDrainSkillsAreExactlyChillTouchAndVampiricTouch() {
        Set<Integer> draining = new HashSet<>();
        for (int skill = 1; skill <= 250; skill++) {
            BattleSpellCatalog.AttackSpell spell = BattleSpellCatalog.attackSpell(skill);
            if (spell != null && spell.drainsLife()) draining.add(skill);
        }
        assertEquals(Set.of(10, 28), draining);
    }

    /**
     * {@code U:3035} {@code heal = this._dmg}（整筆計算傷害，不是目標實際扣掉的 HP），
     * 傷害被接受後 {@code U:781-782}／{@code U:898-899} {@code adjustCurrentHpIfAlive(heal)}（死亡不回、上限 maxHp）。
     */
    @Test void lifeDrainHealsTheFullCalculatedDamageCappedAtMaximumHp() throws Exception {
        String session = source("ClientSession.java");
        String drain = method(session, "private int applyAcceptedLifeDrain(");
        assertTrue(drain.contains("if (!spell.drainsLife() || damage <= 0) return 0;"));
        assertTrue(drain.contains("if (playerHp <= 0) return 0;"), "dead caster is not healed");
        assertTrue(drain.contains("Math.min((long) playerMaxHp,"));
        assertTrue(drain.contains("Math.addExact((long) playerHp, (long) damage)"));
        // 戰鬥玩家目標：只有目標 HP 真的下降（傷害被接受）才回血，回血量是整筆 var54。
        assertTrue(Pattern.compile("if \\(var13\\.playerHp < var68\\) \\{\\s*"
                + "var96 = this\\.applyAcceptedLifeDrain\\(var2, var54, \"battle-player\"\\);")
                .matcher(session).find());
    }

    // ---- 39 魔力奪取 -------------------------------------------------------------------

    /** {@code U:3169-3174}：{@code nextInt(10) + 5 + user.getInt() / 2}，再夾到目標目前 MP。 */
    private static int sourceManaDrain(int roll0to9, int intelligence, int targetMp) {
        int drain = roll0to9 + 5 + intelligence / 2;
        return targetMp < drain ? targetMp : drain;
    }

    @Test void manaDrainAmountMatchesL1SkillUseForEveryRollAndIntelligence() {
        for (int intelligence = 0; intelligence <= 127; intelligence++) {
            for (int roll = 0; roll <= 9; roll++) {
                final int d10 = roll + 1;
                int requested = BattleManaDrain.requestedDrain(intelligence, sides -> {
                    assertEquals(10, sides);
                    return d10;
                });
                for (int targetMp : new int[] {0, 1, 7, 30, 1000}) {
                    assertEquals(sourceManaDrain(roll, intelligence, targetMp),
                            Math.min(targetMp, requested),
                            "int=" + intelligence + " roll=" + roll + " mp=" + targetMp);
                }
            }
        }
    }

    /** 玩家目標：先夾目標 MP，施法者只加實際吸到的量（{@code MagicHitCommitPlan.applyManaDrain} 的 actual）。 */
    @Test void manaDrainTransfersOnlyTheClampedAmount() throws Exception {
        String session = source("ClientSession.java");
        String body = method(session, "void nativeSourceRemainingOriginal39(");
        int clamp = body.indexOf("var49 = Math.min(var50, var48);");
        int credit = body.indexOf("this.playerMp = NativeSkillConsumption.boundMp(this, Math.min(this.playerMaxMp, this.playerMp + var49));");
        assertTrue(clamp > 0 && credit > clamp, "clamp=" + clamp + " credit=" + credit);
        // 吸取量用的 INT 與 MP 消耗同一個有效 INT（裝備＋增益），對應 S1 getInt()。
        assertTrue(body.contains("BattleManaDrain.requestedDrain(var26,"));
        assertTrue(body.contains("NativeSkillConsumptionBridges.ManaDrainSpell_effectiveMpCost_III(var2, var26, var27)"));
    }

    // ---- 58 火牢 ----------------------------------------------------------------------

    /** {@code EFT:244-247}（PC）／{@code :197-200}（NPC）：火屬性只有正值才減傷，{@code floor(0.32*|fire|)/32}。 */
    private static int sourceFireWall(int sourceDamage, int fire) {
        double attrDeffence = 0;
        if (fire > 0) {
            int resistFloor = (int) (0.32 * Math.abs(fire));
            attrDeffence = resistFloor / 32.0;
        }
        return Math.max((int) ((1.0 - attrDeffence) * sourceDamage), 0);
    }

    @Test void fireWallResistanceArithmeticMatchesEffectFirewallTimer() {
        for (int intelligence = 1; intelligence <= 127; intelligence++) {
            int bound = NativeSourceGroundNpc.bound(intelligence);
            assertEquals(Math.max(intelligence / 2, 1), bound, "EFT:244 nextInt bound int=" + intelligence);
            for (int roll = 0; roll < bound; roll += Math.max(1, bound / 7)) {
                int sourceDamage = 19 + roll;
                for (int fire = -128; fire <= 127; fire++) {
                    int expected = sourceFireWall(sourceDamage, fire);
                    assertEquals(expected, BattleElementalResistance.fireWallDamage(sourceDamage, fire),
                            "src=" + sourceDamage + " fire=" + fire);
                    assertEquals(expected, NativeSourceGroundPlayer.resolve(sourceDamage, fire, Set.of()),
                            "source PC path src=" + sourceDamage + " fire=" + fire);
                }
            }
        }
    }

    /** {@code AL:256-286} SKD3：料理 -5／-2、181 -10、211 -15、68 減半（最後才除 2）。 */
    @Test void fireWallPlayerModifiersMatchSkd3() {
        List<Integer> minusFive = new ArrayList<>();
        for (int id = 3008; id <= 3015; id++) minusFive.add(id);
        for (int id = 3024; id <= 3031; id++) minusFive.add(id);
        for (int id = 3040; id <= 3047; id++) minusFive.add(id);
        for (int id : minusFive) assertEquals(-5, NativeSourceGroundPlayer.modifier(id), "cooking " + id);
        for (int id = 3048; id <= 3051; id++) assertEquals(-2, NativeSourceGroundPlayer.modifier(id), "cooking " + id);
        assertEquals(-10, NativeSourceGroundPlayer.modifier(181));
        assertEquals(-15, NativeSourceGroundPlayer.modifier(211));
        for (int id : new int[] {3000, 3007, 3016, 3023, 3032, 3039, 3052, 68, 4000}) {
            assertEquals(0, NativeSourceGroundPlayer.modifier(id), "no SKD3 entry " + id);
        }
        // (30 - 5 - 10) / 2 = 7；S1 先加總 SKD3，再因 IMMUNE_TO_HARM 除 2。
        assertEquals(7, NativeSourceGroundPlayer.resolve(30, 0, List.of(3008, 181, 68)));
        assertEquals(0, NativeSourceGroundPlayer.resolve(19, 0, List.of(3008, 181, 211)));
    }

    /** S1 擲骰用 {@code user.getInt()}（有效 INT）；舊路徑原本讀資料庫基礎 INT（本 PR 修正）。 */
    @Test void fireWallTicksUseEffectiveIntelligence() throws Exception {
        String session = source("ClientSession.java");
        String roll = method(session, "private int fireWallDamage() {");
        assertTrue(roll.contains("intelligence = nativeSkillConsumptionIntelligence();"));
        assertTrue(roll.contains("19 + ThreadLocalRandom.current().nextInt(NativeSourceGroundNpc.bound(intelligence))"));
        assertTrue(source("NativeSourceGroundPlayer.java")
                .contains("int intelligence=caster.nativeSkillConsumptionIntelligence();"));
        assertTrue(source("ClientSession.java").contains(
                "int nativeSkillConsumptionIntelligence() {\n"
                        + "        return this.requiredEquipmentMagicStats().effectiveIntelligence(this.baseIntelligence());"));
    }

    // ---- 29／76 緩速免疫 ---------------------------------------------------------------

    /**
     * {@code U:3053-3061}：目標裝備加速道具（{@code getHasteItemEquipped() > 0}）或強化勇水（braveSpeed 5）時，
     * 緩速術／集體緩速術／藤蔓術直接略過。三個 LiTo 本體都要在 {@code activateSlow} 之前擋下。
     */
    @Test void slowSpellsSkipHasteItemAndStrengthenedBraveTargets() throws Exception {
        String session = source("ClientSession.java");
        String battle = method(session, "void nativeAreaBattleSlowBody(");
        int battleGate = battle.indexOf("var38.inventory.hasEquippedHasteArmor() || var38.timedStatus.brave(var63).mode() == 5");
        int battleSlow = battle.indexOf("var38.timedStatus.activateSlow(");
        assertTrue(battleGate > 0 && battleSlow > battleGate, "battle gate=" + battleGate + " slow=" + battleSlow);

        String world = method(session, "void nativeAreaWorldSlowBody(");
        int worldGate = world.indexOf("!(!var43.inventory.hasEquippedHasteArmor() && var43.timedStatus.brave(var74).mode() != 5)");
        int worldSlow = world.indexOf("var43.timedStatus.activateSlow(");
        assertTrue(worldGate > 0 && worldSlow > worldGate, "world gate=" + worldGate + " slow=" + worldSlow);

        String entangle = method(session, "void nativeMaterialEntangleBody(");
        int entangleGate = entangle.indexOf("var60.inventory.hasEquippedHasteArmor() || var60.timedStatus.brave(var72).mode() == 5");
        int entangleSlow = entangle.indexOf("var60.timedStatus.activateSlow(");
        assertTrue(entangleGate > 0 && entangleSlow > entangleGate, "entangle gate=" + entangleGate + " slow=" + entangleSlow);
        assertTrue(entangle.contains("!var78"), "immune player target must not be slowed");
    }

    /** 29 與 76 都走這三個本體；29 是 EntangleSpell（{@code BattleSpellCatalog} 29），76 是 MassSlowSpell。 */
    @Test void slowSkillsRouteThroughTheGatedBodies() {
        assertEquals(29, BattleSpellCatalog.entangleSpell(29).skillId());
        assertEquals(76, BattleSpellCatalog.massSlowSpell(76).skillId());
    }

    // ---- 44 魔法相消 -------------------------------------------------------------------

    /**
     * 1-伺服器 {@code L1SkillMode.isNotCancelable}（{@code L1SkillMode.java:144-155}）讀
     * {@code skills_相消免疫技能}（{@code SkillNotCancelableTable.java:51}）；LiTo 正式環境讀同一張表，
     * 且同樣只收 {@code skills} 表存在的技能。
     */
    @Test void cancellationImmuneListComesFromTheSameTable() throws Exception {
        String controls = source("SourceSkillControls.java");
        assertTrue(controls.contains(
                "SELECT c.skill_id FROM `skills_相消免疫技能` c INNER JOIN skills s ON s.skill_id=c.skill_id"));
    }

    /**
     * {@code db/380_db.sql:48847-48858} 的 12 筆中，法師相關 12／21／78／79 在 LiTo 存在不被 44 清除的獨立狀態；
     * 被清除的狀態（8／48 武器、3／26／42 能力、43／54 加速）都不在清單內。
     */
    @Test void wizardCancellationSurvivorsMatchTheSourceTable() throws Exception {
        String session = source("ClientSession.java");
        String body = method(session, "void nativeMaterialCancellationBody(");
        // 清除項：BattleWeaponBuffStatus（8、48）、BattleAbilityStatus（3、26、42…）、速度（43、54、29、76）。
        assertTrue(body.contains("var5.weaponBuffStatus.cancelMagicEffects()"));
        assertTrue(body.contains("var5.abilityStatus.cancelMagicEffects()"));
        assertTrue(body.contains("NativeCancellationSpeedImmunity.magic(var10000, var5, var3)"));
        // 存活項：12／107 道具武器附魔、21 道具防具附魔、78 絕對屏障、79 靈魂昇華不在清除清單。
        assertFalse(body.contains("itemWeaponEnchantStatus.cancel"));
        assertFalse(body.contains("itemArmorEnchantStatus.cancel"));
        assertFalse(body.contains("absoluteBarrier"));
        assertFalse(body.contains("advanceSpirit"));
    }

    // ---- 66 沉睡之霧自施 ---------------------------------------------------------------

    /**
     * {@code U:4079-4083}：66 以自己為主目標 {@code isTargetCalc} 失敗 → {@code U:2779} 返回（扣費、不生效）；
     * 範圍內逐目標時自己也被排除（{@code U:2833}）。LiTo 目前在扣費前就拒絕（不扣 MP），
     * 逐目標同樣排除自己。差異列「待使用者裁定」，本測試鎖住 LiTo 現況。
     */
    @Test void fogOfSleepingSelfPrimaryIsRejectedBeforeCostAndSelfIsNeverATarget() throws Exception {
        String session = source("ClientSession.java");
        String battle = method(session, "void nativeAreaBattleSleepBody(");
        int primaryGate = battle.indexOf("&& var13.player() != this");
        int rejected = battle.indexOf("\"[LiTo] C_USE_SPELL sleep rejected target/gate=\"");
        int perTarget = battle.indexOf("&& var21 != this");
        assertTrue(primaryGate > 0 && rejected > primaryGate && perTarget > rejected,
                "battle primary=" + primaryGate + " rejected=" + rejected + " perTarget=" + perTarget);

        String world = method(session, "void nativeAreaWorldSleepBody(");
        int noPrimary = world.indexOf("classic-world sleep rejected primary no longer eligible=");
        int worldCost = world.indexOf("NativeSkillConsumptionBridges.SleepSpell_effectiveMpCost_III(");
        int worldSelf = world.indexOf("&& var21 != this");
        assertTrue(noPrimary > 0 && worldCost > noPrimary && worldSelf > 0,
                "world primary=" + noPrimary + " cost=" + worldCost + " self=" + worldSelf);
    }
}
