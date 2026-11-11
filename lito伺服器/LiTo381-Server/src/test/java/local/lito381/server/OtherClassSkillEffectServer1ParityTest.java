package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Issue #337（T117）：騎士／黑妖／王族／龍騎士／幻術師技能效果數值與 1-伺服器 比對
 * （設計文件 {@code docs/design/skills-others.md} §6）。
 *
 * <p>1-伺服器 參考（唯讀）：{@code src/com/lineage/server/model/L1AttackPc.java}（{@code AP}）、
 * {@code src/com/lineage/server/model/L1AttackList.java}（{@code AL}）、
 * {@code src/com/lineage/server/model/L1MagicNpc.java}（{@code MN}）、
 * {@code src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java}（{@code PR}）、
 * {@code src/com/lineage/server/timecontroller/skill/Skill_Awake_Timer.java}（{@code AT}）。
 * 數值以 1-伺服器 算式寫成參照函式逐值比對；只能從原始碼確認的順序／條件以原始碼文字鎖住。
 * 正式環境（{@code CombatRules.loaded()}）與離線後備值不同時，兩種情況都檢查。</p>
 */
final class OtherClassSkillEffectServer1ParityTest {
    private static final Path MAIN = Path.of("src/main/java/local/lito381/server");
    /** {@code AL:284}：DRAGON_SKIN −10。 */
    private static final int SOURCE_DRAGON_SKIN = 10;
    /** {@code AL:285}：PATIENCE −15（註解「原始 -2」）。 */
    private static final int SOURCE_PATIENCE = 15;

    private static String source(String name) throws Exception {
        return Files.readString(MAIN.resolve(name), StandardCharsets.UTF_8);
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        assertTrue(from >= 0, start);
        int to = text.indexOf(end, from + start.length());
        assertTrue(to > from, end);
        return text.substring(from, to);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }

    // ---- 112 破壞盔甲 ×1.58 ------------------------------------------------------------

    /** {@code AP:1027-1029}：弓(20)／鐵手甲(62) 以外 {@code dmg = (int) (dmg * 1.58)}。 */
    @Test void armorBreakMultiplierMatchesL1AttackPcForEveryDamage() {
        assertEquals(158, BattleArmorBreak.DAMAGE_PERCENT);
        int[] weaponTypes = {0, 1, 4, 5, 10, 11, 13, 20, 24, 40, 46, 50, 58, 62};
        for (int damage = 0; damage <= 5000; damage++) {
            for (int weaponType : weaponTypes) {
                int action = CombatRules.weaponActionType(weaponType);
                int expected = action == 20 || action == 62 ? damage : (int) (damage * 1.58);
                assertEquals(expected, BattleArmorBreak.damageAfterBreak(damage, true, weaponType),
                        "damage=" + damage + " weapon=" + weaponType);
                assertEquals(damage, BattleArmorBreak.damageAfterBreak(damage, false, weaponType));
            }
        }
        assertEquals(11, BattleArmorBreak.damageAfterBreak(7, true, 0));    // 11.06
        assertEquals(158, BattleArmorBreak.damageAfterBreak(100, true, 0));
    }

    /**
     * 1-伺服器 只在 {@code L1AttackPc} 的 PC 目標分支（{@code AP:1027}）放大；NPC 目標沒有對應程式。
     * LiTo 正式環境的 NPC 目標經 {@code CombatNpcDebuffs.armorActive(active, null)} 一律不放大。
     */
    @Test void armorBreakAmplifiesOnlyPlayerTargetsInSourceMode() throws Exception {
        String debuffs = source("CombatNpcDebuffs.java");
        assertTrue(debuffs.contains(
                "static boolean armorActive(boolean active,ClientSession player){return active&&(!CombatRules.loaded()||player!=null);}"));
        assertFalse(CombatNpcDebuffs.armorActive(false, null));
        assertEquals(!CombatRules.loaded(), CombatNpcDebuffs.armorActive(true, null));
        String session = source("ClientSession.java");
        assertTrue(session.contains(
                "var83 = BattleArmorBreak.damageAfterBreak(var83, var130 = CombatNpcDebuffs.armorActive(var84, null), var24.weaponType());"));
        assertTrue(session.contains(
                "var163, var215 = CombatNpcDebuffs.armorActive(var215, var10), var39.weaponType()"));
        assertTrue(session.contains("if (var10 == null && var189 == null) {\n"
                + "                                       var163 = BattleArmorBreak.damageAfterBreak("));
    }

    // ---- PC 目標物理減傷順序（112 ×1.58 與 SKD3：料理、181、211） --------------------------

    /**
     * {@code AP:1021-1034}：增幅防禦 → 破壞盔甲 ×1.58 → {@code getCombatDmgBonus()}（SKD3 全部：
     * 料理 −5／−2、龍之護鎧、耐力）→ 聖結界減半。料理也在 SKD3，必須在 ×1.58 之後。
     */
    @Test void playerPhysicalReductionOrderMatchesL1AttackPc() throws Exception {
        String session = source("ClientSession.java");
        String body = between(session,
                "private int combatDamageAfterCooking(\n            int damage, String source, boolean playerPhysicalAttacker,\n            int attackerWeaponType, boolean applyReductionArmor,",
                "private int combatPhysicalDamageAfterArmorAndCooking(");
        int reductionArmor = body.indexOf("abilityStatus.reductionArmor(now)");
        int armorBreak = body.indexOf("BattleArmorBreak.damageAfterBreak(");
        int cooking = body.indexOf("cookingStatus.active(now)");
        int dragon = body.indexOf("dragonSkinStatus.snapshot(now)");
        int patience = body.indexOf("effectivePatiencePhysicalReduction(now)");
        int immune = body.indexOf("abilityStatus.immuneToHarm(now)");
        assertTrue(reductionArmor > 0 && armorBreak > reductionArmor && cooking > armorBreak
                        && dragon > cooking && patience > dragon && immune > patience,
                "ra=" + reductionArmor + " break=" + armorBreak + " cooking=" + cooking
                        + " dragon=" + dragon + " patience=" + patience + " immune=" + immune);
        assertEquals(1, occurrences(body, "cookingStatus.active(now)"));
        assertTrue(body.contains("BattleCookingCatalog.damageAfterReduction(applied, reduction)"));
    }

    /** {@code AP:1021-1034} 的算式（{@code dmg} 為 double；之後 {@code dmg <= 0} 視為未命中）。 */
    private static int sourcePlayerPhysical(int damage, int level, boolean reductionArmor,
                                            boolean armorBreak, int cooking,
                                            boolean dragonSkin, boolean patience) {
        double dmg = damage;
        if (reductionArmor) dmg -= (Math.max(level, 50) - 50) / 5 + 10;
        if (armorBreak) dmg = (int) (dmg * 1.58);
        dmg += -cooking + (dragonSkin ? -SOURCE_DRAGON_SKIN : 0) + (patience ? -SOURCE_PATIENCE : 0);
        return dmg <= 0 ? 0 : (int) dmg;
    }

    /** LiTo {@code combatDamageAfterCooking} 的步驟（正式環境減傷值），用各步驟的實際元件組合。 */
    private static int litoPlayerPhysical(int damage, int level, boolean reductionArmor,
                                          boolean armorBreak, int cooking,
                                          boolean dragonSkin, boolean patience,
                                          boolean cookingFirst) {
        int applied = damage;
        if (cookingFirst && cooking != 0) applied = BattleCookingCatalog.damageAfterReduction(applied, cooking);
        if (reductionArmor) {
            applied = BattleSpellCatalog.reductionArmorSpell(BattleSpellCatalog.REDUCTION_ARMOR)
                    .applyTo(applied, level, true);
        }
        applied = BattleArmorBreak.damageAfterBreak(applied, armorBreak, 0);
        if (!cookingFirst && cooking != 0) applied = BattleCookingCatalog.damageAfterReduction(applied, cooking);
        applied = new BattleDragonSkinStatus.Snapshot(dragonSkin, SOURCE_DRAGON_SKIN, 1).applyTo(applied);
        if (patience) applied = Math.max(0, applied - SOURCE_PATIENCE);
        return applied;
    }

    @Test void playerPhysicalReductionArithmeticMatchesL1AttackPc() {
        boolean oldOrderDiffers = false;
        for (int damage = 0; damage <= 400; damage++) {
            for (int level : new int[] {1, 49, 50, 54, 55, 60, 79, 99}) {
                for (int cooking : new int[] {0, 2, 5}) {
                    for (int flags = 0; flags < 16; flags++) {
                        boolean ra = (flags & 1) != 0, ab = (flags & 2) != 0;
                        boolean dragon = (flags & 4) != 0, patience = (flags & 8) != 0;
                        int expected = sourcePlayerPhysical(damage, level, ra, ab, cooking, dragon, patience);
                        assertEquals(expected,
                                litoPlayerPhysical(damage, level, ra, ab, cooking, dragon, patience, false),
                                "damage=" + damage + " level=" + level + " cooking=" + cooking + " flags=" + flags);
                        if (litoPlayerPhysical(damage, level, ra, ab, cooking, dragon, patience, true) != expected) {
                            oldOrderDiffers = true;
                        }
                    }
                }
            }
        }
        // 修正前「料理在 ×1.58 之前」：破壞盔甲＋料理時料理減傷被放大成 ×1.58。
        assertTrue(oldOrderDiffers);
        assertEquals(158 - 5, sourcePlayerPhysical(100, 1, false, true, 5, false, false));
        assertEquals(150, litoPlayerPhysical(100, 1, false, true, 5, false, false, true));
    }

    // ---- 181 龍之護鎧／211 耐力 減傷數值 ----------------------------------------------------

    /**
     * {@code AL:284-285}：正式值 181 −10、211 −15；LiTo 正式環境（{@code CombatRules.loaded()}）
     * 用這兩個值，離線後備維持 −10／−2（211 為 {@code AL:285} 註解的「原始 -2」）。
     */
    @Test void dragonSkinAndPatienceUseL1AttackListSkd3Literals() throws Exception {
        assertEquals(SOURCE_DRAGON_SKIN,
                BattleSpellCatalog.dragonSkinSpell(BattleSpellCatalog.DRAGON_SKIN).damageReduction());
        assertEquals(2, BattlePatienceStatus.PHYSICAL_REDUCTION);
        String reductions = source("CombatSourceReductions.java");
        assertTrue(reductions.contains("static int patience(int legacy){return CombatRules.loaded()?15:legacy;}"));
        assertTrue(reductions.contains("static int dragonSkin(int legacy){return CombatRules.loaded()?10:legacy;}"));
        int patience = CombatSourceReductions.patience(BattlePatienceStatus.PHYSICAL_REDUCTION);
        assertEquals(CombatRules.loaded() ? SOURCE_PATIENCE : 2, patience);
        assertEquals(SOURCE_DRAGON_SKIN, CombatSourceReductions.dragonSkin(10));
        assertEquals(patience, BattleSpellCatalog.patienceSpell(BattleSpellCatalog.PATIENCE).physicalDamageReduction());
        // 玩家魔法目標（L1MagicPc.java:880-907）：正式環境同樣減這兩個值。
        String magic = source("CombatPlayerMagic.java");
        assertTrue(magic.contains("result-=CombatSourceReductions.dragonSkin(10);"));
        assertTrue(magic.contains("result-=CombatSourceReductions.patience(2);"));
    }

    /**
     * {@code MN:554-586}：NPC 魔法打 PC 時，SKD3（181 −10、211 −15）和防具減免、增幅防禦一起在
     * {@code calcMrDefense}（{@code MN:526}）之前扣。T117 前 LiTo 三條怪物魔法路徑都沒扣。
     */
    @Test void monsterMagicSubtractsDragonSkinAndPatienceBeforeMagicResistance() throws Exception {
        String session = source("ClientSession.java");
        String helper = between(session, "int sourceNpcMagicSkillReduction(long nowNanos) {",
                "void serverCookingFireExpired(");
        assertTrue(helper.contains("if (!CombatRules.loaded()) return 0;"));
        assertTrue(helper.contains("dragonSkinStatus.snapshot(nowNanos).active()"));
        assertTrue(helper.contains("CombatSourceReductions.dragonSkin(10)"));
        assertTrue(helper.contains("effectivePatiencePhysicalReduction(nowNanos) != 0"));
        assertTrue(helper.contains("CombatSourceReductions.patience(\n                    BattlePatienceStatus.PHYSICAL_REDUCTION)"));
        assertEquals(3, occurrences(session, ".sourceNpcMagicSkillReduction("));
        for (String site : new String[] {
                "var23 = Math.addExact(var23, var15.owner().sourceNpcMagicSkillReduction(var8));",
                "var20 = Math.addExact(var20, var14.sourceNpcMagicSkillReduction(var30));",
                "var19 = Math.addExact(var19, var13.sourceNpcMagicSkillReduction(var30));"}) {
            int at = session.indexOf(site);
            assertTrue(at > 0, site);
            int reductionArmor = session.lastIndexOf("BattleSpellCatalog.reductionArmorSpell(88).reduction(", at);
            int resistance = session.indexOf("ClassicWorldMonsterMagicDamage.afterArmorAndResistance(", at);
            assertTrue(reductionArmor > 0 && at - reductionArmor < 400, site);
            assertTrue(resistance > at && resistance - at < 400, site);
        }
    }

    // ---- 206 專注 MPR+2 ------------------------------------------------------------------

    /** {@code PR:159-213}：WIS 基礎 1／2／3 ＋藍水 ＋技能表（冥想 5、專注 2、料理）＋原始 MPR ＋裝備。 */
    private static int sourceRegenMp(int wisdom, boolean bluePotion, boolean meditation,
                                     boolean concentration, int cooking, int originalMpr, int itemMpr) {
        int base = wisdom <= 14 ? 1 : wisdom <= 16 ? 2 : 3;
        if (bluePotion) base += wisdom < 11 ? 1 : wisdom - 10;
        if (meditation) base += 5;
        if (concentration) base += 2;
        base += cooking;
        if (originalMpr > 0) base += originalMpr;
        return base + itemMpr;
    }

    @Test void concentrationAddsTwoMprLikePcRegenExecutor() {
        assertEquals(2, BattleNaturalMpRegen.CONCENTRATION_BONUS);
        BattleSpellCatalog.RegenSpell spell = BattleSpellCatalog.regenSpell(BattleSpellCatalog.CONCENTRATION);
        assertEquals(BattleSpellCatalog.RegenEffect.CONCENTRATION, spell.effect());
        assertEquals(2, spell.mprBonus());
        assertFalse(spell.breaksOnActivity());
        for (int type = 0; type <= 6; type++) {
            for (int wisdom = 0; wisdom <= 60; wisdom++) {
                for (int originalWisdom = 0; originalWisdom <= 20; originalWisdom++) {
                    int originalMpr = BattleNaturalMpRegen.originalMpRegen(type, originalWisdom);
                    for (int item = 0; item <= 3; item++) {
                        var profile = new BattleNaturalMpRegen.Profile(type, wisdom, originalWisdom, item);
                        for (int flags = 0; flags < 8; flags++) {
                            boolean blue = (flags & 1) != 0, meditation = (flags & 2) != 0;
                            boolean concentration = (flags & 4) != 0;
                            for (int cooking : new int[] {0, 2, 3}) {
                                assertEquals(sourceRegenMp(wisdom, blue, meditation, concentration,
                                                cooking, originalMpr, item),
                                        BattleNaturalMpRegen.amount(profile, blue, meditation,
                                                concentration, cooking),
                                        "type=" + type + " wis=" + wisdom + " flags=" + flags);
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 205／210／215／220 立方 -----------------------------------------------------------

    /**
     * {@code L1SkillUse.java:2735-2757}（NPC 80149～80152、期限 buff_duration 20 秒）、
     * {@code EffectCubeExecutor.java:53}（半徑 3）、{@code L1EffectInstance.java:28}（狀態 8000 ms）、
     * {@code L1Cube.java:181-182}（900／1000 ms）、{@code :233-266} 燃燒第 4、8 跳 10 傷害、
     * {@code :269-300} 地裂第 4、8 跳束縛 1000 ms、{@code :307-319} 和諧 tick%4 MP+5、tick%5 傷害 25、
     * {@code EffectCubeExecutor.java:127-134} 盟友屬性 +30。
     */
    @Test void cubeConstantsMatchL1CubeAndEffectCubeExecutor() {
        assertEquals(80149, BattleCubeIgnitionEffects.NPC_ID);
        assertEquals(80150, BattleCubeQuakeEffects.NPC_ID);
        assertEquals(80151, BattleCubeShockEffects.NPC_ID);
        assertEquals(80152, BattleCubeHarmonyEffects.NPC_ID);
        for (int[] row : new int[][] {
                {BattleCubeIgnitionEffects.DURATION_SECONDS, BattleCubeIgnitionEffects.EFFECT_RADIUS,
                        BattleCubeIgnitionEffects.STATUS_SECONDS, BattleCubeIgnitionEffects.ENEMY_INITIAL_DELAY_MILLIS,
                        BattleCubeIgnitionEffects.ENEMY_TICK_MILLIS},
                {BattleCubeQuakeEffects.DURATION_SECONDS, BattleCubeQuakeEffects.EFFECT_RADIUS,
                        BattleCubeQuakeEffects.STATUS_SECONDS, BattleCubeQuakeEffects.ENEMY_INITIAL_DELAY_MILLIS,
                        BattleCubeQuakeEffects.ENEMY_TICK_MILLIS},
                {BattleCubeShockEffects.DURATION_SECONDS, BattleCubeShockEffects.EFFECT_RADIUS,
                        BattleCubeShockEffects.STATUS_SECONDS, BattleCubeShockEffects.ENEMY_INITIAL_DELAY_MILLIS,
                        BattleCubeShockEffects.ENEMY_TICK_MILLIS},
                {BattleCubeHarmonyEffects.DURATION_SECONDS, BattleCubeHarmonyEffects.EFFECT_RADIUS,
                        BattleCubeHarmonyEffects.STATUS_SECONDS, BattleCubeHarmonyEffects.INITIAL_DELAY_MILLIS,
                        BattleCubeHarmonyEffects.TICK_MILLIS}}) {
            assertEquals(20, row[0]);
            assertEquals(3, row[1]);
            assertEquals(8, row[2]);
            assertEquals(900, row[3]);
            assertEquals(1000, row[4]);
        }
        assertEquals(30, BattleCubeIgnitionEffects.ALLY_FIRE_RESISTANCE);
        assertEquals(30, BattleCubeQuakeEffects.ALLY_EARTH_RESISTANCE);
        assertEquals(30, BattleCubeShockEffects.ALLY_WIND_RESISTANCE);
        assertEquals(10, BattleCubeIgnitionEffects.ENEMY_DAMAGE);
        assertEquals(4, BattleCubeIgnitionEffects.ENEMY_DAMAGE_EVERY_TICKS);
        assertEquals(4, BattleCubeQuakeEffects.ENEMY_BIND_EVERY_TICKS);
        assertEquals(1000, BattleCubeQuakeEffects.ENEMY_BIND_MILLIS);
        assertEquals(5, BattleCubeHarmonyEffects.MP_RECOVERY);
        assertEquals(25, BattleCubeHarmonyEffects.HP_DAMAGE);
        assertEquals(4, BattleCubeHarmonyEffects.MP_EVERY_TICKS);
        assertEquals(5, BattleCubeHarmonyEffects.HP_EVERY_TICKS);
    }

    /**
     * {@code EffectCubeExecutor.java:72-98}：自己／同血盟／同隊伍先判為盟友；安全區只略過敵人。
     * T117 前經典世界的燃燒、地裂先判安全區，盟友在安全區拿不到屬性 +30（衝擊、和諧原本就對）。
     */
    @Test void classicWorldCubesResolveAlliesBeforeTheSafetyZone() throws Exception {
        String hub = source("GameHub.java");
        for (String kind : new String[] {"BattleCubeIgnitionEffects", "BattleCubeQuakeEffects",
                "BattleCubeShockEffects", "BattleCubeHarmonyEffects"}) {
            String marker = "if (ally) return " + kind + ".Relation.ALLY;";
            assertEquals(1, occurrences(hub, marker), kind);
            int ally = hub.indexOf(marker);
            int lambda = hub.lastIndexOf(kind + ".Alliance alliance = (casterId, targetId) -> {", ally);
            int safety = hub.indexOf(".isSafetyZone(", lambda);
            int ignore = hub.indexOf("return " + kind + ".Relation.IGNORE;", lambda);
            assertTrue(lambda > 0 && ally - lambda < 800, kind);
            assertTrue(safety > ally && ignore > safety, kind);
        }
    }

    /**
     * {@code L1Cube.java:303-304} 只設 STATUS_MR_REDUCTION，1-伺服器 沒有任何地方讀它，MR 不變。
     * LiTo 正式戰鬥房由 {@code NativeSourceCubeStatus.read} 回 0；T117 補上經典世界同樣的閘門。
     */
    @Test void cubeShockDoesNotChangeMagicResistanceInSourceMode() throws Exception {
        String status = source("NativeSourceCubeStatus.java");
        assertTrue(status.contains("query==3?0:"));
        String session = source("ClientSession.java");
        String body = between(session, "private int effectiveCubeShockMagicResistanceModifier(long nowNanos) {",
                "void serverCubeHarmonyExpired(");
        int world = body.indexOf("if (phase == Phase.WORLD) {");
        int gate = body.indexOf("if (CombatRules.loaded()) return 0;");
        int read = body.indexOf("hub.classicWorldCubeShockMagicResistanceModifier(");
        assertTrue(world > 0 && gate > world && read > gate);
        assertEquals(-10, BattleCubeShockEffects.ENEMY_MAGIC_RESISTANCE);   // 離線後備（刻意保留）
    }

    // ---- 116／118 呼喚盟友／援護盟友 ------------------------------------------------------

    /**
     * {@code RUN_CLAN.java:31-46}：目標在任一攻城旗區（{@code L1CastleLocation.checkInAllWarArea}，不看戰爭時間）
     * 或地圖不是 0／4／304 → 1192 並解除傳送鎖定。戰爭中可進旗區只屬於 116 的回答路徑（{@code C_Attr.java:1316-1329}）。
     */
    @Test void runClanRejectsEveryFlagAreaWithoutWarTimeException() throws Exception {
        String session = source("ClientSession.java");
        String allowed = between(session, "private static boolean runClanClassicWorldDestinationAllowed(",
                "private void useInvisibilitySpell(");
        assertTrue(allowed.contains("target.classicWorldInstanceId != -1"));
        assertTrue(allowed.contains("&& target.classicWorldMapId != 304)) return false;"));
        assertTrue(allowed.contains("if (target.classicWorldMapId != 4) return true;"));
        assertTrue(allowed.contains("return callClanClassicWorldCastleId(\n                target.positionX, target.positionY) == 0;"));
        assertFalse(allowed.contains("classicCastleWarActive("));
        // 116 回答 Yes 時仍保留「戰爭中可用」的例外。
        assertTrue(session.contains("classicCastleWarActive("));
    }

    /**
     * 待使用者裁定（§6.4）：1-伺服器 118 被 647／1192 拒絕時已播動作並扣 MP
     * （{@code L1SkillUse.java:979-983} 先 {@code runSkill}，{@code :1196-1198} 再 {@code useConsume}）；
     * LiTo 在扣費前拒絕。此測試鎖住現況，裁定後再改。
     */
    @Test void runClanMapRejectionsCurrentlyHappenBeforeCost() throws Exception {
        String session = source("ClientSession.java");
        String body = between(session, "private void useRunClanSpell(byte[] var1, BattleSpellCatalog.RunClanSpell var2)",
                "private static boolean runClanClassicWorldDestinationAllowed(");
        int escape = body.indexOf("this.rejectRunClan(var2, \"source-not-escapable\", 647, null);");
        int destination = body.indexOf("this.rejectRunClan(var2, \"destination-map-instance-or-war-area\", 1192, null);");
        int cost = body.indexOf("this.playerMp = NativeSkillConsumption.boundMp(this, this.playerMp - var9);");
        assertTrue(escape > 0 && destination > escape && cost > destination,
                "escape=" + escape + " destination=" + destination + " cost=" + cost);
    }

    // ---- 185／190／195 覺醒扣 MP -----------------------------------------------------------

    /**
     * {@code AT:106-123}、{@code AT:163-168}：每秒一拍，啟動後倒數 4 拍扣「最多 8」MP，再重設 4；
     * 只有仿正模式（{@code DragonKnightAwakeClassicMode=true}）才啟動。
     */
    @Test void awakeningDrainsUpToEightMpEveryFourSecondsOnlyInClassicMode() throws Exception {
        for (int skill : new int[] {BattleSpellCatalog.AWAKEN_ANTHARAS,
                BattleSpellCatalog.AWAKEN_FAFURION, BattleSpellCatalog.AWAKEN_VALAKAS}) {
            BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(skill);
            assertEquals(8, spell.mpDrainAmount(), "skill=" + skill);
            assertEquals(4, spell.mpDrainIntervalSeconds(), "skill=" + skill);
            BattleAwakenStatus status = new BattleAwakenStatus();
            long t0 = TimeUnit.SECONDS.toNanos(100);
            status.toggle(t0, skill, spell.armorClassModifier(), spell.maximumHpBonus(),
                    spell.dragonGfx(), spell.mpDrainAmount(), spell.mpDrainIntervalSeconds(),
                    spell.magicResistanceModifier(), spell.allElementalResistance(),
                    spell.allAbilityScoreBonus());
            assertEquals(0, status.pollMpDrainTicks(t0 + TimeUnit.SECONDS.toNanos(4) - 1));
            assertEquals(1, status.pollMpDrainTicks(t0 + TimeUnit.SECONDS.toNanos(4)));
            assertEquals(0, status.pollMpDrainTicks(t0 + TimeUnit.SECONDS.toNanos(7)));
            assertEquals(2, status.pollMpDrainTicks(t0 + TimeUnit.SECONDS.toNanos(12)));
        }
        String awaken = source("CombatAwaken.java");
        assertTrue(awaken.contains("return classicMode()?status.pollMpDrainTicks(now):0;"));
        assertTrue(awaken.contains("CombatRules.number(CombatRules.AWAKE_CLASSIC_MODE,1)!=0"));
        String rules = source("CombatRules.java");
        assertTrue(rules.contains("static final String AWAKE_CLASSIC_MODE = \"DragonKnightAwakeClassicMode\";"));
        assertTrue(rules.contains("destination.put(awakeKey, awakeClassicMode(characterProperties, character, defaults) ? 1.0 : 0.0);"));
        // debitCurrentMpUpTo(8)：最多扣到 0，不扣 HP。
        String session = source("ClientSession.java");
        String drain = between(session, "private void serverAwakeningMpDrain(long l)", "private void serverNaturalHpRegen(");
        assertTrue(drain.contains("this.playerMp = (int)Math.max(0L, (long)this.playerMp - l2);"));
        assertTrue(drain.contains("if (!snapshot.active() || this.playerHp <= 0) {"));
        assertFalse(drain.contains("playerHp -"));
    }
}
